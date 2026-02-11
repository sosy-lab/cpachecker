// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqASTNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportFunctionDefinition;

public abstract sealed class SeqFunction implements SeqASTNode
    permits SeqAssumeFunction, SeqMainFunction, SeqThreadSimulationFunction {

  CExportFunctionDefinition functionDefinition;

  SeqFunction(CExportFunctionDefinition pFunctionDefinition) {
    functionDefinition = pFunctionDefinition;
  }

  public CExportFunctionDefinition getFunctionDefinition() {
    return functionDefinition;
  }

  public CFunctionCallStatement buildFunctionCallStatement(ImmutableList<CExpression> pParameters) {
    checkArgument(
        pParameters.size() == functionDefinition.getDeclaration().getParameters().size(),
        "pParameters.size() must be equal to the amount of parameters in functionDefinition.");

    CFunctionCallExpression functionCallExpression =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            functionDefinition.getDeclaration().getType(),
            functionDefinition.getName(),
            pParameters,
            functionDefinition.getDeclaration());
    return new CFunctionCallStatement(FileLocation.DUMMY, functionCallExpression);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    return functionDefinition.toASTString(pAAstNodeRepresentation);
  }
}
