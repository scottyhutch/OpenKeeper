/*
 * Copyright (C) 2014-2025 OpenKeeper
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Legacy view of a {@link SoundEvent} of a category, flattening the event's
 * primary group and every group reachable through property links into a single
 * list of playable files. For the sequenced backgrounds (music, ambience) the
 * groups are also kept as {@link SoundEntry entries} in their original order,
 * with their links.
 */
public final class SoundGroup {

    private final SoundCategory category;
    private final SoundEvent event;
    private final List<SoundFile> files = new ArrayList<>();
    private final List<SoundEntry> entries = new ArrayList<>();

    public SoundGroup(SoundCategory category, SoundEvent event) {
        this.category = category;
        this.event = event;

        parseFiles();
        parseEntries();
    }

    public int getId() {
        return event.getId();
    }

    public SoundCategory getCategory() {
        return category;
    }

    public SoundEvent getEvent() {
        return event;
    }

    public List<SoundFile> getFiles() {
        return files;
    }

    public void addFile(SoundFile file) {
        files.add(file);
    }

    /**
     * The entries in their original order, each with its sounds and links
     *
     * @return the entries
     */
    public List<SoundEntry> getEntries() {
        return entries;
    }

    /**
     * Tells whether this group is played as a sequence that follows the entry
     * links (music, ambience) instead of picking a random file
     *
     * @return true if any entry links to another one
     */
    public boolean isLinked() {
        for (SoundEntry soundEntry : entries) {
            if (!soundEntry.getLinks().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The group that is played when no property is supplied.
     */
    @Nullable
    public SoundEventGroup getPrimaryGroup() {
        return event.getGroup(1);
    }

    private void parseFiles() {
        for (SoundEventGroup eventGroup : collectGroups()) {
            for (SoundSample sample : eventGroup.getSamples()) {
                String filename = sample.getArchive() == null ? null : sample.getArchive().getClipAssetPathNative(
                        sample.getClip());
                if (filename == null) {
                    continue;
                }
                files.add(new SoundFile(this, sample.getClip(), filename, eventGroup, sample));
            }
        }
    }

    private void parseEntries() {
        for (SoundEventGroup eventGroup : event.getGroups()) {
            SoundEntry soundEntry = new SoundEntry(eventGroup.getVolumeMin(), eventGroup.getVolumeMax(),
                    eventGroup.getDelayMin(), eventGroup.getDelayMax());
            entries.add(soundEntry);
            for (SoundLink link : eventGroup.getLinks()) {
                soundEntry.getLinks().add(new SoundEntry.Link(link.getGroupIndex() - 1, link.getPropertyMin(),
                        link.getPropertyMax()));
            }
            for (SoundSample sample : eventGroup.getSamples()) {
                String filename = sample.getArchive() == null ? null : sample.getArchive().getClipAssetPathNative(
                        sample.getClip());
                if (filename != null) {
                    soundEntry.getFiles().add(new SoundFile(this, sample.getClip(), filename, eventGroup, sample));
                }
            }
        }
    }

    private List<SoundEventGroup> collectGroups() {
        SoundEventGroup primary = getPrimaryGroup();
        if (primary == null) {
            return Collections.emptyList();
        }
        if (!event.hasPropertyLinks()) {
            return primary.getSamples().isEmpty() ? Collections.emptyList() : Collections.singletonList(primary);
        }
        return primary.resolve();
    }

    @Override
    public String toString() {
        return "SoundGroup{" + "typeId=" + event.getId() + '}';
    }
}
