package com.remag.throughput.events;

import com.remag.throughput.tasks.ThroughputTask;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.neoforged.bus.api.Event;

/**
 * Base event class for throughput-related events.
 * Fire these on the NeoForge event bus.
 */
public class ThroughputEvents {

    /**
     * Fired when throughput goes over the required rate.
     * The event is posted AFTER the progress is incremented.
     */
    public static class ThroughputReachedEvent extends Event {
        private final ThroughputTask task;
        private final TeamData teamData;
        private final long currentRate;
        private final long requiredRate;

        public ThroughputReachedEvent(ThroughputTask task, TeamData teamData, long currentRate, long requiredRate) {
            this.task = task;
            this.teamData = teamData;
            this.currentRate = currentRate;
            this.requiredRate = requiredRate;
        }

        public ThroughputTask getTask() {
            return task;
        }

        public TeamData getTeamData() {
            return teamData;
        }

        public long getCurrentRate() {
            return currentRate;
        }

        public long getRequiredRate() {
            return requiredRate;
        }
    }

    /**
     * Fired when throughput falls below the required rate.
     * The event is posted BEFORE the progress is regressed or reset.
     */
    public static class ThroughputDroppedEvent extends Event {
        private final ThroughputTask task;
        private final TeamData teamData;
        private final long currentRate;
        private final long requiredRate;
        private final long currentProgress;
        private final boolean willRegress;

        public ThroughputDroppedEvent(ThroughputTask task, TeamData teamData, long currentRate, long requiredRate, long currentProgress, boolean willRegress) {
            this.task = task;
            this.teamData = teamData;
            this.currentRate = currentRate;
            this.requiredRate = requiredRate;
            this.currentProgress = currentProgress;
            this.willRegress = willRegress;
        }

        public ThroughputTask getTask() {
            return task;
        }

        public TeamData getTeamData() {
            return teamData;
        }

        public long getCurrentRate() {
            return currentRate;
        }

        public long getRequiredRate() {
            return requiredRate;
        }

        public long getCurrentProgress() {
            return currentProgress;
        }

        /**
         * @return true if progress will regress by 1, false if progress will be reset to 0
         */
        public boolean willRegress() {
            return willRegress;
        }
    }
}

