// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import java.util.Objects;
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public interface CCfaEdgeTransformer {

  /**
   * Returns a new {@code CCfaEdgeTransformer} for the specified AST-transformer.
   *
   * @param pAstTransformer {@code original CFA-edge, original AST-node --> transformed AST-node}
   * @return a new {@code CCfaEdgeTransformer} for the specified AST-transformer
   * @throws NullPointerException if {@code pAstTransformer == null}
   */
  public static CCfaEdgeTransformer forAstTransformer(
      BiFunction<CFAEdge, CAstNode, CAstNode> pAstTransformer) {

    Objects.requireNonNull(pAstTransformer, "pAstTransformer must not be null");

    return new CCfaEdgeTransformer() {

      @Override
      public CFAEdge transformBlankEdge(
          BlankEdge pOriginalBlankEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {
        return new BlankEdge(
            pOriginalBlankEdge.getRawStatement(),
            pOriginalBlankEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor,
            pOriginalBlankEdge.getDescription());
      }

      @Override
      public CFAEdge transformCAssumeEdge(
          CAssumeEdge pOriginalCAssumeEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {

        CExpression newExpression =
            (CExpression)
                pAstTransformer.apply(pOriginalCAssumeEdge, pOriginalCAssumeEdge.getExpression());

        return new CAssumeEdge(
            pOriginalCAssumeEdge.getRawStatement(),
            pOriginalCAssumeEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor,
            newExpression,
            pOriginalCAssumeEdge.getTruthAssumption(),
            pOriginalCAssumeEdge.isSwapped(),
            pOriginalCAssumeEdge.isArtificialIntermediate());
      }

      @Override
      public CFAEdge transformCDeclarationEdge(
          CDeclarationEdge pOriginalCDeclarationEdge,
          CFANode pNewPredecessor,
          CFANode pNewSuccessor) {

        CDeclaration newDeclaration =
            (CDeclaration)
                pAstTransformer.apply(
                    pOriginalCDeclarationEdge, pOriginalCDeclarationEdge.getDeclaration());

        return new CDeclarationEdge(
            pOriginalCDeclarationEdge.getRawStatement(),
            pOriginalCDeclarationEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor,
            newDeclaration);
      }

      @Override
      public CFAEdge transformCStatementEdge(
          CStatementEdge pOriginalCStatementEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {

        CStatement newStatement =
            (CStatement)
                pAstTransformer.apply(
                    pOriginalCStatementEdge, pOriginalCStatementEdge.getStatement());

        return new CStatementEdge(
            pOriginalCStatementEdge.getRawStatement(),
            newStatement,
            pOriginalCStatementEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor);
      }

      @Override
      public CFunctionCallEdge transformCFunctionCallEdge(
          CFunctionCallEdge pOriginalCFunctionCallEdge,
          CFANode pNewPredecessor,
          CFunctionEntryNode pNewSuccessor,
          CFunctionSummaryEdge pNewCFunctionSummaryEdge) {

        return new CFunctionCallEdge(
            pOriginalCFunctionCallEdge.getRawStatement(),
            pOriginalCFunctionCallEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor,
            pNewCFunctionSummaryEdge.getExpression(),
            pNewCFunctionSummaryEdge);
      }

      @Override
      public CFunctionReturnEdge transformCFunctionReturnEdge(
          CFunctionReturnEdge pOriginalCFunctionReturnEdge,
          FunctionExitNode pNewPredecessor,
          CFANode pNewSuccessor,
          CFunctionSummaryEdge pNewCFunctionSummaryEdge) {

        return new CFunctionReturnEdge(
            pOriginalCFunctionReturnEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor,
            pNewCFunctionSummaryEdge);
      }

      @Override
      public CFunctionSummaryEdge transformCFunctionSummaryEdge(
          CFunctionSummaryEdge pOriginalCFunctionSummaryEdge,
          CFANode pNewPredecessor,
          CFANode pNewSuccessor,
          CFunctionEntryNode pNewCFunctionEntryNode) {

        CFunctionCall newFunctionCall =
            (CFunctionCall)
                pAstTransformer.apply(
                    pOriginalCFunctionSummaryEdge, pOriginalCFunctionSummaryEdge.getExpression());

        return new CFunctionSummaryEdge(
            pOriginalCFunctionSummaryEdge.getRawStatement(),
            pOriginalCFunctionSummaryEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor,
            newFunctionCall,
            pNewCFunctionEntryNode);
      }

      @Override
      public CReturnStatementEdge transformCReturnStatementEdge(
          CReturnStatementEdge pOriginalCReturnStatementEdge,
          CFANode pNewPredecessor,
          FunctionExitNode pNewSuccessor) {

        CReturnStatement newReturnStatement =
            (CReturnStatement)
                pAstTransformer.apply(
                    pOriginalCReturnStatementEdge,
                    pOriginalCReturnStatementEdge.getReturnStatement());

        return new CReturnStatementEdge(
            pOriginalCReturnStatementEdge.getRawStatement(),
            newReturnStatement,
            pOriginalCReturnStatementEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor);
      }

      @Override
      public CFunctionSummaryStatementEdge transformCFunctionSummaryStatementEdge(
          CFunctionSummaryStatementEdge pOriginalCFunctionSummaryStatementEdge,
          CFANode pNewPredecessor,
          CFANode pNewSuccessor) {

        CStatement newStatement =
            (CStatement)
                pAstTransformer.apply(
                    pOriginalCFunctionSummaryStatementEdge,
                    pOriginalCFunctionSummaryStatementEdge.getStatement());
        CFunctionCall newFunctionCall =
            (CFunctionCall)
                pAstTransformer.apply(
                    pOriginalCFunctionSummaryStatementEdge,
                    pOriginalCFunctionSummaryStatementEdge.getFunctionCall());

        return new CFunctionSummaryStatementEdge(
            pOriginalCFunctionSummaryStatementEdge.getRawStatement(),
            newStatement,
            pOriginalCFunctionSummaryStatementEdge.getFileLocation(),
            pNewPredecessor,
            pNewSuccessor,
            newFunctionCall,
            pOriginalCFunctionSummaryStatementEdge.getFunctionName());
      }
    };
  }

  CFAEdge transformBlankEdge(
      BlankEdge pOriginalBlankEdge, CFANode pNewPredecessor, CFANode pNewSuccessor);

  CFAEdge transformCAssumeEdge(
      CAssumeEdge pOriginalCAssumeEdge, CFANode pNewPredecessor, CFANode pNewSuccessor);

  CFAEdge transformCDeclarationEdge(
      CDeclarationEdge pOriginalCDeclarationEdge, CFANode pNewPredecessor, CFANode pNewSuccessor);

  CFAEdge transformCStatementEdge(
      CStatementEdge pOriginalCStatementEdge, CFANode pNewPredecessor, CFANode pNewSuccessor);

  CFunctionCallEdge transformCFunctionCallEdge(
      CFunctionCallEdge pOriginalCFunctionCallEdge,
      CFANode pNewPredecessor,
      CFunctionEntryNode pNewSuccessor,
      CFunctionSummaryEdge pNewCFunctionSummaryEdge);

  CFunctionReturnEdge transformCFunctionReturnEdge(
      CFunctionReturnEdge pOriginalCFunctionReturnEdge,
      FunctionExitNode pNewPredecessor,
      CFANode pNewSuccessor,
      CFunctionSummaryEdge pNewCFunctionSummaryEdge);

  CFunctionSummaryEdge transformCFunctionSummaryEdge(
      CFunctionSummaryEdge pOriginalCFunctionSummaryEdge,
      CFANode pNewPredecessor,
      CFANode pNewSuccessor,
      CFunctionEntryNode pNewCFunctionEntryNode);

  CReturnStatementEdge transformCReturnStatementEdge(
      CReturnStatementEdge pOriginalCReturnStatementEdge,
      CFANode pNewPredecessor,
      FunctionExitNode pNewSuccessor);

  CFunctionSummaryStatementEdge transformCFunctionSummaryStatementEdge(
      CFunctionSummaryStatementEdge pOriginalCFunctionSummaryStatementEdge,
      CFANode pNewPredecessor,
      CFANode pNewSuccessor);
}
