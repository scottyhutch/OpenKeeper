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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.component.ManufactureProgress;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.WorkshopOrders;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.data.ResearchableType;

/**
 * Tells the HUD what each keeper's workshops are manufacturing. Each keeper
 * has an entity of its own that has a {@link ManufactureProgress} while there
 * is something to show.<br>
 * As in the original: the item is the head of the manufacture list, and the
 * percentage is the manufacture points gathered times 100 divided by the
 * points the item needs, rounded down. Nothing is shown at 0 percent.
 */
public final class ManufactureProgressSystem implements IGameLogicUpdatable {

    /**
     * The item to manufacture next
     *
     * @param type trap or door
     * @param typeId the trap or door id
     * @param manufToBuild the manufacture points it needs
     */
    public record Order(ResearchableType type, short typeId, int manufToBuild) {

    }

    /**
     * Where the next item to manufacture comes from
     */
    @FunctionalInterface
    public interface OrderLookup {

        /**
         * The item the keeper's workshops manufacture next
         *
         * @param playerId the keeper
         * @return the item, {@code null} if there is nothing to manufacture
         */
        Order getNext(short playerId);

    }

    private final EntityData entityData;
    private final Map<Short, PlayerManufactureControl> manufactureControls = new HashMap<>();
    private final OrderLookup orders;
    private final Map<Short, EntityId> progressEntities = new HashMap<>();

    public ManufactureProgressSystem(EntityData entityData, Collection<IPlayerController> playerControllers, OrderLookup orders) {
        this.entityData = entityData;
        this.orders = orders;
        for (IPlayerController playerController : playerControllers) {
            PlayerManufactureControl manufactureControl = playerController.getManufactureControl();
            if (manufactureControl != null) {
                manufactureControls.put(playerController.getKeeper().getId(), manufactureControl);
            }
        }
    }

    /**
     * The orders come from the keeper's one manufacture list of trap and door
     * blueprints
     *
     * @param workshopOrders the manufacture list
     * @return the orders
     */
    public static OrderLookup workshopOrders(WorkshopOrders workshopOrders) {
        return playerId -> {
            WorkshopOrders.Order order = workshopOrders.getNext(playerId);
            return order != null ? new Order(order.type(), order.typeId(), order.manufToBuild()) : null;
        };
    }

    @Override
    public void start() {
        for (Short playerId : manufactureControls.keySet()) {
            EntityId entity = entityData.createEntity();
            entityData.setComponent(entity, new Owner(playerId, playerId));
            progressEntities.put(playerId, entity);
        }
    }

    @Override
    public void stop() {
        for (EntityId entity : progressEntities.values()) {
            entityData.removeEntity(entity);
        }
        progressEntities.clear();
    }

    @Override
    public void processTick(float tpf) {
        for (Map.Entry<Short, EntityId> entry : progressEntities.entrySet()) {
            ManufactureProgress progress = getProgress(entry.getKey());
            ManufactureProgress shown = entityData.getComponent(entry.getValue(), ManufactureProgress.class);
            if (progress == null) {
                if (shown != null) {
                    entityData.removeComponent(entry.getValue(), ManufactureProgress.class);
                }
            } else if (!isSame(progress, shown)) {
                entityData.setComponent(entry.getValue(), progress);
            }
        }
    }

    private ManufactureProgress getProgress(short playerId) {
        int points = manufactureControls.get(playerId).getProgress();

        // Nothing to show without points, no need to look for the item
        return points > 0 ? getProgress(orders.getNext(playerId), points) : null;
    }

    /**
     * What to show for the item in production
     *
     * @param order the item, {@code null} if there is none
     * @param points the manufacture points gathered
     * @return the progress, {@code null} if there is nothing to show
     */
    static ManufactureProgress getProgress(Order order, int points) {
        if (order == null) {
            return null;
        }
        int percent = getPercentCompleted(points, order.manufToBuild());
        return percent > 0 ? new ManufactureProgress(order.type(), order.typeId(), percent) : null;
    }

    /**
     * How far the item is, as the original computes it
     * (MyManufactureList_getPercentCompleted, 0x506B70)
     *
     * @param points the manufacture points gathered
     * @param required the points the item needs
     * @return percent completed, rounded down, at most 100
     */
    static int getPercentCompleted(int points, int required) {
        if (points <= 0 || required <= 0) {
            return 0;
        }
        return Math.min(100, points * 100 / required);
    }

    private static boolean isSame(ManufactureProgress progress, ManufactureProgress shown) {
        return shown != null && shown.type == progress.type && shown.typeId == progress.typeId
                && shown.percent == progress.percent;
    }

}
