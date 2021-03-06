package de.wayofquality.sbt.jbake

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.Properties

import sbt.Keys._
import sbt._

object JBake extends AutoPlugin {

  object autoImport {
    val jbakeVersion = settingKey[String]("The jbake version.")
    val jbakeLib = settingKey[ModuleID]("The jbake binary distribution to be used.")
    val jbakeInputDir = settingKey[File]("The input directory for the site generation.")
    val jbakeOutputDir = settingKey[File]("The directory for the generated site.")
    val jbakeMode = settingKey[String]("Run JBake in build or serve mode, default: build")

    val jbakeAsciidocAttributes = taskKey[Map[String, String]]("Asciidoctor attribute to passed to Asciidoctor")
    val jbakeNodeBinDir = taskKey[Option[File]]("The directory where we can find the executables for node modules. (Needed, when using e.g. mermaid)")
    val jbakeSiteAssets = taskKey[Map[File, File]]("Assets to be included in the site")

    val jbakeBuild = taskKey[Seq[File]]("Run the jbake build step.")
    val jbakeSite = taskKey[Seq[File]]("Build the complete site")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(

    jbakeLib := "jbake" % "jbake" % jbakeVersion.value from s"https://dl.bintray.com/jbake/binary/jbake-${jbakeVersion.value}-bin.zip",

    libraryDependencies ++= Seq(
      jbakeLib.value
    ),

    jbakeInputDir := baseDirectory.value,
    jbakeOutputDir := target.value / "site",
    jbakeMode := "build",
    jbakeSiteAssets := Map.empty,

    jbakeAsciidocAttributes := Map(
      "imagesdir" -> "images",
      "imagesoutdir" -> "images"
    ) ++ (Compile/jbakeNodeBinDir).value.map(nd =>
      "mermaid" -> nd.getAbsolutePath()
    ),

    jbakeBuild := {

      val (jbakeFile, jbakeVersion) : (File, String) = {

        val depRes = (Compile / dependencyResolution).value

        val jbakeDep : ModuleID = libraryDependencies.value.filter(id => id.name.equals("jbake")).head

        val f : File = depRes.retrieve(
          jbakeDep,
          None,
          target.value,
          streams.value.log
        ).right.get.head

        (f,jbakeDep.revision)
      }

      val log = streams.value.log
      val jbakeDir = target.value / s"jbake-$jbakeVersion-bin"

      log.info(s"Extracting jbake ...")
      IO.unzip(jbakeFile, target.value)

      SiteGenerator(
        jbakeDir = jbakeDir,
        inputDir = (Compile/jbakeInputDir).value,
        outputDir = (Compile/jbakeOutputDir).value,
        nodeBinDir = (Compile/jbakeNodeBinDir).value,
        attributes = (Compile/jbakeAsciidocAttributes).value,
        mode = (Compile/jbakeMode).value,
      )(streams.value.log).bake()
    },

    jbakeSite := {
      val site = jbakeBuild.value

      val log = streams.value.log

      jbakeSiteAssets.value.foreach { case (from, to) =>
        if (from.exists()) {
          if (from.isDirectory()) {
            log.info(s"Copying site asset directory from [$from] to [$to]")
            IO.copyDirectory(from, to)
          } else {
            log.info(s"Copying site asset file from [$from] to [$to]")
            IO.copyFile(from, to)
          }
        }
      }

      site
    }
  )
}

case class SiteGenerator(
  jbakeDir: File,
  inputDir: File,
  outputDir: File,
  nodeBinDir: Option[File],
  mode: String,
  attributes: Map[String, String]
)(implicit log: Logger) {

  private val jbakeCp : String = (jbakeDir / "lib").listFiles(new FileFilter {
    override def accept(pathname: File): Boolean = pathname.isDirectory() || (pathname.isFile && pathname.getName().endsWith("jar"))
  }).map(_.getAbsolutePath()).mkString(File.pathSeparator)

  def bake(): Seq[File] = {

    val props = new Properties()
    props.load(new FileInputStream(inputDir / "jbake.properties.tpl"))

    val attributeString = attributes.map { case (k, v) => s"$k=$v" }.mkString(",")
    props.put("asciidoctor.attributes", attributeString)

    val os = new FileOutputStream(inputDir / "jbake.properties")
    props.store(os, "Auto generated jbake properties. Perform modifications in [jbake.properties.tpl]")

    IO.copyFile(inputDir / "logback.xml", jbakeDir / "lib" / "logging" / "logback.xml")

    val currPath = System.getenv("PATH")

    val jBakeOptions = ForkOptions()
      .withEnvVars(Map() ++ nodeBinDir.map(nd => "PATH" -> s"${nd.getAbsolutePath()}${File.pathSeparator}$currPath"))

    val args: Seq[String] = Seq(
      "-classpath", jbakeCp,
      "org.jbake.launcher.Main",
      inputDir.getAbsolutePath(),
      outputDir.getAbsolutePath(),
      "-b"
    ) ++ (
      if (mode.equalsIgnoreCase("build")) Seq() else Seq("-s")
    )

    log.info("Running jbake with arguments\n" + args.mkString("\n"))
    val process = Fork.java.fork(jBakeOptions, args)

    process.exitValue() match {
      case 0 =>
        IO.touch(outputDir / ".nojekyll")
        Seq(outputDir)
      case v =>
        throw new Exception(s"JBake ended with return code [$v]")
    }
  }
}
