/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.cfa.mutation.strategy;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Triple;

public class LoopAssumeEdgeStrategy
    extends GenericCFAMutationStrategy<Chain, Triple<CFAEdge, CFAEdge, CFAEdge>> {

  public LoopAssumeEdgeStrategy(LogManager pLogger, int pRate, int pStartDepth) {
    super(pLogger, pRate, pStartDepth, "Branch-loops");
  }

  private Collection<CFAEdge> getBackwardEnteringEdges(final CFANode pNode) {
    return Lists.newArrayList(
        CFAUtils.enteringEdges(pNode).filter(SimpleAssumeEdgeStrategy::isBackwardEdge));
  }

  @Override
  protected Collection<Chain> getAllObjects(ParseResult pParseResult) {
    Collection<Chain> answer = new ArrayList<>();
    for (CFANode node : pParseResult.getCFANodes().values()) {
      if (node.getNumLeavingEdges() != 2) {
        continue;
      }

      Collection<CFAEdge> backwardEnteringEdges = getBackwardEnteringEdges(node);
      if (backwardEnteringEdges.isEmpty()) {
        continue;
      }

      ChainStrategy cs = new ChainStrategy(logger, 0, 0);
      Chain chain = null;
      // continue if the only entering chain does not start on node
      if (backwardEnteringEdges.size() == 1) {
        CFANode onlyBackwardPredecessor =
            Iterables.getOnlyElement(backwardEnteringEdges).getPredecessor();
        if (onlyBackwardPredecessor.getNumEnteringEdges() != 1
            || onlyBackwardPredecessor.getNumLeavingEdges() != 1) {
          continue;
        }
        chain = cs.getChainWith(onlyBackwardPredecessor);
        if (chain.getPredecessor() == node) {
          answer.add(chain);
        }
        continue;
      }
      assert chain == null;

      // try chains on assume edges
      CFANode successor = node.getLeavingEdge(0).getSuccessor();
      if (successor.getNumEnteringEdges() == 1 && successor.getNumLeavingEdges() == 1) {
        chain = cs.getChainWith(successor);
        if (chain.getSuccessor() != node) {
          chain = null;
        }
      }

      Chain otherChain = null;
      successor = node.getLeavingEdge(1).getSuccessor();
      if (successor.getNumEnteringEdges() == 1 && successor.getNumLeavingEdges() == 1) {
        otherChain = cs.getChainWith(successor);
        if (otherChain.getSuccessor() != node) {
          otherChain = null;
        }
      }

      if (chain == null && otherChain == null) {
        continue;
      }

      if (chain == null) {
        chain = otherChain;
      } else if (otherChain != null) {
          logger.logf(
              Level.SEVERE,
              "Got two loop chains at one branching point:\n%s\n%s",
              chain,
              otherChain);
          continue;
      }
      answer.add(chain);
    }

    return answer;
  }

  @Override
  protected Triple<CFAEdge, CFAEdge, CFAEdge> getRollbackInfo(
      ParseResult pParseResult, Chain pObject) {
    CFAEdge edgeToChain = pObject.getEnteringEdge();
    CFAEdge otherEdge = CFAUtils.getComplimentaryAssumeEdge((AssumeEdge) edgeToChain);
    return Triple.of(edgeToChain, otherEdge, pObject.getLeavingEdge());
  }

  @Override
  protected void removeObject(ParseResult pParseResult, Chain pChain) {
    CFAEdge edgeToChain = pChain.getEnteringEdge();
    CFANode branchingPoint = edgeToChain.getPredecessor();
    CFAEdge leavingEdge = CFAUtils.getComplimentaryAssumeEdge((AssumeEdge) edgeToChain);
    CFANode successor = leavingEdge.getSuccessor();
    CFAEdge edgeFromChain = pChain.getLeavingEdge();

    removeNodeFromParseResult(pParseResult, branchingPoint);
    disconnectEdgeFromNode(leavingEdge, successor);
    disconnectEdge(edgeFromChain);
    connectEdge(dupEdge(edgeFromChain, successor));

    disconnectEdgeFromNode(edgeToChain, pChain.getFirst());
    for (CFAEdge enteringEdge : CFAUtils.enteringEdges(branchingPoint)) {
      disconnectEdgeFromNode(enteringEdge, enteringEdge.getPredecessor());
      connectEdge(dupEdge(enteringEdge, pChain.getFirst()));
    }
  }

  @Override
  protected void returnObject(
      ParseResult pParseResult, Triple<CFAEdge, CFAEdge, CFAEdge> pRollbackInfo) {
    CFAEdge edgeToChain = pRollbackInfo.getFirst();
    CFAEdge leavingEdge = pRollbackInfo.getSecond();
    CFAEdge edgeFromChain = pRollbackInfo.getThird();
    CFANode branchingPoint = leavingEdge.getPredecessor();
    CFANode firstNode = edgeToChain.getSuccessor();
    CFANode successor = edgeFromChain.getSuccessor();

    disconnectEdge(edgeFromChain.getPredecessor().getEdgeTo(successor));
    connectEdgeToNode(leavingEdge, successor);
    connectEdge(edgeFromChain);

    for (CFAEdge enteringEdge : CFAUtils.enteringEdges(branchingPoint)) {
      disconnectEdge(enteringEdge.getPredecessor().getEdgeTo(firstNode));
      connectEdgeToNode(enteringEdge, enteringEdge.getPredecessor());
    }
    connectEdgeToNode(edgeToChain, firstNode);
    addNodeToParseResult(pParseResult, branchingPoint);
  }
}
