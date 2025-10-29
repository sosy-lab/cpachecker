// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;

public sealed interface K3SimpleDeclaration extends ASimpleDeclaration, K3AstNode
    permits K3Declaration, K3ParameterDeclaration {

  K3Type getType();
}
