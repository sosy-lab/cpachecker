// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.cfa.ast.AInitializer;

public sealed interface AcslInitializer extends AInitializer, AcslAstNode
    permits AcslInitializerExpression {

  <R, X extends Exception> R accept(AcslInitializerVisitor<R, X> pV) throws X;
}
