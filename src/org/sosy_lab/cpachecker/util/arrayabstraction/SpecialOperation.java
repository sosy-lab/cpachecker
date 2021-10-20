// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.base.MoreObjects;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * Special operations make certain variable usage patterns more explicit and easier to use for the
 * array abstraction algorithm.
 *
 * <p>Special operations are created for CFA edges.
 */
abstract class SpecialOperation {

  private final CSimpleDeclaration declaration;

  private SpecialOperation(CSimpleDeclaration pDeclaration) {
    declaration = pDeclaration;
  }

  /**
   * Tries to fully evaluate the specified expression to an integer.
   *
   * <p>Considers the specified machine model and specified variable assignments during evaluation.
   *
   * @param pExpression the expression to evaluate to an integer
   * @param pMachineModel the machine model to consider during evaluation
   * @param pVariables map from variables (specified by their declarations) to their assigned values
   * @return If its possible to fully evaluate the specified expression, {@code Optional.of(value)}
   *     is returned, where {@code value} is the value the expression evaluates to. Otherwise, if
   *     its not possible to fully evaluate the expression, {@code Optional.empty()} is returned.
   * @throws NullPointerException if an parameter is {@code null}
   */
  public static Optional<BigInteger> eval(
      CExpression pExpression,
      MachineModel pMachineModel,
      Map<CSimpleDeclaration, BigInteger> pVariables) {

    ExpressionEvalVisitor visitor = new ExpressionEvalVisitor(pMachineModel, pVariables);

    return pExpression.accept(visitor);
  }

  /**
   * Returns the declaration of the variable relevant for this special operation.
   *
   * @return the declaration of the variable relevant for this special operation.
   */
  public CSimpleDeclaration getDeclaration() {
    return declaration;
  }

  /** Operation for these assign edges: <code>&lt;var&gt; = &lt;expression&gt;;</code> */
  private static final class ExpressionAssign extends SpecialOperation {

    private final CExpression expression;

    private ExpressionAssign(CSimpleDeclaration pDeclaration, CExpression pExpression) {
      super(pDeclaration);
      expression = pExpression;
    }

    /** Tries to extract an assign operation from the specified CFA edge. */
    private static Optional<ExpressionAssign> forEdge(CFAEdge pEdge) {

      if (pEdge instanceof CDeclarationEdge) {

        CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
        CDeclaration declaration = declarationEdge.getDeclaration();

        if (declaration instanceof CVariableDeclaration) {
          CInitializer initializer = ((CVariableDeclaration) declaration).getInitializer();
          if (initializer instanceof CInitializerExpression) {
            CExpression expression = ((CInitializerExpression) initializer).getExpression();
            return Optional.of(new ExpressionAssign(declaration, expression));
          }
        }
      }

      if (pEdge instanceof CStatementEdge) {

        CStatementEdge statementEdge = (CStatementEdge) pEdge;
        CStatement statement = statementEdge.getStatement();

        if (statement instanceof CExpressionAssignmentStatement) {

          CExpressionAssignmentStatement assignmentStatement =
              (CExpressionAssignmentStatement) statement;

          CLeftHandSide lhs = assignmentStatement.getLeftHandSide();

          if (lhs instanceof CIdExpression) {

            CIdExpression lhsIdExpression = (CIdExpression) lhs;
            CSimpleDeclaration variableDeclaration = lhsIdExpression.getDeclaration();
            CExpression rhs = assignmentStatement.getRightHandSide();

            return Optional.of(new ExpressionAssign(variableDeclaration, rhs));
          }
        }
      }

      return Optional.empty();
    }

    private CExpression getExpression() {
      return expression;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getDeclaration(), expression);
    }

    @Override
    public boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (!(pObject instanceof ExpressionAssign)) {
        return false;
      }

      ExpressionAssign other = (ExpressionAssign) pObject;
      return Objects.equals(getDeclaration(), other.getDeclaration())
          && Objects.equals(expression, other.expression);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("declaration", getDeclaration())
          .add("expression", expression)
          .toString();
    }
  }

  /** Operation for these assign edges: <code>&lt;var&gt; = &lt;constant-expression&gt;;</code> */
  public static final class ConstantAssign extends SpecialOperation {

    private final BigInteger value;

    private ConstantAssign(CSimpleDeclaration pDeclaration, BigInteger pValue) {
      super(pDeclaration);
      value = pValue;
    }

    /**
     * Tries to extract a constant assign operation from the specified edge.
     *
     * @param pEdge the CFA edge to extract the operation from
     * @param pMachineModel the machine model to consider during operation extraction
     * @param pVariables map from variables (specified by their declarations) to their assigned
     *     values
     * @return If its possible to extract a constant assign operation form the specified CFA edge,
     *     {@code Optional.of(constantAssign)} is returned, where {@code constantAssign} is the
     *     constant assign operation extracted from the CFA edge. Otherwise, if its not possible to
     *     extract this operation, {@code Optional.empty()} is returned.
     * @throws NullPointerException if any parameter is {@code null}
     */
    public static Optional<ConstantAssign> forEdge(
        CFAEdge pEdge, MachineModel pMachineModel, Map<CSimpleDeclaration, BigInteger> pVariables) {

      Optional<ExpressionAssign> optExpressionAssign = ExpressionAssign.forEdge(pEdge);

      if (optExpressionAssign.isPresent()) {

        ExpressionAssign expressionAssign = optExpressionAssign.orElseThrow();
        CExpression expression = expressionAssign.getExpression();

        Optional<BigInteger> optConstantValue = eval(expression, pMachineModel, pVariables);

        if (optConstantValue.isPresent()) {
          CSimpleDeclaration declaration = expressionAssign.getDeclaration();
          BigInteger constantValue = optConstantValue.orElseThrow();
          return Optional.of(new ConstantAssign(declaration, constantValue));
        }
      }

      return Optional.empty();
    }

    /**
     * Returns the value that is assigned to the variable
     *
     * @return the value that is assigned to the variable
     */
    public BigInteger getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getDeclaration(), value);
    }

    @Override
    public boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (!(pObject instanceof ConstantAssign)) {
        return false;
      }

      ConstantAssign other = (ConstantAssign) pObject;
      return Objects.equals(getDeclaration(), other.getDeclaration())
          && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("declaration", getDeclaration())
          .add("value", value)
          .toString();
    }
  }

  /**
   * Operation for these assign edges: <code>&lt;var&gt; = &lt;var&gt; &#123; + | - &#125;
   * &lt;constant-expression&gt;;</code> (the <code>&lt;var&gt;</code> must be equal)
   */
  public static final class UpdateAssign extends SpecialOperation {

    private final BigInteger stepValue;

    private UpdateAssign(CSimpleDeclaration pDeclaration, BigInteger pStepValue) {
      super(pDeclaration);
      stepValue = pStepValue;
    }

    /**
     * Tries to extract an update assign operation from the specified edge.
     *
     * @param pEdge the CFA edge to extract the operation from
     * @param pMachineModel the machine model to consider during operation extraction
     * @param pVariables map from variables (specified by their declarations) to their corresponding
     *     assigned value
     * @return If its possible to extract an update assign operation form the specified CFA edge,
     *     {@code Optional.of(updateAssign)} is returned, where {@code updateAssign} is the update
     *     assign operation extracted from the CFA edge. Otherwise, if its not possible to extract
     *     this operation, {@code Optional.empty()} is returned.
     * @throws NullPointerException if any parameter is {@code null}
     */
    public static Optional<UpdateAssign> forEdge(
        CFAEdge pEdge, MachineModel pMachineModel, Map<CSimpleDeclaration, BigInteger> pVariables) {

      Optional<ExpressionAssign> optExpressionAssign = ExpressionAssign.forEdge(pEdge);

      if (optExpressionAssign.isPresent()) {

        ExpressionAssign expressionAssign = optExpressionAssign.orElseThrow();
        CExpression expression = expressionAssign.getExpression();

        if (expression instanceof CBinaryExpression) {

          CBinaryExpression binaryExpression = (CBinaryExpression) expression;
          CExpression operand1 = binaryExpression.getOperand1();
          CBinaryExpression.BinaryOperator operator = binaryExpression.getOperator();

          if ((operator == CBinaryExpression.BinaryOperator.PLUS
                  || operator == CBinaryExpression.BinaryOperator.MINUS)
              && operand1 instanceof CIdExpression) {

            CSimpleDeclaration assignDeclaration = expressionAssign.getDeclaration();
            CSimpleDeclaration operand1Declaration = ((CIdExpression) operand1).getDeclaration();

            if (operand1Declaration.equals(assignDeclaration)) {

              CExpression stepExpression = binaryExpression.getOperand2();
              if (operator == CBinaryExpression.BinaryOperator.MINUS) {
                stepExpression =
                    new CUnaryExpression(
                        FileLocation.DUMMY,
                        stepExpression.getExpressionType(),
                        stepExpression,
                        CUnaryExpression.UnaryOperator.MINUS);
              }

              Optional<BigInteger> optStepValue = eval(stepExpression, pMachineModel, pVariables);

              if (optStepValue.isPresent()) {
                BigInteger stepValue = optStepValue.orElseThrow();
                return Optional.of(new UpdateAssign(assignDeclaration, stepValue));
              }
            }
          }
        }
      }

      return Optional.empty();
    }

    /**
     * Returns by how much the value of the variable is increased when this operation is executed.
     *
     * <p>The returned value can be negative (e.g., for {@code i = i - 1;}).
     *
     * @return by how much the value of the variable is increased when this operation is executed
     */
    public BigInteger getStepValue() {
      return stepValue;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getDeclaration(), stepValue);
    }

    @Override
    public boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (!(pObject instanceof UpdateAssign)) {
        return false;
      }

      UpdateAssign other = (UpdateAssign) pObject;
      return Objects.equals(getDeclaration(), other.getDeclaration())
          && Objects.equals(stepValue, other.stepValue);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("declaration", getDeclaration())
          .add("stepValue", stepValue)
          .toString();
    }
  }

  /**
   * Operation for these assume edges: <code>&lt;var&gt; &#123; &lt; | &lt;= | &gt; | &gt;= &#125;
   * &lt;constant-expression&gt;</code>
   */
  public static final class ComparisonAssume extends SpecialOperation {

    private final Operator operator;
    private final BigInteger value;

    private ComparisonAssume(
        CSimpleDeclaration pDeclaration, Operator pOperator, BigInteger pValue) {
      super(pDeclaration);
      value = pValue;
      operator = pOperator;
    }

    /**
     * Tries to extract a comparison assume operation from the specified edge.
     *
     * @param pEdge the CFA edge to extract the operation from
     * @param pMachineModel the machine model to consider during operation extraction
     * @param pVariables map from variables (specified by their declarations) to their corresponding
     *     assigned value
     * @return If its possible to extract a comparison assume operation form the specified CFA edge,
     *     {@code Optional.of(comparisonAssume)} is returned, where {@code comparisonAssume} is the
     *     comparison assume operation extracted from the CFA edge. Otherwise, if its not possible
     *     to extract this operation, {@code Optional.empty()} is returned.
     * @throws NullPointerException if any parameter is {@code null}
     */
    public static Optional<ComparisonAssume> forEdge(
        CFAEdge pEdge, MachineModel pMachineModel, Map<CSimpleDeclaration, BigInteger> pVariables) {

      if (pEdge instanceof CAssumeEdge) {

        CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
        CExpression expression = assumeEdge.getExpression();

        if (expression instanceof CBinaryExpression) {

          CBinaryExpression binaryExpression = (CBinaryExpression) expression;
          CExpression operand1 = binaryExpression.getOperand1();
          CBinaryExpression.BinaryOperator operator = binaryExpression.getOperator();

          if ((operator == CBinaryExpression.BinaryOperator.LESS_THAN
                  || operator == CBinaryExpression.BinaryOperator.GREATER_THAN
                  || operator == CBinaryExpression.BinaryOperator.LESS_EQUAL
                  || operator == CBinaryExpression.BinaryOperator.GREATER_EQUAL)
              && operand1 instanceof CIdExpression) {

            CExpression valueExpression = binaryExpression.getOperand2();

            if (operator == CBinaryExpression.BinaryOperator.LESS_THAN) {
              valueExpression =
                  new CBinaryExpression(
                      FileLocation.DUMMY,
                      valueExpression.getExpressionType(),
                      valueExpression.getExpressionType(),
                      valueExpression,
                      CIntegerLiteralExpression.ONE,
                      CBinaryExpression.BinaryOperator.MINUS);
            } else if (operator == CBinaryExpression.BinaryOperator.GREATER_THAN) {
              valueExpression =
                  new CBinaryExpression(
                      FileLocation.DUMMY,
                      valueExpression.getExpressionType(),
                      valueExpression.getExpressionType(),
                      valueExpression,
                      CIntegerLiteralExpression.ONE,
                      CBinaryExpression.BinaryOperator.PLUS);
            }

            Optional<BigInteger> optConstantValue =
                eval(valueExpression, pMachineModel, pVariables);

            if (optConstantValue.isPresent()) {

              Operator operationOperator;
              if (operator == CBinaryExpression.BinaryOperator.LESS_THAN) {
                operationOperator = Operator.LESS_EQUAL;
              } else if (operator == CBinaryExpression.BinaryOperator.GREATER_THAN) {
                operationOperator = Operator.GREATER_EQUAL;
              } else if (operator == CBinaryExpression.BinaryOperator.LESS_EQUAL) {
                operationOperator = Operator.LESS_EQUAL;
              } else if (operator == CBinaryExpression.BinaryOperator.GREATER_EQUAL) {
                operationOperator = Operator.GREATER_EQUAL;
              } else {
                throw new AssertionError("Unknown operator: " + operator);
              }

              CSimpleDeclaration variableDeclaration = ((CIdExpression) operand1).getDeclaration();
              BigInteger constantValue = optConstantValue.orElseThrow();

              return Optional.of(
                  new ComparisonAssume(variableDeclaration, operationOperator, constantValue));
            }
          }
        }
      }

      return Optional.empty();
    }

    /**
     * Returns the comparison operator of this comparison (<code>&lt;operator&gt;</code> in <code>
     * &lt;var&gt; &lt;operator&gt; &lt;value&gt;</code>).
     *
     * @return the comparison operator of this comparison
     */
    public Operator getOperator() {
      return operator;
    }

    /**
     * Returns the value to which the variable is compared (<code>&lt;value&gt;</code> in <code>
     * &lt;var&gt; &lt;operator&gt; &lt;value&gt;</code>).
     *
     * @return the value to which the variable is compared
     */
    public BigInteger getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getDeclaration(), operator, value);
    }

    @Override
    public boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (!(pObject instanceof ComparisonAssume)) {
        return false;
      }

      ComparisonAssume other = (ComparisonAssume) pObject;
      return Objects.equals(getDeclaration(), other.getDeclaration())
          && operator == other.operator
          && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("declaration", getDeclaration())
          .add("operator", operator)
          .add("value", value)
          .toString();
    }

    /** The comparison operator (<code>&lt;</code> or <code>&gt;</code>) */
    public enum Operator {
      LESS_EQUAL,
      GREATER_EQUAL
    }
  }

  /**
   * {@link CExpressionVisitor} for expression evaluation.
   *
   * <p>Considers the specified machine model and specified variable assignments during evaluation.
   */
  private static final class ExpressionEvalVisitor
      implements CExpressionVisitor<Optional<BigInteger>, NoException> {

    private final MachineModel machineModel;
    private final Map<CSimpleDeclaration, BigInteger> variables;

    private ExpressionEvalVisitor(
        MachineModel pMachineModel, Map<CSimpleDeclaration, BigInteger> pVariables) {
      machineModel = pMachineModel;
      variables = pVariables;
    }

    private Optional<BigInteger> preventInvalidValue(CType pType, BigInteger pValue) {

      if (pType instanceof CSimpleType) {
        CSimpleType simpleType = (CSimpleType) pType;
        if (simpleType.getType().isIntegerType()) {

          BigInteger minValue = machineModel.getMinimalIntegerValue(simpleType);
          BigInteger maxValue = machineModel.getMaximalIntegerValue(simpleType);

          if (pValue.compareTo(minValue) >= 0 && pValue.compareTo(maxValue) <= 0) {
            return Optional.of(pValue);
          }
        }
      }

      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CFieldReference pIastFieldReference) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CIdExpression pIastIdExpression) {

      CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();

      return Optional.ofNullable(variables.get(declaration));
    }

    @Override
    public Optional<BigInteger> visit(CPointerExpression pPointerExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CComplexCastExpression pComplexCastExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CBinaryExpression pIastBinaryExpression) {

      Optional<BigInteger> optOperand1Value = pIastBinaryExpression.getOperand1().accept(this);
      Optional<BigInteger> optOperand2Value = pIastBinaryExpression.getOperand2().accept(this);

      if (optOperand1Value.isPresent() && optOperand2Value.isPresent()) {

        BigInteger operand1Value = optOperand1Value.orElseThrow();
        BigInteger operand2Value = optOperand2Value.orElseThrow();
        CBinaryExpression.BinaryOperator operator = pIastBinaryExpression.getOperator();
        CType type = pIastBinaryExpression.getExpressionType();

        switch (operator) {
          case PLUS:
            return preventInvalidValue(type, operand1Value.add(operand2Value));
          case MINUS:
            return preventInvalidValue(type, operand1Value.subtract(operand2Value));
          case MULTIPLY:
            return preventInvalidValue(type, operand1Value.multiply(operand2Value));
          case DIVIDE:
            return operand2Value.equals(BigInteger.ZERO)
                ? Optional.empty()
                : preventInvalidValue(type, operand1Value.divide(operand2Value));
          case MODULO:
            return operand2Value.equals(BigInteger.ZERO)
                ? Optional.empty()
                : preventInvalidValue(type, operand1Value.remainder(operand2Value));
          case EQUALS:
            return Optional.of(
                operand1Value.equals(operand2Value) ? BigInteger.ONE : BigInteger.ZERO);
          case NOT_EQUALS:
            return Optional.of(
                !operand1Value.equals(operand2Value) ? BigInteger.ONE : BigInteger.ZERO);
          case LESS_THAN:
            return Optional.of(
                operand1Value.compareTo(operand2Value) < 0 ? BigInteger.ONE : BigInteger.ZERO);
          case LESS_EQUAL:
            return Optional.of(
                operand1Value.compareTo(operand2Value) <= 0 ? BigInteger.ONE : BigInteger.ZERO);
          case GREATER_THAN:
            return Optional.of(
                operand1Value.compareTo(operand2Value) > 0 ? BigInteger.ONE : BigInteger.ZERO);
          case GREATER_EQUAL:
            return Optional.of(
                operand1Value.compareTo(operand2Value) >= 0 ? BigInteger.ONE : BigInteger.ZERO);
          case BINARY_AND:
            return preventInvalidValue(type, operand1Value.and(operand2Value));
          case BINARY_OR:
            return preventInvalidValue(type, operand1Value.or(operand2Value));
          case BINARY_XOR:
            return preventInvalidValue(type, operand1Value.xor(operand2Value));
          default:
            break;
        }
      }

      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CCastExpression pIastCastExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CCharLiteralExpression pIastCharLiteralExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
      return Optional.of(pIastIntegerLiteralExpression.getValue());
    }

    @Override
    public Optional<BigInteger> visit(CStringLiteralExpression pIastStringLiteralExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CTypeIdExpression pIastTypeIdExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CUnaryExpression pIastUnaryExpression) {

      CUnaryExpression.UnaryOperator operator = pIastUnaryExpression.getOperator();

      if (operator == CUnaryExpression.UnaryOperator.MINUS) {

        Optional<BigInteger> optOperandValue = pIastUnaryExpression.getOperand().accept(this);

        if (optOperandValue.isPresent()) {

          BigInteger operandValue = optOperandValue.orElseThrow();
          CType type = pIastUnaryExpression.getExpressionType();

          return preventInvalidValue(type, operandValue.negate());
        }
      }

      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CImaginaryLiteralExpression PIastLiteralExpression) {
      return Optional.empty();
    }

    @Override
    public Optional<BigInteger> visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
      return Optional.empty();
    }
  }
}
