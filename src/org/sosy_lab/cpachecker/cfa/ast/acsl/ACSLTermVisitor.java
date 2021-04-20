// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

public interface ACSLTermVisitor<R, X extends Exception> {

  R visit(ACSLBinaryTerm term) throws X;

  R visit(ACSLUnaryTerm term) throws X;

  R visit(ArrayAccess term) throws X;

  R visit(TermAt term) throws X;

  R visit(Result term) throws X;

  R visit(Cast term) throws X;

  R visit(BoundIdentifier term) throws X;

  R visit(Identifier term) throws X;

  R visit(IntegerLiteral term) throws X;

  R visit(StringLiteral term) throws X;
}
