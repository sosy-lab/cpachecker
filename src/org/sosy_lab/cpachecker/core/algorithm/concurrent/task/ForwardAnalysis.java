// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;

@Options(prefix="concurrent.task.forward")
public class ForwardAnalysis implements Task {
  @SuppressWarnings("FieldMayBeFinal")
  @Option(description="Forward Config")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configFile = null;

  private final Block block;

  private final ReachedSet reached;

  private final TaskFactory taskFactory;

  private final LogManager logManager;

  // Todo: Load in factory and reuse across instances
  private volatile static Configuration forward = null;

  // Todo: Create in factory and reuse across instances
  private Algorithm algorithm = null;

  // Todo: Create in factory and reuse across instances
  private WrapperCPA cpa = null;

  public ForwardAnalysis (
      final Block pBlock, @Nullable final BooleanFormula pPrecondition, final Configuration pConfig,
      final Specification pSpecification, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA,
      final TaskFactory pTaskFactory
      ) throws InvalidConfigurationException, CPAException, InterruptedException {
    pConfig.inject(this);
    loadForwardConfig();

    block = pBlock;
    reached = new ReachedSetFactory(forward, pLogger).create();
    taskFactory = pTaskFactory;
    logManager = pLogger;

    cpa = (WrapperCPA) BlockAwareCompositeCPA.factory()
        .setConfiguration(forward).setLogger(pLogger).setShutdownNotifier(pShutdownNotifier)
        .set(pCFA, CFA.class).set(block, Block.class).set(pSpecification, Specification.class)
        .createInstance();

    algorithm
        = new CPAAlgorithmFactory((ConfigurableProgramAnalysis) cpa, pLogger, forward, pShutdownNotifier).newInstance();

    final CFANode blockEntry = block.getEntry();
    // PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    // Precision predicatePrecision = predicateCPA.getInitialPrecision(blockEntry, getDefaultPartition());
    // LocationCPA locationCPA = cpa.retrieveWrappedCpa(LocationCPA.class);
    // Precision locationPrecision = locationCPA.getInitialPrecision(blockEntry, getDefaultPartition());
    // CompositePrecision precision = new CompositePrecision(List.of(locationPrecision, predicatePrecision));

    Precision precision
        = ((ConfigurableProgramAnalysis) cpa).getInitialPrecision(blockEntry, getDefaultPartition());

    CompositeState state = buildEntryState(blockEntry, pPrecondition);
    reached.add(state, precision);
  }

  private void loadForwardConfig() throws InvalidConfigurationException {
    if(forward == null) {  // Todo: no double-checked locking
      synchronized(ForwardAnalysis.class) {
        if(forward == null) {
          if(configFile != null) {
            try {
              forward = Configuration.builder().loadFromFile(configFile).build();
            } catch(IOException ignored) {
              final String message = "Failed to load file " + configFile + ". "
                                   + "Using default configuration.";
              logManager.log(Level.SEVERE, message);
            }
          }

          if (forward == null) {
            forward = Configuration.builder()
                .loadFromResource(ForwardAnalysis.class, "predicateForward.properties")
                .build();
          }
        }
      }
    }
  }

  private CompositeState buildEntryState(final CFANode pNode, @Nullable final BooleanFormula pFormula)
      throws CPAException {
    PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    if(predicateCPA == null) {
      throw new CPAException("ForwardAnalysis requires a composite CPA with PredicateCPA");
    }
    AbstractState state = predicateCPA.getInitialState(pNode, getDefaultPartition());

    PredicateAbstractState predicateState
        = AbstractStates.extractStateByType(state, PredicateAbstractState.class);

    LocationCPA locationCPA = cpa.retrieveWrappedCpa(LocationCPA.class);
    if(locationCPA == null) {
      throw new CPAException("ForwardAnalysis requires a composite CPA with LocationCPA");
    }
    AbstractState locationState = locationCPA.getInitialState(pNode, getDefaultPartition());

    BooleanFormula formula;
    if(pFormula == null) {
      formula = predicateCPA.getSolver().getFormulaManager().getBooleanFormulaManager().makeTrue();
    } else {
     formula = pFormula;
    }

    PathFormula pathFormula = predicateState.getPathFormula().withFormula(formula);

    PredicateAbstractState predicateEntryState
        = PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(pathFormula, predicateState);

    return new CompositeState(List.of(locationState, predicateEntryState));
  }

  @Override public AlgorithmStatus call()
      throws CPAException, InterruptedException, InvalidConfigurationException {
    logManager.log(Level.INFO, "Starting ForwardAnalysis on ", block);

    AlgorithmStatus status = algorithm.run(reached);

    for(final Entry<CFANode, Block> exit : block.getExits().entrySet()) {
      Collection<AbstractState> states = reached.getReached(exit.getKey());

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
      for(final AbstractState state : states) {
        LocationState location = AbstractStates.extractStateByType(state, LocationState.class);
        if(location == null) {
          throw new CPAException("ForwardAnalysis requires a composite CPA with LocationCPA");
        }

        if(location.getLocationNode() == exit.getKey()) {
          PredicateAbstractState predicateState
              = AbstractStates.extractStateByType(state, PredicateAbstractState.class);

          if(predicateState == null) {
            throw new CPAException("ForwardAnalysis requires a composite CPA with PredicateCPA");
          }

          BooleanFormula exitFormula = predicateState.getPathFormula().getFormula();
          Task next = taskFactory.createForwardAnalysis(exit.getValue(), exitFormula);
          taskFactory.getExecutor().requestJob(next);
        }
      }
    }

    logManager.log(Level.INFO, "Completed ForwardAnalysis on ", block);
    return status;
  }

  public static class AlwaysFalse extends BlockOperator {
    @Override public boolean isBlockEnd(final CFANode loc, final int thresholdValue) {
      return false;
    }
  }
}
