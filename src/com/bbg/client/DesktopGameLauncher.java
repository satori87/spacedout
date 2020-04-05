package com.bbg.client;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

import javax.swing.JOptionPane;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class DesktopGameLauncher {
	public static void main(String[] arg) {
		AssetLoader.started = System.currentTimeMillis();
		XStream xstream = new XStream(new StaxDriver());
		File f = new File("option.xml");
		OptionsXML opt = new OptionsXML();
		if(f.exists()) {
			opt = (OptionsXML) xstream.fromXML(f);
		} else {
			AssetLoader.optionSave = true;
		}
		if(!opt.version.equals(OptionsXML.getVersion())) {
			JOptionPane.showMessageDialog(null, "Options version was changed. You may need to quit and restart the game");
			AssetLoader.optionSave = true;
		}
		Options.load(opt);	
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "SpacedOut";
		cfg.width = 800;
		cfg.height = 600;
		if (Options.fullScreen == 1) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			double width = screenSize.getWidth();
			double height = screenSize.getHeight();
			cfg.fullscreen = true;
			cfg.width = (int) Math.round(width);
			cfg.height = (int) Math.round(height);
			cfg.resizable = true;
		}
		
		// cfg.resizable = false;
		AssetLoader.app = new LwjglApplication(new gdxGame(), cfg);
		//System.out.println(System.currentTimeMillis() - AssetLoader.started);
	}
	

}
