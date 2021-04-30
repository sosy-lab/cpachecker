// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import java.util.Optional;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CAstNodeTransformer;
import org.sosy_lab.cpachecker.util.CCfaTransformer;

class SimpleEdgeTransformer<X extends RuntimeException> implements CCfaTransformer.EdgeTransformer {

  private final Function<CFAEdge, CAstNodeTransformer<X>> edgeToAstTransformer;

  SimpleEdgeTransformer(Function<CFAEdge, CAstNodeTransformer<X>> pEdgeToAstTransformer) {
    edgeToAstTransformer = pEdgeToAstTransformer;
  }

  private CAstNodeTransformer<X> createAstTransformer(CFAEdge pCfaEdge) {
    return edgeToAstTransformer.apply(pCfaEdge);
  }

  @Override
  public CFAEdge transformBlankEdge(
      BlankEdge pOldBlankEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {
    return new BlankEdge(
        pOldBlankEdge.getRawStatement(),
        pOldBlankEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor,
        pOldBlankEdge.getDescription());
  }

  @Override
  public CFAEdge transformCAssumeEdge(
      CAssumeEdge pOldCAssumeEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {

    CExpression newExpression =
        (CExpression)
            createAstTransformer(pOldCAssumeEdge).transform(pOldCAssumeEdge.getExpression());

    return new CAssumeEdge(
        pOldCAssumeEdge.getRawStatement(),
        pOldCAssumeEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor,
        newExpression,
        pOldCAssumeEdge.getTruthAssumption(),
        pOldCAssumeEdge.isSwapped(),
        pOldCAssumeEdge.isArtificialIntermediate());
  }

  @Override
  public CFAEdge transformCDeclarationEdge(
      CDeclarationEdge pOldCDeclarationEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {

    CDeclaration newDeclaration =
        (CDeclaration)
            createAstTransformer(pOldCDeclarationEdge)
                .transform(pOldCDeclarationEdge.getDeclaration());

    return new CDeclarationEdge(
        pOldCDeclarationEdge.getRawStatement(),
        pOldCDeclarationEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor,
        newDeclaration);
  }

  @Override
  public CFAEdge transformCStatementEdge(
      CStatementEdge pOldCStatementEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {

    CStatement newStatement =
        (CStatement)
            createAstTransformer(pOldCStatementEdge).transform(pOldCStatementEdge.getStatement());

    return new CStatementEdge(
        pOldCStatementEdge.getRawStatement(),
        newStatement,
        pOldCStatementEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor);
  }

  @Override
  public CFunctionCallEdge transformCFunctionCallEdge(
      CFunctionCallEdge pOldCFunctionCallEdge,
      CFANode pNewPredecessor,
      CFunctionEntryNode pNewSuccessor,
      CFunctionSummaryEdge pNewCFunctionSummaryEdge) {
    return new CFunctionCallEdge(
        pOldCFunctionCallEdge.getRawStatement(),
        pOldCFunctionCallEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor,
        pNewCFunctionSummaryEdge.getExpression(),
        pNewCFunctionSummaryEdge);
  }

  @Override
  public CFunctionReturnEdge transformCFunctionReturnEdge(
      CFunctionReturnEdge pOldCFunctionReturnEdge,
      FunctionExitNode pNewPredecessor,
      CFANode pNewSuccessor,
      CFunctionSummaryEdge pNewCFunctionSummaryEdge) {
    return new CFunctionReturnEdge(
        pOldCFunctionReturnEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor,
        pNewCFunctionSummaryEdge);
  }

  @Override
  public CFunctionSummaryEdge transformCFunctionSummaryEdge(
      CFunctionSummaryEdge pOldCFunctionSummaryEdge,
      CFANode pNewPredecessor,
      CFANode pNewSuccessor,
      CFunctionEntryNode pNewCFunctionEntryNode) {

    CFunctionCall newFunctionCall =
        (CFunctionCall)
            createAstTransformer(pOldCFunctionSummaryEdge)
                .transform(pOldCFunctionSummaryEdge.getExpression());

    return new CFunctionSummaryEdge(
        pOldCFunctionSummaryEdge.getRawStatement(),
        pOldCFunctionSummaryEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor,
        newFunctionCall,
        pNewCFunctionEntryNode);
  }

  @Override
  public CReturnStatementEdge transformCReturnStatementEdge(
      CReturnStatementEdge pOldCReturnStatementEdge,
      CFANode pNewPredecessor,
      FunctionExitNode pNewSuccessor) {

    Optional<CReturnStatement> optOldReturnStatement =
        pOldCReturnStatementEdge.getRawAST().toJavaUtil();
    CReturnStatement newReturnStatement = null;
    if (optOldReturnStatement.isPresent()) {
      newReturnStatement =
          (CReturnStatement)
              createAstTransformer(pOldCReturnStatementEdge)
                  .transform(optOldReturnStatement.orElseThrow());
    }

    return new CReturnStatementEdge(
        pOldCReturnStatementEdge.getRawStatement(),
        newReturnStatement,
        pOldCReturnStatementEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor);
  }

  @Override
  public CFunctionSummaryStatementEdge transformCFunctionSummaryStatementEdge(
      CFunctionSummaryStatementEdge pOldCFunctionSummaryStatementEdge,
      CFANode pNewPredecessor,
      CFANode pNewSuccessor) {

    CAstNodeTransformer<X> astTransformer = createAstTransformer(pOldCFunctionSummaryStatementEdge);
    CStatement newStatement =
        (CStatement) astTransformer.transform(pOldCFunctionSummaryStatementEdge.getStatement());
    CFunctionCall newFunctionCall =
        (CFunctionCall)
            astTransformer.transform(pOldCFunctionSummaryStatementEdge.getFunctionCall());

    return new CFunctionSummaryStatementEdge(
        pOldCFunctionSummaryStatementEdge.getRawStatement(),
        newStatement,
        pOldCFunctionSummaryStatementEdge.getFileLocation(),
        pNewPredecessor,
        pNewSuccessor,
        newFunctionCall,
        pOldCFunctionSummaryStatementEdge.getFunctionName());
  }
}
