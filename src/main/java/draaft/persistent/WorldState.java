package draaft.persistent;

import draaft.draaft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.io.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WorldState extends PersistentState {

    /**
     * Enum defining the different types of Random Number Generators managed by WorldState.
     */
    public static class RngType {
        public static final RngType PEARL = new RngType("pearl");
        public static final RngType BARTER = new RngType("barter");
        public static final RngType TRIDENT = new RngType("trident");
        public static final RngType SKULL = new RngType("skull");
        public static final RngType CAT = new RngType("cat");
        public static final RngType PHANTOM = new RngType("phantom");
        public static final RngType BLAZE = new RngType("blaze");
        public static final RngType SHULKER = new RngType("shulker");
        public static final RngType RABBIT = new RngType("rabbit");
        public static final RngType TEMPLE = new RngType("temple");
        public static final RngType TNT = new RngType("tnt");
        public static final RngType MINED = new RngType("mined");
        public static final RngType JUNK = new RngType("junk");
        public static final RngType EXPLODING_SHELLS = new RngType("exploding_shells");

        private final String keyName; // The base name used for NBT keys

        public RngType(String keyName) {
            this.keyName = keyName;
        }

        public String getKeyName() {
            return keyName;
        }

        public String getNbtKey() {
            return keyName + "_rng";
        }

        public String getCounterNbtKey() {
            return keyName + "_uses";
        }
    }

    // Use EnumMap for potentially better performance and memory usage with enum keys
    private final Map<RngType, RandomState> randomStates = new HashMap<>();

    public WorldState(String key) {
        super(key);
    }

    /**
     * Gets or creates the persistent WorldState for the given server world.
     *
     * @param world The server world.
     * @return The WorldState instance.
     */
    public static WorldState getServerState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(() -> new WorldState("draaft_world_state"), "draaft_world_state");
    }

    @Override
    public void fromTag(CompoundTag tag) {
        for (var key : tag.getKeys()) {
            var type = new RngType(key);

            randomStates.put(type, deserializeFromTag(tag, type));
        }
    }

    private RandomState deserializeFromTag(CompoundTag tag, RngType type) {
        String primaryKey = type.getNbtKey();
        String counterKey = type.getCounterNbtKey();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(tag.getByteArray(primaryKey)); ObjectInputStream ois = new ObjectInputStream(bais)) {
            var randomState = new RandomState((Random) ois.readObject(), type);
            randomState.setUses(tag.getInt(counterKey));

            return randomState;
        } catch (IOException | ClassNotFoundException e) {
            draaft.LOGGER.warn("Unable to deserialize RNG state for key '{}', will create new random.", primaryKey, e);
        }

        return new RandomState(null, type);
    }

    @Override
    public CompoundTag toTag(CompoundTag nbt) {
        for (RandomState randomState : randomStates.values()) {
            serializeToTag(nbt, randomState);
        }
        return nbt;
    }

    private void serializeToTag(CompoundTag nbt, RandomState randomState) {
        if (randomState.getRandom() != null) {
            String primaryKey = randomState.getNbtKey();
            String counterKey = randomState.getCounterNbtKey();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(randomState.getRandom());
                nbt.putByteArray(primaryKey, baos.toByteArray());
                nbt.putInt(counterKey, randomState.getUses());
            } catch (IOException e) {
                draaft.LOGGER.warn("Unable to serialize RNG state for key '{}'", primaryKey, e);
            }
        }
    }

    /**
     * Gets the RandomState instance for the specified type, creating it based on the
     * world seed if it doesn't exist yet in this session.
     *
     * @param type  The RngType enum constant representing the desired RNG.
     * @param world The ServerWorld instance.
     * @return The RandomState instance for the specified type.
     */
    public RandomState getOrCreateRng(RngType type, ServerWorld world) {
        RandomState randomState = randomStates.computeIfAbsent(type, (k) -> new RandomState(null, type));

        if (randomState.getRandom() == null) {
            draaft.LOGGER.info("Initializing '{}' RNG state. Is Client: {}", type.getKeyName(), world.isClient);
            long seed = world.getSeed();
            randomState.setRandom(new Random(seed));
        }

        this.markDirty();
        return randomState;
    }

    public static class RandomState {
        private Random random;
        private final RngType type;
        private int uses;

        public RandomState(Random random, RngType type) {
            this.random = random;
            this.type = type;
            this.uses = 0;
        }

        public Random getRandom() {
            return random;
        }

        public void setRandom(Random random) {
            this.random = random;
        }

        public String getNbtKey() {
            return type.getNbtKey();
        }

        public String getCounterNbtKey() {
            return type.getCounterNbtKey();
        }

        public RngType getType() {
            return type;
        }

        public int getUses() {
            return uses;
        }

        public void setUses(int uses) {
            this.uses = uses;
        }

        public int incrementUses() {
            return ++this.uses;
        }

        public void resetUses() {
            this.uses = 0;
        }
    }
}
