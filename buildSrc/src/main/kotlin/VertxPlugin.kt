import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.slf4j.LoggerFactory
import java.io.File

private val LOGGER = LoggerFactory.getLogger(VertxPlugin::class.java)

class VertxPlugin : Plugin<Project> {
  private lateinit var gradleCommand: String

  override fun apply(project: Project) {
    findGradleCommand(project)
    createVertxPluginExtension(project)
    applyOtherPlugins(project)
    createVertxTasks(project)
    project.afterEvaluate {
      LOGGER.debug("Vert.x plugin configuration: ${project.vertxPluginExtension()}")
      configureVertxVersion(project)
      addVertxCoreDependency(project)
      defineMainClass(project)
      configureVertxRunTask(project)
      configureVertxDebugTask(project)
    }
  }

  private fun findGradleCommand(project: Project) {
    val globalGradle = if (Os.isFamily(Os.FAMILY_WINDOWS)) "gradle.bat" else "gradle"
    val gradlewScript = if (Os.isFamily(Os.FAMILY_WINDOWS)) "gradlew.bat" else "gradlew"

    fun findRecursively(dir: File): String? {
      val script = File(dir, gradlewScript)
      return when {
        script.exists() -> script.absolutePath
        dir.parentFile != null -> findRecursively(dir.parentFile)
        else -> null
      }
    }

    gradleCommand = findRecursively(project.projectDir) ?: globalGradle
  }

  private fun createVertxPluginExtension(project: Project) {
    project.extensions.create<VertxPluginExtension>("vertx", project)
    LOGGER.debug("Vert.x extension created and added to project")
  }

  private fun Project.vertxPluginExtension(): VertxPluginExtension =
    this.extensions.getByName("vertx") as VertxPluginExtension

  private fun applyOtherPlugins(project: Project) {
    LOGGER.debug("Applying the plugins needed by the Vert.x plugin")
    with(project.pluginManager) {
      apply(JavaPlugin::class.java)
      apply(ApplicationPlugin::class.java)
    }
    LOGGER.debug("The plugins needed by the Vert.x plugin have been applied")
  }

  private fun createVertxTasks(project: Project) {
    project.tasks.create<JavaExec>("vertxRun").dependsOn("classes")
    project.tasks.create<JavaExec>("vertxDebug").dependsOn("classes")
    LOGGER.debug("Vert.x tasks have been created")
  }

  private fun configureVertxVersion(project: Project) {
    val vertxVersion = project.vertxPluginExtension().version
    project.dependencies.apply {
      add("implementation", platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    }
    LOGGER.debug("Recommending Vert.x version $vertxVersion")
  }

  private fun addVertxCoreDependency(project: Project) {
    project.dependencies.apply {
      add("implementation", "io.vertx:vertx-core")
    }
    LOGGER.debug("Added vertx-core as compile dependency")
  }

  private fun defineMainClass(project: Project) {
    val vertxPluginExtension = project.vertxPluginExtension()
    val applicationPluginExtension = project.extensions.getByType<JavaApplication>()
    applicationPluginExtension.mainClass.set(vertxPluginExtension.launcherClass)
    LOGGER.debug("The main class has been set to ${vertxPluginExtension.launcherClass}")
  }

  private fun configureVertxRunTask(project: Project) {
    val vertxPluginExtension = project.vertxPluginExtension()
    val javaPluginExtension = project.extensions.getByType<JavaPluginExtension>()
    val mainSourceSet = javaPluginExtension.sourceSets.getByName("main")
    project.tasks.getByName<JavaExec>("vertxRun").apply {
      group = "Application"
      description = "Run this project as Vert.x application"
      workingDir = File(vertxPluginExtension.workDirectory)
      jvmArgs = vertxPluginExtension.jvmArgs
      classpath = mainSourceSet.runtimeClasspath
      mainClass.set(if (vertxPluginExtension.redeploy) "io.vertx.core.Launcher" else vertxPluginExtension.launcherClass)

      if (vertxPluginExtension.mainVerticleClass == null) {
        if (vertxPluginExtension.launcherClass == "io.vertx.core.Launcher") {
          throw GradleException("Extension property vertx.mainVerticleClass must be specified when using io.vert.core.Launcher as a launcher.")
        }
        args("run")
      } else {
        args("run", vertxPluginExtension.mainVerticleClass)
      }

      if (vertxPluginExtension.redeploy) {
        args("--launcher-class", vertxPluginExtension.launcherClass)
        if (vertxPluginExtension.jvmArgs.isNotEmpty()) {
          args("--java-opts", vertxPluginExtension.jvmArgs.joinToString(separator = " ", prefix = "\"", postfix = "\""))
        }
        args("--redeploy", vertxPluginExtension.watch.joinToString(separator = ","))
        if (vertxPluginExtension.onRedeploy.isNotEmpty()) {
          args("--on-redeploy", "$gradleCommand ${vertxPluginExtension.onRedeploy.joinToString(separator = " ")}")
        }
        args("--redeploy-grace-period", vertxPluginExtension.redeployGracePeriod)
        args("--redeploy-scan-period", vertxPluginExtension.redeployScanPeriod)
        args("--redeploy-termination-period", vertxPluginExtension.redeployTerminationPeriod)
      }

      if (vertxPluginExtension.config != null) {
        args("--conf", vertxPluginExtension.config)
      }

      vertxPluginExtension.args.forEach { args(it) }
    }

    LOGGER.debug("The vertxRun task has been configured")
  }

  private fun configureVertxDebugTask(project: Project) {
    val vertxPluginExtension = project.vertxPluginExtension()
    val javaPluginExtension = project.extensions.getByType<JavaPluginExtension>()
    val mainSourceSet = javaPluginExtension.sourceSets.getByName("main")
    project.tasks.getByName<JavaExec>("vertxDebug").apply {
      group = "Application"
      description = "Debug this project as Vert.x application"
      workingDir = File(vertxPluginExtension.workDirectory)
      jvmArgs = vertxPluginExtension.jvmArgs
      jvmArgs = computeDebugOptions(project)
      classpath = mainSourceSet.runtimeClasspath
      mainClass.set(vertxPluginExtension.launcherClass)

      if (vertxPluginExtension.mainVerticleClass == null) {
        if (vertxPluginExtension.launcherClass == "io.vertx.core.Launcher") {
          throw GradleException("Extension property vertx.mainVerticleClass must be specified when using io.vert.core.Launcher as a launcher.")
        }
        args("run")
      } else {
        args("run", vertxPluginExtension.mainVerticleClass)
      }

      if (vertxPluginExtension.config != null) {
        args("--conf", vertxPluginExtension.config)
      }

      vertxPluginExtension.args.forEach { args(it) }
    }

    LOGGER.debug("The vertxDebug task has been configured")
  }

  private fun computeDebugOptions(project: Project): List<String> {
    val vertxPluginExtension = project.vertxPluginExtension()
    val debugger = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=" +
        (if (vertxPluginExtension.debugSuspend) "y" else "n") + ",address=${vertxPluginExtension.debugPort}"
    val disableEventLoopchecker = "-Dvertx.options.maxEventLoopExecuteTime=${java.lang.Long.MAX_VALUE}"
    val disableWorkerchecker = "-Dvertx.options.maxWorkerExecuteTime=${java.lang.Long.MAX_VALUE}"
    val mark = "-Dvertx.debug=true"

    return arrayListOf(debugger, disableEventLoopchecker, disableWorkerchecker, mark)
  }
}