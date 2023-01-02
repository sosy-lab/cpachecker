// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * The Class ErrorPathShrinker gets an targetPath and creates a new Path, with only the important
 * edges of the Path. The idea behind this Class is, that not every action (CFAEdge) before an error
 * occurs is important for the error, only a few actions (CFAEdges) are important.
 */
public final class ErrorPathShrinker {

  /** The short Path stores the result of PathHandler.handlePath(). */
  private Deque<Pair<CFAEdgeWithAssumptions, Boolean>> shortPath;

  /** This Set stores the important variables of the Path. */
  private Set<String> importantVars;

  /** This is the currently handled CFAEdge. */
  private CFAEdge currentCFAEdge;

  /**
   * This is an important Edge constituted from current handled CFA Edge, plus accumulated
   * assumptions
   */
  private CFAEdgeWithAssumptions currentCFAEdgeWithAssumptions;

  /** This List contains the accumulated assumptions for the current CFAEdge */
  private ImmutableSet.Builder<AExpressionStatement> assumptions;

  /** This is only an UtilityClass. */
  public ErrorPathShrinker() {}

  /**
   * The function shrinkErrorPath gets an targetPath and creates a new Path, with only the important
   * edges of the Path.
   *
   * @param pTargetPath the "long" targetPath
   * @return errorPath the "short" errorPath
   */

  // TODO: step 1: CFAEDgewithAssumptions auch durchiterieren wenn nicht null in der gleichen
  // schleife;
  public ImmutableList<Pair<CFAEdgeWithAssumptions, Boolean>> shrinkErrorPath(
      ARGPath pTargetPath, CFAPathWithAssumptions pAssumePath) {
    List<CFAEdge> targetPath = pTargetPath.getFullPath();

    List<CFAEdgeWithAssumptions> assumePath = null;
    if (pAssumePath != null) {
      assumePath = pAssumePath.subList(0, targetPath.size());
    }

    // create reverse iterator, from lastNode to firstNode
    final Iterator<CFAEdge> revIterator = Lists.reverse(targetPath).iterator();

    // create reverse iterator of path containing assumptions
    Iterator<CFAEdgeWithAssumptions> revAssumIterator = null;
    if (assumePath != null) {
      revAssumIterator = Lists.reverse(assumePath).iterator();
    }

    // Set for storing the important variables
    importantVars = new HashSet<>();

    // Set for storing recent Assumptions
    assumptions = ImmutableSet.builder();

    // the short Path, the result
    final Deque<Pair<CFAEdgeWithAssumptions, Boolean>> shortErrorPath = new ArrayDeque<>();

    /* if the ErrorNode is inside of a function, the long path (pTargetPath) is not handled
     * until the StartNode, but only until the functionCall.
     * so update the sets of variables and call the PathHandler again until
     * the longPath is completely handled.*/
    while (revIterator.hasNext()) {
      handleEdge(revIterator.next(), shortErrorPath, revAssumIterator);
    }

    // TODO assertion disabled, until we can track all pointers completely
    // assert importantVars.isEmpty() : "some variables are never declared: " + importantVars;
    return ImmutableList.copyOf(shortErrorPath);
  }

  // this function is probably not needed, as the full path is believed to always end in the target
  // state
  /* This method iterates a path and copies all the edges until
   * the target state into the result.
   *
   * @param path the Path to iterate */
  /*
  private static List<CFAEdge> getEdgesUntilTarget(final ARGPath path) {
    int targetPos = indexOf(path.asStatesList(), IS_TARGET_STATE);
    if (targetPos > 0) {
      return path.getFullPath().subList(0, targetPos);
    } else {
      return path.getFullPath();
    }
  }
  */

  private void handleEdge(
      final CFAEdge cfaEdge,
      final Deque<Pair<CFAEdgeWithAssumptions, Boolean>> shortErrorPath,
      @Nullable Iterator<CFAEdgeWithAssumptions> revAssumIterator) {
    currentCFAEdge = cfaEdge;
    shortPath = shortErrorPath;

    // define all edges as normal with their according assumptions
    currentCFAEdgeWithAssumptions =
        new CFAEdgeWithAssumptions(currentCFAEdge, assumptions.build(), "");
    if (revAssumIterator != null && revAssumIterator.hasNext()) {
      CFAEdgeWithAssumptions nextRevAssumEdge = revAssumIterator.next();
      assumptions.addAll(nextRevAssumEdge.getExpStmts());
      currentCFAEdgeWithAssumptions =
          new CFAEdgeWithAssumptions(currentCFAEdge, nextRevAssumEdge.getExpStmts(), "");
    }

    // add each edge to the shrinkedErrorPath in the first place
    if (currentCFAEdgeWithAssumptions != null) {
      Pair<CFAEdgeWithAssumptions, Boolean> normalPair =
          Pair.of(currentCFAEdgeWithAssumptions, Boolean.FALSE);
      shortPath.addFirst(normalPair);
    }

    switch (cfaEdge.getEdgeType()) {
      case AssumeEdge:
        handleAssumption(((AssumeEdge) cfaEdge).getExpression());
        break;

      case FunctionCallEdge:
        final FunctionCallEdge fnkCall = (FunctionCallEdge) cfaEdge;
        final FunctionEntryNode succ = fnkCall.getSuccessor();
        handleFunctionCallEdge(fnkCall.getArguments(), succ.getFunctionParameters());
        break;

      case FunctionReturnEdge:
        final FunctionReturnEdge fnkReturnEdge = (FunctionReturnEdge) cfaEdge;
        final FunctionSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
        handleFunctionReturnEdge(fnkReturnEdge, summaryEdge.getExpression());

        break;

      default:
        handleSimpleEdge(cfaEdge);
    }
  }

  /**
   * This function handles simple edges like Declarations, Statements, ReturnStatements and
   * BlankEdges. They have in common, that they all can be part of an MultiEdge.
   */
  @SuppressWarnings("unchecked")
  private void handleSimpleEdge(final CFAEdge cfaEdge) {

    switch (cfaEdge.getEdgeType()) {
      case DeclarationEdge:
        handleDeclarationEdge(((ADeclarationEdge) cfaEdge).getDeclaration());
        break;

      case StatementEdge:
        handleStatementEdge(((AStatementEdge) cfaEdge).getStatement());
        break;

      case ReturnStatementEdge:
        // this statement is a function return, e.g. return (a);
        // note that this is different from return edge,
        // this is a statement edge, which leads the function to the
        // last node of its CFA, where return edge is from that last node
        // to the return site of the caller function
        handleReturnStatementEdge((AReturnStatementEdge) cfaEdge);
        break;

      case BlankEdge:
        handleBlankEdge((BlankEdge) cfaEdge);
        break;

      case CallToReturnEdge:
        throw new AssertionError("function summaries not supported");

      default:
        throw new AssertionError("unknown edge type");
    }
  }

  private void handleBlankEdge(BlankEdge cfaEdge) {
    assert cfaEdge == currentCFAEdge;
    if (currentCFAEdge.getSuccessor().isLoopStart()) {
      addCurrentCFAEdgeToShortPath();
    }
  }

  private void handleReturnStatementEdge(AReturnStatementEdge returnEdge) {
    if (returnEdge.asAssignment().isPresent()) {
      AAssignment assignment = returnEdge.asAssignment().get();
      handleAssignmentToVariable(assignment.getLeftHandSide(), assignment.getRightHandSide());
    }
  }

  /** This method makes a recursive call of handlePath(). */
  private void handleFunctionReturnEdge(
      FunctionReturnEdge fnkReturnEdge, AFunctionCall expression) {

    if (expression instanceof AFunctionCallAssignmentStatement) {
      Optional<? extends AVariableDeclaration> returnVar =
          fnkReturnEdge.getFunctionEntry().getReturnVariable();
      if (returnVar.isPresent() && isImportant(returnVar.get())) {
        remove(returnVar.get());
        track(returnVar.get().getQualifiedName());
      }
    }
  }

  private void handleFunctionCallEdge(
      List<? extends AExpression> arguments,
      List<? extends AParameterDeclaration> functionParameters) {

    addCurrentCFAEdgeToShortPath(); // functioncalls are important
    for (int i = 0; i < functionParameters.size(); i++) {
      AExpression arg = arguments.get(i);
      final String paramName = functionParameters.get(i).getQualifiedName();
      if (isImportant(paramName)) {
        remove(paramName);
        addAllVarsInExpToSet(arg);
      }
    }
  }

  /** This method handles statements. */
  private void handleStatementEdge(AStatement statementExp) {

    // expression is an assignment operation, e.g. a = b;
    if (statementExp instanceof AAssignment) {
      handleAssignmentToVariable(
          ((AAssignment) statementExp).getLeftHandSide(),
          ((AAssignment) statementExp).getRightHandSide());
    }

    // ext(); external functioncall
    else if (statementExp instanceof AFunctionCall) {
      addCurrentCFAEdgeToShortPath();
    }
  }

  /**
   * This method handles the assignment of a variable (a = ?).
   *
   * @param lParam the local name of the variable to assign to
   * @param rightExp the assigning expression
   */
  private void handleAssignmentToVariable(
      final ALeftHandSide lParam, final ARightHandSide rightExp) {

    // FIRST add edge to the Path, THEN remove lParam from Set
    if (isImportant(lParam)) {
      addCurrentCFAEdgeToShortPath();

      // FIRST remove lParam, its history is unimportant.
      remove(lParam);

      // THEN update the Set
      addAllVarsInExpToSet(rightExp);
    }
  }

  /** This method handles variable declarations ("int a;" or "int b=a+123;"). */
  private void handleDeclarationEdge(ADeclaration declaration) {

    /* If the declared variable is important, the edge is important. */
    if (declaration.getName() != null) {
      if (isImportant(declaration)) {
        addCurrentCFAEdgeToShortPath();

        if (declaration instanceof AVariableDeclaration) {
          AInitializer init = ((AVariableDeclaration) declaration).getInitializer();
          if (init != null) {
            final Deque<AInitializer> inits = new ArrayDeque<>();
            inits.add(init);
            while (!inits.isEmpty()) {
              init = inits.pop();
              if (init instanceof CInitializerExpression) {
                addAllVarsInExpToSet(((CInitializerExpression) init).getExpression());
              } else if (init instanceof CInitializerList) {
                inits.addAll(((CInitializerList) init).getInitializers());
              }
            }
          }
        }
        // the variable is declared in this statement,
        // so it is not important in the CFA before. --> remove it.
        remove(declaration);
      }
    }
  }

  /**
   * This method handles assumptions (a==b, a<=b, true, etc.). Assumptions are not handled as
   * important edges, if they are part of a switchStatement. Otherwise this method only adds all
   * variables in an assumption (expression) to the important variables.
   */
  private void handleAssumption(AExpression assumeExp) {
    if (!isSwitchStatement(assumeExp)) {
      addAllVarsInExpToSet(assumeExp);
      addCurrentCFAEdgeToShortPath();
    }
  }

  /**
   * This method checks, if the current assumption is part of a switchStatement. Therefore it
   * compares the current assumption with the expression of the last added CFAEdge. It can also
   * check similar assumptions like "if(x>3) {if(x>4){...}}".
   *
   * @param assumeExp the current assumption
   * @return is the assumption part of a switchStatement?
   */
  private boolean isSwitchStatement(final AExpression assumeExp) {

    // path can be empty at the end of a functionCall ("if (a) return b;")
    if (!shortPath.isEmpty() && shortPath.iterator().next().getSecond()) {
      final CFAEdge lastEdge = shortPath.getFirst().getFirst().getCFAEdge();

      // check, if the last edge was an assumption
      if (assumeExp instanceof ABinaryExpression && lastEdge instanceof AssumeEdge) {
        final AssumeEdge lastAss = (AssumeEdge) lastEdge;
        final AExpression lastExp = lastAss.getExpression();

        // check, if the last edge was like "a==b"
        if (lastExp instanceof ABinaryExpression) {
          final AExpression currentBinExpOp1 = ((ABinaryExpression) assumeExp).getOperand1();
          final AExpression lastBinExpOp1 = ((ABinaryExpression) lastExp).getOperand1();

          // only the first variable of the assignment is checked
          final boolean isEqualVarName =
              currentBinExpOp1.toASTString().equals(lastBinExpOp1.toASTString());

          // check, if lastEdge is the true-branch of "==" or the false-branch of "!="
          ABinaryExpression aLastExp = ((ABinaryExpression) lastExp);
          final boolean isEqualOp;

          if (aLastExp instanceof CBinaryExpression) {
            final CBinaryExpression.BinaryOperator op =
                (CBinaryExpression.BinaryOperator) aLastExp.getOperator();
            isEqualOp =
                (op == CBinaryExpression.BinaryOperator.EQUALS && lastAss.getTruthAssumption())
                    || (op == CBinaryExpression.BinaryOperator.NOT_EQUALS
                        && !lastAss.getTruthAssumption());

          } else {
            final JBinaryExpression.BinaryOperator op =
                (JBinaryExpression.BinaryOperator) aLastExp.getOperator();
            isEqualOp =
                (op == JBinaryExpression.BinaryOperator.EQUALS && lastAss.getTruthAssumption())
                    || (op == JBinaryExpression.BinaryOperator.NOT_EQUALS
                        && !lastAss.getTruthAssumption());
          }
          return isEqualVarName && isEqualOp;
        }
      }
    }
    return false;
  }

  /**
   * This method adds all variables in an expression to a Set. If the expression exist of more than
   * one sub-expressions, the expression is divided into smaller parts and the method is called
   * recursively for each part, until there is only one variable or literal. Literals are not part
   * of important variables.
   *
   * @param exp the expression to be divided and added
   */
  private void addAllVarsInExpToSet(final ARightHandSide exp) {

    // TODO replace with expression-visitor?

    // exp = 8.2 or "return;" (when exp == null),
    // this does not change the Set importantVars,
    if (exp instanceof ALiteralExpression
        || exp instanceof AFunctionCallExpression
        || exp == null) {
      // do nothing
    }

    // exp is an Identifier, i.e. the "b" from "a = b"
    else if (exp instanceof AIdExpression) {
      track((AIdExpression) exp);
    }

    // (cast) b
    else if (exp instanceof CCastExpression) {
      addAllVarsInExpToSet(((CCastExpression) exp).getOperand());
    }

    // -b
    else if (exp instanceof AUnaryExpression) {
      addAllVarsInExpToSet(((AUnaryExpression) exp).getOperand());
    }

    // b op c; --> b is operand1, c is operand2
    else if (exp instanceof ABinaryExpression) {
      final ABinaryExpression binExp = (ABinaryExpression) exp;
      addAllVarsInExpToSet(binExp.getOperand1());
      addAllVarsInExpToSet(binExp.getOperand2());
    }

    // a fieldReference "b->c" is handled as one variable with the name "b->c".
    else if (exp instanceof CFieldReference) {
      track((CFieldReference) exp);
    }
  }

  private boolean isImportant(AExpression exp) {
    return isImportant(str(exp));
  }

  private boolean isImportant(ASimpleDeclaration exp) {
    return isImportant(exp.getQualifiedName());
  }

  private boolean isImportant(String var) {
    return importantVars.contains(var);
  }

  private void track(AExpression exp) {
    track(str(exp));
  }

  private void track(String var) {
    importantVars.add(var);
  }

  private void remove(AExpression exp) {
    remove(str(exp));
  }

  private void remove(ASimpleDeclaration exp) {
    remove(exp.getQualifiedName());
  }

  private void remove(String var) {
    importantVars.remove(var);
  }

  private String str(AExpression exp) {
    if (exp instanceof AIdExpression) {
      return ((AIdExpression) exp).getDeclaration().getQualifiedName();
    } else {
      return exp.toASTString();
    }
  }

  /** This method adds the current CFAEdge in front of the shortPath. */
  private void addCurrentCFAEdgeToShortPath() {

    // when an edge is important, remove the identical edge containing the Boolean value "false" and
    // add the edge again with the Boolean value "true"
    if (shortPath.getFirst().getFirst() == currentCFAEdgeWithAssumptions) {
      shortPath.removeFirst();
    }

    // Add the set of recent assumptions to the important CFAEdgeWithAssumptions
    assumptions.addAll(currentCFAEdgeWithAssumptions.getExpStmts());

    Pair<CFAEdgeWithAssumptions, Boolean> importantPair =
        Pair.of(
            new CFAEdgeWithAssumptions(
                currentCFAEdgeWithAssumptions.getCFAEdge(),
                assumptions.build(),
                currentCFAEdgeWithAssumptions.getComment()),
            Boolean.TRUE);

    // empty assumptions for fresh accumulation for next edge in short path
    assumptions = ImmutableSet.builder();

    shortPath.addFirst(importantPair);
  }
}
