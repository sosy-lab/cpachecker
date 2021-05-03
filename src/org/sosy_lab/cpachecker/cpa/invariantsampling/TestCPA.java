// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariantsampling;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.cdt.internal.core.dom.parser.c.CVariable;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
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
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

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

  private static class RightHandSideInvariantSamplingVisitor
      implements CExpressionVisitor<Integer, Exception>,
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

    private final ImmutableList<CVariableDeclaration> variables;
    private boolean inLoop = false;
//    private final CExpression pre;
//    private final ? cond;
//    private final ? post;

    public InvariantSamplingState() {
      variables = ImmutableList.of();
    }

    private InvariantSamplingState(
        ImmutableList<CVariableDeclaration> newVariables,
        Boolean newInLoop) {
      this.variables = newVariables;
      this.inLoop = newInLoop;
    }

//    private InvariantSamplingState(
//        InvariantSamplingState currentState, String variableName, Integer value) {
//      checkNotNull(variableName);
//      checkNotNull(value);
//      ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
//      builder.put(variableName, value);
//      for (Entry<String, Integer> entry : currentState.getState().entrySet()) {
//        if (!entry.getKey().equals(variableName)) {
//          builder.put(entry);
//        }
//      }
//      state = builder.build();
//    }

    public static InvariantSamplingState newInvariantSamplingState(
        InvariantSamplingState currentState, BlankEdge edge
    ) throws CPAException {
      if (edge.getDescription().equals("while") || edge.getDescription().equals("for")) {
        return new InvariantSamplingState(currentState.variables, true);
      }
      return currentState;
    }

    public static InvariantSamplingState newInvariantSamplingState(
        InvariantSamplingState currentState, CExpression expression
    ) {
//      CExpression pre = currentState.pre && expression;
      return currentState;
    }

    public static InvariantSamplingState newInvariantSamplingState(
        InvariantSamplingState currentState, CVariableDeclaration declaration) throws CPAException {
//      RightHandSideInvariantSamplingVisitor rightVisitor =
//          new RightHandSideInvariantSamplingVisitor();
//      String variableName = declaration.getName();
//      Integer value;
//      try {
//        value = declaration.getInitializer().accept(rightVisitor);
//      } catch (Exception e) {
//        throw new CPAException("Could not determine declaration for InvariantSamplingState", e);
//      }
//      return new InvariantSamplingState(currentState, variableName, value);
      return currentState;
    }

    public static InvariantSamplingState newInvariantSamplingState(
        InvariantSamplingState currentState, CExpressionAssignmentStatement statement)
        throws CPAException {
//      LeftHandSideInvariantSamplingVisitor leftVisitor = new LeftHandSideInvariantSamplingVisitor();
//      RightHandSideInvariantSamplingVisitor rightVisitor =
//          new RightHandSideInvariantSamplingVisitor();

//      String variableName;
//      Integer value;
//      try {
//        variableName = statement.getLeftHandSide().accept(leftVisitor);
//        value = statement.getRightHandSide().accept(rightVisitor);
//      } catch (Exception e) {
//        throw new CPAException("Could not determine assignment for InvariantSamplingState", e);
//      }
      return currentState;
//      return new InvariantSamplingState(currentState, variableName, value);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
//      result = prime * result + ((state == null) ? 0 : state.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
//      if (this == obj) {
//        return true;
//      }
//      if (obj == null) {
//        return false;
//      }
//      if (getClass() != obj.getClass()) {
//        return false;
//      }
//      InvariantSamplingState other = (InvariantSamplingState) obj;
//      if (state == null) {
//        if (other.state != null) {
//          return false;
//        }
//      } else if (!state.equals(other.state)) {
//        return false;
//      }
      return true;
    }

//    public Map<String, Integer> getState() {
//      return state;
//    }

    @Override
    public String toString() {
//      StringBuilder sb = new StringBuilder();
//      sb.append('\n');
//      Iterator<Entry<String, Integer>> iter = state.entrySet().iterator();
//      while (iter.hasNext()) {
//        Entry<String, Integer> entry = iter.next();
//        sb.append(entry.getKey());
//        sb.append('=').append('"');
//        sb.append(entry.getValue());
//        sb.append('"');
//        if (iter.hasNext()) {
//          sb.append('\n');
//        }
//      }
      return String.format("inLoop: %b", this.inLoop);
    }

    @Override
    public String toDOTLabel() {
      return toString();
    }

    @Override
    public boolean shouldBeHighlighted() {
      return false;
    }
  }

  private static class InvariantSamplingTransferRelation
      extends ForwardingTransferRelation<InvariantSamplingState, InvariantSamplingState, Precision>
      implements TransferRelation {

    @Override
    protected InvariantSamplingState handleDeclarationEdge(
        CDeclarationEdge edge, CDeclaration declaration) throws CPATransferException {
      int i = 0;
      try {
        if (declaration instanceof CVariableDeclaration) {
          return InvariantSamplingState.newInvariantSamplingState(
              this.getState(), (CVariableDeclaration) declaration);
        }
        return this.getState();
      } catch (CPAException e) {
        throw new CPATransferException("Could not generate successor for invariant sampling", e);
      }
    }

    protected InvariantSamplingState handleBlankEdge(BlankEdge cfaEdge)
        throws CPATransferException {
      state = this.getState();
      try {
        return InvariantSamplingState.newInvariantSamplingState(state, cfaEdge);
      } catch (CPAException e) {
        throw new CPATransferException("Could not generate a new state.", e);
      }
    }

    @Override
    protected InvariantSamplingState handleAssumption(
        CAssumeEdge edge, CExpression expression, boolean truthAssumption
    ) throws CPATransferException {
      state = this.getState();
      return InvariantSamplingState.newInvariantSamplingState(state, expression);

    }

    @Override
    protected InvariantSamplingState handleFunctionCallEdge(
        CFunctionCallEdge cfaEdge,
        List<CExpression> arguments, List<CParameterDeclaration> parameters,
        String calledFunctionName) {
      return this.getState();
    }

    @Override
    protected InvariantSamplingState handleFunctionReturnEdge(
        CFunctionReturnEdge cfaEdge,
        CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
        throws CPATransferException {
      return this.getState();
    }

    @Override
    protected InvariantSamplingState handleStatementEdge(
        CStatementEdge edge,
        CStatement statement) {
//      try {
//      if (statement instanceof CExpressionAssignmentStatement) {
//        return InvariantSamplingState.newInvariantSamplingState(
//            this.getState(), (CExpressionAssignmentStatement) statement);
//      }
//      return this.getState();
//      } catch (CPAException e) {
//        throw new CPATransferException("Could not generate successor for invariant sampling", e);
//      }
      return this.getState();
    }


    @Override
    protected InvariantSamplingState handleReturnStatementEdge(CReturnStatementEdge edge) {
      return this.getState();
    }

  }
}
