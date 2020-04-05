package com.bbg.mapeditor;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.bbg.mapeditor.Input;
import com.bbg.shared.Entities;
import com.bbg.shared.MapDef;
import com.bbg.shared.MapDef.BaseData;
import com.bbg.shared.Wall;
import com.bbg.mapeditor.AssetLoader;
import com.bbg.mapeditor.GameScreen;

public class Editor {
	
	public boolean[] hover = new boolean[20];
	public String[] caption = new String[20];
	
	public MapDef curMap = new MapDef();
	public int curMapType = 0;
    GameScreen screen;
    public long tick = 0;  
    public long lastClick = 0;
    public long lastEClick = 0;
    public long flickerStamp = 0;
	public int flicker = 0;
	public float glowVal = 0.0f;
	public boolean glowingUp = true;
	public List<Wall> walls = new LinkedList<Wall>();
    public int state = 0; //0 
    int selMap = 0;
    int selEMap = 0;
	
    public float baseCurDirection[] = new float[2];
    public boolean baseGlowing[] = new boolean[2];
    public float baseGlowVal[] = new float[2];
    public Color baseColor[] = new Color[2];
    
    public long moveStamp = 0;
    public int mapX = 400;
    public int mapY = 0;
    
    public Editor(GameScreen screen) {
        this.screen = screen;

        for(int i = 0; i < 20; i++) {
        	caption[i] = "invis";
        	hover[i] = false;
        }
        caption[0] = "Edit Map";
        caption[1] = "invis";
        caption[2] = "Quit";
        baseColor[0] = Color.RED;
        baseColor[1] = Color.BLUE;
    }
    
    public void update(long tick) {
        this.tick = tick;
        if(screen.screenLoaded) {
	        switch (state) {
		    	case 0: //main menu
		    		updateMainMenu();
		    		break;
		    	case 1: //map selection
		    		updateMapMenu();
		    		break;
		    	case 2: //editing map
		    		updateEditMap();
		    		break;
		    	default:
		    		break;
	        }
        }
    }
    
    public void updateEditMap() {
    	Input in = screen.input;
    	int v = 10;
    	if(in.keyDown[Keys.SHIFT_LEFT] || in.keyDown[Keys.SHIFT_RIGHT]) {
    		v *= 2;
    	}
    	if(in.keyDown[Keys.CONTROL_LEFT] || in.keyDown[Keys.CONTROL_RIGHT]) {
    		v *= 2;
    	}
    	if(tick > moveStamp) {
    		moveStamp = tick + 20;
	    
	    	if(in.keyDown[Keys.LEFT]) {
	    		mapX -= v;	    		
	    	} else if (in.keyDown[Keys.RIGHT]) {
	    		mapX += v;
	    	}
	    	if(in.keyDown[Keys.UP]) {
	    		mapY -= v;	    		
	    	} else if (in.keyDown[Keys.DOWN]) {
	    		mapY += v;
	    	}
    	}
    	if(in.mouseDown) {
    		if(in.wasMouseJustClicked) {
    			in.wasMouseJustClicked = false;
    			System.out.println(in.mouseDownX + "," + in.mouseDownY);
    			int mdx = in.mouseDownX;
    			int mdy = in.mouseDownY;
    			if(inBox(mdx, mdy, 796, 996, 24, 224)) {
    				int sX = mdx - 896;
    				int sY = mdy - 124;
    				sX *= 11;
    				sY *= 11;
    				mapX = mapX + sX+112;
    				mapY = mapY + sY;
    			}
    		}
    	}
    }

    public void render() {
    	screen.batcher.setColor(Color.WHITE);
    	switch (state) {
	    	case 0: //main menu
	    		renderMainMenu();
	    		break;
	    	case 1: //edit map screen
	    		renderMapMenu();
	    		break;
	    	case 2:
	    		renderMap();
	    		break;
	    	default:
	    		break;
    		
    	}
    	screen.batcher.setColor(Color.WHITE);
    }
    
    void renderMap() {
    	screen.moveCameraTo((float) Math.round(mapX), (float) Math.round(mapY));
    	drawBackground();
    	drawField();
    	
    	drawOverlay();
    	
    }
    
    void drawOverlay() {
    	int x = Math.round(screen.cam.position.x) - Math.round(screen.viewWidth / 2);
		int y = Math.round(screen.cam.position.y) - Math.round(screen.viewHeight / 2 );
		screen.shapeRenderer.begin(ShapeType.Filled);
		screen.shapeRenderer.setColor(0, 0, 0, 1);
    	screen.shapeRenderer.rect(768+x, y, 256, screen.viewHeight);
    	screen.shapeRenderer.end();
    	drawMiniMap();
    }
    
    public void drawBackground() {

		float x = mapX;
		float y = mapY;
		float minX = curMap.width/-2;
		float maxX = curMap.width/2;
		float minY = curMap.height/-2;
		float maxY = curMap.height/2;
		float relX = x + Math.abs(minX);
		float relMaxX = Math.abs(maxX) + Math.abs(minX);
		float relY = y + Math.abs(minY);
		float relMaxY = Math.abs(maxY) + Math.abs(minY);
		float percentX = relX / relMaxX;
		float percentY = relY / relMaxY;
		int w = Math.round(screen.viewWidth);
		int h = Math.round(screen.viewHeight);
		float dX = Math.round(x - ((float) w / 2f));
		float dY = Math.round(y - ((float) h / 2f));
		int srcX = Math.round(((float) w / 2f) * percentX * (2048 / (float) w));
		int srcY = Math.round(((float) h / 2f) * percentY * (1536 / (float) h));
		screen.batcher.draw(AssetLoader.bgTex[0], dX, dY, w, h, srcX, srcY, w, h, false, true);
		drawGrid();
	}

	void drawGrid() {
		int w = Math.round(screen.viewWidth);
		int h = Math.round(screen.viewHeight);
		float dX = Math.round(Math.round(mapX) - ((float) w / 2f));
		float dY = Math.round(Math.round(mapY) - ((float) h / 2f));
		int sX = 0;
		int sY = 0;
		if (tick > flickerStamp) {
			flickerStamp = tick + 20;
			flicker += 1;
			if (flicker >= 8) {
				flicker = 0;
			}
			if (glowingUp) {
				glowVal += 0.04f;
				if (glowVal > .6f) {
					glowingUp = false;
				}
			} else {
				glowVal -= 0.04f;
				if (glowVal < .2f) {

					glowingUp = true;
				}
			}
		}
		int r = 52;
		int g = 174;
		int b = 237;
		screen.batcher.setColor(Entities.getColor(r, g, b, 1));
		for (int x = -88; x < screen.viewWidth + 88; x += 40) {
			for (int y = -88; y < screen.viewHeight + 88; y += 40) {
				sX = (int) mapX % 40;
				sY = (int) mapY % 40;
				screen.batcher.draw(AssetLoader.grid, dX + x - sX, dY + y - sY, 62, 62, 0, 0, 62, 62, false, true);
			}
		}
		screen.batcher.setColor(Entities.getColor(r, g, b, glowVal));
		for (int x = -88; x < screen.viewWidth + 88; x += 40) {
			for (int y = -88; y < screen.viewHeight + 88; y += 40) {
				sX = (int) mapX % 40;
				sY = (int) mapY % 40;
				screen.batcher.draw(AssetLoader.grid, dX + x - sX, dY + y - sY, 62, 62, 62, 0, 62, 62, false, true);
			}
		}
		screen.batcher.setColor(Color.WHITE);
	}
    
    public void drawField() {
		try {
			float x = mapX;
			float y = mapY;

			for (BaseData e : curMap.bases) {
				if (e != null) {
					if (Math.abs(x - e.x) < (screen.viewWidth / 2) + 200 * 2) {
						if (Math.abs(y - e.y) < (screen.viewHeight / 2) + 200 * 2) {
							renderBase(e);
						}
					}
				}
			}

			for (Wall e : walls) {

				if (Math.abs(x - e.x) < (screen.viewWidth / 2) + 200) {
					if (Math.abs(y - e.y) < (screen.viewHeight / 2) + 200) {
						renderWall(e);
					}
				}
			}
			
		} catch (Throwable t) {
			System.out.println(t);
		}
		
	}
    
    void renderBase(BaseData e) {
    	baseCurDirection[e.team] += e.rotSpeed;
		if (baseCurDirection[e.team] >= (Math.PI * 2)) {
			baseCurDirection[e.team] = 0;				
		}
		if(baseGlowing[e.team]) {
			if(baseGlowVal[e.team] < 0.6f) {
				baseGlowVal[e.team] += 0.01f;					
			} else {
				baseGlowing[e.team] = false;
			}
		} else {
			if(baseGlowVal[e.team] > .2f) {
				baseGlowVal[e.team] -= 0.01f;
			} else {
				baseGlowing[e.team] = true;
			}
		}
		if (baseGlowVal[e.team] > 0.6f) {baseGlowVal[e.team] = 0.6f;}
		if(baseGlowVal[e.team] < .2f) {baseGlowVal[e.team] = .2f;}
		float r = (float) Math.toDegrees(baseCurDirection[e.team]);
		Color cg = new Color(baseColor[e.team]);
		screen.batcher.setColor(cg);
		screen.drawRegion(AssetLoader.getSprite("base"), e.x, e.y, true, r, e.scale);
		
		cg.a = baseGlowVal[e.team];
		screen.batcher.setColor(cg);
		screen.drawRegion(AssetLoader.getSprite("baseg"), e.x, e.y, true, r, e.scale);
		screen.batcher.setColor(Color.WHITE);
    }
    
    public void drawMiniMap() {
		//if (!miniMap) {
		//	return;
		//}
		
		float x = mapX;
		float y = mapY;
		float sX = x + 384;
		float sY = y - 260;
		screen.batcher.flush();
		screen.batcher.end();
		screen.batcher.begin();
		Rectangle scissors = new Rectangle();
		Rectangle clipBounds = new Rectangle(sX - 100, sY - 100, 200, 200);
		ScissorStack.calculateScissors(screen.cam, screen.batcher.getTransformMatrix(), clipBounds, scissors);
		ScissorStack.pushScissors(scissors);
		// spriteBatch.draw(...);

		for (Wall e : walls) {
			if (Math.abs(x - e.x) < 1100) {
				if (Math.abs(y - e.y) < 1100) {					
					screen.batcher.setColor(e.col);
					TextureRegion tex = AssetLoader.getSprite("line" + Integer.toString(e.thickness) + "g");
					float dX = sX + ((e.x - x) / 10f);
					float dY = sY + ((e.y - y) / 10f);
					float r = e.d + 90;
					screen.drawRegion(tex, dX, dY, true, r, .1f);
					tex = AssetLoader.getSprite("line" + Integer.toString(e.thickness));
					screen.drawRegion(tex, dX, dY, true, r, .1f);
				}
			}
		}

		for (BaseData e : curMap.bases) {
			//for (Base e : base) {
				if (e != null) {
					if (Math.abs(x - e.x) < 1100) {
						if (Math.abs(y - e.y) < 1100) {
							float dX = sX + ((e.x - x) / 10f);
							float dY = sY + ((e.y - y) / 10f);
							float r = (float) Math.toDegrees(baseCurDirection[e.team]);
							screen.batcher.setColor(baseColor[e.team]);
							screen.drawRegion(AssetLoader.getSprite("base"), dX, dY, true, r, 0.07f);
							Color dd = new Color(baseColor[e.team]);
							dd.a = 0.5f;
							screen.batcher.setColor(dd);
							screen.drawRegion(AssetLoader.getSprite("baseg"), dX, dY, true, r, 0.07f);
						}
					}
				}
			//
		}
		
		screen.batcher.flush();
		ScissorStack.popScissors();
		screen.batcher.setColor(new Color(1, 1, 1, 0.5f));
		screen.drawFrame(sX - 100, sY - 100, 200, 200, false);
		screen.batcher.setColor(Color.WHITE);
	}
    
    void renderWall(Wall wall) {
		float r = wall.d;
		//int b = 0;
		//if (!wall.contacts.isEmpty() || wall.blink) {
			//b = 1;
		//}
		screen.batcher.setColor(wall.col);
		
		screen.drawRegion(AssetLoader.getSprite("line" + Integer.toString(wall.thickness)), wall.x, wall.y, true, r+90, 1.0f);
		Color g = new Color(wall.col);
		g.a = glowVal;
		screen.batcher.setColor(g);
		screen.drawRegion(AssetLoader.getSprite("line" + Integer.toString(wall.thickness) + "g"), wall.x, wall.y, true, r+90, 1.0f);
		screen.batcher.setColor(Color.WHITE);
    }
    
    int dX = 400;
	int dY = 180;
	int h = 48;
	int w = 384;
	
    void renderMainMenu() {
		screen.drawFrame(192, 140, 416, 320, true);
		for (int i = 0; i < 5; i++) {
			if (!caption[i].equals("invis")) {
				screen.drawButton(hover[i], dX, dY + (i * 60), w, h, true);
				screen.drawFont(0, dX, dY + (i * 60), caption[i], true, 2f, Color.WHITE);
			}
		}
    }
    
    void updateMapMenu() {    	
		int x = screen.input.mouseX;
		int y = screen.input.mouseY;
		if (screen.input.mouseDown) {
			if (screen.input.wasMouseJustClicked) {
				screen.input.wasMouseJustClicked = false;
				checkClick(x,y);
			}
		}
    }
    
    
	public boolean inBox(int x, int y, int lowerX, int upperX, int lowerY, int upperY) {
		return (x >= lowerX && x <= upperX && y >= lowerY && y <= upperY);
	}
    
    void checkClick(int x, int y) { 

    	if (inBox(x, y, 51, 346, 64, 440)) {
    		int i = (y - 64) / 20;
    		if (i < Entities.mapCount) {
    			if(Entities.maps[i] != null) {
    				if(selMap == i) {
    					if(screen.tick - lastClick < 400) {
    						//double click
    						editMap(0, i);
    					}
    				}
    				lastClick = screen.tick;
    				selMap = i;
        		}
    		}
    	}
    	if (inBox(x, y, 451, 746, 64, 480)) {
    		int i = (y - 64) / 20;
    		if (i < 200) {
    			if(Entities.emaps[i] != null) {
    				if(selEMap == i) {
    					if(screen.tick - lastEClick < 400) {
    						//double click    						
    						editMap(1, i);
    					}
    				}
    				lastEClick = screen.tick;
    				selEMap = i;
        		}
    		}
    	}
    }
    
    void editMap(int t, int i) {
    	System.out.println(t + " : " + i);
    	if(t == 0) {
    		curMap = Entities.maps[i];
    	} else {
    		curMap = Entities.emaps[i];
    	}
    	curMapType = t;
		state = 2;
		walls.clear();
		Wall.loadWalls(2, curMap, walls);
    }
    
    
    void renderMapMenu() {
    	screen.drawFrame(0, 0, screen.viewWidth, screen.viewHeight, true);
		screen.drawFont(0, 200, 30, "Active Maps", true, 1.5f,Color.WHITE);
		Color col = Color.WHITE;
		int drawX = 60;
		int drawY = 70;
		screen.drawFrame(46, 60, 300, 440, false);
		for (int i = 0; i < 21; i++) {
			if(i == selMap) {
				for(int x = drawX; x < drawX + 292; x += 2) {
					screen.batcher.draw(AssetLoader.frameTex, x - 10, drawY + i * 20 - 6, 2, 20, 214, 42, 2, 20, false, true);
				}
			}
			if (i < 3) {
				if (Entities.maps[i] != null) {
					screen.drawFont(0, drawX, drawY + i * 20 - 4, i + ": " + Entities.maps[i].name, false, 1, col);
				}
			}
		}
		screen.drawFont(0, screen.viewHeight, 30, "Offline Maps", true, 1.5f,Color.WHITE);
		screen.drawFrame(446, 60, 300, 440, false);
		for (int i = 0; i < 21; i++) {
			if(i == selEMap) {
				for(int x = drawX; x < drawX + 292; x += 2) {
					screen.batcher.draw(AssetLoader.frameTex, x - 10+400, drawY + i * 20 - 6, 2, 20, 214, 42, 2, 20, false, true);
				}
			}
			if (i < 200) {
				if (Entities.emaps[i] != null) {
					screen.drawFont(0, drawX+400, drawY + i * 20 - 4, i + ": " + Entities.emaps[i].name, false, 1, col);
				}
			}
		}
    }
    
    void updateMainMenu() {
    	Input input = screen.input;
		int x = input.mouseX;
		int y = input.mouseY;
    	for (int i = 0; i < 10; i++) {
			if (screen.inBox(x, y, dX, dY + (i * 60), w, h)) {
				hover[i] = true;
				if (screen.input.mouseDown) {
					if (screen.input.wasMouseJustClicked) {
						screen.input.wasMouseJustClicked = false;
						switch(i) {
							case 0:	//edit map	
								state = 1;
								break;
							case 1: //N/A
								break;
							case 2: //quit
								System.exit(0);
								break;
							default:
								break;
						}
					}
				}
			} else {
				hover[i] = false;
			}
		}
    }

    public long getTick() {
        return System.currentTimeMillis();
    }

}
