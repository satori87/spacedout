package com.bbg.lobby;

import com.bbg.lobby.LobbyNetwork.ServerData;
import com.bbg.shared.Network.GameInfo;

public class ServerInfo {
	Lobby lobby;
	public String ip;
	public GameInfo info;
	public long lastUpdate;
	
	public ServerInfo(Lobby lobby, String ip, GameInfo info) {
		this.lobby = lobby;
		this.ip = ip;
		this.info = info;
		lastUpdate = lobby.tick;
	}
	
	public void report(GameInfo info) {
		this.info = info;
		lastUpdate = lobby.tick;
	}
	
	public ServerData getServerData() {
		ServerData sd = new ServerData();
		sd.info = info;
		sd.ip = ip;
		return sd;
	}
}
