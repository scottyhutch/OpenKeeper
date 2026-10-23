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
package toniarts.openkeeper.game.sound;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.state.MainMenuScreenController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The level-won cutscenes name their speech by the file number, which is not
 * the id of the speech in the mentor sound bank
 */
class MentorTypeTest {

    @Test
    void miscSpeechByFileNumber() {
        assertEquals(MentorType.MISC149, MentorType.forMisc(149));

        // Not the bank id 149, which is a room speech (rooma088)
        assertEquals(1418, MentorType.forMisc(149).getId());
        assertEquals(149, MentorType.ROOMA088.getId());
    }

    @Test
    void noSuchMiscSpeech() {
        assertNull(MentorType.forMisc(0));
    }

    @Test
    void everyCampaignCutsceneHasItsSpeech() {
        for (int level = 1; level <= 19; level++) {
            MainMenuScreenController.Cutscene cutscene = MainMenuScreenController.getCutscene(level);
            assertNotNull(cutscene, "Cutscene of level " + level);
            assertNotNull(MentorType.forMisc(Integer.parseInt(cutscene.getSpeechId())), "Speech of level " + level);
        }
    }
}
