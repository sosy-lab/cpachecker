// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableList;
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

      if (node == cfaEdge.getPredecessor()) {
        // if (CFAUtils.allLeavingEdges(node).contains(cfaEdge)) {

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
        return ImmutableList.copyOf(result);
      }
    } finally {
      stats.getSuccessorsTimer.stop();
    }
  }

  Statistics getStatistics() {
    return stats;
  }
}
