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
package toniarts.openkeeper.tools.convert.map;

/**
 * Builds {@link TriggerGeneric} and {@link TriggerAction} fixtures for the GUI
 * button tests. Lives in the trigger package because the setters are
 * {@code protected}.
 */
public final class GuiButtonTriggerFixtures {

    private GuiButtonTriggerFixtures() {
    }

    /**
     * A "GUI button pressed" condition, like the ones in the tutorial levels.
     *
     * @param id the trigger id
     * @param child the id of the first child
     * @param buttonType the button type, see {@link TriggerAction.MakeType}
     * @param buttonId the button id inside the type
     * @return the condition
     */
    public static TriggerGeneric guiButtonPressed(int id, int child, short buttonType, short buttonId) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        trigger.setId(id);
        trigger.setIdChild(child);
        trigger.setRepeatTimes((short) 1);
        trigger.setType(TriggerGeneric.TargetType.GUI_BUTTON_PRESSED);
        trigger.setUserData("targetType", buttonType);
        trigger.setUserData("targetId", buttonId);
        trigger.setUserData("value", 0);
        return trigger;
    }

    /**
     * An action that sets a flag to a constant value
     *
     * @param id the trigger id
     * @param flagId the flag
     * @param value the value
     * @return the action
     */
    public static TriggerAction setFlag(int id, int flagId, int value) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setType(TriggerAction.ActionType.FLAG);
        action.setUserData("flagId", (short) flagId);
        action.setUserData("flag", (short) (0x02 | 0x08)); // Constant value, "="
        action.setUserData("value", value);
        return action;
    }
}
