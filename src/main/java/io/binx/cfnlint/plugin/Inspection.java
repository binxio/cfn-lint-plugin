package io.binx.cfnlint.plugin;

import com.intellij.codeInspection.BatchSuppressableTool;
import com.intellij.codeInspection.ExternalAnnotatorInspectionVisitor;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.codeInspection.ex.UnfairLocalInspectionTool;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Inspection extends LocalInspectionTool implements BatchSuppressableTool, UnfairLocalInspectionTool {

    public Inspection() {
    }

    @NotNull
    @Override
    public String getDisplayName()
    {
        return Bundle.message("inspection.display.name");
    }

    @NotNull
    @Override
    public String getShortName() {
        return Bundle.message("inspection.short.name");
    }

    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
        return ExternalAnnotatorInspectionVisitor.checkFileWithExternalAnnotator(file, manager, isOnTheFly, new CheckExternalAnnotator());
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
        return new ExternalAnnotatorInspectionVisitor(holder, new CheckExternalAnnotator(), isOnTheFly);
    }

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element) {
        return false;
    }

    @NotNull
    @Override
    public SuppressQuickFix @NotNull [] getBatchSuppressActions(@Nullable PsiElement element) {
        return new SuppressQuickFix[0];
    }
}
