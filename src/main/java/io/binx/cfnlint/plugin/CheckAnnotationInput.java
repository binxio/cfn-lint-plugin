package io.binx.cfnlint.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

class CheckAnnotationInput {
    private final PsiFile psiFile;
    private final String fileContent;

    CheckAnnotationInput(PsiFile psiFile, String fileContent) {
        this.psiFile = psiFile;
        this.fileContent = fileContent;
    }

    String getCwd() {
        return psiFile.getProject().getBasePath();
    }

    Project getProject() { return psiFile.getProject(); }

    String getFilePath() {
        return psiFile.getVirtualFile().getPath();
    }

    String getFileContent() {
        return fileContent;
    }

}
