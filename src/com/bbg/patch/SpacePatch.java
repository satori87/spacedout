package com.bbg.patch;

import com.badlogic.gdx.Game;
import com.bbg.patch.AssetLoader;
import com.bbg.patch.GameScreen;

public class SpacePatch extends Game {
	
	GameScreen screen;
    @Override
    public void create() {
        AssetLoader.load(true);
        screen = new GameScreen();
        setScreen(screen);
    }
    
    @Override
    public void dispose() {
        super.dispose();
        AssetLoader.dispose();
    }
    
}