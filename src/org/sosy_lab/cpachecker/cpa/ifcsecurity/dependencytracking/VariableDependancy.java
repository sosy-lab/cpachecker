// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking;

import java.util.NavigableSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/** VisitorClass to determine all variables that occur in a given expression. */
public class VariableDependancy extends DefaultCExpressionVisitor<Void, UnsupportedCodeException> {

  /** Internal variable, that contains all variables that are used in the expression */
  private NavigableSet<Variable> vars = new TreeSet<>();

    /**
     * Construct a new Visitor
     */
    public VariableDependancy() {
      vars=new TreeSet<>();
    }

  /**
   * Return a Set of all variables that occured.
   *
   * @return A Set of all variables that occured.
   */
  public NavigableSet<Variable> getResult() {
      return vars;
    }

    @Override
    protected Void visitDefault(CExpression pExpr) {
       return null;
    }

  @Override
  public Void visit(CArraySubscriptExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CCastExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CComplexCastExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CFieldReference pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CIdExpression pExpr) throws UnsupportedCodeException {
      Variable v=new Variable(pExpr.getDeclaration().getQualifiedName());
      vars.add(v);

      return null;
    }

  @Override
  public Void visit(CUnaryExpression pExpr) throws UnsupportedCodeException {
      pExpr.getOperand().accept(this);
      return null;
    }

  @Override
  public Void visit(CPointerExpression pIastUnaryExpression) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CBinaryExpression pExpr) throws UnsupportedCodeException {
      pExpr.getOperand1().accept(this);
      pExpr.getOperand2().accept(this);
      return null;
    }

  @Override
  public Void visit(CCharLiteralExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CImaginaryLiteralExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CFloatLiteralExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CIntegerLiteralExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CStringLiteralExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CTypeIdExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

  @Override
  public Void visit(CAddressOfLabelExpression pExpr) throws UnsupportedCodeException {
      return null;
    }

}
