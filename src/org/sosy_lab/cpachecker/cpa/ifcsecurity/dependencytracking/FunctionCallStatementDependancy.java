// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * VisitorClass to determine all variables and and a functioncalls that occur in a given expression.
 */
public class FunctionCallStatementDependancy extends VariableDependancy
    implements CRightHandSideVisitor<Void, UnsupportedCodeException> {

    /**
     * Internal variable: Function that is called
     */
    private Variable functionname=null;

    /**
     * Construct a new Visitor
     */
    public FunctionCallStatementDependancy() {
    }

    /**
     * Return the function called.
     * @return  Return the function called.
     */
    public Variable getFunctionname(){
      return functionname;
    }

  @Override
  public Void visit(CFunctionCallExpression pExpr) throws UnsupportedCodeException {
      functionname=new Variable(pExpr.getFunctionNameExpression().toASTString());
      for(CExpression param:pExpr.getParameterExpressions()){
        param.accept(this);
      }
      return null;
    }

}
