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

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;


public class FsmBddStatistics implements Statistics {

  private static final String SIMPLE_STAT_VALUE_FMT = "%-35s %s\n";

  private BDDFactory bddFactory;

  public FsmBddStatistics(BDDFactory pBddFactory) {
    this.bddFactory = pBddFactory;
  }

  private void printFmt (PrintStream pOut, String pName, Object pValue) {
    pOut.printf(SIMPLE_STAT_VALUE_FMT,  pName + ":", pValue.toString());
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    printFmt(pOut, "Number of BDD domains",     bddFactory.numberOfDomains());
    printFmt(pOut, "Number of BDD nodes",       bddFactory.getNodeNum());
    printFmt(pOut, "Size of BDD node table",    bddFactory.getNodeTableSize());
    printFmt(pOut, "Size of BDD cache",         bddFactory.getCacheSize());
    printFmt(pOut, "Number of BDD reorderings", bddFactory.getReorderTimes());
  }

  @Override
  public String getName() {
    return "FsmBddCPA";
  }

}
