package com.bbg.shared;

public class Vector {

	public float xChange;
	public float yChange;
	public float direction;
	public float intensity;

	public Vector(float direction, float intensity) {
		this.direction = direction;
		this.intensity = intensity;
		xChange = (float) (Math.cos(direction - Math.toRadians(90)) * intensity);
		yChange = (float) (Math.sin(direction - Math.toRadians(90)) * intensity);
	}

	public static float distance(float X1, float Y1, float X2, float Y2) {
		return (float) Math.hypot(X2 - X1, Y2 - Y1);
	}

	public static float fixDir(float dir) {
		while (dir < 0) {
			dir += Math.toRadians(360);
		}
		while (dir >= Math.toRadians(360)) {
			dir -= Math.toRadians(360);
		}
		return dir;
	}


	
	public static Vector byChange(float xChange, float yChange) {
		double theta = Math.atan2(yChange, xChange);
		theta += Math.PI / 2.0;
		return new Vector((float) theta, (float) distance(0, 0, xChange, yChange));
	}

	public static float randomDir() {
		return (float) (Math.random() * Math.toRadians(360));
	}

	public static class Coord {
		public float x, y;
	}
	
	public static Coord rot(float point2x, float point2y, float centerX, float centerY, float deg) {
		double x = Math.toRadians(deg);
		Coord c = new Coord();
		c.x = (float)(centerX + (point2x-centerX)*Math.cos(x) - (point2y-centerY)*Math.sin(x));
		c.y = (float)(centerY + (point2x-centerX)*Math.sin(x) + (point2y-centerY)*Math.cos(x));
		return c;
	}
	
}