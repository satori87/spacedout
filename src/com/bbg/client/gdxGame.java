package com.bbg.client;

import com.badlogic.gdx.Game;
import com.bbg.client.AssetLoader;
import com.bbg.client.GameScreen;

public class gdxGame extends Game {

    @Override
    public void create() {
        setScreen(new GameScreen());
    }

    @Override
    public void dispose() {
        super.dispose();
        AssetLoader.dispose();
    }

}
