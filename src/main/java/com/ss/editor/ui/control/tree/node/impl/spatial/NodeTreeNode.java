package com.ss.editor.ui.control.tree.node.impl.spatial;

import static com.ss.editor.util.EditorUtil.*;
import static com.ss.rlib.common.util.ObjectUtils.notNull;
import com.jme3.asset.ModelKey;
import com.jme3.effect.ParticleEmitter;
import com.jme3.scene.AssetLinkNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.ss.editor.FileExtensions;
import com.ss.editor.Messages;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.extension.scene.SceneLayer;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.model.undo.impl.AddChildOperation;
import com.ss.editor.model.undo.impl.MoveChildOperation;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.control.model.ModelNodeTree;
import com.ss.editor.ui.control.tree.NodeTree;
import com.ss.editor.ui.control.tree.action.impl.*;
import com.ss.editor.ui.control.tree.action.impl.audio.CreateAudioNodeAction;
import com.ss.editor.ui.control.tree.action.impl.geometry.CreateBoxAction;
import com.ss.editor.ui.control.tree.action.impl.geometry.CreateQuadAction;
import com.ss.editor.ui.control.tree.action.impl.geometry.CreateSphereAction;
import com.ss.editor.ui.control.tree.action.impl.light.CreateAmbientLightAction;
import com.ss.editor.ui.control.tree.action.impl.light.CreateDirectionLightAction;
import com.ss.editor.ui.control.tree.action.impl.light.CreatePointLightAction;
import com.ss.editor.ui.control.tree.action.impl.light.CreateSpotLightAction;
import com.ss.editor.ui.control.tree.action.impl.particle.emitter.CreateParticleEmitterAction;
import com.ss.editor.ui.control.tree.action.impl.particle.emitter.ResetParticleEmittersAction;
import com.ss.editor.ui.control.tree.action.impl.terrain.CreateTerrainAction;
import com.ss.editor.ui.control.tree.node.TreeNode;
import com.ss.editor.ui.util.UiUtils;
import com.ss.editor.util.EditorUtil;
import com.ss.editor.util.GeomUtils;
import com.ss.editor.util.NodeUtils;
import com.ss.rlib.common.util.array.Array;
import com.ss.rlib.common.util.array.ArrayFactory;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The implementation of the {@link SpatialTreeNode} for representing the {@link Node} in the editor.
 *
 * @param <T> the type of Node
 * @author JavaSaBr
 */
public class NodeTreeNode<T extends Node> extends SpatialTreeNode<T> {

    @FunctionalInterface
    public interface ChildrenFilter {

        @FxThread
        boolean isNeedExclude(@NotNull Node node, @NotNull Spatial spatial);
    }

    @FunctionalInterface
    public interface ParticleEmitterFinder {

        @FxThread
        boolean isExist(@NotNull Node node);
    }

    private static final Array<ParticleEmitterFinder> PARTICLE_EMITTER_FINDERS =
            ArrayFactory.newCopyOnModifyArray(ParticleEmitterFinder.class);

    private static final Array<ChildrenFilter> NODE_CHILDREN_FILTERS =
            ArrayFactory.newCopyOnModifyArray(ChildrenFilter.class);

    /**
     * Register the additional particle emitter finder.
     * The finder should return true when a node contains some particle emitter.
     *
     * @param finder the additional particle emitter finder.
     */
    @FromAnyThread
    public static void registerParticleEmitterFinder(@NotNull ParticleEmitterFinder finder) {
        PARTICLE_EMITTER_FINDERS.add(finder);
    }

    /**
     * Register the additional node children filter.
     * The filter should return true when a child should be excluded.
     *
     * @param filter the additional children filter.
     */
    @FromAnyThread
    public static void registerNodeChildrenFilter(@NotNull ChildrenFilter filter) {
        NODE_CHILDREN_FILTERS.add(filter);
    }

    public NodeTreeNode(@NotNull T element, long objectId) {
        super(element, objectId);
    }

    @Override
    protected @NotNull Optional<Menu> createToolMenu(@NotNull NodeTree<?> nodeTree) {
        var toolMenu = new Menu(Messages.MODEL_NODE_TREE_ACTION_TOOLS, new ImageView(Icons.INFLUENCER_16));
        toolMenu.getItems().addAll(new OptimizeGeometryAction(nodeTree, this));
        return Optional.of(toolMenu);
    }

    @Override
    @FxThread
    protected @NotNull Optional<Menu> createCreationMenu(@NotNull NodeTree<?> nodeTree) {

        var menuOptional = super.createCreationMenu(nodeTree);

        if (!menuOptional.isPresent()) {
            return Optional.empty();
        }

        var createPrimitiveMenu = new Menu(Messages.MODEL_NODE_TREE_ACTION_CREATE_PRIMITIVE,
                new ImageView(Icons.ADD_12));

        createPrimitiveMenu.getItems()
                .addAll(new CreateBoxAction(nodeTree, this),
                        new CreateSphereAction(nodeTree, this),
                        new CreateQuadAction(nodeTree, this));

        var addLightMenu = new Menu(Messages.MODEL_NODE_TREE_ACTION_LIGHT,
                new ImageView(Icons.ADD_12));

        addLightMenu.getItems()
                .addAll(new CreateSpotLightAction(nodeTree, this),
                        new CreatePointLightAction(nodeTree, this),
                        new CreateAmbientLightAction(nodeTree, this),
                        new CreateDirectionLightAction(nodeTree, this));

        var resultMenu = menuOptional.get();
        resultMenu.getItems()
                .addAll(new CreateNodeAction(nodeTree, this),
                        new LoadModelAction(nodeTree, this),
                        new LinkModelAction(nodeTree, this),
                        new CreateSkyAction(nodeTree, this),
                        new CreateEditableSkyAction(nodeTree, this),
                        new CreateParticleEmitterAction(nodeTree, this),
                        new CreateAudioNodeAction(nodeTree, this),
                        new CreateTerrainAction(nodeTree, this),
                        createPrimitiveMenu,
                        addLightMenu);

        return menuOptional;
    }

    @Override
    @FxThread
    public void fillContextMenu(@NotNull NodeTree<?> nodeTree, @NotNull ObservableList<MenuItem> items) {

        if (!(nodeTree instanceof ModelNodeTree)) {
            return;
        }

        var element = getElement();
        var emitter = NodeUtils.findSpatial(element, ParticleEmitter.class::isInstance);

        if (emitter != null || PARTICLE_EMITTER_FINDERS.search(element, ParticleEmitterFinder::isExist) != null) {
            items.add(new ResetParticleEmittersAction(nodeTree, this));
        }

        super.fillContextMenu(nodeTree, items);
    }

    @Override
    @FxThread
    public boolean hasChildren(@NotNull NodeTree<?> nodeTree) {
        return nodeTree instanceof ModelNodeTree;
    }

    @Override
    @FxThread
    public @NotNull Array<TreeNode<?>> getChildren(@NotNull NodeTree<?> nodeTree) {

        var element = getElement();
        var result = ArrayFactory.<TreeNode<?>>newArray(TreeNode.class);
        var children = getSpatialChildren();

        for (var child : children) {
            if (NODE_CHILDREN_FILTERS.search(element, child, ChildrenFilter::isNeedExclude) == null) {
                result.add(FACTORY_REGISTRY.createFor(child));
            }
        }

        result.addAll(super.getChildren(nodeTree));

        return result;
    }

    /**
     * Get the children spatial.
     *
     * @return the children spatial.
     */
    @FxThread
    protected @NotNull List<Spatial> getSpatialChildren() {
        return getElement().getChildren();
    }

    @Override
    @FxThread
    public boolean canAccept(@NotNull TreeNode<?> treeNode, boolean isCopy) {

        if (treeNode == this) {
            return false;
        }

        var element = treeNode.getElement();
        if (element instanceof Spatial) {
            return GeomUtils.canAttach(getElement(), (Spatial) element, isCopy);
        }

        return super.canAccept(treeNode, isCopy);
    }

    @Override
    @FxThread
    public void accept(@NotNull ChangeConsumer changeConsumer, @NotNull Object object, boolean isCopy) {

        var newParent = getElement();

        if (object instanceof Spatial) {

            var spatial = (Spatial) object;

            if (isCopy) {

                var clone = spatial.clone();
                var layer = SceneLayer.getLayer(spatial);

                if (layer != null) {
                    SceneLayer.setLayer(layer, clone);
                }

                changeConsumer.execute(new AddChildOperation(clone, newParent, true));

            } else {
                var parent = spatial.getParent();
                var childIndex = parent.getChildIndex(spatial);
                changeConsumer.execute(new MoveChildOperation(spatial, parent, newParent, childIndex));
            }
        }

        super.accept(changeConsumer, object, isCopy);
    }

    @Override
    @FxThread
    public void add(@NotNull TreeNode<?> child) {
        super.add(child);

        var node = getElement();
        var toAdd = child.getElement();

        if (toAdd instanceof Spatial) {
            node.attachChildAt((Spatial) toAdd, 0);
        }
    }

    @Override
    @FxThread
    public void remove(@NotNull TreeNode<?> child) {
        super.remove(child);

        var node = getElement();
        var toRemove = child.getElement();

        if (toRemove instanceof Spatial) {
            node.detachChild((Spatial) toRemove);
        }
    }

    @Override
    @FxThread
    public @Nullable Image getIcon() {
        return Icons.NODE_16;
    }

    @Override
    @FxThread
    public boolean canAcceptExternal(@NotNull Dragboard dragboard) {
        return UiUtils.isHasFile(dragboard, FileExtensions.JME_OBJECT);
    }

    @Override
    @FxThread
    public void acceptExternal(@NotNull Dragboard dragboard, @NotNull ChangeConsumer consumer) {
        UiUtils.handleDroppedFile(dragboard, FileExtensions.JME_OBJECT,
                getElement(), consumer, this::dropExternalObject);
    }

    /**
     * Add the external object to this node.
     *
     * @param node this node.
     * @param cons the change consumer.
     * @param path the path to the external object.
     */
    @FxThread
    protected void dropExternalObject(
            @NotNull T node,
            @NotNull ChangeConsumer cons,
            @NotNull Path path
    ) {

        var defaultLayer = getDefaultLayer(cons);
        var assetFile = notNull(getAssetFile(path), "Not found asset file for " + path);
        var assetPath = toAssetPath(assetFile);
        var modelKey = new ModelKey(assetPath);

        var assetManager = EditorUtil.getAssetManager();
        var loadedModel = assetManager.loadModel(assetPath);
        var assetLinkNode = new AssetLinkNode(modelKey);
        assetLinkNode.attachLinkedChild(loadedModel, modelKey);

        if (defaultLayer != null) {
            SceneLayer.setLayer(defaultLayer, loadedModel);
        }

        cons.execute(new AddChildOperation(assetLinkNode, node));
    }
}
