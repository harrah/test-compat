package sbt.impl

import org.scalatools.testing._

object Fingerprint
{
	def moduleOnly(className: String) = Array[TestFingerprint](moduleFingerprint(className))
	def classOnly(className: String) = Array[TestFingerprint](classFingerprint(className))
	def moduleAndClass(className: String) = Array[TestFingerprint](classFingerprint(className), moduleFingerprint(className))

	def classFingerprint(className: String) = fingerprint(className, false)
	def moduleFingerprint(className: String) = fingerprint(className, true)

	def fingerprint(className: String, isModuleA: Boolean): TestFingerprint =
		new TestFingerprint {
			val superClassName = className
			val isModule = isModuleA
		}
}