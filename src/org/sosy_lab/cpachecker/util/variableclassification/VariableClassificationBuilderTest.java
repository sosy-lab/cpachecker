// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class VariableClassificationBuilderTest {

  private static final String PROGRAMS = "test/programs/simple/variable_classification/";

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static Set<String> relevantVariables(
      String pProgram, Map.Entry<String, String>... pOptions) throws Exception {
    return TestCfaUtils.makeCfaFromFile(PROGRAMS + pProgram, pOptions)
        .getVarClassification()
        .orElseThrow()
        .getRelevantVariables();
  }

  @Test
  public void resultOfDirectCallStaysIrrelevant() throws Exception {
    assertThat(relevantVariables("result-of-direct-call.c")).doesNotContain("h::__retval__");
  }

  @Test
  public void resultOfStartFunctionIsRelevant() throws Exception {
    Set<String> relevant = relevantVariables("start-function.c");
    assertThat(relevant).contains("f::__retval__");
    assertThat(relevant).doesNotContain("h::__retval__");
  }

  @Test
  public void resultOfFunctionPointerVariableIsRelevant() throws Exception {
    assertThat(relevantVariables("start-function-pointer-variable.c")).contains("f::__retval__");
  }

  @Test
  public void resultOfCastFunctionIsRelevant() throws Exception {
    assertThat(relevantVariables("start-function-cast.c")).contains("run::__retval__");
  }

  @Test
  public void resultOfFunctionInStructIsRelevant() throws Exception {
    assertThat(relevantVariables("start-function-in-struct.c")).contains("f::__retval__");
  }

  @Test
  public void resultOfClonedFunctionIsRelevant() throws Exception {
    assertThat(
            relevantVariables(
                "start-function.c", Map.entry("cfa.useCFACloningForMultiThreadedPrograms", "true")))
        .contains("f__cloned_function__1::__retval__");
  }
}
