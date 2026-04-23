// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton.tests;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.annotations.SuppressForbidden;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.ARGToAutomatonConverter;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

@RunWith(Parameterized.class)
public class ARGToAutomatonConverterTest {

  private static final String TEST_DIR_PATH = "test/programs/programtranslation/";

  private final String program;
  private final Configuration config;
  private final boolean verdict;
  private final boolean forOverflow;
  private final Path automatonPath;
  private final ARGToAutomatonConverter converter;

  public ARGToAutomatonConverterTest(
      @SuppressWarnings("unused") String pTestLabel,
      String pProgram,
      boolean pVerdict,
      boolean pForOverflow)
      throws IOException, InvalidConfigurationException {
    program = pProgram;
    verdict = pVerdict;
    forOverflow = pForOverflow;
    automatonPath = newTempFile();
    String propfile = forOverflow ? "split--overflow.properties" : "split-callstack.properties";
    config =
        TestDataTools.configurationForTest()
            .loadFromResource(ARGToAutomatonConverterTest.class, propfile)
            .setOption("cpa.arg.export.code.handleTargetStates", "VERIFIERERROR")
            .setOption("cpa.arg.export.code.header", "false")
            .build();

    converter =
        new ARGToAutomatonConverter(
            config, MachineModel.LINUX32, LogManager.createTestLogManager());
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();

    b.add(simpleTask("main.c", true));
    b.add(simpleTask("main2.c", false));
    b.add(simpleTask("functionreturn.c", false));
    b.add(simpleTaskForOverflow("overflow.c", false));
    b.add(simpleTaskForOverflow("no_overflow.c", true));

    return b.build();
  }

  private static Object[] simpleTask(String program, boolean verdict) {
    String label = String.format("SimpleTest(%s is %s)", program, verdict);
    return new Object[] {label, program, verdict, false};
  }

  private static Object[] simpleTaskForOverflow(String program, boolean verdict) {
    String label = String.format("SimpleTestForOverflow(%s is %s)", program, verdict);
    return new Object[] {label, program, verdict, true};
  }

  @Test
  public void test() throws Exception {
    Path fullPath = Path.of(TEST_DIR_PATH, program);

    // generate ARG:
    resetCFANodeCounter();
    TestResults firstResult = CPATestRunner.run(config, fullPath.toString());
    ARGState root = (ARGState) firstResult.getCheckerResult().getReached().getFirstState();

    // generate joint automaton
    Automaton aut = converter.getAutomaton(root, true);
    Files.write(automatonPath, aut.toString().getBytes(UTF_8));

    for (String analysis :
        ImmutableList.of("predicateAnalysis.properties", "valueAnalysis.properties")) {
      if (forOverflow) {
        break;
      }
      // test whether C program still gives correct verdict with joint automaton:
      Configuration reConfig =
          TestDataTools.configurationForTest()
              .loadFromResource(ARGToAutomatonConverterTest.class, analysis)
              .setOption("specification", automatonPath.toString())
              .build();
      resetCFANodeCounter();
      TestResults results = CPATestRunner.run(reConfig, fullPath.toString());

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
        Files.write(newAutomatonPath, a.toString().getBytes(UTF_8));
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

    if (forOverflow) {
      Configuration overflowConfig =
          TestDataTools.configurationForTest()
              .loadFromResource(ARGToAutomatonConverterTest.class, "split--overflow.properties")
              .build();
      resetCFANodeCounter();
      TestResults results = CPATestRunner.run(overflowConfig, fullPath.toString());

      assertThat(results).isNotNull();
      if (verdict) {
        results.assertIsSafe();
      } else {
        results.assertIsUnsafe();
      }
    }
  }

  /**
   * For some tests we need to reset the counter for the number of new CFA nodes. This is only OK if
   * it is needed because of files that are generated during the test execution and that refer to
   * fixed CFA node numbers (e.g. a specification automaton that matches transitions to a certain
   * node number).
   */
  @Deprecated
  @SuppressForbidden("reflection only in test")
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

  private static Path newTempFile() throws IOException {
    return TempFile.builder().prefix("automaton").suffix(".spc").create().toAbsolutePath();
  }
}
