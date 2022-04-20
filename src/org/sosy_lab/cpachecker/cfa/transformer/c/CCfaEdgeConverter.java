// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeConverter;
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeSubstitution;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeSubstitution;
import org.sosy_lab.cpachecker.exceptions.NoException;

public final class CCfaEdgeConverter implements CfaEdgeConverter {

  public static final CCfaEdgeConverter IDENTITY = new CCfaEdgeConverter(ImmutableList.of());

  private final ImmutableList<CCfaEdgeAstSubstitution> edgeAstSubstitutions;

  private CCfaEdgeConverter(ImmutableList<CCfaEdgeAstSubstitution> pEdgeAstSubstitutions) {
    edgeAstSubstitutions = pEdgeAstSubstitutions;
  }

  public static CCfaEdgeConverter forSubstitutions(
      ImmutableList<CCfaEdgeAstSubstitution> pEdgeAstSubstitutions) {
    return new CCfaEdgeConverter(checkNotNull(pEdgeAstSubstitutions));
  }

  private CAstNode applyEdgeAstSubstitutions(CFAEdge pEdge, CAstNode pAstNode) {

    CAstNode astNode = pAstNode;
    for (CCfaEdgeAstSubstitution edgeAstSubstitution : edgeAstSubstitutions) {
      astNode = edgeAstSubstitution.apply(pEdge, astNode);
    }

    return astNode;
  }

  @Override
  public Optional<CFAEdge> convertEdge(
      CFAEdge pEdge,
      CfaNetwork pCfaNetwork,
      CfaNodeSubstitution pNodeSubstitution,
      CfaEdgeSubstitution pEdgeSubstitution,
      CfaConnectedness pConnectedness) {

    EndpointPair<CFANode> oldEndpoints = pCfaNetwork.incidentNodes(pEdge);

    CFANode newPredecessor = pNodeSubstitution.get(oldEndpoints.source());
    CFANode newSuccessor = pNodeSubstitution.get(oldEndpoints.target());

    var transformingEdgeVisitor =
        new TransformingCCfaEdgeVisitor(
            pCfaNetwork,
            pNodeSubstitution,
            pEdgeSubstitution,
            pConnectedness,
            newPredecessor,
            newSuccessor);

    return Optional.ofNullable(((CCfaEdge) pEdge).accept(transformingEdgeVisitor));
  }

  private final class TransformingCCfaEdgeVisitor
      implements CCfaEdgeVisitor<@Nullable CFAEdge, NoException> {

    private final CfaNetwork cfaNetwork;
    private final CfaNodeSubstitution nodeSubstitution;
    private final CfaEdgeSubstitution edgeSubstitution;
    private final CfaConnectedness connectedness;

    private final CFANode newPredecessor;
    private final CFANode newSuccessor;

    private TransformingCCfaEdgeVisitor(
        CfaNetwork pCfaNetwork,
        CfaNodeSubstitution pNodeSubstitution,
        CfaEdgeSubstitution pEdgeSubstitution,
        CfaConnectedness pConnectedness,
        CFANode pNewPredecessor,
        CFANode pNewSuccessor) {

      cfaNetwork = pCfaNetwork;
      nodeSubstitution = pNodeSubstitution;
      edgeSubstitution = pEdgeSubstitution;
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
            cfaNetwork.getFunctionSummaryEdge(pCFunctionCallEdge);
        CFunctionSummaryEdge newFunctionSummaryEdge =
            (CFunctionSummaryEdge) edgeSubstitution.get(oldFunctionSummaryEdge);

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
            cfaNetwork.getFunctionSummaryEdge(pCFunctionReturnEdge);
        CFunctionSummaryEdge newFunctionSummaryEdge =
            (CFunctionSummaryEdge) edgeSubstitution.get(oldFunctionSummaryEdge);

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
            cfaNetwork.getFunctionEntryNode(pCFunctionSummaryEdge);
        CFunctionEntryNode newFunctionEntryNode =
            (CFunctionEntryNode) nodeSubstitution.get(oldFunctionEntryNode);

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
