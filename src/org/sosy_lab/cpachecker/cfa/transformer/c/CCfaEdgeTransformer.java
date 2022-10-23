// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import java.util.Arrays;
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
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeSubstitution;
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeSubstitution;
import org.sosy_lab.cpachecker.exceptions.NoException;

/** {@link CfaEdgeTransformer} for CFA edges that are contained in CFAs of C programs. */
public final class CCfaEdgeTransformer implements CfaEdgeTransformer {

  public static final CCfaEdgeTransformer CLONER = new CCfaEdgeTransformer(ImmutableList.of());

  public static final CfaEdgeTransformer SUMMARY_TO_STATEMENT_EDGE_TRANSFORMER =
      new CfaEdgeTransformer() {

        @Override
        public CFAEdge transform(
            CFAEdge pEdge,
            CfaNetwork pCfa,
            CfaNodeSubstitution pNodeSubstitution,
            CfaEdgeSubstitution pEdgeSubstitution) {

          CFAEdge transformedEdge =
              CLONER.transform(pEdge, pCfa, pNodeSubstitution, pEdgeSubstitution);

          if (transformedEdge instanceof CFunctionSummaryEdge) {

            CFunctionSummaryEdge transformedSummaryEdge = (CFunctionSummaryEdge) transformedEdge;

            return new CStatementEdge(
                transformedSummaryEdge.getRawStatement(),
                transformedSummaryEdge.getExpression(),
                transformedSummaryEdge.getFileLocation(),
                transformedSummaryEdge.getPredecessor(),
                transformedSummaryEdge.getSuccessor());
          }

          return transformedEdge;
        }
      };

  private final ImmutableList<CCfaEdgeAstSubstitution> edgeAstSubstitutions;

  private CCfaEdgeTransformer(ImmutableList<CCfaEdgeAstSubstitution> pEdgeAstSubstitutions) {
    edgeAstSubstitutions = pEdgeAstSubstitutions;
  }

  public static CCfaEdgeTransformer withSubstitutions(
      CCfaEdgeAstSubstitution pEdgeAstSubstitution,
      CCfaEdgeAstSubstitution... pEdgeAstSubstitutions) {

    ImmutableList.Builder<CCfaEdgeAstSubstitution> substitutionsBuilder =
        ImmutableList.builderWithExpectedSize(pEdgeAstSubstitutions.length + 1);
    substitutionsBuilder.add(pEdgeAstSubstitution);
    substitutionsBuilder.addAll(Arrays.asList(pEdgeAstSubstitutions));

    return new CCfaEdgeTransformer(substitutionsBuilder.build());
  }

  private CAstNode applyEdgeAstSubstitutions(CFAEdge pEdge, CAstNode pAstNode) {

    CAstNode astNode = pAstNode;
    for (CCfaEdgeAstSubstitution edgeAstSubstitution : edgeAstSubstitutions) {
      astNode = edgeAstSubstitution.apply(pEdge, astNode);
    }

    return astNode;
  }

  @Override
  public CFAEdge transform(
      CFAEdge pEdge,
      CfaNetwork pCfaNetwork,
      CfaNodeSubstitution pNodeSubstitution,
      CfaEdgeSubstitution pEdgeSubstitution) {

    EndpointPair<CFANode> oldEndpoints = pCfaNetwork.incidentNodes(pEdge);

    CFANode newPredecessor = pNodeSubstitution.get(oldEndpoints.source());
    CFANode newSuccessor = pNodeSubstitution.get(oldEndpoints.target());

    var transformingEdgeVisitor =
        new TransformingCCfaEdgeVisitor(
            pCfaNetwork, pNodeSubstitution, pEdgeSubstitution, newPredecessor, newSuccessor);

    return ((CCfaEdge) pEdge).accept(transformingEdgeVisitor);
  }

  private final class TransformingCCfaEdgeVisitor implements CCfaEdgeVisitor<CFAEdge, NoException> {

    private final CfaNetwork cfaNetwork;
    private final CfaNodeSubstitution nodeSubstitution;
    private final CfaEdgeSubstitution edgeSubstitution;

    private final CFANode newPredecessor;
    private final CFANode newSuccessor;

    private TransformingCCfaEdgeVisitor(
        CfaNetwork pCfaNetwork,
        CfaNodeSubstitution pNodeSubstitution,
        CfaEdgeSubstitution pEdgeSubstitution,
        CFANode pNewPredecessor,
        CFANode pNewSuccessor) {

      cfaNetwork = pCfaNetwork;
      nodeSubstitution = pNodeSubstitution;
      edgeSubstitution = pEdgeSubstitution;

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
    public CFAEdge visit(CFunctionCallEdge pCFunctionCallEdge) {

      FunctionSummaryEdge oldFunctionSummaryEdge =
          cfaNetwork.functionSummaryEdge(pCFunctionCallEdge);
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
    }

    @Override
    public CFAEdge visit(CFunctionReturnEdge pCFunctionReturnEdge) {

      FunctionSummaryEdge oldFunctionSummaryEdge =
          cfaNetwork.functionSummaryEdge(pCFunctionReturnEdge);
      CFunctionSummaryEdge newFunctionSummaryEdge =
          (CFunctionSummaryEdge) edgeSubstitution.get(oldFunctionSummaryEdge);

      return new CFunctionReturnEdge(
          pCFunctionReturnEdge.getFileLocation(),
          (FunctionExitNode) newPredecessor,
          newSuccessor,
          newFunctionSummaryEdge);
    }

    @Override
    public CFAEdge visit(CFunctionSummaryEdge pCFunctionSummaryEdge) {

      CFunctionCall newFunctionCall =
          (CFunctionCall)
              applyEdgeAstSubstitutions(
                  pCFunctionSummaryEdge, pCFunctionSummaryEdge.getExpression());

      FunctionEntryNode oldFunctionEntryNode = cfaNetwork.functionEntryNode(pCFunctionSummaryEdge);
      CFunctionEntryNode newFunctionEntryNode =
          (CFunctionEntryNode) nodeSubstitution.get(oldFunctionEntryNode);

      return new CFunctionSummaryEdge(
          pCFunctionSummaryEdge.getRawStatement(),
          pCFunctionSummaryEdge.getFileLocation(),
          newPredecessor,
          newSuccessor,
          newFunctionCall,
          newFunctionEntryNode);
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

      CStatement newStatement =
          (CStatement)
              applyEdgeAstSubstitutions(
                  pCFunctionSummaryStatementEdge, pCFunctionSummaryStatementEdge.getStatement());
      CFunctionCall newFunctionCall =
          (CFunctionCall)
              applyEdgeAstSubstitutions(
                  pCFunctionSummaryStatementEdge, pCFunctionSummaryStatementEdge.getFunctionCall());

      return new CFunctionSummaryStatementEdge(
          pCFunctionSummaryStatementEdge.getRawStatement(),
          newStatement,
          pCFunctionSummaryStatementEdge.getFileLocation(),
          newPredecessor,
          newSuccessor,
          newFunctionCall,
          pCFunctionSummaryStatementEdge.getFunctionName());
    }
  }
}
