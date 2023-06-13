// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

public interface CAstNodeVisitor<R, X extends Exception>
    extends CDesignatorVisitor<R, X>,
        CInitializerVisitor<R, X>,
        CRightHandSideVisitor<R, X>,
        CSimpleDeclarationVisitor<R, X>,
        CStatementVisitor<R, X> {

  R visit(CReturnStatement pNode) throws X;
}
