package io.binx.cfnlint.plugin.settings;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.util.NotNullProducer;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;
import io.binx.cfnlint.plugin.Bundle;
import io.binx.cfnlint.plugin.utils.CheckRunner;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class SettingsPage implements Configurable {
    private final Project project;
    private final Settings settings;

    private JCheckBox pluginEnabledCheckbox;
    private JPanel panel;
    private JPanel errorPanel;
    private JCheckBox treatAllIssuesCheckBox;
    private JCheckBox highlightWholeLineCheckBox;
    private JLabel versionLabel;
    private JLabel exeLabel;
    private TextFieldWithHistoryWithBrowseButton exeField;

    public SettingsPage(@NotNull final Project project) {
        this.project = project;
        this.settings = Settings.getInstance(project);
        initField();
    }

    private void addListeners() {
        pluginEnabledCheckbox.addItemListener(e -> setEnabledState(e.getStateChange() == ItemEvent.SELECTED));
        DocumentAdapter docAdp = new DocumentAdapter() {
            protected void textChanged(@NotNull DocumentEvent e) {
                updateLaterInEDT();
            }
        };
        exeField.getChildComponent().getTextEditor().getDocument().addDocumentListener(docAdp);
    }

    private void updateLaterInEDT() {
        UIUtil.invokeLaterIfNeeded(SettingsPage.this::update);
    }

    private void update() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        updateVersion();
    }

    private void setEnabledState(boolean enabled) {
        Stream.of(exeField, exeLabel,
                        treatAllIssuesCheckBox, highlightWholeLineCheckBox)
                .forEach(c -> c.setEnabled(enabled));
    }

    private void updateVersion() {
        updateVersion(exeField.getChildComponent().getText());
    }

    private void updateVersion(String exe) {
        AtomicReference<String> version = new AtomicReference<>("n.a.");
        if (Finder.validatePath(project, exe)) {
            ProgressManager.getInstance().
                    runProcessWithProgressSynchronously(
                            () -> {
                                try {
                                    version.set(CheckRunner.runVersion(exe, project.getBasePath()));
                                } catch (ExecutionException e) {
                                    version.set("error");
                                }
                            },
                            "",
                            false,
                            project);
        }
        versionLabel.setText(version.get());
    }

    private void initField() {
        TextFieldWithHistory textFieldWithHistory = exeField.getChildComponent();
        textFieldWithHistory.setHistorySize(-1);
        textFieldWithHistory.setMinimumAndPreferredWidth(0);

        SwingHelper.addHistoryOnExpansion(textFieldWithHistory, new NotNullProducer<List<String>>() {
            @NotNull
            public List<String> produce() {
                return Finder.findAllExe();
            }
        });
        SwingHelper.installFileCompletionAndBrowseDialog(project, exeField, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select Exe"));
    }

    @Nls
    @Override
    public String getDisplayName() {
        return Bundle.message("name");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        loadSettings();
        updateVersion();
        addListeners();
        return panel;
    }

    @Override
    public boolean isModified() {
        return pluginEnabledCheckbox.isSelected() != settings.pluginEnabled
                || !exeField.getChildComponent().getText().equals(settings.executable)
                || treatAllIssuesCheckBox.isSelected() != settings.treatAllIssuesAsWarnings
                || highlightWholeLineCheckBox.isSelected() != settings.highlightWholeLine;
    }

    @Override
    public void apply() throws ConfigurationException {
        saveSettings();
    }

    private void saveSettings() {
        settings.pluginEnabled = pluginEnabledCheckbox.isSelected();
        settings.executable = exeField.getChildComponent().getText();
        settings.treatAllIssuesAsWarnings = treatAllIssuesCheckBox.isSelected();
        settings.highlightWholeLine = highlightWholeLineCheckBox.isSelected();
    }

    private void loadSettings() {
        pluginEnabledCheckbox.setSelected(settings.pluginEnabled);
        exeField.getChildComponent().setText(settings.executable);
        treatAllIssuesCheckBox.setSelected(settings.treatAllIssuesAsWarnings);
        highlightWholeLineCheckBox.setSelected(settings.highlightWholeLine);
        setEnabledState(settings.pluginEnabled);
    }

    @Override
    public void reset() {
        loadSettings();
    }

    public void showSettings() {
        String dimensionKey = ShowSettingsUtilImpl.createDimensionKey(this);
        SingleConfigurableEditor singleConfigurableEditor = new SingleConfigurableEditor(project, this, dimensionKey, false);
        singleConfigurableEditor.show();
    }
}
