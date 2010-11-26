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
import java.util.List;
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
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisElement;

/** The Class ErrorPathShrinker gets an targetPath and creates a new Path, 
 * with only the important edges of the Path. The idea behind this Class is, 
 * that not every action (CFAEdge) before an error occurs is important for 
 * the error, only a few actions (CFAEdges) are important. 
 * 
 * @author Friedberger Karlheinz
 */
public class ErrorPathShrinker {

  /** Set<String> for storing the global variables */
  private final static Set<String> globalVars      =
                                                       new LinkedHashSet<String>();

  private static boolean           printForTesting = true;

  /** The function shrinkErrorPath gets an targetPath and creates a new Path, 
   * with only the important edges of the Path. 
   * 
   * @param targetPath the "long" targetPath
   * @return errorPath the "short" errorPath */
  public static final Path shrinkErrorPath(Path targetPath) {

    // first collect all global variables
    findGlobalVarsInPath(targetPath);

    // create reverse iterator, from lastNode to firstNode, 
    Iterator<Pair<ARTElement, CFAEdge>> revIterator =
        targetPath.descendingIterator();

    // Set for storing the important variables
    final Set<String> importantVars = new LinkedHashSet<String>();

    // Path for storing changings of globalVars
    final Path globalVarsPath = new Path();

    // the short Path, the result
    final Path shortErrorPath = new Path();

    handlePath(shortErrorPath, targetPath, revIterator, importantVars,
        globalVarsPath);

    return shortErrorPath;
  }

  /** This method iterates a Path and adds all global Variables to the Set 
   * of global variables. 
   * 
   * @param path the Path to iterate
   */
  private static void findGlobalVarsInPath(Path path) {

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
            globalVars.add("global_" + declarator.getName().toString());
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

  /** This function gets a Path and shrinks it to a shorter Path, 
   * only important edges from the first Path are in the shortPath. 
   * 
   * @param importantVars 
   * @param globalVarsPath 
   * @return isImportantEdge
   */
  private static Path handlePath(Path shortPath, Path longPath,
      Iterator<Pair<ARTElement, CFAEdge>> revIterator,
      Set<String> importantVars, Path globalVarsPath) {

    // the last element of the Path is important, because it is the ErrorNode 
    // or the "return"-Node of a functionCall.
    shortPath.addFirst(revIterator.next());

    // iterate through the Path (backwards) and collect all important variables
    while (revIterator.hasNext()) {
      Pair<ARTElement, CFAEdge> cfaEdgePair = revIterator.next();
      CFAEdge cfaEdge = cfaEdgePair.getSecond();

      // check the type of the edge
      switch (cfaEdge.getEdgeType()) {

      // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:

        /* this is the statement edge which leads the function to the last node
        * of its CFA (not same as a return edge) */
        if (cfaEdge.isJumpEdge()) {
          handleJumpStatement(shortPath, longPath, cfaEdgePair, revIterator,
              importantVars, globalVarsPath);
        }
        // this is a regular statement
        else {
          handleStatement(shortPath, cfaEdgePair, importantVars, globalVarsPath);
        }
        break;

      // edge is a declaration edge, e.g. int a;
      case DeclarationEdge:
        handleDeclaration(shortPath, cfaEdgePair, importantVars, globalVarsPath);
        break;

      // this is an assumption, e.g. if(a == b)
      case AssumeEdge:
        handleAssumption(shortPath, cfaEdgePair, importantVars, globalVarsPath);
        break;

      /* There are several BlankEdgeTypes: 
       * a jumpEdge ("goto label") is important, 
       * a labelEdge and other Types maybe, a really blank edge is not. 
       * TODO are there more types? */
      case BlankEdge:
        if (cfaEdge.isJumpEdge())
        ;
        break;

      // start of a function, so return shortPath to the higher recursive call
      case FunctionCallEdge:
        shortPath.addFirst(cfaEdgePair);
        return shortPath;

        // this is a return edge from function, this is different from return 
        // statement of the function. See case in statement edge for details
      case ReturnEdge:
        handleFunctionReturn((ReturnEdge) cfaEdge, importantVars,
            globalVarsPath);
        break;

      // if edge cannot be handled, it could be important
      default:
        shortPath.addFirst(cfaEdgePair);
      }

      // print the Set of importantVars, for testing
      if (printForTesting) {
        System.out.print("importantVars: { ");
        for (String var : importantVars) {
          System.out.print(var + ", ");
        }
        System.out.println(" } , edge: " + cfaEdge.toString());
      }

    }
    return shortPath;
  }

  /** This method makes a recursive call of handlePath(). 
   * After that it merges the result with the current shortPath. */
  private static void handleJumpStatement(Path shortPath, Path inputPath,
      Pair<ARTElement, CFAEdge> cfaEdgePair,
      Iterator<Pair<ARTElement, CFAEdge>> revIterator,
      Set<String> importantVars, Path globalVarsPath) {

    // Set for storing the important variables, 
    // normally empty when leaving handlePath(), because declarators are removed
    final Set<String> possibleVars = new LinkedHashSet<String>();

    // Path for storing changings of globalVars
    final Path functionGlobalVarsPath = new Path();

    // the short Path, the result
    final Path functionPath = new Path();
    functionPath.add(cfaEdgePair);

    addAllVarsInExpToImportantVars(
        ((StatementEdge) cfaEdgePair.getSecond()).getExpression(), possibleVars);

    // print the Set of possibleVars, for testing, normal output: "{x}"
    if (printForTesting) {
      System.out.print("possibleVars: { ");
      for (String var : possibleVars) {
        System.out.print(var + ", ");
      }
      System.out.println(" } , edge: " + cfaEdgePair.getSecond().toString());
    }

    handlePath(functionPath, inputPath, revIterator, possibleVars,
        functionGlobalVarsPath);

    if (printForTesting) {
      System.out.println("funcPath:\n" + functionPath.toString());
    }

    // TODO merge results, if "a= f(x)" is important (if "a" is in importantVars).
    // if globalVarsPAth != null, it is important.
    
    CFAEdge lastEdge = functionPath.getFirst().getSecond();

    if (lastEdge instanceof FunctionCallEdge) {
      FunctionCallEdge funcEdge = (FunctionCallEdge) lastEdge;
      System.out.println("wow  " + funcEdge.getRawStatement());
    }

    // globalVars are important, if they are used later. 
    // other variables are important, if the function is part of a assignment ("a=func();")
    cfaEdgePair = revIterator.next();

  }

  /**
   * This method handles statements.
   * @param shortPath 
   * @param cfaEdgePair 
   * 
   * @param importantVars 
   * @param globalVarsPath 
   * @return true boolean
   */
  private static void handleStatement(Path shortPath,
      Pair<ARTElement, CFAEdge> cfaEdgePair, Set<String> importantVars,
      Path globalVarsPath) {

    IASTExpression statementExp =
        ((StatementEdge) cfaEdgePair.getSecond()).getExpression();

    // a unary operation, e.g. a++
    // this does not change the Set of important variables, 
    // but the edge could be important
    if (statementExp instanceof IASTUnaryExpression)
      handleUnaryStatement(shortPath, cfaEdgePair,
          (IASTUnaryExpression) statementExp, importantVars, globalVarsPath);

    // expression is a binary operation, e.g. a = b;
    else if (statementExp instanceof IASTBinaryExpression)
      handleAssignment(shortPath, cfaEdgePair,
          (IASTBinaryExpression) statementExp, importantVars, globalVarsPath);

    // ext();
    else if (statementExp instanceof IASTFunctionCallExpression)
      shortPath.addFirst(cfaEdgePair);

    // a;
    else if (statementExp instanceof IASTIdExpression) {
      String varName = statementExp.getRawSignature();
      if (importantVars.contains(varName) || importantVars.isEmpty()) {
        shortPath.addFirst(cfaEdgePair);
      }
      if (globalVars.contains(varName)) {
        globalVarsPath.add(cfaEdgePair);
      }
    }

    else
      shortPath.addFirst(cfaEdgePair);
  }

  /**
   * This method handles unary statements (a++, a--).
   * @param shortPath 
   * @param cfaEdgePair 
   *
   * @param unaryExpression the expression to prove
   * @param globalVarsPath 
   * @param pImportantVars 
   * @return isImportantEdge
   */
  private static void handleUnaryStatement(Path shortPath,
      Pair<ARTElement, CFAEdge> cfaEdgePair,
      IASTUnaryExpression unaryExpression, Set<String> importantVars,
      Path globalVarsPath) {

    // get operand, i.e. "a"
    IASTExpression operand = unaryExpression.getOperand();
    String varName = operand.getRawSignature();

    // if operand is a identifier and is not important, the edge is not 
    // important, in any other case the edge could be important
    if (operand instanceof IASTIdExpression
        && (importantVars.contains(varName) || importantVars.isEmpty())) {
      shortPath.addFirst(cfaEdgePair);
      if (globalVars.contains(varName)) {
        globalVarsPath.add(cfaEdgePair);
      }
    }
  }

  /**
   * This method handles assignments (?a = ??).
   * @param cfaEdgePair 
   * @param shortPath 
   *
   * @param binaryExpression the expression to prove
   * @param globalVarsPath 
   * @param pImportantVars 
   * @return isImportantEdge
   */
  private static void handleAssignment(Path shortPath,
      Pair<ARTElement, CFAEdge> cfaEdgePair,
      IASTBinaryExpression binaryExpression, Set<String> importantVars,
      Path globalVarsPath) {

    IASTExpression lParam = binaryExpression.getOperand1();
    IASTExpression rightExp = binaryExpression.getOperand2();

    // a = ?
    if (lParam instanceof IASTIdExpression)
      handleAssignmentToVariable(shortPath, cfaEdgePair,
          lParam.getRawSignature(), rightExp, importantVars, globalVarsPath);

    // TODO: assignment to pointer, *a = ?
    else if (lParam instanceof IASTUnaryExpression
        && ((IASTUnaryExpression) lParam).getOperator() == IASTUnaryExpression.op_star)
      shortPath.addFirst(cfaEdgePair);

    // TODO assignment to field, a->b = ?
    else if (lParam instanceof IASTFieldReference)
      shortPath.addFirst(cfaEdgePair);

    // TODO assignment to array cell, a[b] = ?
    else if (lParam instanceof IASTArraySubscriptExpression)
      shortPath.addFirst(cfaEdgePair);

    // if the edge is not unimportant, this edge could be important.
    else
      shortPath.addFirst(cfaEdgePair);
  }

  /**
   * This method handles the assignment of a variable (a = ?).
   * @param cfaEdgePair 
   * @param shortPath 
   *
   * @param lParam the local name of the variable to assign to
   * @param rightExp the assigning expression
   * @param importantVars 
   * @param globalVarsPath 
   * @return isImportantEdge
   */
  private static void handleAssignmentToVariable(Path shortPath,
      Pair<ARTElement, CFAEdge> cfaEdgePair, String lParam,
      IASTExpression rightExp, Set<String> importantVars, Path globalVarsPath) {

    // if lParam is important, the edge is important 
    // and every variable in rightExp is important.
    if (importantVars.contains(lParam) || importantVars.isEmpty()) {
      addAllVarsInExpToImportantVars(rightExp, importantVars);
      shortPath.addFirst(cfaEdgePair);
    }
    if (globalVars.contains(lParam)) {
      globalVarsPath.add(cfaEdgePair);
    }
  }

  /**
   * This method handles variable declarations ("int a;"). Expressions like 
   * "int a=b;" are preprocessed by CIL to "int a; \n a=b;", so there is no 
   * need to handle them. The expression "a=b;" is handled as StatementEdge.
   * @param cfaEdgePair 
   * @param shortPath 
   * 
   * @param importantVars 
   * @param globalVarsPath 
   * @return isImportantEdge
   */
  private static void handleDeclaration(Path shortPath,
      Pair<ARTElement, CFAEdge> cfaEdgePair, Set<String> importantVars,
      Path globalVarsPath) {

    DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdgePair.getSecond();

    /* Normally there is only one declarator in the DeclarationEdge. 
     * If there are more than one declarators, CIL divides them into different 
     * declarators while preprocessing: 
     * "int a,b,c;"  -->  CIL  -->  "int a;  int b;  int c;". 
     * If the declared variable is important, the edge is important. */
    for (IASTDeclarator declarator : declarationEdge.getDeclarators()) {
      String varName = declarator.getName().getRawSignature();
      if (importantVars.contains(varName)) {
        shortPath.addFirst(cfaEdgePair);
        // the variable is declared in this statement, 
        // so it is not important in the CFA before. --> remove it.
        importantVars.remove(varName);
      }
      if (globalVars.contains(varName)) {
        globalVarsPath.add(cfaEdgePair);
      }
    }
  }

  /**
   * This method handles assumptions (a==b, a<=b, true, etc.).
   * Assumptions always are handled as important edges. This method only 
   * adds all variables in an assumption (expression) to the important variables.
   * @param cfaEdgePair 
   * @param shortPath 
   * 
   * @param importantVars 
   * @param globalVarsPath 
   * @return true boolean
   */
  private static void handleAssumption(Path shortPath,
      Pair<ARTElement, CFAEdge> cfaEdgePair, Set<String> importantVars,
      Path globalVarsPath) {
    IASTExpression assumeExp =
        ((AssumeEdge) cfaEdgePair.getSecond()).getExpression();
    addAllVarsInExpToImportantVars(assumeExp, importantVars);
    shortPath.addFirst(cfaEdgePair);
  }

  /**
   * This method adds all variables in an expression to the Set of important 
   * variables. If the expression exist of more than one sub-expressions, 
   * the expression is divided into smaller parts and the method is called 
   * recursively for each part, until there is only one variable or literal. 
   * Literals are not part of important variables. 
   *
   * @param exp the expression to be divided and added
   * @param importantVars 
   * @param binaryOperator the binary operator
   * @return isImportantEdge
   */
  private static void addAllVarsInExpToImportantVars(IASTExpression exp,
      Set<String> importantVars) {

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
      addAllVarsInExpToImportantVars(((IASTCastExpression) exp).getOperand(),
          importantVars);
    }

    // -b
    else if (exp instanceof IASTUnaryExpression) {
      addAllVarsInExpToImportantVars(((IASTUnaryExpression) exp).getOperand(),
          importantVars);
    }

    // b op c; --> b is operand1, c is operand2
    else if (exp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = (IASTBinaryExpression) exp;
      addAllVarsInExpToImportantVars(binExp.getOperand1(), importantVars);
      addAllVarsInExpToImportantVars(binExp.getOperand2(), importantVars);
    }

    // func(); or b->c;
    else if (exp instanceof IASTFunctionCallExpression
        || exp instanceof IASTFieldReference) {
      // TODO: what should be added to importantVars?
    }
  }

  private static void handleFunctionReturn(ReturnEdge cfaEdge,
      Set<String> importantVars, Path globalVarsPath) {
    // TODO Auto-generated method stub
  }
}