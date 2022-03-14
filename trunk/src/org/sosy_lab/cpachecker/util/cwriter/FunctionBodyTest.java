// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

public class FunctionBodyTest {

  private static CAssumeEdge createAssumeEdge() {
    return new CAssumeEdge(
        "",
        FileLocation.DUMMY,
        CFANode.newDummyCFANode(),
        CFANode.newDummyCFANode(),
        new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.ZERO),
        true);
  }

  @Test
  public void test_enter_leave_iterationOrder() {
    FunctionBody body = new FunctionBody(0, "dummy");
    body.enterBlock(1, createAssumeEdge(), "if (0)");
    body.enterBlock(2, createAssumeEdge(), "if (0)");
    assertThat(body.getCurrentBlock().getStateId()).isEqualTo(2);
    assertThat(from(body).transform(BasicBlock::getStateId)).containsExactly(0, 1, 2).inOrder();
    body.leaveBlock();
    body.enterBlock(3, createAssumeEdge(), "if (0");
    assertThat(body.getCurrentBlock().getStateId()).isEqualTo(3);
    assertThat(from(body).transform(BasicBlock::getStateId)).containsExactly(0, 1, 3).inOrder();
    body.leaveBlock();
    body.leaveBlock();
    body.leaveBlock();
    assertThat(body).isEmpty();
  }
}
