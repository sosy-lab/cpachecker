/*
* CPAchecker is a tool for configurable software verification.
* This file is part of CPAchecker.
*
* Copyright (C) 2007-2010 Dirk Beyer
* All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*
* CPAchecker web page:
* http://cpachecker.sosy-lab.org
*/

package org.sosy_lab.cpachecker.cpa.art;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

/** The Class ErrorPathShrinker gets an targetPath and creates a new Path, 
 * with only the important edges of the Path. The idea behind this Class is, 
 * that not every action (CFAEdge) before an error is important for the error, 
 * only a few actions (CFAEdges) are important. */
public class ErrorPathShrinker {

  /** The function shrinkErrorPath gets an targetPath and creates a new Path, 
   * with only the important edges of the Path. 
   * It is the "main"-function of this Class and it only iterates the Path 
   * and checks every CFAEdge
   * 
   * @param targetPath the "long" targetPath
   * @return errorPath the "short" errorPath */
  public static final Path shrinkErrorPath(Path targetPath) {

    Path errorPath = new Path();

    // Set<String> for storing the important variables
    final Set<String> importantVars = new HashSet<String>();

    // reverse iterator, from Error to rootNode
    Iterator<Pair<ARTElement, CFAEdge>> iterator =
        targetPath.descendingIterator();

    // the last element of the errorPath is the errorNode,
    errorPath.addFirst(iterator.next());

    // the last "action" before the errorNode is in the secondlast element
    handle(iterator.next().getSecond(), importantVars);

    // iterate through the Path (backwards) and collect all important variables
    while (iterator.hasNext()) {

/*
      // output for testing
      System.out.print("importantVars: { ");
      for (String var : importantVars) {
        System.out.print(var + " , ");
      }
      System.out.println(" }");
*/
      Pair<ARTElement, CFAEdge> cfaEdge = iterator.next();

      boolean isCFAEdgeImportant = handle(cfaEdge.getSecond(), importantVars);

      if (isCFAEdgeImportant) {
        errorPath.addFirst(cfaEdge);
      }

    }
    return errorPath;
  }

  /** This function returns, if the edge is important. 
   * 
   * @param cfaEdge the edge to prove
   * @param importantVars Set of important variables
   * @return isImportantEdge
   */
  private static boolean handle(CFAEdge cfaEdge, Set<String> importantVars) {

    // check the type of the edge
    switch (cfaEdge.getEdgeType()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:

      /* this is the statement edge which leads the function to the last node
      * of its CFA (not same as a return edge) */
      if (cfaEdge.isJumpEdge()) {
        return handleExitFromFunction((StatementEdge) cfaEdge, importantVars);
      }
      // this is a regular statement
      else {
        return handleStatement((StatementEdge) cfaEdge, importantVars);
      }

      // edge is a declaration edge, e.g. int a;
    case DeclarationEdge:
      return handleDeclaration((DeclarationEdge) cfaEdge, importantVars);

      // this is an assumption, e.g. if(a == b)
    case AssumeEdge:
      return handleAssumption(((AssumeEdge) cfaEdge).getExpression(),
          importantVars);

    case BlankEdge:
      return false; // a blank edge is not important

    case FunctionCallEdge:
      return handleFunctionCall((FunctionCallEdge) cfaEdge, importantVars);

      // this is a return edge from function, this is different from return 
      // statement of the function. See case in statement edge for details
    case ReturnEdge:
      return handleFunctionReturn((ReturnEdge) cfaEdge, importantVars);

    default:
    }
    return true;
  }

  /**
   * This method handles variable declarations.
   * 
   * @param declarationEdge the edge to prove
   * @param importantVars Set of important variables
   * @return isImportantEdge
   */
  private static boolean handleDeclaration(DeclarationEdge declarationEdge,
      Set<String> importantVars) {
    // "int a;" --> declarator "a"
    // TODO: "int a=5;" --> declarator "a"
    String varName = declarationEdge.getDeclarators()[0].getRawSignature();
    return importantVars.contains(varName);
  }

  /**
   * This method handles assumptions (a==b, a<=b, etc.).
   */
  private static boolean handleAssumption(IASTExpression assumeExp,
      Set<String> importantVars) {

    // first, unpack the expression to deal with a raw assumption
    if (assumeExp instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression) assumeExp);

      switch (unaryExp.getOperator()) {
      // remove brackets
      case IASTUnaryExpression.op_bracketedPrimary:
        return handleAssumption(unaryExp.getOperand(), importantVars);

        // remove negation
      case IASTUnaryExpression.op_not:
        return handleAssumption(unaryExp.getOperand(), importantVars);

      default:
        return true;
      }
    }

    // a plain (boolean) identifier, e.g. if(a), always add to importantVars
    else if (assumeExp instanceof IASTIdExpression) {
      String varName = (assumeExp.getRawSignature());
      importantVars.add(varName);
      return true;
    }

    // "exp1 op exp2", where expX is only a literal or a identifier, 
    // add identifier to importantVars
    else if (assumeExp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = (IASTBinaryExpression) assumeExp;
      IASTExpression operand1 = binExp.getOperand1();
      IASTExpression operand2 = binExp.getOperand2();

      if (operand1 instanceof IASTIdExpression)
        importantVars.add(operand1.getRawSignature());

      if (operand2 instanceof IASTIdExpression)
        importantVars.add(operand2.getRawSignature());

      return true;
    }

    // default (if edge cannot be handled, it could be important)
    return true;
  }

  private static boolean handleFunctionCall(FunctionCallEdge pCfaEdge,
      Set<String> importantVars) {
    // TODO Auto-generated method stub
    return true;
  }

  private static boolean handleFunctionReturn(ReturnEdge pCfaEdge,
      Set<String> importantVars) {
    // TODO Auto-generated method stub
    return true;
  }

  private static boolean handleExitFromFunction(StatementEdge statementEdge,
      Set<String> importantVars) {
    // nothing to do?
    return true;
  }

  private static boolean handleStatement(StatementEdge statementEdge,
      Set<String> importantVars) {

    // a unary operation, e.g. a++
    // this does not change the Set of important variables, 
    // but the edge could be important
    if (statementEdge.getExpression() instanceof IASTUnaryExpression)
      return handleUnaryStatement(statementEdge, importantVars);

    // expression is a binary operation, e.g. a = b;
    else if (statementEdge.getExpression() instanceof IASTBinaryExpression)
      return handleBinaryStatement(statementEdge, importantVars);

    // ext();
    else if (statementEdge.getExpression() instanceof IASTFunctionCallExpression)
      return true;

    // a; 
    else if (statementEdge.getExpression() instanceof IASTIdExpression) {
      String varName = statementEdge.getExpression().getRawSignature();
      return importantVars.contains(varName);
    }

    else
      return true;
  }

  /**
   * This method handles unary statements.
   *
   * @param statementEdge the edge to prove
   * @param importantVars Set of important variables
   * @return isImportantEdge
   */
  private static boolean handleUnaryStatement(StatementEdge statementEdge,
      Set<String> importantVars) {

    // get unaryExpression, i.e. "a++"
    IASTUnaryExpression unaryExpression =
        (IASTUnaryExpression) statementEdge.getExpression();

    // get operand, i.e. "a"
    IASTExpression operand = unaryExpression.getOperand();

    // if operand is a identifier and is not important, the edge is not 
    // important, in any other case the edge could be important
    if (operand instanceof IASTIdExpression
        && !importantVars.contains(operand.getRawSignature())) {
      return false;
    } else {
      return true;
    }

  }

  /**
   * This method handles binary statements.
   *
   * @param declarationEdge the edge to prove
   * @param importantVars Set of important variables
   * @return isImportantEdge
   */
  private static boolean handleBinaryStatement(StatementEdge statementEdge,
      Set<String> importantVars) {

    IASTBinaryExpression binaryExpression =
        (IASTBinaryExpression) statementEdge.getExpression();

    switch (binaryExpression.getOperator()) {
    // a = ?
    case IASTBinaryExpression.op_assign:
      return handleAssignment(binaryExpression, importantVars);

      // a += ?, a-= ?
    case IASTBinaryExpression.op_plusAssign:
    case IASTBinaryExpression.op_minusAssign:
    case IASTBinaryExpression.op_multiplyAssign:
    case IASTBinaryExpression.op_shiftLeftAssign:
    case IASTBinaryExpression.op_shiftRightAssign:
    case IASTBinaryExpression.op_binaryAndAssign:
    case IASTBinaryExpression.op_binaryXorAssign:
    case IASTBinaryExpression.op_binaryOrAssign:
      return handleOperationAndAssign(binaryExpression, importantVars);

    default:
      return true;
    }
  }

  private static boolean handleOperationAndAssign(
      IASTBinaryExpression binaryExpression, Set<String> importantVars) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * This method handles assignments.
   *
   * @param binaryExpression a binary expression
   * @param declarationEdge the CFA edge
   * @return isImportantEdge
   */
  private static boolean handleAssignment(
      IASTBinaryExpression binaryExpression, Set<String> importantVars) {

    IASTExpression lParam = binaryExpression.getOperand1();
    IASTExpression rightExp = binaryExpression.getOperand2();

    // a = ?
    if (lParam instanceof IASTIdExpression)
      return handleAssignmentToVariable(lParam.getRawSignature(), rightExp,
          importantVars);

    // TODO: assignment to pointer, *a = ?
    else if (lParam instanceof IASTUnaryExpression
        && ((IASTUnaryExpression) lParam).getOperator() == IASTUnaryExpression.op_star)
      return true;

    // TODO assignment to field, a->b = ?
    else if (lParam instanceof IASTFieldReference)
      return true;

    // TODO assignment to array cell, a[b] = ?
    else if (lParam instanceof IASTArraySubscriptExpression)
      return true;

    else
      // if the edge is not unimportant, this edge could be important.
      return true;
  }

  /**
   * This method handles the assignment of a variable.
   *
   * @param lParam the local name of the variable to assign to
   * @param rightExp the assigning expression
   * @return isImportantEdge
   */
  private static boolean handleAssignmentToVariable(String lParam,
      IASTExpression rightExp, Set<String> importantVars) {

    // a = 8.2 or "return;" (when rightExp == null)
    if (rightExp instanceof IASTLiteralExpression || rightExp == null)
      return handleAssignmentOfLiteral(lParam, rightExp, importantVars);

    // a = b
    else if (rightExp instanceof IASTIdExpression)
      return handleAssignmentOfVariable(lParam, rightExp, importantVars);

    // a = (cast) ?
    else if (rightExp instanceof IASTCastExpression)
      return handleAssignmentOfCast(lParam, (IASTCastExpression) rightExp,
          importantVars);

    // a = -b
    else if (rightExp instanceof IASTUnaryExpression)
      return handleAssignmentOfUnaryExp(lParam, (IASTUnaryExpression) rightExp,
          importantVars);

    // a = b op c
    else if (rightExp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = (IASTBinaryExpression) rightExp;

      return handleAssignmentOfBinaryExp(lParam, binExp.getOperand1(),
          binExp.getOperand2(), binExp.getOperator(), importantVars);
    }

    // TODO: a = func(); or a = b->c; currently, the interval of a is unbound
    else if (rightExp instanceof IASTFunctionCallExpression
        || rightExp instanceof IASTFieldReference) return true;

    // if the edge is not unimportant, this edge could be important.
    return true;
  }

  /**
   * This method handles the assignment of a literal to a variable.
   * (i.e. "a = 5;" or "return;" (when rightExp == null))
   *
   * @param lParam the local name of the variable to assign to
   * @param op2 the expression representing the literal
   * @return isEdgeImportant
   */
  private static boolean handleAssignmentOfLiteral(String lParam,
      IASTExpression rightExp, Set<String> importantVars) {

    // this does not change the Set importantVars
    // the edge is important, if "a" is used later in the code 
    // (if it is part of the importantVars-Set)
    return importantVars.contains(lParam);
  }

  private static boolean handleAssignmentOfVariable(String lParam,
      IASTExpression rightExp, Set<String> importantVars) {

    // a = b
    if (importantVars.isEmpty() || importantVars.contains(lParam)) {
      importantVars.add(rightExp.getRawSignature());
      return true;
    } else
      return false;
  }

  private static boolean handleAssignmentOfBinaryExp(String lParam,
      IASTExpression pOperand1, IASTExpression pOperand2, int pOperator,
      Set<String> importantVars) {
    // TODO Auto-generated method stub
    return true;
  }

  private static boolean handleAssignmentOfUnaryExp(String lParam,
      IASTUnaryExpression rightExp, Set<String> importantVars) {
    // TODO Auto-generated method stub
    return true;
  }

  private static boolean handleAssignmentOfCast(String lParam,
      IASTCastExpression rightExp, Set<String> importantVars) {
    // TODO Auto-generated method stub
    return true;
  }

}