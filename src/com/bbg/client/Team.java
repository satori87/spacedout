package com.bbg.client;

import com.badlogic.gdx.graphics.Color;
import com.bbg.shared.Entities;
import com.bbg.shared.Shared;

public class Team {

	Scene scene;

	public String name = "";
	public int index = 0;
	public int score = 0;
	private Color color = Entities.randomBrightColor();

	public Team(Scene scene, int index, String name, int score) {
		this.scene = scene;
		this.index = index;
		this.name = name;
		this.score = score;
		switch (index) {
		case 0:
			color = Shared.xml.t1Col;
			break;
		case 1:
			color = Shared.xml.t2Col;
			break;
		default:
			color = Shared.xml.t1Col;
			break;
		}
	}

	public Color col() {
		Color c = new Color();
		c.r = (float)color.r / 256f;
		c.g = (float)color.g / 256f;
		c.b = (float)color.b / 256f;
		c.a = color.a;
		return c;
	}
	
}
