package net.g24;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.dnd.FileDropHandler;
import com.vaadin.ui.dnd.FileDropTarget;

import net.g24.client.FileDropTargetAndSelectorState;

/**
 * Extension to add drop target functionality to a widget for accepting and uploading files, as well as select files through client's native file
 * selector triggered by a click event on a passed {@link Component}.
 * <p>
 * Single and multi upload is supported (also for file selector). Validation of file count in connection with single or mode must be done server-side.
 * <p>
 * The {@link Component} for buttonRole might be a clickable component, e.g. {@link com.vaadin.ui.Label}, or a clickable {@link com.vaadin.ui.Layout},
 * e.g. {@link com.vaadin.ui.CssLayout}.
 * <p>
 * There is no difference in handling receiving the selected or dropped files on the server-side.
 * <p>
 * See also {@link FileDropTarget}
 *
 * @param <T> Type of the component to be extended for drop capabilities
 */
public class FileDropTargetAndSelector<T extends AbstractComponent> extends FileDropTarget<T> {

    public FileDropTargetAndSelector(T target, Component buttonRole, FileDropHandler<T> fileDropHandler) {
        super(target, fileDropHandler);
        getState().buttonRole = buttonRole;
    }

    @Override
    public FileDropTargetAndSelectorState getState() {
        return (FileDropTargetAndSelectorState) super.getState();
    }

    public void setMultiple(boolean multiple) {
        getState().multiple = multiple;
    }
}
