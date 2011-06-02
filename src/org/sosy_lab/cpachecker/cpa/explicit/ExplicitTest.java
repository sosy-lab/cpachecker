/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.LogManager.StringHandler;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;

import com.google.common.collect.ImmutableMap;

public class ExplicitTest {
  // Specification Tests
  @Test
  public void ignoreVariablesTest1() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.explicit.ExplicitCPA",
        "specification",     "test/config/automata/tmpSpecification.spc",
        "cfa.removeIrrelevantForErrorLocations", "false",
        "cpa.explicit.variableBlacklist", "main::__SELECTED_FEATURE_(\\w)*",
        "cpa.explicit.threshold", "200000"
      );

      File tmpFile = new File("test/config/automata/tmpSpecification.spc");
      Files.writeFile(tmpFile , "ASSERT ! CHECK(ExplicitAnalysis, \"contains(__SELECTED_FEATURE_base)\") ;");
      TestResults results = run(prop, "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertTrue(results.isSafe());
      tmpFile.deleteOnExit();
  }
  @Test
  public void ignoreVariablesTest2() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.explicit.ExplicitCPA",
        "specification",     "test/config/automata/tmpSpecification.spc",
        "cfa.removeIrrelevantForErrorLocations", "false",
        "cpa.explicit.variableBlacklist", "somethingElse",
        "cpa.explicit.threshold", "200000"
      );

      File tmpFile = new File("test/config/automata/tmpSpecification.spc");
      Files.writeFile(tmpFile , "ASSERT ! CHECK(ExplicitAnalysis, \"contains(__SELECTED_FEATURE_base)\") ;");
      TestResults results = run(prop, "test/programs/simple/explicit/explicitIgnoreFeatureVars.c");
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertTrue(results.isUnsafe());
      Assert.assertTrue(results.logContains("Automaton going to ErrorState on edge \"int __SELECTED_FEATURE_base;\""));
      tmpFile.deleteOnExit();
  }
  private TestResults run(Map<String, String> pProperties, String pSourceCodeFilePath) throws Exception {
    Configuration config = Configuration.builder().setOptions(pProperties).build();
    StringHandler stringLogHandler = new LogManager.StringHandler();
    LogManager logger = new LogManager(config, stringLogHandler);
    CPAchecker cpaChecker = new CPAchecker(config, logger);
    CPAcheckerResult results = cpaChecker.run(pSourceCodeFilePath);
    return new TestResults(stringLogHandler.getLog(), results);
  }
  @SuppressWarnings("unused")
  private TestResults run(File configFile, Map<String, String> pProperties, String pSourceCodeFilePath) throws Exception {
    Configuration config = Configuration.builder()
      .loadFromFile(configFile.getAbsolutePath())
      .setOptions(pProperties).build();

    StringHandler stringLogHandler = new LogManager.StringHandler();
    LogManager logger = new LogManager(config, stringLogHandler);
    CPAchecker cpaChecker = new CPAchecker(config, logger);
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
