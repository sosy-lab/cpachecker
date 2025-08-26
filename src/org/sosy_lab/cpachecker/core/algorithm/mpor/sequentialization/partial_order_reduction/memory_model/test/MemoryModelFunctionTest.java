// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public class MemoryModelFunctionTest {

  // Simple Types

  private final CSimpleType INT_TYPE =
      new CSimpleType(false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private final CPointerType INT_POINTER_TYPE = new CPointerType(false, false, INT_TYPE);

  // CFunctionType

  private final CFunctionType DUMMY_FUNCTION_TYPE =
      new CFunctionType(INT_TYPE, ImmutableList.of(), false);

  // CFunctionDeclarations

  private final CFunctionDeclaration DUMMY_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          DUMMY_FUNCTION_TYPE,
          "dummy_function",
          ImmutableList.of(),
          ImmutableSet.of());

  private final CIdExpression DUMMY_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, DUMMY_FUNCTION_DECLARATION);

  // CFunctionCallExpression

  private final CFunctionCallExpression DUMMY_FUNCTION_CALL_EXPRESSION =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          DUMMY_FUNCTION_TYPE,
          DUMMY_ID_EXPRESSION,
          ImmutableList.of(),
          DUMMY_FUNCTION_DECLARATION);

  // CFunctionCallStatement

  private final CFunctionCallStatement DUMMY_FUNCTION_CALL_STATEMENT =
      new CFunctionCallStatement(FileLocation.DUMMY, DUMMY_FUNCTION_CALL_EXPRESSION);

  // CFA Nodes

  private final CFANode DUMMY_PREDECESSOR = CFANode.newDummyCFANode();

  private final CFANode DUMMY_SUCCESSOR = CFANode.newDummyCFANode();

  private final CFunctionEntryNode DUMMY_FUNCTION_ENTRY_NODE =
      (CFunctionEntryNode) CFunctionEntryNode.newDummyCFANode();

  // CFA Edges

  private final CFunctionSummaryEdge DUMMY_FUNCTION_SUMMARY_EDGE =
      new CFunctionSummaryEdge(
          "",
          FileLocation.DUMMY,
          DUMMY_PREDECESSOR,
          DUMMY_SUCCESSOR,
          DUMMY_FUNCTION_CALL_STATEMENT,
          DUMMY_FUNCTION_ENTRY_NODE);

  private final CFunctionCallEdge DUMMY_FUNCTION_CALL_EDGE =
      new CFunctionCallEdge(
          "",
          FileLocation.DUMMY,
          DUMMY_PREDECESSOR,
          DUMMY_FUNCTION_ENTRY_NODE,
          DUMMY_FUNCTION_CALL_STATEMENT,
          DUMMY_FUNCTION_SUMMARY_EDGE);
}
