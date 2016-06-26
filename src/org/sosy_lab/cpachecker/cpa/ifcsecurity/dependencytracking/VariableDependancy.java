/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking;

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
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * VisitorClass to determine all variables that occur in a given expression.
 */
public class VariableDependancy extends DefaultCExpressionVisitor<Void, UnsupportedCCodeException> {

  /**
   * Internal variable, that contains all variables that are used in the expression
   */
  private SortedSet<Variable> vars=new TreeSet<>();

    /**
     * Construct a new Visitor
     */
    public VariableDependancy() {
      vars=new TreeSet<>();
    }

    /**
     * Return a Set of all variables that occured.
     * @return A Set of all variables that occured.
     */
    public SortedSet<Variable> getResult(){
      return vars;
    }

    @Override
    protected Void visitDefault(CExpression pExpr) {
       return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CCastExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CFieldReference pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CIdExpression pExpr) throws UnsupportedCCodeException {
      Variable v=new Variable(pExpr.getDeclaration().getQualifiedName());
      vars.add(v);

      return null;
    }

    @Override
    public Void visit(CUnaryExpression pExpr) throws UnsupportedCCodeException {
      pExpr.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CPointerExpression pIastUnaryExpression) throws UnsupportedCCodeException {
      return null;
    }


    @Override
    public Void visit(CBinaryExpression pExpr) throws UnsupportedCCodeException {
      pExpr.getOperand1().accept(this);
      pExpr.getOperand2().accept(this);
      return null;
    }


    @Override
    public Void visit(CCharLiteralExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CImaginaryLiteralExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CFloatLiteralExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CIntegerLiteralExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CStringLiteralExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

    @Override
    public Void visit(CTypeIdExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }


    @Override
    public Void visit(CAddressOfLabelExpression pExpr) throws UnsupportedCCodeException {
      return null;
    }

}
