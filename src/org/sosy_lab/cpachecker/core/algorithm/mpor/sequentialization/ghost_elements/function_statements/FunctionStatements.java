// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class FunctionStatements {

  public final ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>>
      parameterAssignments;

  private final ImmutableMap<ThreadEdge, FunctionParameterAssignment> startRoutineArgAssignments;

  public final ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
      returnValueAssignments;

  public final ImmutableMap<ThreadEdge, FunctionReturnValueAssignment> startRoutineExitAssignments;

  public FunctionStatements(
      ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>> pParameterAssignments,
      ImmutableMap<ThreadEdge, FunctionParameterAssignment> pStartRoutineArgAssignments,
      ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>> pReturnValueAssignments,
      ImmutableMap<ThreadEdge, FunctionReturnValueAssignment> pStartRoutineExitAssignments) {

    parameterAssignments = pParameterAssignments;
    startRoutineArgAssignments = pStartRoutineArgAssignments;
    returnValueAssignments = pReturnValueAssignments;
    startRoutineExitAssignments = pStartRoutineExitAssignments;
  }

  public FunctionParameterAssignment getStartRoutineArgAssignmentByThreadEdge(
      ThreadEdge pThreadEdge) {

    assert startRoutineArgAssignments.containsKey(pThreadEdge)
        : "startRoutineArgAssignments does not contain pThreadEdge";
    return startRoutineArgAssignments.get(pThreadEdge);
  }
}
