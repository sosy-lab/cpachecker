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
package org.sosy_lab.cpachecker.tiger.testgen.summaries;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStopSep;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeStopOperator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.ImmutableList;

/**
 * Check coverage for summaries that use predicate abstractions.
 */
public class SummaryCoveragePred implements SummaryCoverage, StatisticsProvider {

  private static final int toSkip = 2;
  private static SummaryCoveragePred instance;
  private ImmutableList<StopOperator> stopOperators;
  private SCPStatistics stats;


  public static  SummaryCoveragePred getInstance(ConfigurableProgramAnalysis cpa){
    if (instance == null){
      instance = new SummaryCoveragePred(cpa);
    }

    return instance;
  }

  private SummaryCoveragePred(ConfigurableProgramAnalysis cpa){
    ARGStopSep stopOperator = (ARGStopSep) cpa.getStopOperator();
    //argstopsep = stopOperator;
    CompositeStopOperator compositeStop = (CompositeStopOperator) stopOperator.getWrappedStop();
    stopOperators = compositeStop.getStopOperators();
    stats = new SCPStatistics();
  }



  @Override
  public boolean covers(AbstractState state, Precision precision, Collection<AbstractState> summaries) throws CPAException, InterruptedException {
    stats.summaryTimer.start();

    // quick check if pState is an abstraction
    PredicateAbstractState ps = AbstractStates.extractStateByType(state, PredicateAbstractState.class);
    if (!ps.isAbstractionState()){
      stats.summaryTimer.stop();
      return false;
    }

    ARGState argState = (ARGState)state;
    CompositePrecision compositePrecision = (CompositePrecision) precision;
    CompositeState compState = (CompositeState) argState.getWrappedState();

    //CFANode loc = AbstractStates.extractLocation(state);

    for (AbstractState tocheck : summaries){
      stats.summaryCovCheck++;
      ARGState argToCheck = (ARGState) tocheck;
      CompositeState compToCheck = (CompositeState) argToCheck.getWrappedState();

      List<AbstractState> compositeElements = compState.getWrappedStates();
      List<AbstractState> compositeReachedStates = compToCheck.getWrappedStates();

      List<Precision> compositePrecisions = compositePrecision.getPrecisions();

      boolean res = true;
      for (int idx = 0; idx < compositeElements.size(); idx++) {
        // skip GuardedAutomatonCPA state
        if (idx == SummaryCoveragePred.toSkip){
          continue;
        }

        StopOperator stopOp = stopOperators.get(idx);

        AbstractState absElem1 = compositeElements.get(idx);
        AbstractState absElem2 = compositeReachedStates.get(idx);
        Precision prec = compositePrecisions.get(idx);

        if (!stopOp.stop(absElem1, Collections.singleton(absElem2), prec)) {
          res = false;
          break;
        }
      }

      if (res){
        stats.summaryCovSucc++;
        stats.summaryTimer.stop();
        return true;
      }
    }

    stats.summaryTimer.stop();
    return false;
  }



  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }


  class SCPStatistics implements Statistics {

    private long   summaryCovCheck   = 0;
    private long   summaryCovSucc    = 0;

    private Timer summaryTimer      = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {

      if (summaryCovCheck > 0){
        out.println("Summary cov. successfull:         " + summaryCovSucc+"/"+summaryCovCheck+" ("+((summaryCovSucc*100)/summaryCovCheck)+"%)");
      } else {
        out.println("Summary cov. successfull:         " + summaryCovSucc+"/"+summaryCovCheck+" (NaN%)");
      }
      out.println("Time for summary check:           " + summaryTimer);
      out.println();

    }

    @Override
    public String getName() {
      return "SummaryCoveragePred Statistics";
    }

  }


}
