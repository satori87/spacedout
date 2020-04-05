package com.bbg.shared;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Body;
import com.bbg.shared.MapDef.BaseData;
import com.bbg.shared.Vector.Coord;

public class Wall {

	public float x = 0, y = 0;
	public float d = 0;
	public int shape = 0;
	public int thickness = 0;
	public Body body;

	public Object entity;
	public boolean processed = false;
	public Color col = Entities.randomBrightColor();

	public boolean blink = false;
	public long blinkStamp = 0;
	public List<Object> contacts = new LinkedList<Object>();

	public Wall(float x, float y, float d, int shape, int thick, Color col) {
		this.x = x;
		this.y = y;
		this.d = d;
		this.shape = shape;
		this.thickness = thick;
		this.col = col;
	}

	public void init() {
		contacts = new LinkedList<Object>();
		col = Entities.randomBrightColor();
		blink = false;
		blinkStamp = 0;
	}

	public void update(long tick) {
		if (blink) {
			if (tick > blinkStamp) {
				blink = false;
			}
		}
	}

	public static int getWallThick(int type) {
		switch (type) {
		default:
			return 6;
		case 0:
			return 3;
		case 1:
			return 6;
		}
	}

	public static int getWallHeight(int type) {
		switch (type) {
		default:
			return 6;
		case 0:
			return 3;
		case 1:
			return 6;
		}
	}

	public static int getWallWidth(int type) {
		switch (type) {
		default:
			return 100;
		case 0:
			return 100;
		case 1:
			return 100;
		case 2:
			return 100;
		}
	}

	public static void loadWalls(int type, MapDef map, List<Wall> walls) {
		float t = 0;
		Wall a;
		walls.clear();
		if (type != 2) {
			for (BaseData bd : map.bases) {
				Color col = Entities.randomBrightColor();
				t = (float) Wall.getWallHeight(1) / 2f;
				a = new Wall(bd.x, bd.y - 50 + t, 0, 0, 1, col);
				walls.add(a);
				a = new Wall(bd.x, bd.y + 50 - t, 0, 0, 1, col);
				walls.add(a);
				a = new Wall(bd.x - 50 + t, bd.y, 90, 0, 1, col);
				walls.add(a);
				a = new Wall(bd.x + 50 - t, bd.y, 90, 0, 1, col);
				walls.add(a);
			}
		}
		for (Rect r : map.rects) {
			t = (float)Wall.getWallThick(r.thickness)/2f;
			for(int x = r.x1; x < r.x2; x+=100) {
				a = new Wall(x+50, r.y1+t, 0, 0, r.thickness, Entities.randomBrightColor());
				walls.add(a);
				a = new Wall(x+50, r.y2-t, 0, 0, r.thickness, Entities.randomBrightColor());
				walls.add(a);
			}
			for(int y = r.y1; y < r.y2; y+=100) {
				a = new Wall(r.x1+t, y+50, 90, 0, r.thickness, Entities.randomBrightColor());
				walls.add(a);
				a = new Wall(r.x2-t, y+50, 90, 0, r.thickness, Entities.randomBrightColor());
				walls.add(a);
			}
			
		}
		
		for (Wall w : map.walls) {
			switch (w.shape) {
			case 0: // line
				Wall b = new Wall(w.x, w.y, w.d, w.shape, w.thickness, w.col);
				walls.add(b);
				break;
			case 20: // standard box
				addBox(w.x, w.y, w.d, w.thickness, w.col, walls);
				break;
			case 21: // cross
				addCross(w.x, w.y, w.d, w.thickness, w.col, walls);
				break;
			case 22: // equilateral triangle
				addTriangle(w.x, w.y, w.d, w.thickness, w.col, walls);
				break;
			case 23: // two lines
				a = new Wall(w.x + 50, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				a = new Wall(w.x - 50, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				break;
			case 24: // 3 lines
				a = new Wall(w.x, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				a = new Wall(w.x + 100, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				a = new Wall(w.x - 100, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				break;
			case 25: // 4 lines
				a = new Wall(w.x + 50, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				a = new Wall(w.x - 50, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				a = new Wall(w.x + 150, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				a = new Wall(w.x - 150, w.y, w.d, 0, w.thickness, w.col);
				wallRotate(w, a, w.d);
				walls.add(a);
				break;
			case 100: // base circle, triangles
				addTriangle(w.x, w.y - 300, w.d + 0, w.thickness, w.col, walls);
				addTriangle(w.x + 213, w.y - 213, w.d + 45, w.thickness, w.col, walls);
				addTriangle(w.x + 300, w.y, w.d + 90, w.thickness, w.col, walls);
				addTriangle(w.x + 213, w.y + 213, w.d + 135, w.thickness, w.col, walls);
				addTriangle(w.x, w.y + 300, w.d + 180, w.thickness, w.col, walls);
				addTriangle(w.x - 213, w.y + 213, w.d + 90, w.thickness, w.col, walls);
				addTriangle(w.x - 300, w.y, w.d + 270, w.thickness, w.col, walls);
				addTriangle(w.x - 213, w.y - 213, w.d + 90, w.thickness, w.col, walls);
				break;
			case 101: // base circle, boxes
				addBox(w.x, w.y - 300, w.d + 90, w.thickness, w.col, walls);
				addBox(w.x + 213, w.y - 213, w.d + 45, w.thickness, w.col, walls);
				addBox(w.x + 300, w.y, w.d + 90, w.thickness, w.col, walls);
				addBox(w.x + 213, w.y + 213, w.d + 45, w.thickness, w.col, walls);
				addBox(w.x, w.y + 300, w.d + 90, w.thickness, w.col, walls);
				addBox(w.x - 213, w.y + 213, w.d + 45, w.thickness, w.col, walls);
				addBox(w.x - 300, w.y, w.d + 90, w.thickness, w.col, walls);
				addBox(w.x - 213, w.y - 213, w.d + 45, w.thickness, w.col, walls);
				break;
			case 102: // base circle, crosses
				addCross(w.x, w.y - 300, w.d + 0, w.thickness, w.col, walls);
				addCross(w.x + 213, w.y - 213, w.d + 45, w.thickness, w.col, walls);
				addCross(w.x + 300, w.y, w.d + 90, w.thickness, w.col, walls);
				addCross(w.x + 213, w.y + 213, w.d + 45, w.thickness, w.col, walls);
				addCross(w.x, w.y + 300, w.d + 90, w.thickness, w.col, walls);
				addCross(w.x - 213, w.y + 213, w.d + 45, w.thickness, w.col, walls);
				addCross(w.x - 300, w.y, w.d + 90, w.thickness, w.col, walls);
				addCross(w.x - 213, w.y - 213, w.d + 45, w.thickness, w.col, walls);
				break;

			}
		}
	}

	static void addCross(float x, float y, float d, int thickness, Color col, List<Wall> walls) {
		Wall a = new Wall(x, y, d, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
		a = new Wall(x, y, d + 90, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
	}

	static void addTriangle(float x, float y, float d, int thickness, Color col, List<Wall> walls) {
		float t = 0;
		Wall a;
		t = (float) Wall.getWallHeight(thickness) / 2f;
		a = new Wall(x, y + 43, d, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
		a = new Wall(x + 27 - t, y + t - 1, d + 60, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
		a = new Wall(x - 27 + t, y + t - 1, d - 60, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
	}

	static void addBox(float x, float y, float d, int thickness, Color col, List<Wall> walls) {
		float t = 0;
		Wall a;
		t = (float) Wall.getWallHeight(thickness) / 2f;
		a = new Wall(x, y - 50 + t, d, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
		a = new Wall(x, y + 50 - t, d, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
		a = new Wall(x - 50 + t, y, d + 90, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
		a = new Wall(x + 50 - t, y, d + 90, 0, thickness, col);
		wallRotate(x, y, a, d);
		walls.add(a);
	}

	static void wallRotate(Wall w, Wall a, float deg) {
		float cX = w.x;
		float cY = w.y;
		Coord c = Vector.rot(a.x, a.y, cX, cY, deg);
		a.x = c.x;
		a.y = c.y;
	}

	static void wallRotate(float x, float y, Wall a, float deg) {
		float cX = x;
		float cY = y;
		Coord c = Vector.rot(a.x, a.y, cX, cY, deg);
		a.x = c.x;
		a.y = c.y;
	}

}