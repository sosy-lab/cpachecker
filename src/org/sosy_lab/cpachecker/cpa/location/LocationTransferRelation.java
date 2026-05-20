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
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.transformation.SubCFA;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class LocationTransferRelation implements TransferRelation {

  private final LocationStateFactory factory;

  public LocationTransferRelation(LocationStateFactory pFactory) {
    factory = pFactory;
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    CFANode node = ((LocationState) element).getLocationNode();

    // TMP-Bugfix ThreadingTransferRelation calls this with SingletonPrecision
    if (prec instanceof SingletonPrecision){
      if (node.getAllLeavingEdges().contains(cfaEdge)) {
        return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
      }
      return ImmutableSet.of();
    }

    if (node.getAllLeavingEdges().contains(cfaEdge)) {
      CFANode successor = cfaEdge.getSuccessor();
      LocationPrecision locPrec = (LocationPrecision) prec;
      ImmutableSet<SubCFA> allowedStrategiesForNode = locPrec.getStrategiesForNode(node);
      Optional<SubCFA> currentStrategy = LocationPrecision.select(allowedStrategiesForNode);
      if (currentStrategy.isEmpty()){
        // follow base strategy
        if (cfaEdge.getRawStatement().startsWith("enter program transformation: ")){
          return ImmutableSet.of();
        }
        return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
      } else {
        if (successor == currentStrategy.get().subCFAEntryNode()){
          return Collections.singleton(factory.getState(successor));
        }
      }
    }
    return ImmutableSet.of();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element, Precision prec)
      throws CPATransferException {
    // TODO find out if this needs to be implemented for LocationCPA
    CFANode node = ((LocationState) element).getLocationNode();
    ImmutableList<LocationState>
        successors = CFAUtils.successorsOf(node).transform(n -> factory.getState(n)).toList();
    //LocationPrecision locPrec = (LocationPrecision) prec;
    return successors;
  }
}
