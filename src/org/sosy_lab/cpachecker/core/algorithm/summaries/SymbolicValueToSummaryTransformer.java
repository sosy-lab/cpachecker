// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.summaries;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
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
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link SymbolicValueToSummaryTransformer} represents a transformation of a {@link
 * SymbolicExpression} to a summary.<br>
 * Currently, summaries are represented as plain {@link String}, and the return value of this
 * transformation is the {@link StringBuilder} used to construct the summary. The transformation
 * recursively visits all subexpressions of the {@link SymbolicExpression}, and adds them to the
 * summary.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class SymbolicValueToSummaryTransformer implements SymbolicValueVisitor<StringBuilder> {

  /**
   * The {@link StringBuilder} with which the summary is created.<br/>
   * After the recursive visiting of all subexpressions of a {@link SymbolicExpression}, it contains
   * the generated summary.
   */
  private final StringBuilder strBuilder = new StringBuilder();

  /**
   * Flag which indicates whether outer parenthesis have been added.<br>
   * For consistent notation, it is required to wrap the whole summary expression (which occurs to
   * the right of the function signature in the summary string) in parenthesis. If the summary only
   * contains a single variable or constant, no parenthesis are required.
   *
   * <p>During <i>only the first</i> invocation of any <code>visit</code> method of a transformer
   * for a {@link SymbolicExpression}, it must therefore use {@link
   * #unwrapWithParenthesis(SymbolicExpression)} on the expression itself, instead of directly
   * unpacking its operands. {@link #unwrapWithParenthesis(SymbolicExpression)} also takes care of
   * not putting parenthesis around single variables or constants.
   *
   * <p>This internal flag indicates whether that operation (i. e. creation of the outermost
   * parenthesis) has already been performed, or is still pending.
   */
  private boolean outerParenthesisAdded = false;

  /**
   * {@link MemoryLocation} and {@link ValueAnalysisState} of function parameters.<br>
   * To identify the original parameters in the symbolic expression for the return value of the
   * function, the {@link ValueAndType} of each parameter is stored for the state directly after
   * entering the function. Then, if these expressions are found again while unpacking the symbolic
   * expression of the return value, the parameter identifier is inserted directly (instead of
   * further unpacking the represented operations; which would include operations performed
   * <i>before</i> entering the function).
   *
   * <p>Currently, it is not possible to directly identify function parameters which have been set
   * to constant values during the function call. For example. if a function <code>f(x, y)</code> is
   * called as <br>
   * <code>    f(x, 5)</code>,<br>
   * the symbolic expression representing the argument <code>y=5</code> will not be recognized as an
   * parameter.
   *
   * <p>The reason is that in the list of parameters available from the {@link ValueAnalysisState}
   * at the function entry location, the parameter is not represented as {@link SymbolicExpression}
   * which then contains the actual number; but instead, as a plain {@link NumericValue}. However,
   * from comparing two numeric values, the parameter can not be identified reliably in the symbolic
   * expressions which represents the operations of the function (e. g. because the same numeric
   * value could be used for multiple parameters).
   *
   * <p>This is different from more complex arguments, e. g. <code>f(x, y + 5)</code>. Here, the
   * list of parameters at the function entry location contains a {@link SymbolicExpression} (which
   * represents the addition, and possibly casting operations), which is then uniquely identified
   * within the analyzed {@link SymbolicExpression} of the function return value.
   *
   * <p>As a result, plain numerical arguments are just removed from the list of parameters, and
   * replaced with their corresponding value in the summary. <code>f(x, 5)</code> therefore leads to
   * a summary with signature <code>f(x)</code>, and all occurrences of <code>x</code> within the
   * function body will be replaced by the number <code>5</code>.
   *
   * <p>During initialization, the list of parameters is assigned based on the {@link
   * ValueAnalysisState} of the function entry location.
   */
  private ImmutableMap<MemoryLocation, ValueAndType> parameters;

  private SymbolicValueToSummaryTransformer() {}

  /**
   * Create a new {@link SymbolicValueToSummaryTransformer} for a summary.<br>
   * Different to the default constructor, the generated summary is started by appending the
   * signature of the function represented in the provided parameters.
   *
   * @param function Declaration of the function for which the summary is created.
   * @param entryState The {@link ValueAnalysisState} of the abstract state at the position where
   *     the summarized function is entered.
   */
  public SymbolicValueToSummaryTransformer(
      final AFunctionDeclaration function, final ValueAnalysisState entryState) {
    final String scope = function.getQualifiedName();

    parameters =
        entryState.getConstants().stream()
            .filter(constant -> constant.getKey().isOnFunctionStack(scope))
            .filter(constant -> !constant.getValue().getValue().isNumericValue())
            .collect(
                ImmutableMap.toImmutableMap(entry -> entry.getKey(), entry -> entry.getValue()));

    addFunctionSignature(function);
  }

  @Override
  public StringBuilder visit(SymbolicIdentifier pValue) {
    final MemoryLocation variable = pValue.getRepresentedLocation().orElseThrow();
    return strBuilder.append(variable.getIdentifier());
  }

  @Override
  public StringBuilder visit(ConstantSymbolicExpression pExpression) {
    final Optional<MemoryLocation> variable = pExpression.getRepresentedLocation();

    if (variable.isPresent()) {
      String identifier = variable.get().getIdentifier();
      strBuilder.append(identifier);
    } else {
      final Value value = pExpression.getValue();
      strBuilder.append(value.asNumericValue().getNumber());
    }

    return strBuilder;
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
    /*
     * Summaries currently don't convey no type information.
     * Cast operations are therefore ignored, and the operand expression is unpacked directly.
     */
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
   *
   * @param operator {@link String} representation of the operation represented by the symbolic
   *                         expression. Added with <i>prefix notation</i> in the summary.
   *
   * @param expr The unary symbolic expression which is added to the summary.
   */
  private StringBuilder unwrap(final String operator, final UnarySymbolicExpression expr) {
    final SymbolicExpression operand = expr.getOperand();
    strBuilder.append(" ").append(operator);
    return unwrapWithParenthesis(operand);
  }

  /**
   * Identical to {@link #unwrap(String, UnarySymbolicExpression)}, despite operating on a
   * {@link BinarySymbolicExpression}.<br/>
   * This method therefore has to recursively unwrap <i>two operands</i> instead of one, and adds
   * the operator in between with <i>infix notation</i>.
   *
   * @see #unwrap(String, UnarySymbolicExpression)
   *
   * @param op {@link String} representation of the operation represented by the symbolic
   *                         expression. Added with <i>infix notation</i> in the summary.
   *
   * @param expr The binary symbolic expression which is added to the summary.
   */
  private StringBuilder unwrap(final String op, final BinarySymbolicExpression expr) {
    if (!outerParenthesisAdded) {
      outerParenthesisAdded = true;
      return unwrapWithParenthesis(expr);
    }

    unwrapWithParenthesis(expr.getOperand1());
    strBuilder.append(" ").append(op).append(" ");
    return unwrapWithParenthesis(expr.getOperand2());
  }

  /**
   * Recursively continue visiting subexpressions, and wrap the current one with parenthesis.<br/>
   * Parenthesis are omitted if the subexpression contains only a single variable or constant.
   *
   * @param expr The {@link SymbolicExpression} which is visited next, and whose value is wrapped in
   *             parenthesis.
   *
   * @return The {@link StringBuilder} of the created summary.
   */
  private StringBuilder unwrapWithParenthesis(final SymbolicExpression expr) {
    if (isParameter(expr)) {
      return strBuilder.append(expr.getRepresentedLocation().get().getIdentifier());
    }

    final boolean constant = expr instanceof ConstantSymbolicExpression;
    final boolean cast = expr instanceof CastExpression;

    if (constant || cast) {
      expr.accept(this);
    } else {
      strBuilder.append("(");
      expr.accept(this);
      strBuilder.append(")");
    }

    return strBuilder;
  }

  /**
   * Append function signature to summary.<br>
   * Internal utility method which derives the function signature of the current scope from the
   * parameters already stored, and the provided {@link AFunctionDeclaration}, and appends it to the
   * generated summary (which should yet be empty when this method is called).
   *
   * <p>This method is used internally to begin the created summary with the function signature.
   *
   * @param function The {@link AFunctionDeclaration} of the function for which the summary is
   *     created.
   */
  private void addFunctionSignature(AFunctionDeclaration function) {
    final String name = function.getName();

    strBuilder.append(name);
    strBuilder.append(": (");

    int index = 0;
    for (final Map.Entry<MemoryLocation, ValueAndType> entry : this.parameters.entrySet()) {
      strBuilder.append(entry.getKey().getIdentifier());

      if (++index < this.parameters.size()) {
        strBuilder.append(",");
      }
    }

    if (this.parameters.isEmpty()) {
      strBuilder.append(" ");
    }

    strBuilder.append(") -> ");
  }

  /**
   * Check whether expression is function parameter.<br>
   * Internal utility method which checks whether the provided expression is identical to one of the
   * parameters with which the function was called. In this case, it can be replaced with the
   * parameter identifier in the created summary.
   *
   * <p>Some limitations apply, see description on {@link #parameters}.
   *
   * @param expr The expression which is checked for being a function parameter.
   * @return true if the expression is a function parameter, false otherwise (with limitations).
   */
  private boolean isParameter(final SymbolicExpression expr) {
    final Optional<MemoryLocation> location = expr.getRepresentedLocation();

    if (location.isEmpty()) {
      return false;
    }

    final ValueAndType param = this.parameters.get(location.get());
    if (param != null && param.getValue().equals(expr)) {
      return true;
    }

    return false;
  }
}
