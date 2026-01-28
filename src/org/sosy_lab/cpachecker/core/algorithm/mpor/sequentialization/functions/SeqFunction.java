// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CCompoundStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public abstract sealed class SeqFunction
    permits SeqAssumeFunction, SeqMainFunction, SeqThreadSimulationFunction {

  /**
   * The {@link CFunctionDeclaration} only contains a {@link String} representation of the name, but
   * a {@link CFunctionCallExpression} requires a {@link CIdExpression}, so we keep this separate.
   */
  private final CIdExpression name;

  private final CFunctionDeclaration declaration;

  private final CCompoundStatement body;

  SeqFunction(CFunctionDeclaration pDeclaration, CCompoundStatement pBody) {
    name = new CIdExpression(FileLocation.DUMMY, pDeclaration);
    declaration = pDeclaration;
    body = pBody;
  }

  /**
   * Basically {@link CFunctionDeclaration#toASTString()} with parameter names but without the
   * suffix {@code ;}.
   */
  private String buildSignature() {
    StringBuilder parameters = new StringBuilder();
    for (int i = 0; i < declaration.getParameters().size(); i++) {
      CParameterDeclaration param = declaration.getParameters().get(i);
      String suffix =
          i == declaration.getParameters().size() - 1
              ? SeqSyntax.EMPTY_STRING
              : SeqSyntax.COMMA + SeqSyntax.SPACE;
      parameters.append(param.toASTString()).append(suffix);
    }
    return declaration.getType().getReturnType().toASTString(SeqSyntax.EMPTY_STRING)
        + SeqSyntax.SPACE
        + declaration.getName()
        + SeqSyntax.BRACKET_LEFT
        + parameters
        + SeqSyntax.BRACKET_RIGHT;
  }

  /** Returns a {@link String} of the entire function, including signature and body. */
  public final String buildDefinition() throws UnrecognizedCodeException {
    StringJoiner rDefinition = new StringJoiner(System.lineSeparator());
    rDefinition.add(buildSignature());
    rDefinition.add(body.toASTString());
    return rDefinition.toString();
  }

  public final CFunctionCallStatement buildFunctionCallStatement(
      ImmutableList<CExpression> pParameters) {

    checkArgument(
        declaration.getParameters().size() == pParameters.size(),
        "pParameters.size() must equal parameter declaration amount");
    CFunctionCallExpression functionCallExpression =
        new CFunctionCallExpression(
            FileLocation.DUMMY, declaration.getType(), name, pParameters, declaration);
    return new CFunctionCallStatement(FileLocation.DUMMY, functionCallExpression);
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
}
