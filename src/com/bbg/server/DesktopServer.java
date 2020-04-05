package com.bbg.server;


import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JLabel;
import com.bbg.shared.Network;
import com.bbg.shared.Shared;
import com.bbg.shared.SharedXML;
import com.bbg.server.upnp.Punch;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class DesktopServer {
	
	Server server;
	Game game;
	GameConnection[] connections;
	boolean running = true;
	public String shared;
	long lastLoopTime = System.currentTimeMillis();

	public DesktopServer () throws IOException {
		
		XStream xstream = new XStream(new StaxDriver());
		File f = new File("assets/server.xml");
		if (f.exists()) {
			Config.xml = (ConfigXML) xstream.fromXML(f);
		} else {           
			System.exit(0);
		}
		f = new File("assets/shared.xml");
		if (f.exists()) {
			Shared.xml = (SharedXML) xstream.fromXML(f);
		} else {
			System.exit(0);
		}
		shared = new String(Files.readAllBytes(Paths.get("assets/shared.xml")));
		
		game = new Game(this);	
		if(Config.xml.uPNP) {Punch.configPNP(Shared.tcpPort, Shared.udpPort);}
		connections = new GameConnection[Shared.maxPlayers];
		
		server = new Server(16384, 4096) {
			protected Connection newConnection () {
				return new GameConnection();
			}
		};
		Network.register(server);
		server.addListener(new Listener() {
			public void received (Connection c, Object object) {				
				GameConnection connection = (GameConnection)c;
				receiveData(connection, object);					
			}
			public synchronized void disconnected (Connection c) {
				GameConnection connection = (GameConnection)c;
				if (connection.player != null) {
					game.disconnected(connection.player);
				}
			}
		});
		server.bind(Shared.tcpPort, Shared.udpPort);
		server.start();

		// Open a window to provide an easy way to stop the server.
		JFrame frame = new JFrame("Server");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed (WindowEvent evt) {
				server.stop();
				running = false;
			}
		});
		frame.getContentPane().add(new JLabel("Be happy instead of sad."));
		frame.setSize(320, 200);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		serverLoop();
	}

	// This holds per connection state.
	public static class GameConnection extends Connection {
		public Player player;
		public int index = -1;
		public int packets = 0;
	}

	public static void main (String[] args) throws IOException {		
		new DesktopServer();
	}
	

	public int freeConnection() {
		for(int i = 0; i < Shared.maxPlayers; i++) {
			if (connections[i] == null) {
				return i;
			} else {
				if(!connections[i].isConnected()) {
					return i;
				}
			}
		}
		return -1; //full!
	}
	
	public void serverLoop() {
		game.start();
		while (running) {
			lastLoopTime  = System.currentTimeMillis();
			game.update();
		    try {
		    	int timeStep = Config.xml.loopTime;
		    	long t = timeStep - (System.currentTimeMillis()-lastLoopTime);
		    	if(t > timeStep) {t = timeStep;}
		    	if(t < 1) {t = 1;}
		    	Thread.sleep(t);
		    } catch (Exception ex) {
		    	System.out.println("Caught: " + ex.toString());
		    } finally {};
		}
		System.exit(0);
	}

	public void receiveData(GameConnection c, Object object) {
		game.receiveData(c, object);
	}
	
}