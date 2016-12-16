package com.ss.editor.model.workspace;

import static rlib.util.ClassUtils.unsafeCast;

import com.ss.editor.manager.WorkspaceManager;
import com.ss.editor.ui.component.editor.EditorDescription;
import com.ss.editor.ui.component.editor.FileEditor;
import com.ss.editor.ui.component.editor.state.EditorState;
import com.ss.editor.util.EditorUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import rlib.logging.Logger;
import rlib.logging.LoggerManager;

/**
 * The workspace.
 *
 * @author JavaSaBr
 */
public class Workspace implements Serializable {

    public static final long serialVersionUID = 61;

    private static final Logger LOGGER = LoggerManager.getLogger(Workspace.class);

    /**
     * The changes counter.
     */
    private final AtomicInteger changes;

    /**
     * The asset folder of this workspace.
     */
    private transient Path assetFolder;

    /**
     * The table of opened files.
     */
    private volatile Map<String, String> openedFiles;

    /**
     * The table with states of editors.
     */
    private volatile Map<String, EditorState> editorStateMap;

    /**
     * The current edited file.
     */
    private String currentEditedFile;

    public Workspace() {
        this.changes = new AtomicInteger();
    }

    /**
     * Notify about finished restoring this workspace.
     */
    public void notifyRestored() {

        if (openedFiles == null) {
            openedFiles = new HashMap<>();
        }

        if (editorStateMap == null) {
            editorStateMap = new HashMap<>();
        }
    }

    /**
     * @return the current edited file.
     */
    @Nullable
    public String getCurrentEditedFile() {
        return currentEditedFile;
    }

    /**
     * Update the current edited file.
     *
     * @param file the current edited file.
     */
    public synchronized void updateCurrentEditedFile(@Nullable final Path file) {

        if (file == null) {
            this.currentEditedFile = null;
            return;
        }

        final Path assetFile = EditorUtil.getAssetFile(getAssetFolder(), file);
        this.currentEditedFile = EditorUtil.toAssetPath(assetFile);
    }

    /**
     * @return the table with states of editors.
     */
    @NotNull
    private Map<String, EditorState> getEditorStateMap() {
        return editorStateMap;
    }

    /**
     * Get the editor state for the file.
     *
     * @param file the edited file.
     * @return the state of the editor or null.
     */
    public synchronized <T extends EditorState> T getEditorState(@NotNull final Path file) {
        return getEditorState(file, null);
    }

    /**
     * Get the editor state for the file.
     *
     * @param file         the edited file.
     * @param stateFactory the state factory.
     * @return the state of the editor or null.
     */
    public synchronized <T extends EditorState> T getEditorState(@NotNull final Path file, @Nullable Supplier<EditorState> stateFactory) {

        final Path assetFile = EditorUtil.getAssetFile(getAssetFolder(), file);
        final String assetPath = EditorUtil.toAssetPath(assetFile);

        final Map<String, EditorState> editorStateMap = getEditorStateMap();

        if (stateFactory != null && !editorStateMap.containsKey(assetPath)) {
            editorStateMap.put(assetPath, stateFactory.get());
        }

        return unsafeCast(editorStateMap.get(assetPath));
    }

    /**
     * Update the editor state.
     */
    public synchronized void updateEditorState(@NotNull final Path file, @NotNull final EditorState editorState) {

        final Path assetFile = EditorUtil.getAssetFile(getAssetFolder(), file);
        final String assetPath = EditorUtil.toAssetPath(assetFile);

        final Map<String, EditorState> editorStateMap = getEditorStateMap();
        editorStateMap.put(assetPath, editorState);

        incrementChanges();
    }

    /**
     * Remove the editor state.
     */
    public synchronized void removeEditorState(@NotNull final Path file, @NotNull final EditorState editorState) {

        final Path assetFile = EditorUtil.getAssetFile(getAssetFolder(), file);
        final String assetPath = EditorUtil.toAssetPath(assetFile);

        final Map<String, EditorState> editorStateMap = getEditorStateMap();
        editorStateMap.remove(assetPath);

        incrementChanges();
    }

    /**
     * @param assetFolder the asset folder of this workspace.
     */
    public void setAssetFolder(@NotNull final Path assetFolder) {
        this.assetFolder = assetFolder;
    }

    /**
     * @return the table of opened files.
     */
    @NotNull
    public Map<String, String> getOpenedFiles() {
        return openedFiles;
    }

    /**
     * Add a new opened file.
     *
     * @param file       the opened file.
     * @param fileEditor the editor.
     */
    public synchronized void addOpenedFile(@NotNull final Path file, @NotNull final FileEditor fileEditor) {

        final Path assetFile = EditorUtil.getAssetFile(getAssetFolder(), file);
        final String assetPath = EditorUtil.toAssetPath(assetFile);

        final EditorDescription description = fileEditor.getDescription();

        final Map<String, String> openedFiles = getOpenedFiles();
        openedFiles.put(assetPath, description.getEditorId());

        incrementChanges();
    }

    /**
     * Remove the opened file.
     */
    public synchronized void removeOpenedFile(@NotNull final Path file) {

        final Path assetFile = EditorUtil.getAssetFile(getAssetFolder(), file);
        final String assetPath = EditorUtil.toAssetPath(assetFile);

        final Map<String, String> openedFiles = getOpenedFiles();
        openedFiles.remove(assetPath);

        incrementChanges();
    }

    /**
     * @return the asset folder of this workspace.
     */
    @NotNull
    public Path getAssetFolder() {
        return assetFolder;
    }

    /**
     * Increase the counter of changes.
     */
    private void incrementChanges() {
        changes.incrementAndGet();
    }

    /**
     * Clear this workspace.
     */
    public void clear() {
        getOpenedFiles().clear();
    }

    /**
     * Save this workspace.
     */
    public void save() {
        if (changes.get() == 0) return;

        final Path assetFolder = getAssetFolder();
        final Path workspaceFile = assetFolder.resolve(WorkspaceManager.FOLDER_EDITOR).resolve(WorkspaceManager.FILE_WORKSPACE);

        try {

            if (!Files.exists(workspaceFile)) {
                Files.createDirectories(workspaceFile.getParent());
                Files.createFile(workspaceFile);
            }

        } catch (final IOException e) {
            LOGGER.warning(e);
        }

        try {

            final Boolean hidden = (Boolean) Files.getAttribute(workspaceFile, "dos:hidden", LinkOption.NOFOLLOW_LINKS);

            if (hidden != null && !hidden) {
                Files.setAttribute(workspaceFile, "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
            }

        } catch (final IOException e) {
            LOGGER.warning(e);
        }

        changes.set(0);

        final byte[] serialize = EditorUtil.serialize(this);

        try (final SeekableByteChannel channel = Files.newByteChannel(workspaceFile, StandardOpenOption.WRITE)) {

            final ByteBuffer buffer = ByteBuffer.wrap(serialize);
            buffer.position(serialize.length);
            buffer.flip();

            channel.write(buffer);

        } catch (IOException e) {
            LOGGER.warning(e);
        }
    }
}
