package com.bbg.client;

import com.badlogic.gdx.graphics.Color;
import com.bbg.shared.Wall;

public class WallEntity extends Entity {
	
	public Wall wall;

	public WallEntity(Wall wall) {
		this.wall = wall;
	}

	public void render(GameScreen screen) {
		float r = wall.d;
		//int b = 0;
		//if (!wall.contacts.isEmpty() || wall.blink) {
			//b = 1;
		//}
		screen.batcher.setColor(wall.col);
		screen.drawRegion(AssetLoader.getSprite("line" + Integer.toString(wall.thickness)), wall.x, wall.y, true, r+90, 1.0f);
		Color g = new Color(wall.col);
		g.a = screen.scene.glowVal;
		screen.batcher.setColor(g);
		screen.drawRegion(AssetLoader.getSprite("line" + Integer.toString(wall.thickness) + "g"), wall.x, wall.y, true, r+90, 1.0f);
		screen.batcher.setColor(Color.WHITE);
	}
	

}
