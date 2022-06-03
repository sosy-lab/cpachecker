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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.EmptyStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.FunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.Statement.Label;
import org.sosy_lab.cpachecker.util.cwriter.Statement.SimpleStatement;

public class CFAToCTranslator {

  // Use original, unqualified names for variables
  private static final boolean NAMES_QUALIFIED = false;

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
  private Collection<FunctionDefinition> functions;
  private final TranslatorConfig config;

  public CFAToCTranslator(Configuration pConfig) throws InvalidConfigurationException {
    config = new TranslatorConfig(pConfig);
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

    for (FunctionEntryNode func : pCfa.getAllFunctionHeads()) {
      translate((CFunctionEntryNode) func);
    }

    return generateCCode();
  }

  private String generateCCode() throws IOException {
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

  private void translate(CFunctionEntryNode pEntry) throws CPAException {
    // waitlist for the edges to be processed
    Deque<NodeAndBlock> waitlist = new ArrayDeque<>();
    Multimap<CFANode, NodeAndBlock> ingoingBlocks = HashMultimap.create();

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
            getBlockToContinueWith(currentNode, originalBlock, ingoingBlocks.get(currentNode));
        // create new NodeAndBlock because the block may have changed from the start of the loop
        if (currentBlock != originalBlock) {
          current = new NodeAndBlock(currentNode, currentBlock);
        }

        Collection<NodeAndBlock> nextNodes = handleNode(currentNode, currentBlock);
        for (NodeAndBlock next : nextNodes) {
          CFANode nextNode = next.getNode();
          ingoingBlocks.put(nextNode, current);
          pushToWaitlist(waitlist, next);
        }
      }
    }
  }

  private Statement createLabel(CFALabelNode pNode) {
    Statement s = new Label(pNode.getLabel());
    createdStatements.put(pNode, s);
    return s;
  }

  private CompoundStatement getBlockToContinueWith(
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

  private NodeAndBlock getNextElement(Deque<NodeAndBlock> pWaitlist) {
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

  private CFANode getSuccessorNode(CFAEdge pE) {
    if (pE.getEdgeType().equals(FunctionCallEdge)) {
      return ((CFunctionCallEdge) pE).getSummaryEdge().getSuccessor();
    } else {
      return pE.getSuccessor();
    }
  }

  private FluentIterable<CFANode> getPredecessorNodes(CFANode pN) {
    FluentIterable<CFANode> predecessors =
        getRelevantEnteringEdges(pN).transform(e -> e.getPredecessor());
    if (pN.getEnteringSummaryEdge() != null) {
      predecessors = predecessors.append(pN.getEnteringSummaryEdge().getPredecessor());
    }
    return predecessors;
  }

  private FunctionDefinition startFunction(CFunctionEntryNode pFunctionStartNode) {
    String lFunctionHeader =
        pFunctionStartNode.getFunctionDefinition().toASTString(NAMES_QUALIFIED).replace(";", "");
    return new FunctionDefinition(
        lFunctionHeader, createCompoundStatement(pFunctionStartNode, null));
  }

  private FluentIterable<CFAEdge> getRelevantLeavingEdges(CFANode pNode) {
    return CFAUtils.leavingEdges(pNode)
        .filter(e -> !(e instanceof FunctionReturnEdge))
        .filter(e -> !(e instanceof CFunctionSummaryStatementEdge));
  }

  private FluentIterable<CFAEdge> getRelevantEnteringEdges(CFANode pNode) {
    return CFAUtils.enteringEdges(pNode)
        .filter(e -> !(e instanceof FunctionReturnEdge))
        .filter(e -> !(e instanceof CFunctionSummaryStatementEdge));
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
            cond = "if (" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + ")";
          } else {
            cond = "if (!(" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + "))";
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

  private List<CFAEdge> swapElements(List<CFAEdge> pListWithTwoElements) {
    checkArgument(pListWithTwoElements.size() == 2, "List must have exactly two arguments");

    List<CFAEdge> swapped = new ArrayList<>(2);
    swapped.add(pListWithTwoElements.get(1));
    swapped.add(pListWithTwoElements.get(0));
    return swapped;
  }

  private SimpleStatement createSimpleStatement(
      CFANode pNode, String pStatement, @Nullable CFAEdge pOrigin) {
    SimpleStatement st = new SimpleStatement(pOrigin, pStatement);
    createdStatements.put(pNode, st);
    return st;
  }

  private SimpleStatement createSimpleStatement(CFANode pNode, String pStatement) {
    return createSimpleStatement(pNode, pStatement, null);
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

  // 'ignored' is not used, but that for-each is necessary to iterate over iterable
  @SuppressWarnings("unused")
  private boolean hasMoreThanOneElement(final FluentIterable<?> pIterable) {
    int count = 0;
    for (Object ignored : pIterable) {
      count++;
      if (count > 1) {
        return false;
      }
    }
    return true;
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

  private CompoundStatement addIfStatementToBlock(
      CFANode pNode, CompoundStatement block, String conditionCode, CFAEdge pOrigin) {
    block.addStatement(createSimpleStatement(pNode, conditionCode, pOrigin));
    CompoundStatement newBlock = createCompoundStatement(pNode, block);
    block.addStatement(newBlock);
    return newBlock;
  }

  private String translateSimpleEdge(CFAEdge pCFAEdge) throws CPAException {
    if (pCFAEdge == null) {
      return "";
    }

    switch (pCFAEdge.getEdgeType()) {
      case BlankEdge:
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
                "Failed to translate CFA into program because a type could not be properly"
                    + " resolved.");
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
