/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.test;

import com.google.common.collect.ImmutableSet;
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

/**
 * Helper class for running CPA tests.
 */
public class CPATestRunner {

  public static TestResults run(
      Map<String, String> pProperties,
      String pSourceCodeFilePath) throws Exception {

    Configuration config = TestDataTools.configurationForTest()
        .setOptions(pProperties)
        .build();
    return run(config, pSourceCodeFilePath);
  }

  public static TestResults run(Configuration config, String pSourceCodeFilePath) throws Exception {
    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();
    stringLogHandler.setLevel(Level.INFO);
    stringLogHandler.setFormatter(ConsoleLogFormatter.withoutColors());
    LogManager logger = BasicLogManager.createWithHandler(stringLogHandler);

    ShutdownManager shutdownManager = ShutdownManager.create();
    CPAchecker cpaChecker = new CPAchecker(config, logger, shutdownManager, ImmutableSet.of());
    CPAcheckerResult results = cpaChecker.run(pSourceCodeFilePath);
    logger.flush();
    return new TestResults(stringLogHandler.getLog(), results);
  }
}
