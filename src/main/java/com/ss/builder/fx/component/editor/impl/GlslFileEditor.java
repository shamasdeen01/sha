package com.ss.builder.ui.component.editor.impl;

import com.ss.builder.FileExtensions;
import com.ss.builder.Messages;
import com.ss.builder.annotation.FromAnyThread;
import com.ss.builder.annotation.FxThread;
import com.ss.editor.FileExtensions;
import com.ss.editor.Messages;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.ui.component.editor.EditorDescriptor;
import com.ss.editor.ui.control.code.BaseCodeArea;
import com.ss.editor.ui.control.code.GLSLCodeArea;
import org.jetbrains.annotations.NotNull;

/**
 * The implementation of editor to edit GLSL files.
 *
 * @author JavaSaBr
 */
public class GlslFileEditor extends CodeAreaFileEditor {

    public static final EditorDescriptor DESCRIPTOR = new EditorDescriptor(
            GlslFileEditor::new,
            Messages.GLSL_FILE_EDITOR_NAME,
            GlslFileEditor.class.getSimpleName(),
            FileExtensions.SHADER_EXTENSIONS
    );

    @Override
    @FxThread
    protected @NotNull BaseCodeArea createCodeArea() {
        return new GLSLCodeArea();
    }

    @Override
    @FromAnyThread
    public @NotNull EditorDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
}