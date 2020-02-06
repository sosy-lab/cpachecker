/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.cfa.model.CFAEdgeType.FunctionCallEdge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.FunctionBody;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.SimpleStatement;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.Statement;

public class CFAToCTranslator {

  // Use original, unqualified names for variables
  private static final boolean NAMES_QUALIFIED = false;

  private static class EmptyStatement extends Statement {

    @Override
    void translateToCode0(StringBuilder pBuffer, int pIndent) {
      // do nothing
    }
  }

  private static class NodeAndBlock {
    private final CFANode node;
    private final CompoundStatement currentBlock;

    public NodeAndBlock(CFANode pNode, CompoundStatement pCurrentBlock) {
      node = pNode;
      currentBlock = pCurrentBlock;
    }

    public CFANode getNode() {
      return node;
    }

    public CompoundStatement getCurrentBlock() {
      return currentBlock;
    }
  }

  private final List<String> globalDefinitionsList = new ArrayList<>();
  private final ListMultimap<CFANode, Statement> createdStatements = ArrayListMultimap.create();
  private Collection<FunctionBody> functions;

  private Set<CFANode> unhandledSinceLastProgress = new HashSet<>();

  public String translateCfa(CFA pCfa) throws CPAException, InvalidConfigurationException {
    functions = new ArrayList<>(pCfa.getNumberOfFunctions());

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be written to C for C programs, at the moment");
    }
    for (FunctionEntryNode func : pCfa.getAllFunctionHeads()) {
      translate((CFunctionEntryNode) func);
    }

    return generateCCode();
  }

  private String generateCCode() {
    StringBuilder buffer = new StringBuilder();

    for (String globalDef : globalDefinitionsList) {
      buffer.append(globalDef).append("\n");
    }
    buffer.append("\n");

    for (FunctionBody f : functions) {
      f.translateToCode(buffer, 0);
      buffer.append("\n");
    }

    return buffer.toString();
  }

  private void translate(CFunctionEntryNode pEntry) throws CPAException {
    // waitlist for the edges to be processed
    Deque<NodeAndBlock> waitlist = new ArrayDeque<>();

    FunctionBody f = startFunction(pEntry);
    functions.add(f);

    for (CFAEdge relevant : getRelevantEdges(pEntry)) {
      pushToWaitlist(waitlist, new NodeAndBlock(relevant.getSuccessor(), f.getFunctionBody()));
    }

    while (!waitlist.isEmpty()) {
      NodeAndBlock current = waitlist.poll();
      CFANode currentNode = current.getNode();

      if (!unhandledSinceLastProgress.contains(currentNode)) {
        boolean anyUnhandled = false;
        for (CFAEdge e : CFAUtils.enteringEdges(currentNode)) {
          if (!createdStatements.containsKey(e.getPredecessor())) {
            anyUnhandled = true;
            break;
          }
        }
        if (anyUnhandled) {
          pushToWaitlist(waitlist, current);
          unhandledSinceLastProgress.add(currentNode);
          continue;
        }
      }

      if (createdStatements.containsKey(currentNode)) {
        Statement gotoStatement = createGoto(currentNode, currentNode);
        current.getCurrentBlock().addStatement(gotoStatement);

      } else {

        Collection<NodeAndBlock> nextNodes = handleNode(currentNode, current.getCurrentBlock());
        for (NodeAndBlock next : nextNodes) {
          pushToWaitlist(waitlist, next);
        }
      }
      unhandledSinceLastProgress.clear();
    }
  }

  private Statement createGoto(CFANode pCurrentNode, CFANode pTarget) {
    String go = "goto " + createdStatements.get(pTarget).get(0).getLabel() + ";";
    return createSimpleStatement(pCurrentNode, go);
  }

  private ImmutableCollection<NodeAndBlock> handleNode(CFANode pNode, CompoundStatement pBlock)
      throws CPAException {
    ImmutableList.Builder<NodeAndBlock> nextOnes = ImmutableList.builder();

    if (pNode instanceof CFATerminationNode || pNode.getNumLeavingEdges() == 0) {
      pBlock.addStatement(createSimpleStatement(pNode, "abort();"));
      return ImmutableList.of();
    }

    Collection<Pair<CFAEdge, CompoundStatement>> outgoingEdges =
        handlePotentialBranching(pNode, pBlock);
    for (var p : outgoingEdges) {
      CFAEdge currentEdge = p.getFirst();
      CompoundStatement currentBlock = p.getSecond();

      String statement = translateSimpleEdge(currentEdge);
      if (!statement.isEmpty()) {
        pBlock.addStatement(createSimpleStatement(pNode, statement));
      }

      CFANode successor = getSuccessorNode(currentEdge);
      nextOnes.add(new NodeAndBlock(successor, currentBlock));
    }

    if (pBlock.isEmpty()) {
      // add placeholder
      pBlock.addStatement(createEmptyStatement(pNode));
    }
    return nextOnes.build();
  }

  private CFANode getSuccessorNode(CFAEdge pE) {
    if (pE.getEdgeType().equals(FunctionCallEdge)) {
      return ((CFunctionCallEdge) pE).getSummaryEdge().getSuccessor();
    } else {
      return pE.getSuccessor();
    }
  }

  private FunctionBody startFunction(CFunctionEntryNode pFunctionStartNode) {
    String lFunctionHeader =
        pFunctionStartNode.getFunctionDefinition().toASTString(NAMES_QUALIFIED).replace(";", "");
    return new FunctionBody(lFunctionHeader, createCompoundStatement(pFunctionStartNode, null));
  }

  private Collection<CFAEdge> getRelevantEdges(CFANode pNode) {
    return CFAUtils.leavingEdges(pNode)
        .filter(e -> !(e instanceof FunctionReturnEdge))
        .filter(e -> !(e instanceof CFunctionSummaryStatementEdge))
        .toList();
  }

  private ImmutableCollection<Pair<CFAEdge, CompoundStatement>> handlePotentialBranching(
      CFANode pNode, CompoundStatement pStartingBlock) {

    Collection<CFAEdge> outgoingEdges = getRelevantEdges(pNode);
    if (outgoingEdges.size() == 1) {
      CFAEdge edgeToChild = Iterables.getOnlyElement(outgoingEdges);

      if (edgeToChild instanceof CAssumeEdge) {
        throw new IllegalStateException("Assume-edge without counterpart in CFA: " + edgeToChild);
      }
      return ImmutableSet.of(Pair.of(edgeToChild, pStartingBlock));

    } else if (outgoingEdges.size() > 1) {
      // if there are more than one children, then this must be a branching
      assert outgoingEdges.size() == 2
          : "branches with more than two options not supported (was the program prepocessed with CIL?)";
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
            cond = "if (" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + ")";
          } else {
            cond = "if (!(" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + "))";
          }
        } else {
          // must be else-branch, second in list
          assert ifAndElseEdge.get(1) == currentEdge;
          cond = "else ";
        }

        // create a new block starting with this condition
        CompoundStatement newBlock = addIfStatementToBlock(pNode, pStartingBlock, cond);
        branches.add(Pair.of(currentEdge, newBlock));
      }
      return branches.build();
    }
    return ImmutableSet.of();
  }

  private List<CFAEdge> swapElements(List<CFAEdge> pListWithTwoElements) {
    checkArgument(pListWithTwoElements.size() == 2, "List must have exactly two arguments");

    List<CFAEdge> swapped = new ArrayList<>(2);
    swapped.add(pListWithTwoElements.get(1));
    swapped.add(pListWithTwoElements.get(0));
    return swapped;
  }

  private SimpleStatement createSimpleStatement(CFANode pNode, String pStatement) {
    SimpleStatement st = new SimpleStatement(pStatement);
    createdStatements.put(pNode, st);
    return st;
  }

  private CompoundStatement createCompoundStatement(CFANode pNode, CompoundStatement pOuterBlock) {
    CompoundStatement st = new CompoundStatement(pOuterBlock);
    createdStatements.put(pNode, st);
    return st;
  }

  private Statement createEmptyStatement(CFANode pNode) {
    Statement s = new EmptyStatement();
    createdStatements.put(pNode, s);
    return s;
  }

  private boolean getRealTruthAssumption(final CAssumeEdge assumption) {
    return assumption.isSwapped() != assumption.getTruthAssumption();
  }

  private void pushToWaitlist(Deque<NodeAndBlock> pWaitlist, NodeAndBlock pNodeAndBlock) {
    pWaitlist.offer(pNodeAndBlock);
  }

  private CompoundStatement addIfStatementToBlock(
      CFANode pNode, CompoundStatement block, String conditionCode) {
    block.addStatement(createSimpleStatement(pNode, conditionCode));
    CompoundStatement newBlock = createCompoundStatement(pNode, block);
    block.addStatement(newBlock);
    return newBlock;
  }

  private String getLabelCode(final String pLabelName) {
    return pLabelName + ":; ";
  }

  private String translateSimpleEdge(CFAEdge pCFAEdge) throws CPAException {
    if (pCFAEdge == null) {
      return "";
    }

    switch (pCFAEdge.getEdgeType()) {
      case BlankEdge:
        {
          CFANode succ = pCFAEdge.getSuccessor();
          if (succ instanceof CLabelNode) {
              return getLabelCode(((CLabelNode) succ).getLabel());
          } else {
            // nothing to do
            break;
          }
        }

      case AssumeEdge:
        {
          // nothing to do
          break;
        }

      case StatementEdge:
      case ReturnStatementEdge:
        {
          String statementText = pCFAEdge.getCode();
          if (statementText.matches("^__CPAchecker_TMP_[0-9]+;?$")) {
            return ""; // ignore empty temporary variable statements;
          }
          return statementText + (statementText.endsWith(";") ? "" : ";");
        }

      case FunctionCallEdge:
        {
          String statement = ((CFunctionCallEdge) pCFAEdge).getSummaryEdge().getCode();
          return statement + (statement.endsWith(";") ? "" : ";");
        }

      case DeclarationEdge:
        {
          CDeclarationEdge lDeclarationEdge = (CDeclarationEdge) pCFAEdge;
          String declaration;
          // TODO adapt if String in
          // org.sosy_lab.cpachecker.cfa.parser.eclipse.c.ASTConverter#createInitializedTemporaryVariable is changed
          if (lDeclarationEdge
              .getDeclaration()
              .toASTString(NAMES_QUALIFIED)
              .contains("__CPAchecker_TMP_")) {
            declaration = lDeclarationEdge.getDeclaration().toASTString(NAMES_QUALIFIED);
          } else {
            // TODO check if works without lDeclarationEdge.getRawStatement();
            declaration = lDeclarationEdge.getDeclaration().toASTString(NAMES_QUALIFIED);

            if (lDeclarationEdge.getDeclaration() instanceof CVariableDeclaration) {
              CVariableDeclaration varDecl =
                  (CVariableDeclaration) lDeclarationEdge.getDeclaration();
              if (varDecl.getType() instanceof CArrayType
                  && varDecl.getInitializer() instanceof CInitializerExpression) {
                int assignAfterPos = declaration.indexOf("=") + 1;
                declaration =
                    declaration.substring(0, assignAfterPos)
                        + "{"
                        + declaration.substring(assignAfterPos, declaration.lastIndexOf(";"))
                        + "};";
              }
            }

            if (declaration.contains(",")) {
              for (CFAEdge predEdge : CFAUtils.enteringEdges(pCFAEdge.getPredecessor())) {
                if (predEdge
                    .getRawStatement()
                    .equals(lDeclarationEdge.getDeclaration().toASTString())) {
                  declaration = "";
                  break;
                }
              }
            }
          }

          if (declaration.contains("org.eclipse.cdt.internal.core.dom.parser.ProblemType@")) {
            throw new CPAException(
                "Failed to translate CFA into program because a type could not be properly resolved.");
          }

          if (lDeclarationEdge.getDeclaration().isGlobal()) {
            globalDefinitionsList.add(declaration + (declaration.endsWith(";") ? "" : ";"));
          } else {
            return declaration;
          }

          break;
        }

      case CallToReturnEdge:
        {
          //          this should not have been taken
          throw new AssertionError("CallToReturnEdge in path: " + pCFAEdge);
        }

      default:
        {
          throw new AssertionError(
              "Unexpected edge " + pCFAEdge + " of type " + pCFAEdge.getEdgeType());
        }
    }

    return "";
  }
}
