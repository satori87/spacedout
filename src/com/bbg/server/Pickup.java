package com.bbg.server;

import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Shared;
import com.bbg.shared.Vector;
import com.bbg.shared.Network.PickupData;
import com.bbg.shared.Network.SyncData;
import com.bbg.shared.PickupDef;
import com.bbg.shared.Random;

public class Pickup extends Entity {

	public int index = 0;
	public int type = 0;
	public int qty = 0;
	public long diesAt = 0;

	public Pickup(Game game, int index, int type, int qty, float x, float y) {
		this.game = game;
		this.index = index;
		this.type = type;
		diesAt = game.tick + 70000 + Random.getInt(3000);
		this.qty = qty;
		this.x = x;
		this.y = y;
		setupBody(PickupDef.getShape(type), 0.5f, 0.5f, 0.5f);
		Vector v = new Vector(Random.dir(), (float) (Math.random() * 0.2f) + 0.1f);

		speedX = v.xChange;
		speedY = v.yChange;

		createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, false, this, true, false);
	}

	public SyncData getSyncData() {
		// express thyself
		SyncData s = new SyncData();
		s.direction = direction;
		s.index = (short) (index + Shared.maxPlayers + Shared.maxBullets);
		s.x = (short) x;
		s.y = (short) y;
		s.vX = speedX;
		s.vY = speedY;
		return s;
	}

	public void preStep() {
		if (game.tick > diesAt) {
			remove();
		} else {
			super.preStep();
		}
	}

	public void postStep() {
		if (!remove) {
			super.postStep();
		}
	}

	public PickupData getPickupData() {
		PickupData ad = new PickupData();
		ad.direction = direction;
		ad.index = index;
		ad.type = type;
		ad.x = x;
		ad.y = y;
		ad.vX = speedX;
		ad.vY = speedY;
		ad.remove = remove;
		ad.qty = qty;
		return ad;
	}

	public void remove() {
		super.remove();
		PickupData bd = getPickupData();
		bd.remove = true;
		game.sendAllPlaying(bd, false);
	}

}
