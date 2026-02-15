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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A wrapper for a {@link CFunctionCallExpression} with a list of {@link CExportExpression} to
 * replace the existing {@link CExpression} parameters.
 *
 * <p>A simple example is {@code function(p1 && p2, p3)} which is a {@link
 * CFunctionCallExpressionWrapper} because it uses both {@link CLogicalAndExpression} for {@code p1
 * && p2} and {@link CExpressionWrapper} for {@code p3} as parameters, so a list of {@link
 * CExportExpression} instead of {@link CExpression}.
 *
 * <p>Note that this class should only be used if there is at least one parameter, otherwise use
 * {@link CFunctionCallExpression}.
 *
 * @param functionCallExpression The {@link CFunctionCallExpression} whose parameters will be
 *     replaced. Note that the parameters in the {@link CFunctionCallExpression} must match the size
 *     of the second parameter {@code parameters}.
 * @param parameters The list of {@link CExportExpression} that replace the parameters of {@code
 *     functionCallExpression}. The constructor throws {@link IllegalArgumentException} if the list
 *     is empty, or if its size is not equal to the number of parameters in {@code
 *     functionCallExpression}.
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
