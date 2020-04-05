package com.bbg.mapeditor;

import com.badlogic.gdx.Game;
import com.bbg.mapeditor.AssetLoader;
import com.bbg.mapeditor.GameScreen;

public class MapEditor extends Game {
	
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