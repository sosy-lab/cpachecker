// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import org.sosy_lab.cpachecker.cfa.model.BlankEdge;

public interface CCfaEdgeVisitor<R, X extends Exception> {

  R visit(BlankEdge pBlankEdge) throws X;

  R visit(CAssumeEdge pCAssumeEdge) throws X;

  R visit(CDeclarationEdge pCDeclarationEdge) throws X;

  R visit(CStatementEdge pCStatementEdge) throws X;

  R visit(CFunctionCallEdge pCFunctionCallEdge) throws X;

  R visit(CFunctionReturnEdge pCFunctionReturnEdge) throws X;

  R visit(CFunctionSummaryEdge pCFunctionSummaryEdge) throws X;

  R visit(CReturnStatementEdge pCReturnStatementEdge) throws X;

  R visit(CFunctionSummaryStatementEdge pCFunctionSummaryStatementEdge) throws X;
}
