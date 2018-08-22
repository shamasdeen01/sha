package com.ss.builder.editor.part3d.control.impl;

import static com.ss.editor.editor.part3d.EditableSceneEditor3dPart.PROP_IS_EDITING;
import static com.ss.editor.editor.part3d.control.impl.InputStateEditor3dPartControl.PROP_IS_CONTROL_DOWN;
import static com.ss.editor.editor.part3d.impl.scene.AbstractSceneEditor3dPart.KEY_IGNORE_RAY_CAST;
import com.jme3.collision.CollisionResult;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.ss.builder.annotation.FromAnyThread;
import com.ss.builder.annotation.JmeThread;
import com.ss.builder.jme.control.painting.PaintingInput;
import com.ss.builder.editor.part3d.EditableSceneEditor3dPart;
import com.ss.builder.editor.part3d.ExtendableEditor3dPart;
import com.ss.builder.editor.part3d.event.impl.FinishEditingEvent;
import com.ss.builder.editor.part3d.event.impl.StartEditingEvent;
import com.ss.builder.editor.part3d.impl.scene.AbstractSceneEditor3dPart;
import com.ss.builder.manager.ExecutorManager;
import com.ss.builder.util.*;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.JmeThread;
import com.ss.editor.control.painting.PaintingInput;
import com.ss.editor.manager.ExecutorManager;
import com.ss.editor.editor.part3d.EditableSceneEditor3dPart;
import com.ss.editor.editor.part3d.ExtendableEditor3dPart;
import com.ss.editor.editor.part3d.event.impl.FinishEditingEvent;
import com.ss.editor.editor.part3d.event.impl.StartEditingEvent;
import com.ss.editor.util.*;
import com.ss.rlib.common.util.dictionary.ObjectDictionary;
import javafx.scene.input.MouseButton;
import org.jetbrains.annotations.NotNull;

/**
 * The control to implement painting support on the scene editor 3d part.
 *
 * @author JavaSaBr
 */
public class PaintingSupportEditor3dPartControl<T extends EditableSceneEditor3dPart & ExtendableEditor3dPart>
        extends BaseInputEditor3dPartControl<T> {

    private static final ObjectDictionary<String, Trigger> TRIGGERS =
            ObjectDictionary.ofType(String.class, Trigger.class);

    private static final String MOUSE_RIGHT_CLICK = "jMB.paintingSupportEditor.mouseRightClick";
    private static final String MOUSE_LEFT_CLICK = "jMB.paintingSupportEditor.mouseLeftClick";

    private static final String[] MAPPINGS;

    static {

        TRIGGERS.put(MOUSE_RIGHT_CLICK, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        TRIGGERS.put(MOUSE_LEFT_CLICK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        MAPPINGS = TRIGGERS.keyArray(String.class)
                .toArray(String.class);
    }

    /**
     * The cursor node.
     */
    @NotNull
    private final Node cursorNode;

    /**
     * The markers node.
     */
    @NotNull
    private final Node markersNode;

    /**
     * The flag of painting mode.
     */
    private boolean paintingMode;

    public PaintingSupportEditor3dPartControl(@NotNull T editor3dPart) {
        super(editor3dPart);

        this.cursorNode = new Node("Cursor node");
        this.markersNode = new Node("Markers node");

        actionHandlers.put(MOUSE_LEFT_CLICK, (isPressed, tpf) -> {
            if (paintingMode) {
                if (isPressed) {
                    startPainting(getPaintingInput(MouseButton.PRIMARY));
                } else {
                    finishPainting(getPaintingInput(MouseButton.PRIMARY));
                }
            }
        });
        actionHandlers.put(MOUSE_RIGHT_CLICK, (isPressed, tpf) -> {
            if (paintingMode) {
                if (isPressed) {
                    startPainting(getPaintingInput(MouseButton.SECONDARY));
                } else {
                    finishPainting(getPaintingInput(MouseButton.SECONDARY));
                }
            }
        });
    }

    @Override
    @JmeThread
    public void register(@NotNull InputManager inputManager) {
        TRIGGERS.forEach(inputManager, JmeUtils::addMapping);
        inputManager.addListener(getActionListener(), MAPPINGS);
    }

    /**
     * Start painting.
     */
    @JmeThread
    private void startPainting(@NotNull PaintingInput input) {

        var control = PaintingUtils.getPaintingControl(cursorNode);
        var paintedModel = PaintingUtils.getPaintedModel(cursorNode);

        if (control == null || paintedModel == null || control.isStartedPainting()) {
            return;
        }

        control.startPainting(input, cursorNode.getLocalRotation(), cursorNode.getLocalTranslation());
    }

    /**
     * Finish painting.
     */
    @JmeThread
    private void finishPainting(@NotNull PaintingInput input) {

        var control = PaintingUtils.getPaintingControl(cursorNode);
        var paintedModel = PaintingUtils.getPaintedModel(control);

        if (control == null || paintedModel == null) {
            return;
        } else if (!control.isStartedPainting() || control.getCurrentInput() != input) {
            return;
        }

        control.finishPainting(cursorNode.getLocalRotation(), cursorNode.getLocalTranslation());
    }

    @Override
    @JmeThread
    public void postCameraUpdate(float tpf) {
        if (paintingMode) {
            updatePaintingNodes();
            updatePainting(tpf);
        }
    }

    /**
     * Update painting.
     */
    @JmeThread
    private void updatePainting(float tpf) {

        var control = PaintingUtils.getPaintingControl(cursorNode);
        var model = PaintingUtils.getPaintedModel(control);

        if (control == null || model == null || !control.isStartedPainting()) {
            return;
        }

        control.updatePainting(cursorNode.getLocalRotation(), cursorNode.getLocalTranslation(), tpf);
    }

    /**
     * Update editing nodes.
     */
    @JmeThread
    private void updatePaintingNodes() {

        if (!paintingMode) {
            return;
        }

        var control = PaintingUtils.getPaintingControl(cursorNode);
        var paintedModel = PaintingUtils.getPaintedModel(control);

        if (paintedModel == null) {
            return;
        }

        var collisions = GeomUtils.getCollisionsFromCursor(paintedModel, editor3dPart.getCamera());

        if (collisions.size() < 1) {
            return;
        }

        CollisionResult result = null;

        for (var collision : collisions) {

            var geometry = collision.getGeometry();
            var parent = NodeUtils.findParent(geometry, spatial ->
                    spatial.getUserData(AbstractSceneEditor3dPart.KEY_IGNORE_RAY_CAST) == Boolean.TRUE);

            if (parent == null) {
                result = collision;
                break;
            }
        }

        if (result == null) {
            result = collisions.getClosestCollision();
        }

        var contactPoint = result.getContactPoint();
        var contactNormal = result.getContactNormal();

        var local = LocalObjects.get();
        var rotation = local.nextRotation();
        rotation.lookAt(contactNormal, Vector3f.UNIT_Y);

        cursorNode.setLocalRotation(rotation);
        cursorNode.setLocalTranslation(contactPoint);
    }

    /**
     * Get the painting input.
     *
     * @param mouseButton the mouse button.
     * @return the painting input.
     */
    @FromAnyThread
    protected @NotNull PaintingInput getPaintingInput(@NotNull MouseButton mouseButton) {

        switch (mouseButton) {
            case SECONDARY: {

                if (editor3dPart.getBooleanProperty(PROP_IS_CONTROL_DOWN)) {
                    return PaintingInput.MOUSE_SECONDARY_WITH_CTRL;
                }

                return PaintingInput.MOUSE_SECONDARY;
            }
            case PRIMARY: {
                return PaintingInput.MOUSE_PRIMARY;
            }
        }

        return PaintingInput.MOUSE_PRIMARY;
    }

    /**
     * Change enabling of painting mode.
     *
     * @param paintingMode true if painting mode is enabled.
     */
    @FromAnyThread
    public void changePaintingMode(boolean paintingMode) {
        ExecutorManager.getInstance()
                .addJmeTask(() -> changePaintingModeInJme(paintingMode));
    }

    /**
     * Change enabling of painting mode in jME thread.
     *
     * @param paintingMode true if painting mode is enabled.
     */
    @JmeThread
    private void changePaintingModeInJme(boolean paintingMode) {
        this.paintingMode = paintingMode;

        var toolNode = editor3dPart.getToolNode();

        if (paintingMode) {

            if (editor3dPart.getBooleanProperty(EditableSceneEditor3dPart.PROP_IS_EDITING)) {
                return;
            }

            toolNode.attachChild(cursorNode);
            toolNode.attachChild(markersNode);

            editor3dPart.notify(new StartEditingEvent(this));

        } else {

            toolNode.detachChild(cursorNode);
            toolNode.detachChild(markersNode);

            editor3dPart.notify(new FinishEditingEvent(this));
        }
    }

    @Override
    @JmeThread
    public boolean hasProperty(@NotNull String propertyId) {
        return EditableSceneEditor3dPart.PROP_IS_EDITING.equals(propertyId);
    }

    @Override
    @JmeThread
    public boolean getBooleanProperty(@NotNull String propertyId) {
        return EditableSceneEditor3dPart.PROP_IS_EDITING.equals(propertyId) && paintingMode;
    }
}