plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
}

val localBuildRoot = providers.environmentVariable("SALT_LYRIC_BUILD_DIR")
if (localBuildRoot.isPresent) {
    allprojects {
        val buildName = path.removePrefix(":").replace(':', '-').ifBlank { "root" }
        layout.buildDirectory.set(file("${localBuildRoot.get()}/$buildName"))
    }
}
