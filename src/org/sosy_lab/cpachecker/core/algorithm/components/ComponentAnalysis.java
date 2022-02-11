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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockToDotWriter;
import org.sosy_lab.cpachecker.cfa.blocks.builder.BlockPartitioningBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockOperatorDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.CFADecomposer;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.GivenSizeDecomposer;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.memory.InMemoryConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.network.NetworkConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer.ErrorMessageObserver;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer.FaultLocalizationMessageObserver;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer.MessageListener;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer.ResultMessageObserver;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer.StatusObserver;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.ComponentsBuilder;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.ComponentsBuilder.Components;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.FaultLocalizationWorker;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.Worker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "components")
public class ComponentAnalysis implements Algorithm {

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
  private int desiredNumberOfBlocks = 4;

  @Option(description = "maximal overall wall-time for parallel analysis")
  @TimeSpanOption(codeUnit = TimeUnit.MILLISECONDS, min = 0)
  private TimeSpan maxWallTime = TimeSpan.ofSeconds(15 * 60);

  @Option(description = "whether to use daemon threads for workers")
  private boolean daemon = true;

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

  /**
   * Currently, fault localization worker require linear blocks
   * @throws InvalidConfigurationException if configuration for block analysis is invalid
   */
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
    MessageListener listener = new MessageListener();
    listener.register(new ResultMessageObserver(reachedSet));
    listener.register(new ErrorMessageObserver());
    listener.register(new StatusObserver());
    try {
      // create block tree and reduce to relevant parts
      CFADecomposer decomposer = getDecomposer();
      BlockTree tree = decomposer.cut(cfa);
      logger.logf(Level.INFO, "Decomposed CFA in %d blocks using the %s.",
          tree.getDistinctNodes().size(), decomposer.getClass().getCanonicalName());
      //drawBlockDot(tree);
      Collection<BlockNode> removed = tree.removeEmptyBlocks();
      if (!removed.isEmpty()) {
        logger.log(Level.INFO, "Removed " + removed.size() + " empty BlockNodes from the tree.");
      }
      if (tree.isEmpty()) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

      // create type map (maps variables to their type)
      SSAMap map = getTypeMap();

      // create workers
      Collection<BlockNode> blocks = tree.getDistinctNodes();
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
      builder = builder.addTimeoutWorker(maxWallTime);
      Components components = builder.addVisualizationWorker(tree).build();

      // run workers
      for (Worker worker : components.getWorkers()) {
        Thread thread = new Thread(worker, worker.getId());
        thread.setDaemon(daemon);
        thread.start();
      }

      // listen to messages
      Connection mainThreadConnection = components.getAdditionalConnections().get(0);
      if (workerType == WorkerType.FAULT_LOCALIZATION) {
        listener.register(new FaultLocalizationMessageObserver(logger, mainThreadConnection));
      }

      // wait for result
      while (true) {
        // breaks if one observer wants to finish.
        if (listener.process(mainThreadConnection.read())) {
          break;
        }
      }

      // finish and shutdown
      listener.finish();
      for (Worker worker : components.getWorkers()) {
        worker.shutdown();
      }
      mainThreadConnection.close();

      return listener.getObserver(StatusObserver.class).getStatus();
    } catch (InvalidConfigurationException | IOException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException pE) {
      logger.log(Level.SEVERE, "Block analysis stopped due to ", pE);
      throw new CPAException("Component Analysis run into an error.", pE);
    } finally {
      logger.log(Level.INFO, "Block analysis finished.");
    }
  }

  private SSAMap getTypeMap()
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
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
    return manager.makeFormulaForPath(blockEdges).getSsa();
  }

  private void drawBlockDot(BlockTree tree) {
    BlockPartitioningBuilder builder = new BlockPartitioningBuilder();
    for (BlockNode distinctNode : tree.getDistinctNodes()) {
      builder.addBlock(distinctNode.getNodesInBlock(), distinctNode.getStartNode());
    }
    new BlockToDotWriter(builder.build(cfa)).dump(Path.of("./output/hahahah.dot"), logger);
  }

}
