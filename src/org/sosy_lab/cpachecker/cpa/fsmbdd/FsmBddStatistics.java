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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import java.io.PrintStream;

import net.sf.javabdd.BDDFactory;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class FsmBddStatistics implements Statistics {

  private static final String SIMPLE_STAT_VALUE_FMT = "%-40s %s\n";

  private int maxEncodedAssumptions;
  public int mergeCalls = 0;
  public int mergesBecauseEqualBdd = 0;
  public int mergesBecauseEqualSyntax = 0;
  public int mergesBecauseBothEmptySyntax = 0;
  public final Timer blockAbstractionAllTimer = new Timer();
  public final Timer blockAbstractionBeginOnLastEncodeTimer = new Timer();
  public final Timer blockAbstractionBeginOnFirstEncodeTimer = new Timer();
  public final Timer blockAbstractionConjunctTimer = new Timer();
  public final Timer disjunctStateBddTimer = new Timer();
  public final Timer conjunctStateBddTimer = new Timer();
  public final Timer undefineVarInBddTimer = new Timer();
  public final Timer assignToVarTimer = new Timer();

  private BDDFactory bddFactory;

  public FsmBddStatistics(BDDFactory pBddFactory) {
    this.bddFactory = pBddFactory;
    this.maxEncodedAssumptions = Integer.MIN_VALUE;
  }

  private void printValue (PrintStream pOut, String pName, Object pValue) {
    pOut.printf(SIMPLE_STAT_VALUE_FMT,  pName + ":", pValue.toString());
  }

  public void signalNumOfEncodedAssumptions(int pEncodedAssumptions) {
    maxEncodedAssumptions = Math.max(maxEncodedAssumptions, pEncodedAssumptions);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    printValue(pOut, "Number of BDD domains",     bddFactory.numberOfDomains());
    printValue(pOut, "Number of BDD nodes",       bddFactory.getNodeNum());
    printValue(pOut, "Size of BDD node table",    bddFactory.getNodeTableSize());
    printValue(pOut, "Size of BDD cache",         bddFactory.getCacheSize());
    printValue(pOut, "Number of BDD reorderings", bddFactory.getReorderTimes());
    printValue(pOut, "Max. encoded assumptions",  maxEncodedAssumptions);
    printValue(pOut, "Merge called",  mergeCalls);
    printValue(pOut, "Merges because of equal BDD",  mergesBecauseEqualBdd);
    printValue(pOut, "Merges because of equal syntax",  mergesBecauseEqualSyntax);
    printValue(pOut, "Merges because of both empty syntax",  mergesBecauseBothEmptySyntax);
    printValue(pOut, "Time for condition block abstraction",  blockAbstractionAllTimer);
    printValue(pOut, "Time for condition block abst. conj.",  blockAbstractionConjunctTimer);
    printValue(pOut, "Time for condition block abst. lfe.",  blockAbstractionBeginOnLastEncodeTimer);
    printValue(pOut, "Time for condition block abst. ffe",  blockAbstractionBeginOnFirstEncodeTimer);
    printValue(pOut, "Time for state BDD disjunction",  disjunctStateBddTimer);
    printValue(pOut, "Time for state BDD conjunction",  conjunctStateBddTimer);
    printValue(pOut, "Time for undefining var in BDD",  undefineVarInBddTimer);
    printValue(pOut, "Time for assign values in BDD",  assignToVarTimer);
  }

  @Override
  public String getName() {
    return "FsmBddCPA";
  }

}
