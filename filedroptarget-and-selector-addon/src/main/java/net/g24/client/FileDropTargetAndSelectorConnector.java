package net.g24.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.Util;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.extensions.FileDropTargetConnector;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.client.ui.AbstractLayoutConnector;
import com.vaadin.client.ui.LayoutClickEventHandler;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.LayoutClickRpc;
import elemental.events.Event;
import net.g24.FileDropTargetAndSelector;

@Connect(FileDropTargetAndSelector.class)
public class FileDropTargetAndSelectorConnector extends FileDropTargetConnector {

    private transient FileUpload fileUpload;
    private transient HandlerRegistration handlerRegistration = () -> {
    };

    @Override
    protected void extend(ServerConnector target) {
        super.extend(target);

        fileUpload = GWT.create(FileUpload.class);
        fileUpload.getElement().setAttribute("style", "display:none");
        addOnChangeEventHandler(fileUpload.getElement(), this);

        addFileUploadElementToDropTarget((AbstractComponentConnector) target);
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);
        if (stateChangeEvent.hasPropertyChanged("multiple")) {
            if (getState().multiple) {
                fileUpload.getElement().setAttribute("multiple", "multiple");
            } else {
                fileUpload.getElement().removeAttribute("multiple");
            }
        }
        if (stateChangeEvent.hasPropertyChanged("buttonRole")) {
            unregisterHandler();
            if (getState().buttonRole instanceof AbstractLayoutConnector) {
                registerClientSideLayoutClickListener((AbstractLayoutConnector) getState().buttonRole);
            } else if (getState().buttonRole instanceof ComponentConnector) {
                registerClientSideClickListener((AbstractComponentConnector) getState().buttonRole);
            }
        }
    }

    private void unregisterHandler() {
        handlerRegistration.removeHandler();
        handlerRegistration = () -> {
        };
    }

    @Override
    protected void onDrop(Event event) {
        super.onDrop(event);
    }

    @Override
    public FileDropTargetAndSelectorState getState() {
        return (FileDropTargetAndSelectorState) super.getState();
    }

    private void addFileUploadElementToDropTarget(AbstractComponentConnector target) {
        Widget widget = target.getWidget();
        widget.getElement().appendChild(fileUpload.getElement());
    }

    private void registerClientSideLayoutClickListener(AbstractLayoutConnector connector) {
        handlerRegistration = new OverridableLayoutClickEventHandler(connector, fileUpload).forceEventHandlerRegistration();
    }

    private void registerClientSideClickListener(ComponentConnector connector) {
        Widget widget = connector.getWidget();
        if (widget instanceof HasClickHandlers) {
            handlerRegistration = ((HasClickHandlers) widget).addClickHandler(event -> {
                if (connector.isEnabled()) {
                    fileUpload.click();
                }
            });
        }
    }

    private native void addOnChangeEventHandler(Element element, FileDropTargetAndSelectorConnector instance)
        /*-{
            element.onchange = function () {
                var nativeEvent = {"dataTransfer": {"files": element.files}};
                instance.@net.g24.client.FileDropTargetAndSelectorConnector::onDrop(Lelemental/events/Event;)(nativeEvent);

            };
        }-*/;

    private static class OverridableLayoutClickEventHandler extends LayoutClickEventHandler {

        private final FileUpload fileUpload;
        private boolean forceRegistration = true;

        public OverridableLayoutClickEventHandler(ComponentConnector connector, FileUpload fileUpload) {
            super(connector);
            this.fileUpload = fileUpload;
        }

        public HandlerRegistration forceEventHandlerRegistration() {
            forceRegistration = true;
            handleEventHandlerRegistration();
            return () -> {
                forceRegistration = false;
                handleEventHandlerRegistration();
            };
        }

        @Override
        public boolean hasEventListener() {
            return forceRegistration;
        }

        @Override
        protected ComponentConnector getChildComponent(com.google.gwt.user.client.Element element) {
            return Util.getConnectorForElement(connector.getConnection(), connector.getWidget(), element);
        }

        @Override
        protected void fireClick(NativeEvent event) {
            if (connector.isEnabled()) {
                fileUpload.click();
            }
        }

        @Override
        protected LayoutClickRpc getLayoutClickRPC() {
            return null;
        }
    }
}
