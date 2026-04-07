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
import org.sosy_lab.cpachecker.cfa.Language;

/** Helper class for Distributed Summary Synthesis tests. */
public class TestUtil {
  private static final Language language = Language.C;

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
}
