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
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
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
      AbstractState state, Precision prec,  CFAEdge cfaEdge) throws CPATransferException {

    LocationState predState = (LocationState) state;
    CFANode predLocation = predState.getLocationNode();

    if (CFAUtils.allEnteringEdges(predLocation).contains(cfaEdge)) {
      return factory.getState(cfaEdge.getPredecessor());
    }

    return ImmutableSet.of();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState state,
      Precision prec) throws CPATransferException {

    CFANode predLocation = ((LocationState)state).getLocationNode();

    if (state instanceof AbstractStateWithEdge) {
      AbstractEdge edge = ((AbstractStateWithEdge) state).getAbstractEdge();
      if (edge instanceof WrapperCFAEdge) {
        return getAbstractSuccessorsForEdge(state, prec, ((WrapperCFAEdge) edge).getCFAEdge());
      } else if (edge instanceof EmptyEdge) {
        return Collections.singleton((LocationState) state);
      } else {
        throw new UnsupportedOperationException(
            edge.getClass() + " edges are not supported in LocationCPA");
      }
    } else {
      List<LocationState> allSuccessors = new ArrayList<>(predLocation.getNumEnteringEdges());

      for (CFANode predecessor : CFAUtils.predecessorsOf(predLocation)) {
        allSuccessors.addAll(factory.getState(predecessor));
      }

      return allSuccessors;
    }
  }
}
