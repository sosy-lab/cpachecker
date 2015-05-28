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

import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;

/**
 * Helper class for running CPA tests.
 */
public class CPATestRunner {

  public static TestResults runAndLogToSTDOUT(
      Map<String, String> pProperties,
      String pSourceCodeFilePath) throws Exception {
    return run(pProperties, pSourceCodeFilePath, true);
  }

  public static TestResults run(
      Map<String, String> pProperties,
      String pSourceCodeFilePath) throws Exception {
    return run(pProperties, pSourceCodeFilePath, false);
  }

  public static TestResults run(
      Map<String, String> pProperties,
      String pSourceCodeFilePath,
      boolean writeLogToSTDOUT) throws Exception {

    Configuration config = TestDataTools.configurationForTest()
        .setOptions(pProperties)
        .build();

    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();

    Handler h;
    if (writeLogToSTDOUT) {
      h = new StreamHandler(System.out, new SimpleFormatter());
    } else {
      h = stringLogHandler;
    }

    LogManager logger = new BasicLogManager(config, h);
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();
    CPAchecker cpaChecker = new CPAchecker(config, logger, shutdownNotifier);
    try {
      CPAcheckerResult results = cpaChecker.run(pSourceCodeFilePath);
      return new TestResults(stringLogHandler.getLog(), results);
    } finally {
      logger.flush();

    }

  }
}
