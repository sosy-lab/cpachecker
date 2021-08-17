// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

@Options(prefix = "concurrent.task.forward")
public class ForwardAnalysis implements Task {
  // Todo: Load in factory and reuse across instances
  private static volatile Configuration forward = null;

  private final Block block;

  private final ReachedSet reached;

  private final TaskFactory taskFactory;

  private final LogManager logManager;

  private final FormulaManagerView formulaManager;

  private final PathFormulaManager pathFormulaManager;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(description = "Forward Config")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configFile = null;

  // Todo: Create in factory and reuse across instances
  private Algorithm algorithm = null;

  // Todo: Create in factory and reuse across instances
  private BlockAwareCompositeCPA cpa = null;

  public ForwardAnalysis(
      final Block pBlock,
      @Nullable final ShareableBooleanFormula pPrecondition,
      final Configuration pConfig,
      final Specification pSpecification,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA,
      final TaskFactory pTaskFactory)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    pConfig.inject(this);
    loadForwardConfig();

    block = pBlock;
    taskFactory = pTaskFactory;
    logManager = pLogger;

    CoreComponentsFactory factory =
        new CoreComponentsFactory(
            forward, logManager, pShutdownNotifier, new AggregatedReachedSets());
    reached = factory.createReachedSet();

    CompositeCPA compositeCpa = (CompositeCPA) factory.createCPA(pCFA, pSpecification);

    if (compositeCpa.retrieveWrappedCpa(PredicateCPA.class) == null) {
      throw new InvalidConfigurationException(
          "Forward analysis requires a composite CPA with predicateCPA as component CPA.");
    }
    if (compositeCpa.retrieveWrappedCpa(LocationCPA.class) == null) {
      throw new InvalidConfigurationException(
          "Forward analysis requires a composite CPA with locationCPA as component CPA.");
    }

    cpa =
        (BlockAwareCompositeCPA)
            BlockAwareCompositeCPA.factory()
                .setConfiguration(forward)
                .setLogger(pLogger)
                .setShutdownNotifier(pShutdownNotifier)
                .set(pCFA, CFA.class)
                .set(block, Block.class)
                .set(compositeCpa, CompositeCPA.class)
                .createInstance();

    algorithm = new CPAAlgorithmFactory(cpa, pLogger, forward, pShutdownNotifier).newInstance();

    final CFANode blockEntry = block.getEntry();

    PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    assert predicateCPA != null;
    formulaManager = predicateCPA.getSolver().getFormulaManager();
    pathFormulaManager = predicateCPA.getPathFormulaManager();

    Precision precision =
        ((ConfigurableProgramAnalysis) cpa).getInitialPrecision(blockEntry, getDefaultPartition());

    AbstractState state = buildEntryState(blockEntry, pPrecondition);
    reached.add(state, precision);
  }

  private void loadForwardConfig() throws InvalidConfigurationException {
    if (forward == null) { // Todo: no double-checked locking
      synchronized (ForwardAnalysis.class) {
        if (forward == null) {
          if (configFile != null) {
            try {
              forward = Configuration.builder().loadFromFile(configFile).build();
            } catch (IOException ignored) {
              final String message =
                  "Failed to load file " + configFile + ". " + "Using default configuration.";
              logManager.log(Level.SEVERE, message);
            }
          }

          if (forward == null) {
            forward =
                Configuration.builder()
                    .loadFromResource(ForwardAnalysis.class, "predicateForward.properties")
                    .build();
          }
        }
      }
    }
  }

  private AbstractState buildEntryState(
      final CFANode pNode, @Nullable final ShareableBooleanFormula pContext) {
    AbstractState rawInitialState = null;
    while (rawInitialState == null) {
      try {
        rawInitialState = cpa.getInitialState(pNode, getDefaultPartition());
      } catch (InterruptedException ignored) {
        /*
         * If the thread gets interrupted while waiting to obtain the initial state,
         * 'rawInterruptedState' remains 'null' and the operation is tried again.
         * TODO: Check for shutdown request.
         */
      }
    }

    PredicateAbstractState rawPredicateState =
        AbstractStates.extractStateByType(rawInitialState, PredicateAbstractState.class);
    assert rawPredicateState != null;

    BooleanFormulaManager booleanFormulaManager = formulaManager.getBooleanFormulaManager();

    BooleanFormula contextFormula;
    if (pContext == null) {
      contextFormula = booleanFormulaManager.makeTrue();
    } else {
      contextFormula = pContext.getFor(formulaManager);
    }

    PathFormula context = rawPredicateState.getPathFormula().withFormula(contextFormula);

    PredicateAbstractState predicateState =
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(context, rawPredicateState);

    List<AbstractState> componentStates = new ArrayList<>();
    for (ConfigurableProgramAnalysis componentCPA : cpa.getWrappedCPAs()) {
      AbstractState componentState = null;
      if (componentCPA instanceof PredicateCPA) {
        componentState = predicateState;
      } else {
        while (componentState == null) {
          try {
            componentState = componentCPA.getInitialState(pNode, getDefaultPartition());
          } catch (InterruptedException ignored) {
            /*
             * If the task gets interrupted while waiting for the initial state, 'componentState'
             * remains 'null' and the task tries to obtain the initial state again.
             */
          }
        }
      }
      componentStates.add(componentState);
    }

    return new CompositeState(componentStates);
  }

  @Override
  public AlgorithmStatus call()
      throws CPAException, InterruptedException, InvalidConfigurationException {
    logManager.log(Level.INFO, "Starting ForwardAnalysis on ", block);

    AlgorithmStatus status = algorithm.run(reached);

    // for(final Entry<CFANode, Block> exit : block.getExits().entrySet()) {
    // Collection<AbstractState> states = reached.getReached(exit.getKey());

    /*
     * UnmodifableReachedSet#getReached(CFANode location) returns all abstract states in reached.
     * Therefore, its required to iterate through them. TODO: Further explanation
     * If only the actual state belonging to exit would get returned, no iteration would be
     * required.
     * With this situation, a different control structure would be beneficial:
     * The code could loop through all states in the reached set, and only check whether they
     * belong to an exit location.
     * However, getReached always requires a CFANode as input, and the interface does not
     * guarantee it to return all abstract states (as it does for the actual ReachedSet
     * implementation found here). Therefore, the current iteration with two loops (where the
     * outer one iterates through block exits, and the inner one through states) remains
     * mandatory.
     */
    for (final AbstractState state : reached.asCollection()) {
      if (AbstractStates.isTargetState(state)) {
        logManager.log(Level.FINE, "! Target State:", state);
      }

      LocationState location = AbstractStates.extractStateByType(state, LocationState.class);
      assert location != null;

      if (block.getExits().containsKey(location.getLocationNode())) {
        PredicateAbstractState predicateState =
            AbstractStates.extractStateByType(state, PredicateAbstractState.class);
        assert predicateState != null;

        BooleanFormula exitFormula = predicateState.getPathFormula().getFormula();

        Block exit = block.getExits().get(location.getLocationNode());
        final var shareableFormula = new ShareableBooleanFormula(formulaManager, exitFormula);
        Task next = taskFactory.createForwardAnalysis(exit, shareableFormula);
        taskFactory.getExecutor().requestJob(next);
      }
    }
    // }

    logManager.log(Level.INFO, "Completed ForwardAnalysis on ", block);
    return status;
  }

  public static class AlwaysFalse extends BlockOperator {
    @Override
    public boolean isBlockEnd(final CFANode loc, final int thresholdValue) {
      return false;
    }
  }
}
