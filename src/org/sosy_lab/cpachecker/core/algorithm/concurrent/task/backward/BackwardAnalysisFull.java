// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward;

import static java.lang.Math.max;
import static org.sosy_lab.cpachecker.core.AnalysisDirection.BACKWARD;
import static org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus.SOUND_AND_PRECISE;
import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.makeName;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ConfigurationLoader;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BackwardAnalysisFull extends Task {
  private static volatile ConfigurationLoader configLoader = null;

  private final Block target;
  private final ErrorOrigin origin;
  private final CFANode start;
  private final PathFormula errorCondition;
  private final PathFormula blockSummary;
  private final PathFormulaManager pfMgr;
  private final ReachedSet reached;
  private final Algorithm algorithm;
  private final ARGCPA argcpa;
  private final Solver solver;
  private final FormulaManagerView fMgr;
  private final AlgorithmStatus status = SOUND_AND_PRECISE;
  private final BackwardAnalysisFullStatistics statistics;
  
  public BackwardAnalysisFull(
      final Block pBlock,
      final ErrorOrigin pOrigin,
      final CFANode pStart,
      final ShareableBooleanFormula pErrorCondition,
      final ShareableBooleanFormula pBlockSummary,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final ARGCPA pARGCPA,
      final MessageFactory pMessageFactory,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    super(pMessageFactory, pLogManager, pShutdownNotifier);

    argcpa = pARGCPA;
    PredicateCPA predicateCPA = argcpa.retrieveWrappedCpa(PredicateCPA.class);

    pfMgr = predicateCPA.getPathFormulaManager();
    solver = predicateCPA.getSolver();
    fMgr = solver.getFormulaManager();

    target = pBlock;
    start = pStart;
    origin = pOrigin;
    errorCondition = pErrorCondition.getFor(fMgr, pfMgr);
    blockSummary = pBlockSummary.getFor(fMgr, pfMgr);
    statistics = new BackwardAnalysisFullStatistics();
    
    reached = pReachedSet;
    algorithm = pAlgorithm;
  }

  public static Configuration getConfiguration(
      final LogManager pLogManager,
      final Path pConfigFile, final Configuration pAnalysisConfiguration)
      throws InvalidConfigurationException {
    if (configLoader == null) {
      synchronized (BackwardAnalysisFull.class) {
        if (configLoader == null) {
          configLoader =
              new ConfigurationLoader(pAnalysisConfiguration, BackwardAnalysisFull.class,
                  "predicateBackward.properties",
                  pConfigFile, pLogManager);
        }
      }
    }
    return configLoader.getConfiguration();
  }

  private ARGState buildEntryState() throws InterruptedException {
    PredicateAbstractState predicateEntryState = buildPredicateEntryState();

    assert argcpa.getWrappedCPAs().size() == 1 && argcpa.getWrappedCPAs().get(0) instanceof BlockAwareCompositeCPA;
    BlockAwareCompositeCPA blockAwareCPA = (BlockAwareCompositeCPA) argcpa.getWrappedCPAs().get(0);
        
    List<AbstractState> componentStates = new ArrayList<>();
    for (ConfigurableProgramAnalysis componentCPA : blockAwareCPA.getWrappedCPAs()) {
      AbstractState componentState = null;
      if (componentCPA instanceof PredicateCPA) {
        componentState = predicateEntryState;
      } else {
        while (componentState == null) {
          try {
            componentState = componentCPA.getInitialState(start, getDefaultPartition());
          } catch (InterruptedException ignored) {
            shutdownNotifier.shutdownIfNecessary();
          }
        }
      }
      componentStates.add(componentState);
    }

    return BlockAwareCompositeState.createAndWrap(new CompositeState(componentStates), target, BACKWARD);
  }

  private PredicateAbstractState buildPredicateEntryState() {
    PredicateCPA predicateCPA = argcpa.retrieveWrappedCpa(PredicateCPA.class);
    
    return PredicateAbstractState.mkAbstractionState(
        errorCondition,
        predicateCPA.getPredicateManager().makeTrueAbstractionFormula(errorCondition),
        PathCopyingPersistentTreeMap.of());
  }

  /**
   * Todo: Merge Pointer Target Sets
   */
  private PathFormula stitchIndicesTogether(final PathFormula lower, final PathFormula upper) {
    BooleanFormula targetRaw = upper.getFormula();

    SSAMap lowerSSA = lower.getSsa();
    SSAMap upperSSA = upper.getSsa();

    Map<String, String> replacements = new HashMap<>();
    SSAMapBuilder newSSABuilder = upperSSA.builder();

    for (final String name : lowerSSA.allVariables()) {
      int maxLowerIndex = lowerSSA.getIndex(name);

      if (upperSSA.containsVariable(name)) {
        int maxUpperIndex = upperSSA.getIndex(name);
        int maxIndex = max(maxLowerIndex, maxUpperIndex);

        replacements.put(makeName(name, maxUpperIndex), makeName(name, maxLowerIndex));

        for (int index = 1; index < maxUpperIndex; ++index) {
          replacements.put(makeName(name, index), makeName(name, maxIndex + index));
        }

        final CType type = upperSSA.getType(name);
        newSSABuilder = newSSABuilder.setIndex(name, type, maxLowerIndex + maxUpperIndex);
      }
    }

    if (replacements.isEmpty()) {
      return upper;
    }

    targetRaw = fMgr.renameFreeVariablesAndUFs(targetRaw,
        (pName) -> replacements.getOrDefault(pName, pName));
    SSAMap newSSA = newSSABuilder.build();

    PathFormula result = pfMgr.makeEmptyPathFormula();
    return result.withFormula(targetRaw).withContext(newSSA, upper.getPointerTargetSet());
  }

  @Override
  protected void execute() throws Exception {
    PathFormula condition = stitchIndicesTogether(blockSummary, errorCondition);
    BooleanFormula reachable = fMgr.makeAnd(blockSummary.getFormula(), condition.getFormula());
    if (solver.isUnsat(reachable)) {
      logManager.log(Level.INFO, "Verdict: Swallowed error condition: ",
          errorCondition.getFormula());
      messageFactory.sendTaskCompletedMessage(this, status, statistics);
      return;
    }

    ARGState entryState = buildEntryState();
    Precision precision = argcpa.getInitialPrecision(start, getDefaultPartition());
    reached.add(entryState, precision);

    shutdownNotifier.shutdownIfNecessary();
    new BackwardAnalysisCore(target, reached, origin, algorithm, argcpa,
        solver, messageFactory,
        logManager,
        shutdownNotifier).run();
  }

  @Override public String toString() {
    return "BackwardAnalysisFull on block with entry location " + target.getEntry();
  }
}
