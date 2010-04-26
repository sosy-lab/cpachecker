/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.mustmay;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class MustMayAnalysisTransferRelation implements TransferRelation {

  private TransferRelation mMustTransferRelation;
  private TransferRelation mMayTransferRelation;

  private MustMayAnalysisElement mBottomElement;

  private AbstractElement mMayBottomElement;

  public MustMayAnalysisTransferRelation(TransferRelation pMustTransferRelation, TransferRelation pMayTransferRelation, MustMayAnalysisElement pBottomElement) {
    assert(pMustTransferRelation != null);
    assert(pMayTransferRelation != null);
    assert(pBottomElement != null);

    mMustTransferRelation = pMustTransferRelation;
    mMayTransferRelation = pMayTransferRelation;

    mBottomElement = pBottomElement;
    mMayBottomElement = mBottomElement.getMayElement();
  }

  @Override
  // TODO: public <T extends AbstractElement> Collection<AbstractElement> getAbstractSuccessors(T pCurrentElement, Precision pPrecision, CFAEdge pCfaEdge)
  public Collection<AbstractElement> getAbstractSuccessors(AbstractElement pCurrentElement, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {

    assert(pCurrentElement != null);
    assert(pCurrentElement instanceof MustMayAnalysisElement);

    assert(pPrecision != null);
    assert(pPrecision instanceof MustMayAnalysisPrecision);

    assert(pCfaEdge != null);

    MustMayAnalysisElement lCurrentElement = (MustMayAnalysisElement)pCurrentElement;

    AbstractElement lCurrentMayElement = lCurrentElement.getMayElement();
    AbstractElement lCurrentMustElement = lCurrentElement.getMustElement();

    MustMayAnalysisPrecision lPrecision = (MustMayAnalysisPrecision)pPrecision;

    Collection<? extends AbstractElement> lMaySuccessors = mMayTransferRelation.getAbstractSuccessors(lCurrentMayElement, lPrecision.getMayPrecision(), pCfaEdge);

    HashSet<AbstractElement> lConsolidatedMaySuccessors = new HashSet<AbstractElement>();

    for (AbstractElement lSuccessor : lMaySuccessors) {
      if (!lSuccessor.equals(mMayBottomElement)) {
        // lSuccessor is not bottom element of may analysis
        lConsolidatedMaySuccessors.add(lSuccessor);
      }
    }

    if (lConsolidatedMaySuccessors.isEmpty()) {
      // if there are no may successors, then there can't be
      // must successors, thus return the empty set
      // TODO: discuss that bottom elements is not allowed to be returned
      // what says paper about this?
      return Collections.emptySet();
    }

    Collection<? extends AbstractElement> lMustSuccessors = mMustTransferRelation.getAbstractSuccessors(lCurrentMustElement, lPrecision.getMustPrecision(), pCfaEdge);

    HashSet<AbstractElement> lConsolidatedMustSuccessors = new HashSet<AbstractElement>();

    lConsolidatedMustSuccessors.addAll(lMustSuccessors);

    if (lConsolidatedMustSuccessors.isEmpty()) {
      // add bottom element for cross product generation
      lConsolidatedMustSuccessors.add(mBottomElement.getMustElement());
    }

    HashSet<AbstractElement> lSuccessors = new HashSet<AbstractElement>();

    // generate cross product
    for (AbstractElement lMaySuccessor : lConsolidatedMaySuccessors) {
      for (AbstractElement lMustSuccessor : lConsolidatedMustSuccessors) {
        // TODO: the strengthening operator of the must transfer relation has to guarantee (and establish) the subset relation of concretizations
        Collection<? extends AbstractElement> lStrengthenList = mMustTransferRelation.strengthen(lMustSuccessor, Collections.singletonList(lMaySuccessor), pCfaEdge, lPrecision.getMustPrecision());

        if (lStrengthenList == null) {
          lSuccessors.add(new MustMayAnalysisElement(lMustSuccessor, lMaySuccessor));
        }
        else{
          if (lStrengthenList.isEmpty()) {
            lSuccessors.add(new MustMayAnalysisElement(mBottomElement, lMaySuccessor));
          }
          else {
            for (AbstractElement lStrengthenedSuccessor : lStrengthenList) {
              lSuccessors.add(new MustMayAnalysisElement(lStrengthenedSuccessor, lMaySuccessor));
            }
          }
        }
      }
    }

    return lSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
      List<AbstractElement> pOtherElements, CFAEdge pCfaEdge,
      Precision pPrecision) throws CPATransferException {
    // TODO Auto-generated method stub
    return null;
  }

}
