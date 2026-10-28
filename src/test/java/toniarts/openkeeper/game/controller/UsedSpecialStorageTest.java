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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A special used while stored in a room has to free its storage slot
 */
class UsedSpecialStorageTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> removed = new ArrayList<>();

    @Test
    void usedStoredSpecialIsRemovedFromItsRoom() {
        EntityId room = entityData.createEntity();
        EntityId special = entityData.createEntity();
        entityData.setComponent(special, new Stored(room, ObjectType.SPECIAL));

        GameWorldController.removeFromRoomStorage(entityData, mapController(room), special);

        assertEquals(List.of(special), removed);
    }

    @Test
    void looseSpecialIsLeftAlone() {
        EntityId room = entityData.createEntity();
        EntityId special = entityData.createEntity();

        GameWorldController.removeFromRoomStorage(entityData, mapController(room), special);

        assertTrue(removed.isEmpty());
    }

    private IMapController mapController(EntityId room) {
        IRoomObjectControl objectControl = (IRoomObjectControl) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> {
                    if (method.getName().equals("removeItem")) {
                        removed.add((EntityId) args[0]);
                    }
                    return null;
                });
        IRoomController roomController = (IRoomController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) ->
                        method.getName().equals("getObjectControl") && args[0] == ObjectType.SPECIAL ? objectControl : null);
        return (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) ->
                        method.getName().equals("getRoomController") && args[0].equals(room) ? roomController : null);
    }

}
