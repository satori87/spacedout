package com.bbg.shared;

import java.io.File;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class Entities {

	public static WeaponDef[] weapons = new WeaponDef[7];
	public static MapDef[] maps = new MapDef[16];
	public static MapDef[] emaps = new MapDef[200];
	public static int mapCount = 0;

	public static ItemDef[] items = new ItemDef[4];

	public static void init() {
		mapCount = 0;
		XStream xstream = new XStream(new StaxDriver());

		File f = new File("assets/maps.xml");
		if (f.exists()) {
			maps = (MapDef[]) xstream.fromXML(f);
			for (MapDef m : maps) {
				m.init();
				mapCount++;
			}
		} else {
			System.exit(0);
		}
		f = new File("assets/weapons.xml");
		if (f.exists()) {
			weapons = (WeaponDef[]) xstream.fromXML(f);
		} else {
			System.exit(0);
		}
		f = new File("assets/items.xml");
		if (f.exists()) {
			// maps = (Map[]) xstream.fromXML(f);
			// for(Map m : maps) {
			// m.init();
			// }
		} else {
			// System.exit(0);
		}
		for (int i = 0; i < 4; i++) {
			items[i] = new ItemDef(i);
		}
	}
	
	public static void editInit() {
		mapCount = 0;
		XStream xstream = new XStream(new StaxDriver());
		File f = new File("assets/maps.xml");
		if (f.exists()) {
			maps = (MapDef[]) xstream.fromXML(f);
			for (MapDef m : maps) {
				m.init();
				mapCount++;
			}
		} else {
			System.out.println("no maps file");
			System.exit(0);
		}
		f = new File("assets/emaps.xml");
		if (f.exists()) {
			System.out.println("yo22");
			emaps = (MapDef[]) xstream.fromXML(f);
			for (MapDef m : emaps) {
				m.init();
			}
		} else {
			System.out.println("yo2");
			emaps = new MapDef[200];
			for (int i = 0; i < 200; i++) {
				emaps[i] = new MapDef();
				emaps[i].name = "empty";
				emaps[i].width = 500;
				emaps[i].height = 500;
				
			}
			FileHandle fh = Gdx.files.local("assets/emaps.xml");
			String s = xstream.toXML(emaps);
			fh.writeString(s, false);
			System.exit(0);
		}
	}

	public static boolean validWeapon(int i) {
		System.out.println(weapons.length);
		return (i < weapons.length);
	}

	public static boolean validItem(int i) {
		System.out.println(items.length);
		return (i < items.length);
	}

	public static boolean isColorBright(Color c) {
		float r, g, b;
		float[] hsv = new float[3];
		r = c.r * 256;
		g = c.g * 256;
		b = c.b * 256;
		java.awt.Color.RGBtoHSB((int) r, (int) g, (int) b, hsv);
		return (hsv[1] >= 0.95f && hsv[2] >= 0.95f);
	}

	public static Color alterColor(Color c, float x) {
		Color col = new Color(c);
		col.r = col.r - x + (float) (Math.random() * x * 2);
		col.g = col.g - x + (float) (Math.random() * x * 2);
		col.b = col.b - x + (float) (Math.random() * x * 2);
		if (col.r > 1) {
			col.r = 1;
		}
		if (col.g > 1) {
			col.g = 1;
		}
		if (col.b > 1) {
			col.b = 1;
		}
		if (col.r < 0) {
			col.r = 0;
		}
		if (col.g < 0) {
			col.g = 0;
		}
		if (col.b < 0) {
			col.b = 0;
		}
		return col;
	}

	public static Color randomBrightColor() {
		float r, g, b;
		// float br;
		float[] hsv = new float[3];
		float sat = 0;
		do {
			r = Random.getInt(256);
			g = Random.getInt(256);
			b = Random.getInt(256);
			java.awt.Color.RGBtoHSB((int) r, (int) g, (int) b, hsv);
			sat = hsv[1];

		} while (sat < 0.95f || hsv[2] < 0.95f || hsv[2] > 1.0f);
		return getColor((int) r, (int) g, (int) b, 1);
	}

	public static Color getColor(int r, int g, int b, float a) {
		float fr = (float) r;
		float fg = (float) g;
		float fb = (float) b;
		return new Color(fr / 256f, fg / 256f, fb / 256f, a);
	}

	public static CircleShape[] getCircleArray(float radius, float physicsScale) {
		CircleShape circle[] = new CircleShape[1];
		circle[0] = new CircleShape();
		circle[0].setRadius(radius / physicsScale);
		return circle;
	}

	public static CircleShape getCircle(float radius, float physicsScale) {
		CircleShape circle = new CircleShape();
		circle.setRadius(radius / physicsScale);
		return circle;
	}

	public static String getMapName(int a) {
		return maps[a].name;
	}

}