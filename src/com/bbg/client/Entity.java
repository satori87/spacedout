package com.bbg.client;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.bbg.shared.Vector;
import com.bbg.shared.Shared;

import com.bbg.shared.Network.SyncData;

public class Entity {

	public long lastPacketTick=0;
	public long newPacketTick=0;
	public int drawDuration=0;
	public long drawStart=0;
	public float rot = 0;
	public long removeAt = 0;

	public long lastUpdate = 0; // this is just like newPacketTick except it
								// uses OUR local time, not servers!

	Body body;
	Shape[] p;

	public boolean active = true;

	public Scene scene;
	public boolean requiresUpdate = true;
	public float x = 0;
	public float y = 0;
	public float drawX = 0;
	public float drawY = 0; // the tweened coordinates between x/y and newX/Y.
							// these are what are drawn
	public float newX = 0;
	public float newY = 0;
	public float curDirection = 0; // rads, d00d
	public float newDirection = 0;
	public float drawDirection = 0;

	public int drawFrame = 0; // which member of the tex array
	public int numDrawFrames = 0;
	public boolean centered = true;
	public float scale = 1.0f;

	public float speedX = 0;
	public float speedY = 0;

	public float density = 0;
	public float friction = 0;
	public float restitution = 0;

	public Entity() {

	}

	public Entity(Scene scene, float x, float y, float direction) {
		this.scene = scene;
		this.x = x;
		this.y = y;
		this.curDirection = direction;
		this.newDirection = direction;
		this.drawDirection = direction;
	}

	public void setupBody(Shape[] p, float density, float friction, float restitution) {
		this.p = p;
		this.density = density;
		this.friction = friction;
		this.restitution = restitution;
	}

	public void createBody(short category, short mask, boolean sensor, boolean dynamic, boolean b) {
		// This is a generic dynamic body creation function using x, y, array of
		// shapes, density, friction, and restitution
		BodyDef bodyDef = new BodyDef();
		if (dynamic) {
			bodyDef.type = BodyType.DynamicBody;
		} else {
			bodyDef.type = BodyType.StaticBody;
		}
		bodyDef.position.set(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale);
		if (this instanceof Bullet) {
			bodyDef.angle = newDirection;
		}
		bodyDef.angularVelocity = rot;
		body = scene.world.createBody(bodyDef);
		body.setUserData(this);
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
		if (b) {
			body.setBullet(true);
		}
	}

	public void sync(long serverTick, SyncData sd) {
		int duration = (int) (serverTick - newPacketTick);
		//if (duration < Shared.xml.stepTime) {
			//duration = Shared.xml.stepTime;
		//}
		//duration = 100;
		scene.tickDuration = serverTick - scene.serverTick;
		scene.serverTick = serverTick;
		lastPacketTick = newPacketTick;
		newPacketTick = serverTick;
		drawDuration = duration;
		drawStart = scene.tick;
		scene.lastDrawStart = drawStart;
		lastUpdate = scene.tick;
		x = sd.x;
		y = sd.y;
		speedX = sd.vX;
		speedY = sd.vY;
		curDirection = sd.direction;
		// drawDirection = curDirection;
		if (active == false) {
			drawX = x;
			drawY = y;
		}
		active = true;
	}

	public void update() {

		if (requiresUpdate && scene.tick - lastUpdate > 1000) {
			active = false;
		}
		if (drawDuration == 0) {
			drawX = newX;
			drawY = newY;
			drawDirection = newDirection;
		} else {

			long t = scene.tick;
			float percent = ((float) (t - drawStart) / (float) drawDuration);
			// if(percent > 2) {percent = 2;}
			if (percent < 0) {
				percent = 0;
			}
			// percent += 1;
			drawX = ((newX - x) * percent) + x;
			drawY = ((newY - y) * percent) + y;
			float newR = Vector.fixDir(newDirection); // rotate this much
			float curR = Vector.fixDir(curDirection);
			float angleChange = 0;
			// this algorithm works because we can assume the difference between
			// newR and curR is small
			if (newR > curR) {// probably clockwise
				if (newR - curR > Math.toRadians(180)) { // no, its ccw
					newR -= Math.toRadians(360);
					angleChange = -Math.abs(curR - newR);
				} else { // yes, definitely clockwise
					angleChange = Math.abs(newR - curR);
				}
			} else { // probably counterclockwise
				if (curR - newR > Math.toRadians(180)) { // no, its clockwise
					curR -= Math.toRadians(360);
					angleChange = Math.abs(newR - curR);
				} else {// definitely counterclockwise
					angleChange = -Math.abs(curR - newR);
				}
			}
			drawDirection = (angleChange * percent) + curDirection;
			if (this instanceof Player) {
				Player p = (Player) this;
				if (p.dead) {
					drawX = x;
					drawY = y;
				}
			}
		}
	}

}
