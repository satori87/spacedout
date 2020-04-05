package com.bbg.client;

import java.io.IOException;

import com.bbg.lobby.LobbyPrefs;
import com.bbg.shared.Shared;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

public class NetClient {

	Client client;
	Scene scene;
	GameScreen screen;
	String host;
	Connection conn;
	public boolean lobby = false;

	public NetClient(GameScreen screen, boolean lobby) {
		this.lobby = lobby;
		Log.set(Log.LEVEL_DEBUG);
		this.screen = screen;
		this.scene = screen.scene;
		client = new Client(32768, 4096);
		new Thread(client).start();

		if (lobby) {
			com.bbg.lobby.LobbyNetwork.register(client);
		} else {
			com.bbg.shared.Network.register(client);
		}
		client.addListener(new Listener() {
			public void connected(Connection connection) {
				conn = connection;
				clientConnected();
			}

			public void received(Connection connection, Object object) {
				receiveData(object);
			}

			public void disconnected(Connection connection) {
				clientDisconnected();
			}
		});
	}

	public void connectGame(String hostname) {
		host = hostname;
		new Thread("Connect") {
			public void run() {
				try {
					client.connect(5000, host, Shared.tcpPort, Shared.udpPort);
				} catch (IOException ex) {
					clientFailedConnect();
				}
			}
		}.start();
	}

	public void connectLobby(String hostname) {
		host = hostname;
		new Thread("Connect") {
			public void run() {
				try {
					client.connect(5000, host, LobbyPrefs.port);
				} catch (IOException ex) {
					clientFailedConnect();
				}
			}
		}.start();
	}

	public void receiveData(Object object) {
		screen.receiveData(object);
	}

	public void clientConnected() {
		if (lobby) {
			screen.connectedLobby();
		} else {
			screen.connectedGame();
		}
	}

	public void clientDisconnected() {
		if (lobby) {
			screen.disconnectedLobby();
		} else {
			screen.disconnectedGame();
		}
	}

	public void clientFailedConnect() {
		if (lobby) {
			screen.failedConnectLobby();
		} else {
			screen.failedConnectGame();
		}
	}

}
