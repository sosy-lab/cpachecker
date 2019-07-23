/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import java.util.Map;
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

  protected final Map<Loop, AtomicInteger> lassosPerLoop = Maps.newConcurrentMap();

  protected final AtomicInteger maxLassosPerIteration = new AtomicInteger();

  protected final AtomicInteger lassosCurrentIteration = new AtomicInteger();

  protected final Multimap<Loop, TerminationArgument> terminationArguments =
      MultimapBuilder.linkedHashKeys().arrayListValues().build();

  protected final Map<Loop, NonTerminationArgument> nonTerminationArguments =
      Maps.newConcurrentMap();

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
    lassosPerLoop.computeIfAbsent(pLoop, l -> new AtomicInteger()).addAndGet(numberOfLassos);
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
