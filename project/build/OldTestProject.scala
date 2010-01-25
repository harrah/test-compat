import sbt._

class OldTestProject(info: ProjectInfo) extends DefaultProject(info)
{
	val testInterface = "org.scala-tools.testing" % "test-interface" % "0.4" % "provided"
	val scalacheck = "org.scala-tools.testing" % "scalacheck" % "1.5" % "provided"
	val scalatest = "org.scalatest" % "scalatest" % "1.0" % "provided"
	val specs = "org.scala-tools.testing" % "specs" % "1.6.0" % "provided"
	override def scratch = true
}