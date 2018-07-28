package io.binx.cfnlint.plugin;

import com.intellij.psi.PsiFile;

class CheckAnnotationInput {
    private final CheckProjectComponent component;
    private final PsiFile psiFile;
    private final String fileContent;

    CheckAnnotationInput(CheckProjectComponent component, PsiFile psiFile, String fileContent) {
        this.component = component;
        this.psiFile = psiFile;
        this.fileContent = fileContent;
    }

    CheckProjectComponent getComponent() {
        return component;
    }

    String getCwd() {
        return psiFile.getProject().getBasePath();
    }

    String getFilePath() {
        return psiFile.getVirtualFile().getPath();
    }

    String getFileContent() {
        return fileContent;
    }

}
