// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner.ExpectedVerdict;
import org.sosy_lab.cpachecker.util.witnesses.CommonTestingWitnessV2;

public class MemsafetyWitnessV2Test extends CommonTestingWitnessV2 {

  @Test
  public void testSimpleDoubleFree() throws Exception {
    performExportTest(
        "simple-double-free.c",
        "simple-double-free-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testDifferentVariablesDoubleFree() throws Exception {

    performExportTest(
        "diff-var-double-free.c",
        "diff-var-double-free-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testConditionalDoubleFree() throws Exception {

    performExportTest(
        "conditional-double-free.c",
        "conditional-double-free-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testBlockedDoubleFree() throws Exception {

    performExportTest(
        "blocked-double-free.c",
        "blocked-double-free-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testSimpleInvalidDereference() throws Exception {

    performExportTest(
        "simple-invalid-deref.c",
        "simple-invalid-deref-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testSimpleNilDereference() throws Exception {

    performExportTest(
        "simple-nil-deref.c",
        "simple-nil-deref-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testBufOverrunDereference() throws Exception {

    performExportTest(
        "buf-overrun-deref.c",
        "buf-overrun-deref-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testSimpleInvalidMemtrack() throws Exception {

    performExportTest(
        "simple-invalid-memtrack.c",
        "simple-invalid-memtrack-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testVarOverrideInvalidMemtrack() throws Exception {

    performExportTest(
        "var-override-invalid-memtrack.c",
        "var-override-invalid-memtrack-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

}
