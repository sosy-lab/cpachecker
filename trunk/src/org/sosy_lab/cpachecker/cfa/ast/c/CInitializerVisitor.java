// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

public interface CInitializerVisitor<R, X extends Exception> {

  R visit(CInitializerExpression pInitializerExpression) throws X;

  R visit(CInitializerList pInitializerList) throws X;

  R visit(CDesignatedInitializer pCStructInitializerPart) throws X;
}
