import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._

scalaVersion := "2.11.7"

val nameLiteral = "FTypes-CaseClass"

lazy val buildSettings = Seq(
    organization := s"com.bryghts.${nameLiteral.toLowerCase}",
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.6", "2.11.7")
)

lazy val compilerOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen"
)

lazy val baseSettings = Seq(
    scalacOptions ++= compilerOptions ++ (
        CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((2, 11)) => Seq("-Ywarn-unused-import")
            case _ => Nil
        }
        ),
    scalacOptions in (Compile, console) := compilerOptions,
    scalacOptions in (Compile, test) := compilerOptions,
    resolvers ++= Seq(
        Resolver.sonatypeRepo("releases"),
        Resolver.sonatypeRepo("snapshots")
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies in ThisBuild <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val commonJsSettings = Seq(
    postLinkJSEnv := NodeJSEnv().value,
    scalaJSStage in Global := FastOptStage
)

lazy val docSettings = site.settings ++ ghpages.settings ++ unidocSettings ++ Seq(
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
    scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
        "-groups",
        "-implicits",
        "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
        "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
    ),
    git.remoteRepo := s"git@github.com:;marcesquerra/$nameLiteral.git",
    unidocProjectFilter in (ScalaUnidoc, unidoc) :=
        inAnyProject // -- inProjects( crossJs )
)

lazy val root = project.in(file("."))
    .settings(allSettings)
    .settings(docSettings)
    .settings(noPublishSettings)
    .aggregate(
        crossJvm //, crossJs
    )
    .dependsOn(crossJvm)

lazy val cross = crossProject.in(file("."))
    .settings(
        moduleName := nameLiteral,
        name := nameLiteral,
        libraryDependencies ++= Seq( "com.bryghts.ftypes-array" %%% "ftypes-array" % "0.0.1" ),
        libraryDependencies +=  "com.lihaoyi" %%% "utest" % "0.3.1" % "test",
        testFrameworks      +=  new TestFramework("utest.runner.Framework")
    )
    .settings(allSettings: _*)
    .jsSettings(commonJsSettings: _*)
    .jvmConfigure(_.copy(id = "crossJvm"))
    .jsConfigure(_.copy(id  = "crossJs"))

lazy val crossJvm = cross.jvm
//lazy val crossJs  = cross.js

lazy val publishSettings = Seq(
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    homepage := Some(url("http://www.brights.com")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value)
            Some("snapshots" at nexus + "content/repositories/snapshots")
        else
            Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    autoAPIMappings := true,
    apiURL := Some(url(s"https://marcesquerra.github.io/$nameLiteral/api/")),
    scmInfo := Some(
        ScmInfo(
            url(s"https://github.com/marcesquerra/$nameLiteral"),
            s"scm:git:git@github.com:marcesquerra/$nameLiteral.git"
        )
    ),
    pomExtra := (
        <licenses>
            <license>
                <name>mit</name>
            </license>
        </licenses>
            <developers>
                <developer>
                    <name>Marc Esquerrà i Bayo</name>
                    <email>esquerra@bryghts.com</email>
                </developer>
            </developers>
        )
)

lazy val noPublishSettings = Seq(
    publish := (),
    publishLocal := (),
    publishArtifact := false
)

lazy val sharedReleaseProcess = Seq(
    releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        publishArtifacts,
        setNextVersion,
        commitNextVersion,
        pushChanges
    )
)

credentials ++= (
    for {
        username <- Option(System.getenv().get("SONATYPE_USERNAME"))
        password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
    } yield Credentials(
        "Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        username,
        password
    )
    ).toSeq

val jvmProjects = Seq(
    "crossJvm"
)

val jsProjects = Seq(
    "crossJs"
)

addCommandAlias("buildJVM", jvmProjects.map(";" + _ + "/compile").mkString)
addCommandAlias("validateJVM", ";buildJVM;tests/test;unidoc")
addCommandAlias("buildJS", jsProjects.map(";" + _ + "/compile").mkString)
addCommandAlias("validateJS", ";buildJS;testsJS/test")
addCommandAlias("validate", ";validateJVM;validateJS")

