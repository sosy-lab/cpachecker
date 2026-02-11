// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.expression;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A wrapper for a {@link CFunctionCallExpression} so that {@link CExportExpression} can be used as
 * parameters. Example:
 *
 * <pre>{@code
 * assume(A || B);
 *
 * }</pre>
 *
 * <p>Note that this class should only be used if there is at least one parameter, otherwise use
 * {@link CFunctionCallExpression}.
 */
public record CFunctionCallExpressionWrapper(
    CFunctionCallExpression functionCallExpression, ImmutableList<CExportExpression> parameters)
    implements CExportExpression {

  public CFunctionCallExpressionWrapper {
    checkArgument(
        !parameters.isEmpty(),
        "The parameters list cannot be empty, because a CFunctionCallStatementWrapper should only"
            + " be created if at least one of the parameters can only be expressed using a"
            + " CExportExpression (e.g., logical &&). If there are no parameters, use"
            + " CFunctionCallStatement instead.");
    // this is not necessary because the parameters are replaced anyway, but still good practice
    checkArgument(
        functionCallExpression.getParameterExpressions().size() == parameters.size(),
        "The amount of parameters in functionCallExpression must match parameters.size().");
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {
    // take the name of the function from the CFunctionCallStatement
    String functionName =
        functionCallExpression.getFunctionNameExpression().toASTString(pAAstNodeRepresentation);

    // replace the parameters of the CFunctionCallStatement, separated by ', '
    StringJoiner parameterList = new StringJoiner(", ");
    for (CExportExpression parameter : parameters) {
      parameterList.add(parameter.toASTString(pAAstNodeRepresentation));
    }
    return functionName + "(" + parameterList + ")";
  }
}
