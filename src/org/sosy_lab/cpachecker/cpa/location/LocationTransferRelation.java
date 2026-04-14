// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.transformation.SubCFA;
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

    if (node.getAllLeavingEdges().contains(cfaEdge)) {
      return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
    }

    return ImmutableSet.of();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element, Precision prec)
      throws CPATransferException {

    CFANode node = ((LocationState) element).getLocationNode();
    ImmutableList<LocationState>
        successors = CFAUtils.successorsOf(node).transform(n -> factory.getState(n)).toList();
    if (prec instanceof LocationPrecision locPrec) {
      if (locPrec.hasProgramTransformations()) {
        // when program transformations are present use the precision to select abstract successors
        Builder<LocationState> successorBuilder = ImmutableList.builder();
        for (LocationState successor : successors) {
          Optional<SubCFA> successorProgramTransformation = locPrec.isPartOfProgramTransformation(successor.getLocationNode());
          if (successorProgramTransformation.isPresent()) {
            // only add program transformation successor nodes if they are in the allowed precision set
            if(locPrec.getAllowedProgramTransformations().contains(successorProgramTransformation.get()))
              successorBuilder.add(successor);
          } else {
            // always add successor nodes of the original CFA
            successorBuilder.add(successor);
          }
        }
        return successorBuilder.build();
      }
      // when no program transformations are present return all successors
    }
      return successors;
  }
}
