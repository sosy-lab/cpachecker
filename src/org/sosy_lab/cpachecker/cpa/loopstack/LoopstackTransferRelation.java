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
package org.sosy_lab.cpachecker.cpa.loopstack;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFA.Loop;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.loopstack")
public class LoopstackTransferRelation implements TransferRelation {

  @Option
  private int maxLoopIterations = 0; 
  
  private final Multimap<String, Loop> loops = CFACreator.loops;
  
  private final Map<CFAEdge, Loop> loopEntryEdges;
  private final Map<CFAEdge, Loop> loopExitEdges;
  
  private final Multimap<CFANode, Loop> loopHeads;
  
  public LoopstackTransferRelation() throws CPAException {
    if (loops == null) {
      throw new CPAException("LoopstackCPA does not work without loop information!");
    }
    ImmutableMap.Builder<CFAEdge, Loop> entryEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> exitEdges  = ImmutableMap.builder();
    ImmutableMultimap.Builder<CFANode, Loop> heads = ImmutableMultimap.builder();
    
    for (Loop l : loops.values()) {
      for (CFAEdge e : l.getIncomingEdges()) {
        entryEdges.put(e, l);
      }
      for (CFAEdge e : l.getOutgoingEdges()) {
        exitEdges.put(e, l);
      }
      for (CFANode h : l.getLoopHeads()) {
        heads.put(h, l);
      }
    }
    loopEntryEdges = entryEdges.build();
    loopExitEdges = exitEdges.build();
    loopHeads = heads.build();
  }
  
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    if (pCfaEdge instanceof FunctionCallEdge) {
      // such edges do never change loop stack status 
      // Return here because they might be mis-classified as exit edges
      // because our idea of a loop contains only those nodes within the same function
      return Collections.singleton(pElement); 
    }
    
    CFANode loc = pCfaEdge.getSuccessor();
    LoopstackElement e = (LoopstackElement)pElement;

    Loop oldLoop = loopExitEdges.get(pCfaEdge);
    if (oldLoop != null) {
      assert oldLoop.equals(e.getLoop()) : e + " " + oldLoop + " " + pCfaEdge;
      e = e.getPreviousElement();
    }
    
    if (pCfaEdge instanceof FunctionReturnEdge) {
      // such edges may be real loop-exit edges "while () { return; }",
      // but never loop-entry edges
      // Return here because they might be mis-classified as entry edges 
      return Collections.singleton(pElement); 
    }
    
    Loop newLoop = loopEntryEdges.get(pCfaEdge);
    if (newLoop != null) {
      e = new LoopstackElement(e, newLoop, 0, false);
    }
    
    Collection<Loop> loops = loopHeads.get(loc);
    assert loops.size() <= 1;
    if (loops.contains(e.getLoop())) {
      boolean stop = (maxLoopIterations > 0) && (e.getIteration() >= maxLoopIterations);
      e = new LoopstackElement(e.getPreviousElement(), e.getLoop(), e.getIteration()+1, stop);
    }
    
    return Collections.singleton(e); 
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}