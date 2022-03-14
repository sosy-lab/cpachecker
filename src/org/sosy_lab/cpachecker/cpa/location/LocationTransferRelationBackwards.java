// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class LocationTransferRelationBackwards implements TransferRelation {

  private final LocationStateFactory factory;

  public LocationTransferRelationBackwards(LocationStateFactory pFactory) {
    factory = pFactory;
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision prec, CFAEdge cfaEdge) throws CPATransferException {

    LocationState predState = (LocationState) state;
    CFANode predLocation = predState.getLocationNode();

    if (CFAUtils.allEnteringEdges(predLocation).contains(cfaEdge)) {
      return Collections.singleton(factory.getState(cfaEdge.getPredecessor()));
    }

    return ImmutableSet.of();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState state, Precision prec)
      throws CPATransferException {

    CFANode predLocation = ((LocationState) state).getLocationNode();

    List<LocationState> allSuccessors = new ArrayList<>(predLocation.getNumEnteringEdges());

    for (CFANode predecessor : CFAUtils.predecessorsOf(predLocation)) {
      allSuccessors.add(factory.getState(predecessor));
    }

    return allSuccessors;
  }
}
