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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import toniarts.openkeeper.game.component.CasinoBigWinner;
import toniarts.openkeeper.game.component.CasinoDisco;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.MentorType;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.SoundVoice;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.PlayerMapViewState;
import toniarts.openkeeper.view.effect.EffectManagerState;

/**
 * The sights and sounds of the Casino's jackpot party, as in DK II (the casino
 * tick at 0x4D6270 and the jackpot at 0x4D6AA0):
 * <ul>
 * <li>the big win is announced to its keeper and the Casino Jackpot effect
 * goes off at the winner</li>
 * <li>a Casino in disco mode plays Disco Inferno from its centre, the louder
 * the closer the camera is, and turns the level music down as much</li>
 * <li>every 12 seconds there are fireworks over a random tile of the
 * Casino</li>
 * </ul>
 * This only looks at the components the server sets on the big winner and the
 * Casino.
 */
public final class CasinoDiscoState extends AbstractPauseAwareState {

    private static final int DISCO_MUSIC = 0x316;
    private static final int DISCO_OVER = 0x33E;
    private static final int FIREWORKS_SOUND = 0x344;
    private static final int FIREWORKS_EFFECT = 0x140;
    private static final int JACKPOT_EFFECT = 0x5B;
    private static final float FIREWORKS_INTERVAL_SECONDS = 12;

    /**
     * The music fades out over this many tiles from the Casino's centre
     */
    private static final int MUSIC_DISTANCE_TILES = 45;
    private static final int MAX_VOLUME = 100;

    private final EntityData entityData;
    private final Keeper player;
    private final IKwdFile kwdFile;
    private final PlayerMapViewState mapViewState;
    private final Map<EntityId, Integer> musicHandles = new HashMap<>();
    private final Map<EntityId, Float> fireworksTimers = new HashMap<>();
    private final Random random = new Random();
    private EntitySet discos;
    private EntitySet bigWinners;
    private Node rootNode;
    private AppStateManager stateManager;

    public CasinoDiscoState(EntityData entityData, Keeper player, IKwdFile kwdFile, PlayerMapViewState mapViewState) {
        this.entityData = entityData;
        this.player = player;
        this.kwdFile = kwdFile;
        this.mapViewState = mapViewState;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.stateManager = stateManager;
        rootNode = ((SimpleApplication) app).getRootNode();
        discos = entityData.getEntities(CasinoDisco.class, RoomComponent.class, Owner.class);
        bigWinners = entityData.getEntities(CasinoBigWinner.class, Position.class, Owner.class);
    }

    @Override
    public void update(float tpf) {
        if (bigWinners.applyChanges()) {
            for (Entity bigWinner : bigWinners.getAddedEntities()) {
                announceJackpot(bigWinner);
            }
        }

        if (discos.applyChanges()) {
            for (Entity disco : discos.getRemovedEntities()) {
                stopDisco(disco);
            }
        }

        // The loudest disco turns the music down the most
        int loudest = 0;
        for (Entity disco : discos) {
            Point centre = disco.get(RoomComponent.class).location;
            loudest = Math.max(loudest, playMusic(disco.getId(), getSoundCategory(disco), centre));

            float timer = fireworksTimers.getOrDefault(disco.getId(), 0f) + tpf;
            if (timer >= FIREWORKS_INTERVAL_SECONDS) {
                timer -= FIREWORKS_INTERVAL_SECONDS;
                fireworks(disco);
            }
            fireworksTimers.put(disco.getId(), timer);
        }
        SoundState soundState = stateManager.getState(SoundState.class);
        if (soundState != null) {
            soundState.setMusicVolumeFactor(1f - loudest / (float) MAX_VOLUME);
        }
    }

    private void announceJackpot(Entity bigWinner) {
        short ownerId = bigWinner.get(Owner.class).ownerId;
        if (ownerId == player.getId()) {
            SoundState soundState = stateManager.getState(SoundState.class);
            if (soundState != null) {
                soundState.attachMentorSpeech(MentorType.CASIN001, null);
            }
        }
        loadEffect(bigWinner.get(Position.class).position, JACKPOT_EFFECT, ownerId);
    }

    /**
     * Plays the disco music, louder the closer the camera is to the Casino
     *
     * @return the volume of the music
     */
    private int playMusic(EntityId disco, String category, Point centre) {
        SoundEngine engine = SoundManager.getEngine();
        if (engine == null || category == null) {
            return 0;
        }
        Integer handle = musicHandles.get(disco);
        if (handle == null) {
            handle = SoundManager.playSound(category, DISCO_MUSIC);
            if (handle == SoundEngine.NO_HANDLE) {
                return 0;
            }
            musicHandles.put(disco, handle);
        }

        // What is hidden by the fog is not heard
        int volume = 0;
        if (mapViewState.getFogOfWarInformation().isExplored(centre)) {
            Point listener = engine.getListener();
            int distance = Math.min(MUSIC_DISTANCE_TILES, Math.max(Math.abs(listener.x - centre.x), Math.abs(listener.y - centre.y)));
            volume = MAX_VOLUME - MAX_VOLUME * distance / MUSIC_DISTANCE_TILES;
        }
        SoundVoice voice = engine.getVoice(handle);
        if (voice == null) {
            return 0;
        }
        voice.setVolume(volume);

        return volume;
    }

    private void stopDisco(Entity disco) {
        fireworksTimers.remove(disco.getId());
        Integer handle = musicHandles.remove(disco.getId());
        if (handle != null) {
            SoundManager.stopSound(handle);

            // The needle comes off the record
            String category = getSoundCategory(disco);
            if (category != null) {
                SoundManager.playSound(category, DISCO_OVER, disco.get(RoomComponent.class).location);
            }
        }
    }

    /**
     * Fireworks go off a tile above a random tile of the Casino
     */
    private void fireworks(Entity disco) {
        IMapDataInformation<? extends IMapTileInformation> mapData = mapViewState.getMapInformation().getMapData();
        List<Point> tiles = new ArrayList<>();
        for (int x = 0; x < mapData.getWidth(); x++) {
            for (int y = 0; y < mapData.getHeight(); y++) {
                IMapTileInformation tile = mapData.getTile(x, y);
                if (tile != null && disco.getId().equals(tile.getRoomId())) {
                    tiles.add(tile.getLocation());
                }
            }
        }
        if (tiles.isEmpty()) {
            return;
        }

        Point tile = tiles.get(random.nextInt(tiles.size()));
        loadEffect(WorldUtils.pointToVector3f(tile).addLocal(0, WorldUtils.TILE_HEIGHT, 0), FIREWORKS_EFFECT,
                disco.get(Owner.class).ownerId);
        SoundManager.playSound(GlobalCategory.EFFECTS, FIREWORKS_SOUND, tile);
    }

    private void loadEffect(Vector3f location, int effectId, short ownerId) {
        EffectManagerState effectManager = stateManager.getState(EffectManagerState.class);
        if (effectManager != null) {
            effectManager.load(rootNode, location.clone(), effectId, false, ownerId);
        }
    }

    private String getSoundCategory(Entity disco) {
        Room room = kwdFile.getRoomById(disco.get(RoomComponent.class).roomId);
        return room != null ? room.getSoundCategory() : null;
    }

    @Override
    public void cleanup() {
        for (Integer handle : musicHandles.values()) {
            SoundManager.stopSound(handle);
        }
        musicHandles.clear();
        fireworksTimers.clear();
        SoundState soundState = stateManager.getState(SoundState.class);
        if (soundState != null) {
            soundState.setMusicVolumeFactor(1f);
        }
        discos.release();
        bigWinners.release();

        super.cleanup();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

}
