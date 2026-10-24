/*
 * Copyright (C) 2014-2017 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.simsilica.es.Entity;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.FetchPortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.view.camera.ReaperChaseCamera;

/**
 * A state that handles ended game actions, such as cinematics and stuff
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class EndGameState extends AbstractAppState implements RawInputListener {

    private Main app;
    private AppStateManager stateManager;
    private final PlayerState playerState;
    private EntitySet portalGemFetchers;
    private ReaperChaseCamera chaseCamera;

    public EndGameState(PlayerState playerState) {
        this.playerState = playerState;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        this.stateManager = stateManager;

        // Add the listener
        app.getInputManager().addRawInputListener(this);

        // Add the texts
        playerState.setWideScreen(true);
        playerState.setGeneralText(2901);

        // The camera follows the Horned Reaper who comes for the Portal Gem
        portalGemFetchers = playerState.getEntityData().getEntities(FetchPortalGem.class, Position.class, CreatureComponent.class);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (portalGemFetchers == null) {
            return;
        }

        portalGemFetchers.applyChanges();
        for (Entity reaper : portalGemFetchers) {
            if (chaseCamera == null) {
                chaseCamera = new ReaperChaseCamera(app.getCamera(), reaper.get(CreatureComponent.class).height);
            }
            chaseCamera.update(reaper.get(Position.class).position, tpf);
            break;
        }
    }

    @Override
    public void cleanup() {
        app.getInputManager().removeRawInputListener(this);
        stopChase();
        super.cleanup();
    }

    /**
     * Gives the camera back, the player looks around again
     */
    private void stopChase() {
        if (portalGemFetchers != null) {
            portalGemFetchers.release();
            portalGemFetchers = null;
        }
    }

    @Override
    public void beginInput() {

    }

    @Override
    public void endInput() {

    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {

    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {

    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {

    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {

    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (evt.isPressed()) {
            if (evt.getKeyCode() == KeyInput.KEY_SPACE) {
                stateManager.detach(this);
                playerState.quitToDebriefing();
            } else if (evt.getKeyCode() == KeyInput.KEY_ESCAPE || evt.getKeyCode() == KeyInput.KEY_LCONTROL) {
                playerState.setWideScreen(false);
                playerState.setGeneralText(2901);
                cleanup();
                playerState.setEnabled(true);
            }
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {

    }

}
