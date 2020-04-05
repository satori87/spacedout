package com.bbg.shared;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Shape;

public class PickupDef {

	public static Shape[] getShape(int type) {
		return Entities.getCircleArray(8, Shared.xml.physicsScale);
	}

	public static Color getColor(int type) {
		switch(type) {
		case 0:
			return Color.RED;
		case 1:
			return Color.CYAN;
		case 2:
			return Color.YELLOW;
		case 3:
			return Color.GREEN;
		case 4:
			return Color.MAGENTA;
		}
		return Color.WHITE;
	}
}
