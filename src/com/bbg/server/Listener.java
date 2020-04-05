package com.bbg.server;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.bbg.shared.Shared;


public class Listener implements ContactListener {

    Game game;

    public Listener(Game game) {
        this.game = game;
    }

    @Override
    public void beginContact(Contact contact) {
    	Vector2[] points =  contact.getWorldManifold().getPoints();
        Entity e1 = (Entity) contact.getFixtureA().getBody().getUserData();
        Entity e2 = (Entity) contact.getFixtureB().getBody().getUserData();
        game.beginCollision(e1, e2, points);
        if(e1 instanceof Bullet) {
        	contact.setEnabled(false);
        	Bullet b = (Bullet) e1;
        	b.deathX = points[0].x * Shared.xml.physicsScale;
        	b.deathY = points[0].y * Shared.xml.physicsScale; 
        	b.death = true;
        	System.out.println(points.length + ": " + b.deathX + "," + b.deathY);
        }
        if(e2 instanceof Bullet) {
        	contact.setEnabled(false);
        	Bullet b = (Bullet) e2;
        	b.deathX = points[0].x * Shared.xml.physicsScale;
        	b.deathY = points[0].y * Shared.xml.physicsScale;
        	b.death = true;
        	System.out.println(points.length + ": " + b.deathX + "," + b.deathY);
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
        Entity e1 = (Entity) contact.getFixtureA().getBody().getUserData();
        Entity e2 = (Entity) contact.getFixtureB().getBody().getUserData();
        game.endCollision(e1, e2, contact.getWorldManifold().getPoints());
        if(e1 instanceof Bullet || e2 instanceof Bullet) {
        	contact.setEnabled(false);
        }
        
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    	 Entity e1 = (Entity) contact.getFixtureA().getBody().getUserData();
         Entity e2 = (Entity) contact.getFixtureB().getBody().getUserData();
         game.endCollision(e1, e2, contact.getWorldManifold().getPoints());
         if(e1 instanceof Bullet || e2 instanceof Bullet) {
         	contact.setEnabled(false);
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
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }

    public static class CollisionData {

        public Entity e1, e2;
        public Vector2[] points;
    }

};
