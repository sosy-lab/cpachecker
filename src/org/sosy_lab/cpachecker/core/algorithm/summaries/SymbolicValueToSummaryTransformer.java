// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.summaries;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SubtractionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * <code>SymbolicValueToSummaryTransformer</code> represents a transformation of a
 * <code>SymbolicValue</code> to a summary.<br/>
 * Currently, summaries are represented as plain <code>String</code>, and the returned entity of
 * this transformer is the <code>StringBuilder</code> used to construct the summary.<p>
 * The transformation recursively visits all subexpressions of the <code>SymbolicValue</code>, and
 * adds them to the summary.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class SymbolicValueToSummaryTransformer implements SymbolicValueVisitor<StringBuilder> {

  /**
   * The <code>StringBuilder</code> with which the summary is created.<br/>
   * After the recursive visiting of all subexpressions of a <code>SymbolicValue</code> is finished,
   * it contains the generated summary.
   *
   */
  private final StringBuilder strBuilder = new StringBuilder();

  /**
   * For consistent notation, it is required to wrap the whole summary expression (which occurs to
   * the right of the function signature in the summary string) in parenthesis. If the summary
   * only contains a single variable or constant, no parenthesis are required.<p>
   *
   * During <i>only</i> the first invocation of any <code>visit</code> method of a transformer with
   * a subexpression, it must therefore first use {@link #unwrapWithParenthesis(SymbolicExpression)}
   * on the expression itself, instead of directly unpacking its operands.
   * {@link #unwrapWithParenthesis(SymbolicExpression)} also takes care of not putting parenthesis
   * around single variables or constants.<p>
   *
   * This internal flag indicates whether this behavior during <i>only</i> the first invocation of
   * any visit method of a <code>SymbolicValueToSummaryTransformer</code> has already been
   * performed.
   */
  private boolean outerParenthesisAdded = false;

  /**
   * Create a new <code>SymbolicValueToSummaryTransformer</code> <i>without</i> function
   * signature.<br/>
   * Different to {@link #SymbolicValueToSummaryTransformer(LocationState, ValueAnalysisState)}, the
   * generated summary is <i>not</i> started by prepending the function signature.
   */
  public SymbolicValueToSummaryTransformer() {
  }

  /**
   * Create a new <code>SymbolicValueToSummaryTransformer</code> with function signature.<br/>
   * Different to the default constructor, the generated summary is started by appending the
   * signature of the function represented in the provided parameters.
   *
   * @param locationState The LocationState of the abstract valueState for which the function scope
   *                      is analyzed.
   *
   * @param valueState The <code>ValueAnalysisState</code> of the abstract state for which the
   *                   function scope is analyzed.
   */
  public SymbolicValueToSummaryTransformer(
      final LocationState locationState, final ValueAnalysisState valueState) {
    addFunctionSignature(locationState, valueState);
  }

  @Override
  public StringBuilder visit(SymbolicIdentifier pValue) {
    final MemoryLocation variable = pValue.getRepresentedLocation().orElseThrow();
    return strBuilder.append(variable.getIdentifier());
  }

  @Override
  public StringBuilder visit(ConstantSymbolicExpression pExpression) {
    final var value = pExpression.getValue();

    if (value instanceof SymbolicIdentifier) {
      final SymbolicIdentifier identifier = (SymbolicIdentifier) value;
      return identifier.accept(this);
    } else {
      final Number number = value.asNumericValue().getNumber();
      return strBuilder.append(number);
    }
  }

  @Override
  public StringBuilder visit(AdditionExpression pExpression) {
    return unwrap("+", pExpression);
  }

  @Override
  public StringBuilder visit(SubtractionExpression pExpression) {
    return unwrap("-", pExpression);
  }

  @Override
  public StringBuilder visit(MultiplicationExpression pExpression) {
    return unwrap("*", pExpression);
  }

  @Override
  public StringBuilder visit(DivisionExpression pExpression) {
    return unwrap("/", pExpression);
  }

  @Override
  public StringBuilder visit(ModuloExpression pExpression) {
    return unwrap("%", pExpression);
  }

  @Override
  public StringBuilder visit(BinaryAndExpression pExpression) {
    return unwrap("&", pExpression);
  }

  @Override
  public StringBuilder visit(BinaryNotExpression pExpression) {
    return unwrap("~", pExpression);
  }

  @Override
  public StringBuilder visit(BinaryOrExpression pExpression) {
    return unwrap("|", pExpression);
  }

  @Override
  public StringBuilder visit(BinaryXorExpression pExpression) {
    return unwrap("^", pExpression);
  }

  @Override
  public StringBuilder visit(ShiftRightExpression pExpression) {
    return unwrap(">>", pExpression);
  }

  @Override
  public StringBuilder visit(ShiftLeftExpression pExpression) {
    return unwrap("<<", pExpression);
  }

  @Override
  public StringBuilder visit(LogicalNotExpression pExpression) {
    return unwrap("!", pExpression);
  }

  @Override
  public StringBuilder visit(LessThanOrEqualExpression pExpression) {
    return unwrap("<=", pExpression);
  }

  @Override
  public StringBuilder visit(LessThanExpression pExpression) {
    return unwrap("<", pExpression);
  }

  @Override
  public StringBuilder visit(EqualsExpression pExpression) {
    return unwrap("==", pExpression);
  }

  @Override
  public StringBuilder visit(LogicalOrExpression pExpression) {
    return unwrap("||", pExpression);
  }

  @Override
  public StringBuilder visit(LogicalAndExpression pExpression) {
    return unwrap("&&", pExpression);
  }

  @Override
  public StringBuilder visit(CastExpression pExpression) {
    return pExpression.getOperand().accept(this);
  }

  @Override
  public StringBuilder visit(PointerExpression pExpression) {
    return unwrap("*", pExpression);
  }

  @Override
  public StringBuilder visit(AddressOfExpression pExpression) {
    return unwrap("&", pExpression);
  }

  @Override
  public StringBuilder visit(NegationExpression pExpression) {
    return unwrap("!", pExpression);
  }

  /**
   * Add expression to summary, and recursively continue visiting its subexpressions.<br/>
   * @param op String representation of the operation represented by the symbolic expression.
   *           Added as prefix operator of the expression in the summary.
   * @param expr The unary symbolic expression which is added to the summary.
   */
  private StringBuilder unwrap(final String op, final UnarySymbolicExpression expr) {
    final SymbolicExpression operand = expr.getOperand();
    strBuilder.append(" ").append(op);
    return unwrapWithParenthesis(operand);
  }

  /**
   * Identical to {@link #unwrap(String, UnarySymbolicExpression)}, despite operating on a
   * <code>BinarySymbolicExpression</code>.<br/>
   * This method therefore has to recursively unwrap <i>two operands</i> instead of one, and adds
   * the operator in between with <i>infix notation</i>.
   *
   * @see #unwrap(String, UnarySymbolicExpression)
   *
   * @param op String representation of the operation represented by the symbolic expression.
   *           Added as infix operator of the expression in the summary.
   * @param expr The binary symbolic expression which is added to the summary.
   */
  private StringBuilder unwrap(final String op, final BinarySymbolicExpression expr) {
    if (!outerParenthesisAdded) {
      outerParenthesisAdded = true;
      return unwrapWithParenthesis(expr);
    }

    final SymbolicExpression lhs = expr.getOperand1();
    final SymbolicExpression rhs = expr.getOperand2();

    unwrapWithParenthesis(lhs);
    strBuilder.append(" ").append(op).append(" ");
    return unwrapWithParenthesis(rhs);
  }

  /**
   * Recursively continue visiting subexpressions, and wrap the current one with parenthesis.<br/>
   * Parenthesis are omitted if the subexpression contains just a single variable or constant.
   *
   * @param expr The SymbolicExpression which is visited next, and whose value is wrapped in
   *             parenthesis.
   *
   * @return The <code>StringBuilder</code> of the created summary.
   */
  private StringBuilder unwrapWithParenthesis(final SymbolicExpression expr) {
    final boolean constant = expr instanceof ConstantSymbolicExpression;

    if (!constant) {
      strBuilder.append("(");
      expr.accept(this);
      strBuilder.append(")");
    } else {
      expr.accept(this);
    }

    return strBuilder;
  }

  /**
   * Append function signature to summary.<br/>
   * Internal utility method which derives the function signature of the current scope for the
   * provided locationState, and appends it to the generated summary (which should yet be empty when
   * this method is called).<p>
   * If <code>SymbolicValueToSummaryTransformer</code> is created with the non-trivial constructor,
   * this method is used internally to start the created summary with the function signature.
   *
   * @param locationState The LocationState of the abstract valueState for which the function scope is
   *                 analyzed.
   *
   * @param valueState The <code>ValueAnalysisState</code> of the abstract state for which the
   *                   function scope is analyzed.
   */
  private void addFunctionSignature(LocationState locationState, ValueAnalysisState valueState) {
    final AFunctionDeclaration function = locationState.getLocationNode().getFunction();
    final String name = function.getName();

    strBuilder.append(name);
    strBuilder.append(": (");

    ImmutableList<AParameterDeclaration> params =
        determineActualParameters(function, valueState.getTrackedMemoryLocations());

    int index = 0;
    for (final AParameterDeclaration param : params) {
      strBuilder.append(param.getName());

      if (++index < params.size()) {
        strBuilder.append(",");
      }
    }

    if (params.isEmpty()) {
      strBuilder.append(" ");
    }

    strBuilder.append(") -> ");
  }

  /**
   * Return all function parameters which remain as symbolic variables in the summary.<br/>
   * During Symbolic Execution, formal parameters which are filled with constant
   * arguments in the analyzed program will be represented as
   * {@link ConstantSymbolicExpression} which contains the corresponding constant value. As a
   * result, the information where they actually occurred as variables within the function body is
   * not readily available. In the produced summary, only the actual argument (i.e. the constant
   * value present during function invocation) is used and makes it into the summary.<p>
   *
   * This can result in summaries like<br/>
   * <code>f: (x, y) -> x + 5</code><br/>
   * if <code>f</code> is called as <code>f(x, 5)</code> in the analyzed code.<p>
   *
   * To prevent the occurrence of such parameters in the function signature within summaries, this
   * internal utility method filters the list of parameters and returns an updated one where only
   * "real" parameters which make it into the generated summary as symbolic variables remain.<p>
   *
   * @param function The function for which the parameters are analyzed. Used to determine the list
   *                 of formal parameters.
   *
   * @param memoryLocations The list of memory locations at the analyzed location in the function.
   *                        Used to determine which parameters actually become tracked memory
   *                        locations (and therefore symbolic variables in the summary).
   *
   * @return Immutable list of function parameters which actually occur as symbolic variables in the
   *          summary.
   */
  ImmutableList<AParameterDeclaration> determineActualParameters(
      final AFunctionDeclaration function, final Set<MemoryLocation> memoryLocations) {
    final List<? extends AParameterDeclaration> params = function.getParameters();

    ImmutableList<String> actualParameters = memoryLocations.stream()
        .map(memoryLocation -> memoryLocation.getIdentifier())
        .collect(ImmutableList.toImmutableList());

    return params.stream()
        .filter(parameter -> actualParameters.contains(parameter.getName()))
        .collect(ImmutableList.toImmutableList());
  }
}
