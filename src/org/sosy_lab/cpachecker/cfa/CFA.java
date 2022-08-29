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

  CfaMetadata getMetadata();
}
