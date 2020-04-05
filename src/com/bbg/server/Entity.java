package com.bbg.server;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.bbg.shared.Shared;
import com.bbg.shared.Vector;

public class Entity {

	public Game game;

	public float x = 0;
	public float y = 0;
	public float direction; // rads, d00d

	public boolean remove = false;

	public float speedX = 0;
	public float speedY = 0;

	public float lastX = 0;
	public float lastY = 0;

	public float density = 0;
	public float friction = 0;
	public float restitution = 0;
	
	public float rot = 0;

	Body body;

	Shape[] p;

	public Entity() {

	}

	public Entity(Game game, float X, float Y) {
		this.game = game;
		this.x = X;
		this.y = Y;
	}

	public void remove() {
		remove = true;
	}

	
	
	public boolean active() {
		if (game.gameState != 2) {
			return false;
		}
		// for typical entitites, just making sure remove=false
		if (this instanceof Player) {
			Player p = (Player) this;
			return p.activelyPlaying && p.joined() && !remove && !p.spectating;
		} else {
			return !remove;
		}
	}

	public void destroy() {
		destroyBody();
	}

	public void setupBody(Shape[] p, float density, float friction, float restitution) {
		this.p = p;
		this.density = density;
		this.friction = friction;
		this.restitution = restitution;
	}

	public void createBody(short category, short mask, boolean sensor, Entity ud, boolean dynamic,
			boolean continuousTunnel) {
		// This is a generic dynamic body creation function using x, y, array of
		// shapes, density, friction, and restitution
		BodyDef bodyDef = new BodyDef();
		if (dynamic) {
			bodyDef.type = BodyType.DynamicBody;
		} else {
			bodyDef.type = BodyType.StaticBody;
		}
		bodyDef.position.set(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale);
		if(this instanceof Bullet) {
			bodyDef.angle = direction;
		}
		bodyDef.angularVelocity = rot;
		body = game.world.createBody(bodyDef);
		body.setUserData(ud);
		for (Shape s : p) {
			FixtureDef fixtureDef = new FixtureDef();
			fixtureDef.isSensor = sensor;
			fixtureDef.shape = s;
			fixtureDef.density = density;
			fixtureDef.friction = friction;
			fixtureDef.restitution = restitution;
			fixtureDef.filter.categoryBits = category;
			fixtureDef.filter.maskBits = mask;
			body.createFixture(fixtureDef);
		}
		body.setLinearDamping(0);
		if (continuousTunnel) {
			body.setBullet(true);
		}
	}

	public void destroyBody() {
		if (body != null && game.gameState == 2) {
			game.world.destroyBody(body);
		}
		body = null;
	}

	void capSpeed(float limit) {

		Vector curV = Vector.byChange(speedX, speedY);
		float sp = curV.intensity;
		if (sp > limit) {
			sp = limit;
		}
		Vector newV = new Vector(curV.direction, sp);
		speedX = newV.xChange;
		speedY = newV.yChange;
	}

	public void preStep() {
		if (this instanceof Player) {
			Player p = (Player) this;
			p.preStep();
		} else if (this instanceof Bullet) {
			if (active()) {
				body.setTransform(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale, direction);
				body.setAngularVelocity(rot);
				body.setLinearVelocity(speedX, speedY);
			}
		} else if (this instanceof Flag) {
			Flag f = (Flag) this;
			if (!f.held && body != null) {
				body.setTransform(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale, 0);
				body.setLinearVelocity(speedX, speedY);

			}
		} else {
			if (active()) {
				if (body != null) {
					body.setLinearVelocity(speedX, speedY);
				}
			}
		}
	}

	public void postStep() {
		if (body == null) {
			return;
		}
		if (this instanceof Player) {
			Player p = (Player) this;
			if (p.dead) {
				return;
			}
		}
		if (this instanceof Flag) {
			Flag f = (Flag) this;
			if (f.held) {
				return;
			}

		}
		if (active() && body != null) {
			speedX = body.getLinearVelocity().x;
			speedY = body.getLinearVelocity().y;
			lastX = x;
			lastY = y;
			x = body.getPosition().x * Shared.xml.physicsScale;
			y = body.getPosition().y * Shared.xml.physicsScale;
			direction = body.getAngle();
		}

	}

	public boolean inRange(Entity e) {
		if (Math.abs(x - e.x) < Config.xml.inRangeDistance && Math.abs(y - e.y) < Config.xml.inRangeDistance) {
			return true;
		}
		return false;
	}

}
