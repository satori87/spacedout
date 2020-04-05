package com.bbg.shared;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;

public class ShipDef {


	static public void rotateToDesired(Body body, float dR, float d, float rot) {
		int s = 0;
		float mr = Vector.fixDir(dR); // rotate this much
		float pr = Vector.fixDir(d);
		float r2;
		float dist1, dist2;
		float angleChange = 0;
		float gdist = 0;
		float dd = pr - mr;
		if (dd > 0) { // player.direction is higher
			r2 = mr + (float) Math.toRadians(360);
			dist1 = pr - mr;
			dist2 = r2 - pr;
		} else { // mouseR is higher
			r2 = mr - (float) Math.toRadians(360);
			dist1 = pr - r2;
			dist2 = mr - pr;
		} // basically sandwich player direction between two R differences and
			// see which is closer
		if (dist1 > dist2) {
			gdist = dist2;
			s = 1;
		} else {
			gdist = dist1;
			s = -1;
		}
		if (Math.abs(gdist) > Math.PI) {
			angleChange = rot * s;
	//	} else if (Math.abs(gdist) > 0.05f) {
	//		angleChange = Math.abs(gdist)*s;
	//	} else if (Math.abs(gdist) > 0.01f) {
	//		angleChange = Math.abs(gdist/5)*s;
			
		} else {
			angleChange = rot * Math.abs(gdist/4) * s;
			//body.setTransform(body.getPosition(), dR);
			//angleChange = 0;
		}
		
		float sp = rot;
		if (sp > Math.abs(angleChange * 6.6f)) {
			sp = Math.abs(angleChange * 6.6f);
		}
		if(sp > rot) {
			sp = rot;
		}
		body.setAngularVelocity(sp * s);
	}

	public static Shape[] getShipShape(int t, float physicsScale, float scale) {
		Vector2[] v;
		PolygonShape[] shapes;
		switch (t) {
		default:
			return Entities.getCircleArray(1, physicsScale);
		case 1:
			shapes = new PolygonShape[1];
			// main body of ship
			v = new Vector2[3];
			v[0] = new Vector2(64, 39);
			v[1] = new Vector2(85, 78);
			v[2] = new Vector2(43, 78);
			ShipDef.fix(v, 64, physicsScale, scale);
			shapes[0] = new PolygonShape();
			shapes[0].set(v);
			return shapes;
		}
	}

	public static int getShipMaxHP(int a) {
		switch (a) {
		case 0:
			return 100;
		case 1:
			return Shared.xml.lightHP;
		case 2:
			return Shared.xml.mediumHP;
		default:
			return Shared.xml.heavyHP;
		}
	}

	public static void fix(Vector2[] vectors, float radius, float physicsScale, float scale) {
		// for user-friendliness, the vectors are defined in terms of 0,0
		// origin, then and then adjusted by the radius to give us
		// the correct transformations
		for (Vector2 v : vectors) {
			v.x -= radius;
			v.y -= radius;
			v.x /= physicsScale;
			v.y /= physicsScale;
			v.x *= scale;
			v.y *= scale;
		}
	}

	public static float getArmorDensity(int armor) {
		switch (armor) {
		default:
			return 1;
		case 0:
			return Shared.xml.lightDensity;
		case 1:
			return Shared.xml.mediumDensity;
		case 2:
			return Shared.xml.heavyDensity;
		}
	}

	public static float getShipDensity(int a) {
		return Shared.xml.shipDensity * getArmorDensity(a);
	}

	public static float getShipScale(int armor) {
		switch (armor) {
		default:
			return 1;
		case 0:
			return Shared.xml.lightScale;
		case 1:
			return Shared.xml.mediumScale;
		case 2:
			return Shared.xml.heavyScale;
		}
	}

	public static float getArmorThrust(int a) {
		switch (a) {
		default:
			return 1;
		case 0:
			return Shared.xml.lightThrust;
		case 1:
			return Shared.xml.mediumThrust;
		case 2:
			return Shared.xml.heavyThrust;
		}
	}

	public static float getShipRotationSpeed(int a) {
		float f = getArmorRotationSpeed(a);
		return f * Shared.xml.rotationSpeed;
	}

	public static float getArmorRotationSpeed(int a) {
		switch (a) {
		default:
			return 1;
		case 0:
			return Shared.xml.lightRotation;
		case 1:
			return Shared.xml.mediumRotation;
		case 2:
			return Shared.xml.heavyRotation;
		}
	}

	public static float getShipThrust(int b, int a, boolean turboing) {
		float f = getArmorThrust(a);
		float t = 0;
		switch (b) {
		default:
		case 0:
			t = Shared.xml.fThrust * f;
			break;
		case 1:
			t = Shared.xml.bThrust * f;
			break;
		case 2:
			t = Shared.xml.sThrust * f;
			break;
		}
		if (turboing) {
			t *= 2f;
		}
		return t;
	}

	public static float getShipSpeed(int a, boolean turbo) {
		float f = getArmorSpeed(a);
		if (turbo) {
			f += .4f;
		}
		return f;
	}

	public static float getArmorSpeed(int a) {
		switch (a) {
		case 0:
			return Shared.xml.lightSpeed;
		case 1:
			return Shared.xml.mediumSpeed;
		case 2:
			return Shared.xml.heavySpeed;
		default:
			return 1;
		}
	}

	public static int getArmorItems(int t) {
		switch (t) {
		default:
			return 5;
		case 0:
			return Shared.lightItems;
		case 1:
			return Shared.mediumItems;
		case 2:
			return Shared.heavyItems;
		}
	}

	public static int getArmorSecondaries(int t) {
		switch (t) {
		default:
			return 5;
		case 0:
			return Shared.lightSecondaries;
		case 1:
			return Shared.mediumSecondaries;
		case 2:
			return Shared.heavySecondaries;
		}
	}

	public static int getArmorPrimaries(int t) {
		switch (t) {
		default:
			return 5;
		case 0:
			return Shared.lightPrimaries;
		case 1:
			return Shared.mediumPrimaries;
		case 2:
			return Shared.heavyPrimaries;
		}
	}

	public static String getArmorName(int t) {
		switch (t) {
		default:
			return "Bear";
		case 0:
			return "Light";
		case 1:
			return "Medium";
		case 2:
			return "Heavy";
		}
	}

	public static boolean canWear(int armor, WeaponDef w) {
		switch (armor) {
		case 0:
			return w.forLight;
		case 1:
			return w.forMedium;
		case 2:
			return w.forHeavy;
		}
		return true;
	}

}
