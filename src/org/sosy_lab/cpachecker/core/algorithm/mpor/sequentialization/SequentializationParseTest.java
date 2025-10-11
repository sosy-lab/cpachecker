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

import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.exceptions.ParserException;

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
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.DECIMAL,
            true,
            true,
            true,
            MultiControlStatementEncoding.IF_ELSE_CHAIN,
            MultiControlStatementEncoding.IF_ELSE_CHAIN,
            false,
            true,
            true,
            false,
            true,
            true,
            0,
            false,
            true,
            false,
            true,
            NondeterminismSource.NUM_STATEMENTS,
            true,
            false,
            ReductionMode.READ_AND_WRITE,
            ReductionOrder.CONFLICT_THEN_BITVECTOR,
            true,
            true,
            false,
            true);
    testProgram(path, options);
  }

  @Test
  public void test_13_privatized_69_refine_protected_loop_interval_true() throws Exception {
    // this program had issues with infinite recursion when reordering blocks
    Path path =
        Path.of(
            "./test/programs/mpor/sequentialization/13-privatized_69-refine-protected-loop-interval_true.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.BINARY,
            false,
            true,
            false,
            MultiControlStatementEncoding.BINARY_SEARCH_TREE,
            MultiControlStatementEncoding.NONE,
            true,
            false,
            false,
            false,
            true,
            true,
            0,
            false,
            // keep enabled
            true,
            true,
            false,
            NondeterminismSource.NUM_STATEMENTS,
            true,
            false,
            ReductionMode.NONE,
            ReductionOrder.NONE,
            false,
            true,
            false,
            // keep enabled
            true);
    testProgram(path, options);
  }

  @Test
  public void test_28_race_reach_45_escape_racing() throws Exception {
    // this program contains a start_routine argument passed via pthread_create
    Path path = Path.of("./test/programs/mpor/sequentialization/28-race_reach_45-escape_racing.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.NONE,
            false,
            false,
            false,
            MultiControlStatementEncoding.IF_ELSE_CHAIN,
            MultiControlStatementEncoding.BINARY_SEARCH_TREE,
            true,
            false,
            false,
            false,
            true,
            true,
            42,
            true,
            false,
            true,
            true,
            NondeterminismSource.NUM_STATEMENTS,
            false,
            false,
            ReductionMode.ACCESS_ONLY,
            ReductionOrder.NONE,
            true,
            false,
            false,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_36_apron_41_threadenter_no_locals_unknown_1_pos() throws Exception {
    // this program contains only local variables, no global variables
    Path path =
        Path.of(
            "./test/programs/mpor/sequentialization/36-apron_41-threadenter-no-locals_unknown_1_pos.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.BINARY,
            true,
            false,
            false,
            MultiControlStatementEncoding.IF_ELSE_CHAIN,
            MultiControlStatementEncoding.SWITCH_CASE,
            false,
            false,
            false,
            false,
            true,
            true,
            1,
            false,
            true,
            false,
            true,
            NondeterminismSource.NEXT_THREAD,
            true,
            false,
            ReductionMode.READ_AND_WRITE,
            ReductionOrder.NONE,
            false,
            false,
            false,
            true);
    testProgram(path, options);
  }

  @Test
  public void test_fib_safe7() throws Exception {
    // this example demonstrates the need to handle local variables with initializers explicitly.
    // otherwise the local variables are declared (and initialized) and then never updated in cases.
    Path path = Path.of("./test/programs/mpor/sequentialization/fib_safe-7.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.DECIMAL,
            true,
            true,
            true,
            MultiControlStatementEncoding.SWITCH_CASE,
            MultiControlStatementEncoding.BINARY_SEARCH_TREE,
            true,
            true,
            true,
            false,
            true,
            true,
            0,
            false,
            false,
            true,
            true,
            NondeterminismSource.NEXT_THREAD_AND_NUM_STATEMENTS,
            false,
            false,
            ReductionMode.ACCESS_ONLY,
            ReductionOrder.BITVECTOR_THEN_CONFLICT,
            true,
            true,
            true,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_lazy01() throws Exception {
    Path path = Path.of("./test/programs/mpor/sequentialization/lazy01.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.DECIMAL,
            true,
            false,
            false,
            MultiControlStatementEncoding.SWITCH_CASE,
            MultiControlStatementEncoding.SWITCH_CASE,
            false,
            false,
            false,
            false,
            true,
            true,
            7,
            true,
            true,
            true,
            true,
            NondeterminismSource.NEXT_THREAD,
            false,
            false,
            ReductionMode.ACCESS_ONLY,
            ReductionOrder.NONE,
            false,
            true,
            true,
            true);
    testProgram(path, options);
  }

  @Test
  public void test_mix014_power_oepc_pso_oepc_rmo_oepc() throws Exception {
    // this program is ... very large
    Path path =
        Path.of("./test/programs/mpor/sequentialization/mix014_power.oepc_pso.oepc_rmo.oepc.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.HEXADECIMAL,
            false,
            false,
            true,
            MultiControlStatementEncoding.SWITCH_CASE,
            MultiControlStatementEncoding.IF_ELSE_CHAIN,
            true,
            false,
            false,
            false,
            false,
            true,
            0,
            false,
            false,
            false,
            true,
            NondeterminismSource.NEXT_THREAD,
            false,
            true,
            ReductionMode.READ_AND_WRITE,
            ReductionOrder.NONE,
            false,
            true,
            false,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_queue_longest() throws Exception {
    // this program has a start_routine return via pthread_exit, and pthread_join stores the retval
    Path path = Path.of("./test/programs/mpor/sequentialization/queue_longest.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.NONE,
            false,
            false,
            false,
            MultiControlStatementEncoding.BINARY_SEARCH_TREE,
            MultiControlStatementEncoding.SWITCH_CASE,
            true,
            false,
            false,
            false,
            true,
            false,
            Integer.MAX_VALUE,
            false,
            true,
            false,
            true,
            NondeterminismSource.NEXT_THREAD,
            false,
            false,
            ReductionMode.NONE,
            ReductionOrder.NONE,
            true,
            true,
            false,
            true);
    testProgram(path, options);
  }

  @Test
  public void test_race_4_1_thread_local_vars() throws Exception {
    // this program had issues with infinite recursion when reordering blocks
    Path path = Path.of("./test/programs/mpor/sequentialization/race-4_1-thread_local_vars.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.DECIMAL,
            false,
            true,
            true,
            MultiControlStatementEncoding.IF_ELSE_CHAIN,
            MultiControlStatementEncoding.SWITCH_CASE,
            true,
            false,
            false,
            false,
            true,
            true,
            0,
            false,
            // keep enabled
            true,
            true,
            false,
            NondeterminismSource.NEXT_THREAD_AND_NUM_STATEMENTS,
            true,
            false,
            ReductionMode.READ_AND_WRITE,
            ReductionOrder.NONE,
            false,
            false,
            false,
            // keep enabled
            true);
    testProgram(path, options);
  }

  @Test
  public void test_read_write_lock_2() throws Exception {
    // this program contains start_routines that start directly with a function call.
    // this forces us to reorder the thread statements, because function statements are usually
    // at the bottom of a thread simulation.
    Path path = Path.of("./test/programs/mpor/sequentialization/read_write_lock-2.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.BINARY,
            true,
            true,
            true,
            MultiControlStatementEncoding.BINARY_SEARCH_TREE,
            MultiControlStatementEncoding.BINARY_SEARCH_TREE,
            true,
            false,
            false,
            false,
            true,
            true,
            0,
            false,
            false,
            true,
            true,
            NondeterminismSource.NEXT_THREAD,
            false,
            false,
            ReductionMode.ACCESS_ONLY,
            ReductionOrder.CONFLICT_THEN_BITVECTOR,
            true,
            false,
            false,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_simple_two() throws Exception {
    // this program contains no return statements for the created threads
    Path path = Path.of("./test/programs/mpor/sequentialization/simple_two.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.SPARSE,
            true,
            false,
            false,
            MultiControlStatementEncoding.IF_ELSE_CHAIN,
            MultiControlStatementEncoding.SWITCH_CASE,
            false,
            false,
            false,
            true,
            false,
            true,
            9999,
            false,
            true,
            true,
            false,
            NondeterminismSource.NEXT_THREAD_AND_NUM_STATEMENTS,
            true,
            false,
            ReductionMode.READ_AND_WRITE,
            ReductionOrder.NONE,
            false,
            true,
            false,
            true);
    testProgram(path, options);
  }

  @Test
  public void test_singleton_with_uninit_problems_b() throws Exception {
    // this program has thread creations inside a non-main thread
    Path path =
        Path.of("./test/programs/mpor/sequentialization/singleton_with-uninit-problems-b.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.NONE,
            false,
            true,
            false,
            MultiControlStatementEncoding.SWITCH_CASE,
            MultiControlStatementEncoding.BINARY_SEARCH_TREE,
            true,
            true,
            true,
            false,
            false,
            true,
            16,
            true,
            false,
            false,
            false,
            NondeterminismSource.NEXT_THREAD_AND_NUM_STATEMENTS,
            false,
            false,
            ReductionMode.NONE,
            ReductionOrder.NONE,
            true,
            false,
            true,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_stack_1() throws Exception {
    Path path = Path.of("./test/programs/mpor/sequentialization/stack-1.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            true,
            BitVectorEncoding.SPARSE,
            true,
            true,
            true,
            MultiControlStatementEncoding.BINARY_SEARCH_TREE,
            MultiControlStatementEncoding.IF_ELSE_CHAIN,
            false,
            false,
            false,
            false,
            false,
            true,
            32,
            false,
            true,
            false,
            true,
            NondeterminismSource.NEXT_THREAD,
            false,
            false,
            ReductionMode.ACCESS_ONLY,
            ReductionOrder.BITVECTOR_THEN_CONFLICT,
            false,
            true,
            true,
            true);
    testProgram(path, options);
  }

  private CFA buildCfaTestInstance(
      Path pInputFilePath, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws Exception {

    // create cfa for test program pInputFilePath. always use preprocessor, we work with .c files
    CFACreator cfaCreator = MPORUtil.buildCfaCreatorWithPreprocessor(pLogger, pShutdownNotifier);
    return cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));
  }

  private String buildTestOutputProgram(
      MPOROptions pOptions,
      Path pInputFilePath,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger)
      throws Exception {

    CFA cfaA = buildCfaTestInstance(pInputFilePath, pLogger, pShutdownNotifier);
    return Sequentialization.tryBuildProgramString(
        pOptions, cfaA, "test", pShutdownNotifier, pLogger);
  }

  private void testProgram(Path pInputFilePath, MPOROptions pOptions) throws Exception {
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();

    // create two sequentializations A, B of the same input program with the same option
    String programA = buildTestOutputProgram(pOptions, pInputFilePath, shutdownNotifier, logger);
    String programB = buildTestOutputProgram(pOptions, pInputFilePath, shutdownNotifier, logger);

    // test that the output programs of A, B are equal
    // (this does not imply that our algorithm is deterministic)
    testEqualOutput(programA, programB);
    // test if program A parses (which implies that program B parses too)
    testParse(programA, logger, shutdownNotifier);
  }

  // TODO also try to compare SequentializationFields for equality
  /**
   * Checks whether two sequentializations with the exact same input result in the exact same
   * output, i.e. the same {@link String} output and the same {@link SequentializationFields}
   */
  private void testEqualOutput(String pStringA, String pStringB) {
    ImmutableList<String> linesA = SeqStringUtil.splitOnNewline(pStringA);
    ImmutableList<String> linesB = SeqStringUtil.splitOnNewline(pStringB);
    assertThat(linesA.size() == linesB.size()).isTrue();
    for (int i = 0; i < linesA.size(); i++) {
      String lineA = linesA.get(i);
      String lineB = linesB.get(i);
      assertWithMessage(
              "lineA, lineB with number " + (i + Sequentialization.FIRST_LINE) + " are not equal: ")
          .that(lineA)
          .isEqualTo(lineB);
    }
  }

  private void testParse(
      String pSequentialization, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, ParserException, InterruptedException {

    assertThat(pSequentialization.isEmpty()).isFalse();

    // test that seq can be parsed and cfa created -> code compiles
    CFACreator cfaCreator = MPORUtil.buildCfaCreator(pLogger, pShutdownNotifier);
    CFA seqCfa = cfaCreator.parseSourceAndCreateCFA(pSequentialization);
    assertThat(seqCfa != null).isTrue();

    // "anti" test: just remove the last 100 chars from the seq, it probably won't compile
    String faultySeq = pSequentialization.substring(0, pSequentialization.length() - 100);
    assertThat(faultySeq.isEmpty()).isFalse();

    // test that we get an exception while parsing the new "faulty" program
    boolean fail = false;
    try {
      cfaCreator.parseSourceAndCreateCFA(faultySeq);
    } catch (Exception exception) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }
}
