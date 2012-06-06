/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class ARGReducer implements Reducer {

  private final Reducer wrappedReducer;

  public ARGReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractState getVariableReducedElement(
      AbstractState pExpandedElement, Block pContext,
      CFANode pLocation) {

    return new ARGElement(wrappedReducer.getVariableReducedElement(((ARGElement)pExpandedElement).getWrappedElement(), pContext, pLocation), null);
  }

  @Override
  public AbstractState getVariableExpandedElement(
      AbstractState pRootElement, Block pReducedContext,
      AbstractState pReducedElement) {

    return new ARGElement(wrappedReducer.getVariableExpandedElement(((ARGElement)pRootElement).getWrappedElement(), pReducedContext, ((ARGElement)pReducedElement).getWrappedElement()), null);
  }

  @Override
  public Object getHashCodeForElement(AbstractState pElementKey, Precision pPrecisionKey) {

    return wrappedReducer.getHashCodeForElement(((ARGElement)pElementKey).getWrappedElement(), pPrecisionKey);
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision,
      Block pContext) {
    return wrappedReducer.getVariableReducedPrecision(pPrecision, pContext);
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return wrappedReducer.getVariableExpandedPrecision(rootPrecision, rootContext, reducedPrecision);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return wrappedReducer.measurePrecisionDifference(pPrecision, pOtherPrecision);
  }

}
