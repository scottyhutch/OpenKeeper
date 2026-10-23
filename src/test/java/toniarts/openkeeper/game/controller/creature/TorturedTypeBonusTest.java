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
package toniarts.openkeeper.game.controller.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Owner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In DK II a keeper's researchers work harder while the keeper tortures one
 * of his own creatures of the same type
 */
class TorturedTypeBonusTest {

    private static final short KEEPER_ID = 3;
    private static final short OTHER_KEEPER_ID = 4;
    private static final short WARLOCK = 5;
    private static final short TROLL = 10;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void bonusOnceASecond() {

        // 25 % at 4 ticks a second, as DK II counts it
        assertEquals(1, CreatureController.getTorturedTypeBonus(1, 4, 25));
        assertEquals(2, CreatureController.getTorturedTypeBonus(2, 4, 25));
        assertEquals(8, CreatureController.getTorturedTypeBonus(7, 4, 25));
        assertEquals(0, CreatureController.getTorturedTypeBonus(0, 4, 25));
    }

    @Test
    void ownCreatureOfTheTypeTortured() {
        victim(WARLOCK, KEEPER_ID, KEEPER_ID);

        assertTrue(CreatureController.isTypeTortured(entityData, WARLOCK, KEEPER_ID));
    }

    @Test
    void otherTypeTortured() {
        victim(TROLL, KEEPER_ID, KEEPER_ID);

        assertFalse(CreatureController.isTypeTortured(entityData, WARLOCK, KEEPER_ID));
    }

    @Test
    void enemyOfTheTypeTortured() {
        victim(WARLOCK, OTHER_KEEPER_ID, KEEPER_ID);

        assertFalse(CreatureController.isTypeTortured(entityData, WARLOCK, KEEPER_ID));
    }

    @Test
    void tortureByAnotherKeeperDoesNotCount() {
        victim(WARLOCK, KEEPER_ID, OTHER_KEEPER_ID);

        assertFalse(CreatureController.isTypeTortured(entityData, WARLOCK, KEEPER_ID));
    }

    private void victim(short creatureId, short ownerId, short torturerId) {
        EntityId victim = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(victim, creatureComponent);
        entityData.setComponent(victim, new Owner(ownerId, torturerId));
        entityData.setComponent(victim, new CreatureTortured(0, 0, 0));
    }

}
