/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

class Witness {
  private final String entryStateNodeId;
  private final Multimap<String, Edge> leavingEdges;
  private final Multimap<String, Edge> enteringEdges;
  private final WitnessOptions witnessOptions;
  private final SetMultimap<String, NodeFlag> nodeFlags;
  private final Multimap<String, Property> violatedProperties;
  private final Map<String, ExpressionTree<Object>> stateInvariants;
  private final Map<String, ExpressionTree<Object>> stateQuasiInvariants;
  private final Map<String, String> stateScopes;
  private final Set<String> invariantExportStates;

  public Witness(
      String pEntryStateNodeId,
      Multimap<String, Edge> pLeavingEdges,
      Multimap<String, Edge> pEnteringEdges,
      WitnessOptions pWitnessOptions,
      SetMultimap<String, NodeFlag> pNodeFlags,
      Multimap<String, Property> pViolatedProperties,
      Map<String, ExpressionTree<Object>> pStateInvariants,
      Map<String, ExpressionTree<Object>> pStateQuasiInvariants,
      Map<String, String> pStateScopes,
      Set<String> pInvariantExportStates) {
    entryStateNodeId = pEntryStateNodeId;
    leavingEdges = pLeavingEdges;
    enteringEdges = pEnteringEdges;
    witnessOptions = pWitnessOptions;
    nodeFlags = pNodeFlags;
    violatedProperties = pViolatedProperties;
    stateInvariants = pStateInvariants;
    stateQuasiInvariants = pStateQuasiInvariants;
    stateScopes = pStateScopes;
    invariantExportStates = pInvariantExportStates;
  }

  public String getEntryStateNodeId() {
    return entryStateNodeId;
  }

  /**
   * Returns a {@link Multimap} from a state's id {@link String} to its leaving edges {@link Edge}
   */
  public Multimap<String, Edge> getLeavingEdges() {
    return leavingEdges;
  }

  public Multimap<String, Edge> getEnteringEdges() {
    return enteringEdges;
  }

  public WitnessOptions getWitnessOptions() {
    return witnessOptions;
  }

  public SetMultimap<String, NodeFlag> getNodeFlags() {
    return nodeFlags;
  }

  public Multimap<String, Property> getViolatedProperties() {
    return violatedProperties;
  }

  public ExpressionTree<Object> getStateInvariant(String pStateId) {
    ExpressionTree<Object> result = stateInvariants.get(pStateId);
    if (result == null) {
      return ExpressionTrees.getTrue();
    }
    return result;
  }

  public boolean hasQuasiInvariant(String pEntryStateNodeId) {
    return stateQuasiInvariants.containsKey(pEntryStateNodeId);
  }

  public ExpressionTree<Object> getQuasiInvariant(final String pNodeId) {
    ExpressionTree<Object> result = stateQuasiInvariants.get(pNodeId);
    if (result == null) {
      return ExpressionTrees.getFalse();
    }
    return result;
  }

  public Map<String, String> getStateScopes() {
    return stateScopes;
  }

  public Set<String> getInvariantExportStates() {
    return invariantExportStates;
  }
}