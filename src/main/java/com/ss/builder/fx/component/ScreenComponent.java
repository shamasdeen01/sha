package com.ss.builder.ui.component;

import com.ss.builder.annotation.BackgroundThread;
import com.ss.builder.annotation.FromAnyThread;
import com.ss.editor.annotation.BackgroundThread;
import com.ss.editor.annotation.FromAnyThread;
import org.jetbrains.annotations.Nullable;

/**
 * The interface to implement a scene component.
 *
 * @author JavaSaBr
 */
public interface ScreenComponent {

    /**
     * Gets the component id.
     *
     * @return the component id.
     */
    @FromAnyThread
    default @Nullable String getComponentId() {
        return null;
    }

    /**
     * Notify about finishing building the result scene.
     */
    @BackgroundThread
    default void notifyFinishBuild() {
    }
}