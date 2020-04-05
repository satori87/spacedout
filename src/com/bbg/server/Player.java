package com.bbg.server;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.bbg.server.DesktopServer.GameConnection;
import com.bbg.shared.Entities;
import com.bbg.shared.CollisionFlags;
import com.bbg.shared.Vector;
import com.bbg.shared.Network.BulletData;
import com.bbg.shared.Network.ChangeLoad;
import com.bbg.shared.Network.ChangeTeam;
import com.bbg.shared.Network.ChatToServer;
import com.bbg.shared.Network.ClientAction;
import com.bbg.shared.Network.ClientSync;
import com.bbg.shared.Network.FrayError;
import com.bbg.shared.Network.JoinFray;
import com.bbg.shared.Network.JoinSpec;
import com.bbg.shared.Network.KillData;
import com.bbg.shared.Network.LeaveFray;
import com.bbg.shared.Network.LeftSpec;
import com.bbg.shared.Network.LoadoutData;
import com.bbg.shared.Network.PingPacket;
import com.bbg.shared.Network.PlayPacket;
import com.bbg.shared.Network.PlayerData;
import com.bbg.shared.Network.PlayingData;
import com.bbg.shared.Network.PongPacket;
import com.bbg.shared.Network.SecondData;
import com.bbg.shared.Network.SecondPacket;
import com.bbg.shared.Network.SyncData;
import com.bbg.shared.Vector.Coord;
import com.bbg.shared.WeaponDef;
import com.bbg.shared.Shared;

import com.bbg.shared.ShipDef;

public class Player extends Entity {

	public String name;
	public GameConnection conn;
	public int index;
	public boolean auth = false;
	public int kills = 0, deaths = 0, score = 0;
	public int team = -1;
	public Flag flag = null;
	public ClientSync cs = new ClientSync();
	public float rotationSpeed;
	public long fireStamp = 0;
	public long reloadStamp = 0;
	public int packets = 0;
	public int maxhp;
	public boolean spectating = false;
	public int hp;
	public boolean beenDead = false; // can respawn yet
	public long pingStamp = 0;
	public int ping = 0;
	public long lastSentPing;
	public boolean joinedGame = false;
	public boolean activelyPlaying = false;
	public long respawnStamp = 0;
	public LoadoutData load = new LoadoutData();
	public boolean dead = false;
	public int curWeapon = 0;
	public InvWeapon[] weapons = new InvWeapon[20];
	public int[] items = new int[3];
	public boolean reloading = false;
	public boolean turboing = false;
	public long turboStamp = 0;
	public boolean healing = false;
	public boolean dontHeal = false;
	public long healStamp = 0;
	public boolean shielding = false;
	public long shieldStamp = 0;
	public List<Player> mutes = new LinkedList<Player>();
	public long changeStamp = 0;
	public long chatStamp = 0;
	public int actions = 0;
	public int numWeapons = 0;
	public Player lastAttacker = null;
	public long lastAttackStamp = 0;
	public int spec = -1;
	public Player specP;
	public int sX = 0, sY = 0;

	public static class InvWeapon {
		public int type = 0;
		public int shotsLeft = 0;
	}

	public Player(Game game, GameConnection c, String name) {
		conn = c;
		this.game = game;
		this.name = name;
		index = c.index;
	}

	public boolean alive() {
		return active() && !dead;
	}

	public void reset() {
		Coord c = game.findClearSpot(team);
		x = c.x;
		y = c.y;
		dead = false;
		beenDead = false;
		maxhp = ShipDef.getShipMaxHP(load.armor);
		hp = maxhp;
		speedX = 0;
		speedY = 0;
		reloading = false;
		lastAttacker = null;
		grantLoadItems();
		grantLoadWeapons();
		cs = new ClientSync();
		body = null;
		rotationSpeed = ShipDef.getShipRotationSpeed(load.armor);
		setupBody(ShipDef.getShipShape(1, Shared.xml.physicsScale, ShipDef.getShipScale(load.armor)),
				ShipDef.getShipDensity(load.armor), Shared.xml.shipFriction, Shared.xml.shipRestitution);
		changeWeapon();
	}

	void grantLoadWeapons() {
		for (int e = 0; e < 20; e++) {
			weapons[e] = new InvWeapon();
			weapons[e].type = -1;
		}
		int w = 0;
		for (Integer i : load.weapons) {
			weapons[w].type = i;
			weapons[w].shotsLeft = Entities.weapons[weapons[w].type].clip;
			w++;
		}
		if (w == 0) {
			weapons[w].type = 0;
			weapons[w].shotsLeft = Entities.weapons[weapons[w].type].clip;
			w++;
		}
		numWeapons = w;
	}

	public void forceSwitch() {
		destroyBody();
		team = 1 - team;
		game.playerMsg(this, "You have been team changed", Color.YELLOW);
		for (Bullet b : game.bullets) {
			if (b != null) {
				if (b.owner == this) {
					b.remove();
				}
			}
		}
		joinFray();
	}

	void pickup(Pickup p) {
		if (dead) {
			System.out.println("ugh");
			return;
		}
		String s = "!";
		if (p.qty > 1) {
			s = "s!";
		}
		switch (p.type) {
		case 0: // repair pack
			items[0] += p.qty;
			game.playerMsg(this, "You got " + p.qty + " Repair Pack" + s, Color.WHITE);
			break;
		case 1:
			items[2] += p.qty;
			game.playerMsg(this, "You got " + p.qty + " Turbo" + s, Color.WHITE);
			break;
		case 2:
			items[1] += p.qty;
			game.playerMsg(this, "You got " + p.qty + " Shield" + s, Color.WHITE);
			break;
		case 3:
			healBy(p.qty);
			game.playerMsg(this, "You got " + p.qty + " Health!", Color.WHITE);
			break;
		case 4:
			game.playerMsg(this, "You got " + p.qty + " Energy!", Color.WHITE);
			break;
		}
		p.remove();
	}

	public void healBy(int q) {
		hp += q;
		if(hp > maxhp) {
			hp = maxhp;
		}
		if(!healing) {
			healStamp = game.tick + 500;
			healing = true;
			dontHeal = true;
		}
	}

	void grantLoadItems() {
		items[0] = 0;
		items[1] = 0;
		items[2] = 0;
		for (Integer i : load.items) {
			switch (i) {
			case 1:
				items[0] += 1;
				break;
			case 2:
				items[1] += 2;
				break;
			case 3:
				items[2] += 5;
				break;
			}
		}
	}

	public void checkReload() {
		WeaponDef ws = Entities.weapons[weapons[curWeapon].type];
		if (reloading) {
			if (game.tick > reloadStamp) {
				reloading = false;
				weapons[curWeapon].shotsLeft = ws.clip;
			}
		} else if (ws.hasClip && weapons[curWeapon].shotsLeft < 1) {
			reloading = true;
			reloadStamp = game.tick + ws.reloadTime;
		}
	}

	void reload() {
		if (reloading) {
			return;
		}
		WeaponDef ws = Entities.weapons[weapons[curWeapon].type];
		if (ws.hasClip && weapons[curWeapon].shotsLeft < ws.clip) {
			reloading = true;
			reloadStamp = game.tick + ws.reloadTime;
		}
	}

	public void changeWeapon() {
		curWeapon++;
		if (curWeapon >= numWeapons) {
			curWeapon = 0;
		}
		int w = curWeapon;
		if (weapons[w].type < 0) {
			weapons[w].type = 0;
		}
		reloading = false;
		if (weapons[curWeapon].shotsLeft == 0) {
			reloading = true;
			reloadStamp = game.tick + Entities.weapons[weapons[curWeapon].type].reloadTime;
		}
	}

	public boolean joined() {
		return joinedGame || spectating;
	}

	public boolean changeLoad(LoadoutData load) {
		int a = load.armor;
		if (a < 0 || a > 2) {
			return false;
		}
		LoadoutData m = new LoadoutData();
		// VERIFY THAT THIS LOADOUT FOLLOWS RULES OR HAX
		int mprims = ShipDef.getArmorPrimaries(load.armor);
		int msecs = ShipDef.getArmorSecondaries(load.armor);
		int mitems = ShipDef.getArmorItems(load.armor);
		int prim = 0;
		int sec = 0;
		int it = 0;
		for (Integer i : load.weapons) {
			if (Entities.validWeapon(i)) {
				if (Entities.weapons[i].primary) {
					if (prim < mprims && ShipDef.canWear(a, Entities.weapons[i])) {
						m.weapons.add(i);
						prim++;
					}
				} else {
					if (sec < msecs && ShipDef.canWear(a, Entities.weapons[i])) {
						m.weapons.add(i);
						sec++;
					}
				}
			} else {
				return false;
			}
		}
		if (prim + sec < 1) {
			m.weapons.add(0);
		}
		for (Integer i : load.items) {
			if (Entities.validItem(i)) {
				if (it < mitems) {
					m.items.add(i);
					it++;
				}
			} else {
				return false;
			}
		}
		m.armor = a;
		m.col = load.col;
		if (!Entities.isColorBright(load.col)) {
			return false;
		}
		this.load = m;
		return true;
	}

	public void receive(Object obj) {
		if (obj instanceof JoinFray) {
			JoinFray j = (JoinFray) obj;
			if (joined() && !active()) {
				if (game.tick > changeStamp) {
					if (!changeLoad(j.load)) {
						conn.close();
						return;
					}
					team = -1;
					if (game.numTeams > 0) {
						if (j.team == 0 || j.team == 1) {
							if (game.teams[j.team].numPlayers() <= game.teams[1 - j.team].numPlayers()) {
								team = j.team;
							} else {
								// do not join if we cant join the team we want
								// send a msg instead
								conn.sendTCP(new FrayError("Teams must be even"));
								return;
							}
						} else {
							game.playerMsg(this, "Try again soon", Color.GRAY);
							return; // HAX
						}
					}
					changeStamp = game.tick + 2000;
					joinFray();
				} else {
					game.playerMsg(this, "Try again soon", Color.GRAY);
				}
			} else {
				game.playerMsg(this, "Try again soon", Color.GRAY);
			}
		}
		if (obj instanceof JoinSpec) {
			JoinSpec js = (JoinSpec) obj;
			if (js.hi == 1) {
				if (joined() && !active() && !spectating) {
					spectate(1);
				}
			} else if (js.hi == 0) {
				if (joined() && !active() && spectating) {
					spectate(0);
				}
			} else if (js.hi == 2) {
				if (joined() && !active() && spectating) {
					spectate(2);
				}
			}
		}
		if (obj instanceof ChangeTeam) {
			if (game.numTeams > 0 && flag == null) {
				if (game.tick > changeStamp) {
					changeStamp = game.tick + 500;
					if (active() && team >= 0) {
						if (game.teams[team].numPlayers() > game.teams[1 - team].numPlayers()) {
							forceSwitch();
							changeStamp = game.tick + 20000;
						} else {
							game.playerMsg(this, "Try again soon", Color.GRAY);
						}
					} else {
						game.playerMsg(this, "Try again soon", Color.GRAY);
					}
				} else {
					game.playerMsg(this, "Try again soon", Color.GRAY);
				}
			} else {
				game.playerMsg(this, "Try again soon", Color.GRAY);
			}
		}
		if (obj instanceof LeaveFray) {
			leaveFray();
		}
		if (obj instanceof ChangeLoad) {
			ChangeLoad c = (ChangeLoad) obj;
			changeLoad(c.load);
		}
		if (obj instanceof PongPacket) {
			PongPacket p = (PongPacket) obj;
			if (p.id == lastSentPing) {
				// PONG
				ping = (int) (game.tick - p.id);

			} else {

				// ping = (int) (game.tick - lastSentPing);
			}
		}
		if (obj instanceof ClientSync) {
			ClientSync tempCS = (ClientSync) obj;
			if (tempCS.ignore) {
				return;
			}
			cs = (ClientSync) obj;
		}
		if (obj instanceof ClientAction) {
			ClientAction ca = (ClientAction) obj;
			actions++;
			switch (ca.act) {
			case 1: // change weapon
				changeWeapon();
				break;
			case 2: // use health kit
				if (items[0] > 0) {
					if (!healing) {
						healStamp = game.tick + 2000;
						healing = true;
						dontHeal = false;
					}
					items[0] -= 1;
				}
				break;
			case 3: // use shield
				if (items[1] > 0) {
					if (!shielding) {
						shieldStamp = game.tick + 7000;
						shielding = true;
					}
					items[1] -= 1;
				}
				break;
			case 4: // reload
				if (game.tick > fireStamp) {
					reload();
				}
				break;
			case 5:
				if (active() && !dead) {
					suicide();
				}
				break;
			}
		}
		if (obj instanceof ChatToServer) {
			if (game.tick > chatStamp || auth) {
				chatStamp = game.tick + 2000;
			} else {
				game.playerMsg(this, "You must wait 2 seconds between messages", Color.GRAY);
				return;
			}
			ChatToServer cs = (ChatToServer) obj;
			if (cs.msg.length() > 0 && cs.msg.length() < 150) {
				String s = cs.msg;
				if (s.charAt(0) == '/') {
					String rest = s.substring(1);
					String[] words = rest.split(" ");
					if (words.length > 0) {
						switch (words[0]) {
						case "team":
						case "tea":
						case "te":
						case "t":
							if (words.length > 1) {
								int w = 0;
								String msg = "";
								for (String ss : words) {
									if (w > 0) {
										if (w > 1) {
											msg = msg + " " + ss;
										} else {
											msg = ss;
										}
									}
									w++;
								}

								msg = "[" + game.getTime() + "]<TEAM>" + name + ": " + msg;
								game.teamMsg(this, team, msg);
							}
							break;
						case "pm":
						case "p":
							if (words.length > 2) {
								// System.out.println("ab");
								// System.out.println(words[1]);

								Player p = game.findPlayer(words[1]);
								if (p != null) {
									int w = 0;
									String msg = "";
									for (String ss : words) {
										if (w > 1) {
											if (w > 2) {
												msg = msg + " " + ss;
											} else {
												msg = ss;
											}
										}
										w++;
									}
									// System.out.println(msg);

									String rmsg = "[" + game.getTime() + "]<PM>" + name + ": " + msg;
									game.playerMsg(this, p, rmsg, Color.MAGENTA);
									rmsg = "[" + game.getTime() + "]<@" + p.name + ">: " + msg;
									game.playerMsg(this, this, rmsg, Color.MAGENTA);

								} else {
									// System.out.println("ac");
								}
							} else {
								// System.out.println("aa");
							}
							break;
						case "mute":
							if (words.length > 1) {
								if (words[1].equals(name)) {
									return;
								}
								Player p = game.findPlayer(words[1]);
								if (p != null) {
									String msg = "";
									if (!inMutes(p)) {
										mutes.add(p);
										msg = "[" + game.getTime() + "] You have muted " + words[1];
									} else {
										msg = "[" + game.getTime() + "] " + words[1] + " is already muted";
									}
									game.playerMsg(this, this, msg, Color.GRAY);
								} else {
									// System.out.println("ab");
								}
							} else {
								// System.out.println("aa");
							}
							break;
						case "unmute":
							if (words.length > 1) {
								if (words[1].equals(name)) {
									return;
								}
								Player p = game.findPlayer(words[1]);
								if (p != null) {
									String msg = "";
									if (inMutes(p)) {
										mutes.remove(p);
										msg = "[" + game.getTime() + "] You have unmuted " + words[1];
									} else {
										msg = "[" + game.getTime() + "] " + words[1] + " is not muted";
									}
									game.playerMsg(this, this, msg, Color.GRAY);
								}
							}
							break;
						case "auth":
							if (words.length == 2) {
								if (words[1].equals("honeypot")) {
									auth = true;
									game.playerMsg(this, "Authorized", Color.RED);
								}
							}
							break;
						case "map":
							if (authed() && words.length == 5) {
								int m = Integer.parseInt(words[1]);
								if (m < 0) {
									m = 0;
								}
								int t = Integer.parseInt(words[2]);
								if (t < 0) {
									t = 0;
								}
								if (t > 2) {
									t = 2;
								}
								if (m >= Entities.maps.length) {
									m = Entities.maps.length - 1;
								}
								int ti = Integer.parseInt(words[3]);
								if (ti < 0) {
									ti = 0;
								}
								int sc = Integer.parseInt(words[4]);
								if (sc < 0) {
									sc = 0;
								}
								game.forcedMap = true;
								game.forceMap = m;
								game.forceScore = sc;
								game.forceTime = ti;
								game.forceType = t;
								game.globalMsg(name + " has set the next map to " + Entities.maps[m].name + " ["
										+ game.getGameTypeString(t) + "]", Color.RED);
							}
							break;
						case "endgame":
							if (authed()) {
								game.globalMsg("SERVER: Game ended ", Color.RED);
								game.endGame();
							}
							break;
						case "warp":
							if (authed() && words.length == 3) {
								int wX = Integer.parseInt(words[1]);
								int wY = Integer.parseInt(words[2]);
								forceWarp(wX, wY);
								game.playerMsg(this, "Warped", Color.RED);
							}
							break;
						}
					}
				} else {
					String msg = "[" + game.getTime() + "] " + name + ": " + cs.msg;
					game.globalMsg(this, msg, Color.WHITE);
				}
			}
		}
	}

	public boolean authed() {
		return (name.equals("bear"));
	}

	public void forceWarp(float wX, float wY) {
		x = wX;
		y = wY;
		body.setTransform(x / Shared.xml.physicsScale, y / Shared.xml.physicsScale, body.getAngle());
		float cX = 2800;
		float cY = -1600;
		float d = 0;
		for (int a = 0; a < 8; a++) {
			Vector v = new Vector(d, 300);
			System.out.println((cX + v.xChange) + "," + (cY + v.yChange));
			d += (float) (Math.PI / 4);
		}

	}

	boolean inMutes(Player m) {
		for (Player p : mutes) {
			if (p == m) {
				return true;
			}
		}
		return false;
	}

	public void fire() {
		if (dead || reloading) {
			return;
		}
		WeaponDef ws = Entities.weapons[weapons[curWeapon].type];
		if (game.tick > fireStamp && (!ws.hasClip || (weapons[curWeapon].shotsLeft > 0))) {
			fireStamp = game.tick + ws.fireTime;
			weapons[curWeapon].shotsLeft -= 1;
			for (Float f : ws.spread) {
				Vector v = new Vector(direction + f, 28);
				game.newBullet(this, ws.type, x + (v.xChange), y + (v.yChange), direction + f);
			}
		}
	}

	void turbo() {
		if (turboing) {
			if (game.tick > turboStamp) {
				turboing = false;
			}
		}
		if (!turboing && cs.turbo) {
			if (items[2] > 0 || authed()) {
				if (!authed()) {
					items[2] = items[2] - 1;
				}
				turboStamp = game.tick + 1000;
				turboing = true;
			}
		}

	}

	public void leaveFray() {
		if (active()) {
			if (!dead) {
				game.playerMsg(this, "You must be dead to leave the fray", Color.GRAY);
				return;
			}
			if (game.tick > changeStamp) {
				changeStamp = game.tick + 5000;
				destroyBody();
				activelyPlaying = false;
				team = -1;
				PlayerData pd = getPlayerData();
				pd.deactive = true;
				game.sendAllJoined(pd, false);
			} else {
				game.playerMsg(this, "Try again soon", Color.GRAY);
			}
		}
	}

	public void joinFray() {
		reset();
		if (game.gameState == 2) {
			createMyBody();
		}
		PlayPacket j = new PlayPacket();
		j.hp = maxhp;
		j.x = (int) x;
		j.y = (int) y;
		j.armor = load.armor;
		j.col = load.col;
		j.direction = direction;
		j.minX = (int) game.minX;
		j.minY = (int) game.minY;
		j.maxX = (int) game.maxX;
		j.maxY = (int) game.maxY;
		j.team = team;
		j.spectating = false;
		j.items = items;
		j.bullets = new LinkedList<BulletData>();
		j.bullets = new LinkedList<BulletData>();
		for (Bullet b : game.bullets) {
			if (b != null) {
				j.bullets.add(b.getBulletData());
			}
		}
		for (Player p : game.players) {
			if (p != null) {
				PlayingData pd = new PlayingData();
				pd.index = p.index;
				pd.playing = p.activelyPlaying;
				j.pad.add(pd);
			}
		}
		game.sendTo(this, j, false);
		PlayerData pd = getPlayerData();
		pd.active = true;
		game.sendToAllJoinedBut(this, pd, false);
		activelyPlaying = true;
	}

	public void destroy() {
		super.destroy();
		if (conn != null) {
			conn.close();
			conn = null;
		}
	}

	public SyncData getSyncData() {
		// express thyself
		SyncData s = new SyncData();
		s.direction = direction;
		s.index = (short) index;
		s.x = (short) x;
		s.y = (short) y;
		s.vX = speedX;
		s.vY = speedY;
		s.accel = cs.accel;
		s.decel = cs.decel;
		s.strafeLeft = cs.strafeLeft;
		s.strafeRight = cs.strafeRight;
		s.desiredR = cs.desiredRotation;
		s.turbo = turboing;
		s.healing = healing;
		s.shielding = shielding;
		return s;
	}

	public PlayerData getPlayerData() {
		PlayerData pd = new PlayerData();
		pd.index = index;
		pd.name = name;
		pd.dead = dead;
		pd.kills = kills;
		pd.deaths = deaths;
		pd.score = score;
		pd.x = x;
		pd.y = y;
		pd.direction = direction;
		pd.team = team;
		pd.col = load.col;
		pd.armor = load.armor;
		return pd;
	}

	public void disconnected() {
		if (joined()) {
			if (active() && !alive()) {
				suicide();
			}
			joinedGame = false;
			activelyPlaying = false;
			spectating = false;
			remove();
		}
	}

	public void remove() {
		super.remove();
		// lets announce that jawn
		PlayerData pd = getPlayerData();
		pd.leave = true;
		game.sendAllJoined(pd, false);
	}

	public void struckBy(Bullet b, Vector2[] pts) {
		if (b.remove) {
			return;
		}
		if (dead) {
			return;
		}
		b.playerStruck = index;
		b.sX = pts[0].x * Shared.xml.physicsScale;
		b.sY = pts[0].y * Shared.xml.physicsScale;

		Player shooter = b.owner;
		if (shooter != null) {
			if (game.numTeams > 0) {
				if (shooter.team == team) {
					return; // still removes the bullet, but does no damage
							// unless
							// we enable friendly fire later on
				}
			}
			if (shooter.name.equals(name)) {
				return;
			}
			lastAttacker = shooter;
			lastAttackStamp = game.tick;
		}
		int d = Entities.weapons[b.type].dam;
		if (shielding) {
			d /= 3;
		}
		hp -= d;
		if (hp <= 0) {
			died(shooter, b);
		}
	}

	public void died(Player p, Bullet b) {
		// awwww shit
		// COME BACK HERE TO FIX FOR WHEN THE KILLER HAS DROPPED
		dropItems();
		KillData kd = new KillData();
		if (p == null) {
			p = this;
			kd.lostKiller = true;
		}
		dead = true;
		respawnStamp = game.tick + Shared.xml.respawnTime;
		kd.killedIndex = index;
		kd.killerIndex = p.index;
		if (b != null) {
			kd.type = b.firingWeapon;
		} else {
			kd.type = -1;
		}
		game.sendAllPlaying(kd, false);
		destroyBody();
		deaths += 1;
		p.tallyKill(this);
		score -= 1;

	}

	void dropItems() {
		if(items[0] > 0) {
			game.newPickup(0, items[0], x, y);
		}
		if(items[2] > 0) {
			game.newPickup(1, items[2], x, y);
		}
		if(items[1] > 0) {
			game.newPickup(2, items[1], x, y);
		}
		int h = 16;
		switch(load.armor) {
		case 0:
			h = 12;
			break;
		case 1:
			h = 16;
			break;
		case 2:
			h = 20;
			break;		
		}
		game.newPickup(3, h, x, y);
	}

	void suicide() {
		KillData kd = new KillData();
		if (lastAttacker != null && lastAttacker.active() && game.tick - 30000 < lastAttackStamp) {
			kd.killerIndex = lastAttacker.index;
			kd.lostKiller = false;
		} else {
			kd.lostKiller = true;
			kd.killerIndex = index;
		}
		dead = true;
		respawnStamp = game.tick + Shared.xml.respawnTime;
		kd.killedIndex = index;
		kd.type = -1;
		game.sendAllPlaying(kd, false);
		destroyBody();
		deaths += 1;
		score -= 1;

	}

	void tallyKill(Player killed) {
		kills += 1;
		score += 1;
		if (game.gameType == 1 && team >= 0) {
			game.teams[team].tally();
		}
		game.tallyKill();
	}

	public void respawn() {
		reset();
		createMyBody();
		game.sendAllPlaying(getPlayerData(), false);

	}

	public void createMyBody() {
		if (game.gameType == 2) {
			if (team == 1) {
				createBody(CollisionFlags.CAT_T1PLAYER, CollisionFlags.MASK_T1PLAYER, false, this, true, false);
			} else {
				createBody(CollisionFlags.CAT_T2PLAYER, CollisionFlags.MASK_T2PLAYER, false, this, true, false);
			}
		} else {
			createBody(CollisionFlags.CAT_SOLID, CollisionFlags.CAT_SOLID, false, this, true, false);
		}
	}

	void pingPacket() {
		if (game.tick > pingStamp) {
			pingStamp = game.tick + Config.xml.pingInterval;
			PingPacket pp = new PingPacket();
			lastSentPing = game.tick;
			pp.id = lastSentPing;
			conn.sendTCP(pp);
			// lets also send everyone our last ping
			SecondPacket ps = new SecondPacket();
			ps.data = new LinkedList<SecondData>();
			for (Player p2 : game.players) {
				if (p2.joined()) {
					SecondData pd = new SecondData();
					pd.index = p2.index;
					pd.ping = p2.ping;
					pd.score = p2.score;
					ps.data.add(pd);
				}
			}
			if (game.numTeams > 0) {
				ps.score0 = game.teams[0].score;
				ps.score1 = game.teams[1].score;
			}
			game.sendTo(this, ps, false);
		}
	}

	public void preStep() {
		if (active() && dead) {
			if (beenDead) {
				if (game.tick > respawnStamp + Config.xml.idleTime) { // gone
																		// idle
																		// after
																		// death
					leaveFray();
					game.playerMsg(this, "You have been ejected from the game for being idle.", Color.YELLOW);
				} else if (cs.fire) {
					respawn();
				} else {
					return;
				}
			} else {
				if (game.tick > respawnStamp) {
					beenDead = true;
				}
				return;
			}
		}
		pingPacket();
		if (active()) {
			applyHeal();
			if (shielding && game.tick > shieldStamp) {
				shielding = false;
			}
			checkReload();
			ShipDef.rotateToDesired(body, cs.desiredRotation, direction, ShipDef.getShipRotationSpeed(load.armor));
			turbo();
			applyThrust();
			capSpeed(ShipDef.getShipSpeed(load.armor, turboing));
			body.setLinearVelocity(speedX, speedY);
		} else if (spectating) {
			if (spec == -1) {
				System.out.println("goat");
				int v = 10;
				if (cs.turbo) {
					v *= 3;
				}
				if (cs.accel) {
					sY -= v;
				} else if (cs.decel) {
					sY += v;
				}
				if (cs.strafeLeft) {
					sX -= v;
				} else if (cs.strafeRight) {
					sX += v;
				}
				x = sX;
				y = sY;
				if (x < -game.map.width / 2) {
					x = -game.map.width / 2;
				}
				if (x > game.map.width / 2) {
					x = game.map.width / 2;
				}
				if (y < -game.map.height / 2) {
					y = -game.map.height / 2;
				}
				if (y > game.map.height / 2) {
					y = game.map.height / 2;
				}
			} else {
				if (cs.accel || cs.decel || cs.strafeLeft || cs.strafeRight) {
					System.out.println("goat2");
					spec = -1;
					specP = null;
					sX = (int) x;
					sY = (int) y;
				} else {
					System.out.println("goat3");
					if (specP != null) {
						System.out.println("goat4");
						x = specP.x;
						y = specP.y;
					}
				}
			}
		}
	}

	void applyHeal() {
		if (healing && game.tick > healStamp) {
			healing = false;
		}
		if (healing) {
			if (!dontHeal) {
				hp += 3;
			}
			if (hp > maxhp) {
				hp = maxhp;
				// healing = false;
			}
		}
	}

	void applyThrust() {
		if (cs.accel) {
			Vector v = new Vector(direction, ShipDef.getShipThrust(0, load.armor, turboing));
			body.applyForceToCenter(v.xChange, v.yChange, true);
		} else if (cs.decel) {
			Vector v = new Vector(direction, ShipDef.getShipThrust(1, load.armor, turboing));
			body.applyForceToCenter(-v.xChange, -v.yChange, true);
		}
		if (cs.strafeLeft) {
			Vector v = new Vector(direction - (float) Math.toRadians(90),
					ShipDef.getShipThrust(2, load.armor, turboing));
			body.applyForceToCenter(v.xChange, v.yChange, true);
		} else if (cs.strafeRight) {
			Vector v = new Vector(direction + (float) Math.toRadians(90),
					ShipDef.getShipThrust(2, load.armor, turboing));
			body.applyForceToCenter(v.xChange, v.yChange, true);
		}
	}

	void spectate(int s) {
		boolean changed = false;
		if (s == 1) {
			spectating = true;
			spec = -1;
			specP = null;
			sX = 0;
			sY = 0;
			for (Player p : game.players) {
				if (p.active()) {
					spec = p.index;
					specP = p;
					break;
				}
			}
			String sStr = "";
			if (spec >= 0) {
				Player sp = game.findPlayer(spec);
				sStr = " " + sp.name;
			}
			game.playerMsg(this, "You are now spectating" + sStr, Color.WHITE);
			dead = false;
			x = 0;
			y = 0;
			PlayPacket j = new PlayPacket();
			j.team = -2;
			team = -2;
			j.spectating = true;
			j.bullets = new LinkedList<BulletData>();
			for (Bullet b : game.bullets) {
				if (b != null) {
					j.bullets.add(b.getBulletData());
				}
			}
			for (Player p : game.players) {
				if (p != null) {
					PlayingData pd = new PlayingData();
					pd.index = p.index;
					pd.playing = p.activelyPlaying;
					j.pad.add(pd);
				}
			}
			game.sendTo(this, j, false);
		} else if (s == 0) {
			team = -1;
			spectating = false;
			game.playerMsg(this, "You are no longer spectating", Color.WHITE);
			game.sendTo(this, new LeftSpec(), false);
		} else if (s == 2) {
			if (spec == -1) {
				for (Player p : game.players) {
					if (p.active()) {
						spec = p.index;
						specP = p;
						break;
					}
				}
			} else {
				for (Player p : game.players) {
					if (p.active() && p.index > spec) {
						spec = p.index;
						specP = p;
						changed = true;
						break;
					}
				}
				if (!changed) {
					for (Player p : game.players) {
						if (p.active()) {
							spec = p.index;
							specP = p;
							break;
						}
					}
				}
			}
		}
	}

}
