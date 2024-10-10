// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.helper_vars;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

public class FunctionVars {

  public final ImmutableMap<ThreadEdge, ImmutableList<CExpressionAssignmentStatement>> paramAssigns;

  public final ImmutableMap<ThreadEdge, ImmutableSet<CExpressionAssignmentStatement>> returnStmts;

  public final ImmutableMap<ThreadEdge, CExpressionAssignmentStatement> returnPcStorages;

  public final ImmutableMap<ThreadNode, CExpressionAssignmentStatement> returnPcRetrievals;

  public FunctionVars(
      ImmutableMap<ThreadEdge, ImmutableList<CExpressionAssignmentStatement>> pParamAssigns,
      ImmutableMap<ThreadEdge, ImmutableSet<CExpressionAssignmentStatement>> pReturnStmts,
      ImmutableMap<ThreadEdge, CExpressionAssignmentStatement> pReturnPcStorages,
      ImmutableMap<ThreadNode, CExpressionAssignmentStatement> pReturnPcRetrievals) {

    paramAssigns = pParamAssigns;
    returnStmts = pReturnStmts;
    returnPcStorages = pReturnPcStorages;
    returnPcRetrievals = pReturnPcRetrievals;
  }
}
