package com.eligotech.sbt.plugins

import sbt._
import sbt.Keys._

object DSbt extends Plugin {

  val Config = config("sbt-dir") extend (Compile)

  val libsDirectory = SettingKey[File](
    "lib-directory", "name of directory containing libraries"
  )

  val transferDirectories = SettingKey[Iterable[(File, File)]](
    "copy-dir-content", "copy content of named directory to target directory of distribution"
  )

  val transferFilesInto = SettingKey[Iterable[(File, File)]](
    "copy-file-into", "copy file into specified directory"
  )

  val buildDist = InputKey[Unit]("build-dist", "")
  val distSettings: Seq[sbt.Project.Setting[_]] = Seq(
    libsDirectory := new File("dist") / "lib",
    transferDirectories := Seq.empty,
    transferFilesInto := Seq.empty,
    buildDist <<= createDistribution
  )

  private def createDistribution = inputTask {
    (argTask: TaskKey[Seq[String]]) =>
      (argTask, update, DSbt.libsDirectory, DSbt.transferDirectories, DSbt.transferFilesInto, artifactPath in Compile in packageBin) map {
        (argTask, upd, libs, dirCopies, fileCopies, artifactFile) =>
          println("copying %s" format (artifactFile))
          copyDependencies(upd, libs)
          copyBulkDirectories(dirCopies)
          copySingleFiles(fileCopies)
          IO.copyFile(artifactFile, libs / artifactFile.getName)
      }
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
    IO.delete(libs)
    IO.createDirectory(libs)
    updateReport.select(Set("compile", "runtime")) foreach {
      file =>
        IO.copyFile(file, libs / file.getName)
    }
  }
}
