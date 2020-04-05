package com.bbg.client;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.bbg.shared.Entities;
import com.bbg.shared.ItemDef;
import com.bbg.shared.Network.LoadoutData;
import com.bbg.shared.ShipDef;
import com.bbg.shared.WeaponDef;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class GameMenu {

	public GameScreen screen;
	
	public int maxbind = OptionsXML.numBinds - 9;

	private int state = 0;
	public int fullScreen = Options.fullScreen;
	public int music = Options.music;
	public int sound = Options.sound;

	public int cScroll = 0;

	public long editClick = 0;
	public boolean firstClick = false;

	int fromSource = 0;

	public LoadoutData[] load = new LoadoutData[5];
	public boolean[] hover = new boolean[20];
	public String[] caption = new String[20];

	public boolean[] aHover = new boolean[10];
	public boolean[] pHover = new boolean[10];
	public boolean[] iHover = new boolean[10];
	public boolean[] sHover = new boolean[10];
	public boolean okHover = false;
	public boolean acHover = false;
	public boolean colHover = false;

	public int editType = 0;
	public int editSlot = 0;
	public int editArmor = 0;
	public int editCur = 0;
	public boolean editing = false;
	public boolean[] soundHover = new boolean[10];

	public boolean[] cHover = new boolean[100];
	public int curBind = 0;
	public boolean binding = false;

	public int[] bind = new int[100];

	int sp = 60;
	int dX = 400;
	int dY = 180;
	int w = 384;
	int h = 48;

	int curEditLoad = 0;

	public GameMenu() {
		reset();
		for (int i = 0; i < 20; i++) {
			hover[i] = false;
			caption[i] = "invis";
		}
		for (int i = 0; i < 100; i++) {
			bind[i] = 0;
		}
		for (int i = 0; i < 4; i++) {
			soundHover[i] = false;
			if (i < 3) {
				aHover[i] = false;
			}
		}
		changeCap(0);
	}

	void reset() {
		load = Options.load;
		OptionsXML freshie = new OptionsXML();
		for (int i = 0; i < 100; i++) {
			bind[i] = Options.bind[i];
			if(bind[i] == 0) {
				bind[i] = freshie.bind[i];
				Options.bind[i] = bind[i];
			}
		}
		
		fullScreen = Options.fullScreen;
		music = Options.music;
		sound = Options.sound;
	}

	public int state() {
		return this.state;
	}

	public String getBindName(int b) {
		if (bind[b] < 256) {
			return com.badlogic.gdx.Input.Keys.toString(bind[b]);
		} else {
			int m = bind[b] - 256;
			return "Mouse " + Integer.toString(m);
		}
	}

	public String getBindFunction(int b) {
		switch (b) {
		default:
			return "invis";
		case 0:
			return "Forward";
		case 1:
			return "Backward";
		case 2:
			return "Strafe Left";
		case 3:
			return "Strafe Right";
		case 4:
			return "Fire";
		case 5:
			return "Change Weapon";
		case 6:
			return "Menu";
		case 7:
			return "Chat";
		case 8:
			return "Game Info";
		case 9:
			return "Quick Loadout";
		case 10:
			return "Edit Loadouts";
		case 11:
			return "Repair Kit";
		case 12:
			return "Shield";
		case 13:
			return "Turbo";
		case 14:
			return "Chat Scroll Up";
		case 15:
			return "Chat Scroll Down";
		case 16:
			return "Resize/Hide Chat";
		case 17:
			return "Change Team";
		case 18:
			return "Leave Game";
		case 19:
			return "Reload";
		case 20:
			return "Suicide";
		case 21:
			return "Spectator Mode";
		case 22:
			return "Next Player";
		case 23:
			return "Toggle map";
		}
	}

	void changeCap(int gs) {
		if (gs == 3) {
			for (int i = 0; i < 5; i++) {
				caption[0] = "Quit";
				caption[1] = "Return to Lobby";
				caption[2] = "Options";
				caption[3] = "Loadouts";
				caption[4] = "Resume";
			}
		} else {
			for (int i = 0; i < 5; i++) {
				caption[0] = "Quit";
				caption[1] = "Credits";
				caption[2] = "Options";
				caption[3] = "Loadouts";
				caption[4] = "Resume";
			}
		}
	}

	public void update(GameScreen screen) {
		this.screen = screen;
		Scene scene = screen.scene;
		if (scene.tick > editClick + 500) {
			firstClick = false;
		}
		Input input = scene.input;
		int x = input.mouseX;
		int y = input.mouseY;

		if (!binding) {
			checkEscape(screen);
			switch (state) {
			case 5:
				checkAudio(scene);
				break;
			case 6:
				checkControls(scene);
				break;
			case 8:
				if (editing) {
					checkEdits(scene);
				} else {
					checkLoadouts(scene);
				}
				break;
			}
			if (editing) {
				return;
			}
			for (int i = 0; i < 10; i++) {
				if (scene.inBox(x, y, dX, dY + (i * 60), w, h)) {
					hover[i] = true;
					if (input.mouseDown) {
						if (input.wasMouseJustClicked) {
							input.wasMouseJustClicked = false;
							click(screen, i);
						}
					}
				} else {
					hover[i] = false;
				}
			}
		} else {
			if (!input.keyPress.isEmpty()) {
				boolean first = true;
				for (Integer i : input.keyPress) {
					if (first) {
						first = false;
						binding = false;
						bind[curBind] = i;
					}
				}
				input.keyPress.clear();
			} else if (input.mouseDown) {
				binding = false;
				bind[curBind] = 256 + input.mouseButton;
				input.mouseDown = false;
				input.wasMouseJustClicked = false;
			}

		}
	}

	void checkEscape(GameScreen screen) {
		if (state == 7) {
			if (screen.scene.checkBindPress(10)) {
				screen.scene.playSound(23, 1, 1);
				fromSource = 0;
				state = 0;
				screen.scene.clearInput();
			}
		}
		if (state == 9) {
			if (screen.scene.checkBindPress(9)) {
				screen.scene.playSound(23, 1, 1);
				fromSource = 0;
				state = 0;
				screen.scene.clearInput();
			}
		}
		
		if (screen.scene.tick > screen.escCoolAt) {
			if (screen.scene.checkBindPress(6)) { // esc
				screen.scene.playSound(24, 1, 1);
				screen.scene.clearInput();
				screen.escCoolAt = screen.scene.tick + 50;
				if (editing) {
					finishEdit(screen.scene);
					return;
				}
				switch (state) {
				case 0:
					state = 0;
					break;
				case 1:
					state = 0;
					break;
				case 2:
					state = 1;
					break;
				case 3:
					state = 1;
					break;
				case 4:
					state = 3;
					saveOptions();
					break;
				case 5:
					state = 3;
					saveOptions();
					break;
				case 6:
					state = 3;
					saveOptions();
					break;
				case 7:
					state = 1;
					if (fromSource > 0) {
						fromSource = 0;
						state = 0;
					}
					break;
				case 8:
					state = 7;
					saveOptions();
					break;
				case 9:
					state = 0;
					break;
				}
			}
		}

	}

	void checkLoadouts(Scene scene) {
		Input input = scene.input;
		int x = input.mouseX;
		int y = input.mouseY;

		for (int b = 0; b < 3; b++) {
			if (scene.inBox(x, y, 250 + (150 * b), dY - 30, 120, 24)) {
				if (input.mouseDown) {
					if (input.wasMouseJustClicked) {
						input.wasMouseJustClicked = false;
						load[curEditLoad].armor = b;
						int prims = 0;
						int secs = 0;
						int its = 0;
						int mprims = ShipDef.getArmorPrimaries(b);
						int msecs = ShipDef.getArmorSecondaries(b);
						int mits = ShipDef.getArmorItems(b);
						List<Integer> drops = new LinkedList<Integer>();
						for (Integer i : load[curEditLoad].weapons) {
							if (i >= 0) {
								WeaponDef ws = Entities.weapons[i];
								if (ws.primary) {
									prims++;
									if (prims > mprims) {
										drops.add(i);
									}
								} else {
									secs++;
									if (secs > msecs) {
										drops.add(i);
									}
								}
							}
						}
						for (Integer d : drops) {
							load[curEditLoad].weapons.remove(d);
						}
						drops.clear();
						for (Integer i : load[curEditLoad].items) {
							if (i > 0) {
								its++;
								if (its > mits) {
									drops.add(i);
								}
							}
						}
						for (Integer d : drops) {
							load[curEditLoad].items.remove(d);
						}
					}
				}
				aHover[b] = true;
			} else {
				aHover[b] = false;
			}
			int armor = load[curEditLoad].armor;
			int d = ShipDef.getArmorPrimaries(armor);
			int f = ShipDef.getArmorSecondaries(armor);
			int q = ShipDef.getArmorItems(armor);
			Integer[] prims = new Integer[20];
			Integer[] secs = new Integer[20];
			Integer[] items = new Integer[20];
			for (int i = 0; i < 20; i++) {
				prims[i] = -2;
				secs[i] = -2;
				items[i] = -2;
			}
			int prim = 0;
			int sec = 0;
			int it = 0;
			for (Integer i : load[curEditLoad].items) {
				items[it] = i;
				it++;
			}
			for (Integer i : load[curEditLoad].weapons) {
				WeaponDef ws = Entities.weapons[i];
				if (ws.primary) {
					prims[prim] = i;
					prim++;
				} else {
					secs[sec] = i;
					sec++;
				}
			}
			for (int p = 0; p < d; p++) {
				if (scene.inBox(x, y, 208 + 200 / 2 + 45, 235 + p * 35, 80, 24)) {
					if (input.mouseDown) {
						if (input.wasMouseJustClicked) {
							input.wasMouseJustClicked = false;
							startEdit(0, p, armor, prims[p]);
						}
					}
					pHover[p] = true;
				} else {
					pHover[p] = false;
				}
			}
			for (int s = 0; s < f; s++) {
				if (scene.inBox(x, y, 683, 235 + s * 35, 80, 24)) {
					sHover[s] = true;
					if (input.mouseDown) {
						if (input.wasMouseJustClicked) {
							input.wasMouseJustClicked = false;
							startEdit(1, s, armor, secs[s]);
						}
					}
				} else {
					sHover[s] = false;
				}
			}
			int u = 0;
			for (int v = 0; v < q; v++) {
				if (v < 2) {
					u = v;
				} else {
					u = v - 2;
				}
				if (scene.inBox(x, y, 353, 390 + v * 35, 80, 24) && v < 2) {
					iHover[v] = true;
					if (input.mouseDown) {
						if (input.wasMouseJustClicked) {
							input.wasMouseJustClicked = false;
							startEdit(2, v, armor, items[v]);
						}
					}
				} else if (scene.inBox(x, y, 683, 390 + u * 35, 80, 24) && v >= 2) {
					iHover[v] = true;
					if (input.mouseDown) {
						if (input.wasMouseJustClicked) {
							input.wasMouseJustClicked = false;
							startEdit(2, v, armor, items[v]);
						}
					}
				} else {
					iHover[v] = false;
				}
			}
			if (scene.inBox(x, y, 655, 110, 120, 30)) {
				acHover = true;
				if (input.mouseDown) {
					if (input.wasMouseJustClicked) {
						input.wasMouseJustClicked = false;
						if (curEditLoad != scene.curLoad) {
							scene.curLoad = curEditLoad;
						}
					}
				}
			} else {
				acHover = false;
			}
			if (scene.inBox(x, y, 145, 110, 120, 30)) {
				colHover = true;
				if (input.mouseDown) {
					if (input.wasMouseJustClicked) {
						input.wasMouseJustClicked = false;
						load[curEditLoad].col = Entities.randomBrightColor();
					}
				}
			} else {
				colHover = false;
			}
		}
	}

	void checkEdits(Scene scene) {
		Input input = scene.input;
		int x = input.mouseX;
		int y = input.mouseY;
		int d = editSel;
		if (scene.inBox(x, y, 400, 508, 180, 40)) {
			okHover = true;
			if (input.mouseDown) {
				if (input.wasMouseJustClicked) {
					input.wasMouseJustClicked = false;
					input.mouseDown = false;
					finishEdit(scene);
				}
			}
		} else {
			okHover = false;
		}
		for (int b = 0; b < 20 && b < editCount; b++) {
			if (scene.inBox(x, y, 400, 85 + (b * 20), 200, 20)) {
				if (input.mouseDown) {
					if (b < editCount) {
						editSel = b;
					}
					if (input.wasMouseJustClicked) {

						if (firstClick && input.wasMouseJustReleased && editSel == d) {
							// dbl click
							finishEdit(scene);
							return;
						} else {
							firstClick = true;
							input.wasMouseJustReleased = false;
							editClick = scene.tick;
						}
						break;
					}
				}
			}
		}
	}

	Integer[] editList = new Integer[20];
	int editSel = 0;
	int editCount = 0;
	int editInstance = 0;
	Integer[] gprims = new Integer[20];
	Integer[] gsecs = new Integer[20];
	Integer[] gitems = new Integer[20];

	void startEdit(int type, int slot, int armor, int current) {
		editing = true;
		editType = type;
		editSlot = slot;
		editArmor = armor;
		editCur = current;
		editCount = 0;
		editList = new Integer[20];
		for (int i = 0; i < 20; i++) {
			editList[i] = -2;
		}
		gprims = new Integer[20];
		gsecs = new Integer[20];
		gitems = new Integer[20];
		for (int i = 0; i < 20; i++) {
			gprims[i] = -2;
			gsecs[i] = -2;
			gitems[i] = -2;
		}
		int prim = 0;
		int sec = 0;
		int it = 0;
		for (Integer i : load[curEditLoad].items) {
			gitems[it] = i;
			it++;
		}
		for (Integer i : load[curEditLoad].weapons) {
			WeaponDef ws = Entities.weapons[i];
			if (ws.primary) {
				gprims[prim] = i;
				prim++;
			} else {
				gsecs[sec] = i;
				sec++;
			}
		}

		switch (type) {
		case 0:
			if (slot > 0) {
				for (int z = 0; z < slot; z++) {
					if (gprims[z] == current) {
						editInstance++;
					}
				}
			} else {
				editInstance = 0;
			}
			for (WeaponDef w : Entities.weapons) {
				if (w != null && w.type > 0 && w.primary && ShipDef.canWear(armor, w)) {
					editList[editCount] = w.type;
					editSel = 0;
					if (w.type == current) {
						editSel = editCount;
					}
					editCount++;
				}
			}
			break;
		case 1:
			if (slot > 0) {
				for (int z = 0; z < slot; z++) {
					if (gsecs[z] == current) {
						editInstance++;
					}
				}
			} else {
				editInstance = 0;
			}
			for (WeaponDef w : Entities.weapons) {
				if (w != null && w.type > 0 && !w.primary && ShipDef.canWear(armor, w)) {
					editList[editCount] = w.type;
					editSel = 0;
					if (w.type == current) {
						editSel = editCount;
					}
					editCount++;
				}
			}
			break;
		case 2:
			if (slot > 0) {
				for (int z = 0; z < slot; z++) {
					if (gitems[z] == current) {
						editInstance++;
					}
				}
			} else {
				editInstance = 0;
			}
			for (ItemDef w : Entities.items) {
				if (w != null && w.type > 0) {
					editList[editCount] = w.type;
					editSel = 0;
					if (w.type == current) {
						editSel = editCount;
					}
					editCount++;
				}
			}
			break;
		}
		if (editCur < 0) {
			// editInstance = 0;
		}
	}

	@SuppressWarnings("unused")
	void finishEdit(Scene scene) {
		editing = false;
		if (editCur < 0) {
			if (editType < 2) {
				load[curEditLoad].weapons.add(editList[editSel]);
			} else {
				load[curEditLoad].items.add(editList[editSel]);
			}
		}
		if (editList[editSel] != editCur) {
			int index = 0;
			if (editType < 2) {
				for (Integer ii : load[curEditLoad].weapons) {
					if (load[curEditLoad].weapons.get(index) == editCur) {
						if (editInstance <= 0) {
							load[curEditLoad].weapons.set(index, editList[editSel]);
							break;
						} else {
							editInstance--;
						}
					}
					index++;
				}
			} else {
				for (Integer ii : load[curEditLoad].items) {
					if (load[curEditLoad].items.get(index) == editCur) {
						if (editInstance <= 0) {
							load[curEditLoad].items.set(index, editList[editSel]);
							break;
						} else {
							editInstance--;
						}
					}
					index++;
				}
			}
		}
		scene.input.mouseDown = false;
		scene.input.wasMouseJustClicked = false;
	}

	void checkControls(Scene scene) {
		Input input = scene.input;
		int x = input.mouseX;
		int y = input.mouseY;
		for (int b = 0; b < 9; b++) {
			if (scene.inBox(x, y, 570, 175 + (b * 30), 120, 24)) {
				if (input.mouseDown) {
					if (input.wasMouseJustClicked) {
						input.wasMouseJustClicked = false;
						startBind(b + cScroll, input);
					}
				}
				cHover[b + cScroll] = true;
			} else {
				cHover[b + cScroll] = false;
			}
		}
		if (scene.inBox(x, y, 660, 180, 32, 32)) {
			sHover[0] = true;
			if (input.mouseDown) {
				if (input.wasMouseJustClicked) {
					input.wasMouseJustClicked = false;
					cScroll--;
					if (cScroll < 0) {
						cScroll = 0;
					}
				}
			}
		} else {
			sHover[0] = false;
		}
		if (scene.inBox(x, y, 660, 410, 32, 32)) {
			sHover[1] = true;
			if (input.mouseDown) {
				if (input.wasMouseJustClicked) {
					input.wasMouseJustClicked = false;
					cScroll++;
					if (cScroll > maxbind) {
						cScroll = maxbind;
					}
				}
			}
		} else {
			sHover[1] = false;
		}

	}

	void startBind(int b, Input input) {
		input.keyPress.clear();
		input.mouseDown = false;
		binding = true;
		curBind = b;
	}

	void checkAudio(Scene scene) {
		Input input = scene.input;
		int x = input.mouseX;
		int y = input.mouseY;
		int oldMusic = music;
		int oldSound = sound;
		if (scene.inBox(x, y, 304 + 8, dY + 66 - 8, 16, 16)) {
			if (input.mouseDown) {
				music -= 1;
			}
			soundHover[0] = true;
		} else {
			soundHover[0] = false;
		}
		if (scene.inBox(x, y, 304 + 8 + 192, dY + 66 - 8, 16, 16)) {
			if (input.mouseDown) {
				music += 1;
			}
			soundHover[1] = true;
		} else {
			soundHover[1] = false;
		}
		if (scene.inBox(x, y, 304 + 8, dY + 66 - 8 + 60, 16, 16)) {
			if (input.mouseDown) {
				sound -= 1;
			}
			soundHover[2] = true;
		} else {
			soundHover[2] = false;
		}
		if (scene.inBox(x, y, 304 + 8 + 192, dY + 66 - 8 + 60, 16, 16)) {
			if (input.mouseDown) {
				sound += 1;
			}
			soundHover[3] = true;
		} else {
			soundHover[3] = false;
		}
		if (oldMusic != music || oldSound != sound) {
			if (music < 0) {
				music = 0;
			}
			if (music > 100) {
				music = 100;
			}
			if (sound < 0) {
				sound = 0;
			}
			if (sound > 100) {
				sound = 100;
			}
			Options.music = music;
			Options.sound = sound;
			for (int i = 0; i < 11; i++) {
				if (AssetLoader.mus[i].isPlaying()) {
					AssetLoader.mus[i].setVolume(Options.music());
				}
			}
		}
	}

	void click(GameScreen screen, int i) {
		this.screen = screen;
		if (state != 5) {
			screen.scene.clearInput();
		}
		switch (state) {
		case 1: // main menu
			switch (i) {
			case 0: // quit
				System.exit(0);
				break;
			case 1: // lobby/credits
				if (screen.gameState == 3) { // return to lobby
					screen.net.client.close();
					if (screen.connectedLobby) {
						screen.gameState = 8;
					} else {
						screen.gameState = 0;
					}
					state = 0;
				} else {
					// creditsssss
					state = 2;
				}
				break;
			case 2:// options
				state = 3;
				break;
			case 3: // loadouts
				state = 7;
				break;
			case 4: // resume
				state = 0;
				screen.scene.input.mouseDown = false;
				break;
			}
			break;
		case 2: // credits
			switch (i) {
			case 4:
				// back
				state = 1;
				break;
			}
			break;
		case 3: // options
			switch (i) {
			case 0: // video
				state = 4;
				break;
			case 1: // audio
				state = 5;
				break;
			case 2: // controls
				Options.beenControls = true;
				state = 6;
				break;
			case 3: // empty
				break;
			case 4:
				// back
				state = 1;
				break;
			}
			break;
		case 4: // video options
			switch (i) {
			case 2:
				fullScreen = 1 - fullScreen;
				break;
			case 4:
				// back
				state = 3;
				// if(fullScreen != Config.fullScreen) { //change video and save
				// change
				saveOptions();

				break;
			}
			break;
		case 5: // audio options
			switch (i) {
			case 4:
				// back
				state = 3;
				saveOptions();
				break;
			}
			break;
		case 6: // controls
			switch (i) {
			case 5:
				// back
				state = 3;
				saveOptions();
				break;
			}
			break;
		case 7: // loadouts
			switch (i) {
			default:
				state = 8;
				Options.beenLoadout = true;
				curEditLoad = i;
				break;
			case 5: // back
				state = 1;
				if (fromSource > 0) {
					fromSource = 0;
					state = 0;
				}
				break;
			}
			break;
		case 8: // edit loadout
			switch (i) {
			case 5: // back
				state = 7;
				saveOptions();
				break;
			}
			break;
		case 9:
			switch (i) {
			default:
				state = 0;
				screen.scene.curLoad = i;
				saveOptions();
				break;
			case 5:
				state = 0;
				break;
			}
			break;
		}

	}

	void saveOptions() {
		Options.version = OptionsXML.getVersion();
		XStream xstream = new XStream(new StaxDriver());
		FileHandle fh = Gdx.files.local("option.xml");
		OptionsXML opt = new OptionsXML();
		opt.fullScreen = fullScreen;
		opt.music = Options.music;
		opt.sound = Options.sound;
		for (int i = 0; i < 11; i++) {
			if (AssetLoader.mus[i].isPlaying()) {
				AssetLoader.mus[i].setVolume(Options.music());
			}
		}
		opt.beenControls = Options.beenControls;
		opt.beenLoadout = Options.beenLoadout;
		for (int i = 0; i < 100; i++) {
			opt.bind[i] = bind[i];
			Options.bind[i] = bind[i];
		}
		for (int i = 0; i < 5; i++) {
			opt.load[i] = load[i];
		}
		for (int i = 0; i < 5; i++) {
			Options.load[i] = load[i];
		}
		String s = xstream.toXML(opt);
		fh.writeString(s, false);
	}

	String getSlotArmorName(int slot) {
		int t = load[slot].armor;
		return ShipDef.getArmorName(t);
	}

	public boolean binding() {
		return binding;
	}

	public void open(int gs, int i) {
		state = i;
		reset();
		changeCap(gs);
	}

	public void render(GameScreen screen) {
		if (screen.gameState == 3) {
			screen.xO = screen.scene.players[screen.myIndex].drawX - 400;
			screen.yO = screen.scene.players[screen.myIndex].drawY - 300;
		}
		switch (state) {
		case 1:
			renderMainMenu(screen);
			break;
		case 2: // credits
			renderCredits(screen);
			break;
		case 3: // options
			renderOptions(screen);
			break;
		case 4: // video options
			renderVideo(screen);
			break;
		case 5: // audio options
			renderAudio(screen);
			break;
		case 6: // controls
			renderControls(screen);
			break;
		case 7: // loadouts
			renderLoadout(screen, "Managed Loadouts");
			break;
		case 8: // edit loadout
			renderEditLoadout(screen);
			break;
		case 9:
			renderLoadout(screen, "Pick Loadout");
			break;
		}
		if (screen.gameState == 3) {
			screen.xO = 0;
			screen.yO = 0;
		}
	}

	void renderMainMenu(GameScreen screen) {
		screen.drawFrame(192, 140, 416, 320, true);
		for (int i = 0; i < 5; i++) {
			if (!caption[i].equals("invis")) {
				screen.scene.drawButton(hover[i], dX, dY + (i * 60), w, h, true);
				screen.drawFont(0, dX, dY + (i * 60), caption[i], true, 2);
			}
		}
	}

	void renderCredits(GameScreen screen) {
		screen.drawFrame(192, 140, 416, 320, true);
		screen.scene.drawButton(hover[4], dX, dY + (4 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (4 * 60), "Back", true, 2);
	}

	void renderOptions(GameScreen screen) {
		screen.drawFrame(192, 140, 416, 320, true);
		screen.scene.drawButton(hover[0], dX, dY + (0 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (0 * 60), "Video Options", true, 2);
		screen.scene.drawButton(hover[1], dX, dY + (1 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (1 * 60), "Audio Options", true, 2);
		screen.scene.drawButton(hover[2], dX, dY + (2 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (2 * 60), "Controls", true, 2);
		screen.scene.drawButton(hover[4], dX, dY + (4 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (4 * 60), "Back", true, 2);
	}

	void renderVideo(GameScreen screen) {
		screen.drawFrame(192, 140, 416, 320, true);
		screen.drawFont(0, 400, 180, "Video Options", true, 2);
		int b = fullScreen;
		String s = "On";
		if (b == 0) {
			s = "Off";
		}
		screen.scene.drawButton(hover[2], dX, dY + (2 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (2 * 60), "Fullscreen: " + s, true, 2);
		screen.drawFont(0, 400, 380, "Changes take effect on restart", true, 1);
		screen.scene.drawButton(hover[4], dX, dY + (4 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (4 * 60), "Back", true, 2);
	}

	void renderAudio(GameScreen screen) {
		screen.drawFrame(192, 140, 416, 320, true);
		screen.drawFont(0, 400, 180, "Audio Options", true, 2);
		screen.scene.drawButton(hover[4], dX, dY + (4 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (4 * 60), "Back", true, 2);

		screen.drawFont(0, dX - 150, dY + (1 * 60) - 2, "Music", true, 1);
		screen.drawFont(0, dX - 150, dY + (2 * 60) - 2, "Sound", true, 1);
		screen.drawFont(0, dX - 150 + 304, dY + (1 * 60) - 2, Integer.toString(music), true, 1);
		screen.drawFont(0, dX - 150 + 304, dY + (2 * 60) - 2, Integer.toString(sound), true, 1);
		for (int sx = 320; sx < 512; sx += 32) {
			screen.drawRegion(AssetLoader.frame[7], sx, dY + (1 * 60) - 32, false, 0, 1);
			screen.drawRegion(AssetLoader.frame[7], sx, dY + (2 * 60) - 32, false, 0, 1);
		}
		int o = 0;
		o = 11;
		// 0
		if (soundHover[0]) {
			o += 2;
		}
		screen.drawRegion(AssetLoader.frame[o], 304, dY + (1 * 60) + 6, false, -90, 1);
		o = 11;
		// 2
		if (soundHover[2]) {
			o += 2;
		}
		screen.drawRegion(AssetLoader.frame[o], 304, dY + (2 * 60) + 6, false, -90, 1);
		o = 12;
		if (soundHover[1]) {
			o += 2;
		}
		// 1
		screen.drawRegion(AssetLoader.frame[o], 304 + 192, dY + (1 * 60) + 6, false, -90, 1);
		o = 12;
		if (soundHover[3]) {
			o += 2;
		}
		// 3
		screen.drawRegion(AssetLoader.frame[o], 304 + 192, dY + (2 * 60) + 6, false, -90, 1);
		float mus = (float) music / 100f;
		int musX = (int) (mus * 164f) + 320;
		float sou = (float) sound / 100f;
		int soundX = (int) (sou * 162f) + 320;
		screen.drawRegion(AssetLoader.frame[16], musX, dY + (1 * 60) - 10, false, 0, 1);
		screen.drawRegion(AssetLoader.frame[16], soundX, dY + (2 * 60) - 10, false, 0, 1);
	}

	void renderControls(GameScreen screen) {
		screen.drawFrame(96, 76, 608, 448, true);
		screen.drawFont(0, 400, 116, "Controls", true, 2);
		screen.yO += 15;
		screen.drawFrame(110, 135, 580, 292, false);

		for (int b = 0; b < 9; b++) {
			screen.drawFont(0, 190, 160 + (b * 30), getBindFunction(b + cScroll), true, 1);
			if (binding && curBind == b + cScroll) {
				screen.scene.drawTextArea(true, 400, 160 + (b * 30), 220, true);
				screen.drawFont(0, 400, 160 + (b * 30), "Waiting for input", true, 1);
			} else {
				screen.scene.drawTextArea(false, 400, 160 + (b * 30), 220, true);
				screen.drawFont(0, 400, 160 + (b * 30), getBindName(b + cScroll), true, 1);
			}
			screen.scene.drawButton(cHover[b + cScroll], 570, 160 + (b * 30), 120, 24, true);
			screen.drawFont(0, 570, 160 + (b * 30), "Set", true, 1);
		}
		screen.scene.drawButton(sHover[0], 660, 165, 32, 32, true);
		screen.drawFont(0, 660, 165, "/\\", true, 1);
		screen.scene.drawButton(sHover[1], 660, 395, 32, 32, true);
		screen.drawFont(0, 660, 395, "\\/", true, 1);
		screen.yO -= 15;
		screen.scene.drawButton(hover[5], dX, dY + (5 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (5 * 60), "Back", true, 2);
	}

	void renderLoadout(GameScreen screen, String s) {
		screen.drawFrame(192, 76, 416, 448, true);
		screen.drawFont(0, 400, 116, s, true, 2);
		screen.scene.drawButton(hover[0], dX, dY + (0 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (0 * 60), "1: " + getSlotArmorName(0), true, 2);
		screen.scene.drawButton(hover[1], dX, dY + (1 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (1 * 60), "2: " + getSlotArmorName(1), true, 2);
		screen.scene.drawButton(hover[2], dX, dY + (2 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (2 * 60), "3: " + getSlotArmorName(2), true, 2);
		screen.scene.drawButton(hover[3], dX, dY + (3 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (3 * 60), "4: " + getSlotArmorName(3), true, 2);
		screen.scene.drawButton(hover[4], dX, dY + (4 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (4 * 60), "5: " + getSlotArmorName(4), true, 2);
		screen.batcher.setColor(Color.GREEN);
		screen.drawRegion(AssetLoader.getSprite("bullet0"), 230, dY + (screen.scene.curLoad * 60), true, 0, 1);
		screen.drawRegion(AssetLoader.getSprite("bullet0"), 570, dY + (screen.scene.curLoad * 60), true, 0, 1);
		screen.batcher.setColor(Color.WHITE);
		screen.scene.drawButton(hover[5], dX, dY + (5 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (5 * 60), "Back", true, 2);
	}

	void renderEditLoadout(GameScreen screen) {
		int armor = load[curEditLoad].armor;
		screen.drawFrame(48, 76, 704, 448, true);
		screen.drawFont(0, 400, 116, "Editing Loadout " + Integer.toString(curEditLoad + 1), true, 2);
		int dd = -30;
		screen.scene.drawButton(aHover[0] || armor == 0, 250, dY + (0 * 60) + dd, 120, 24, true);
		screen.drawFont(0, 250, dY + (0 * 60) + dd, "Light", true, 1);
		screen.scene.drawButton(aHover[1] || armor == 1, 400, dY + (0 * 60) + dd, 120, 24, true);
		screen.drawFont(0, 400, dY + (0 * 60) + dd, "Medium", true, 1);
		screen.scene.drawButton(aHover[2] || armor == 2, 550, dY + (0 * 60) + dd, 120, 24, true);
		screen.drawFont(0, 550, dY + (0 * 60) + dd, "Heavy", true, 1);
		int d = ShipDef.getArmorPrimaries(armor);
		int f = ShipDef.getArmorSecondaries(armor);
		screen.drawFont(0, 400, 180, "You can carry " + d + " primaries and " + f + " secondaries", true, 1);
		screen.yO -= 25;
		screen.drawFont(0, 230, 230, "Primaries", true, 1.4f);
		screen.drawFont(0, 570, 230, "Secondaries", true, 1.4f);
		// Object[] buzz =
		Integer[] prims = new Integer[20];
		Integer[] secs = new Integer[20];
		Integer[] items = new Integer[20];
		int prim = 0;
		int sec = 0;
		int it = 0;
		for (Integer i : load[curEditLoad].items) {
			items[it] = i;
			it++;
		}
		for (Integer i : load[curEditLoad].weapons) {
			WeaponDef ws = Entities.weapons[i];
			if (ws.primary) {
				prims[prim] = i;
				prim++;
			} else {
				secs[sec] = i;
				sec++;
			}
		}
		for (int p = 0; p < d; p++) {
			screen.scene.drawTextArea(false, 190, 265 + p * 35, 230, true);
			if (p < prim) {
				WeaponDef ws = Entities.weapons[prims[p]];
				screen.drawFont(0, 188, 265 + p * 35, ws.name, true, 1);
			}
			screen.scene.drawButton(pHover[p], 353, 265 + p * 35, 80, 24, true);
			screen.drawFont(0, 353, 265 + p * 35, "Change", true, 1);
		}
		for (int p = 0; p < f; p++) {
			screen.scene.drawTextArea(false, 520, 265 + p * 35, 230, true);
			if (p < sec) {
				WeaponDef ws = Entities.weapons[secs[p]];
				screen.drawFont(0, 518, 265 + p * 35, ws.name, true, 1);
			}
			screen.scene.drawButton(sHover[p], 683, 265 + p * 35, 80, 24, true);
			screen.drawFont(0, 683, 265 + p * 35, "Change", true, 1);
		}
		screen.yO += 25;
		int q = ShipDef.getArmorItems(armor);
		screen.drawFont(0, 400, 350, "Items", true, 1.4f);
		for (int v = 0; v < q; v++) {
			if (v < 2) {
				screen.scene.drawTextArea(false, 190, 390 + v * 35, 230, true);
				if (v < it) {
					screen.drawFont(0, 188, 390 + v * 35, ItemDef.getItemPackName(items[v]), true, 1);
				}
				screen.scene.drawButton(iHover[v], 353, 390 + v * 35, 80, 24, true);
				screen.drawFont(0, 353, 390 + v * 35, "Change", true, 1);
			} else {
				screen.scene.drawTextArea(false, 520, 390 + (v - 2) * 35, 230, true);
				if (v < it) {
					screen.drawFont(0, 518, 390 + (v - 2) * 35, ItemDef.getItemPackName(items[v]), true, 1);
				}
				screen.scene.drawButton(iHover[v], 683, 390 + (v - 2) * 35, 80, 24, true);
				screen.drawFont(0, 683, 390 + (v - 2) * 35, "Change", true, 1);
			}
		}
		screen.scene.drawButton(hover[5], dX, dY + (5 * 60), w, h, true);
		screen.drawFont(0, dX, dY + (5 * 60), "Back", true, 2);
		String act = "";
		boolean ac = (curEditLoad == screen.scene.curLoad);
		if (ac) {
			act = "Active";
		} else {
			act = "Make Active";
		}
		screen.scene.drawButton(acHover || ac, 655, 110, 120, 30, true);
		screen.drawFont(0, 655, 110, act, true, 1);
		if (ac) {
			screen.batcher.setColor(Color.GREEN);
			screen.drawRegion(AssetLoader.getSprite("bullet0"), 610, 110, true, 0, 1);
			screen.drawRegion(AssetLoader.getSprite("bullet0"), 700, 110, true, 0, 1);
			screen.batcher.setColor(Color.WHITE);
		}
		screen.scene.drawButton(colHover, 145, 110, 120, 30, true);
		screen.drawFont(0, 145, 110, "New Color", true, 1);
		float ox = 0, oy = 0;
		if (screen.gameState == 3) {
			ox = screen.xO + screen.originX;
			oy = screen.yO + screen.originY;
		} else {
			ox = 0;
			oy = 0;
		}
		screen.batcher.setColor(load[curEditLoad].col);
		screen.drawRegion(AssetLoader.getSprite("shape1"), 135, 170, true, 0, ShipDef.getShipScale(armor));
		screen.batcher.setColor(Color.WHITE);
		if (editing) {
			screen.drawFrame(288, 60, 224, 480, true);
			screen.drawFrame(300, 70, 200, 410, false);
			for (int x = 300; x < 300 + 200 - 10; x += 2) {
				screen.batcher.draw(AssetLoader.frameTex, x + 5 + ox, 70 + (editSel * 20) + 5 + oy, 2, 20, 214, 42, 2,
						20, false, true);
			}
			for (int i = 0; i < 20; i++) {
				if (editList[i] >= 0) {
					if (editType < 2) {
						screen.drawFont(0, 400, 70 + i * 20 + 16, Entities.weapons[editList[i]].name, true, 1);
					} else {
						screen.drawFont(0, 400, 70 + i * 20 + 16, Entities.items[editList[i]].name, true, 1);
					}
				}
			}
			screen.scene.drawButton(okHover, 400, 508, 180, 40, true);
			screen.drawFont(0, 400, 508, "Ok", true, 1);
		}
	}

}
