/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.test;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;

public class Interval {

  public enum Comparators {
    LE,
    LT,
    EQ,
    GE,
    GT
  }

  private BigInteger upperBound;
  private BigInteger lowerBound;
  private int indexOfValue;
  private Comparators lowerBoundComparator;
  private Comparators upperBoundComparator;
  private Comparators valueComparator;


  public Interval(BigInteger pLowerBound, Comparators pLowerBoundComparator, BigInteger pUpperBound,
      Comparators pUpperBoundComparator, int pIndexOfValue, Comparators pValueComparator) {
    lowerBound = pLowerBound;
    lowerBoundComparator = pLowerBoundComparator;
    upperBound = pUpperBound;
    upperBoundComparator = pUpperBoundComparator;
    indexOfValue = pIndexOfValue;
    valueComparator = pValueComparator;
  }

  public boolean compare(BigInteger pVar, TestCase testcase) {
    if (lowerBound != null) {
      if (!compare(pVar, lowerBound, lowerBoundComparator)) { return false; }
    }

    if (upperBound != null) {
      if (!compare(pVar, upperBound, upperBoundComparator)) { return false; }
    }

    if (indexOfValue > 0) {
      if (!compare(pVar, testcase.getInputs().get(indexOfValue), valueComparator)) { return false; }
    }

    return true;
  }

  private static boolean compare(BigInteger pVar, BigInteger pLowerBound,
      Comparators pLowerBoundComparator) {
    switch (pLowerBoundComparator) {
      case EQ:
        return pVar.compareTo(pLowerBound) == 0;
      case LE:
        return pVar.compareTo(pLowerBound) <= 0;
      case LT:
        return pVar.compareTo(pLowerBound) < 0;
      case GE:
        return pVar.compareTo(pLowerBound) >= 0;
      case GT:
        return pVar.compareTo(pLowerBound) > 0;
      default:
        return true;
    }
  }

}
