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

import static org.sosy_lab.cpachecker.cfa.model.CFAEdgeType.FunctionCallEdge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
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

    public CFANode getSuccessorNode() {
      return cfaEdge.getSuccessor();
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
      CFANode labelNode = labelNodes.get(l);
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
      EdgeAndBlock nextEdge = waitlist.pop();
      handleEdge(nextEdge, waitlist, pRelevantEdges);
    }
  }

  private FunctionBody startFunction(CFunctionEntryNode pFunctionStartNode) {
    String lFunctionHeader =
        pFunctionStartNode.getFunctionDefinition().toASTString(NAMES_QUALIFIED).replace(";", "");
    return new FunctionBody(lFunctionHeader, createCompoundStatement(pFunctionStartNode));
  }

  private void getRelevantEdgesOfElement(
      CFANode currentElement,
      Deque<EdgeAndBlock> waitlist,
      CompoundStatement currentBlock,
      @Nullable Collection<CFAEdge> pRelevantEdges) {
    discoveredElements.add(currentElement);
    if (!createdStatements.containsKey(currentElement)) {
      Statement placeholder = new EmptyStatement();
      createdStatements.put(currentElement, placeholder);
      currentBlock.addStatement(placeholder);
    }

    // find the next elements to add to the waitlist
    Collection<CFAEdge> outgoingEdges =
        CFAUtils.leavingEdges(currentElement)
            .filter(e -> !(e instanceof FunctionReturnEdge))
            .filter(e -> pRelevantEdges == null || pRelevantEdges.contains(e))
            .toList();

    if (outgoingEdges.size() == 1) {
      // get the next edge, process it using the same stack and add it to the waitlist
      CFAEdge edgeToChild = Iterables.getOnlyElement(outgoingEdges);

      if (edgeToChild instanceof CAssumeEdge) {
        // due to some reason the other edge is not considered

        // if part
        CAssumeEdge assumeEdge = (CAssumeEdge) edgeToChild;
        // create a new block starting with this condition
        boolean truthAssumptionInProgram = getRealTruthAssumption(assumeEdge);

        String cond;
        if (truthAssumptionInProgram == assumeEdge.getTruthAssumption()) {
          cond = "if (" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + ")";
        } else {
          cond = "if (!(" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + "))";
        }

        CompoundStatement newBlock = addIfStatement(currentElement, currentBlock, cond);

        if (truthAssumptionInProgram) {
          pushToWaitlist(waitlist, edgeToChild, newBlock);
        } else {
          pushToWaitlist(
              waitlist,
              edgeToChild.getPredecessor().getLeavingEdge(0) == edgeToChild
                  ? edgeToChild.getPredecessor().getLeavingEdge(1)
                  : edgeToChild.getPredecessor().getLeavingEdge(0),
              newBlock);
        }

        // else part
        newBlock = addIfStatement(currentElement, currentBlock, "else ");

        if (truthAssumptionInProgram) {
          pushToWaitlist(
              waitlist,
              edgeToChild.getPredecessor().getLeavingEdge(0) == edgeToChild
                  ? edgeToChild.getPredecessor().getLeavingEdge(1)
                  : edgeToChild.getPredecessor().getLeavingEdge(0),
              newBlock);
        } else {
          pushToWaitlist(waitlist, edgeToChild, newBlock);
        }

      } else {
        pushToWaitlist(waitlist, edgeToChild, currentBlock);
      }
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
          newBlock = createCompoundStatement(currentElement);
          elseCond = cond;
        } else {
          newBlock = addIfStatement(currentElement, currentBlock, cond);
        }

        if (truthAssumption && elseCond != null) {
          currentBlock.addStatement(createSimpleStatement(currentElement, elseCond));
          currentBlock.addStatement(result.get(0).getCurrentBlock());
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

  private SimpleStatement createSimpleStatement(CFANode pNode, String pStatement) {
    SimpleStatement st = new SimpleStatement(pStatement);
    createdStatements.put(pNode, st);
    return st;
  }

  private CompoundStatement createCompoundStatement(CFANode pNode) {
    CompoundStatement st = new CompoundStatement();
    createdStatements.put(pNode, st);
    return st;
  }

  private boolean getRealTruthAssumption(final CAssumeEdge assumption) {
    return assumption.isSwapped() != assumption.getTruthAssumption();
  }

  private void pushToWaitlist(
      Deque<EdgeAndBlock> pWaitlist, CFAEdge pEdgeToChild, CompoundStatement pCurrentBlock) {
    pWaitlist.push(new EdgeAndBlock(pEdgeToChild, pCurrentBlock));
  }

  private CompoundStatement addIfStatement(
      CFANode pNode, CompoundStatement block, String conditionCode) {
    block.addStatement(createSimpleStatement(pNode, conditionCode));
    CompoundStatement newBlock = createCompoundStatement(pNode);
    block.addStatement(newBlock);
    return newBlock;
  }

  private String getLabelCode(final String pLabelName) {
    return pLabelName + ":; ";
  }

  private void handleEdge(
      EdgeAndBlock nextEdge, Deque<EdgeAndBlock> waitlist, Collection<CFAEdge> relevantEdges)
      throws CPAException {
    CFAEdge edge = nextEdge.getCfaEdge();
    CompoundStatement currentBlock = nextEdge.getCurrentBlock();

    processEdge(edge, currentBlock);

    //    if (childElement.getParents().size() > 1) {
    //      mergeElements.add(childElement);
    //    }

    CFANode childElement;
    if (edge.getEdgeType().equals(FunctionCallEdge)) {
      childElement = ((CFunctionCallEdge) edge).getSummaryEdge().getSuccessor();
    } else {
      childElement = nextEdge.getSuccessorNode();
    }
    if (!discoveredElements.contains(childElement)) {
      if (childElement instanceof CFATerminationNode) {
        currentBlock.addStatement(createSimpleStatement(childElement, "abort();"));
      }
      // this element was not already processed; find children of it
      getRelevantEdgesOfElement(childElement, waitlist, currentBlock, relevantEdges);
    } else {
      String label = getLabel(childElement, edge);
      String gotoStatement = "goto " + label + ";";
      currentBlock.addStatement(createSimpleStatement(childElement, gotoStatement));
    }
  }

  private String getLabel(CFANode pNode, CFAEdge pEdgeToIgnore) {
    if (pNode instanceof CLabelNode) {
      return ((CLabelNode) pNode).getLabel();
    } else {
      com.google.common.base.Optional<CFAEdge> maybeBlankPredecessor = CFAUtils.enteringEdges(pNode)
          .firstMatch(x -> !x.equals(pEdgeToIgnore) && x.getEdgeType() == CFAEdgeType.BlankEdge);
      if (maybeBlankPredecessor.isPresent()) {
        return getLabel(maybeBlankPredecessor.get().getPredecessor(), pEdgeToIgnore);
      } else {
        return createdStatements.get(pNode).get(0).getLabel();
      }
    }
  }

  private void processEdge(CFAEdge edge, CompoundStatement currentBlock) throws CPAException {
    String statement = processSimpleEdge(edge);
    if (!statement.isEmpty()) {
      currentBlock.addStatement(createSimpleStatement(edge.getPredecessor(), statement));
    }
  }

  private String processSimpleEdge(CFAEdge pCFAEdge) throws CPAException {
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
