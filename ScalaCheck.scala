package sbt.impl

import org.scalatools.testing._

class ScalaCheckFramework extends Framework
{
	val name = "ScalaCheck"
	val tests = Array[TestFingerprint](propertiesFingerprint)

	def testRunner(loader: ClassLoader,  loggers: Array[Logger]): Runner = new ScalaCheckRunner(loader, loggers)

	private def propertiesFingerprint =
		new TestFingerprint {
			val superClassName = "org.scalacheck.Properties"
			val isModule = true
		}
}

/** The test runner for ScalaCheck tests. */
private class ScalaCheckRunner(loader: ClassLoader, loggers: Array[Logger]) extends Runner
{
	import org.scalacheck.{Pretty, Properties, Test}
	def run(testClassName: String, fingerprint: TestFingerprint, handler: EventHandler, args: Array[String])
	{
		val test = Module(testClassName, loader).asInstanceOf[Properties]
		Test.checkProperties(test, Test.defaultParams, propReport, testReport(handler))
	}
	private def propReport(pName: String, s: Int, d: Int) {}
	private def testReport(handler: EventHandler)(pName: String, res: Test.Result) =
	{
		lazy val msg = (if (res.passed) "+ " else "! ") + pName + ": " + pretty(res)
		lazy val log: Logger => Unit = if(res.passed) _ info msg else _ error msg
		loggers.foreach(log)

		val result =
			res.status match
			{
				case Test.Passed | _: Test.Proved => Result.Success
				case _:Test.Failed => Result.Error
				case Test.Exhausted => Result.Skipped
				case _:Test.PropException | _: Test.GenException => Result.Failure
			}
		val exception =
			res.status match
			{
				case Test.PropException(_, e, _) => e
				case Test.GenException(e) => e
				case _ => null
			}
		handler.handle(new TestEvent(pName, result, exception))
	}
	private def pretty(res: Test.Result): String =
	{
		try { pretty1_5(res) }
		catch { case e: NoSuchMethodError => pretty1_6(res) }
	}
	private def pretty1_5(res: Test.Result): String = Pretty.pretty(res)
	private def pretty1_6(res: Test.Result): String =
	{
		// the following is equivalent to: (Pretty.prettyTestRes(res))(Pretty.defaultParams)
		// and is necessary because of binary incompatibility in Pretty between ScalaCheck 1.5 and 1.6
		val loader = getClass.getClassLoader
		val prettyObj = Module("org.scalacheck.Pretty", loader)
		val prettyInst = prettyObj.getClass.getMethod("prettyTestRes", classOf[Test.Result]).invoke(prettyObj, res)
		val defaultParams = prettyObj.getClass.getMethod("defaultParams").invoke(prettyObj)
		prettyInst.getClass.getMethod("apply", Class.forName("org.scalacheck.Pretty$Params", true, loader)).invoke(prettyInst, defaultParams).toString
	}
}
object Module
{
	def apply(name: String, loader: ClassLoader): AnyRef =
	{
		val obj = Class.forName(name + "$", true, loader)
		obj.getField("MODULE$").get(null)
	}
}