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
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class FunctionStatements {

  public final ImmutableListMultimap<ThreadEdge, FunctionParameterAssignment> parameterAssignments;

  private final ImmutableMap<ThreadEdge, FunctionParameterAssignment> startRoutineArgAssignments;

  public final ImmutableMap<ThreadEdge, FunctionReturnValueAssignment> returnValueAssignments;

  public final ImmutableMap<ThreadEdge, FunctionReturnValueAssignment> startRoutineExitAssignments;

  FunctionStatements(
      ImmutableListMultimap<ThreadEdge, FunctionParameterAssignment> pParameterAssignments,
      ImmutableMap<ThreadEdge, FunctionParameterAssignment> pStartRoutineArgAssignments,
      ImmutableMap<ThreadEdge, FunctionReturnValueAssignment> pReturnValueAssignments,
      ImmutableMap<ThreadEdge, FunctionReturnValueAssignment> pStartRoutineExitAssignments) {

    parameterAssignments = pParameterAssignments;
    startRoutineArgAssignments = pStartRoutineArgAssignments;
    returnValueAssignments = pReturnValueAssignments;
    startRoutineExitAssignments = pStartRoutineExitAssignments;
  }

  public Optional<FunctionParameterAssignment> tryGetStartRoutineArgAssignmentByThreadEdge(
      ThreadEdge pThreadEdge) {

    if (startRoutineArgAssignments.containsKey(pThreadEdge)) {
      return Optional.ofNullable(startRoutineArgAssignments.get(pThreadEdge));
    }
    return Optional.empty();
  }
}
