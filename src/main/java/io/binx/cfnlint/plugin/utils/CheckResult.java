package io.binx.cfnlint.plugin.utils;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class CheckResult {
    private final List<Issue> issues;
    private final String errorOutput;

    public CheckResult(List<Issue> issues, String errorOutput) {
        this.issues = issues == null ? Collections.emptyList() : issues;
        this.errorOutput = errorOutput;
    }

    public CheckResult(String errorOutput) {
        this(null, errorOutput);
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public String getErrorOutput() {
        return errorOutput;
    }

    public static class Rule {
        @SerializedName("Id")
        public String id;
        @SerializedName("Description")
        public String description;
        @SerializedName("ShortDescription")
        public String shortDescription;
        @SerializedName("Source")
        public String source;

        @Override
        public String toString() {
            return "Rule{" +
                    "id='" + id + '\'' +
                    ", description='" + description + '\'' +
                    ", shortDescription='" + shortDescription + '\'' +
                    ", source='" + source + '\'' +
                    '}';
        }
    }
    public static class Offset {
        @SerializedName("ColumnNumber")
        public int columnNumber;
        @SerializedName("LineNumber")
        public int lineNumber;

        @Override
        public String toString() {
            return "Offset{" +
                    "columnNumber=" + columnNumber +
                    ", lineNumber=" + lineNumber +
                    '}';
        }
    }

    public static class Location {
        @SerializedName("Start")
        public Offset start;
        @SerializedName("End")
        public Offset end;

        @Override
        public String toString() {
            return "Location{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }
    }

    public static class Issue {
        @SerializedName("Rule")
        public Rule rule;
        @SerializedName("Location")
        public Location location;
        @SerializedName("Level")
        public String level;
        @SerializedName("Message")
        public String message;
        @SerializedName("Filename")
        public String filename;
        public String getFormattedMessage() {
            return message.trim() + " [" + (rule == null ? "none" : rule.id) + "]";
        }

        @Override
        public String toString() {
            return "Issue{" +
                    "rule=" + rule +
                    ", location=" + location +
                    ", level='" + level + '\'' +
                    ", message='" + message + '\'' +
                    ", filename='" + filename + '\'' +
                    '}';
        }
    }
}
