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
package org.sosy_lab.cpachecker.cpa.featurevariables;

import java.io.File;
import java.util.HashMap;
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

public class FeatureVarsTest {
  @Test
  public void assignmentTest_True() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "specification",     "config/specification/FeatureVarsErrorLocation.spc",
        "cpa.explicit.threshold", "200",
        "cpa.explicit.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
        "cpa.featurevars.variableWhitelist", "__SELECTED_FEATURE_(\\w)*"
      );

      prop = new HashMap<String, String>(prop);
      prop.put("cfa.removeIrrelevantForErrorLocations", "false");
      prop.put("analysis.traversal.order", "bfs");
      prop.put("analysis.traversal.useReversePostorder", "true");
      prop.put("analysis.traversal.useCallstack", "true");
      prop.put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA, cpa.explicit.ExplicitCPA");

      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile,
        "int main() {\n"+
        "  __SELECTED_FEATURE_Verify = 1;" +
        "  int tmp;\n"+
        "    if (__SELECTED_FEATURE_Verify)\n"+
        "              tmp = 1;\n"+
        "    else tmp =  0;\n"+
        "  if (! tmp) error: fail();\n"+
        "  }");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      Assert.assertTrue(results.isSafe());
  }
  @Test
  public void assignmentTest_False() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "specification",     "config/specification/FeatureVarsErrorLocation.spc",
        "cpa.explicit.threshold", "200",
        "cpa.explicit.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
        "cpa.featurevars.variableWhitelist", "__SELECTED_FEATURE_(\\w)*"
      );

      prop = new HashMap<String, String>(prop);
      prop.put("cfa.removeIrrelevantForErrorLocations", "false");
      prop.put("analysis.traversal.order", "bfs");
      prop.put("analysis.traversal.useReversePostorder", "true");
      prop.put("analysis.traversal.useCallstack", "true");
      prop.put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA, cpa.explicit.ExplicitCPA");

      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile,
        "int main() {\n"+
        "  __SELECTED_FEATURE_Verify = 0;" +
        "  int tmp;\n"+
        "    if (!__SELECTED_FEATURE_Verify)\n"+
        "              tmp = 0;\n"+
        "    else tmp =  1;\n"+
        "  if (tmp) error: fail();\n"+
        "  }");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      Assert.assertTrue(results.isSafe());
  }
  // Specification Tests
  @Test
  public void cooperationWithExplicit3VarsWithFunctionCall() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "specification",     "config/specification/FeatureVarsErrorLocation.spc",
        "cpa.explicit.threshold", "200",
        "cpa.explicit.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
        "cpa.featurevars.variableWhitelist", "__SELECTED_FEATURE_(\\w)*"
      );

      prop = new HashMap<String, String>(prop);
      prop.put("cfa.removeIrrelevantForErrorLocations", "false");
      prop.put("analysis.traversal.order", "bfs");
      prop.put("analysis.traversal.useReversePostorder", "true");
      prop.put("analysis.traversal.useCallstack", "true");
      prop.put("cpa","cpa.art.ARTCPA");
      prop.put("ARTCPA.cpa","cpa.composite.CompositeCPA");
      prop.put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA, cpa.explicit.ExplicitCPA");

      TestResults results = run(prop, "test/programs/simple/featureVarsTest.c");
      //results.getCheckerResult().printStatistics(System.out); // to get an error path
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertFalse(results.logLineMatches(".*Product violating in line (\\d)*: TRUE.*"));
      Assert.assertTrue(results.logContains("Product violating in line 16:"));
      Assert.assertTrue(results.logContains("!__SELECTED_FEATURE_Verify"));
      Assert.assertTrue(results.logContains("!__SELECTED_FEATURE_Sign"));
      Assert.assertTrue(results.logContains("__SELECTED_FEATURE_Forward"));
      Assert.assertTrue(results.isUnsafe());
  }
  @Test
  public void testStateReduction() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "specification",     "config/specification/FeatureVarsErrorLocation.spc",
        "cpa.explicit.threshold", "200",
        "cpa.explicit.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
        "cpa.featurevars.variableWhitelist", "__SELECTED_FEATURE_(\\w)*"
      );

      prop = new HashMap<String, String>(prop);
      prop.put("cfa.removeIrrelevantForErrorLocations", "false");
      prop.put("cpa","cpa.art.ARTCPA");
      prop.put("analysis.traversal.order", "bfs");
      prop.put("analysis.traversal.useReversePostorder", "true");
      prop.put("analysis.traversal.useCallstack", "true");
      prop.put("ARTCPA.cpa","cpa.composite.CompositeCPA");
      prop.put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA, cpa.explicit.ExplicitCPA");

      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile,
        "int main() {\n"+
        "  int tmp = 0;\n"+
        "    if (! __SELECTED_FEATURE_Verify)\n"+
        "        if (__SELECTED_FEATURE_Forward)\n"+
        "            if (! __SELECTED_FEATURE_Sign)\n"+
        "              tmp = 0;\n"+
        "            else tmp =  1;\n"+
        "        else tmp =  1;\n"+
        "    else tmp =  1;\n"+
        "  if (__SELECTED_FEATURE_Sign) error: fail();\n"+
        "  }");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      // only the feature Sign causes the error. (analysis should have joined the states before, because all other analysis' states are equal).
      Assert.assertTrue(results.logLineMatches(".*Product violating in line (\\d)*: __SELECTED_FEATURE_Sign \\(Automaton.*"));
      Assert.assertTrue(results.isUnsafe());
  }
  @Test
  public void cooperationWithExplicit3VarsWithoutFunctionCall() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "specification",     "config/specification/FeatureVarsErrorLocation.spc",
        "cpa.explicit.threshold", "200",
        "cpa.explicit.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
        "cpa.featurevars.variableWhitelist", "__SELECTED_FEATURE_(\\w)*"
      );

      prop = new HashMap<String, String>(prop);
      prop.put("cfa.removeIrrelevantForErrorLocations", "false");
      prop.put("cpa","cpa.art.ARTCPA");
      prop.put("ARTCPA.cpa","cpa.composite.CompositeCPA");
      prop.put("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA, cpa.explicit.ExplicitCPA");

      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile, "int main() {\n"+
        "  int tmp = 0;\n"+
        "    if (! __SELECTED_FEATURE_Verify)\n"+
        "        if (__SELECTED_FEATURE_Forward)\n"+
        "            if (! __SELECTED_FEATURE_Sign)\n"+
        "              tmp = 0;\n"+
        "            else tmp =  1;\n"+
        "        else tmp =  1;\n"+
        "    else tmp =  1;\n"+
        "  if (! tmp) error: fail();\n"+
        "  }");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      //results.getCheckerResult().printStatistics(System.out); // to get an error path
      System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertTrue(results.logContains("Valid Product: __SELECTED_FEATURE_Verify"));
      Assert.assertTrue(results.logContains("Product violating in line 10:"));
      Assert.assertTrue(results.logContains("!__SELECTED_FEATURE_Verify"));
      Assert.assertTrue(results.logContains("!__SELECTED_FEATURE_Sign"));
      Assert.assertTrue(results.logContains("__SELECTED_FEATURE_Forward"));
      Assert.assertTrue(results.isUnsafe());
  }
  @Test
  public void cooperationWithExplicit() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA, cpa.explicit.ExplicitCPA",
        "specification",     "test/config/automata/tmpSpecification.spc",
        "cfa.removeIrrelevantForErrorLocations", "false",
        "cpa.explicit.variableBlacklist", "__SELECTED_FEATURE_(\\w)*",
        "cpa.featurevars.variableWhitelist", "__SELECTED_FEATURE_(\\w)*"
      );

      File tmpFile = new File("test/config/automata/tmpSpecification.spc");
      tmpFile.deleteOnExit();
      Files.writeFile(tmpFile , "OBSERVER AUTOMATON tmpAutomaton\n" +
          "INITIAL STATE Init;\n"+
          "STATE Init :\n"+
          "MATCH {$1} ->\n"+
          "PRINT \"Found $1 in Line $line\" GOTO Init;\n"+
          "END AUTOMATON");
      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile, "int __SELECTED_FEATURE_base; int main() { " +
          "if (__SELECTED_FEATURE_base && !__SELECTED_FEATURE_base ) { foo(1); } else {foo(2);} " +
          "}");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertTrue(results.logContains("Found foo(2);"));
      Assert.assertFalse(results.logContains("Found foo(1);"));
      Assert.assertTrue(results.isSafe());
  }
  @Test
  public void trackVariable() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA",
        "specification",     "test/config/automata/tmpSpecification.spc",
        "log.consoleLevel",               "INFO",
        "cfa.removeIrrelevantForErrorLocations", "false",
        "cpa.featurevars.variableWhitelist", "__SELECTED_FEATURE_(\\w)*"
      );

      File tmpFile = new File("test/config/automata/tmpSpecification.spc");
      tmpFile.deleteOnExit();
      Files.writeFile(tmpFile , "OBSERVER AUTOMATON tmpAutomaton\n" +
      		"INITIAL STATE Init;\n"+
          "STATE Init :\n"+
          "MATCH {$1} ->\n"+
          "PRINT \"Found $1 in Line $line\" GOTO Init;\n"+
          "END AUTOMATON");
      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile, "int __SELECTED_FEATURE_base; int main() { " +
      		"if (__SELECTED_FEATURE_base && !__SELECTED_FEATURE_base ) { foo(1); } else {foo(2);} " +
      		"}");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertTrue(results.logContains("Found foo(2);"));
      Assert.assertFalse(results.logContains("Found foo(1);"));
      Assert.assertTrue(results.isSafe());
  }
  @Test
  public void ignoreVariable() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA",
        "specification",     "test/config/automata/tmpSpecification.spc",
        "log.consoleLevel",               "INFO",
        "cfa.removeIrrelevantForErrorLocations", "false",
        "cpa.featurevars.variableWhitelist", "trackNone"
      );

      File tmpFile = new File("test/config/automata/tmpSpecification.spc");
      tmpFile.deleteOnExit();
      Files.writeFile(tmpFile , "OBSERVER AUTOMATON tmpAutomaton\n" +
          "INITIAL STATE Init;\n"+
          "STATE Init :\n"+
          "MATCH {$1} ->\n"+
          "PRINT \"Found $1 in Line $line\" GOTO Init;\n"+
          "END AUTOMATON");
      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile, "int __SELECTED_FEATURE_base; int main() { " +
          "if (__SELECTED_FEATURE_base && !__SELECTED_FEATURE_base ) { foo(1); } else {foo(2);} " +
          "}");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertTrue(results.logContains("Found foo(2);"));
      Assert.assertTrue(results.logContains("Found foo(1);"));
      Assert.assertTrue(results.isSafe());
  }
  @Test
  public void trackVariable2Vars() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA",
        "specification",     "test/config/automata/tmpSpecification.spc",
        "log.consoleLevel",               "INFO",
        "cfa.removeIrrelevantForErrorLocations", "false",
        "cpa.featurevars.variableWhitelist", "__SELECTED_FEATURE_(\\w)*"
      );

      File tmpFile = new File("test/config/automata/tmpSpecification.spc");
      tmpFile.deleteOnExit();
      Files.writeFile(tmpFile , "OBSERVER AUTOMATON tmpAutomaton\n" +
          "INITIAL STATE Init;\n"+
          "STATE Init :\n"+
          "MATCH {$1} ->\n"+
          "PRINT \"Found $1 in Line $line\" GOTO Init;\n"+
          "END AUTOMATON");
      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile, "int __SELECTED_FEATURE_base; int __SELECTED_FEATURE_F2; int main() { " +
          "if (__SELECTED_FEATURE_base && __SELECTED_FEATURE_F2 ){ " +
          "if (! __SELECTED_FEATURE_F2 ) { foo(1); } else {foo(2);} " +
          "}" +
          "}");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertTrue(results.logContains("Found foo(2);"));
      Assert.assertFalse(results.logContains("Found foo(1);"));
      Assert.assertTrue(results.isSafe());
  }
  @Test
  public void ignoreVariable2Vars() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.featurevariables.FeatureVarsCPA",
        "specification",     "test/config/automata/tmpSpecification.spc",
        "log.consoleLevel",               "INFO",
        "cfa.removeIrrelevantForErrorLocations", "false",
        "cpa.featurevars.variableWhitelist", ""
      );

      File tmpFile = new File("test/config/automata/tmpSpecification.spc");
      tmpFile.deleteOnExit();
      Files.writeFile(tmpFile , "OBSERVER AUTOMATON tmpAutomaton\n" +
          "INITIAL STATE Init;\n"+
          "STATE Init :\n"+
          "MATCH {$1} ->\n"+
          "PRINT \"Found $1 in Line $line\" GOTO Init;\n"+
          "END AUTOMATON");
      File sourceFile = new File("test/programs/simple/tmpProgram.c");
      sourceFile.deleteOnExit();
      Files.writeFile(sourceFile, "int __SELECTED_FEATURE_base; int __SELECTED_FEATURE_F2; int main() { " +
          "if (__SELECTED_FEATURE_base && __SELECTED_FEATURE_F2 ){ " +
          "if (! __SELECTED_FEATURE_F2 ) { foo(1); } else {foo(2);} " +
          "}" +
          "}");
      TestResults results = run(prop, "test/programs/simple/tmpProgram.c");
      //System.out.println(results.getLog());
      //System.out.println(results.getCheckerResult().getResult());
      Assert.assertTrue(results.logContains("Found foo(2);"));
      Assert.assertTrue(results.logContains("Found foo(1);"));
      Assert.assertTrue(results.isSafe());
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

  @SuppressWarnings("all")
  private static class TestResults {
    private String log;
    private CPAcheckerResult checkerResult;
    public TestResults(String pLog, CPAcheckerResult pCheckerResult) {
      super();
      log = pLog;
      checkerResult = pCheckerResult;
    }
    public String getLog() {
      return log;
    }
    @SuppressWarnings("unused")
    public CPAcheckerResult getCheckerResult() {
      return checkerResult;
    }
    boolean logContains(String string) {
     return log.contains(string);
    }
    boolean logLineMatches(String pattern) {
      String[] lines = this.log.split("\n");
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].matches(pattern)) {
          return true;
        }
      }
      return false;
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
