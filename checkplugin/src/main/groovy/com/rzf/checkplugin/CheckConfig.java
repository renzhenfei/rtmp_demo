package com.rzf.checkplugin;

public class CheckConfig {

    public String whiteList;

    public String outputDir;

    public boolean enable;

    public void whiteList(String whiteList) {
        this.whiteList = whiteList;
    }

    public void outputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public void enable(boolean enable) {
        this.enable = enable;
    }
}
