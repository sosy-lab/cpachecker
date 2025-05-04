// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.FunctionDefinition;

public class CFAToCTranslator extends AbstractToCTranslator {

  public CFAToCTranslator(Configuration pConfig) throws InvalidConfigurationException {
    super(pConfig);
  }

  /**
   * Translates the given {@link CFA} into a C program. The given C program is semantically
   * equivalent to the CFA. It is <b>not</b> guaranteed that a CFA created from the resulting C
   * program will be equal to the given CFA.
   *
   * @param pCfa the CFA to translate
   * @return C representation of the given CFA
   * @throws CPAException if the given CFA is not complete (e.g., if it contains {@link CProblemType
   *     CProblemTypes})
   * @throws InvalidConfigurationException if the given CFA is not a CFA for a C program
   */
  public String translateCfa(CFA pCfa)
      throws CPAException, InvalidConfigurationException, IOException {
    functions = new ArrayList<>(pCfa.getNumberOfFunctions());

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be written to C for C programs, at the moment");
    }

    // the final C program may contain `abort()` statements, so we need a suitable declaration
    globalDefinitionsList.add("extern void abort();");

    for (FunctionEntryNode func : pCfa.entryNodes()) {
      translate((CFunctionEntryNode) func);
    }

    return generateCCode();
  }

  private void translate(CFunctionEntryNode pEntry) throws CPAException {
    // waitlist for the edges to be processed
    Deque<NodeAndBlock> waitlist = new ArrayDeque<>();
    Multimap<CFANode, NodeAndBlock> incomingBlocks = HashMultimap.create();

    FunctionDefinition f = startFunction(pEntry);
    functions.add(f);

    for (CFAEdge relevant : getRelevantLeavingEdges(pEntry)) {
      CFANode succ = relevant.getSuccessor();
      pushToWaitlist(waitlist, new NodeAndBlock(succ, f.getFunctionBody()));
    }

    while (!waitlist.isEmpty()) {
      NodeAndBlock current = getNextElement(waitlist);
      CFANode currentNode = current.getNode();

      if (createdStatements.containsKey(currentNode)) {
        // current node has already been handled, so just add a goto to it
        current.getCurrentBlock().addStatement(createGoto(currentNode, currentNode));

      } else {
        final CompoundStatement originalBlock = current.getCurrentBlock();
        final CompoundStatement currentBlock =
            getBlockToContinueWith(currentNode, originalBlock, incomingBlocks.get(currentNode));
        // create new NodeAndBlock because the block may have changed from the start of the loop
        if (currentBlock != originalBlock) {
          current = new NodeAndBlock(currentNode, currentBlock);
        }

        Collection<NodeAndBlock> nextNodes = handleNode(currentNode, currentBlock);
        for (NodeAndBlock next : nextNodes) {
          CFANode nextNode = next.getNode();
          incomingBlocks.put(nextNode, current);
          pushToWaitlist(waitlist, next);
        }
      }
    }
  }

  private ImmutableCollection<NodeAndBlock> handleNode(CFANode pNode, CompoundStatement pBlock)
      throws CPAException {
    ImmutableList.Builder<NodeAndBlock> nextOnes = ImmutableList.builder();

    if (pNode instanceof CFATerminationNode || pNode.getNumLeavingEdges() == 0) {
      pBlock.addStatement(createSimpleStatement(pNode, "abort();"));
      return ImmutableList.of();
    }

    if (pNode instanceof CFALabelNode) {
      pBlock.addStatement(createLabel((CFALabelNode) pNode));
    }

    Collection<Pair<CFAEdge, CompoundStatement>> outgoingEdges =
        handlePotentialBranching(pNode, pBlock);
    for (var p : outgoingEdges) {
      CFAEdge currentEdge = p.getFirst();
      CompoundStatement currentBlock = p.getSecond();

      String statement = translateSimpleEdge(currentEdge);
      if (!statement.isEmpty()) {
        pBlock.addStatement(createSimpleStatement(pNode, statement, currentEdge));
      }

      CFANode successor = getSuccessorNode(currentEdge);
      nextOnes.add(new NodeAndBlock(successor, currentBlock));
    }

    if (!createdStatements.containsKey(pNode)) {
      // add placeholder
      pBlock.addStatement(createEmptyStatement(pNode));
    }
    return nextOnes.build();
  }

  private FunctionDefinition startFunction(CFunctionEntryNode pFunctionStartNode) {
    String lFunctionHeader =
        pFunctionStartNode
            .getFunctionDefinition()
            .toASTString(AAstNodeRepresentation.DEFAULT)
            .replace(";", "");
    return new FunctionDefinition(
        lFunctionHeader, createCompoundStatement(pFunctionStartNode, null));
  }

  private ImmutableCollection<Pair<CFAEdge, CompoundStatement>> handlePotentialBranching(
      CFANode pNode, CompoundStatement pStartingBlock) {

    Collection<CFAEdge> outgoingEdges = getRelevantLeavingEdges(pNode).toList();
    if (outgoingEdges.size() == 1) {
      CFAEdge edgeToChild = Iterables.getOnlyElement(outgoingEdges);

      if (edgeToChild instanceof CAssumeEdge) {
        throw new IllegalStateException("Assume-edge without counterpart in CFA: " + edgeToChild);
      }
      return ImmutableSet.of(Pair.of(edgeToChild, pStartingBlock));

    } else if (outgoingEdges.size() > 1) {
      // if there are more than one children, then this must be a branching
      assert outgoingEdges.size() == 2
          : "branches with more than two options not supported (was the program prepocessed with"
              + " CIL?)";
      for (CFAEdge edgeToChild : outgoingEdges) {
        assert edgeToChild instanceof CAssumeEdge
            : "something wrong: branch in CFA without condition: " + edgeToChild;
      }

      ImmutableList.Builder<Pair<CFAEdge, CompoundStatement>> branches = ImmutableList.builder();

      List<CFAEdge> ifAndElseEdge = new ArrayList<>(outgoingEdges);
      if (!getRealTruthAssumption((CAssumeEdge) ifAndElseEdge.get(0))) {
        // swap elements so that if-branch comes before else-branch in list
        ifAndElseEdge = swapElements(ifAndElseEdge);
      }

      for (CFAEdge currentEdge : ifAndElseEdge) {
        CAssumeEdge assumeEdge = (CAssumeEdge) currentEdge;
        boolean truthAssumption = getRealTruthAssumption(assumeEdge);

        String cond;
        if (truthAssumption) {
          // must be if-branch, first in list
          assert ifAndElseEdge.get(0) == currentEdge;
          if (assumeEdge.getTruthAssumption()) {
            cond =
                "if ("
                    + assumeEdge.getExpression().toASTString(AAstNodeRepresentation.DEFAULT)
                    + ")";
          } else {
            cond =
                "if (!("
                    + assumeEdge.getExpression().toASTString(AAstNodeRepresentation.DEFAULT)
                    + "))";
          }
        } else {
          // must be else-branch, second in list
          assert ifAndElseEdge.get(1) == currentEdge;
          cond = "else";
        }

        // create a new block starting with this condition
        CompoundStatement newBlock =
            addIfStatementToBlock(pNode, pStartingBlock, cond, currentEdge);
        branches.add(Pair.of(currentEdge, newBlock));
      }
      return branches.build();
    }
    return ImmutableSet.of();
  }

  private void pushToWaitlist(Deque<NodeAndBlock> pWaitlist, NodeAndBlock pNodeAndBlock) {
    // Make sure that join nodes are always handled soon-ishly by pushing them to the waitlist;
    // to keep the structure of the generated program as shallow as possible.
    if (hasMoreThanOneElement(getPredecessorNodes(pNodeAndBlock.getNode()))) {
      assert getPredecessorNodes(pNodeAndBlock.getNode()).stream()
          .allMatch(createdStatements::containsKey);
      pWaitlist.push(pNodeAndBlock);
    } else {
      pWaitlist.offer(pNodeAndBlock);
    }
  }
}
