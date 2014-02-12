/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.bdd;

import java.math.BigInteger;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class BitvectorManager {

  private BDDRegionManager rmgr;

  public BitvectorManager(Configuration config, BDDRegionManager pRmgr)
          throws InvalidConfigurationException {
    this.rmgr = pRmgr;
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
    assert n.signum() != -1 : "bitvector from negative number is not possible";
    // TODO allow negative numbers and flip first bit, signed vs unsigned?
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
  private Region makeLess(final Region[] A, final Region[] B, final boolean equal, final boolean signed) {
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

  public Region[] wrapLast(final Region r, final int size) {
    return toBitsize(size, false, r);
  }

  /** returns a new Array with given length, that is filled with elements from the old regions.
   * If the new size is smaller, front elements are ignored.
   * If the new size is greater, the missing elements are filled as needed to guarantee signedness. */
  public Region[] toBitsize(int bitsize, boolean signed, Region... regions) {
    assert regions != null: "can not expand NULL";

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
