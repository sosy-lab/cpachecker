// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadBuilder;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/**
 * Tests if {@link SequentializationFields} are expected depending on the input program, e.g. number
 * of threads.
 */
public class SequentializationFieldsTest {

  @Test
  public void test_13_privatized_04_priv_multi_true() throws Exception {
    // this program contains multiple loops whose condition only contains local variables
    Path path =
        Path.of("./test/programs/mpor/sequentialization/13-privatized_04-priv_multi_true.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(4);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(4);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_28_race_reach_45_escape_racing() throws Exception {
    // this program contains a start_routine argument passed via pthread_create
    Path path = Path.of("./test/programs/mpor/sequentialization/28-race_reach_45-escape_racing.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(2);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // mutex1, mutex2, i (implicit global with pthread_create) and __global_lock (from racemacros.h)
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(4);
    // we want to identify int * p = (int *) arg; as a pointer assignment, even on declaration
    assertThat(memoryModel.pointerAssignments.size()).isEqualTo(1);
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    // access(*p); is a deref of p
    assertThat(memoryModel.pointerDereferences).hasSize(1);
    // check that we (only) identify the passing of &i to pthread_create as start_routine arg
    assertThat(memoryModel.startRoutineArgAssignments.size()).isEqualTo(1);
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_36_apron_41_threadenter_no_locals_unknown_1_pos() throws Exception {
    // this program contains only local variables, no global variables
    Path path =
        Path.of(
            "./test/programs/mpor/sequentialization/36-apron_41-threadenter-no-locals_unknown_1_pos.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(2);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // only local variables
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(0);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_chl_match_symm_wvr() throws Exception {
    // this program contains multiple calls to the same function in a single thread
    Path path = Path.of("./test/programs/mpor/sequentialization/chl-match-symm.wvr.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(3);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(8);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();

    // check that each __CPAchecker_TMP variable storing function return values is present once.
    // this program contains a lot of __CPAchecker_TMP variables, and we want to ensure that they
    // are in their correct place and not mixed up during the substitution process.
    Set<CLeftHandSide> visited = new HashSet<>();
    for (FunctionStatements functionStatements :
        fields.ghostElements.functionStatements().values()) {
      for (FunctionReturnValueAssignment returnValueAssignment :
          functionStatements.returnValueAssignments().values()) {
        assertWithMessage(
                "Duplicate __CPAchecker_TMP variable encountered in assignment: %s",
                returnValueAssignment.statement().toASTString())
            .that(visited.add(returnValueAssignment.statement().getLeftHandSide()))
            .isTrue();
      }
    }
  }

  @Test
  public void test_fib_safe7() throws Exception {
    // this example demonstrates the need to handle local variables with initializers explicitly.
    // otherwise the local variables are declared (and initialized) and then never updated in cases.
    Path path = Path.of("./test/programs/mpor/sequentialization/fib_safe-7.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(3);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(8);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_lazy01() throws Exception {
    Path path = Path.of("./test/programs/mpor/sequentialization/lazy01.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(4);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // mutex and data
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(2);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_mix008_tso_oepc() throws Exception {
    // this program was incorrect 'true' with last_thread order reduction
    Path path = Path.of("./test/programs/mpor/sequentialization/mix008_tso.oepc.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(5);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // 49 global variables, but 4 are never accessed -> not identified by tracker
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(45);
    assertThat(memoryModel.parameterAssignments.size()).isEqualTo(2);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_mix014_power_oepc_pso_oepc_rmo_oepc() throws Exception {
    // this program is ... very large
    Path path =
        Path.of("./test/programs/mpor/sequentialization/mix014_power.oepc_pso.oepc_rmo.oepc.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(5);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // 32 global variables, but _Bool a$read_delayed; and int *a$read_delayed_var; are never
    // accessed, i.e. never substituted -> tracker does not identify them
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(30);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_queue_longest() throws Exception {
    // this program has a start_routine return via pthread_exit, and pthread_join stores the retval
    Path path = Path.of("./test/programs/mpor/sequentialization/queue_longest.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(3);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // TODO should actually be 9, though 5 is still fine (overapproximation)
    // check that each member of queue struct is identified as relevant individually
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(5);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    // 2 in main, 3 in t1, 1 in t2
    // (pthread_mutex_lock(&m) does not count as poitner parameter assignment)
    assertThat(memoryModel.pointerParameterAssignments.size()).isEqualTo(6);
    assertThat(memoryModel.pointerDereferences).hasSize(16);
    // both pthread_create calls take &queue as arguments
    assertThat(memoryModel.startRoutineArgAssignments.size()).isEqualTo(2);
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_read_write_lock_2() throws Exception {
    // this program contains start_routines that start directly with a function call.
    // this forces us to reorder the thread statements, because function statements are usually
    // at the bottom of a thread simulation.
    Path path = Path.of("./test/programs/mpor/sequentialization/read_write_lock-2.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(5);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(4);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_simple_two() throws Exception {
    // this program contains no return statements for the created threads
    Path path = Path.of("./test/programs/mpor/sequentialization/simple_two.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(4);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(1);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    assertThat(memoryModel.pointerDereferences).isEmpty();
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_singleton_with_uninit_problems_b() throws Exception {
    // this program has thread creations inside a non-main thread
    Path path =
        Path.of("./test/programs/mpor/sequentialization/singleton_with-uninit-problems-b.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(7);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(1);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    assertThat(memoryModel.pointerParameterAssignments).isEmpty();
    // v[0] counts as pointer dereference, but only once (same declaration)
    assertThat(memoryModel.pointerDereferences).hasSize(1);
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  @Test
  public void test_stack_1() throws Exception {
    Path path = Path.of("./test/programs/mpor/sequentialization/stack-1.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.getDefaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads).isEqualTo(3);
    assertThat(fields.numThreads).isEqualTo(fields.substitutions.size());
    assertThat(fields.memoryModel).isPresent();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount()).isEqualTo(3);
    assertThat(memoryModel.pointerAssignments).isEmpty();
    // unsigned int * stack = static unsigned int arr[SIZE]
    // counts as pointer parameter assignments
    assertThat(memoryModel.pointerParameterAssignments.size()).isEqualTo(2);
    // stack[get_top()] count as pointer dereferences
    assertThat(memoryModel.pointerDereferences).hasSize(2);
    assertThat(memoryModel.startRoutineArgAssignments).isEmpty();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id()).isEqualTo(MPORThreadBuilder.MAIN_THREAD_ID);
    assertThat(fields.mainSubstitution.thread.threadObject()).isEmpty();
  }

  private SequentializationFields getSequentializationFields(
      Path pInputFilePath, MPOROptions pOptions) throws Exception {

    // create cfa for test program pInputFilePath
    Configuration config = TestDataTools.configurationForTest().build();
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    CFACreator cfaCreator = MPORUtil.buildTestCfaCreatorWithPreprocessor(logger, shutdownNotifier);
    CFA inputCfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));
    SequentializationUtils utils =
        SequentializationUtils.of(inputCfa, config, logger, shutdownNotifier);
    return new SequentializationFields(pOptions, inputCfa, utils);
  }
}
