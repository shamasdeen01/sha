package com.ss.editor.ui.control.property.impl;

import static com.ss.editor.FileExtensions.TEXTURE_EXTENSIONS;
import static com.ss.editor.config.DefaultSettingsProvider.Defaults.PREF_DEFAULT_FLIPPED_TEXTURES;
import static com.ss.editor.config.DefaultSettingsProvider.Preferences.PREF_FLIPPED_TEXTURES;
import static com.ss.editor.extension.property.EditablePropertyType.BOOLEAN;
import static com.ss.editor.extension.property.EditablePropertyType.ENUM;
import static com.ss.editor.util.EditorUtil.*;
import static com.ss.rlib.common.util.ObjectUtils.notNull;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.ss.editor.Messages;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.config.EditorConfig;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.plugin.api.dialog.GenericFactoryDialog;
import com.ss.editor.plugin.api.property.PropertyDefinition;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.control.property.PropertyControl;
import com.ss.editor.ui.css.CssClasses;
import com.ss.editor.ui.tooltip.ImageChannelPreview;
import com.ss.editor.ui.util.UiUtils;
import com.ss.editor.util.EditorUtil;
import com.ss.rlib.common.util.VarTable;
import com.ss.rlib.common.util.array.ArrayFactory;
import com.ss.rlib.fx.util.FxUtils;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The implementation of the {@link PropertyControl} to edit {@link com.jme3.texture.Texture2D} values.
 *
 * @param <C> the type of a change consumer.
 * @param <D> the type of an editing object.
 * @author JavaSaBr
 */
public class Texture2dPropertyControl<C extends ChangeConsumer, D> extends PropertyControl<C, D, Texture2D> {

    /**
     * The constant NO_TEXTURE.
     */
    @NotNull
    protected static final String NO_TEXTURE = Messages.MATERIAL_MODEL_PROPERTY_CONTROL_NO_TEXTURE;

    @NotNull
    protected static final Point DIALOG_SIZE = new Point(600, -1);

    @NotNull
    protected static final String PROP_FLIP = "flip";

    @NotNull
    protected static final String PROP_WRAP_MODE_S = "wrapModeS";

    @NotNull
    protected static final String PROP_WRAP_MODE_T = "wrapModeT";

    @NotNull
    protected static final String PROP_MAG_FILTER = "magFilter";

    @NotNull
    protected static final String PROP_MIN_FILTER = "minFilter";

    /**
     * The image channels preview.
     */
    @Nullable
    private ImageChannelPreview textureTooltip;

    /**
     * The image preview.
     */
    @Nullable
    private ImageView texturePreview;

    /**
     * The label for of path to a texture.
     */
    @Nullable
    private Label textureLabel;

    /**
     * The field container.
     */
    @Nullable
    private HBox fieldContainer;

    public Texture2dPropertyControl(
            @Nullable Texture2D propertyValue,
            @NotNull String propertyName,
            @NotNull C changeConsumer
    ) {
        super(propertyValue, propertyName, changeConsumer);
        setOnDragOver(this::handleDragOverEvent);
        setOnDragDropped(this::handleDragDroppedEvent);
    }

    @Override
    @FxThread
    public void changeControlWidthPercent(double controlWidthPercent) {
    }

    /**
     * Handle drag dropped events.
     *
     * @param dragEvent the drag dropped event.
     */
    @FxThread
    protected void handleDragDroppedEvent(@NotNull DragEvent dragEvent) {
        UiUtils.handleDroppedFile(dragEvent, TEXTURE_EXTENSIONS, this, Texture2dPropertyControl::changeTexture);
    }

    /**
     * Handle drag over events.
     *
     * @param dragEvent the drag over event.
     */
    @FxThread
    protected void handleDragOverEvent(@NotNull DragEvent dragEvent) {
        UiUtils.acceptIfHasFile(dragEvent, TEXTURE_EXTENSIONS);
    }

    @Override
    @FxThread
    protected void createComponents(@NotNull HBox container) {
        super.createComponents(container);

        fieldContainer = new HBox();

        if (!isSingleRow()) {
            fieldContainer.prefWidthProperty()
                    .bind(container.widthProperty());
        }

        textureTooltip = new ImageChannelPreview();

        var previewContainer = new VBox();

        texturePreview = new ImageView();
        texturePreview.fitHeightProperty()
                .bind(previewContainer.heightProperty());
        texturePreview.fitWidthProperty()
                .bind(previewContainer.widthProperty());

        Tooltip.install(texturePreview, textureTooltip);

        var settingsButton = new Button();
        settingsButton.setGraphic(new ImageView(Icons.SETTINGS_16));
        settingsButton.setOnAction(event -> openSettings());
        settingsButton.disableProperty().bind(buildDisableRemoveCondition());

        var addButton = new Button();
        addButton.setGraphic(new ImageView(Icons.ADD_12));
        addButton.setOnAction(event -> addNewTexture());

        var removeButton = new Button();
        removeButton.setGraphic(new ImageView(Icons.REMOVE_12));
        removeButton.setOnAction(event -> removeTexture());
        removeButton.disableProperty().bind(buildDisableRemoveCondition());

        if (!isSingleRow()) {

            textureLabel = new Label(NO_TEXTURE);
            textureLabel.prefWidthProperty()
                    .bind(widthProperty()
                        .subtract(removeButton.widthProperty())
                        .subtract(previewContainer.widthProperty())
                        .subtract(settingsButton.widthProperty())
                        .subtract(addButton.widthProperty()));

            FxUtils.addClass(textureLabel,
                            CssClasses.ABSTRACT_PARAM_CONTROL_ELEMENT_LABEL)
                    .addClass(fieldContainer,
                            CssClasses.TEXT_INPUT_CONTAINER,
                            CssClasses.ABSTRACT_PARAM_CONTROL_INPUT_CONTAINER);

            FxUtils.addChild(fieldContainer, textureLabel);

        } else {
            FxUtils.addClass(fieldContainer,
                    CssClasses.TEXT_INPUT_CONTAINER_WITHOUT_PADDING);
        }

        FxUtils.addClass(previewContainer,
                        CssClasses.ABSTRACT_PARAM_CONTROL_PREVIEW_CONTAINER)
                .addClass(settingsButton, addButton, removeButton,
                        CssClasses.FLAT_BUTTON,
                        CssClasses.INPUT_CONTROL_TOOLBAR_BUTTON);

        FxUtils.addChild(fieldContainer, previewContainer, addButton, settingsButton, removeButton)
                .addChild(container, fieldContainer)
                .addChild(previewContainer, texturePreview);
    }

    /**
     * Get the disable|remove condition.
     *
     * @return the disable|remove condition.
     */
    @FxThread
    protected @NotNull BooleanBinding buildDisableRemoveCondition() {
        return getTexturePreview().imageProperty().isNull();
    }

    /**
     * Get the texture label.
     *
     * @return the texture label.
     */
    @FxThread
    private @NotNull Label getTextureLabel() {
        return notNull(textureLabel);
    }

    /**
     * Get the field container.
     *
     * @return the field container.
     */
    @FxThread
    protected @NotNull HBox getFieldContainer() {
        return notNull(fieldContainer);
    }

    /**
     * Get the texture preview.
     *
     * @return the texture preview.
     */
    @FxThread
    private @NotNull ImageView getTexturePreview() {
        return notNull(texturePreview);
    }

    /**
     * Get the image channels preview.
     *
     * @return the image channels preview.
     */
    @FxThread
    private @NotNull ImageChannelPreview getTextureTooltip() {
        return notNull(textureTooltip);
    }

    /**
     * Remove the current texture.
     */
    @FxThread
    protected void removeTexture() {
        changeTexture(null);
    }

    /**
     * Open the dialog to choose a new texture.
     */
    @FxThread
    protected void addNewTexture() {
        UiUtils.openFileAssetDialog(this::changeTexture, TEXTURE_EXTENSIONS, DEFAULT_ACTION_TESTER);
    }

    /**
     * Open a dialog with texture's settings.
     */
    @FxThread
    protected void openSettings() {

        var texture = notNull(getPropertyValue());
        var key = (TextureKey) texture.getKey();
        var flipY = key.isFlipY();
        var wrapS = texture.getWrap(Texture.WrapAxis.S);
        var wrapT = texture.getWrap(Texture.WrapAxis.T);
        var magFilter = texture.getMagFilter();
        var minFilter = texture.getMinFilter();

        var properties = ArrayFactory.<PropertyDefinition>newArray(PropertyDefinition.class);
        properties.add(new PropertyDefinition(BOOLEAN, Messages.MATERIAL_MODEL_PROPERTY_CONTROL_FLIP_Y, PROP_FLIP, flipY));
        properties.add(new PropertyDefinition(ENUM, Messages.MATERIAL_MODEL_PROPERTY_CONTROL_WRAP_MODE_S, PROP_WRAP_MODE_S, wrapS));
        properties.add(new PropertyDefinition(ENUM, Messages.MATERIAL_MODEL_PROPERTY_CONTROL_WRAP_MODE_T, PROP_WRAP_MODE_T, wrapT));
        properties.add(new PropertyDefinition(ENUM, Messages.MATERIAL_MODEL_PROPERTY_CONTROL_MAG_FILTER, PROP_MAG_FILTER, magFilter));
        properties.add(new PropertyDefinition(ENUM, Messages.MATERIAL_MODEL_PROPERTY_CONTROL_MIN_FILTER, PROP_MIN_FILTER, minFilter));

        var dialog = new GenericFactoryDialog(properties, this::applyChanges);
        dialog.setTitle(Messages.MATERIAL_MODEL_PROPERTY_CONTROL_TEXTURE_SETTINGS);
        dialog.setButtonOkText(Messages.SIMPLE_DIALOG_BUTTON_APPLY);
        dialog.setButtonCloseText(Messages.SIMPLE_DIALOG_BUTTON_CANCEL);
        dialog.configureSize(DIALOG_SIZE);
        dialog.show();
    }

    /**
     * Apply new changes if need.
     *
     * @param vars the vars table.
     */
    @FxThread
    private void applyChanges(@NotNull VarTable vars) {

        var texture = notNull(getPropertyValue());
        var key = (TextureKey) texture.getKey();
        var flipY = key.isFlipY();
        var wrapS = texture.getWrap(Texture.WrapAxis.S);
        var wrapT = texture.getWrap(Texture.WrapAxis.T);
        var magFilter = texture.getMagFilter();
        var minFilter = texture.getMinFilter();

        var needFlipY = vars.getBoolean(PROP_FLIP);
        var needWrapS = vars.getEnum(PROP_WRAP_MODE_S, Texture.WrapMode.class);
        var needWrapT = vars.getEnum(PROP_WRAP_MODE_T, Texture.WrapMode.class);
        var needMagFilter = vars.getEnum(PROP_MAG_FILTER, Texture.MagFilter.class);
        var needMinFilter = vars.getEnum(PROP_MIN_FILTER, Texture.MinFilter.class);

        if (flipY == needFlipY &&
                wrapS == needWrapS &&
                wrapT == needWrapT &&
                magFilter == needMagFilter &&
                minFilter == needMinFilter) {
            return;
        }

        var newKey = new TextureKey(key.getName());
        newKey.setFlipY(needFlipY);

        var assetManager = EditorUtil.getAssetManager();
        assetManager.deleteFromCache(key);

        var loadedTexture = (Texture2D) assetManager.loadTexture(newKey);
        loadedTexture.setWrap(Texture.WrapAxis.S, needWrapS);
        loadedTexture.setWrap(Texture.WrapAxis.T, needWrapT);
        loadedTexture.setMagFilter(needMagFilter);
        loadedTexture.setMinFilter(needMinFilter);

        changed(loadedTexture, texture);
    }

    /**
     * Change a texture to the file.
     *
     * @param file the file of a new texture.
     */
    @FxThread
    protected void changeTexture(@Nullable Path file) {

        if (file == null) {
            changed(null, getPropertyValue());
        } else {

            var config = EditorConfig.getInstance();
            var assetFile = notNull(getAssetFile(file));
            var textureKey = new TextureKey(toAssetPath(assetFile));
            textureKey.setFlipY(config.getBoolean(PREF_FLIPPED_TEXTURES, PREF_DEFAULT_FLIPPED_TEXTURES));

            var texture = (Texture2D) EditorUtil.getAssetManager()
                    .loadTexture(textureKey);
            texture.setWrap(Texture.WrapMode.Repeat);

            changed(texture, getPropertyValue());
        }
    }

    @Override
    @FxThread
    protected void reload() {

        var texture2D = getPropertyValue();
        var key = texture2D == null ? null : texture2D.getKey();

        if (!isSingleRow()) {
            getTextureLabel().setText(key == null ? NO_TEXTURE : key.getName());
        }

        var textureTooltip = getTextureTooltip();
        var preview = getTexturePreview();

        if (key == null) {
            preview.setImage(null);
            textureTooltip.clean();
            preview.setDisable(true);
            preview.setMouseTransparent(true);
        } else {

            preview.setDisable(false);
            preview.setMouseTransparent(false);

            var realFile = notNull(getRealFile(key.getName()));

            if (Files.exists(realFile)) {
                preview.setImage(IMAGE_MANAGER.getImagePreview(realFile, 24, 24));
                textureTooltip.showImage(realFile);
            } else {
                preview.setImage(IMAGE_MANAGER.getImagePreview(key.getName(), 24, 24));
                textureTooltip.showImage(key.getName());
            }
        }
    }
}
