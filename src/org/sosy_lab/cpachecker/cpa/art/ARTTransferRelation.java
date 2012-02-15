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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationMapping;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.ImmutableList;

public class ARTTransferRelation implements TransferRelation, StatisticsProvider {

  private final TransferRelation transferRelation;
  private RGLocationMapping locationMapping;
  private final int tid;

  private final Stats stats;

  public ARTTransferRelation(TransferRelation tr, RGLocationMapping locationMapping, int tid) {
    this.transferRelation = tr;
    this.locationMapping = locationMapping;
    this.tid = tid;
    this.stats = new Stats(tid);
  }

  protected void setLocationMapping(RGLocationMapping lm){
    locationMapping = lm;
  }

  @Override
  public Collection<ARTElement> getAbstractSuccessors(AbstractElement pElement, Precision pPrecision, CFAEdge edge)throws CPATransferException, InterruptedException {

    assert edge != null;
    ARTElement element = (ARTElement)pElement;

    ImmutableList<Integer> succLocCl = getSuccessorLocationClasses(element, edge);
    // TODO statistics
    if (succLocCl == null){
      return Collections.emptySet();
    }

    AbstractElement wrappedElement = element.getWrappedElement();
    Collection<? extends AbstractElement> successors = transferRelation.getAbstractSuccessors(wrappedElement, pPrecision, edge);


    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    Map<ARTElement, CFAEdge> parents = new HashMap<ARTElement, CFAEdge>(1);
    parents.put(element, edge);
    Collection<ARTElement> wrappedSuccessors = new ArrayList<ARTElement>();
    for (AbstractElement absElement : successors) {
      ARTElement successorElem = new ARTElement(absElement, parents, succLocCl);
      wrappedSuccessors.add(successorElem);
    }
    return wrappedSuccessors;
  }


  /**
   * Returns the location class of the successor element or null if the edge predecessor's and
   * element's classes don't match.
   * @param element
   * @param edge
   * @return
   */
  private ImmutableList<Integer> getSuccessorLocationClasses(ARTElement element, CFAEdge edge) {

    stats.locChecksNo++;
    stats.locProcessingTimer.start();

    ImmutableList<Integer> locCl = element.getLocationClasses();
    ImmutableList<Integer> succLocCl;

    if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
      RGCFAEdge rgEdge = (RGCFAEdge) edge;
      RGEnvTransition et = rgEdge.getRgEnvTransition();

      // check if the location classes of the predecessor and the element match
      ImmutableList<Integer> predClass = et.getSourceARTElement().getLocationClasses();
      if (!locCl.equals(predClass)){
        stats.locNegNo++;
        succLocCl = null;
      } else {
        succLocCl = et.getTargetARTElement().getLocationClasses();
      }
    } else {
      // local edge can always be applied; change the class for this thread
      List<Integer> list = new Vector<Integer>(locCl);
      Integer newClass = locationMapping.get(edge.getSuccessor());

      if (newClass == null){
        System.out.println(this.getClass());
      }

      assert newClass != null;
      list.set(tid, newClass);
      succLocCl = ImmutableList.copyOf(list);
    }

    stats.locProcessingTimer.stop();
    return succLocCl;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    return null;
  }

  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
  }

  public static class Stats implements Statistics {

    private final int tid;
    public final Timer locProcessingTimer = new Timer();
    public int locChecksNo = 0;
    public int locNegNo = 0;

    public Stats(int tid){
      this.tid = tid;
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("time on location class checks "+tid+":  " + locProcessingTimer);
      out.println("edges eliminated by loc. class "+tid+": " + formatString(locNegNo+"/"+locChecksNo));
    }

    @Override
    public String getName() {
      return "ARTTransferRelation "+tid;
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }

    private String formatString(String str){
      return String.format("  %7s", str);
    }

  }



}
