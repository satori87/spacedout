package com.bbg.client;

import java.util.LinkedList;
import java.util.List;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.bbg.shared.Entities;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Random;
import com.bbg.shared.Vector;
import com.bbg.shared.Shared;

import com.bbg.shared.ShipDef;
import com.bbg.shared.Network.SyncData;

public class Player extends Entity {

	public long lastPoop = 0;
	public long flySoundStamp = 0;
	public int index;
	public String name = "";
	public boolean bodySetup = false;
	public int hp = 0;
	public int maxhp = 0;
	public float cD = 0;
	public int kills = 0, deaths = 0, score = 0;
	public int armor = 0;
	public int team = 0;

	boolean turboing = false;
	boolean healing = true;
	public long flickerStamp = 0;
	public int flicker = 0;
	float glowVal = 0.0f;
	boolean glowingUp = true;
	public int ping = 0;
	public boolean shielding = false;
	
	List<Vector2> contacts = new LinkedList<Vector2>();
	public float pan = 0;
	public Color col = Entities.randomBrightColor();
	public boolean reloading = false;
	public boolean panup = true;
	public float desiredR = 0;

	public long carrySoundStamp = 0;

	public boolean accel = false, decel = false, strafeLeft = false, strafeRight = false;

	public boolean dead = false;

	public boolean playing = false;

	public List<PolyPoop> poop = new LinkedList<PolyPoop>();

	public Player(Scene scene, int index, String name, float x, float y, float direction) {
		this.scene = scene;
		this.name = name;
		this.x = x;
		this.index = index;
		this.y = y;
		this.newX = x;
		this.newY = y;
		this.drawX = x;
		this.drawY = y;
		this.drawDirection = direction;
		this.newDirection = direction;
		this.scale = 1f;

		active = true;

	}

	public void setupBody() {
		setupBody(ShipDef.getShipShape(1, Shared.xml.physicsScale, ShipDef.getShipScale(armor)),
				ShipDef.getShipDensity(armor), Shared.xml.shipFriction, Shared.xml.shipRestitution);
		bodySetup = true;
	}

	public void sync(long serverTick, SyncData sd) {
		if (!bodySetup) {
			return;
		}
		super.sync(serverTick, sd);
		active = true;
		playing = true;
		if (!dead) {
			createMyBody();
		}
		accel = sd.accel;
		decel = sd.decel;
		strafeLeft = sd.strafeLeft;
		strafeRight = sd.strafeRight;
		desiredR = sd.desiredR;
		turboing = sd.turbo;
		healing = sd.healing;
		shielding = sd.shielding;
	}

	void checkWalls() {
		for (Contact c : scene.world.getContactList()) {
			Entity e1 = (Entity) c.getFixtureA().getBody().getUserData();
			Entity e2 = (Entity) c.getFixtureB().getBody().getUserData();
			if (c.isTouching()) {
				if (e1 instanceof WallEntity && !(e2 instanceof WallEntity)) {
					WallEntity we = (WallEntity) e1;
					we.wall.blink = true;
					we.wall.blinkStamp = scene.tick + 200;
				}
				if (e2 instanceof WallEntity && !(e1 instanceof WallEntity)) {
					WallEntity we = (WallEntity) e2;
					we.wall.blink = true;
					we.wall.blinkStamp = scene.tick + 200;
				}
			}
		}
	}

	
	float desiredR() {
		if (index == scene.screen.myIndex) {
			return scene.cs.desiredRotation;
		} else {
			return desiredR;
		}
	}
	

	
	
	public void preStep() {
		ShipDef.rotateToDesired(body, desiredR(), curDirection, ShipDef.getShipRotationSpeed(armor));
		checkWalls();
		body.setTransform(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale, curDirection);
		x = drawX;
		y = drawY;
		if (accel) {
			Vector v = new Vector(curDirection, ShipDef.getShipThrust(0, armor, turboing));
			body.applyForceToCenter(v.xChange, v.yChange, true);
		} else if (decel) {
			Vector v = new Vector(curDirection, ShipDef.getShipThrust(1, armor, turboing));
			body.applyForceToCenter(-v.xChange, -v.yChange, true);
		}
		if (strafeLeft) {
			Vector v = new Vector(curDirection - (float) Math.toRadians(90), ShipDef.getShipThrust(2, armor, turboing));
			body.applyForceToCenter(v.xChange, v.yChange, true);
		} else if (strafeRight) {
			Vector v = new Vector(curDirection + (float) Math.toRadians(90), ShipDef.getShipThrust(2, armor, turboing));
			body.applyForceToCenter(v.xChange, v.yChange, true);
		}
		capSpeed();

		body.setLinearVelocity(speedX, speedY);
	}

	public void update() {
		super.update();
		makeSound();
	}

	void makeSound() {
		if (!active || !playing || dead) {
			return;
		}
		if (scene.tick > flySoundStamp) {
			flySoundStamp = scene.tick + 160;
			if (turboing) {
				flySoundStamp = scene.tick + 80;
			}
			if (index == scene.screen.myIndex) {
				if (scene.cs.accel || scene.cs.decel || scene.cs.strafeLeft || scene.cs.strafeRight) {
					scene.playSound(0, 1, 1);
					scene.playSound(0, 1, 1.5f);
				}
			} else {
				if (accel || decel || strafeLeft || strafeRight) {
					scene.play3D(0, drawX, drawY, 1, 1);
					scene.play3D(0, drawX, drawY, 1, 1.5f);
				}
			}
		}
		if (scene.info.type == 2) {
			if (scene.flag[1 - team] != null) {
				if (scene.flag[1 - team].held && scene.flag[1 - team].holder == index) {
					if (scene.tick > carrySoundStamp) {
						carrySoundStamp = scene.tick + 2000;
						scene.play3D(9, drawX, drawY, 1, 1);
					}
				}
			}
		}
	}

	private void capSpeed() {
		Player p = (Player) this;
		float limit = ShipDef.getShipSpeed(p.armor, turboing);
		Vector curV = Vector.byChange(speedX, speedY);
		float sp = curV.intensity;
		if (sp > limit) {
			sp = limit;
		}
		Vector newV = new Vector(curV.direction, sp);
		speedX = newV.xChange;
		speedY = newV.yChange;
	}

	public long healSoundStamp = 0;
	public long shieldSoundStamp = 0;

	public void postStep() {
		speedX = body.getLinearVelocity().x;
		speedY = body.getLinearVelocity().y;
		newX = body.getPosition().x * Shared.xml.physicsScale;
		newY = body.getPosition().y * Shared.xml.physicsScale;
		checkWalls();
		//newDirection = curDirection;
		newDirection = body.getAngle(); // should be unchanged
		makePoop(turboing);
		if (turboing) {
			makePoop(turboing);

		}
		if (healing) {
			if (scene.tick > healSoundStamp) {
				healSoundStamp = scene.tick + 100;
				scene.play3D(8, drawX, drawY, 1, 1);
			}
			dePoop(10, Color.GREEN, false, 40, 5);

		}
		if (shielding) {
			if (scene.tick > shieldSoundStamp) {
				shieldSoundStamp = scene.tick + 350;
				scene.play3D(7, drawX, drawY, 1, 1);
			}
			dePoop(5, getColor(.8f), true, 8, 8);
		}
		// lets do our exhaust jawn
	}

	public Color getColor(float a) {
		Color c;
		if (scene.info.type == 0) {
			c = new Color(col);
			c.a = a;
			return c;
		} else {
			c = new Color(scene.teams[team].col());
			c.a = a;
			return c;
		}

	}

	void makeSparks(Vector2 pt, float factor) {
		for (int i = 0; i < 3; i++) {
			Color col = getColor(1);
			col.r = col.r - .2f + (float) (Math.random() * .4f);
			col.g = col.g - .2f + (float) (Math.random() * .4f);
			col.b = col.b - .2f + (float) (Math.random() * .4f);
			if (col.r > 1) {
				col.r = 1;
			}
			if (col.g > 1) {
				col.g = 1;
			}
			if (col.b > 1) {
				col.b = 1;
			}
			if (col.r < 0) {
				col.r = 0;
			}
			if (col.g < 0) {
				col.g = 0;
			}
			if (col.b < 0) {
				col.b = 0;
			}
			float intensity = factor * (2f + (float) (Math.random() * 2));
			float size = factor * (.1f + (float) (Math.random() / 10f));
			PolyPoop p = new PolyPoop(scene, pt.x, pt.y, 350 + Random.getInt(300), intensity, size, col);
			scene.explosions.add(p);
		}
	}

	public void render(GameScreen screen) {
		// render thyself, peasant
		if (dead || !playing || !active) {
			return;
		}
		float r = (float) Math.toDegrees(drawDirection);
		try {
			List<PolyPoop> drops = new LinkedList<PolyPoop>();
			for (PolyPoop p : poop) {
				if (p.remove) {
					drops.add(p);
				}
			}
			for (PolyPoop p : drops) {
				poop.remove(p);
			}
			drops.clear();
			// lets do our exhaust jawn
			for (PolyPoop p : poop) {
				if (!p.fore) {
					p.render(screen, drawX, drawY);
				}
			}
			for (Vector2 v : contacts) {
				scene.play3D(3, v.x * Shared.xml.physicsScale, v.y * Shared.xml.physicsScale, 0.4f, 1);
				makeSparks(new Vector2(v.x * Shared.xml.physicsScale, v.y * Shared.xml.physicsScale), 1);
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		contacts.clear();

		if (scene.tick > flickerStamp) {
			flickerStamp = scene.tick + 20;
			flicker += 1;
			if (flicker >= 8) {
				flicker = 0;
			}
			if (glowingUp) {
				glowVal += 0.08f;
				if (glowVal > 0.5f) {
					glowingUp = false;
				}
			} else {
				glowVal -= 0.08f;
				if (glowVal < 0f) {
					glowingUp = true;
				}
			}
		}
		if (glowVal > 1) {
			glowVal = 1;
		}
		if (glowVal < 0) {
			glowVal = 0;
		}
		// renderExhaust(screen);
		// renderPolygonExhaust(screen);
		TextureRegion tex = AssetLoader.getSprite("shape1");

		screen.batcher.setColor(getColor(glowVal));
		screen.drawRegion(tex, drawX, drawY, centered, r, ShipDef.getShipScale(armor));
		if (index == scene.screen.myIndex) {
			Color retCol = Entities.getColor(255, 255, 255, 0.4f);
			screen.batcher.setColor(retCol);
			// getColor(0.4f)
			TextureRegion ret = AssetLoader.getSprite("ret");
			Vector v = new Vector(drawDirection, 96);
			screen.drawRegion(ret, drawX + Math.round(v.xChange), drawY + Math.round(v.yChange), centered, r, 2);
		}
		// {
		// if(scene.tick > soundStamp) {
		// Sound snd = AssetLoader.snd[0];
		// Vector v = new Vector(cD,1);
		// snd.play(1, 1+(v.yChange/10f), v.xChange);
		// soundStamp = scene.tick + 200;
		// }
		// }
		screen.batcher.setColor(getColor(1));

		screen.drawRegion(tex, drawX, drawY, centered, r, ShipDef.getShipScale(armor));

		screen.batcher.setColor(Color.WHITE);
		try {
			for (PolyPoop p : poop) {
				if (p.fore) {
					p.render(screen, drawX, drawY);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		 * if(def) { Color scol = new Color(getColor(.8f)); scol.r = scol.r -
		 * .2f + (float)(Math.random() * .2f); scol.g = scol.g - .2f +
		 * (float)(Math.random() * .2f); scol.b = scol.b - .2f +
		 * (float)(Math.random() * .2f); if(scol.r > 1) {scol.r = 1;} if(scol.g
		 * > 1) {scol.g = 1;} if(scol.b > 1) {scol.b = 1;} if(scol.r < 0)
		 * {scol.r = 0;} if(scol.g < 0) {scol.g = 0;} if(scol.b < 0) {scol.b =
		 * 0;} screen.batcher.setColor(scol);
		 * screen.drawRegion(AssetLoader.getSprite("shape2"), drawX, drawY,
		 * true, 0, defV); }
		 */
		screen.batcher.setColor(Color.WHITE);
	}

	public static float invSqrt(float x) {
		float xhalf = 0.5f * x;
		int i = Float.floatToIntBits(x);
		i = 0x5f3759df - (i >> 1);
		x = Float.intBitsToFloat(i);
		x *= (1.5f - xhalf * x * x);
		return x;
	}

	void dePoop(int it, Color pcol, boolean rot, float dist, float in) {
		float pX = 0;
		float pY = 0;
		float pD = 0;
		float pI = 0;
		for (int i = 0; i < it; i++) {
			Color col = new Color(pcol);
			col.r = col.r - .2f + (float) (Math.random() * .4f);
			col.g = col.g - .2f + (float) (Math.random() * .4f);
			col.b = col.b - .2f + (float) (Math.random() * .4f);
			if (col.r > 1) {
				col.r = 1;
			}
			if (col.g > 1) {
				col.g = 1;
			}
			if (col.b > 1) {
				col.b = 1;
			}
			if (col.r < 0) {
				col.r = 0;
			}
			if (col.g < 0) {
				col.g = 0;
			}
			if (col.b < 0) {
				col.b = 0;
			}
			pD = Random.dir();
			pI = dist;
			Vector v = new Vector(pD, pI);
			pX = v.xChange;
			pY = v.yChange;
			PolyPoop p = new PolyPoop(scene, pD + (float) Math.PI, pX, pY, 600, in, .22f, col);
			p.rot = rot;
			poop.add(p);
		}

	}

	void makePoop(boolean turbo) {
		boolean acc, dec, sleft, sright;
		if (index == scene.screen.myIndex) { // for our own thrust, use client
												// side info to feel more
												// responsive
			acc = scene.cs.accel;
			dec = scene.cs.decel;
			sleft = scene.cs.strafeLeft;
			sright = scene.cs.strafeRight;
		} else {
			acc = accel;
			dec = decel;
			sleft = strafeLeft;
			sright = strafeRight;
		}
		float pX = 0;
		float pY = 0;
		float pD = 0;
		float pI = 0;

		float tf = 1f;
		if (turbo) {
			tf = 2f;
		}
		if (acc) {
			for (int i = 0; i < 5 * tf; i++) {
				pD = drawDirection + (float) Math.PI - 0.3f + (float) (Math.random() * 0.6f);
				pI = 12 + Random.getInt(5);
				Vector v = new Vector(pD, pI);
				pX = drawX + v.xChange;
				pY = drawY + v.yChange;
				PolyPoop p = new PolyPoop(scene, pX, pY, drawDirection);
				poop.add(p);
			}
		} else if (dec) {
			for (int i = 0; i < 2 * tf; i++) {
				pD = drawDirection + (float) Math.toRadians(180 + 45) + (float) Math.PI - 0.3f
						+ (float) (Math.random() * 0.6f);
				pI = 12 + Random.getInt(5);
				Vector v = new Vector(pD, pI);
				pX = drawX + v.xChange;
				pY = drawY + v.yChange;
				PolyPoop p = new PolyPoop(scene, pX, pY, drawDirection + (float) Math.toRadians(180 + 45));
				poop.add(p);
			}
			for (int i = 0; i < 2 * tf; i++) {
				pD = drawDirection + (float) Math.toRadians(180 - 45) + (float) Math.PI - 0.3f
						+ (float) (Math.random() * 0.6f);
				pI = 12 + Random.getInt(5);
				Vector v = new Vector(pD, pI);
				pX = drawX + v.xChange;
				pY = drawY + v.yChange;
				PolyPoop p = new PolyPoop(scene, pX, pY, drawDirection + (float) Math.toRadians(180 - 45));
				poop.add(p);
			}

		}
		if (sright) {
			for (int i = 0; i < 3 * tf; i++) {
				pD = drawDirection + (float) Math.toRadians(180 - 45) + (float) Math.PI - 0.3f
						+ (float) (Math.random() * 0.6f);
				pI = 12 + Random.getInt(5);
				Vector v = new Vector(pD, pI);
				pX = drawX + v.xChange;
				pY = drawY + v.yChange;
				PolyPoop p = new PolyPoop(scene, pX, pY, drawDirection + (float) Math.toRadians(180 - 45));
				poop.add(p);
			}
		} else if (sleft) {
			for (int i = 0; i < 3 * tf; i++) {
				pD = drawDirection + (float) Math.toRadians(180 + 45) + (float) Math.PI - 0.3f
						+ (float) (Math.random() * 0.6f);
				pI = 12 + Random.getInt(5);
				Vector v = new Vector(pD, pI);
				pX = drawX + v.xChange;
				pY = drawY + v.yChange;
				PolyPoop p = new PolyPoop(scene, pX, pY, drawDirection + (float) Math.toRadians(180 + 45));
				poop.add(p);
			}
		}

	}

	public boolean hasAmmo() {
		return true;
	}

	public void reset() {
		Player p = this;
		p.accel = false;
		p.playing = false;
		p.deaths = 0;
		p.decel = false;
		p.hp = p.maxhp;
		p.kills = 0;
		p.reloading = false;
		p.score = 0;
		p.strafeLeft = false;
		p.strafeRight = false;
	}

	public void renderText(GameScreen screen) {
		if (!playing || !active) {
			return;
		}
		if (name.length() > 0) {
			Color c = Color.WHITE;
			if (index == scene.screen.myIndex) {
				float p = (float) hp / (float) maxhp;
				if (p > 0.75) {
					c = Color.GREEN;
				} else if (p > 0.5) {
					c = Color.YELLOW;
				} else if (p > 0.25) {
					c = Color.ORANGE;
				} else {
					c = Color.RED;
				}
				int weapon = scene.weapon;
				String weaponName = Entities.weapons[weapon].name;
				String shotsLeft = Integer.toString(scene.shotsLeft);
				String clip = Integer.toString(Entities.weapons[weapon].clip);
				String weapStr = weaponName;
				if ((Entities.weapons[weapon].hasClip)) {
					weapStr = weapStr + " [" + shotsLeft + "/" + clip + "]";
				} else {
					// weapStr = weapStr + ;
				}
				screen.drawFont(0, drawX, drawY - 15, "hp: " + hp + "/" + maxhp, true, 1.0f, c);

				if (!hasAmmo()) {
					if (scene.step250.step[2] == 1) {
						screen.drawFont(0, drawX, drawY + 15, "OUT OF AMMO", true, 1.0f, Color.RED);
					}
				} else if (reloading) {
					if (scene.step250.step[2] == 1) {
						screen.drawFont(0, drawX, drawY + 15, "RELOADING", true, 1.0f, Color.RED);
					}
				} else {
					screen.drawFont(0, drawX, drawY + 15, weapStr, true, 1.0f, Color.CYAN);
				}
				c = Color.WHITE;
			} else {
				if (scene.info.numTeams == 0) {
					c = Color.RED;
				} else if (scene.players[scene.screen.myIndex].team == team) {
					c = Color.CYAN;
				} else {
					c = Color.RED;
				}
			}
			screen.drawFont(0, drawX, drawY, name, true, 1.0f, c);
		}
	}

	public void createMyBody() {
		if (scene.info.type == 2) {
			if (team == 1) {
				createBody(CollisionFlags.CAT_T1PLAYER, CollisionFlags.MASK_T1PLAYER, false, true, false);
			} else {
				createBody(CollisionFlags.CAT_T2PLAYER, CollisionFlags.MASK_T2PLAYER, false, true, false);
			}
		} else {
			createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, false, true, false);
		}
	}

}
