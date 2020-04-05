package com.bbg.client;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.bbg.shared.Entities;

public class Listener implements ContactListener {

	Scene scene;

	public Listener(Scene scene) {
		this.scene = scene;
	}

	@Override
	public void beginContact(Contact contact) {
		Vector2[] pts = contact.getWorldManifold().getPoints();
		Entity e1 = (Entity) contact.getFixtureA().getBody().getUserData();
		Entity e2 = (Entity) contact.getFixtureB().getBody().getUserData();
		scene.beginCollision(e1, e2, pts);
		if (e1 instanceof WallEntity || e2 instanceof WallEntity) {
			if (e1 instanceof WallEntity && e2 instanceof WallEntity) {
				// two static walls collided? as if!
			} else if (e1 instanceof WallEntity) {
				// e1 is a wall colliding with something
				WallEntity we = (WallEntity) e1;
				we.wall.contacts.add(e2);
				we.wall.col = Entities.randomBrightColor();
				//we.wall.blink = true;
				//we.wall.blinkStamp = scene.tick + 200;
			} else { // safe to assume cd.e2 instance of WallEntity == true
				// e2 is a wall colliding with something
				WallEntity we = (WallEntity) e2;
				we.wall.contacts.add(e1);
				//we.wall.blink = true;
				//we.wall.blinkStamp = scene.tick + 200;
				we.wall.col = Entities.randomBrightColor();
			}
		}
		if (e1 instanceof Player && !(e2 instanceof Bullet)) {
			Player p = (Player) e1;
			p.contacts.add(pts[0]);
		}
		if (e2 instanceof Player && !(e1 instanceof Bullet)) {
			Player p = (Player) e2;
			p.contacts.add(pts[0]);
		}
		if (e1 instanceof Player && e2 instanceof Pickup) {
			contact.setEnabled(false);
		}
		if (e2 instanceof Player && e1 instanceof Pickup) {
			contact.setEnabled(false);
		}
 		if (e1 instanceof Player && e2 instanceof Flag) {
 			Flag f = (Flag) e2;
 			if(!f.home) {
 				contact.setEnabled(false);
 			}
		}
		if (e2 instanceof Player && e1 instanceof Flag) {
			Flag f = (Flag) e1;
 			if(!f.home) {
 				contact.setEnabled(false);
 			}
		}
	}

	@Override
	public void endContact(Contact contact) {
		//Vector2[] pts = contact.getWorldManifold().getPoints();
		Entity e1 = (Entity) contact.getFixtureA().getBody().getUserData();
		Entity e2 = (Entity) contact.getFixtureB().getBody().getUserData();
		// scene.endCollision(e1, e2, contact.getWorldManifold().getPoints());
		if (e1 instanceof WallEntity || e2 instanceof WallEntity) {
			if (e1 instanceof WallEntity && e2 instanceof WallEntity) {
				// two static walls collided? as if!
			} else if (e1 instanceof WallEntity) {
				// e1 is a wall colliding with something
				WallEntity we = (WallEntity) e1;
				we.wall.contacts.remove(e2);
			} else { // safe to assume cd.e2 instance of WallEntity == true
				// e2 is a wall colliding with something
				WallEntity we = (WallEntity) e2;
				we.wall.contacts.remove(e1);
			}
		}
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		Entity e1 = (Entity) contact.getFixtureA().getBody().getUserData();
		Entity e2 = (Entity) contact.getFixtureB().getBody().getUserData();
		if (e1 instanceof Player && e2 instanceof Pickup) {
			contact.setEnabled(false);
		}
		if (e2 instanceof Player && e1 instanceof Pickup) {
			contact.setEnabled(false);
		}
 		if (e1 instanceof Player && e2 instanceof Flag) {
 			Flag f = (Flag) e2;
 			if(!f.home) {
 				contact.setEnabled(false);
 			}
		}
		if (e2 instanceof Player && e1 instanceof Flag) {
			Flag f = (Flag) e1;
 			if(!f.home) {
 				contact.setEnabled(false);
 			}
		}
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {


	}

	public static class CollisionData {
		public Entity e1, e2;
		public Vector2[] points;
	}

};