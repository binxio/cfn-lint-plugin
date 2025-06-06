plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("io.freefair.git-version") version "8.13.1"
}

group = "io.binx.cfnlint.plugin"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1.1.1")
    }
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }
        changeNotes = """
       <p>0.2.0 2025.* support by Philippe Jandot</p>
       <p>0.1.28 2024.* support by Philippe Jandot</p>
       <p>0.1.27 2023.* support, improve highlights, refactor and remove deprecated code to pass JetBrains plugin validation by Philippe Jandot</p>
       <p>0.1.26 2023.1 support, handle informational lints, fix partial highlights, removed some deprecated usages by Philippe Jandot</p>
       <p>0.1.25 fixed IntelliJ 2022.3 support by Andreas Franz</p>
       <p>0.1.24 fixed IntelliJ 2022.2 support by Philippe Jandot</p>
       <p>0.1.23 fixed IntelliJ 2022.1 support by Michael Brewer</p>
       <p>0.1.22 fixed IntelliJ 2021.3 support by Philippe Jandot</p>
       <p>0.1.21 fixed IntelliJ 2021.2 support by Michael Brewer</p>
       <p>0.1.20 fixed IntelliJ 2021.1 support by Philippe Jandot</p>
       <p>0.1.19 support IntelliJ 2021.1</p>
       <p>0.1.18 support IntelliJ 2020.2.4</p>
       <p>0.1.17 support IntelliJ 2020.3</p>
       <p>0.1.16 support IntelliJ 2020.2.2</p>
       <p>0.1.15 support IntelliJ 2020.2</p>
       <p>0.1.14 support IntelliJ 2020.1.4</p>
       <p>0.1.13 support IntelliJ 2020.1.3</p>
       <p>0.1.12 support IntelliJ 2020.1.2</p>
       <p>0.1.11 support IntelliJ 2020.1</p>
       <p>0.1.10 support IntelliJ 2019.3.4</p>
       <p>0.1.9 support IntelliJ 2019.3.3</p>
       <p>0.1.8 support IntelliJ 2019.3</p>
       <p>0.1.7 support IntelliJ 2019.2</p>
       <p>0.1.6 fix Broken pipe errors by Philippe Jandot</p>
       <p>0.1.5 official build for 2019.1</p>
       <p>0.1.3 process all json and yaml files with "AWSTemplateFormatVersion" in it</p>
       <p>0.1.2 official build for 2018.2</p>
       <p>0.1.1 updated to support from build 181 and upwards</p>
       <p>0.1.0 First experimental version based upon <a href="https://github.com/pwielgolaski/shellcheck-plugin">shellcheck</a></p>
    """.trim()
    }
}

tasks {
    publishPlugin {
        token = providers.gradleProperty("intellijPublishToken")
    }
}

apply(plugin = "io.freefair.git-version")
