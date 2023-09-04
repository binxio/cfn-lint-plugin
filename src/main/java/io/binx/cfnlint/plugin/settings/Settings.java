package io.binx.cfnlint.plugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@State(name = "CheckProjectComponent", storages = {@Storage("cfnlintPlugin.xml") })
public class Settings implements PersistentStateComponent<Settings> {
    public String executable = "";
    public boolean treatAllIssuesAsWarnings;
    public boolean highlightWholeLine;
    public boolean pluginEnabled;

    public static Settings getInstance(Project project) {
        return project.getService(Settings.class);
    }

    @Nullable
    @Override
    public Settings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull Settings state) {
        XmlSerializerUtil.copyBean(state, this);
    }


    public boolean isValid(Project project) {
        return Finder.validatePath(project, executable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Settings settings = (Settings) o;
        return treatAllIssuesAsWarnings == settings.treatAllIssuesAsWarnings &&
                highlightWholeLine == settings.highlightWholeLine &&
                pluginEnabled == settings.pluginEnabled &&
                Objects.equals(executable, settings.executable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executable, treatAllIssuesAsWarnings, highlightWholeLine, pluginEnabled);
    }
}
