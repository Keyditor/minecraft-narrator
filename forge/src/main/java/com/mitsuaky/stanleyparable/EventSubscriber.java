package com.mitsuaky.stanleyparable;

import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Mod.EventBusSubscriber(modid = "stanleyparable", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EventSubscriber {
    private static final Logger LOGGER = LogManager.getLogger(EventSubscriber.class);

    public enum Event {
        ITEM_CRAFTED("item_crafted"),
        BLOCK_BROKEN("block_broken"),
        BLOCK_PLACED("block_placed"),
        PLAYER_DEATH("player_death"),
        ADVANCEMENT("advancement"),
        ITEM_PICKUP("item_pickup"),
        MOB_KILLED("mob_killed"),
        DIMENSION_CHANGED("dimension_changed");

        private final String value;

        Event(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Action {
        IGNORE("ignore"),
        CANCEL_EVENT("cancel_event"),
        SEND_CHAT("send_chat");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public static Action fromString(String text) {
            for (Action b : Action.values()) {
                if (b.value.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static String getAsId(Item item) {
        return item.getDescriptionId();
    }

    public static String getAsId(net.minecraft.world.level.block.Block block) {
        return block.getDescriptionId();
    }

    public static String getAsId(Entity entity) {
        return entity.getType().getDescriptionId();
    }

    public static String getAsId(AdvancementHolder advancement) {
        return advancement.id().toString();
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        LOGGER.debug("ItemCraftedEvent triggered");
        LOGGER.debug("ItemCraftedEvent triggered but not in crafting phase");
        if (event.getEntity() == null || event.getCrafting().isEmpty()) {
            LOGGER.debug("ItemCraftedEvent triggered without valid entity or crafting item");
            return;
        }

        String item = getAsId(event.getCrafting().getItem());
        int amount = event.getCrafting().getCount();
        Player player = event.getEntity();
        ItemCraftedEventData eventData = new ItemCraftedEventData(item, amount);
        IncomingEvent<ItemCraftedEventData> incomingEvent = new IncomingEvent<>(Event.ITEM_CRAFTED, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LOGGER.debug("BlockBreakEvent triggered");
        if (event.getPlayer() == null || event.getState().isAir()) {
            LOGGER.debug("BlockBreakEvent triggered without valid player or block state");
            return;
        }

        String tool = getAsId(event.getPlayer().getMainHandItem().getItem());
        String block = getAsId(event.getState().getBlock());
        Player player = event.getPlayer();
        BlockBrokenEventData eventData = new BlockBrokenEventData(block, tool);
        IncomingEvent<BlockBrokenEventData> incomingEvent = new IncomingEvent<>(Event.BLOCK_BROKEN, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LOGGER.debug("BlockPlaceEvent triggered");
        if (event.getEntity() == null || event.getPlacedBlock().isAir() || !(event.getEntity() instanceof Player)) {
            LOGGER.debug("BlockPlaceEvent triggered without valid player or block state");
            return;
        }

        String block = getAsId(event.getPlacedBlock().getBlock());
        Player player = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;
        BlockPlacedEventData eventData = new BlockPlacedEventData(block);
        IncomingEvent<BlockPlacedEventData> incomingEvent = new IncomingEvent<>(Event.BLOCK_PLACED, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        event.setCanceled(false);
        LOGGER.debug("Player LivingDeathEvent triggered");
        if (event.getEntity() == null || !(event.getEntity() instanceof Player player)) {
            LOGGER.debug("LivingDeathEvent triggered but is not a player");
            return;
        }

        String deathCause = event.getSource().getMsgId();
        PlayerDeathEventData eventData = new PlayerDeathEventData(deathCause);
        LOGGER.debug(eventData);
        IncomingEvent<PlayerDeathEventData> incomingEvent = new IncomingEvent<>(Event.PLAYER_DEATH, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onAchievement(AdvancementEarnEvent event) {
        LOGGER.debug("AdvancementEvent triggered");
        if (event.getEntity() == null) {
            LOGGER.debug("AdvancementEvent triggered without valid player");
            return;
        }

        AdvancementHolder advancement = event.getAdvancement();
        if (advancement.value().rewards().getRecipes().length > 0) {
            LOGGER.debug("AdvancementEvent triggered but is a recipe");
            return;
        }

        AdvancementEventData eventData = new AdvancementEventData(getAsId(event.getAdvancement()));
        IncomingEvent<AdvancementEventData> incomingEvent = new IncomingEvent<>(Event.ADVANCEMENT, eventData);
        processApiResponse(event.getEntity(), event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        LOGGER.debug("DimensionChangeEvent triggered");
        if (event.getEntity() == null || !(event.getEntity() instanceof Player)) {
            LOGGER.debug("DimensionChangeEvent triggered without valid player");
            return;
        }
        String dimension = event.getTo().location().toString();
        LOGGER.debug(dimension);
        DimensionChangeEventData eventData = new DimensionChangeEventData(dimension);
        IncomingEvent<DimensionChangeEventData> incomingEvent = new IncomingEvent<>(Event.DIMENSION_CHANGED, eventData);
        processApiResponse(event.getEntity(), event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        LOGGER.debug("ItemPickupEvent triggered");
        if (event.getEntity() == null || event.getStack().isEmpty()) {
            LOGGER.debug("ItemPickupEvent triggered without valid player or item");
            return;
        }

        String item = getAsId(event.getStack().getItem());
        int amount = event.getStack().getCount();
        Player player = event.getEntity();
        ItemPickupEventData eventData = new ItemPickupEventData(item, amount);
        IncomingEvent<ItemPickupEventData> incomingEvent = new IncomingEvent<>(Event.ITEM_PICKUP, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        LOGGER.debug("MobKilledEvent triggered");
        if (event.getEntity() == null || event.getSource().getEntity() == null || !(event.getSource().getEntity() instanceof Player player)) {
            LOGGER.debug("MobKilledEvent triggered without valid player or mob");
            return;
        }

        String mob = getAsId(event.getEntity());
        String weapon = getAsId(player.getMainHandItem().getItem());
        MobKilledEventData eventData = new MobKilledEventData(mob, weapon);
        IncomingEvent<MobKilledEventData> incomingEvent = new IncomingEvent<>(Event.MOB_KILLED, eventData);
        processApiResponse(player, event, incomingEvent.toJson());
    }

    private static void processApiResponse(Player player, net.minecraftforge.eventbus.api.Event event, JsonObject jsonEvent) {
        CompletableFuture<JsonObject> future = APICommunicator.sendEventAsync(jsonEvent);
        future.whenComplete(
                (response, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof TimeoutException) {
                            LOGGER.error("Timeout sending event to API: " + throwable.getMessage(), throwable);
                        } else {
                            LOGGER.error("Error sending event to API: " + throwable.getMessage(), throwable);
                        }
                    } else {
                        if (response == null) {
                            LOGGER.error("Received null response from API");
                            return;
                        }
                        LOGGER.debug("Received response from API: " + response);
                        handleResponse(player, event, response);
                    }
                }
        );
    }

    private static void handleResponse(Player player, net.minecraftforge.eventbus.api.Event event, JsonObject response) {
        Action action = Action.fromString(response.get("action").getAsString());
        if (action == null) {
            LOGGER.error("Received invalid action from API");
            return;
        }
        switch (action) {
            case IGNORE:
                LOGGER.debug("Ignoring event: " + event.getClass().getSimpleName());
                break;
            case CANCEL_EVENT:
                LOGGER.debug("Cancelling event: " + event.getClass().getSimpleName());
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }
                break;
            case SEND_CHAT:
                LOGGER.debug("Sending chat message: " + response.getAsJsonObject("data").get("text").getAsString());
                String chatMessage = response.getAsJsonObject("data").get("text").getAsString();
                player.sendSystemMessage(Component.literal(chatMessage));
                break;
            default:
                LOGGER.warn("Unhandled action: " + action);
        }
    }
}

class BaseEventData {
    JsonObject toJson() {
        return new JsonObject();
    }
}

class ItemCraftedEventData extends BaseEventData {
    String item;
    int amount;

    ItemCraftedEventData(String item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", item);
        json.addProperty("amount", amount);
        return json;
    }
}

class BlockBrokenEventData extends BaseEventData {
    String block;
    String tool;

    BlockBrokenEventData(String block, String tool) {
        this.block = block;
        this.tool = tool;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("block", block);
        json.addProperty("tool", tool);
        return json;
    }
}

class BlockPlacedEventData extends BaseEventData {
    String block;

    BlockPlacedEventData(String block) {
        this.block = block;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("block", block);
        return json;
    }
}

class PlayerDeathEventData extends BaseEventData {
    String cause;

    PlayerDeathEventData(String cause) {
        this.cause = cause;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("cause", cause);
        return json;
    }
}

class AdvancementEventData extends BaseEventData {
    String advancement;

    AdvancementEventData(String advancement) {
        this.advancement = advancement;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("advancement", advancement);
        return json;
    }
}

class DimensionChangeEventData extends BaseEventData {
    String dimension;

    DimensionChangeEventData(String dimension) {
        this.dimension = dimension;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("dimension", dimension);
        return json;
    }
}

class ItemPickupEventData extends BaseEventData {
    String item;
    int amount;

    ItemPickupEventData(String item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", item);
        json.addProperty("amount", amount);
        return json;
    }
}

class MobKilledEventData extends BaseEventData {
    String mob;
    String weapon;

    MobKilledEventData(String mob, String weapon) {
        this.mob = mob;
        this.weapon = weapon;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("mob", mob);
        json.addProperty("weapon", weapon);
        return json;
    }
}

class IncomingEvent<T extends BaseEventData> {
    String event;
    T data;

    IncomingEvent(EventSubscriber.Event event, T data) {
        this.event = event.getValue();
        this.data = data;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("event", event);
        json.add("data", data.toJson());
        return json;
    }
}
