package com.kiishi.ite.intellij.runtime

data class IteRuntime(
    val installed: Boolean,
    val executable: String,
    val source: String,
    val message: String = "",
)

data class IteUpdate(
    val updateKey: String,
    val installedVersion: String,
    val latestVersion: String,
)
