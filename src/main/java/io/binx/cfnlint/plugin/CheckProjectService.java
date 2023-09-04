package io.binx.cfnlint.plugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import io.binx.cfnlint.plugin.settings.Settings;
import io.binx.cfnlint.plugin.settings.SettingsPage;

import static io.binx.cfnlint.plugin.Bundle.message;

@Service(Service.Level.PROJECT)
public final class CheckProjectService {
    private final Project project;
    private boolean settingValidStatus;
    private int settingHashCode;
    private static final String PLUGIN_NAME = "cfn-lint";

    public static CheckProjectService getInstance(Project project) {
        return project.getService(CheckProjectService.class);
    }

    public CheckProjectService(Project project) {
        this.project = project;
    }

    boolean isEnabled() {
        return Settings.getInstance(project).pluginEnabled;
    }

    boolean isSettingsValid() {
        Settings settings = Settings.getInstance(project);
        if (settings.hashCode() != settingHashCode) {
            settingHashCode = settings.hashCode();
            settingValidStatus = settings.isValid(project);
            if (!settingValidStatus)
                validationFailed(message("settings.invalid"));
        }
        return settingValidStatus;
    }

    private void validationFailed(String msg) {
        NotificationAction action = NotificationAction.createSimple(message("settings.fix"), () -> new SettingsPage(project).showSettings());
        showInfoNotification(msg, NotificationType.WARNING, action);
        settingValidStatus = false;
    }

    void showInfoNotification(String content, NotificationType type) {
        showInfoNotification(content, type, null);
    }

    private void showInfoNotification(String content, NotificationType type, NotificationAction action) {
        Notification notification = new Notification(PLUGIN_NAME, PLUGIN_NAME, content, type);
        if (action != null)
            notification.addAction(action);
        Notifications.Bus.notify(notification, this.project);
    }
}
