// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.location.LocationState.BackwardsLocationState;

@Options(prefix = "cpa.location")
public class LocationStateFactory {

  private final LocationState[] states;

  private final AnalysisDirection locationType;

  @Option(
      secure = true,
      description =
          "With this option enabled, function calls that occur"
              + " in the CFA are followed. By disabling this option one can traverse a function"
              + " without following function calls (in this case FunctionSummaryEdges are used)")
  private boolean followFunctionCalls = true;

  public LocationStateFactory(CFA pCfa, AnalysisDirection pLocationType, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    locationType = checkNotNull(pLocationType);

    ImmutableSortedSet<CFANode> allNodes;
    Collection<CFANode> tmpNodes = pCfa.getAllNodes();
    if (tmpNodes instanceof ImmutableSortedSet) {
      allNodes = (ImmutableSortedSet<CFANode>) tmpNodes;
    } else {
      allNodes = ImmutableSortedSet.copyOf(tmpNodes);
    }

    int maxNodeNumber = allNodes.last().getNodeNumber();
    states = new LocationState[maxNodeNumber + 1];
    for (CFANode node : allNodes) {
      LocationState state = createLocationState(node);
      states[node.getNodeNumber()] = state;
    }
  }

  public LocationState getState(CFANode node) {
    int nodeNumber = checkNotNull(node).getNodeNumber();

    if (nodeNumber >= 0 && nodeNumber < states.length) {
      return Preconditions.checkNotNull(
          states[nodeNumber],
          "LocationState for CFANode %s in function %s requested,"
              + " but this node is not part of the current CFA.",
          node,
          node.getFunctionName());

    } else {
      return createLocationState(node);
    }
  }

  private LocationState createLocationState(CFANode node) {
    return locationType == AnalysisDirection.BACKWARD
        ? new BackwardsLocationState(node, followFunctionCalls)
        : new LocationState(node, followFunctionCalls);
  }
}
