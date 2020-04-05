package com.bbg.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Entities;
import com.bbg.shared.Network.FlagData;
import com.bbg.shared.Network.SyncData;
import com.bbg.shared.Shared;

public class Flag extends Entity {

	public int index = 0;
	public int team = 0;

	public int frame = 0;
	public long frameStamp = 0;

	public boolean held = false;
	public boolean home = true;
	public int d = 0;
	public int holder = -1;

	public Flag(Scene scene, int index, int team, float x, float y) {
		this.scene = scene;
		this.x = x;
		this.index = index;
		this.team = team;
		this.y = y;
		this.newX = x;
		this.newY = y;
		this.drawX = x;
		this.drawY = y;
		this.drawDirection = 0;
		this.newDirection = 0;
		this.curDirection = 0;
		this.scale = 1.0f;
		active = true;
		speedX = 0;
		speedY = 0;
		setupBody(Entities.getCircleArray(16, Shared.xml.physicsScale), 1f, 0.5f, 0.5f);
		// createBody(Prefs.CATEGORYSOLID, Prefs.MASKSOLID, false);
		// body.setFixedRotation(true);
	}

	public void sync(long serverTick, FlagData fd) {
		SyncData sd = new SyncData();
		float oldX = drawX;
		float oldY = drawY;
		sd.x = (short) fd.x;
		sd.y = (short) fd.y;
		sd.vX = fd.vX;
		sd.vY = fd.vY;
		sd.direction = 0;
		super.sync(serverTick, sd);
		if (held && !fd.held && !fd.home) {
			// dropped in field
			// make sparks where it was held
			if (holder >= 0 && scene.players[holder] != null) {
				scene.playSound(11, 1, 1);
				scene.addChat(scene.players[holder].name + " has dropped the " + scene.teams[team].name + " flag",
						Color.RED);
				scene.makePoop(15, scene.players[holder].drawX, scene.players[holder].drawY, 7, Color.WHITE, true);
			}
			drawX = sd.x;
			drawY = sd.y;
		}
		if (home && !held && fd.held) {
			// grabbed from base, make sparks at base
			if (fd.holder >= 0 && scene.players[fd.holder] != null) {
				scene.addChat(scene.players[fd.holder].name + " has grabbed the " + scene.teams[team].name + " flag",
						Color.RED);
				scene.playSound(10, 1, 1);
				scene.makePoop(15, scene.players[fd.holder].drawX, scene.players[fd.holder].drawY, 7, Color.WHITE,
						true);
			}
		}
		if (!home && !held && fd.held) {
			// grabbed from field, make sparks on holder
			if (fd.holder >= 0 && scene.players[fd.holder] != null) {
				scene.addChat(scene.players[fd.holder].name + " has grabbed the " + scene.teams[team].name + " flag",
						Color.RED);
				scene.playSound(10, 1, 1);
				scene.makePoop(15, scene.players[fd.holder].drawX, scene.players[fd.holder].drawY, 7, Color.WHITE,
						true);
			}
		}

		if (!home && !held && !fd.held && fd.home) {
			// returned home from field
			// make sparks on returner
			scene.makePoop(15, oldX, oldY, 7, Color.WHITE, true);
			if (fd.holder >= 0) {
				if (scene.players[fd.holder] != null) {
					String st = scene.players[fd.holder].name + " has returned the " + scene.teams[team].name + " flag";
					scene.addChat(st, Color.RED);
				} else {
					scene.addChat("Someone has returned the " + scene.teams[team].name + " flag", Color.RED);
				}
			} else {
				scene.addChat("The " + scene.teams[team].name + " flag was returned to base", Color.RED);
			}
			if (team == scene.players[scene.screen.myIndex].team) {
				scene.playSound(13, 1, 1);
			} else {
				scene.playSound(12, 1, 1);
			}

			scene.makePoop(15, scene.base[team].x, scene.base[team].y, 7, Color.WHITE, true);
			drawX = sd.x;
			drawY = sd.y;
		}
		if (!home && fd.home && held && !fd.held) {
			scene.addChat(scene.players[holder].name + " has captured the " + scene.teams[team].name + " flag",
					Color.RED);
			if (team == scene.players[scene.screen.myIndex].team) {
				scene.playSound(15, 0.7f, 1);
			} else {
				scene.playSound(16, 0.7f, 1);
			}
			// capped, sparks at both bases
			scene.makePoop(15, scene.base[0].x, scene.base[0].y, 7, Color.WHITE, true);
			scene.makePoop(15, scene.base[1].x, scene.base[1].y, 7, Color.WHITE, true);
		}
		held = fd.held;
		home = fd.home;
		holder = fd.holder;

		if (body == null && !held) {
			createMyBody();
		}

	}

	public void preStep() {
		if (!held && body != null) {
			body.setTransform(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale, 0);
			x = drawX;
			y = drawY;
			body.setLinearVelocity(speedX, speedY);
		}

	}

	public void postStep() {
		if (!held && body != null) {
			speedX = body.getLinearVelocity().x;
			speedY = body.getLinearVelocity().y;
			newX = body.getPosition().x * Shared.xml.physicsScale;
			newY = body.getPosition().y * Shared.xml.physicsScale;
			drawDirection = 0;
			newDirection = 0;
		}
	}

	public void createMyBody() {
		if (!held) {
			if (home) {
				if (team == 1) {
					createBody(CollisionFlags.CAT_T1FLAG, CollisionFlags.MASK_T1FLAG, false, false, false);
				} else {
					createBody(CollisionFlags.CAT_T2FLAG, CollisionFlags.MASK_T2FLAG, false, false, false);
				}
			} else {
				createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, false, true, false);
			}
			body.setFixedRotation(true);
		}
	}

	public void render(GameScreen screen) {
		if (scene.getMe().dead && scene.tick > scene.respawnStamp) {
			return;
		}
		if (scene.tick > frameStamp) {
			frameStamp = scene.tick + 100;
			frame++;
			if (frame >= 14) {
				frame = 0;
			}
			d -= 8;
			if (d < 0) {
				d += 360;
			}
		}
		if (!held) {
			screen.batcher.setColor(screen.scene.teams[team].col());
			Color dg = new Color(screen.scene.teams[team].col());
			dg.a = screen.scene.base[team].glowVal;
			TextureRegion pole = AssetLoader.getSprite("banner");
			screen.drawRegion(pole, drawX, drawY, true, 0, 0.4f);
			screen.batcher.setColor(dg);
			TextureRegion poleg = AssetLoader.getSprite("bannerg");
			screen.drawRegion(poleg, drawX, drawY, true, 0, 0.4f);
			scene.screen.batcher.setColor(Color.WHITE);
		}
	}

	public void renderBase(GameScreen screen) {
		if (scene.getMe().dead && scene.tick > scene.respawnStamp) {
			return;
		}
		if (!held) {
			screen.batcher.setColor(scene.teams[team].col());
			Color dg = new Color(scene.teams[team].col());
			dg.a = scene.base[team].glowVal;
			TextureRegion disc = AssetLoader.getSprite("flag");
			screen.drawRegion(disc, drawX, drawY, true, d, 0.5f);
			screen.batcher.setColor(dg);
			TextureRegion discg = AssetLoader.getSprite("flagg");
			screen.drawRegion(discg, drawX, drawY, true, d, 0.5f);
			screen.batcher.setColor(Color.WHITE);
		}
	}

	public static void renderBanner(GameScreen screen, float x, float y, int team) {
		if (screen.scene.getMe().dead && screen.scene.tick > screen.scene.respawnStamp) {
			return;
		}
		screen.batcher.setColor(screen.scene.teams[team].col());
		Color dg = new Color(screen.scene.teams[team].col());
		dg.a = screen.scene.base[team].glowVal;
		TextureRegion pole = AssetLoader.getSprite("banner");
		screen.drawRegion(pole, x, y, true, 0, 0.4f);
		screen.batcher.setColor(dg);
		TextureRegion poleg = AssetLoader.getSprite("bannerg");
		screen.drawRegion(poleg, x, y, true, 0, 0.4f);
		screen.batcher.setColor(Color.WHITE);
	}

}
