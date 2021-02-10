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
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
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

  private static class LeftHandSideInvariantSamplingVisitor implements
                                                            CLeftHandSideVisitor<String, Exception> {

    @Override
    public String visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws Exception {
      return null;
    }

    @Override
    public String visit(CFieldReference pIastFieldReference) throws Exception {
      return null;
    }

    @Override
    public String visit(CIdExpression expression) throws Exception {
      return expression.getName();
    }

    @Override
    public String visit(CPointerExpression pointerExpression) throws Exception {
      return null;
    }

    @Override
    public String visit(CComplexCastExpression complexCastExpression) throws Exception {
      return null;
    }
  }

  private static class RightHandSideInvariantSamplingVisitor implements CExpressionVisitor<Integer, Exception>,
                                                                        CSimpleDeclarationVisitor<Integer, Exception>,
                                                                        CInitializerVisitor<Integer, Exception> {

    @Override
    public Integer visit(CBinaryExpression expression) throws Exception {
      Integer value1 = expression.getOperand1().accept(this);
      BinaryOperator operator = expression.getOperator();
      Integer value2 = expression.getOperand2().accept(this);
      switch (operator.name()) {
        case "PLUS":
          return value1 + value2;
        case "Minus":
          return value1 - value2;
        case "MULTPLY":
          return value1 * value2;
        default:
          throw new Exception("The operator is unknown.");
      }
    }

    @Override
    public Integer visit(CCastExpression pIastCastExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CCharLiteralExpression pIastCharLiteralExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CIntegerLiteralExpression expression)
        throws Exception {
      Integer value = expression.getValue().intValue();
      return value;
    }

    @Override
    public Integer visit(CStringLiteralExpression pIastStringLiteralExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CTypeIdExpression pIastTypeIdExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CUnaryExpression pIastUnaryExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CImaginaryLiteralExpression PIastLiteralExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws Exception {
      return null;
    }

    @Override
    public Integer visit(CFieldReference pIastFieldReference) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CIdExpression expression) throws Exception {
      Integer value = expression.getDeclaration().accept(this);
      return value;
    }

    @Override
    public Integer visit(CPointerExpression pointerExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CComplexCastExpression complexCastExpression) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CFunctionDeclaration pDecl) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CComplexTypeDeclaration pDecl) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CTypeDefDeclaration pDecl) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CVariableDeclaration decl) throws Exception {
      Integer value = decl.getInitializer().accept(this);
      return value;
    }

    @Override
    public Integer visit(CParameterDeclaration pDecl) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CEnumerator pDecl) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CInitializerExpression expression) throws Exception {
      Integer value = expression.getExpression().accept(this);
      return value;
    }

    @Override
    public Integer visit(CInitializerList pInitializerList) throws Exception {
      return null;
    }

    @Override
    public Integer visit(CDesignatedInitializer pCStructInitializerPart) throws Exception {
      return null;
    }
  }

  private static class InvariantSamplingState implements AbstractState, Graphable {
    private Map<String, Integer> state;

    private LeftHandSideInvariantSamplingVisitor leftVisitor = new LeftHandSideInvariantSamplingVisitor();
    private RightHandSideInvariantSamplingVisitor rightVisitor = new RightHandSideInvariantSamplingVisitor();

    public InvariantSamplingState() {
      state = new HashMap<String, Integer>();
    }

    public InvariantSamplingState(
        InvariantSamplingState currentState,
        CVariableDeclaration declaration
    ) {
      state = currentState.getState();
      String variableName = declaration.getName();
      Integer value;
      try {
        value = declaration.getInitializer().accept(rightVisitor);
      } catch (Exception pE) {
        return;
      }
      if (variableName != null && value != null) {
        state.put(variableName, value);
      }
    }

    public InvariantSamplingState(
        InvariantSamplingState currentState, CExpressionAssignmentStatement statement
    ) {
      state = currentState.getState();

      String variableName;
      Integer value;
      try {
        variableName = statement.getLeftHandSide().accept(leftVisitor);
      } catch (Exception pE) {
        // this should never happen?
        return;
      }
      try {
        value = statement.getRightHandSide().accept(rightVisitor);
      } catch (Exception pE) {
        // TODO as we do not know what the variable evaluates to we should just remove it
        return;
      }
      if (variableName != null && value != null) {
        state.put(variableName, value);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof InvariantSamplingState)) {
        return false;
      }

      InvariantSamplingState state2 = (InvariantSamplingState) obj;

      boolean equals = state.equals(state2.getState());
      return equals;
    }

    public Map<String, Integer> getState() {
      return state;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('\n');
      Iterator<Entry<String, Integer>> iter = state.entrySet().iterator();
      while (iter.hasNext()) {
        Entry<String, Integer> entry = iter.next();
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
