package com.bbg.client;

import com.bbg.shared.Entities;
import com.bbg.shared.Network.LoadoutData;

public class OptionsXML {
	public String version = getVersion();
	public int fullScreen = 0;
	public int music = 30;
	public int sound = 100;
	public int bind[] = new int[100];
	public LoadoutData load[] = new LoadoutData[5];
	public int curLoad = 0;
	
	public boolean beenControls = false;
	public boolean beenLoadout = false;
	
	public static final int numBinds = 24; //ADJUST THIS SO CLIENT KNOWS HOW MANY TO SHOW
	
	public static String getVersion() {
		return "d";
	}
	
	public OptionsXML() {
		for(int i = 0; i < 100; i++) {
			bind[i] = 0;
		}
		bind[0] = 51;
		bind[1] = 47;
		bind[2] = 29;
		bind[3] = 32;
		bind[4] = 256;
		bind[5] = 45;
		bind[6] = 131;
		bind[7] = 66;
		bind[8] = 61;
		bind[9] = 59;
		bind[10] = 40;
		bind[11] = 36;
		bind[12] = 54;
		bind[13] = 129;
		bind[14] = 92;
		bind[15] = 93;
		bind[16] = 31;
		bind[17] = 43;
		bind[18] = 37;
		bind[19] = 46;
		bind[20] = 39;
		bind[21] = 71;
		bind[22] = 72;
		bind[23] = 41;
		for(int i = 0; i < 5; i++) {
			load[i] = new LoadoutData();
			load[i].armor = i % 3;
			load[i].col = Entities.randomBrightColor();
		}		
	}
	
}
