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
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public interface CFA extends CfaNetwork {

  default MachineModel getMachineModel() {
    return getMetadata().getMachineModel();
  }

  /**
   * Returns an immutable {@link CFA} that represents the same CFA as this {@link CFA}.
   *
   * @return an immutable {@link CFA} that represents the same CFA as this {@link CFA}.
   */
  @Override
  CFA immutableCopy();

  int getNumberOfFunctions();

  NavigableSet<String> getAllFunctionNames();

  FunctionEntryNode getFunctionHead(String name);

  NavigableMap<String, FunctionEntryNode> getAllFunctions();

  default FunctionEntryNode getMainFunction() {
    return getMetadata().getMainFunctionEntry();
  }

  default Optional<LoopStructure> getLoopStructure() {
    return getMetadata().getLoopStructure();
  }

  default Optional<ImmutableSet<CFANode>> getAllLoopHeads() {
    return getLoopStructure().map(LoopStructure::getAllLoopHeads);
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
}
