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

  MachineModel getMachineModel();

  boolean isEmpty();

  int getNumberOfFunctions();

  NavigableSet<String> getAllFunctionNames();

  Collection<FunctionEntryNode> getAllFunctionHeads();

  FunctionEntryNode getFunctionHead(String name);

  NavigableMap<String, FunctionEntryNode> getAllFunctions();

  Collection<CFANode> getAllNodes();

  FunctionEntryNode getMainFunction();

  Optional<LoopStructure> getLoopStructure();

  Optional<ImmutableSet<CFANode>> getAllLoopHeads();

  Optional<VariableClassification> getVarClassification();

  Optional<LiveVariables> getLiveVariables();

  Language getLanguage();

  List<Path> getFileNames();
}
