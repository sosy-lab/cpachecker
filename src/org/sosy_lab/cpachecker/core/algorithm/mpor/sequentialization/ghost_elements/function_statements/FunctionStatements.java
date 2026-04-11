// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

public record FunctionStatements(
    ImmutableListMultimap<CFAEdgeForThread, FunctionParameterAssignment> parameterAssignments,
    ImmutableMap<CFAEdgeForThread, FunctionParameterAssignment> startRoutineArgAssignments,
    ImmutableMap<CFAEdgeForThread, FunctionReturnValueAssignment> returnValueAssignments,
    ImmutableMap<CFAEdgeForThread, FunctionReturnValueAssignment> startRoutineExitAssignments) {

  public Optional<FunctionParameterAssignment> tryGetStartRoutineArgAssignmentByThreadEdge(
      CFAEdgeForThread pThreadEdge) {

    if (startRoutineArgAssignments.containsKey(pThreadEdge)) {
      return Optional.ofNullable(startRoutineArgAssignments.get(pThreadEdge));
    }
    return Optional.empty();
  }
}
