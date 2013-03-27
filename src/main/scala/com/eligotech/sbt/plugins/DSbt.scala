package com.eligotech.sbt.plugins

import sbt._
import sbt.Keys._
import scala.util.control.Exception._

object DSbt extends Plugin with Logging {
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

  def compliant(file: File, exclusionRules: Iterable[FileFilter]) =
    exclusionRules.find(rule => rule.apply(file.getName)).isEmpty

  private def createDistribution =
      (update, baseDirectory, DSbt.libsDirectory, DSbt.excludeLibs, DSbt.transferDirectories, DSbt.transferFilesInto, artifactPath in Compile in packageBin, fullClasspath in Compile) map {
        (upd, baseDir, libs, exclude, dirCopies, fileCopies, artifactFile, fullClassPath) =>
          logInfo("copying %s" format (artifactFile))
          copyDependencies(fullClassPath, libs, exclude)
          copyBulkDirectories(baseDir, dirCopies)
          copySingleFiles(baseDir, fileCopies)
          IO.copyFile(artifactFile, baseDist / artifactFile.getName)
      }
  
  private def createArchive =
    (baseDirectory, DSbt.transferDirectories, DSbt.transferFilesInto, DSbt.distType, name, version) map { (baseDir, dirs, files, distType, distName, distVersion) =>
      distType match {
        case Zip if !(dirs ++ files).isEmpty => createZip(baseDir, (dirs ++ files).unzip._2, "%s-%s.zip" format (distName, distVersion))
        case _ => null
      }
    }

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

  private def createZip(baseDir: File, files: Iterable[File], name: String): File = {
    require (
      files.map(f =>  baseDir / f.getPath).filter(!_.exists()).isEmpty,
      "Missing files in assembly. Make sure all required files have been added to the distribution."
    )
    
    val toZip = files map(f =>  baseDir / f.getPath) flatMap { f  =>  f ** -DirectoryFilter x relativeTo(baseDir / "dist")  }
        
    logInfo("creating zip from : ")
    toZip foreach { sd => logInfo("%s into %s" format (sd._1.getAbsolutePath, sd._2)) }

    logInfo("packaging files ...")
    catching(classOf[java.util.zip.ZipException]) either { IO.zip ( toZip, baseDir / baseDist.getPath / name ) } match {
      case Left(t) => {
        sys.error("Unable to create zip - Reason : %s" format t.getMessage)
      }
      case _ => baseDir / baseDist.getPath / name
    }
  }

  private def copySingleFiles(baseDir: File, filesToCopy: Iterable[(File, File)]) {
    filesToCopy map { sd =>
      val (src, dest) = sd
      (baseDir / src.getPath, baseDir / dest.getPath) 
    } foreach {
      entry =>
        val (sourceFile, newDir) = entry
        IO.transfer(sourceFile, newDir / sourceFile.getName)
    }
  }

  private def copyBulkDirectories(baseDir: File, dirsToCopy: Iterable[(File, File)]) {
    dirsToCopy map { sd =>
      val (src, dest) = sd
      (baseDir / src.getPath, baseDir / dest.getPath)
    } foreach { entry =>
        val (source, newDir) = entry
        IO.copyDirectory(source, newDir)
    }
  }

  private def copyDependencies(fullClassPath: Id[Keys.Classpath], libs: File, exclusionRules: Iterable[FileFilter]) {
    if (!libs.exists) IO.createDirectory(libs)
    fullClassPath.collect {
      case f if f.data.isFile => f.data
    } foreach { file =>
        if (compliant(file, exclusionRules)) IO.copyFile(file, libs / file.getName)
    }
  }

}
