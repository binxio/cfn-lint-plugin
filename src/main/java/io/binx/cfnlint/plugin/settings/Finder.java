package io.binx.cfnlint.plugin.settings;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.NotNullProducer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public final class Finder {
    private Finder() {
    }

    @NotNull
    static List<String> findAllExe() {
        List<File> fromPath = PathEnvironmentVariableUtil.findAllExeFilesInPath(getBinName("cfn-lint"));
        return fromPath.stream().map(File::getAbsolutePath).distinct().collect(Collectors.toList());
    }

    static String getBinName(String baseBinName) {
        // TODO do we need different name for windows?
        return SystemInfo.isWindows ? baseBinName + ".cmd" : baseBinName;
    }

    static boolean validatePath(Project project, String path) {
        File filePath = new File(path);
        if (filePath.isAbsolute()) {
            return filePath.exists() && filePath.isFile();
        } else {
            if (project == null || ProjectUtil.guessProjectDir(project) == null) {
                return true;
            }
            VirtualFile child = ProjectUtil.guessProjectDir(project).findFileByRelativePath(path);
            return child != null && child.exists() && !child.isDirectory();
        }
    }
}
