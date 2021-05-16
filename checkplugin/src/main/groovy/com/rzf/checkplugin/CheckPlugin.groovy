
package com.rzf.checkplugin
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.function.Consumer

public class CheckPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {

        project.extensions.create("checkConfig",CheckConfig)
        project.extensions.create("checkConfigNext",CheckConfigNext)
        project.checkConfig.extensions.create("checkConfigInner",CheckConfigInner)
        _print(project)
        println('================================================')
        project.afterEvaluate {
            _print(project)
        }

    }

    private void _print(Project project){
        println('---------project----------' + project.name)
        println('---------project----------' + project.extensions)

        CheckConfig checkConfig = project.checkConfig
        println("----------------checkConfig------------------" + checkConfig.whiteList)
        CheckConfigInner inner = project.checkConfig.checkConfigInner
        println('--------------inner------------' + inner.class)
        println('--------------inner------------' + inner.name)
        CheckConfigNext checkConfigNext = project.checkConfigNext
        println("------------checkConfigNext--------------" + checkConfigNext.whiteList)
    }
}