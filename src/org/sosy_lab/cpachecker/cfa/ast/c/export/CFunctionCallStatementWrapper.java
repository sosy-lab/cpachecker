// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c.export;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;

public record CFunctionCallStatementWrapper(
    CFunctionCallStatement functionCallStatement, ImmutableList<CExportExpression> parameters)
    implements CExportStatement {

  public CFunctionCallStatementWrapper {
    checkArgument(
        !parameters.isEmpty(),
        "The parameters list cannot be empty, because constructing a CWrapperFunctionCallStatement"
            + " only makes sense if at least one of the parameters can only be expressed using a"
            + " CAstExpression (e.g., logical &&). If there are now parameters, use"
            + " CFunctionCallStatement instead.");
    // this is not necessary because the parameters are replaced anyway, but still good practice
    checkArgument(
        functionCallStatement.getFunctionCallExpression().getParameterExpressions().size()
            == parameters.size(),
        "The amount of parameters in functionCallStatement must match parameters.size().");
  }

  @Override
  public String toASTString() {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    // take the name of the function from the CFunctionCallStatement
    String functionName =
        functionCallStatement.getFunctionCallExpression().getFunctionNameExpression().toASTString();
    // replace the parameters of the CFunctionCallStatement, separated by ', '
    StringJoiner parameterList = new StringJoiner(", ");
    parameters.forEach(p -> parameterList.add(p.toASTString()));
    return functionName + "(" + parameterList + ");";
  }
}
