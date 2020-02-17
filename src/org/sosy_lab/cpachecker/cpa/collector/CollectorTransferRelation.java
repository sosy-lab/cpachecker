/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CollectorTransferRelation implements TransferRelation {

  private final TransferRelation transferRelation;
  private final LogManager logger;
  private final ArrayList<ARGState> parents = new ArrayList<>();


  public CollectorTransferRelation(TransferRelation tr, LogManager trLogger) {
    transferRelation = tr;
    logger = trLogger;
  }


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision)
      throws CPATransferException, InterruptedException {

  assert pElement instanceof CollectorState;

    ARGState wrappedState = (ARGState) ((CollectorState) pElement).getWrappedState();

    Collection<? extends AbstractState> successors;
    assert transferRelation instanceof ARGTransferRelation : "Transfer relation no ARG transfer"
        + " relation, but " + transferRelation.getClass().getSimpleName();

    successors = transferRelation.getAbstractSuccessors(Objects.requireNonNull(wrappedState), pPrecision);


    Collection<AbstractState> wrappedSuccessors = new ArrayList<>();
    for (AbstractState absElement : successors) {
      ARGState succARG = (ARGState) absElement;
      Collection<ARGState> wrappedParent = succARG.getParents();
      parents.addAll(wrappedParent);
      myARGState mytransferARG =
          new myARGState(succARG, wrappedState, parents, null, false, logger);
      CollectorState successorElem =
          new CollectorState(absElement, null, mytransferARG, false, null, null, null, logger);
      wrappedSuccessors.add(successorElem);
      parents.clear();
    }

    return wrappedSuccessors;

  }

  // same as in ARGTransferRelation
  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "ARGCPA needs to be used as the outer-most CPA,"
            + " thus it does not support returning successors for a single edge.");
  }
}
