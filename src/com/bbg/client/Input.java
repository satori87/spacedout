package com.bbg.client;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.InputProcessor;

public class Input implements InputProcessor {

	GameScreen screen;
	public List<Integer> keyPress; // store in parallel so we can process
									// first-time clicks
	public boolean[] keyDown; // as well as whats still being held down
	public boolean mouseDown = false;
	public boolean wasMouseJustClicked = false;
	public boolean wasMouseJustReleased = false;
	public boolean clickHandled = false;
	public int mouseDownX = 0;
	public int mouseDownY = 0;
	public int mouseUpX = 0;
	public int mouseUpY = 0;
	public int mouseX = 0;
	public int mouseY = 0;
	public int mouseButton = 0;

	public Input(GameScreen screen) {
		this.screen = screen;
		keyPress = new ArrayList<Integer>();
		keyDown = new boolean[300];
		for (int i = 0; i < 300; i++) {
			keyDown[i] = false;
		}
	}

	@Override
	public boolean keyDown(int keycode) {
		keyDown[keycode] = true;
		keyPress.add(keycode);
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		keyDown[keycode] = false;
		return true;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		int X = screen.getRelativeX(screenX);
		int Y = screen.getRelativeY(screenY);
		mouseDown = true;
		wasMouseJustClicked = true;
		mouseDownX = X;
		mouseDownY = Y;
		mouseButton = button;
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		int X = screen.getRelativeX(screenX);
		int Y = screen.getRelativeY(screenY);
		wasMouseJustReleased = true;
		mouseUpX = X;
		mouseUpY = Y;
		mouseDown = false;
		clickHandled = false;
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		int X = screen.getRelativeX(screenX);
		int Y = screen.getRelativeY(screenY);
		mouseX = X;
		mouseY = Y;
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		int X = screen.getRelativeX(screenX);
		int Y = screen.getRelativeY(screenY);
		mouseX = X;
		mouseY = Y;
		return true;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

}
