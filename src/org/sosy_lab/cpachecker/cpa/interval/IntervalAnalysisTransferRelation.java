// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static org.sosy_lab.cpachecker.cpa.interval.Interval.ONE;
import static org.sosy_lab.cpachecker.cpa.interval.Interval.ZERO;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class IntervalAnalysisTransferRelation
    extends ForwardingTransferRelation<
        Collection<IntervalAnalysisState>, IntervalAnalysisState, Precision> {

  private final boolean splitIntervals;
  private final int threshold;
  private final LogManager logger;

  public IntervalAnalysisTransferRelation(
      boolean pSplitIntervals, int pThreshold, LogManager pLogger) {
    splitIntervals = pSplitIntervals;
    threshold = pThreshold;
    logger = pLogger;
  }

  @Override
  protected Collection<IntervalAnalysisState> postProcessing(
      Collection<IntervalAnalysisState> successors, CFAEdge edge) {
    return new HashSet<>(successors);
  }

  @Override
  protected Collection<IntervalAnalysisState> handleBlankEdge(BlankEdge cfaEdge) {
    IntervalAnalysisState newState = state;
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      assert "default return".equals(cfaEdge.getDescription())
          || "skipped unnecessary edges".equals(cfaEdge.getDescription());

      // delete variables from returning function,
      // we do not need them after this location, because the next edge is the functionReturnEdge.
      newState = newState.dropFrame(functionName);
    }

    return soleSuccessor(newState);
  }

  /**
   * Handles return from one function to another function.
   *
   * @param cfaEdge return edge from a function to its call site.
   * @return new abstract state.
   */
  @Override
  protected Collection<IntervalAnalysisState> handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge, CFunctionCall summaryExpr, String callerFunctionName)
      throws UnrecognizedCodeException {

    IntervalAnalysisState newState = state;
    Optional<CVariableDeclaration> retVar = cfaEdge.getFunctionEntry().getReturnVariable();
    if (retVar.isPresent()) {
      newState = newState.removeInterval(retVar.orElseThrow().getQualifiedName());
    }

    // expression is an assignment operation, e.g. a = g(b);
    if (summaryExpr instanceof CFunctionCallAssignmentStatement funcExp) {
      // left hand side of the expression has to be a variable
      if (state.contains(retVar.orElseThrow().getQualifiedName())) {
        newState =
            addInterval(
                newState,
                funcExp.getLeftHandSide(),
                state.getInterval(retVar.orElseThrow().getQualifiedName()));
      }

    } else if (summaryExpr instanceof CFunctionCallStatement) {
      // nothing to do
    } else {
      throw new UnrecognizedCodeException("on function return", cfaEdge, summaryExpr);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles function calls.
   *
   * @param callEdge the respective CFA edge
   * @return the successor state
   */
  @Override
  protected Collection<IntervalAnalysisState> handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {

    if (callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert parameters.size() <= arguments.size();
      logger.log(
          Level.WARNING,
          "Ignoring parameters passed as varargs to function",
          callEdge.getSuccessor().getFunctionDefinition().toASTString());
    } else {
      assert parameters.size() == arguments.size();
    }

    IntervalAnalysisState newState = state;

    // set the interval of each formal parameter to the interval of its respective actual parameter
    for (int i = 0; i < parameters.size(); i++) {
      // get value of actual parameter in caller function context
      Interval interval = evaluateInterval(state, arguments.get(i), callEdge);
      String formalParameterName = parameters.get(i).getQualifiedName();
      newState = newState.addInterval(formalParameterName, interval, threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles the statement edge which leads the function to the last node of its CFA
   * (not same as a return edge).
   *
   * @param returnEdge the CFA edge corresponding to this statement
   * @return the successor states
   */
  @Override
  protected Collection<IntervalAnalysisState> handleReturnStatementEdge(
      CReturnStatementEdge returnEdge) throws UnrecognizedCodeException {
    IntervalAnalysisState newState = state.dropFrame(functionName);

    // assign the value of the function return to a new variable
    if (returnEdge.asAssignment().isPresent()) {
      CAssignment ass = returnEdge.asAssignment().orElseThrow();
      newState =
          newState.addInterval(
              ((CIdExpression) ass.getLeftHandSide()).getDeclaration().getQualifiedName(),
              evaluateInterval(state, ass.getRightHandSide(), returnEdge),
              threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles assumptions.
   *
   * @param expression the expression containing the assumption
   * @param cfaEdge the CFA edge corresponding to this expression
   * @param truthValue flag to determine whether this is the then- or the else-branch of the
   *     assumption
   * @return the successor states
   */
  @Override
  protected Collection<IntervalAnalysisState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthValue)
      throws UnrecognizedCodeException {

    Interval currentEvaluation = evaluateInterval(state, expression, cfaEdge);
    Interval truthValueInterval = truthValue ? ONE : ZERO;

    // test whether the assumption is unsatisfiable
    if (!currentEvaluation.intersects(truthValueInterval)) {
      return noSuccessors();
    }

    if (expression instanceof CBinaryExpression binaryExpression) {
      BinaryOperator operator = binaryExpression.getOperator();
      if (!truthValue) {
        operator = operator.getOppositLogicalOperator();
      }
      CExpression operand1 = binaryExpression.getOperand1();
      CExpression operand2 = binaryExpression.getOperand2();
      return handleBinaryComparisonAssumption(cfaEdge, operand1, operator, operand2);
    } else {
      // Cannot handle assumptions that are not binary comparisons yet
      return soleSuccessor(state);
    }
  }

  private Collection<IntervalAnalysisState> handleBinaryComparisonAssumption(
      CAssumeEdge cfaEdge, CExpression operand1, BinaryOperator operator, CExpression operand2)
      throws UnrecognizedCodeException {

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, cfaEdge);

    return Stream.concat(
        oneSidedAssume(
            cfaEdge,
            operand1,
            operator,
            operand2.accept(visitor)
        ).stream(),
        oneSidedAssume(
            cfaEdge,
            operand2,
            operator.getSwitchOperandsSidesLogicalOperator(),
            operand1.accept(visitor)
        ).stream()
    ).collect(Collectors.toUnmodifiableSet());
  }


  private Collection<IntervalAnalysisState> oneSidedAssume(
      CAssumeEdge cfaEdge, CExpression dynamicOperand, BinaryOperator operator, Interval staticComparee)
      throws UnrecognizedCodeException {

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, cfaEdge);
    Interval dynamicOperandValue = dynamicOperand.accept(visitor);

    return switch (operator) {
      case LESS_THAN -> ImmutableSet.of(assign(
          dynamicOperand,
          dynamicOperandValue.limitUpperBoundBy(staticComparee.minus(1L)),
          cfaEdge
      ));
      case LESS_EQUAL -> ImmutableSet.of(assign(
          dynamicOperand,
          dynamicOperandValue.limitUpperBoundBy(staticComparee),
          cfaEdge
      ));
      case GREATER_THAN -> ImmutableSet.of(assign(
          dynamicOperand,
          dynamicOperandValue.limitLowerBoundBy(staticComparee.plus(1L)),
          cfaEdge
      ));
      case GREATER_EQUAL -> ImmutableSet.of(assign(
          dynamicOperand,
          dynamicOperandValue.limitLowerBoundBy(staticComparee),
          cfaEdge
      ));
      case EQUALS -> ImmutableSet.of(assign(
          dynamicOperand,
          dynamicOperandValue.intersect(staticComparee),
          cfaEdge
      ));
      case NOT_EQUALS -> dynamicOperandValue.getRelativeComplement(staticComparee).stream()
          .map(comparandPart -> assign(dynamicOperand, comparandPart, cfaEdge))
          .collect(Collectors.toSet());
      default -> throw new UnrecognizedCodeException("Assume operator not implemented", cfaEdge);
    };
  }

  private IntervalAnalysisState addInterval(
      IntervalAnalysisState newState, CExpression lhs, Interval interval) {
    // we currently only handle IdExpressions and ignore more complex Expressions
    if (lhs instanceof CIdExpression id) {
      return newState.addInterval(id.getDeclaration().getQualifiedName(), interval, threshold);
    }
    return newState;
  }

  /**
   * This method handles variable declarations.
   *
   * <p>So far, only primitive types are supported, pointers are not supported either.
   *
   * @param declarationEdge the CFA edge
   * @return the successor state
   */
  @Override
  protected Collection<IntervalAnalysisState> handleDeclarationEdge(
      CDeclarationEdge declarationEdge, CDeclaration declaration) throws UnrecognizedCodeException {

    if (declarationEdge.getDeclaration() instanceof CVariableDeclaration decl) {
      return handleVariableDeclarationEdge(declarationEdge, decl);
    }

    return soleSuccessor(state);
  }

  private Collection<IntervalAnalysisState> handleVariableDeclarationEdge(CDeclarationEdge declarationEdge, CVariableDeclaration decl)
      throws UnrecognizedCodeException {

    if (decl.getType() instanceof CSimpleType) {
      return handleSimpleTypeVariableDeclarationEdge(declarationEdge, decl);
    }
    if (decl.getType() instanceof CArrayType) {
      return handleArrayVariableDeclarationEdge(declarationEdge, decl);
    }
    return soleSuccessor(state);
  }

  private Collection<IntervalAnalysisState> handleSimpleTypeVariableDeclarationEdge(CDeclarationEdge declarationEdge, CVariableDeclaration decl)
      throws UnrecognizedCodeException {
    Interval interval;

    // variable may be initialized explicitly on the spot ...
    if (decl.getInitializer() instanceof CInitializerExpression initializerExpression) {
      CExpression expression = initializerExpression.getExpression();
      interval = evaluateInterval(state, expression, declarationEdge);
    } else {
      interval = Interval.UNBOUND;
    }

    return soleSuccessor(state.addInterval(decl.getQualifiedName(), interval, threshold));
  }

  private Collection<IntervalAnalysisState> handleArrayVariableDeclarationEdge(CDeclarationEdge declarationEdge, CVariableDeclaration decl)
      throws UnrecognizedCodeException {
    if (decl.getInitializer() instanceof CInitializerList initializerList) {
      ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, declarationEdge);
      return soleSuccessor(state.addArray(decl.getQualifiedName(), FunArray.ofInitializerList(initializerList.getInitializers(), visitor)));
    } else if (decl.getType() instanceof CArrayType arrayType) {
      FunArray simpleArray = new FunArray(arrayType.getLength());
      return soleSuccessor(state.addArray(decl.getQualifiedName(), simpleArray));
    }
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * This method handles unary and binary statements.
   *
   * @param expression the current expression
   * @param cfaEdge the CFA edge
   * @return the successor
   */
  @Override
  protected Collection<IntervalAnalysisState> handleStatementEdge(
      CStatementEdge cfaEdge, CStatement expression) throws UnrecognizedCodeException {
    if (expression instanceof CAssignment assignExpression) {
      CExpression assignee = assignExpression.getLeftHandSide();
      Interval value = assignExpression.getRightHandSide().accept(new ExpressionValueVisitor(state, cfaEdge));
      return ImmutableSet.of(assign(assignee, value, cfaEdge));
    }
    return ImmutableSet.of();
  }

  private IntervalAnalysisState assign(CExpression assignee, Interval value, CFAEdge cfaEdge) {
    if (assignee instanceof CIdExpression id) {
      return state.addInterval(id.getDeclaration().getQualifiedName(), value, threshold);
    } else if (assignee instanceof CArraySubscriptExpression arraySubscript) {
      if (arraySubscript.getArrayExpression() instanceof CIdExpression arrayId) {
        String arrayName = arrayId.getDeclaration().getQualifiedName();
        CExpression index = arraySubscript.getSubscriptExpression();
        ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, cfaEdge);
        return state.assignArrayElement(arrayName, index, value, visitor);
      }
    }
    return state;
  }

  private Interval evaluateInterval(
      IntervalAnalysisState readableState, CRightHandSide expression, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {
    return expression.accept(new ExpressionValueVisitor(readableState, cfaEdge));
  }

  private Collection<IntervalAnalysisState> soleSuccessor(IntervalAnalysisState successor) {
    return Collections.singleton(successor);
  }

  private Collection<IntervalAnalysisState> noSuccessors() {
    return ImmutableSet.of();
  }
}
