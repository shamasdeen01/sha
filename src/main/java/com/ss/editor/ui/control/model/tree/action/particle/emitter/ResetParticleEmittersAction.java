package com.ss.editor.ui.control.model.tree.action.particle.emitter;

import static com.ss.editor.util.NodeUtils.visitSpatial;
import com.jme3.effect.ParticleEmitter;
import com.jme3.scene.Node;
import com.ss.editor.Messages;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.control.tree.action.AbstractNodeAction;
import com.ss.editor.ui.control.tree.NodeTree;
import com.ss.editor.ui.control.tree.node.TreeNode;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tonegod.emitter.ParticleEmitterNode;

/**
 * The action to reset a {@link ParticleEmitterNode} and a {@link ParticleEmitter}.
 *
 * @author JavaSaBr
 */
public class ResetParticleEmittersAction extends AbstractNodeAction<ModelChangeConsumer> {

    public ResetParticleEmittersAction(@NotNull final NodeTree<?> nodeTree, @NotNull final TreeNode<?> node) {
        super(nodeTree, node);
    }

    @Override
    @FxThread
    protected @Nullable Image getIcon() {
        return Icons.REPLAY_16;
    }

    @Override
    @FxThread
    protected @NotNull String getName() {
        return Messages.MODEL_NODE_TREE_ACTION_RESET_PARTICLE_EMITTERS;
    }

    @Override
    @FxThread
    protected void process() {
        super.process();

        final TreeNode<?> treeNode = getNode();
        final Node node = (Node) treeNode.getElement();

        EXECUTOR_MANAGER.addJMETask(() -> visitSpatial(node, ParticleEmitterNode.class, ParticleEmitterNode::reset));
        EXECUTOR_MANAGER.addJMETask(() -> visitSpatial(node, ParticleEmitter.class, ParticleEmitter::killAllParticles));
    }
}
