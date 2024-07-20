// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.cwriter.CFAToCTranslator;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class Sequentialization {

  private final Configuration config;

  private final MutableNetwork<CFANode, CFAEdge> mutableNetwork;

  private final CFAToCTranslator cfaToCTranslator;

  private final AFunctionDeclaration mainFunction;

  public Sequentialization(Configuration pConfig, AFunctionDeclaration pMainFunction)
      throws InvalidConfigurationException {
    config = pConfig;
    // TODO allowSelfLoops? should be false based on my current unterstanding
    mutableNetwork = NetworkBuilder.directed().allowsSelfLoops(true).build();
    cfaToCTranslator = new CFAToCTranslator(config);
    mainFunction = pMainFunction;
  }

  public String createCProgram() {
    return null;
    // TODO how do we get from a MutableNetwork to a CFA?
    //  MutableCFA only contains functions for adding nodes, not for adding edges
    //  CfaMutableNetwork takes only a MutableNetwork as a parameter
    //  CCfaTransformer.createCFA takes a CfaMutableNetwork and also other parameters
    //  return cfaToCTranslator.translateCfa(cfa...);
  }

  /**
   * Tries to add pNode to the {@link Sequentialization#mutableNetwork}.
   *
   * @param pNode CFANode to be added
   * @throws IllegalArgumentException if {@link Sequentialization#mutableNetwork} contains pNode
   *     already
   */
  public void addNode(CFANode pNode) {
    checkArgument(mutableNetwork.addNode(pNode), "mutableNetwork contains pNode already");
  }

  /**
   * Tries to add pEdge to the {@link Sequentialization#mutableNetwork}.
   *
   * @param pPredecessor CFANode whose leaving CFAEdges contains pEdge
   * @param pSuccessor CFANode whose entering CFAEdges contains pEdge
   * @param pEdge CFAEdge to be added
   * @throws IllegalArgumentException if {@link Sequentialization#mutableNetwork} contains a CFAEdge
   *     from pPredecessor to pSuccessor already
   */
  public void addEdge(CFANode pPredecessor, CFANode pSuccessor, CFAEdge pEdge) {
    checkArgument(
        mutableNetwork.addEdge(pPredecessor, pSuccessor, pEdge),
        "mutableNetwork contains a CFAEdge from pPredecessor to pSuccessor already");
  }

  /**
   * TODO
   *
   * @param pNode TODO
   * @return TODO
   */
  public CFANode cloneNode(final CFANode pNode) {
    checkNotNull(pNode);

    CFANode rNewNode = getCastedClone(pNode);
    // copy information from original node
    rNewNode.setReversePostorderId(pNode.getReversePostorderId());
    if (pNode.isLoopStart()) {
      rNewNode.setLoopStart();
    }
    return rNewNode;
  }

  /**
   * TODO
   *
   * @param pNode TODO
   * @return TODO
   */
  private CFANode getCastedClone(final CFANode pNode) {
    if (pNode instanceof CFALabelNode labelNode) {
      return new CFALabelNode(mainFunction, labelNode.getLabel());

    } else if (pNode instanceof CFATerminationNode) {
      return new CFATerminationNode(mainFunction);

    } else if (pNode instanceof FunctionExitNode) {
      return new FunctionExitNode(mainFunction);

    } else if (pNode instanceof CFunctionEntryNode cFunctionEntryNode) {
      return new CFunctionEntryNode(
          cFunctionEntryNode.getFileLocation(),
          cFunctionEntryNode.getFunctionDefinition(),
          cFunctionEntryNode.getExitNode().orElse(null),
          cFunctionEntryNode.getReturnVariable());
    }
    checkArgument(
        pNode.getClass() == CFANode.class, "unhandled subclass for CFANode: " + pNode.getClass());
    return new CFANode(mainFunction);
  }

  /**
   * TODO
   *
   * @param pEdge TODO
   * @param pPredecessor TODO
   * @param pSuccessor TODO
   * @return TODO
   */
  public CFAEdge cloneEdge(
      final CFAEdge pEdge, final CFANode pPredecessor, final CFANode pSuccessor) {

    checkNotNull(pEdge);
    checkNotNull(pPredecessor);
    checkNotNull(pSuccessor);

    return getCastedClone(pEdge, pPredecessor, pSuccessor);
  }

  /**
   * TODO
   *
   * @param pEdge TODO
   * @param pPredecessor TODO
   * @param pSuccessor TODO
   * @return TODO
   */
  private CFAEdge getCastedClone(
      final CFAEdge pEdge, final CFANode pPredecessor, final CFANode pSuccessor) {

    final FileLocation fileLocation = pEdge.getFileLocation();
    final String rawStatement = pEdge.getRawStatement();

    switch (pEdge.getEdgeType()) {
      case BlankEdge:
        return new BlankEdge(
            rawStatement, fileLocation, pPredecessor, pSuccessor, pEdge.getDescription());

      case AssumeEdge:
        checkArgument(pEdge instanceof CAssumeEdge);
        CAssumeEdge cAssumeEdge = (CAssumeEdge) pEdge;
        return new CAssumeEdge(
            rawStatement,
            fileLocation,
            pPredecessor,
            pSuccessor,
            cAssumeEdge.getExpression(),
            cAssumeEdge.getTruthAssumption(),
            cAssumeEdge.isSwapped(),
            cAssumeEdge.isArtificialIntermediate());

      case StatementEdge:
        checkArgument(pEdge instanceof CStatementEdge);
        CStatementEdge cStatementEdge = (CStatementEdge) pEdge;
        return new CStatementEdge(
            rawStatement, cStatementEdge.getStatement(), fileLocation, pPredecessor, pSuccessor);

      case DeclarationEdge:
        checkArgument(pEdge instanceof CDeclarationEdge);
        CDeclarationEdge cDeclarationEdge = (CDeclarationEdge) pEdge;
        return new CDeclarationEdge(
            rawStatement,
            fileLocation,
            pPredecessor,
            pSuccessor,
            cDeclarationEdge.getDeclaration());

      case ReturnStatementEdge:
        checkArgument(pEdge instanceof CReturnStatementEdge);
        checkArgument(pSuccessor instanceof FunctionExitNode);
        CReturnStatementEdge cReturnStatementEdge = (CReturnStatementEdge) pEdge;
        return new CReturnStatementEdge(
            rawStatement,
            cReturnStatementEdge.getReturnStatement(),
            fileLocation,
            pPredecessor,
            (FunctionExitNode) pSuccessor);

      case FunctionCallEdge:
        checkArgument(pEdge instanceof CFunctionCallEdge);
        checkArgument(pSuccessor instanceof CFunctionEntryNode);
        CFunctionCallEdge cFunctionCallEdge = (CFunctionCallEdge) pEdge;
        return new CFunctionCallEdge(
            rawStatement,
            fileLocation,
            pPredecessor,
            (CFunctionEntryNode) pSuccessor,
            cFunctionCallEdge.getFunctionCall(),
            cFunctionCallEdge.getSummaryEdge());

      case FunctionReturnEdge:
        checkArgument(pEdge instanceof CFunctionReturnEdge);
        checkArgument(pPredecessor instanceof FunctionExitNode);
        CFunctionReturnEdge cFunctionReturnEdge = (CFunctionReturnEdge) pEdge;
        return new CFunctionReturnEdge(
            fileLocation,
            (FunctionExitNode) pPredecessor,
            pSuccessor,
            cFunctionReturnEdge.getSummaryEdge());

      case CallToReturnEdge:
        checkArgument(pEdge instanceof CFunctionSummaryEdge);
        CFunctionSummaryEdge cFunctionSummaryEdge = (CFunctionSummaryEdge) pEdge;
        return new CFunctionSummaryEdge(
            rawStatement,
            fileLocation,
            pPredecessor,
            pSuccessor,
            cFunctionSummaryEdge.getExpression(),
            cFunctionSummaryEdge.getFunctionEntry());

      default:
        throw new IllegalArgumentException("unhandled edge type " + pEdge.getEdgeType());
    }
  }
}
