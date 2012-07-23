package com.eligotech.sbt.plugins

import sbt._
import sbt.Keys._

object DSbt extends Plugin {
  val libsDirectory = SettingKey[File](
    "lib-directory", "name of directory containing libraries"
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
    transferDirectories := Seq.empty,
    transferFilesInto := Seq.empty,
    buildDist <<= createDistribution,
    distClean <<= clean
  )


  private def createDistribution =
      (update, DSbt.libsDirectory, DSbt.transferDirectories, DSbt.transferFilesInto, artifactPath in Compile in packageBin) map {
        (upd, libs, dirCopies, fileCopies, artifactFile) =>
          println("copying %s" format (artifactFile))
          copyDependencies(upd, libs)
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

  private def copyDependencies(updateReport: Id[UpdateReport], libs: File) {
    if (!libs.exists) IO.createDirectory(libs)
    updateReport.select(Set("compile", "runtime")) foreach {
      file =>
        IO.copyFile(file, libs / file.getName)
    }
  }

  private def clean =
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
