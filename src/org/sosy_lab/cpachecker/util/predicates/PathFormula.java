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
  // for RelyGuarantee
  public static final String PRIME_SYMBOL   = "^";
  public static final String THREAD_SYMBOL  = "$";



  // precompiled regex expression
  //private static Pattern primeRegex = Pattern.compile("(.+)"+PRIME_SYMBOL+"(\\d+)$");
  // regural expression that groups proper variable name and the number of primes. Works on uninstantiated variables.
  private static Pattern primeRegex = Pattern.compile("(.+)\\^(\\d+)$");
  private static Pattern nonModularRegex = Pattern.compile("(.+)\\$(\\d+)$");

  public PathFormula(Formula pf, SSAMap ssa, int pLength) {
    this.formula = pf;
    this.ssa = ssa;
    this.length = pLength;
    this.primedNo = 0;
  }

  public PathFormula(Formula pf, SSAMap ssa, int pLength, int pPrimedNo) {
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
        && ssa.equals(other.ssa)
        && length == other.length;
  }

  @Override
  public int hashCode() {
    return (formula.hashCode() * 17 + ssa.hashCode()) * 31 + length;
  }



  /**
   * Returns unprimed variable name and the primed no. If the variable is
   * not primed, then the primed no is null. Works only on uninstantiated variables.
   * @param pFirst
   * @return (unprimed variable name, primed no)
   */
  public static Pair<String, Integer> getPrimeData(String pFirst) {
    Integer currentPrime;
    String bareName;
    Matcher m = primeRegex.matcher(pFirst);
    if(m.find()) {
      bareName = m.group(1);
      String currentPrimeStr = m.group(2);
      currentPrime = Integer.parseInt(currentPrimeStr);
    } else {
      currentPrime = null;
      bareName = pFirst;
    }

    return new Pair<String, Integer>(bareName, currentPrime);
  }
//TODO maybe some caching?
  // returns (unprimed name, number of primes)



  // returns (unprimed name, number of primes)
  public static Pair<String, Integer> getNonModularData(String pFirst) {
    Integer foreignThread;
    String bareName;
    Matcher m = nonModularRegex.matcher(pFirst);
    if(m.find()) {
      bareName = m.group(1);
      String currentPrimeStr = m.group(2);
      foreignThread = Integer.parseInt(currentPrimeStr);
    } else {
      foreignThread = null;
      bareName = pFirst;
    }

    return new Pair<String, Integer>(bareName, foreignThread);
  }

  // primes a variable given number of times
  public static String primeVariable(String pFirst, int pHowManyPrimes) {
    // get the current number of primes
    Pair<String, Integer> data = getPrimeData(pFirst);
    Integer currentPrimed = data.getSecond() == null ? 0 : data.getSecond();
    return data.getFirst()+PRIME_SYMBOL+(currentPrimed+pHowManyPrimes);
  }
}