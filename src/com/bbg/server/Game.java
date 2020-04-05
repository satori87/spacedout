package com.bbg.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.bbg.server.DesktopServer.GameConnection;
import com.bbg.server.Listener.CollisionData;
import com.bbg.shared.Entities;
import com.bbg.shared.MapDef;
import com.bbg.shared.MapDef.BaseData;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Random;
import com.bbg.shared.Rect;
import com.bbg.shared.Vector;
import com.bbg.shared.Wall;
import com.bbg.shared.WeaponDef;
import com.bbg.shared.Shared;

import com.bbg.shared.Network.BulletData;
import com.bbg.shared.Network.ChatToClient;
import com.bbg.shared.Network.GameInfo;
import com.bbg.shared.Network.JoinError;
import com.bbg.shared.Network.JoinGame;
import com.bbg.shared.Network.Login;
import com.bbg.shared.Network.PickupData;
import com.bbg.shared.Network.PlayerData;
import com.bbg.shared.Network.ServerName;
import com.bbg.shared.Network.Sync;
import com.bbg.shared.Network.RequestServerName;
import com.bbg.shared.Vector.Coord;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;

public class Game {
	public String serverName = "Dev Server";
	public String version = "hotrod4";
	public long reconnectStamp = 0;
	public boolean connectedLobby = false;
	public boolean connectingLobby = false;
	public boolean fr = true;
	DesktopServer server;
	public boolean forcedMap = false;
	public int forceMap = 0;
	public int forceScore = 0;
	public int forceTime = 0;
	public int forceType = 0;
	World world;
	Listener listener; // collision listener
	public List<Wall> walls = new LinkedList<Wall>();
	public int gameType = 0; // 0 = Freeforall, 1 = team deathmatch, 2 = ctf
	public int maxGamePlayers = 0; // different from MAXPLAYERS (which
	// determines # of connections)
	public int numTeams = 0;
	public Team[] teams;
	public int gameState = 0; // 0 = init, 1 = starting game, 2 = game in
	// progress, 3 = ending game
	public int timeLimit = 0; // time limit in minutes, 0 = no time base
	public int scoreLimit = 0; // score limit for individual/team, 0 = no limit
	public int startTime = 0; // how long before starting game
	public int endTime = 0; // how long after end of game
	public long gameStartStamp = 0; // tick count for start
	public long gameInitStamp = 0;
	public long gameEndStamp = 0;
	public int totalKills = 0;
	public int curMap = 1;
	public int mapCount = 0;
	public int curRot = 0;
	public long minuteStamp = 0;
	public long tenStamp = 0;
	public RotationData[] rotation = new RotationData[40];
	public long lobbyStamp = 0;
	public NetClient net;
	float minX, maxX, minY, maxY;
	public FlagBase[] flagbase = new FlagBase[2];
	public Flag[] flag = new Flag[2];
	public List<Player> players;
	public Bullet[] bullets;
	public Pickup[] pickups;
	public MapDef map;
	public Queue<PacketData> packetQueue;
	public boolean checkingPacketQueue = false;
	public boolean checkingDiscQueue = false;
	public Queue<Player> discQueue;
	public Queue<CollisionData> collisions = new LinkedList<CollisionData>();
	List<Entity> drops = new LinkedList<Entity>();
	public int balanceWarn = 0;

	public static class PacketData {

		GameConnection c;
		Object object;
	}

	public long stepStamp = 0;
	public long syncStamp = 0;

	public Body box;
	public String lobbyIP;

	public long tick = System.currentTimeMillis();
	public long oldTick = System.currentTimeMillis();

	public Game(DesktopServer server) {
		net = new NetClient(this);
		Entities.init();
		this.server = server;
		players = new LinkedList<Player>();
		bullets = new Bullet[Shared.maxBullets];
		pickups = new Pickup[Shared.maxPickups];
		drops = new LinkedList<Entity>();
		packetQueue = new LinkedList<PacketData>();
		discQueue = new LinkedList<Player>();
		world = new World(new Vector2(0, 0), true);
		listener = new Listener(this);
		world.setContactListener(listener);
		world.setContinuousPhysics(true);
		World.setVelocityThreshold(World.getVelocityThreshold() * Shared.xml.velocityThresholdFactor);
	}

	public void start() {
		tick = System.currentTimeMillis();
		oldTick = System.currentTimeMillis();
		loadMapRotation();
		nextGame();
		minuteStamp = tick + 60000;
		tenStamp = tick + 10000;
	}

	void serverMessage(String s, Color c) {
		ChatToClient cc = new ChatToClient();
		cc.msg = "[" + getTime() + "] " + "Server Message" + " : " + s;
		cc.c = c;
		sendAllJoined(cc, false);
	}

	public void connectLobby() {
		if (connectingLobby) {
			return;
		} else {
			if (reconnectStamp > tick) {
				return;
			}
		}
		try {
			lobbyIP = new String(Files.readAllBytes(Paths.get("assets/lobbyIP.txt")));
			net.connectLobby(lobbyIP);
			connectingLobby = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void restart() {
		StringBuilder cmd = new StringBuilder();
		cmd.append(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java ");
		for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			cmd.append(jvmArg + " ");
		}
		cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
		cmd.append(Window.class.getName()).append(" ");

		try {
			Runtime.getRuntime().exec(cmd.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public void tallyKill() {
		totalKills += 1;
		if (scoreLimit > 0) {
			switch (gameType) {
			case 0:
				if (totalKills >= scoreLimit) {
					endGame();
				}
				break;
			case 1:
				for (int i = 0; i < numTeams; i++) {
					if (teams[i].score >= scoreLimit) {
						endGame();
					}
				}
				break;
			}

		}
	}

	int mapNum = 0;

	void nextGame() {
		System.out.println("Game init at " + getTime());
		if (!forcedMap) {
			mapNum = rotation[curRot].map;
			timeLimit = rotation[curRot].time;
			scoreLimit = rotation[curRot].score;
			gameType = rotation[curRot].type;
			curRot++;
			if (curRot >= mapCount) {
				curRot = 0;
			}
		} else {
			mapNum = forceMap;
			timeLimit = forceTime;
			scoreLimit = forceScore;
			gameType = forceType;
			forcedMap = false;
		}
		newGame(mapNum, timeLimit, scoreLimit, 200, 10000);
	}

	public String getGameTypeString(int t) {
		switch (t) {
		case 0:
			return "Free for all";
		case 1:
			return "Deathmatch";
		case 2:
			return "CTF";
		default:
			return "Free for all";
		}
	}

	void resetWorld(float arenaWidth, float arenaHeight) {
		world.dispose();
		world = new World(new Vector2(0, 0), true);
		World.setVelocityThreshold(World.getVelocityThreshold() * Shared.xml.velocityThresholdFactor);
		listener = new Listener(this);
		world.setContactListener(listener);
		world.setContinuousPhysics(true);
		minX = -arenaWidth / 2;
		minY = -arenaHeight / 2;
		maxX = -arenaWidth / 2;
		maxY = arenaHeight / 2;
		for (int i = 0; i < Shared.maxBullets; i++) {
			bullets[i] = null;
		}
		for (int i = 0; i < Shared.maxPickups; i++) {
			pickups[i] = null;
		}
		for (Player p : players) {
			p.reset();
			p.kills = 0;
			p.deaths = 0;
			p.score = 0;
		}

		Wall.loadWalls(gameType, map, walls);
		addWalls();
	}

	void addWalls() {
		for (Wall w : walls) {
			switch (w.shape) {
			default:
				addWall(w, w.x, w.y, w.d);
				break;
			}
		}
	}

	void addWall(Wall w, float x, float y, float d) {
		w.entity = new WallEntity(w);
		w.body = addWallBody(w, x, y, Wall.getWallWidth(w.thickness), Wall.getWallThick(w.thickness));
		w.body.setTransform(w.body.getPosition().x, w.body.getPosition().y, (float) Math.toRadians(d));
	}

	public Body addWallBody(Wall wall, float x, float y, float w, float h) {
		Body b;
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.set(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale);
		b = world.createBody(bodyDef);
		b.setUserData(wall.entity);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = Shared.xml.barrierDensity;
		fixtureDef.friction = Shared.xml.barrierFriction;
		fixtureDef.restitution = Shared.xml.barrierRestitution;
		fixtureDef.filter.categoryBits = CollisionFlags.CAT_SOLID;
		fixtureDef.filter.maskBits = CollisionFlags.MASKSOLID;
		PolygonShape p = new PolygonShape();
		p.setAsBox((w / Shared.xml.physicsScale) / 2, (h / Shared.xml.physicsScale) / 2, new Vector2(0, 0), 0);
		
		fixtureDef.shape = p;
		b.createFixture(fixtureDef);
		return b;
	}

	void loadMapRotation() {
		try {
			File dir = new File("./assets");
			File fin = new File(dir.getCanonicalPath() + File.separator + "maprot.txt");
			FileInputStream fis = new FileInputStream(fin);

			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String[] words = new String[1];
			String line = null;
			mapCount = 0;
			while ((line = br.readLine()) != null) {
				words = line.split(",");
				rotation[mapCount] = new RotationData(words[0], words[1], words[2], words[3]);
				mapCount++;
			}

			br.close();
		} catch (Exception e) {

		}
	}

	void newGame(int mapNum, int timeLimit, int scoreLimit, int startTime, int endTime) {
		if (startTime < 1000) {
			this.startTime = 1000;
		}
		if (endTime < 2000) {
			this.endTime = 2000;
		}
		curMap = mapNum;
		map = Entities.maps[curMap];
		maxGamePlayers = 32;
		// gameType = map.type;
		resetWorld(map.width, map.height);
		gameStartStamp = tick + startTime;
		this.timeLimit = timeLimit;
		this.scoreLimit = scoreLimit;
		this.startTime = startTime;
		this.endTime = endTime;
		gameInitStamp = tick;
		gameState = 1;
		switch (gameType) {
		case 0: // FFA
			numTeams = 0;
			for (Player p : players) {
				p.team = -1;
			}
			break;
		case 1: // team deathmatch
			numTeams = 2;
			break;
		case 2: // CTF
			numTeams = 2;
			flagbase = new FlagBase[numTeams];
			flag = new Flag[numTeams];
			for (BaseData bd : map.bases) {
				flag[bd.index] = new Flag(this, bd.index, bd.team, bd.x, bd.y);
				flagbase[bd.index] = new FlagBase(this, bd.index, bd.team, bd.x, bd.y);
			}
			break;
		}
		teams = new Team[numTeams];
		for (int i = 0; i < numTeams; i++) {
			teams[i] = new Team(this, i);
		}
		sendAllJoined(getGameInfo(), false);
	}

	void startGame() {
		balanceWarn = 0;
		System.out.println("Game start at " + getTime());
		gameState = 2;
		gameStartStamp = tick;
		for (Player p : players) {
			p.activelyPlaying = false;
			if (p.active()) {
				// if(numTeams == 0 && p.team == -1)
				// p.joinFray();
			}
		}
		sendAllJoined(getGameInfo(), false);
		// perfect place to re-organize teams
		// iterate through players. anyone who is prepped to join automatically
		// spawns
	}

	void endGame() {
		System.out.println("Game completion at " + getTime());
		gameState = 3;
		gameEndStamp = tick;
		sendAllJoined(getGameInfo(), false);
		for (Player p : players) {
			p.destroyBody();
		}
	}

	void processGame() {
		switch (gameState) {
		case 1:
			if (tick > gameInitStamp + startTime) {
				// START ER UP
				startGame();
			}
			break;
		case 2:
			if (timeLimit > 0) {
				if (tick > gameStartStamp + timeLimit * 60000) {
					// times up newb
					endGame();
				}
			}
			break;
		case 3:
			if (tick > gameEndStamp + endTime) {
				nextGame();
			}
			break;

		}
	}

	public GameInfo getGameInfo() {
		GameInfo g = new GameInfo();
		g.gameState = gameState;
		g.gameTime = getGameTime();
		g.howLongBeforeNext = getGameNextChange();
		g.maxGamePlayers = maxGamePlayers;
		g.numTeams = numTeams;
		g.scoreLimit = scoreLimit;
		g.timeLimit = timeLimit;
		g.type = gameType;
		g.mapNum = curMap;
		g.numPlayers = getActivePlayerCount();
		g.name = this.serverName;
		g.version = version;
		if (numTeams > 0) {
			for (int i = 0; i < numTeams; i++) {
				g.teams.add(teams[i].getTeamData());
			}
		}
		return g;
	}

	public int lowestTeam() {
		int lowest = 0;
		int c = 100;
		for (int i = 0; i < numTeams; i++) {
			if (teams[i].numPlayers() < c) {
				lowest = i;
				c = teams[i].numPlayers();
			}
		}
		return lowest;
	}

	public int getActivePlayerCount() {
		int c = 0;
		for (Player p : players) {
			if (p.active()) {
				c++;
			}
		}
		return c;
	}

	public int getGameNextChange() {
		int h = 0;
		switch (gameState) {
		case 1:
			h = (int) ((gameInitStamp + startTime) - tick);
			break;
		case 2:
			if (timeLimit > 0) {
				h = (int) ((gameStartStamp + timeLimit * 60000) - tick);
			} else {
				h = 0;
			}
			break;
		case 3:
			h = (int) ((gameEndStamp + endTime) - tick);
			break;
		}
		return h;
	}

	public long getGameTime() {
		return tick - gameStartStamp;
	}

	public Player joinGame(GameConnection c, String user) {

		Player p = new Player(this, c, user);
		players.add(p);
		JoinGame j = new JoinGame();
		j.serverName = serverName;
		j.index = p.conn.index;
		j.name = user;
		j.players = new LinkedList<PlayerData>();
		for (Player po : players) {
			PlayerData jd = po.getPlayerData();
			if (po.active()) {
				jd.active = true;
			}
			j.players.add(jd);
		}
		j.info = getGameInfo();
		sendTo(p, j, false);
		PlayerData pd = p.getPlayerData();
		pd.join = true;
		sendToAllJoinedBut(p, pd, false);
		p.joinedGame = true;
		return p;
	}

	public synchronized boolean accessPacketQueue(boolean changeIt, boolean changeTo) {
		if (changeIt) {
			checkingPacketQueue = changeTo;
		}
		return checkingPacketQueue;
	}

	public synchronized boolean accessDiscQueue(boolean changeIt, boolean changeTo) {
		if (changeIt) {
			checkingDiscQueue = changeTo;
		}
		return checkingDiscQueue;
	}

	public void connectedLobby() {
		System.out.println("Connected to lobby");
		connectedLobby = true;
		connectingLobby = false;
	}

	public void disconnectedLobby() {
		connectingLobby = false;
		connectedLobby = false;
		reconnectStamp = tick + 3000;
		System.out.println("Disconnected from lobby. Retrying in 3");
	}

	public void failedConnectLobby() {
		connectingLobby = false;
		connectedLobby = false;
		reconnectStamp = tick + 3000;
		System.out.println("Disconnected from lobby. Retrying in 3");
	}

	public void receiveData(GameConnection c, Object object) {
		while (accessPacketQueue(false, false)) {
			// the other thread is actively processing packets so we must wait a
			// SHORT while
		}
		PacketData p = new PacketData();
		p.c = c;
		p.object = object;
		packetQueue.add(p);
	}

	public void processPacket(PacketData p) {
		GameConnection c = p.c;
		Object object = p.object;
		if (p.c == null) {
			// lobby packet
			return;
		}
		c.packets += 1;
		if (object instanceof RequestServerName) {
			// requestServerName n = (requestServerName) object;
			// come back later are put version info in this packet
			ServerName s = new ServerName();
			s.name = serverName;
			s.shared = Shared.xml;
			c.sendTCP(s);
		}
		if (c.player == null) {
			if (object instanceof Login) {
				Login pkt = (Login) object;
				if (pkt.version.equals(version)) {
					if (pkt.newAccount) {
						newAccount(c, pkt.user, pkt.pass);
					} else {
						login(c, pkt.user, pkt.pass);
					}
				} else {
					JoinError je = new JoinError();
					je.s = "Your client is not the right version";
					c.sendTCP(je);
				}
			}
		} else {
			c.player.receive(object);
		}
	}

	public void disconnected(Player p) {
		while (accessDiscQueue(false, false)) {

		}
		if (p != null) {
			discQueue.add(p);
		}
	}

	public void processPacketQueue() {
		accessPacketQueue(true, true);
		try {

			while (!packetQueue.isEmpty()) {
				processPacket(packetQueue.remove());
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		accessPacketQueue(true, false);
	}

	public void processDiscQueue() {
		accessDiscQueue(true, true);
		while (!discQueue.isEmpty()) {
			Player p = discQueue.remove();
			if (p != null) { // Note that disconnected is from server detecting
				// a halt in connection
				p.disconnected(); // but that we will be using disconnect() for
				// client-requested termination
			}
		}
		accessDiscQueue(true, false);
	}

	public void updateLobby() {
		if (!connectedLobby) {
			connectLobby();
		} else {
			if (tick > lobbyStamp) {
				lobbyStamp = tick + 2000;
				GameInfo info = getGameInfo();
				net.client.sendTCP(info);
			}
		}
	}

	public void update() {

		oldTick = tick;
		tick = System.currentTimeMillis();
		processPacketQueue();
		updateLobby();

		if (tick > stepStamp) {
			processDiscQueue();
			stepStamp = tick + Shared.xml.stepTime;

			preStep();
			processDrops(); // remove anything that died during prestep
			if (gameState == 2) {
				for (int step = 0; step < 100; step++) {
					world.step(0.001f, Shared.xml.velocityIterations, Shared.xml.positionIterations);
				}

			}
			postStep();
			processDrops(); // remove anything that died during poststep
			processGame();
			if (tick > minuteStamp) {
				minuteStamp += 60000;
				minuteTimer();
			}
			if (tick > tenStamp) {
				tenStamp += 10000;
				balanceTimer();
			}
			for (Player p : players) {
				if (p.alive()) {
					if (p.cs.fire) {
						p.fire();
					}
				}
			}
			sync();
		}
	}

	void minuteTimer() {

	}

	void balanceTimer() {
		if (numTeams > 0) {
			checkTeamBalance();
		}
		for (GameConnection c : server.connections) {
			if (c != null) {
				if (c.packets > 200) {
					c.close();
					System.out.println("too many packets: " + c.packets);
				}
				c.packets = 0;
			}
		}
		for (Player p : players) {
			if (p != null) {
				if (p.actions > 40) {
					p.conn.close();
					System.out.println("debug2");
				}
				p.actions = 0;
			}
		}
	}

	void checkTeamBalance() {
		if (balanceWarn > 0) {
			if (teams[0].numPlayers() - teams[1].numPlayers() > 1) {
				balanceTeams(0);
			} else if (teams[1].numPlayers() - teams[0].numPlayers() > 1) {
				balanceTeams(1);
			}
			balanceWarn = 0;
		} else {
			if (teams[0].numPlayers() - teams[1].numPlayers() > 2) {
				balanceTeams(0);
			} else if (teams[1].numPlayers() - teams[0].numPlayers() > 2) {
				balanceTeams(1);
			} else if (teams[0].numPlayers() - teams[1].numPlayers() > 1) {
				balanceWarn++;
			} else if (teams[1].numPlayers() - teams[0].numPlayers() > 1) {
				balanceWarn++;
			}
		}
		if (balanceWarn > 0) {
			globalMsg("Auto-balancing in 10 seconds", Color.YELLOW);
		}
	}

	void balanceTeams(int heavier) {
		int diff = teams[heavier].numPlayers() - teams[1 - heavier].numPlayers();
		int move = diff / 2;
		globalMsg("Auto-balancing teams", Color.YELLOW);
		int m = 0;
		for (int i = players.size() - 1; i >= 0; i--) {
			// work backwards to prioritize longest playing
			if (players.get(i) != null) {
				if (m < move) {
					Player p = players.get(i);
					if (p.active() && p.team == heavier && p.flag == null) {
						p.forceSwitch();
						m++;
					}
				}
			}
		}
	}

	void processDrops() {
		for (Entity e : drops) {
			if (e instanceof Player) {
				Player p = (Player) e;
				p.destroy();
				if (server.connections[p.index] != null) {
					server.connections[p.index].close();
					server.connections[p.index] = null;
				}
				players.remove(p);
			} else if (e instanceof Bullet) {
				Bullet b = (Bullet) e;
				bullets[b.index].destroy();
				bullets[b.index] = null;
			} else if (e instanceof Pickup) {
				Pickup p = (Pickup) e;
				pickups[p.index].destroy();
				pickups[p.index] = null;
			}
		}
		drops.clear();
	}

	void preStep() {
		collisions.clear();
		if (gameType == 2) {
			for (int i = 0; i < numTeams; i++) {
				flag[i].update();
				flag[i].preStep();
			}
		}
		for (Player p : players) {
			if (p.joined()) {
				p.preStep();
			}
			if (p.remove) {
				drops.add(p);
			}
		}
		for (Bullet b : bullets) {
			if (b != null) {
				b.preStep();
				if (b.remove) {
					drops.add(b);
				}
			}
		}
		for (Pickup b : pickups) {
			if (b != null) {
				b.preStep();
				if (b.remove) {
					drops.add(b);
				}
			}
		}
	}

	void postStep() {
		if (gameType == 2) {
			for (int i = 0; i < numTeams; i++) {
				flag[i].postStep();
			}
		}
		for (Player p : players) {
			if (p.joined()) {
				p.postStep();
			}
			if (p.remove) {
				drops.add(p);
			}
		}
		for (Bullet b : bullets) {
			if (b != null) {
				b.postStep();
				if (b.remove) {
					drops.add(b);
				}
			}
		}
		for (Pickup b : pickups) {
			if (b != null) {
				b.postStep();
				if (b.remove) {
					drops.add(b);
				}
			}
		}
		processCollisions();
	}

	void sync() {

		for (Player p : players) {
			if (p.active() || p.spectating) {
				Sync s = new Sync();
				s.tick = tick;
				if (!p.spectating) {
					s.hp = p.hp;
					s.maxhp = p.maxhp;
					s.reloading = p.reloading;
					s.weapon = p.weapons[p.curWeapon].type;
					s.shotsLeft = p.weapons[p.curWeapon].shotsLeft;
					s.item0 = p.items[0];
					s.item1 = p.items[1];
					s.item2 = p.items[2];
				} else {
					s.sx = (int) p.x;
					s.sy = (int) p.y;
					s.si = p.spec;
					s.item0 = 0;
					s.item1 = 1;
					s.item2 = 2;
				}
				if (!p.beenDead) {
					for (Player p2 : players) {
						if (p2.active() && !p2.beenDead) {
							if (p.inRange(p2)) {
								s.s.add(p2.getSyncData());
							}
						}
					}
					for (Pickup p2 : pickups) {
						if (p2 != null && p2.active()) {
							if (p.inRange(p2)) {
								s.s.add(p2.getSyncData());
							}
						}
					}
					for (Bullet p2 : bullets) {
						if (p2 != null && p2.active()) {
							if (p.inRange(p2)) {
								s.s.add(p2.getSyncData());
							}
						}
					}
					if (gameType == 2) {
						for (int i = 0; i < numTeams; i++) {
							s.flags.add(flag[i].getFlagData());
						}
					}
				}
				sendTo(p, s, true);
			}
		}
	}

	void beginCollision(Entity e1, Entity e2, Vector2[] points) {
		CollisionData cd = new CollisionData();
		cd.e1 = e1;
		cd.e2 = e2;
		cd.points = points;
		collisions.add(cd);
	}

	void processCollisions() {
		while (!collisions.isEmpty()) {
			CollisionData cd = collisions.remove();
			if (cd != null) {
				if (cd.e1 instanceof Bullet || cd.e2 instanceof Bullet) {
					if (cd.e1 instanceof Bullet && cd.e2 instanceof Bullet) {
						// two bullets collided but this shouldnt happen because
						// of collision flags
					} else if (cd.e1 instanceof Bullet) {
						// bullet hit something
						if (cd.e2 instanceof Player) {
							Player p = (Player) cd.e2;
							Bullet b = (Bullet) cd.e1;
							if (b.owner.name != p.name) {
								// HIT AN ENEMY
								p.struckBy(b, cd.points);

								b.remove();
							}
						} else if (!(cd.e2 instanceof FlagBase)) {
							Bullet b = (Bullet) cd.e1;

							b.remove();
						}
					} else { // safe to assume cd.e2 instance of Bullet == true
						// bullet hit something
						if (cd.e1 instanceof Player) {
							Player p = (Player) cd.e1;
							Bullet b = (Bullet) cd.e2;
							if (b.owner.name != p.name) {
								// HIT AN ENEMY
								p.struckBy(b, cd.points);
								b.remove();
							}
						} else if (!(cd.e1 instanceof FlagBase)) {
							Bullet b = (Bullet) cd.e2;
							b.remove();
						}
					}
				} else if (cd.e1 instanceof Pickup || cd.e2 instanceof Pickup) {
					if (cd.e1 instanceof Pickup && cd.e2 instanceof Pickup) {
						// two pickups collided, blah blah
					} else if (cd.e1 instanceof Pickup) {
						if (cd.e2 instanceof Player) {
							Player p = (Player) cd.e2;
							Pickup b = (Pickup) cd.e1;
							System.out.println("woo");
							p.pickup(b);
						}
					} else { // safe to assume cd.e2 instance of Pickup == true
						if (cd.e1 instanceof Player) {
							Player p = (Player) cd.e1;
							Pickup b = (Pickup) cd.e2;
							System.out.println("woo");
							p.pickup(b);
						}
					}
				} else if (cd.e1 instanceof Player || cd.e2 instanceof Player) {
					if (cd.e1 instanceof Player && cd.e2 instanceof Player) {
						// two ships collided, do some damage or something
					} else if (cd.e1 instanceof Player) {
						// e1 is a player colliding with something besides a
						// bullet or player
					} else { // safe to assume cd.e2 instance of Player == true
						// e2 is a player colliding with something besides a
						// bullet or player
					}
				}
				if (cd.e2 instanceof Player) {
					Player p = (Player) cd.e2;
					checkFlag(cd.e1, p);
				}
				if (cd.e1 instanceof Player) {
					Player p = (Player) cd.e1;
					checkFlag(cd.e2, p);
				}

			}
		}
	}

	void checkFlag(Entity e1, Player p) {
		if (e1 instanceof FlagBase) {
			FlagBase fb = (FlagBase) e1;
			fb.collide(p);
		}
		if (e1 instanceof Flag) {
			Flag f = (Flag) e1;
			f.collide(p);
		}
	}

	void endCollision(Entity e1, Entity e2, Vector2[] points) {

	}

	public void login(GameConnection c, String user, String pass) {
		if (verifyUser(c, user, pass)) {
			c.index = server.freeConnection();
			if (c.index >= 0) {
				server.connections[c.index] = c;
				c.player = joinGame(c, user);
			} else {
				JoinError j = new JoinError();
				j.s = "The server is full!";
				c.sendTCP(j);
			}
		}
	}

	public String getTime() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm");// dd/MM/yyyy
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	public void newAccount(GameConnection c, String user, String pass) {
		// add passes and accounts later ya dig
	}

	public boolean validName(GameConnection c, String user) {
		// first make sure its correct length
		if (user == null) {
			return false;
		} // dont even bother sending a packet because this is hacker activity
		if (!(user.length() >= Shared.minUserLength && user.length() <= Shared.maxUserLength)) {
			return false;
		} // same
			// now make sure nobody else is using it
		for (Player p : players) {
			if (p.name.toUpperCase().equals(user.toUpperCase())) {
				JoinError j = new JoinError();
				j.s = "That name is already in use";
				j.c = 1;
				c.sendTCP(j);
				return false;
			}
		}
		return true; // good to go m8
	}

	public boolean verifyUser(GameConnection c, String user, String pass) {
		return (validName(c, user));
		// add passes and accounts later ya dig
	}

	public int freeBullet() {
		for (int i = 0; i < Shared.maxBullets; i++) {
			if (bullets[i] == null) {
				return i;
			}
		}
		return -1;
	}

	public int freePickup() {
		for (int i = 0; i < Shared.maxPickups; i++) {
			if (pickups[i] == null) {
				return i;
			}
		}
		return -1;
	}

	public void newBullet(Player shooter, int type, float x, float y, float direction) {
		int i = freeBullet();
		if (i >= 0) {
			bullets[i] = new Bullet(this, shooter, i, type, x, y, direction);
			BulletData bd = bullets[i].getBulletData();
			WeaponDef wd = Entities.weapons[shooter.weapons[shooter.curWeapon].type];
			bd.head = wd.head;
			bd.body = wd.body;
			sendAllPlaying(bd, false);
		}
	}

	public void newPickup( int type, int qty, float x, float y) {
		int i = freePickup();
		if (i >= 0) {
			pickups[i] = new Pickup(this, i, type, qty, x, y);
			PickupData bd = pickups[i].getPickupData();
			sendAllPlaying(bd, false);
		}
	}

	public void sendAllJoined(Object obj, boolean UDP) {
		for (Player p : players) {
			if (p.joined()) {
				if (UDP) {
					p.conn.sendUDP(obj);
				} else {
					p.conn.sendTCP(obj);
				}
			}
		}
	}

	public void sendAllPlaying(Object obj, boolean UDP) {
		for (Player p : players) {
			if (p.active() || p.spectating) {
				if (UDP) {
					p.conn.sendUDP(obj);
				} else {
					p.conn.sendTCP(obj);
				}
			}
		}
	}

	public void sendToAllJoinedBut(Player but, Object obj, boolean UDP) {
		for (Player p : players) {
			if (p != but) {
				if (p.joined()) {
					if (UDP) {
						p.conn.sendUDP(obj);
					} else {
						p.conn.sendTCP(obj);
					}
				}
			}
		}
	}

	public void sendToAllPlayingBut(Player but, Object obj, boolean UDP) {
		for (Player p : players) {
			if (p != but) {
				if (p.active() || p.spectating) {
					if (UDP) {
						p.conn.sendUDP(obj);
					} else {
						p.conn.sendTCP(obj);
					}
				}
			}
		}
	}

	public void sendTo(Player p, Object obj, boolean UDP) {
		if (UDP) {
			server.server.sendToUDP(p.conn.getID(), obj);
		} else {
			server.server.sendToTCP(p.conn.getID(), obj);
		}
	}

	public boolean emptySpot(float x, float y, int team) {
		float d = 0;
		for (Player p : players) {
			if (gameType == 0) {
				d = Config.xml.spawnBufferPlayer;
			} else {
				if(team == p.team) {
					d = Config.xml.spawnBufferFriendly;
				} else {
					d = Config.xml.spawnBufferPlayer;
				}
			}
			if (Vector.distance(x, y, p.x, p.y) < d) {
				return false;
			}

		}
		for (Wall w : walls) {
			if (Vector.distance(x, y, w.x, w.y) < Config.xml.spawnBufferWall) {
				return false;
			}
		}
		for (Flag f : flag) {
			if (f != null) {
				if (Vector.distance(x, y, f.x, f.y) < Config.xml.spawnBufferWall) {
					return false;
				}
			}
		}
		for (Rect r : map.rects) {
			if (x > r.x1 && x < r.x2 && y > r.y1 && y < r.y2) {
				return false;
			}
		}
		if (gameType == 2) {
			float bx = 0, by = 0;
			for (BaseData b : map.bases) {
				if (b.team == team) {
					bx = b.x;
					by = b.y;
				}
			}
			if (Vector.distance(x, y, bx, by) > 500) {
				return false;
			}
		}
		return true;
	}

	public Coord findClearSpot(int team) {
		float x = 0, y = 0;
		Coord c = new Coord();
		c.x = 900;
		c.y = 9000;
		int i = 0;
		boolean found = false;
		do {
			i++;
			x = Random.getInt(map.width - 50) - ((map.width - 50) / 2);
			y = Random.getInt(map.height - 50) - ((map.height - 50) / 2);
			if (emptySpot(x, y, team)) {
				found = true;
				c.x = x;
				c.y = y;
				return c;
			}
			if (i > 10000) {
				found = true;
			}
		} while (found == false);
		return c;
	}

	public Player findPlayer(String name) {
		for (Player p : players) {
			if (p != null) {
				String pname = p.name.substring(0, name.length());
				if (p.joined() && name.toUpperCase().equals(pname.toUpperCase())) {
					return p;
				}
			}
		}
		return null;
	}

	public Player findPlayer(int i) {
		for (Player p : players) {
			if (p != null) {
				if (p.joined() && i == p.index) {
					return p;
				}
			}
		}
		return null;
	}

	public void globalMsg(Player s, String msg, Color c) {
		for (Player p : players) {
			if (p != null && p.joined()) {
				playerMsg(s, p, msg, c);
			}
		}
	}

	public void globalMsg(String msg, Color c) {
		for (Player p : players) {
			if (p != null && p.joined()) {
				playerMsg(p, msg, c);
			}
		}
	}

	public void teamMsg(Player s, int team, String msg) {
		ChatToClient cc = new ChatToClient();
		cc.c = Color.CYAN;
		cc.msg = msg;
		for (Player p : players) {
			if (p != null) {
				if (p.active()) {
					if (p.team == team) {
						playerMsg(s, p, msg, cc.c);
					}
				}
			}
		}
	}

	public void playerMsg(Player s, Player p, String msg, Color c) {
		ChatToClient cc = new ChatToClient();
		cc.c = c;
		cc.msg = msg;
		if (p != null && p.joined()) {
			if (!p.inMutes(s)) {
				p.conn.sendTCP(cc);
			}
		}
	}

	public void playerMsg(Player p, String msg, Color c) {
		ChatToClient cc = new ChatToClient();
		cc.c = c;
		cc.msg = msg;
		if (p != null && p.joined()) {
			p.conn.sendTCP(cc);
		}
	}

}
