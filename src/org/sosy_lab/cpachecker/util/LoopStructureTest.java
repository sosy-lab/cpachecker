// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class LoopStructureTest {

  @Test
  public void testModifiedVariablesAfterNestedCall() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromString(
            """
            int x;
            void g() {}
            void f() {
              g();
              x++;
            }
            int main() {
              while (1) {
                f();
              }
            }
            """);
    Loop loop = Iterables.getOnlyElement(cfa.getLoopStructure().orElseThrow().getAllLoops());

    assertThat(from(loop.getModifiedVariables()).transform(CVariableDeclaration::getQualifiedName))
        .contains("x");
  }
}
