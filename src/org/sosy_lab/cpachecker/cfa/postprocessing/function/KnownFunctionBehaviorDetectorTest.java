// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.KnownFunctionBehavior.MEMSET;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.KnownFunctionBehavior;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

/**
 * Tests which functions of the programs in {@value #PROGRAMS} are recognized as having a known
 * behavior. Each program describes at its top which functions behave like which well-known
 * function.
 *
 * <p>The tests only check facts that do not depend on how much the detection supports: functions
 * that are implementations of a well-known function in a common form have to be recognized, and
 * functions that behave differently must never be recognized.
 */
public class KnownFunctionBehaviorDetectorTest {

  private static final String PROGRAMS = "test/programs/simple/known-function-behavior/";

  private static ImmutableMap<String, KnownFunctionBehavior> detect(String pProgram)
      throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromFile(
            PROGRAMS + pProgram, Map.entry("cfa.detectKnownFunctionBehavior", "true"));
    return cfa.getMetadata().getKnownFunctionBehaviors().orElseThrow();
  }

  @Test
  public void awsImplementationAndWrapper() throws Exception {
    assertThat(detect("memset-aws.c")).containsExactly("memset_impl", MEMSET, "my_memset", MEMSET);
  }

  @Test
  public void implementationWithoutGuard() throws Exception {
    assertThat(detect("memset-without-guard.c")).containsExactly("fill_bytes", MEMSET);
  }

  @Test
  public void implementationWithUnsignedCharAndWithoutResult() throws Exception {
    assertThat(detect("memset-unsigned-char.c")).containsExactly("clear_memory", MEMSET);
  }

  @Test
  public void wrappersInTwoLevels() throws Exception {
    assertThat(detect("memset-wrapper-chain.c"))
        .containsExactly("set_bytes", MEMSET, "set_bytes_checked", MEMSET, "set_memory", MEMSET);
  }

  @Test
  public void functionsThatAreNoMemset() throws Exception {
    assertThat(detect("not-memset.c")).isEmpty();
  }

  @Test
  public void noDetectionWithoutOption() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(PROGRAMS + "memset-aws.c");
    assertThat(cfa.getMetadata().getKnownFunctionBehaviors()).isEmpty();
  }
}
