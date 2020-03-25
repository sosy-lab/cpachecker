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

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public abstract class AbstractCFAMutationStrategy implements StatisticsProvider {

  protected final LogManager logger;
  protected AbstractMutationStatistics stats = new AbstractMutationStatistics();

  protected class AbstractMutationStatistics implements Statistics {
    protected final StatCounter rounds = new StatCounter("rounds");
    protected final StatCounter rollbacks = new StatCounter("rollbacks");
    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter.writingStatisticsTo(pOut).beginLevel().put(rounds).put(rollbacks).endLevel();
    }
    @Override
    public @Nullable String getName() {
      return this.toString();
    }
  }

  public AbstractCFAMutationStrategy(LogManager pLogger) {
    logger = pLogger;
  }

  public abstract boolean mutate(ParseResult parseResult);

  public abstract void rollback(ParseResult parseResult);

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  public abstract void makeAftermath(ParseResult pParseResult);

  protected void addNodeToParseResult(ParseResult parseResult, CFANode pNode) {
    logger.logf(Level.FINER, "adding node %s", pNode);
    assert parseResult.getCFANodes().put(pNode.getFunctionName(), pNode);
  }

  protected void connectEdge(CFAEdge pEdge) {
    logger.logf(Level.FINER, "adding edge %s", pEdge);
    CFANode pred = pEdge.getPredecessor();
    CFANode succ = pEdge.getSuccessor();
    assert !pred.hasEdgeTo(succ)
        : "can not connect edge "
            + pEdge
            + ": "
            + pred.getFunctionName()
            + ":"
            + pred
            + " already has edge "
            + pred.getEdgeTo(succ);
    assert !CFAUtils.enteringEdges(succ).contains(pEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(pEdge);
  }

  protected void connectEdgeToNode(CFAEdge pEdge, CFANode pNode) {
    logger.logf(Level.FINER, "adding edge %s to node %s", pEdge, pNode);
    if (pEdge.getPredecessor() == pNode) {
      assert !pNode.hasEdgeTo(pEdge.getSuccessor());
      pNode.addLeavingEdge(pEdge);
    } else if (pEdge.getSuccessor() == pNode) {
      assert !CFAUtils.enteringEdges(pNode).contains(pEdge);
      pNode.addEnteringEdge(pEdge);
    } else {
      assert false : "Tried to add edge " + pEdge + " to node " + pNode;
    }
  }

  protected void removeNodeFromParseResult(ParseResult parseResult, CFANode pNode) {
    logger.logf(Level.FINER, "removing node %s", pNode);
    assert parseResult.getCFANodes().remove(pNode.getFunctionName(), pNode);
  }

  protected void disconnectEdge(CFAEdge pEdge) {
    logger.logf(Level.FINER, "removing edge %s", pEdge);
    CFACreationUtils.removeEdgeFromNodes(pEdge);
  }

  protected void disconnectEdgeFromNode(CFAEdge pEdge, CFANode pNode) {
    logger.logf(Level.FINER, "removing edge %s from node %s", pEdge, pNode);
    if (pEdge.getPredecessor() == pNode) {
      pNode.removeLeavingEdge(pEdge);
    } else if (pEdge.getSuccessor() == pNode) {
      pNode.removeEnteringEdge(pEdge);
    } else {
      assert false : "Tried to remove edge " + pEdge + " from node " + pNode;
    }
  }

  protected CFAEdge dupEdge(CFAEdge pEdge, CFANode pSuccessor) {
    return dupEdge(pEdge, pEdge.getPredecessor(), pSuccessor);
  }

  // return an edge with same "contents" but from pPredNode to pSuccNode
  protected CFAEdge dupEdge(CFAEdge pEdge, CFANode pPredecessor, CFANode pSuccessor) {

    assert pPredecessor.getFunctionName().equals(pSuccessor.getFunctionName());

    CFAEdge newEdge;

    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        if (pEdge instanceof CAssumeEdge) {
          CAssumeEdge cAssumeEdge = (CAssumeEdge) pEdge;
          newEdge =
              new CAssumeEdge(
                  cAssumeEdge.getRawStatement(),
                  cAssumeEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor,
                  cAssumeEdge.getExpression(),
                  cAssumeEdge.getTruthAssumption(),
                  cAssumeEdge.isSwapped(),
                  cAssumeEdge.isArtificialIntermediate());
        } else if (pEdge instanceof JAssumeEdge) {
          JAssumeEdge jAssumeEdge = (JAssumeEdge) pEdge;
          newEdge =
              new JAssumeEdge(
                  jAssumeEdge.getRawStatement(),
                  jAssumeEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor,
                  jAssumeEdge.getExpression(),
                  jAssumeEdge.getTruthAssumption());
        } else {
          throw new UnsupportedOperationException("Unexpected edge class " + pEdge.getClass());
        }
        break;
      case BlankEdge:
        BlankEdge blankEdge = (BlankEdge) pEdge;
        newEdge =
            new BlankEdge(
                blankEdge.getRawStatement(),
                blankEdge.getFileLocation(),
                pPredecessor,
                pSuccessor,
                blankEdge.getDescription());
        break;
      case DeclarationEdge:
        if (pEdge instanceof CDeclarationEdge) {
          CDeclarationEdge cDeclarationEdge = (CDeclarationEdge) pEdge;
          newEdge =
              new CDeclarationEdge(
                  cDeclarationEdge.getRawStatement(),
                  cDeclarationEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor,
                  cDeclarationEdge.getDeclaration());
        } else if (pEdge instanceof JDeclarationEdge) {
          JDeclarationEdge jDeclarationEdge = (JDeclarationEdge) pEdge;
          newEdge =
              new JDeclarationEdge(
                  jDeclarationEdge.getRawStatement(),
                  jDeclarationEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor,
                  jDeclarationEdge.getDeclaration());
        } else {
          throw new UnsupportedOperationException("Unexpected edge class " + pEdge.getClass());
        }
        break;
      case StatementEdge:
        if (pEdge instanceof CStatementEdge) {
          CStatementEdge cStatementEdge = (CStatementEdge) pEdge;
          newEdge =
              new CStatementEdge(
                  cStatementEdge.getRawStatement(),
                  cStatementEdge.getStatement(),
                  cStatementEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor);
        } else if (pEdge instanceof JStatementEdge) {
          JStatementEdge jStatementEdge = (JStatementEdge) pEdge;
          newEdge =
              new JStatementEdge(
                  jStatementEdge.getRawStatement(),
                  jStatementEdge.getStatement(),
                  jStatementEdge.getFileLocation(),
                  pPredecessor,
                  pSuccessor);
        } else {
          throw new UnsupportedOperationException("Unexpected edge class " + pEdge.getClass());
        }
        break;
      case FunctionCallEdge:
      case FunctionReturnEdge:
      case ReturnStatementEdge:
      case CallToReturnEdge:
      default:
        throw new UnsupportedOperationException(
            "Unsupported type of edge " + pEdge.getEdgeType() + " at edge " + pEdge);
    }

    logger.logf(Level.FINEST, "duplicated edge %s as %s", pEdge, newEdge);

    return newEdge;
  }
}
