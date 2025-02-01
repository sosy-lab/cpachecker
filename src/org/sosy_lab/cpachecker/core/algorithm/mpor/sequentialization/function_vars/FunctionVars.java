// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

public class FunctionVars {

  public final ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>>
      parameterAssignments;

  public final ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
      returnValueAssignments;

  public final ImmutableMap<ThreadEdge, FunctionReturnPcWrite> returnPcWrites;

  public final ImmutableMap<ThreadNode, FunctionReturnPcRead> returnPcReads;

  public FunctionVars(
      ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>> pParameterAssignments,
      ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>> pReturnValueAssignments,
      ImmutableMap<ThreadEdge, FunctionReturnPcWrite> pReturnPcWrites,
      ImmutableMap<ThreadNode, FunctionReturnPcRead> pReturnPcReads) {

    parameterAssignments = pParameterAssignments;
    returnValueAssignments = pReturnValueAssignments;
    returnPcWrites = pReturnPcWrites;
    returnPcReads = pReturnPcReads;
  }
}
