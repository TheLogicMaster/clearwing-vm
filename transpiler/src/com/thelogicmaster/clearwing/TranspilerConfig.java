package com.thelogicmaster.clearwing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TranspilerConfig {
    private List<String> nonOptimized = new ArrayList<>(); // Patterns for classes to not optimize out even if unused
    private List<String> reflective = new ArrayList<>(); // Patterns for classes to generate reflection metadata for
    private List<String> sourceIgnores = new ArrayList<>(); // Patterns for source files to ignore for JNIGen style inlined C++
    private List<String> intrinsics = new ArrayList<>(); // A list of methods to treat as native so that they can be patched (For example, `java.lang.Integer.toString()Ljava/lang/String;`)
    private List<String> definitions = new ArrayList<>(); // Custom definitions to add to config.hpp
    private boolean projectFiles = true; // Whether to generate basic project files like the CMake config
    private String mainClass; // An optional "main class" that contains the entrypoint main function
    private boolean stackTraces = true; // Enable stack traces (Disable for a slight performance increase)
    private boolean lineNumbers = true; // Enable stack trace line numbers (Requires stack traces, disable for a slight performance increase)
    private boolean stackCookies = true; // Enable Java stack cookies (Only needed for debugging VM)
    private boolean valueChecks = false; // Enable type/NPE checks at runtime, has substantial performance overhead
    private boolean platformOverride = false; // Enable custom platform implementation for env vars and such
    private boolean leakCheck = false; // Enable leak check on exit

    public TranspilerConfig() {
    }

    public TranspilerConfig(String contents) {
        JSONObject json = new JSONObject(contents);
        nonOptimized = getArray(json, "nonOptimized");
        reflective = getArray(json, "reflective");
        sourceIgnores = getArray(json, "sourceIgnores");
        intrinsics = getArray(json, "intrinsics");
        definitions = getArray(json, "definitions");
        projectFiles = json.optBoolean("generateProjectFiles", false);
        mainClass = json.optString("mainClass");
        stackTraces = json.optBoolean("useStackTraces", true);
        lineNumbers = json.optBoolean("useLineNumbers", true);
        valueChecks = json.optBoolean("useValueChecks", true);
        stackCookies = json.optBoolean("useStackCookies", false);
        leakCheck = json.optBoolean("useLeakCheck", false);
        platformOverride = json.optBoolean("platformOverride", false);
    }

    public TranspilerConfig(List<String> nonOptimized, List<String> reflective, List<String> sourceIgnores, List<String> intrinsics, List<String> definitions, boolean projectFiles, String mainClass, boolean stackTraces, boolean lineNumbers, boolean stackCookies, boolean valueChecks) {
        this.nonOptimized = nonOptimized;
        this.reflective = reflective;
        this.sourceIgnores = sourceIgnores;
        this.intrinsics = intrinsics;
        this.definitions = definitions;
        this.projectFiles = projectFiles;
        this.mainClass = mainClass;
        this.stackTraces = stackTraces;
        this.lineNumbers = lineNumbers;
        this.stackCookies = stackCookies;
        this.valueChecks = valueChecks;
    }

    private static List<String> getArray(JSONObject json, String name) {
        JSONArray array = json.optJSONArray(name);
        return array == null ? new ArrayList<>() : array.toList().stream().map(item -> (String)item).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Merge another config into this one (Ignores most boolean flags and main class)
     */
    public void merge(TranspilerConfig config) {
        nonOptimized.addAll(config.nonOptimized);
        reflective.addAll(config.reflective);
        sourceIgnores.addAll(config.sourceIgnores);
        intrinsics.addAll(config.intrinsics);
        definitions.addAll(config.definitions);
        platformOverride = platformOverride || config.platformOverride;
    }

    public List<String> getNonOptimized() {
        return nonOptimized;
    }

    public void setNonOptimized(List<String> nonOptimized) {
        this.nonOptimized = nonOptimized;
    }

    public List<String> getReflective() {
        return reflective;
    }

    public void setReflective(List<String> reflective) {
        this.reflective = reflective;
    }

    public List<String> getSourceIgnores() {
        return sourceIgnores;
    }

    public void setSourceIgnores(List<String> sourceIgnores) {
        this.sourceIgnores = sourceIgnores;
    }

    public List<String> getIntrinsics() {
        return intrinsics;
    }

    public void setIntrinsics(List<String> intrinsics) {
        this.intrinsics = intrinsics;
    }

    public boolean isWritingProjectFiles() {
        return projectFiles;
    }

    public void setWritingProjectFiles(boolean projectFiles) {
        this.projectFiles = projectFiles;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public boolean hasStackTraces() {
        return stackTraces;
    }

    public void setStackTraces(boolean stackTraces) {
        this.stackTraces = stackTraces;
    }

    public boolean hasLineNumbers() {
        return lineNumbers;
    }

    public void setLineNumbers(boolean lineNumbers) {
        this.lineNumbers = lineNumbers;
    }

    public boolean hasStackCookies() {
        return stackCookies;
    }

    public void setStackCookies(boolean stackCookies) {
        this.stackCookies = stackCookies;
    }

    public boolean hasValueChecks() {
        return valueChecks;
    }

    public void setValueChecks(boolean valueChecks) {
        this.valueChecks = valueChecks;
    }

    public boolean hasPlatformOverride() {
        return platformOverride;
    }

    public void setPlatformOverride(boolean platformOverride) {
        this.platformOverride = platformOverride;
    }

    public boolean hasLeakCheck() {
        return leakCheck;
    }

    public void setLeakCheck(boolean leakCheck) {
        this.leakCheck = leakCheck;
    }
}
