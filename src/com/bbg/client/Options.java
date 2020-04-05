package com.bbg.client;

import com.bbg.shared.Network.LoadoutData;

public class Options {
	public static String version = OptionsXML.getVersion();
	public static int fullScreen = 0;
	public static int music = 70;
	public static int sound = 70;
	public static int[] bind = new int[100];
	public static LoadoutData load[] = new LoadoutData[5];
	public static int curLoad = 0;
	
	public static boolean beenControls = false;
	public static boolean beenLoadout = false;
	
	public static void load(OptionsXML o) {
		fullScreen = o.fullScreen;
		music = o.music;
		sound = o.sound;
		for(int i = 0; i < 100; i++) {
			bind[i] = o.bind[i];
		}
		if(o.load != null) {
			for(int i = 0; i < 5; i++) {
				load[i] = o.load[i];
			}
			curLoad = o.curLoad;
		} else {
			load = new LoadoutData[5];
			for(int i = 0; i < 5; i++) {				
				load[i] = new LoadoutData();
			}
		}
		beenControls = o.beenControls;
		beenLoadout = o.beenLoadout;
	}
	
	public static float sound() {
		return (float)sound / 100f;
	}
	public static float music() {
		return (float)music / 200f;
	}
	
	

}
