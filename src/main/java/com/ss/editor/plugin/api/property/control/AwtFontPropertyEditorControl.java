package com.ss.editor.plugin.api.property.control;

import static com.ss.rlib.common.util.ObjectUtils.notNull;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.manager.ExecutorManager;
import com.ss.editor.plugin.api.property.PropertyDefinition;
import com.ss.editor.ui.css.CssClasses;
import com.ss.editor.ui.util.AwtFontSuggestionProvider;
import com.ss.rlib.common.util.StringUtils;
import com.ss.rlib.common.util.VarTable;
import com.ss.rlib.fx.util.FxControlUtils;
import com.ss.rlib.fx.util.FxUtils;
import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;

/**
 * The control to choose string value from list.
 *
 * @author JavaSaBr
 */
public class AwtFontPropertyEditorControl extends PropertyEditorControl<Font> {

    private static final GraphicsEnvironment GRAPHICS_ENVIRONMENT = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private static final Font[] FONTS = GRAPHICS_ENVIRONMENT.getAllFonts();

    private static final StringConverter<Font> STRING_CONVERTER = new StringConverter<>() {

        @Override
        public @NotNull String toString(@Nullable Font font) {
            return font == null ? StringUtils.EMPTY : font.getFontName();
        }

        @Override
        public @Nullable Font fromString(@NotNull String fontName) {
            return Arrays.stream(FONTS)
                    .filter(font -> font.getFontName().equals(fontName))
                    .findAny().orElse(null);
        }
    };

    /**
     * The list of available options of the string value.
     */
    @Nullable
    private ComboBox<Font> comboBox;

    protected AwtFontPropertyEditorControl(
            @NotNull VarTable vars,
            @NotNull PropertyDefinition definition,
            @NotNull Runnable validationCallback
    ) {
        super(vars, definition, validationCallback);
    }

    @Override
    @FxThread
    protected void createComponents() {
        super.createComponents();

        comboBox = new ComboBox<>();
        comboBox.getItems().addAll(FONTS);
        comboBox.setVisibleRowCount(20);
        comboBox.setConverter(STRING_CONVERTER);
        comboBox.setEditable(true);
        comboBox.prefWidthProperty()
                .bind(widthProperty().multiply(DEFAULT_FIELD_W_PERCENT));

        var selectionModel = comboBox.getSelectionModel();

        var editor = comboBox.getEditor();
        var binding = new AutoCompletionTextFieldBinding<Font>(editor,
                new AwtFontSuggestionProvider(comboBox.getItems()), STRING_CONVERTER);
        binding.setOnAutoCompleted(event -> selectionModel.select(event.getCompletion()));
        binding.prefWidthProperty().bind(comboBox.widthProperty().multiply(1.3));

        FxControlUtils.onSelectedItemChange(comboBox, newValue -> {
            var executorManager = ExecutorManager.getInstance();
            executorManager.addFxTask(() -> editor.positionCaret(newValue.getFontName().length()));
        });

        FxControlUtils.onSelectedItemChange(comboBox, this::change);

        FxUtils.addClass(editor,
                        CssClasses.TRANSPARENT_TEXT_FIELD, CssClasses.TEXT_FIELD_IN_COMBO_BOX)
                .addClass(comboBox,
                        CssClasses.PROPERTY_CONTROL_COMBO_BOX);

        FxUtils.addChild(this, comboBox);
    }

    /**
     * @return The list of available options of the string value.
     */
    @FxThread
    private @NotNull ComboBox<Font> getComboBox() {
        return notNull(comboBox);
    }

    @Override
    @FxThread
    public void reload() {
        super.reload();
        var value = getPropertyValue();
        getComboBox().getSelectionModel()
                .select(value);
    }

    @Override
    @FxThread
    protected void changeImpl() {

        var selectionItem = getComboBox()
                .getSelectionModel()
                .getSelectedItem();

        setPropertyValue(selectionItem);

        super.changeImpl();
    }
}
