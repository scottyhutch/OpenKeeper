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
package toniarts.openkeeper.game.controller.player;

import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ResearchOrderFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.ResearchOrder.ResearchOrderType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerResearchControlTest {

    private final IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
            (proxy, method, args) -> method.getName().equals("getResearchOrder")
                    ? List.of(ResearchOrderFixtures.researchOrder(ResearchOrderType.ROOM, 1, 10),
                            ResearchOrderFixtures.researchOrder(ResearchOrderType.ROOM, 2, 10)) : null);
    private final Keeper keeper = new Keeper(false, (short) 1);
    private final ResearchableEntity first = new ResearchableEntity((short) 1, ResearchableType.ROOM);
    private final ResearchableEntity second = new ResearchableEntity((short) 2, ResearchableType.ROOM);
    private final PlayerResearchControl control = new PlayerResearchControl(keeper, kwdFile);

    PlayerResearchControlTest() {
        keeper.getAvailableRooms().add(first);
        keeper.getAvailableRooms().add(second);
        control.initialize();
    }

    @Test
    void finishedResearchIsKeptUntilCompleted() {
        assertNull(control.research(5));
        assertEquals(5, first.getResearch());
        assertSame(first, control.research(5));
        assertEquals(10, first.getResearch());

        // The book could not be stored: still the same item, nothing lost, tried again next time
        assertSame(first, keeper.getCurrentResearch());
        assertSame(first, control.research(5));
        assertEquals(15, first.getResearch());

        // Stored: now the progress is reset and the next item is researched
        control.completeResearch();
        assertEquals(0, first.getResearch());
        assertSame(second, keeper.getCurrentResearch());
    }

}
