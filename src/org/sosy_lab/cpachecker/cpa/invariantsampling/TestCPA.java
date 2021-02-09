// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariantsampling;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

@Options(prefix = "invariantsampling")
public class TestCPA extends AbstractCPA {

  @Option(
      secure = true,
      description = "Determines which stop operator to use for the invariant sampling analysis."
  )
  private String stopType = "sep";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TestCPA.class);
  }

  protected TestCPA(Configuration pConfig) throws InvalidConfigurationException {
    super("sep", "sep", new InvariantSamplingTransferRelation());
    pConfig.inject(this);
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition
  ) {
    return new InvariantSamplingState();
  }

  private static class InvariantSamplingState implements AbstractState, Graphable {
    private Map<String, String> state;

    public InvariantSamplingState() {
      state = new HashMap<String, String>();
    }

    public InvariantSamplingState(
        InvariantSamplingState currentState,
        CVariableDeclaration declaration) {
      state = currentState.getState();
      state.put(declaration.getOrigName(), declaration.getInitializer().toString());
    }

    public InvariantSamplingState(
        InvariantSamplingState currentState, CExpressionAssignmentStatement statement
    ) {
      state = currentState.getState();
      // statement.getRightHandSide()
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof InvariantSamplingState)) {
        return false;
      }

      InvariantSamplingState state2 = (InvariantSamplingState) obj;

      return state.equals(state2.getState());
    }

    public Map<String, String> getState() {
      return state;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('\n');
      Iterator<Entry<String, String>> iter = state.entrySet().iterator();
      while (iter.hasNext()) {
        Entry<String, String> entry = iter.next();
        sb.append(entry.getKey());
        sb.append('=').append('"');
        sb.append(entry.getValue());
        sb.append('"');
        if (iter.hasNext()) {
          sb.append('\n');
        }
      }
      return sb.toString();
    }

    @Override
    public String toDOTLabel() {
      return toString();
    }

    @Override
    public boolean shouldBeHighlighted() {
      return true;
    }
  }

  private static class InvariantSamplingTransferRelation
      extends ForwardingTransferRelation<InvariantSamplingState, InvariantSamplingState, Precision>
      implements TransferRelation {

    @Override
    protected InvariantSamplingState handleDeclarationEdge(
        CDeclarationEdge edge, CDeclaration declaration
    ) {
      if (declaration instanceof CVariableDeclaration) {
        return new InvariantSamplingState(this.getState(), (CVariableDeclaration) declaration);
      }
      return this.getState();
    }

    @Override
    protected InvariantSamplingState handleAssumption(
        CAssumeEdge edge, CExpression expression, boolean truthAssumption
    ) {
      return this.getState();
    }

    @Override
    protected InvariantSamplingState handleStatementEdge(
        CStatementEdge edge, CStatement statement
    ) {
      if (statement instanceof CExpressionAssignmentStatement) {
        return new InvariantSamplingState(
            this.getState(), (CExpressionAssignmentStatement) statement
        );
      }
      return this.getState();
    }

  }
}
