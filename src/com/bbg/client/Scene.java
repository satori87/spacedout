package com.bbg.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.bbg.client.Listener.CollisionData;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Entities;
import com.bbg.shared.MapDef;
import com.bbg.shared.Random;
import com.bbg.shared.Stepper;
import com.bbg.shared.Vector;
import com.bbg.shared.Wall;
import com.bbg.shared.Shared;

import com.bbg.shared.ShipDef;
import com.bbg.shared.Network.ChangeTeam;
import com.bbg.shared.Network.ClientAction;
import com.bbg.shared.Network.ClientSync;
import com.bbg.shared.Network.GameInfo;
import com.bbg.shared.Network.JoinFray;
import com.bbg.shared.Network.JoinSpec;
import com.bbg.shared.Network.LeaveFray;
import com.bbg.shared.Vector.Coord;

public class Scene {

	public GameScreen screen;

	public long tickDuration = 0;
	public List<PolyPoop> explosions = new LinkedList<PolyPoop>();
	public List<PolyPoop> explosionDrops = new LinkedList<PolyPoop>();
	public int curMus = 10;
	public boolean playChatting = false;

	public boolean fr = false;
	public int weapon = 0;
	public boolean miniMap = true;
	public long reloadSoundStamp = 0;
	public int chatLines = 4;
	public long flickerStamp = 0;
	public int flicker = 0;
	public float glowVal = 0.0f;
	public boolean glowingUp = true;
	public List<Wall> walls = new LinkedList<Wall>();
	public int shotsLeft = 0;
	public boolean okHover = false;
	public int curLoad = 0;

	public long respawnStamp = 0;

	public long keyAgainAt = 0;
	//public long keyingStamp = 0;
	public boolean keying = false;
	public int keyKey = 0;
	public long serverTick = 0;
	public long gameStamp = 0; // variable based on screen.gameState in info
	public long clientActionStamp = 0;

	public Queue<CollisionData> collisions = new LinkedList<CollisionData>();

	public World world;
	public Listener listener; // collision listener
	Body box;
	int type = 0;

	float gameWidth, gameHeight; // the size of the viewport

	public long fireStamp = 0;

	public long syncStamp = 0;

	public long lastDrawStart = 0;

	public boolean joined = false;
	public boolean active = false;

	float minX, maxX, minY, maxY;
	public int gameType = 0;
	public int maxGamePlayers = 0;
	public Team[] teams;

	public GameInfo info = new GameInfo();
	public int numBases = 2;
	public Base[] base = new Base[numBases];

	Player[] players;
	Bullet[] bullets;
	Pickup[] pickups;

	public int mapNum = -2;
	MapDef map = Entities.maps[0];
	public Flag[] flag = new Flag[2];
	public int[] items = new int[3];

	public Input input;

	ClientSync cs = new ClientSync();
	// ClientSync lastcs = new ClientSync();

	public String[] chatLog = new String[400];
	public Color[] chatLogColor = new Color[400];

	float sampleR = Random.frnd();
	float sampleG = Random.frnd();
	float sampleB = Random.frnd();
	float sampleRot = 0;

	public long tick = 0;

	public Stepper step250 = new Stepper(250);

	public boolean playHover = false;
	public boolean quitHover = false;
	public boolean colorHover = false;
	public boolean nickHover = false;
	public boolean gameJoinHover = false;
	public boolean team0join = false;
	public boolean team1join = false;

	public boolean spectating = false;
	public boolean specHover = false;
	public int si = -1;
	public String curStatus = "";
	public String curNick = "";
	public String curChatText = "";
	public int curChat = 0;
	
	public Scene(GameScreen screen, float gameWidth, float gameHeight) {
		world = new World(new Vector2(0, 0), true);
		listener = new Listener(this);
		world.setContactListener(listener);
		world.setContinuousPhysics(true);
		World.setVelocityThreshold(World.getVelocityThreshold() * Shared.xml.velocityThresholdFactor);
		this.screen = screen;
		this.gameWidth = gameWidth;
		this.gameHeight = gameHeight;
		input = new Input(screen);
		players = new Player[Shared.maxPlayers];
		bullets = new Bullet[Shared.maxBullets];
		pickups = new Pickup[Shared.maxPickups];
		cs = new ClientSync();
		cs.accel = false;
		cs.decel = false;
		cs.desiredRotation = 0;
		for (int i = 0; i < 400; i++) {
			chatLog[i] = "";
			chatLogColor[i] = Color.WHITE;
		}
	}

	void startMusic() {
		AssetLoader.mus[curMus].setLooping(false);
		AssetLoader.mus[curMus].setVolume(Options.music());
		AssetLoader.mus[curMus].play();
	}

	void checkMusic() {
		if (!screen.connectingYet) {
			return;
		}
		if (!AssetLoader.mus[curMus].isPlaying()) {
			curMus++;
			if (curMus > 10) {
				curMus = 0;
			}
			startMusic();
		}
	}

	public void playSound(int s, float vol, float pitchF) {
		AssetLoader.snd[s].play(vol * Options.sound(), pitchF, 0);
	}

	public void play3D(int s, float drawX, float drawY, float vol, float pitchF) {
		Player me = players[screen.myIndex];
		if (!me.playing && !spectating) {
			return;
		}
		float mX = me.drawX;
		float mY = me.drawY;
		float d = Vector.distance(mX, mY, drawX, drawY);
		if (d >= 800) {
			return;
		}
		float p = 0;
		p = 1 - (d * d) / 500000f;
		if (p < 0) {
			return;
		}
		Vector vv = Vector.byChange(drawX - mX, drawY - mY);
		float cD = Vector.fixDir(vv.direction);
		Vector v = new Vector(cD, 1);
		float vx = v.xChange;
		float vy = v.yChange / 10f;
		float f = 800f / d;
		vx = vv.xChange / f;
		vy = vv.yChange / f;
		if (vx < -1) {
			vx = -1;
		}
		if (vx > 1) {
			vx = 1;
		}
		if (vy < -1) {
			vy = -1;
		}
		if (vy > 1) {
			vy = 1;
		}

		AssetLoader.snd[s].play(p * vol * Options.sound(), (1 - (vy / 10f)) * pitchF, vx);
	}

	void addChat(String s, Color c) {
		float l = AssetLoader.getStringWidth(s, 1, 0, 1);
		int u = 0;
		int k = 0;
		// 790
		boolean f = true;
		if (l >= 790) { // split it up
			for (char ch : s.toCharArray()) {
				if (f) {
					k++;
					u += AssetLoader.fontWidth[ch] + 1;
					if (u >= 790) {
						String s1 = s.substring(0, k - 1);
						s = s.substring(k);
						addChat(s1, c);
						f = false;
					}
				}
			}

		}
		String[] newLog = new String[400];
		Color[] newLogColor = new Color[400];
		for (int i = 0; i < 400 - 1; i++) {
			newLog[i + 1] = chatLog[i];
			newLogColor[i + 1] = chatLogColor[i];
		}
		newLog[0] = s;
		newLogColor[0] = c;
		chatLog = newLog;
		chatLogColor = newLogColor;
	}

	void update(float delta) {
		if (spectating) {
			int i = si;
			int myIndex = screen.myIndex;
			if (i >= 0) {
				if (players[i] != null) {
					players[myIndex].drawX = players[i].drawX;
					players[myIndex].drawY = players[i].drawY;
					players[myIndex].x = players[i].x;
					players[myIndex].y = players[i].y;
					players[myIndex].newX = players[i].newX;
					players[myIndex].newY = players[i].newY;
				}
			}
		}
		checkMusic();
		if (screen.lobby != null) {
			screen.lobby.update();
		}
		tick = screen.tick;
		step250.update(tick);
		switch (screen.gameState) {
		case 0:
			break;
		case 1: //
			if (players[screen.myIndex] != null) {
				players[screen.myIndex].x = 0;
				players[screen.myIndex].y = 0;
				players[screen.myIndex].newX = 0;
				players[screen.myIndex].newY = 0;
				players[screen.myIndex].drawX = 0;
				players[screen.myIndex].drawY = 0;
				players[screen.myIndex].drawDuration = 0;
				players[screen.myIndex].speedX = 0;
				players[screen.myIndex].speedY = 0;
			}
			break;
		case 2:
			break;
		case 3: // playing
			cs = new ClientSync();
			rotateToMouse();

			for (Player p : players) {
				if (p != null) {
					if (p.active) {
						p.update();
					}
				}
			}
			for (Bullet b : bullets) {
				if (b != null) {
					if (b.active) {
						b.update();
						if (b.removeAt > 0 && tick > b.removeAt) {
							b.active = false;
						}
					}
				}
			}
			for (Pickup b : pickups) {
				if (b != null) {
					if (b.active) {
						b.update();
						if (b.removeAt > 0 && tick > b.removeAt) {
							b.active = false;
						}
					}
				}
			}

			for (Wall w : walls) {
				w.update(tick);
			}

			if (info.type == 2) {
				for (int i = 0; i < info.numTeams; i++) {
					if (flag[i] != null) {
						Flag f = flag[i];
						f.update();
					}
				}
			}
			if(screen.menu.state() == 0) {
				checkKeys();
			}
			if (tick > syncStamp) {
				syncStamp = tick + 100;
				if (screen.menu.state() > 0 || playChatting) {
					float dR = cs.desiredRotation;
					cs = new ClientSync();
					cs.desiredRotation = dR;
				}
				screen.net.client.sendUDP(cs);
			}

			break;
		case 4:
			break;
		case 6:
			break;
		case 8:
			break;
		}
		if (screen.menu.state() > 0) {
			screen.menu.update(screen);
		} else if (screen.gameState != 3) {
			checkKeys();
		}
	}

	public void beginCollision(Entity e1, Entity e2, Vector2[] points) {
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
							if (b.owner != p.index) {
								b.trigger();
							}
						} else if (cd.e2 instanceof Pickup) {
							// do nothing
						} else {
							Bullet b = (Bullet) cd.e1;
							b.trigger();
						}
					} else { // safe to assume cd.e2 instance of Bullet == true
						// bullet hit something
						if (cd.e1 instanceof Player) {
							Player p = (Player) cd.e1;
							Bullet b = (Bullet) cd.e2;
							if (b.owner != p.index) {
								b.trigger();
							}
						} else if (cd.e2 instanceof Pickup) {
							// do nothing
						} else {
							Bullet b = (Bullet) cd.e2;
							b.trigger();
						}
					}
				} else if (cd.e1 instanceof Player || cd.e2 instanceof Player) {
					if (cd.e1 instanceof Player && cd.e2 instanceof Player) {
						// two ships collided, do some damage or something
					} else if (cd.e1 instanceof Player) {
						// e1 is a player colliding with something besides a
						// bullet, pickup, or player
					} else { // safe to assume cd.e2 instance of Player == true
						// e2 is a player colliding with something besides a
						// bullet, pickup, or player
					}
				}

			}
		}
	}

	public void render() {
		float b = 0;
		switch (screen.gameState) {
		case 0: // load gfx/connect to lobby
			screen.moveCameraTo(gameWidth / 2, gameHeight / 2);
			drawFrame(0, 0, gameWidth, gameHeight, true);
			if (AssetLoader.loadedFull) {
				screen.drawFont(0, gameWidth / 2, gameHeight / 2, "Connecting", true, 3);
			} else {
				screen.drawFont(0, gameWidth / 2, gameHeight / 2, "Loading", true, 3);
			}
			if (AssetLoader.loadedFirst && screen.connectedLobby) {
				drawButton(quitHover, (gameWidth / 2), 550, 128, 48, true);
				screen.drawFont(0, (gameWidth / 2), 550, "Lobby", true, 1.5f);
			}
			break;
		case 1: // connected, show menu
			screen.moveCameraTo(gameWidth / 2, gameHeight / 2);
			drawFrame(0, 0, gameWidth, gameHeight, true);
			screen.drawFont(0, gameWidth / 2, 200, "Connected to " + screen.serverName, true, 2);
			screen.drawFont(0, gameWidth / 2, 270, "[" + screen.serverIP + "]", true, 2);

			drawButton(playHover, (gameWidth / 2) + 100, 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2) + 100, 550, "Join", true, 1.5f);

			drawButton(quitHover, (gameWidth / 2) - 100, 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2) - 100, 550, "Lobby", true, 1.5f);

			screen.drawFont(0, (gameWidth / 2) - 130, 480, "Nickname", true, 1.6f);
			drawTextArea(true, gameWidth / 2 + 100, 480, 300, true);

			b = AssetLoader.getStringWidth(curNick, 1, 0, 1);
			screen.drawFont(0, (gameWidth / 2) - 20, 480 - 6, curNick, false, 1);
			if (step250.step[2] == 0) {
				screen.drawFont(0, (gameWidth / 2) - 20 + b, 480 - 6, "|", false, 1);
			}
			if (curStatus.length() > 0) {
				screen.drawFont(0, gameWidth / 2, 400, curStatus, true, 1.7f);
			}
			break;
		case 2: // joining
			screen.moveCameraTo(gameWidth / 2, gameHeight / 2);
			drawFrame(0, 0, gameWidth, gameHeight, true);
			screen.drawFont(0, gameWidth / 2, gameHeight / 2, "Joining Game", true, 3);
			if (curStatus.length() > 0) {
				screen.drawFont(0, gameWidth / 2, 400, curStatus, true, 1.7f);
			}
			drawButton(quitHover, (gameWidth / 2), 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2), 550, "Lobby", true, 1.5f);
			break;
		case 3: // playing
			drawGame();
			break;
		case 4: // cant connect to game
			screen.moveCameraTo(gameWidth / 2, gameHeight / 2);
			drawFrame(0, 0, gameWidth, gameHeight, true);
			screen.drawFont(0, gameWidth / 2, gameHeight / 2 - 50, "Disconnected", true, 3);
			screen.drawFont(0, gameWidth / 2, gameHeight / 2 + 50,
					"Retrying in " + ((screen.reconnectStamp - tick) / 1000) + "s", true, 3);
			drawButton(quitHover, (gameWidth / 2), 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2), 550, "Lobby", true, 1.5f);
			break;
		case 5: // cant connect to lobby
			screen.moveCameraTo(gameWidth / 2, gameHeight / 2);
			drawFrame(0, 0, gameWidth, gameHeight, true);
			screen.drawFont(0, gameWidth / 2, gameHeight / 2 - 50, "Disconnected", true, 3);
			screen.drawFont(0, gameWidth / 2, gameHeight / 2 + 50,
					"Retrying in " + ((screen.reconnectStamp - tick) / 1000) + "s", true, 3);
			drawButton(quitHover, (gameWidth / 2), 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2), 550, "Quit", true, 1.5f);
			break;
		case 6: // connected to lobby
			screen.moveCameraTo(gameWidth / 2, gameHeight / 2);
			drawFrame(0, 0, gameWidth, gameHeight, true);
			screen.drawFont(0, gameWidth / 2, 200, "Connected to Lobby", true, 2);
			screen.drawFont(0, gameWidth / 2, 270, "[" + screen.lobbyIP + "]", true, 2);

			drawButton(playHover, (gameWidth / 2) + 100, 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2) + 100, 550, "Join", true, 1.5f);

			drawButton(quitHover, (gameWidth / 2) - 100, 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2) - 100, 550, "Quit", true, 1.5f);

			screen.drawFont(0, (gameWidth / 2) - 130, 480, "Nickname", true, 1.6f);
			drawTextArea(true, gameWidth / 2 + 100, 480, 300, true);

			b = AssetLoader.getStringWidth(curNick, 1, 0, 1);
			screen.drawFont(0, (gameWidth / 2) - 20, 480 - 6, curNick, false, 1);
			if (step250.step[2] == 0) {
				screen.drawFont(0, (gameWidth / 2) - 20 + b, 480 - 6, "|", false, 1);
			}
			if (curStatus.length() > 0) {
				screen.drawFont(0, gameWidth / 2, 400, curStatus, true, 1.7f);
			}
			if (screen.firstMessage) {
				drawFrame(0, 0, 800, 600, true);
				drawButton(okHover, 400, 550, 200, 48, true);
				screen.drawFont(0, 400, 550, "Ok", true, 1);
				screen.drawFont(0, 400, 50, "Beginner's Note", true, 2);
				screen.drawFont(0, 400, 150, "Be sure to check the menu by hitting Escape", true, 1);
				screen.drawFont(0, 400, 180, "In the menu, check out Options like controls as well as setting loadouts",
						true, 1);
				screen.drawFont(0, 400, 210, "Do not proceed until you have setup a full loadout", true, 1);

			}
			break;
		case 7: // joining lobby
			screen.moveCameraTo(gameWidth / 2, gameHeight / 2);
			drawFrame(0, 0, gameWidth, gameHeight, true);
			screen.drawFont(0, gameWidth / 2, gameHeight / 2, "Joining Lobby", true, 3);
			break;
		case 8: // in lobby
			screen.moveCameraTo(gameWidth / 2, gameHeight / 2);
			screen.lobby.render();
			drawButton(quitHover, (gameWidth / 2) - 200, 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2) - 200, 550, "Quit", true, 1.5f);
			drawButton(playHover, (gameWidth / 2), 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2), 550, "Menu", true, 1.5f);
			drawButton(nickHover, (gameWidth / 2) + 200, 550, 128, 48, true);
			screen.drawFont(0, (gameWidth / 2) + 200, 550, "Nick", true, 1.5f);

			break;
		}
		if (screen.menu.state() > 0) {
			screen.menu.render(screen);
		}
	}

	void resetWorld() {
		world.dispose();
		world = new World(new Vector2(0, 0), true);
		World.setVelocityThreshold(World.getVelocityThreshold() * Shared.xml.velocityThresholdFactor);
		world.setContactListener(listener);
		world.setContinuousPhysics(true);
		// createBoundary();
		for (int i = 0; i < Shared.maxPlayers; i++) {
			if (players[i] != null) {
				players[i].body = null;
			}
		}
		for (int i = 0; i < Shared.maxBullets; i++) {
			if (bullets[i] != null) {
				bullets[i].body = null;
				if (bullets[i].dontClear) {
					bullets[i].createBody(CollisionFlags.CAT_SOLID, CollisionFlags.MASKSOLID, true, true, true);
					bullets[i].body.setBullet(true);
				}
			}
		}
		for (int i = 0; i < Shared.maxPickups; i++) {
			if (pickups[i] != null) {
				pickups[i].body = null;
			}
		}
		if (info.type == 2) {
			for (int i = 0; i < info.numTeams; i++) {
				if (flag[i] != null) {
					flag[i].body = null;
				}
			}
		}

		addWalls();
	}

	void addWalls() {
		for (Wall w : walls) {
			switch (w.shape) {
			case 0:
				addWall(w, w.x, w.y, w.d);
				break;
			case 1:
				addWall(w, w.x, w.y, w.d);
				break;

			}
		}
	}

	void wallRotate(Wall w, Wall a, float deg) {
		float cX = w.x;
		float cY = w.y;
		Coord c = Vector.rot(a.x, a.y, cX, cY, deg);
		a.x = c.x;
		a.y = c.y;
	}

	void addWall(Wall w, float x, float y, float d) {
		if (w.entity == null) {
			w.entity = new WallEntity(w);
		}
		addWallBody(w, x, y, d, w.thickness);
		w.body.setTransform(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale, (float) Math.toRadians(d));
		w.contacts.clear();
	}

	public void addWallBody(Wall wall, float x, float y, float d, int thickness) {
		BodyDef wallDef = new BodyDef();
		wallDef.type = BodyType.StaticBody;
		wallDef.position.set(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale);
		// wallDef.position.setAngle((float)Math.toRadians(d));
		wall.body = world.createBody(wallDef);
		wall.body.setUserData(wall.entity);
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = Shared.xml.barrierDensity;
		fixtureDef.friction = Shared.xml.barrierFriction;
		fixtureDef.restitution = Shared.xml.barrierRestitution;
		fixtureDef.filter.categoryBits = CollisionFlags.CAT_SOLID;
		fixtureDef.filter.maskBits = CollisionFlags.MASKSOLID;
		PolygonShape p = new PolygonShape();
		float w = Wall.getWallWidth(thickness);
		float h = Wall.getWallHeight(thickness);
		p.setAsBox((w / Shared.xml.physicsScale) / 2, (h / Shared.xml.physicsScale) / 2, new Vector2(0, 0), 0);
		fixtureDef.shape = p;
		wall.body.createFixture(fixtureDef);
		p.dispose();
	}

	public void drawTab(float alpha, boolean showJoin) {
		Player me = players[screen.myIndex];
		float drawX = me.drawX;
		float drawY = me.drawY;
		float tabWidth = 736;
		float tabHeight = 416 + 32;
		float tabX = drawX - 368;
		float tabY = drawY - 200;
		Color tabColor = new Color(1, 1, 1, alpha);
		Queue<Player> plist = new LinkedList<Player>();
		Queue<Player> plist2 = new LinkedList<Player>();

		if (info.numTeams == 0) {
			plist = getPlayersSorted(-1);
		} else {
			plist = getPlayersSorted(0);
			plist2 = getPlayersSorted(1);
		}

		screen.batcher.setColor(tabColor);
		drawFrame(tabX, tabY, tabWidth, tabHeight, true);
		if (showJoin) {
			if (info.type == 0) {
				drawButton(gameJoinHover, tabX + tabWidth / 2, tabY + 40, 96, 48, true);
				screen.drawFont(0, tabX + tabWidth / 2, tabY + 40, "Join", true, 1.0f);
			} else {
				drawButton(team0join, tabX + (tabWidth / 2) - 50, tabY + 40, 64, 32, true);
				screen.drawFont(0, tabX + (tabWidth / 2) - 50, tabY + 40, "Join", true, 1.0f);
				drawButton(team1join, tabX + (tabWidth / 2) + 50, tabY + 40, 64, 32, true);
				screen.drawFont(0, tabX + (tabWidth / 2) + 50, tabY + 40, "Join", true, 1.0f);
			}
			drawButton(specHover, tabX + (tabWidth / 2), tabY + 470, 96, 32, true);
			screen.drawFont(0, tabX + (tabWidth / 2), tabY + 470, "Spectate", true, 1);
		}

		tabY += 32;
		int d = 0;

		if (info.numTeams > 0) {
			Team t1 = teams[0];
			Team t2 = teams[1];
			screen.drawFont(0, tabX + 160, tabY - 5, t1.name, true, 2, new Color(0.75f, 0.75f, 0.75f, alpha));
			screen.drawFont(0, tabX + 200 + 366, tabY - 5, t2.name, true, 2, new Color(0.75f, 0.75f, 0.75f, alpha));
			screen.drawFont(0, tabX + 160, tabY + 25, "Score: " + t1.score, true, 1,
					new Color(0.75f, 0.75f, 0.75f, alpha));
			screen.drawFont(0, tabX + 200 + 366, tabY + 25, "Score: " + t2.score, true, 1,
					new Color(0.75f, 0.75f, 0.75f, alpha));

		}

		screen.drawFont(0, tabX + 8 + 2, tabY + 40, "Name", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		screen.drawFont(0, tabX + 178, tabY + 40, "Ping", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		screen.drawFont(0, tabX + 220, tabY + 40, "Kills", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		screen.drawFont(0, tabX + 222 + 38, tabY + 40, "Deaths", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		screen.drawFont(0, tabX + 248 + 70, tabY + 40, "Score", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		tabX += 366;
		screen.drawFont(0, tabX + 8 + 2, tabY + 40, "Name", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		screen.drawFont(0, tabX + 178, tabY + 40, "Ping", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		screen.drawFont(0, tabX + 220, tabY + 40, "Kills", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		screen.drawFont(0, tabX + 222 + 38, tabY + 40, "Deaths", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		screen.drawFont(0, tabX + 248 + 70, tabY + 40, "Score", false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
		tabX -= 366;
		for (int i = 0; i < 32; i++) {
			d = 1 - d;
			if (i < 16) {
				for (int a = 0; a < 362; a += 2) {
					screen.drawRegion(AssetLoader.frame[9 + d], tabX + a + 5, tabY + 59 + (i * 22), false, 0, 1);
				}
				for (int a = 362; a < tabWidth - 10; a += 2) {
					screen.drawRegion(AssetLoader.frame[9 + (1 - d)], tabX + a + 5, tabY + 59 + (i * 22), false, 0, 1);
				}
			}
			if (info.numTeams == 0) {
				drawPlayerStats(plist, tabX, tabY, i, alpha);
			} else {
				if (i < 16) {
					drawPlayerStats(plist, tabX, tabY, i, alpha);
				} else {
					drawPlayerStats(plist2, tabX, tabY, i, alpha);
				}
			}
		}
		screen.batcher.setColor(Color.WHITE);
		if (screen.frayError.length() > 0) {
			screen.drawFont(0, drawX, drawY - 184, screen.frayError, true, 1);
		}
	}

	public String getTimeLimitString() {
		String timeLimStr = Integer.toString(info.timeLimit) + " minutes";
		if (info.timeLimit == 0) {
			timeLimStr = "None";
		}
		return timeLimStr;
	}

	public String getScoreLimitString() {
		if (info.scoreLimit == 0) {
			return "None";
		}
		String scoreLimStr = Integer.toString(info.scoreLimit);
		switch (info.type) {
		case 0:
			scoreLimStr = scoreLimStr + " global kills";
			break;
		case 1:
			scoreLimStr = scoreLimStr + " team kills";
			break;
		case 2:
			scoreLimStr = scoreLimStr + " flag caps";
			break;
		}
		return scoreLimStr;
	}

	public String getGameTimeString(long time) {
		long t = (time / 1000);
		int h = 0, m = 0, s = 0;
		h = (int) (t / 3600);
		t -= h * 3600;
		m = (int) (t / 60);
		t -= m * 60;
		s = (int) t;
		String hs, ms, ss;
		if (h < 10) {
			hs = "0" + h;
		} else {
			hs = Integer.toString(h);
		}
		if (m < 10) {
			ms = "0" + m;
		} else {
			ms = Integer.toString(m);
		}
		if (s < 10) {
			ss = "0" + s;
		} else {
			ss = Integer.toString(s);
		}
		return hs + ":" + ms + ":" + ss;
	}

	public String getTimePlayedString() {
		return getGameTimeString(info.gameTime + (tick - gameStamp));
	}

	public String getTimeLeftString() {
		return getGameTimeString(info.howLongBeforeNext - (tick - gameStamp) + 1000);
	}

	public String getGameTypeString() {
		switch (info.type) {
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

	public void drawOverlay() {
		Player me = players[screen.myIndex];
		float drawX = me.drawX;
		float drawY = me.drawY;
		float x = drawX - gameWidth / 2;
		float y = drawY - gameHeight / 2;
		if (checkBind(8)) {
			drawTab(0.5f, false);
		}
		// screen.drawFont(0, x, y, getGameTypeString(), false, 1);
		screen.drawFont(0, x + 270, y, "Time: " + getTimePlayedString(), false, 1);
		screen.drawFont(0, x + 420, y, "Left: " + getTimeLeftString(), false, 1);

		screen.drawFont(0, x + 2, y + 2, "Repairs: " + items[0], false, 1);
		screen.drawFont(0, x + 2, y + 18, "Shields: " + items[1], false, 1);
		screen.drawFont(0, x + 2, y + 34, "Turbos: " + items[2], false, 1);

		Player p = getMe();
		if (!spectating && p.dead) {
			if (step250.step[2] == 0) {
				screen.drawFont(0, p.drawX, p.drawY - 100, "RENDERED", true, 3, Color.RED);

			}
			if (tick - respawnStamp >= 0) {
				screen.drawFont(0, p.drawX, p.drawY + 60, "Fire to respawn", true, 2);
			} else {
				screen.drawFont(0, p.drawX, p.drawY + 60,
						"You can respawn in " + (int) (((respawnStamp - tick) / 1000) + 1), true, 2);
			}
		}
		// screen.drawFont(0, x + 700, y + 2, Math.round(drawX) + "," +
		// Math.round(drawY), false, 1);

		// int mouseX = (input.mouseX - 400) + (int)drawX;
		// int mouseY = (input.mouseY - 300) + (int)drawY;
		// screen.drawFont(0, x + 700, y + 24, mouseX + "," + mouseY, false, 1);

	}

	public Player getMe() {
		return players[screen.myIndex];
	}

	public void drawGame() {
		Player me = players[screen.myIndex];
		if (me.playing) {
			// return;
		}
		if (si >= 0 && players[si] != null) {
			int i = si;
			int myIndex = screen.myIndex;
			players[myIndex].drawX = players[i].drawX;
			players[myIndex].drawY = players[i].drawY;
			players[myIndex].x = players[i].x;
			players[myIndex].y = players[i].y;
			players[myIndex].newX = players[i].newX;
			players[myIndex].newY = players[i].newY;
			screen.moveCameraTo((float) Math.round(players[si].drawX) + screen.originX,
					(float) Math.round(players[si].drawY) + screen.originY);
		} else {
			screen.moveCameraTo((float) Math.round(players[screen.myIndex].drawX) + screen.originX,
					(float) Math.round(players[screen.myIndex].drawY) + screen.originY);
		}
		drawBackground();
		float drawX = me.drawX;
		float drawY = me.drawY;
		float x = drawX;
		float y = drawY - gameHeight / 2;

		switch (info.gameState) {
		case 1:
			screen.drawFont(0, drawX, drawY - gameHeight / 2 + 30, "Game starting in", true, 2);
			screen.drawFont(0, drawX, drawY - gameHeight / 2 + 65,
					Math.round((info.howLongBeforeNext - (tick - gameStamp)) / 1000) + "s", true, 2);
			drawTab(1, false);
			break;
		case 2:
			if (me.playing || spectating) {
				drawField();
				drawMiniMap();
				drawOverlay();
			} else {
				String elapsed = getTimePlayedString();
				String timeLeft = getTimeLeftString();
				screen.drawFont(0, x, y + 30, "Game in progress ", true, 2);
				screen.drawFont(0, x, y + 60, "Time Played: [" + elapsed + "]", true, 1.5f);
				screen.drawFont(0, x, y + 90, "Time Left: [" + timeLeft + "]", true, 1.5f);
				drawTab(1, true);
				screen.drawFont(0, x - 388, y + 10, "Game Type: " + getGameTypeString(), false, 1);
				screen.drawFont(0, x - 388, y + 30, "Time Limit: " + getTimeLimitString(), false, 1);
				screen.drawFont(0, x - 388, y + 50, "Win Condition: " + getScoreLimitString(), false, 1);
			}
			break;
		case 3:
			screen.drawFont(0, drawX, drawY - gameHeight / 2 + 30, "Next game in", true, 2);
			screen.drawFont(0, drawX, drawY - gameHeight / 2 + 65,
					Math.round((info.howLongBeforeNext - (tick - gameStamp)) / 1000) + "s", true, 2);
			drawTab(1, false);
			break;
		}
		drawChat(chatLines);
	}

	public void drawChat(int numLines) {
		Player me = players[screen.myIndex];
		float drawX = me.drawX;
		float drawY = me.drawY;
		screen.drawFont(0, drawX - (gameWidth / 2) + 4, drawY + (gameHeight / 2) - 20, ":" + curChatText, false, 1);
		for (int i = 0; i < numLines; i++) {
			screen.drawFont(0, drawX - (gameWidth / 2) + 4, drawY + (gameHeight / 2) - 40 - i * 20,
					chatLog[i + curChat], false, 1, chatLogColor[i + curChat]);
		}
		if (playChatting && (step250.step[2] == 0) && curChatText.length() < 1) {
			screen.drawFont(0, drawX - (gameWidth / 2) + 8, drawY + (gameHeight / 2) - 20, "|" + curChatText, false, 1);
		}
	}

	int getHighestChat() {
		for (int i = 1; i < 400; i++) {
			if (chatLog[i].length() < 1) {
				return i - 1;
			}
		}
		return 400;
	}

	void makePoop(int num, float drawX, float drawY, float s, Color c, boolean random) {
		try {
			Color col = new Color(c);
			for (int i = 0; i < num; i++) {
				if (random) {
					col = Entities.randomBrightColor();
				}
				col.r = col.r - .2f + (float) (Math.random() * .4f);
				col.g = col.g - .2f + (float) (Math.random() * .4f);
				col.b = col.b - .2f + (float) (Math.random() * .4f);
				if (col.r > 1) {
					col.r = 1;
				}
				if (col.g > 1) {
					col.g = 1;
				}
				if (col.b > 1) {
					col.b = 1;
				}
				if (col.r < 0) {
					col.r = 0;
				}
				if (col.g < 0) {
					col.g = 0;
				}
				if (col.b < 0) {
					col.b = 0;
				}
				PolyPoop p = new PolyPoop(this, drawX, drawY, 1200, s / 5f, s / 10f, col);
				explosions.add(p);
			}
		} catch (Throwable t) {
			System.out.println(t);
		}
	}

	public void drawPlayerStats(Queue<Player> plist, float tabX, float tabY, int i, float alpha) {
		if (!plist.isEmpty()) {
			Player n = plist.remove();
			if (n.ping > 9999) {
				n.ping = 9999;
			}
			if (i >= 16) {
				tabX += 366;
				tabY -= (16 * 22);
			}
			String pingStr = Integer.toString(n.ping);
			screen.drawFont(0, tabX + 8, tabY + 62 + (i * 22), n.name, false, 1, new Color(0.75f, 0.75f, 0.75f, alpha));
			tabY += 8;
			screen.drawFont(0, tabX + 196, tabY + 62 + (i * 22), pingStr, true, 1,
					new Color(0.75f, 0.75f, 0.75f, alpha));
			screen.drawFont(0, tabX + 239, tabY + 62 + (i * 22), Integer.toString(n.kills), true, 1,
					new Color(0.75f, 0.75f, 0.75f, alpha));
			screen.drawFont(0, tabX + 250 + 35, tabY + 62 + (i * 22), Integer.toString(n.deaths), true, 1,
					new Color(0.75f, 0.75f, 0.75f, alpha));
			screen.drawFont(0, tabX + 248 + 93, tabY + 62 + (i * 22), Integer.toString(n.score), true, 1,
					new Color(0.75f, 0.75f, 0.75f, alpha));
			tabY -= 8;
		}
	}

	public void joinFray(int team) {
		screen.frayError = "";
		JoinFray j = new JoinFray();
		j.load = Options.load[curLoad];
		j.team = team;
		if (tick > clientActionStamp) {
			screen.net.client.sendTCP(j);
			clientActionStamp = tick + 200;
		}
	}

	public Queue<Player> getPlayersSorted(int team) {
		Queue<Player> listPlayers = new LinkedList<Player>();
		Queue<Player> sortedList = new LinkedList<Player>();
		for (int i = 0; i < Shared.maxPlayers; i++) {
			if (players[i] != null) {
				if (players[i].team == team) {
					listPlayers.add(players[i]);
				}
			}
		}

		int n = listPlayers.size();
		// now lets sort by score
		for (int i = 0; i < n; i++) {
			int s = -100000;
			Player highest = listPlayers.peek();
			for (Player p : listPlayers) {
				if (p.score > s) {
					highest = p;
					s = p.score;
				}
			}
			if (highest != null) {
				listPlayers.remove(highest);
				sortedList.add(highest);
			}
		}
		return sortedList;
	}

	public void drawBackground() {

		float x = Math.round(players[screen.myIndex].drawX);
		float y = Math.round(players[screen.myIndex].drawY);
		float relX = x + Math.abs(minX);
		float relMaxX = Math.abs(maxX) + Math.abs(minX);
		float relY = y + Math.abs(minY);
		float relMaxY = Math.abs(maxY) + Math.abs(minY);
		float percentX = relX / relMaxX;
		float percentY = relY / relMaxY;
		int w = Math.round(screen.viewWidth);
		int h = Math.round(screen.viewHeight);
		float dX = Math.round(x - ((float) w / 2f) + (float) screen.originX);
		float dY = Math.round(y - ((float) h / 2f) + (float) screen.originY);
		int srcX = Math.round(((float) w / 2f) * percentX * (2048 / (float) w));
		int srcY = Math.round(((float) h / 2f) * percentY * (1536 / (float) h));
		screen.batcher.draw(AssetLoader.bgTex[0], dX, dY, w, h, srcX, srcY, w, h, false, true);
		drawGrid();
	}

	void drawGrid() {
		int w = Math.round(screen.viewWidth);
		int h = Math.round(screen.viewHeight);
		float dX = Math.round(Math.round(players[screen.myIndex].drawX) - ((float) w / 2f) + (float) screen.originX);
		float dY = Math.round(Math.round(players[screen.myIndex].drawY) - ((float) h / 2f) + (float) screen.originY);
		int sX = 0;
		int sY = 0;
		if (tick > flickerStamp) {
			flickerStamp = tick + 20;
			flicker += 1;
			if (flicker >= 8) {
				flicker = 0;
			}
			if (glowingUp) {
				glowVal += 0.04f;
				if (glowVal > .6f) {
					glowingUp = false;
				}
			} else {
				glowVal -= 0.04f;
				if (glowVal < .2f) {

					glowingUp = true;
				}
			}
		}
		int r = 52;
		int g = 174;
		int b = 237;
		screen.batcher.setColor(Entities.getColor(r, g, b, 1));
		for (int x = -88; x < screen.viewWidth + 88; x += 40) {
			for (int y = -88; y < screen.viewHeight + 88; y += 40) {
				sX = (int) players[screen.myIndex].drawX % 40;
				sY = (int) players[screen.myIndex].drawY % 40;
				screen.batcher.draw(AssetLoader.grid, dX + x - sX, dY + y - sY, 62, 62, 0, 0, 62, 62, false, true);
			}
		}
		screen.batcher.setColor(Entities.getColor(r, g, b, glowVal));
		for (int x = -88; x < screen.viewWidth + 88; x += 40) {
			for (int y = -88; y < screen.viewHeight + 88; y += 40) {
				sX = (int) players[screen.myIndex].drawX % 40;
				sY = (int) players[screen.myIndex].drawY % 40;
				screen.batcher.draw(AssetLoader.grid, dX + x - sX, dY + y - sY, 62, 62, 62, 0, 62, 62, false, true);
			}
		}
		screen.batcher.setColor(Color.WHITE);
	}

	public void drawMiniMap() {
		if (!miniMap) {
			return;
		}
		
		float x = players[screen.myIndex].drawX;
		float y = players[screen.myIndex].drawY;
		float sX = x + 300;
		float sY = y - 200;
		screen.batcher.flush();
		screen.batcher.end();
		screen.batcher.begin();
		Rectangle scissors = new Rectangle();
		Rectangle clipBounds = new Rectangle(sX - 100 + screen.originX, sY - 100 + screen.originY, 200, 200);
		ScissorStack.calculateScissors(screen.cam, screen.batcher.getTransformMatrix(), clipBounds, scissors);
		ScissorStack.pushScissors(scissors);
		// spriteBatch.draw(...);

		for (Wall e : walls) {
			if (Math.abs(x - e.x) < 1100) {
				if (Math.abs(y - e.y) < 1100) {
					WallEntity we = (WallEntity) e.entity;
					if (we != null) {
						screen.batcher.setColor(we.wall.col);
						TextureRegion tex = AssetLoader.getSprite("line" + Integer.toString(we.wall.thickness) + "g");
						float dX = sX + ((we.wall.x - x) / 10f);
						float dY = sY + ((we.wall.y - y) / 10f);
						float r = we.wall.d + 90;
						screen.drawRegion(tex, dX, dY, true, r, .1f);
						tex = AssetLoader.getSprite("line" + Integer.toString(we.wall.thickness));
						screen.drawRegion(tex, dX, dY, true, r, .1f);
					}
				}
			}
		}

		if (info.type == 2) {
			for (Base e : base) {
				if (e != null) {
					if (Math.abs(x - e.x) < 1100) {
						if (Math.abs(y - e.y) < 1100) {
							float dX = sX + ((e.x - x) / 10f);
							float dY = sY + ((e.y - y) / 10f);
							float r = (float) Math.toDegrees(e.curDirection);
							screen.batcher.setColor(teams[e.team].col());
							screen.drawRegion(AssetLoader.getSprite("base"), dX, dY, true, r, 0.07f);
							Color dd = new Color(teams[e.team].col());
							dd.a = 0.5f;
							screen.batcher.setColor(dd);
							screen.drawRegion(AssetLoader.getSprite("baseg"), dX, dY, true, r, 0.07f);
						}
					}
				}
			}
		}
		if (getMe().dead && tick > respawnStamp) {
			
		} else {
			TextureRegion tex = AssetLoader.getSprite("shape1");
			for (int i = 0; i < Shared.maxPlayers; i++) {
				Player e = players[i];
				if (e != null) {
					if (e.active) {
						if (Math.abs(x - e.x) < 1100) {
							if (Math.abs(y - e.y) < 1100) {
								screen.batcher.setColor(e.getColor(1f));
								float dX = sX + ((e.drawX - x) / 10f);
								float dY = sY + ((e.drawY - y) / 10f);
								float r = (float) Math.toDegrees(e.drawDirection);
								float scale = ShipDef.getShipScale(e.armor) / 5f;
								if (e.dead) {
									if (step250.step[2] == 0) {
										screen.drawFont(0, dX, dY, "x", true, 0.5f, e.getColor(1f));
									}
								} else {
									screen.drawRegion(tex, dX, dY, true, r, scale);
								}
							}
						}
					}
				}
			}
	
			if (info.type == 2) {
				for (int i = 0; i < info.numTeams; i++) {
					if (flag[i] != null) {
						Flag e = flag[i];
						if (!e.held) {
							if (Math.abs(x - e.x) < 1100) {
								if (Math.abs(y - e.y) < 1100) {
									screen.batcher.setColor(screen.scene.teams[e.team].col());
									TextureRegion pole = AssetLoader.getSprite("banner");
									float dX = sX + ((e.drawX - x) / 10f);
									float dY = sY + ((e.drawY - y) / 10f);
									screen.drawRegion(pole, dX, dY, true, 0, .06f + getMe().glowVal / 20f);
									pole = AssetLoader.getSprite("bannerg");
									screen.drawRegion(pole, dX, dY, true, 0, .06f + getMe().glowVal / 20f);
								}
							}
						} else {
							if (players[e.holder] != null) {
								screen.batcher.setColor(screen.scene.teams[e.team].col());
								TextureRegion pole = AssetLoader.getSprite("banner");
								float dX = sX + ((players[e.holder].drawX - x) / 10f);
								float dY = sY + ((players[e.holder].drawY - y) / 10f);
								screen.drawRegion(pole, dX, dY, true, 0, 0.1f);
								pole = AssetLoader.getSprite("bannerg");
								screen.drawRegion(pole, dX, dY, true, 0, 0.1f);
							}
						}
					}
				}
			}
		}
		screen.batcher.flush();
		ScissorStack.popScissors();
		screen.batcher.setColor(new Color(1, 1, 1, 0.5f));
		screen.drawFrame(sX - 100, sY - 100, 200, 200, false);
		screen.batcher.setColor(Color.WHITE);
	}

	public void drawField() {
		try {
			float x = players[screen.myIndex].drawX;
			float y = players[screen.myIndex].drawY;
			if (info.type == 2) {
				for (Base e : base) {
					if (e != null) {
						if (Math.abs(x - e.x) < (gameWidth / 2) + 200 * 2) {
							if (Math.abs(y - e.y) < (gameHeight / 2) + 200 * 2) {
								e.render(screen);
							}
						}
					}
				}
				for (int i = 0; i < info.numTeams; i++) {
					if (flag[i] != null) {
						Flag e = flag[i];
						if (Math.abs(x - e.x) < (gameWidth / 2) + 200) {
							if (Math.abs(y - e.y) < (gameHeight / 2) + 200) {
								e.renderBase(screen);
							}
						}
					}
				}
			}
			for (Wall e : walls) {
				if (Math.abs(x - e.x) < (gameWidth / 2) + 200) {
					if (Math.abs(y - e.y) < (gameHeight / 2) + 200) {
						WallEntity we = (WallEntity) e.entity;
						if (we != null) {
							we.render(screen);
						}
					}
				}
			}
			for (int i = 0; i < Shared.maxBullets; i++) {
				Bullet e = bullets[i];
				if (e != null) {
					if (e.active) {
						if (Math.abs(x - e.x) < (gameWidth / 2) + 200) {
							if (Math.abs(y - e.y) < (gameHeight / 2) + 200) {
								e.render(screen);
							}
						}
					}
				}
			}
			for (int i = 0; i < Shared.maxPickups; i++) {
				Pickup e = pickups[i];
				if (e != null) {
					if (e.active) {
						if (Math.abs(x - e.x) < (gameWidth / 2) + 200) {
							if (Math.abs(y - e.y) < (gameHeight / 2) + 200) {
								e.render(screen);
							}
						}
					}
				}
			}
			for (int i = 0; i < Shared.maxPlayers; i++) {
				Player e = players[i];
				if (e != null) {
					if (e.active) {
						if (Math.abs(x - e.x) < (gameWidth / 2) + 200) {
							if (Math.abs(y - e.y) < (gameHeight / 2) + 200) {
								e.render(screen);
							} else {
								e.active = false;
							}
						} else {
							e.active = false;
						}
					}
				}
			}
			explosionDrops.clear();
			for (PolyPoop p : explosions) {
				p.render(screen, 0, 0);
				if (p.remove) {
					explosionDrops.add(p);
				}
			}
	
			for (PolyPoop p : explosionDrops) {
				explosions.remove(p);
			}
			if (info.type == 2) {
				for (int i = 0; i < info.numTeams; i++) {
					if (flag[i] != null) {
						Flag e = flag[i];
						if (Math.abs(x - e.x) < (gameWidth / 2) + 200) {
							if (Math.abs(y - e.y) < (gameHeight / 2) + 200) {
								e.render(screen);
							}
						}
					}
				}
				for (int i = 0; i < info.numTeams; i++) {
					if (flag[i] != null) {
						Flag f = flag[i];
						if (f.held) {
							if (f.holder >= 0) {
								Player e = players[f.holder];
								if (e != null && e.active) {
									if (Math.abs(x - e.x) < (gameWidth / 2) + 200) {
										if (Math.abs(y - e.y) < (gameHeight / 2) + 200) {
											Flag.renderBanner(screen, e.drawX, e.drawY, 1 - e.team);
										}
									}
								}
							}
						}
					}
				}
			}
			for (int i = 0; i < Shared.maxPlayers; i++) {
				Player e = players[i];
				if (e != null) {
					if (e.active) {
						if (Math.abs(x - e.x) < (gameWidth / 2) + 200) {
							if (Math.abs(y - e.y) < (gameHeight / 2) + 200) {
								e.renderText(screen);
							} else {
								e.active = false;
							}
						} else {
							e.active = false;
						}
					}
				}
			}
		} catch (Throwable t) {
			System.out.println(t);
		}

	}

	public boolean inBox(float x, float y, float centerX, float centerY, float width, float height) {
		float topY = centerY - (height / 2);
		float bottomY = centerY + (height / 2);
		float leftX = centerX - (width / 2);
		float rightX = centerX + (width / 2);
		if (x > leftX && x < rightX && y > topY && y < bottomY) {
			if (input.mouseDown && input.wasMouseJustClicked && !input.clickHandled) {
				input.clickHandled = true;
				playSound(23, 1, 1);
			}
			return true;
		}
		return false;
	}

	public void keying(int i) {
		if (keying) {
			keyAgainAt = tick + 50;
		} else {
			keyAgainAt = tick + 500;
		}
		keying = true;
		keyKey = i;
	}

	public boolean shifting() {
		return input.keyDown[59] || input.keyDown[60];
	}

	public char getNumPadKey(int i) {
		switch (i) {
		case 145:
			return '!';
		case 146:
			return '@';
		case 147:
			return '#';
		case 148:
			return '$';
		case 149:
			return '%';
		case 150:
			return '^';
		case 151:
			return '&';
		case 152:
			return '*';
		case 153:
			return '(';
		case 144:
			return ')';
		default:
			return '!';
		}
	}

	public char getShiftedNum(int i) {
		switch (i) {
		case 8:
			return '!';
		case 9:
			return '@';
		case 10:
			return '#';
		case 11:
			return '$';
		case 12:
			return '%';
		case 13:
			return '^';
		case 14:
			return '&';
		case 15:
			return '*';
		case 16:
			return '(';
		case 7:
			return ')';
		default:
			return '!';
		}
	}

	public char getOtherKey(int i) {
		switch (i) {
		default:
			Character ch = Character.MIN_VALUE;
			return ch;
		case 17:
			return '*';
		case 112:
			return '.';

		case 55:
			if (shifting()) {
				return '<';
			} else {
				return ',';
			}
		case 56:
			if (shifting()) {
				return '>';
			} else {
				return '.';
			}
		case 71:
			if (shifting()) {
				return '{';
			} else {
				return '[';
			}
		case 72:
			if (shifting()) {
				return '}';
			} else {
				return ']';
			}
		case 73:
			if (shifting()) {
				return '|';
			} else {
				return '\\';
			}
		case 74:
			if (shifting()) {
				return ':';
			} else {
				return ';';
			}
		case 75:
			if (shifting()) {
				return '"';
			} else {
				return '\'';
			}
		case 68:
			if (shifting()) {
				return '~';
			} else {
				return '`';
			}
		case 69:
			if (shifting()) {
				return '_';
			} else {
				return '-';
			}
		case 70:
			if (shifting()) {
				return '+';
			} else {
				return '=';
			}
		case 76:
			if (shifting()) {
				return '?';
			} else {
				return '/';
			}
		case 81:
			return '+';
		}
	}

	public void processKeyPress(int max) {
		String s = "";
		int gs = screen.gameState;
		switch (gs) {
		case 1:
			s = curNick;
			break;
		case 3:
			s = curChatText;
			break;
		case 6:
			s = curNick;
			break;
		case 8:
			s = screen.lobby.curChat;
			break;
		}
		for (Integer ii : input.keyPress) {
			// System.out.println(ii);
			int i = (int) ii;
			if (i != keyKey) {
				keying = false;
			}
			if (i >= 29 && i <= 54) {
				char c = (char) (i + 36);
				c = Character.toLowerCase(c);
				if (shifting()) {
					c = Character.toUpperCase(c);
				}
				keying(i);
				s += c;
			} else if (i >= 7 && i <= 16) {
				char c = (char) (i + 41);
				if (gs == 3 || gs == 8) {
					if (shifting()) {
						s += getShiftedNum(i);
					} else {
						s += c;
					}
				} else {
					s += c;
				}
				keying(i);
			} else if (i == 62) {
				if (gs == 3 || gs == 8) {
					s += " ";
				}
			} else if (i == 67) {
				if (s.length() > 0) {
					s = s.substring(0, s.length() - 1);
					keying(i);
				}
			} else if (i == 66) {
				if (gs == 1) {
					screen.scene.playSound(23, 1, 1);
					if (s.length() >= 3) {
						screen.play();
					} else {
						curStatus = "Username must be at least 3 characters";
					}
				} else if (gs == 3) {
					screen.scene.playSound(23, 1, 1);
					playChatting = false;
					if (s.length() > 0) {
						screen.sendChat(s);
						s = "";
					}
				} else if (gs == 6) {
					screen.scene.playSound(23, 1, 1);
					if (s.length() >= 3) {
						screen.enterLobby();
					} else {
						curStatus = "Username must be at least 3 characters";
					}
				} else if (gs == 8) {
					if (s.length() > 0) {
						screen.lobby.sendChat(screen.lobby.curChat);
						s = "";
					}
				}
			} else {
				if (gs != 1 && gs != 6) {
					char p = getOtherKey(i);
					if (p != Character.MIN_VALUE) {
						s += getOtherKey(i);
					}
				}
			}
		}
		input.keyPress.clear();
		if (keying) {
			if (input.keyDown[keyKey]) {
				if (tick > keyAgainAt) {
					keyAgainAt = tick + 20;
					// repeat keyKey
					input.keyPress.add(keyKey);
				}
			} else {
				keying = false;
			}
		}
		if (s.length() > max) {
			return;
		}
		switch (gs) {
		case 1:
			curNick = s;
			break;
		case 3:
			curChatText = s;
			break;
		case 6:
			curNick = s;
			break;
		case 8:
			screen.lobby.curChat = s;
			break;
		}
	}

	public void checkKeys() {
		Player me = players[screen.myIndex];
		// float drawX;
		float drawY;
		if (me == null) {
			// drawX = 0;
			drawY = 0;
		} else {
			// drawX = me.drawX;
			drawY = me.drawY;
		}
		float tabY = drawY - 200;
		int x, y;
		int gs = screen.gameState;
		x = input.mouseX;
		y = input.mouseY;

		checkEscape();
		if (!playChatting) {
			checkBinds();
		}
		switch (gs) {
		case 0:
			if (AssetLoader.loadedFull) {
				if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
					quitHover = true;
				} else {
					quitHover = false;
				}
				if (input.mouseDown) {
					if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
						screen.net.client.close();
						if (screen.connectedLobby) {
							screen.gameState = 8;
							screen.lobby.selGame = -1;
						} else {
							screen.gameState = 5;
						}
					}
				}
			}
			break;
		case 1:
			if (inBox(x, y, (gameWidth / 2) + 100, 550, 128, 48)) {
				playHover = true;
			} else {
				playHover = false;
			}
			if (inBox(x, y, (gameWidth / 2) - 100, 550, 128, 48)) {
				quitHover = true;
			} else {
				quitHover = false;
			}
			if (input.mouseDown) {
				if (inBox(x, y, (gameWidth / 2) + 100, 550, 128, 48)) {
					if (input.wasMouseJustClicked) {
						input.wasMouseJustClicked = false;
						if (curNick.length() >= 3) {
							screen.play();
						} else {
							curStatus = "Username must be at least 3 characters";
						}
					}
				}
				if (inBox(x, y, (gameWidth / 2) - 100, 550, 128, 48)) {
					if (input.wasMouseJustClicked) {
						input.wasMouseJustClicked = false;
						screen.net.client.close();
						if (screen.connectedLobby) {
							screen.gameState = 8;
							screen.lobby.selGame = -1;
						} else {
							screen.gameState = 5;
						}
					}
				}

			}
			processKeyPress(15);
			break;
		case 2:
			if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
				quitHover = true;
			} else {
				quitHover = false;
			}
			if (input.mouseDown) {
				if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
					screen.net.client.close();
					if (screen.connectedLobby) {
						screen.gameState = 8;
						screen.lobby.selGame = -1;
					} else {
						screen.gameState = 5;
					}
				}
			}
			break;
		case 3:
			if (!me.playing && !spectating) {
				if (info.type == 0) {
					if (inBox(x, y, gameWidth / 2, gameHeight / 2 + tabY + 40, 96, 48)) {
						gameJoinHover = true;
						if (input.mouseDown) {
							if (input.wasMouseJustClicked) {
								input.wasMouseJustClicked = false;
								joinFray(-1);
							}
						}
					} else {
						gameJoinHover = false;
					}
				} else {
					if (inBox(x, y, (gameWidth / 2) - 50, gameHeight / 2 + tabY + 40, 96, 48)) {
						team0join = true;
						if (input.mouseDown) {
							if (input.wasMouseJustClicked) {
								input.wasMouseJustClicked = false;
								joinFray(0);
							}
						}
					} else {
						team0join = false;
					}
					if (inBox(x, y, (gameWidth / 2) + 50, gameHeight / 2 + tabY + 40, 96, 48)) {
						team1join = true;
						if (input.mouseDown) {
							if (input.wasMouseJustClicked) {
								input.wasMouseJustClicked = false;
								joinFray(1);
							}
						}
					} else {
						team1join = false;
					}
				}
				if (inBox(x, y, gameWidth / 2, gameHeight / 2 + tabY + 470, 96, 32)) {
					if (input.mouseDown) {
						if (input.wasMouseJustClicked) {
							input.wasMouseJustClicked = false;
							JoinSpec js = new JoinSpec(1);
							screen.net.client.sendTCP(js);
						}
					}
					specHover = true;
				} else {
					specHover = false;
				}
			} else {
				if (playChatting) {
					processKeyPress(140);
				} else {
					checkPlayBinds();
				}
			}
			break;
		case 4:
			if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
				quitHover = true;
			} else {
				quitHover = false;
			}
			if (input.mouseDown) {
				if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
					screen.net.client.close();
					if (screen.connectedLobby) {
						screen.gameState = 8;
						screen.lobby.selGame = -1;
					} else {
						screen.gameState = 5;
					}
				}
			}
			break;
		case 5:
			if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
				quitHover = true;
			} else {
				quitHover = false;
			}
			if (input.mouseDown) {
				if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
					System.exit(0);
				}
			}
			break;
		case 6:
			if (!screen.firstMessage) {
				if (inBox(x, y, (gameWidth / 2) + 100, 550, 128, 48)) {
					playHover = true;
				} else {
					playHover = false;
				}
				if (inBox(x, y, (gameWidth / 2) - 100, 550, 128, 48)) {
					quitHover = true;
				} else {
					quitHover = false;
				}
				if (input.mouseDown) {
					if (inBox(x, y, (gameWidth / 2) + 100, 550, 128, 48)) {
						if (input.wasMouseJustClicked) {
							input.wasMouseJustClicked = false;
							if (curNick.length() >= 3) {
								screen.enterLobby();
							} else {
								curStatus = "Username must be at least 3 characters";
							}
						}
					}
					if (inBox(x, y, (gameWidth / 2) - 100, 550, 128, 48)) {
						System.exit(0);
					}

				}
				processKeyPress(15);
			} else {
				if (inBox(x, y, 400, 550, 200, 48)) {
					okHover = true;
					if (input.mouseDown) {
						input.mouseDown = false;
						input.wasMouseJustClicked = false;
						screen.firstMessage = false;
						input.keyPress.clear();
					}
				} else {
					okHover = false;
				}
			}
			break;
		case 8:
			if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
				playHover = true;
			} else {
				playHover = false;
			}
			if (inBox(x, y, (gameWidth / 2) - 200, 550, 128, 48)) {
				quitHover = true;
			} else {
				quitHover = false;
			}
			if (inBox(x, y, (gameWidth / 2) + 200, 550, 128, 48)) {
				nickHover = true;
			} else {
				nickHover = false;
			}
			if (input.mouseDown) {
				if (input.wasMouseJustClicked) {
					input.wasMouseJustClicked = false;
					if (inBox(x, y, (gameWidth / 2) - 200, 550, 128, 48)) {
						System.exit(0);
					}
					if (inBox(x, y, (gameWidth / 2) + 200, 550, 128, 48)) {
						screen.lobbynet.client.close();
						// screen.gameState = 0;
					}
					if (inBox(x, y, (gameWidth / 2), 550, 128, 48)) {
						screen.menu.open(screen.gameState, 1);
					}
					screen.lobby.checkClick(x, y);
				}
			}
			processKeyPress(60);
			break;
		}
	}

	public float assertX(float x) {
		if (x < -map.width / 2) {
			return -map.width / 2;
		}
		if (x > map.width / 2) {
			return map.width / 2;
		}
		return x;
	}

	public float assertY(float y) {
		if (y < -map.height / 2) {
			return -map.height / 2;
		}
		if (y > map.width / 2) {
			return map.height / 2;
		}
		return y;
	}

	public boolean checkBind(int i) {
		return (input.keyDown[Options.bind[i]] || (input.mouseDown && input.mouseButton + 256 == Options.bind[i]));
	}

	public boolean checkBindPress(int b) {
		boolean f = false;
		for (Integer i : input.keyPress) {
			if (i == Options.bind[b]) {
				f = true;
			}
		}
		if (f) {
			input.keyPress.clear();
		}
		return (f || (input.mouseDown && input.mouseButton + 256 == Options.bind[b]));

	}

	void checkPlayBinds() {
		if (checkBind(0)) { // forward
			cs.accel = true;
		} else if (checkBind(1)) { // backward
			cs.decel = true;
		}
		if (checkBind(2)) { // left
			cs.strafeLeft = true;
		} else if (checkBind(3)) { // right
			cs.strafeRight = true;
		}
		if (checkBind(4)) { // fire
			if (players[screen.myIndex].reloading) {
				if (tick > reloadSoundStamp) {
					reloadSoundStamp = tick + 300;
					playSound(18, 1, 1);
				}
			}
			cs.fire = true;
		}
		if (checkBindPress(5)) { // change weapon
			if (tick > clientActionStamp) {
				ClientAction ca = new ClientAction();
				ca.act = 1;
				screen.net.client.sendTCP(ca);
				playSound(23, 1, 1);
				clientActionStamp = tick + 200;
			}
		}
		if (checkBindPress(7)) {
			screen.scene.playSound(23, 1, 1);
			playChatting = true;
		}
		if (checkBindPress(11)) {
			// repair
			if (items[0] > 0) {
				if (tick > clientActionStamp) {
					ClientAction ca = new ClientAction();
					ca.act = 2;
					screen.net.client.sendTCP(ca);
					playSound(5, 1, 1);
					clientActionStamp = tick + 200;
				}
			} else {
				playSound(6, 1, 1);
			}
		}
		if (checkBindPress(12)) {
			if (items[1] > 0) {
				if (tick > clientActionStamp) {
					ClientAction ca = new ClientAction();
					ca.act = 3;
					screen.net.client.sendTCP(ca);
					playSound(5, 1, 1);
					clientActionStamp = tick + 200;
				}
			} else {
				playSound(6, 1, 1);
			}
		}
		cs.turbo = checkBind(13);

		if (checkBind(14)) {
			screen.scene.playSound(23, 1, 1);
			curChat += 1;
			if (curChat > (400 - chatLines)) {
				curChat = 400 - chatLines;
			}
			if (curChat > getHighestChat() + 1 - chatLines) {
				curChat = getHighestChat() + 1 - chatLines;
			}
		}
		if (checkBind(15)) {
			screen.scene.playSound(23, 1, 1);
			curChat -= 1;
			if (curChat < 0) {
				curChat = 0;
			}
		}

		if (checkBindPress(16)) {
			screen.scene.playSound(23, 1, 1);
			toggleChat();
		}

		if (checkBindPress(17)) {
			if (tick > clientActionStamp) {
				clientActionStamp = tick + 200;
				if (info.numTeams > 0) {
					screen.net.client.sendTCP(new ChangeTeam());
				}
			}
		}

		if (checkBindPress(18)) {
			if (tick > clientActionStamp) {
				clientActionStamp = tick + 200;
				if (players[screen.myIndex].playing) {
					screen.net.client.sendTCP(new LeaveFray());
				}
			}
		}

		if (checkBindPress(19)) {
			if (tick > clientActionStamp) {
				clientActionStamp = tick + 200;
				ClientAction ca = new ClientAction();
				ca.act = 4;
				screen.net.client.sendTCP(ca);
			}
		}

		if (checkBindPress(20)) {
			if (tick > clientActionStamp) {
				clientActionStamp = tick + 200;
				ClientAction ca = new ClientAction();
				ca.act = 5;
				screen.net.client.sendTCP(ca);
			}
		}

		if (checkBindPress(23)) {
			if (tick > clientActionStamp) {
				toggleMiniMap();
			}
		}

	}

	void toggleChat() {
		chatLines++;
		if (chatLines > 10) {
			chatLines = 0;
		}
		if (chatLines > getHighestChat()) {
			chatLines = getHighestChat();
			addChat("Chat resized to: " + getHighestChat(), Color.LIGHT_GRAY);
		}
	}

	void toggleMiniMap() {
		miniMap = !miniMap;
	}

	void checkBinds() {
		int gs = screen.gameState;
		if (gs == 0) {
			return;
		}
		if (!(gs == 1 || gs == 6 || (gs == 3 && playChatting))) {
			if (checkBindPress(9)) {
				// quick select loadout
				screen.scene.playSound(23, 1, 1);
				if (screen.menu.state() != 9) {
					screen.menu.open(gs, 9);
				}
			}
			if (checkBindPress(10)) {
				screen.scene.playSound(23, 1, 1);
				// loadout screen
				if (screen.menu.state() == 0) {
					screen.menu.fromSource = 1; // so when we close out of
												// loadout screen we return to
												// where we were, not main menu
					screen.menu.open(gs, 7);
				}
			}
		}
		if (gs == 3 && !players[screen.myIndex].playing) {
			if (checkBindPress(21)) {
				if (tick > clientActionStamp) {
					clientActionStamp = tick + 200;
					JoinSpec js = new JoinSpec(0);
					if (spectating) {
						js = new JoinSpec(0);
					} else {
						js = new JoinSpec(1);
					}
					screen.net.client.sendTCP(js);
				}
			}
			if (checkBindPress(22)) {
				if (spectating) {
					if (tick > clientActionStamp) {
						clientActionStamp = tick + 200;
						screen.net.client.sendTCP(new JoinSpec(2));
					}
				}
			}
		}

	}

	void checkEscape() {
		if (checkBindPress(6)) { // esc
			screen.scene.playSound(24, 1, 1);
			screen.menu.open(screen.gameState, 1);
		}
	}

	public void rotateToMouse() {
		if (screen.menu.state() > 0) {
			return;
		}
		if (input.mouseX < 0 || input.mouseX >= gameWidth || input.mouseY < 0 || input.mouseY >= gameHeight) {
			return;
		}
		float r;
		float windowWidth = gameWidth;
		float windowHeight = gameHeight;

		if (input.mouseY < gameHeight / 2) {
			r = (float) (Math.atan2((input.mouseY - windowHeight / 2), (input.mouseX - windowWidth / 2))
					+ Math.toRadians(90));
		} else {
			r = (float) (Math.atan2((input.mouseY - windowHeight / 2), (input.mouseX - windowWidth / 2))
					+ Math.toRadians(90));
		}
		cs.desiredRotation = Vector.fixDir(r);
	}

	public void drawFrame(float x, float y, float width, float height, boolean useBackground) {
		screen.drawFrame(x, y, width, height, useBackground);
	}

	public void drawButton(boolean pressed, float dx, float dy, float width, float height, boolean centered) {
		float x, y;
		if (centered) {
			x = dx - (width / 2);
			y = dy - (height / 2);
		} else {
			x = dx;
			y = dy;
		}
		TextureRegion[][] button = AssetLoader.button;
		int p;
		if (pressed) {
			p = 1;
		} else {
			p = 0;
		}
		for (int a = 8; a < height - 8; a += 8) {
			for (int b = 8; b < width - 8; b += 8) {
				screen.drawRegion(button[p][8], x + b, y + a, false, 0, 1);
			}
		}

		// draw top left
		screen.drawRegion(button[p][0], x, y, false, 0, 1);
		// top right
		screen.drawRegion(button[p][1], x + width - 8, y, false, 0, 1);
		// bottom left
		screen.drawRegion(button[p][2], x, y + height - 8, false, 0, 1);
		// bottom right
		screen.drawRegion(button[p][3], x + width - 8, y + height - 8, false, 0, 1);

		// left side
		for (int b = 8; b < height - 8; b += 8) {
			screen.drawRegion(button[p][4], x, y + b, false, 0, 1);
		}
		// right side
		for (int b = 8; b < height - 8; b += 8) {
			screen.drawRegion(button[p][5], x + width - 8, y + b, false, 0, 1);
		}
		// top side
		for (int b = 8; b < width - 8; b += 8) {
			screen.drawRegion(button[p][6], x + b, y, false, 0, 1);
		}
		// bottom side
		for (int b = 8; b < width - 8; b += 8) {
			screen.drawRegion(button[p][7], x + b, y + height - 8, false, 0, 1);
		}

	}

	public void drawTextArea(boolean lit, float dx, float dy, float width, boolean centered) {
		float x, y;
		if (centered) {
			x = dx - (width / 2);
			y = dy - 13;
		} else {
			x = dx;
			y = dy;
		}
		int l = 1;
		if (lit) {
			l = 0;
		}
		// screen.batcher.setColor(new Color(128,128,128,1));
		screen.drawRegion(AssetLoader.field[l][0], x, y, false, 0, 1);
		for (int b = 42; b < width - 42; b += 32) {
			screen.drawRegion(AssetLoader.field[l][1], x + b, y, false, 0, 1);
		}
		screen.drawRegion(AssetLoader.field[l][2], x + width - 42, y, false, 0, 1);

	}

	void clearInput() {
		input.mouseDown = false;
		input.wasMouseJustClicked = false;
		input.clickHandled = false;
		input.keyPress.clear();
	}

}
