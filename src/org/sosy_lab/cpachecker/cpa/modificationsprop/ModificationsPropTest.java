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

  private final String configFile = "config/differencePropPredicateAnalysis.properties";

  @Test
  public void successfulCoverage() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "differential.program", "test/programs/modified/propbased_diff_original.c",
            "differential.badstateProperties", "config/specification/sv-comp-reachability.spc",
            "differential.performPreprocessing", "true");

    TestResults results =
        CPATestRunner.run(
            getProperties(configFile, prop), "test/programs/modified/propbased_diff_mod.c");
    results.assertIsSafe();
  }

  private static Configuration getProperties(
      String pConfigFile, Map<String, String> pOverrideOptions)
      throws InvalidConfigurationException, IOException {
    ConfigurationBuilder configBuilder =
        TestDataTools.configurationForTest().loadFromFile(pConfigFile);
    return configBuilder.setOptions(pOverrideOptions).build();
  }
}
