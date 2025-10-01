// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public abstract sealed class FunctionCallExpression extends SymbolicExpression
    permits PopcountFunctionExpression {

  @Serial private static final long serialVersionUID = -3517582895332012546L;

  private final List<SymbolicExpression> arguments;

  // Original expression, includes the type(s) etc.
  private final AFunctionCallExpression functionCallExpr;

  FunctionCallExpression(
      final List<SymbolicExpression> pArguments, final AFunctionCallExpression pFunctionCallExpr) {
    arguments = checkNotNull(pArguments);
    functionCallExpr = checkNotNull(pFunctionCallExpr);
    checkArgument(pArguments.size() == pFunctionCallExpr.getParameterExpressions().size());
  }

  FunctionCallExpression(
      final List<SymbolicExpression> pArguments,
      final AFunctionCallExpression pFunctionCallExpr,
      final MemoryLocation pRepresentedLocation) {
    super(pRepresentedLocation);
    arguments = checkNotNull(pArguments);
    functionCallExpr = checkNotNull(pFunctionCallExpr);
    checkArgument(pArguments.size() == pFunctionCallExpr.getParameterExpressions().size());
  }

  FunctionCallExpression(
      final List<SymbolicExpression> pArguments,
      final AFunctionCallExpression pFunctionCallExpr,
      final AbstractState pAbstractState) {
    super(pAbstractState);
    arguments = checkNotNull(pArguments);
    functionCallExpr = checkNotNull(pFunctionCallExpr);
    checkArgument(pArguments.size() == pFunctionCallExpr.getParameterExpressions().size());
  }

  protected final AFunctionCallExpression getFunctionCallExpression() {
    return functionCallExpr;
  }

  @Override
  public Type getType() {
    return functionCallExpr.getExpressionType();
  }

  public List<SymbolicExpression> getArguments() {
    return arguments;
  }

  public List<Type> getArgumentTypes() {
    return functionCallExpr.getParameterExpressions().stream()
        .map(a -> a.getExpressionType())
        .collect(ImmutableList.toImmutableList());
  }

  public AExpression getFunctionNameExpression() {
    return functionCallExpr.getFunctionNameExpression();
  }

  public AFunctionDeclaration getDeclaration() {
    return functionCallExpr.getDeclaration();
  }

  @Override
  public boolean isTrivial() {
    return arguments.isEmpty() || arguments.stream().allMatch(s -> s.isTrivial());
  }

  @Override
  @SuppressWarnings("EqualsGetClass") // on purpose, case-class structure with single equals()
  public final boolean equals(Object o) {
    // Comment to silence CI
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof FunctionCallExpression otherFunCallExpr)) {
      return false;
    }

    if (arguments.size() != otherFunCallExpr.arguments.size()
        || !getArgumentTypes().equals(otherFunCallExpr.getArgumentTypes())
        || !getType().equals(otherFunCallExpr.getType())
        || !super.equals(otherFunCallExpr)
        || !getFunctionNameExpression().equals(otherFunCallExpr.getFunctionNameExpression())) {
      return false;
    }

    if (hasAbstractState()
        && otherFunCallExpr.hasAbstractState()
        && getAbstractState() instanceof SMGState thisState
        && otherFunCallExpr.getAbstractState() instanceof SMGState thatState) {
      // SMG values do not really care about the type, as the SMG knows their types and checks
      // that as well
      for (int i = 0; i < arguments.size(); i++) {
        if (!SMGState.areValuesEqual(
            thisState, arguments.get(i), thatState, otherFunCallExpr.arguments.get(i))) {
          return false;
        }
      }
      return true;
    }

    return arguments.equals(otherFunCallExpr.arguments);
  }

  @Override
  public final int hashCode() {
    return super.hashCode()
        + Objects.hash(
            getClass().getCanonicalName(),
            arguments,
            getArgumentTypes(),
            getType(),
            getFunctionNameExpression());
  }

  @Override
  public String getRepresentation() {
    if (getRepresentedLocation().isPresent()) {
      return getRepresentedLocation().orElseThrow().toString();

    } else {
      return getOperationString()
          + "("
          + Joiner.on(", ").join(arguments.stream().map(v -> v.getRepresentation()).iterator())
          + ")";
    }
  }

  @Override
  public String toString() {
    // TODO: use getFunctionNameExpression() instead?
    return getOperationString() + "(" + Joiner.on(", ").join(arguments) + ")";
  }

  public abstract String getOperationString();
}
