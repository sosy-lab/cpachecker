/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

public class FunctionBodyTest {

  private static CAssumeEdge createAssumeEdge() {
    return new CAssumeEdge(
        "",
        FileLocation.DUMMY,
        new CFANode(CFunctionDeclaration.DUMMY),
        new CFANode(CFunctionDeclaration.DUMMY),
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
