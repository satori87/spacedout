package com.bbg.server;

import java.util.LinkedList;
import java.util.List;

import com.bbg.shared.Shared;
import com.bbg.shared.Network.TeamData;

public class Team {

	Game game;

	public String name = "";
	public int index = 0;
	public int score = 0;

	public Team(Game game, int index) {
		this.game = game;
		this.index = index;
		switch (index) {
		case 0:
			name = Shared.xml.t1Name;
			break;
		case 1:
			name = Shared.xml.t2Name;
			break;
		}
	}

	public List<Player> getList() {
		List<Player> list = new LinkedList<Player>();
		for (Player p : game.players) {
			if (p.team == index) {
				list.add(p);
			}
		}
		return list;
	}

	public int numPlayers() {
		int n = getList().size();
		return n;
	}

	public void tally() {
		score += 1;
		if (game.scoreLimit > 0 && score >= game.scoreLimit) {
			// victory
			game.endGame();
		}
	}

	public TeamData getTeamData() {
		TeamData t = new TeamData();
		t.index = index;
		t.name = name;
		t.score = score;
		return t;
	}
}
