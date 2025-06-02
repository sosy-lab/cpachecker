// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.ControlFlowEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

/**
 * Tests if programs with different characteristics are correctly transformed by our implementation
 * i.e. parse correctly. The programs are in part taken from SV-Benchmarks, but may be altered to
 * include more varying characteristics, such as additional pthread functions.
 */
public class SequentializationTest {

  // TODO these programs assign function pointers (also as initializers), some are not found:
  // pthread-driver-races/char_pc8736x_gpio_pc8736x_gpio_change_pc8736x_gpio_configure

  // TODO this triggers a pthread_create loop error, even though its outside the loop
  // "divinefifo-bug_1w1r"

  // TODO this program has "0;" statements, that can be pruned (probably "pre-evaluated" statements
  //  by CPAchecker during CFA creation)
  // pthread-divine/tls_basic

  // TODO this program is analyzed as false by multiple tools (Bubaak, CBMC, CPAchecker, ESBMC,
  //  Symbiotic) even though its true, but only when linkReduction is enabled
  // weaver/chl-match-symm.wvr.c

  // TODO linkReduction infinite recursion errors for:
  // pthread-complex/elimination_backoff_stack

  @Test
  public void test_13_privatized_04_priv_multi_true() throws Exception {
    // this program contains multiple loops whose condition only contains local variables
    Path path =
        Path.of("./test/programs/mpor_seq/seq_compilable/13-privatized_04-priv_multi_true.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.SCALAR,
            true,
            BitVectorReduction.READ_AND_WRITE,
            true,
            ControlFlowEncoding.SWITCH_CASE,
            false,
            true,
            true,
            true,
            true,
            false,
            true,
            true,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_28_race_reach_45_escape_racing() throws Exception {
    // this program contains a start_routine argument passed via pthread_create
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/28-race_reach_45-escape_racing.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.DECIMAL,
            false,
            BitVectorReduction.ACCESS_ONLY,
            false,
            ControlFlowEncoding.BINARY_IF_TREE,
            true,
            true,
            true,
            true,
            false,
            false,
            true,
            true,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_36_apron_41_threadenter_no_locals_unknown_1_pos() throws Exception {
    // this program contains only local variables, no global variables
    Path path =
        Path.of(
            "./test/programs/mpor_seq/seq_compilable/36-apron_41-threadenter-no-locals_unknown_1_pos.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.BINARY,
            true,
            BitVectorReduction.READ_AND_WRITE,
            false,
            ControlFlowEncoding.SWITCH_CASE,
            false,
            true,
            true,
            false,
            false,
            false,
            true,
            false,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_fib_safe7() throws Exception {
    // this example demonstrates the need to handle local variables with initializers explicitly.
    // otherwise the local variables are declared (and initialized) and then never updated in cases.
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/fib_safe-7.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.NONE,
            false,
            BitVectorReduction.NONE,
            true,
            ControlFlowEncoding.BINARY_IF_TREE,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true);
    testProgram(path, options);
  }

  @Test
  public void test_lazy01() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/lazy01.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.DECIMAL,
            false,
            BitVectorReduction.ACCESS_ONLY,
            false,
            ControlFlowEncoding.SWITCH_CASE,
            false,
            true,
            true,
            false,
            true,
            true,
            true,
            false,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_mix014_power_oepc_pso_oepc_rmo_oepc() throws Exception {
    // this program is ... very large
    Path path =
        Path.of("./test/programs/mpor_seq/seq_compilable/mix014_power.oepc_pso.oepc_rmo.oepc.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.NONE,
            false,
            BitVectorReduction.NONE,
            false,
            ControlFlowEncoding.BINARY_IF_TREE,
            true,
            false,
            true,
            false,
            true,
            false,
            true,
            false,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_queue_longest() throws Exception {
    // this program has a start_routine return via pthread_exit, and pthread_join stores the retval
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/queue_longest.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.NONE,
            false,
            BitVectorReduction.NONE,
            false,
            ControlFlowEncoding.SWITCH_CASE,
            true,
            true,
            false,
            true,
            true,
            false,
            true,
            false,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_read_write_lock_2() throws Exception {
    // this program contains start_routines that start directly with a function call.
    // this forces us to reorder the thread statements, because function statements are usually
    // at the bottom of a thread simulation.
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/read_write_lock-2.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.BINARY,
            false,
            BitVectorReduction.ACCESS_ONLY,
            true,
            ControlFlowEncoding.BINARY_IF_TREE,
            true,
            true,
            true,
            true,
            false,
            false,
            true,
            false,
            false);
    testProgram(path, options);
  }

  @Test
  public void test_simple_two() throws Exception {
    // this program contains no return statements for the created threads
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/simple_two.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.HEXADECIMAL,
            true,
            BitVectorReduction.READ_AND_WRITE,
            false,
            ControlFlowEncoding.SWITCH_CASE,
            false,
            false,
            true,
            false,
            true,
            false,
            false,
            true,
            true);
    testProgram(path, options);
  }

  @Test
  public void test_singleton_with_uninit_problems_b() throws Exception {
    // this program has thread creations inside a non-main thread
    Path path =
        Path.of("./test/programs/mpor_seq/seq_compilable/singleton_with-uninit-problems-b.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.NONE,
            false,
            BitVectorReduction.NONE,
            true,
            ControlFlowEncoding.BINARY_IF_TREE,
            true,
            false,
            true,
            true,
            false,
            true,
            false,
            true,
            true);
    testProgram(path, options);
  }

  @Test
  public void test_stack_1() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/stack-1.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options =
        MPOROptions.testInstance(
            BitVectorEncoding.SCALAR,
            false,
            BitVectorReduction.ACCESS_ONLY,
            true,
            ControlFlowEncoding.SWITCH_CASE,
            false,
            false,
            true,
            false,
            true,
            true,
            true,
            false,
            true);
    testProgram(path, options);
  }

  private void testProgram(Path pInputFilePath, MPOROptions pOptions) throws Exception {
    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    CFACreator creatorWithPreProcessor =
        new CFACreator(
            Configuration.builder().setOption("parser.usePreprocessor", "true").build(),
            logger,
            shutdownNotifier);
    CFA inputCfa =
        creatorWithPreProcessor.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));

    // create mpor algorithm and generate seq
    MPORAlgorithm algorithm = MPORAlgorithm.testInstance(pOptions, logger, inputCfa);
    String inputFileName = "test.i";
    String sequentialization =
        algorithm
            .buildSequentialization(inputFileName, SeqToken.__MPOR_SEQ__ + inputFileName)
            .toString();

    // test that seq can be parsed and cfa created -> code compiles
    CFACreator creator = new CFACreator(Configuration.builder().build(), logger, shutdownNotifier);
    CFA seqCfa = creator.parseSourceAndCreateCFA(sequentialization);
    assertThat(seqCfa != null).isTrue();

    // "anti" test: just remove the last 100 chars from the seq, it probably won't compile
    String faultySeq = sequentialization.substring(0, sequentialization.length() - 100);

    // test that we get an exception while parsing the new "faulty" program
    boolean fail = false;
    try {
      creator.parseSourceAndCreateCFA(faultySeq);
    } catch (Exception exception) {
      fail = true;
    }
    assertThat(fail).isTrue();
  }
}
