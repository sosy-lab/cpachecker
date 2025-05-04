// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.cfa.model.CFAEdgeType.FunctionCallEdge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.EmptyStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.FunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.Statement.Label;
import org.sosy_lab.cpachecker.util.cwriter.Statement.SimpleStatement;

public abstract class AbstractToCTranslator {

  static class NodeAndBlock {
    private final CFANode node;
    private final CompoundStatement currentBlock;

    NodeAndBlock(CFANode pNode, CompoundStatement pCurrentBlock) {
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

  protected final List<String> globalDefinitionsList = new ArrayList<>();
  protected final ListMultimap<CFANode, Statement> createdStatements = ArrayListMultimap.create();
  protected Collection<FunctionDefinition> functions = new ArrayList<>();
  protected final TranslatorConfig config;

  public AbstractToCTranslator(Configuration pConfig) throws InvalidConfigurationException {
    config = new TranslatorConfig(pConfig);
  }

  protected String generateCCode() throws IOException {
    StringBuilder buffer = new StringBuilder();
    try (StatementWriter writer = StatementWriter.getWriter(buffer, config)) {

      for (String globalDef : globalDefinitionsList) {
        writer.write(globalDef);
      }
      for (FunctionDefinition f : functions) {
        f.accept(writer);
      }

      return buffer.toString();
    }
  }

  Statement createLabel(CFALabelNode pNode) {
    Statement s = new Label(pNode.getLabel());
    createdStatements.put(pNode, s);
    return s;
  }

  CompoundStatement getBlockToContinueWith(
      final CFANode pCurrentNode,
      final CompoundStatement pCurrentBlock,
      final Collection<NodeAndBlock> pEnteringBlocks) {

    if (CFAUtils.enteringEdges(pCurrentNode).size() <= 1
        || pEnteringBlocks == null
        || pEnteringBlocks.size() <= 1) {
      return pCurrentBlock;
    }

    // the current block is the last statement of the outer block and empty.
    // this only happens for empty else-statements.
    boolean isEmptyElseAtEndOfBlock =
        pCurrentBlock.isEmpty()
            && pEnteringBlocks.stream()
                .anyMatch(n -> isLastStatementOfBlock(pCurrentBlock, n.getCurrentBlock()));
    if (isEmptyElseAtEndOfBlock) {
      // eliminate an empty, unneeded block. this would just be unnecessary nesting
      return pCurrentBlock.getSurroundingBlock();

    } else if (isSameOuterBlockForAll(pEnteringBlocks)) {
      return Iterables.getFirst(pEnteringBlocks, null).getCurrentBlock().getSurroundingBlock();

    } else {
      return pCurrentBlock;
    }
  }

  private boolean isSameOuterBlockForAll(Collection<NodeAndBlock> pBlocks) {
    return Collections3.allElementsEqual(
        pBlocks.stream().map(n -> n.getCurrentBlock().getSurroundingBlock()));
  }

  private boolean isLastStatementOfBlock(CompoundStatement pStatement, CompoundStatement pBlock) {
    return pBlock.equals(pStatement.getSurroundingBlock()) && pBlock.getLast().equals(pStatement);
  }

  NodeAndBlock getNextElement(Deque<NodeAndBlock> pWaitlist) {
    final NodeAndBlock lastElement = pWaitlist.peekLast();
    NodeAndBlock current;
    do {
      current = pWaitlist.pollFirst();
      if (current == lastElement) {
        return current;
      }
      boolean allPredecessorsHandled =
          getPredecessorNodes(current.getNode()).stream()
              .anyMatch(n -> !createdStatements.containsKey(n));
      if (allPredecessorsHandled) {
        return current;
      }
      // if not used, re-schedule node at the end
      pWaitlist.offer(current);
    } while (current != lastElement);
    return current; // if no other fits, use last element
  }

  Statement createGoto(CFANode pCurrentNode, CFANode pTarget) {
    String go = "goto " + createdStatements.get(pTarget).get(0).getLabel() + ";";
    return createSimpleStatement(pCurrentNode, go);
  }

  CFANode getSuccessorNode(CFAEdge pE) {
    if (pE.getEdgeType().equals(FunctionCallEdge)) {
      return ((CFunctionCallEdge) pE).getReturnNode();
    } else {
      return pE.getSuccessor();
    }
  }

  FluentIterable<CFANode> getPredecessorNodes(CFANode pN) {
    FluentIterable<CFANode> predecessors =
        getRelevantEnteringEdges(pN).transform(CFAEdge::getPredecessor);
    if (pN.getEnteringSummaryEdge() != null) {
      predecessors = predecessors.append(pN.getEnteringSummaryEdge().getPredecessor());
    }
    return predecessors;
  }

  FluentIterable<CFAEdge> getRelevantLeavingEdges(CFANode pNode) {
    return CFAUtils.leavingEdges(pNode)
        .filter(e -> !(e instanceof FunctionReturnEdge))
        .filter(e -> !(e instanceof CFunctionSummaryStatementEdge));
  }

  FluentIterable<CFAEdge> getRelevantEnteringEdges(CFANode pNode) {
    return CFAUtils.enteringEdges(pNode)
        .filter(e -> !(e instanceof FunctionReturnEdge))
        .filter(e -> !(e instanceof CFunctionSummaryStatementEdge));
  }

  List<CFAEdge> swapElements(List<CFAEdge> pListWithTwoElements) {
    checkArgument(pListWithTwoElements.size() == 2, "List must have exactly two arguments");

    List<CFAEdge> swapped = new ArrayList<>(2);
    swapped.add(pListWithTwoElements.get(1));
    swapped.add(pListWithTwoElements.get(0));
    return swapped;
  }

  SimpleStatement createSimpleStatement(
      CFANode pNode, String pStatement, @Nullable CFAEdge pOrigin) {
    SimpleStatement st = new SimpleStatement(pOrigin, pStatement);
    createdStatements.put(pNode, st);
    return st;
  }

  SimpleStatement createSimpleStatement(CFANode pNode, String pStatement) {
    return createSimpleStatement(pNode, pStatement, null);
  }

  CompoundStatement createCompoundStatement(CFANode pNode, CompoundStatement pOuterBlock) {
    CompoundStatement st = new CompoundStatement(pOuterBlock);
    createdStatements.put(pNode, st);
    return st;
  }

  Statement createEmptyStatement(CFANode pNode) {
    Statement s = new EmptyStatement();
    createdStatements.put(pNode, s);
    return s;
  }

  boolean getRealTruthAssumption(final CAssumeEdge assumption) {
    return assumption.isSwapped() != assumption.getTruthAssumption();
  }

  // 'ignored' is not used, but that for-each is necessary to iterate over iterable
  @SuppressWarnings("unused")
  boolean hasMoreThanOneElement(final FluentIterable<?> pIterable) {
    int count = 0;
    for (Object ignored : pIterable) {
      count++;
      if (count > 1) {
        return false;
      }
    }
    return true;
  }

  CompoundStatement addIfStatementToBlock(
      CFANode pNode, CompoundStatement block, String conditionCode, CFAEdge pOrigin) {
    block.addStatement(createSimpleStatement(pNode, conditionCode, pOrigin));
    CompoundStatement newBlock = createCompoundStatement(pNode, block);
    block.addStatement(newBlock);
    return newBlock;
  }

  String translateSimpleEdge(CFAEdge pCFAEdge) throws CPAException {
    if (pCFAEdge == null) {
      return "";
    }

    switch (pCFAEdge.getEdgeType()) {
      case BlankEdge, AssumeEdge -> {
        // nothing to do
      }
      case StatementEdge, ReturnStatementEdge -> {
        String statementText = pCFAEdge.getCode();
        if (statementText.matches("^__CPAchecker_TMP_[0-9]+;?$")) {
          return ""; // ignore empty temporary variable statements;
        }
        return statementText + (statementText.endsWith(";") ? "" : ";");
      }
      case FunctionCallEdge -> {
        String statement = ((CFunctionCallEdge) pCFAEdge).getSummaryEdge().getCode();
        return statement + (statement.endsWith(";") ? "" : ";");
      }
      case DeclarationEdge -> {
        CDeclarationEdge lDeclarationEdge = (CDeclarationEdge) pCFAEdge;
        String declaration;
        // TODO adapt if String in
        // org.sosy_lab.cpachecker.cfa.parser.eclipse.c.ASTConverter#createInitializedTemporaryVariable is changed
        if (lDeclarationEdge
            .getDeclaration()
            .toASTString(AAstNodeRepresentation.DEFAULT)
            .contains("__CPAchecker_TMP_")) {
          declaration =
              lDeclarationEdge.getDeclaration().toASTString(AAstNodeRepresentation.DEFAULT);
        } else {
          // TODO check if works without lDeclarationEdge.getRawStatement();
          declaration =
              lDeclarationEdge.getDeclaration().toASTString(AAstNodeRepresentation.DEFAULT);

          if (lDeclarationEdge.getDeclaration() instanceof CVariableDeclaration) {
            CVariableDeclaration varDecl = (CVariableDeclaration) lDeclarationEdge.getDeclaration();
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
              "Failed to translate CFA into program because a type could not be properly"
                  + " resolved.");
        }

        if (lDeclarationEdge.getDeclaration().isGlobal()) {
          globalDefinitionsList.add(declaration + (declaration.endsWith(";") ? "" : ";"));
        } else {
          return declaration;
        }
      }
      case CallToReturnEdge ->
          //          this should not have been taken
          throw new AssertionError("CallToReturnEdge in path: " + pCFAEdge);
      default ->
          throw new AssertionError(
              "Unexpected edge " + pCFAEdge + " of type " + pCFAEdge.getEdgeType());
    }

    return "";
  }
}
