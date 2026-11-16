// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;

public sealed interface AcslLogicDeclaration extends AcslSimpleDeclaration, ADeclaration
    permits AcslFunctionDeclaration, AcslPredicateDeclaration {

  /** Returns the declaration as in the ACSL syntax of logic definitions, without the body. */
  String typedDeclarationString();

  /** Returns the identifier with its type variables and parameters as in the ACSL syntax. */
  static String polyIdWithParameters(
      String pName,
      List<AcslTypeVariableDeclaration> pTypeVariables,
      List<AcslParameterDeclaration> pParameters) {
    return pName
        + (pTypeVariables.isEmpty()
            ? ""
            : "<" + Joiner.on(", ").join(Lists.transform(pTypeVariables, t -> t.getName())) + ">")
        + (pParameters.isEmpty()
            ? ""
            : "(" + Joiner.on(", ").join(Lists.transform(pParameters, p -> p.toASTString())) + ")");
  }
}
