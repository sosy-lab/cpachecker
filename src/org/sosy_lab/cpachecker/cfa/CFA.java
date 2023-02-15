// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public interface CFA {

  default MachineModel getMachineModel() {
    return getMetadata().getMachineModel();
  }

  boolean isEmpty();

  int getNumberOfFunctions();

  NavigableSet<String> getAllFunctionNames();

  Collection<FunctionEntryNode> getAllFunctionHeads();

  FunctionEntryNode getFunctionHead(String name);

  NavigableMap<String, FunctionEntryNode> getAllFunctions();

  Collection<CFANode> getAllNodes();

  default FunctionEntryNode getMainFunction() {
    return getMetadata().getMainFunctionEntry();
  }

  default Optional<LoopStructure> getLoopStructure() {
    return getMetadata().getLoopStructure();
  }

  default Optional<ImmutableSet<CFANode>> getAllLoopHeads() {
    return getLoopStructure().map(loopStructure -> loopStructure.getAllLoopHeads());
  }

  default Optional<VariableClassification> getVarClassification() {
    return getMetadata().getVariableClassification();
  }

  default Optional<LiveVariables> getLiveVariables() {
    return getMetadata().getLiveVariables();
  }

  default Language getLanguage() {
    return getMetadata().getLanguage();
  }

  default List<Path> getFileNames() {
    return getMetadata().getFileNames();
  }

  /**
   * Returns the metadata associated with this CFA.
   *
   * <p>CFA metadata stores additional data about a CFA and may contain all data that isn't
   * necessary for the actual graph representation of a program.
   *
   * @return the metadata associated with this CFA
   */
  CfaMetadata getMetadata();

  /**
   * Returns a {@link CfaNetwork} view that represents this {@link CFA}.
   *
   * <p>All changes to this CFA are reflected in the returned {@link CfaNetwork}. The CFA
   * represented by the returned {@link CfaNetwork} always matches the CFA represented by its
   * individual elements (e.g., {@link CFAEdge#getSuccessor()} and {@link
   * CfaNetwork#successor(CFAEdge)} always return the same value).
   *
   * <p>IMPORTANT: This CFA must not contain any parallel edges (i.e., edges that connect the same
   * nodes in the same order) and never add them in the future (if the CFA is mutable).
   * Additionally, the collections returned by {@link CFA#getAllNodes()} and {@link
   * CFA#getAllFunctionHeads()} must not contain any duplicates and never add them in the future. Be
   * aware that these requirements are not enforced if Java assertions are disabled.
   *
   * @return a {@link CfaNetwork} view that represents this {@link CFA}
   */
  default CfaNetwork asNetwork() {
    return CfaNetwork.wrap(this);
  }
}
