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
public final class ErrorPathShrinker {

  /** This is only an UtilityClass. */
  private ErrorPathShrinker() {
  }

  /** Set<String> for storing the global variables. */
  private static final Set<String> GLOBAL_VARS     =
                                                       new LinkedHashSet<String>();

  private static boolean           printForTesting = false;

  /** The function shrinkErrorPath gets an targetPath and creates a new Path,
   * with only the important edges of the Path.
   * 
   * @param targetPath the "long" targetPath
   * @return errorPath the "short" errorPath */
  public static Path shrinkErrorPath(final Path targetPath) {

    // first collect all global variables
    findGlobalVarsInPath(targetPath);

    // create reverse iterator, from lastNode to firstNode
    final Iterator<Pair<ARTElement, CFAEdge>> revIterator =
        targetPath.descendingIterator();

    // Set for storing the important variables
    final Set<String> importantVars = new LinkedHashSet<String>();

    // Set for storing the global variables, that are important and used
    // during proving the edges.
    final Set<String> importantGlobalVarsIn = new LinkedHashSet<String>();

    // Path for storing changings of globalVars
    final Path globalVarsPath = new Path();

    // the short Path, the result
    final Path shortErrorPath = new Path();

    // the errorNode is important
    shortErrorPath.addFirst(revIterator.next());

    handlePath(shortErrorPath, revIterator, importantVars,
        importantGlobalVarsIn, globalVarsPath);

    //TODO add the rest of the GlobalVarEdges

    return shortErrorPath;
  }

  /** This method iterates a Path and adds all global Variables to the Set
   * of global variables.
   * 
   * @param path the Path to iterate
   */
  private static void findGlobalVarsInPath(final Path path) {

    // iterate through the Path and collect all important variables
    final Iterator<Pair<ARTElement, CFAEdge>> iterator = path.iterator();
    while (iterator.hasNext()) {
      CFAEdge cfaEdge = iterator.next().getSecond();

      // only globalDeclarations (SubType of Declaration) are important
      if (cfaEdge instanceof GlobalDeclarationEdge) {
        DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;

        /* Normally there is only one declarator in the DeclarationEdge.
         * If there are more than one declarators, CIL divides them into
         * different declarators while preprocessing:
         * "int a,b,c;"  -->  CIL  -->  "int a;  int b;  int c;". */
        for (IASTDeclarator declarator : declarationEdge.getDeclarators()) {

          // if a variable (declarator) is not null and no pointer variable,
          // it is added to the list of global variables
          if ((declarator != null)
              && (declarator.getPointerOperators().length == 0)) {
            GLOBAL_VARS.add(declarator.getName().toString());
          }
        }
      }
    }

    if (printForTesting) {
      System.out.print("globlaVars: { ");
      for (String var : GLOBAL_VARS) {
        System.out.print(var + ", ");
      }
      System.out.println(" }");
    }
  }

  /** This function gets a Path and shrinks it to a shorter Path,
   * only important edges from the first Path are in the shortPath.
   *
   * @param shortPath
   * @param revIterator
   * @param importantVars
   * @param importantGlobalVars
   * @param importantGlobalVarsIn
   * @param pPossibleImportantVarsForGlobalVarsIn
   * @param globalVarsPath
   */
  private static void handlePath(final Path shortPath,
      final Iterator<Pair<ARTElement, CFAEdge>> revIterator,
      final Set<String> importantVars, final Set<String> importantGlobalVarsIn,
      final Path globalVarsPath) {

    // Set for storing all variables, that are important for a global variable
    // used after the function
    final Set<String> possibleImportantVarsForGlobalVarsIn =
        new LinkedHashSet<String>();

    // iterate through the Path (backwards) and collect all important variables
    while (revIterator.hasNext()) {
      Pair<ARTElement, CFAEdge> cfaEdgePair = revIterator.next();
      CFAEdge cfaEdge = cfaEdgePair.getSecond();

      // check the type of the edge
      switch (cfaEdge.getEdgeType()) {

      // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:

        // this is the statement edge which leads the function to the last node
        // of its CFA (not same as a return edge)
        if (cfaEdge.isJumpEdge()) {
          handleJumpStatement(shortPath, cfaEdgePair, revIterator,
              importantVars, importantGlobalVarsIn, globalVarsPath);
        }

        // this is a regular statement
        else {
          handleStatement(shortPath, cfaEdgePair, importantVars,
              importantGlobalVarsIn, possibleImportantVarsForGlobalVarsIn,
              globalVarsPath);
        }
        break;

      // edge is a declaration edge, e.g. int a;
      case DeclarationEdge:
        handleDeclaration(shortPath, cfaEdgePair, importantVars,
            possibleImportantVarsForGlobalVarsIn, globalVarsPath);
        break;

      // this is an assumption, e.g. if(a == b)
      case AssumeEdge:
        handleAssumption(shortPath, cfaEdgePair, importantVars,
            importantGlobalVarsIn);
        break;

      /* There are several BlankEdgeTypes:
       * a jumpEdge ("goto") and a loopstart ("while") are important,
       * a labelEdge maybe, a really blank edge is not important.
       * TODO are there more types? */
      case BlankEdge:
        if (cfaEdge.isJumpEdge() || cfaEdge.getSuccessor().isLoopStart()) {
          shortPath.addFirst(cfaEdgePair);
        }
        break;

      // start of a function, so "return" to the higher recursive call
      case FunctionCallEdge:
        shortPath.addFirst(cfaEdgePair);
        return;

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
        System.out.println(" } , edge: " + cfaEdge.toString() + ", type: "
            + cfaEdge.getEdgeType());
      }

    }
    return;
  }

  /** This method makes a recursive call of handlePath().
   * After that it merges the result with the current shortPath.
   *
   * @param shortPath
   * @param longPath
   * @param cfaEdgePair
   * @param revIterator
   * @param importantVars
   * @param importantGlobalVars
   * @param importantGlobalVarsIn
   * @param globalVarsPath
   * */
  private static void handleJumpStatement(final Path shortPath,
      final Pair<ARTElement, CFAEdge> cfaEdgePair,
      final Iterator<Pair<ARTElement, CFAEdge>> revIterator,
      final Set<String> importantVars, final Set<String> importantGlobalVarsIn,
      final Path globalVarsPath) {

    // Set for storing the important variables, normally empty when leaving
    // handlePath(), because declarators are removed, except globalVars.
    final Set<String> possibleVars = new LinkedHashSet<String>();

    // in the expression "return r" the value "r" is possibly important.
    addAllVarsInExpToImportantVars(
        ((StatementEdge) cfaEdgePair.getSecond()).getExpression(),
        possibleVars, importantGlobalVarsIn);

    final Pair<ARTElement, CFAEdge> returnEdgePair = cfaEdgePair;

    // Path for storing changings of variables of importantGlobalVarsIn
    final Path functionGlobalVarsPath = new Path();

    // the short Path is the result, the last element is the "return"-Node
    final Path shortFunctionPath = new Path();

    // Set for storing the global variables, that are possibly important
    // in the function. copy all global variables in another Set,
    // they could be assigned in the function.
    final Set<String> possibleImportantGlobalVarsIn =
        new LinkedHashSet<String>();
    possibleImportantGlobalVarsIn.addAll(importantGlobalVarsIn);

    // print the Set of possibleVars, for testing, normal output: "{r}"
    if (printForTesting) {
      System.out.print("importantGlobalVarsIn: { ");
      for (String var : importantGlobalVarsIn) {
        System.out.print(var + ", ");
      }
      System.out.println(" } , edge: " + cfaEdgePair.getSecond().toString());
      System.out.print("possibleVars: { ");
      for (String var : possibleVars) {
        System.out.print(var + ", ");
      }
      System.out.println(" } ");
    }

    // this is a recursive call to handle the Path inside of the function
    handlePath(shortFunctionPath, revIterator, possibleVars,
        possibleImportantGlobalVarsIn, functionGlobalVarsPath);

    if (printForTesting) {
      System.out.println("funcPath:\n" + shortFunctionPath.toString());
      System.out.println("globPath:\n" + functionGlobalVarsPath.toString());
    }

    // the recursive call stops at the functionStart,
    // so the lastEdge is the functionCall.
    final CFAEdge lastEdge = shortFunctionPath.getFirst().getSecond();
    assert (lastEdge instanceof FunctionCallEdge);
    final FunctionCallEdge funcEdge = (FunctionCallEdge) lastEdge;

    /* TODO: is there any possibility to get the funcAssumeVar through the AST?
     * current solution: split the rawStatement at the expression " = ",
     * the expression " = " is the "middle" of the assumption.
     * The first part is the funcAssumeVar.
     * "a = f(x)"-->"a", "a = f(x==y)"-->"a", "f(x)"-->"f(x)" */
    final String funcAssumeVarName = funcEdge.getRawStatement().split(" = ")[0];

    // if the variable funcAssumeVar is important,
    // the edge and all arguments of the function are important
    if (importantVars.contains(funcAssumeVarName)) {

      // get "x" and "y" from "a = f(x,y)"
      // all variables in the expressions "x" and "y" are important
      if (funcEdge.getArguments() != null) {
        for (IASTExpression exp : funcEdge.getArguments()) {
          addAllVarsInExpToImportantVars(exp, importantVars,
              importantGlobalVarsIn);
        }
      }

      // add the returnEdge and the functionPath in front of the shortPath
      shortPath.addFirst(returnEdgePair);
      shortPath.addAll(0, shortFunctionPath);

      // if global variables are used in the function and they have an effect
      // to the result of the function, add them to the important variables
      // and to the importantGlobalVars.
      importantVars.addAll(possibleImportantGlobalVarsIn);
      importantGlobalVarsIn.addAll(possibleImportantGlobalVarsIn);
    }

    // if the result is not important,
    // check, if important global variables are changed.
    else if (!functionGlobalVarsPath.isEmpty()) {

      // get "x" and "y" from "a = f(x,y)"
      // all variables in the expressions "x" and "y" are handled as important,
      // TODO: sometimes not all of them are important.
      if (funcEdge.getArguments() != null) {
        for (IASTExpression exp : funcEdge.getArguments()) {
          addAllVarsInExpToImportantVars(exp, importantVars,
              importantGlobalVarsIn);
        }
      }

      // add the returnEdge and the functionPath in front of the shortPath
      shortPath.addFirst(returnEdgePair);
      shortPath.addAll(0, shortFunctionPath);

      // if global variables are used in the function,
      // add them to the importantGlobalVars.
      importantGlobalVarsIn.addAll(possibleImportantGlobalVarsIn);
    }
  }

  /**
   * This method handles statements.
   * 
   * @param shortPath
   * @param cfaEdgePair
   * @param importantVars
   * @param importantGlobalVarsIn
   * @param possibleImportantVarsForGlobalVarsIn
   * @param globalVarsPath
   */
  private static void handleStatement(final Path shortPath,
      final Pair<ARTElement, CFAEdge> cfaEdgePair,
      final Set<String> importantVars, final Set<String> importantGlobalVarsIn,
      final Set<String> possibleImportantVarsForGlobalVarsIn,
      final Path globalVarsPath) {

    IASTExpression statementExp =
        ((StatementEdge) cfaEdgePair.getSecond()).getExpression();

    // a unary operation, e.g. a++
    // this does not change the Set of important variables,
    // but the edge could be important
    if (statementExp instanceof IASTUnaryExpression) {
      handleUnaryStatement(shortPath, cfaEdgePair,
          (IASTUnaryExpression) statementExp, importantVars,
          possibleImportantVarsForGlobalVarsIn, globalVarsPath);
    }

    // expression is a binary operation, e.g. a = b;
    else if (statementExp instanceof IASTBinaryExpression) {
      handleAssignment(shortPath, cfaEdgePair,
          (IASTBinaryExpression) statementExp, importantVars,
          importantGlobalVarsIn, possibleImportantVarsForGlobalVarsIn,
          globalVarsPath);
    }

    // ext();
    else if (statementExp instanceof IASTFunctionCallExpression) {
      shortPath.addFirst(cfaEdgePair);
    }

    // a;
    else if (statementExp instanceof IASTIdExpression) {
      final String varName = statementExp.getRawSignature();
      if (importantVars.contains(varName)) {
        shortPath.addFirst(cfaEdgePair);
      }
      if (importantGlobalVarsIn.contains(varName)) {
        globalVarsPath.addFirst(cfaEdgePair);
      }
    }

    else {
      shortPath.addFirst(cfaEdgePair);
    }
  }

  /**
   * This method handles unary statements (a++, a--).
   *
   * @param shortPath
   * @param cfaEdgePair
   * @param unaryExpression the expression to prove
   * @param importantVars
   * @param possibleImportantVarsForGlobalVarsIn
   * @param globalVarsPath
   */
  private static void handleUnaryStatement(final Path shortPath,
      final Pair<ARTElement, CFAEdge> cfaEdgePair,
      final IASTUnaryExpression unaryExpression,
      final Set<String> importantVars,
      final Set<String> possibleImportantVarsForGlobalVarsIn,
      final Path globalVarsPath) {

    // get operand, i.e. "a"
    final IASTExpression operand = unaryExpression.getOperand();

    if (operand instanceof IASTIdExpression) {
      final String varName = operand.getRawSignature();

      // an identifier is important, if it has been marked as important before.
      if (importantVars.contains(varName)) {
        shortPath.addFirst(cfaEdgePair);
      }

      if (possibleImportantVarsForGlobalVarsIn.contains(varName)) {
        globalVarsPath.addFirst(cfaEdgePair);
      }
    }
  }

  /**
   * This method handles assignments (?a = ??).
   * 
   * @param shortPath
   * @param cfaEdgePair
   * @param binaryExpression the expression to prove
   * @param importantVars
   * @param importantGlobalVarsIn
   * @param possibleImportantVarsForGlobalVarsIn
   * @param globalVarsPath
   */
  private static void handleAssignment(final Path shortPath,
      final Pair<ARTElement, CFAEdge> cfaEdgePair,
      final IASTBinaryExpression binaryExpression,
      final Set<String> importantVars, final Set<String> importantGlobalVarsIn,
      final Set<String> possibleImportantVarsForGlobalVarsIn,
      final Path globalVarsPath) {

    IASTExpression lParam = binaryExpression.getOperand1();
    IASTExpression rightExp = binaryExpression.getOperand2();

    // a = ?
    if (lParam instanceof IASTIdExpression) {
      handleAssignmentToVariable(shortPath, cfaEdgePair,
          lParam.getRawSignature(), rightExp, importantVars,
          importantGlobalVarsIn, possibleImportantVarsForGlobalVarsIn,
          globalVarsPath);
    }

    // TODO: assignment to pointer, *a = ?
    else if (lParam instanceof IASTUnaryExpression
        && ((IASTUnaryExpression) lParam).getOperator() == IASTUnaryExpression.op_star) {
      shortPath.addFirst(cfaEdgePair);
    }

    // TODO assignment to field, a->b = ?
    else if (lParam instanceof IASTFieldReference) {
      shortPath.addFirst(cfaEdgePair);
    }

    // TODO assignment to array cell, a[b] = ?
    else if (lParam instanceof IASTArraySubscriptExpression) {
      shortPath.addFirst(cfaEdgePair);
    }

    // if the edge is not unimportant, this edge could be important.
    else {
      shortPath.addFirst(cfaEdgePair);
    }
  }

  /**
   * This method handles the assignment of a variable (a = ?).
   * 
   * @param shortPath
   * @param cfaEdgePair
   * @param lParam the local name of the variable to assign to
   * @param rightExp the assigning expression
   * @param importantVars
   * @param importantGlobalVarsIn
   * @param possibleImportantVarsForGlobalVarsIn
   * @param globalVarsPath
   */
  private static void handleAssignmentToVariable(final Path shortPath,
      final Pair<ARTElement, CFAEdge> cfaEdgePair, final String lParam,
      final IASTExpression rightExp, final Set<String> importantVars,
      final Set<String> importantGlobalVarsIn,
      final Set<String> possibleImportantVarsForGlobalVarsIn,
      final Path globalVarsPath) {

    // if lParam is important, the edge is important
    // and every variable in rightExp is important.
    if (importantVars.contains(lParam)) {
      addAllVarsInExpToImportantVars(rightExp, importantVars,
          importantGlobalVarsIn);
    }

    // if lParam is a globalVar, all variables in the right expression are
    // important for a global variable and the Edge is part of globalVarPath.
    if (importantGlobalVarsIn.contains(lParam)
        || possibleImportantVarsForGlobalVarsIn.contains(lParam)) {
      addAllVarsInExpToImportantVars(rightExp,
          possibleImportantVarsForGlobalVarsIn, importantGlobalVarsIn);
      globalVarsPath.addFirst(cfaEdgePair);
    }

    if (importantVars.contains(lParam)
        || importantGlobalVarsIn.contains(lParam)
        || possibleImportantVarsForGlobalVarsIn.contains(lParam)) {
      shortPath.addFirst(cfaEdgePair);
    }
  }

  /**
   * This method handles variable declarations ("int a;"). Expressions like
   * "int a=b;" are preprocessed by CIL to "int a; \n a=b;", so there is no
   * need to handle them. The expression "a=b;" is handled as StatementEdge.
   * 
   * @param shortPath
   * @param cfaEdgePair
   * @param importantVars
   * @param possibleImportantVarsForGlobalVarsIn
   * @param globalVarsPath
   */
  private static void handleDeclaration(final Path shortPath,
      final Pair<ARTElement, CFAEdge> cfaEdgePair,
      final Set<String> importantVars,
      final Set<String> possibleImportantVarsForGlobalVarsIn,
      final Path globalVarsPath) {

    DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdgePair.getSecond();

    /* Normally there is only one declarator in the DeclarationEdge.
     * If there are more than one declarators, CIL divides them into different
     * declarators while preprocessing:
     * "int a,b,c;"  -->  CIL  -->  "int a;  int b;  int c;".
     * If the declared variable is important, the edge is important. */
    for (IASTDeclarator declarator : declarationEdge.getDeclarators()) {
      final String varName = declarator.getName().getRawSignature();
      if (importantVars.contains(varName)) {
        shortPath.addFirst(cfaEdgePair);
        // the variable is declared in this statement,
        // so it is not important in the CFA before. --> remove it.
        importantVars.remove(varName);
      }
      if (possibleImportantVarsForGlobalVarsIn.contains(varName)) {
        globalVarsPath.addFirst(cfaEdgePair);
      }
    }
  }

  /**
   * This method handles assumptions (a==b, a<=b, true, etc.).
   * Assumptions always are handled as important edges. This method only adds
   * all variables in an assumption (expression) to the important variables.
   * 
   * @param shortPath
   * @param cfaEdgePair
   * @param importantVars
   * @param importantGlobalVarsIn
   */
  private static void handleAssumption(final Path shortPath,
      final Pair<ARTElement, CFAEdge> cfaEdgePair,
      final Set<String> importantVars, final Set<String> importantGlobalVarsIn) {
    final IASTExpression assumeExp =
        ((AssumeEdge) cfaEdgePair.getSecond()).getExpression();
    addAllVarsInExpToImportantVars(assumeExp, importantVars,
        importantGlobalVarsIn);
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
   * @param importantGlobalVarsIn
   */
  private static void addAllVarsInExpToImportantVars(final IASTExpression exp,
      final Set<String> importantVars, final Set<String> importantGlobalVarsIn) {

    // exp = 8.2 or "return;" (when exp == null),
    // this does not change the Set importantVars,
    if (exp instanceof IASTLiteralExpression || exp == null) {
      // do nothing
    }

    // exp is an Identifier, i.e. the "b" from "a = b"
    else if (exp instanceof IASTIdExpression) {
      final String varName = exp.getRawSignature();
      importantVars.add(varName);
      if (GLOBAL_VARS.contains(varName)) {
        importantGlobalVarsIn.add(varName);
      }
    }

    // (cast) b
    else if (exp instanceof IASTCastExpression) {
      addAllVarsInExpToImportantVars(((IASTCastExpression) exp).getOperand(),
          importantVars, importantGlobalVarsIn);
    }

    // -b
    else if (exp instanceof IASTUnaryExpression) {
      addAllVarsInExpToImportantVars(((IASTUnaryExpression) exp).getOperand(),
          importantVars, importantGlobalVarsIn);
    }

    // b op c; --> b is operand1, c is operand2
    else if (exp instanceof IASTBinaryExpression) {
      final IASTBinaryExpression binExp = (IASTBinaryExpression) exp;
      addAllVarsInExpToImportantVars(binExp.getOperand1(), importantVars,
          importantGlobalVarsIn);
      addAllVarsInExpToImportantVars(binExp.getOperand2(), importantVars,
          importantGlobalVarsIn);
    }

    // func(); or b->c;
    else if (exp instanceof IASTFunctionCallExpression
        || exp instanceof IASTFieldReference) {
      // TODO: what should be added to importantVars?
    }
  }

  /** nothing to do? */
  private static void handleFunctionReturn(final ReturnEdge cfaEdge,
      final Set<String> importantVars, final Path globalVarsPath) {
    // TODO Auto-generated method stub
  }
}