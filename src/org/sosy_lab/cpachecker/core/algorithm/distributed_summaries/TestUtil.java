// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.io.IOException;
import java.nio.file.Path;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

/** Helper class for Distributed Summary Synthesis tests. */
public class TestUtil {
  private static final Language language = Language.C;

  private static final String CFA_CONFIGURATION_FILE = "config/generateCFA.properties";
  // public so that this can be accessed for the LinearDecomposition test
  public static final String DSS_CONFIGURATION_FILE = "config/dss.properties";

  // Do not use TestDataTools.configurationForTest() because we want output files
  public static Configuration generateConfig(String configFile, Path testFolder)
      throws InvalidConfigurationException, IOException {
    Configuration configForFiles =
        Configuration.builder().setOption("output.path", testFolder.toString()).build();
    FileTypeConverter fileTypeConverter = FileTypeConverter.create(configForFiles);
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder
        .loadFromFile(configFile)
        .setOption("language", language.name())
        .addConverter(FileOption.class, fileTypeConverter);
    return configBuilder.build();
  }

  public static CFA buildTestCFA(String path) throws Exception {

    Configuration config =
        TestDataTools.configurationForTest().loadFromFile(CFA_CONFIGURATION_FILE).build();

    TestResults result = CPATestRunner.run(config, path);

    CFA cfa = result.getCheckerResult().getCfa();

    if (cfa == null) {
      throw new IllegalArgumentException("Could not create a CFA out of the file '" + path + "'");
    }

    return result.getCheckerResult().getCfa();
  }

}
