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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class SequentializationTest {

  // TODO this triggers an AssertionError
  // goblint-regression/36-apron_21-traces-cluster-based-true.c
  // TODO this triggers a substitute not found because the pthread_create call passes
  //  a parameter to the start routine and the thread reads it
  // "ring_2w1r-2",
  // TODO this triggers a pthread_create loop error, even though its outside the loop
  // "divinefifo-bug_1w1r"

  @Test
  public void testCompileSeq_fib_safe7() throws Exception {
    // contains __VERIFIER_atomic_begin and __VERIFIER_atomic_end
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/fib_safe-7.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = new MPOROptions(true, true, true);
    testCompile(path, options);
  }

  @Test
  public void testCompileSeq_lazy01() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/lazy01.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = new MPOROptions(true, false, true);
    testCompile(path, options);
  }

  @Test
  public void testCompileSeq_queue_longest() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/queue_longest.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = new MPOROptions(false, true, true);
    testCompile(path, options);
  }

  @Test
  public void testCompileSeq_simple_two() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/simple_two.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = new MPOROptions(false, false, true);
    testCompile(path, options);
  }

  @Test
  public void testCompileSeq_singleton_with_uninit_problems_b() throws Exception {
    Path path =
        Path.of("./test/programs/mpor_seq/seq_compilable/singleton_with-uninit-problems-b.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = new MPOROptions(true, true, false);
    testCompile(path, options);
  }

  @Test
  public void testCompileSeq_stack1() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/stack-1.c");
    assertThat(Files.exists(path)).isTrue();
    MPOROptions options = new MPOROptions(true, false, false);
    testCompile(path, options);
  }

  private void testCompile(Path pInputFilePath, MPOROptions pOptions) throws Exception {
    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    CFACreator creatorWithPreProcessor =
        new CFACreator(
            Configuration.builder().setOption("parser.usePreprocessor", "true").build(),
            logger,
            ShutdownNotifier.createDummy());
    CFA inputCfa =
        creatorWithPreProcessor.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));

    // create mpor algorithm and generate seq
    MPORAlgorithm algorithm = MPORAlgorithm.testInstance(pOptions, logger, inputCfa);
    String initSeq = algorithm.buildInitSeq();
    String testProgram = "test.i";
    String finalSeq =
        algorithm.buildFinalSeq(testProgram, SeqToken.__MPOR_SEQ__ + testProgram, initSeq);

    // test that seq can be parsed and cfa created -> code compiles
    CFACreator creator = new CFACreator(Configuration.builder().build(), logger, shutdownNotifier);
    CFA seqCfa = creator.parseSourceAndCreateCFA(finalSeq);
    assertThat(seqCfa != null).isTrue();

    // "anti" test: just remove the last 100 chars from the seq, it probably won't compile
    String faultySeq = finalSeq.substring(0, finalSeq.length() - 100);

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
