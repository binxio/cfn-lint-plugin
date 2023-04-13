package io.binx.cfnlint.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CheckRunner {
    private CheckRunner() {
    }

    private static final Logger LOG = Logger.getInstance(CheckRunner.class);
    private static final int TIME_OUT = (int) TimeUnit.SECONDS.toMillis(120L);

    public static CheckResult runCheck(@NotNull String exe, @NotNull String cwd, @NotNull String file, String content) {
        CheckResult result;
        try {
            GeneralCommandLine commandLine = createCommandLine(exe, cwd);
            if (content == null) {
                commandLine = commandLine.withParameters("-c", "I", "-f", "json", "-t", file);
            } else {
                commandLine = ((CommandLineWithInput) commandLine)
                        .withInput(content)
                        .withParameters("-c", "I", "-f", "json", "-t", "-");
            }
            ProcessOutput out = execute(commandLine);
            try {
                result = new CheckResult(parse(out.getStdout()), out.getStderr());
            } catch (Exception e) {
                result = new CheckResult(out.getStdout());
            }
        } catch (Exception e) {
            LOG.error("Problem with running exe", e);
            result = new CheckResult(e.toString());
        }
        return result;
    }

    private static List<CheckResult.Issue> parse(String json) {
        Gson g = new GsonBuilder().create();
        Type listType = new TypeToken<List<CheckResult.Issue>>() {
        }.getType();
        return g.fromJson(json, listType);
    }

    @NotNull
    public static String runVersion(@NotNull String exe, @NotNull String cwd) throws ExecutionException {
        if (!new File(exe).exists()) {
            LOG.warn("Calling version with invalid exe " + exe);
            return "";
        }

        ProcessOutput out = execute(createCommandLine(exe, cwd).withParameters("--version"));
        if (out.getExitCode() == 0) {
            String output = out.getStdout().trim();
            Matcher matcher = Pattern.compile("^version:(.+)$", Pattern.MULTILINE).matcher(output);
            return matcher.find() ? matcher.group(1).trim() : output;
        }

        return "";
    }

    @NotNull
    private static CommandLineWithInput createCommandLine(@NotNull String exe, @NotNull String cwd) {
        CommandLineWithInput commandLine = new CommandLineWithInput();
        commandLine.setExePath(exe);
        commandLine.setWorkDirectory(cwd);
        return commandLine;
    }

    @NotNull
    public static ProcessOutput execute(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
        LOG.info("Running command: " + commandLine.getCommandLineString());
        Process process = commandLine.createProcess();
        OSProcessHandler processHandler = new ColoredProcessHandler(process, commandLine.getCommandLineString(), StandardCharsets.UTF_8);
        final ProcessOutput output = new ProcessOutput();
        processHandler.addProcessListener(new ProcessAdapter() {
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (outputType.equals(ProcessOutputTypes.STDERR)) {
                    output.appendStderr(event.getText());
                } else if (!outputType.equals(ProcessOutputTypes.SYSTEM)) {
                    output.appendStdout(event.getText());
                }
            }
        });
        processHandler.startNotify();
        if (processHandler.waitFor(TIME_OUT)) {
            output.setExitCode(process.exitValue());
        } else {
            processHandler.destroyProcess();
            output.setTimeout();
        }
        if (output.isTimeout()) {
            throw new ExecutionException("Command '" + commandLine.getCommandLineString() + "' is timed out.");
        }
        return output;
    }
}
