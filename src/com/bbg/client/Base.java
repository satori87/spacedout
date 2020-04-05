package com.bbg.client;

import com.badlogic.gdx.graphics.Color;

public class Base extends Entity {
	// for now they are simple enough to handle without network communication
	// and without the physics world
	// just use entity manager

	public int index = 0;
	public int team = -1;
	public float rotSpeed = 0.001f;
	float glowVal = 0.1f;
	boolean glowing = true;
	public Base(Scene scene, int index, int team, float x, float y, float rotSpeed, float scale) {
		this.scene = scene;
		this.index = index;
		this.team = team;
		this.x = x;
		this.y = y;
		this.rotSpeed = rotSpeed;
		this.scale = scale;
	}

	public void update() {
		this.curDirection += rotSpeed;
		if (curDirection >= (Math.PI * 2)) {
			curDirection = 0;				
		}
		if(glowing) {
			if(glowVal < 0.6f) {
				glowVal += 0.01f;					
			} else {
				glowing = false;
			}
		} else {
			if(glowVal > .2f) {
				glowVal -= 0.01f;
			} else {
				glowing = true;
			}
		}
		if (glowVal > 0.6f) {glowVal = 0.6f;}
		if(glowVal < .2f) {glowVal = .2f;}
	}

	public void render(GameScreen screen) {
		if(!active) {return;}
		if(scale <= 0) {return;}
		update();
		float r = (float) Math.toDegrees(curDirection);
		screen.batcher.setColor(scene.teams[team].col());
		screen.drawRegion(AssetLoader.getSprite("base"), x, y, true, r, scale);
		Color cg = new Color(scene.teams[team].col());
		cg.a = glowVal;
		screen.batcher.setColor(cg);
		screen.drawRegion(AssetLoader.getSprite("baseg"), x, y, true, r, scale);
		screen.batcher.setColor(Color.WHITE);
	}

}
