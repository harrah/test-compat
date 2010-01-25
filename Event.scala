package sbt.impl

import org.scalatools.testing._

case class TestEvent(testName: String, result: Result, error: Throwable) extends Event
{
	def description = testName
}