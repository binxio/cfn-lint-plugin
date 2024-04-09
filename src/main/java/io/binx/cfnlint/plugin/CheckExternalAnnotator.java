package io.binx.cfnlint.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import io.binx.cfnlint.plugin.settings.Settings;
import io.binx.cfnlint.plugin.utils.CheckResult;
import io.binx.cfnlint.plugin.utils.CheckRunner;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckExternalAnnotator extends
        ExternalAnnotator<CheckAnnotationInput, AnnotationResult> {

    private static final Logger LOG = Logger.getInstance(CheckExternalAnnotator.class);

    @Nullable
    @Override
    public CheckAnnotationInput collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return collectInformation(file);
    }

    @Nullable
    @Override
    public CheckAnnotationInput collectInformation(@NotNull PsiFile file) {
        if (file.getContext() != null) {
            return null;
        }
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            return null;
        }
        if (file.getViewProvider() instanceof MultiplePsiFilesPerDocumentFileViewProvider) {
            return null;
        }
        CheckProjectService service = CheckProjectService.getInstance(file.getProject());
        if (!service.isSettingsValid() || !service.isEnabled() || !isFile(file)) {
            return null;
        }
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        boolean fileModified = fileDocumentManager.isFileModified(virtualFile);
        return new CheckAnnotationInput(file, fileModified ? file.getText() : null);
    }

    @Nullable
    @Override
    public AnnotationResult doAnnotate(CheckAnnotationInput input) {
        Project project = input.getProject();
        CheckProjectService service = CheckProjectService.getInstance(project);
        Settings settings = Settings.getInstance(project);

        try {
            CheckResult result = CheckRunner.runCheck(settings.executable, input.getCwd(), input.getFilePath(), input.getFileContent());

            if (StringUtils.isNotEmpty(result.getErrorOutput())) {
                service.showInfoNotification(result.getErrorOutput(), NotificationType.WARNING);
                return null;
            }
            return new AnnotationResult(input, result);
        } catch (Exception e) {
            LOG.error("Error running  inspection: ", e);
            service.showInfoNotification("Error running  inspection: " + e.getMessage(), NotificationType.ERROR);
        }
        return null;
    }

    @Override
    public void apply(@NotNull PsiFile file, AnnotationResult annotationResult, @NotNull AnnotationHolder holder) {
        if (annotationResult == null) {
            return;
        }
        Project project = file.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }

        Settings settings = Settings.getInstance(project);
        for (CheckResult.Issue issue : annotationResult.getIssues()) {
            HighlightSeverity severity = getHighlightSeverity(issue, settings.treatAllIssuesAsWarnings);
            createAnnotation(holder, document, issue, severity, settings);
        }
    }

    private static HighlightSeverity getHighlightSeverity(CheckResult.Issue issue, boolean treatAsWarnings) {
        return switch (issue.level.toLowerCase()) {
            case "error" -> treatAsWarnings ? HighlightSeverity.WARNING : HighlightSeverity.ERROR;
            case "warning" -> HighlightSeverity.WARNING;
            case "informational" -> HighlightSeverity.WEAK_WARNING;
            default -> HighlightSeverity.INFORMATION;
        };
    }

    private void createAnnotation(@NotNull AnnotationHolder holder, @NotNull Document document, @NotNull CheckResult.Issue issue,
                                  @NotNull HighlightSeverity severity, @NotNull Settings settings) {
//        LOG.warn(issue.toString());

        int startErrorLine = issue.location.start.lineNumber - 1;
        if (startErrorLine < 0 || startErrorLine >= document.getLineCount()) {
            return;
        }
        int endErrorLine = issue.location.end.lineNumber - 1;
//        LOG.warn("startErrorLine: " + startErrorLine + " endErrorLine: " + endErrorLine);

        int start = document.getLineStartOffset(startErrorLine) + issue.location.start.columnNumber - 1;
        int end = !settings.highlightWholeLine ?
                document.getLineStartOffset(endErrorLine) + issue.location.end.columnNumber - 1 : document.getLineEndOffset(endErrorLine);
//        LOG.warn("start: " + start + " end: " + end);
        TextRange range = new TextRange(start, end);
        holder.newAnnotation(severity, issue.getFormattedMessage()).range(range).create();
    }

    private static boolean isFile(PsiFile file) {
        String fileType = file.getFileType().getName().toLowerCase();
        return switch (fileType) {
            case "yaml", "json" -> file.getText().contains("AWSTemplateFormatVersion");
            default -> false;
        };
    }

}
