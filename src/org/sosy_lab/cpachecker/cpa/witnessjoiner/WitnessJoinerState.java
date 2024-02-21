// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.witnessjoiner;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSerializableSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class WitnessJoinerState extends AbstractSerializableSingleWrapperState
    implements ExpressionTreeReportingState, Graphable {

  private static final long serialVersionUID = 1125821322111655263L;

  protected WitnessJoinerState(@Nullable AbstractState pWrappedState) {
    super(pWrappedState);
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximation(
      final FunctionEntryNode pFunctionScope, final CFANode pLocation) throws InterruptedException {
    ExpressionTreeFactory<AExpression> factory = ExpressionTrees.newFactory();
    List<ExpressionTree<AExpression>> result = new ArrayList<>();

    AbstractState wrapped = getWrappedState();
    if (wrapped != null) {
      for (AutomatonState as :
          AbstractStates.asIterable(wrapped)
              .filter(AutomatonState.class)
              .filter(state -> !state.hasDefaultCandidateInvariants())) {
        result.add(as.getCandidateInvariants());
      }
    }
    return ExpressionTrees.cast(factory.or(result));
  }

  @Override
  public String toString() {
    AbstractState wrapped = getWrappedState();
    return wrapped != null ? wrapped.toString() : "";
  }

  @Override
  public String toDOTLabel() {
    AbstractState wrapped = getWrappedState();
    return wrapped instanceof Graphable ? ((Graphable) wrapped).toDOTLabel() : toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
