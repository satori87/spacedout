package com.bbg.lobby;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.bbg.shared.Network.GameInfo;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class LobbyNetwork {

	static public void register(EndPoint endPoint) {
		Kryo kryo = endPoint.getKryo();
		kryo.register(LobbyLogin.class);
		kryo.register(UserData.class);
		kryo.register(JoinLobby.class);
		kryo.register(LobbyJoinError.class);
		kryo.register(LinkedList.class);
		kryo.register(GameInfo.class);
		kryo.register(com.bbg.shared.Network.TeamData.class);
		kryo.register(ServerData.class);
		kryo.register(LobbySendChat.class);
		kryo.register(LobbyChatMsg.class);
		kryo.register(Color.class);
	}
	
	static public class ServerData {
		public GameInfo info;
		public String ip;
	}
	
	static public class LobbyLogin {
		public String user, pass;
		public boolean newAccount;
	}

	static public class LobbySendChat {
		public String msg;
	}
	
	static public class LobbyChatMsg {
		public String msg;
		public Color color;
	}
	
	static public class UserData {
		public String name;
		public int index;
		public boolean join = false;
		public boolean leave = false;
	}

	static public class JoinLobby {
		public int index;
		public String name;
		public List<UserData> users;
		public List<ServerData> games;
	}

	static public class LobbyJoinError {
		public String s;
	}

}
