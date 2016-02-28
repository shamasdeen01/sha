package com.ss.editor.ui.component.asset.tree;

import com.ss.editor.config.EditorConfig;
import com.ss.editor.manager.ExecutorManager;
import com.ss.editor.ui.component.asset.tree.context.menu.action.CopyFileAction;
import com.ss.editor.ui.component.asset.tree.context.menu.action.CutFileAction;
import com.ss.editor.ui.component.asset.tree.context.menu.action.DeleteFileAction;
import com.ss.editor.ui.component.asset.tree.context.menu.action.OpenFileAction;
import com.ss.editor.ui.component.asset.tree.context.menu.action.OpenWithFileAction;
import com.ss.editor.ui.component.asset.tree.context.menu.action.PasteFileAction;
import com.ss.editor.ui.component.asset.tree.resource.FileElement;
import com.ss.editor.ui.component.asset.tree.resource.ResourceElement;
import com.ss.editor.ui.component.asset.tree.resource.ResourceElementFactory;
import com.ss.editor.ui.component.asset.tree.resource.ResourceLoadingElement;
import com.ss.editor.ui.css.CSSClasses;
import com.ss.editor.ui.util.UIUtils;
import com.ss.editor.util.EditorUtil;

import java.nio.file.Path;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rlib.ui.util.FXUtils;
import rlib.util.StringUtils;
import rlib.util.array.Array;
import rlib.util.array.ArrayComparator;
import rlib.util.array.ArrayFactory;

import static com.ss.editor.ui.component.asset.tree.ResourceTreeCell.CELL_FACTORY;
import static com.ss.editor.ui.component.asset.tree.resource.ResourceElementFactory.createFor;
import static com.ss.editor.ui.css.CSSClasses.MAIN_FONT_13;
import static com.ss.editor.ui.util.UIUtils.findItemForValue;

/**
 * Реализация дерева ресурсов.
 *
 * @author Ronn
 */
public class ResourceTree extends TreeView<ResourceElement> {

    private static final ExecutorManager EXECUTOR_MANAGER = ExecutorManager.getInstance();

    private static final ArrayComparator<ResourceElement> COMPARATOR = ResourceElement::compareTo;
    private static final ArrayComparator<ResourceElement> NAME_COMPARATOR = (first, second) -> {

        final Path firstFile = first.getFile();
        final String firstName = firstFile.getFileName().toString();

        final Path secondFile = second.getFile();
        final String secondName = secondFile.getFileName().toString();

        return StringUtils.compareIgnoreCase(firstName, secondName);
    };

    private static final ArrayComparator<TreeItem<ResourceElement>> ITEM_COMPARATOR = (first, second) -> {

        final ResourceElement firstElement = first.getValue();
        final ResourceElement secondElement = second.getValue();

        return NAME_COMPARATOR.compare(firstElement, secondElement);
    };

    /**
     * Развернутые элементы.
     */
    private final Array<ResourceElement> expandedElements;

    /**
     * Выбранные элементы.
     */
    private final Array<ResourceElement> selectedElements;

    public ResourceTree() {
        this.expandedElements = ArrayFactory.newConcurrentAtomicArray(ResourceElement.class);
        this.selectedElements = ArrayFactory.newConcurrentAtomicArray(ResourceElement.class);

        FXUtils.addClassTo(this, CSSClasses.TRANSPARENT_TREE_VIEW);

        setCellFactory(CELL_FACTORY);
        setOnKeyPressed(this::processKey);
        setShowRoot(true);
    }

    /**
     * Обновление контекстного меню под указанный элемент.
     */
    public void updateContextMenu(final ResourceElement element) {

        final EditorConfig editorConfig = EditorConfig.getInstance();
        final Path currentAsset = editorConfig.getCurrentAsset();

        final ContextMenu contextMenu = new ContextMenu();
        final ObservableList<MenuItem> items = contextMenu.getItems();

        final Path file = element.getFile();

        if (element instanceof FileElement) {
            items.add(new OpenFileAction(element));
            items.add(new OpenWithFileAction(element));
            items.add(new CopyFileAction(element));
            items.add(new CutFileAction(element));
        }

        if (EditorUtil.hasFileInClipboard()) {
            items.add(new PasteFileAction(element));
        }

        if (!currentAsset.equals(file)) {
            items.add(new DeleteFileAction(element));
        }

        final Array<MenuItem> allItems = ArrayFactory.newArray(MenuItem.class);
        items.forEach(subItem -> UIUtils.getAllItems(allItems, subItem));
        allItems.forEach(menuItem -> FXUtils.addClassTo(menuItem, MAIN_FONT_13));

        setContextMenu(contextMenu);
    }

    /**
     * Заполнить дерево по новой папке асета.
     *
     * @param assetFolder новая папка ассета.
     */
    public void fill(final Path assetFolder) {

        final TreeItem<ResourceElement> currentRoot = getRoot();

        if (currentRoot != null) {
            setRoot(null);
        }

        showLoading();

        EXECUTOR_MANAGER.addBackgroundTask(() -> startBackgroundFill(assetFolder));
    }

    /**
     * @return развернутые элементы.
     */
    public Array<ResourceElement> getExpandedElements() {
        return expandedElements;
    }

    /**
     * @return выбранные элементы.
     */
    public Array<ResourceElement> getSelectedElements() {
        return selectedElements;
    }

    /**
     * Обновить дерево.
     */
    public void refresh() {

        final EditorConfig config = EditorConfig.getInstance();
        final Path currentAsset = config.getCurrentAsset();

        if (currentAsset == null) {
            setRoot(null);
            return;
        }

        updateSelectedElements();
        updateExpandedElements();

        setRoot(null);
        showLoading();

        EXECUTOR_MANAGER.addBackgroundTask(() -> startBackgroundRefresh(currentAsset));
    }

    /**
     * Обновление развернутых элементов.
     */
    private void updateExpandedElements() {

        final Array<ResourceElement> expandedElements = getExpandedElements();
        expandedElements.writeLock();
        try {

            expandedElements.clear();

            final Array<TreeItem<ResourceElement>> allItems = UIUtils.getAllItems(this);
            allItems.forEach(item -> {

                if (!item.isExpanded()) {
                    return;
                }

                expandedElements.add(item.getValue());
            });

        } finally {
            expandedElements.writeUnlock();
        }
    }

    /**
     * Обновление списка выбранных элементов.
     */
    private void updateSelectedElements() {

        final Array<ResourceElement> selectedElements = getSelectedElements();
        selectedElements.writeLock();
        try {

            selectedElements.clear();

            final MultipleSelectionModel<TreeItem<ResourceElement>> selectionModel = getSelectionModel();
            final ObservableList<TreeItem<ResourceElement>> selectedItems = selectionModel.getSelectedItems();
            selectedItems.forEach(item -> selectedElements.add(item.getValue()));

        } finally {
            selectedElements.writeUnlock();
        }
    }

    /**
     * Отобразить прогресс прогрузки.
     */
    private void showLoading() {
        setRoot(new TreeItem<>(ResourceLoadingElement.getInstance()));
    }

    /**
     * Запустить фоновое построение дерева.
     */
    private void startBackgroundFill(final Path assetFolder) {

        final ResourceElement rootElement = createFor(assetFolder);
        final TreeItem<ResourceElement> newRoot = new TreeItem<>(rootElement);
        newRoot.setExpanded(true);

        fill(newRoot);

        EXECUTOR_MANAGER.addFXTask(() -> setRoot(newRoot));
    }

    /**
     * Запустить фоновое обновление дерева.
     */
    private void startBackgroundRefresh(final Path assetFolder) {

        final ResourceElement rootElement = createFor(assetFolder);
        final TreeItem<ResourceElement> newRoot = new TreeItem<>(rootElement);
        newRoot.setExpanded(true);

        fill(newRoot);

        final Array<ResourceElement> expandedElements = getExpandedElements();
        expandedElements.writeLock();
        try {

            expandedElements.sort(COMPARATOR);
            expandedElements.forEach(element -> {

                final TreeItem<ResourceElement> item = findItemForValue(newRoot, element);

                if (item == null) {
                    return;
                }

                item.setExpanded(true);
            });

            expandedElements.clear();

        } finally {
            expandedElements.writeUnlock();
        }

        EXECUTOR_MANAGER.addFXTask(() -> {
            setRoot(newRoot);
            restoreSelection();
        });
    }

    /**
     * Восстановление выбранных элементов.
     */
    private void restoreSelection() {
        EXECUTOR_MANAGER.addFXTask(() -> {

            final Array<ResourceElement> selectedElements = getSelectedElements();
            selectedElements.writeLock();
            try {

                final MultipleSelectionModel<TreeItem<ResourceElement>> selectionModel = getSelectionModel();

                selectedElements.forEach(element -> {

                    final TreeItem<ResourceElement> item = findItemForValue(getRoot(), element);

                    if (item == null) {
                        return;
                    }

                    selectionModel.select(item);
                });

                selectedElements.clear();

            } finally {
                selectedElements.writeUnlock();
            }
        });
    }

    /**
     * Заполнить узел.
     */
    private void fill(final TreeItem<ResourceElement> treeItem) {

        final ResourceElement element = treeItem.getValue();

        if (!element.hasChildren()) {
            return;
        }

        final ObservableList<TreeItem<ResourceElement>> items = treeItem.getChildren();

        final Array<ResourceElement> children = element.getChildren();
        children.sort(NAME_COMPARATOR);
        children.forEach(child -> items.add(new TreeItem<>(child)));

        items.forEach(this::fill);
    }

    /**
     * Уведомление о созданном файле.
     *
     * @param file созданный файл.
     */
    public void notifyCreated(final Path file) {

        final EditorConfig editorConfig = EditorConfig.getInstance();
        final Path currentAsset = editorConfig.getCurrentAsset();
        final Path folder = file.getParent();

        if (!folder.startsWith(currentAsset)) {
            return;
        }

        final ResourceElement element = ResourceElementFactory.createFor(folder);

        TreeItem<ResourceElement> folderItem = UIUtils.findItemForValue(getRoot(), element);

        if (folderItem == null) {
            notifyCreated(folder);
            folderItem = UIUtils.findItemForValue(getRoot(), folder);
        }

        if (folderItem == null) {
            return;
        }

        final ObservableList<TreeItem<ResourceElement>> children = folderItem.getChildren();
        children.add(new TreeItem<>(ResourceElementFactory.createFor(file)));
        children.sorted(ITEM_COMPARATOR);
    }

    /**
     * Уведомление об удаленном файле.
     */
    public void notifyDeleted(final Path file) {

        final ResourceElement element = ResourceElementFactory.createFor(file);
        final TreeItem<ResourceElement> treeItem = UIUtils.findItemForValue(getRoot(), element);

        if (treeItem == null) {
            return;
        }

        final TreeItem<ResourceElement> parent = treeItem.getParent();

        if (parent == null) {
            return;
        }

        final ObservableList<TreeItem<ResourceElement>> children = parent.getChildren();
        children.remove(treeItem);
    }

    /**
     * Обработка нажатий на хоткеи.
     */
    private void processKey(final KeyEvent event) {

        final MultipleSelectionModel<TreeItem<ResourceElement>> selectionModel = getSelectionModel();
        final TreeItem<ResourceElement> selectedItem = selectionModel.getSelectedItem();

        if(selectedItem == null) {
            return;
        }

        final ResourceElement item = selectedItem.getValue();

        if(item == null || item instanceof ResourceLoadingElement || !event.isControlDown()) {
            return;
        }

        final KeyCode keyCode = event.getCode();

        if(keyCode == KeyCode.C && item instanceof FileElement) {

            final CopyFileAction action = new CopyFileAction(item);
            final EventHandler<ActionEvent> onAction = action.getOnAction();
            onAction.handle(null);

        } else if(keyCode == KeyCode.X && item instanceof FileElement) {

            final CutFileAction action = new CutFileAction(item);
            final EventHandler<ActionEvent> onAction = action.getOnAction();
            onAction.handle(null);

        } else if(keyCode == KeyCode.V && EditorUtil.hasFileInClipboard()) {

            final PasteFileAction action = new PasteFileAction(item);
            final EventHandler<ActionEvent> onAction = action.getOnAction();
            onAction.handle(null);
        }
    }
}