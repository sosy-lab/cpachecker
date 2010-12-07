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

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix="cpa.loopstack")
public class LoopstackTransferRelation implements TransferRelation {

  @Option
  private int maxLoopIterations = 0; 
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    CFANode loc = pCfaEdge.getSuccessor();
    LoopstackElement e = (LoopstackElement)pElement;
    LoopstackElement result;

    // TODO detect and handle loop exit edges!
    
    if (loc.isLoopStart()) {
      if (loc.equals(e.getLoopHeadNode())) {
        boolean stop = (maxLoopIterations > 0) && (e.getIteration() >= maxLoopIterations);
        result = new LoopstackElement(e.getPreviousElement(), loc, e.getIteration()+1, stop);
      } else {
        result = new LoopstackElement(e, loc, 1, false);
      }

    } else {
      result = e;
    }

    return Collections.singleton(result); 
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}