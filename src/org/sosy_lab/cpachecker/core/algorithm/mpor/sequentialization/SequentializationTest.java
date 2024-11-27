// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;

public class SequentializationTest {

  // TODO these trigger an error where the return value assignment is empty
  // "singleton-b.i",
  // "fib_safe-5.i",
  // TODO this triggers a substitute not found because the pthread_create call passes
  //  a parameter to the start routine and the thread reads it
  // "ring_2w1r-2.i",
  // TODO this triggers a pthread_create loop error, even though its outside the loop
  // "divinefifo-bug_1w1r.i"

  @Test
  public void testCompileSeqLazy() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/lazy01.i");
    assertThat(Files.exists(path)).isTrue();
    testCompile(path);
  }

  @Test
  public void testCompileSeqQueueLongest() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/queue_longest.i");
    assertThat(Files.exists(path)).isTrue();
    testCompile(path);
  }

  @Test
  public void testCompileSeqStack() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable/stack-1.i");
    assertThat(Files.exists(path)).isTrue();
    testCompile(path);
  }

  // TODO this triggers an assertion failure in SeqUtil
  @Ignore
  @Test
  public void testCompileSeqSingletonWithUninitProblems() throws Exception {
    Path path =
        Path.of("./test/programs/mpor_seq/seq_compilable/singleton_with-uninit-problems-b.i");
    assertThat(Files.exists(path)).isTrue();
    testCompile(path);
  }

  private void testCompile(Path pInputFilePath) throws Exception {
    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    CFACreator creator =
        new CFACreator(Configuration.builder().build(), logger, ShutdownNotifier.createDummy());
    String program = Files.readString(pInputFilePath);
    CFA inputCfa = creator.parseSourceAndCreateCFA(program);

    // create seq with mpor algorithm
    MPORAlgorithm algorithm = MPORAlgorithm.testInstance(logger, inputCfa, true);
    String initSeq = algorithm.buildInitSeq();
    String finalSeq = algorithm.buildFinalSeq("test.i", "__mpor_seq__test.i", initSeq);

    // test that seq can be parsed and cfa created ==> code compiles
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
