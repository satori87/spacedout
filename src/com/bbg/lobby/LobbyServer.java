package com.bbg.lobby;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class LobbyServer {

	Server server;
	Lobby lobby;
	LobbyConnection[] connections;
	boolean running = true;
	long lastLoopTime = System.currentTimeMillis();

	public LobbyServer() throws IOException {

		lobby = new Lobby(this);

		connections = new LobbyConnection[LobbyPrefs.maxUsers];

		server = new Server(16384, 4096) {
			protected Connection newConnection() {
				return new LobbyConnection();
			}
		};
		LobbyNetwork.register(server);
		com.bbg.shared.Network.register(server);
		server.addListener(new Listener() {
			public void received(Connection c, Object object) {
				LobbyConnection connection = (LobbyConnection) c;
				receiveData(connection, object);
			}

			public synchronized void disconnected(Connection c) {
				LobbyConnection connection = (LobbyConnection) c;
				if (connection.player != null) {
					lobby.disconnected(connection.player);
				}
			}
		});
		server.bind(LobbyPrefs.port);
		server.start();

		// Open a window to provide an easy way to stop the server.
		JFrame frame = new JFrame("Lobby");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent evt) {
				server.stop();
				running = false;
			}
		});
		frame.getContentPane().add(new JLabel("Just remember to relax."));
		frame.setSize(500, 100);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		serverLoop();
	}

	// This holds per connection state.
	public static class LobbyConnection extends Connection {
		public User player;
		public int index = -1;
	}

	public static void main(String[] args) throws IOException {
		new LobbyServer();
	}

	public int freeConnection() {
		System.out.println("w1");
		for (int i = 0; i < LobbyPrefs.maxUsers; i++) {
			if (connections[i] == null) {
				System.out.println(i);
				return i;
			} else {
				if(!connections[i].isConnected()) {
					return i;
				}
			}
		}

		System.out.println("badbad");
		return -1; // full!
	}

	public void serverLoop() {
		lobby.start();
		while (running) {
			lastLoopTime = System.currentTimeMillis();
			lobby.update();
			try {
				int timeStep = LobbyPrefs.loopTime;
				long t = timeStep - (System.currentTimeMillis() - lastLoopTime);
				if (t > timeStep) {
					t = timeStep;
				}
				if (t < 1) {
					t = 1;
				}
				Thread.sleep(t);
			} catch (Exception ex) {
				System.out.println("Caught: " + ex.toString());
			}
		}
		System.exit(0);
	}

	public void receiveData(LobbyConnection c, Object object) {
		lobby.receiveData(c, object);
	}

}