package com.ss.editor.ui.control.filter.operation;

import com.ss.editor.model.undo.editor.SceneChangeConsumer;
import com.ss.editor.model.undo.impl.AbstractEditorOperation;
import com.ss.editor.extension.scene.SceneNode;
import com.ss.editor.extension.scene.filter.SceneFilter;

import org.jetbrains.annotations.NotNull;

/**
 * The implementation of the {@link AbstractEditorOperation} to add a new {@link SceneFilter} to a {@link SceneNode}.
 *
 * @author JavaSaBr.
 */
public class AddSceneFilterOperation extends AbstractEditorOperation<SceneChangeConsumer> {

    /**
     * The new filter.
     */
    @NotNull
    private final SceneFilter sceneFilter;

    /**
     * The scene node.
     */
    @NotNull
    private final SceneNode sceneNode;

    /**
     * Instantiates a new Add scene filter operation.
     *
     * @param sceneFilter the scene filter
     * @param sceneNode   the scene node
     */
    public AddSceneFilterOperation(@NotNull final SceneFilter sceneFilter, @NotNull final SceneNode sceneNode) {
        this.sceneFilter = sceneFilter;
        this.sceneNode = sceneNode;
    }

    @Override
    protected void redoImpl(@NotNull final SceneChangeConsumer editor) {
        EXECUTOR_MANAGER.addJmeTask(() -> {
            sceneNode.addFilter(sceneFilter);
            EXECUTOR_MANAGER.addFxTask(() -> editor.notifyAddedFilter(sceneFilter));
        });
    }

    @Override
    protected void undoImpl(@NotNull final SceneChangeConsumer editor) {
        EXECUTOR_MANAGER.addJmeTask(() -> {
            sceneNode.removeFilter(sceneFilter);
            EXECUTOR_MANAGER.addFxTask(() -> editor.notifyRemovedFilter(sceneFilter));
        });
    }
}
