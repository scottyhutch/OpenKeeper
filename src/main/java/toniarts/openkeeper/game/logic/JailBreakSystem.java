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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.JailBreaker;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The jail break. A hero with the party flag FREE_FRIENDS_ON_JAIL_BREAK that
 * stands in the Prison of an enemy frees the first imprisoned creature that is
 * a friend of the hero (DKII.EXE 0x4E8660, called for a creature standing in a
 * room from its tick at 0x488930). Workers do not, nor do creatures that are
 * imprisoned or tortured themselves.
 * <p>
 * In DKII the hero goes through a jail break state to do this. Here the
 * prisoner is simply released, one at a time per hero, with a delay.
 */
public final class JailBreakSystem implements IGameLogicUpdatable {

    /**
     * Seconds between two prisoners freed by the same hero. Not from DKII.
     */
    private static final double FREE_INTERVAL = 1.0;

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final ILevelInfo levelInfo;
    private final IMapController mapController;
    private final EntitySet heroes;

    public JailBreakSystem(EntityData entityData, IGameTimer gameTimer, ILevelInfo levelInfo, IMapController mapController) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.levelInfo = levelInfo;
        this.mapController = mapController;
        heroes = entityData.getEntities(JailBreaker.class, Position.class, Owner.class, CreatureComponent.class,
                Health.class);
    }

    @Override
    public void processTick(float tpf) {
        double gameTime = gameTimer.getGameTime();
        heroes.applyChanges();
        for (Entity hero : heroes) {
            if (canBreakOut(hero) && hero.get(JailBreaker.class).nextFreeTime <= gameTime
                    && freeFriend(hero)) {
                hero.set(new JailBreaker(gameTime + FREE_INTERVAL));
            }
        }
    }

    private boolean canBreakOut(Entity hero) {
        return !hero.get(CreatureComponent.class).worker
                && entityData.getComponent(hero.getId(), CreatureImprisoned.class) == null
                && entityData.getComponent(hero.getId(), CreatureTortured.class) == null;
    }

    private boolean freeFriend(Entity hero) {
        IRoomController room = mapController.getRoomControllerByCoordinates(
                WorldUtils.vectorToPoint(hero.get(Position.class).position));
        if (room == null || !room.hasObjectControl(ObjectType.PRISONER)) {
            return false;
        }
        Keeper keeper = levelInfo.getPlayer(hero.get(Owner.class).ownerId);
        IRoomObjectControl<?> control = room.getObjectControl(ObjectType.PRISONER);
        if (keeper == null || !keeper.isEnemy(room.getOwnerId()) || !(control instanceof RoomPrisonerControl prison)) {
            return false;
        }
        return prison.releaseFirst(prisoner -> isFriend(keeper, prisoner)) != null;
    }

    private boolean isFriend(Keeper keeper, EntityId prisoner) {
        Owner owner = entityData.getComponent(prisoner, Owner.class);
        return owner != null && keeper.isAlly(owner.ownerId);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        heroes.release();
    }
}
