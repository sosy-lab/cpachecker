/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.core;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;

import com.google.common.collect.ImmutableMap;

public class CFATest {
  // Specification Tests
  @Test
  public void ignoreVariablesTest1() throws Exception {

    ImmutableMap<String, String> prop =
        new ImmutableMap.Builder<String, String>()
            .put("cpa", "cpa.arg.ARGCPA")
            .put("ARGCPA.cpa", "cpa.composite.CompositeCPA")
            .put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.explicit.ExplicitCPA")
            .put("specification", "config/specification/default.spc")
            .put("cpa.explicit.variableBlacklist", "__SELECTED_FEATURE_(\\w)*")
            .put("cpa.composite.precAdjust", "COMPONENT")
            .put("log.consoleLevel", "FINER")
            .build();

    TestResults results = run(prop, "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
    System.out.println(results.getLog());
    //System.out.println(results.getCheckerResult().getResult());
    Assert.assertTrue(results.isUnsafe());
  }
  private TestResults run(Map<String, String> pProperties, String pSourceCodeFilePath) throws Exception {
    Configuration config = Configuration.builder()
      .addConverter(FileOption.class, new FileTypeConverter(Configuration.defaultConfiguration()))
      .setOptions(pProperties).build();
    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();
    LogManager logger = new BasicLogManager(config, stringLogHandler);
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();
    CPAchecker cpaChecker = new CPAchecker(config, logger, shutdownNotifier);
    CPAcheckerResult results = cpaChecker.run(pSourceCodeFilePath);
    return new TestResults(stringLogHandler.getLog(), results);
  }

  private static class TestResults {
    private String log;
    private CPAcheckerResult checkerResult;
    public TestResults(String pLog, CPAcheckerResult pCheckerResult) {
      super();
      log = pLog;
      checkerResult = pCheckerResult;
    }
    @SuppressWarnings("unused")
    public String getLog() {
      return log;
    }
    @SuppressWarnings("unused")
    public CPAcheckerResult getCheckerResult() {
      return checkerResult;
    }
    @SuppressWarnings("unused")
    boolean logContains(String pattern) {
     return log.contains(pattern);
    }
    boolean isSafe() {
      return checkerResult.getResult().equals(CPAcheckerResult.Result.SAFE);
    }
    boolean isUnsafe() {
      return checkerResult.getResult().equals(CPAcheckerResult.Result.UNSAFE);
    }
    @Override
    public String toString() {
      return log;
    }
  }
}
