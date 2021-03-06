package net.g24.client;

import com.google.gwt.core.client.Scheduler;
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

    private transient HandlerRegistration handlerRegistration = null;
    private transient AbstractComponentConnector target;
    private transient FileUpload fileUpload;

    @Override
    protected void extend(ServerConnector target) {
        this.target = (AbstractComponentConnector) target;
        super.extend(target);
        // add (default) file chooser for autom. test purposes
        appendFileUpload();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);
        if (stateChangeEvent.hasPropertyChanged("multiple")) {
            setMultiple();
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

    private void setMultiple() {
        if (getState().multiple) {
            fileUpload.getElement().setAttribute("multiple", "multiple");
        } else {
            fileUpload.getElement().removeAttribute("multiple");
        }
    }

    private void unregisterHandler() {
        if (handlerRegistration != null) {
            handlerRegistration.removeHandler();
            handlerRegistration = null;
        }
    }

    @Override
    protected void onDrop(Event event) {
        super.onDrop(event);
        appendFileUpload();
        Scheduler.get().scheduleDeferred(() -> {
            getConnection().getServerRpcQueue().flush();
        });
    }


    @Override
    public FileDropTargetAndSelectorState getState() {
        return (FileDropTargetAndSelectorState) super.getState();
    }

    private void registerClientSideLayoutClickListener(AbstractLayoutConnector connector) {
        handlerRegistration = new OverridableLayoutClickEventHandler(connector, this).forceEventHandlerRegistration();
    }

    private void registerClientSideClickListener(ComponentConnector connector) {
        Widget widget = connector.getWidget();
        if (widget instanceof HasClickHandlers) {
            handlerRegistration = ((HasClickHandlers) widget).addClickHandler(event -> {
                if (connector.isEnabled()) {
                    openFileChooser();
                }
            });
        }
    }

    private static class OverridableLayoutClickEventHandler extends LayoutClickEventHandler {

        private final FileDropTargetAndSelectorConnector selector;
        private boolean forceRegistration = true;

        public OverridableLayoutClickEventHandler(ComponentConnector connector, FileDropTargetAndSelectorConnector selector) {
            super(connector);
            this.selector = selector;
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
                selector.openFileChooser();
            }
        }

        @Override
        protected LayoutClickRpc getLayoutClickRPC() {
            return null;
        }
    }

    private void openFileChooser() {
        appendFileUpload();
        fileUpload.click();
    }

    private void appendFileUpload() {
        if (fileUpload != null) {
            // remove old file input element (reset won't work) - needed in case same file must be uploaded twice
            // intentionally use js native functions instead of GWT's #removeFromParent because parent got lost in some cases
            removeElement(fileUpload.getElement());
        }
        fileUpload = GWT.create(FileUpload.class);
        fileUpload.getElement().setAttribute("style", "display:none");
        setMultiple();
        addOnChangeEventHandler(fileUpload.getElement(), this);

        target.getWidget().getElement().appendChild(fileUpload.getElement());
    }

    private native void addOnChangeEventHandler(Element element, FileDropTargetAndSelectorConnector instance)
        /*-{
            element.onchange = function () {
                var nativeEvent = {
                    "dataTransfer": {"files": element.files},
                    "preventDefault": function () {
                    },
                    "stopPropagation": function () {
                    }
                };
                instance.@net.g24.client.FileDropTargetAndSelectorConnector::onDragEnter(Lelemental/events/Event;)(nativeEvent);
                instance.@net.g24.client.FileDropTargetAndSelectorConnector::onDrop(Lelemental/events/Event;)(nativeEvent);
            };
        }-*/;

    private native void removeElement(Element element)
        /*-{
            if (element) {
                if (element.remove) {
                    element.remove();
                    return;
                }
                if (element.parentNode !== null) {
                    element.parentNode.removeChild(element);
                }
            }
        }-*/;
}
