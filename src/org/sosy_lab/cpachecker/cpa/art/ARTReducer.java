/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class ARTReducer implements Reducer {

  private final Reducer wrappedReducer;
  private final Map<AbstractElement, AbstractElement> expandedToReducedCache = new HashMap<AbstractElement, AbstractElement>();

  public ARTReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext,
      CFANode pLocation) {

    AbstractElement reducedElement = expandedToReducedCache.get(pExpandedElement);
    if(reducedElement != null) {
      return reducedElement;
    }

    ARTElement aElement = (ARTElement) pExpandedElement;
    reducedElement = new ARTElement(wrappedReducer.getVariableReducedElement(aElement.getWrappedElement(), pContext, pLocation), null, null, null, 0);

    expandedToReducedCache.put(pExpandedElement, reducedElement);

    return reducedElement;
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, Block pReducedContext,
      AbstractElement pReducedElement) {

    AbstractElement expandedElement = new ARTElement(wrappedReducer.getVariableExpandedElement(((ARTElement)pRootElement).getWrappedElement(), pReducedContext, ((ARTElement)pReducedElement).getWrappedElement()), null, null, null, 0);

    expandedToReducedCache.put(expandedElement, pReducedElement);

    return expandedElement;
  }

  @Override
  public Object getHashCodeForElement(AbstractElement pElementKey, Precision pPrecisionKey) {

    return wrappedReducer.getHashCodeForElement(((ARTElement)pElementKey).getWrappedElement(), pPrecisionKey);
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

}
