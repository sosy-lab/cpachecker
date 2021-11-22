// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.components.cut.BlockOperatorCutter;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Worker;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Worker.WorkerFactory;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerClient;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerSocket;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockTree;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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
      BlockTree tree = new BlockOperatorCutter(configuration).cut(cfa);
      if (tree.isEmpty()) {
        // empty program
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      WorkerFactory workerFactory = new WorkerFactory();

      // Create main socket
      BlockingQueue<Message> messages = new PriorityBlockingQueue<>();
      WorkerSocket mainSocket = workerFactory.getSocketFactory().makeSocket(logger, messages,"localhost", 8090);
      Thread mainSocketThread = new Thread(() -> {
        try {
          mainSocket.startServer();
        } catch (IOException pE) {
          logger.log(Level.SEVERE, pE);
        }
      });
      mainSocketThread.start();

      // TODO: side effect -> must be executed before mainClients are created
      Set<Worker> workers = createWorkers(tree, workerFactory);

      // create all clients for the
      Set<WorkerClient> mainClients = new HashSet<>();
      for (InetSocketAddress address : workerFactory.getSocketFactory().getAddresses()) {
        mainClients.add(
            new WorkerClient(address.getAddress().getHostAddress(), address.getPort()));
      }

      Set<String> staleIds = new HashSet<>();
      Result result = Result.UNKNOWN;
      boolean exitLoop = false;
      while (!exitLoop) {
        Message m = messages.take();
        switch (m.getType()) {
          case STALE:
            if (Boolean.parseBoolean(m.getPayload())) {
              staleIds.add(m.getUniqueBlockId());
            } else {
              staleIds.remove(m.getUniqueBlockId());
            }
            if (staleIds.size() == workers.size()) {
              result = Result.TRUE;
              for (WorkerClient client : mainClients) {
                client.broadcast(Message.newShutdownMessage());
              }
              exitLoop = true;
            }
            break;
          case FOUND_VIOLATION:
            result = Result.FALSE;
            for (WorkerClient client : mainClients) {
              client.broadcast(Message.newShutdownMessage());
            }
            exitLoop = true;
            break;
          case SHUTDOWN:
          case PRECONDITION:
          case POSTCONDITION:
            break;
          default:
            throw new AssertionError("Unknown MessageType " + m.getType());
        }
      }
      for (WorkerClient mainClient : mainClients) {
        mainClient.close();
      }
      mainSocketThread.interrupt();
      logger.log(Level.INFO, "Block analysis finished.");
      if (result == Result.FALSE) {
        ARGState state = (ARGState) reachedSet.getFirstState();
        CompositeState cState = (CompositeState) state.getWrappedState();
        Precision initialPrecision = reachedSet.getPrecision(state);
        List<AbstractState> states = new ArrayList<>();
        states.addAll(cState.getWrappedStates());
        states.add(DummyTargetState.withoutTargetInformation());
        reachedSet.add(new ARGState(new CompositeState(states), null), initialPrecision);
      }
      if (result == Result.TRUE) {
        reachedSet.clear();
      }
      return workers.stream().map(Worker::getStatus).reduce(AlgorithmStatus::update).orElseThrow();
    } catch (InvalidConfigurationException | IOException pE) {
      logger.log(Level.SEVERE, "Block analysis stopped due to ", pE);
      throw new CPAException("Invalid configuration", pE);
    }
  }

  private Set<Worker> createWorkers(BlockTree tree, WorkerFactory workerFactory)
      throws IOException, CPAException, InterruptedException, InvalidConfigurationException {
    Set<Worker> workers = new HashSet<>();
    int port = 8091;
    for (BlockNode node : tree.getDistinctNodes()) {
      Worker worker =
          workerFactory.createWorker(node, logger, cfa, specification, configuration,
              shutdownManager,"localhost", port++);
      workers.add(worker);
    }
    for (Worker worker : workers) {
      for (InetSocketAddress address : workerFactory.getSocketFactory().getAddresses()) {
        worker.addClient(new WorkerClient(address));
      }
      new Thread(worker).start();
    }
    return workers;
  }

  // TODO: Parallel back and forward in two queues
  // FastForward backwards?


}
