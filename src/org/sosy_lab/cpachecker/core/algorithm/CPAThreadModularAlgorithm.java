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
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CompatibilityCheck;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisTM;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCovering;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationTM;
import org.sosy_lab.cpachecker.core.interfaces.WaitlistElement;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ThreadModularReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class CPAThreadModularAlgorithm extends AbstractCPAAlgorithm {

  private final CompatibilityCheck compatibleCheck;
  private final StopOperator stopForInferenceObject;
  private final MergeOperator mergeForInferenceObject;


  protected CPAThreadModularAlgorithm(ConfigurableProgramAnalysisTM pCpa,
      LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      ForcedCovering pForcedCovering, boolean pIsImprecise) {
    super(pCpa, pLogger, pShutdownNotifier, pForcedCovering, pIsImprecise);
    compatibleCheck = pCpa.getCompatibilityCheck();
    stopForInferenceObject = pCpa.getStopForInferenceObject();
    mergeForInferenceObject = pCpa.getMergeForInferenceObject();
  }

  public static CPAThreadModularAlgorithm create(ConfigurableProgramAnalysisTM pCpa,
      LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    return new CPAThreadModularAlgorithm(pCpa, pLogger, pShutdownNotifier, null, false);
  }

  @Override
  protected void frontier(ReachedSet pReached, AbstractState pSuccessor, Precision pPrecision) {
    if (pSuccessor instanceof InferenceObject) {
      Collection<AbstractState> states = ((ThreadModularReachedSet)pReached).getStates();

      for (AbstractState state : states) {
        if (compatibleCheck.compatible(state, (InferenceObject) pSuccessor)) {
          pReached.reAddToWaitlist(new ThreadModularWaitlistElement(state, (InferenceObject) pSuccessor, pPrecision));
        }
      }
    } else {
      Collection<InferenceObject> objects = ((ThreadModularReachedSet) pReached).getInferenceObjects();

      for (InferenceObject object : objects) {
        if (compatibleCheck.compatible(pSuccessor, object)) {
          pReached.reAddToWaitlist(new ThreadModularWaitlistElement(pSuccessor, object, pPrecision));
        }
      }
      pReached.add(pSuccessor, pPrecision);
    }
  }

  @Override
  protected void update(
      ReachedSet pReachedSet,
      List<AbstractState> pToRemove,
      List<Pair<AbstractState, Precision>> pToAdd) {
    Preconditions.checkArgument(pToRemove.size() == pToAdd.size());

    for (int i = 0; i < pToRemove.size(); i++) {
      AbstractState toRemove = pToRemove.get(i);
      Pair<AbstractState, Precision> pair = pToAdd.get(i);

      pReachedSet.remove(toRemove);
      frontier(pReachedSet, pair.getFirst(), pair.getSecond());

    }
  }

  @Override
  protected Collection<Pair<? extends AbstractState, ? extends Precision>> getAbstractSuccessors(
      WaitlistElement pElement) throws CPATransferException, InterruptedException {

    Preconditions.checkArgument(pElement instanceof ThreadModularWaitlistElement);

    AbstractState state = ((ThreadModularWaitlistElement) pElement).getState();
    Precision precision = ((ThreadModularWaitlistElement) pElement).getPrecision();
    InferenceObject object = ((ThreadModularWaitlistElement) pElement).getInferenceObject();

    Collection<Pair<AbstractState, InferenceObject>> successors;
    successors = ((TransferRelationTM) transferRelation).getAbstractSuccessors(state, object, precision);

    Set<Pair<? extends AbstractState, ? extends Precision>> result = new HashSet<>();
    for (Pair<AbstractState, InferenceObject> tmpPair : successors) {
      result.add(Pair.of(tmpPair.getFirst(), precision));
      InferenceObject newObject = tmpPair.getSecond();
      if (object != null && newObject != EmptyInferenceObject.getInstance()) {
        result.add(Pair.of(newObject, precision));
      }
    }
    return result;
  }

  @Override
  protected boolean stop(AbstractState pSuccessor, Collection<AbstractState> pReached, Precision pSuccessorPrecision) throws CPAException, InterruptedException {
    if (pSuccessor instanceof InferenceObject) {
      return stopForInferenceObject.stop(pSuccessor, pReached, pSuccessorPrecision);
    } else {
      return stopOperator.stop(pSuccessor, pReached, pSuccessorPrecision);
    }
  }

  @Override
  protected AbstractState merge(AbstractState pSuccessor, AbstractState pReachedState, Precision pSuccessorPrecision) throws CPAException, InterruptedException {
    if (pSuccessor instanceof InferenceObject) {
      return mergeForInferenceObject.merge(pSuccessor, pReachedState, pSuccessorPrecision);
    } else {
      return mergeOperator.merge(pSuccessor, pReachedState, pSuccessorPrecision);
    }
  }
}
