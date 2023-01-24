// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockNodeToCFAConverter {
  private final LogManager logger;
  private final ShutdownNotifier notifier;
  private final CFACreator creator;

  public BlockNodeToCFAConverter(
      Configuration pConfiguration, LogManager pLogger, ShutdownNotifier pNotifier)
      throws InvalidConfigurationException {
    logger = pLogger;
    notifier = pNotifier;
    Configuration update =
        Configuration.builder()
            .copyFrom(pConfiguration)
            .setOption("cfa.exportPerFunction", "false")
            .setOption("cfa.export", "false")
            .build();
    creator = new CFACreator(update, logger, notifier);
  }

  public CFA createEmptyCFA(CFA pCFA)
      throws ParserException, InterruptedException, InvalidConfigurationException {
    CFA emptyCFA = creator.parseSourceAndCreateCFA("int main(){}");
    FlexCfaNetwork cfaNetwork = FlexCfaNetwork.copy(emptyCFA);
    for (CFANode allNode : emptyCFA.getAllNodes()) {
      if (allNode instanceof FunctionEntryNode) {
        continue;
      }
      cfaNetwork.removeNode(allNode);
    }
    return CCfaFactory.CLONER.createCfa(
        cfaNetwork,
        pCFA.getMetadata().withMainFunctionEntry(emptyCFA.getMainFunction()),
        logger,
        notifier);
  }

  public CFA convert(BlockNodeMetaData pBlockNode, CFA pCFA)
      throws ParserException, InterruptedException, InvalidConfigurationException {
    List<CFANode> waitlist = new ArrayList<>();
    // create empty network
    EmptyNetwork emptyNetwork = createEmptyNetwork(pBlockNode.getStartNode());
    FlexCfaNetwork flexCfaNetwork = emptyNetwork.flexCfaNetwork();
    waitlist.add(emptyNetwork.root());
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.remove(0);
      for (CFAEdge cfaEdge : findSuccessorsInBlock(current, pBlockNode)) {
        flexCfaNetwork.insertSuccessor(current, cfaEdge, cfaEdge.getSuccessor());
        waitlist.add(cfaEdge.getSuccessor());
      }
    }
    BlockEndBlankEdge endEdge = new BlockEndBlankEdge(FileLocation.DUMMY, pBlockNode.getLastNode());
    flexCfaNetwork.insertSuccessor(pBlockNode.getLastNode(), endEdge, endEdge.getSuccessor());
    FunctionEntryNode entryNode = emptyNetwork.entryNode();
    return CCfaFactory.CLONER.createCfa(
        flexCfaNetwork, pCFA.getMetadata().withMainFunctionEntry(entryNode), logger, notifier);
  }

  private Set<CFAEdge> findSuccessorsInBlock(CFANode pNode, BlockNodeMetaData pBlockNode) {
    return Sets.intersection(pBlockNode.getEdgesInBlock(), CFAUtils.allLeavingEdges(pNode).toSet());
  }

  private EmptyNetwork createEmptyNetwork(CFANode pStartNode)
      throws ParserException, InterruptedException, InvalidConfigurationException {
    CFA emptyCFA = creator.parseSourceAndCreateCFA("int main(){}");
    FlexCfaNetwork cfaNetwork = FlexCfaNetwork.copy(emptyCFA);
    FunctionEntryNode root;
    CFAEdge successor;
    CFANode successorNode;
    if (pStartNode instanceof FunctionEntryNode entry) {
      root = entry;
      assert entry.getNumLeavingEdges() == 1;
      successor = entry.getLeavingEdge(0);
      successorNode = successor.getSuccessor();
    } else {
      root = emptyCFA.getMainFunction();
      assert emptyCFA.getMainFunction().getNumLeavingEdges() == 1;
      successor = emptyCFA.getMainFunction().getLeavingEdge(0);
      successorNode = pStartNode;
    }
    for (CFANode node : emptyCFA.getAllNodes()) {
      if (node.equals(emptyCFA.getMainFunction())) {
        continue;
      }
      cfaNetwork.removeNode(node);
    }
    if (pStartNode instanceof FunctionEntryNode) {
      cfaNetwork.insertSuccessor(
          emptyCFA.getMainFunction(),
          new BlankEdge("", FileLocation.DUMMY, emptyCFA.getMainFunction(), pStartNode, ""),
          pStartNode);
      cfaNetwork.removeNode(emptyCFA.getMainFunction());
    }
    cfaNetwork.insertSuccessor(root, successor, successorNode);
    return new EmptyNetwork(cfaNetwork, root, successorNode);
  }

  private record EmptyNetwork(
      FlexCfaNetwork flexCfaNetwork, FunctionEntryNode entryNode, CFANode root) {}
}
