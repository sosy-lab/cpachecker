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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

@Options(prefix = "concurrent.task.forward")
public class ForwardAnalysis implements Task {
  private static volatile Configuration forward = null;

  private final Block block;

  private final ReachedSet reached;

  private final TaskFactory taskFactory;

  private final LogManager logManager;

  private final FormulaManagerView formulaManager;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(description = "Forward Config")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configFile = null;

  private Algorithm algorithm = null;

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

    algorithm = factory.createAlgorithm(cpa, pCFA, pSpecification);

    final CFANode blockEntry = block.getEntry();

    PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    assert predicateCPA != null;
    formulaManager = predicateCPA.getSolver().getFormulaManager();

    Precision precision =
        ((ConfigurableProgramAnalysis) cpa).getInitialPrecision(blockEntry, getDefaultPartition());

    AbstractState state = buildEntryState(blockEntry, pPrecondition);
    reached.add(state, precision);
  }

  private void loadForwardConfig() throws InvalidConfigurationException {
    if (forward == null) {
      synchronized (ForwardAnalysis.class) {
        if (forward == null) {
          if (configFile != null) {
            try {
              forward = Configuration.builder().loadFromFile(configFile).build();
            } catch (IOException ignored) {
              logManager.log(
                  Level.SEVERE,
                  "Failed to load file ",
                  configFile,
                  ". Using default configuration.");
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
         * If the task gets interrupted while waiting to obtain the initial state,
         * 'rawInterruptedState' remains 'null' and it tries the operation again.
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
             * If the task gets interrupted while waiting to obtain the initial state,
             * 'componentState' remains 'null' and it tries the operation again.
             * TODO: Check for shutdown request.
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

    for (final AbstractState state : reached.asCollection()) {
      if (AbstractStates.isTargetState(state)) {
        logManager.log(Level.FINE, "Target State:", state);
      }

      LocationState location = AbstractStates.extractStateByType(state, LocationState.class);
      assert location != null;

      if (block.getExits().containsKey(location.getLocationNode())) {
        PredicateAbstractState predicateState =
            AbstractStates.extractStateByType(state, PredicateAbstractState.class);
        assert predicateState != null;

        BooleanFormula exitFormula = predicateState.getPathFormula().getFormula();

        Block exit = block.getExits().get(location.getLocationNode());
        final ShareableBooleanFormula shareableFormula =
            new ShareableBooleanFormula(formulaManager, exitFormula);

        Task next = taskFactory.createForwardAnalysis(exit, shareableFormula);
        taskFactory.getExecutor().requestJob(next);
      }
    }

    logManager.log(Level.INFO, "Completed ForwardAnalysis on ", block);
    return status;
  }
}
