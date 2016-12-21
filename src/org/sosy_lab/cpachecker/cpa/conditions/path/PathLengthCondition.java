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

import java.io.PrintStream;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
 * A {@link PathCondition} where the condition is the length of the current path.
 */
@Options(prefix="cpa.conditions.path.length")
public class PathLengthCondition implements PathCondition, Statistics {

  @Option(secure=true, description="maximum path length (-1 for infinite)",
      name="limit")
  @IntegerOption(min=-1)
  private int threshold = -1;

  private int increaseThresholdBy = 0;

  private int maxPathLength = 0;



  public PathLengthCondition(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AvoidanceReportingState getInitialState(CFANode pNode) {
    return new PathLengthConditionState(0, false);
  }

  @Override
  public AvoidanceReportingState getAbstractSuccessor(AbstractState pState, CFAEdge pEdge) {

    PathLengthConditionState current = (PathLengthConditionState)pState;
    if (current.thresholdReached) {
      return current;
    }

    int pathLength = current.pathLength + 1;
    boolean thresholdReached = (threshold >= 0) && (pathLength >= threshold);

    maxPathLength = Math.max(pathLength, maxPathLength);

    return new PathLengthConditionState(pathLength, thresholdReached);
  }

  @Override
  public boolean adjustPrecision() {
    if (threshold == -1) {
      // set the initial threshold value
      // TODO PW: Do this calculations make sense? I just copied them from PathLengthHeuristicsPrecision.
      threshold = maxPathLength / 5;
      increaseThresholdBy = threshold / 4;

    } else {
      threshold = threshold + increaseThresholdBy;
    }
    return true;
  }

  @Override
  public String getName() {
    return "Path length condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    out.println("Maximum length of a path: " + maxPathLength);
    out.println("Threshold value:          " + threshold);
  }


  private static class PathLengthConditionState implements AbstractState, AvoidanceReportingState {

    private final int pathLength;
    private final boolean thresholdReached;

    private PathLengthConditionState(int pPathLength, boolean pThresholdReached) {
      pathLength = pPathLength;
      thresholdReached = pThresholdReached;
    }

    @Override
    public boolean mustDumpAssumptionForAvoidance() {
      return thresholdReached;
    }

    @Override
    public BooleanFormula getReasonFormula(FormulaManagerView pMgr) {
      return PreventingHeuristic.PATHLENGTH.getFormula(pMgr, pathLength);
    }

    @Override
    public String toString() {
      return "path length: " + pathLength
          + (thresholdReached ? " (threshold reached)" : "");
    }
  }


  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }
}
