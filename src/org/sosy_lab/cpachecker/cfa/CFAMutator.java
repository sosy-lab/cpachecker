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

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;

@Options
public class CFAMutator extends CFACreator {
  private enum MutationType {
    NodeAndEdgeRemoval,
    EdgeBlanking,
    EdgeSticking
  }

  private ParseResult parseResult = null;
  private MutationType lastMutation = null;
  private CFAEdge lastRemovedEdge = null;
  private Set<CFAEdge> restoredEdges = new HashSet<>();
  private boolean canMutate = true;
  private CFA prevCFA = null;

  public CFAMutator(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected ParseResult parseToCFAs(final List<String> sourceFiles)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {
    if (parseResult == null) {
      parseResult = super.parseToCFAs(sourceFiles);
      return parseResult;
    } else {
      parseResult = removeAnEdge(parseResult);
      if (parseResult == null) {
        exportCFAAsync(prevCFA);
      }
      return parseResult;
    }
  }

  private ParseResult returnEdge(ParseResult pParseResult) {
    System.out.println("returning edge " + lastRemovedEdge);
    // TODO undo mutation: insert node, reconnect edges, insert lastRemovedEdge
    SortedSetMultimap<String, CFANode> nodes = TreeMultimap.create(pParseResult.getCFANodes());

    CFANode removedNode = lastRemovedEdge.getPredecessor();
    nodes.put(removedNode.getFunctionName(), removedNode);

    CFANode successor = lastRemovedEdge.getSuccessor();
    assert successor.getNumEnteringEdges() > 0;
    CFAEdge insertedEdge = successor.getEnteringEdge(0);

    CFACreationUtils.removeEdgeFromNodes(insertedEdge);
    CFAEdge firstEdge = dupEdge(insertedEdge, null, removedNode);

    CFACreationUtils.addEdgeToCFA(firstEdge, logger);
    CFACreationUtils.addEdgeToCFA(lastRemovedEdge, logger);

    restoredEdges.add(lastRemovedEdge);

    return new ParseResult(
        pParseResult.getFunctions(),
        nodes,
        pParseResult.getGlobalDeclarations(),
        pParseResult.getFileNames());
  }

  private ParseResult removeAnEdge(ParseResult pParseResult) {
    SortedSetMultimap<String, CFANode> nodes = TreeMultimap.create(pParseResult.getCFANodes());

    canMutate = false;

    for (CFANode node : pParseResult.getCFANodes().values()) {
      for (CFAEdge edge : CFAUtils.allEnteringEdges(node)) {
        if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge
            || edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          CFACreationUtils.removeEdgeFromNodes(edge);
        }
      }
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge
            || edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          CFACreationUtils.removeEdgeFromNodes(edge);
        }
      }

      if (node.getNumLeavingEdges() != 1 || node.getNumEnteringEdges() != 1) {
        continue;
      }

      CFAEdge leavingEdge = node.getLeavingEdge(0);
      if (restoredEdges.contains(leavingEdge)) {
        System.out.println("skipping restored edge " + leavingEdge + " after node " + node);
        continue;
      }

      CFANode successor = leavingEdge.getSuccessor();
      CFAEdge enteringEdge = node.getEnteringEdge(0);
      CFANode predecessor = enteringEdge.getPredecessor();

      // TODO can't duplicate such edges
      if (enteringEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
          || enteringEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge
          || enteringEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge
          || enteringEdge.getEdgeType() == CFAEdgeType.CallToReturnEdge
          || leavingEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
          || leavingEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge
          || leavingEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge
          || leavingEdge.getEdgeType() == CFAEdgeType.CallToReturnEdge) {
        continue;
      }

      if (!predecessor.getFunctionName().equals(node.getFunctionName())
          || !successor.getFunctionName().equals(node.getFunctionName())) {
        System.out.println(
            enteringEdge + ", type " + enteringEdge.getEdgeType() + " and " + leavingEdge);
        continue;
      }

      if (predecessor.hasEdgeTo(successor)) {
        if (leavingEdge.getEdgeType() == CFAEdgeType.BlankEdge) {
          // restoredEdges.add(leavingEdge);
          continue;
          // stickAssumeEdgesIntoOne(predecessor, successor);
        } else {
          blankifyEdge(leavingEdge);
        }
      } else {
        removeNodeAndLeavingEdge(node);
        nodes.remove(node.getFunctionName(), node);
        System.out.println(nodes);
      }
      canMutate = true;
      break;
    }

    if (!canMutate) {
      System.out.println("no edge was removed");
      return null;
    }
    return new ParseResult(
        pParseResult.getFunctions(),
        nodes,
        pParseResult.getGlobalDeclarations(),
        pParseResult.getFileNames());
  }

  private void stickAssumeEdgesIntoOne(CFANode pPredecessor, CFANode pSuccessor) {
    lastMutation = MutationType.EdgeSticking;
    CFAEdge left = pPredecessor.getLeavingEdge(0);
    CFAEdge right = pPredecessor.getLeavingEdge(1);

    if (left.getSuccessor() == pSuccessor) {
      lastRemovedEdge = right;
    } else {
      lastRemovedEdge = left;
    }
    // TODO
  }

  private void removeNodeAndLeavingEdge(CFANode pNode) {
    lastMutation = MutationType.NodeAndEdgeRemoval;
    lastRemovedEdge = pNode.getLeavingEdge(0);
    System.out.println("removing " + pNode + " with edge " + lastRemovedEdge);

    CFANode successor = lastRemovedEdge.getSuccessor();
    CFAEdge enteringEdge = pNode.getEnteringEdge(0);

    CFAEdge newEdge = dupEdge(enteringEdge, null, successor);
    CFACreationUtils.removeEdgeFromNodes(enteringEdge);
    CFACreationUtils.addEdgeToCFA(newEdge, logger);

    CFACreationUtils.removeEdgeFromNodes(lastRemovedEdge);
  }

  private void blankifyEdge(CFAEdge pEdge) {
    lastMutation = MutationType.EdgeBlanking;
    lastRemovedEdge = pEdge;
    System.out.println("blanking edge " + pEdge);

    CFAEdge newEdge =
        new BlankEdge(
            "", FileLocation.DUMMY, pEdge.getPredecessor(), pEdge.getSuccessor(), "blank edge");
    CFACreationUtils.removeEdgeFromNodes(pEdge);
    CFACreationUtils.addEdgeToCFA(newEdge, logger);
  }

  private CFAEdge dupEdge(CFAEdge pEdge, CFANode pPredNode, CFANode pSuccNode) {
    if (pPredNode == null) {
      pPredNode = pEdge.getPredecessor();
    }
    if (pSuccNode == null) {
      pSuccNode = pEdge.getSuccessor();
    }

    assert pPredNode.getFunctionName().equals(pSuccNode.getFunctionName());

    CFAEdge newEdge = new DummyCFAEdge(pPredNode, pSuccNode);

    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        if (pEdge instanceof CAssumeEdge) {
          CAssumeEdge cAssumeEdge = (CAssumeEdge) pEdge;
          newEdge =
              new CAssumeEdge(
                  cAssumeEdge.getRawStatement(),
                  cAssumeEdge.getFileLocation(),
                  cAssumeEdge.getPredecessor(),
                  pSuccNode,
                  cAssumeEdge.getExpression(),
                  cAssumeEdge.getTruthAssumption(),
                  cAssumeEdge.isSwapped(),
                  cAssumeEdge.isArtificialIntermediate());
        } else {
          // TODO JAssumeEdge
          throw new UnsupportedOperationException("JAssumeEdge");
        }
        break;
      case BlankEdge:
        BlankEdge blankEdge = (BlankEdge) pEdge;
        newEdge =
            new BlankEdge(
                blankEdge.getRawStatement(),
                blankEdge.getFileLocation(),
                blankEdge.getPredecessor(),
                pSuccNode,
                blankEdge.getDescription());
        break;
      case DeclarationEdge:
        if (pEdge instanceof CDeclarationEdge) {
          CDeclarationEdge cDeclarationEdge = (CDeclarationEdge) pEdge;
          newEdge =
              new CDeclarationEdge(
                  cDeclarationEdge.getRawStatement(),
                  cDeclarationEdge.getFileLocation(),
                  cDeclarationEdge.getPredecessor(),
                  pSuccNode,
                  cDeclarationEdge.getDeclaration());
        } else {
          // TODO JDeclarationEdge
          throw new UnsupportedOperationException("JDeclarationEdge");
        }
        break;
      case FunctionCallEdge:
        if (pEdge instanceof CFunctionCallEdge) {
          CFunctionCallEdge cFunctionCallEdge = (CFunctionCallEdge) pEdge;
          newEdge =
              new CFunctionCallEdge(
                  cFunctionCallEdge.getRawStatement(),
                  cFunctionCallEdge.getFileLocation(),
                  cFunctionCallEdge.getPredecessor(),
                  (CFunctionEntryNode) pSuccNode, // TODO?
                  cFunctionCallEdge.getRawAST().get(),
                  cFunctionCallEdge.getSummaryEdge());
        } else {
          // TODO JMethodCallEdge
          throw new UnsupportedOperationException("JMethodCallEdge");
        }
        break;
      case FunctionReturnEdge:
        if (pEdge instanceof CFunctionReturnEdge) {
          CFunctionReturnEdge cFunctionReturnEdge = (CFunctionReturnEdge) pEdge;
          newEdge =
              new CFunctionReturnEdge(
                  cFunctionReturnEdge.getFileLocation(),
                  cFunctionReturnEdge.getPredecessor(),
                  pSuccNode,
                  cFunctionReturnEdge.getSummaryEdge());
        } else {
          // TODO JMethodReturnEdge
          throw new UnsupportedOperationException("JMethodReturnEdge");
        }
        break;
      case ReturnStatementEdge:
        if (pEdge instanceof CReturnStatementEdge) {
          CReturnStatementEdge cRerurnStatementEdge = (CReturnStatementEdge) pEdge;
          System.out.println("reconnecting edge " + pEdge + " to " + pSuccNode);
          System.out.flush();

          newEdge =
              new CReturnStatementEdge(
                  cRerurnStatementEdge.getRawStatement(),
                  cRerurnStatementEdge.getRawAST().get(),
                  cRerurnStatementEdge.getFileLocation(),
                  cRerurnStatementEdge.getPredecessor(),
                  (FunctionExitNode) pSuccNode); // TODO?
        } else {
          // TODO JReturnStatementEdge
          throw new UnsupportedOperationException("JReturnStatementEdge");
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
                  cStatementEdge.getPredecessor(),
                  pSuccNode);
        } else {
          // TODO JStatementEdge
          throw new UnsupportedOperationException("JStatementEdge");
        }
        break;
      case CallToReturnEdge:
        throw new UnsupportedOperationException("SummaryEdge");
      default:
        throw new UnsupportedOperationException("Unknown Type of edge: " + pEdge.getEdgeType());
    }

    System.out.println("duplicated edge " + pEdge + " as " + newEdge);

    return newEdge;
  }

  private ParseResult removeFunction(ParseResult pParseResult) {
    NavigableMap<String, FunctionEntryNode> func = new TreeMap<>(pParseResult.getFunctions());
    System.out.println("removing " + func.firstEntry().getKey());
    func.remove(func.firstEntry().getKey());
    ParseResult ans =
        new ParseResult(
            func,
            pParseResult.getCFANodes(),
            pParseResult.getGlobalDeclarations(),
            pParseResult.getFileNames());
    return ans;
  }

  @Override
  protected void exportCFAAsync(final CFA cfa) {
    if (cfa != null) {
      System.out.println("Count of CFA nodes: " + cfa.getAllNodes().size());
    } else {
      System.out.println("Null cfa, canMutate = " + canMutate);
    }
    System.out.println("CANMUTATE IS " + canMutate);
    if (!canMutate) {
      super.exportCFAAsync(cfa);
    } else {
      System.out.println("got nonnull cfa: " + (cfa != null));
      prevCFA = cfa;
    }
  }

  public void rollback() {
    if (lastMutation == MutationType.NodeAndEdgeRemoval) {
      parseResult = returnEdge(parseResult);
    } else {
      parseResult = refillEdge(parseResult);
    }
  }

  private ParseResult refillEdge(ParseResult pParseResult) {
    System.out.println("refilling edge " + lastRemovedEdge);
    CFAEdge blank = lastRemovedEdge.getPredecessor().getLeavingEdge(0);
    restoredEdges.add(lastRemovedEdge);

    CFACreationUtils.removeEdgeFromNodes(blank);
    CFACreationUtils.addEdgeToCFA(lastRemovedEdge, logger);

    return pParseResult;
  }
}
