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
package org.sosy_lab.cpachecker.cpa.explicit;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class ExplicitReducer implements Reducer {

  private boolean occursInBlock(Block pBlock, String pVar) {
    // TODO could be more efficient (avoid linear runtime)
    for(ReferencedVariable referencedVar : pBlock.getReferencedVariables()) {
      if(referencedVar.getName().equals(pVar)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AbstractElement getVariableReducedElement(AbstractElement pExpandedElement, Block pContext, CFANode pCallNode) {
    ExplicitElement expandedElement = (ExplicitElement)pExpandedElement;

    ExplicitElement clonedElement = expandedElement.clone();
    for(String trackedVar : expandedElement.getTrackedVariableNames()) {
      if(!occursInBlock(pContext, trackedVar)) {
        clonedElement.deleteValue(trackedVar);
      }
    }

    return clonedElement;
  }

  @Override
  public AbstractElement getVariableExpandedElement(AbstractElement pRootElement, Block pReducedContext,
      AbstractElement pReducedElement) {
    ExplicitElement rootElement = (ExplicitElement)pRootElement;
    ExplicitElement reducedElement = (ExplicitElement)pReducedElement;

    ExplicitElement diffElement = rootElement.clone();
    for(String trackedVar : reducedElement.getTrackedVariableNames()) {
      diffElement.deleteValue(trackedVar);
    }
    //TODO: following is needed with threshold != inf
  /*  for(String trackedVar : diffElement.getTrackedVariableNames()) {
      if(occursInBlock(pReducedContext, trackedVar)) {
        diffElement.deleteValue(trackedVar);
      }
    }*/
    for(String trackedVar : reducedElement.getTrackedVariableNames()) {
      Long value = reducedElement.getValueFor(trackedVar);
      if(value != null) {
        diffElement.assignConstant(trackedVar, reducedElement.getValueFor(trackedVar));
      } else {
        diffElement.forget(trackedVar);
      }
    }

    return diffElement;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    ExplicitPrecision precision = (ExplicitPrecision)pPrecision;

    // TODO: anything meaningful we can do here?

    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {
    //ExplicitPrecision rootPrecision = (ExplicitPrecision)pRootPrecision;
    ExplicitPrecision reducedPrecision = (ExplicitPrecision)pReducedPrecision;

    // TODO: anything meaningful we can do here?

    return reducedPrecision;
  }

  @Override
  public Object getHashCodeForElement(AbstractElement pElementKey, Precision pPrecisionKey) {
    ExplicitElement elementKey = (ExplicitElement)pElementKey;
    ExplicitPrecision precisionKey = (ExplicitPrecision)pPrecisionKey;

    return Pair.of(elementKey.getConstantsMap(), precisionKey);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

}
