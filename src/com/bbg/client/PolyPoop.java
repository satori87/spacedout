package com.bbg.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bbg.shared.Entities;
import com.bbg.shared.Random;
import com.bbg.shared.Vector;

public class PolyPoop {
	Scene scene;
	public boolean centered = true;
	public float x = 0;
	public float y = 0;
	public float d = 0;
	public int flavor = Random.getInt(3);
	public int r = 0;
	public int vR = 0;
	public float size = 0.1f;
	public Color col = Color.WHITE;
	public float vX = 0;
	public float vY = 0;
	public float glowVal = 1;
	public long diesAt = 0;
	public long fadeAt = 0;
	public boolean remove = false;
	boolean de = false;
	public float intensity = 0;
	public boolean fore = false;
	public boolean rot = false;
	public float cX = 0;
	public float cY = 0;
	
	public PolyPoop(Scene scene, float x, float y, float direction) {
		this.scene = scene;
		this.x = x;
		this.y = y;
		this.size = 0.06f + (float)(Math.random() * 0.2f);
		r = Random.getInt(360);
		vR = Random.getInt(10) + 10;
		diesAt = scene.tick + 800;
		fadeAt = scene.tick+200;
		this.col = Entities.randomBrightColor();
		glowVal = 1;
		d = (direction + (float)Math.PI) - 0.6f + (float)(Math.random() * 1.2f);
		Vector v = new Vector(d, (float)(Math.random() * 3f) + 3f);
		vX = v.xChange;
		vY = v.yChange;
	}
	
	public PolyPoop(Scene scene, float dir, float x, float y, int TTL, float intensity, float size, Color col) {
		this.scene = scene;
		this.x = x;
		this.y = y;
		this.size = size * (1f - .2f + (float)(Math.random() * .4f)); // +/- 20%
		r = Random.getInt(360);
		vR = Random.getInt(10) + 10;
		diesAt = scene.tick + TTL;
		fadeAt = scene.tick + 400;
		this.col = col;
		glowVal = 1;
		d = dir;
		cX = 0;
		cY = 0;
		de = true;
		fore = true;
		this.intensity = intensity;
	}
	
	public PolyPoop(Scene scene, float x, float y, int TTL, float intensity, float size, Color col) {
		this.scene = scene;
		this.x = x;
		this.y = y;
		this.size = size * (1f - .2f + (float)(Math.random() * .4f)); // +/- 20%
		r = Random.getInt(360);
		vR = Random.getInt(10) + 10;
		diesAt = scene.tick + TTL + TTL;
		fadeAt = scene.tick + 400;
		this.col = col;
		glowVal = 1;
		d = (float)(Math.random() * Math.PI * 2);
		Vector v = new Vector(d, (float)(intensity * (1f - .2f + (float)(Math.random() * .4f))));
		vX = v.xChange;
		vY = v.yChange;		
	}
	
	public void render(GameScreen screen, float dX, float dY) {
		
		if(scene.tick >= diesAt) {
			remove = true;
			return;
		} else {
			if(scene.tick >= fadeAt) {
				glowVal -= 0.08f;
			}
			r += vR;
			if (de) {
				//d = Vector.byChange(dX - x, dY - y);
				d = (float)Math.atan2(cX - y, cY - x);
				float dO = 0;
				if(!rot) {
					dO = (float)(Math.PI/2);
				}
				Vector v = new Vector(d+dO,intensity);
				vX = v.xChange;
				vY = v.yChange;
			}
			x += vX;
			y += vY;			
		}
		if(glowVal <= 0) {
			remove = true;
			return;
		}
        //TextureRegion glowTex = AssetLoader.getSprite("shape" + Integer.toString(flavor) + "g");
        TextureRegion shapeTex = AssetLoader.getSprite("shape" + Integer.toString(flavor));
        //screen.batcher.setColor(Entities.getColor(34, 255, 255, glowVal));
        Color col2 = new Color(col); 
        col2.a = glowVal;
        screen.batcher.setColor(col2);
        if(de) {
        	screen.drawRegion(shapeTex, x+dX, y+dY, centered, r, size);
        } else {
        	screen.drawRegion(shapeTex, x, y, centered, r, size);
        }
       // screen.drawRegion(glowTex, x, y, centered, r, size);
        //screen.batcher.setColor(Entities.getColor(34, 255, 255, 1));
        screen.batcher.setColor(Color.WHITE);
        
	}
	
}
