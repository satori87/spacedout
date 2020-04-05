package com.bbg.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bbg.shared.Entities;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Network.SyncData;
import com.bbg.shared.PickupDef;
import com.bbg.shared.Random;
import com.bbg.shared.Shared;
import com.bbg.shared.Vector;

public class Pickup extends Entity {
	public int index;
	public int type;
	public long altStamp = 0;
	public float dir = Random.dir();
	public Color col = Color.WHITE;
	public Color originalCol = Color.WHITE;

	public Pickup(Scene scene, int index, int type, float x, float y) {
		this.scene = scene;
		this.x = x;
		this.y = y;
		this.newX = x;
		this.newY = y;
		this.drawX = x;
		this.drawY = y;
		requiresUpdate = false;
		this.scale = 1.0f;
		this.index = index;
		this.type = type;
		setupBody(PickupDef.getShape(type), 0.5f, 0.5f, 0.5f);
		originalCol = PickupDef.getColor(type);
		col = originalCol;
	}

	public void preStep() {
		body.setTransform(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale, 0);
		x = drawX;
		y = drawY;
		body.setLinearVelocity(speedX, speedY);
	}

	public void postStep() {
		speedX = body.getLinearVelocity().x;
		speedY = body.getLinearVelocity().y;
		newX = body.getPosition().x * Shared.xml.physicsScale;
		newY = body.getPosition().y * Shared.xml.physicsScale;
	}

	public void sync(long serverTick, SyncData sd) {
		super.sync(serverTick, sd);
		active = true;
		if (body == null) {
			createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, false, true, false);
		}
	}

	public void update() {
		super.update();
		dir += 0.05f;
		dir = Vector.fixDir(dir);
	}

	public void render(GameScreen screen) {
		if (scene.getMe().dead && scene.tick > scene.respawnStamp) {
			return;
		}
		// render thyself, peasant
		float r = (float) Math.toDegrees(dir);
		TextureRegion tex = new TextureRegion();
		if (scene.tick > altStamp) {
			altStamp = scene.tick + 200;
			col = Entities.alterColor(originalCol, .15f);
		}
		tex = AssetLoader.getSprite("pickup0g");
		col.a = scene.glowVal;
		scene.screen.batcher.setColor(col);
		screen.drawRegion(tex, drawX, drawY, centered, r, scale);
		col.a = 1;
		scene.screen.batcher.setColor(col);
		tex = AssetLoader.getSprite("pickup0");
		screen.drawRegion(tex, drawX, drawY, centered, r, scale);
		tex = AssetLoader.getSprite("item" + type);
		screen.drawRegion(tex, drawX, drawY, centered, r, scale);
		scene.screen.batcher.setColor(Color.WHITE);
	}

}
