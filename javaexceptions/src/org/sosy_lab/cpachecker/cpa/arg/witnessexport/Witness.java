// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.automaton.VerificationTaskMetaData;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

/**
 * This class represents the information that is available for exporting a witness. Objects of this
 * class are designed to be immutable.
 */
public class Witness {
  private final WitnessType witnessType;
  private final String originFile;
  private final CFA cfa;
  private final VerificationTaskMetaData metaData;
  private final String entryStateNodeId;
  private final Multimap<String, Edge> leavingEdges;
  private final Multimap<String, Edge> enteringEdges;
  private final WitnessOptions witnessOptions;
  private final SetMultimap<String, NodeFlag> nodeFlags;
  private final Multimap<String, TargetInformation> violatedProperties;
  private final Map<String, ExpressionTree<Object>> stateInvariants;
  private final Map<String, ExpressionTree<Object>> stateQuasiInvariants;
  private final Map<String, String> stateScopes;
  private final Set<String> invariantExportStates;
  private final Multimap<String, ARGState> stateToARGStates;
  private final Multimap<Edge, CFAEdge> edgeToCFAEdges;

  public Witness(
      WitnessType pWitnessType,
      String pOriginFile,
      CFA pCfa,
      VerificationTaskMetaData pMetaData,
      String pEntryStateNodeId,
      Multimap<String, Edge> pLeavingEdges,
      Multimap<String, Edge> pEnteringEdges,
      WitnessOptions pWitnessOptions,
      SetMultimap<String, NodeFlag> pNodeFlags,
      Multimap<String, TargetInformation> pViolatedProperties,
      Map<String, ExpressionTree<Object>> pStateInvariants,
      Map<String, ExpressionTree<Object>> pStateQuasiInvariants,
      Map<String, String> pStateScopes,
      Set<String> pInvariantExportStates,
      Multimap<String, ARGState> pStateToARGStates,
      Multimap<Edge, CFAEdge> pEdgeToCFAEdges) {
    witnessType = pWitnessType;
    originFile = pOriginFile;
    cfa = pCfa;
    metaData = pMetaData;
    entryStateNodeId = pEntryStateNodeId;
    leavingEdges = ImmutableListMultimap.copyOf(pLeavingEdges);
    enteringEdges = ImmutableListMultimap.copyOf(pEnteringEdges);
    witnessOptions = pWitnessOptions;
    nodeFlags = ImmutableSetMultimap.copyOf(pNodeFlags);
    violatedProperties = ImmutableListMultimap.copyOf(pViolatedProperties);
    stateInvariants = ImmutableMap.copyOf(pStateInvariants);
    stateQuasiInvariants = ImmutableMap.copyOf(pStateQuasiInvariants);
    stateScopes = ImmutableMap.copyOf(pStateScopes);
    invariantExportStates = ImmutableSet.copyOf(pInvariantExportStates);
    stateToARGStates = ImmutableListMultimap.copyOf(pStateToARGStates);
    edgeToCFAEdges = ImmutableListMultimap.copyOf(pEdgeToCFAEdges);
  }

  public WitnessType getWitnessType() {
    return witnessType;
  }

  public String getOriginFile() {
    return originFile;
  }

  public CFA getCfa() {
    return cfa;
  }

  public VerificationTaskMetaData getMetaData() {
    return metaData;
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

  public Multimap<String, TargetInformation> getViolatedProperties() {
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

  public Collection<ARGState> getARGStatesFor(String id) {
    if (stateToARGStates.containsKey(id)) {
      return stateToARGStates.get(id);
    } else {
      return ImmutableList.of();
    }
  }

  public List<CFAEdge> getCFAEdgeFor(Edge edge) {
    if (edgeToCFAEdges.containsKey(edge)) {
      return ImmutableList.copyOf(edgeToCFAEdges.get(edge));
    } else {
      return ImmutableList.of();
    }
  }
}
