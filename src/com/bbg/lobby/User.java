package com.bbg.lobby;

import com.bbg.lobby.LobbyServer.LobbyConnection;
import com.badlogic.gdx.graphics.Color;
import com.bbg.lobby.LobbyNetwork.LobbyChatMsg;
import com.bbg.lobby.LobbyNetwork.LobbySendChat;
import com.bbg.lobby.LobbyNetwork.UserData;

public class User {

	public String name;
	public LobbyConnection conn;
	public int index;
	public Lobby lobby;
	public boolean remove = false;
	public boolean joined = false;

	public User(Lobby lobby, LobbyConnection c, String name) {
		conn = c;
		this.lobby = lobby;
		this.name = name;
		index = c.index;
	}

	public void receive(Object object) {
		if (object instanceof LobbySendChat) {
			LobbySendChat cs = (LobbySendChat) object;
			LobbyChatMsg cc = new LobbyChatMsg();
			cc.msg = "[" + lobby.getTime() + "] " + name + " : " + cs.msg;
			cc.color = Color.WHITE;
			lobby.sendAll(cc,false);
		}
	}

	public UserData getUserData() {
		UserData pd = new UserData();
		pd.index = index;
		pd.name = name;
		return pd;
	}

	public void disconnected() {
		if (joined) {
			UserData ud = getUserData();
			ud.leave = true;
			lobby.sendAll(ud, false);
			joined = false;
		}
		remove = true;
	}

}
