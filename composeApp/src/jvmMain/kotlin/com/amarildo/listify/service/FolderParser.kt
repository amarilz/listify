package com.amarildo.listify.service

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class FolderParser {

    companion object {

        suspend fun selectDirectory(): Result<String> = withContext(Dispatchers.IO) {
            try {
                val selectedDirectory = FileKit.openDirectoryPicker()
                if (selectedDirectory != null) {
                    Result.success(selectedDirectory.file.absolutePath)
                } else {
                    Result.failure(IOException("No directory selected"))
                }
            } catch (e: Exception) {
                Result.failure(IOException("Error on selecting the folder: ${e.message}", e))
            }
        }

        fun processDirectory(inputDir: String, prefixesToFilter: String): String {
            val startDir = File(inputDir)
            val outFile = File(inputDir + File.separator + "listify.txt")

            if (!startDir.exists() || !startDir.isDirectory) {
                throw IllegalArgumentException("Il percorso $inputDir non è una cartella valida")
            }

            // pulizia del file di output se già esiste
            if (outFile.exists()) {
                outFile.writeText("")
            }

            val outPath: java.nio.file.Path? = outFile.absoluteFile.normalize().toPath()

            // 1. scrivi in cima l'alberatura
            outFile.appendText("== DIRECTORY TREE ==\n")
            writeDirectoryTree(
                root = startDir,
                outFile = outFile,
                outPath = outPath,
            )
            outFile.appendText("\n\n== FILE CONTENTS ==\n")

            // 2. traversal ricorsivo dei file
            val files: List<File> = startDir.walk(FileWalkDirection.TOP_DOWN)
                .filter { it.isFile }
                .map { it.absoluteFile.normalize() }
                .filter { it.toPath() != outPath }
                .distinctBy { it.toPath() } // evita duplicati (es. alias/symlink risolti uguali)
                .sortedBy { it.absolutePath }
                .toList()

            val prefixes: List<String> = prefixesToFilter
                .split(",")
                .filter { it.trim().isNotEmpty() }
                .map { it.trim() }
                .toList()
            files.forEach { file -> appendFileContent(file, outFile, prefixes) }

            return outFile.absolutePath
        }

        private fun writeDirectoryTree(
            root: File,
            outFile: File,
            outPath: java.nio.file.Path?,
        ) {
            // connectors
            val tee = "├── "
            val last = "└── "
            val pipe = "│   "
            val space = "    "

            fun childrenOf(dir: File): List<File> {
                val arr = dir.listFiles() ?: return emptyList()
                return arr.asSequence()
                    .map { it.absoluteFile.normalize() }
                    .filter { it.toPath() != outPath }
                    .sortedWith(
                        compareBy<File> { !it.isDirectory } // directory prima dei file
                            .thenBy { it.name.lowercase() },
                    )
                    .toList()
            }

            fun walk(dir: File, prefix: String) {
                val children = childrenOf(dir)
                children.forEachIndexed { index, child ->
                    val isLast = index == children.lastIndex
                    val connector = if (isLast) last else tee
                    outFile.appendText(prefix + connector + child.name + "\n")
                    if (child.isDirectory) {
                        val nextPrefix = prefix + if (isLast) space else pipe
                        walk(child, nextPrefix)
                    }
                }
            }

            // radice
            outFile.appendText(root.name + "\n")
            walk(root, "")
        }

        private fun appendFileContent(
            file: File,
            outFile: File,
            prefixesToFilter: List<String> = emptyList(),
        ) {
            outFile.appendText("=== File: ${file.absolutePath} ===\n")
            try {
                val content: String = file.useLines { lines ->
                    val filteredLines: List<String> = if (prefixesToFilter.isEmpty()) {
                        lines.toList()
                    } else {
                        lines.filterNot { line ->
                            prefixesToFilter.any { prefix ->
                                line.trimStart().startsWith(prefix)
                            }
                        }.toList()
                    }

                    // remove empty lines on top
                    filteredLines
                        .dropWhile { it.isBlank() }
                        .joinToString("\n", postfix = "\n")
                }

                outFile.appendText(content)
            } catch (ex: Exception) {
                throw IOException("Errore nella lettura del file ${file.absolutePath}: ${ex.message}", ex)
            }
            outFile.appendText("\n\n\n")
        }
    }
}
