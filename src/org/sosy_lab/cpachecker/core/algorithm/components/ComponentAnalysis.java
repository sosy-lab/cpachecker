// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockToDotWriter;
import org.sosy_lab.cpachecker.cfa.blocks.builder.BlockPartitioningBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockOperatorDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.CFADecomposer;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.GivenSizeDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.memory.InMemoryConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.network.NetworkConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.ComponentsBuilder;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.ComponentsBuilder.Components;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.FaultLocalizationWorker;
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

@Options(prefix = "components")
public class ComponentAnalysis implements Algorithm {

  private enum DecompositionType {
    BLOCK_OPERATOR,
    GIVEN_SIZE
  }

  private enum ConnectionType {
    NETWORK,
    IN_MEMORY
  }

  private enum WorkerType {
    DEFAULT,
    SMART,
    MONITORED,
    FAULT_LOCALIZATION
  }

  private final Configuration configuration;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownManager shutdownManager;
  private final Specification specification;

  @Option(description = "algorithm to decompose the CFA")
  private DecompositionType decompositionType = DecompositionType.BLOCK_OPERATOR;

  @Option(description = "how to send messages")
  private ConnectionType connectionType = ConnectionType.NETWORK;

  @Option(description = "which worker to use")
  private WorkerType workerType = WorkerType.DEFAULT;

  @Option(description = "desired number of BlockNodes")
  private int desiredNumberOfBlocks = 10;

  public ComponentAnalysis(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownManager pShutdownManager,
      Specification pSpecification) throws InvalidConfigurationException {
    configuration = pConfig;
    configuration.inject(this);
    logger = pLogger;
    cfa = pCfa;
    shutdownManager = pShutdownManager;
    specification = pSpecification;
    checkConfig();
  }

  private void checkConfig() throws InvalidConfigurationException {
    if (workerType == WorkerType.FAULT_LOCALIZATION
        && decompositionType != DecompositionType.BLOCK_OPERATOR) {
      throw new InvalidConfigurationException(
          FaultLocalizationWorker.class.getCanonicalName() + " needs decomposition with type "
              + DecompositionType.BLOCK_OPERATOR + " but got " + decompositionType);
    }
  }

  private CFADecomposer getDecomposer() throws InvalidConfigurationException {
    switch (decompositionType) {
      case BLOCK_OPERATOR:
        return new BlockOperatorDecomposer(configuration);
      case GIVEN_SIZE:
        return new GivenSizeDecomposer(new BlockOperatorDecomposer(configuration),
            desiredNumberOfBlocks);
      default:
        throw new AssertionError("Unknown DecompositionType: " + decompositionType);
    }
  }

  private ComponentsBuilder analysisWorker(ComponentsBuilder pBuilder, BlockNode pNode, SSAMap pMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    switch (workerType) {
      case DEFAULT:
        return pBuilder.addAnalysisWorker(pNode, pMap);
      case SMART:
        return pBuilder.addSmartAnalysisWorker(pNode, pMap);
      case MONITORED:
        return pBuilder.addMonitoredAnalysisWorker(pNode, pMap);
      case FAULT_LOCALIZATION:
        return pBuilder.addFaultLocalizationWorker(pNode, pMap);
      default:
        throw new AssertionError("Unknown WorkerType: " + workerType);
    }
  }

  private Class<? extends ConnectionProvider<?>> getConnectionProvider() {
    switch (connectionType) {
      case NETWORK:
        return NetworkConnectionProvider.class;
      case IN_MEMORY:
        return InMemoryConnectionProvider.class;
      default:
        throw new AssertionError("Unknown ConnectionType " + connectionType);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Starting block analysis...");
    try {
      CFADecomposer decomposer = getDecomposer();
      BlockTree tree = decomposer.cut(cfa);
      // drawBlockDot(tree);
      Collection<BlockNode> removed = tree.removeEmptyBlocks();
      if (!removed.isEmpty()) {
        logger.log(Level.INFO, "Removed " + removed.size() + " empty BlockNodes from the tree.");
      }
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
      builder = builder.withConnectionType(getConnectionProvider())
          .createAdditionalConnections(1);
      for (BlockNode distinctNode : blocks) {
        if (distinctNode.isRoot()) {
          builder = builder.addRootWorker(distinctNode);
        } else {
          builder = analysisWorker(builder, distinctNode, map);
        }
      }
      builder = builder.addResultCollectorWorker(blocks);
      builder = builder.addTimeoutWorker(900000);
      Components components = builder.addVisualizationWorker(tree).build();

      // run all workers
      for (Worker worker : components.getWorkers()) {
        Thread thread = new Thread(worker, worker.getId());
        thread.setDaemon(true);
        thread.start();
      }

      Connection mainThreadConnection = components.getAdditionalConnections().get(0);

      Set<Message> faults = new HashSet<>();
      // Wait for result
      Message resultMessage;
      Result result;
      while (true) {
        Message m = mainThreadConnection.read();
        if (m.getType() == MessageType.ERROR_CONDITION && m.getPayload()
            .containsKey(Payload.FAULT_LOCALIZATION)) {
          faults.add(m);
        }
        if (m.getType() == MessageType.FOUND_RESULT) {
          resultMessage = m;
          result = Result.valueOf(m.getPayload().get(Payload.RESULT));
          while (!mainThreadConnection.isEmpty()) {
            m = mainThreadConnection.read();
            if (m.getType() == MessageType.ERROR_CONDITION && m.getPayload()
                .containsKey(Payload.FAULT_LOCALIZATION)) {
              faults.add(m);
            }
          }
          break;
        }
        if (m.getType() == MessageType.ERROR) {
          throw new CPAException(m.getPayload().toJSONString());
        }
      }

      Set<String> visitedBlocks = new HashSet<>(Splitter.on(",")
          .splitToList(resultMessage.getPayload().getOrDefault(Payload.VISITED, "")));
      faults.removeIf(m -> !(visitedBlocks.contains(m.getUniqueBlockId())));
      if (!faults.isEmpty() && result == Result.FALSE) {
        logger.log(Level.INFO, "Found faults:\n" + Joiner.on("\n").join(
            faults.stream().map(m -> m.getPayload().getOrDefault(Payload.FAULT_LOCALIZATION, ""))
                .collect(
                    Collectors.toSet())));
      }

      for (Worker worker : components.getWorkers()) {
        worker.shutdown();
      }
      mainThreadConnection.close();

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

  private void drawBlockDot(BlockTree tree) {
    BlockPartitioningBuilder builder = new BlockPartitioningBuilder();
    for (BlockNode distinctNode : tree.getDistinctNodes()) {
      builder.addBlock(distinctNode.getNodesInBlock(), distinctNode.getStartNode());
    }
    new BlockToDotWriter(builder.build(cfa)).dump(Path.of("./output/hahahah.dot"), logger);
  }

}
