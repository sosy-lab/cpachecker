/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.automaton.tests;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.ARGToAutomatonConverter;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.util.test.AbstractTranslationTest;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

@RunWith(Parameterized.class)
public class ARGToAutomatonConverterTest extends AbstractTranslationTest {

  public static final String AUTOMATA_FILE_TEMPLATE = "ARG.%06d.spc";

  private final String program;
  private final Configuration config;
  private final boolean verdict;
  private final Path automatonPath;
  private final ARGToAutomatonConverter converter;

  public ARGToAutomatonConverterTest(
      @SuppressWarnings("unused") String pTestLabel, String pProgram, boolean pVerdict)
      throws IOException, InvalidConfigurationException {
    filePrefix = "automaton";
    program = pProgram;
    verdict = pVerdict;
    automatonPath = newTempFile();
    String propfile = "split-callstack.properties";
    ConfigurationBuilder configBuilder =
        TestDataTools.configurationForTest()
            .loadFromResource(ARGToAutomatonConverterTest.class, propfile)
            .setOption("cpa.arg.export.code.handleTargetStates", "VERIFIERERROR")
            .setOption("cpa.arg.export.code.header", "false");
    config = configBuilder.build();


    converter = new ARGToAutomatonConverter(config, MachineModel.LINUX32, logger);
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();

    b.add(simpleTask("main.c", true));
    b.add(simpleTask("main2.c", false));
    b.add(simpleTask("functionreturn.c", false));

    return b.build();
  }

  private static Object[] simpleTask(String program, boolean verdict) {
    String label = String.format("SimpleTest(%s is %s)", program, Boolean.toString(verdict));
    return new Object[] {label, program, verdict};
  }

  @Test
  public void test() throws Exception {
    Path fullPath = Paths.get(TEST_DIR_PATH, program);

    // generate ARG:
    resetCFANodeCounter();
    ARGState root = run(config,fullPath);

    // generate joint automaton
    Automaton aut = converter.getAutomaton(root, true);
    Files.write(automatonPath, aut.toString().getBytes("utf-8"));

    for (String analysis :
        ImmutableList.of("predicateAnalysis.properties", "valueAnalysis.properties")) {
      // test whether C program still gives correct verdict with joint automaton:
      Configuration reConfig =
          TestDataTools.configurationForTest()
              .loadFromResource(ARGToAutomatonConverterTest.class, analysis)
              .setOption("specification", automatonPath.toString())
              .build();
      TestResults results = null;
      try {
      resetCFANodeCounter();
        results = CPATestRunner.run(reConfig, fullPath.toString());
      } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
        throw new AssertionError(e);
      }
      assertThat(results).isNotNull();
      if (verdict) {
        results.assertIsSafe();
      } else {
        results.assertIsUnsafe();
      }

      // test whether we get to the same conclusion using the split automata
      Iterable<Automaton> res = converter.getAutomata(root);
      boolean fullVerdict = true;
      for (Automaton a : res) {
        resetCFANodeCounter();
        Path newAutomatonPath = newTempFile();
        Files.write(newAutomatonPath, a.toString().getBytes("utf-8"));
        reConfig =
            TestDataTools.configurationForTest()
                .loadFromResource(ARGToAutomatonConverterTest.class, analysis)
                .setOption("specification", newAutomatonPath.toString())
                .build();
        CPAcheckerResult.Result partialVerdict =
            CPATestRunner.run(reConfig, fullPath.toString()).getCheckerResult().getResult();
        assertThat(partialVerdict).isAnyOf(Result.TRUE, Result.FALSE);
        fullVerdict = fullVerdict && partialVerdict.equals(Result.TRUE);
      }
      assertThat(fullVerdict).isEqualTo(verdict);
    }
  }

  /**
   * For some tests we need to reset the counter for the number of new CFA nodes. This is only OK if
   * it is needed because of files that are generated during the test execution and that refer to
   * fixed CFA node numbers (e.g. a specification automaton that matches transitions to a certain
   * node number).
   */
  @Deprecated
  private void resetCFANodeCounter() throws NoSuchFieldException, IllegalAccessException {
    Field idGenerator = CFANode.class.getDeclaredField("idGenerator");
    idGenerator.setAccessible(true);
    UniqueIdGenerator uid = (UniqueIdGenerator) idGenerator.get(null);
    Field nextId = uid.getClass().getDeclaredField("nextId");
    nextId.setAccessible(true);
    AtomicInteger i = (AtomicInteger) nextId.get(uid);
    i.set(0);
    nextId.setAccessible(false);
    idGenerator.setAccessible(false);
  }

}
