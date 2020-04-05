package com.bbg.shared;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;

public class MapDef {
	public int width = 0;
	public int height = 0;
	public String name = "map";
	public List<Wall> walls = new LinkedList<Wall>();
	
	public List<BaseData> bases = new LinkedList<BaseData>();
	
	public List<Rect> rects = new LinkedList<Rect>();
	

	public  class BaseData {
		public float x = 0;
		public float y = 0;
		public int team = 0;
		public int index = 0;
		public float scale = 0.7f;
		public float rotSpeed = 0.001f;
	}

	public void init() {
		for (int x = (-width / 2) + 50; x <= (width / 2) - 50; x += 100) {
			Wall b = new Wall(x, -height / 2, 0, 0, 0, Color.RED);
			walls.add(b);
			b = new Wall(x, (height / 2), 0, 0, 0, Color.RED);
			walls.add(b);
		}
		for (int y = (-height / 2) + 50; y <= (height / 2) - 50; y += 100) {
			Wall b = new Wall(-width / 2, y, 90, 0, 0, Color.RED);
			walls.add(b);
			b = new Wall((width / 2) - 3, y, 90, 0, 0, Color.RED);
			walls.add(b);
		}
		for (Wall w : walls) {
			w.init();
		}
	}

}
