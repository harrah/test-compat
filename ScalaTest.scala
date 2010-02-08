/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Josh Cough, Mark Harrah
 */
package sbt.impl

import org.scalatools.testing._

class ScalaTestFramework extends Framework
{
	val name = "ScalaTest"
	val tests = Fingerprint.classOnly("org.scalatest.Suite")

	def testRunner(loader: ClassLoader,  loggers: Array[Logger]): Runner = new ScalaTestRunner(loader, loggers)
}

/** The test runner for ScalaTest tests. Based on Josh Cough's translation of sbt's original runner.*/
private class ScalaTestRunner(loader: ClassLoader, loggers: Array[Logger]) extends Runner
{
	def run(testClassName: String, fingerprint: TestFingerprint, handler: EventHandler, args: Array[String])
	{
		def result(r: Result) { handler.handle(new TestEvent(testClassName, r, null)) }
		import org.scalatest.{Filter, Stopper, Suite, Tracker}
		val test = Load.`class`(testClassName, loader).asInstanceOf[Suite]
		val reporter = new ScalaTestReporter
		test.run(None, reporter, new Stopper {}, Filter(), Map.empty, None, new Tracker)
		result( if(reporter.succeeded) Result.Success else Result.Failure )
	}

	private class ScalaTestReporter extends org.scalatest.Reporter with NotNull
	{
		import org.scalatest.events._
		def apply(event: Event)
		{
			event match
			{
				case _: RunCompleted => info("Run completed.")
				case _: RunStarting => info("Run starting")
				case _: RunStopped => error("Run stopped")
				case _: RunAborted => error("Run aborted")
				case ts: TestStarting => info(ts.testName, "Test Starting", None)
				case _: TestPending =>
				case tf: TestFailed => error(tf.testName, "Test Failed", None, tf.throwable)
				case ts: TestSucceeded => info(ts.testName, "Test Succeeded", None)
				case ti: TestIgnored => info(ti.testName, "Test Ignored", None)
				case sc: SuiteCompleted => info(sc.suiteName, "Suite Completed", None)
				case sa: SuiteAborted => error(sa.suiteName, "Suite Aborted", Some(sa.message), sa.throwable)
				case ss: SuiteStarting => info(ss.suiteName, "Suite Starting", None)
				case ip: InfoProvided => info(ip.message)
			}
		}
		def info(name: String, event: String, message: Option[String]): Unit = info(messageString(name, event, message))
		def error(name: String, event: String, message: Option[String], t: Option[Throwable])
		{
			succeeded = false
			t.foreach(trace)
			error(messageString(name, event, message))
		}

		private def messageString(name: String, event: String, message: Option[String]) = event + " - " + name + withMessage(message)
		private def withMessage(message: Option[String]) =
		{
			val trimmed = message.map(_.trim).getOrElse("")
			if(trimmed.isEmpty) "" else ": " + trimmed
		}

		private def info(msg: String): Unit = loggers.foreach(_.info(msg))
		private def trace(t: Throwable): Unit = loggers.foreach(_.trace(t))
		private def error(msg: String): Unit =
		{
			succeeded = false
			loggers.foreach(_.error(msg))
		}

		var succeeded = true
	}

}