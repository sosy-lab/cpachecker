/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class AnalysisNotifier {
  private AnalysisNotifier() {
    listeners = new ArrayList<>();
  }

  private static AnalysisNotifier singleton;

  public static AnalysisNotifier getInstance() {
    if(singleton!=null) {
      return singleton;
    }
    singleton = new AnalysisNotifier();
    return singleton;
  }

  private boolean isEnabled = false;
  private List<AnalysisListener> listeners;

  public void register(AnalysisListener listener) {
    isEnabled=true;
    listeners.add(listener);
  }

  public static interface AnalysisListener  {
    public void beforeAbstractionStep(ReachedSet pReachedSet) throws CPAException;
    public Result updateResult(Result pResult);
    public void onStartAnalysis() throws CPAException;
    public void onSpecificationAutomatonCreate(List<Automaton> pAutomata);
    public void beforeRefinement(ARGState pLastElement);
    public void afterRefinement(boolean isSpurious, ReachedSet pReached, ARGReachedSet reached, ARGPath pPath, int pRefinementNumber)
        throws CPAException;
    public void onPrecisionIncrementCreate(AdjustablePrecision pAdjustablePrecision);
  }

  public void beforeAbstractionStep(ReachedSet pReachedSet) throws CPAException {
    if(isEnabled) {
      for(AnalysisListener e : listeners) {
         e.beforeAbstractionStep(pReachedSet);
      }
    }
  }

  public Result updateResult(Result pResult) {
    if(isEnabled) {
      Result res = pResult;
      for(AnalysisListener e : listeners) {
        res = e.updateResult(res);
      }
      return res;
    }
    return pResult;
  }


  public void onStartAnalysis() throws CPAException {
    if(isEnabled) {
      for(AnalysisListener e : listeners) {
         e.onStartAnalysis();
      }
    }
  }

  public void onSpecificationAutomatonCreate(List<Automaton> pAutomata) {
    if(isEnabled) {
      for(AnalysisListener e : listeners) {
         e.onSpecificationAutomatonCreate(pAutomata);
      }
    }

  }

  public void beforeRefinement(ARGState pLastElement) {
    if(isEnabled) {
      for(AnalysisListener e : listeners) {
         e.beforeRefinement(pLastElement);
      }
    }
  }

  public void afterRefinement(boolean isSpurious, ReachedSet pReached, ARGReachedSet reached, ARGPath pPath, int pRefinementNumber)
      throws CPAException {
    if(isEnabled) {
      for(AnalysisListener e : listeners) {
         e.afterRefinement(isSpurious, pReached, reached, pPath, pRefinementNumber);
      }
    }
  }

  public void onPrecisionIncrementCreate(AdjustablePrecision adjustablePrecision) {
    if(isEnabled) {
      for(AnalysisListener e : listeners) {
         e.onPrecisionIncrementCreate(adjustablePrecision);
      }
    }
  }

}
