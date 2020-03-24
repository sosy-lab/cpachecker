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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public class SimpleAssumeEdgeStrategy
    extends GenericCFAMutationStrategy<Pair<AssumeEdge, AssumeEdge>, Pair<AssumeEdge, AssumeEdge>> {

  public SimpleAssumeEdgeStrategy(LogManager pLogger, int pRate, int pStartDepth) {
    super(pLogger, pRate, pStartDepth);
  }

  @Override
  protected Collection<Pair<AssumeEdge, AssumeEdge>> getAllObjects(ParseResult pParseResult) {
    List<Pair<AssumeEdge, AssumeEdge>> answer = new ArrayList<>();

    for (CFANode node : pParseResult.getCFANodes().values()) {
      if (node.getNumLeavingEdges() != 2) {
        continue;
      }

      AssumeEdge e0 = (AssumeEdge) node.getLeavingEdge(0);
      AssumeEdge e1 = (AssumeEdge) node.getLeavingEdge(1);

      // loops can be disconnected from cfa,
      // CFANode.isLoopStart is not enough for some reason
      CFANode s = e0.getSuccessor();
      if (isBackwardEdge(e0) && s.getNumEnteringEdges() > 1 || countForwardEnteringEdges(s) > 1) {
        answer.add(Pair.of(e0, e1));
      }
      s = e1.getSuccessor();
      if (isBackwardEdge(e1) && s.getNumEnteringEdges() > 1 || countForwardEnteringEdges(s) > 1) {
        answer.add(Pair.of(e1, e0));
      }
    }

    return answer;
  }

  protected static boolean isBackwardEdge(CFAEdge pEdge) {
    return pEdge.getPredecessor().getReversePostorderId()
        <= pEdge.getSuccessor().getReversePostorderId();
  }

  private int countForwardEnteringEdges(CFANode pNode) {
    int count = 0;
    for (CFANode p : CFAUtils.predecessorsOf(pNode)) {
      if (p.getReversePostorderId() > pNode.getReversePostorderId()) {
        count++;
      }
    }
    return count;
  }

  @Override
  protected Collection<Pair<AssumeEdge, AssumeEdge>> getObjects(
      ParseResult pParseResult, int count) {
    List<Pair<AssumeEdge, AssumeEdge>> result = new ArrayList<>();
    Set<CFANode> preds = new HashSet<>();
    Set<CFANode> succs0 = new HashSet<>();

    int found = 0;
    for (Pair<AssumeEdge, AssumeEdge> pair : getAllObjects(pParseResult)) {
      if (!canRemove(pParseResult, pair)) {
        continue;
      }

      // can't mutate pair and swapped pair simultaneously
      CFANode predecessor = pair.getFirst().getPredecessor();
      if (preds.contains(predecessor)) {
        continue;
      }
      // can't remove assume edge because it might be last entering edge to successor
      // TODO *might be* -> *is*
      CFANode successor0 = pair.getFirst().getSuccessor();
      if (succs0.contains(successor0)) {
        continue;
      }

      preds.add(predecessor);
      succs0.add(successor0);

      result.add(pair);

      if (++found >= count) {
        break;
      }
    }

    return result;
  }

  @Override
  protected Pair<AssumeEdge, AssumeEdge> getRollbackInfo(
      ParseResult pParseResult, Pair<AssumeEdge, AssumeEdge> pObject) {
    return pObject;
  }

  @Override
  protected void removeObject(ParseResult pParseResult, Pair<AssumeEdge, AssumeEdge> pair) {
    AssumeEdge edgeToRemove = pair.getFirst();
    AssumeEdge edgeToBlank = pair.getSecond();

    logger.logf(
        Level.INFO, "removing %s and replacing %s with blank edge", edgeToRemove, edgeToBlank);
    disconnectEdge(edgeToRemove);
    disconnectEdge(edgeToBlank);
    BlankEdge newEdge =
        new BlankEdge(
            "", // pEdge.getRawStatement(),
            edgeToBlank.getFileLocation(),
            edgeToBlank.getPredecessor(),
            edgeToBlank.getSuccessor(),
            "blanked " + edgeToBlank.getDescription());
    connectEdge(newEdge);
  }

  @Override
  protected void returnObject(ParseResult pParseResult, Pair<AssumeEdge, AssumeEdge> pair) {
    CFANode predecessor = pair.getFirst().getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    BlankEdge insertedEdge = (BlankEdge) predecessor.getLeavingEdge(0);
    disconnectEdge(insertedEdge);
    connectEdge(pair.getFirst());
    connectEdge(pair.getSecond());
  }
}
