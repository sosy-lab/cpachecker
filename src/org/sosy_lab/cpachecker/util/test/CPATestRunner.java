// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.ConsoleLogFormatter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;

/** Helper class for running CPA tests. */
public class CPATestRunner {

  public enum ExpectedVerdict {
    TRUE,
    FALSE,
    NONE
  }

  public static TestResults run(Map<String, String> pProperties, String pSourceCodeFilePath)
      throws Exception {

    Configuration config = TestDataTools.configurationForTest().setOptions(pProperties).build();
    return run(config, pSourceCodeFilePath);
  }

  public static TestResults run(Configuration config, String pSourceCodeFilePath) throws Exception {
    return run(config, pSourceCodeFilePath, Level.INFO);
  }

  public static TestResults run(Configuration config, String pSourceCodeFilePath, Level logLevel)
      throws Exception {
    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();
    stringLogHandler.setLevel(logLevel);
    stringLogHandler.setFormatter(ConsoleLogFormatter.withoutColors());
    LogManager logger = BasicLogManager.createWithHandler(stringLogHandler);

    ShutdownManager shutdownManager = ShutdownManager.create();
    CPAchecker cpaChecker = new CPAchecker(config, logger, shutdownManager);
    CPAcheckerResult results = cpaChecker.run(ImmutableList.of(pSourceCodeFilePath));
    logger.flush();
    return new TestResults(stringLogHandler.getLog(), results);
  }
}
