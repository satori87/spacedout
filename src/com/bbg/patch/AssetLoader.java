package com.bbg.patch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AssetLoader {

	public static Texture texture; //blank texture we use to load resources one at a time

	public static Texture wallTex;
	public static TextureRegion[] wall;
	
	public static TextureRegion[] frame;
	
	public static TextureRegion[] title;
	public static TextureRegion[][] button;
	public static TextureRegion[][] field; //lit, section
	
	public static TextureRegion[][] font; //type, ascii code
	public static int[] fontWidth; //type, ascii code
	public static int[] fontX;
	
	public static boolean loaded = false;
	
	public static void load(boolean firstRun) {
		
		loadTextures(firstRun);		
		loaded = true;
	}

	public static TextureRegion newTR(Texture tex, int x, int y, int w, int h) {
		TextureRegion t = new TextureRegion(tex,x,y,w,h);
		fix(t,false,true);
		return t;
	}
	
	public static void loadTextures(boolean firstRun) {
		int i = 0;
		loadTexture("font");
		font = new TextureRegion[2][256];
		fontWidth = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,5,6,9,9,10,10,3,5,5,9,7,3,8,3,7,7,5,7,7,8,7,7,7,7,7,3,3,8,6,8,7,9,7,7,7,8,7,7,7,7,5,8,7,7,9,8,7,7,8,8,7,7,7,7,9,8,7,7,5,7,5,10,7,5,7,7,7,7,7,6,7,7,5,5,7,4,9,7,7,7,8,7,7,7,7,7,9,7,7,7,6,3,6,8,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		fontX = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,10,17,25,36,47,59,71,76,83,90,101,110,115,125,130,139,148,155,164,173,183,192,201,210,219,228,233,238,248,256,266,275,286,295,304,313,323,332,341,350,359,366,376,385,394,405,415,424,433,443,453,462,471,480,489,500,510,519,528,535,544,551,563,572,579,588,597,606,615,624,632,641,650,657,664,673,679,690,699,708,717,727,736,745,754,763,772,783,792,801,810,818,823,831,841,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		
		for(i = 0; i < 256; i++) {
			if (fontWidth[i] > 0) {
				fontWidth[i] += 1;
				for(int t = 0; t < 2; t++) {
					font[t][i] = new TextureRegion(texture,fontX[i],t*16,fontWidth[i],16);
					fix(font[t][i],false,true);
				}
			}
		}
	
		loadTexture("frame");			
		title = new TextureRegion[4];
		title[0] = new TextureRegion(texture,0,32,32,32); //left piece
		fix(title[0],false,true);
		title[1] = new TextureRegion(texture,32,32,32,32); //32x32 middle chunk
		fix(title[1],false,true);
		title[2] = new TextureRegion(texture,32,32,96,32); //96x32 middle chunk
		fix(title[2],false,true);
		title[3] = new TextureRegion(texture,128,32,32,32); //right piece
		fix(title[3],false,true);
		
		frame = new TextureRegion[11];
		for(i = 0; i < 8; i++) {
			frame[i] = new TextureRegion(texture,i * 32, 0, 32, 32);
			fix(frame[i],false, true);
		}
		frame[8] = new TextureRegion(texture, 0, 56, 32, 32);
		fix(frame[8],false,true);
		frame[9] = new TextureRegion(texture, 200, 42, 2, 22);
		fix(frame[9],false,true);
		frame[10] = new TextureRegion(texture, 214, 42, 2, 22);
		fix(frame[10],false,true);
		

		loadTexture("wall");
		wallTex = texture;
		wall = new TextureRegion[2];
		wall[0] = newTR(texture, 0, 0, 238, 63);
		wall[1] = newTR(texture, 0, 63, 238, 63);
		
	}

	public static void loadTexture(String name) {
		//texture = new Texture(Gdx.files.internal("gfx/" + name + ".png"));
		texture = new Texture(Gdx.files.local("assets/gfx/" + name + ".png"));
		texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
	}
	
	public static void fix(TextureRegion tex, boolean flipX, boolean flipY) {
		fixBleeding(tex);
		tex.flip(flipX, flipY);
	}
	
	public static void fixBleeding(TextureRegion region) {
        float fix = 0.01f;
        float x = region.getRegionX();
        float y = region.getRegionY();
        float width = region.getRegionWidth();
        float height = region.getRegionHeight();
        float invTexWidth = 1f / region.getTexture().getWidth();
        float invTexHeight = 1f / region.getTexture().getHeight();
        region.setRegion((x + fix) * invTexWidth, (y + fix) * invTexHeight, (x + width - fix) * invTexWidth, (y + height - fix) * invTexHeight); // Trims Region
    }
	
	public static void dispose() {
		// We must dispose of the texture when we are finished.
		if(texture != null) {
			texture.dispose();
		}
	}
	
	public static float getStringWidth(String s, float scale, float padding, float spacing) {
		float total = 0;
		if(s == null) {return 0;}
		if(s.length() < 1) {return 0;}
		for(char c : s.toCharArray()) {
			int ascii = (int)c;
			total += AssetLoader.fontWidth[ascii]*scale + padding*2 + spacing;
		}
		return total;
	}
	
}