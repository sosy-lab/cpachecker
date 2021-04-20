// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public interface ACSLTermVisitor<R, X extends Exception> {

  R visit(ACSLBinaryTerm term) throws X;

  R visit(ACSLUnaryTerm term) throws X;

  R visit(ACSLArrayAccess term) throws X;

  R visit(TermAt term) throws X;

  R visit(ACSLResult term) throws X;

  R visit(ACSLCast term) throws X;

  R visit(BoundIdentifier term) throws X;

  R visit(ACSLIdentifier term) throws X;

  R visit(ACSLIntegerLiteral term) throws X;

  R visit(ACSLStringLiteral term) throws X;
}
