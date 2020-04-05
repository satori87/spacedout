package com.bbg.client;

import com.bbg.lobby.LobbyNetwork.JoinLobby;
import com.bbg.lobby.LobbyNetwork.LobbyChatMsg;
import com.bbg.lobby.LobbyNetwork.LobbySendChat;
import com.bbg.lobby.LobbyNetwork.ServerData;
import com.bbg.lobby.LobbyNetwork.UserData;
import java.util.Hashtable;
import com.bbg.lobby.LobbyPrefs;
import com.bbg.shared.Entities;
import com.badlogic.gdx.graphics.Color;
import com.bbg.client.ServerInfo;
import com.bbg.client.ServerInfo.GameListData;
import com.bbg.client.User.UserListData;

public class Lobby {

	int inde = 0;
	GameScreen screen;
	String curChat = "";
	public String[] chatLog = new String[400];
	public Color[] chatLogColor = new Color[400];
	public User[] users = new User[LobbyPrefs.maxUsers];
	public ServerInfo[] servers = new ServerInfo[LobbyPrefs.maxGames];
	public Hashtable<String, Integer> games = new Hashtable<String, Integer>();
	public UserListData[] userList = new UserListData[LobbyPrefs.maxUsers];
	public GameListData[] gameList = new GameListData[LobbyPrefs.maxGames];

	public int selGame = -1;
	public int selUser = -1;
	public int chatStart = 0;
	public int gameStart = 0;
	public int userStart = 0;
	
	public long lastClick = 0;
	public int lastClickIndex = -1;

	public Lobby(GameScreen screen) {
		this.screen = screen;
		for (int i = 0; i < 400; i++) {
			chatLog[i] = "";
			chatLogColor[i] = Color.WHITE;
		}
	}

	int freeServer() {
		for (int i = 0; i < LobbyPrefs.maxGames; i++) {
			if (servers[i] == null) {
				return i;
			} else if (servers[i].inUse == false) {
				return i;
			}
		}
		return -1;
	}

	public void sendChat(String s) {
		LobbySendChat lsc = new LobbySendChat();
		lsc.msg = s;
		screen.lobbynet.client.sendTCP(lsc);
	}

	public boolean checkServer(String ip) {
		return (getGame(ip) != null);
	}

	public ServerInfo getGame(String ip) {
		int i = getGameIndex(ip);
		if (i == -1) {
			return null;
		} else {
			return servers[getGameIndex(ip)];
		}
	}

	public int getGameIndex(String ip) {
		if (games.get(ip) != null) {
			return (Integer) games.get(ip);
		} else {
			return -1;
		}
	}

	void receiveLobby(Object object) {
		if (object instanceof JoinLobby) {
			JoinLobby j = (JoinLobby) object;
			curChat = "";
			int i = j.index;
			users[i] = new User(screen, i, j.name);
			for (UserData pd : j.users) {
				users[pd.index] = new User(screen, pd.index, pd.name);
			}
			for (ServerData sd : j.games) {
				if(sd.info.version.equals(screen.version)) {					
					int index = freeServer();
					if (index != -1) {
						servers[index] = new ServerInfo(screen, sd.ip, sd.info);
						games.put(sd.ip, index);
					}
				}
			}
			screen.gameState = 8;
			screen.lobby.selGame = -1;
		}
		if (object instanceof UserData) {
			UserData ud = (UserData) object;
			if (ud.leave) {
				if (users[ud.index] != null) {
					addChat(ud.name + " has left the lobby.", Color.WHITE);
					users[ud.index] = null;
				}
			} else if (ud.join) {
				users[ud.index] = new User(screen, ud.index, ud.name);
				addChat(ud.name + " has entered the lobby.", Color.WHITE);
			} else {
				users[ud.index] = new User(screen, ud.index, ud.name);
			}
		}
		if (object instanceof LobbyChatMsg) {
			LobbyChatMsg cc = (LobbyChatMsg) object;
			addChat(cc.msg, cc.color);
		}
		if (object instanceof ServerData) {
			ServerData sd = (ServerData) object;
			if(!(sd.info.version.equals(screen.version))) {
				return;
			}
			if (checkServer(sd.ip)) {
				getGame(sd.ip).report(sd.info);
			} else {
				boolean f = false;
				for(int i = 0; i < LobbyPrefs.maxGames; i++) {
					if(!f) {
						if(servers[i] == null) {
							servers[i] = new ServerInfo(screen, sd.ip, sd.info);
							games.put(sd.ip, i);
							f = true;
						} else if (servers[i].inUse == false) {
							servers[i] = new ServerInfo(screen, sd.ip, sd.info);
							games.put(sd.ip, i);
							f = true;
						}
					}
				}
			}
		}
	}

	void addChatLine(String s, Color c) {
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

	void addChat(String s, Color col) {
		String curLine = "";
		int lineW = 0;
		for (char c : s.toCharArray()) {
			curLine += c;
			lineW += AssetLoader.fontWidth[c];
			if (lineW >= 430) {
				addChatLine(curLine, col);
				curLine = "";
				lineW = 0;
			}
		}
		if (lineW > 0) {
			addChatLine(curLine, col);
		}
	}

	void update() {
		for (int i = 0; i < LobbyPrefs.maxGames; i++) {
			if (servers[i] != null) {
				if (servers[i].inUse) {
					if (screen.tick > servers[i].lastUpdate + 2000) {
						games.remove(servers[i].ip);
						servers[i] = null;
					}
				}
			}
		}
	}

	public void render() {
		screen.moveCameraTo(400, 300);

		screen.drawFrame(0, 0, 800, 600, true);

		screen.drawFont(0, 400, 30, "Server List", true, 1.5f);
		//screen.drawFrame(50, 50, 500, 212, false); // chat box
		//screen.drawFrame(50, 258, 700, 32, false); // cur chat box
		//screen.drawFrame(546, 50, 204, 212, false); // user box
		//drawUsers(userStart, 10);
		//drawChat(chatStart, 10);
		drawGames(gameStart, 13);
		//screen.drawFont(0, 400, 306, "Games", true, 1.5f);
		screen.drawFrame(50, 60, 700, 440, false);
	}

	public void drawChat(int start, int numLines) {
		int drawX = 60;
		int drawY = 60;
		screen.drawFont(0, drawX, drawY + numLines * 20 + 6, ":" + curChat, false, 1);
		for (int i = start; i < start + numLines; i++) {
			if (i < 400) {
				screen.drawFont(0, drawX, drawY + (numLines - i) * 20 - 22, chatLog[i], false, 1, chatLogColor[i]);
			}
		}
	}

	void getUserList() {
		int c = 0;
		userList = new UserListData[LobbyPrefs.maxUsers];
		for (int i = 0; i < LobbyPrefs.maxUsers; i++) {
			if (users[i] != null) {
				UserListData uld = new UserListData();
				uld.user = users[i];
				userList[c] = uld;
				c++;
			}
		}
	}

	void getGameList() {
		int c = 0;
		gameList = new GameListData[LobbyPrefs.maxGames];
		for (int i = 0; i < LobbyPrefs.maxGames; i++) {
			if (servers[i] != null) {
				GameListData gld = new GameListData();
				gld.game = servers[i];
				gameList[c] = gld;
				c++;
			}
		}
		if(selGame == -1 && c > 0) {
			selGame = 0;
		}
		if(selGame >= c) {
			selGame = c - 1;
		}
	}

	public void drawUsers(int start, int numLines) {
		int drawX = 560;
		int drawY = 70;
		getUserList();
		for (int i = start; i < start + numLines; i++) {
			if(i == selUser) {
				for(int x = drawX; x < drawX + 196; x += 2) {
					screen.drawRegion(AssetLoader.frame[10], x - 10, drawY + i * 20 - 6, false, 0, 1);
				}
			}
			if (i < LobbyPrefs.maxUsers) {
				if (userList[i] != null) {
					if (userList[i].user != null) {
						screen.drawFont(0, drawX, drawY + i * 20 - 3, userList[i].user.name, false, 1, Color.WHITE);
					}
				}
			}
		}
	}

	public void drawGames(int start, int numLines) {
		Color col = Color.WHITE;
		int drawX = 60;
		int drawY = 70;
		getGameList();
		for (int i = start; i < start + numLines; i++) {
			if(i == selGame) {
				for(int x = drawX; x < drawX + 696; x += 2) {
					//screen.drawRegion(AssetLoader.frame[10], x - 10, drawY + i * 20 - 6, false, 0, 1);
					screen.batcher.draw(AssetLoader.frameTex, x - 10, drawY + i * 20 - 6, 2, 20, 214, 42, 2, 20, false, true);
				}
			}
			if (i < LobbyPrefs.maxGames) {
				if (gameList[i] != null) {
					if (gameList[i].game != null) {
						screen.drawFont(0, drawX, drawY + i * 20 - 4, gameList[i].game.info.name, false, 1, col);
						screen.drawFont(0, drawX+140, drawY + i * 20 - 4, gameList[i].game.ip, false, 1, col);
						screen.drawFont(0, drawX+250, drawY + i * 20 - 4, gameList[i].game.getGameTypeString(), false, 1, col);
						screen.drawFont(0, drawX+440, drawY + i * 20 - 4, gameList[i].game.info.numPlayers + "/" + gameList[i].game.info.maxGamePlayers, false, 1, col);
						screen.drawFont(0, drawX+500, drawY + i * 20 - 4, Entities.getMapName(gameList[i].game.info.mapNum), false, 1, col);
					}
				}
			}
		}
	}

	public boolean inBox(int x, int y, int lowerX, int upperX, int lowerY, int upperY) {
		return (x >= lowerX && x <= upperX && y >= lowerY && y <= upperY);
	}
	
	public void checkClick(int x, int y) {
		if (inBox(x, y, 55, 746, 64, 494)) {
			int i = (y - 64) / 20;
			if(gameList[i] != null) {
				if(gameList[i].game.inUse) {
					if(selGame == i) {
						if(screen.tick - lastClick < 400) {
							//double click
							screen.scene.curStatus = "";
							screen.connectGame(gameList[i].game.ip);
						}
					}
					lastClick = screen.tick;
					selGame = i;
				}
			}
			//in box
		}
	}
	
}
