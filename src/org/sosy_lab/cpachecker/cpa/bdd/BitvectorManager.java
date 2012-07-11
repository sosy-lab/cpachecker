/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import java.util.Arrays;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

@Options(prefix = "cpa.bdd")
public class BitvectorManager {

  private NamedRegionManager rmgr;

  @Option(description = "bitsize for values")
  private int bitsize = 8;

  public BitvectorManager(NamedRegionManager rmgr, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    this.rmgr = rmgr;
  }

  public int getBitSize(){
    return bitsize;
  }

  // checks whether one of the regions is true
  public boolean isTrue(Region[] regions) {
    for (Region r : regions) {
      if (r.isTrue()) { return true; }
    }
    return false;
  }

  // checks whether all regions are false
  public boolean isFalse(Region[] regions) {
    for (Region r : regions) {
      if (!r.isFalse()) { return false; }
    }
    return true;
  }

  /** returns OR of regions */
  public Region makeOr(Region[] regions) {
    Region tmp = rmgr.makeFalse(); // neutral for OR
    for (Region r : regions) {
      tmp = rmgr.makeOr(tmp, r);
    }
    return tmp;
  }

  /** returns 0001 */
  public Region[] makeTrue() {
    return wrapLast(rmgr.makeFalse(), rmgr.makeTrue());
  }

  /** returns 0000 */
  public Region[] makeFalse() {
    return wrapLast(rmgr.makeFalse(), rmgr.makeFalse());
  }

  /** returns bitRepresentation of number, 5 --> 00101 --> [0,0,1,0,1] */
  public Region[] makeNumber(BigInteger n) {
    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      // TODO simplify?
      if (BigInteger.ZERO.equals(n.and(BigInteger.valueOf(1 << (bitsize - i - 1))))) {
        newRegions[i] = rmgr.makeFalse();
      } else {
        newRegions[i] = rmgr.makeTrue();
      }
    }
    return newRegions;
  }

  /** returns regions for positions of a variable, s --> [s@1, s@2, s@3, ...] */
  public Region[] createPredicate(String s) {
    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      newRegions[i] = rmgr.createPredicate(s + "@" + (bitsize - i - 1));
    }
    return newRegions;
  }

  /** removes predicate from region */
  public Region makeExists(Region region, Region[] existing) {
    for (int i = 0; i < bitsize; i++) {
      region = rmgr.makeExists(region, existing[i]);
    }
    return region;
  }

  /** 1100 --> 0000, 0000 --> 0001 */
  public Region[] makeNot(Region[] r) {
    Region tmp = rmgr.makeFalse(); // neutral region for OR
    for (int i = 0; i < bitsize; i++) {
      tmp = rmgr.makeOr(tmp, r[i]);
    }
    return wrapLast(rmgr.makeFalse(), rmgr.makeNot(tmp));
  }

  /** 1100 & 1010 --> 1000 */
  public Region[] makeBinaryAnd(Region[] r1, Region[] r2) {
    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      newRegions[i] = rmgr.makeAnd(r1[i], r2[i]);
    }
    return newRegions;
  }

  /** 1100 && 1010 --> 0001 */
  public Region[] makeLogicalAnd(Region[] r1, Region[] r2) {
    Region tmp1 = rmgr.makeFalse(); // neutral region for OR
    Region tmp2 = rmgr.makeFalse();
    for (int i = 0; i < bitsize; i++) {
      tmp1 = rmgr.makeOr(tmp1, r1[i]);
      tmp2 = rmgr.makeOr(tmp2, r2[i]);
    }
    return wrapLast(rmgr.makeFalse(), rmgr.makeAnd(tmp1, tmp2));
  }

  /** 1100 | 1010 --> 1110 */
  public Region[] makeBinaryOr(Region[] r1, Region[] r2) {
    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      newRegions[i] = rmgr.makeOr(r1[i], r2[i]);
    }
    return newRegions;
  }

  /** 1100 || 1010 --> 0001 */
  public Region[] makeLogicalOr(Region[] r1, Region[] r2) {
    Region tmp1 = rmgr.makeFalse(); // neutral region for OR
    Region tmp2 = rmgr.makeFalse();
    for (int i = 0; i < bitsize; i++) {
      tmp1 = rmgr.makeOr(tmp1, r1[i]);
      tmp2 = rmgr.makeOr(tmp2, r2[i]);
    }
    return wrapLast(rmgr.makeFalse(), rmgr.makeOr(tmp1, tmp2));
  }

  /** 1100 <==> 1010 --> 1001 */
  public Region[] makeBinaryEqual(Region[] r1, Region[] r2) {
    Region[] newRegions = new Region[bitsize];
    for (int i = 0; i < bitsize; i++) {
      newRegions[i] = rmgr.makeEqual(r1[i], r2[i]);
    }
    return newRegions;
  }

  /** 1100 == 1010 --> 0000 */
  public Region[] makeLogicalEqual(Region[] r1, Region[] r2) {
    Region equality = rmgr.makeTrue();
    for (int i = 0; i < bitsize; i++) {
      Region equalPos = rmgr.makeEqual(r1[i], r2[i]);
      equality = rmgr.makeAnd(equality, equalPos);
    }
    return wrapLast(rmgr.makeFalse(), equality);

  }

  /** 1100 ^ 1010 --> 0110 */
  public Region[] makeXor(Region[] r1, Region[] r2) {
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
    Region[] r2tmp = makeBinaryEqual(r2, makeFalse());
    Region carrier = rmgr.makeTrue();
    return fullAdder(r1, r2tmp, carrier);
  }

  private Region[] fullAdder(Region[] r1, Region[] r2, Region carrier) {
    Region[] newRegions = new Region[bitsize];
    for (int i = bitsize - 1; i >= 0; i--) {// reverse iteration order!

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

  /** A<B,  */
  public Region[] makeLess(Region[] A, Region[] B) {
    Region[] diff = makeSub(A, B);
    return wrapLast(rmgr.makeFalse(), diff[0]);
  }

  /** returns an Array filled with (n-1)*first and 1*last */
  private Region[] wrapLast(Region first, Region last) {
    Region[] newRegions = new Region[bitsize];
    Arrays.fill(newRegions, 0, bitsize - 1, first);
    newRegions[bitsize - 1] = last;
    return newRegions;
  }
}
