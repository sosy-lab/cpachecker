// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.conditions.path;

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

/**
 * A {@link PathCondition} where the condition is based on the number of assume edges seen so far on
 * the current path.
 */
@Options(prefix = "cpa.conditions.path.assumeedges")
public class AssumeEdgesInPathCondition implements PathCondition, Statistics {

  @Option(
      secure = true,
      description = "maximum number of assume edges length (-1 for infinite)",
      name = "limit")
  @IntegerOption(min = -1)
  private int threshold = -1;

  private int increaseThresholdBy = 0;

  private int maxAssumeEdgesInPath = 0;

  public AssumeEdgesInPathCondition(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AvoidanceReportingState getInitialState(CFANode pNode) {
    return new AssumeEdgesInPathConditionState(0, false);
  }

  @Override
  public AvoidanceReportingState getAbstractSuccessor(AbstractState pState, CFAEdge pEdge) {
    AssumeEdgesInPathConditionState current = (AssumeEdgesInPathConditionState) pState;

    if (pEdge.getEdgeType() != CFAEdgeType.AssumeEdge) {
      return current;
    }

    if (current.isThresholdReached()) {
      return current;
    }

    int assumeEdgesInPath = current.getPathLength() + 1;
    boolean thresholdReached = (threshold >= 0) && (assumeEdgesInPath >= threshold);

    maxAssumeEdgesInPath = Math.max(assumeEdgesInPath, maxAssumeEdgesInPath);

    return new AssumeEdgesInPathConditionState(assumeEdgesInPath, thresholdReached);
  }

  @Override
  public boolean adjustPrecision() {
    if (threshold == -1) {
      // set the initial threshold value
      // TODO PW: Do this calculations make sense? I just copied them from
      // AssumeEdgesInPathHeuristicsPrecision.
      threshold = maxAssumeEdgesInPath / 5;
      increaseThresholdBy = threshold / 4;

    } else {
      threshold = threshold + increaseThresholdBy;
    }
    return true;
  }

  @Override
  public String getName() {
    return "Assume edges in path condition";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    out.println("Maximum length of a path: " + maxAssumeEdgesInPath);
    out.println("Threshold value:          " + threshold);
  }

  @Override
  public Reducer getReducer() {
    return NoOpReducer.getInstance();
  }
}
