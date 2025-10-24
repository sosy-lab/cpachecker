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
import com.google.common.collect.ImmutableSet;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public abstract class SeqFunction {

  abstract String buildBody() throws UnrecognizedCodeException;

  abstract CType getReturnType();

  abstract CIdExpression getFunctionName();

  abstract ImmutableList<CParameterDeclaration> getParameterDeclarations();

  public final CFunctionType getFunctionType() {
    return new CFunctionType(
        getReturnType(),
        getParameterDeclarations().stream()
            .map(CParameterDeclaration::getType)
            .collect(ImmutableList.toImmutableList()),
        false);
  }

  public final CFunctionDeclaration getFunctionDeclaration() {
    return new CFunctionDeclaration(
        FileLocation.DUMMY,
        getFunctionType(),
        getFunctionName().toASTString(),
        getParameterDeclarations(),
        ImmutableSet.of());
  }

  /**
   * Basically {@link CFunctionDeclaration#toASTString()} with parameter names but without the
   * suffix {@code ;}.
   */
  private String buildSignature() {
    StringBuilder parameters = new StringBuilder();
    for (int i = 0; i < getParameterDeclarations().size(); i++) {
      CParameterDeclaration param = getParameterDeclarations().get(i);
      String suffix =
          i == getParameterDeclarations().size() - 1
              ? SeqSyntax.EMPTY_STRING
              : SeqSyntax.COMMA + SeqSyntax.SPACE;
      parameters.append(param.toASTString()).append(suffix);
    }
    return getReturnType().toASTString(SeqSyntax.EMPTY_STRING)
        + SeqSyntax.SPACE
        + getFunctionName().getName()
        + SeqSyntax.BRACKET_LEFT
        + parameters
        + SeqSyntax.BRACKET_RIGHT;
  }

  public final String buildDefinition() throws UnrecognizedCodeException {
    StringJoiner rDefinition = new StringJoiner(SeqSyntax.NEWLINE);
    rDefinition.add(SeqStringUtil.appendCurlyBracketLeft(buildSignature()));
    rDefinition.add(buildBody());
    rDefinition.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    return rDefinition.toString();
  }

  public final CFunctionCallExpression buildFunctionCallExpression(
      ImmutableList<CExpression> pParameters) {

    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        getReturnType(),
        getFunctionName(),
        pParameters,
        getFunctionDeclaration());
  }

  public final CFunctionCallStatement buildFunctionCallStatement(
      ImmutableList<CExpression> pParameters) {

    checkArgument(
        getParameterDeclarations().size() == pParameters.size(),
        "pParameters.size() must equal parameter declaration amount");
    CFunctionCallExpression functionCallExpression = buildFunctionCallExpression(pParameters);
    return new CFunctionCallStatement(FileLocation.DUMMY, functionCallExpression);
  }
}
