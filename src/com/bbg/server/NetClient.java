package com.bbg.server;

import java.io.IOException;

import com.bbg.lobby.LobbyPrefs;
import com.bbg.shared.Shared;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

public class NetClient {

	Client client;
	Game game;
	String host;
	Connection conn;
	public NetClient(Game game) {
		Log.set(Log.LEVEL_TRACE);
		this.game = game;
		client = new Client(32768, 4096);
		new Thread(client).start();
		com.bbg.lobby.LobbyNetwork.register(client);
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
		game.receiveData(null, object);
	}

	public void clientConnected() {
		game.connectedLobby();

	}

	public void clientDisconnected() {
		game.disconnectedLobby();
	}

	public void clientFailedConnect() {
		game.failedConnectLobby();
	}

}
