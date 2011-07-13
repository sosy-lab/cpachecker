/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

public class PathFormula {

  private final Formula formula;
  private final SSAMap ssa;
  // the  number of edges in the longest path in encoded the formula
  private final int length;
  // how many times a formula has been primed
  private int primedNo;



  // precompiled regex expression
  private static Pattern primeRegex = Pattern.compile("(.+)\\^(\\d+)$");
  private static int UNKNOWN  = Integer.MIN_VALUE;

  protected PathFormula(Formula pf, SSAMap ssa, int pLength) {
    this.formula = pf;
    this.ssa = ssa;
    this.length = pLength;
    this.primedNo = 0;
  }

  protected PathFormula(Formula pf, SSAMap ssa, int pLength, int pPrimedNo) {
    this.formula = pf;
    this.ssa = ssa;
    this.length = pLength;
    this.primedNo = pPrimedNo;

  }




  public Formula getFormula() {
    return formula;
  }

  public SSAMap getSsa() {
    return ssa;
  }

  public int getLength() {
    return length;
  }

  public int getPrimedNo() {
    return this.primedNo;
  }

  public int getAtomNo() {
    return this.formula.getAtomNo();
  }

  @Override
  public String toString(){
    return getFormula().toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PathFormula)) {
      return false;
    }

    PathFormula other = (PathFormula)obj;
    return formula.equals(other.formula)
        && ssa.equals(other.ssa);
  }

  @Override
  public int hashCode() {
    return formula.hashCode() * 17 + ssa.hashCode();
  }

  // TODO maybe some caching?
  // returns (unprimed name, number of primes)
  public static Pair<String, Integer> getPrimeData(String pFirst) {
    Integer currentPrime;
    String bareName;
    Matcher m = primeRegex.matcher(pFirst);
    if(m.find()) {
      bareName = m.group(1);
      String currentPrimeStr = m.group(2);
      currentPrime = Integer.parseInt(currentPrimeStr);
    } else {
      currentPrime = 0;
      bareName = pFirst;
    }

    return new Pair<String, Integer>(bareName, currentPrime);
  }

  // primes a variable given number of times
  public static String primeVariable(String pFirst, int pHowManyPrimes) {
    // get the current number of primes
    Pair<String, Integer> data = getPrimeData(pFirst);
    return data.getFirst()+"^"+(data.getSecond()+pHowManyPrimes);
  }
}