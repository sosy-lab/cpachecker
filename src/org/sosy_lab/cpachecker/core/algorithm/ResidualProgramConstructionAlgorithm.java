/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator;

@Options(prefix="residualprogram")
public class ResidualProgramConstructionAlgorithm implements Algorithm, StatisticsProvider {

  @Option(secure=true, name="slice", description="write collected assumptions as automaton to file")
  private boolean doSlicing = true;

  @Option(secure=true, name="file", description="write residual program to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path residualProgram = Paths.get("residualProgram.c");

  @Option(secure = true, name = "assumptionGuider",
      description = "set specification file to automaton which guide analysis along assumption produced by incomplete analysis,e.g., config/specification/AssumptionGuidingAutomaton.spc, to enable residual program from combination of program and assumption condition")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path conditionSpec = null;

  private final CFA cfa;
  private final Algorithm innerAlgorithm;
  private final LogManager logger;
  private final ShutdownNotifier shutdown;

  private final ARGToCTranslator translator;
  private final Collection<Statistics> stats = new ArrayList<>();

  public ResidualProgramConstructionAlgorithm(final CFA pCfa, final Algorithm pAlgorithm,
      final Configuration pConfig, final LogManager pLogger, final ShutdownNotifier pShutdown)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    cfa = pCfa;
    innerAlgorithm = pAlgorithm;
    logger = pLogger;
    shutdown = pShutdown;
    translator = new ARGToCTranslator(logger, pConfig);

    if(innerAlgorithm instanceof StatisticsProvider){
      ((StatisticsProvider)innerAlgorithm).collectStatistics(stats);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    Preconditions.checkArgument(pReachedSet.getFirstState() instanceof ARGState,
        "Top most abstract state must be an ARG state");
    Preconditions.checkArgument(AbstractStates.extractLocation(pReachedSet.getFirstState()) != null,
        "Require location information to build residual program");

    AlgorithmStatus status= AlgorithmStatus.SOUND_AND_PRECISE;

    try {
      logger.log(Level.INFO, "Start analysis");
      status = status.update(innerAlgorithm.run(pReachedSet));
    } catch (InfeasibleCounterexampleException | RefinementFailedException e) {
    }

    if (!pReachedSet.hasWaitingState()) {
      logger.log(Level.INFO, "Analysis complete");
      // analysis alone succeeded
      return status;
    }

    logger.log(Level.INFO, "Analysis incomplete, some states could not be explored. Generate residual program.");
    ARGState argRoot = (ARGState) pReachedSet.getFirstState();

    CFANode mainFunction = AbstractStates.extractLocation(argRoot);
    assert(mainFunction != null);

    Path assumptionAutomaton = null;

    if (conditionSpec != null) {
      try {
        assumptionAutomaton = MoreFiles.createTempFile("assumptions", "txt", null);

        try (Writer automatonWriter =
            MoreFiles.openOutputFile(assumptionAutomaton, Charset.defaultCharset())) {
          AssumptionCollectorAlgorithm.writeAutomaton(automatonWriter, argRoot,
              computeRelevantStates(pReachedSet.getWaitlist()),
              Sets.newHashSet(pReachedSet.getWaitlist()), 0, true);
        }
      } catch (IOException e1) {
        throw new CPAException("Could not generate assumption automaton needed to generate residual program" ,e1);
      }
    }

    argRoot = prepareARGToConstructResidualProgram(mainFunction, assumptionAutomaton);

    if(argRoot == null) {
      throw new CPAException("Failed to build structure of residual program");
    }

    boolean useCombination = isCombination(argRoot);

    if (doSlicing) {
      Set<CFANode> addPragma;
      if (useCombination) {
        addPragma = getAllCFANodesAfterAssertFail();
      } else {
        addPragma = getAllCFANodesAfterAssertFailNotFullyExplored(
            extractLocations(pReachedSet.getWaitlist()));
      }
      writeResidualProgram(argRoot, addPragma);
    } else {
      writeResidualProgram(argRoot, null);
    }

    return status;
  }


  private Set<ARGState> computeRelevantStates(final Collection<AbstractState> pWaitlist) {
    TreeSet<ARGState> uncoveredAncestors = new TreeSet<>();
    Deque<ARGState> toAdd = new ArrayDeque<>();

    for (AbstractState unexplored : pWaitlist) {
      toAdd.push((ARGState) unexplored);
    }

    while (!toAdd.isEmpty()) {
      ARGState current = toAdd.pop();
      assert !current.isCovered();

      if (uncoveredAncestors.add(current)) {
        // current was not yet contained in parentSet,
        // so we need to handle its parents

        toAdd.addAll(current.getParents());

        for (ARGState coveredByCurrent : current.getCoveredByThis()) {
          toAdd.addAll(coveredByCurrent.getParents());
        }
      }
    }
    return uncoveredAncestors;
  }

  private boolean isCombination(final AbstractState pState) {
    CompositeState compState = AbstractStates.extractStateByType(pState, CompositeState.class);
    if (compState != null) {
      for (AbstractState innerState : compState.getWrappedStates()) {
        if (innerState instanceof AutomatonState
            && ((AutomatonState) innerState).getOwningAutomatonName()
                .equals("AssumptionGuidingAutomaton")) { return true; }
      }
    }
    return false;
  }

  private Set<CFANode> getAllCFANodesAfterAssertFail() {
    return Sets.newHashSet(Iterables.filter(cfa.getAllNodes(), node -> CFAUtils.enteringEdges(node)
        .anyMatch(edge -> isAssertFail(edge))));
  }

  private Set<CFANode> getAllCFANodesAfterAssertFailNotFullyExplored(final Set<CFANode> startNodes) {
    // overapproximating this set, considering all syntactical paths
    Set<CFANode> result = Sets.newHashSetWithExpectedSize(cfa.getAllNodes().size());

    Set<CFANode> seen = Sets.newHashSetWithExpectedSize(cfa.getAllNodes().size());
    Deque<CFANode> toProcess = new ArrayDeque<>();
    CFANode current;

    for (CFANode node : startNodes) {
      if (seen.add(node)) {
        toProcess.add(node);
      }
    }

    while (!toProcess.isEmpty()) {
      current = toProcess.pop();

      for (CFAEdge leaving : CFAUtils.leavingEdges(current)) {
        if (isAssertFail(leaving)) {
          result.add(leaving.getSuccessor());
        }
        if (seen.add(leaving.getSuccessor())) {
          toProcess.push(leaving.getSuccessor());
        }
      }
    }

    return result;
  }

  private Set<CFANode> extractLocations(final Collection<AbstractState> pStates) {
    Set<CFANode> result = Sets.newHashSetWithExpectedSize(pStates.size());
    for (AbstractState state : pStates) {
      result.add(AbstractStates.extractLocation(state));
    }
    return result;
  }

  private boolean isAssertFail(final CFAEdge pEdge) {
    return pEdge.getRawStatement().startsWith("__assert_fail");
  }

  private void writeResidualProgram(final ARGState pArgRoot, final Set<CFANode> pAddPragma) {
    logger.log(Level.INFO, "Generate residual program");
    try (Writer writer = MoreFiles.openOutputFile(residualProgram, Charset.defaultCharset())) {
      writer.write(translator.translateARG(pArgRoot, pAddPragma));
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write residual program to file");
    }
  }

  private @Nullable ARGState prepareARGToConstructResidualProgram(final CFANode mainFunction,
      final @Nullable Path assumptionAutomaton) {
    try {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.setOption("cpa", "cpa.arg.ARGCPA");
      configBuilder.setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA");
      configBuilder.setOption("CompositeCPA.cpas",
          "cpa.location.LocationCPA,cpa.callstack.CallstackCPA");
      Configuration config = configBuilder.build();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(config, logger, shutdown, new AggregatedReachedSets());

      Specification spec = Specification.alwaysSatisfied();
      if (conditionSpec != null) {
        assert (assumptionAutomaton != null);
        spec = Specification.fromFiles(Collections.emptySet(),
            ImmutableList.of(conditionSpec, assumptionAutomaton), cfa,
            config, logger);
      }
      ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, spec);

      ReachedSet reached = coreComponents.createReachedSet();
      reached.add(cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition()));

      Algorithm algo = CPAAlgorithm.create(cpa, logger, config, shutdown);
      algo.run(reached);

      if (reached.hasWaitingState()) {
        logger.log(Level.SEVERE, "Location analysis run to get structure of residual program is incomplete");
        return null;
      }

      return (ARGState) reached.getFirstState();
    } catch (InvalidConfigurationException | CPAException | IllegalArgumentException
        | InterruptedException e1) {
      logger.logException(Level.SEVERE, e1, "Analysis to build structure of residual program failed");
      return null;
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.addAll(stats);
  }
}
