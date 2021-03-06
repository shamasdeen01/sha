package com.ss.editor.ui.control.tree.action.impl;

import com.ss.editor.Messages;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.dialog.GenerateTangentsDialog;
import com.ss.editor.ui.control.tree.NodeTree;
import com.ss.editor.ui.control.tree.action.AbstractNodeAction;
import com.ss.editor.ui.control.tree.node.TreeNode;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The action for generating tangents.
 *
 * @author JavaSaBr
 */
public class TangentGeneratorAction extends AbstractNodeAction<ModelChangeConsumer> {

    public TangentGeneratorAction(@NotNull final NodeTree<?> nodeTree, @NotNull final TreeNode<?> node) {
        super(nodeTree, node);
    }

    @Override
    @FxThread
    protected @NotNull String getName() {
        return Messages.MODEL_NODE_TREE_ACTION_TANGENT_GENERATOR;
    }

    @Override
    @FxThread
    protected @Nullable Image getIcon() {
        return Icons.MESH_16;
    }

    @Override
    @FxThread
    protected void process() {
        super.process();
        final GenerateTangentsDialog dialog = new GenerateTangentsDialog(getNodeTree(), getNode());
        dialog.show();
    }
}
