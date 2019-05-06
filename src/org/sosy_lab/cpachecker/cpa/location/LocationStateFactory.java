/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.cpa.location.LocationState.BackwardsLocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateWithEdge.BackwardsLocationStateWithEdge;

@Options(prefix = "cpa.location")
public class LocationStateFactory {

  private final LocationState[] states;

  private final AnalysisDirection locationType;

  @Option(
    secure = true,
    description =
        "With this option enabled, function calls that occur"
            + " in the CFA are followed. By disabling this option one can traverse a function"
            + " without following function calls (in this case FunctionSummaryEdges are used)"
  )
  private boolean followFunctionCalls = true;

  @Option(secure = true, description = "Use special states with edges")
  private boolean enableStatesWithEdges = false;

  public LocationStateFactory(CFA pCfa, AnalysisDirection pLocationType, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    locationType = checkNotNull(pLocationType);

    if (!enableStatesWithEdges) {
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
    } else {
      states = new LocationState[0];
    }
  }

  public Collection<LocationState> getState(CFANode node) {
    int nodeNumber = checkNotNull(node).getNodeNumber();

    if (enableStatesWithEdges) {
      return createLocationStateWithEdges(node);
    } else {
      if (nodeNumber >= 0 && nodeNumber < states.length) {
        LocationState result =
            Preconditions.checkNotNull(
                states[nodeNumber],
                "LocationState for CFANode %s in function %s requested,"
                    + " but this node is not part of the current CFA.",
                node,
                node.getFunctionName());
        return Collections.singleton(result);

      } else {
        return Collections.singleton(createLocationState(node));
      }
    }
  }

  private LocationState createLocationState(CFANode node) {
    return locationType == AnalysisDirection.BACKWARD
        ? new BackwardsLocationState(node, followFunctionCalls)
        : new LocationState(node, followFunctionCalls);
  }

  private Collection<LocationState> createLocationStateWithEdges(CFANode node) {
    List<LocationState> result;
    if (locationType == AnalysisDirection.BACKWARD) {
      result = new ArrayList<>(node.getNumEnteringEdges());
      for (int i = 0; i< node.getNumEnteringEdges(); i++) {
        CFAEdge edge = node.getEnteringEdge(i);
        result.add(
            new BackwardsLocationStateWithEdge(
                node,
                followFunctionCalls,
                new WrapperCFAEdge(edge)));
      }
      if (result.size() == 0) {
        result.add(
            new BackwardsLocationStateWithEdge(node, followFunctionCalls, EmptyEdge.getInstance()));
      }
    } else {
      result = new ArrayList<>(node.getNumLeavingEdges());
      for (int i = 0; i< node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        result.add(
            new LocationStateWithEdge(
                node,
                followFunctionCalls,
                new WrapperCFAEdge(edge)));
      }
      if (result.size() == 0) {
        result.add(new LocationStateWithEdge(node, followFunctionCalls, EmptyEdge.getInstance()));
      }

    }
    return result;
  }
}
