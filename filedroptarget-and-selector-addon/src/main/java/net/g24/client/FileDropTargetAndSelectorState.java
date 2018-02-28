package net.g24.client;

import com.vaadin.shared.Connector;
import com.vaadin.shared.ui.dnd.FileDropTargetState;

public class FileDropTargetAndSelectorState extends FileDropTargetState {
    public boolean multiple;
    public Connector buttonRole;
}
