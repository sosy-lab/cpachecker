// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CFunctionCallExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExpressionStatementWrapper;

/**
 * A class to represent an entire C function, including return {@link CType}, function name as
 * {@link CIdExpression}, {@link CFunctionDeclaration} with parameters and a {@link
 * CCompoundStatement} to represent the body of the function. Example:
 *
 * <pre>{@code
 * int main(int arg) {
 *     int x = 1;
 * }
 * }</pre>
 */
public final class CExportFunctionDefinition {

  /**
   * The {@link CFunctionDeclaration} only contains a {@link String} representation of the name, but
   * a {@link CFunctionCallExpression} requires a {@link CIdExpression}, so we keep this separate.
   */
  private final CIdExpression name;

  private final CFunctionDeclaration declaration;

  private final CCompoundStatement body;

  /**
   * A {@link CFunctionCallExpression} with dummy parameters. Can be used to create a {@link
   * CFunctionCallExpressionWrapper} with actual parameters.
   */
  private final CFunctionCallExpression dummyFunctionCallExpression;

  public CExportFunctionDefinition(CFunctionDeclaration pDeclaration, CCompoundStatement pBody) {
    name = new CIdExpression(FileLocation.DUMMY, pDeclaration);
    declaration = pDeclaration;
    body = pBody;
    ImmutableList<CExpression> dummyParameters =
        declaration.getParameters().stream()
            .map(p -> new CIdExpression(FileLocation.DUMMY, p))
            .collect(ImmutableList.toImmutableList());
    dummyFunctionCallExpression =
        new CFunctionCallExpression(
            FileLocation.DUMMY, declaration.getType(), name, dummyParameters, declaration);
  }

  public String toASTString() throws UnrecognizedCodeException {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringJoiner rDefinition = new StringJoiner(System.lineSeparator());

    StringJoiner parameters = new StringJoiner(", ");
    declaration
        .getParameters()
        .forEach(p -> parameters.add(p.toASTString(pAAstNodeRepresentation)));
    String returnType = declaration.getType().getReturnType().toASTString("");
    rDefinition.add(returnType + " " + declaration.getName() + "(" + parameters + ")");

    rDefinition.add(body.toASTString(pAAstNodeRepresentation));
    return rDefinition.toString();
  }

  public CType getReturnType() {
    return declaration.getType().getReturnType();
  }

  public CIdExpression getName() {
    return name;
  }

  public CFunctionDeclaration getDeclaration() {
    return declaration;
  }

  public CCompoundStatement getBody() {
    return body;
  }

  public CExpressionStatementWrapper buildFunctionCallStatement(
      ImmutableList<CExportExpression> pParameters) {

    checkArgument(
        pParameters.size() == declaration.getParameters().size(),
        "pParameters.size() must be equal to the amount of parameters in declaration.");

    CFunctionCallExpressionWrapper functionCallExpressionWrapper =
        new CFunctionCallExpressionWrapper(dummyFunctionCallExpression, pParameters);
    return new CExpressionStatementWrapper(functionCallExpressionWrapper);
  }
}
