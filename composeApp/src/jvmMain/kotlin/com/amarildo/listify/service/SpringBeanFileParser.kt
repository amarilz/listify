package com.amarildo.listify.service

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private const val COMPONENT = "@Component"
private const val CONTROLLER = "@Controller"
private const val REST_CONTROLLER = "@RestController"
private const val SERVICE = "@Service"
private const val REPOSITORY = "@Repository"

private val targetAnnotations: Set<String> = setOf(
    COMPONENT,
    SERVICE,
    REPOSITORY,
    CONTROLLER,
    REST_CONTROLLER,
)

val groups = mapOf(
    "CONTROLLER" to "_CONTROLLER",
    "SERVICE" to "_SERVICE",
    "COMPONENT" to "_COMPONENT",
    "REPOSITORY" to "_REPOSITORY",
)

data class Total(val allFiles: List<ClassInfo>) {
    val classesSet: Set<String> = allFiles.map { it.className }.toSet()
}

data class ClassInfo(
    val packageName: String?,
    val className: String,
    val annotations: List<String>,
    val variables: List<String>,
    val filePath: String,
) {
    fun isController() = (CONTROLLER in annotations) or (REST_CONTROLLER in annotations)
    fun isRepository() = REPOSITORY in annotations
    fun isService() = SERVICE in annotations
    fun isComponent() = COMPONENT in annotations
}

class MermaidClassMapper(classes: List<ClassInfo>) {
    private val repositoryCounter = AtomicInteger(1)
    private val serviceCounter = AtomicInteger(1)
    private val componentCounter = AtomicInteger(1)
    private val controllerCounter = AtomicInteger(1)

    val mappedClasses: Map<String, String> = classes
        .mapNotNull { info ->
            when {
                info.isService() -> groups["SERVICE"] + "${serviceCounter.getAndIncrement()}" to info.className
                info.isComponent() -> groups["COMPONENT"] + "${componentCounter.getAndIncrement()}" to info.className
                info.isRepository() -> groups["REPOSITORY"] + "${repositoryCounter.getAndIncrement()}" to info.className
                info.isController() -> groups["CONTROLLER"] + "${controllerCounter.getAndIncrement()}" to info.className
                else -> null
            }
        }
        .toMap()

    fun getMermaidIdForClass(className: String): String? = mappedClasses.entries
        .find { it.value == className }?.key
}

class SpringBeanFileParser {

    fun printGraph(startDir: File): String {
        val allFiles: List<File> = getAllFiles(startDir)

        val result: MutableList<ClassInfo> = mutableListOf()
        for (file in allFiles) {
            val parseFile: ClassInfo? = parseFile(file)
            if (parseFile != null) {
                result.add(parseFile)
            }
        }

        return generateMermaidGraph(Total(result).allFiles)
    }

    private fun parseFile(file: File): ClassInfo? {
        var packageName: String? = null
        val annotations = mutableListOf<String>()
        var className: String? = null

        file.forEachLine { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("package ") ->
                    packageName = trimmed.removePrefix("package").removeSuffix(";").trim()

                trimmed.startsWith("public class") ||
                    trimmed.startsWith("class ") ||
                    trimmed.contains(" class ") -> {
                    if (className == null) {
                        className = trimmed.substringAfter("class").trim().split(" ", "{")[0]
                    }
                }

                trimmed.contains("public interface ") ||
                    trimmed.contains(" interface ") -> {
                    if (className == null) {
                        className = trimmed.substringAfter("interface").trim().split(" ", "{")[0]
                    }
                }

                trimmed.startsWith("@") ->
                    annotations += trimmed.substringBefore("(")
            }
        }

        val variables = findClassVariableTypes(file)
        val matchedAnnotations = annotations.filter { it in targetAnnotations }

        return if (packageName != null && className != null && matchedAnnotations.isNotEmpty()) {
            ClassInfo(
                packageName = packageName,
                className = className,
                annotations = matchedAnnotations,
                variables = variables,
                filePath = file.absolutePath,
            )
        } else {
            null
        }
    }

    fun generateMermaidGraph(classes: List<ClassInfo>): String {
        val mapper = MermaidClassMapper(classes)
        val mermaidGraph = StringBuilder("graph LR;\n")

        groups.forEach { (label, prefix) ->
            if (label != "CONTROLLER") {
                mermaidGraph.append("    subgraph $label\n")
            }
            mapper.mappedClasses.filterKeys { it.startsWith(prefix) }.forEach { (id, name) ->
                val node = when (prefix) {
                    groups["CONTROLLER"] -> "$id[[$name]]"
                    groups["REPOSITORY"] -> "$id[($name)]"
                    else -> "$id($name)"
                }
                mermaidGraph.append("    $node;\n")
                if (node.contains("_CONTROLLER")) {
                    mermaidGraph.append("    style $id fill:#008000,stroke:#333,stroke-width:4px\n")
                }
            }
            if (label != "CONTROLLER") {
                mermaidGraph.append("    end\n")
            }
        }

        val classMap = classes.associateBy { it.className }

        classes.forEach { classInfo ->
            val validDependencies = classInfo.variables.filter { it in classMap }
            validDependencies.forEach { dependency ->
                val targetId = mapper.getMermaidIdForClass(dependency)
                val sourceId = mapper.getMermaidIdForClass(classInfo.className) ?: classInfo.className
                val target = targetId ?: dependency
                mermaidGraph.append("    $sourceId --> $target;\n")
            }
        }

        return mermaidGraph.toString().trimEnd()
    }
}

private fun getAllFiles(startDir: File): List<File> = startDir.walk(FileWalkDirection.TOP_DOWN)
    .filter { it.isFile }
    .map { it.absoluteFile.normalize() }
    .distinctBy { it.toPath() } // evita duplicati (es. alias/symlink risolti uguali)
    .sortedBy { it.absolutePath }
    .toList()

fun findClassVariableTypes(file: File): List<String> {
    val regex = Regex("\\b(?:public|private|protected|static|final)?\\s*(\\w+)\\s+[a-zA-Z_]\\w*\\s*[=;]")
    val variableTypes = mutableListOf<String>()

    try {
        var inMethod = false
        var braceCount = 0

        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("//") || trimmed.startsWith("/*")) return@forEachLine

            if ("{" in trimmed) {
                braceCount++
                if (trimmed.matches(Regex(".*\\(.*\\).*\\{"))) inMethod = true
            }
            if ("}" in trimmed) {
                braceCount--
                if (inMethod && braceCount == 1) inMethod = false
            }

            if (!inMethod) regex.find(trimmed)?.let { variableTypes += it.groupValues[1] }
        }
    } catch (e: Exception) {
        println("Errore durante la lettura del file: ${e.message}")
    }

    return variableTypes
}
