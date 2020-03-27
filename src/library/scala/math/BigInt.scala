/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala
package math

import java.math.BigInteger
import scala.language.implicitConversions
import scala.collection.immutable.NumericRange

object BigInt {

  private[this] val minCached = -1024
  private[this] val maxCached = 1024
  private[this] val cache = new Array[BigInt](maxCached - minCached + 1)
  private val minusOne = BigInteger.valueOf(-1)

  /** Constructs a `BigInt` whose value is equal to that of the
   *  specified integer value.
   *
   *  @param i the specified integer value
   *  @return  the constructed `BigInt`
   */
  def apply(i: Int): BigInt =
    if (minCached <= i && i <= maxCached) {
      val offset = i - minCached
      var n = cache(offset)
      if (n eq null) { n = new BigInt(BigInteger.valueOf(i.toLong)); cache(offset) = n }
      n
    } else new BigInt(BigInteger.valueOf(i.toLong))

  /** Constructs a `BigInt` whose value is equal to that of the
   *  specified long value.
   *
   *  @param l the specified long value
   *  @return  the constructed `BigInt`
   */
  def apply(l: Long): BigInt =
    if (minCached <= l && l <= maxCached) apply(l.toInt)
    else new BigInt(BigInteger.valueOf(l))

  /** Translates a byte array containing the two's-complement binary
   *  representation of a BigInt into a BigInt.
   */
  def apply(x: Array[Byte]): BigInt =
    new BigInt(new BigInteger(x))

  /** Translates the sign-magnitude representation of a BigInt into a BigInt.
   *
   * @param  signum    signum of the number (-1 for negative, 0 for zero, 1
   *                   for positive).
   * @param  magnitude big-endian binary representation of the magnitude of
   *                   the number.
   */
  def apply(signum: Int, magnitude: Array[Byte]): BigInt =
    new BigInt(new BigInteger(signum, magnitude))

  /** Constructs a randomly generated positive BigInt that is probably prime,
   *  with the specified bitLength.
   */
  def apply(bitlength: Int, certainty: Int, rnd: scala.util.Random): BigInt =
    new BigInt(new BigInteger(bitlength, certainty, rnd.self))

  /** Constructs a randomly generated BigInt, uniformly distributed over the
   *  range `0` to `(2 ^ numBits - 1)`, inclusive.
   */
  def apply(numbits: Int, rnd: scala.util.Random): BigInt =
    new BigInt(new BigInteger(numbits, rnd.self))

  /** Translates the decimal String representation of a BigInt into a BigInt.
   */
  def apply(x: String): BigInt =
    new BigInt(new BigInteger(x))

  /** Translates the string representation of a `BigInt` in the
   *  specified `radix` into a BigInt.
   */
  def apply(x: String, radix: Int): BigInt =
    new BigInt(new BigInteger(x, radix))

  /** Translates a `java.math.BigInteger` into a BigInt.
   */
  def apply(x: BigInteger): BigInt =
    new BigInt(x)

  /** Returns a positive BigInt that is probably prime, with the specified bitLength.
   */
  def probablePrime(bitLength: Int, rnd: scala.util.Random): BigInt =
    new BigInt(BigInteger.probablePrime(bitLength, rnd.self))

  /** Implicit conversion from `Int` to `BigInt`.
   */
  implicit def int2bigInt(i: Int): BigInt = apply(i)

  /** Implicit conversion from `Long` to `BigInt`.
   */
  implicit def long2bigInt(l: Long): BigInt = apply(l)

  /** Implicit conversion from `java.math.BigInteger` to `scala.BigInt`.
   */
  implicit def javaBigInteger2bigInt(x: BigInteger): BigInt = apply(x)
}

final class BigInt(val bigInteger: BigInteger)
  extends ScalaNumber
    with ScalaNumericConversions
    with Serializable
    with Ordered[BigInt]
{
  /** Returns the hash code for this BigInt. */
  override def hashCode(): Int =
    if (isValidLong) unifiedPrimitiveHashcode
    else bigInteger.##

  /** Compares this BigInt with the specified value for equality.
   */
  override def equals(that: Any): Boolean = that match {
    case that: BigInt     => this equals that
    case that: BigDecimal => that equals this
    case that: Double     => isValidDouble && toDouble == that
    case that: Float      => isValidFloat && toFloat == that
    case x                => isValidLong && unifiedPrimitiveEquals(x)
  }
  override def isValidByte: Boolean = this >= Byte.MinValue && this <= Byte.MaxValue
  override def isValidShort: Boolean = this >= Short.MinValue && this <= Short.MaxValue
  override def isValidChar: Boolean = this >= Char.MinValue && this <= Char.MaxValue
  override def isValidInt: Boolean = this >= Int.MinValue && this <= Int.MaxValue
           def isValidLong: Boolean = this >= Long.MinValue && this <= Long.MaxValue
  /** Returns `true` iff this can be represented exactly by [[scala.Float]]; otherwise returns `false`.
    */
  def isValidFloat: Boolean = {
    val bitLen = bitLength
    (bitLen <= 24 ||
      {
        val lowest = lowestSetBit
        bitLen <= java.lang.Float.MAX_EXPONENT + 1 && // exclude this < -2^128 && this >= 2^128
        lowest >= bitLen - 24 &&
        lowest < java.lang.Float.MAX_EXPONENT + 1 // exclude this == -2^128
      }
    ) && !bitLengthOverflow
  }
  /** Returns `true` iff this can be represented exactly by [[scala.Double]]; otherwise returns `false`.
    */
  def isValidDouble: Boolean = {
    val bitLen = bitLength
    (bitLen <= 53 ||
      {
        val lowest = lowestSetBit
        bitLen <= java.lang.Double.MAX_EXPONENT + 1 && // exclude this < -2^1024 && this >= 2^1024
        lowest >= bitLen - 53 &&
        lowest < java.lang.Double.MAX_EXPONENT + 1 // exclude this == -2^1024
      }
    ) && !bitLengthOverflow
  }
  /** Some implementations of java.math.BigInteger allow huge values with bit length greater than Int.MaxValue.
   * The BigInteger.bitLength method returns truncated bit length in this case.
   * This method tests if result of bitLength is valid.
   * This method will become unnecessary if BigInt constructors reject huge BigIntegers.
   */
  private def bitLengthOverflow = {
    val shifted = bigInteger.shiftRight(Int.MaxValue)
    (shifted.signum != 0) && !(shifted equals BigInt.minusOne)
  }

  def isWhole: Boolean = true
  def underlying: BigInteger = bigInteger

  /** Compares this BigInt with the specified BigInt for equality.
   */
  def equals (that: BigInt): Boolean = compare(that) == 0

  /** Compares this BigInt with the specified BigInt
   */
  def compare (that: BigInt): Int = this.bigInteger.compareTo(that.bigInteger)

  /** Addition of BigInts
   */
  def +  (that: BigInt): BigInt = new BigInt(this.bigInteger.add(that.bigInteger))

  /** Subtraction of BigInts
   */
  def -  (that: BigInt): BigInt = new BigInt(this.bigInteger.subtract(that.bigInteger))

  /** Multiplication of BigInts
   */
  def *  (that: BigInt): BigInt = new BigInt(this.bigInteger.multiply(that.bigInteger))

  /** Division of BigInts
   */
  def /  (that: BigInt): BigInt = new BigInt(this.bigInteger.divide(that.bigInteger))

  /** Remainder of BigInts
   */
  def %  (that: BigInt): BigInt = new BigInt(this.bigInteger.remainder(that.bigInteger))

  /** Returns a pair of two BigInts containing (this / that) and (this % that).
   */
  def /% (that: BigInt): (BigInt, BigInt) = {
    val dr = this.bigInteger.divideAndRemainder(that.bigInteger)
    (new BigInt(dr(0)), new BigInt(dr(1)))
  }

  /** Leftshift of BigInt
   */
  def << (n: Int): BigInt = new BigInt(this.bigInteger.shiftLeft(n))

  /** (Signed) rightshift of BigInt
   */
  def >> (n: Int): BigInt = new BigInt(this.bigInteger.shiftRight(n))

  /** Bitwise and of BigInts
   */
  def &  (that: BigInt): BigInt = new BigInt(this.bigInteger.and(that.bigInteger))

  /** Bitwise or of BigInts
   */
  def |  (that: BigInt): BigInt = new BigInt(this.bigInteger.or (that.bigInteger))

  /** Bitwise exclusive-or of BigInts
   */
  def ^  (that: BigInt): BigInt = new BigInt(this.bigInteger.xor(that.bigInteger))

  /** Bitwise and-not of BigInts. Returns a BigInt whose value is (this & ~that).
   */
  def &~ (that: BigInt): BigInt = new BigInt(this.bigInteger.andNot(that.bigInteger))

  /** Returns the greatest common divisor of abs(this) and abs(that)
   */
  def gcd (that: BigInt): BigInt = new BigInt(this.bigInteger.gcd(that.bigInteger))

  /** Returns a BigInt whose value is (this mod that).
   *  This method differs from `%` in that it always returns a non-negative BigInt.
   *  @param that A positive number
   */
  def mod (that: BigInt): BigInt = new BigInt(this.bigInteger.mod(that.bigInteger))

  /** Returns the minimum of this and that
   */
  def min (that: BigInt): BigInt = new BigInt(this.bigInteger.min(that.bigInteger))

  /** Returns the maximum of this and that
   */
  def max (that: BigInt): BigInt = new BigInt(this.bigInteger.max(that.bigInteger))

  /** Returns a BigInt whose value is (<tt>this</tt> raised to the power of <tt>exp</tt>).
   */
  def pow (exp: Int): BigInt = new BigInt(this.bigInteger.pow(exp))

  /** Returns a BigInt whose value is
   *  (<tt>this</tt> raised to the power of <tt>exp</tt> modulo <tt>m</tt>).
   */
  def modPow (exp: BigInt, m: BigInt): BigInt =
    new BigInt(this.bigInteger.modPow(exp.bigInteger, m.bigInteger))

  /** Returns a BigInt whose value is (the inverse of <tt>this</tt> modulo <tt>m</tt>).
   */
  def modInverse (m: BigInt): BigInt = new BigInt(this.bigInteger.modInverse(m.bigInteger))

  /** Returns a BigInt whose value is the negation of this BigInt
   */
  def unary_- : BigInt   = new BigInt(this.bigInteger.negate())

  /** Returns the absolute value of this BigInt
   */
  def abs: BigInt = new BigInt(this.bigInteger.abs())

  /** Returns the sign of this BigInt;
   *   -1 if it is less than 0,
   *   +1 if it is greater than 0,
   *   0  if it is equal to 0.
   */
  def signum: Int = this.bigInteger.signum()

  /** Returns the sign of this BigInt;
   *   -1 if it is less than 0,
   *   +1 if it is greater than 0,
   *   0  if it is equal to 0.
   */
  def sign: BigInt = signum

  /** Returns the bitwise complement of this BigInt
   */
  def unary_~ : BigInt = new BigInt(this.bigInteger.not())

  /** Returns true if and only if the designated bit is set.
   */
  def testBit (n: Int): Boolean = this.bigInteger.testBit(n)

  /** Returns a BigInt whose value is equivalent to this BigInt with the designated bit set.
   */
  def setBit  (n: Int): BigInt  = new BigInt(this.bigInteger.setBit(n))

  /** Returns a BigInt whose value is equivalent to this BigInt with the designated bit cleared.
   */
  def clearBit(n: Int): BigInt  = new BigInt(this.bigInteger.clearBit(n))

  /** Returns a BigInt whose value is equivalent to this BigInt with the designated bit flipped.
   */
  def flipBit (n: Int): BigInt  = new BigInt(this.bigInteger.flipBit(n))

  /** Returns the index of the rightmost (lowest-order) one bit in this BigInt
   * (the number of zero bits to the right of the rightmost one bit).
   */
  def lowestSetBit: Int         = this.bigInteger.getLowestSetBit()

  /** Returns the number of bits in the minimal two's-complement representation of this BigInt,
   *  excluding a sign bit.
   */
  def bitLength: Int            = this.bigInteger.bitLength()

  /** Returns the number of bits in the two's complement representation of this BigInt
   *  that differ from its sign bit.
   */
  def bitCount: Int             = this.bigInteger.bitCount()

  /** Returns true if this BigInt is probably prime, false if it's definitely composite.
   *  @param certainty  a measure of the uncertainty that the caller is willing to tolerate:
   *                    if the call returns true the probability that this BigInt is prime
   *                    exceeds (1 - 1/2 ^ certainty).
   *                    The execution time of this method is proportional to the value of
   *                    this parameter.
   */
  def isProbablePrime(certainty: Int): Boolean = this.bigInteger.isProbablePrime(certainty)

  /** Converts this BigInt to a <tt>byte</tt>.
   *  If the BigInt is too big to fit in a byte, only the low-order 8 bits are returned.
   *  Note that this conversion can lose information about the overall magnitude of the
   *  BigInt value as well as return a result with the opposite sign.
   */
  override def byteValue: Byte = intValue.toByte

  /** Converts this BigInt to a <tt>short</tt>.
   *  If the BigInt is too big to fit in a short, only the low-order 16 bits are returned.
   *  Note that this conversion can lose information about the overall magnitude of the
   *  BigInt value as well as return a result with the opposite sign.
   */
  override def shortValue: Short = intValue.toShort

  /** Converts this BigInt to a <tt>char</tt>.
   *  If the BigInt is too big to fit in a char, only the low-order 16 bits are returned.
   *  Note that this conversion can lose information about the overall magnitude of the
   *  BigInt value and that it always returns a positive result.
   */
  def charValue: Char = intValue.toChar

  /** Converts this BigInt to an <tt>int</tt>.
   *  If the BigInt is too big to fit in an int, only the low-order 32 bits
   *  are returned. Note that this conversion can lose information about the
   *  overall magnitude of the BigInt value as well as return a result with
   *  the opposite sign.
   */
  def intValue: Int = this.bigInteger.intValue

  /** Converts this BigInt to a <tt>long</tt>.
   *  If the BigInt is too big to fit in a long, only the low-order 64 bits
   *  are returned. Note that this conversion can lose information about the
   *  overall magnitude of the BigInt value as well as return a result with
   *  the opposite sign.
   */
  def longValue: Long = this.bigInteger.longValue

  /** Converts this `BigInt` to a `float`.
   *  If this `BigInt` has too great a magnitude to represent as a float,
   *  it will be converted to `Float.NEGATIVE_INFINITY` or
   *  `Float.POSITIVE_INFINITY` as appropriate.
   */
  def floatValue: Float = this.bigInteger.floatValue

  /** Converts this `BigInt` to a `double`.
   *  if this `BigInt` has too great a magnitude to represent as a double,
   *  it will be converted to `Double.NEGATIVE_INFINITY` or
   *  `Double.POSITIVE_INFINITY` as appropriate.
   */
  def doubleValue: Double = this.bigInteger.doubleValue

  /** Create a `NumericRange[BigInt]` in range `[start;end)`
   *  with the specified step, where start is the target BigInt.
   *
   *  @param end    the end value of the range (exclusive)
   *  @param step   the distance between elements (defaults to 1)
   *  @return       the range
   */
  def until(end: BigInt, step: BigInt = BigInt(1)): NumericRange.Exclusive[BigInt] = Range.BigInt(this, end, step)

  /** Like until, but inclusive of the end value.
   */
  def to(end: BigInt, step: BigInt = BigInt(1)): NumericRange.Inclusive[BigInt] = Range.BigInt.inclusive(this, end, step)

  /** Returns the decimal String representation of this BigInt.
   */
  override def toString(): String = this.bigInteger.toString()

  /** Returns the String representation in the specified radix of this BigInt.
   */
  def toString(radix: Int): String = this.bigInteger.toString(radix)

  /** Returns a byte array containing the two's-complement representation of
   *  this BigInt. The byte array will be in big-endian byte-order: the most
   *  significant byte is in the zeroth element. The array will contain the
   *  minimum number of bytes required to represent this BigInt, including at
   *  least one sign bit.
   */
  def toByteArray: Array[Byte] = this.bigInteger.toByteArray()
}
