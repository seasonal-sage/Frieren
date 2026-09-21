package com.timecapsule.scheduler;

import com.timecapsule.core.Memory;

/**
 * Observer of broken seals (Observer Pattern). Implementations are told
 * once per entry when its date arrives.
 */
public interface CapsuleListener {

    /**
     * Called on the scheduler's thread — UI work must hop to the FX thread
     * (e.g. {@code Platform.runLater}) before touching the scene.
     */
    void onCapsuleOpened(Memory memory);
}
