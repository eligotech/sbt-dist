package com.eligotech.sbt.plugins

import sbt._
import sbt.Keys._
import util.matching.Regex

object DSbt extends Plugin {
  type FileFilter = (String) => Boolean

  val libsDirectory = SettingKey[File](
    "lib-directory", "name of directory containing libraries"
  )

  val excludeLibs = SettingKey[Iterable[FileFilter]] (
    "exclude-libs", "sets of filter to exclude unwanted libs"
  )

  val transferDirectories = SettingKey[Iterable[(File, File)]](
    "copy-dir-content", "copy content of named directory to target directory of distribution"
  )

  val transferFilesInto = SettingKey[Iterable[(File, File)]](
    "copy-file-into", "copy file into specified directory"
  )

  val buildDist = TaskKey[Unit]("build-dist", "")

  val distClean = TaskKey[Unit]("clean-dist", "Remove dist files.")

  val distSettings: Seq[sbt.Project.Setting[_]] =  Seq(
    libsDirectory := new File("dist") / "lib",
    excludeLibs := Seq(filterSourceFile),
    transferDirectories := Seq.empty,
    transferFilesInto := Seq.empty,
    buildDist <<= createDistribution,
    distClean <<= cleanDist
  )

  private val sourceFilter = """.+(\-source).+""".r
  private val filterSourceFile: FileFilter = (name: String) => name match {
    case sourceFilter(_) => true
    case _ => false
  }

  private def createDistribution =
      (update, DSbt.libsDirectory, DSbt.excludeLibs, DSbt.transferDirectories, DSbt.transferFilesInto, artifactPath in Compile in packageBin) map {
        (upd, libs, exclude, dirCopies, fileCopies, artifactFile) =>
          println("copying %s" format (artifactFile))
          copyDependencies(upd, libs, exclude)
          copyBulkDirectories(dirCopies)
          copySingleFiles(fileCopies)
          IO.copyFile(artifactFile, libs / artifactFile.getName)
      }

  private def copySingleFiles(filesToCopy: Iterable[(File, File)]) {
    filesToCopy.foreach {
      entry =>
        val (sourceFile, newDir) = entry
        IO.transfer(sourceFile, newDir / sourceFile.getName)
    }
  }

  private def copyBulkDirectories(dirsToCopy: Iterable[(File, File)]) {
    dirsToCopy.foreach {
      entry =>
        val (source, newDir) = entry
        IO.copyDirectory(source, newDir)
    }
  }

  private def copyDependencies(updateReport: Id[UpdateReport], libs: File, exclusionRules: Iterable[FileFilter]) {
    if (!libs.exists) IO.createDirectory(libs)
    updateReport.select(Set("compile", "runtime")) foreach { file =>
        if (compliant(file, exclusionRules)) IO.copyFile(file, libs / file.getName)
    }
  }


  def compliant(file: File, exclusionRules: Iterable[FileFilter]) =
      exclusionRules.find(rule => rule.apply(file.getName)).isEmpty

  private def cleanDist =
    (DSbt.libsDirectory, DSbt.transferDirectories, DSbt.transferFilesInto) map { (libs, dirs, fileTransfer) =>
      fileTransfer foreach { pair =>
        val (_, targetDir) = pair
        IO.delete(targetDir)
      }

      dirs foreach { pair =>
        val (_, targetDir) = pair
        IO.delete(targetDir)
      }

      IO.delete(libs)
    }
}
