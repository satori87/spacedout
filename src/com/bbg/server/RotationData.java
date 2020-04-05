package com.bbg.server;

public class RotationData {
	public int map = 0;
	public int type = 0;
	public int time = 0;
	public int score = 0;
	
	public RotationData(int m, int ty, int ti, int s) {
		map = m;
		type = ty;
		time = ti;
		score = s;
	}	
	
	public RotationData(String m, String ty, String ti, String s) {
		map = Integer.parseInt(m);
		type = Integer.parseInt(ty);
		time = Integer.parseInt(ti);
		score = Integer.parseInt(s);
	}
}
