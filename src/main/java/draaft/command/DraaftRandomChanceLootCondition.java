package draaft.command;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import draaft.persistent.WorldState;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonSerializer;
import net.minecraft.util.registry.Registry;

public class DraaftRandomChanceLootCondition implements LootCondition {
    private static final Identifier IDENTIFIER = new Identifier("draaft", "random_chance");

    private static final LootConditionType TYPE =
        Registry.register(Registry.LOOT_CONDITION_TYPE, IDENTIFIER, new LootConditionType(new Serializer()));

    private final String rng;
    private final double chance;

    public DraaftRandomChanceLootCondition(String rng, double chance) {
        this.rng = rng;
        this.chance = chance;
    }

    @Override
    public LootConditionType getType() {
        return TYPE;
    }

    @Override
    public boolean test(LootContext lootContext) {
        var worldState = WorldState.getServerState(lootContext.getWorld());

        var rng = worldState.getOrCreateRng(new WorldState.RngType(this.rng), lootContext.getWorld());

        return rng.getRandom().nextDouble() < this.chance;
    }

    private static class Serializer implements JsonSerializer<DraaftRandomChanceLootCondition> {
        @Override
        public void toJson(JsonObject json, DraaftRandomChanceLootCondition instance, JsonSerializationContext context) {
            json.addProperty("rng", instance.rng);
            json.addProperty("chance", instance.chance);
        }

        @Override
        public DraaftRandomChanceLootCondition fromJson(JsonObject json, JsonDeserializationContext context) {
            return new DraaftRandomChanceLootCondition(
                json.get("rng").getAsString(),
                json.get("chance").getAsDouble()
            );
        }
    }
}
