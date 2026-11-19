// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.KnownFunctionBehavior.MEMSET_WITH_GUARDS;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.KnownFunctionBehavior;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
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

  private static final String PROGRAMS = "test/programs/simple/memset/";

  private static ImmutableMap<String, KnownFunctionBehavior> detect(String pProgram)
      throws Exception {
    return knownBehaviors(
        TestCfaUtils.makeCfaFromFile(
            PROGRAMS + pProgram, Map.entry("cfa.detectKnownFunctionBehavior", "true")));
  }

  /** The functions of the given CFA with a known behavior, by their name. */
  private static ImmutableMap<String, KnownFunctionBehavior> knownBehaviors(CFA pCfa) {
    ImmutableMap.Builder<String, KnownFunctionBehavior> result = ImmutableMap.builder();
    for (FunctionEntryNode entry : pCfa.entryNodes()) {
      if (entry instanceof CFunctionEntryNode cEntry && cEntry.getKnownBehavior().isPresent()) {
        result.put(cEntry.getFunctionName(), cEntry.getKnownBehavior().orElseThrow());
      }
    }
    return result.buildOrThrow();
  }

  @Test
  public void awsImplementationAndWrapper() throws Exception {
    assertThat(detect("byte-loop-memset-aws.c"))
        .containsExactly("memset_impl", MEMSET_WITH_GUARDS, "my_memset", MEMSET_WITH_GUARDS);
  }

  @Test
  public void implementationWithoutGuard() throws Exception {
    assertThat(detect("byte-loop-memset-without-guard.c"))
        .containsExactly("fill_bytes", MEMSET_WITH_GUARDS);
  }

  @Test
  public void implementationWithUnsignedCharAndWithoutResult() throws Exception {
    assertThat(detect("byte-loop-memset-unsigned-char.c"))
        .containsExactly("clear_memory", MEMSET_WITH_GUARDS);
  }

  @Test
  public void wrappersInTwoLevels() throws Exception {
    assertThat(detect("byte-loop-memset-wrapper-chain.c"))
        .containsExactly(
            "set_bytes",
            MEMSET_WITH_GUARDS,
            "set_bytes_checked",
            MEMSET_WITH_GUARDS,
            "set_memory",
            MEMSET_WITH_GUARDS);
  }

  @Test
  public void functionsThatAreNoMemset() throws Exception {
    assertThat(detect("byte-loop-not-memset.c")).isEmpty();
  }

  @Test
  public void noDetectionIfDisabled() throws Exception {
    assertThat(
            knownBehaviors(
                TestCfaUtils.makeCfaFromFile(
                    PROGRAMS + "byte-loop-memset-aws.c",
                    Map.entry("cfa.detectKnownFunctionBehavior", "false"))))
        .isEmpty();
  }
}
