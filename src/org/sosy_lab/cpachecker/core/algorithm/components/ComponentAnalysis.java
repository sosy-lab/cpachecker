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
import java.util.concurrent.LinkedBlockingQueue;
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
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Worker;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerClient;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerSocket;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerSocket.WorkerSocketFactory;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.util.ActionLogger;
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
      WorkerSocketFactory factory = new WorkerSocketFactory();
      BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
      WorkerSocket mainSocket = factory.makeSocket(logger, new ActionLogger("main"), messages, "main", "localhost", 8090);
      Thread mainSocketThread = new Thread(() -> {
        try {
          mainSocket.startServer();
        } catch (IOException pE) {
          logger.log(Level.SEVERE, pE);
        }
      });
      mainSocketThread.start();
      Set<Worker> workers = new HashSet<>();
      int port = 8091;
      for (BlockNode node : tree.getDistinctNodes()) {
        Worker worker =
            Worker.registerNodeAndGetWorker(node, logger, cfa, specification, configuration,
                shutdownManager, factory, "localhost", port++);
        workers.add(worker);
      }
      for (Worker worker : workers) {
        for (InetSocketAddress address : factory.getAddresses()) {
          worker.addClient(
              new WorkerClient(address.getAddress().getHostAddress(), address.getPort()));
        }
        new Thread(worker).start();
      }

      // TODO: correct formulas, sat check, termination, false as pathFormulas??
      WorkerClient mainClient = new WorkerClient("localhost", 8090);
      Set<Message> finished = new HashSet<>();
      Result result;
      while (true) {
        Message m = messages.take();
        if (m.getType() == MessageType.FINISHED) {
          result = Result.valueOf(m.getPayload());
          finished.add(m);
          if (finished.size() == workers.size()) {
            mainClient.broadcast(Message.newFinishMessage("main", 0, result));
            break;
          }
        }
      }
      mainClient.close();
      mainSocketThread.interrupt();
      ARGState state = (ARGState) reachedSet.getFirstState();
      CompositeState cState = (CompositeState) state.getWrappedState();
      Precision initialPrecision = reachedSet.getPrecision(state);
      List<AbstractState> states = new ArrayList<>();
      states.addAll(cState.getWrappedStates());
      states.add(DummyTargetState.withoutProperty());
      logger.log(Level.INFO, "Block analysis finished.");
      if (result.equals(Result.FALSE)) {
        reachedSet.add(new ARGState(new CompositeState(states), null), initialPrecision);
      }
      return workers.stream().map(Worker::getStatus).reduce(AlgorithmStatus::update).orElseThrow();
    } catch (InvalidConfigurationException | IOException pE) {
      logger.log(Level.SEVERE, "Block analysis stopped due to ", pE);
      throw new CPAException("Invalid configuration", pE);
    }
  }


}
