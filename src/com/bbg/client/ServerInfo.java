package com.bbg.client;

import com.bbg.shared.Network.GameInfo;

public class ServerInfo {
	GameScreen screen;
	public String ip;
	public GameInfo info;
	public long lastUpdate;
	public boolean inUse = false;
	
	public ServerInfo(GameScreen screen, String ip, GameInfo info) {
		this.screen = screen;
		this.ip = ip;
		this.info = info;
		lastUpdate = screen.tick;
		inUse = true;
	}
	
	public void report(GameInfo info) {
		this.info = info;
		inUse = true;
		lastUpdate = screen.tick;
	}
	
	void disable() {
		inUse = false;
	}
	
	public static class GameListData {
		public ServerInfo game;
	}
	
	public String getGameTypeString() {
		switch(info.type) {
		case 0:
			return "Free for All";
		case 1:
			return "Team Deathmatch";
		case 2:
			return "Capture the Flag";
		}
		return "Game";
	}
	
	public String getGameStateString() {
		switch(info.gameState) {
		case 1:
			return "Starting";
		case 2:
			return "Playing";
		case 3:
			return "Ending";	
		}
		return "Game";
	}
}
