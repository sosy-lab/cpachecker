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
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
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
  private static final Set<String> GLOBAL_VARS = new LinkedHashSet<String>();

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
    final Set<String> importantVarsForGlobalVars = new LinkedHashSet<String>();

    // Path for storing changings of globalVars
    final Path globalVarsPath = new Path();

    // the short Path, the result
    final Path shortErrorPath = new Path();

    // the errorNode is important
    shortErrorPath.addFirst(revIterator.next());

    // if the ErrorNode is inside of a function, 
    // the longPath is not handled until the StartNode, 
    // so call handlePath again until the longPath is completely handled.
    while (revIterator.hasNext()) {

      new PathHandler(shortErrorPath, revIterator, importantVars,
          importantVarsForGlobalVars, globalVarsPath);

      PathHandler.handlePath();
    }

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
  }

  /** This is a inner Class, that can handle a Path until a functionCallEdge. */
  private final static class PathHandler {

    /** the short Path, the result */
    static Path                                SHORT_PATH;

    /** reverse iterator, from lastNode to firstNode */
    static Iterator<Pair<ARTElement, CFAEdge>> REV_ITERATOR;

    /** Set for storing the important variables */
    static Set<String>                         IMPORTANT_VARS;

    /** Set for storing the global variables, that are important and used
     * during proving the edges */
    static Set<String>                         IMPORTANT_VARS_FOR_GLOBAL_VARS;

    /** Path for storing changings of globalVars */
    static Path                                GLOBAL_VARS_PATH;

    /** The currently handled CFAEdgePair */
    static Pair<ARTElement, CFAEdge>           CURRENT_CFA_EDGE_PAIR;

    /** The Constructor of tis Class  */
    PathHandler(final Path shortPathOut,
        final Iterator<Pair<ARTElement, CFAEdge>> revIteratorOut,
        final Set<String> importantVarsOut,
        final Set<String> importantVarsForGlobalVarsOut,
        final Path globalVarsPathOut) {
      SHORT_PATH = shortPathOut;
      REV_ITERATOR = revIteratorOut;
      IMPORTANT_VARS = importantVarsOut;
      IMPORTANT_VARS_FOR_GLOBAL_VARS = importantVarsForGlobalVarsOut;
      GLOBAL_VARS_PATH = globalVarsPathOut;
    }

    /** This function gets a Path and shrinks it to a shorter Path,
     * only important edges from the first Path are in the shortPath. */
    private static void handlePath() {

      // iterate through the Path (backwards) and collect all important variables
      while (REV_ITERATOR.hasNext()) {
        CURRENT_CFA_EDGE_PAIR = REV_ITERATOR.next();
        CFAEdge cfaEdge = CURRENT_CFA_EDGE_PAIR.getSecond();

        // check the type of the edge
        switch (cfaEdge.getEdgeType()) {

        // if edge is a statement edge, e.g. a = b + c
        case StatementEdge:

          // this is the statement edge which leads the function to the last node
          // of its CFA (not same as a return edge)
          if (cfaEdge.isJumpEdge()) {
            handleJumpStatement();
          }

          // this is a regular statement
          else {
            handleStatement();
          }
          break;

        // edge is a declaration edge, e.g. int a;
        case DeclarationEdge:
          handleDeclaration();
          break;

        // this is an assumption, e.g. if(a == b)
        case AssumeEdge:
          handleAssumption();
          break;

        /* There are several BlankEdgeTypes:
         * a jumpEdge ("goto") and a loopstart ("while") are important,
         * a labelEdge maybe, a really blank edge is not important.
         * TODO are there more types? */
        case BlankEdge:
          if (cfaEdge.isJumpEdge() || cfaEdge.getSuccessor().isLoopStart()) {
            addCurrentCFAEdgePairToShortPath();
          }
          break;

        // start of a function, so "return" to the higher recursive call
        case FunctionCallEdge:
          addCurrentCFAEdgePairToShortPath();
          if (!GLOBAL_VARS_PATH.isEmpty()) {
            GLOBAL_VARS_PATH.addFirst(CURRENT_CFA_EDGE_PAIR);
          }
          return;

          // this is a return edge from function, this is different from return
          // statement of the function. See case in statement edge for details
        case ReturnEdge:
          // TODO: what to do?
          break;

        // if edge cannot be handled, it could be important
        default:
          addCurrentCFAEdgePairToShortPath();
        }
      }
    }

    /** This method makes a recursive call of handlePath().
     * After that it merges the result with the current shortPath. */
    private static void handleJumpStatement() {

      // Set for storing the important variables, normally empty when leaving
      // handlePath(), because declarators are removed, except globalVars.
      final Set<String> possibleVars = new LinkedHashSet<String>();
      addGlobalVarsFromSetToSet(IMPORTANT_VARS_FOR_GLOBAL_VARS, possibleVars);

      // in the expression "return r" the value "r" is possibly important.
      final IASTExpression returnExp =
          ((StatementEdge) CURRENT_CFA_EDGE_PAIR.getSecond()).getExpression();
      addAllVarsInExpToImportantVars(returnExp, possibleVars);

      final Pair<ARTElement, CFAEdge> returnEdgePair = CURRENT_CFA_EDGE_PAIR;

      // Path for storing changings of variables of IMPORTANT_VARS_FOR_GLOBAL_VARS
      final Path functionGlobalVarsPath = new Path();

      // the short Path is the result, the last element is the "return"-Node
      final Path shortFunctionPath = new Path();

      // Set for storing the global variables, that are possibly important
      // in the function. copy all global variables in another Set,
      // they could be assigned in the function.
      final Set<String> possibleImportantVarsForGlobalVars =
          new LinkedHashSet<String>();

      // only global variables can be used inside AND outside of a function
      addGlobalVarsFromSetToSet(IMPORTANT_VARS_FOR_GLOBAL_VARS,
          possibleImportantVarsForGlobalVars);

      // this is a recursive call to handle the Path inside of the function
      new PathHandler(shortFunctionPath, REV_ITERATOR, possibleVars,
          possibleImportantVarsForGlobalVars, functionGlobalVarsPath);
      PathHandler.handlePath();

      /*
      System.out.println("funcPath:\n" + shortFunctionPath.toString());
      System.out.println("globPath:\n" + functionGLOBAL_VARS_PATH.toString());
      */

      // the recursive call stops at the functionStart,
      // so the lastEdge is the functionCall and there exist a CallToReturnEdge, 
      // that jumps over the hole function
      final CFAEdge lastEdge = shortFunctionPath.getFirst().getSecond();
      assert (lastEdge instanceof FunctionCallEdge);
      final FunctionCallEdge funcEdge = (FunctionCallEdge) lastEdge;
      final CallToReturnEdge funcSummaryEdge =
          funcEdge.getPredecessor().getLeavingSummaryEdge();
      final IASTExpression funcExp = funcSummaryEdge.getExpression();

      // "f(x)", without a variable "a" as "a = f(x)".
      // if the function changes the global variables,
      // get the variables from the function and update the Sets and Paths
      if (funcExp instanceof IASTFunctionCallExpression
          && !functionGlobalVarsPath.isEmpty()) {

        getImportantVarsFromFunctionCall(funcEdge,
            possibleImportantVarsForGlobalVars);

        // add the important edges in front of the shortPath
        SHORT_PATH.addFirst(returnEdgePair);
        GLOBAL_VARS_PATH.addFirst(returnEdgePair);
        GLOBAL_VARS_PATH.addAll(0, functionGlobalVarsPath);
        SHORT_PATH.addAll(0, functionGlobalVarsPath);
      }

      // "a = f(x)"
      if (funcExp instanceof IASTBinaryExpression) {
        final IASTExpression lParam =
            ((IASTBinaryExpression) funcExp).getOperand1();

        // if the function has a important result or changes the global variables,
        // get the params from the function and update the Sets
        if (IMPORTANT_VARS.contains(lParam)
            || !functionGlobalVarsPath.isEmpty()) {

          getImportantVarsFromFunctionCall(funcEdge,
              possibleImportantVarsForGlobalVars);

          // add the returnEdge in front of the shortPath
          SHORT_PATH.addFirst(returnEdgePair);
          GLOBAL_VARS_PATH.addFirst(returnEdgePair);
          GLOBAL_VARS_PATH.addAll(0, functionGlobalVarsPath);
        }

        // if the variable funcAssumeVar (result of the function) is important,
        // add the functionPath in front of the shortPath,
        // (the GLOBAL_VARS_PATH is always part of the shortFunctionPath)
        if (IMPORTANT_VARS.contains(lParam)) {
          SHORT_PATH.addAll(0, shortFunctionPath);
        }

        // if the variable funcAssumeVar (result of the function) is unimportant, 
        // but the function changes values of global variables used later,
        // add the functionGLOBAL_VARS_PATH in front of the shortPath
        else if (!functionGlobalVarsPath.isEmpty()) {
          SHORT_PATH.addAll(0, functionGlobalVarsPath);
        }
      }
    }

    /** This method adds all global variables used in the function to the Sets 
     * of important variables. Global variables assigned in the function will 
     * be deleted. The variables in the Expression "x" and "y" from "f(x,y)" 
     * are added to the Sets of important variables, too. 
     * 
     * @param funcEdge
     * @param possibleImportantVarsForGlobalVars */
    private static void getImportantVarsFromFunctionCall(
        final FunctionCallEdge funcEdge,
        final Set<String> possibleImportantVarsForGlobalVars) {

      // delete global variables assigned in the function,
      // delete all globalVars, the important ones will be added again later.
      IMPORTANT_VARS_FOR_GLOBAL_VARS.removeAll(GLOBAL_VARS);
      IMPORTANT_VARS.removeAll(GLOBAL_VARS);

      // if global variables are used in the function and they have an effect
      // to the result or the globalPath of the function,
      // add them to the important variables and to the importantGlobalVars.
      addGlobalVarsFromSetToSet(possibleImportantVarsForGlobalVars,
          IMPORTANT_VARS_FOR_GLOBAL_VARS);
      addGlobalVarsFromSetToSet(possibleImportantVarsForGlobalVars,
          IMPORTANT_VARS);

      // get a list with the expressions "x" and "y" from "f(x,y)"
      // all variables in the expressions are important
      final IASTExpression[] listOfExp = funcEdge.getArguments();
      if (listOfExp != null) {
        for (IASTExpression exp : listOfExp) {
          addAllVarsInExpToImportantVars(exp, IMPORTANT_VARS);
        }
      }
    }

    /** This method handles statements. */
    private static void handleStatement() {

      IASTExpression statementExp =
          ((StatementEdge) CURRENT_CFA_EDGE_PAIR.getSecond()).getExpression();

      // a unary operation, e.g. a++
      // this does not change the Set of important variables,
      // but the edge could be important
      if (statementExp instanceof IASTUnaryExpression) {
        handleUnaryStatement((IASTUnaryExpression) statementExp);
      }

      // expression is a binary operation, e.g. a = b;
      else if (statementExp instanceof IASTBinaryExpression) {
        handleAssignment((IASTBinaryExpression) statementExp);
      }

      // ext();
      else if (statementExp instanceof IASTFunctionCallExpression) {
        addCurrentCFAEdgePairToShortPath();
      }

      // a;
      else if (statementExp instanceof IASTIdExpression) {
        final String varName = statementExp.getRawSignature();
        if (IMPORTANT_VARS.contains(varName)) {
          addCurrentCFAEdgePairToShortPath();
        }
        if (IMPORTANT_VARS_FOR_GLOBAL_VARS.contains(varName)) {
          GLOBAL_VARS_PATH.addFirst(CURRENT_CFA_EDGE_PAIR);
        }
      }

      else {
        addCurrentCFAEdgePairToShortPath();
      }
    }

    /** This method handles unary statements (a++, a--).
     *
     * @param unaryExpression the expression to prove */
    private static void handleUnaryStatement(
        final IASTUnaryExpression unaryExpression) {

      // get operand, i.e. "a"
      final IASTExpression operand = unaryExpression.getOperand();

      if (operand instanceof IASTIdExpression) {
        final String varName = operand.getRawSignature();

        // an identifier is important, if it has been marked as important before.
        if (IMPORTANT_VARS.contains(varName)) {
          addCurrentCFAEdgePairToShortPath();
        }

        if (IMPORTANT_VARS_FOR_GLOBAL_VARS.contains(varName)) {
          GLOBAL_VARS_PATH.addFirst(CURRENT_CFA_EDGE_PAIR);
        }
      }
    }

    /** This method handles assignments (?a = ??).
     * 
     * @param binaryExpression the expression to prove */
    private static void handleAssignment(
        final IASTBinaryExpression binaryExpression) {

      IASTExpression lParam = binaryExpression.getOperand1();
      IASTExpression rightExp = binaryExpression.getOperand2();

      // a = ?
      if (lParam instanceof IASTIdExpression) {
        handleAssignmentToVariable(lParam.getRawSignature(), rightExp);
      }

      // TODO: assignment to pointer, *a = ?
      else if (lParam instanceof IASTUnaryExpression
          && ((IASTUnaryExpression) lParam).getOperator() == IASTUnaryExpression.op_star) {
        addCurrentCFAEdgePairToShortPath();
      }

      // TODO assignment to field, a->b = ?
      else if (lParam instanceof IASTFieldReference) {
        addCurrentCFAEdgePairToShortPath();
      }

      // TODO assignment to array cell, a[b] = ?
      else if (lParam instanceof IASTArraySubscriptExpression) {
        addCurrentCFAEdgePairToShortPath();
      }

      // if the edge is not unimportant, this edge could be important.
      else {
        addCurrentCFAEdgePairToShortPath();
      }
    }

    /** This method handles the assignment of a variable (a = ?).
     * 
     * @param lParam the local name of the variable to assign to
     * @param rightExp the assigning expression */
    private static void handleAssignmentToVariable(final String lParam,
        final IASTExpression rightExp) {

      // FIRST add edge to the Path, THEN remove lParam from Set
      if (IMPORTANT_VARS.contains(lParam)
          || IMPORTANT_VARS_FOR_GLOBAL_VARS.contains(lParam)) {
        addCurrentCFAEdgePairToShortPath();
      }

      // if lParam is important, the edge and rightExp are important.
      if (IMPORTANT_VARS.contains(lParam)) {

        // FIRST remove lParam, its history is unimportant.
        IMPORTANT_VARS.remove(lParam);

        // THEN update the Set
        addAllVarsInExpToImportantVars(rightExp, IMPORTANT_VARS);
      }

      // if lParam is a globalVar, all variables in the right expression are
      // important for a global variable and the Edge is part of globalVarPath.
      if (IMPORTANT_VARS_FOR_GLOBAL_VARS.contains(lParam)) {

        GLOBAL_VARS_PATH.addFirst(CURRENT_CFA_EDGE_PAIR);

        // FIRST remove lParam, its history is unimportant.
        IMPORTANT_VARS_FOR_GLOBAL_VARS.remove(lParam);

        // THEN update the Set
        addAllVarsInExpToImportantVars(rightExp, IMPORTANT_VARS);
        addAllVarsInExpToImportantVars(rightExp, IMPORTANT_VARS_FOR_GLOBAL_VARS);
      }
    }

    /** This method handles variable declarations ("int a;"). Expressions like
     * "int a=b;" are preprocessed by CIL to "int a; \n a=b;", so there is no
     * need to handle them. The expression "a=b;" is handled as StatementEdge. */
    private static void handleDeclaration() {

      DeclarationEdge declarationEdge =
          (DeclarationEdge) CURRENT_CFA_EDGE_PAIR.getSecond();

      /* Normally there is only one declarator in the DeclarationEdge.
       * If there are more than one declarators, CIL divides them into different
       * declarators while preprocessing:
       * "int a,b,c;"  -->  CIL  -->  "int a;  int b;  int c;".
       * If the declared variable is important, the edge is important. */
      for (IASTDeclarator declarator : declarationEdge.getDeclarators()) {
        final String varName = declarator.getName().getRawSignature();
        if (IMPORTANT_VARS.contains(varName)) {
          addCurrentCFAEdgePairToShortPath();
          // the variable is declared in this statement,
          // so it is not important in the CFA before. --> remove it.
          IMPORTANT_VARS.remove(varName);
        }
        if (IMPORTANT_VARS_FOR_GLOBAL_VARS.contains(varName)) {
          GLOBAL_VARS_PATH.addFirst(CURRENT_CFA_EDGE_PAIR);
        }
      }
    }

    /** This method handles assumptions (a==b, a<=b, true, etc.).
     * Assumptions always are handled as important edges. This method only adds
     * all variables in an assumption (expression) to the important variables. */
    private static void handleAssumption() {

      final IASTExpression assumeExp =
          ((AssumeEdge) CURRENT_CFA_EDGE_PAIR.getSecond()).getExpression();

      addAllVarsInExpToImportantVars(assumeExp, IMPORTANT_VARS);
      addCurrentCFAEdgePairToShortPath();

      if (!GLOBAL_VARS_PATH.isEmpty()) {
        addAllVarsInExpToImportantVars(assumeExp,
            IMPORTANT_VARS_FOR_GLOBAL_VARS);
        GLOBAL_VARS_PATH.addFirst(CURRENT_CFA_EDGE_PAIR);
      }
    }

    /** This method adds the current CFAEdgePair in front of the shortPath. */
    private static void addCurrentCFAEdgePairToShortPath() {
      SHORT_PATH.addFirst(CURRENT_CFA_EDGE_PAIR);
    }

    /** This method adds all variables in an expression to the Set of important
     * variables. If the expression exist of more than one sub-expressions,
     * the expression is divided into smaller parts and the method is called
     * recursively for each part, until there is only one variable or literal.
     * Literals are not part of important variables.
     *
     * @param exp the expression to be divided and added
     * @param importantVars all currently important variables */
    private static void addAllVarsInExpToImportantVars(
        final IASTExpression exp, final Set<String> importantVars) {

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
          IMPORTANT_VARS_FOR_GLOBAL_VARS.add(varName);
        }
      }

      // (cast) b
      else if (exp instanceof IASTCastExpression) {
        addAllVarsInExpToImportantVars(((IASTCastExpression) exp).getOperand(),
            importantVars);
      }

      // -b
      else if (exp instanceof IASTUnaryExpression) {
        addAllVarsInExpToImportantVars(
            ((IASTUnaryExpression) exp).getOperand(), importantVars);
      }

      // b op c; --> b is operand1, c is operand2
      else if (exp instanceof IASTBinaryExpression) {
        final IASTBinaryExpression binExp = (IASTBinaryExpression) exp;
        addAllVarsInExpToImportantVars(binExp.getOperand1(), importantVars);
        addAllVarsInExpToImportantVars(binExp.getOperand2(), importantVars);
      }
      // func(); 
      else if (exp instanceof IASTFunctionCallExpression) {
        // this case is handled as 'getImportantVarsFromFunctionCall' 
        // because of the functionReturnEgde after a functionCall.
        // so it should never appear.
        assert (false);
      }

      // or b->c;
      else if (exp instanceof IASTFieldReference) {
        // TODO: what should be added to importantVars?
      }
    }

    /** This function adds all globalVars from one Set to another Set.
     * 
     *  @param sourceSet where to read the variables
     *  @param targetSet where to store the variables */
    private static void addGlobalVarsFromSetToSet(final Set<String> sourceSet,
        final Set<String> targetSet) {
      for (String varName : sourceSet) {
        if (GLOBAL_VARS.contains(varName)) {
          targetSet.add(varName);
        }
      }
    }
  }
}