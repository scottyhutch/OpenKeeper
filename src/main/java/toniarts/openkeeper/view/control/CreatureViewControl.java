/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Collection;
import java.util.Objects;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Morphed;
import toniarts.openkeeper.game.controller.SlapRules;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.text.EntityTextParser;

/**
 * View control that is intended specifically for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureViewControl extends EntityViewControl<Creature, Creature.AnimationType> {

    private final GameObject morphObject;
    private boolean morphed;

    /**
     * Creates a creature view control
     *
     * @param entityId the creature entity
     * @param entityData the entity data
     * @param data the creature
     * @param animation the starting animation
     * @param morphObject the object the Chicken spell turns creatures into,
     * can be {@code null}
     * @param assetManager the asset manager
     * @param textParser the tooltip parser, can be {@code null}
     */
    public CreatureViewControl(EntityId entityId, EntityData entityData, Creature data, Creature.AnimationType animation,
            GameObject morphObject, AssetManager assetManager, EntityTextParser<Creature> textParser) {
        super(entityId, entityData, data, animation, assetManager, textParser);
        this.morphObject = morphObject;

        morphed = isMorphed();
    }

    @Override
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        Collection<Class<? extends EntityComponent>> components = super.getWatchedComponents();
        components.add(CreatureAi.class);
        components.add(CreatureImprisoned.class);
        components.add(Morphed.class);
        return components;
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);

        // The Chicken spell changes the look, and back again when it wears off
        boolean nowMorphed = isMorphed();
        if (nowMorphed != morphed) {
            ArtResource before = currentState != null ? getAnimationData(currentState) : null;
            morphed = nowMorphed;
            if (currentState != null && before != getAnimationData(currentState)) {
                playAnimation(currentState);
            }
        }
    }

    private boolean isMorphed() {
        return morphObject != null && getEntity().get(Morphed.class) != null;
    }

    @Override
    public boolean isSlappable(short playerId) {
        Interaction interaction = getEntity().get(Interaction.class);
        if (interaction == null || !interaction.slappable) {
            return false;
        }

        boolean imprisoned = getEntity().get(CreatureImprisoned.class) != null;
        return SlapRules.canSlapCreature(playerId, getOwnerId(), getControlId(), imprisoned);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Play the starting animation
        if (spatial != null && currentState != null) {
            playAnimation(currentState);
        }
    }

    @Override
    public String getTooltip(short playerId) {
        String tooltip;
        if (getOwnerId() == playerId) {
            tooltip = Utils.getMainTextResourceBundle().getString("2841");
        } else {
            tooltip = Utils.getMainTextResourceBundle().getString(Integer.toString(getDataObject().getTooltipStringId()));
        }

        return textParser.parseText(tooltip, getEntity(), getDataObject());
    }


    @Override
    public boolean isStopAnimation() {
        return super.isStopAnimation() || (currentState != Creature.AnimationType.WALK && currentState != Creature.AnimationType.IN_HAND && !isWorkAnimation());
    }

    private void playAnimation(Creature.AnimationType animation) {
        AnimationLoader.playAnimation(getSpatial(), getAnimationData(animation), assetManager);
        if (getAnimationData(animation) == getDataObject().getAnimation(animation) && getDataObject().getFlags().contains(CreatureFlag.IS_UNIQUE)) {
            CreatureVariantTextures.apply(getSpatial(), assetManager);
        }
        isAnimationPlaying = true;
        currentState = animation;
    }

    @Override
    public void setTargetState(Creature.AnimationType state) {
        super.setTargetState(state);

        // Play immediately
        if (!Objects.equals(currentState, targetState)) {
            playAnimation(state);
        }
    }

    @Override
    public int getSlapEffectId(short playerId) {
        return getDataObject().getSlapEffectId();
    }

     @Override
    public int getDeathEffectId() {
        return getDataObject().getDeathEffectId();
    }

    @Override
    public ArtResource getInHandIcon() {
        return getDataObject().getIcon1Resource();
    }

    @Override
    public ArtResource getInHandMesh() {
        return getAnimationData(Creature.AnimationType.IN_HAND);
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    protected ArtResource getAnimationData(Creature.AnimationType state) {
        // A creature morphed into a chicken is drawn with the chicken object's mesh, also in hand,
        // but it dies with its own animations
        if (morphed && state != Creature.AnimationType.DIE && state != Creature.AnimationType.DEATH_POSE) {
            return state == Creature.AnimationType.IN_HAND ? morphObject.getInHandMeshResource() : morphObject.getMeshResource();
        }
        return getDataObject().getAnimation(state);
    }

    private boolean isWorkAnimation() {
        CreatureAi creatureAi = getEntity().get(CreatureAi.class);
        if (creatureAi != null) {
            return creatureAi.getCreatureState() == CreatureState.WORK;
        }
        return false;
    }

}
