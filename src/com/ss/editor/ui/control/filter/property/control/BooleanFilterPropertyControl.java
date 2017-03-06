package com.ss.editor.ui.control.filter.property.control;

import static com.ss.editor.ui.control.filter.property.control.FilterPropertyControl.newChangeHandler;

import com.ss.editor.model.undo.editor.SceneChangeConsumer;
import com.ss.editor.ui.control.property.AbstractPropertyControl;
import com.ss.editor.ui.control.property.impl.AbstractBooleanPropertyControl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of the {@link AbstractPropertyControl} to edit boolean values.
 *
 * @author JavaSaBr
 */
public class BooleanFilterPropertyControl<T> extends AbstractBooleanPropertyControl<SceneChangeConsumer, T> {

    public BooleanFilterPropertyControl(@Nullable final Boolean propertyValue, @NotNull final String propertyName,
                                        @NotNull final SceneChangeConsumer changeConsumer) {
        super(propertyValue, propertyName, changeConsumer, newChangeHandler());
    }
}