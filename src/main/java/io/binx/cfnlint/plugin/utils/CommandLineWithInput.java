package io.binx.cfnlint.plugin.utils;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CommandLineWithInput extends GeneralCommandLine {

    private String input;

    public CommandLineWithInput withInput(String input) {
        this.input = input;
        return this;
    }

    @NotNull
    @Override
    public Process createProcess() throws ExecutionException {
        Process process = super.createProcess();
        if (input != null) {
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(input.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return process;
    }
}
