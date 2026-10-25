/*
 * Copyright (C) 2014-2026 OpenKeeper
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
import com.jme3.app.state.AppStateManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.ManufactureProgress;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.gui.nifty.CustomTabControl;
import toniarts.openkeeper.gui.nifty.CustomTabGroupControl;
import toniarts.openkeeper.gui.nifty.jme.ClockSweepMesh;

/**
 * Shows how far the workshops are with the trap or door they are making, as
 * the original does: a clock sweep over the Workshop tab button and over the
 * item's button. The part the hand hasn't swept over yet is tinted purple
 * (0x780078), the hand starts at 12 o'clock and goes clockwise.<br>
 * Drawn on the GUI node over the Nifty buttons, so the panel isn't rebuilt.
 * The mesh only changes when the percentage does.
 */
public final class ManufactureProgressState extends AbstractPauseAwareState {

    private static final String TABS_ID = "tabs-hud";
    private static final String WORKSHOP_TAB_ID = "tab-workshop";
    private static final ColorRGBA TINT = new ColorRGBA(0x78 / 255f, 0, 0x78 / 255f, 1);

    /**
     * How often the buttons are looked up again, the manufacture tab is
     * rebuilt now and then
     */
    private static final float LOOKUP_INTERVAL = 0.25f;

    private final EntityData entityData;
    private final short playerId;
    private Main app;
    private EntitySet progressEntities;
    private ManufactureProgress progress;
    private Sweep tabSweep;
    private Sweep itemSweep;
    private float sinceLookup;

    public ManufactureProgressState(EntityData entityData, short playerId) {
        this.entityData = entityData;
        this.playerId = playerId;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;

        tabSweep = new Sweep("WorkshopTabProgress");
        itemSweep = new Sweep("ManufactureItemProgress");
        progressEntities = entityData.getEntities(new FieldFilter(Owner.class, "ownerId", playerId),
                ManufactureProgress.class, Owner.class);
        sinceLookup = LOOKUP_INTERVAL;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        boolean changed = progressEntities.applyChanges();
        if (changed) {
            progress = getProgress(progressEntities);
        }
        sinceLookup += tpf;
        if (changed || sinceLookup >= LOOKUP_INTERVAL) {
            sinceLookup = 0;
            lookUpButtons();
        }

        boolean hudShown = isHudShown();
        tabSweep.update(hudShown);
        itemSweep.update(hudShown);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (!enabled && isInitialized()) {
            tabSweep.update(false);
            itemSweep.update(false);
        }
    }

    @Override
    public void cleanup() {
        progressEntities.release();
        tabSweep.geometry.removeFromParent();
        itemSweep.geometry.removeFromParent();

        super.cleanup();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    private static ManufactureProgress getProgress(EntitySet entities) {
        for (Entity entity : entities) {
            return entity.get(ManufactureProgress.class);
        }
        return null;
    }

    private void lookUpButtons() {
        Screen hud = app.getNifty().getScreen(IPlayerScreenController.SCREEN_HUD_ID);
        float swept = progress != null ? progress.percent / 100f : 0;
        tabSweep.set(progress != null && hud != null ? getWorkshopTabButton(hud) : null, swept);
        itemSweep.set(progress != null && hud != null ? hud.findElementById(getItemButtonId(progress)) : null, swept);
    }

    private boolean isHudShown() {
        Nifty nifty = app.getNifty();
        Screen screen = nifty.getCurrentScreen();
        return isEnabled() && screen != null && IPlayerScreenController.SCREEN_HUD_ID.equals(screen.getScreenId());
    }

    private static Element getWorkshopTabButton(Screen hud) {
        CustomTabGroupControl tabs = hud.findNiftyControl(TABS_ID, CustomTabGroupControl.class);
        CustomTabControl tab = hud.findNiftyControl(WORKSHOP_TAB_ID, CustomTabControl.class);
        if (tabs == null || tab == null) {
            return null;
        }
        int index = tabs.indexOf(tab);
        return index >= 0 ? tabs.getTabButton(index) : null;
    }

    /**
     * The id of the item's button in the manufacture tab, see
     * PlayerScreenController.createIcon
     */
    private static String getItemButtonId(ManufactureProgress progress) {
        return (progress.type == ResearchableType.DOOR ? "door_" : "trap_") + progress.typeId;
    }

    /**
     * One sweep over a button
     */
    private final class Sweep {

        private final Geometry geometry;
        private final ClockSweepMesh mesh = new ClockSweepMesh();
        private Element button;
        private float swept = -1;

        private Sweep(String name) {
            geometry = new Geometry(name, mesh);
            Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            material.setColor("Color", TINT);

            // Tint what is under it, the button
            RenderState renderState = material.getAdditionalRenderState();
            renderState.setBlendMode(RenderState.BlendMode.Modulate);
            renderState.setFaceCullMode(RenderState.FaceCullMode.Off);
            renderState.setDepthTest(false);
            renderState.setDepthWrite(false);
            geometry.setMaterial(material);
            geometry.setCullHint(Spatial.CullHint.Always);
            app.getGuiNode().attachChild(geometry);
        }

        private void set(Element button, float swept) {
            this.button = button;
            if (button != null && swept != this.swept) {
                this.swept = swept;
                mesh.setSwept(swept);
                geometry.updateModelBound();
            }
        }

        private void update(boolean hudShown) {
            boolean shown = hudShown && button != null && swept > 0 && swept < 1 && button.isVisibleWithParent();
            geometry.setCullHint(shown ? Spatial.CullHint.Never : Spatial.CullHint.Always);
            if (shown) {

                // Nifty is y down, the GUI node y up
                int screenHeight = app.getCamera().getHeight();
                geometry.setLocalTranslation(button.getX(), screenHeight - button.getY() - button.getHeight(), 0);
                geometry.setLocalScale(button.getWidth(), button.getHeight(), 1);
            }
        }

    }

}
