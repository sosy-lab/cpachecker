// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multiset;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public abstract class LassoAnalysisStatistics implements Statistics {

  protected final Timer lassoTime = new Timer();

  protected final Timer lassoConstructionTime = new Timer();

  protected final Timer lassoNonTerminationTime = new Timer();

  protected final Timer lassoTerminationTime = new Timer();

  protected final Timer lassoStemLoopConstructionTime = new Timer();

  protected final Timer lassosCreationTime = new Timer();

  protected final Multiset<Loop> lassosPerLoop = ConcurrentHashMultiset.create();

  protected final AtomicInteger maxLassosPerIteration = new AtomicInteger();

  protected final AtomicInteger lassosCurrentIteration = new AtomicInteger();

  protected final Multimap<Loop, TerminationArgument> terminationArguments =
      MultimapBuilder.linkedHashKeys().arrayListValues().build();

  protected final Map<Loop, NonTerminationArgument> nonTerminationArguments =
      new ConcurrentHashMap<>();

  protected LassoAnalysisStatistics() {}

  public void analysisOfLassosStarted() {
    lassoTime.start();
  }

  public void analysisOfLassosFinished() {
    lassoTime.stop();
    lassoConstructionTime.stopIfRunning();
    lassoNonTerminationTime.stopIfRunning();
    lassoTerminationTime.stopIfRunning();
    maxLassosPerIteration.accumulateAndGet(lassosCurrentIteration.getAndSet(0), Math::max);
  }

  public void lassoConstructionStarted() {
    lassoConstructionTime.start();
  }

  public void lassoConstructionFinished() {
    lassoConstructionTime.stop();
  }

  public void lassosConstructed(Loop pLoop, int numberOfLassos) {
    lassosPerLoop.add(pLoop, numberOfLassos);
    lassosCurrentIteration.addAndGet(numberOfLassos);
  }

  public void nonTerminationAnalysisOfLassoStarted() {
    lassoNonTerminationTime.start();
  }

  public void nonTerminationAnalysisOfLassoFinished() {
    lassoNonTerminationTime.stop();
  }

  public void terminationAnalysisOfLassoStarted() {
    lassoTerminationTime.start();
  }

  public void terminationAnalysisOfLassoFinished() {
    lassoTerminationTime.stop();
  }

  public void synthesizedNonTerminationArgument(
      Loop pLoop, NonTerminationArgument pNonTerminationArgument) {
    nonTerminationArguments.put(pLoop, pNonTerminationArgument);
  }

  protected void synthesizedTerminationArgument(
      Loop pLoop, TerminationArgument pTerminationArgument) {
    terminationArguments.put(pLoop, pTerminationArgument);
  }

  public void stemAndLoopConstructionStarted() {
    lassoStemLoopConstructionTime.start();
  }

  public void stemAndLoopConstructionFinished() {
    lassoStemLoopConstructionTime.stop();
  }

  public void lassosCreationStarted() {
    lassosCreationTime.start();
  }

  public void lassosCreationFinished() {
    lassosCreationTime.stop();
  }
}
