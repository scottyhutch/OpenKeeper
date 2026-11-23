/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.AnimationFixtures;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DK II plays the walk and run by the distance walked (0x48BE50, 0x4B41E0)
 */
class AnimationTimingTest {

    /** The Mistress's Mis_Walk: 48 frames at 20 a second, start distance 573 */
    private static final ArtResource MISTRESS_WALK = AnimationFixtures.walk("Mis_Walk", 48, 20, 573);
    /** The Imp's Imp_Run: 12 frames at 20 a second, start distance 1556 */
    private static final ArtResource IMP_RUN = AnimationFixtures.walk("Imp_Run", 12, 20, 1556);

    @Test
    void theWalkPlaysAsFastAsTheCreatureWalks() {

        // The Mistress walks 0.7 tiles a second, her walk plays five times as fast as its frame rate, two cycles a second
        double played = 0;
        for (int frame = 0; frame < 60; frame++) {
            played += AnimationTiming.walkedTime(MISTRESS_WALK, 0.7 / 60, 0, 1 / 60.0, 4);
        }
        assertEquals(0.7 * 4096 / 573, played, 1e-9);
        assertEquals(2.08, played / AnimationTiming.duration(MISTRESS_WALK, 1), 0.01);

        // At half the speed, half as fast
        assertEquals(played / 2, 60 * AnimationTiming.walkedTime(MISTRESS_WALK, 0, -0.35 / 60, 1 / 60.0, 4), 1e-9);
    }

    @Test
    void aStepIsMeasuredLikeDkIiDoes() {

        // The longer side and half the shorter one
        assertEquals(0.15 * 4096 / 573, AnimationTiming.walkedTime(MISTRESS_WALK, 0.1, -0.1, 1, 4), 1e-9);
    }

    @Test
    void lessThanACycleATick() {

        // The Imp runs 2.15 tiles a second, more than four cycles of 0.6 seconds a second at four ticks a second
        double cycle = AnimationTiming.duration(IMP_RUN, 1);
        assertEquals(0.6, cycle, 1e-9);
        assertEquals(4 * cycle / 60, AnimationTiming.walkedTime(IMP_RUN, 2.15 / 60, 0, 1 / 60.0, 4), 1e-9);
        assertEquals(0.5 * 4096 / 1556 / 60, AnimationTiming.walkedTime(IMP_RUN, 0.5 / 60, 0, 1 / 60.0, 4), 1e-9);
    }

    @Test
    void withoutAStartDistanceTheAnimationStandsStill() {
        assertEquals(0, AnimationTiming.walkedTime(AnimationFixtures.resource("Maiden-Run", 17, 30), 1, 1, 1, 4));
    }

    @Test
    void onlyTheWalkAndTheRunAreWalked() {
        assertTrue(AnimationTiming.isWalked(MISTRESS_WALK, Creature.AnimationType.WALK));
        assertTrue(AnimationTiming.isWalked(IMP_RUN, Creature.AnimationType.RUN));
        assertFalse(AnimationTiming.isWalked(MISTRESS_WALK, Creature.AnimationType.DRUNKED_WALK));
        assertFalse(AnimationTiming.isWalked(MISTRESS_WALK, Creature.AnimationType.STEALTH_WALK));
    }
}
