package io.binx.cfnlint.plugin;

import com.intellij.lang.annotation.Annotation;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        switch (issue.level.toLowerCase()) {
            case "error":
                return treatAsWarnings ? HighlightSeverity.WARNING : HighlightSeverity.ERROR;
            case "warning":
                return HighlightSeverity.WARNING;
            case "info":
                return HighlightSeverity.INFORMATION;
            default:
                return HighlightSeverity.INFORMATION;
        }
    }

    @Nullable
    private Annotation createAnnotation(@NotNull AnnotationHolder holder, @NotNull Document document, @NotNull CheckResult.Issue issue,
                                        @NotNull HighlightSeverity severity,
                                        CheckProjectComponent component) {
        int errorLine = issue.location.start.lineNumber - 1;
        boolean showErrorOnWholeLine = component.getSettings().highlightWholeLine;

        if (errorLine < 0 || errorLine >= document.getLineCount()) {
            return null;
        }

        int lineStartOffset = document.getLineStartOffset(errorLine);
        int lineEndOffset = document.getLineEndOffset(errorLine);

        int errorLineStartOffset = appendNormalizeColumn(document, lineStartOffset, lineEndOffset, issue.location.start.columnNumber - 1);
        if (errorLineStartOffset == -1) {
            return null;
        }

        TextRange range;
        if (showErrorOnWholeLine) {
            int start = DocumentUtil.getFirstNonSpaceCharOffset(document, lineStartOffset, lineEndOffset);
            range = new TextRange(start, lineEndOffset);
        } else {
            range = new TextRange(errorLineStartOffset, errorLineStartOffset + 1);
        }

        Annotation annotation = holder.createAnnotation(severity, range, ": " + issue.getFormattedMessage());
        if (annotation != null) {
            annotation.setAfterEndOfLine(errorLineStartOffset == lineEndOffset);
        }
        return annotation;
    }

    private int appendNormalizeColumn(@NotNull Document document, int startOffset, int endOffset, int column) {
        CharSequence text = document.getImmutableCharSequence();
        int col = 0;
        for (int i = startOffset; i < endOffset; i++) {
            char c = text.charAt(i);
            col += (c == '\t' ? 8 : 1);
            if (col > column) {
                return i;
            }
        }
        return startOffset;
    }

    private static boolean isFile(PsiFile file) {
        // TODO move to settings?
        List<String> acceptedExtensions = Arrays.asList("yml", "yaml", "json");
        boolean isCloudFormation = file.getFileType().getName().equals("CloudFormation");
        String fileExtension = Optional.ofNullable(file.getVirtualFile()).map(VirtualFile::getExtension).orElse("");
        return isCloudFormation || acceptedExtensions.contains(fileExtension);
    }
}


