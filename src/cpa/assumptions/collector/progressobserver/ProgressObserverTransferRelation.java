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
package cpa.assumptions.collector.progressobserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

/**
 * Transfer relation for the analysis controller. Note that we
 * use side-effects (related to the hashtable) to improve performance.
 * @author g.theoduloz
 */
public class ProgressObserverTransferRelation implements TransferRelation {

  @Override
  public Collection<ProgressObserverElement> getAbstractSuccessors(
      AbstractElement el, Precision pPrecision, CFAEdge edge)
      throws CPATransferException {
    ProgressObserverElement pre = (ProgressObserverElement)el;
    List<StopHeuristicsData> preData = pre.getComponents();
    List<StopHeuristicsData> postData = new ArrayList<StopHeuristicsData>(preData.size());

    for (StopHeuristicsData d : preData) {
      StopHeuristicsData postD = d.processEdge(edge);
      if (postD.isBottom())
        // 'squash' to bottom
        return Collections.emptySet();
      else
        postData.add(postD);
    }
    return Collections.singleton(new ProgressObserverElement(postData));
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
      List<AbstractElement> pOtherElements, CFAEdge pCfaEdge,
      Precision pPrecision) throws CPATransferException {
    return null;
  }

}
