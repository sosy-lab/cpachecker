/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.io.PrintWriter;

import org.sosy_lab.cpachecker.core.ReachedElements;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

class ObserverStatistics implements Statistics {

  private final ObserverAutomatonCPA mCpa;
  
  public ObserverStatistics(ObserverAutomatonCPA pCpa) {
    mCpa = pCpa;
  }
  
  @Override
  public String getName() {
    return "ObserverAnalysis";
  }

  @Override
  public void printStatistics(PrintWriter out, Result pResult,
      ReachedElements pReached) {
    
    ObserverTransferRelation trans = mCpa.getTransferRelation();
    out.println("Total time for sucessor computation: " + toTime(trans.totalPostTime));
    out.println("  Time for transition matches:       " + toTime(trans.matchTime));
    out.println("  Time for transition assertions:    " + toTime(trans.assertionsTime));
    out.println("  Time for transition actions:       " + toTime(trans.actionTime));
    out.println("Total time for strengthen operator:  " + toTime(trans.totalStrengthenTime));
  }
  
  private String toTime(long timeMillis) {
    return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
  }

}
