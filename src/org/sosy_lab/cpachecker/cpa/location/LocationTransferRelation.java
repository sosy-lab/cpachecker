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
package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.NoEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class LocationTransferRelation implements TransferRelation {

  private final LocationStateFactory factory;
  private final LocationStatistics stats;

  public LocationTransferRelation(LocationStateFactory pFactory, LocationStatistics pStats) {
    factory = pFactory;
    stats = pStats;
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {

    try {
      stats.getSuccessorsForEdgeTimer.start();
      CFANode node = ((LocationState) element).getLocationNode();

      // if (node == cfaEdge.getPredecessor()) {
      if (CFAUtils.allLeavingEdges(node).contains(cfaEdge)) {

        stats.createStateTimer.start();
        Collection<LocationState> result = factory.getState(cfaEdge.getSuccessor());
        stats.createStateTimer.stop();
        return result;
      }

      return ImmutableSet.of();
    } finally {

      stats.getSuccessorsForEdgeTimer.stop();
    }
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element,
      Precision prec) throws CPATransferException {

    try {
      stats.getSuccessorsTimer.start();
      if (element instanceof AbstractStateWithEdge) {
        AbstractEdge edge = ((AbstractStateWithEdge) element).getAbstractEdge();
        if (edge instanceof WrapperCFAEdge) {
          return getAbstractSuccessorsForEdge(element, prec, ((WrapperCFAEdge) edge).getCFAEdge());
        } else if (edge == EmptyEdge.getInstance()) {
          // Again return all next edges
          return factory.getState(((LocationState) element).locationNode);
        } else if (edge == NoEdge.getInstance()) {
          // Again return all next edges
          return ImmutableSet.of();
        } else {
          throw new UnsupportedOperationException(
              edge.getClass() + " edges are not supported in LocationCPA");
        }
      } else {
        CFANode node = ((LocationState) element).getLocationNode();
        List<LocationState> result = new ArrayList<>();
        CFAUtils.successorsOf(node).forEach(n -> result.addAll(factory.getState(n)));
        return result;
      }
    } finally {
      stats.getSuccessorsTimer.stop();
    }
  }

  Statistics getStatistics() {
    return stats;
  }
}
