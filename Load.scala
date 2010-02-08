/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Mark Harrah
 */
package sbt.impl

import org.scalatools.testing.TestFingerprint

object Load
{
	def module(name: String, loader: ClassLoader): AnyRef =
	{
		val obj = Class.forName(name + "$", true, loader)
		obj.getField("MODULE$").get(null)
	}
	def `class`(name: String, loader: ClassLoader): AnyRef =
	{
		val testClass = Class.forName(name, true, loader)
		testClass.newInstance.asInstanceOf[AnyRef]
	}
	def apply(name: String, loader: ClassLoader, fingerprint: TestFingerprint): AnyRef =
		if(fingerprint.isModule) module(name, loader) else `class`(name, loader)
}