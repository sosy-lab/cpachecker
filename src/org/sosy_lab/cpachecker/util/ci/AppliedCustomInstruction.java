/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;



public class AppliedCustomInstruction {

  private final CFANode ciStartNode;
  private final Collection<CFANode> ciEndNode;

  /**
   * Constructor of AppliedCustomInstruction.
   * Creates a AppliedCustomInstruction with a start node and a set of endNodes
   * @param pCiStartNode CFANode
   * @param pCiEndNode ImmutableSet
   */
  public AppliedCustomInstruction (final CFANode pCiStartNode, final Collection<CFANode> pCiEndNode){
    ciStartNode = pCiStartNode;
    ciEndNode = pCiEndNode;
  }

  /**
   * Compares the given AbstractState pState to ciStartNode
   * @param pState AbstractState
   * @return true if pState equals ciStartNode, false if not.
   * @throws CPAException if the given AbstractState pState cant't be extracted to a CFANode
   */
  public boolean isStartState (AbstractState pState) throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);
    if (locState == null) {
      throw new CPAException("TheState " + pState+ " has to contain a location state!");
    }

    return locState.equals(ciStartNode);
  }

  /**
   * Compares the given AbstractState pState to ciStartNode
   * @param pState AbstractState
   * @return true if pState equals ciEndNode, false if not.
   * @throws CPAException if the given AbstractState pState cant't be extracted to a CFANode
   */
  public boolean isEndState (AbstractState pState) throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);
    if (locState == null) {
      throw new CPAException("The State " + pState+ " has to contain a location state!");
    }

    return ciEndNode.contains(locState);
  }
}
