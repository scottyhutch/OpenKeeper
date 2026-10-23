/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Shot;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_CREATURE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_OBJECT;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.LIGHTNING;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.MODIFY_HEALTH;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.POSSESS_CREATURE;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ShotsController implements IShotsController {

    private static final Logger logger = System.getLogger(ShotsController.class.getName());

    private final IKwdFile kwdFile;
    private final EntityData entityData;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final IGameTimer gameTimer;
    private final IGameController gameController;
    private final IMapController mapController;
    private final ILevelInfo levelInfo;
    private final IObjectsController objectsController;
    private final ICreaturesController creaturesController;

    public ShotsController(IKwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, IGameTimer gameTimer,
            IGameController gameController, IMapController mapController, ILevelInfo levelInfo, IObjectsController objectsController, ICreaturesController creaturesController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.gameController = gameController;
        this.mapController = mapController;
        this.levelInfo = levelInfo;
        this.objectsController = objectsController;
        this.creaturesController = creaturesController;
    }

    @Override
    public void createShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target) {
        Shot shot = kwdFile.getShotById(shotTypeId);
        switch (shot.getProcessType()) {
            case CREATE_CREATURE -> {
                creaturesController.spawnCreature((short) shotData1, playerId, shotData2, WorldUtils.vector3fToVector2f(position), ICreaturesController.SpawnType.CONJURE);
            }
            case CREATE_OBJECT -> {
                objectsController.loadObject((short) shotData1, playerId, position, 0);
            }
            case POSSESS_CREATURE -> {
                creaturesController.createController(target).setPossession(true);
            }
            case MODIFY_HEALTH -> {
                EntityController.setDamage(entityData, target, -shotData1);
            }
            case LIGHTNING -> {

                // Only the keeper's Thunderbolt for now, a creature's lightning still deals its damage in the attack
                if (isKeeperSpellShot(shotTypeId)) {
                    strikeWithLightning(target, shotData1);
                }
            }
            default ->
                logger.log(Logger.Level.WARNING, "Shot type {0} not implemented", shot.getProcessType());
        }
    }

    private boolean isKeeperSpellShot(short shotTypeId) {
        for (KeeperSpell keeperSpell : kwdFile.getKeeperSpells()) {
            if (keeperSpell.getShotTypeId() == shotTypeId || keeperSpell.getBonusShotTypeId() == shotTypeId) {
                return true;
            }
        }

        return false;
    }

    /**
     * Strikes the target with lightning. Shot data 1 is the damage, twice that
     * when the target stands in water. A creature is also knocked off its
     * feet, unless it is immune to lightning, then nothing happens at all
     *
     * @param target the struck entity, can be null
     * @param damage the damage
     */
    private void strikeWithLightning(EntityId target, int damage) {
        Position position = target != null ? entityData.getComponent(target, Position.class) : null;
        if (position == null || entityData.getComponent(target, Health.class) == null) {
            return;
        }

        CreatureComponent creatureComponent = entityData.getComponent(target, CreatureComponent.class);
        Creature creature = creatureComponent != null ? kwdFile.getCreature(creatureComponent.creatureId) : null;
        if (creature != null && creature.getFlags3() != null
                && creature.getFlags3().contains(Creature.CreatureFlag3.IS_IMMUNE_TO_LIGHTNING)) {
            return;
        }

        boolean inWater = mapController.isWater(WorldUtils.vectorToPoint(position.position));
        EntityController.setDamage(entityData, target, inWater ? damage * 2 : damage);

        if (creature != null && creaturesController.isValidEntity(target)) {
            creaturesController.createController(target).knockDown();
        }
    }

}
