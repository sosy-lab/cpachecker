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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationTM;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public class LocationTransferRelation implements TransferRelationTM {

  private final LocationStateFactory factory;

  public LocationTransferRelation(LocationStateFactory pFactory) {
    factory = pFactory;
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {

    CFANode node = ((LocationState) element).getLocationNode();

    if (CFAUtils.allLeavingEdges(node).contains(cfaEdge)) {
      return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
    }

    return Collections.emptySet();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element,
      Precision prec) throws CPATransferException {

    CFANode node = ((LocationState) element).getLocationNode();
    return CFAUtils.successorsOf(node).transform(n -> factory.getState(n)).toList();
  }

  @Override
  public Collection<Pair<AbstractState, InferenceObject>> getAbstractSuccessors(AbstractState pState, InferenceObject pInferenceObject, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (pInferenceObject == TauInferenceObject.getInstance()) {
      return from(getAbstractSuccessors(pState, pPrecision))
          .transform(s -> Pair.of((AbstractState) s, (InferenceObject) EmptyInferenceObject.getInstance()))
          .toSet();
    } else {
      Preconditions.checkArgument(pInferenceObject instanceof EmptyInferenceObject);
      return Collections.singleton(Pair.of(pState, EmptyInferenceObject.getInstance()));
    }
  }

  @Override
  public Collection<Pair<AbstractState, InferenceObject>> getAbstractSuccessorForEdge(AbstractState pState, InferenceObject pInferenceObject, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    if (pInferenceObject == TauInferenceObject.getInstance()) {
      return from(getAbstractSuccessorsForEdge(pState, pPrecision, pCfaEdge))
          .transform(s -> Pair.of((AbstractState) s, (InferenceObject) EmptyInferenceObject.getInstance()))
          .toSet();
    } else {
      Preconditions.checkArgument(pInferenceObject instanceof EmptyInferenceObject);
      return Collections.singleton(Pair.of(pState, EmptyInferenceObject.getInstance()));
    }
  }
}
