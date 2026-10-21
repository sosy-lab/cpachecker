// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Assume;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class ClonedCfaCreationHookTest {

  @Test
  public void hookReplacesCreatedCfaWithStructuralClone() throws Exception {
    Assume.assumeTrue(ClonedCfaCreationHook.isAgentInstalled());
    ClonedCfaCreationHook.install();
    CFA cfa = TestCfaUtils.makeCfaFromString("int main() { return 0; }");

    assertThat(cfa.getMainFunction().getOriginal()).isNotSameInstanceAs(cfa.getMainFunction());
    assertThat(cfa.nodes()).isNotEmpty();
    assertThat(cfa.edges()).isNotEmpty();
    assertThat(cfa.edges().stream().allMatch(edge -> edge.getOriginal() != edge)).isTrue();
  }
}
