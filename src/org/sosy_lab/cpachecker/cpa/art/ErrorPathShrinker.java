/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

package org.sosy_lab.cpachecker.cpa.art;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

/** The Class ErrorPathShrinker gets an targetPath and creates a new Path,
 * with only the important edges of the Path. The idea behind this Class is,
 * that not every action (CFAEdge) before an error occurs is important for
 * the error, only a few actions (CFAEdges) are important.
 */
public final class ErrorPathShrinker {

  /** This is only an UtilityClass. */
  public ErrorPathShrinker() {
  }

  /** Set<String> for storing the global variables. */
  private static final Set<String> GLOBAL_VARS = new LinkedHashSet<String>();

  /** The function shrinkErrorPath gets an targetPath and creates a new Path,
   * with only the important edges of the Path.
   *
   * @param targetPath the "long" targetPath
   * @return errorPath the "short" errorPath */
  public Path shrinkErrorPath(final Path targetPath) {

    // first collect all global variables
    findGlobalVarsInPath(targetPath);

    // create reverse iterator, from lastNode to firstNode
    final Iterator<Pair<ARTElement, CFAEdge>> revIterator =
        targetPath.descendingIterator();

    // Set for storing the important variables
    Set<String> importantVars = new LinkedHashSet<String>();

    // Set for storing the global variables, that are important and used
    // during proving the edges.
    Set<String> importantVarsForGlobalVars = new LinkedHashSet<String>();

    // Path for storing changings of globalVars
    final Path globalVarsPath = new Path();

    // the short Path, the result
    final Path shortErrorPath = new Path();

    // the errorNode is important
    shortErrorPath.addFirst(revIterator.next());

    /* if the ErrorNode is inside of a function, the longPath is not handled
     * until the StartNode, but only until the functionCall.
     * so update the sets of variables and call the PathHandler again until
     * the longPath is completely handled.*/
    while (revIterator.hasNext()) {

      final PathHandler pathHandler =
          new PathHandler(shortErrorPath, revIterator, importantVars,
              importantVarsForGlobalVars, globalVarsPath);
      pathHandler.handlePath();

      // the pathHandler stops at a functionStart or at the start of program.
      // so if the lastEdge is a functionCall, the path is not finished.
      final CFAEdge lastEdge = shortErrorPath.getFirst().getSecond();
      if (lastEdge instanceof FunctionCallEdge) {
        final FunctionCallEdge funcEdge = (FunctionCallEdge) lastEdge;

        // "f(x)" or "a = f(x)",
        // the Error occured in the function, the left param is not important,
        // only global vars and the 'x' from 'f(x)' can influence the Error.

        // put only global vars to the new Set
        final Set<String> newImportantVars = new LinkedHashSet<String>();
        addGlobalVarsFromSetToSet(importantVars, newImportantVars);
        importantVars = newImportantVars;

        final Set<String> newImportantVarsForGlobalVars =
            new LinkedHashSet<String>();
        addGlobalVarsFromSetToSet(importantVarsForGlobalVars,
            newImportantVarsForGlobalVars);
        importantVarsForGlobalVars = newImportantVarsForGlobalVars;

        getImportantVarsFromFunctionCall(funcEdge, importantVars,
            importantVarsForGlobalVars);
      }
    }
    return shortErrorPath;
  }

  /** This method iterates a Path and adds all global Variables to the Set
   * of global variables.
   *
   * @param path the Path to iterate */
  private void findGlobalVarsInPath(final Path path) {

    // iterate through the Path and collect all important variables
    final Iterator<Pair<ARTElement, CFAEdge>> iterator = path.iterator();
    while (iterator.hasNext()) {
      CFAEdge cfaEdge = iterator.next().getSecond();

      // only globalDeclarations (SubType of Declaration) are important
      if (cfaEdge instanceof GlobalDeclarationEdge) {
        DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;

        IType specifier = declarationEdge.getDeclSpecifier();
        if (declarationEdge.getName() != null) {
          // if a variable (declarator) is no pointer variable,
          // it is added to the list of global variables
          if (!(specifier instanceof IASTPointerTypeSpecifier)) {
            GLOBAL_VARS.add(declarationEdge.getName().toString());
          }
        }
      }
    }
  }

  /** This method adds all variables in an expression to a Set.
   * If the expression exist of more than one sub-expressions,
   * the expression is divided into smaller parts and the method is called
   * recursively for each part, until there is only one variable or literal.
   * Literals are not part of important variables.
   *
   * @param exp the expression to be divided and added
   * @param importantVars all currently important variables
   * @param importantVarsForGlobalVars variables, that influence global vars */
  private void addAllVarsInExpToSet(final IASTExpression exp,
      final Set<String> importantVars,
      final Set<String> importantVarsForGlobalVars) {

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
        importantVarsForGlobalVars.add(varName);
      }
    }

    // (cast) b
    else if (exp instanceof IASTCastExpression) {
      addAllVarsInExpToSet(((IASTCastExpression) exp).getOperand(),
          importantVars, importantVarsForGlobalVars);
    }

    // -b
    else if (exp instanceof IASTUnaryExpression) {
      addAllVarsInExpToSet(((IASTUnaryExpression) exp).getOperand(),
          importantVars, importantVarsForGlobalVars);
    }

    // b op c; --> b is operand1, c is operand2
    else if (exp instanceof IASTBinaryExpression) {
      final IASTBinaryExpression binExp = (IASTBinaryExpression) exp;
      addAllVarsInExpToSet(binExp.getOperand1(), importantVars,
          importantVarsForGlobalVars);
      addAllVarsInExpToSet(binExp.getOperand2(), importantVars,
          importantVarsForGlobalVars);
    }

    // a fieldReference "b->c" is handled as one variable with the name "b->c".
    else if (exp instanceof IASTFieldReference) {
      final String varName = exp.getRawSignature();
      importantVars.add(varName);
      if (GLOBAL_VARS.contains(varName)) {
        importantVarsForGlobalVars.add(varName);
      }
    }
  }

  /** This function adds all globalVars from one Set to another Set.
   *
   *  @param sourceSet where to read the variables
   *  @param targetSet where to store the variables */
  private void addGlobalVarsFromSetToSet(final Set<String> sourceSet,
      final Set<String> targetSet) {
    for (String varName : sourceSet) {
      if (GLOBAL_VARS.contains(varName)) {
        targetSet.add(varName);
      }
    }
  }

  /** This method adds the variables in the Expression "x" and "y" from
   * "f(x,y)" to the Sets of important variables.
   *
   * @param funcEdge the Edge, where the variables are taken from
   * @param importantVars all important variables
   * @param importantVarsForGlobalVars variables, that influence global vars */
  private void getImportantVarsFromFunctionCall(
      final FunctionCallEdge funcEdge, final Set<String> importantVars,
      final Set<String> importantVarsForGlobalVars) {

    // get a list with the expressions "x" and "y" from "f(x,y)"
    // all variables in the expressions are important
    for (IASTExpression exp : funcEdge.getArguments()) {
      addAllVarsInExpToSet(exp, importantVars, importantVarsForGlobalVars);
    }
  }

  /** This is a inner Class, that can handle a Path until a functionCallEdge. */
  private final class PathHandler {

    /** The short Path stores the result of PathHandler.handlePath(). */
    private final Path                                shortPath;

    /** The reverse iterator runs from lastNode to firstNode. */
    private final Iterator<Pair<ARTElement, CFAEdge>> reverseIterator;

    /** This Set stores the important variables of the Path. */
    private final Set<String>                         importantVars;

    /** This Set stores the global variables, that are important and used
     * during proving the edges. */
    private final Set<String>                         importantVarsForGlobalVars;

    /** This Path stores CFAEdges, where globalVars or
     * importantVarsForGlobalVars are assigned.*/
    private final Path                                globalVarsPath;

    /** This is the currently handled CFAEdgePair. */
    private Pair<ARTElement, CFAEdge>                 currentCFAEdgePair;

    /** The Constructor of this Class gets some references (pointers) from the
     * callerFunction, all of them may be changed during 'handlePath()'.
     *
     *  @param shortPathOut the shortErrorPath of the callerFunction
     *  @param revIteratorOut the reverse iterator of the callerFunction,
     *         storing the current CFAEdgePair of the longErrorPath
     *  @param importantVarsOut a Set with important variables
     *  @param importantVarsForGlobalVarsOut a Set with important variables,
     *         that influence globalVars
     *  @param globalVarsPathOut the Path of the callerFunction, storing the
     *         CFAEdgePairs, where globalVars or importantVarsForGlobalVars
     *         are assigned */
    private PathHandler(final Path shortPathOut,
        final Iterator<Pair<ARTElement, CFAEdge>> revIteratorOut,
        final Set<String> importantVarsOut,
        final Set<String> importantVarsForGlobalVarsOut,
        final Path globalVarsPathOut) {
      shortPath = shortPathOut;
      reverseIterator = revIteratorOut;
      importantVars = importantVarsOut;
      importantVarsForGlobalVars = importantVarsForGlobalVarsOut;
      globalVarsPath = globalVarsPathOut;
    }

    /** This function gets a Path and shrinks it to a shorter Path,
     * only important edges from the first Path are in the shortPath. */
    private void handlePath() {

      // iterate the Path (backwards) and collect all important variables
      while (reverseIterator.hasNext()) {
        currentCFAEdgePair = reverseIterator.next();
        CFAEdge cfaEdge = currentCFAEdgePair.getSecond();

        // check the type of the edge
        switch (cfaEdge.getEdgeType()) {

        // if edge is a statement edge, e.g. a = b + c
        case StatementEdge:
          // this is a regular statement
          handleStatement();
          break;

        case ReturnStatementEdge:
          // this is the statement edge which leads the function to the
          // last node of its CFA (not same as a return edge)
          handleJumpStatement();
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
         * a loopstart ("while" or "goto loopstart") is important,
         * a jumpEdge ("goto") is important, iff it contains the word "error",
         * a labelEdge and a really blank edge are not important.
         * TODO are there more types? */
        case BlankEdge:
          if (cfaEdge.getSuccessor().isLoopStart()
              || (cfaEdge.isJumpEdge() && cfaEdge.getRawStatement()
                  .toLowerCase().contains("error"))) {
            addCurrentCFAEdgePairToShortPath();
          }
          break;

        // start of a function, so "return" to the higher recursive call
        case FunctionCallEdge:
          addCurrentCFAEdgePairToShortPath();
          if (!globalVarsPath.isEmpty()) {
            globalVarsPath.addFirst(currentCFAEdgePair);
          }
          return;

          // this is a return edge from function, this is different from return
          // statement of the function. See case in statement edge for details
        case FunctionReturnEdge:
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
    private void handleJumpStatement() {

      // Set for storing the important variables, normally empty when leaving
      // handlePath(), because declarators are removed, except globalVars.
      final Set<String> possibleVars = new LinkedHashSet<String>();
      addGlobalVarsFromSetToSet(importantVarsForGlobalVars, possibleVars);

      // in the expression "return r" the value "r" is possibly important.
      final IASTExpression returnExp =
          ((ReturnStatementEdge) currentCFAEdgePair.getSecond()).getExpression();
      addAllVarsInExpToSet(returnExp, possibleVars,
          importantVarsForGlobalVars);

      final Pair<ARTElement, CFAEdge> returnEdgePair = currentCFAEdgePair;

      // Path for storing changings of variables of importantVarsForGlobalVars
      final Path functionGlobalVarsPath = new Path();

      // the short Path is the result, the last element is the "return"-Node
      final Path shortFunctionPath = new Path();

      // Set for storing the global variables, that are possibly important
      // in the function. copy all global variables in another Set,
      // they could be assigned in the function.
      final Set<String> possibleImportantVarsForGlobalVars =
          new LinkedHashSet<String>();

      // only global variables can be used inside AND outside of a function
      addGlobalVarsFromSetToSet(importantVarsForGlobalVars,
          possibleImportantVarsForGlobalVars);

      // this is a recursive call to handle the Path inside of the function
      final PathHandler recPathHandler =
          new PathHandler(shortFunctionPath, reverseIterator, possibleVars,
              possibleImportantVarsForGlobalVars, functionGlobalVarsPath);
      recPathHandler.handlePath();

      /*
      System.out.println("funcPath:\n" + shortFunctionPath);
      System.out.println("globPath:\n" + functionGlobalVarsPath);
      */

      mergeResultsOfFunctionCall(shortFunctionPath, returnEdgePair,
          possibleVars, possibleImportantVarsForGlobalVars,
          functionGlobalVarsPath);
    }

    /** This method merges the Path of the function with the Path of the
     * calling program. It also merges the Sets of Variables of the
     * functionCall with the Set from the calling program.
     * @param shortFunctionPath the short Path of the function
     * @param returnEdgePair the last CFAEdgePair of the function
     * @param possibleVars
     *          variables, that are important for the result of the function
     * @param possibleImportantVarsForGlobalVars
     *          variables, that are important for global vars in the function
     * @param functionGlobalVarsPath
     *          Path with Edges that influence global vars */
    private void mergeResultsOfFunctionCall(final Path shortFunctionPath,
        final Pair<ARTElement, CFAEdge> returnEdgePair,
        final Set<String> possibleVars,
        final Set<String> possibleImportantVarsForGlobalVars,
        final Path functionGlobalVarsPath) {

      // the recursive call stops at the functionStart,
      // so the lastEdge is the functionCall and there exist a
      // CallToReturnEdge, that jumps over the hole function
      final CFAEdge lastEdge = shortFunctionPath.getFirst().getSecond();
      assert (lastEdge instanceof FunctionCallEdge);
      final FunctionCallEdge funcEdge = (FunctionCallEdge) lastEdge;
      final CallToReturnEdge funcSummaryEdge =
          funcEdge.getPredecessor().getLeavingSummaryEdge();
      final IASTFunctionCall funcExp = funcSummaryEdge.getExpression();

      // "f(x)", without a variable "a" as "a = f(x)".
      // if the function changes the global variables,
      // get the variables from the function and update the Sets and Paths
      if (funcExp instanceof IASTFunctionCallStatement
          && !functionGlobalVarsPath.isEmpty()) {

        getImportantVarsFromFunction(possibleImportantVarsForGlobalVars);
        getImportantVarsFromFunctionCall(funcEdge, importantVars,
            importantVarsForGlobalVars);

        // add the important edges in front of the shortPath
        shortPath.addFirst(returnEdgePair);
        globalVarsPath.addFirst(returnEdgePair);
        shortPath.addAll(0, functionGlobalVarsPath);
        globalVarsPath.addAll(0, functionGlobalVarsPath);
      }

      // "a = f(x)"
      if (funcExp instanceof IASTFunctionCallAssignmentStatement) {
        final String lParam =
            ((IASTFunctionCallAssignmentStatement) funcExp).getLeftHandSide().getRawSignature();

        // if the function has a important result or changes the global
        // variables, get the params from the function and update the Sets.
        if (importantVars.contains(lParam)
            || !functionGlobalVarsPath.isEmpty()) {

          getImportantVarsFromFunction(possibleImportantVarsForGlobalVars);
          getImportantVarsFromFunctionCall(funcEdge, importantVars,
              importantVarsForGlobalVars);

          // add the returnEdge in front of the shortPath
          shortPath.addFirst(returnEdgePair);
          globalVarsPath.addFirst(returnEdgePair);
          globalVarsPath.addAll(0, functionGlobalVarsPath);
        }

        // if the variable funcAssumeVar (result of the function) is important,
        // add the functionPath in front of the shortPath,
        // (the globalVarsPath is always part of the shortFunctionPath)
        if (importantVars.contains(lParam)) {
          shortPath.addAll(0, shortFunctionPath);
        }

        // if the variable funcAssumeVar (result of function) is unimportant,
        // but the function changes values of global variables used later,
        // add the functionglobalVarsPath in front of the shortPath
        else if (!functionGlobalVarsPath.isEmpty()) {
          shortPath.addAll(0, functionGlobalVarsPath);
        }
      }
    }

    /** This method adds all global variables used in the function to the Sets
     * of important variables. Global variables assigned in the function will
     * be deleted.
     *
     * @param possibleImportantVarsForGlobalVars
     *        Set of possible important variables */
    private void getImportantVarsFromFunction(
        final Set<String> possibleImportantVarsForGlobalVars) {

      // delete global variables assigned in the function,
      // delete all globalVars, the important ones will be added again later.
      importantVarsForGlobalVars.removeAll(GLOBAL_VARS);
      importantVars.removeAll(GLOBAL_VARS);

      // if global variables are used in the function and they have an effect
      // to the result or the globalPath of the function,
      // add them to the important variables and to the importantGlobalVars.
      addGlobalVarsFromSetToSet(possibleImportantVarsForGlobalVars,
          importantVarsForGlobalVars);
      addGlobalVarsFromSetToSet(possibleImportantVarsForGlobalVars,
          importantVars);
    }

    /** This method handles statements. */
    private void handleStatement() {

      IASTStatement statementExp =
          ((StatementEdge) currentCFAEdgePair.getSecond()).getStatement();

      // expression is an assignment operation, e.g. a = b;
      if (statementExp instanceof IASTExpressionAssignmentStatement) {
        handleAssignment((IASTExpressionAssignmentStatement) statementExp);
      }

      // ext();
      else if (statementExp instanceof IASTFunctionCall) {
        addCurrentCFAEdgePairToShortPath();
      }
    }

    /** This method handles assignments (?a = ??).
     *
     * @param binaryExpression the expression to prove */
    private void handleAssignment(final IASTExpressionAssignmentStatement assignmentExpression) {

      IASTExpression lParam = assignmentExpression.getLeftHandSide();
      IASTExpression rightExp = assignmentExpression.getRightHandSide();

      // a = ?
      if (lParam instanceof IASTIdExpression) {
        handleAssignmentToVariable(lParam.getRawSignature(), rightExp);
      }

      // TODO: assignment to pointer, *a = ?
      else if (lParam instanceof IASTUnaryExpression
          && ((IASTUnaryExpression) lParam).getOperator() == UnaryOperator.STAR) {
        addCurrentCFAEdgePairToShortPath();
      }

      // "a->b = ?", assignment to field is handled as assignment to variable.
      else if (lParam instanceof IASTFieldReference) {
        handleAssignmentToVariable(lParam.getRawSignature(), rightExp);
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
    private void handleAssignmentToVariable(final String lParam,
        final IASTExpression rightExp) {

      // FIRST add edge to the Path, THEN remove lParam from Set
      if (importantVars.contains(lParam)
          || importantVarsForGlobalVars.contains(lParam)) {
        addCurrentCFAEdgePairToShortPath();
      }

      // if lParam is important, the edge and rightExp are important.
      if (importantVars.contains(lParam)) {

        // FIRST remove lParam, its history is unimportant.
        importantVars.remove(lParam);

        // THEN update the Set
        addAllVarsInExpToSet(rightExp, importantVars,
            importantVarsForGlobalVars);
      }

      // if lParam is a globalVar, all variables in the right expression are
      // important for a global variable and the Edge is part of globalVarPath.
      if (importantVarsForGlobalVars.contains(lParam)) {

        globalVarsPath.addFirst(currentCFAEdgePair);

        // FIRST remove lParam, its history is unimportant.
        importantVarsForGlobalVars.remove(lParam);

        // THEN update the Set
        addAllVarsInExpToSet(rightExp, importantVars,
            importantVarsForGlobalVars);
        addAllVarsInExpToSet(rightExp, importantVarsForGlobalVars,
            importantVarsForGlobalVars);
      }
    }

    /** This method handles variable declarations ("int a;").
     * Expressions like "int a=b;" are divided by CIL into "int a;" and "a=b;",
     * so there is no need to handle them. The expression "a=b;" is then
     * handled as StatementEdge. Global declarations are not divided by CIL. */
    private void handleDeclaration() {

      DeclarationEdge declarationEdge =
          (DeclarationEdge) currentCFAEdgePair.getSecond();

      /* If the declared variable is important, the edge is important. */
      if (declarationEdge.getName() != null) {
        final String varName = declarationEdge.getName();
        if (importantVars.contains(varName)) {
          addCurrentCFAEdgePairToShortPath();

          // the variable is declared in this statement,
          // so it is not important in the CFA before. --> remove it.
          importantVars.remove(varName);
        }
        if (importantVarsForGlobalVars.contains(varName)) {
          globalVarsPath.addFirst(currentCFAEdgePair);
        }
      }
    }

    /** This method handles assumptions (a==b, a<=b, true, etc.).
     * Assumptions are not handled as important edges, if they are part of a
     * switchStatement. Otherwise this method only adds all variables in an
     * assumption (expression) to the important variables. */
    private void handleAssumption() {
      final IASTExpression assumeExp =
          ((AssumeEdge) currentCFAEdgePair.getSecond()).getExpression();

      if (!isSwitchStatement(assumeExp)) {
        addAllVarsInExpToSet(assumeExp, importantVars,
            importantVarsForGlobalVars);
        addCurrentCFAEdgePairToShortPath();

        if (!globalVarsPath.isEmpty()) {
          addAllVarsInExpToSet(assumeExp, importantVarsForGlobalVars,
              importantVarsForGlobalVars);
          globalVarsPath.addFirst(currentCFAEdgePair);
        }
      }
    }

    /** This method checks, if the current assumption is part of a
     * switchStatement. Therefore it compares the current assumption with
     * the expression of the last added CFAEdge. It can also check similar
     * assumptions like  "if(x>3) {if(x>4){...}}".
     *
     * @param assumeExp the current assumption
     * @return is the assumption part of a switchStatement? */
    private boolean isSwitchStatement(final IASTExpression assumeExp) {

      // Path can be empty at the end of a functionCall ("if (a) return b;")
      if (!shortPath.isEmpty()) {
        final CFAEdge lastEdge = shortPath.getFirst().getSecond();

        //check, if the last edge was an assumption
        if (assumeExp instanceof IASTBinaryExpression
            && lastEdge instanceof AssumeEdge) {
          final IASTExpression lastExp =
              ((AssumeEdge) lastEdge).getExpression();

          // check, if the last egde was like "a==b"
          if (lastExp instanceof IASTBinaryExpression) {
            final IASTExpression currentBinExpOp1 =
                ((IASTBinaryExpression) assumeExp).getOperand1();
            final IASTExpression lastBinExpOp1 =
                ((IASTBinaryExpression) lastExp).getOperand1();

            // only the first variable of the assignment is checked
            final boolean isEqualVarName = currentBinExpOp1.getRawSignature().
            equals(lastBinExpOp1.getRawSignature());

            // switchStatement:     !(x==3);(x==4);   -> operator "=="
            // similar assumption:  (x>3);(x>4);      -> operator ">"
            // the operator is stored as 'int'
            final boolean isEqualOperator =
                ((IASTBinaryExpression) assumeExp).getOperator()
                == ((IASTBinaryExpression) lastExp).getOperator();

            return (isEqualVarName && isEqualOperator);
          }
        }
      }
      return false;
    }

    /** This method adds the current CFAEdgePair in front of the shortPath. */
    private void addCurrentCFAEdgePairToShortPath() {
      shortPath.addFirst(currentCFAEdgePair);
    }
  }
}