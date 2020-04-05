package com.bbg.server;

import com.badlogic.gdx.graphics.Color;
import com.bbg.shared.Entities;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Vector;
import com.bbg.shared.WeaponDef;
import com.bbg.shared.Shared;
import com.bbg.shared.Network.BulletData;
import com.bbg.shared.Network.SyncData;

public class Bullet extends Entity {
	int index;
	public int type = 0;
	public Player owner;
	public int firingWeapon = 0;
	public float initialDirection = 0;
	public Color col = Color.WHITE;
	public float deathX = 0, deathY = 0;
	public boolean death = false;
	public int playerStruck = -1;
	public float sX = 0;
	public float sY = 0;

	public Bullet(Game game, Player shooter, int index, int type, float x, float y, float direction) {
		this.game = game;
		this.type = type;
		this.x = x;
		this.y = y;
		this.index = index;
		this.owner = shooter;
		firingWeapon = shooter.weapons[shooter.curWeapon].type;
		this.direction = direction;
		rot = Entities.weapons[type].rot;
		setupBody(WeaponDef.getBulletShape(type, Shared.xml.physicsScale), Shared.xml.bulletDensity, Shared.xml.bulletFriction,
				Shared.xml.bulletRestitution);
		Vector v = new Vector(direction, Entities.weapons[firingWeapon].speed);
		speedX = owner.speedX * Shared.xml.bulletInheritance + v.xChange;
		speedY = owner.speedY * Shared.xml.bulletInheritance + v.yChange;
		createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, false, this, true, true);
		body.setBullet(true);
		initialDirection = direction;
	}

	public SyncData getSyncData() {
		// express thyself
		SyncData s = new SyncData();
		s.direction = direction;
		s.index = (short) (index + Shared.maxPlayers);
		s.x = (short) x;
		s.y = (short) y;
		s.vX = speedX;
		s.vY = speedY;
		return s;
	}

	public BulletData getBulletData() {
		BulletData ad = new BulletData();
		ad.direction = initialDirection;
		ad.index = index;
		ad.type = type;
		ad.x = x;
		ad.y = y;
		ad.vX = speedX;
		ad.vY = speedY;
		ad.remove = remove;
		ad.playerStruck = playerStruck;
		ad.sX = sX;
		ad.sY = sY;
		if (owner != null) {
			ad.owner = owner.index;
		} else {
			ad.owner = 0;
		}
		return ad;
	}

	public void remove() {
		super.remove();
		BulletData bd = getBulletData();
		bd.remove = true;
		if (death) {	
			bd.x = deathX;
			bd.y = deathY;
		}
		game.sendAllPlaying(bd, false);
	}
}
