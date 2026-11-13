allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// Force all plugins to compile against SDK 36
subprojects {
    val forceSdk = Action<Project> {
        val android = extensions.findByName("android")
        if (android != null) {
            try {
                android.javaClass.getMethod("compileSdkVersion", Int::class.javaPrimitiveType).invoke(android, 36)
            } catch (e: Exception) { }
        }
    }
    if (state.executed) forceSdk.execute(this) else afterEvaluate(forceSdk)
}
