package com.ss.editor.ui.control.tree.node.impl.physics.shape;

import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.ss.editor.Messages;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.ui.Icons;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of node to show {@link HullCollisionShape}.
 *
 * @author JavaSaBr
 */
public class HullCollisionShapeTreeNode extends CollisionShapeTreeNode<HullCollisionShape> {

    public HullCollisionShapeTreeNode(@NotNull final HullCollisionShape element, final long objectId) {
        super(element, objectId);
    }

    @Override
    @FxThread
    public @Nullable Image getIcon() {
        return Icons.MESH_16;
    }

    @Override
    @FromAnyThread
    public @NotNull String getName() {
        return Messages.MODEL_FILE_EDITOR_NODE_HULL_COLLISION_SHAPE;
    }
}
