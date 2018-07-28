package io.binx.cfnlint.plugin;

import io.binx.cfnlint.plugin.utils.CheckResult;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

class AnnotationResult {

    private final CheckAnnotationInput input;
    private final CheckResult result;

    AnnotationResult(CheckAnnotationInput input, CheckResult result) {
        this.input = input;
        this.result = result;
    }

    public List<CheckResult.Issue> getIssues() {
        return Optional.ofNullable(result).map(CheckResult::getIssues).orElse(Collections.emptyList());
    }

    public CheckAnnotationInput getInput() {
        return input;
    }

}
