package com.ss.editor.ui.component.editor.impl.material;

import static com.jme3.renderer.queue.RenderQueue.Bucket.Inherit;
import static com.jme3.renderer.queue.RenderQueue.Bucket.values;
import static com.ss.editor.Messages.MATERIAL_EDITOR_NAME;
import static com.ss.editor.util.EditorUtil.getAssetFile;
import static com.ss.editor.util.EditorUtil.toAssetPath;
import static com.ss.editor.util.MaterialUtils.updateMaterialIdNeed;
import static com.ss.rlib.util.ObjectUtils.notNull;
import static javafx.collections.FXCollections.observableArrayList;
import com.jme3.asset.AssetManager;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.TextureKey;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.ss.editor.FileExtensions;
import com.ss.editor.Messages;
import com.ss.editor.annotation.BackgroundThread;
import com.ss.editor.annotation.FXThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.config.EditorConfig;
import com.ss.editor.manager.ResourceManager;
import com.ss.editor.model.node.material.RootMaterialSettings;
import com.ss.editor.model.undo.EditorOperationControl;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.model.undo.editor.MaterialChangeConsumer;
import com.ss.editor.plugin.api.editor.Advanced3DFileEditorWithSplitRightTool;
import com.ss.editor.serializer.MaterialSerializer;
import com.ss.editor.state.editor.impl.material.MaterialEditor3DState;
import com.ss.editor.state.editor.impl.material.MaterialEditor3DState.ModelType;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.component.editor.EditorDescription;
import com.ss.editor.ui.component.editor.state.EditorState;
import com.ss.editor.ui.component.editor.state.impl.EditorMaterialEditorState;
import com.ss.editor.ui.component.tab.EditorToolComponent;
import com.ss.editor.ui.control.property.PropertyEditor;
import com.ss.editor.ui.control.property.operation.PropertyOperation;
import com.ss.editor.ui.control.tree.NodeTree;
import com.ss.editor.ui.control.tree.node.TreeNode;
import com.ss.editor.ui.css.CSSClasses;
import com.ss.editor.ui.event.impl.FileChangedEvent;
import com.ss.editor.ui.util.DynamicIconSupport;
import com.ss.editor.ui.util.UIUtils;
import com.ss.editor.util.MaterialUtils;
import com.ss.rlib.ui.util.FXUtils;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The implementation of the Editor to edit materials.
 *
 * @author JavaSaBr
 */
public class MaterialFileEditor extends
        Advanced3DFileEditorWithSplitRightTool<MaterialEditor3DState, EditorMaterialEditorState> implements MaterialChangeConsumer {

    /**
     * The constant DESCRIPTION.
     */
    @NotNull
    public static final EditorDescription DESCRIPTION = new EditorDescription();

    static {
        DESCRIPTION.setConstructor(MaterialFileEditor::new);
        DESCRIPTION.setEditorName(MATERIAL_EDITOR_NAME);
        DESCRIPTION.setEditorId(MaterialFileEditor.class.getSimpleName());
        DESCRIPTION.addExtension(FileExtensions.JME_MATERIAL);
    }

    /**
     * The default flag of enabling light.
     */
    public static final boolean DEFAULT_LIGHT_ENABLED = true;

    @NotNull
    private static final ResourceManager RESOURCE_MANAGER = ResourceManager.getInstance();

    @NotNull
    private static final ObservableList<RenderQueue.Bucket> BUCKETS = observableArrayList(values());

    /**
     * The settings tree.
     */
    @Nullable
    private NodeTree<MaterialChangeConsumer> settingsTree;

    /**
     * The property editor.
     */
    @Nullable
    private PropertyEditor<MaterialChangeConsumer> propertyEditor;

    /**
     * The current editing material.
     */
    @Nullable
    private Material currentMaterial;

    /**
     * The button to use a cube.
     */
    @Nullable
    private ToggleButton cubeButton;

    /**
     * The button to use a sphere.
     */
    @Nullable
    private ToggleButton sphereButton;

    /**
     * The button to use a plane.
     */
    @Nullable
    private ToggleButton planeButton;

    /**
     * The button to use a light.
     */
    @Nullable
    private ToggleButton lightButton;

    /**
     * The list of RenderQueue.Bucket.
     */
    @Nullable
    private ComboBox<RenderQueue.Bucket> bucketComboBox;

    /**
     * The list of material definitions.
     */
    @Nullable
    private ComboBox<String> materialDefinitionBox;


    private MaterialFileEditor() {
        super();
    }

    @Override
    @FXThread
    protected @NotNull MaterialEditor3DState create3DEditorState() {
        return new MaterialEditor3DState(this);
    }

    @Override
    @FXThread
    protected void processChangedFile(@NotNull final FileChangedEvent event) {
        super.processChangedFile(event);

        final Material currentMaterial = getCurrentMaterial();
        final Path file = event.getFile();

        EXECUTOR_MANAGER.addJMETask(() -> {
            final Material newMaterial = updateMaterialIdNeed(file, currentMaterial);
            if (newMaterial != null) {
                EXECUTOR_MANAGER.addFXTask(() -> reload(newMaterial));
            }
        });
    }

    @Override
    @BackgroundThread
    public void doSave(@NotNull final Path toStore) throws IOException {
        super.doSave(toStore);

        final Material currentMaterial = getCurrentMaterial();
        final String content = MaterialSerializer.serializeToString(currentMaterial);

        try (final PrintWriter out = new PrintWriter(Files.newOutputStream(toStore))) {
            out.print(content);
        }
    }

    @Override
    @FXThread
    protected void handleExternalChanges() {
        super.handleExternalChanges();

        final Path assetFile = notNull(getAssetFile(getEditFile()));
        final MaterialKey materialKey = new MaterialKey(toAssetPath(assetFile));

        final AssetManager assetManager = EDITOR.getAssetManager();
        final Material material = assetManager.loadAsset(materialKey);

        reload(material);

        final EditorOperationControl operationControl = getOperationControl();
        operationControl.clear();
    }

    @Override
    @FXThread
    protected boolean handleKeyActionImpl(@NotNull final KeyCode keyCode, final boolean isPressed,
                                          final boolean isControlDown, final boolean isButtonMiddleDown) {

        if (isPressed && isControlDown && keyCode == KeyCode.Z) {
            undo();
            return true;
        } else if (isPressed && isControlDown && keyCode == KeyCode.Y) {
            redo();
            return true;
        } else if (isPressed && keyCode == KeyCode.C && !isControlDown && !isButtonMiddleDown) {
            final ToggleButton cubeButton = getCubeButton();
            cubeButton.setSelected(true);
            return true;
        } else if (isPressed && keyCode == KeyCode.S && !isControlDown && !isButtonMiddleDown) {
            final ToggleButton sphereButton = getSphereButton();
            sphereButton.setSelected(true);
            return true;
        } else if (isPressed && keyCode == KeyCode.P && !isControlDown && !isButtonMiddleDown) {
            final ToggleButton planeButton = getPlaneButton();
            planeButton.setSelected(true);
            return true;
        } else if (isPressed && keyCode == KeyCode.L && !isControlDown && !isButtonMiddleDown) {
            final ToggleButton lightButton = getLightButton();
            lightButton.setSelected(!lightButton.isSelected());
            return true;
        }

        return super.handleKeyActionImpl(keyCode, isPressed, isControlDown, isButtonMiddleDown);
    }

    @Override
    @FXThread
    protected void createToolComponents(@NotNull final EditorToolComponent container, @NotNull final StackPane root) {
        super.createToolComponents(container, root);

        settingsTree = new NodeTree<>(this::selectedFromTree, this);
        propertyEditor = new PropertyEditor<>(this);
        propertyEditor.prefHeightProperty().bind(root.heightProperty());

        container.addComponent(buildSplitComponent(settingsTree, propertyEditor, root), Messages.MATERIAL_SETTINGS_MAIN);

        FXUtils.addClassTo(settingsTree.getTreeView(), CSSClasses.TRANSPARENT_TREE_VIEW);
    }

    /**
     * @return the settings tree.
     */
    @FromAnyThread
    private @NotNull NodeTree<MaterialChangeConsumer> getSettingsTree() {
        return notNull(settingsTree);
    }

    /**
     * @return the property editor.
     */
    @FromAnyThread
    private @NotNull PropertyEditor<MaterialChangeConsumer> getPropertyEditor() {
        return notNull(propertyEditor);
    }

    /**
     * Handle selected object from tree.
     *
     * @param object the selected object.
     */
    private void selectedFromTree(@Nullable final Object object) {

        Object parent = null;
        Object element;

        if (object instanceof TreeNode<?>) {
            final TreeNode treeNode = (TreeNode) object;
            final TreeNode parentNode = treeNode.getParent();
            parent = parentNode == null ? null : parentNode.getElement();
            element = treeNode.getElement();
        } else {
            element = object;
        }

        getPropertyEditor().buildFor(element, parent);
    }

    /**
     * Try to apply dropped texture.
     *
     * @param editor    the editor.
     * @param dragEvent the drag event.
     * @param path      the path to the texture.
     */
    private void applyTexture(@NotNull final MaterialFileEditor editor, @NotNull final DragEvent dragEvent,
                              @NotNull final Path path) {

        final String textureName = path.getFileName().toString();
        final int textureType = MaterialUtils.getPossibleTextureType(textureName);

        if (textureType == 0) {
            return;
        }

        final String[] paramNames = MaterialUtils.getPossibleParamNames(textureType);
        final Material currentMaterial = getCurrentMaterial();
        final MaterialDef materialDef = currentMaterial.getMaterialDef();

        final Optional<MatParam> param = Arrays.stream(paramNames)
                .map(materialDef::getMaterialParam)
                .filter(Objects::nonNull)
                .filter(p -> p.getVarType() == VarType.Texture2D)
                .findAny();

        if (!param.isPresent()) {
            return;
        }

        final MatParam matParam = param.get();

        EXECUTOR_MANAGER.addJMETask(() -> {

            final EditorConfig config = EditorConfig.getInstance();
            final Path assetFile = notNull(getAssetFile(path));
            final TextureKey textureKey = new TextureKey(toAssetPath(assetFile));
            textureKey.setFlipY(config.isDefaultUseFlippedTexture());

            final AssetManager assetManager = EDITOR.getAssetManager();
            final Texture texture = assetManager.loadTexture(textureKey);
            texture.setWrap(Texture.WrapMode.Repeat);

            final String paramName = matParam.getName();
            final MatParamTexture textureParam = currentMaterial.getTextureParam(paramName);
            final Texture currentTexture = textureParam == null? null : textureParam.getTextureValue();

            PropertyOperation<ChangeConsumer, Material, Texture> operation =
                    new PropertyOperation<>(currentMaterial, paramName, texture, currentTexture);
            operation.setApplyHandler((material, newTexture) -> material.setTexture(paramName, newTexture));

            execute(operation);
        });
    }

    @Override
    protected void dragDropped(@NotNull final DragEvent dragEvent) {
        super.dragDropped(dragEvent);
        UIUtils.handleDroppedFile(dragEvent, FileExtensions.TEXTURE_EXTENSIONS, this,
                dragEvent, this::applyTexture);
    }

    @Override
    protected void dragOver(@NotNull final DragEvent dragEvent) {
        super.dragOver(dragEvent);
        UIUtils.acceptIfHasFile(dragEvent, FileExtensions.TEXTURE_EXTENSIONS);
    }


    @Override
    @FXThread
    protected void doOpenFile(@NotNull final Path file) {
        super.doOpenFile(file);

        final Path assetFile = notNull(getAssetFile(file));
        final MaterialKey materialKey = new MaterialKey(toAssetPath(assetFile));

        final AssetManager assetManager = EDITOR.getAssetManager();
        final Material material = assetManager.loadAsset(materialKey);

        final MaterialEditor3DState editor3DState = getEditor3DState();
        editor3DState.changeMode(ModelType.BOX);

        reload(material);
    }

    @Override
    @FXThread
    protected void loadState() {
        super.loadState();

        switch (ModelType.valueOf(editorState.getModelType())) {
            case BOX:
                getCubeButton().setSelected(true);
                break;
            case SPHERE:
                getSphereButton().setSelected(true);
                break;
            case QUAD:
                getPlaneButton().setSelected(true);
                break;
        }

        getBucketComboBox().getSelectionModel().select(editorState.getBucketType());
        getLightButton().setSelected(editorState.isLightEnable());
    }

    @Override
    protected @Nullable Supplier<EditorState> getEditorStateFactory() {
        return EditorMaterialEditorState::new;
    }

    /**
     * Reload the material.
     */
    @FXThread
    private void reload(@NotNull final Material material) {
        setCurrentMaterial(material);
        setIgnoreListeners(true);
        try {

            final MaterialEditor3DState editor3DState = getEditor3DState();
            editor3DState.updateMaterial(material);

            getSettingsTree().fill(new RootMaterialSettings(material));

            final ComboBox<String> materialDefinitionBox = getMaterialDefinitionBox();
            final ObservableList<String> items = materialDefinitionBox.getItems();
            items.clear();
            items.addAll(RESOURCE_MANAGER.getAvailableMaterialDefinitions());

            final MaterialDef materialDef = material.getMaterialDef();
            materialDefinitionBox.getSelectionModel().select(materialDef.getAssetName());

        } finally {
            setIgnoreListeners(false);
        }
    }

    /**
     * @return the list of material definitions.
     */
    @FromAnyThread
    private @NotNull ComboBox<String> getMaterialDefinitionBox() {
        return notNull(materialDefinitionBox);
    }

    @Override
    @FXThread
    protected boolean needToolbar() {
        return true;
    }

    @Override
    @FXThread
    protected void createToolbar(@NotNull final HBox container) {

        cubeButton = new ToggleButton();
        cubeButton.setTooltip(new Tooltip(Messages.MATERIAL_FILE_EDITOR_ACTION_CUBE + " (C)"));
        cubeButton.setGraphic(new ImageView(Icons.CUBE_16));
        cubeButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                changeModelType(ModelType.BOX, newValue));

        sphereButton = new ToggleButton();
        sphereButton.setTooltip(new Tooltip(Messages.MATERIAL_FILE_EDITOR_ACTION_SPHERE + " (S)"));
        sphereButton.setGraphic(new ImageView(Icons.SPHERE_16));
        sphereButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                changeModelType(ModelType.SPHERE, newValue));

        planeButton = new ToggleButton();
        planeButton.setTooltip(new Tooltip(Messages.MATERIAL_FILE_EDITOR_ACTION_PLANE + " (P)"));
        planeButton.setGraphic(new ImageView(Icons.PLANE_16));
        planeButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                changeModelType(ModelType.QUAD, newValue));

        lightButton = new ToggleButton();
        lightButton.setTooltip(new Tooltip(Messages.MATERIAL_FILE_EDITOR_ACTION_LIGHT + " (L)"));
        lightButton.setGraphic(new ImageView(Icons.LIGHT_16));
        lightButton.setSelected(DEFAULT_LIGHT_ENABLED);
        lightButton.selectedProperty().addListener((observable, oldValue, newValue) -> changeLight(newValue));

        final Label materialDefinitionLabel = new Label(Messages.MATERIAL_EDITOR_MATERIAL_TYPE_LABEL + ":");

        materialDefinitionBox = new ComboBox<>();
        materialDefinitionBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> changeType(newValue));

        final Label bucketLabel = new Label(Messages.MATERIAL_FILE_EDITOR_BUCKET_TYPE_LABEL + ":");

        bucketComboBox = new ComboBox<>(BUCKETS);
        bucketComboBox.getSelectionModel().select(Inherit);
        bucketComboBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> changeBucketType(newValue));

        FXUtils.addToPane(createSaveAction(), container);
        FXUtils.addToPane(cubeButton, container);
        FXUtils.addToPane(sphereButton, container);
        FXUtils.addToPane(planeButton, container);
        FXUtils.addToPane(lightButton, container);
        FXUtils.addToPane(materialDefinitionLabel, container);
        FXUtils.addToPane(materialDefinitionBox, container);
        FXUtils.addToPane(bucketLabel, container);
        FXUtils.addToPane(bucketComboBox, container);

        DynamicIconSupport.addSupport(cubeButton, sphereButton, planeButton, lightButton);

        FXUtils.addClassTo(materialDefinitionLabel, bucketLabel, CSSClasses.FILE_EDITOR_TOOLBAR_LABEL);
        FXUtils.addClassTo(materialDefinitionBox, bucketComboBox, CSSClasses.FILE_EDITOR_TOOLBAR_FIELD);
        FXUtils.addClassTo(cubeButton, sphereButton, planeButton, lightButton, CSSClasses.FILE_EDITOR_TOOLBAR_BUTTON);
    }

    /**
     * Handle changing the bucket type.
     */
    @FXThread
    private void changeBucketType(@NotNull final RenderQueue.Bucket newValue) {

        final MaterialEditor3DState editor3DState = getEditor3DState();
        editor3DState.changeBucketType(newValue);

        final EditorMaterialEditorState editorState = getEditorState();
        if (editorState != null) editorState.setBucketType(newValue);
    }

    /**
     * Handle changing the type.
     */
    @FXThread
    private void changeType(@Nullable final String newType) {
        if (isIgnoreListeners()) return;
        processChangeTypeImpl(newType);
    }

    /**
     * Handle changing the type.
     */
    @FXThread
    private void processChangeTypeImpl(@Nullable final String newType) {
        if (newType == null) return;

        final AssetManager assetManager = EDITOR.getAssetManager();
        final Material newMaterial = new Material(assetManager, newType);

        MaterialUtils.migrateTo(newMaterial, getCurrentMaterial());

        final EditorOperationControl operationControl = getOperationControl();
        operationControl.clear();

        incrementChange();
        reload(newMaterial);
    }

    /**
     * Handle changing the light enabling.
     */
    @FXThread
    private void changeLight(@NotNull final Boolean newValue) {

        final MaterialEditor3DState editor3DState = getEditor3DState();
        editor3DState.updateLightEnabled(newValue);

        final EditorMaterialEditorState editorState = getEditorState();
        if (editorState != null) editorState.setLightEnable(newValue);
    }

    /**
     * @return the button to use a cube.
     */
    @FromAnyThread
    private @NotNull ToggleButton getCubeButton() {
        return notNull(cubeButton);
    }

    /**
     * @return the button to use a plane.
     */
    @FromAnyThread
    private @NotNull ToggleButton getPlaneButton() {
        return notNull(planeButton);
    }

    /**
     * @return the button to use a sphere.
     */
    @FromAnyThread
    private @NotNull ToggleButton getSphereButton() {
        return notNull(sphereButton);
    }

    /**
     * @return the button to use a light.
     */
    @FromAnyThread
    private @NotNull ToggleButton getLightButton() {
        return notNull(lightButton);
    }

    /**
     * @return the list of RenderQueue.Bucket.
     */
    @FromAnyThread
    private @NotNull ComboBox<RenderQueue.Bucket> getBucketComboBox() {
        return notNull(bucketComboBox);
    }

    /**
     * Handle the changed model type.
     */
    @FXThread
    private void changeModelType(@NotNull final ModelType modelType, @NotNull final Boolean newValue) {
        if (newValue == Boolean.FALSE) return;

        final MaterialEditor3DState editor3DState = getEditor3DState();

        final ToggleButton cubeButton = getCubeButton();
        final ToggleButton sphereButton = getSphereButton();
        final ToggleButton planeButton = getPlaneButton();

        if (modelType == ModelType.BOX) {
            cubeButton.setMouseTransparent(true);
            sphereButton.setMouseTransparent(false);
            planeButton.setMouseTransparent(false);
            cubeButton.setSelected(true);
            sphereButton.setSelected(false);
            planeButton.setSelected(false);
            editor3DState.changeMode(modelType);
        } else if (modelType == ModelType.SPHERE) {
            cubeButton.setMouseTransparent(false);
            sphereButton.setMouseTransparent(true);
            planeButton.setMouseTransparent(false);
            cubeButton.setSelected(false);
            sphereButton.setSelected(true);
            planeButton.setSelected(false);
            editor3DState.changeMode(modelType);
        } else if (modelType == ModelType.QUAD) {
            cubeButton.setMouseTransparent(false);
            sphereButton.setMouseTransparent(false);
            planeButton.setMouseTransparent(true);
            sphereButton.setSelected(false);
            cubeButton.setSelected(false);
            planeButton.setSelected(true);
            editor3DState.changeMode(modelType);
        }

        final EditorMaterialEditorState editorState = getEditorState();
        if (editorState != null) editorState.setModelType(modelType);
    }

    @Override
    @FromAnyThread
    public @NotNull Material getCurrentMaterial() {
        return notNull(currentMaterial);
    }

    @Override
    @FXThread
    public void notifyFXChangeProperty(@NotNull final Object object, @NotNull final String propertyName) {
        if (object instanceof Material) {
            getPropertyEditor().refresh();
        } else {
            getPropertyEditor().syncFor(object);
        }
    }

    /**
     * @param currentMaterial the current editing material.
     */
    @FXThread
    private void setCurrentMaterial(@NotNull final Material currentMaterial) {
        this.currentMaterial = currentMaterial;
    }

    @Override
    @FromAnyThread
    public @NotNull EditorDescription getDescription() {
        return DESCRIPTION;
    }
}
