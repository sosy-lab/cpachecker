// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.Substitution;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.SubstitutionException;

public class CParameterDeclarationSubstitution implements Substitution {

  /** The map of thread specific parameter to variable declaration substitutes. */
  public final ImmutableMap<MPORThread, ImmutableMap<CParameterDeclaration, CVariableDeclaration>>
      substitutes;

  private final CBinaryExpressionBuilder binExprBuilder;

  public CParameterDeclarationSubstitution(
      ImmutableMap<MPORThread, ImmutableMap<CParameterDeclaration, CVariableDeclaration>>
          pSubstitutes,
      CBinaryExpressionBuilder pBinExprBuilder) {
    substitutes = pSubstitutes;
    binExprBuilder = pBinExprBuilder;
  }

  @Override
  public CExpression substitute(CExpression e) throws SubstitutionException {
    // TODO
    return null;
  }
}
