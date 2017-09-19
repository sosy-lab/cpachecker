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
package org.sosy_lab.cpachecker.cpa.conditions.path;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import java.io.PrintStream;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A {@link PathCondition} where the condition is based on the number of appearances
 * of edges in a path. I.e., if the threshold is 3, a path is cut off as soon
 * as any edge is seen the third time in the path.
 * However, only FunctionCallEdges and outgoing edges of a loop head are tracked.
 */
@Options(prefix="cpa.conditions.path.repetitions")
public class RepetitionsInPathCondition implements PathCondition, Statistics {

  @Option(secure=true, description="maximum repetitions of any edge in a path (-1 for infinite)",
      name="limit")
  @IntegerOption(min=-1)
  private int threshold = -1;

  private int increaseThresholdBy = 0;

  private int maxRepetitionsInPath = 0;



  public RepetitionsInPathCondition(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AvoidanceReportingState getInitialState(CFANode pNode) {
    return new RepetitionsInPathConditionState(ImmutableMultiset.of(), threshold, false);
  }

  private boolean isInteresting(CFAEdge edge) {
    return (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
        || (edge.getPredecessor().isLoopStart());
  }

  @Override
  public AvoidanceReportingState getAbstractSuccessor(AbstractState pState, CFAEdge pEdge) {
    RepetitionsInPathConditionState current = (RepetitionsInPathConditionState)pState;

    if (!isInteresting(pEdge)) {
      return current;
    }

    if (current.thresholdReached) {
      return current;
    }

    int repetitions = current.frequencyMap.count(pEdge);
    repetitions++;

    boolean thresholdReached = (threshold >= 0) && (repetitions >= threshold);

    maxRepetitionsInPath = Math.max(repetitions, maxRepetitionsInPath);

    Multiset<CFAEdge> newFrequencyMap =
        ImmutableMultiset.<CFAEdge>builder()
            .addAll(current.frequencyMap)
            .setCount(pEdge, repetitions)
            .build();

    return new RepetitionsInPathConditionState(newFrequencyMap, threshold, thresholdReached);
  }

  @Override
  public boolean adjustPrecision() {
    if (threshold == -1) {
      // set the initial threshold value
      // TODO PW: Do this calculations make sense? I just copied them from RepetitionsInPathHeuristicsPrecision.
      threshold = maxRepetitionsInPath / 5;
      increaseThresholdBy = threshold;

    } else {
      threshold = threshold + increaseThresholdBy;
    }
    return true;
  }

  @Override
  public String getName() {
    return "Repetitions in path condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    out.println("Maximum repetitions in a path: " + maxRepetitionsInPath);
    out.println("Threshold value:               " + threshold);
  }


  private static class RepetitionsInPathConditionState implements AbstractState, AvoidanceReportingState {

    private final ImmutableMultiset<CFAEdge> frequencyMap;
    private final int threshold;
    private final boolean thresholdReached;

    private RepetitionsInPathConditionState(
        Multiset<CFAEdge> pFrequencyMap, int pThreshold, boolean pThresholdReached) {
      frequencyMap = ImmutableMultiset.copyOf(pFrequencyMap);
      threshold = pThreshold;
      thresholdReached = pThresholdReached;
    }

    @Override
    public boolean mustDumpAssumptionForAvoidance() {
      return thresholdReached;
    }

    @Override
    public BooleanFormula getReasonFormula(FormulaManagerView pMgr) {
      return PreventingHeuristic.REPETITIONSINPATH.getFormula(pMgr, threshold);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      for (Entry<CFAEdge> entry : frequencyMap.entrySet()) {
        builder.append(entry.getCount()).append("x(").append(entry.getElement()).append(") ");
      }
      return builder.toString();
    }
  }

  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }

}
