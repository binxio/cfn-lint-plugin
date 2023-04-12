package io.binx.cfnlint.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.DocumentUtil;
import io.binx.cfnlint.plugin.utils.CheckResult;
import io.binx.cfnlint.plugin.utils.CheckRunner;
import org.apache.commons.lang.StringUtils;
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
        CheckProjectComponent component = file.getProject().getComponent(CheckProjectComponent.class);
        if (!component.isSettingsValid() || !component.isEnabled() || !isFile(file)) {
            return null;
        }
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        boolean fileModified = fileDocumentManager.isFileModified(virtualFile);
        return new CheckAnnotationInput(component, file, fileModified ? file.getText() : null);
    }

    @Nullable
    @Override
    public AnnotationResult doAnnotate(CheckAnnotationInput input) {
        CheckProjectComponent component = input.getComponent();
        try {
            CheckResult result = CheckRunner.runCheck(component.getSettings().executable, input.getCwd(), input.getFilePath(), input.getFileContent());

            if (StringUtils.isNotEmpty(result.getErrorOutput())) {
                component.showInfoNotification(result.getErrorOutput(), NotificationType.WARNING);
                return null;
            }
            return new AnnotationResult(input, result);
        } catch (Exception e) {
            LOG.error("Error running  inspection: ", e);
            component.showInfoNotification("Error running  inspection: " + e.getMessage(), NotificationType.ERROR);
        }
        return null;
    }

    @Override
    public void apply(@NotNull PsiFile file, AnnotationResult annotationResult, @NotNull AnnotationHolder holder) {
        if (annotationResult == null) {
            return;
        }
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return;
        }

        CheckProjectComponent component = annotationResult.getInput().getComponent();
        for (CheckResult.Issue issue : annotationResult.getIssues()) {
            HighlightSeverity severity = getHighlightSeverity(issue, component.getSettings().treatAllIssuesAsWarnings);
            createAnnotation(holder, document, issue, severity, component);
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
                                  @NotNull HighlightSeverity severity, CheckProjectComponent component) {
        int errorLine = issue.location.start.lineNumber - 1;
        boolean showErrorOnWholeLine = component.getSettings().highlightWholeLine;

        if (errorLine < 0 || errorLine >= document.getLineCount()) {
            return;
        }

        int lineStartOffset = document.getLineStartOffset(errorLine);
        int lineEndOffset = document.getLineEndOffset(errorLine);

        if (showErrorOnWholeLine) {
            int start = DocumentUtil.getFirstNonSpaceCharOffset(document, lineStartOffset, lineEndOffset);
            TextRange range = new TextRange(start, lineEndOffset);
            holder.newAnnotation(severity, issue.getFormattedMessage()).range(range).create();
        } else {
            int start = lineStartOffset + issue.location.start.columnNumber - 1;
            int end = lineStartOffset + issue.location.end.columnNumber - 1;
            if (end >= lineEndOffset)
                holder.newAnnotation(severity, issue.getFormattedMessage()).afterEndOfLine().create();
            else {
                TextRange range = new TextRange(start, end);
                holder.newAnnotation(severity, issue.getFormattedMessage()).range(range).create();
            }
        }
    }

    private static boolean isFile(PsiFile file) {
        String fileType = file.getFileType().getName().toLowerCase();
        return switch (fileType) {
            case "yaml", "json" -> file.getText().contains("AWSTemplateFormatVersion");
            default -> false;
        };
    }

}
