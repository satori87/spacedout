package com.bbg.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Entities;
import com.bbg.shared.Network.SyncData;
import com.bbg.shared.Random;
import com.bbg.shared.Shared;
import com.bbg.shared.WeaponDef;

public class Bullet extends Entity {

	public int index;
	public int type;
	public int owner = -1;
	public Color col = Color.WHITE;
	public int sprite = 0;
	public boolean frot = false;
	public boolean triggered = false;
	public int numColors = 0;

	public float initialDirection = 0;

	public boolean dontClear = false;

	public Bullet(Scene scene, int index, int type, float x, float y, float direction, int owner) {
		this.scene = scene;
		this.x = x;
		this.y = y;
		this.newX = x;
		this.owner = owner;
		this.newY = y;
		this.drawX = x;
		this.drawY = y;
		this.drawDirection = direction;
		this.newDirection = direction;
		this.initialDirection = direction;
		requiresUpdate = false;
		this.scale = 1.0f;
		this.index = index;
		this.type = type;
		rot = Entities.weapons[type].rot;
		// active = true;
		col = Entities.weapons[type].col();
		sprite = Entities.weapons[type].sprite;
		if (sprite >= 10) {
			sprite = Random.getInt(3) + 10;
			col = Entities.randomBrightColor();
		}
		setupBody(WeaponDef.getBulletShape(type, Shared.xml.physicsScale), Shared.xml.bulletDensity,
				Shared.xml.bulletFriction, Shared.xml.bulletRestitution);

	}

	public void preStep() {
		if (triggered) {
			return;
		}
		curDirection = newDirection;
		x = scene.assertX(x);
		y = scene.assertY(y);		
		body.setTransform(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale, curDirection);
		body.setAngularVelocity(rot);
		x = drawX;
		y = drawY;
		body.setLinearVelocity(speedX, speedY);
	}

	public void postStep() {

		speedX = body.getLinearVelocity().x;
		speedY = body.getLinearVelocity().y;
		newX = body.getPosition().x * Shared.xml.physicsScale;
		newY = body.getPosition().y * Shared.xml.physicsScale;
		newDirection = body.getAngle();
	}

	public void render(GameScreen screen) {
		// render thyself, peasant
		if (scene.getMe().dead && scene.tick > scene.respawnStamp) {
			return;
		}
		float r = (float) Math.toDegrees(drawDirection);
		if (triggered) {
			return;
		}
		TextureRegion tex = new TextureRegion();
		if (sprite < 10) {
			tex = AssetLoader.getSprite("bullet" + sprite);
			scene.screen.batcher.setColor(Entities.alterColor(col, .2f));
			screen.drawRegion(tex, drawX, drawY, centered, r, Entities.weapons[type].scale);
			scene.screen.batcher.setColor(Color.WHITE);
		} else {
			tex = AssetLoader.getSprite("shape" + (sprite - 10) + "g");
			col.a = .5f;
			scene.screen.batcher.setColor(Entities.alterColor(col, .2f));
			screen.drawRegion(tex, drawX, drawY, centered, r, Entities.weapons[type].scale);
			tex = AssetLoader.getSprite("shape" + (sprite - 10));
			col.a = 1;
			scene.screen.batcher.setColor(Entities.alterColor(col, .2f));
			screen.drawRegion(tex, drawX, drawY, centered, r, Entities.weapons[type].scale);
			scene.screen.batcher.setColor(Color.WHITE);
		}
	}

	public void sync(long serverTick, SyncData sd) {
		super.sync(serverTick, sd);
		active = true;
		if (body == null) {
			createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, true, true, true);
		}
	}

	public void update() {
		super.update();
	}

	public void trigger() {
		active = false;
	}

	public void trig(int playerStruck, Vector2 struckCoord) {
		if (playerStruck >= 0) {
			scene.players[playerStruck].makeSparks(struckCoord, 1.4f);
			// if(playerStruck == scene.screen.myIndex) {
			scene.play3D(4, drawX, drawY, 1, 1);
			// }
		}
		triggered = true;
		active = false;
		WeaponDef bs = Entities.weapons[type];
		Color c = Color.WHITE;
		for (int i = 0; i < bs.expCount; i++) {
			if (bs.sprite >= 10 && bs.sprite <= 12) {
				c = Entities.randomBrightColor();
			} else {
				c = bs.col();
			}
			PolyPoop p = new PolyPoop(scene, drawX, drawY, 1200, bs.expSpeed, bs.expScale, Entities.alterColor(c, .2f));
			scene.explosions.add(p);
		}
		WeaponDef bd = Entities.weapons[type];
		if (bd.expSnd >= 0) {
			scene.play3D(bd.expSnd, drawX, drawY, 1, 1);
		}

	}
}
