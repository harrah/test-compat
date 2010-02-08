/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Mark Harrah
 */
package sbt.impl

import org.scalatools.testing._

case class TestEvent(testName: String, result: Result, error: Throwable) extends Event
{
	def description = testName
}