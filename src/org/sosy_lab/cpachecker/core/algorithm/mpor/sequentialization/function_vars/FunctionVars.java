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

  // TODO store respective ReturnPc storage here
  public final ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>>
      returnValueAssignments;

  public final ImmutableMap<ThreadEdge, FunctionReturnPcStorage> returnPcStorages;

  public final ImmutableMap<ThreadNode, FunctionReturnPcRetrieval> returnPcRetrievals;

  public FunctionVars(
      ImmutableMap<ThreadEdge, ImmutableList<FunctionParameterAssignment>> pParameterAssignments,
      ImmutableMap<ThreadEdge, ImmutableSet<FunctionReturnValueAssignment>> pReturnValueAssignments,
      ImmutableMap<ThreadEdge, FunctionReturnPcStorage> pReturnPcStorages,
      ImmutableMap<ThreadNode, FunctionReturnPcRetrieval> pReturnPcRetrievals) {

    parameterAssignments = pParameterAssignments;
    returnValueAssignments = pReturnValueAssignments;
    returnPcStorages = pReturnPcStorages;
    returnPcRetrievals = pReturnPcRetrievals;
  }
}
