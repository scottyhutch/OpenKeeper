/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;

/** Shared metadata interpretation. Simulation never needs to load a mesh. */
public final class AnimationTiming {
    public static final double SOURCE_FPS = 30;
    private AnimationTiming() { }

    public static double framesPerSecond(ArtResource resource) {
        Number fps = resource == null ? null : resource.getData(ArtResource.KEY_FPS);
        return fps != null && Double.isFinite(fps.doubleValue()) && fps.doubleValue() > 0
                ? fps.doubleValue() : SOURCE_FPS;
    }

    /**
     * The authored time of a walk or run animation that a step of the
     * creature plays. DK II (0x48BE50, 0x4B41E0) plays these two by the
     * distance walked: the start distance of the animation is the speed, in
     * 1/4096 tiles a second, its frames play at their own rate, faster or
     * slower in proportion to the speed of the creature. It measures a step
     * as the longer of its sides plus half the shorter one, and plays less
     * than a cycle a tick. Without a start distance the animation stands
     * still.
     *
     * @param resource the walk or run animation
     * @param dx the step along x, in tiles
     * @param dz the step along z, in tiles
     * @param seconds how long the step took
     * @param ticksPerSecond the logic ticks a second of the level
     * @return the authored seconds to play
     */
    public static double walkedTime(ArtResource resource, double dx, double dz, double seconds, int ticksPerSecond) {
        Number startDistance = resource == null ? null : resource.getData(ArtResource.KEY_START_DIST);
        if (startDistance == null || startDistance.doubleValue() <= 0) return 0;
        double longer = Math.max(Math.abs(dx), Math.abs(dz)), shorter = Math.min(Math.abs(dx), Math.abs(dz));
        double time = (longer + shorter / 2) * 4096 / startDistance.doubleValue();
        return Math.min(time, Math.max(0, seconds) * ticksPerSecond * duration(resource, 1));
    }

    /** Whether DK II plays the animation by the distance walked, its walk and run */
    public static boolean isWalked(ArtResource resource, Creature.AnimationType animation) {
        return resource != null && resource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH
                && (animation == Creature.AnimationType.WALK || animation == Creature.AnimationType.RUN);
    }

    public static double duration(ArtResource resource, double fallback) {
        if (resource == null || resource.getType() == ArtResource.ArtResourceType.NONE) return fallback;
        Number frames = resource.getData(ArtResource.KEY_FRAMES);
        return frames != null && Double.isFinite(frames.doubleValue()) && frames.doubleValue() > 0
                ? frames.doubleValue() / framesPerSecond(resource) : fallback;
    }
}
