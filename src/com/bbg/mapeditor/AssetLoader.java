package com.bbg.mapeditor;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AssetLoader {

	public static long started = 0;
	
	public static Texture texture; // temp loading texture
	
	public static Texture frameTex;
	public static Texture[] bgTex;
	public static Texture grid;
	public static Hashtable<String, TextureRegion> sprites = new Hashtable<String, TextureRegion>();

	public static LwjglApplication app;

	public static TextureRegion[] frame;

	public static TextureRegion[][] button;
	public static TextureRegion[][] field; // lit, section

	public static TextureRegion[][] font; // type, ascii code
	public static int[] fontWidth; // type, ascii code
	public static int[] fontX;

	public static boolean loaded = false;

	int curSprite = 0;

	public static void load(boolean firstRun) {
		System.out.println("assetsbegin:" + (System.currentTimeMillis() - AssetLoader.started));	
		loadTextures(firstRun);
		loaded = true;
		System.out.println("assetsloaded:" + (System.currentTimeMillis() - AssetLoader.started));
	}


	public static int findNextLetter(int start) {
		if (start < 0 || start >= 1024) {
			return -1;
		}
		if (!texture.getTextureData().isPrepared()) {
			texture.getTextureData().prepare();
		}
		Pixmap pixmap = texture.getTextureData().consumePixmap();
		Color c;
		for (int x = start; x < start + 16; x++) {
			if (x >= 0 && x < texture.getTextureData().getWidth()) {
				c = new Color(pixmap.getPixel(x, 1));
				if (c.a == 1.0 && c.b == 1.0 && c.g == 0) {
					return x;
				}
				try {
					Thread.sleep(1);
				} catch (Exception ex) {
					System.out.println("Caught: " + ex.toString());
				} finally {
				}
				;
			}
		}
		return -1;
	}

	public static void outputBreaks() {
		int a = 32;
		int curX = 0;
		int startX, endX, width;
		boolean checkAgain;
		do {
			checkAgain = false;
			startX = findNextLetter(curX);
			if (startX >= 0) {
				endX = findNextLetter(startX + 2);
				if (endX >= 0) {
					width = endX - startX - 2;
					fontWidth[a] = width;
					fontX[a] = startX + 1;
					checkAgain = true;
					curX = endX;
					a++;
				}
			}
		} while (checkAgain == true);
		for (int i = 0; i < 256; i++) {
			System.out.print(fontX[i] + ",");
		}
	}

	public static TextureRegion newTR(Texture tex, int x, int y, int w, int h) {
		TextureRegion t = new TextureRegion(tex, x, y, w, h);
		fix(t, false, true);
		return t;
	}

	public static void loadTextures(boolean firstRun) {
		System.out.println("1:" + (System.currentTimeMillis() - AssetLoader.started));
		int i = 0;
		if (firstRun) {
			loadTexture("font");
			font = new TextureRegion[2][256];
			fontWidth = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 7, 5, 6, 9, 9, 10, 10, 3, 5, 5, 9, 7, 3, 8, 3, 7, 7, 5, 7, 7, 8, 7, 7, 7, 7, 7, 3, 3, 8,
					6, 8, 7, 9, 7, 7, 7, 8, 7, 7, 7, 7, 5, 8, 7, 7, 9, 8, 7, 7, 8, 8, 7, 7, 7, 7, 9, 8, 7, 7, 5, 7, 5,
					10, 7, 5, 7, 7, 7, 7, 7, 6, 7, 7, 5, 5, 7, 4, 9, 7, 7, 7, 8, 7, 7, 7, 7, 7, 9, 7, 7, 7, 6, 3, 6, 8,
					9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
			fontX = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 1, 10, 17, 25, 36, 47, 59, 71, 76, 83, 90, 101, 110, 115, 125, 130, 139, 148, 155, 164,
					173, 183, 192, 201, 210, 219, 228, 233, 238, 248, 256, 266, 275, 286, 295, 304, 313, 323, 332, 341,
					350, 359, 366, 376, 385, 394, 405, 415, 424, 433, 443, 453, 462, 471, 480, 489, 500, 510, 519, 528,
					535, 544, 551, 563, 572, 579, 588, 597, 606, 615, 624, 632, 641, 650, 657, 664, 673, 679, 690, 699,
					708, 717, 727, 736, 745, 754, 763, 772, 783, 792, 801, 810, 818, 823, 831, 841, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

			for (i = 0; i < 256; i++) {
				if (fontWidth[i] > 0) {
					fontWidth[i] += 1;
					for (int t = 0; t < 2; t++) {
						font[t][i] = new TextureRegion(texture, fontX[i], t * 16, fontWidth[i], 16);
						fix(font[t][i], false, true);
						font[t][i].getTexture().setFilter(TextureFilter.Nearest,TextureFilter.Nearest);
					}
				}
			}
			
			System.out.println("2:" + (System.currentTimeMillis() - AssetLoader.started));
			
			loadTexture("frame");
			frameTex = texture;
			frame = new TextureRegion[19];
			for (i = 0; i < 8; i++) {
				frame[i] = newTR(texture, i * 32, 0, 32, 32);
			}
			frame[8] = newTR(texture, 0, 56, 32, 32);
			frame[9] = newTR(texture, 200, 42, 2, 22);
			frame[10] = newTR(texture, 214, 42, 2, 22);
			frame[11] = newTR(texture, 62+16, 56, 16, 16);
			frame[12] = newTR(texture, 62+16, 56+16, 16, 16);
			frame[13] = newTR(texture, 62, 56, 16, 16);
			frame[14] = newTR(texture, 62, 56+16, 16, 16);
			frame[15] = newTR(texture, 94, 56+16, 12, 16);
			frame[16] = newTR(texture, 94, 56, 12, 16);
			frame[17] = newTR(texture, 106, 56, 13, 13);
			frame[18] = newTR(texture, 106, 56+13, 13, 13);
			
			System.out.println("3:" + (System.currentTimeMillis() - AssetLoader.started));
			
			button = new TextureRegion[2][9];
			for (int b = 0; b < 2; b++) {
				for (i = 0; i < 8; i++) {
					button[b][i] = new TextureRegion(texture, 119 + i * 8, 56 + b * 8, 8, 8);
					fix(button[b][i], false, true);
				}
				button[b][8] = new TextureRegion(texture, 119 + 64, 56 + b * 8, 8, 8);
				fix(button[b][8], false, true);
			}
			System.out.println("4:" + (System.currentTimeMillis() - AssetLoader.started));
			field = new TextureRegion[2][3];
			for (int b = 0; b < 2; b++) {
				field[b][0] = new TextureRegion(texture, 0, 88 + b * 26, 42, 26);
				fix(field[b][0], false, true);
				field[b][1] = new TextureRegion(texture, 42, 88 + b * 26, 32, 26);
				fix(field[b][1], false, true);
				field[b][2] = new TextureRegion(texture, 74, 88 + b * 26, 41, 26);
				fix(field[b][2], false, true);

			}
			texture.setFilter(TextureFilter.Nearest,TextureFilter.Nearest);
			System.out.println("5:" + (System.currentTimeMillis() - AssetLoader.started));
			//return;
		}

		loadTexture("grid");
			grid = texture;
			grid.setFilter(TextureFilter.Nearest,TextureFilter.Nearest);

		bgTex = new Texture[4];
		for (i = 0; i < 4; i++) {
			loadTexture("bg" + 0);
			bgTex[i] = texture;
		}
		String lastTex = "";
		FileHandle artFile = Gdx.files.local("assets/art.def");
		String text = artFile.readString();
		List<String> lines = Arrays.asList(text.split("\\r?\\n"));
		String[] words = new String[1];
		for (String curLine : lines) {
			words = curLine.split(",");
			if(words[0] != lastTex) {
				loadTexture(words[0]);
				lastTex = words[0];
			}
			TextureRegion tr = newTR(texture, Integer.parseInt(words[2]), Integer.parseInt(words[3]),
					Integer.parseInt(words[4]), Integer.parseInt(words[5]));
			sprites.put(words[1], tr);

		}

	}

	public static TextureRegion getSprite(String key) {
		return (TextureRegion) sprites.get(key);
	}

	public static int getAstW(int b) {
		int w = 0;
		switch (b) {
		case 0:
			w = 8;
			break;
		case 1:
			w = 16;
			break;
		case 2:
			w = 33;
			break;
		case 3:
			w = 48;
			break;
		}
		return w;
	}

	public static int getAstH(int b) {
		int h = 0;
		switch (b) {
		case 0:
			h = 8;
			break;
		case 1:
			h = 16;
			break;
		case 2:
			h = 33;
			break;
		case 3:
			h = 48;
			break;
		}
		return h;
	}

	public static int getAstX(int i, int b) {
		int x = 0;
		switch (b) {
		case 0:
			x = 0;
			break;
		case 1:
			x = 8;
			break;
		case 2:
			x = 24;
			break;
		case 3:
			x = 57;
			break;
		}
		if (i > 5) {
			x += 105;
		}
		return x;
	}

	public static int getAstY(int i) {
		if (i > 5) {
			i -= 6;
		}
		int y = i * 48;
		return y;
	}

	public static void loadTexture(String name) {
		// texture = new Texture(Gdx.files.internal("gfx/" + name + ".png"));
		texture = new Texture(Gdx.files.local("/assets/gfx/" + name + ".png"));
		
		
	}

	public static void fix(TextureRegion tex, boolean flipX, boolean flipY) {
		fixBleeding(tex);
		tex.flip(flipX, flipY);
		tex.getTexture().setFilter(TextureFilter.Linear,TextureFilter.Linear);
	}

	public static void fixBleeding(TextureRegion region) {
		float fix = 0.01f;
		float x = region.getRegionX();
		float y = region.getRegionY();
		float width = region.getRegionWidth();
		float height = region.getRegionHeight();
		float invTexWidth = 1f / region.getTexture().getWidth();
		float invTexHeight = 1f / region.getTexture().getHeight();
		region.setRegion((x + fix) * invTexWidth, (y + fix) * invTexHeight, (x + width - fix) * invTexWidth,
				(y + height - fix) * invTexHeight); // Trims Region
	}

	public static void dispose() {
		// We must dispose of the texture when we are finished.
		if (texture != null) {
			texture.dispose();
		}
	}

	public static float getStringWidth(String s, float scale, float padding, float spacing) {
		float total = 0;
		for (char c : s.toCharArray()) {
			int ascii = (int) c;
			total += AssetLoader.fontWidth[ascii] * scale + padding * 2 + spacing;
		}
		return total;
	}
	
	

}