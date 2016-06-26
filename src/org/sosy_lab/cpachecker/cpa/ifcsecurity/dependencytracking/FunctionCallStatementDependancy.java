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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

/**
 * VisitorClass to determine all variables and and a functioncalls that occur in a given expression.
 */
public class FunctionCallStatementDependancy extends VariableDependancy implements CRightHandSideVisitor<Void, UnsupportedCCodeException> {

    /**
     * Internal variable: Function that is called
     */
    private Variable functionname=null;

    /**
     * Construct a new Visitor
     */
    public FunctionCallStatementDependancy() {
      super();
    }

    /**
     * Return the function called.
     * @return  Return the function called.
     */
    public Variable getFunctionname(){
      return functionname;
    }


    @Override
    public Void visit(CFunctionCallExpression pExpr) throws UnsupportedCCodeException {
      functionname=new Variable(pExpr.getFunctionNameExpression().toASTString());
      for(CExpression param:pExpr.getParameterExpressions()){
        param.accept(this);
      }
      return null;
    }

}
