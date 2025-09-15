// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public interface AFunctionCall extends AStatement {

  AFunctionCallExpression getFunctionCallExpression();

  default AFunctionDeclaration getFunctionDeclaration() {
    return getFunctionCallExpression().getDeclaration();
  }

  default List<? extends AExpression> getParameterExpressions() {
    return getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  default String toASTString() {
    return getFunctionCallExpression().toASTString();
  }
}
