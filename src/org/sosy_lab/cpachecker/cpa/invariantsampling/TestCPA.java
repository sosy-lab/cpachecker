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
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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
//    private Map<String, String> state;

    public InvariantSamplingState() {
//      this.state = new HashMap<String, String>();
    }

    public InvariantSamplingState(InvariantSamplingState currentState, CDeclaration declaration) {
//      this.state = currentState.state; // new HashMap<String, String>();
//      this.state = currentState.getState();
//      this.state.put(declaration.getName(), declaration.getInitializer().toString());
//      this.state.put(declaration.getName(), "test");
    }

    public InvariantSamplingState(InvariantSamplingState currentState, CStatementEdge statementEdge) {

    }

//    @Override
//    public int hashCode() {
//      return 1;
//    }

//    public Map<String, String> getState() {
//      return state;
//    }
//
//    public String toString() {
//      StringBuilder sb = new StringBuilder();
//      Iterator<Entry<String, String>> iter = state.entrySet().iterator();
//      while (iter.hasNext()) {
//        Entry<String, String> entry = iter.next();
//        sb.append(entry.getKey());
//        sb.append('=').append('"');
//        sb.append(entry.getValue());
//        sb.append('"');
//        if (iter.hasNext()) {
//          sb.append(',').append(' ');
//        }
//      }
//      return sb.toString();
//    }

    @Override
    public String toDOTLabel() {
      return "hello";
//      return toString();
    }

    @Override
    public boolean shouldBeHighlighted() {
      return true;
    }
  }

  private static class InvariantSamplingTransferRelation
      extends ForwardingTransferRelation<InvariantSamplingState, InvariantSamplingState, Precision>
      implements TransferRelation
  {

    @Override
    protected InvariantSamplingState handleDeclarationEdge(
        CDeclarationEdge edge, CDeclaration declaration
    ) {
      return new InvariantSamplingState();
//      if (declaration instanceof CVariableDeclaration) {
//        return new InvariantSamplingState(this.getState(), (CVariableDeclaration) declaration);
//      }
//      return this.getState();
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
      return this.getState();
    }

  }
}
