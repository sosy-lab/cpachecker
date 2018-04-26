/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.IOStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompositeStopForInferenceObjects implements IOStopOperator {

  private final ImmutableList<IOStopOperator> stopOperators;

  CompositeStopForInferenceObjects(ImmutableList<IOStopOperator> stopOperators) {
    this.stopOperators = stopOperators;
  }

  @Override
  public boolean stop(
      InferenceObject pState, Collection<InferenceObject> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    if (pState == EmptyInferenceObject.getInstance()) {
      return true;
    }

    CompositeInferenceObject compositeState = (CompositeInferenceObject) pState;
    CompositePrecision compositePrecision = (CompositePrecision) pPrecision;

    for (InferenceObject e : pReached) {
      if (stop(compositeState, (CompositeInferenceObject) e, compositePrecision)) {
        return true;
      }
    }
    return false;
  }

  private boolean stop(CompositeInferenceObject compositeState, CompositeInferenceObject compositeReachedState, CompositePrecision compositePrecision) throws CPAException, InterruptedException {
    List<InferenceObject> compositeElements = compositeState.getInferenceObjects();
    checkArgument(compositeElements.size() == stopOperators.size(), "State with wrong number of component states given");
    List<InferenceObject> compositeReachedStates = compositeReachedState.getInferenceObjects();

    List<Precision> compositePrecisions = compositePrecision.getWrappedPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      IOStopOperator stopOp = stopOperators.get(idx);

      InferenceObject absElem1 = compositeElements.get(idx);
      InferenceObject absElem2 = compositeReachedStates.get(idx);
      Precision prec = compositePrecisions.get(idx);

      if (!stopOp.stop(absElem1, Collections.singleton(absElem2), prec)) {
        return false;
      }
    }
    return true;
  }
}
