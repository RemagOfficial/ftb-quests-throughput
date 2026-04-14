package com.remag.throughput.events;

import com.remag.throughput.FTBQuestsThroughputAddon;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Example event listener for throughput events.
 *
 * Usage:
 * - Listen to ThroughputReachedEvent when throughput goes ABOVE the required rate
 * - Listen to ThroughputDroppedEvent when throughput goes BELOW the required rate
 *
 * To use in your own mod:
 * @EventBusSubscriber(modid = "your_mod_id")
 * public class YourEventListener {
 *     @SubscribeEvent
 *     public static void onThroughputReached(ThroughputEvents.ThroughputReachedEvent event) {
 *         // Handle throughput reached
 *     }
 *
 *     @SubscribeEvent
 *     public static void onThroughputDropped(ThroughputEvents.ThroughputDroppedEvent event) {
 *         // Handle throughput dropped
 *     }
 * }
 */
@EventBusSubscriber(modid = FTBQuestsThroughputAddon.MODID)
public class ThroughputEventListeners {

    @SubscribeEvent
    public static void onThroughputReached(ThroughputEvents.ThroughputReachedEvent event) {
        // This event is fired when throughput goes over the required rate
        // Use this for custom logic when the required throughput is sustained
    }

    @SubscribeEvent
    public static void onThroughputDropped(ThroughputEvents.ThroughputDroppedEvent event) {
        // This event is fired when throughput drops below the required rate
        // You can check:
        // - event.willRegress() to see if progress will regress (true) or reset (false)
        // - event.getCurrentProgress() to see current progress before the change
        // - event.getCurrentRate() to see what the current rate was
        // - event.getRequiredRate() to compare against the requirement
    }
}

