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

  @Ignore("Functionality not yet implemented")
  @Test
  public void testSimpleMemtrackWitnessExport() throws Exception {
    performExportTest(
        "simple-memtrack-unsafe.c",
        "simple-memtrack-unsafe-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Ignore("Functionality not yet implemented")
  @Test
  public void testSimpleValidWitnessExport() throws Exception {

    performExportTest(
        "simple-valid.c",
        "simple-valid-expected.witness.yml",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Ignore("Functionality not yet implemented")
  @Test
  public void testSimpleMemtrackWitnessValidation() throws Exception {

    performValidationTest(
        "simple-memtrack-unsafe.c",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        "simple-memtrack-unsafe-expected.witness.yml");
  }

  @Ignore("Functionality not yet implemented")
  @Test
  public void testSimpleValidWitnessValidation() throws Exception {
    performValidationTest(
        "simple-valid.c",
        ExpectedVerdict.FALSE,
        getMemorySafetySpec(),
        "simple-valid-expected.witness.yml");
  }
}
