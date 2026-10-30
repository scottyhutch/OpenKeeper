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
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.PortalGemReaper;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * A creature carrying the Portal Gem drops it when it dies, so that it lies
 * where it fell for the Horned Reaper to take
 */
public final class PortalGemDropSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IObjectsController objectsController;
    private final Short gemObjectId;
    private final EntitySet deadCarriers;

    /**
     * @param entityData the entities
     * @param kwdFile the level
     * @param objectsController the objects
     */
    public PortalGemDropSystem(EntityData entityData, IKwdFile kwdFile, IObjectsController objectsController) {
        this.entityData = entityData;
        this.objectsController = objectsController;
        gemObjectId = PortalGemReaper.getPortalGemObjectId(kwdFile);

        deadCarriers = entityData.getEntities(PortalGem.class, Death.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        if (!deadCarriers.applyChanges() || gemObjectId == null) {
            return;
        }

        List<Entity> carriers = new ArrayList<>(deadCarriers.getAddedEntities());
        for (Entity carrier : carriers) {
            PortalGemReaper.dropPortalGem(entityData, objectsController, gemObjectId, carrier.getId());
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        deadCarriers.release();
    }

}
