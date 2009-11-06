/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.invariant.controller;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;

/**
 * Transfer relation for the analysis controller. Note that we
 * use side-effects (related to the hashtable) to improve performance.
 * @author g.theoduloz
 */
public class AnalysisControllerTransferRelation implements TransferRelation {
  
  private final AnalysisControllerDomain domain;
  
  public AnalysisControllerTransferRelation(AnalysisControllerDomain d)
  {
    domain = d;
  }
  
  /**
   * @return true iff we should stop exploring because the threshold for
   *         this node is exceeded
   */
  protected boolean isThresholdExceeded(CFANode node, Context context)
  {
    return false;
  }
  
  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement el, CFAEdge edge, Precision p)
    throws CPATransferException
  {
    AnalysisControllerElement pre = (AnalysisControllerElement)el;
    List<StopHeuristicsData> preData = pre.getComponents();
    List<StopHeuristicsData> postData = new ArrayList<StopHeuristicsData>(preData.size());

    for (StopHeuristicsData d : preData) {
      StopHeuristicsData postD = d.processEdge(edge);
      if (postD.isBottom())
        // 'squash' to bottom
        return domain.getBottomElement();
      else
        postData.add(postD);
    }
    return new AnalysisControllerElement(postData);
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation pElement, Precision pPrecision)
      throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  @Override
  public AbstractElement strengthen(AbstractElement pElement,
      List<AbstractElement> pOtherElements, CFAEdge pCfaEdge,
      Precision pPrecision) throws CPATransferException {
    return null;
  }

}
