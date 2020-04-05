package com.bbg.shared;

public class Rect {
	
	public int x1, x2, y1, y2, thickness;

	public Rect() {
		x1 = 0;
		y1 = 0;
		x2 = 0;
		y2 = 0;
		thickness = 1;
	}

	public Rect(int x1, int y1, int x2, int y2, int thick) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		thickness = thick;
	}

}
