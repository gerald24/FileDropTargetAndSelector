package net.g24.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.StreamVariable;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.ProgressBarRenderer;

/**
 * Sample implementation of a component, which handles a collection of {@link Html5File}s with {@link StreamVariable}s. It shows progress bars for
 * each file and make remaining upload progress cancellable. In case of single upload mode the first file of the given collection will be taken.
 */
public class FileStreamDialog extends Window {
    private static final String CANCEL_RUNNING_AND_CLOSE_CAPTION = "Cancel running and Close";
    private static final String CLOSE_CAPTION = "Close";

    private final Grid<HandledHtml5File> grid = new Grid<>();
    private final Button button = new Button(CANCEL_RUNNING_AND_CLOSE_CAPTION, event -> close());
    private Registration streamRegistration = () -> {};

    public FileStreamDialog() {
        grid.setSizeFull();

        grid.addColumn(HandledHtml5File::getFileName).setCaption("Name");
        grid.addColumn(HandledHtml5File::getType).setCaption("Type");
        grid.addColumn(HandledHtml5File::getFileSize).setCaption("Size");
        grid.addColumn(HandledHtml5File::getPercentLoaded, new ProgressBarRenderer()).setCaption("Loaded");

        button.setDisableOnClick(true);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.addComponentsAndExpand(grid);
        verticalLayout.addComponent(button);

        setModal(true);
        setResizable(true);
        setContent(verticalLayout);
        setHeight(50, Unit.PERCENTAGE);
        setWidth(80, Unit.PERCENTAGE);
    }

    @Override
    public void attach() {
        super.attach();
        center();
    }

    public FileStreamDialog show(UI ui) {
        ui.addWindow(this);
        return this;
    }

    @Override
    public void close() {
        streamRegistration.remove();
        streamRegistration = () -> {};
        super.close();
    }

    public void load(Collection<Html5File> files, boolean multiple) {
        List<HandledHtml5File> handledFiles = files
            .stream() //
            .limit(multiple ? files.size() : 1)
            .map(HandledHtml5File::new)
            .collect(Collectors.toList());
        ListDataProvider<HandledHtml5File> provider = new ListDataProvider<>(handledFiles);

        grid.setDataProvider(provider);
        // not very effective but well enough for demo
        handledFiles.forEach(file -> {
            file.setStateChangeHandler(() -> provider.refreshItem(file));
            file.setFinishedHandler(() -> updateButton(handledFiles.stream().anyMatch(HandledHtml5File::isNotFinished)));
        });

        streamRegistration = () -> handledFiles.forEach(file -> file.interrupted = true);
    }

    private void updateButton(boolean notFinished) {
        button.setCaption(notFinished ? CANCEL_RUNNING_AND_CLOSE_CAPTION : CLOSE_CAPTION);
    }

    private static class HandledHtml5File {
        private final Html5File file;
        private long received;
        private boolean listening = true;
        private boolean interrupted;
        private boolean finished;
        private Runnable stateChangeHandler;
        private Runnable finishedHandler;
        private Path tempFile;
        private OutputStream outputStream;

        private HandledHtml5File(Html5File file) {
            this.file = file;

            try {
                tempFile = Files.createTempFile("demoview", "stream");
                tempFile.toFile().deleteOnExit();
                outputStream = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
            } catch (IOException e) {

            }

            file.setStreamVariable(new StreamVariable() {
                @Override
                public OutputStream getOutputStream() {
                    return outputStream;
                }

                @Override
                public boolean listenProgress() {
                    return HandledHtml5File.this.listening;
                }

                @Override
                public void onProgress(StreamingProgressEvent event) {
                    HandledHtml5File.this.received = event.getBytesReceived();
                    HandledHtml5File.this.stateChanged();
                }

                @Override
                public void streamingStarted(StreamingStartEvent event) {}

                @Override
                public void streamingFinished(StreamingEndEvent event) {
                    HandledHtml5File.this.finished = true;
                    HandledHtml5File.this.handleFileFinished();
                    ensureOutputStreamClosedAndRemoveTempFile();
                }

                @Override
                public void streamingFailed(StreamingErrorEvent event) {
                    ensureOutputStreamClosedAndRemoveTempFile();
                }

                @Override
                public boolean isInterrupted() {
                    return HandledHtml5File.this.interrupted;
                }
            });
        }

        private void stateChanged() {
            if (stateChangeHandler != null) {
                stateChangeHandler.run();
            }
        }

        private void handleFileFinished() {
            if (finishedHandler != null) {
                finishedHandler.run();
            }
        }

        public String getFileName() {
            return file.getFileName();
        }

        public String getType() {
            return file.getType();
        }

        public long getFileSize() {
            return file.getFileSize();
        }

        public double getPercentLoaded() {
            return getFileSize() > 0 ? (received * 100 / getFileSize()) / 100.0d : 0.0d;
        }

        public void setStateChangeHandler(Runnable stateChangeHandler) {
            this.stateChangeHandler = stateChangeHandler;
        }

        public void setFinishedHandler(Runnable finishedHandler) {
            this.finishedHandler = finishedHandler;
        }

        public boolean isNotFinished() {
            return !finished;
        }

        private void ensureOutputStreamClosedAndRemoveTempFile() {
            // for demo purpose we do not need uploaded resources
            IOUtils.closeQuietly(outputStream);
            outputStream = null;
            if (tempFile != null) {
                try {
                    tempFile.toFile().delete();
                } catch (RuntimeException e) {
                    // ignore;
                }
                tempFile = null;
            }
        }

    }
}
