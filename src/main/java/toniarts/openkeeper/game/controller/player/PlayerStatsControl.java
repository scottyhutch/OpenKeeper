/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.TriggerAction;

/**
 * A class to hold player's miscellaneous statistics
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerStatsControl {

    private int payDays;
    private final Map<Creature, Integer> slapsMap = new HashMap<>();
    private final Map<Creature, Integer> pickUpsMap = new HashMap<>();
    private final Map<Creature, Integer> dropsMap = new HashMap<>();
    private final Map<PressedButton, Integer> buttonPressesMap = new ConcurrentHashMap<>();
    private int enemyCreaturesKilled = 0;
    private int heroesDestroyed = 0;
    private int specialsFound = 0;
    private int itemsManufactured = 0;
    private int creaturesConverted = 0;

    public boolean hasSlapped() {
        return !slapsMap.isEmpty();
    }

    public boolean hasSlapped(Creature creature) {
        return slapsMap.containsKey(creature);
    }

    /**
     * Counts a pay day of the player
     */
    public void payDay() {
        payDays++;
    }

    /**
     * Get how many pay days the player has had
     *
     * @return the pay days
     */
    public int getPayDays() {
        return payDays;
    }

    public void creatureSlapped(Creature creature) {
        increaseMapCount(creature, slapsMap);
    }

    private void increaseMapCount(Creature creature, Map<Creature, Integer> map) {
        Integer count = map.get(creature);
        if (count == null) {
            count = 0;
        }
        count++;
        map.put(creature, count);
    }

    public boolean hasPickedUp() {
        return !pickUpsMap.isEmpty();
    }

    public boolean hasPickedUp(Creature creature) {
        return pickUpsMap.containsKey(creature);
    }

    public void creaturePickedUp(Creature creature) {
        increaseMapCount(creature, pickUpsMap);
    }

    public boolean hasDropped() {
        return !dropsMap.isEmpty();
    }

    public boolean hasDropped(Creature creature) {
        return dropsMap.containsKey(creature);
    }

    public void creatureDropped(Creature creature) {
        increaseMapCount(creature, dropsMap);
    }

    /**
     * An enemy creature has been killed or knocked out. Heroes are counted separately.
     *
     * @param creature the creature that was killed
     */
    public void creatureKilled(Creature creature) {
        if (creature.getFlags().contains(Creature.CreatureFlag.IS_GOOD)) {
            heroesDestroyed++;
        } else {
            enemyCreaturesKilled++;
        }
    }

    /**
     * Get the number of enemy creatures, not heroes, killed
     *
     * @return enemy creatures killed
     */
    public int getEnemyCreaturesKilled() {
        return enemyCreaturesKilled;
    }

    /**
     * Get the number of heroes killed
     *
     * @return heroes destroyed
     */
    public int getHeroesDestroyed() {
        return heroesDestroyed;
    }

    /**
     * The player has activated a special
     */
    public void specialFound() {
        specialsFound++;
    }

    /**
     * Get the number of specials the player has activated
     *
     * @return specials found
     */
    public int getSpecialsFound() {
        return specialsFound;
    }

    /**
     * A workshop has finished a trap or a door for the player
     */
    public void itemManufactured() {
        itemsManufactured++;
    }

    /**
     * Get the number of traps and doors the player's workshops have finished
     *
     * @return items manufactured
     */
    public int getItemsManufactured() {
        return itemsManufactured;
    }

    /**
     * A creature has been converted to the player's side in the torture chamber
     */
    public void creatureConverted() {
        creaturesConverted++;
    }

    /**
     * Get the number of creatures the player has converted by torturing
     *
     * @return creatures converted
     */
    public int getCreaturesConverted() {
        return creaturesConverted;
    }

    /**
     * Get how many times each creature type has been slapped
     *
     * @return read only view
     */
    public Map<Creature, Integer> getSlaps() {
        return Collections.unmodifiableMap(slapsMap);
    }

    /**
     * Get how many times each creature type has been picked up
     *
     * @return read only view
     */
    public Map<Creature, Integer> getPickUps() {
        return Collections.unmodifiableMap(pickUpsMap);
    }

    /**
     * Get how many times each creature type has been dropped
     *
     * @return read only view
     */
    public Map<Creature, Integer> getDrops() {
        return Collections.unmodifiableMap(dropsMap);
    }

    /**
     * Mark a HUD button as pressed
     *
     * @param type the button type
     * @param buttonId the button id inside the type
     */
    public void buttonPressed(TriggerAction.MakeType type, short buttonId) {
        buttonPressesMap.merge(new PressedButton(type, buttonId), 1, Integer::sum);
    }

    /**
     * Mark a HUD button as pressed
     *
     * @param buttonType the button type, a {@link TriggerAction.MakeType} value
     * @param buttonId the button id inside the type
     */
    public void buttonPressed(short buttonType, short buttonId) {
        buttonPressed(ConversionUtils.parseEnum(buttonType, TriggerAction.MakeType.class), buttonId);
    }

    /**
     * Get how many times each HUD button has been pressed
     *
     * @return read only view
     */
    public Map<PressedButton, Integer> getButtonPresses() {
        return Collections.unmodifiableMap(buttonPressesMap);
    }

    /**
     * A HUD button that level scripts can wait for
     *
     * @param type the button type
     * @param buttonId the button, a room, spell, door, trap or creature id, or
     * a {@link TriggerAction.ButtonType} value for the misc buttons
     */
    public record PressedButton(TriggerAction.MakeType type, short buttonId) {

    }

}
