/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.core.MainCPAStatistics;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult.CounterexampleInfoResult;
import org.sosy_lab.cpachecker.core.algorithm.mpa.MultiPropertyAnalysisFullReset;
import org.sosy_lab.cpachecker.core.algorithm.mpa.TargetSummary;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.algorithm.tgar.TGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.tgar.TGARStatistics;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;
import org.sosy_lab.cpachecker.cpa.automaton.MarkingAutomatonBuilder;
import org.sosy_lab.cpachecker.cpa.automaton.SafetyProperty;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.InterruptProvider;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

@Options(prefix = "analysis.tigerdc")
public class TigerDcAlgorithm extends MultiPropertyAnalysisFullReset{

  private final TGARStatistics tgarStatistics;
  private final TigerConfiguration cfg;
  private final TestGeneration tg;

  @Option(secure = true, description = "test")
  boolean test = false;

  private Map<Automaton, Automaton> markingAutomataInstances = Maps.newHashMap();

  public TigerDcAlgorithm(Configuration pConfig, LogManager pLogger, ShutdownNotifier pGlobalShutdownNotifier,
      InterruptProvider pShutdownNotifier, CFA pCfa, String pProgramDenotation, MainCPAStatistics pStats)
    throws InvalidConfigurationException, CPAException {
    super(pConfig, pLogger, pGlobalShutdownNotifier, pShutdownNotifier, pCfa, pProgramDenotation, pStats);

    tgarStatistics = new TGARStatistics(pLogger);
    cfg = new TigerConfiguration(pConfig);
    tg = new TestGeneration(cfg, pCfa, pLogger);

  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Running TigerDC with " + partitionOperator.getClass().getSimpleName());

    final AlgorithmStatus result = super.run(pReachedSet);

    // Write generated test suite and mapping to file system
    tg.dumpTestSuite();

    return result;
  }

  @Override
  protected TargetSummary identifyViolationsInRun(Algorithm pAlgorithm, ReachedSet pReached)
      throws InterruptedException {

    if (pAlgorithm instanceof TGARAlgorithm) {
      TGARAlgorithm tgar = (TGARAlgorithm) pAlgorithm;
      AlgorithmResult tgarResult = tgar.getResult();
      Preconditions.checkState(tgarResult instanceof CounterexampleInfoResult);
      CounterexampleInfoResult cexInfo = (CounterexampleInfoResult) tgarResult;

      if (!cexInfo.getCounterexampleInfo().isPresent()) {
        return TargetSummary.none();
      } else {
        //
        // TEST GENERATION!!!! TEST GENERATION!!!! TEST GENERATION!!!! TEST GENERATION!!!!
        //
        ARGState lastState = cexInfo.getCounterexampleInfo().get().getTargetPath().getLastState();
        Set<SafetyProperty> violatedAtLastState = Sets.newLinkedHashSet(Iterables.filter(lastState.getViolatedProperties(), SafetyProperty.class));
        Map<SafetyProperty, Optional<PresenceCondition>>  covered = tg.feasibleCounterexample(cexInfo.getCounterexampleInfo().get());

        return TargetSummary.of(logger, covered);
      }
    } else {
      return super.identifyViolationsInRun(pAlgorithm, pReached);
    }
  }

  @Override
  protected ImmutableSet<Property> getFullSetOfPropertiesToCheck(ReachedSet pReachedSet) {
    return ImmutableSet.copyOf(Iterables.filter(tg.getTestSuite().getGoals(), Property.class));
  }

  @Nullable
  @Override
  protected List<Automaton> getAutomataFor(Set<Property> pProperties) {
    Set<Goal> goals = Sets.newLinkedHashSet(Iterables.filter(pProperties, Goal.class));

    List<Automaton> componentAutomata = Lists.newArrayList();
    {
      List<Automaton> goalAutomata = Lists.newArrayList();

      for (Goal goal : goals) {
        Automaton a = goal.createControlAutomaton();
        if (cfg.useMarkingAutomata) {
          final Automaton markingAutomata;
          if (markingAutomataInstances.containsKey(a)) {
            markingAutomata = markingAutomataInstances.get(a);
          } else {
            markingAutomata = MarkingAutomatonBuilder.build(a);
            markingAutomataInstances.put(a, markingAutomata);
          }
          a = markingAutomata;
        }

        goalAutomata.add(a);
        dumpAutomaton(a);
        checkAutomaton(a);
      }

      componentAutomata.addAll(goalAutomata);
    }

    tg.setActiveGoalSet(goals);

    return componentAutomata;
  }

  private void dumpAutomaton(Automaton pA) {
    if (cfg.dumpGoalAutomataTo == null) {
      return;
    }

    try (Writer w = MoreFiles.openOutputFile(cfg.dumpGoalAutomataTo.getPath(pA.getName()), Charset.defaultCharset())) {
      pA.writeDotFile(w);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
    }
  }

  /**
   * Check some properties of the automaton to ensure that it works as expected.
   *
   * @param pAutomaton
   */
  private static void checkAutomaton(Automaton pAutomaton) {
    for (AutomatonInternalState q: pAutomaton.getStates()) {
      if (!q.isNonDetState()) {
        Set<Pair<AutomatonBoolExpr, ImmutableList<AStatement>>> distinct = Sets.newHashSet();
        // No similar triggers!
        for (AutomatonTransition t: q.getTransitions()) {
          Pair<AutomatonBoolExpr, ImmutableList<AStatement>> key = Pair.of(t.getTrigger(), t.getAssumptions());
          if (!distinct.add(key)) {
            throw new RuntimeException("Transition not unique on MATCH-FIRST state: " + t);
          }
        }
      }
    }
  }

  @Override
  protected Partitioning initAnalysisAndReached(
      final Partitioning pCheckPartitions, Set<Property> pAllProperties, Set<Property> pRemain)
      throws CPAException, InterruptedException {

    final Partitioning result = super.initAnalysisAndReached(pCheckPartitions, pAllProperties, pRemain);

    Preconditions.checkState(partitionAnalysis.getAlgorithm() instanceof TGARAlgorithm, "Please configure 'algorithm.TGAR=true'!");

    TGARAlgorithm tgar = (TGARAlgorithm) partitionAnalysis.getAlgorithm();
    tgar.setStats(tgarStatistics);

    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(tgarStatistics);
  }

  public AlgorithmResult getResult() {
    return tg.getTestSuite();
  }

}
