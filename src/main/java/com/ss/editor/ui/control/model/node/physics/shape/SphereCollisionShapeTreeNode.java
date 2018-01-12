package com.ss.editor.ui.control.model.node.physics.shape;

import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.ss.editor.Messages;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.ui.Icons;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of node to show {@link SphereCollisionShape}.
 *
 * @author JavaSaBr
 */
public class SphereCollisionShapeTreeNode extends CollisionShapeTreeNode<SphereCollisionShape> {

    public SphereCollisionShapeTreeNode(@NotNull final SphereCollisionShape element, final long objectId) {
        super(element, objectId);
    }

    @Override
    @FxThread
    public @Nullable Image getIcon() {
        return Icons.SPHERE_16;
    }

    @Override
    @FromAnyThread
    public @NotNull String getName() {
        return Messages.MODEL_FILE_EDITOR_NODE_SPHERE_COLLISION_SHAPE;
    }
}
