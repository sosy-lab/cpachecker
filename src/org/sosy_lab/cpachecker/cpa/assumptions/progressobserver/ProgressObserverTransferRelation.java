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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Transfer relation for the analysis controller. Note that we
 * use side-effects (related to the hashtable) to improve performance.
 * @author g.theoduloz
 */
public class ProgressObserverTransferRelation implements TransferRelation {

  private final ImmutableList<StopHeuristics<? extends StopHeuristicsData>> heuristics;
  private final LogManager logger;

  public ProgressObserverTransferRelation(ProgressObserverCPA aCPA) {
    heuristics = aCPA.getEnabledHeuristics();
    logger = aCPA.getLogger();
  }

  @Override
  public Collection<ProgressObserverElement> getAbstractSuccessors(
      AbstractElement el, Precision pPrecision, CFAEdge edge)
      throws CPATransferException {
    ProgressObserverElement pre = (ProgressObserverElement)el;

    if (pre.mustDumpAssumptionForAvoidance())
      // i.e., it is bottom; only needed if assumption collection
      // is not used
      return Collections.emptySet();

    List<StopHeuristicsData> preData = pre.getComponents();
    List<StopHeuristicsData> postData = new ArrayList<StopHeuristicsData>(preData.size());

    Iterator<StopHeuristics<? extends StopHeuristicsData>> heuristicsIt = heuristics.iterator();
    Iterator<StopHeuristicsData> preIt = preData.iterator();

    while (preIt.hasNext()) {
      StopHeuristics<? extends StopHeuristicsData> h = heuristicsIt.next();
      StopHeuristicsData d = preIt.next();
      StopHeuristicsData postD = h.processEdge(d, edge);
      if (postD.isBottom()) {
        logger.log(Level.WARNING, "Giving up at edge", edge.toString(), "because of", h.getClass().getSimpleName());
        logger.log(Level.FINEST, "Observer element at the time was:", el.toString());
      }
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
