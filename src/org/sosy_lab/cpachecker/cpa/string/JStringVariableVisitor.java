// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cpa.string.utils.JStringVariableIdentifier;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/*
 * Visitor used to create the string identifier JVariableIdentifier
 */
public class JStringVariableVisitor implements JLeftHandSideVisitor<JStringVariableIdentifier, NoException> {

  @Override
  public JStringVariableIdentifier visit(JArraySubscriptExpression pE)
      throws NoException {
    JExpression je = pE.getSubscriptExpression();
    if (je instanceof JLeftHandSide) {
      return ((JLeftHandSide) je).accept(this);
    }
    return null;
  }

  @Override
  public JStringVariableIdentifier visit(JIdExpression pE) throws NoException {
    JSimpleDeclaration decl =pE.getDeclaration();
    return new JStringVariableIdentifier(decl.getType(),MemoryLocation.forDeclaration(decl));
  }

  public JStringVariableIdentifier visit(JLeftHandSide pOp1) {
    return pOp1.accept(this);
  }

  public JStringVariableIdentifier visit(JSimpleDeclaration pE) {
    return new JStringVariableIdentifier(pE.getType(), MemoryLocation.forDeclaration(pE));

  }

}
