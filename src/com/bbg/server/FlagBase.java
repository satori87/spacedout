package com.bbg.server;

import com.bbg.shared.Entities;
import com.bbg.shared.Shared;

import com.bbg.shared.CollisionFlags;

public class FlagBase extends Entity {

	public int index = 0;
	public int team = 0;

	public boolean home = true;

	public FlagBase(Game game, int index, int team, float x, float y) {
		this.game = game;
		this.index = index;
		this.team = team;
		this.x = x;
		this.y = y;
		direction = 0;
		speedX = 0;
		speedY = 0;
		setupBody(Entities.getCircleArray(18, Shared.xml.physicsScale), 0, 0, 0);
		createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, true, this, true, false);
	}

	public void collide(Player e) {
		if (e instanceof Player) {
			Player p = (Player) e;
			if (!p.active() || p.dead) {
				return;
			}
			if (p.team == team) {
				if (p.flag != null && home) {
					// player capped it!
					game.teams[p.team].tally();
					p.flag.home(p, true);
					p.flag = null;
					game.flagbase[1 - team].home = true;
				}
			} else {
				if (home) {
					home = false;
					p.flag = game.flag[index];
					game.flag[index].grab(p);
					// player grabbed it!
				}
			}
		}
	}

}
