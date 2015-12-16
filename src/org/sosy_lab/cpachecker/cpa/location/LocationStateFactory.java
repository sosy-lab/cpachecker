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
package org.sosy_lab.cpachecker.cpa.location;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.SortedSet;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cpa.location.LocationState.BackwardsLocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationState.BackwardsLocationStateNoTarget;
import org.sosy_lab.cpachecker.cpa.location.LocationState.ForwardsLocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationState.LocationStateType;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;

@Options(prefix="cpa.location")
public class LocationStateFactory {

  private final LocationState[] statesByNodeNumber;

  @Option(secure=true, description="With this option enabled, unction calls that occur"
      + " in the CFA are followed. By disabling this option one can traverse a function"
      + " without following function calls (in this case FunctionSummaryEdges are used)")
  private boolean followFunctionCalls = true;

  public LocationStateFactory(CFA pCfa, LocationStateType locationType, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);

    SortedSet<CFANode> allNodes = extractAllNodes(pCfa);

    int maxNodeNumber = allNodes.last().getNodeNumber();
    statesByNodeNumber = new LocationState[maxNodeNumber+1];
    for (CFANode node : allNodes) {
      LocationState state = locationType == LocationStateType.BACKWARD
          ? new BackwardsLocationState(node, pCfa, followFunctionCalls)
          : locationType == LocationStateType.BACKWARDNOTARGET
              ? new BackwardsLocationStateNoTarget(node, pCfa, followFunctionCalls)
              : new ForwardsLocationState(node, followFunctionCalls);

      statesByNodeNumber[node.getNodeNumber()] = state;
    }
  }

  private SortedSet<CFANode> extractAllNodes(CFA pCfa) {
    SortedSet<CFANode> allNodes = from(pCfa.getAllNodes())
        // First, we collect all CFANodes in between the inner edges of all MultiEdges.
        // This is necessary for cpa.composite.splitMultiEdges
        .transformAndConcat(new Function<CFANode, Iterable<CFAEdge>>() {
              @Override
              public Iterable<CFAEdge> apply(CFANode pInput) {
                return CFAUtils.leavingEdges(pInput);
              }
            })
        .filter(MultiEdge.class)
        .transformAndConcat(new Function<MultiEdge, Iterable<CFAEdge>>() {
              @Override
              public Iterable<CFAEdge> apply(MultiEdge pInput) {
                return pInput.getEdges();
              }
            })
        .transform(CFAUtils.TO_SUCCESSOR)
        // Second, we collect all normal CFANodes
        .append(pCfa.getAllNodes())
        // Third, sort and remove duplicates
        .toSortedSet(Ordering.natural());

    return allNodes;
  }

  /**
   * Get a state that represents a node that is part of the CFA.
   *
   * @param node  The CFA node.
   * @return      The state that represents the CFA node.
   */
  public LocationState getState(CFANode node) {

    return Preconditions.checkNotNull(statesByNodeNumber[checkNotNull(node).getNodeNumber()],
        "LocationState for CFANode %s in function %s requested,"
        + " but this node is not part of the current CFA.",
        node, node.getFunctionName());
  }

  public LocationState createStateWithShadowTransitions(Iterable<CFAEdge> pShadowTransitions,
      CFANode pEndInNodeOfCfa) {

    return new LocationState.ForwardsShadowLocationState(pShadowTransitions, pEndInNodeOfCfa);
  }
}
