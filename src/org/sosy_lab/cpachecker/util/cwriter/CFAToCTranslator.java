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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cfa.model.CFAEdgeType.FunctionCallEdge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.FunctionBody;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.SimpleStatement;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.Statement;

@Options(prefix = "cfa.exportToC")
public class CFAToCTranslator {

  // Use original, unqualified names for variables
  private static final boolean NAMES_QUALIFIED = false;

  private static class EmptyStatement extends Statement {

    @Override
    void translateToCode0(StringBuilder pBuffer, int pIndent) {
      // do nothing
    }
  }

  private static class EdgeAndBlock {
    private final CFAEdge cfaEdge;
    private final CompoundStatement currentBlock;

    public EdgeAndBlock(CFAEdge pCfaEdge, CompoundStatement pCurrentBlock) {
      cfaEdge = pCfaEdge;
      currentBlock = pCurrentBlock;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }

    public CompoundStatement getCurrentBlock() {
      return currentBlock;
    }
  }

  @Option(
      secure = true,
      name = "reduceToLabels",
      description =
          "Only write CFA parts syntactically reaching the labels listed in the given file."
              + " The file is expected to contain one label name per line.")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path whitelistLabelFile = null;

  private final List<String> globalDefinitionsList = new ArrayList<>();
  private final Set<CFANode> discoveredElements = new HashSet<>();
  private final ListMultimap<CFANode, Statement> createdStatements = ArrayListMultimap.create();
  private Collection<FunctionBody> functions;

  private CFAEdge noProgressSince = null;

  public CFAToCTranslator(final Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public String translateCfa(CFA pCfa)
      throws CPAException, InvalidConfigurationException, IOException {
    functions = new ArrayList<>(pCfa.getNumberOfFunctions());

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be written to C for C programs, at the moment");
    }

    Collection<CFAEdge> relevantEdges = null;
    if (whitelistLabelFile != null) {
      relevantEdges = getEdgesReachingLabels(pCfa, whitelistLabelFile);
    }

    for (FunctionEntryNode func : pCfa.getAllFunctionHeads()) {
      translate((CFunctionEntryNode) func, relevantEdges);
    }

    return generateCCode();
  }

  private Collection<CFAEdge> getEdgesReachingLabels(CFA pCfa, Path pWhitelistLabelFile)
      throws IOException {

    final List<String> labels = Files.readAllLines(pWhitelistLabelFile, Charset.defaultCharset());
    final Map<String, CFANode> labelNodes =
        pCfa.getAllNodes()
            .parallelStream()
            .filter(n -> n instanceof CLabelNode)
            .collect(Collectors.toMap(n -> ((CLabelNode) n).getLabel(), n -> n));

    final Set<CFAEdge> reachableNodes = new HashSet<>();
    for (String l : labels) {
      CFANode labelNode = checkNotNull(labelNodes.get(l));
      // a backwards traversal through the CFA, starting at the node of interest,
      // will give us all edges that can reach the node of interest.
      reachableNodes.addAll(
          CFATraversal.dfs()
              .backwards()
              .ignoreEdges(reachableNodes)
              .collectEdgesReachableFrom(labelNode));
    }

    return reachableNodes;
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

  private void translate(CFunctionEntryNode pEntry, @Nullable Collection<CFAEdge> pRelevantEdges)
      throws CPAException {
    // waitlist for the edges to be processed
    Deque<EdgeAndBlock> waitlist = new ArrayDeque<>();

    FunctionBody f = startFunction(pEntry);
    functions.add(f);

    getRelevantEdgesOfElement(pEntry, waitlist, f.getFunctionBody(), pRelevantEdges);

    while (!waitlist.isEmpty()) {
      EdgeAndBlock nextEdge = waitlist.poll();
      handleEdge(nextEdge, waitlist, pRelevantEdges);
    }
  }

  private FunctionBody startFunction(CFunctionEntryNode pFunctionStartNode) {
    String lFunctionHeader =
        pFunctionStartNode.getFunctionDefinition().toASTString(NAMES_QUALIFIED).replace(";", "");
    return new FunctionBody(lFunctionHeader, createCompoundStatement(null));
  }

  private void getRelevantEdgesOfElement(
      CFANode currentElement,
      Deque<EdgeAndBlock> waitlist,
      CompoundStatement currentBlock,
      @Nullable Collection<CFAEdge> pRelevantEdges) {
    discoveredElements.add(currentElement);

    // find the next elements to add to the waitlist
    Collection<CFAEdge> outgoingEdges =
        getRelevant(CFAUtils.allLeavingEdges(currentElement), pRelevantEdges).toList();

    if (currentElement instanceof CLabelNode) {
      String labelStmt = getLabelCode(getLabel(currentElement, Collections.emptySet()));
      currentBlock.addStatement(createSimpleStatement(currentElement, labelStmt));
    }

    if (outgoingEdges.size() == 1) {
      assert !(Iterables.getOnlyElement(outgoingEdges) instanceof CAssumeEdge)
          : "The worst case happened: A single assume edge!";
      pushToWaitlist(waitlist, Iterables.getLast(outgoingEdges), currentBlock);

    } else if (outgoingEdges.size() > 1) {
      // if there are more than one children, then this is a condition
      assert outgoingEdges.size() == 2
          : "branches with more than two options not supported yet (was the program prepocessed with CIL?)"; // TODO: why not btw?

      // collect edges of condition branch
      ArrayList<EdgeAndBlock> result = new ArrayList<>(2);
      int ind = 0;
      boolean previousTruthAssumption = false;
      String elseCond = null;
      for (CFAEdge edgeToChild : outgoingEdges) {
        assert edgeToChild instanceof CAssumeEdge
            : "something wrong: branch in ARG without condition: " + edgeToChild;
        CAssumeEdge assumeEdge = (CAssumeEdge) edgeToChild;
        boolean truthAssumption = getRealTruthAssumption(assumeEdge);

        String cond = "";

        if (truthAssumption) {
          if (assumeEdge.getTruthAssumption()) {
            cond = "if (" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + ")";
          } else {
            cond = "if (!(" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + "))";
          }
        } else {
          cond = "else ";
        }

        if (ind > 0 && truthAssumption == previousTruthAssumption) {
          throw new AssertionError(
              "Two assume edges with same truth value, thus, cannot generate C program from ARG.");
        }

        ind++;

        // create a new block starting with this condition
        CompoundStatement newBlock;
        if (ind == 1 && !truthAssumption) {
          newBlock = createCompoundStatement(currentBlock);
          elseCond = cond;
        } else {
          newBlock = addIfStatement(assumeEdge, currentBlock, cond);
        }

        if (truthAssumption && elseCond != null) {
          addStatement(currentBlock, createSimpleStatement(currentElement, elseCond));
          addStatement(currentBlock, result.get(0).getCurrentBlock());
        }

        EdgeAndBlock newEdge = new EdgeAndBlock(edgeToChild, newBlock);
        if (truthAssumption) {
          result.add(0, newEdge);
        } else {
          result.add(newEdge);
        }

        previousTruthAssumption = truthAssumption;
      }

      // add edges in reversed order to waitlist
      for (int i = result.size() - 1; i >= 0; i--) {
        EdgeAndBlock e = result.get(i);
        pushToWaitlist(waitlist, e.getCfaEdge(), e.getCurrentBlock());
      }
    }
  }

  private void addStatement(CompoundStatement pCurrentBlock, Statement pStatement) {
    assert !(pStatement instanceof SimpleStatement)
        || pStatement.getSurroundingBlock() == null
        : "Statement already assigned to some block: " + pStatement;
    pCurrentBlock.addStatement(pStatement);
    assert pStatement.getSurroundingBlock() == pCurrentBlock;
  }

  private SimpleStatement createSimpleStatement(CFANode pNode, String pStatement) {
    SimpleStatement st = new SimpleStatement(pStatement);
    createdStatements.put(pNode, st);
    return st;
  }

  private CompoundStatement createCompoundStatement(@Nullable CompoundStatement pOuterBlock) {
    CompoundStatement st;
    if (pOuterBlock != null) {
      st = new CompoundStatement(pOuterBlock);
    } else {
      st = new CompoundStatement();
    }
    //createdStatements.put(pNode, st);
    return st;
  }

  private boolean getRealTruthAssumption(final CAssumeEdge assumption) {
    return assumption.isSwapped() != assumption.getTruthAssumption();
  }

  private void pushToWaitlist(
      Deque<EdgeAndBlock> pWaitlist, CFAEdge pEdgeToChild, CompoundStatement pCurrentBlock) {
    pWaitlist.offer(new EdgeAndBlock(pEdgeToChild, pCurrentBlock));
  }

  private CompoundStatement addIfStatement(
      CFAEdge pConditionEdge, CompoundStatement block, String conditionCode) {
    addStatement(block, createSimpleStatement(pConditionEdge.getPredecessor(), conditionCode));
    CompoundStatement newBlock = createCompoundStatement(block);
    addStatement(block, newBlock);
    return newBlock;
  }

  private String getLabelCode(final String pLabelName) {
    return pLabelName + ":; ";
  }

  /**
   * Computes the block to continue with after a join. Returns <code>null</code> if computation
   * fails.
   */
  private @Nullable CompoundStatement getJoinBlock(
      CFANode pJoinNode, Collection<CFAEdge> pRelevantEdges) {
    FluentIterable<CFANode> predecessorNodes =
        getRelevant(CFAUtils.allEnteringEdges(pJoinNode), pRelevantEdges)
            .transform(e -> e.getPredecessor());
    if (predecessorNodes.anyMatch(n -> !createdStatements.containsKey(n))) {
      return null;
    }

    Set<CompoundStatement> blocksBeforePredecessor =
        predecessorNodes
            .transform(n -> createdStatements.get(n).get(0).getSurroundingBlock())
            .toSet();

    boolean madeProgress;
    do {
      madeProgress = false;
      Set<CompoundStatement> newBlocksBeforePredecessor = new HashSet<>();
      for (CompoundStatement s : blocksBeforePredecessor) {
        CompoundStatement surroundingBlock = s.getSurroundingBlock();
        boolean foundMatch = false;
        while (surroundingBlock != null) {
          for (CompoundStatement o : blocksBeforePredecessor) {
            if (o.equals(s)) {
              continue;
            }
            if (surroundingBlock.equals(o) || surroundingBlock.equals(o.getSurroundingBlock())) {
              foundMatch = true;
              newBlocksBeforePredecessor.add(surroundingBlock);
              break;
            }
          }
          surroundingBlock = surroundingBlock.getSurroundingBlock();
        }
        if (foundMatch) {
          madeProgress = true;
        } else {
          newBlocksBeforePredecessor.add(s);
        }
      }
      blocksBeforePredecessor = newBlocksBeforePredecessor;
    } while (madeProgress);
    if (blocksBeforePredecessor.size() == 1) {
      return Iterables.getOnlyElement(blocksBeforePredecessor);
    } else {
      return null;
    }
  }

  private boolean areAllEnteringEdgesHandled(CFANode pNode, Collection<CFAEdge> pRelevantEdges) {
    return pNode instanceof CFunctionEntryNode
        || pNode.isLoopStart()
        || getRelevant(CFAUtils.allEnteringEdges(pNode), pRelevantEdges)
            .allMatch(e -> createdStatements.containsKey(e.getPredecessor()));
  }

  private void handleEdge(
      EdgeAndBlock nextEdge, Deque<EdgeAndBlock> waitlist, Collection<CFAEdge> relevantEdges)
      throws CPAException {
    CFAEdge edge = nextEdge.getCfaEdge();
    CompoundStatement currentBlock = nextEdge.getCurrentBlock();

    CFANode predecessor = edge.getPredecessor();
    if (!areAllEnteringEdgesHandled(predecessor, relevantEdges)) {
      if (noProgressSince == null) {
        noProgressSince = edge;
      } else if (noProgressSince.equals(edge)) {
        throw new CPAException("No progress in C translation at edge " + edge);
      }
      waitlist.offer(nextEdge);
      return;
    }

    processEdge(edge, currentBlock);
    noProgressSince = null;

    //    if (childElement.getParents().size() > 1) {
    //      mergeElements.add(childElement);
    //    }

    assert !edge.getEdgeType().equals(FunctionCallEdge);
    CFANode childElement = edge.getSuccessor();
    if (!discoveredElements.contains(childElement)
        && areAllEnteringEdgesHandled(childElement, relevantEdges)) {

      CompoundStatement blockToContinueWith = currentBlock;
      if (getRelevant(CFAUtils.allEnteringEdges(childElement), relevantEdges).size() > 1) {
        CompoundStatement joinBlock = getJoinBlock(childElement, relevantEdges);
        if (joinBlock != null) {
          blockToContinueWith = joinBlock;
        }
      }

      if (childElement instanceof CFATerminationNode) {
        addStatement(blockToContinueWith, createSimpleStatement(childElement, "abort();"));
      }
      // this element was not already processed; find children of it
      getRelevantEdgesOfElement(childElement, waitlist, blockToContinueWith, relevantEdges);

    } else if (createdStatements.containsKey(childElement)) {
      String label = getLabel(childElement, ImmutableSet.of(edge));
      String gotoStatement = "goto " + label + ";";
      addStatement(currentBlock, createSimpleStatement(childElement, gotoStatement));
    }
  }

  private FluentIterable<CFAEdge> getRelevant(FluentIterable<CFAEdge> pEdges, Collection<CFAEdge> pRelevantEdges ) {
    return pEdges
        .filter(e -> pRelevantEdges == null || pRelevantEdges.contains(e))
        .filter(e -> e.getEdgeType() != CFAEdgeType.FunctionReturnEdge)
        .filter(e -> e.getEdgeType() != CFAEdgeType.FunctionCallEdge);
  }

  private String getLabel(CFANode pNode, Set<CFAEdge> pEdgesToIgnore) {
    if (pNode instanceof CLabelNode) {
      return ((CLabelNode) pNode).getLabel();
    } else {
      CompoundStatement targetBlock = createdStatements.get(pNode).get(0).getSurroundingBlock();
      com.google.common.base.Optional<CFANode> maybeBlankPredecessor = CFAUtils.allEnteringEdges(pNode)
          .filter(x -> !pEdgesToIgnore.contains(x))
          .filter(x -> x.getEdgeType() == CFAEdgeType.BlankEdge)
          .transform(x -> x.getPredecessor())
          .filter(createdStatements::containsKey)
          .filter(x -> createdStatements.get(x).get(0).getSurroundingBlock().equals(targetBlock))
          .first();
      if (maybeBlankPredecessor.isPresent()) {
        return getLabel(maybeBlankPredecessor.get(), pEdgesToIgnore);
      } else {
        return createdStatements.get(pNode).get(0).getLabel();
      }
    }
  }

  private void processEdge(CFAEdge edge, CompoundStatement currentBlock) throws CPAException {
    String statement = processSimpleEdge(edge);
    Statement s;
    if (statement.isEmpty()) {
      s = new EmptyStatement();
      createdStatements.put(edge.getPredecessor(), s);
    } else {
      s = createSimpleStatement(edge.getPredecessor(), statement);
    }
    addStatement(currentBlock, s);
  }

  private String processSimpleEdge(CFAEdge pCFAEdge) throws CPAException {
    if (pCFAEdge == null) {
      return "";
    }

    String stmt = "";
    if (pCFAEdge instanceof CFunctionSummaryEdge) {
      stmt += pCFAEdge.getCode();
      return stmt + (stmt.endsWith(";") ? "" : ";");
    }

    switch (pCFAEdge.getEdgeType()) {
      case BlankEdge:
        {
            // nothing to do
            break;
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
          if (!statementText.matches("^__CPAchecker_TMP_[0-9]+;?$")) {
            stmt += statementText + (statementText.endsWith(";") ? "" : ";");
          }
          break;
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
            assert stmt.isEmpty();
            globalDefinitionsList.add(declaration + (declaration.endsWith(";") ? "" : ";"));
          } else {
            stmt += declaration;
          }

          break;
        }

      case FunctionCallEdge:
      case CallToReturnEdge:
        {
          // this should not have been taken
          throw new AssertionError("Unexpected edge type: " + pCFAEdge.getEdgeType()
              + " for edge " + pCFAEdge);
        }

      default:
        {
          throw new AssertionError(
              "Unexpected edge " + pCFAEdge + " of type " + pCFAEdge.getEdgeType());
        }
    }

    return stmt;
  }
}
