// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class ModificationsPropTest {

  private static final String CONFIG_FILE = "config/differencePropPredicateAnalysis.properties";
  private static final String OLD_SEC =
      "test/programs/modified/intraprocedural_base_true-unreach-call.c";
  private static final String OLD_UNSEC =
      "test/programs/modified/intraprocedural_inc_false-unreach-call.c";
  private static final String PROGRAM_ORIGINAL = "test/programs/modified/propbased_diff_original.c";
  private static final String PROGRAM_MODIFIED = "test/programs/modified/propbased_diff_mod.c";
  private static final String FCTCALL_ORIGINAL = "test/programs/modified/function_call_original.c";
  private static final String FCTCALL_MODIFIED = "test/programs/modified/function_call_mod.c";
  private static final String REACH_PROPERTY = "config/properties/unreach-call.prp";
  private static final String DEFAULT_PROPERTY = "config/specification/default.spc";
  private static final String PROP_ORIGINAL_PROGRAM = "differential.program";
  private static final String PROP_REACH_PROPERTY = "differential.badstateProperties";
  private static final String PROP_PREPROCESSING = "differential.performPreprocessing";
  private static final String PROP_DECLARATION_IGNORE = "differential.ignoreDeclarations";
  private static final String PROP_MERGE = "differential.variableSetMerge";
  private static final String PROP_SPEC = "specification";
  private static final String PROP_ENTRY = "analysis.entryFunction";
  private static final String VAL_ENTRY = "main";
  private static final String TRUE = "true";
  private static final String FALSE = "false";

  // If this leads to a timeout something most likely goes wrong in the CPA such that the loop to
  // 1000000 is being re-explored.
  @Test
  public void worksOnDiffcondExample() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            PROP_SPEC, REACH_PROPERTY,
            PROP_ORIGINAL_PROGRAM, OLD_UNSEC,
            PROP_REACH_PROPERTY, REACH_PROPERTY,
            PROP_PREPROCESSING, TRUE,
            PROP_MERGE, FALSE,
            PROP_ENTRY, VAL_ENTRY);

    TestResults results = CPATestRunner.run(getProperties(CONFIG_FILE, prop), OLD_SEC);
    results.assertIsSafe();

    prop =
        ImmutableMap.of(
            PROP_ORIGINAL_PROGRAM, OLD_SEC,
            PROP_REACH_PROPERTY, REACH_PROPERTY,
            PROP_PREPROCESSING, TRUE,
            PROP_MERGE, FALSE,
            PROP_SPEC, REACH_PROPERTY,
            PROP_DECLARATION_IGNORE, FALSE,
            PROP_ENTRY, VAL_ENTRY);

    results = CPATestRunner.run(getProperties(CONFIG_FILE, prop), OLD_UNSEC);
    results.assertIsUnsafe();

    // Now, both once more looking into default.spc

    prop =
        ImmutableMap.of(
            PROP_ORIGINAL_PROGRAM, OLD_UNSEC,
            PROP_REACH_PROPERTY, DEFAULT_PROPERTY,
            PROP_PREPROCESSING, TRUE,
            PROP_MERGE, FALSE,
            PROP_SPEC, DEFAULT_PROPERTY,
            PROP_DECLARATION_IGNORE, FALSE);

    results = CPATestRunner.run(getProperties(CONFIG_FILE, prop), OLD_SEC);
    results.assertIsSafe();

    prop =
        ImmutableMap.of(
            PROP_ORIGINAL_PROGRAM, OLD_SEC,
            PROP_REACH_PROPERTY, DEFAULT_PROPERTY,
            PROP_PREPROCESSING, TRUE,
            PROP_SPEC, DEFAULT_PROPERTY,
            PROP_MERGE, FALSE,
            PROP_DECLARATION_IGNORE, FALSE);

    results = CPATestRunner.run(getProperties(CONFIG_FILE, prop), OLD_UNSEC);
    results.assertIsUnsafe();
  }

  @Test
  public void successfulCoverage() throws Exception {
    // leave out preprocessing here to check it is working as well
    final Map<String, String> prop =
        ImmutableMap.of(
            PROP_ORIGINAL_PROGRAM, PROGRAM_ORIGINAL,
            PROP_REACH_PROPERTY, REACH_PROPERTY,
            PROP_PREPROCESSING, FALSE,
            PROP_MERGE, FALSE,
            PROP_SPEC, REACH_PROPERTY,
            PROP_DECLARATION_IGNORE, FALSE,
            PROP_ENTRY, VAL_ENTRY);

    final TestResults results =
        CPATestRunner.run(getProperties(CONFIG_FILE, prop), PROGRAM_MODIFIED);
    results.assertIsSafe();
  }

  @Test
  public void functionCalls() throws Exception {
    // tests programs that use function calls
    Map<String, String> prop =
        ImmutableMap.of(
            PROP_ORIGINAL_PROGRAM, FCTCALL_ORIGINAL,
            PROP_REACH_PROPERTY, REACH_PROPERTY,
            PROP_PREPROCESSING, TRUE,
            PROP_MERGE, FALSE,
            PROP_SPEC, REACH_PROPERTY,
            PROP_DECLARATION_IGNORE, TRUE,
            PROP_ENTRY, VAL_ENTRY);

    TestResults results = CPATestRunner.run(getProperties(CONFIG_FILE, prop), FCTCALL_MODIFIED);
    results.assertIsSafe();

    prop =
        ImmutableMap.of(
            PROP_ORIGINAL_PROGRAM, FCTCALL_ORIGINAL,
            PROP_REACH_PROPERTY, REACH_PROPERTY,
            PROP_PREPROCESSING, TRUE,
            PROP_MERGE, FALSE,
            PROP_SPEC, REACH_PROPERTY,
            PROP_DECLARATION_IGNORE, FALSE,
            PROP_ENTRY, VAL_ENTRY);

    results = CPATestRunner.run(getProperties(CONFIG_FILE, prop), FCTCALL_MODIFIED);
    results.assertIsUnsafe();
  }

  @Test
  public void correctFail() throws Exception {
    // swaps modified and original program to result in no coverage
    final Map<String, String> prop =
        ImmutableMap.of(
            PROP_ORIGINAL_PROGRAM, PROGRAM_MODIFIED,
            PROP_REACH_PROPERTY, REACH_PROPERTY,
            PROP_PREPROCESSING, TRUE,
            PROP_MERGE, FALSE,
            PROP_SPEC, REACH_PROPERTY,
            PROP_DECLARATION_IGNORE, FALSE,
            PROP_ENTRY, VAL_ENTRY);

    final TestResults results =
        CPATestRunner.run(getProperties(CONFIG_FILE, prop), PROGRAM_ORIGINAL);
    results.assertIsUnsafe();
  }

  private static Configuration getProperties(
      final String pConfigFile, final Map<String, String> pOverrideOptions)
      throws InvalidConfigurationException, IOException {
    final ConfigurationBuilder configBuilder =
        TestDataTools.configurationForTest().loadFromFile(pConfigFile);
    return configBuilder.setOptions(pOverrideOptions).build();
  }
}
