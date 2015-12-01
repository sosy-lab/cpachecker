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
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.ImmutableSet;



public class AppliedCustomInstruction {

  private final CFANode ciStartNode;
  private final Collection<CFANode> ciEndNodes;
  private final List<String> inputVariables;
  private final List<String> outputVariables;
  private final List<String> inputVariablesAndConstants;
  private final Pair<List<String>, String> fakeDescription;
  private final SSAMap indicesForReturnVars;

  /**
   * Constructor of AppliedCustomInstruction.
   * Creates a AppliedCustomInstruction with a start node and a set of endNodes
   * @param pCiStartNode CFANode
   * @param pCiEndNodes ImmutableSet
   */
  public AppliedCustomInstruction (final CFANode pCiStartNode, final Collection<CFANode> pCiEndNodes,
      final List<String> pInputVariables, final List<String> pOutputVariables, final List<String> inputVarsAndConstants,
      final Pair<List<String>, String> pFakeDescription, final SSAMap pIndicesForReturnVars){

    ciStartNode = pCiStartNode;
    ciEndNodes = pCiEndNodes;
    inputVariables = pInputVariables;
    outputVariables = pOutputVariables;
    inputVariablesAndConstants = inputVarsAndConstants;
    fakeDescription = pFakeDescription;
    indicesForReturnVars = pIndicesForReturnVars;
  }

  /**
   * Compares the given AbstractState pState to ciStartNode
   * @param pState AbstractState
   * @return true if pState equals ciStartNode, false if not.
   * @throws CPAException if the given AbstractState pState cant't be extracted to a CFANode
   */
  public boolean isStartState (final AbstractState pState) throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);
    if (locState == null) {
      throw new CPAException("The State " + pState+ " has to contain a location state!");
    }

    return locState.equals(ciStartNode);
  }

  /**
   * Compares the given AbstractState pState to ciStartNode
   * @param pState AbstractState
   * @return true if pState equals ciEndNode, false if not.
   * @throws CPAException if the given AbstractState pState cant't be extracted to a CFANode
   */
  public boolean isEndState (final AbstractState pState) throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);
    if (locState == null) {
      throw new CPAException("The State " + pState+ " has to contain a location state!");
    }

    return ciEndNodes.contains(locState);
  }

  public Pair<List<String>, String> getFakeSMTDescription() {
    return fakeDescription;
  }

  public SSAMap getIndicesForReturnVars() {
    return indicesForReturnVars;
  }

  public Collection<CFANode> getStartAndEndNodes() {
    return ImmutableSet.<CFANode> builder().add(ciStartNode).addAll(ciEndNodes).build();
  }

  public List<String> getInputVariables() {
    return inputVariables;
  }

  public List<String> getOutputVariables() {
    return outputVariables;
  }

  public List<String> getInputVariablesAndConstants() {
    return inputVariablesAndConstants;
  }
}
