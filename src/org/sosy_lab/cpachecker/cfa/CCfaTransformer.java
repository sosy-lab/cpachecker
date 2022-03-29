// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetworks;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdgeVisitor;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;

public final class CCfaTransformer extends CfaTransformer {

  private final ImmutableList<CfaProcessor> cfaProcessors;

  private final ImmutableList<NodeAstSubstitution> nodeAstSubstitutions;
  private final ImmutableList<EdgeAstSubstitution> edgeAstSubstitutions;

  private CCfaTransformer(
      ImmutableList<CfaProcessor> pCfaProcessors,
      ImmutableList<NodeAstSubstitution> pNodeAstSubstitutions,
      ImmutableList<EdgeAstSubstitution> pEdgeAstSubstitutions) {

    cfaProcessors = pCfaProcessors;

    nodeAstSubstitutions = pNodeAstSubstitutions;
    edgeAstSubstitutions = pEdgeAstSubstitutions;
  }

  public static Builder builder() {
    return new Builder();
  }

  private CFunctionDeclaration applyNodeAstSubstitutions(
      CFANode pNode, CFunctionDeclaration pFunction) {

    CFunctionDeclaration function = pFunction;
    for (NodeAstSubstitution nodeAstSubstitution : nodeAstSubstitutions) {
      function = nodeAstSubstitution.apply(pNode, function);
    }

    return function;
  }

  private Optional<CVariableDeclaration> applyNodeAstSubstitutions(
      CFunctionEntryNode pFunctionEntryNode, Optional<CVariableDeclaration> pReturnVariable) {

    Optional<CVariableDeclaration> returnVariable = pReturnVariable;
    for (NodeAstSubstitution nodeAstSubstitution : nodeAstSubstitutions) {
      returnVariable = nodeAstSubstitution.apply(pFunctionEntryNode, returnVariable);
    }

    return returnVariable;
  }

  private CAstNode applyEdgeAstSubstitutions(CFAEdge pEdge, CAstNode pAstNode) {

    CAstNode astNode = pAstNode;
    for (EdgeAstSubstitution edgeAstSubstitution : edgeAstSubstitutions) {
      astNode = edgeAstSubstitution.apply(pEdge, astNode);
    }

    return astNode;
  }

  private @Nullable CFANode convertNode(
      CFANode pNode, CfaNetwork pCfaNetwork, CfaTransformer.Substitution pSubstitution) {
    return new NodeConverter(pCfaNetwork, pSubstitution).convert(pNode);
  }

  private CFAEdge convertEdge(
      CFAEdge pEdge,
      CfaNetwork pCfaNetwork,
      CfaTransformer.Substitution pSubstitution,
      CfaConnectedness pConnectedness) {

    EndpointPair<CFANode> oldEndpoints = pCfaNetwork.incidentNodes(pEdge);

    CFANode newPredecessor = pSubstitution.toSubstitute(oldEndpoints.source());
    CFANode newSuccessor = pSubstitution.toSubstitute(oldEndpoints.target());

    var transformingEdgeVisitor =
        new TransformingCCfaEdgeVisitor(
            pCfaNetwork, pSubstitution, pConnectedness, newPredecessor, newSuccessor);

    return ((CCfaEdge) pEdge).accept(transformingEdgeVisitor);
  }

  @Override
  public CFA transform(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata, LogManager pLogger) {
    return createCfa(
        pCfaNetwork, pCfaMetadata, pLogger, cfaProcessors, this::convertNode, this::convertEdge);
  }

  public interface NodeAstSubstitution {

    CFunctionDeclaration apply(CFANode pNode, CFunctionDeclaration pFunction);

    Optional<CVariableDeclaration> apply(
        CFunctionEntryNode pFunctionEntryNode, Optional<CVariableDeclaration> pReturnVariable);
  }

  @FunctionalInterface
  public interface EdgeAstSubstitution {

    CAstNode apply(CFAEdge pEdge, CAstNode pAstNode);
  }

  public static final class Builder {

    private final ImmutableList.Builder<CfaProcessor> cfaProcessors;

    private final ImmutableList.Builder<NodeAstSubstitution> nodeAstSubstitutions;
    private final ImmutableList.Builder<EdgeAstSubstitution> edgeAstSubstitutions;

    private Builder() {

      cfaProcessors = ImmutableList.builder();

      nodeAstSubstitutions = ImmutableList.builder();
      edgeAstSubstitutions = ImmutableList.builder();
    }

    public Builder addCfaProcessor(CfaProcessor pCfaProcessor) {

      cfaProcessors.add(pCfaProcessor);

      return this;
    }

    public Builder addNodeAstSubstitution(NodeAstSubstitution pNodeAstSubstitution) {

      nodeAstSubstitutions.add(pNodeAstSubstitution);

      return this;
    }

    public Builder addEdgeAstSubstitution(EdgeAstSubstitution pEdgeAstSubstitution) {

      edgeAstSubstitutions.add(pEdgeAstSubstitution);

      return this;
    }

    public CfaTransformer build() {
      return new CCfaTransformer(
          cfaProcessors.build(), nodeAstSubstitutions.build(), edgeAstSubstitutions.build());
    }
  }

  private final class NodeConverter {

    private final CfaNetwork cfaNetwork;
    private final CfaTransformer.Substitution substitution;

    private NodeConverter(CfaNetwork pCfaNetwork, CfaTransformer.Substitution pSubstitution) {
      cfaNetwork = pCfaNetwork;
      substitution = pSubstitution;
    }

    private CFunctionDeclaration newFunctionDeclaration(CFANode pOldNode) {
      return applyNodeAstSubstitutions(pOldNode, (CFunctionDeclaration) pOldNode.getFunction());
    }

    private CFALabelNode newCfaLabelNode(CFALabelNode pOldNode) {
      return new CFALabelNode(newFunctionDeclaration(pOldNode), pOldNode.getLabel());
    }

    private CFunctionEntryNode newCFunctionEntryNode(CFunctionEntryNode pOldNode) {

      FunctionExitNode oldExitNode =
          CfaNetworks.getFunctionExitNode(cfaNetwork, pOldNode).orElse(pOldNode.getExitNode());
      FunctionExitNode newExitNode = (FunctionExitNode) substitution.toSubstitute(oldExitNode);

      Optional<CVariableDeclaration> newReturnVariable =
          applyNodeAstSubstitutions(pOldNode, pOldNode.getReturnVariable());

      CFunctionEntryNode newEntryNode =
          new CFunctionEntryNode(
              pOldNode.getFileLocation(),
              newFunctionDeclaration(pOldNode),
              newExitNode,
              newReturnVariable);
      newExitNode.setEntryNode(newEntryNode);

      return newEntryNode;
    }

    private FunctionExitNode newFunctionExitNode(FunctionExitNode pOldNode) {
      return new FunctionExitNode(newFunctionDeclaration(pOldNode));
    }

    private CFATerminationNode newCfaTerminationNode(CFATerminationNode pOldNode) {
      return new CFATerminationNode(newFunctionDeclaration(pOldNode));
    }

    private CFANode newCfaNode(CFANode pOldNode) {
      return new CFANode(newFunctionDeclaration(pOldNode));
    }

    private CFANode convert(CFANode pOldNode) {

      if (pOldNode instanceof CFALabelNode) {
        return newCfaLabelNode((CFALabelNode) pOldNode);
      } else if (pOldNode instanceof CFunctionEntryNode) {
        return newCFunctionEntryNode((CFunctionEntryNode) pOldNode);
      } else if (pOldNode instanceof FunctionExitNode) {
        return newFunctionExitNode((FunctionExitNode) pOldNode);
      } else if (pOldNode instanceof CFATerminationNode) {
        return newCfaTerminationNode((CFATerminationNode) pOldNode);
      } else {
        return newCfaNode(pOldNode);
      }
    }
  }

  private final class TransformingCCfaEdgeVisitor
      implements CCfaEdgeVisitor<@Nullable CFAEdge, NoException> {

    private final CfaNetwork cfaNetwork;
    private final CfaTransformer.Substitution substitution;
    private final CfaConnectedness connectedness;

    private final CFANode newPredecessor;
    private final CFANode newSuccessor;

    private TransformingCCfaEdgeVisitor(
        CfaNetwork pCfaNetwork,
        CfaTransformer.Substitution pSubstitution,
        CfaConnectedness pConnectedness,
        CFANode pNewPredecessor,
        CFANode pNewSuccessor) {

      cfaNetwork = pCfaNetwork;
      substitution = pSubstitution;
      connectedness = pConnectedness;

      newPredecessor = pNewPredecessor;
      newSuccessor = pNewSuccessor;
    }

    @Override
    public CFAEdge visit(BlankEdge pBlankEdge) {
      return new BlankEdge(
          pBlankEdge.getRawStatement(),
          pBlankEdge.getFileLocation(),
          newPredecessor,
          newSuccessor,
          pBlankEdge.getDescription());
    }

    @Override
    public CFAEdge visit(CAssumeEdge pCAssumeEdge) {

      CExpression newExpression =
          (CExpression) applyEdgeAstSubstitutions(pCAssumeEdge, pCAssumeEdge.getExpression());

      return new CAssumeEdge(
          pCAssumeEdge.getRawStatement(),
          pCAssumeEdge.getFileLocation(),
          newPredecessor,
          newSuccessor,
          newExpression,
          pCAssumeEdge.getTruthAssumption(),
          pCAssumeEdge.isSwapped(),
          pCAssumeEdge.isArtificialIntermediate());
    }

    @Override
    public CFAEdge visit(CDeclarationEdge pCDeclarationEdge) {

      CDeclaration newDeclaration =
          (CDeclaration)
              applyEdgeAstSubstitutions(pCDeclarationEdge, pCDeclarationEdge.getDeclaration());

      return new CDeclarationEdge(
          pCDeclarationEdge.getRawStatement(),
          pCDeclarationEdge.getFileLocation(),
          newPredecessor,
          newSuccessor,
          newDeclaration);
    }

    @Override
    public CFAEdge visit(CStatementEdge pCStatementEdge) {

      CStatement newStatement =
          (CStatement) applyEdgeAstSubstitutions(pCStatementEdge, pCStatementEdge.getStatement());

      return new CStatementEdge(
          pCStatementEdge.getRawStatement(),
          newStatement,
          pCStatementEdge.getFileLocation(),
          newPredecessor,
          newSuccessor);
    }

    @Override
    public @Nullable CFAEdge visit(CFunctionCallEdge pCFunctionCallEdge) {
      if (connectedness == CfaConnectedness.SUPERGRAPH) {

        FunctionSummaryEdge oldFunctionSummaryEdge =
            CfaNetworks.getFunctionSummaryEdge(cfaNetwork, pCFunctionCallEdge)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Missing FunctionSummaryEdge for FunctionCallEdge: "
                                + pCFunctionCallEdge));
        CFunctionSummaryEdge newFunctionSummaryEdge =
            (CFunctionSummaryEdge) substitution.toSubstitute(oldFunctionSummaryEdge);

        CFunctionCall newFunctionCall =
            (CFunctionCall)
                applyEdgeAstSubstitutions(pCFunctionCallEdge, pCFunctionCallEdge.getFunctionCall());

        return new CFunctionCallEdge(
            pCFunctionCallEdge.getRawStatement(),
            pCFunctionCallEdge.getFileLocation(),
            newPredecessor,
            (CFunctionEntryNode) newSuccessor,
            newFunctionCall,
            newFunctionSummaryEdge);
      } else {
        return null;
      }
    }

    @Override
    public @Nullable CFAEdge visit(CFunctionReturnEdge pCFunctionReturnEdge) {
      if (connectedness == CfaConnectedness.SUPERGRAPH) {

        FunctionSummaryEdge oldFunctionSummaryEdge =
            CfaNetworks.getFunctionSummaryEdge(cfaNetwork, pCFunctionReturnEdge)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Missing FunctionSummaryEdge for FunctionReturnEdge: "
                                + pCFunctionReturnEdge));
        CFunctionSummaryEdge newFunctionSummaryEdge =
            (CFunctionSummaryEdge) substitution.toSubstitute(oldFunctionSummaryEdge);

        return new CFunctionReturnEdge(
            pCFunctionReturnEdge.getFileLocation(),
            (FunctionExitNode) newPredecessor,
            newSuccessor,
            newFunctionSummaryEdge);
      } else {
        return null;
      }
    }

    @Override
    public CFAEdge visit(CFunctionSummaryEdge pCFunctionSummaryEdge) {

      CFunctionCall newFunctionCall =
          (CFunctionCall)
              applyEdgeAstSubstitutions(
                  pCFunctionSummaryEdge, pCFunctionSummaryEdge.getExpression());

      if (connectedness == CfaConnectedness.SUPERGRAPH) {

        FunctionEntryNode oldFunctionEntryNode =
            CfaNetworks.getFunctionEntryNode(cfaNetwork, pCFunctionSummaryEdge)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Missing FunctionEntryNode for FunctionSummaryEdge: "
                                + pCFunctionSummaryEdge));
        CFunctionEntryNode newFunctionEntryNode =
            (CFunctionEntryNode) substitution.toSubstitute(oldFunctionEntryNode);

        return new CFunctionSummaryEdge(
            pCFunctionSummaryEdge.getRawStatement(),
            pCFunctionSummaryEdge.getFileLocation(),
            newPredecessor,
            newSuccessor,
            newFunctionCall,
            newFunctionEntryNode);
      } else {
        return new CStatementEdge(
            pCFunctionSummaryEdge.getRawStatement(),
            newFunctionCall,
            pCFunctionSummaryEdge.getFileLocation(),
            pCFunctionSummaryEdge.getPredecessor(),
            pCFunctionSummaryEdge.getSuccessor());
      }
    }

    @Override
    public CFAEdge visit(CReturnStatementEdge pCReturnStatementEdge) {

      CReturnStatement newReturnStatement =
          (CReturnStatement)
              applyEdgeAstSubstitutions(
                  pCReturnStatementEdge, pCReturnStatementEdge.getReturnStatement());

      return new CReturnStatementEdge(
          pCReturnStatementEdge.getRawStatement(),
          newReturnStatement,
          pCReturnStatementEdge.getFileLocation(),
          newPredecessor,
          (FunctionExitNode) newSuccessor);
    }

    @Override
    public CFAEdge visit(CFunctionSummaryStatementEdge pCFunctionSummaryStatementEdge) {
      if (connectedness == CfaConnectedness.SUPERGRAPH) {

        CStatement newStatement =
            (CStatement)
                applyEdgeAstSubstitutions(
                    pCFunctionSummaryStatementEdge, pCFunctionSummaryStatementEdge.getStatement());
        CFunctionCall newFunctionCall =
            (CFunctionCall)
                applyEdgeAstSubstitutions(
                    pCFunctionSummaryStatementEdge,
                    pCFunctionSummaryStatementEdge.getFunctionCall());

        return new CFunctionSummaryStatementEdge(
            pCFunctionSummaryStatementEdge.getRawStatement(),
            newStatement,
            pCFunctionSummaryStatementEdge.getFileLocation(),
            newPredecessor,
            newSuccessor,
            newFunctionCall,
            pCFunctionSummaryStatementEdge.getFunctionName());
      } else {
        return null;
      }
    }
  }
}
