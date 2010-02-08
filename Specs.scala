/* sbt -- Simple Build Tool
 * Copyright 2009, 2010 Mark Harrah
* Copyright 2007-2008 Eric Torreborre.
 */
package sbt.impl

import org.scalatools.testing._

class SpecsFramework extends Framework
{
	val name = "specs"
	val tests = Fingerprint.moduleAndClass("org.specs.Specification")

	def testRunner(loader: ClassLoader,  loggers: Array[Logger]): Runner = new SpecsRunner(loader, loggers)
}

/** The test runner for specs tests. */
private class SpecsRunner(loader: ClassLoader, loggers: Array[Logger]) extends Runner
{
	import org.specs.Specification
	import org.specs.specification.{Example, Sus}

	def run(testClassName: String, fingerprint: TestFingerprint, handler: EventHandler, args: Array[String])
	{
		val test = Load(testClassName, loader, fingerprint).asInstanceOf[Specification]
		val output = new SpecsOutput(loggers)
		output.output(reportSpecification(test))
		handler.handle(new TestEvent(testClassName, if(output.succeeded) Result.Success else Result.Failure, null))
	}

	/* The following is closely based on org.specs.runner.OutputReporter,
	* part of specs, which is Copyright 2007-2008 Eric Torreborre.
	* */

	private def reportSpecification(spec: Specification): SpecificationReportEvent =
	{
		return SpecificationReportEvent(spec.successes.size, spec.failures.size, spec.errors.size, spec.skipped.size, spec.pretty,
			reportSystems(spec.systems), reportSpecifications(spec.subSpecifications))
	}
	private def reportSpecifications(specifications: Seq[Specification]): Seq[SpecificationReportEvent] =
		specifications map reportSpecification
	private def reportSystems(systems: Seq[Sus]): Seq[SystemReportEvent] =
		systems map reportSystem
	private def reportSystem(sus: Sus): SystemReportEvent =
	{
		val litD = sus.literateDescription
		val format = litD.map(_.desc.map(_.text))
		SystemReportEvent(sus.description, sus.verb, sus.skipped, format, reportExamples(sus.specification.examples))
	}
	private def reportExamples(examples: Seq[Example]): Seq[ExampleReportEvent] =
		examples map reportExample
	private def reportExample(example: Example): ExampleReportEvent =
		ExampleReportEvent(example.description, example.errors, example.failures, example.skipped, reportExamples(example.examples))
}

class SpecsOutput(loggers: Seq[Logger]) extends NotNull
{
	private val Indent = "  "
	var succeeded = true

	private def info(msg: String): Unit = loggers.foreach(_.info(msg))
	private def trace(t: Throwable): Unit = loggers.foreach(_.trace(t))
	private def warn(msg: String): Unit = loggers.foreach(_.warn(msg))
	private def error(msg: String): Unit =
	{
		succeeded = false
		loggers.foreach(_.error(msg))
	}

	def output(event: SpecsEvent) = event match
		{
			case sre: SpecificationReportEvent => reportSpecification(sre, "")
			case sre: SystemReportEvent => reportSystem(sre, "")
			case ere: ExampleReportEvent => reportExample(ere, "")
		}

	/* The following is closely based on org.specs.runner.OutputReporter,
	* part of specs, which is Copyright 2007-2008 Eric Torreborre.
	* */

	private def reportSpecification(specification: SpecificationReportEvent, padding: String)
	{
		val newIndent = padding + Indent
		reportSpecifications(specification.subSpecs, newIndent)
		reportSystems(specification.systems, newIndent)
	}
	private def reportSpecifications(specifications: Iterable[SpecificationReportEvent], padding: String)
	{
		for(specification <- specifications)
			reportSpecification(specification, padding)
	}
	private def reportSystems(systems: Iterable[SystemReportEvent], padding: String)
	{
		for(system <- systems)
			reportSystem(system, padding)
	}
	private def reportSystem(sus: SystemReportEvent, padding: String)
	{
		val skipped = if(sus.skipped.isEmpty) "" else sus.skipped.map(_.getMessage).mkString(" (skipped: ", ", ", ")")
		info(padding + sus.description + " " + sus.verb + skipped)
		for(description <- sus.literateDescription)
			info(padding + description.mkString)
		reportExamples(sus.examples, padding)
		info(" ")
	}
	private def reportExamples(examples: Iterable[ExampleReportEvent], padding: String)
	{
		for(example <- examples)
		{
			reportExample(example, padding)
			reportExamples(example.subExamples, padding + Indent)
		}
	}
	private def status(example: ExampleReportEvent) =
	{
		if (example.errors.size + example.failures.size > 0)
			"x "
		else if (example.skipped.size > 0)
			"o "
		else
			"+ "
	}
	private def reportExample(example: ExampleReportEvent, padding: String)
	{
		info(padding + status(example) + example.description)
		for(skip <- example.skipped)
		{
			trace(skip)
			warn(padding + skip.toString)
		}
		for(e <- example.failures ++ example.errors)
		{
			trace(e)
			error(padding + e.toString)
		}
	}
}


sealed trait SpecsEvent extends NotNull
{
	def result: Option[Result]
}
final case class SpecificationReportEvent(successes: Int, failures: Int, errors: Int, skipped: Int, pretty: String, systems: Seq[SystemReportEvent], subSpecs: Seq[SpecificationReportEvent]) extends SpecsEvent
{
	def result = if(errors > 0) Some(Result.Error) else if(failures > 0) Some(Result.Failure) else Some(Result.Success)
}
final case class SystemReportEvent(description: String, verb: String, skipped: Seq[Throwable], literateDescription: Option[Seq[String]], examples: Seq[ExampleReportEvent]) extends SpecsEvent { def result = None }
final case class ExampleReportEvent(description: String, errors: Seq[Throwable], failures: Seq[RuntimeException], skipped: Seq[RuntimeException], subExamples: Seq[ExampleReportEvent]) extends SpecsEvent { def result = None }