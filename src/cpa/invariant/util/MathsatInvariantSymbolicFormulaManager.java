/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.invariant.util;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.mathsat.MathsatSymbolicFormula;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;

/**
 * Implementation of InvariantSymbolicFormulaManager based on
 * MathsatSymbolicFormulaManager. This is a singleton class.
 * 
 * @author g.theoduloz
 */
public class MathsatInvariantSymbolicFormulaManager
  extends MathsatSymbolicFormulaManager
  implements InvariantSymbolicFormulaManager
{
  
  /**
   * Dummy SSA map that always return 1 as index. Only used here
   * to circumvent the assumptions of MathsatSymbolicFormulaManager
   */
  private static class DummySSAMap
    extends SSAMap
  {
    @Override
    public int getIndex(String pName, SymbolicFormula[] pArgs) {
      return 1;
    }
    
    @Override
    public int getIndex(String pVariable) {
      return 1;
    }
    
    @Override
    public void setIndex(String pName, SymbolicFormula[] pArgs, int pIdx) {
    }
    
    @Override
    public void setIndex(String pVariable, int pIdx) {
    }
  }
  
  private static MathsatInvariantSymbolicFormulaManager instance = null;
  
  /**
   * Return the singleton instance for this class
   */
  public static MathsatInvariantSymbolicFormulaManager getInstance()
  {
    if (instance == null)
      instance = new MathsatInvariantSymbolicFormulaManager();
    return instance;
  }

  /**
   * Private constructor. To get instance, call getInstance()
   */
  private MathsatInvariantSymbolicFormulaManager() {
    super();
  }
  
  private final SSAMap dummySSAMap = new DummySSAMap(); 

  public SymbolicFormula buildSymbolicFormula(IASTExpression p)
  {
    return buildSymbolicFormula(p, true);
  }
  
  private SymbolicFormula buildSymbolicFormula(IASTExpression p, boolean sign)
  {
    // first, check whether we have &&, ||, or !
    if (p instanceof IASTBinaryExpression) {
      IASTBinaryExpression binop = (IASTBinaryExpression) p;
      switch (binop.getOperator()) {
      case IASTBinaryExpression.op_logicalAnd:
        if (sign)
          return makeAnd(
              buildSymbolicFormula(binop.getOperand1(), true),
              buildSymbolicFormula(binop.getOperand2(), true));
        else
          return makeOr(
              buildSymbolicFormula(binop.getOperand1(), false),
              buildSymbolicFormula(binop.getOperand2(), false));
      case IASTBinaryExpression.op_logicalOr:
        if (sign)
          return makeOr(
              buildSymbolicFormula(binop.getOperand1(), true),
              buildSymbolicFormula(binop.getOperand2(), true));
        else
          return makeAnd(
              buildSymbolicFormula(binop.getOperand1(), false),
              buildSymbolicFormula(binop.getOperand2(), false));
      }
    } else if (p instanceof IASTUnaryExpression) {
      IASTUnaryExpression unop = (IASTUnaryExpression) p;
      if (unop.getOperator() == IASTUnaryExpression.op_not)
        return buildSymbolicFormula(unop.getOperand(), !sign);
    }
    
    // atomic formula    
    MathsatSymbolicFormula ssaFormula = buildFormulaPredicate(p, sign, dummySSAMap, false);
    return uninstantiate(ssaFormula);
  }
  
  @Override
  public SymbolicFormula makeAnd(SymbolicFormula f, IASTExpression p) {
    return makeAnd(f, buildSymbolicFormula(p));
  }

}
