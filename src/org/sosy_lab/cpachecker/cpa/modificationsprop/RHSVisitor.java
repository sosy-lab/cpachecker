// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

/** Provides a set of variable names used. */
public class RHSVisitor extends VariableIdentifierVisitor
    implements CRightHandSideVisitor<Set<String>, NoException> {

  @Override
  public Set<String> visit(CFunctionCallExpression pIastFunctionCallExpression) throws NoException {
    Builder<String> imsBuild = new ImmutableSet.Builder<>();
    for (CExpression exp : pIastFunctionCallExpression.getParameterExpressions()) {
      imsBuild.addAll(exp.accept(this));
    }
    return imsBuild.build();
  }
}
