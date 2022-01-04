// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockOperatorDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.memory.InMemoryConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.ComponentsBuilder;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.ComponentsBuilder.Components;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.Worker;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class ComponentAnalysis implements Algorithm {

  private final Configuration configuration;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownManager shutdownManager;
  private final Specification specification;

  public ComponentAnalysis(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownManager pShutdownManager,
      Specification pSpecification) {
    configuration = pConfig;
    logger = pLogger;
    cfa = pCfa;
    shutdownManager = pShutdownManager;
    specification = pSpecification;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Starting block analysis...");
    try {
      BlockTree tree = new BlockOperatorDecomposer(configuration).cut(cfa);
      if (tree.isEmpty()) {
        // empty program
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

      // create run
      Collection<BlockNode> blocks = tree.getDistinctNodes();

      Set<CFANode> allNodes = ImmutableSet.copyOf(cfa.getAllNodes());
      List<CFAEdge> blockEdges = new ArrayList<>();
      for (CFANode cfaNode : allNodes) {
        CFAUtils.leavingEdges(cfaNode).filter(edge -> allNodes.contains(edge.getSuccessor()))
            .copyInto(blockEdges);
      }

      Solver solver = Solver.create(configuration, logger, shutdownManager.getNotifier());
      PathFormulaManagerImpl manager =
          new PathFormulaManagerImpl(
              solver.getFormulaManager(),
              configuration,
              logger,
              shutdownManager.getNotifier(),
              cfa,
              AnalysisDirection.FORWARD);
      SSAMap map = manager.makeFormulaForPath(blockEdges).getSsa();


      ComponentsBuilder builder =
          new ComponentsBuilder(logger, cfa, specification, configuration, shutdownManager);
      builder = builder.withConnectionType(InMemoryConnectionProvider.class)
          .createAdditionalConnections(1);
      for (BlockNode distinctNode : blocks) {
        builder = builder.addAnalysisWorker(distinctNode, map);
      }
      builder = builder.addResultCollectorWorker(blocks);
      builder = builder.addTimeoutWorker(900000);
      Components components = builder.addVisualizationWorker(tree, solver).build();

      // run all workers
      for (Worker worker : components.getWorkers()) {
        new Thread(worker).start();
      }

      Connection mainThreadConnection = components.getAdditionalConnections().get(0);

      // Wait for result
      Result result;
      while (true) {
        Message m = mainThreadConnection.read();
        if (m.getType() == MessageType.FOUND_RESULT) {
          result = Result.valueOf(m.getPayload());
          break;
        }
        if (m.getType() == MessageType.ERROR) {
          result = Result.UNKNOWN;
          break;
        }
      }

      // print result
      if (result == Result.FALSE) {
        ARGState state = (ARGState) reachedSet.getFirstState();
        CompositeState cState = (CompositeState) state.getWrappedState();
        Precision initialPrecision = reachedSet.getPrecision(state);
        List<AbstractState> states = new ArrayList<>(cState.getWrappedStates());
        states.add(DummyTargetState.withoutTargetInformation());
        reachedSet.add(new ARGState(new CompositeState(states), null), initialPrecision);
      }
      if (result == Result.TRUE) {
        reachedSet.clear();
      }
      // TODO?????
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } catch (InvalidConfigurationException | IOException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException pE) {
      logger.log(Level.SEVERE, "Block analysis stopped due to ", pE);
      throw new CPAException("Component Analysis run into an error.", pE);
    } finally {
      logger.log(Level.INFO, "Block analysis finished.");
    }
  }

}
