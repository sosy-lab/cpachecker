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
package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.Pair;


public class AssumeEdgeStrategy extends AbstractCFAMutationStrategy {

  private final List<Pair<AssumeEdge, AssumeEdge>> deleted = new ArrayList<>();

  public AssumeEdgeStrategy(LogManager pLogger) {
    super(pLogger);
  }

  private boolean checkNode(CFANode pNode) {
    if (pNode.getNumLeavingEdges() == 2) {
      CFAEdge left = pNode.getLeavingEdge(0);
      CFAEdge right = pNode.getLeavingEdge(1);
      assert left instanceof AssumeEdge && right instanceof AssumeEdge;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public long countPossibleMutations(ParseResult pParseResult) {
    int count = 0;
    for (CFANode node : pParseResult.getCFANodes().values()) {
      if (checkNode(node)) {
        count++;
      }
    }
    return count;
  }

  private Collection<Pair<AssumeEdge, AssumeEdge>> chooseEdgesToDelete(ParseResult pParseResult) {
    List<Pair<AssumeEdge, AssumeEdge>> answer = new ArrayList<>();

    int found = 0;
    for (CFANode node : pParseResult.getCFANodes().values()) {
      if (!checkNode(node)) {
        continue;
      }

      AssumeEdge edge0 = (AssumeEdge) node.getLeavingEdge(0);
      AssumeEdge edge1 = (AssumeEdge) node.getLeavingEdge(1);

      if (edge0.getSuccessor().getNumEnteringEdges() > 1) {
        answer.add(Pair.of(edge0, edge1));
        found++;
      } else if (edge1.getSuccessor().getNumEnteringEdges() > 1) {
        answer.add(Pair.of(edge1, edge0));
        found++;
      }

      if (found > 0) {
        break;
      }
    }

    return answer;
  }

  @Override
  public boolean mutate(ParseResult pParseResult) {
    deleted.clear();
    Collection<Pair<AssumeEdge, AssumeEdge>> chosenEdges = chooseEdgesToDelete(pParseResult);
    if (chosenEdges.isEmpty()) {
      return false;
    }

    for (Pair<AssumeEdge, AssumeEdge> pair : chosenEdges) {
      logger.logf(
          Level.INFO,
          "removing %s and replacing %s with blank edge",
          pair.getFirst(),
          pair.getSecond());
      disconnectEdge(pair.getFirst());
      blankEdge(pair.getSecond());
      deleted.add(pair);
    }

    return true;
  }

  @Override
  public void rollback(ParseResult pParseResult) {
    for (Pair<AssumeEdge, AssumeEdge> pair : deleted) {
      CFANode predecessor = pair.getFirst().getPredecessor();
      assert predecessor.getNumLeavingEdges() == 1;
      BlankEdge insertedEdge = (BlankEdge) predecessor.getLeavingEdge(0);
      disconnectEdge(insertedEdge);
      connectEdge(pair.getFirst());
      connectEdge(pair.getSecond());
    }
  }

  private void blankEdge(AssumeEdge pEdge) {
    disconnectEdge(pEdge);
    BlankEdge newEdge =
        new BlankEdge(
            "", // pEdge.getRawStatement(),
            pEdge.getFileLocation(),
            pEdge.getPredecessor(),
            pEdge.getSuccessor(),
            "blanked " + pEdge.getDescription());
    connectEdge(newEdge);
  }
}
