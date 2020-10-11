organization in ThisBuild := "com.surajgharat.practice.grpc"

version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.13.1"

lazy val main = (project in file(".")).settings(
  libraryDependencies ++= Seq(
    "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
  )
)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
)

PB.protoSources in Compile := Seq(sourceDirectory.value / "main/proto")
