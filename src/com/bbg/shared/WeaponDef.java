package com.bbg.shared;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Shape;

public class WeaponDef {
	public String name = "gun";
	public String killVerb = "fragged";
	public float speed = 0.1f;
	public int fireTime = 100;
	public List<Float> spread = new LinkedList<Float>();
	public int reloadTime = 1;
	public int clip = 1;
	public boolean hasClip = true;
	public int type = 0;
	public boolean forLight = true;
	public boolean forMedium = true;
	public boolean forHeavy = true;
	public boolean primary = true;
	public int head = 0; //sound
	public int body = 0; //sound
	public Color color = Color.WHITE;
	public int sprite = 0; //bullet sprite
	public float radius = 0;
	public float scale = 0;
	public int dam = 0;
	public float expScale = 0;
	public float expSpeed = 0;
	public float expCount = 10;
	public int expSnd = 0;
	public float rot = 0;

	public static Shape[] getBulletShape(int type, float physicsScale) {
		return Entities.getCircleArray(Entities.weapons[type].radius, physicsScale);
	}

	public Color col() {
		Color c = new Color();
		c.r = (float)color.r / 256f;
		c.g = (float)color.g / 256f;
		c.b = (float)color.b / 256f;
		c.a = color.a;
		return c;
	}
	
	public static int getBulletW(int b) {
		switch (b) {
		default:
			return 1;
		case 0:
			return 37;
		case 1:
			return 37;
		case 2:
			return 23;
		case 3:
			return 31;
		case 4:
			return 31;
		case 5:
			return 29;
		case 6:
			return 59;
		case 7:
			return 77;
		case 8:
			return 95;
		case 9:
			return 39;
		}
	}
	
}
