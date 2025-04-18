// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class FunctionStatements {

  public final ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>>
      parameterAssignments;

  public final ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
      returnValueAssignments;

  public FunctionStatements(
      ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>> pParameterAssignments,
      ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
          pReturnValueAssignments) {

    parameterAssignments = pParameterAssignments;
    returnValueAssignments = pReturnValueAssignments;
  }
}
