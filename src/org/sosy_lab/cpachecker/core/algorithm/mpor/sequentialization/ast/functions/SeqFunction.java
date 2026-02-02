// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqASTNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CFunctionDefinitionStatement;

public abstract sealed class SeqFunction implements SeqASTNode
    permits SeqAssumeFunction, SeqMainFunction, SeqThreadSimulationFunction {

  CFunctionDefinitionStatement functionDefinition;

  SeqFunction(CFunctionDefinitionStatement pFunctionDefinition) {
    functionDefinition = pFunctionDefinition;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    return functionDefinition.toASTString(pAAstNodeRepresentation);
  }
}
