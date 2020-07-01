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
package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ARGTransferRelation extends AbstractSingleWrapperTransferRelation {

  public ARGTransferRelation(TransferRelation tr) {
    super(tr);
  }

  @Override
  public Collection<ARGState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    ARGState element = (ARGState)pElement;

    // covered elements may be in the reached set, but should always be ignored
    if (element.isCovered()) {
      return ImmutableSet.of();
    }

    element.markExpanded();

    AbstractState wrappedState = element.getWrappedState();
    Collection<? extends AbstractState> successors;
    try {
      successors = transferRelation.getAbstractSuccessors(wrappedState, pPrecision);
    } catch (UnrecognizedCodeException e) {
      // setting parent of this unsupported code part
      e.setParentState(element);
      throw e;
    }

    if (successors.isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableList.Builder<ARGState> wrappedSuccessors = ImmutableList.builder();
    for (AbstractState absElement : successors) {
      ARGState successorElem = new ARGState(absElement, element);
      wrappedSuccessors.add(successorElem);
    }

    return wrappedSuccessors.build();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "ARGCPA needs to be used as the outer-most CPA,"
        + " thus it does not support returning successors for a single edge.");
  }

  @Override
  @Nullable
  public <T extends TransferRelation> T retrieveWrappedTransferRelation(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(transferRelation.getClass())) {
      return pType.cast(transferRelation);
    } else if (transferRelation instanceof WrapperTransferRelation) {
      return ((WrapperTransferRelation) transferRelation).retrieveWrappedTransferRelation(pType);
    } else {
      return null;
    }
  }

  @Override
  public Iterable<TransferRelation> getWrappedTransferRelations() {
    return ImmutableList.of(transferRelation);
  }
}
