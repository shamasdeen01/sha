package com.ss.editor.state.editor.impl.audio;

import static com.ss.rlib.util.ObjectUtils.notNull;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource.Status;
import com.jme3.scene.Node;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.JmeThread;
import com.ss.editor.state.editor.impl.AbstractEditor3DState;
import com.ss.editor.ui.component.editor.impl.AudioViewerEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of an editor app state for the {@link AudioViewerEditor}.
 *
 * @author JavaSaBr
 */
public class AudioViewer3DState extends AbstractEditor3DState<AudioViewerEditor> {

    /**
     * The previous status.
     */
    @Nullable
    private volatile Status prevStatus;

    /**
     * The audio node.
     */
    @Nullable
    private AudioNode audioNode;

    /**
     * The audio data.
     */
    @Nullable
    private AudioData audioData;

    /**
     * The audio key.
     */
    @Nullable
    private AudioKey audioKey;

    /**
     * Create a file editor app state.
     *
     * @param fileEditor the editor.
     */
    public AudioViewer3DState(@NotNull final AudioViewerEditor fileEditor) {
        super(fileEditor);
    }

    /**
     * Load the audio data.
     *
     * @param audioData the audio data.
     * @param audioKey  the audio key.
     */
    @FromAnyThread
    public void load(@NotNull final AudioData audioData, @NotNull final AudioKey audioKey) {
        EXECUTOR_MANAGER.addJMETask(() -> loadImpl(audioData, audioKey));
    }

    /**
     * Load the audio data.
     *
     * @param audioData the audio data.
     * @param audioKey  the audio key.
     */
    @JmeThread
    private void loadImpl(@NotNull final AudioData audioData, @NotNull final AudioKey audioKey) {
        removeAudioNode();
        setAudioData(audioData);
        setAudioKey(audioKey);

        final Node stateNode = getStateNode();

        final AudioNode audioNode = new AudioNode(audioData, audioKey);
        audioNode.setPositional(false);
        stateNode.attachChild(audioNode);

        setAudioNode(audioNode);
    }

    /**
     * Remove the current audio node.
     */
    @JmeThread
    private void removeAudioNode() {

        final AudioNode currentAudioNode = getAudioNode();
        if (currentAudioNode == null) return;

        final Node stateNode = getStateNode();
        final Status status = currentAudioNode.getStatus();

        if (status == Status.Playing || status == Status.Paused) {
            currentAudioNode.stop();
        }

        stateNode.detachChild(currentAudioNode);

        setAudioNode(null);
        setPrevStatus(null);
    }

    /**
     * Play the current audio.
     */
    @FromAnyThread
    public void play() {
        EXECUTOR_MANAGER.addJMETask(this::playImpl);
    }

    /**
     * Play the current audio.
     */
    @JmeThread
    private void playImpl() {

        final AudioNode currentAudioNode = getAudioNode();

        if (currentAudioNode != null) {

            final Status status = currentAudioNode.getStatus();

            if (status == Status.Paused) {
                currentAudioNode.play();
                return;
            } else if (status == Status.Playing) {
                return;
            }
        }

        loadImpl(getAudioData(), getAudioKey());

        final AudioNode audioNode = getAudioNode();
        audioNode.play();
    }

    @Override
    @JmeThread
    public void cleanup() {
        stopImpl();
        super.cleanup();
    }

    /**
     * Pause the current audio.
     */
    @FromAnyThread
    public void pause() {
        EXECUTOR_MANAGER.addJMETask(this::pauseImpl);
    }

    /**
     * Pause the current audio.
     */
    @JmeThread
    private void pauseImpl() {
        final AudioNode currentAudioNode = getAudioNode();
        if (currentAudioNode == null) return;
        currentAudioNode.pause();
    }

    /**
     * Stop the current audio.
     */
    @FromAnyThread
    public void stop() {
        EXECUTOR_MANAGER.addJMETask(this::stopImpl);
    }

    /**
     * Stop the current audio.
     */
    @JmeThread
    private void stopImpl() {
        removeAudioNode();
        EXECUTOR_MANAGER.addFXTask(() -> getFileEditor().notifyChangedStatus(Status.Stopped));
    }

    @Override
    @JmeThread
    public void update(final float tpf) {
        super.update(tpf);

        final AudioNode audioNode = getAudioNode();
        if (audioNode == null) return;

        final Status status = audioNode.getStatus();
        if (status != getPrevStatus()) {
            EXECUTOR_MANAGER.addFXTask(() -> getFileEditor().notifyChangedStatus(status));
            setPrevStatus(status);
        }
    }

    /**
     * @return the audio node.
     */
    @JmeThread
    private @Nullable AudioNode getAudioNode() {
        return audioNode;
    }

    /**
     * @param audioNode the audio node.
     */
    @JmeThread
    private void setAudioNode(@Nullable final AudioNode audioNode) {
        this.audioNode = audioNode;
    }

    /**
     * @return the audio data.
     */
    @JmeThread
    private @NotNull AudioData getAudioData() {
        return notNull(audioData);
    }

    /**
     * @param audioData the audio data.
     */
    @JmeThread
    private void setAudioData(@NotNull final AudioData audioData) {
        this.audioData = audioData;
    }

    /**
     * @return the audio key.
     */
    @JmeThread
    private @NotNull AudioKey getAudioKey() {
        return notNull(audioKey);
    }

    /**
     * @param audioKey the audio key.
     */
    @JmeThread
    private void setAudioKey(@NotNull final AudioKey audioKey) {
        this.audioKey = audioKey;
    }

    /**
     * Gets prev status.
     *
     * @return the previous status.
     */
    @FromAnyThread
    public @Nullable Status getPrevStatus() {
        return prevStatus;
    }

    /**
     * @param prevStatus the previous status.
     */
    @JmeThread
    private void setPrevStatus(@Nullable final Status prevStatus) {
        this.prevStatus = prevStatus;
    }

    @Override
    public String toString() {
        return "AudioViewer3DState{" +
                "prevStatus=" + prevStatus +
                ", audioNode=" + audioNode +
                ", audioData=" + audioData +
                ", audioKey=" + audioKey +
                "} " + super.toString();
    }
}
