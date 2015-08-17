/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.IterationStatistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

/**
 * Summary of all (so far) feasible counterexamples
 * that can be found in the current set 'reached'.
 */
@Options
public class CounterexamplesSummary implements IterationStatistics {

  private final static class ViolationInfo {
    final CounterexampleInfo info;
    final Map<String, AutomatonInternalState> properties; // There might multiple violations on one (target) abstract state

    public ViolationInfo(CounterexampleInfo pInfo,
        Map<String, AutomatonInternalState> pProperty) {
      info = pInfo;
      properties = pProperty;
    }
  }

  private final LogManager logger;

  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final Map<ARGState, ViolationInfo> feasibleViolations = new WeakHashMap<>();
  private Multiset<AutomatonInternalState> feasibleReachedAcceptingStates = HashMultiset.create();

  public CounterexamplesSummary(Configuration pConfig, LogManager pLogger, MachineModel pMachineModel)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    this.logger = pLogger;
    this.assumptionToEdgeAllocator = new AssumptionToEdgeAllocator(pConfig, pLogger, pMachineModel);
  }

  public Map<ARGState, CounterexampleInfo> getCounterexamples() {
    return Maps.transformValues(feasibleViolations, new Function<ViolationInfo, CounterexampleInfo>() {
      @Override
      public CounterexampleInfo apply(ViolationInfo pArg0) {
        return pArg0.info;
      }
    });
  }

  public Multiset<AutomatonInternalState> getFeasibleReachedAcceptingStates() {
    return feasibleReachedAcceptingStates;
  }

  public Multiset<String> getFeasiblePropertyViolations() {
    HashMultiset<String> result = HashMultiset.create();
    for (ARGState e: feasibleViolations.keySet()) {
      ViolationInfo v = feasibleViolations.get(e);
      result.addAll(v.properties.keySet());
    }
    return result;
  }

  public void addFeasibleCounterexample(ARGState pTargetState, CounterexampleInfo pCounterexample) {
    checkArgument(pTargetState.isTarget());
    checkArgument(!pCounterexample.isSpurious());
    if (pCounterexample.getTargetPath() != null) {
      // With BAM, the targetState and the last state of the path
      // may actually be not identical.
      checkArgument(pCounterexample.getTargetPath().getLastState().isTarget());
    }

    final Map<String, AutomatonInternalState> violatedProperties = Maps.newHashMap();

    // We assume that all properties are encoded in automata!!!

    Collection<AutomatonState> qs = AbstractStates.extractStatesByType(pTargetState, AutomatonState.class);
    for (AutomatonState q: qs) {
      if (q.isTarget()) {
        // One target state can belong to different properties
        //    Example:
        //      We have an automata that matches failing assertions, i.e., __assert_fail
        //      __assert_fail appears several times in the program; each time for a different property.

        final String prop = q.getViolatedPropertyDescription();
        violatedProperties.put(prop, q.getInternalState());

        feasibleReachedAcceptingStates.add(q.getInternalState());
      }
    }

    final ViolationInfo vi = new ViolationInfo(pCounterexample, violatedProperties);
    feasibleViolations.put(pTargetState, vi);
  }

  public Map<ARGState, CounterexampleInfo> getAllCounterexamples(final ReachedSet pReached) {
    // 'counterexamples' may contain too many counterexamples
    // (for target states that were in the mean time removed from the ReachedSet),
    // as well as too few counterexamples
    // (for target states where we don't have a CounterexampleInfo
    // because we did no refinement).
    // So we create a map with all target states,
    // adding the CounterexampleInfo where we have it (null otherwise).

    Map<ARGState, CounterexampleInfo> allCexs = new HashMap<>();

    for (AbstractState targetState : from(pReached).filter(IS_TARGET_STATE)) {
      ARGState s = (ARGState)targetState;
      ViolationInfo vi = feasibleViolations.get(s);
      CounterexampleInfo cex = null;
      if (vi != null) {
        cex = vi.info;
      }
      if (cex == null) {
        ARGPath path = ARGUtils.getOnePathTo(s);
        if (path.getInnerEdges().contains(null)) {
          // path is invalid,
          // this might be a partial path in BAM, from an intermediate TargetState to root of its ReachedSet.
          // TODO this check does not avoid dummy-paths in BAM, that might exist in main-reachedSet.
        } else {

          RichModel model = createModelForPath(path);
          cex = CounterexampleInfo.feasible(path, model);
        }
      }

      if (cex != null) {
        allCexs.put(s, cex);
      }
    }

    return allCexs;
  }

  private RichModel createModelForPath(ARGPath pPath) {
    final ConfigurableProgramAnalysis cpa = GlobalInfo.getInstance().getCPA().get();

    FluentIterable<ConfigurableProgramAnalysisWithConcreteCex> cpas =
        CPAs.asIterable(cpa).filter(ConfigurableProgramAnalysisWithConcreteCex.class);

    CFAPathWithAssumptions result = null;

    // TODO Merge different paths
    for (ConfigurableProgramAnalysisWithConcreteCex wrappedCpa : cpas) {
      ConcreteStatePath path = wrappedCpa.createConcreteStatePath(pPath);
      CFAPathWithAssumptions cexPath = CFAPathWithAssumptions.of(path, assumptionToEdgeAllocator);

      if (result != null) {
        result = result.mergePaths(cexPath);
      } else {
        result = cexPath;
      }
    }

    if(result == null) {
      return RichModel.empty();
    } else {
      return RichModel.empty().withAssignmentInformation(result);
    }
  }

  public void removeInfeasibleState(Set<ARGState> toRemove) {
    feasibleViolations.keySet().removeAll(toRemove);
  }

  public void clearCounterexamples(Set<ARGState> toRemove) {
    // Actually the goal would be that this method is not necessary
    // because the GC automatically removes counterexamples when the ARGState
    // is removed from the ReachedSet.
    // However, counterexamples may reference their target state through
    // the target path attribute, so the GC may not remove the counterexample.
    // While this is not a problem for correctness
    // (we check in the end which counterexamples are still valid),
    // it may be a memory leak.
    // Thus this method.

    feasibleViolations.keySet().removeAll(toRemove);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    final int cols = 40;

    // Determine the observing automata
    Map<String, Automaton> observingAutomata = Maps.newHashMap();
    int transitionsToTargetStatesCount = 0;
    {
      AbstractState initial = pReached.getFirstState();
      Collection<AutomatonState> automataComponents = AbstractStates.extractStatesByType(initial, AutomatonState.class);
      for (AutomatonState e: automataComponents) {
        // An automata can have multiple target states!
        //  And: An automata might be parametric...
        if (e.getOwningAutomaton().getIsObservingOnly()) {
          observingAutomata.put(e.getOwningAutomatonName(), e.getOwningAutomaton());
          transitionsToTargetStatesCount += e.getOwningAutomaton().getTransitionsToTargetStatesCount();
        }
      }
    }

    // Determine the violated properties
    HashMultimap<String, ARGState> violatedProps = HashMultimap.create();
    for (ARGState e: feasibleViolations.keySet()) {
      ViolationInfo v = feasibleViolations.get(e);
      for (String p: v.properties.keySet()) {
        violatedProps.put(p, e);
      }
    }

    // Write the statistics!!
    StatisticsUtils.write(pOut, 0, cols,
        "Observing property automata",
        observingAutomata.size());

    StatisticsUtils.write(pOut, 0, cols,
        "Target state edges",
        transitionsToTargetStatesCount);

    StatisticsUtils.write(pOut, 0, cols,
        "Violated (distinct) properties",
        violatedProps.keySet().size());

    for (String prop: violatedProps.keySet()) {
      StatisticsUtils.write(pOut, 1, cols,
          prop, "");
      StatisticsUtils.write(pOut, 2, cols,
          "Violating ARG states", violatedProps.get(prop).size());
    }
  }

  @Override
  public String getName() {
    return "Counterexamples";
  }

  @Override
  public void printIterationStatistics(PrintStream pOut, ReachedSet pReached) {

  }


}
