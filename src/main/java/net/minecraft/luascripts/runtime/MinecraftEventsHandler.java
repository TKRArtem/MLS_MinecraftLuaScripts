package net.minecraft.luascripts.runtime;

import net.minecraft.world.IWorld;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class MinecraftEventsHandler {
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        IWorld world = event.getWorld();
        Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("world", world);
        dependencies.put("event", event);


    }
}
