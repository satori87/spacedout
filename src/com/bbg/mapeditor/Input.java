package com.bbg.mapeditor;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.InputProcessor;

public class Input implements InputProcessor {

	GameScreen screen;
	public List<Integer> keyPress; //store in parallel so we can process first-time clicks
	public boolean [] keyDown;  //as well as whats still being held down
	public boolean mouseDown = false;
	public boolean wasMouseJustClicked = false;
	public boolean wasMouseJustReleased = false;
	
	public int mouseDownX = 0;
	public int mouseDownY = 0;
	public int mouseUpX = 0;
	public int mouseUpY = 0;
	public int mouseX = 0;
	public int mouseY = 0;
	
	public Input(GameScreen screen) {
		this.screen = screen;
		keyPress = new ArrayList<Integer>();
		keyDown = new boolean[256];
		for(int i=0;i<256;i++){keyDown[i]=false;}
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
		mouseDown = true;
		wasMouseJustClicked = true;
		mouseDownX = screenX;
		mouseDownY = screenY;
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		wasMouseJustReleased = true;
		mouseUpX = screenX;
		mouseUpY = screenY;
		mouseDown = false;
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		mouseX = screenX;
		mouseY = screenY;
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		mouseX = screenX;
		mouseY = screenY;
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
