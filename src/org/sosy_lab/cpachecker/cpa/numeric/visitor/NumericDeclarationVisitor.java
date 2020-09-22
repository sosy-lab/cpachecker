// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.visitor;

import com.google.common.collect.ImmutableSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cpa.numeric.NumericState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.numericdomains.Value.NewVariableValue;
import org.sosy_lab.numericdomains.environment.Variable;

public class NumericDeclarationVisitor
    implements CSimpleDeclarationVisitor<NumericState, UnrecognizedCodeException> {
  private final NumericState state;
  private final LogManager logger;

  public NumericDeclarationVisitor(NumericState pState, LogManager logManager) {
    this.state = pState;
    logger = logManager;
  }

  @Override
  public NumericState visit(CVariableDeclaration pDecl) throws UnrecognizedCodeException {
    Variable variable = new Variable(pDecl.getQualifiedName());
    logger.log(Level.FINEST, "Variable declaration: " + variable.toString());
    return state.addVariables(ImmutableSet.of(variable), ImmutableSet.of(), NewVariableValue.ZERO);
  }

  @Override
  public NumericState visit(CFunctionDeclaration pDecl) throws UnrecognizedCodeException {
    return state;
  }

  @Override
  public NumericState visit(CComplexTypeDeclaration pDecl) throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public NumericState visit(CTypeDefDeclaration pDecl) throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public NumericState visit(CParameterDeclaration pDecl) throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public NumericState visit(CEnumerator pDecl) throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }
}
