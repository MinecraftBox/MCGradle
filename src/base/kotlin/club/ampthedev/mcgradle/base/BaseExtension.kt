package club.ampthedev.mcgradle.base

import groovy.lang.GroovyObjectSupport
import org.gradle.api.GradleException
import club.ampthedev.mcgradle.base.utils.checkValidConstantProperty
import club.ampthedev.mcgradle.base.versioning.GitVersion
import org.gradle.api.Project

abstract class BaseExtension(private val project: Project) : GroovyObjectSupport() {
    var version = "1.8.9"
    var mappingChannel = "stable"
    var mappingVersion = "22"
    var runDirectory = "run"
    var clientMainClass = "net.minecraft.client.main.Main"
    var serverMainClass = "net.minecraft.server.dedicated.DedicatedServer"
    var kotlinVersion: String? = null
    var gitVersion: Boolean = false
        set(v) {
            project.version = GitVersion.gitVersion(project.projectDir)
            field = v
        }
    val properties = hashMapOf<String, Any>()

    fun gitVersion(gitVersion: Boolean) {
        this.gitVersion = gitVersion
    }

    fun version(version: String) {
        this.version = version
    }

    fun mappingChannel(channel: String) {
        this.mappingChannel = channel
    }

    fun mappingVersion(version: String) {
        this.mappingVersion = version
    }

    fun runDirectory(directory: String) {
        this.runDirectory = directory
    }

    fun clientMainClass(mainClass: String) {
        this.clientMainClass = mainClass
    }

    fun serverMainClass(mainClass: String) {
        this.serverMainClass = mainClass
    }

    fun kotlinVersion(version: String) {
        this.kotlinVersion = version
    }

    override fun invokeMethod(name: String?, args: Any?): Any {
        setProperty(name, args)
        return Unit
    }

    override fun setProperty(property: String?, newValue: Any?) {
        if (property == null) throw GradleException("Property names cannot be null")
        checkValidConstantProperty(newValue)
        properties[property] = newValue!!
    }

    override fun getProperty(property: String?): Any {
        if (property == null) throw GradleException("Property names cannot be null")
        return properties[property] ?: throw GradleException("Property $property does not exist")
    }
}