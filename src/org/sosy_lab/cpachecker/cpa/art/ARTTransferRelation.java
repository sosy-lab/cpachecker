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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ARTTransferRelation implements TransferRelation {

  private final TransferRelation transferRelation;

  public ARTTransferRelation(TransferRelation tr) {
    transferRelation = tr;
  }

  @Override
  public Collection<ARTElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    ARTElement element = (ARTElement)pElement;

    AbstractElement wrappedElement = element.getWrappedElement();
    Collection<? extends AbstractElement> successors = null;
    if (pCfaEdge == null && element.getEnvEdgesToBeApplied() != null && !element.getEnvEdgesToBeApplied().isEmpty()){
      Vector<AbstractElement> allSucc = new Vector<AbstractElement>();
      for (RelyGuaranteeCFAEdge rgEdge : element.getEnvEdgesToBeApplied()){
        allSucc.addAll(transferRelation.getAbstractSuccessors(wrappedElement, pPrecision, rgEdge));
      }
      element.getEnvEdgesToBeApplied().clear();
      successors = allSucc;
    } else {
      successors = transferRelation.getAbstractSuccessors(wrappedElement, pPrecision, pCfaEdge);
    }

    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    Collection<ARTElement> wrappedSuccessors = new ArrayList<ARTElement>();
    for (AbstractElement absElement : successors) {
      ARTElement successorElem = new ARTElement(absElement, element);
      wrappedSuccessors.add(successorElem);
    }
    return wrappedSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    return null;
  }
}
