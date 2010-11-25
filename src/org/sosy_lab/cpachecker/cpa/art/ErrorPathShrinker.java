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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
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
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

/** The Class ErrorPathShrinker gets an targetPath and creates a new Path, 
 * with only the important edges of the Path. The idea behind this Class is, 
 * that not every action (CFAEdge) before an error occurs is important for 
 * the error, only a few actions (CFAEdges) are important. 
 * 
 * @author Friedberger Karlheinz
 */
public class ErrorPathShrinker {

  /** Set<String> for storing the important variables */
  private final static Set<String> importantVars   =
                                                       new LinkedHashSet<String>();

  /** Set<String> for storing the global variables */
  private final static Set<String> globalVars      =
                                                       new LinkedHashSet<String>();

  private static boolean           printForTesting = false;

  /** The function shrinkErrorPath gets an targetPath and creates a new Path, 
   * with only the important edges of the Path. 
   * It only iterates the Path and checks every CFAEdge
   * 
   * @param targetPath the "long" targetPath
   * @return errorPath the "short" errorPath */
  public static final Path shrinkErrorPath(Path targetPath) {

    // first collect all global variables
    findGolbalVarsInPath(targetPath);

    Path errorPath = new Path();

    // reverse iterator, from errorNode to rootNode
    Iterator<Pair<ARTElement, CFAEdge>> revIterator =
        targetPath.descendingIterator();

    // the last element of the errorPath is the errorNode (important),
    errorPath.addFirst(revIterator.next());

    // iterate through the Path (backwards) and collect all important variables
    while (revIterator.hasNext()) {

      Pair<ARTElement, CFAEdge> cfaEdgePair = revIterator.next();
      CFAEdge cfaEdge = cfaEdgePair.getSecond();
      boolean isCFAEdgeImportant = true;

      // check the type of the edge
      switch (cfaEdge.getEdgeType()) {

      // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:

        /* this is the statement edge which leads the function to the last node
        * of its CFA (not same as a return edge) */
        if (cfaEdge.isJumpEdge()) {
          System.out.println("exitFromFuctionEdge : " + cfaEdge.toString());
          
          isCFAEdgeImportant =
              handleExitFromFunction(((StatementEdge) cfaEdge).getExpression());
          
          // TODO handle complete functions, from functionExit to functionCall
          cfaEdgePair = revIterator.next();
          while (!(cfaEdgePair.getSecond() instanceof FunctionCallEdge)){
            // TODO handle all edges in a function, 
            // globalVars are important, if they are used later. 
            // other variables are important, if the function is part of a assignment ("a=func();")
            System.out.println("Edge : " + cfaEdgePair.getSecond().toString());
            cfaEdgePair = revIterator.next();
          }
          
        }
        // this is a regular statement
        else {
          isCFAEdgeImportant =
              handleStatement(((StatementEdge) cfaEdge).getExpression());
        }
        break;

      // edge is a declaration edge, e.g. int a;
      case DeclarationEdge:
        isCFAEdgeImportant = handleDeclaration((DeclarationEdge) cfaEdge);
        break;

      // this is an assumption, e.g. if(a == b)
      case AssumeEdge:
        isCFAEdgeImportant =
            handleAssumption(((AssumeEdge) cfaEdge).getExpression());
        break;

      /* There are several BlankEdgeTypes: 
       * a jumpEdge ("goto label") is important, 
       * a labelEdge and other Types maybe, a really blank edge is not. 
       * TODO are there more types? */
      case BlankEdge:
        isCFAEdgeImportant = cfaEdge.isJumpEdge();
        break;

      case FunctionCallEdge:
        isCFAEdgeImportant = handleFunctionCall((FunctionCallEdge) cfaEdge);
        break;

      // this is a return edge from function, this is different from return 
      // statement of the function. See case in statement edge for details
      case ReturnEdge:
        isCFAEdgeImportant = handleFunctionReturn((ReturnEdge) cfaEdge);
        break;

      // if edge cannot be handled, it could be important
      default:
        isCFAEdgeImportant = true;
      }

      if (isCFAEdgeImportant) {
        errorPath.addFirst(cfaEdgePair);
      }

      // print the Set of importantVars, for testing
      if (printForTesting) {
        if (isCFAEdgeImportant) {
          System.out.print("important? yes   ");
        } else {
          System.out.print("important?       ");
        }
        System.out.print("importantVars: { ");
        for (String var : importantVars) {
          System.out.print(var + ", ");
        }
        System.out.println(" } , edge: " + cfaEdge.toString());
      }

    }
    return errorPath;
  }

  /** This method iterates a Path and adds all golbal Variables to the Set 
   * of global variables. 
   * 
   * @param path the Path to iterate
   */
  private static void findGolbalVarsInPath(Path path) {

    // iterate through the Path and collect all important variables
    Iterator<Pair<ARTElement, CFAEdge>> iterator = path.iterator();
    while (iterator.hasNext()) {
      CFAEdge cfaEdge = iterator.next().getSecond();

      // only globalDeclarations (SubType of Declaration) are important
      if (cfaEdge instanceof GlobalDeclarationEdge) {
        DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;

        for (IASTDeclarator declarator : declarationEdge.getDeclarators()) {

          // ignore null and pointer variables
          if ((declarator != null)
              && (declarator.getPointerOperators().length == 0)) {
            // a global variable is added to the list of global variables
            globalVars.add(declarator.getName().toString());
          }
        }
      }
    }

    if (printForTesting) {
      System.out.print("globlaVars: { ");
      for (String var : globalVars) {
        System.out.print(var + ", ");
      }
      System.out.println(" }");
    }
  }

  /** This function returns, if the edge is important. 
   * 
   * @param cfaEdge the edge to prove
   * @return isImportantEdge
   */
  private static boolean handle(CFAEdge cfaEdge) {

    // check the type of the edge
    switch (cfaEdge.getEdgeType()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:

      /* this is the statement edge which leads the function to the last node
      * of its CFA (not same as a return edge) */
      if (cfaEdge.isJumpEdge()) {
        System.out.println("exitFromFuctionEdge : " + cfaEdge.toString());
        return handleExitFromFunction(((StatementEdge) cfaEdge).getExpression());
      }
      // this is a regular statement
      else {
        return handleStatement(((StatementEdge) cfaEdge).getExpression());
      }

      // edge is a declaration edge, e.g. int a;
    case DeclarationEdge:
      return handleDeclaration((DeclarationEdge) cfaEdge);

      // this is an assumption, e.g. if(a == b)
    case AssumeEdge:
      return handleAssumption(((AssumeEdge) cfaEdge).getExpression());

      /* There are several BlankEdgeTypes: 
       * a jumpEdge ("goto label") is important, 
       * a labelEdge and other Types maybe, a really blank edge is not. 
       * TODO are there more types? */
    case BlankEdge:
      return cfaEdge.isJumpEdge();

    case FunctionCallEdge:
      return handleFunctionCall((FunctionCallEdge) cfaEdge);

      // this is a return edge from function, this is different from return 
      // statement of the function. See case in statement edge for details
    case ReturnEdge:
      return handleFunctionReturn((ReturnEdge) cfaEdge);

      // if edge cannot be handled, it could be important
    default:
      return true;
    }
  }

  /**
   * This method handles statements.
   * 
   * @param statementExp the expression to prove
   * @return true boolean
   */
  private static boolean handleStatement(IASTExpression statementExp) {

    // a unary operation, e.g. a++
    // this does not change the Set of important variables, 
    // but the edge could be important
    if (statementExp instanceof IASTUnaryExpression)
      return handleUnaryStatement((IASTUnaryExpression) statementExp);

    // expression is a binary operation, e.g. a = b;
    else if (statementExp instanceof IASTBinaryExpression)
      return handleAssignment((IASTBinaryExpression) statementExp);

    // ext();
    else if (statementExp instanceof IASTFunctionCallExpression)
      return true;

    // a;
    else if (statementExp instanceof IASTIdExpression) {
      String varName = statementExp.getRawSignature();
      return importantVars.contains(varName);
    }

    else
      return true;
  }

  /**
   * This method handles unary statements (a++, a--).
   *
   * @param unaryExpression the expression to prove
   * @return isImportantEdge
   */
  private static boolean handleUnaryStatement(
      IASTUnaryExpression unaryExpression) {

    // get operand, i.e. "a"
    IASTExpression operand = unaryExpression.getOperand();

    // if operand is a identifier and is not important, the edge is not 
    // important, in any other case the edge could be important
    return !(operand instanceof IASTIdExpression && !importantVars
        .contains(operand.getRawSignature()));
  }

  /**
   * This method handles assignments (?a = ??).
   *
   * @param binaryExpression the expression to prove
   * @return isImportantEdge
   */
  private static boolean handleAssignment(IASTBinaryExpression binaryExpression) {

    IASTExpression lParam = binaryExpression.getOperand1();
    IASTExpression rightExp = binaryExpression.getOperand2();

    // a = ?
    if (lParam instanceof IASTIdExpression)
      return handleAssignmentToVariable(lParam.getRawSignature(), rightExp);

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

    // if the edge is not unimportant, this edge could be important.
    else
      return true;
  }

  /**
   * This method handles the assignment of a variable (a = ?).
   *
   * @param lParam the local name of the variable to assign to
   * @param rightExp the assigning expression
   * @return isImportantEdge
   */
  private static boolean handleAssignmentToVariable(String lParam,
      IASTExpression rightExp) {

    // if lParam is important, the edge is important 
    // and every variable in rightExp is important.
    if (importantVars.contains(lParam)) {
      addAllVarsInExpToImportantVars(rightExp);
      return true;
    } else
      return false;
  }

  /**
   * This method handles variable declarations ("int a;"). Expressions like 
   * "int a=b;" are preprocessed by CIL to "int a; \n a=b;", so there is no 
   * need to handle them. The expression "a=b;" is handled as StatementEdge.
   * 
   * @param declarationEdge the edge to prove
   * @return isImportantEdge
   */
  private static boolean handleDeclaration(DeclarationEdge declarationEdge) {

    // boolean for iteration
    boolean isImportant = false;

    /* Normally there is only one declarator in the DeclarationEdge. 
     * If there are more than one declarators, CIL divides them into different 
     * declarators while preprocessing: 
     * "int a,b,c;"  -->  CIL  -->  "int a;  int b;  int c;". 
     * If the declared variable is important, the edge is important.
     * One important declarator is enough for an edge to be important, 
     * normally there is only one declarator, if CIL has been run. */
    for (IASTDeclarator declarator : declarationEdge.getDeclarators()) {
      String varName = declarator.getName().getRawSignature();
      if (importantVars.contains(varName)) {
        isImportant = true;
      }
    }
    return isImportant;
  }

  /**
   * This method handles assumptions (a==b, a<=b, true, etc.).
   * Assumptions always are handled as important edges. This method only 
   * adds all variables in an assumption (expression) to the important variables.
   * 
   * @param assumeExp the expression to prove
   * @return true boolean
   */
  private static boolean handleAssumption(IASTExpression assumeExp) {
    addAllVarsInExpToImportantVars(assumeExp);
    return true;
  }

  /**
   * This method adds all variables in an expression to the Set of important 
   * variables. If the expression exist of more than one sub-expressions, 
   * the expression is divided into smaller parts and the method is called 
   * recursively for each part, until there is only one variable or literal. 
   * Literals are not part of important variables. 
   *
   * @param exp the expression to be divided and added
   * @param binaryOperator the binary operator
   * @return isImportantEdge
   */
  private static void addAllVarsInExpToImportantVars(IASTExpression exp) {

    // exp = 8.2 or "return;" (when exp == null),
    // this does not change the Set importantVars,
    if (exp instanceof IASTLiteralExpression || exp == null) {
      // do nothing
    }

    // exp is an Identifier
    else if (exp instanceof IASTIdExpression) {
      importantVars.add(exp.getRawSignature());
    }

    // (cast) b 
    else if (exp instanceof IASTCastExpression) {
      addAllVarsInExpToImportantVars(((IASTCastExpression) exp).getOperand());
    }

    // -b
    else if (exp instanceof IASTUnaryExpression) {
      addAllVarsInExpToImportantVars(((IASTUnaryExpression) exp).getOperand());
    }

    // b op c; --> b is operand1, c is operand2
    else if (exp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = (IASTBinaryExpression) exp;
      addAllVarsInExpToImportantVars(binExp.getOperand1());
      addAllVarsInExpToImportantVars(binExp.getOperand2());
    }

    // func(); or b->c;
    else if (exp instanceof IASTFunctionCallExpression
        || exp instanceof IASTFieldReference) {
      // TODO: what should be added to importantVars?
    }
  }

  private static boolean handleFunctionCall(FunctionCallEdge pCfaEdge) {
    // TODO Auto-generated method stub
    return true;
  }

  private static boolean handleFunctionReturn(ReturnEdge pCfaEdge) {
    // TODO Auto-generated method stub
    return true;
  }

  /**
   * This method handles exits from a function ("return a;").
   *
   * @param exitExpression the expression to be handled
   * @return isImportantEdge
   */
  private static boolean handleExitFromFunction(IASTExpression exitExpression) {
    /* TODO exitFromFunction is only important, if the function is important. 
     * the function is called before exitFromFunction, 
     * so it is before the exitFromFunction in the targetPath.
     * Currently every exitFromFunction is important. */
    addAllVarsInExpToImportantVars(exitExpression);
    return true;
  }

}