package com.eligotech.sbt.plugins

import sbt._
import sbt.Keys._
import scala.util.control.Exception._


object DSbt extends Plugin {
  type FileFilter = (String) => Boolean

  // TODO add more archive types
  trait DistType
  case object Zip extends DistType

  
  val baseDist = file("dist")

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
  
  val distType = SettingKey[DistType](
    "dist-type", "sets the type of distribution (zip, or simple dir)"
  )

  val buildDist = TaskKey[Unit]("build-dist", "")    

  val distClean = TaskKey[Unit]("clean-dist", "Remove dist files.")
  
  val archiveDist = TaskKey[File]("archive-dist", "Creates an archive of the distribution")

  val distSettings: Seq[sbt.Project.Setting[_]] =  Seq(
    libsDirectory := baseDist / "lib",
    excludeLibs := Seq(filterSourceFile),
    transferDirectories := Seq.empty,
    transferFilesInto := Seq.empty,    
    buildDist <<= createDistribution,
    distType := Zip,
    archiveDist <<= createArchive,
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
  
  private def createArchive =
    (DSbt.transferDirectories, DSbt.transferFilesInto, DSbt.distType, name, version) map { (dirs, files, distType, distName, distVersion) =>
      distType match {
        case Zip => createZip(( dirs ++ files).unzip._2, "%s-%s.zip" format (distName, distVersion))

      }
    }

  private def createZip(files: Iterable[File], name: String): File = {
    require (
      files.filter(!_.exists()).isEmpty ,
      "Missing files in assembly. Make sure all required files have been added to the distribution."
    )

    val toZip = files flatMap { f  =>  f ** -DirectoryFilter x relativeTo(baseDist) }

    println("creating zip from : ")
    toZip foreach println

    println("packaging files ...")
    catching(classOf[java.util.zip.ZipException]) either { IO.zip ( toZip, baseDist / name ) } match {
      case Left(t) => {
        sys.error("Unable to create zip. Make sure the distribution is created first by running \"build-dist\"")
      }
      case _ => baseDist / name
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
