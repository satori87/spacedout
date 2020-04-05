package com.bbg.mapeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.bbg.shared.Entities;

public class GameScreen implements Screen {
	
	OrthographicCamera cam;
	public ShapeRenderer shapeRenderer;
	SpriteBatch batcher;
	boolean screenLoaded = false;	

	public float viewWidth = Prefs.WINDOWWIDTH;
	public float viewHeight = Prefs.WINDOWHEIGHT;
	
	public Input input;		
	boolean rendering = false;
	public Editor editor = new Editor(this);
	long tick = 0;
	
	public GameScreen() {
		Entities.editInit();
		cam = new OrthographicCamera();
		cam.setToOrtho(true, Prefs.WINDOWWIDTH, Prefs.WINDOWHEIGHT);
		batcher = new SpriteBatch();
		batcher.setProjectionMatrix(cam.combined);
		input = new Input(this);
		Gdx.input.setInputProcessor(input);
		shapeRenderer = new ShapeRenderer();
		shapeRenderer.setProjectionMatrix(cam.combined);
		screenLoaded = true;
	}
	
	@Override
	public void render(float delta) {
		while(accessRenderState(false, false)) {
			//the other thread is actively working so we must wait a SHORT while
			//shouldnt we add a sleep?
		}
		accessRenderState(true, true);
		
		tick = System.currentTimeMillis();
		if(AssetLoader.loaded && screenLoaded) {
			editor.update(tick);
			Gdx.gl.glClearColor(0, 0, 0, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			batcher.enableBlending();
			batcher.begin();
			editor.render();
			batcher.end();
		}

		accessRenderState(true, false);
	}
	
	public void moveCameraTo(float x, float y) {
		// cam.position.lerp(new Vector3(x,y,0.1f), 0.1f);
		cam.position.y = y;
		cam.position.x = x;
		cam.update();
		batcher.setProjectionMatrix(cam.combined);
		shapeRenderer.setProjectionMatrix(cam.combined);
	}
	
	public synchronized boolean accessRenderState(boolean changeIt, boolean changeTo) {
		if (changeIt) {rendering = changeTo;}
		return rendering;
	}
		
	void drawRegion(TextureRegion region, float X, float Y, boolean centered, float rotation, float scale) {
		if (region == null) {
			return;
		}
		int width = region.getRegionWidth();
		int height = region.getRegionHeight();
		float eX = 0;
		float eY = 0;

		eX = X;
		eY = Y;
		if (centered) {
			eX -= (width / 2);
			eY -= (height / 2);
		}
		// we gotta round the floats
		int dX = Math.round(eX);
		int dY = Math.round(eY);
		if (centered) {
			batcher.draw(region, dX, dY, width / 2, height / 2, width, height, scale, scale, rotation);
		} else {
			batcher.draw(region, dX, dY, 0, 0, width, height, scale, scale, rotation);
		}
	}
	
	void drawRegion(TextureRegion region, int X, int Y, boolean centered, float rotation, float scale)
	{
		if(region == null) {return;}
		int width = region.getRegionWidth();
		int height = region.getRegionHeight();
		if (centered) {
			X -= (width / 2);
			Y -= (height /2);
		}
		if(centered) {
			batcher.draw(region, X, Y, width / 2, height / 2, width, height, scale, scale, rotation);
		} else {
			batcher.draw(region, X, Y, 0, 0, width, height, scale, scale, rotation);
		}
	}
	
	boolean inBox(int x, int y, int centerX, int centerY, int width, int height) {
		int topY = centerY - (height/2);
		int bottomY = centerY + (height/2);
		int leftX = centerX - (width/2);
		int rightX = centerX + (width/2);
		if(x > leftX && x < rightX && y > topY && y < bottomY) {
			return true;
		}
		return false;
	}
	
	void checkClick() {
		int x = input.mouseX;
		int y = input.mouseY;
		if(input.mouseDown) {
			if(input.wasMouseJustClicked) {
				input.wasMouseJustClicked = false;//first pass on mouseDown	
				if(inBox(x,y,320,240,96,48)) {
					//blah
				}				
			} else {
				//still holding...
			}
		}	
	}
	
	void drawFrame(float x, float y, float width, float height, boolean useBackground) {
		TextureRegion[] frame = AssetLoader.frame;
		
		if(useBackground) {
			for(int a = 0; a < height; a+= 32) {
				for(int b = 0; b < width; b += 32) {
					drawRegion(frame[8],x+b, y+a, false, 0, 1);
				}
			}
		}
		
		//draw top left
		drawRegion(frame[0], x, y, false, 0, 1);
		//top right
		drawRegion(frame[1], x+width-32, y, false, 0, 1);
		//bottom left
		drawRegion(frame[2], x, y+height-32, false, 0, 1);
		//bottom right
		drawRegion(frame[3], x+width-32, y+height-32, false, 0, 1);
		
		//left side
		for(int b = 32; b <= height-32; b+=32) {
			drawRegion(frame[4], x, y+b, false, 0, 1);
		}
		//right side
		for(int b = 32; b <= height-32; b+=32) {
			drawRegion(frame[5], x+width-32, y+b, false, 0, 1);
		}
		//top side
		for(int b = 32; b <= width-32; b+=32) {
			drawRegion(frame[6], x+b, y, false, 0, 1);
		}
		//bottom side
		for(int b = 32; b <= width-32; b+=32) {
			drawRegion(frame[7], x+b, y+height-32, false, 0, 1);
		}
		
		
	}
	
	void drawButton(boolean pressed, int dx, int dy, int width, int height, boolean centered) {
		int x,y;
		if(centered) {
			x = dx - (width/2);
			y = dy - (height/2);
		} else {
			x = dx;
			y = dy;
		}
		TextureRegion[][] button = AssetLoader.button;
		int p;
		if(pressed) {p=1;}else{p=0;}
		for(int a = 8; a < height-8; a+= 8) {
			for(int b = 8; b < width-8; b += 8) {
				drawRegion(button[p][8],x+b, y+a, false, 0, 1);
			}
		}
		//assh
		//draw top left
		drawRegion(button[p][0], x, y, false, 0, 1);
		//top right
		drawRegion(button[p][1], x+width-8, y, false, 0, 1);
		//bottom left
		drawRegion(button[p][2], x, y+height-8, false, 0, 1);
		//bottom right
		drawRegion(button[p][3], x+width-8, y+height-8, false, 0, 1);
		
		//left side
		for(int b = 8; b < height-8; b+=8) {
			drawRegion(button[p][4], x, y+b, false, 0, 1);
		}
		//right side
		for(int b = 8; b < height-8; b+=8) {
			drawRegion(button[p][5], x+width-8, y+b, false, 0, 1);
		}
		//top side
		for(int b = 8; b < width-8; b+=8) {
			drawRegion(button[p][6], x+b, y, false, 0, 1);
		}
		//bottom side
		for(int b = 8; b < width-8; b+=8) {
			drawRegion(button[p][7], x+b, y+height-8, false, 0, 1);
		}		
		
	}
	
	void drawTextArea(boolean lit, int dx, int dy, int width, boolean centered) {
		int x,y;
		if(centered) {
			x = dx - (width/2);
			y = dy - 13;
		} else {
			x = dx;
			y = dy;
		}
		int l = 1;
		if(lit) {l=0;}
		//batcher.setColor(new Color(128,128,128,1));		
		drawRegion(AssetLoader.field[l][0], x, y, false, 0, 1);
		for(int b = 42; b < width-42;b+=32) {
			drawRegion(AssetLoader.field[l][1], x+b, y, false, 0, 1);
		}
		drawRegion(AssetLoader.field[l][2], x+width-42, y, false, 0, 1);
		
	}
	
	public void drawFont(int type, float x, float y, String s, boolean centered, float scale, Color col) {
		float curX = x;
		float padding = 0 * scale;
		float spacing = 1.0f * scale;
		float total = 0;
		float oX, oY;
		//get a quick count of width
		if(centered) {
			total = AssetLoader.getStringWidth(s, scale, padding, spacing);
			oX = Math.round(-total / 2);
			oY = Math.round((scale * -16.0f)/2);
		} else {
			oX = 0;
			oY = 0;
		}
		Color cur = batcher.getColor();
		batcher.setColor(col);
		for(char c : s.toCharArray()) {
			int ascii = (int)c;
			if (AssetLoader.fontWidth[ascii] > 0) {
				drawRegion(AssetLoader.font[type][ascii],Math.round(curX+padding+oX),Math.round((float)y+oY),false,0,scale);
				curX += AssetLoader.fontWidth[ascii]*scale + padding*2 + spacing;
			}
		}
		batcher.setColor(cur);
	}
	
	void drawFont(int type, float X, float Y, String s, boolean centered, float scale) {
		drawFont(type, X, Y, s, centered, scale, Color.WHITE);
	}
	
	@Override
	public void resize(int width, int height) {
		
	}

	@Override
	public void show() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		// Leave blank
	}

	public void debug(String s) {
		System.out.println(s);
	}
	
}
