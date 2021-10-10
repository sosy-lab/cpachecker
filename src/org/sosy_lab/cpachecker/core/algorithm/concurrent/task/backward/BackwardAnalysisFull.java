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
import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.makeName;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ConfigurationLoader;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
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
  private final BlockAwareCompositeCPA cpa;
  private final Solver solver;
  private final FormulaManagerView fMgr;

  public BackwardAnalysisFull(
      final Block pBlock,
      final ErrorOrigin pOrigin,
      final CFANode pStart,
      final ShareableBooleanFormula pErrorCondition,
      final ShareableBooleanFormula pBlockSummary,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final BlockAwareCompositeCPA pCPA,
      final MessageFactory pMessageFactory,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    super(pMessageFactory, pLogManager, pShutdownNotifier);
    
    cpa = pCPA;
    PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    assert predicateCPA != null;
    pfMgr = predicateCPA.getPathFormulaManager();
    solver = predicateCPA.getSolver();
    fMgr = solver.getFormulaManager();

    target = pBlock;
    start = pStart;
    origin = pOrigin;
    errorCondition = pErrorCondition.getFor(fMgr, pfMgr);
    blockSummary = pBlockSummary.getFor(fMgr, pfMgr);

    reached = pReachedSet;
    algorithm = pAlgorithm;
  }

  public static Configuration getConfiguration(
      final LogManager pLogManager,
      @Nullable final Path pConfigFile) throws InvalidConfigurationException {
    if (configLoader == null) {
      synchronized (BackwardAnalysisFull.class) {
        if (configLoader == null) {
          configLoader =
              new ConfigurationLoader(BackwardAnalysisFull.class, "predicateBackward.properties",
                  pConfigFile, pLogManager);
        }
      }
    }
    return configLoader.getConfiguration();
  }

  private BlockAwareCompositeState buildEntryState() throws InterruptedException {
    PredicateAbstractState predicateEntryState = buildPredicateEntryState();

    List<AbstractState> componentStates = new ArrayList<>();
    for (ConfigurableProgramAnalysis componentCPA : cpa.getWrappedCPAs()) {
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

    return BlockAwareCompositeState.create(new CompositeState(componentStates), target, BACKWARD);
  }

  private PredicateAbstractState buildPredicateEntryState() throws InterruptedException {
    PredicateAbstractState rawPredicateState = getRawPredicateEntryState();
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        errorCondition, rawPredicateState);
  }

  private PredicateAbstractState getRawPredicateEntryState() throws InterruptedException {
    AbstractState rawInitialState = null;
    while (rawInitialState == null) {
      try {
        rawInitialState = cpa.getInitialState(start, getDefaultPartition());
      } catch (InterruptedException ignored) {
        shutdownNotifier.shutdownIfNecessary();
      }
    }

    PredicateAbstractState rawPredicateState =
        extractStateByType(rawInitialState, PredicateAbstractState.class);
    assert rawPredicateState != null;

    return rawPredicateState;
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
      messageFactory.sendTaskCompletionMessage(this);
      return;
    }

    BlockAwareCompositeState entryState = buildEntryState();
    Precision precision = cpa.getInitialPrecision(start, getDefaultPartition());
    reached.add(entryState, precision);

    shutdownNotifier.shutdownIfNecessary();
    new BackwardAnalysisCore(target, reached, origin, algorithm, cpa, solver, messageFactory, logManager,
        shutdownNotifier).run();
  }
}
