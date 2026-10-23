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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.PortalGemFixtures;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalGemDropSystemTest {

    private static final short HERO_ID = 1;
    private static final short GEM_ID = 28;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Object[]> loadedObjects = new ArrayList<>();

    @Test
    void deadCarrierDropsThePortalGem() {
        EntityId lord = addCarrier();
        PortalGemDropSystem system = system();

        entityData.setComponent(lord, new Death(10));
        system.processTick(0.25f);

        assertEquals(1, loadedObjects.size());
        assertEquals(GEM_ID, loadedObjects.get(0)[0]);
        assertEquals(HERO_ID, loadedObjects.get(0)[1]);
        assertEquals(WorldUtils.pointToVector3f(4, 5), loadedObjects.get(0)[2]);
        assertNull(entityData.getComponent(lord, PortalGem.class));
    }

    @Test
    void livingCarrierKeepsThePortalGem() {
        EntityId lord = addCarrier();

        system().processTick(0.25f);

        assertTrue(loadedObjects.isEmpty());
        assertNotNull(entityData.getComponent(lord, PortalGem.class));
    }

    private EntityId addCarrier() {
        EntityId lord = entityData.createEntity();
        entityData.setComponent(lord, new PortalGem());
        entityData.setComponent(lord, new Owner(HERO_ID, HERO_ID));
        entityData.setComponent(lord, new Position(0, WorldUtils.pointToVector3f(4, 5)));
        return lord;
    }

    private PortalGemDropSystem system() {
        List<GameObject> objects = List.of(PortalGemFixtures.portalGem(GEM_ID));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("getObjectList")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return objects;
                });
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("loadObject") || !(args[2] instanceof Vector3f)) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    loadedObjects.add(args);
                    return entityData.createEntity();
                });
        return new PortalGemDropSystem(entityData, kwdFile, objectsController);
    }

}
