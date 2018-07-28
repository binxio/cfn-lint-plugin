package io.binx.cfnlint.plugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import io.binx.cfnlint.plugin.settings.Settings;
import io.binx.cfnlint.plugin.settings.SettingsPage;
import org.jetbrains.annotations.NotNull;

public class CheckProjectComponent implements com.intellij.openapi.components.ProjectComponent {
    private Project project;
    private Settings settings;
    private boolean settingValidStatus;
    private int settingHashCode;

    private static final Logger LOG = Logger.getInstance(CheckProjectComponent.class);

    private static final String PLUGIN_NAME = "cfn-lint";

    public CheckProjectComponent(Project project, Settings settings) {
        this.project = project;
        this.settings = settings;
    }

    @Override
    public void projectOpened() {
        if (isEnabled()) {
            isSettingsValid();
        }
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void initComponent() {
        if (isEnabled()) {
            isSettingsValid();
        }
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return CheckProjectComponent.class.getName();
    }

    Settings getSettings() {
        return settings;
    }

    boolean isEnabled() {
        return settings.pluginEnabled;
    }

    boolean isSettingsValid() {
        if (settings.hashCode() != settingHashCode) {
            settingHashCode = settings.hashCode();
            settingValidStatus = settings.isValid(project);
            if (!settingValidStatus) {
                validationFailed(Bundle.message("settings.invalid"));
            }
        }
        return settingValidStatus;
    }

    private void validationFailed(String msg) {
        NotificationListener notificationListener = (notification, event) -> new SettingsPage(project, settings).showSettings();
        String errorMessage = msg + Bundle.message("settings.fix");
        showInfoNotification(errorMessage, NotificationType.WARNING, notificationListener);
        LOG.debug(msg);
        settingValidStatus = false;
    }

    void showInfoNotification(String content, NotificationType type) {
        showInfoNotification(content, type, null);
    }

    private void showInfoNotification(String content, NotificationType type, NotificationListener notificationListener) {
        Notification notification = new Notification(PLUGIN_NAME, PLUGIN_NAME, content, type, notificationListener);
        Notifications.Bus.notify(notification, this.project);
    }
}
