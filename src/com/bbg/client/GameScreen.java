package com.bbg.client;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.bbg.shared.Entities;
import com.bbg.shared.MapDef.BaseData;
import com.bbg.lobby.LobbyNetwork.LobbyJoinError;
import com.bbg.lobby.LobbyNetwork.LobbyLogin;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Network.BulletData;
import com.bbg.shared.Network.ChangeLoad;
import com.bbg.shared.Network.ChatToClient;
import com.bbg.shared.Network.ChatToServer;
import com.bbg.shared.Network.FlagData;
import com.bbg.shared.Network.FrayError;
import com.bbg.shared.Network.GameInfo;
import com.bbg.shared.Network.JoinError;
import com.bbg.shared.Network.JoinGame;
import com.bbg.shared.Network.KillData;
import com.bbg.shared.Network.LeftSpec;
import com.bbg.shared.Network.Login;
import com.bbg.shared.Network.PickupData;
import com.bbg.shared.Network.PingPacket;
import com.bbg.shared.Network.PlayPacket;
import com.bbg.shared.Network.PlayerData;
import com.bbg.shared.Network.PlayingData;
import com.bbg.shared.Network.PongPacket;
import com.bbg.shared.Network.SecondData;
import com.bbg.shared.Network.SecondPacket;
import com.bbg.shared.Network.ServerName;
import com.bbg.shared.Network.Sync;
import com.bbg.shared.Network.SyncData;
import com.bbg.shared.Network.TeamData;
import com.bbg.shared.Network.RequestServerName;
import com.bbg.shared.Shared;

import com.bbg.shared.Wall;

public class GameScreen implements Screen {
	public String serverIP = "";
	public String lobbyIP = "";
	public String version = "hotrod4";
	private float screenWidth, screenHeight;
	public float viewWidth, viewHeight;
	public int originX, originY;
	public OrthographicCamera cam;
	private ShapeRenderer shapeRenderer;
	public SpriteBatch batcher;
	public String frayError = "";
	public boolean connectedLobby = false;
	public long newPacketTick = 0;
	public long escCoolAt = 0;

	public float xO = 0;
	public float yO = 0;

	public boolean connectingYet = false;
	public int c = 0;
	public String serverName = "";

	public boolean screenLoaded = false;

	public long reconnectStamp = 0;

	Scene scene;
	Lobby lobby;
	GameMenu menu = new GameMenu();

	public int gameState = 0; // 0 = connecting, 1 = connected, 2 = ?, 3 =
								// profit (playing), 4 = failed to connect
	NetClient net;
	NetClient lobbynet;

	public boolean rendering = false;

	public long lastSyncAt = 0;

	public String user, pass;

	long tick = 0;
	int myIndex;

	public boolean firstMessage = false;

	public GameScreen() {

		lobby = new Lobby(this);
		Entities.init();

		net = new NetClient(this, false);
		lobbynet = new NetClient(this, true);
		setupScreen(Shared.gameWidth, Shared.gameHeight);
		scene = new Scene(this, Shared.gameWidth, Shared.gameHeight);
		Gdx.input.setInputProcessor(scene.input);
		screenLoaded = true;

	}

	private void setupScreen(float gameWidth, float gameHeight) {
		screenWidth = Gdx.graphics.getWidth();
		screenHeight = Gdx.graphics.getHeight();
		float screenR = (float) screenWidth / (float) screenHeight;
		float gameR = gameWidth / gameHeight;
		if (screenR == gameR) {
			originX = 0;
			originY = 0;
			viewWidth = gameWidth;
			viewHeight = gameHeight;
		} else if (screenR > gameR) {
			viewWidth = gameHeight * screenR;
			viewHeight = gameHeight;
			originX = (int) ((viewWidth - gameWidth) / 2.0f);
			originY = 0;
		} else if (screenR < gameR) {
			viewWidth = gameWidth;
			viewHeight = gameWidth / screenR;
			originX = 0;
			originY = (int) ((viewHeight - gameHeight) / 2.0f);
		}
		// Set up our camera, which handles the screen scaling, use viewWidth to
		// include letterbox area
		cam = new OrthographicCamera();
		cam.setToOrtho(true, Math.round(viewWidth), Math.round(viewHeight));
		// Create our sprite batcher and shape renderer from the camera
		batcher = new SpriteBatch();
		batcher.setProjectionMatrix(cam.combined);

		shapeRenderer = new ShapeRenderer();
		shapeRenderer.setProjectionMatrix(cam.combined);
	}

	public void connectedGame() {
		scene.flag[0] = null;
		scene.flag[1] = null;
		scene.spectating = false;
		gameState = 2;
		// play();
		RequestServerName n = new RequestServerName();
		net.client.sendTCP(n);
	}

	public void failedConnectGame() {
		gameState = 4;
		reconnectStamp = tick + 3000;
	}

	public void disconnectedGame() {
		if (myIndex >= 0 && scene.players[myIndex] != null) {
			scene.players[myIndex].playing = false;
		}
		scene.playChatting = false;
		gameState = 4;
		reconnectStamp = tick + 3000;

	}

	public void connectedLobby() {
		connectedLobby = true;
		gameState = 6;
		scene.playSound(17, 1, 1);
		if (!(Options.beenControls && Options.beenLoadout)) {
			firstMessage = true;
		} else {
			firstMessage = false;
			scene.input.keyPress.clear();
		}
		scene.input.keyPress.clear();
		// requestServerName n = new requestServerName();
		// lobbynet.client.sendTCP(n);
	}

	public void failedConnectLobby() {
		connectedLobby = false;
		gameState = 5;
		reconnectStamp = tick + 3000;
	}

	public void disconnectedLobby() {
		connectedLobby = false;
		System.out.println("Lobby disconnect!");
		if (gameState == 3) {
			return;
		}
		gameState = 5;
		reconnectStamp = tick + 3000;
		moveCameraTo(scene.gameWidth / 2, scene.gameHeight / 2);
	}

	public void play() {
		gameState = 2;
		Login n = new Login();
		n.user = scene.curNick;
		// n.user = "lol";
		n.version = version;
		net.client.sendTCP(n);
	}

	public void enterLobby() {
		gameState = 7;
		LobbyLogin n = new LobbyLogin();
		n.user = scene.curNick;
		lobbynet.client.sendTCP(n);
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

	public void connectGame(String ip) {
		serverIP = ip;
		net.connectGame(serverIP);
		gameState = 0;
	}

	public void connectLobby() {
		FileHandle fh = Gdx.files.local("assets/lobbyIP.txt");
		lobbyIP = fh.readString();
		System.out.println(lobbyIP);
		
		lobbynet.connectLobby(lobbyIP);
	}

	public void sendChat(String s) {
		ChatToServer n = new ChatToServer();
		n.msg = s;
		net.client.sendTCP(n);
	}

	public Player getMe() {
		return scene.players[myIndex];
	}

	public void processGameInfo() {
		int oldNumTeams = scene.info.numTeams;
		int winningTeam = 0;
		TeamData high = new TeamData();
		high.index = -1;
		int highest = -1;
		if (oldNumTeams > 0) {
			for (TeamData td : scene.info.teams) {
				if (td.score > highest) {
					high = td;
					highest = td.score;
				}
			}
			if (highest >= 0) {
				winningTeam = high.index;
			}
		}
		scene.gameStamp = scene.tick;
		scene.map = Entities.maps[scene.info.mapNum];

		scene.mapNum = scene.info.mapNum;

		if (!scene.map.bases.isEmpty()) {
			scene.numBases = scene.map.bases.size();
			scene.base = new Base[scene.numBases];
			for (BaseData bd : scene.map.bases) {
				scene.base[bd.index] = new Base(scene, bd.index, bd.team, bd.x, bd.y, bd.rotSpeed, bd.scale);
			}
		}
		if (scene.info.type != 2) {
			if (scene.base[0] != null) {
				scene.base[0].active = false;
			}
			if (scene.base[1] != null) {
				scene.base[1].active = false;
			}

		}
		if (scene.info.numTeams > 0) {
			scene.teams = new Team[scene.info.numTeams];
			for (TeamData td : scene.info.teams) {
				scene.teams[td.index] = new Team(scene, td.index, td.name, td.score);
			}
		}
		scene.playChatting = false;
		switch (scene.info.gameState) {
		case 1:
			scene.explosions.clear();
			for (int i = 0; i < Shared.maxPlayers; i++) {
				if (scene.players[i] != null) {
					scene.players[i].reset();
					scene.players[i].playing = false;
					scene.players[i].team = -1;
					scene.players[i].active = false;
					scene.players[i].dead = false;
				}
			}

			break;
		case 2:
			scene.walls.clear();
			Wall.loadWalls(scene.info.type, scene.map, scene.walls);
			for (Player p : scene.players) {
				if (p != null) {
					p.playing = false;
					p.drawX = 0;
					p.drawY = 0;
					p.x = 0;
					p.y = 0;
					p.newX = 0;
					p.newY = 0;
				}
			}
			scene.playSound(25, 1, 1);
			break;
		case 3:
			highest = 0;
			int shigh = -1;
			for (int i = 0; i < Shared.maxPlayers; i++) {
				if (scene.players[i] != null) {
					Player p = scene.players[i];
					if (p.score > highest) {
						shigh = i;
						highest = p.score;
					}
					p.active = false;
					p.drawDuration = 0;
					p.x = p.drawX;
					p.y = p.drawY;
					p.newX = p.x;
					p.newY = p.y;
				}
			}
			int s = 21;
			if (oldNumTeams == 0) {
				if (shigh >= 0) {
					if (scene.players[myIndex].score < highest) {
						s = 22;
					}
				}
			} else {
				if (scene.players[myIndex].team != winningTeam) {
					s = 22;
				}
			}
			scene.playSound(s, 1, 1);
			break;
		}
	}

	public void receiveData(Object object) {
		if (object == null) {
			return;
		}
		while (accessRenderState(false, false)) {
			// the other thread is actively processing packets so we must wait a
			// SHORT while
			// shouldnt we add a sleep?
		}
		accessRenderState(true, true);
		lobby.receiveLobby(object);
		if (object instanceof GameInfo) {
			GameInfo info = (GameInfo) object;
			scene.info = info;
			processGameInfo();
		}
		if (object instanceof FrayError) {
			FrayError fe = (FrayError) object;
			frayError = fe.s;
		}
		if (object instanceof SecondPacket) {
			SecondPacket pp = (SecondPacket) object;
			for (SecondData pd : pp.data) {
				int i = pd.index;
				if (scene.players[i] != null) {
					scene.players[i].ping = pd.ping;
					scene.players[i].score = pd.score;
				}
				if (scene.info.numTeams > 0) {
					scene.teams[0].score = pp.score0;
					scene.teams[1].score = pp.score1;
				}
			}

		}
		if (object instanceof PingPacket) {
			PingPacket pp = (PingPacket) object;
			PongPacket po = new PongPacket();
			po.id = pp.id;
			net.client.sendTCP(po);
		}
		if (object instanceof JoinError) {
			JoinError j = (JoinError) object;
			scene.curStatus = j.s;
			if (j.c == 1) {
				gameState = 1;
			}
		}
		if (object instanceof LobbyJoinError) {
			LobbyJoinError j = (LobbyJoinError) object;
			gameState = 6;
			scene.curStatus = j.s;
		}
		if (object instanceof ChatToClient) {
			ChatToClient cc = (ChatToClient) object;
			scene.addChat(cc.msg, cc.c);
		}
		if (object instanceof ServerName) {
			ServerName n = (ServerName) object;
			serverName = n.name;
			Shared.xml = n.shared;
			play();
		}
		if (object instanceof JoinGame) {
			JoinGame j = (JoinGame) object;
			int i = j.index;
			serverName = j.serverName;
			for (int t = 0; t < Shared.maxPlayers; t++) {
				scene.players[t] = null;
			}
			scene.players[i] = new Player(scene, i, j.name, 0, 0, 0);

			myIndex = i;

			scene.joined = true;
			scene.active = false;

			for (PlayerData pd : j.players) {
				scene.players[pd.index] = new Player(scene, pd.index, pd.name, 0, 0, 0);
				scene.players[pd.index].kills = pd.kills;
				scene.players[pd.index].deaths = pd.deaths;
				scene.players[pd.index].score = pd.score;
				scene.players[pd.index].team = pd.team;
				scene.players[pd.index].col = pd.col;
				scene.players[pd.index].armor = pd.armor;
				if (pd.dead) {
					scene.players[pd.index].dead = true;
				}
				if (pd.active) {
					scene.players[pd.index].playing = true;

				}
			}

			scene.info = j.info;
			processGameInfo();
			gameState = 3;
		}

		if (object instanceof PlayerData) {
			PlayerData j = (PlayerData) object;
			Player p = scene.players[j.index];
			if (j.leave) {
				// announce leavegame
				scene.addChat(j.name + " has left the game", Color.YELLOW);
				scene.players[j.index] = null;
			} else {
				if (p == null) {
					scene.players[j.index] = new Player(scene, j.index, j.name, 0, 0, 0);
					p = scene.players[j.index];
				}
				p.index = j.index;
				p.name = j.name;
				p.dead = j.dead;
				p.kills = j.kills;
				p.deaths = j.deaths;
				p.col = j.col;
				p.score = j.score;
				p.drawDuration = 0;
				p.x = j.x;
				p.y = j.y;
				p.newX = j.x;
				p.newY = j.y;
				p.drawX = j.x;
				p.drawY = j.y;
				p.armor = j.armor;
				p.curDirection = j.direction;
				p.newDirection = j.direction;
				p.drawDirection = j.direction;
				p.team = j.team;
				if (j.join) {
					// announce joingame
					scene.addChat(j.name + " has joined the game", Color.YELLOW);
				}
				if (j.active) {
					p.playing = true;
					p.setupBody();
					scene.play3D(20, p.x, p.y, 1, 1);
				} else if (j.deactive) {
					p.playing = false;
					p.active = false;
					p.x = 0;
					p.y = 0;
					p.newX = 0;
					p.newY = 0;
					p.drawX = 0;
					p.drawY = 0;
				}
			}
		}

		if (object instanceof PlayPacket) {
			PlayPacket j = (PlayPacket) object;
			int i = myIndex;
			for (int u = 0; u < Shared.maxBullets; u++) {
				scene.bullets[u] = null;
			}
			for (int u = 0; u < Shared.maxPickups; u++) {
				scene.pickups[u] = null;
			}
			if (!j.spectating) {
				scene.minX = j.minX;
				scene.minY = j.minY;
				scene.maxX = j.maxX;
				scene.maxY = j.maxY;
				scene.players[i].maxhp = j.hp;
				scene.players[i].col = j.col;
				scene.players[i].hp = j.hp;
				scene.players[i].x = j.x;
				scene.players[i].y = j.y;
				scene.players[i].drawX = j.x;
				scene.players[i].drawY = j.y;
				scene.players[i].drawDuration = 0;
				scene.players[i].newX = j.x;
				scene.players[i].newY = j.y;
				scene.players[i].newDirection = j.direction;
				scene.players[i].curDirection = j.direction;
				scene.players[i].drawDirection = j.direction;
				scene.players[i].team = j.team;
				scene.players[i].armor = j.armor;
				scene.items = j.items;
				scene.players[i].dead = false;
			}
			scene.spectating = j.spectating;
			for (BulletData bd : j.bullets) {
				scene.bullets[bd.index] = new Bullet(scene, bd.index, bd.type, bd.x, bd.y, bd.direction, bd.owner);
			}
			for (PickupData bd : j.pickups) {
				scene.pickups[bd.index] = new Pickup(scene, bd.index, bd.type, bd.x, bd.y);
			}
			for (PlayingData pd : j.pad) {
				if (scene.players[pd.index] != null) {
					scene.players[pd.index].playing = pd.playing;
					scene.players[pd.index].setupBody();
				}
			}
			scene.playSound(20, 1, 1);
			scene.players[i].playing = !j.spectating;
			scene.input.mouseDown = false;
			scene.input.wasMouseJustClicked = false;
			scene.resetWorld();
		}

		if (object instanceof BulletData) {
			BulletData bd = (BulletData) object;
			if (bd.remove) {
				if (scene.bullets[bd.index] != null) {
					Bullet b = scene.bullets[bd.index];
					b.x = bd.x;
					b.y = bd.y;
					b.drawX = bd.x;
					b.drawY = bd.y;
					b.newX = bd.x;
					b.newY = bd.y;
					if (!b.triggered) {
						b.trig(bd.playerStruck, new Vector2(bd.sX, bd.sY));
					} else {
					}
					scene.bullets[bd.index] = null;

				} else {

				}
			} else {
				scene.bullets[bd.index] = new Bullet(scene, bd.index, bd.type, bd.x, bd.y, bd.direction, bd.owner);
				Bullet b = scene.bullets[bd.index];
				b.speedX = bd.vX;
				b.speedY = bd.vY;
				if (bd.head >= 0) {
					scene.play3D(100 + bd.head, b.x, b.y, 0.6f, 1);
				}
				if (bd.body >= 0) {
					scene.play3D(110 + bd.body, b.x, b.y, 0.6f, 1);
				}
				b.active = true;
				b.drawDuration = 100;
				b.drawStart = scene.lastDrawStart + 100;
				b.lastUpdate = scene.tick;
				b.drawX = b.x;
				b.drawY = b.y;
				b.curDirection = bd.direction;
				b.newDirection = bd.direction;
				b.drawDirection = b.curDirection;
				b.dontClear = true;
				if (b.body == null) {
					b.createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, true, true, true);
					b.body.setBullet(true);
				}
			}
		}

		if (object instanceof PickupData) {
			PickupData bd = (PickupData) object;
			if (bd.remove) {
				if (scene.pickups[bd.index] != null) {
					Pickup b = scene.pickups[bd.index];
					scene.makePoop(15, b.drawX, b.drawY, 2, Entities.alterColor(b.originalCol, .2f), true);
					scene.pickups[bd.index] = null;
				} else {

				}
			} else {
				scene.pickups[bd.index] = new Pickup(scene, bd.index, bd.type, bd.x, bd.y);
				Pickup b = scene.pickups[bd.index];
				b.speedX = bd.vX;
				b.speedY = bd.vY;
				b.active = true;
				b.drawDuration = 100;
				b.drawStart = scene.lastDrawStart + 100;
				b.lastUpdate = scene.tick;
				b.drawX = b.x;
				b.drawY = b.y;
				b.curDirection = bd.direction;
				b.newDirection = bd.direction;
				b.drawDirection = b.curDirection;
			}
		}

		if (object instanceof KillData) {
			String killerName;
			KillData kd = (KillData) object;
			Player killer = scene.players[kd.killerIndex];
			Player killed = scene.players[kd.killedIndex];

			if (!kd.lostKiller) {
				if (killer == null) {
					return;
				}
				killer.kills += 1;
				killerName = killer.name;
			} else {
				killerName = "Someone";
			}
			killed.deaths += 1;
			for (int i = 0; i < 15; i++) {
				PolyPoop p = new PolyPoop(scene, killed.drawX, killed.drawY, 1200, 5f, .5f,
						Entities.randomBrightColor());
				scene.explosions.add(p);
			}
			scene.play3D(19, killed.drawX, killed.drawY, 1, 1);
			scene.makePoop(10, killer.drawX, killer.drawY, 3, Color.WHITE, true);
			String kv = killed.name + " commit suicide";
			if (!killerName.equals("Someone")) {
				kv = kv + "[Last shot by " + killerName + "]";
			}
			if (kd.type >= 0) {
				kv = killerName + " " + Entities.weapons[kd.type].killVerb + " " + killed.name;
			}
			scene.addChat(kv, Color.RED);
			if (scene.players[kd.killedIndex] != null) {
				scene.players[kd.killedIndex].dead = true;
				if (kd.killedIndex == myIndex) {
					ChangeLoad cl = new ChangeLoad();
					cl.load = menu.load[scene.curLoad];
					scene.screen.net.client.sendTCP(cl);
					scene.respawnStamp = scene.tick + Shared.xml.respawnTime;
				}
			}
		}
		if (object instanceof LeftSpec) {
			scene.spectating = false;
		}
		if (object instanceof Sync) {
			if (gameState == 3) {
				Sync s = (Sync) object;
				if (s.tick > newPacketTick) {
					newPacketTick = s.tick;
					lastSyncAt = tick;
					scene.resetWorld();
					if (!scene.spectating) {
						scene.weapon = s.weapon;
						scene.shotsLeft = s.shotsLeft;
						scene.players[myIndex].turboing = s.turbo;
						scene.items[0] = s.item0;
						scene.items[1] = s.item1;
						scene.items[2] = s.item2;
					} else {
						if (s.si == -1) {
							scene.players[myIndex].drawX = s.sx;
							scene.players[myIndex].drawY = s.sy;
							scene.players[myIndex].x = s.sx;
							scene.players[myIndex].y = s.sy;
							scene.players[myIndex].newX = s.sx;
							scene.players[myIndex].newY = s.sy;
							scene.si = -1;
						} else {
							scene.si = s.si;
						}

					}
					for (SyncData sd : s.s) {
						int index = sd.index;
						if (index >= (Shared.maxPlayers + Shared.maxBullets)) {
							index -= (Shared.maxPlayers + Shared.maxBullets);
							Pickup b = scene.pickups[index];
							if (b != null) {
								b.sync(s.tick, sd);
								b.lastUpdate = scene.tick;
							}
						} else if (index >= (Shared.maxPlayers)) {
							index -= (Shared.maxPlayers);
							Bullet b = scene.bullets[index];
							if (b != null) {
								b.sync(s.tick, sd);
								b.lastUpdate = scene.tick;
							}

						} else {
							if (scene.players[index] != null) {
								Player p = scene.players[index];
								if (index == myIndex) {
									p.hp = s.hp;
									p.maxhp = s.maxhp;
									p.reloading = s.reloading;
								}
								p.accel = sd.accel;
								p.decel = sd.decel;
								p.strafeLeft = sd.strafeLeft;
								p.strafeRight = sd.strafeRight;
								p.sync(s.tick, sd);
								p.lastUpdate = scene.tick;
							}
						}
					}

					// flag stuff
					if (scene.info.type == 2) {
						for (FlagData fd : s.flags) {
							if (scene.flag[fd.index] == null) {
								scene.flag[fd.index] = new Flag(scene, fd.index, fd.team, fd.x, fd.y);
							}
							Flag f = scene.flag[fd.index];
							f.index = fd.index;
							f.team = fd.team;
							f.x = fd.x;
							f.y = fd.y;
							f.sync(s.tick, fd);
							f.lastUpdate = scene.tick;
						}
					}

					// if (s.lastOne) {
					scene.collisions.clear();
					if (scene.info.type == 2) {
						for (int i = 0; i < scene.info.numTeams; i++) {
							if (scene.flag[i] != null) {
								scene.flag[i].preStep();
							}
						}
					}
					for (int i = 0; i < Shared.maxPlayers; i++) {
						if (scene.players[i] != null) {
							if (scene.players[i].active && scene.players[i].playing && scene.players[i].body != null
									&& !scene.players[i].dead) {
								scene.players[i].preStep();
							}
						}
					}
					for (int i = 0; i < Shared.maxBullets; i++) {
						if (scene.bullets[i] != null) {
							if (scene.bullets[i].active) {
								if (scene.bullets[i].body != null) {
									scene.bullets[i].preStep();
								}
							}
						}
					}
					for (int i = 0; i < Shared.maxPickups; i++) {
						if (scene.pickups[i] != null) {
							if (scene.pickups[i].active) {
								if (scene.pickups[i].body != null) {
									scene.pickups[i].preStep();
								}
							}
						}
					}
					for (int step = 0; step < 100; step++) {
						scene.world.step(0.001f, Shared.xml.velocityIterations, Shared.xml.positionIterations);
					}
					for (int i = 0; i < Shared.maxPlayers; i++) {
						if (scene.players[i] != null) {
							if (scene.players[i].active && scene.players[i].playing && scene.players[i].body != null
									&& !scene.players[i].dead) {
								scene.players[i].postStep();
							}
						}
					}
					for (int i = 0; i < Shared.maxBullets; i++) {
						if (scene.bullets[i] != null) {
							if (scene.bullets[i].active) {
								if (scene.bullets[i].body != null) {
									scene.bullets[i].postStep();
								}
							}
						}
					}
					for (int i = 0; i < Shared.maxPickups; i++) {
						if (scene.pickups[i] != null) {
							if (scene.pickups[i].active) {
								if (scene.pickups[i].body != null) {
									scene.pickups[i].postStep();
								}
							}
						}
					}
					if (scene.info.type == 2) {
						for (int i = 0; i < scene.info.numTeams; i++) {
							if (scene.flag[i] != null) {
								scene.flag[i].postStep();
							}
						}
					}
					scene.processCollisions();
				}
			}
		}
		accessRenderState(true, false);
	}

	BitmapFont font = new BitmapFont();

	@Override
	public void render(float delta) {
		while (accessRenderState(false, false)) {
			// the other thread is actively working so we must wait a SHORT
			// while
			// shouldnt we add a sleep?
			break;
		}
		accessRenderState(true, true);
		tick = System.currentTimeMillis();
		if (gameState == 4 && tick > reconnectStamp) {
			gameState = 0;
			connectGame(serverIP);
		}
		if (gameState == 5 && tick > reconnectStamp) {
			gameState = 0;
			connectLobby();
		}

		if (AssetLoader.loadedFirst && screenLoaded) {
			if (AssetLoader.loadedFull) {
				if (!connectingYet) {
					connectingYet = true;
					scene.startMusic();
					connectLobby();
				}
			} else {
				//if (c < 0) {
				//	c++;
				//} else {
					AssetLoader.load(false);
				//}
			}
			scene.update(delta);
			// Fill the entire screen with black, to prevent potential
			// flickering.
			Gdx.gl.glClearColor(0, 0, 0, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			batcher.enableBlending();
			batcher.begin(); // Begin SpriteBatch
			scene.render();
			batcher.end(); // End SpriteBatch
			letterBox();
		} else {
			Gdx.gl.glClearColor(0, 0, 0, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			batcher.enableBlending();
			batcher.begin(); // Begin SpriteBatch
			font = new BitmapFont(true);
			font.setColor(Color.WHITE);
			font.draw(batcher, "Initializing",380,300);
			batcher.end(); // End SpriteBatch
			letterBox();
			AssetLoader.load(true);
		}
		//System.out.println(tick - AssetLoader.started);
		System.gc();
		accessRenderState(true, false);
	}

	public synchronized boolean accessRenderState(boolean changeIt, boolean changeTo) {
		if (changeIt) {
			rendering = changeTo;
		}
		return rendering;
	}

	public void moveCameraBy(float x, float y) {
		cam.position.y += y;
		cam.position.x += x;
		cam.update();
		batcher.setProjectionMatrix(cam.combined);
		shapeRenderer.setProjectionMatrix(cam.combined);
	}

	public void moveCameraTo(float x, float y) {
		// cam.position.lerp(new Vector3(x,y,0.1f), 0.1f);
		cam.position.y = y;
		cam.position.x = x;
		cam.update();
		batcher.setProjectionMatrix(cam.combined);
		shapeRenderer.setProjectionMatrix(cam.combined);
	}

	private void letterBox() {
		int x = Math.round(cam.position.x) - Math.round(Shared.gameWidth / 2 + originX);
		int y = Math.round(cam.position.y) - Math.round(Shared.gameHeight / 2 + originY);
		// ensure our letterbox area is completely black (or filled with
		// whatever letterbox design we choose
		shapeRenderer.begin(ShapeType.Filled);
		shapeRenderer.setColor(0, 0, 0, 1);
		if (originY > 0) {
			shapeRenderer.rect(x, y - 1, viewWidth, originY + 1); // Top bar
			shapeRenderer.rect(x, y + viewHeight - originY, viewWidth, originY + 1); // Bottom
																						// bar
		} else if (originX > 0) {
			shapeRenderer.rect(x - 1, y, originX + 1, viewHeight); // Left bar
			shapeRenderer.rect(x + viewWidth - originX, y, originX, viewHeight + 1); // Right
																						// bar
		}
		shapeRenderer.end();
	}

	void drawRegion(TextureRegion region, float X, float Y, boolean centered, float rotation, float scale) {
		if (region == null) {
			return;
		}
		int width = region.getRegionWidth();
		int height = region.getRegionHeight();
		float eX = 0;
		float eY = 0;
		if (gameState == 3) {
			eX = X + originX;
			eY = Y + originY;
		} else {
			eX = X;
			eY = Y;
		}
		if (centered) {
			eX -= (width / 2);
			eY -= (height / 2);
		}
		// we gotta round the floats
		int dX = Math.round(eX + xO);
		int dY = Math.round(eY + yO);
		if (centered) {
			batcher.draw(region, dX, dY, width / 2, height / 2, width, height, scale, scale, rotation);
		} else {
			batcher.draw(region, dX, dY, 0, 0, width, height, scale, scale, rotation);
		}
	}

	void drawRegionAbout(TextureRegion region, float X, float Y, float aboutX, float aboutY, boolean centered,
			float rotation, float scale) {
		if (region == null) {
			return;
		}
		int width = region.getRegionWidth();
		int height = region.getRegionHeight();
		float eX = X + originX;
		float eY = Y + originY;
		if (centered) {
			eX -= (width / 2);
			eY -= (height / 2);
		}
		// we gotta round the floats
		float orX = aboutX + originX;
		float orY = aboutY + originY;
		int dX = Math.round(eX);
		int dY = Math.round(eY);
		int oX = Math.round(orX);
		int oY = Math.round(orY);
		batcher.draw(region, dX, dY, oX, oY, width, height, scale, scale, rotation);

	}

	void drawFont(int type, float X, float Y, String s, boolean centered, float scale, Color col) {
		if (s.length() < 1) {
			return;
		}
		float curX = X;
		float padding = 0 * scale;
		float spacing = 1.0f * scale;
		float total = 0;
		float oX, oY;
		// get a quick count of width
		if (centered) {
			total = AssetLoader.getStringWidth(s, scale, padding, spacing);
			oX = Math.round(-total / 2);
			oY = Math.round((scale * -16.0f) / 2);
		} else {
			oX = 0;
			oY = 0;
		}
		Color cur = batcher.getColor();
		batcher.setColor(col);
		for (char c : s.toCharArray()) {
			int ascii = (int) c;
			if (AssetLoader.fontWidth[ascii] > 0) {
				drawRegion(AssetLoader.font[type][ascii], Math.round(curX + padding + oX), Math.round(Y + oY), false, 0,
						scale);
				curX += AssetLoader.fontWidth[ascii] * scale + padding * 2 + spacing;
			}
		}
		batcher.setColor(cur);
		// drawRegion(AssetLoader.font[type][a])
		// font[type].draw(batcher, s, X + originX, Y + originY);
	}

	public void drawFrame(float x, float y, float width, float height, boolean useBackground) {
		TextureRegion[] frame = AssetLoader.frame;

		if (useBackground) {
			for (int a = 0; a < height; a += 32) {
				for (int b = 0; b < width; b += 32) {
					drawRegion(frame[8], x + b, y + a, false, 0, 1);
				}
			}
		}

		// left side
		for (int b = 32; b < height - 32; b += 32) {
			drawRegion(frame[4], x, y + b, false, 0, 1);
		}
		// right side
		for (int b = 32; b < height - 32; b += 32) {
			drawRegion(frame[5], x + width - 32, y + b, false, 0, 1);
		}
		// top side
		for (int b = 32; b < width - 32; b += 32) {
			drawRegion(frame[6], x + b, y, false, 0, 1);
		}
		// bottom side
		for (int b = 32; b < width - 32; b += 32) {
			drawRegion(frame[7], x + b, y + height - 32, false, 0, 1);
		}
		// draw top left
		drawRegion(frame[0], x, y, false, 0, 1);
		// top right
		drawRegion(frame[1], x + width - 32, y, false, 0, 1);
		// bottom left
		drawRegion(frame[2], x, y + height - 32, false, 0, 1);
		// bottom right
		drawRegion(frame[3], x + width - 32, y + height - 32, false, 0, 1);

	}

	void drawFont(int type, float X, float Y, String s, boolean centered, float scale) {

		drawFont(type, X, Y, s, centered, scale, Color.WHITE);
	}

	int getRelativeX(int X) {
		return Math.round(((float) X / Gdx.graphics.getWidth()) * viewWidth - originX);
	}

	int getRelativeY(int Y) {
		return Math.round(((float) Y / Gdx.graphics.getHeight()) * viewHeight - originY);
	}

	@Override
	public void resize(int width, int height) {
		setupScreen(Shared.gameWidth, Shared.gameHeight);
	}

	@Override
	public void show() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		// Leave blank
	}

}
