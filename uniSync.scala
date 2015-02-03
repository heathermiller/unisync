#!/bin/sh
SCRIPT="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
DIR=`dirname "${SCRIPT}"}`
exec scala -save -cp $DIR/commons-io-2.4.jar $0 $DIR $SCRIPT $@
::!#

/** Usage:
  *  uniSync /path/to/srcDir /path/to/destDir
  */

import java.io.{File => JFile}
import java.io.{FileWriter, PrintWriter}
import scala.io.Source
import org.apache.commons.io._

object App {
  def main(args: Array[String]): Unit = {

    val files = args.map(new JFile(_))
    val currentDir = System.getProperty("user.dir")
    val sourceDir = files(2)
    val destDir = files(3)

    println("You called uniSync inside of directory: " + currentDir)
    val dotUnisync = new JFile(currentDir, ".unisync");
    if (!dotUnisync.exists) {
      println("Creating a new .unisync file in current directory...")
      initialize(sourceDir, dotUnisync)
      println("uniSync initialized. Run uniSync again to actually do your uni-directional sync.")
      return
    } else println("Reusing .unisync file in current directory...")
    println("Running uniSync with source dir: %s, and destination dir: %s.\n".format(sourceDir, destDir))

    // figure out the difference between the last run of uniSync and the current run
    val previousFileList = Source.fromFile(dotUnisync.getAbsolutePath).getLines.toList
    val currentFileList = mkFileList(sourceDir)


    // delete files in destination that have been deleted at the source
    val deletedFilesAtSrc = previousFileList filter { prevFile =>
      !currentFileList.exists(file => file.getAbsolutePath == prevFile)
    }

    val filesToDeleteAtDest = deletedFilesAtSrc map { fileName =>
      findCorrespondingInDestDir(sourceDir, destDir, new JFile(fileName))
    }

    if (deletedFilesAtSrc.isEmpty) println("No files have been removed since last run.")
    else {
      println(deletedFilesAtSrc.length + " files removed from source directory. Removing the following corresponding files from destination directory...")
      filesToDeleteAtDest foreach println
    }

    if (!filesToDeleteAtDest.isEmpty) {
      print("Deleting... ")
      filesToDeleteAtDest foreach {
        file => FileUtils.deleteQuietly(file)
        val parentDir = file.getParentFile
        if(parentDir.list().length == 0) FileUtils.deleteQuietly(parentDir) // that means the parent directory is empty
      }
      print("DONE.")
    }


    // copy all files from source to destination
    print("\nCopying to " + destDir + "... ")
    currentFileList foreach { file =>
      FileUtils.copyFile(file, findCorrespondingInDestDir(sourceDir, destDir, file))
    }
    print("DONE.")

    // update .unisync file with what's now in the source directory
    print("\nUpdating .unisync... ")
    writeUnisyncFile(currentFileList.toList, dotUnisync)
    print("DONE.\n")

    // print summary
    println("Summary: copied " + currentFileList.length + " files, deleted " + deletedFilesAtSrc.length + " files.")

    // println("\n\nPreviously:")
    // previousFileList foreach println

    // println("\n\nCurrently:")
    // currentFileList foreach println
  }

  def findCorrespondingInDestDir(sourceDir: JFile, destDir: JFile, file: JFile): JFile = {
    val relativePath = file.getAbsolutePath.drop(sourceDir.getAbsolutePath.length + 1)
    val destFile = new JFile(destDir, relativePath)
    destFile
  }

  def initialize(sourceDir: JFile, dotUnisync: JFile) = {
    check(sourceDir)
    val fileList = mkFileList(sourceDir)
    writeUnisyncFile(fileList.toList, dotUnisync)
  }

  def writeUnisyncFile(files: List[JFile], dotUnisync: JFile) = {
    val writer = new PrintWriter(new FileWriter(dotUnisync))

    // output file list to newly-created .unisync file
    files foreach (file => writer.println(file.getAbsolutePath))
    writer.close
  }

  /** Lists all the files contained in `dir` and, recursively, the children of `dir`.
    * Note that this contains only files, and not directories.
    */
  def mkFileList(dir: JFile): Array[JFile] = {
      val children = if (dir.isDirectory) {
        dir.listFiles
      } else Array[JFile]()

      val fileList =
        if (dir.isDirectory) children.flatMap(child => mkFileList(child))
        else Array(dir)

      fileList
  }

  def check(file: JFile): Unit = if (!file.exists) sys.error("Provided path to directory, "+file.getAbsolutePath+" doesn't exist")
}