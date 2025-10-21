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

public class FunctionStatements {

  public final ImmutableListMultimap<CFAEdgeForThread, FunctionParameterAssignment>
      parameterAssignments;

  private final ImmutableMap<CFAEdgeForThread, FunctionParameterAssignment>
      startRoutineArgAssignments;

  public final ImmutableMap<CFAEdgeForThread, FunctionReturnValueAssignment> returnValueAssignments;

  public final ImmutableMap<CFAEdgeForThread, FunctionReturnValueAssignment>
      startRoutineExitAssignments;

  FunctionStatements(
      ImmutableListMultimap<CFAEdgeForThread, FunctionParameterAssignment> pParameterAssignments,
      ImmutableMap<CFAEdgeForThread, FunctionParameterAssignment> pStartRoutineArgAssignments,
      ImmutableMap<CFAEdgeForThread, FunctionReturnValueAssignment> pReturnValueAssignments,
      ImmutableMap<CFAEdgeForThread, FunctionReturnValueAssignment> pStartRoutineExitAssignments) {

    parameterAssignments = pParameterAssignments;
    startRoutineArgAssignments = pStartRoutineArgAssignments;
    returnValueAssignments = pReturnValueAssignments;
    startRoutineExitAssignments = pStartRoutineExitAssignments;
  }

  public Optional<FunctionParameterAssignment> tryGetStartRoutineArgAssignmentByThreadEdge(
      CFAEdgeForThread pThreadEdge) {

    if (startRoutineArgAssignments.containsKey(pThreadEdge)) {
      return Optional.ofNullable(startRoutineArgAssignments.get(pThreadEdge));
    }
    return Optional.empty();
  }
}
