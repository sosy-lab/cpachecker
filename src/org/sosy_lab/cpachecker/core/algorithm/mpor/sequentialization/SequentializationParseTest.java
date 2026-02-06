// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/**
 * Tests if programs with different characteristics and options parse correctly. The programs are in
 * part taken from SV-Benchmarks, but may be altered to include more varying characteristics, such
 * as additional pthread functions.
 */
public class SequentializationParseTest {

  // TODO these programs assign function pointers (also as initializers), some are not found:
  // pthread-driver-races/char_pc8736x_gpio_pc8736x_gpio_change_pc8736x_gpio_configure

  // TODO this triggers a pthread_create loop error, even though its outside the loop
  // "divinefifo-bug_1w1r"

  // TODO this program has "0;" statements, that can be pruned (probably "pre-evaluated" statements
  //  by CPAchecker during CFA creation)
  // pthread-divine/tls_basic

  @Test
  public void test_13_privatized_04_priv_multi_true() throws Exception {
    // this program contains multiple loops whose condition only contains local variables
    Path path =
        Path.of("./test/programs/mpor/sequentialization/13-privatized_04-priv_multi_true.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "DECIMAL")
            .setOption("analysis.algorithm.MPOR.comments", "true")
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "IF_ELSE_CHAIN")
            .setOption("analysis.algorithm.MPOR.noBackwardLoopGoto", "false")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption("analysis.algorithm.MPOR.reduceLastThreadOrder", "true")
            .setOption("analysis.algorithm.MPOR.reduceUntilConflict", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "READ_AND_WRITE")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_13_privatized_69_refine_protected_loop_interval_true() throws Exception {
    // this program had issues with infinite recursion when reordering blocks
    Path path =
        Path.of(
            "./test/programs/mpor/sequentialization/13-privatized_69-refine-protected-loop-interval_true.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "BINARY")
            .setOption("analysis.algorithm.MPOR.comments", "true")
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "BINARY_SEARCH_TREE")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "true")
            .setOption("analysis.algorithm.MPOR.reduceUntilConflict", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "READ_AND_WRITE")
            .setOption("analysis.algorithm.MPOR.scalarPc", "false")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_28_race_reach_45_escape_racing() throws Exception {
    // this program contains a start_routine argument passed via pthread_create
    Path path = Path.of("./test/programs/mpor/sequentialization/28-race_reach_45-escape_racing.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "IF_ELSE_CHAIN")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "true")
            .setOption("analysis.algorithm.MPOR.loopIterations", "42")
            .setOption("analysis.algorithm.MPOR.noBackwardGoto", "false")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption("analysis.algorithm.MPOR.nondeterminismSource", "NUM_STATEMENTS")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .setOption("analysis.algorithm.MPOR.validateNoBackwardGoto", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_36_apron_41_threadenter_no_locals_unknown_1_pos() throws Exception {
    // this program contains only local variables, no global variables
    Path path =
        Path.of(
            "./test/programs/mpor/sequentialization/36-apron_41-threadenter-no-locals_unknown_1_pos.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "BINARY")
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "IF_ELSE_CHAIN")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "SWITCH_CASE")
            .setOption("analysis.algorithm.MPOR.loopIterations", "1")
            .setOption("analysis.algorithm.MPOR.noBackwardLoopGoto", "false")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption("analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD")
            .setOption("analysis.algorithm.MPOR.pruneBitVectorEvaluations", "true")
            .setOption("analysis.algorithm.MPOR.reduceUntilConflict", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "READ_AND_WRITE")
            .setOption("analysis.algorithm.MPOR.scalarPc", "false")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_fib_safe7() throws Exception {
    // this example demonstrates the need to handle local variables with initializers explicitly.
    // otherwise the local variables are declared (and initialized) and then never updated in cases.
    Path path = Path.of("./test/programs/mpor/sequentialization/fib_safe-7.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "DECIMAL")
            .setOption("analysis.algorithm.MPOR.comments", "true")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "BINARY_SEARCH_TREE")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "true")
            .setOption("analysis.algorithm.MPOR.noBackwardGoto", "false")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption(
                "analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD_AND_NUM_STATEMENTS")
            .setOption("analysis.algorithm.MPOR.reduceIgnoreSleep", "true")
            .setOption("analysis.algorithm.MPOR.reduceLastThreadOrder", "true")
            .setOption("analysis.algorithm.MPOR.reduceUntilConflict", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "ACCESS_ONLY")
            .setOption("analysis.algorithm.MPOR.validateNoBackwardGoto", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_lazy01() throws Exception {
    Path path = Path.of("./test/programs/mpor/sequentialization/lazy01.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "DECIMAL")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "SWITCH_CASE")
            .setOption("analysis.algorithm.MPOR.loopIterations", "7")
            .setOption("analysis.algorithm.MPOR.loopUnrolling", "true")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption("analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD")
            .setOption("analysis.algorithm.MPOR.reduceUntilConflict", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "ACCESS_ONLY")
            .setOption("analysis.algorithm.MPOR.scalarPc", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_mix014_power_oepc_pso_oepc_rmo_oepc() throws Exception {
    // this program is ... very large
    Path path =
        Path.of("./test/programs/mpor/sequentialization/mix014_power.oepc_pso.oepc_rmo.oepc.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "HEXADECIMAL")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "IF_ELSE_CHAIN")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "true")
            .setOption("analysis.algorithm.MPOR.noBackwardGoto", "false")
            .setOption("analysis.algorithm.MPOR.noBackwardLoopGoto", "false")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption("analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD")
            .setOption("analysis.algorithm.MPOR.pruneBitVectorEvaluations", "true")
            .setOption("analysis.algorithm.MPOR.reduceLastThreadOrder", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "READ_AND_WRITE")
            .setOption("analysis.algorithm.MPOR.scalarPc", "false")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .setOption("analysis.algorithm.MPOR.validateNoBackwardGoto", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_queue_longest() throws Exception {
    // this program has a start_routine return via pthread_exit, and pthread_join stores the retval
    Path path = Path.of("./test/programs/mpor/sequentialization/queue_longest.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "BINARY_SEARCH_TREE")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "SWITCH_CASE")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "true")
            .setOption("analysis.algorithm.MPOR.linkReduction", "false")
            .setOption("analysis.algorithm.MPOR.loopIterations", "2000000000")
            .setOption("analysis.algorithm.MPOR.noBackwardLoopGoto", "false")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption("analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_race_4_1_thread_local_vars() throws Exception {
    // this program had issues with infinite recursion when reordering blocks
    Path path = Path.of("./test/programs/mpor/sequentialization/race-4_1-thread_local_vars.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "DECIMAL")
            .setOption("analysis.algorithm.MPOR.comments", "true")
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "IF_ELSE_CHAIN")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "SWITCH_CASE")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "true")
            .setOption(
                "analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD_AND_NUM_STATEMENTS")
            .setOption("analysis.algorithm.MPOR.pruneBitVectorEvaluations", "true")
            .setOption("analysis.algorithm.MPOR.reduceLastThreadOrder", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "READ_AND_WRITE")
            .setOption("analysis.algorithm.MPOR.scalarPc", "false")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_read_write_lock_2() throws Exception {
    // this program contains start_routines that start directly with a function call.
    // this forces us to reorder the thread statements, because function statements are usually
    // at the bottom of a thread simulation.
    Path path = Path.of("./test/programs/mpor/sequentialization/read_write_lock-2.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "BINARY")
            .setOption("analysis.algorithm.MPOR.comments", "true")
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "BINARY_SEARCH_TREE")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "BINARY_SEARCH_TREE")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "true")
            .setOption("analysis.algorithm.MPOR.noBackwardGoto", "false")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption("analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD")
            .setOption("analysis.algorithm.MPOR.reduceLastThreadOrder", "true")
            .setOption("analysis.algorithm.MPOR.reduceUntilConflict", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "ACCESS_ONLY")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .setOption("analysis.algorithm.MPOR.validateNoBackwardGoto", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_simple_two() throws Exception {
    // this program contains no return statements for the created threads
    Path path = Path.of("./test/programs/mpor/sequentialization/simple_two.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "SPARSE")
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "IF_ELSE_CHAIN")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "SWITCH_CASE")
            .setOption("analysis.algorithm.MPOR.loopIterations", "9999")
            .setOption(
                "analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD_AND_NUM_STATEMENTS")
            .setOption("analysis.algorithm.MPOR.pruneBitVectorEvaluations", "true")
            .setOption("analysis.algorithm.MPOR.pruneSparseBitVectors", "true")
            .setOption("analysis.algorithm.MPOR.pruneSparseBitVectorWrites", "true")
            .setOption("analysis.algorithm.MPOR.reduceUntilConflict", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "READ_AND_WRITE")
            .setOption("analysis.algorithm.MPOR.scalarPc", "false")
            .setOption("analysis.algorithm.MPOR.shortVariableNames", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_singleton_with_uninit_problems_b() throws Exception {
    // this program has thread creations inside a non-main thread
    Path path =
        Path.of("./test/programs/mpor/sequentialization/singleton_with-uninit-problems-b.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.comments", "true")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "BINARY_SEARCH_TREE")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "true")
            .setOption("analysis.algorithm.MPOR.loopIterations", "16")
            .setOption("analysis.algorithm.MPOR.loopUnrolling", "true")
            .setOption("analysis.algorithm.MPOR.noBackwardGoto", "false")
            .setOption("analysis.algorithm.MPOR.noBackwardLoopGoto", "false")
            .setOption(
                "analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD_AND_NUM_STATEMENTS")
            .setOption("analysis.algorithm.MPOR.validateNoBackwardGoto", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  @Test
  public void test_stack_1() throws Exception {
    Path path = Path.of("./test/programs/mpor/sequentialization/stack-1.c");
    assertThat(Files.exists(path)).isTrue();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("analysis.algorithm.MPOR.bitVectorEncoding", "SPARSE")
            .setOption("analysis.algorithm.MPOR.comments", "true")
            .setOption("analysis.algorithm.MPOR.controlEncodingStatement", "BINARY_SEARCH_TREE")
            .setOption("analysis.algorithm.MPOR.controlEncodingThread", "IF_ELSE_CHAIN")
            .setOption("analysis.algorithm.MPOR.inputFunctionDeclarations", "false")
            .setOption("analysis.algorithm.MPOR.loopIterations", "32")
            .setOption("analysis.algorithm.MPOR.noBackwardLoopGoto", "false")
            .setOption("analysis.algorithm.MPOR.nondeterminismSigned", "true")
            .setOption("analysis.algorithm.MPOR.nondeterminismSource", "NEXT_THREAD")
            .setOption("analysis.algorithm.MPOR.reduceUntilConflict", "true")
            .setOption("analysis.algorithm.MPOR.reductionMode", "ACCESS_ONLY")
            .setOption("analysis.algorithm.MPOR.scalarPc", "false")
            .build();
    MPOROptions options = new MPOROptions(config);
    testProgram(path, options);
  }

  private CFA buildCfaTestInstance(
      Path pInputFilePath, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws Exception {

    // create cfa for test program pInputFilePath. always use preprocessor, we work with .c files
    CFACreator cfaCreator =
        MPORUtil.buildTestCfaCreatorWithPreprocessor(pLogger, pShutdownNotifier);
    return cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));
  }

  private String buildTestOutputProgram(
      MPOROptions pOptions,
      Path pInputFilePath,
      Configuration pConfiguration,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger)
      throws Exception {

    CFA cfa = buildCfaTestInstance(pInputFilePath, pLogger, pShutdownNotifier);
    SequentializationUtils utils =
        SequentializationUtils.of(cfa, pConfiguration, pLogger, pShutdownNotifier);
    return Sequentialization.tryBuildProgramString(pOptions, cfa, utils);
  }

  private void testProgram(Path pInputFilePath, MPOROptions pOptions) throws Exception {
    Configuration configuration = TestDataTools.configurationForTest().build();
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();

    // create two sequentializations A, B of the same input program with the same option
    String programA =
        buildTestOutputProgram(pOptions, pInputFilePath, configuration, shutdownNotifier, logger);
    String programB =
        buildTestOutputProgram(pOptions, pInputFilePath, configuration, shutdownNotifier, logger);

    // test that the output programs of A, B are equal
    // (this does not imply that our algorithm is deterministic)
    testEqualOutput(programA, programB);
    // test if program A parses (which implies that program B parses too)
    testParse(programA, logger, shutdownNotifier);
  }

  private static final String ANON_TYPE_KEYWORD = "__anon_type_";

  private static final int FIRST_LINE = 1;

  /**
   * Checks whether two sequentializations with the exact same input result in the exact same
   * output, i.e. the same {@link String} output and the same {@link SequentializationFields}
   */
  private void testEqualOutput(String pStringA, String pStringB) {
    ImmutableList<String> linesA = SeqStringUtil.splitOnNewline(pStringA);
    ImmutableList<String> linesB = SeqStringUtil.splitOnNewline(pStringB);
    assertThat(linesA).hasSize(linesB.size());
    for (int i = 0; i < linesA.size(); i++) {
      String lineA = linesA.get(i);
      String lineB = linesB.get(i);
      // ignore __anon_type_{count} since the static counter is not reset between CFA creation runs
      if (!lineA.contains(ANON_TYPE_KEYWORD) && !lineB.contains(ANON_TYPE_KEYWORD)) {
        assertWithMessage("lineA, lineB with number %s are not equal: ", (i + FIRST_LINE))
            .that(lineA)
            .isEqualTo(lineB);
      }
    }
  }

  private void testParse(
      String pSequentialization, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, ParserException, InterruptedException {

    assertThat(pSequentialization).isNotEmpty();

    // test that seq can be parsed and cfa created -> code compiles
    CFACreator cfaCreator = MPORUtil.buildTestCfaCreator(pLogger, pShutdownNotifier);
    CFA seqCfa = cfaCreator.parseSourceAndCreateCFA(pSequentialization);
    assertThat(seqCfa).isNotNull();

    // "anti" test: just remove the last 100 chars from the seq, it probably won't compile
    String faultySeq = pSequentialization.substring(0, pSequentialization.length() - 100);
    assertThat(faultySeq).isNotEmpty();

    // test that we get an exception while parsing the new "faulty" program
    assertThrows(ParserException.class, () -> cfaCreator.parseSourceAndCreateCFA(faultySeq));
  }
}
