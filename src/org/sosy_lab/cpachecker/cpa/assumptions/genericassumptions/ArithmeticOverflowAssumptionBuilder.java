/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.c.CASTVisitor;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.util.assumptions.DummyASTBinaryExpression;
import org.sosy_lab.cpachecker.util.assumptions.DummyASTIdExpression;
import org.sosy_lab.cpachecker.util.assumptions.DummyASTNumericalLiteralExpression;

/**
 * Class to generate assumptions related to over/underflow
 * of integer arithmetic operations
 *
 * @author g.theoduloz
 */
public class ArithmeticOverflowAssumptionBuilder
implements GenericAssumptionBuilder
{

  public static boolean isDeclGlobal = false;

  private static Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression> boundsForType(IType typ)
  {
    try {
      if (typ instanceof IBasicType) {
        IBasicType btyp = (IBasicType) typ;

        switch (btyp.getType()) {
        case IBasicType.t_int:
          // TODO not handled yet by mathsat so we assume all vars are signed integers for now
          // will enable later
          return Pair.of
          (DummyASTNumericalLiteralExpression.INT_MIN, DummyASTNumericalLiteralExpression.INT_MAX);
          //          if (btyp.isLong())
          //            if (btyp.isUnsigned())
          //              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
          //          (DummyASTNumericalLiteralExpression.ULONG_MIN, DummyASTNumericalLiteralExpression.ULONG_MAX);
          //            else
          //              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
          //          (DummyASTNumericalLiteralExpression.LONG_MIN, DummyASTNumericalLiteralExpression.LONG_MAX);
          //          else if (btyp.isShort())
          //            if (btyp.isUnsigned())
          //              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
          //          (DummyASTNumericalLiteralExpression.USHRT_MIN, DummyASTNumericalLiteralExpression.USHRT_MAX);
          //            else
          //              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
          //          (DummyASTNumericalLiteralExpression.SHRT_MIN, DummyASTNumericalLiteralExpression.SHRT_MAX);
          //          else
          //            if (btyp.isUnsigned())
          //              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
          //          (DummyASTNumericalLiteralExpression.UINT_MIN, DummyASTNumericalLiteralExpression.UINT_MAX);
          //            else
          //              return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
          //          (DummyASTNumericalLiteralExpression.INT_MIN, DummyASTNumericalLiteralExpression.INT_MAX);
          //        case IBasicType.t_char:
          //          if (btyp.isUnsigned())
          //            return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
          //          (DummyASTNumericalLiteralExpression.UCHAR_MIN, DummyASTNumericalLiteralExpression.UCHAR_MAX);
          //          else
          //            return new Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression>
          //          (DummyASTNumericalLiteralExpression.CHAR_MIN, DummyASTNumericalLiteralExpression.CHAR_MAX);
        }
      }
    } catch (DOMException e) { /* oops... just ignore, and return (null, null) */ }
    return Pair.of(null, null);
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
    private IASTNode result;

    /**
     * Default constructor. The result is initially reseted.
     */
    public CustomASTVisitor()
    {
      reset();
      shouldVisitExpressions = true;
      shouldVisitDeclarations = true;
    }

    /**
     * Returns the invariant accumulated so far
     * @return A non-null predicate
     */
    public IASTNode getResult()
    {
      return result;
    }

    //    public IASTDeclaration getDecl(){
    //      return declaration;
    //    }

    /**
     * Reset the visitor by dropping the invariant computed so far
     */
    public void reset()
    {
      result = DummyASTNumericalLiteralExpression.TRUE;
    }

    /**
     * Compute and conjunct the assumption for the given arithmetic
     * expression, ignoring bounds if applicable. The method does
     * not check that the expression is indeed an arithmetic expression.
     */
    private void conjunctPredicateForArithmeticExpression(IASTExpression exp)
    {
      conjunctPredicateForArithmeticExpression(exp.getExpressionType(), exp);
    }

    /**
     * Compute and conjunct the assumption for the given arithmetic
     * expression, given as its type and its expression.
     * The two last, boolean arguments allow to avoid generating
     * lower and/or upper bounds predicates.
     */
    private void conjunctPredicateForArithmeticExpression(
        IType typ, IASTExpression exp)
    {
      Pair<DummyASTNumericalLiteralExpression, DummyASTNumericalLiteralExpression> bounds = boundsForType(typ);
      if (bounds.getFirst() != null){
        result = new DummyASTBinaryExpression(
            IASTBinaryExpression.op_logicalAnd,
            (IASTExpression)result,
            new DummyASTBinaryExpression(
                IASTBinaryExpression.op_greaterEqual,
                exp,
                bounds.getFirst()));
      }
      if (bounds.getSecond() != null){
        result = new DummyASTBinaryExpression(
            IASTBinaryExpression.op_logicalAnd,
            (IASTExpression)result,
            new DummyASTBinaryExpression(
                IASTBinaryExpression.op_lessEqual,
                exp,
                bounds.getSecond()));
      }
    }

    @SuppressWarnings("static-access")
    @Override
    public int visit(IASTDeclaration pDeclaration) {
      result = pDeclaration;
      return super.PROCESS_ABORT;
    }

    @SuppressWarnings("static-access")
    @Override
    public int visit(IASTExpression pExpression) {
      if(pExpression instanceof IASTIdExpression){
        conjunctPredicateForArithmeticExpression(pExpression);
      }
      if (pExpression instanceof IASTBinaryExpression)
      {
        IASTBinaryExpression binexp = (IASTBinaryExpression)pExpression;
        IASTExpression op1 = binexp.getOperand1();
        // Only variables for now, ignoring * & operators
        if(op1 instanceof IASTIdExpression){
          conjunctPredicateForArithmeticExpression(op1);
        }
      }
      else if (pExpression instanceof IASTUnaryExpression)
      {
        IASTUnaryExpression unexp = (IASTUnaryExpression)pExpression;
        IASTExpression op1 = unexp.getOperand();
        // Only variables. Ignoring * & operators for now
        if(op1 instanceof IASTIdExpression){
          conjunctPredicateForArithmeticExpression(op1);
        }
      }
      else if (pExpression instanceof IASTCastExpression)
      {
        IASTCastExpression castexp = (IASTCastExpression)pExpression;
        IType toType = castexp.getExpressionType();
        conjunctPredicateForArithmeticExpression(toType, castexp.getOperand());
      }
      // we don't want to continue, the assumption talks only
      // about the left-hand side of the statement
      // if we want to analyze rhs, call super.visit
      //      return super.visit(pExpression);
      return super.PROCESS_ABORT;
    }
  }

  // visitor
  private CustomASTVisitor visitor = new CustomASTVisitor();

  @Override
  public IASTNode assumptionsForEdge(CFAEdge pEdge) {
    visitor.reset();
    switch (pEdge.getEdgeType()) {
    case DeclarationEdge:
      DeclarationEdge declarationEdge = (DeclarationEdge) pEdge;
      declarationEdge.getRawAST().accept(visitor);
      isDeclGlobal = declarationEdge.isGlobal();
      break;
    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge) pEdge;
      assumeEdge.getExpression().accept(visitor);
      break;
    case FunctionCallEdge:
      FunctionCallEdge fcallEdge = (FunctionCallEdge) pEdge;
      if (!fcallEdge.getArguments().isEmpty()) {
        FunctionDefinitionNode fdefnode = fcallEdge.getSuccessor();
        List<? extends IASTParameterDeclaration> formalParams = fdefnode.getFunctionParameters();
        for (IASTParameterDeclaration paramdecl : formalParams)
        {
          DummyASTIdExpression exp = new DummyASTIdExpression(paramdecl.getDeclarator().getName());
          exp.accept(visitor);
        }
      }
      break;
    case StatementEdge:
      StatementEdge stmtEdge = (StatementEdge) pEdge;

      IASTExpression iastExp = stmtEdge.getExpression();
      // TODO replace with a global nondet variable
      if(iastExp != null && iastExp.getRawSignature().contains("__BLAST_NONDET")){
        break;
      }

      if(stmtEdge.getExpression() != null){
        stmtEdge.getExpression().accept(visitor);
      }
      break;
    case ReturnStatementEdge:
      ReturnStatementEdge returnEdge = (ReturnStatementEdge) pEdge;

      if(returnEdge.getExpression() != null){
        returnEdge.getExpression().accept(visitor);
      }
      break;
    }
    return visitor.getResult();
  }
}
