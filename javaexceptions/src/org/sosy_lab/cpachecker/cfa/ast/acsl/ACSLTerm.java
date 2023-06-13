// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public sealed interface ACSLTerm extends ACSLLogicExpression
    permits ACSLArrayAccess,
        ACSLBinaryTerm,
        ACSLCast,
        ACSLIdentifier,
        ACSLIntegerLiteral,
        ACSLResult,
        ACSLStringLiteral,
        ACSLUnaryTerm,
        BoundIdentifier,
        TermAt {

  <R, X extends Exception> R accept(ACSLTermVisitor<R, X> visitor) throws X;
}
