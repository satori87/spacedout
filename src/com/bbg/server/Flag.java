package com.bbg.server;

import com.bbg.shared.Entities;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Network.FlagData;
import com.bbg.shared.Shared;

public class Flag extends Entity {
	
	public int index;
	public int team;
	public float homeX, homeY;
	
	public boolean home = true;
	
	public long fieldStamp = 0; //the tick when the flag was released into open field
	public boolean held = false;
	public Player holder;
	
	public float holderX=0,holderY=0, holderVX=0, holderVY=0;
	
	public Flag(Game game, int index, int team, float homeX, float homeY) {
		this.game = game;
		this.index = index;
		this.team = team;
		this.homeX = homeX;
		this.homeY = homeY;
		x = homeX;
		y = homeY;
		direction = 0;
		speedX = 0;
		speedY = 0;
		setupBody(Entities.getCircleArray(16, Shared.xml.physicsScale),1f, 0.5f, 0.5f);
		createMyBody();
		body.setFixedRotation(true);
	}
	
	public void collide(Player p) {
		if(p == null) {return;}
		if(held || !p.alive()) {return;}
		if(p.team == team) {
			if(!home) {
				home(p, false);
			} else {
			}
		} else {
			if(!home) {
				grab(p); //grab the flag!
			} else {
				//let the flagbase handle this case
			}
		}
	}
	
	public void grab(Player p) {
		//announce p grabbed the flag!
		if(p == null) {return;}
		p.flag = this;
		holder = p;
		held = true;
		home = false;
		holderX = holder.x;
		holderY = holder.y;
		holderVX = holder.speedX;
		holderVY = holder.speedY;
		if(body != null) {
			destroy(); //destroy the physics flag cause its in the players ship
		}
	}
	
	public void drop() {
		
		x = holderX;
		y = holderY;
		speedX = holderVX;
		speedY = holderVY;
		held = false;
		if(holder != null) {
			holder.flag = null;
		}
		holder = null;
		fieldStamp = game.tick;
		home = false;
		createMyBody();
	}
	
	public void update() {
		if(held) {
			if(holder == null) {
				drop();
			} else if(holder.dead || !holder.alive()) {
				drop();
			}
		}
		if(held) {	
			holderX = holder.x;
			holderY = holder.y;
			holderVX = holder.speedX;
			holderVY = holder.speedY;
		}
		
		if(!home && !held) {
			if(game.tick > fieldStamp + Config.xml.flagReturnTime) {
				home(null, false);
			}
		}
		
	}
	
	public void home(Player p, boolean captured) {
		x = homeX;
		y = homeY;
		game.flagbase[index].home = true;
		speedX = 0;
		speedY = 0;
		held = false;
		home = true;
		if(p != null) {
			if(captured) {
				//announce that p captured the flag!
			} else {
				//announce that p returned the flag!
			}
		} else {
			//announce the flag returned (by timelimit)
		}
		holder = p;
		if(body == null) {
			createMyBody();
		} else {
			destroyBody();
			createMyBody();
		}
		body.setTransform(x/Shared.xml.physicsScale, y/Shared.xml.physicsScale, 0);
	}
	
	public void createMyBody() {
		if(home) {
			if(team == 1) {
				createBody(CollisionFlags.CAT_T1FLAG, CollisionFlags.MASK_T1FLAG, false, this, false, false);
			} else {
				createBody(CollisionFlags.CAT_T2FLAG, CollisionFlags.MASK_T2FLAG, false, this, false, false);
			}
		} else {
			createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, false, this, true, false);
		}
		body.setFixedRotation(true);
	}
	
	public FlagData getFlagData() {
		//express thyself
		FlagData fd = new FlagData();
		fd.index = index;
		if(holder != null && holder.active() && holder.team != team) {
			fd.holder = holder.index;
		} else {
			holder = null;
			fd.holder = -1;
		}
		fd.held = held;
		fd.home = home;
		fd.x = x;
		fd.y = y;
		fd.vX = speedX;
		fd.vY = speedY;
		fd.time = getFlagTime();
		fd.team = team;
		return fd;
	}
	
	public int getFlagTime() {
		if(home || held) {return 0;}
		//returns how long until flag goes back to base
		int t = (int)((fieldStamp + Config.xml.flagReturnTime) - game.tick);
		if(t < 0) {t = 0;} //shouldnt happen tho
		return t;
	}
	
	
	
	
	
}
