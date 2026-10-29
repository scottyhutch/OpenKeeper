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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector3f;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.navigation.INavigationService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Imps only fetch loose gold the player can still store
 */
class FetchObjectTaskTest {

    @Test
    void goldIsNotFetchedWhenPlayerGoldIsAtMaximum() {

        // A treasury still reports free room, but the player's gold is at its maximum
        assertFalse(task(ObjectType.GOLD, 1000, 1000, false).isValid(null));
    }

    @Test
    void goldIsFetchedWhenThereIsRoomForIt() {
        assertTrue(task(ObjectType.GOLD, 500, 1000, false).isValid(null));
    }

    @Test
    void goldIsNotFetchedWhenRoomsAreFull() {
        assertFalse(task(ObjectType.GOLD, 500, 1000, true).isValid(null));
    }

    private static FetchObjectTask task(ObjectType type, int gold, int goldMax, boolean roomsFull) {
        Keeper keeper = new Keeper(false, (short) 1);
        keeper.setGold(gold);
        PlayerGoldControl goldControl = new PlayerGoldControl(keeper);
        goldControl.setGoldMax(goldMax);

        IPlayerController player = stub(IPlayerController.class, "getGoldControl", goldControl);
        IRoomController room = stub(IRoomController.class, "isFullCapacity", roomsFull);
        IMapController map = stub(IMapController.class, "getRoomsByFunction", List.of(room));
        IObjectController object = (IObjectController) Proxy.newProxyInstance(IObjectController.class.getClassLoader(),
                new Class<?>[]{IObjectController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getPosition" ->
                new Vector3f(3, 0, 4);
            case "getType" ->
                type;
            case "isPickableByPlayerCreature" ->
                true;
            case "isRemoved", "isStoredInRoom", "isDragged" ->
                false;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        INavigationService navigation = stub(INavigationService.class, "", null);
        return new FetchObjectTask(null, navigation, map, object, (short) 1, player);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, String method, Object result) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, m, args) -> {
                    if (m.getName().equals(method)) {
                        return result;
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
    }

}
