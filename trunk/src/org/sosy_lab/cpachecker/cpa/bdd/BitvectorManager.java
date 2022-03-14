// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.math.IntMath;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;

/**
 * The BitvectorManager computes more expensive (arithmetic) operations based on bitvectors from
 * bits (predicates, regions) from the given {@link RegionManager}.
 *
 * <p>This class is thread-safe, iff the delegated {@link RegionManager} is thread-safe.
 */
public class BitvectorManager {

  private final RegionManager rmgr;

  public BitvectorManager(RegionManager pRmgr) {
    rmgr = pRmgr;
  }

  private int getBitSize(Region[] r1, Region[] r2) {
    assert r1.length == r2.length : "bitvectors must have equal length";
    return r1.length;
  }

  /** returns OR of regions */
  public Region makeOr(Region[] regions) {
    Region tmp = rmgr.makeFalse(); // neutral for OR
    for (Region r : regions) {
      tmp = rmgr.makeOr(tmp, r);
    }
    return tmp;
  }

  /** returns bitRepresentation of number, 5 --> 00101 --> [0,0,1,0,1] */
  public Region[] makeNumber(long n, int size) {
    return makeNumber(BigInteger.valueOf(n), size);
  }

  /** returns bitRepresentation of number, 5 --> 00101 --> [0,0,1,0,1] */
  public Region[] makeNumber(BigInteger n, int size) {
    if (n.signum() == -1) {
      n = BigInteger.ONE.shiftLeft(size).add(n); // -1 == (1<<32) -1 --> 2-complement-representation
    }
    Region[] newRegions = new Region[size];
    for (int i = 0; i < size; i++) {
      if (n.testBit(i)) {
        newRegions[i] = rmgr.makeTrue();
      } else {
        newRegions[i] = rmgr.makeFalse();
      }
    }
    return newRegions;
  }

  /** 1100 --> 0000, 0000 --> 0001 */
  public Region makeNot(Region... regions) {
    Region tmp = rmgr.makeFalse(); // neutral region for OR
    for (Region r : regions) {
      tmp = rmgr.makeOr(tmp, r);
    }
    return rmgr.makeNot(tmp);
  }

  /** 1100 & 1010 --> 1000 */
  public Region[] makeBinaryAnd(Region[] r1, Region[] r2) {
    int bitsize = getBitSize(r1, r2);

    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      newRegions[i] = rmgr.makeAnd(r1[i], r2[i]);
    }
    return newRegions;
  }

  /** 1100 && 1010 --> 0001 */
  @Deprecated // logical AND (&&) is not allowed in the AST any more
  public Region makeLogicalAnd(Region[] r1, Region[] r2) {
    int bitsize = getBitSize(r1, r2);

    Region tmp1 = rmgr.makeFalse(); // neutral region for OR
    Region tmp2 = rmgr.makeFalse();
    for (int i = 0; i < bitsize; i++) {
      tmp1 = rmgr.makeOr(tmp1, r1[i]);
      tmp2 = rmgr.makeOr(tmp2, r2[i]);
    }
    return rmgr.makeAnd(tmp1, tmp2);
  }

  /** 1100 | 1010 --> 1110 */
  public Region[] makeBinaryOr(Region[] r1, Region[] r2) {
    int bitsize = getBitSize(r1, r2);

    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      newRegions[i] = rmgr.makeOr(r1[i], r2[i]);
    }
    return newRegions;
  }

  /** 1100 || 1010 --> 0001 */
  @Deprecated // logical OR (||) is not allowed in the AST any more
  public Region makeLogicalOr(Region[] r1, Region[] r2) {
    int bitsize = getBitSize(r1, r2);

    Region tmp1 = rmgr.makeFalse(); // neutral region for OR
    Region tmp2 = rmgr.makeFalse();
    for (int i = 0; i < bitsize; i++) {
      tmp1 = rmgr.makeOr(tmp1, r1[i]);
      tmp2 = rmgr.makeOr(tmp2, r2[i]);
    }
    return rmgr.makeOr(tmp1, tmp2);
  }

  /** 1100 <==> 1010 --> 1001 */
  public Region[] makeBinaryEqual(Region[] r1, Region[] r2) {
    int bitsize = getBitSize(r1, r2);

    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      newRegions[i] = rmgr.makeEqual(r1[i], r2[i]);
    }
    return newRegions;
  }

  /** 1100 == 1010 --> 0000 */
  public Region makeLogicalEqual(Region[] r1, Region[] r2) {
    int bitsize = getBitSize(r1, r2);

    Region equality = rmgr.makeTrue();
    for (int i = 0; i < bitsize; i++) {
      Region equalPos = rmgr.makeEqual(r1[i], r2[i]);
      equality = rmgr.makeAnd(equality, equalPos);
    }
    return equality;
  }

  /** 1100 ^ 1010 --> 0110 */
  public Region[] makeXor(Region[] r1, Region[] r2) {
    int bitsize = getBitSize(r1, r2);

    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      newRegions[i] = rmgr.makeUnequal(r1[i], r2[i]);
    }
    return newRegions;
  }

  /** 0011 + 0110 --> 1001 */
  public Region[] makeAdd(Region[] r1, Region[] r2) {
    Region carrier = rmgr.makeFalse();
    return fullAdder(r1, r2, carrier);
  }

  /** 0111 - 0011 --> 0100 */
  public Region[] makeSub(Region[] r1, Region[] r2) {
    int bitsize = getBitSize(r1, r2);

    // build the binary complement of the second operand (+5:0101 --> -5:1011) with two steps:
    //   1) invert all bits: 0101<==>0000 --> 1010
    //   2) add '1' by setting carrier to 'true'
    Region[] r2tmp = makeBinaryEqual(r2, makeNumber(BigInteger.ZERO, bitsize));
    Region carrier = rmgr.makeTrue();
    return fullAdder(r1, r2tmp, carrier);
  }

  private Region[] fullAdder(Region[] r1, Region[] r2, Region carrier) {
    int bitsize = getBitSize(r1, r2);

    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {

      // first half-adder
      Region xor = rmgr.makeUnequal(r1[i], r2[i]);
      Region and = rmgr.makeAnd(r1[i], r2[i]);

      // second half-adder
      newRegions[i] = rmgr.makeUnequal(carrier, xor);
      Region tmp = rmgr.makeAnd(carrier, xor);

      // carrier for next position
      carrier = rmgr.makeOr(tmp, and);
    }
    return newRegions;
  }

  /** A<B */
  public Region makeLess(final Region[] A, final Region[] B, final boolean signed) {
    return makeLess(A, B, false, signed);
  }

  /** A<=B */
  public Region makeLessOrEqual(final Region[] A, final Region[] B, final boolean signed) {
    return makeLess(A, B, true, signed);
  }

  /** returns A<=B if equal is true else A<B */
  private Region makeLess(
      final Region[] A, final Region[] B, final boolean equal, final boolean signed) {
    final int bitsize = getBitSize(A, B);
    Region less = equal ? rmgr.makeTrue() : rmgr.makeFalse();

    for (int i = 0; i < (signed ? bitsize - 1 : bitsize); i++) {
      final Region lessFirst = rmgr.makeAnd(rmgr.makeNot(A[i]), B[i]);
      final Region equalFirst = rmgr.makeEqual(A[i], B[i]);
      less = rmgr.makeOr(lessFirst, rmgr.makeAnd(equalFirst, less));
    }

    if (signed) { // if signed, invert first bit, because here 1 is less than 0.
      final int firstPos = bitsize - 1;
      final Region invLessFirst = rmgr.makeAnd(A[firstPos], rmgr.makeNot(B[firstPos]));
      final Region equalFirst = rmgr.makeEqual(A[firstPos], B[firstPos]);
      less = rmgr.makeOr(invLessFirst, rmgr.makeAnd(equalFirst, less));
    }

    return less;
  }

  /**
   * 101101 << 011 --> 101000,
   *
   * <p>We only use the lower bits of r2, thus r2 can be shorter than r1. We use r2 like a positive
   * unsigned bit-vector.
   */
  public Region[] makeShiftLeft(final Region[] r1, final Region[] r2) {
    final int bitsize = r1.length;
    final int shiftsize = IntMath.log2(bitsize, RoundingMode.FLOOR) + 1;

    Region[] result = r1;
    for (int pos = 0; pos < shiftsize; pos++) {
      int shift = 1 << pos;
      Region bit = r2[pos];
      Region[] tmp = new Region[bitsize];

      // fill lower bits
      for (int i = 0; i < shift; i++) {
        tmp[i] = rmgr.makeIte(bit, rmgr.makeFalse(), result[i]);
      }
      // fill higher bits
      for (int i = shift; i < bitsize; i++) {
        tmp[i] = rmgr.makeIte(bit, result[i - shift], result[i]);
      }

      result = tmp;
    }

    return result;
  }

  /**
   * 101101 >> 011 --> signed ? 000101 : 111101,
   *
   * <p>We only use the lower bits of r2, thus r2 can be shorter than r1. We use r2 like a positive
   * unsigned bit-vector.
   */
  public Region[] makeShiftRight(final Region[] r1, final Region[] r2, final boolean signed) {
    final int bitsize = r1.length;
    final int shiftsize = IntMath.log2(bitsize, RoundingMode.FLOOR) + 1;

    Region[] result = r1;
    for (int pos = 0; pos < shiftsize; pos++) {
      int shift = 1 << pos;
      Region bit = r2[pos];
      Region[] tmp = new Region[bitsize];

      // fill higher bits
      for (int i = bitsize - 1; i >= bitsize - shift; i--) {
        tmp[i] = rmgr.makeIte(bit, signed ? result[bitsize - 1] : rmgr.makeFalse(), result[i]);
      }
      // fill lower bits
      for (int i = bitsize - shift - 1; i >= 0; i--) {
        tmp[i] = rmgr.makeIte(bit, result[i + shift], result[i]);
      }

      result = tmp;
    }

    return result;
  }

  /**
   * Simple multiplier circuit, 1101 * 0101 --> 0001, for -3*5 --> -15. Requires same input length
   * and returns same output length.
   *
   * <p>Strategy: The multiplication of AAAA (r1) and BBBB (r2) is done in a matrix. Upper- and
   * lower-case C (line) is the multiplication of bits from A and B. Upper- and lower-case D
   * (result) is the sum of rows. Lower-case c and d will be ignored and will not even be computed.
   *
   * <pre>
   * AAAA x BBBB
   * -----------
   *        CCCC
   *       cCCC
   *      ccCC
   *     cccC
   * -----------
   *    ddddDDDD
   * </pre>
   *
   * Warning: depending on variable ordering, this operation can be exponentially expensive!
   */
  public Region[] makeMult(final Region[] r1, final Region[] r2) {
    final int bitsize = getBitSize(r1, r2);

    Region[] result = new Region[bitsize];
    Arrays.fill(result, rmgr.makeFalse());

    for (int row = 0; row < bitsize; row++) {

      Region[] line = new Region[bitsize];
      // left lower triangle
      for (int i = 0; i < row; i++) {
        line[i] = rmgr.makeFalse();
      }
      // multiplication of bits
      for (int i = row; i < bitsize; i++) {
        line[i] = rmgr.makeAnd(r1[row], r2[i - row]);
      }

      result = makeAdd(result, line);
    }

    return result;
  }

  /**
   * returns the quotient of r1 and r2, rounded towards zero.
   *
   * <p>Info: - division by zero returns 0001 if signed or positive else 1111 (undefined by C99
   * standard). - signed MIN_INT divided by -1 returns MIN_INT.
   */
  public Region[] makeDiv(final Region[] r1, final Region[] r2, final boolean signed) {
    return makeDivMod(r1, r2, signed).getFirst();
  }

  /**
   * returns the remainder of r1 and r2, with same sign as r1.
   *
   * <p>Info: - modulo by zero returns r1 for signed and unsigned input (undefined by C99 standard).
   * - signed MIN_INT modulo -1 returns zero.
   */
  public Region[] makeMod(final Region[] r1, final Region[] r2, final boolean signed) {
    return makeDivMod(r1, r2, signed).getSecond();
  }

  private Pair<Region[], Region[]> makeDivMod(
      final Region[] r1, final Region[] r2, final boolean signed) {
    if (signed) {
      final int bitsize = getBitSize(r1, r2);
      final Region isDividendNegative = r1[bitsize - 1];
      final Region isDivisorNegative = r2[bitsize - 1];

      // make positive unsigned numbers
      Region[] unsignedR1 = negateIf(isDividendNegative, r1);
      Region[] unsignedR2 = negateIf(isDivisorNegative, r2);

      Pair<Region[], Region[]> divMod = makeDivModUnsigned(unsignedR1, unsignedR2);

      // make signed numbers
      // C99: quotient is negative if exactly one of the operands is negative
      Region[] quotient =
          negateIf(rmgr.makeUnequal(isDividendNegative, isDivisorNegative), divMod.getFirst());
      // C99: remainder has same sign as dividend
      Region[] remainder = negateIf(isDividendNegative, divMod.getSecond());

      return Pair.of(quotient, remainder);
    } else {
      return makeDivModUnsigned(r1, r2);
    }
  }

  private Region[] negateIf(Region condition, Region[] r) {
    final int bitsize = r.length;
    final Region[] zero = makeNumber(0, bitsize);
    Region[] neg = makeSub(zero, r);
    Region[] result = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      result[i] = rmgr.makeIte(condition, neg[i], r[i]);
    }
    return result;
  }

  /** compute the quotient and remainder for two unsinged bit-vectors. */
  private Pair<Region[], Region[]> makeDivModUnsigned(final Region[] r1, final Region[] r2) {
    final int bitsize = getBitSize(r1, r2);

    Region[] resultDiv = new Region[bitsize];
    Region[] rest = new Region[bitsize];
    Arrays.fill(rest, rmgr.makeFalse());

    for (int pos = bitsize - 1; pos >= 0; pos--) {
      // first update rest by adding right-most bit
      Region[] shiftedRest = new Region[bitsize];
      shiftedRest[0] = r1[pos];
      for (int i = 1; i < bitsize; i++) {
        shiftedRest[i] = rest[i - 1];
      }

      // then calculate new rest
      Region less = makeLessOrEqual(r2, shiftedRest, false);
      Region[] sub = makeSub(shiftedRest, r2);

      Region[] tmp = new Region[bitsize];
      for (int i = 0; i < bitsize; i++) {
        tmp[i] = rmgr.makeIte(less, sub[i], shiftedRest[i]);
      }
      resultDiv[pos] = less;
      rest = tmp;
    }

    return Pair.of(resultDiv, rest);
  }

  public Region[] wrapLast(final Region r, final int size) {
    return toBitsize(size, false, r);
  }

  /**
   * returns a new Array with given length, that is filled with elements from the old regions. If
   * the new size is smaller, front elements are ignored. If the new size is greater, the missing
   * elements are filled as needed to guarantee signedness.
   */
  public Region[] toBitsize(int bitsize, boolean signed, Region... regions) {
    assert regions != null : "can not expand NULL";

    int min = Math.min(regions.length, bitsize);
    final Region[] newRegions = new Region[bitsize];

    // copy old elements
    System.arraycopy(regions, 0, newRegions, 0, min);

    // fill front with new elements
    for (int i = min; i < bitsize; i++) {
      newRegions[i] = signed ? regions[regions.length - 1] : rmgr.makeFalse();
    }

    return newRegions;
  }
}
