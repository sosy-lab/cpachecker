// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class YAMLWitnessContentTest {

  private static final String TEST_DIR_PATH = "test/programs/witnessValidation/";
  private static final String CONFIG_DIR_PATH = "config";
  private static final String SPEC_DIR_PATH = "config/specification";


  /**
   * Builds a {@link Configuration} object from the given configuration file, the given override options, and adds the specification to the configuration.
   * @param pConfigFile The path to the configuration file. (Relative to root)
   * @param pOverrideOptions A map of options to override in the configuration.
   * @param pSpecification The path to the specification. (Relative to root)
   * @return A {@link Configuration} object to pass into the {@link CPATestRunner}.
   * @throws InvalidConfigurationException If the configuration is invalid.
   * @throws IOException If the configuration file could not be read.
   */
  private Configuration generateConfiguration(
      String pConfigFile,
      Map<String, String> pOverrideOptions,
      String pSpecification
  ) throws InvalidConfigurationException, IOException {
    ConfigurationBuilder configBuilder =
        TestDataTools.configurationForTest().loadFromFile(pConfigFile);
    if (!Strings.isNullOrEmpty(pSpecification)) {
      pOverrideOptions.put("specification", pSpecification);
    }
    return configBuilder.setOptions(pOverrideOptions).build();
  }

}
