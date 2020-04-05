package com.bbg.lobby;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import com.bbg.lobby.LobbyServer.LobbyConnection;
import com.bbg.shared.Network.GameInfo;
import com.bbg.lobby.LobbyNetwork.JoinLobby;
import com.bbg.lobby.LobbyNetwork.LobbyJoinError;
import com.bbg.lobby.LobbyNetwork.LobbyLogin;
import com.bbg.lobby.LobbyNetwork.ServerData;
import com.bbg.lobby.LobbyNetwork.UserData;

public class Lobby {

	LobbyServer server;

	public Queue<PacketData> packetQueue = new LinkedList<PacketData>();
	public boolean checkingPacketQueue = false;
	public boolean checkingDiscQueue = false;
	public Queue<User> discQueue = new LinkedList<User>();
	public List<User> users = new LinkedList<User>();
	public Hashtable<String, ServerInfo> games = new Hashtable<String, ServerInfo>();
	public List<ServerInfo> servers = new LinkedList<ServerInfo>();

	public static class PacketData {
		LobbyConnection c;
		Object object;
	}

	public long stepStamp = 0;

	public long tick = System.currentTimeMillis();

	public Lobby(LobbyServer server) {
		this.server = server;

	}

	public void start() {

	}

	public User joinLobby(LobbyConnection c, String user) {
		User p = new User(this, c, user);
		users.add(p);
		JoinLobby j = new JoinLobby();
		j.index = p.conn.index;
		j.users = new LinkedList<UserData>();
		j.games = new LinkedList<ServerData>();
		j.name = user;
		for (User po : users) {
			UserData jd = po.getUserData();
			j.users.add(jd);
		}
		for (ServerInfo si : servers) {
			ServerData sd = si.getServerData();
			j.games.add(sd);
		}
		sendTo(p, j);
		UserData pd = p.getUserData();
		pd.join = true;
		sendAllBut(p, pd, false);
		p.joined = true;
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

	public void receiveData(LobbyConnection c, Object object) {
		while (accessPacketQueue(false, false)) {
			// the other thread is actively processing packets so we must wait a
			// SHORT while
		}
		PacketData p = new PacketData();
		p.c = c;
		p.object = object;
		packetQueue.add(p);
	}

	public boolean checkServer(String ip) {
		return(getGame(ip) != null);
	}
	
	public ServerInfo getGame(String key) {
		return (ServerInfo) games.get(key);
	}
	
	public void processPacket(PacketData p) {
		LobbyConnection c = p.c;
		Object object = p.object;
		if (c.player == null) {
			if (object instanceof LobbyLogin) {
				LobbyLogin pkt = (LobbyLogin) object;
				if (pkt.newAccount) {
					newAccount(c, pkt.user, pkt.pass);
				} else {
					login(c, pkt.user, pkt.pass);
				}
			}
			if (object instanceof com.bbg.shared.Network.GameInfo) {
				String[] words = c.getRemoteAddressTCP().toString().split(":");
				String ip = words[0].substring(1, words[0].length());
				GameInfo info = (GameInfo) object;
				if (checkServer(ip)) {
					getGame(ip).report(info);
				} else {
					//add to the table					
					ServerInfo si = new ServerInfo(this, ip, info);
					servers.add(si);
					games.put(ip, si);
				}
			}
		} else {
			c.player.receive(object);
		}
	}

	public void disconnected(User p) {
		while (accessDiscQueue(false, false)) {

		}
		if (p != null) {
			discQueue.add(p);
		}
		//if(p.conn != null) {
		//	p.conn.close();
		//	p.conn = null;
		//}
	}

	public void processPacketQueue() {
		accessPacketQueue(true, true);
		try {

			while (!packetQueue.isEmpty()) {
				processPacket(packetQueue.remove());
			}

		} catch (Exception e) {

		} finally {
		}
		accessPacketQueue(true, false);
	}

	public void processDiscQueue() {
		accessDiscQueue(true, true);
		while (!discQueue.isEmpty()) {
			User p = discQueue.remove();
			if (p != null) { // Note that disconnected is from server detecting
								// a halt in connection
				p.disconnected(); // but that we will be using disconnect() for
									// client-requested termination
			}
		}
		accessDiscQueue(true, false);
	}

	public void update() {
		List<User> drops = new LinkedList<User>();
		List<ServerInfo> servDrops = new LinkedList<ServerInfo>();
		tick = System.currentTimeMillis();
		for (ServerInfo serv : servers) {
			if (tick > serv.lastUpdate + 5000) {
				games.remove(serv.ip);
				servDrops.add(serv);
			}
		}
		for (ServerInfo s : servDrops) {
			servers.remove(s);
		}		
		processPacketQueue();
		if (tick > stepStamp) {
			processDiscQueue();
			for (User u : users) {
				if (u.remove) {
					drops.add(u);
				} else {
					for(ServerInfo s : servers) {
						u.conn.sendTCP(s.getServerData());
					}
				}
			}
			for (User u : drops) {
				users.remove(u);
			}
			stepStamp = tick + 100;
		}
	}

	public void login(LobbyConnection c, String user, String pass) {
		if (verifyUser(c, user, pass)) {
			c.index = server.freeConnection();
			if (c.index >= 0) {
				server.connections[c.index] = c;
				c.player = joinLobby(c, user);
			} else {
				LobbyJoinError j = new LobbyJoinError();
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

	public void newAccount(LobbyConnection c, String user, String pass) {
		// add passes and accounts later ya dig
	}

	public boolean validName(LobbyConnection c, String user) {
		// first make sure its correct length
		if (user == null) {
			return false;
		} // dont even bother sending a packet because this is hacker activity
		if (!(user.length() >= 3 && user.length() <= 15)) {
			return false;
		} // same
		// now make sure nobody else is using it
		for (User p : users) {
			if (p.name.toUpperCase().equals(user.toUpperCase())) {
				LobbyJoinError j = new LobbyJoinError();
				j.s = "That name is already in use";
				c.sendTCP(j);
				return false;
			}
		}
		return true; // good to go m8
	}

	public boolean verifyUser(LobbyConnection c, String user, String pass) {
		return (validName(c, user));
		// add passes and accounts later ya dig
	}

	public void sendAll(Object obj, boolean UDP) {
		for (User p : users) {
			if (p.joined && p.conn != null) {
				if(p.conn.isConnected()) {
					if (UDP) {
						p.conn.sendUDP(obj);
					} else {
						p.conn.sendTCP(obj);
					}
				}
			}
		}
	}

	public void sendAllBut(User but, Object obj, boolean UDP) {
		for (User p : users) {
			if (p != but) {
				if (p.joined) {
					if (UDP) {
						p.conn.sendUDP(obj);
					} else {
						p.conn.sendTCP(obj);
					}
				}
			}
		}
	}

	public void sendTo(User p, Object obj) {
		try {
			p.conn.sendTCP(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	
}
