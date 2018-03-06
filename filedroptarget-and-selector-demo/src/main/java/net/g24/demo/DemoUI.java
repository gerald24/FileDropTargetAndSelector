package net.g24.demo;

import java.util.Arrays;
import java.util.List;
import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.dnd.FileDropTarget;
import com.vaadin.ui.dnd.event.FileDropEvent;
import com.vaadin.ui.themes.ValoTheme;
import net.g24.FileDropTargetAndSelector;

@Push
@Theme("demo")
@Title("FileDropTargetAndSelector Add-on Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI {

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = DemoUI.class)
    public static class Servlet extends VaadinServlet {

    }

    private static final String DROP_AREA_DESCRIPTION = String.format( //
                                                                       "<i>droparea</i>:<br><br><b>any component</b>, which is also accepted by <b>%s</b>",
                                                                       FileDropTarget.class.getName());
    private static final String CLICKABLE_LAYOUT_DESCRIPTION = String.format( //
                                                                              "<i>clickable</i>:<br><br><b>any clickable Layout</b> (e.g. %s)",
                                                                              CssLayout.class);
    private static final String CLICKABLE_COMPONET_DESCRIPTION = String.format( //
                                                                                "<i>clickable</i>:<br><br><b>any clickable component</b>, which implements client-side %s (e.g. %s)",
                                                                                "com.google.gwt.event.dom.client.HasClickHandlers",
                                                                                Label.class.getName());

    private final Label header = new Label("Sample UI for FileDropTargetAndSelector AddOn");
    private final Label description = new Label(
            "FileDropTargetAndSelector extends FileDropTarget with client-side file selector activation through click events");

    private final CheckBox multipleField = new CheckBox("multiple");
    private final CheckBox enabledField = new CheckBox("enabled", true);
    private final CheckBox visibleField = new CheckBox("visible", true);

    private final CssLayout dropArea1 = new CssLayout();
    private final CssLayout clickableLayout = new CssLayout();

    private final CssLayout dropArea2 = new CssLayout();
    private final Label clickableComponent = new Label(CLICKABLE_COMPONET_DESCRIPTION, ContentMode.HTML);

    private final List<Component> controlables = Arrays.asList(dropArea1, dropArea2, clickableLayout, clickableComponent);

    private FileDropTargetAndSelector<CssLayout> targetAndSelector1;
    private FileDropTargetAndSelector<CssLayout> targetAndSelector2;

    @Override
    protected void init(VaadinRequest request) {
        header.addStyleName(ValoTheme.LABEL_H1);
        description.addStyleName(ValoTheme.LABEL_SMALL);

        initDropTarget1();
        initDropTarget2();
        initControls();

        VerticalLayout layout = new VerticalLayout();
        layout.setStyleName("demoContentLayout");
        layout.addComponents(header, description, layoutControls(), layoutDropTargets());
        setContent(layout);
    }

    private void initDropTarget1() {
        dropArea1.addStyleName("droparea");
        dropArea1.addComponent(new Label(DROP_AREA_DESCRIPTION, ContentMode.HTML));

        clickableLayout.addStyleName("clickable");
        clickableLayout.addComponent(new Label(CLICKABLE_LAYOUT_DESCRIPTION, ContentMode.HTML));

        targetAndSelector1 = new FileDropTargetAndSelector<>(dropArea1, clickableLayout, this::handleFiles);
    }

    private void initDropTarget2() {
        dropArea2.addStyleName("droparea");
        dropArea2.addComponent(new Label(DROP_AREA_DESCRIPTION, ContentMode.HTML));

        clickableComponent.addStyleName("clickable");
        targetAndSelector2 = new FileDropTargetAndSelector<>(dropArea2, clickableComponent, this::handleFiles);
    }

    private void initControls() {
        multipleField.addValueChangeListener(event -> {
            targetAndSelector1.setMultiple(event.getValue());
            targetAndSelector2.setMultiple(event.getValue());
        });
        enabledField.addValueChangeListener(event -> {
            controlables.forEach(controlable -> controlable.setEnabled(event.getValue()));
        });
        visibleField.addValueChangeListener(event -> {
            controlables.forEach(controlable -> controlable.setVisible(event.getValue()));
        });
    }

    private HorizontalLayout layoutControls() {
        HorizontalLayout controlLayout = new HorizontalLayout();

        Button changeButtonRole = new Button("Change ButtonRole Components");
        changeButtonRole.setDescription("Test replace button-role component (which triggers file selector)");
        changeButtonRole.addStyleName(ValoTheme.BUTTON_SMALL);
        changeButtonRole.addClickListener(e -> {
            controlLayout.removeComponent(changeButtonRole);

            CssLayout replacementClickableLayout = new CssLayout();
            replacementClickableLayout.addStyleName("clickable");
            replacementClickableLayout.addComponent(new Label("Replacement for clickable Layout)"));
            targetAndSelector1.setButtonRole(replacementClickableLayout);

            Label replacementClickableComponent = new Label("Replacement for clickable Label");
            targetAndSelector2.setButtonRole(replacementClickableComponent);

            controlLayout.addComponents(replacementClickableLayout, replacementClickableComponent);

            getContent().addComponent(new Label("Clickable Components changed. Original clickable components must not trigger file selector anymore"));
        });

        controlLayout.addComponents(multipleField, enabledField, visibleField, changeButtonRole);
        return controlLayout;
    }

    private HorizontalLayout layoutDropTargets() {
        VerticalLayout drop1Layout = new VerticalLayout();
        drop1Layout.setCaption("Drop Area with clickable Layout");
        drop1Layout.addComponents(dropArea1, clickableLayout);

        VerticalLayout drop2Layout = new VerticalLayout();
        drop2Layout.setCaption("Drop Area with clickable Component");
        drop2Layout.addComponents(dropArea2, clickableComponent);

        HorizontalLayout dropLayout = new HorizontalLayout();
        dropLayout.addComponents(drop1Layout, drop2Layout);
        return dropLayout;
    }

    private void handleFiles(FileDropEvent<?> event) {
        // NOTE: room for handling files count and single/multi mode validation, as well as mimetype and size restrictions
        new FileStreamDialog().show(getUI()).load(event.getFiles(), multipleField.getValue());
    }

    @Override
    public VerticalLayout getContent() {
        return (VerticalLayout) super.getContent();
    }
}
