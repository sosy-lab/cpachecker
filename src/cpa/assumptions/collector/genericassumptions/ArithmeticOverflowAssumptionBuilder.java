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
package cpa.assumptions.collector.genericassumptions;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.c.CASTVisitor;

import assumptions.DummyASTBinaryExpression;
import assumptions.DummyASTNumericalLiteralExpression;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.StatementEdge;

import common.Pair;


/**
 * Class to generate assumptions related to over/underflow 
 * of integer arithmetic operations
 * 
 * @author g.theoduloz
 */
public class ArithmeticOverflowAssumptionBuilder
  implements GenericAssumptionBuilder
{
  private static Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression> boundsForType(IType typ)
  {
    try {
      if (typ instanceof IBasicType) {
        IBasicType btyp = (IBasicType) typ;
        switch (btyp.getType()) {
        case IBasicType.t_int:
          if (btyp.isLong())
            if (btyp.isUnsigned())
              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
                (DummyASTNumericalLiteralExpression.ULONG_MIN, DummyASTNumericalLiteralExpression.ULONG_MAX);
            else
              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
                (DummyASTNumericalLiteralExpression.LONG_MIN, DummyASTNumericalLiteralExpression.LONG_MAX);
          else if (btyp.isShort())
            if (btyp.isUnsigned())
              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
                (DummyASTNumericalLiteralExpression.USHRT_MIN, DummyASTNumericalLiteralExpression.USHRT_MAX);
            else
              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
                (DummyASTNumericalLiteralExpression.SHRT_MIN, DummyASTNumericalLiteralExpression.SHRT_MAX);
          else
            if (btyp.isUnsigned())
              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
                (DummyASTNumericalLiteralExpression.UINT_MIN, DummyASTNumericalLiteralExpression.UINT_MAX);
            else
              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
                (DummyASTNumericalLiteralExpression.INT_MIN, DummyASTNumericalLiteralExpression.INT_MAX);
        case IBasicType.t_char:
          if (btyp.isUnsigned())
            return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
              (DummyASTNumericalLiteralExpression.UCHAR_MIN, DummyASTNumericalLiteralExpression.UCHAR_MAX);
          else
            return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
              (DummyASTNumericalLiteralExpression.CHAR_MIN, DummyASTNumericalLiteralExpression.CHAR_MAX);
        }
      }
    } catch (DOMException e) { /* oops... just ignore, and return (null, null) */ }
      return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
        (null, null);
    }
  
  /**
   * Visitor to produce the invariant.
   * Note it is only intended to be used on code
   * occurring on one single edge.
   * 
   * @author g.theoduloz
   */
  private class CustomASTVisitor
    extends CASTVisitor
  {
    // Fields to accumulate the built invariants
    private IASTExpression result;
    
    /**
     * Default constructor. The result is initially reseted.
     */
    public CustomASTVisitor()
    {
      reset();
      shouldVisitExpressions = true;
    }
    
    /**
     * Returns the invariant accumulated so far
     * @return A non-null predicate
     */
    public IASTExpression getResult()
    {
      return result;
    }
    
    /**
     * Reset the visitor by dropping the invariant computed so far
     */
    public void reset()
    {
      result = DummyASTNumericalLiteralExpression.TRUE;
    }
    
    /**
     * Compute and conjunct the assumption for the given arithmetic
     * expression. The method does not check that the expression is
     * indeed an arithmetic expression.
     */
    private void conjunctPredicateForArithmeticExpression(IASTExpression exp)
    {
      conjunctPredicateForArithmeticExpression(exp, false, false);
    }
    
    /**
     * Compute and conjunct the assumption for the given arithmetic
     * expression, ignoring bounds if applicable. The method does
     * not check that the expression is indeed an arithmetic expression.
     */
    private void conjunctPredicateForArithmeticExpression(IASTExpression exp, boolean ignoreLower, boolean ignoreUpper)
    {
      conjunctPredicateForArithmeticExpression(exp.getExpressionType(), exp, ignoreLower, ignoreUpper);
    }
    
    /**
     * Compute and conjunct the assumption for the given arithmetic
     * expression, given as its type and its expression.
     * The two last, boolean arguments allow to avoid generating
     * lower and/or upper bounds predicates.
     */
    private void conjunctPredicateForArithmeticExpression(
        IType typ, IASTExpression exp,
        boolean ignoreLower, boolean ignoreUpper)
    {
      Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression> bounds = boundsForType(typ);
      if ((!ignoreLower) && (bounds.getFirst() != null))
        result = new DummyASTBinaryExpression(
            IASTBinaryExpression.op_logicalAnd,
            result,
            new DummyASTBinaryExpression(
                IASTBinaryExpression.op_greaterEqual,
                exp,
                bounds.getFirst()));
      
      if ((!ignoreUpper) && (bounds.getSecond() != null))
        result = new DummyASTBinaryExpression(
            IASTBinaryExpression.op_logicalAnd,
            result,
            new DummyASTBinaryExpression(
                IASTBinaryExpression.op_lessEqual,
                exp,
                bounds.getSecond()));
    }
    
    /**
     * Analyse a term to determine whether we can immediately
     * say whether it is positive, null, or negative
     * @return a pair <n,p> s.t. n => t <= 0, p => t >= 0
     */
    private Pair<Boolean, Boolean> analyzeTermSign(IASTExpression t)
    {
      boolean isNegative = false;
      boolean isNull = false;
      boolean isPositive = false;
      
      if (t instanceof IASTLiteralExpression)
      {
        IASTLiteralExpression lit = (IASTLiteralExpression)t;
        switch (lit.getKind())
        {
        case (IASTLiteralExpression.lk_integer_constant):
        case (IASTLiteralExpression.lk_float_constant):  
          String repr = lit.getRawSignature();
          if (repr.charAt(0) == '-')
            isNegative = true;
          else
            isPositive = true;
          
          isNull = true;
          for (char c : repr.toCharArray()) {
            if ((c != '0') && Character.isDigit(c)) {
              isNull = false;
              break;
            }
          }
        }
      }
      else
      {
        IType typ = t.getExpressionType();
        if (typ instanceof IBasicType) {
          try {
            if (((IBasicType)typ).isUnsigned())
              isPositive = true;
          } catch (DOMException e) { }
        }
      }
      
      return new Pair<Boolean, Boolean>(isNegative || isNull, isPositive || isNull);
    }
    
    @Override
    public int visit(IASTExpression pExpression) {
      if (pExpression instanceof IASTBinaryExpression)
      {
        IASTBinaryExpression binexp = (IASTBinaryExpression)pExpression;
        int op = binexp.getOperator();
        
        // Sign analysis
        Pair<Boolean, Boolean> signs1 = analyzeTermSign(binexp.getOperand1());
        Pair<Boolean, Boolean> signs2 = analyzeTermSign(binexp.getOperand2());
        boolean ignoreLower = false;
        boolean ignoreUpper = false;
        switch (op)
        {
        case IASTBinaryExpression.op_plus:
        case IASTBinaryExpression.op_multiply:
          ignoreLower = signs2.getSecond() || signs1.getSecond();
          ignoreUpper = signs2.getFirst() || signs1.getFirst();
          break;
        case IASTBinaryExpression.op_plusAssign:
        case IASTBinaryExpression.op_multiplyAssign:
          ignoreLower = signs2.getSecond();
          ignoreUpper = signs2.getFirst();
          break;
        case IASTBinaryExpression.op_minus:
          ignoreLower = signs2.getFirst() || signs1.getFirst();
          ignoreUpper = signs2.getSecond() || signs1.getSecond();
          break;
        case IASTBinaryExpression.op_minusAssign:
          ignoreLower = signs2.getFirst();
          ignoreUpper = signs2.getSecond();
          break;
        }
        
        switch (op) {
        case IASTBinaryExpression.op_plus:
        case IASTBinaryExpression.op_minus:
        case IASTBinaryExpression.op_multiply:
          conjunctPredicateForArithmeticExpression(binexp, ignoreLower, ignoreUpper);
          break;
        case IASTBinaryExpression.op_plusAssign: {
          conjunctPredicateForArithmeticExpression(
              binexp.getExpressionType(),
              new DummyASTBinaryExpression(
                    IASTBinaryExpression.op_plus,
                    binexp.getOperand1(),
                    binexp.getOperand2()),
              ignoreLower, ignoreUpper);
          break;
        }
        case IASTBinaryExpression.op_minusAssign:
          conjunctPredicateForArithmeticExpression(
              binexp.getExpressionType(),
              new DummyASTBinaryExpression(
                    IASTBinaryExpression.op_minus,
                    binexp.getOperand1(),
                    binexp.getOperand2()),
              ignoreLower, ignoreUpper);
          break;
        case IASTBinaryExpression.op_multiplyAssign:
          conjunctPredicateForArithmeticExpression(
              binexp.getExpressionType(),
              new DummyASTBinaryExpression(
                    IASTBinaryExpression.op_multiply,
                    binexp.getOperand1(),
                    binexp.getOperand2()),
              ignoreLower, ignoreUpper);
          break;
        }
      }
      else if (pExpression instanceof IASTUnaryExpression)
      {
        IASTUnaryExpression unexp = (IASTUnaryExpression)pExpression;
        switch (unexp.getOperator()) {
        case IASTUnaryExpression.op_minus:
          conjunctPredicateForArithmeticExpression(unexp);
          break;
        case IASTUnaryExpression.op_prefixIncr:
        case IASTUnaryExpression.op_postFixIncr:
          conjunctPredicateForArithmeticExpression(
              unexp.getExpressionType(),
              new DummyASTBinaryExpression(
                    IASTBinaryExpression.op_plus,
                    unexp.getOperand(),
                    DummyASTNumericalLiteralExpression.ONE),
              true, false);
          break;          
        case IASTUnaryExpression.op_prefixDecr:
        case IASTUnaryExpression.op_postFixDecr:
          conjunctPredicateForArithmeticExpression(
              unexp.getExpressionType(),
              new DummyASTBinaryExpression(
                    IASTBinaryExpression.op_minus,
                    unexp.getOperand(),
                    DummyASTNumericalLiteralExpression.ONE),
              false, true);
          break;
        }
      }
      else if (pExpression instanceof IASTCastExpression)
      {
        IASTCastExpression castexp = (IASTCastExpression)pExpression;
        IType fromType = castexp.getOperand().getExpressionType();
        IType toType = castexp.getExpressionType();
        
        Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression> fromBounds = boundsForType(fromType);
        Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression> toBounds = boundsForType(toType);
        
        boolean ignoreLower = true;
        boolean ignoreUpper = true;
        
        if ((fromBounds.getFirst() != null) && (toBounds.getFirst() != null))
          if (fromBounds.getFirst().compareTo(toBounds.getFirst()) < 0)
            ignoreLower = false;
        
        if ((fromBounds.getSecond() != null) && (toBounds.getSecond() != null))
          if (fromBounds.getSecond().compareTo(toBounds.getSecond()) > 0)
            ignoreUpper = false;
        
        conjunctPredicateForArithmeticExpression(toType, castexp.getOperand(), ignoreLower, ignoreUpper); 
      }
      return super.visit(pExpression);
    }
  }

  // visitor
  private CustomASTVisitor visitor = new CustomASTVisitor();
  
  @Override
  public IASTExpression assumptionsForEdge(CFAEdge pEdge) {
    visitor.reset();
    switch (pEdge.getEdgeType()) {
    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge) pEdge;
      assumeEdge.getExpression().accept(visitor);
      break;
    case FunctionCallEdge:
      FunctionCallEdge fcallEdge = (FunctionCallEdge) pEdge;
      for (IASTExpression arg : fcallEdge.getArguments())
      {
        arg.accept(visitor);
      }
      break;
    case StatementEdge:
      StatementEdge stmtEdge = (StatementEdge) pEdge;
      stmtEdge.getExpression().accept(visitor);
      break;
    case MultiStatementEdge:
      MultiStatementEdge mstmtEdge = (MultiStatementEdge) pEdge;
      for (IASTExpression exp : mstmtEdge.getExpressions())
      {
        exp.accept(visitor);
      }
      break;
    }
    return visitor.getResult();
  }

}
