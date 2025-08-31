// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

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
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 4).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 4).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerDereferences.isEmpty()).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_28_race_reach_45_escape_racing() throws Exception {
    // this program contains a start_routine argument passed via pthread_create
    Path path = Path.of("./test/programs/mpor/sequentialization/28-race_reach_45-escape_racing.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 2).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // mutex1, mutex2, i (implicit global with pthread_create) and __global_lock (from racemacros.h)
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 4).isTrue();
    // we want to identify int * p = (int *) arg; as a pointer assignment, even on declaration
    assertThat(memoryModel.pointerAssignments.size() == 1).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    // access(*p); is a deref of p
    assertThat(memoryModel.pointerDereferences.size() == 1).isTrue();
    // check that we (only) identify the passing of &i to pthread_create as start_routine arg
    assertThat(memoryModel.startRoutineArgAssignments.size() == 1).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_36_apron_41_threadenter_no_locals_unknown_1_pos() throws Exception {
    // this program contains only local variables, no global variables
    Path path =
        Path.of(
            "./test/programs/mpor/sequentialization/36-apron_41-threadenter-no-locals_unknown_1_pos.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 2).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // only local variables
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 0).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerDereferences.isEmpty()).isTrue();
    assertThat(memoryModel.startRoutineArgAssignments.isEmpty()).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_fib_safe7() throws Exception {
    // this example demonstrates the need to handle local variables with initializers explicitly.
    // otherwise the local variables are declared (and initialized) and then never updated in cases.
    Path path = Path.of("./test/programs/mpor/sequentialization/fib_safe-7.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 3).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 9).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerDereferences.isEmpty()).isTrue();
    assertThat(memoryModel.startRoutineArgAssignments.isEmpty()).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_lazy01() throws Exception {
    Path path = Path.of("./test/programs/mpor/sequentialization/lazy01.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 4).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // mutex and data
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 2).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerDereferences.isEmpty()).isTrue();
    assertThat(memoryModel.startRoutineArgAssignments.isEmpty()).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_mix014_power_oepc_pso_oepc_rmo_oepc() throws Exception {
    // this program is ... very large
    Path path =
        Path.of("./test/programs/mpor/sequentialization/mix014_power.oepc_pso.oepc_rmo.oepc.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 5).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // 32 global variables, but _Bool a$read_delayed; and int *a$read_delayed_var; are never
    // accessed, i.e. never substituted -> tracker does not identify them
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 30).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerDereferences.isEmpty()).isTrue();
    assertThat(memoryModel.startRoutineArgAssignments.isEmpty()).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_queue_longest() throws Exception {
    // this program has a start_routine return via pthread_exit, and pthread_join stores the retval
    Path path = Path.of("./test/programs/mpor/sequentialization/queue_longest.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 3).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    // TODO
    // check that each member of queue struct is identified as relevant individually
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 9).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    // 2 in main, 3 in t1, 1 in t2
    // (pthread_mutex_lock(&m) does not count as poitner parameter assignment)
    assertThat(memoryModel.pointerParameterAssignments.size() == 6).isTrue();
    assertThat(memoryModel.pointerDereferences.size() == 17).isTrue();
    // both pthread_create calls take &queue as arguments
    assertThat(memoryModel.startRoutineArgAssignments.size() == 2).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_read_write_lock_2() throws Exception {
    // this program contains start_routines that start directly with a function call.
    // this forces us to reorder the thread statements, because function statements are usually
    // at the bottom of a thread simulation.
    Path path = Path.of("./test/programs/mpor/sequentialization/read_write_lock-2.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 5).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 4).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerDereferences.isEmpty()).isTrue();
    assertThat(memoryModel.startRoutineArgAssignments.isEmpty()).isTrue();
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_simple_two() throws Exception {
    // this program contains no return statements for the created threads
    Path path = Path.of("./test/programs/mpor/sequentialization/simple_two.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 3).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 1).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerDereferences.isEmpty()).isTrue();
    assertThat(memoryModel.startRoutineArgAssignments.isEmpty()).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_singleton_with_uninit_problems_b() throws Exception {
    // this program has thread creations inside a non-main thread
    Path path =
        Path.of("./test/programs/mpor/sequentialization/singleton_with-uninit-problems-b.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 7).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 1).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    assertThat(memoryModel.pointerParameterAssignments.isEmpty()).isTrue();
    // array subscripts e.g. v[0] do not count as pointer dereferences
    assertThat(memoryModel.pointerDereferences.isEmpty()).isTrue();
    assertThat(memoryModel.startRoutineArgAssignments.isEmpty()).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  @Test
  public void test_stack_1() throws Exception {
    Path path = Path.of("./test/programs/mpor/sequentialization/stack-1.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = MPOROptions.defaultTestInstance();
    SequentializationFields fields = getSequentializationFields(path, options);
    assertThat(fields.numThreads == 3).isTrue();
    assertThat(fields.numThreads == fields.substitutions.size()).isTrue();
    assertThat(fields.memoryModel.isPresent()).isTrue();
    MemoryModel memoryModel = fields.memoryModel.orElseThrow();
    assertThat(memoryModel.getRelevantMemoryLocationAmount() == 3).isTrue();
    assertThat(memoryModel.pointerAssignments.isEmpty()).isTrue();
    // unsigned int * stack = static unsigned int arr[SIZE]
    // counts as pointer parameter assignments
    assertThat(memoryModel.pointerParameterAssignments.size() == 2).isTrue();
    // stack[get_top()] count as pointer dereferences
    assertThat(memoryModel.pointerDereferences.size() == 2).isTrue();
    assertThat(memoryModel.startRoutineArgAssignments.isEmpty()).isTrue();
    // the main thread should always have id 0
    assertThat(fields.mainSubstitution.thread.id == 0).isTrue();
    assertThat(fields.mainSubstitution.thread.threadObject.isEmpty()).isTrue();
  }

  private SequentializationFields getSequentializationFields(
      Path pInputFilePath, MPOROptions pOptions) throws Exception {

    // create cfa for test program pInputFilePath
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
    MPORAlgorithm algorithm = MPORAlgorithm.testInstance(logger, inputCfa, pOptions);
    String inputFileName = "test.i";
    Sequentialization sequentialization =
        algorithm.buildSequentialization(inputFileName, SeqToken.__MPOR_SEQ__ + inputFileName);
    return sequentialization.buildFields();
  }
}
