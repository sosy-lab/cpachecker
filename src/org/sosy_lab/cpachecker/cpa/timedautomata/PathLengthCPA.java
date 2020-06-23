// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.timedautomata;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Preconditions;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

@Options(prefix = "cpa.timedautomata")
public class PathLengthCPA extends AbstractCPA
    implements AdjustableConditionCPA, ReachedSetAdjustingCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PathLengthCPA.class);
  }

  @Option(secure = true, description = "Initial maximum length of paths that will be explored.")
  private int initialMaximumPathLength = 1;

  @Option(
      secure = true,
      description = "Amount of steps by which to increase the maximum explored path length.")
  private int pathLengthAdujstmentStep = 1;

  @Option(
      secure = true,
      description = "Maximum value that the maximum path length bound will be increased to.")
  private int pathLengthUpperBound = 10;

  private int maximumPathLength;

  public PathLengthCPA() {
    super("SEP", "NEVER", new PathLengthTransferRelation());
    Preconditions.checkState(
        initialMaximumPathLength >= 0,
        "Initial maximum path length must be greater or equal to zero");
    Preconditions.checkState(
        pathLengthAdujstmentStep > 0, "Path length adjustment step must be greater than zero");
    Preconditions.checkState(
        pathLengthUpperBound >= initialMaximumPathLength,
        "Path length upper bound must be greater or equal to initial maximum path length");

    maximumPathLength = initialMaximumPathLength;
    var transferRelation = (PathLengthTransferRelation) getTransferRelation();
    transferRelation.setMaximumPathLength(maximumPathLength);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new PathLengthState(0, maximumPathLength == 0);
  }

  @Override
  public boolean adjustPrecision() {
    if (maximumPathLength + pathLengthAdujstmentStep <= pathLengthUpperBound) {
      maximumPathLength += pathLengthAdujstmentStep;
      var transferRelation = (PathLengthTransferRelation) getTransferRelation();
      transferRelation.setMaximumPathLength(maximumPathLength);
      return true;
    }
    return false;
  }

  @Override
  public void adjustReachedSet(final ReachedSet pReachedSet) {
    Set<AbstractState> blockedByBound = new LinkedHashSet<>();
    for (AbstractState s : pReachedSet) {
      var pathLengthState = extractStateByType(s, PathLengthState.class);
      if (pathLengthState != null && pathLengthState.didReachBound()) {
        blockedByBound.add(s);
        if (pathLengthState.getPathLength() < maximumPathLength) {
          pathLengthState.setDidReachedBoundFalse();
        }
      }
    }

    blockedByBound.forEach(pReachedSet::reAddToWaitlist);
  }
}
