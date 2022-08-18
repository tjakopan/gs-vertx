import org.gradle.api.Project

open class VertxPluginExtension(private val project: Project) {
  var version = "4.3.3"
  var launcherClass = "io.vertx.core.Launcher"
  var mainVerticleClass: String? = null
  var args = listOf<String>()
  var config: String? = null
  var workDirectory: String = project.projectDir.absolutePath
  var jvmArgs = listOf<String>()
  var redeploy = true
  var watch = listOf("${project.projectDir.absolutePath}/src/**/*")
  var onRedeploy = listOf("classes")
  var redeployScanPeriod = 1000L
  var redeployGracePeriod = 1000L
  var redeployTerminationPeriod = 1000L
  var debugPort = 5005L
  var debugSuspend = false

  override fun toString(): String {
    return "VertxPluginExtension(project=$project, version='$version', launcherClass='$launcherClass', mainVerticleClass=$mainVerticleClass, args=$args, config=$config, workDirectory='$workDirectory', jvmArgs=$jvmArgs, redeploy=$redeploy, watch=$watch, onRedeploy=$onRedeploy, redeployScanPeriod=$redeployScanPeriod, redeployGracePeriod=$redeployGracePeriod, redeployTerminationPeriod=$redeployTerminationPeriod, debugPort=$debugPort, debugSuspend=$debugSuspend)"
  }
}