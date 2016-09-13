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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LocationTransferRelationBackwards implements TransferRelation {

  private final LocationStateFactory factory;

  public LocationTransferRelationBackwards(LocationStateFactory pFactory) {
    factory = pFactory;
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision prec,  CFAEdge cfaEdge) throws CPATransferException {

    LocationState predState = (LocationState) state;
    CFANode predLocation = predState.getLocationNode();

    if (CFAUtils.allEnteringEdges(predLocation).contains(cfaEdge)) {
      return Collections.singleton(factory.getState(cfaEdge.getPredecessor()));
    }

    return Collections.emptySet();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState state,
      Precision prec) throws CPATransferException {

    CFANode predLocation = ((LocationState)state).getLocationNode();

    List<LocationState> allSuccessors = new ArrayList<>(predLocation.getNumEnteringEdges());

    for (CFANode predecessor : CFAUtils.predecessorsOf(predLocation)) {
      allSuccessors.add(factory.getState(predecessor));
    }

    return allSuccessors;
  }
}
