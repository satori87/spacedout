package com.bbg.shared;

import java.util.LinkedList;
import java.util.List;
import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

// This class is a convenient place to keep things common to both the client and server.
public class Network {

	// This registers objects that are going to be sent over the network.
	static public void register(EndPoint endPoint) {
		Kryo kryo = endPoint.getKryo();
		kryo.register(Login.class);
		kryo.register(String[].class);
		kryo.register(Integer[].class);
		kryo.register(Integer.class);
		kryo.register(int[].class);
		kryo.register(JoinGame.class);
		kryo.register(LinkedList.class);
		kryo.register(ClientSync.class);
		kryo.register(Sync.class);
		kryo.register(SyncData.class);
		kryo.register(PlayerData.class);
		kryo.register(BulletData.class);
		kryo.register(ClientAction.class);
		kryo.register(KillData.class);
		kryo.register(ChatToServer.class);
		kryo.register(ChatToClient.class);
		kryo.register(Color.class);
		kryo.register(RequestServerName.class);
		kryo.register(ServerName.class);
		kryo.register(JoinError.class);
		kryo.register(PingPacket.class);
		kryo.register(PongPacket.class);
		kryo.register(SecondPacket.class);
		kryo.register(PlayPacket.class);
		kryo.register(GameInfo.class);
		kryo.register(TeamData.class);
		kryo.register(JoinFray.class);
		kryo.register(SecondData.class);
		kryo.register(WallData.class);
		kryo.register(FlagData.class);
		kryo.register(Shared.class);
		kryo.register(PlayingData.class);
		kryo.register(LoadoutData.class);
		kryo.register(ChangeLoad.class);
		kryo.register(FrayError.class);
		kryo.register(ChangeTeam.class);
		kryo.register(LeaveFray.class);
		kryo.register(SharedXML.class);
		kryo.register(JoinSpec.class);
		kryo.register(LeftSpec.class);
		kryo.register(PickupData.class);
	}

	// TCP Client to Server

	static public class ChangeTeam {
		int hi = 7;
	}

	static public class LeaveFray {
		int hi = 7;
	}

	static public class JoinSpec {
		public JoinSpec() {
			hi = 0;
		}

		public JoinSpec(int a) {
			hi = a;
		}

		public int hi = 0;
	}

	static public class LeftSpec {
		int hi = 7;
	}

	static public class JoinFray {
		public int team = 0;
		public LoadoutData load = new LoadoutData();
	}

	static public class LoadoutData {
		public int armor = 1;
		public List<Integer> weapons = new LinkedList<Integer>();
		public List<Integer> items = new LinkedList<Integer>();
		public Color col = Entities.randomBrightColor();
	}

	static public class PongPacket {
		public long id;
	}

	static public class Login {
		public String user, pass;
		public boolean newAccount;
		public String version;
	}

	static public class ClientAction {
		public int act;
	}

	static public class ChatToServer {
		public String msg;
	}

	static public class RequestServerName {
		public String name;
	}

	static public class ChangeLoad {
		public LoadoutData load = new LoadoutData();
	}

	// UDP Client to Server

	static public class ClientSync {
		public boolean accel = false, decel = false, strafeLeft = false, strafeRight = false, fire = false;
		public float desiredRotation = 0;
		public boolean ignore = false;
		public boolean turbo = false;

	}

	// TCP Server to Client

	static public class FrayError {
		public String s = "";

		public FrayError() {
			s = "";
		}

		public FrayError(String s) {
			this.s = s;
		}
	}

	static public class FlagData {
		public int index, holder;
		public float x, y, vX, vY;
		public boolean home, held;
		public int team;
		public int time;
	}

	static public class GameInfo {
		public int type = 0;
		public int maxGamePlayers = 0;
		public int numTeams = 0;
		public List<TeamData> teams = new LinkedList<TeamData>();
		public int gameState = -1;
		public long gameTime = 0;
		public int timeLimit = 0; // time limit in minutes, 0 = no time base
		public int scoreLimit = 0; // score limit for individual/team, 0 = no
									// limit
		public int howLongBeforeNext = 0; // how long before game start/newgame,
											// depending on state
		public int mapNum = 0;
		public int numPlayers = 0;
		public String name = "";
		public String version = "";
	}

	static public class TeamData {
		public String name = "";
		public int index = 0;
		public int score = 0;
	}

	static public class PlayPacket {
		public float x, y;
		public float direction;
		public List<BulletData> bullets = new LinkedList<BulletData>();;
		public List<PlayingData> pad = new LinkedList<PlayingData>();
		public List<PickupData> pickups = new LinkedList<PickupData>();;
		public int minX, maxX, minY, maxY;
		public int team;
		public int hp = 0;
		public int armor;
		public Color col;
		public boolean spectating = false;
		public int[] items = new int[3];
	}

	static public class PlayingData {
		public int index = 0;
		public boolean playing = false;
	}

	static public class SecondPacket {
		public List<SecondData> data;
		public int score0 = 0;
		public int score1 = 0;
	}

	static public class SecondData {
		public int index;
		public int score;
		public int ping;
	}

	static public class PingPacket {
		public long id;
	}

	static public class JoinError {
		public String s;
		public int c = 0;
	}

	static public class ChatToClient {
		public String msg;
		public Color c;
	}

	static public class JoinGame {
		public int index;
		public String name;
		public String serverName;
		public List<PlayerData> players;
		public GameInfo info;
	}

	static public class PlayerData {
		public float x, y, direction;
		public int index;
		public String name;
		public boolean join = false;
		public boolean leave = false;
		public boolean active = false; // these are not steady states and only
										// are true when it happens
		public boolean deactive = false;// these are not steady states and only
										// are true when it happens
		public boolean dead = false;
		public int kills, deaths, score;
		public int team = 0;
		public Color col = Color.WHITE;
		public int armor = 0;
	}

	static public class BulletData {
		public int index, type;
		public float x, y;
		public float vX, vY;
		public float direction;
		public boolean remove = false;
		public int owner;
		public int playerStruck = -1;
		public float sX = 0, sY = 0;
		public int head = -1;
		public int body = -1;
	}

	static public class PickupData {
		public int index = 0, type = 0;
		public float x = 0, y = 0;
		public float vX = 0, vY = 0;
		public float direction = 0;
		public boolean remove = false;
		public int qty = 0;
	}

	static public class KillData {
		public int killerIndex;
		public int killedIndex;
		public int type; // type of bullet
		public boolean lostKiller = false;
	}

	static public class ServerName {
		public String name;
		// public SharedXML shared = new SharedXML();
		public SharedXML shared;
	}

	// UDP Server to Client

	static public class SyncData {
		public short x, y;
		public float direction;
		public short index;
		public boolean accel = false, decel = false, strafeLeft = false, strafeRight = false;
		public float vX, vY;
		public float desiredR;
		public boolean turbo = false;
		public boolean healing = false;
		public boolean shielding = false;

	}

	static public class Sync {
		public List<SyncData> s = new LinkedList<SyncData>();
		public long tick = 0;
		public int hp = 0;
		public int maxhp = 0;
		public boolean reloading = false;
		public int weapon = 0, shotsLeft = 0;
		public boolean turbo = false;
		public List<WallData> w = new LinkedList<WallData>();
		public List<FlagData> flags = new LinkedList<FlagData>();
		public int item0 = 0, item1 = 0, item2 = 0;
		public int sx = 0, sy = 0;
		public int si = -1;
	}

	static public class WallData {
		public boolean blink = false;
		public int index = 0;
	}

	// This is a sort of internal packet to make parallel use of the packet
	// queue for thread safe disconnect handling
	static public class Disconnected {
		public int index;
	}

}
