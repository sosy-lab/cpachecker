// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;
import org.sosy_lab.java_smt.api.FloatingPointNumber;

/**
 * Creates assumption along an error path based on a given {@link CFAEdge} edge and a given {@link
 * ConcreteState} state.
 */
@Options(prefix = "counterexample.export.assumptions")
public class AssumptionToEdgeAllocator {

  private final LogManager logger;
  private final MachineModel machineModel;

  private static final int FIRST = 0;

  @Option(
      secure = true,
      description =
          "Try to avoid using operations that exceed the capabilities"
              + " of linear arithmetics when extracting assumptions from the model."
              + " This option aims to prevent witnesses that are inconsistent with "
              + " models that are, due to an analysis limited to linear"
              + " arithmetics, actually incorrect.\n"
              + " This option does not magically produce a correct witness from an"
              + " incorrect model,"
              + " and since the difference between an incorrect witness consistent"
              + " with the model and an incorrect witness that is inconsistent with"
              + " the model is academic, you usually want this option to be off.")
  private boolean assumeLinearArithmetics = false;

  @Option(
      secure = true,
      description =
          "If the option assumeLinearArithmetics is set, this option can be used to"
              + " allow multiplication between operands with at least one constant.")
  private boolean allowMultiplicationWithConstants = false;

  @Option(
      secure = true,
      description =
          "If the option assumeLinearArithmetics is set, this option can be used to"
              + " allow division and modulo by constants.")
  private boolean allowDivisionAndModuloByConstants = false;

  /**
   * Creates an instance of the allocator that takes an {@link CFAEdge} edge along an error path and
   * a {@link ConcreteState} state that contains the concrete values of the variables and of the
   * memory at that edge and creates concrete assumptions for the variables at the given edge.
   *
   * @param pConfig the configuration.
   * @param pLogger logger for logging purposes.
   * @param pMachineModel the machine model that holds for the error path of the given edge.
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  public static AssumptionToEdgeAllocator create(
      Configuration pConfig, LogManager pLogger, MachineModel pMachineModel)
      throws InvalidConfigurationException {
    return new AssumptionToEdgeAllocator(pConfig, pLogger, pMachineModel);
  }

  private AssumptionToEdgeAllocator(
      Configuration pConfig, LogManager pLogger, MachineModel pMachineModel)
      throws InvalidConfigurationException {

    Preconditions.checkNotNull(pLogger);
    Preconditions.checkNotNull(pMachineModel);

    pConfig.inject(this);

    logger = pLogger;
    machineModel = pMachineModel;
  }

  /**
   * Assigns assumptions to the variables of the given {@link CFAEdge} edge.
   *
   * @param pCFAEdge an edge along the error path.
   * @param pConcreteState a state that contains the concrete values of the variables at the given
   *     edge.
   * @return An {@link CFAEdgeWithAssumptions} edge that contains concrete values for variables
   *     represented as assumptions
   */
  public CFAEdgeWithAssumptions allocateAssumptionsToEdge(
      CFAEdge pCFAEdge, ConcreteState pConcreteState) {
    ImmutableSet<AExpressionStatement> assignmentsAtEdge =
        createAssignmentsAtEdge(pCFAEdge, pConcreteState);
    String comment = createComment(pCFAEdge, pConcreteState);
    return new CFAEdgeWithAssumptions(pCFAEdge, assignmentsAtEdge, comment);
  }

  private String createComment(CFAEdge pCfaEdge, ConcreteState pConcreteState) {
    return switch (pCfaEdge.getEdgeType()) {
      case AssumeEdge -> handleAssumeComment((AssumeEdge) pCfaEdge, pConcreteState);
      case DeclarationEdge -> handleDclComment((ADeclarationEdge) pCfaEdge, pConcreteState);
      case ReturnStatementEdge ->
          handleReturnStatementComment((AReturnStatementEdge) pCfaEdge, pConcreteState);
      default -> "";
    };
  }

  private String handleReturnStatementComment(
      AReturnStatementEdge pCfaEdge, ConcreteState pConcreteState) {
    Optional<? extends AExpression> returnExpression = pCfaEdge.getExpression();
    if (returnExpression.isPresent() && returnExpression.get() instanceof CExpression returnExp) {

      if (returnExp instanceof CLiteralExpression) {
        /*boring expression*/
        return "";
      }

      String functionname = pCfaEdge.getPredecessor().getFunctionName();
      LModelValueVisitor v = new LModelValueVisitor(functionname, pConcreteState);
      Number value = v.evaluateNumericalValue(returnExp);

      if (value == null) {
        return "";
      }

      return returnExp.toASTString() + " = " + value;
    }

    return "";
  }

  private String handleDclComment(ADeclarationEdge pCfaEdge, ConcreteState pConcreteState) {
    if (pCfaEdge instanceof CDeclarationEdge) {
      return getCommentOfDclAddress(
          (CSimpleDeclaration) pCfaEdge.getDeclaration(), pCfaEdge, pConcreteState);
    }
    return "";
  }

  private String getCommentOfDclAddress(
      CSimpleDeclaration dcl, CFAEdge edge, ConcreteState pConcreteState) {

    String functionName = edge.getPredecessor().getFunctionName();

    // function name may be null
    LModelValueVisitor v = new LModelValueVisitor(functionName, pConcreteState);
    Address address = v.getAddress(dcl);

    // concrete addresses already assumptions
    if (address.isUnknown() || address.isConcrete()) {
      return "";
    }

    return "&" + dcl.getName() + " == " + address.getCommentRepresentation();
  }

  private ImmutableSet<AExpressionStatement> createAssignmentsAtEdge(
      CFAEdge pCFAEdge, ConcreteState pConcreteState) {
    ImmutableSet.Builder<AExpressionStatement> result = ImmutableSet.builder();

    // Get all Assumptions of this edge
    switch (pCFAEdge.getEdgeType()) {
      case DeclarationEdge ->
          result.addAll(
              handleDeclaration(
                  ((ADeclarationEdge) pCFAEdge).getDeclaration(),
                  pCFAEdge.getPredecessor().getFunctionName(),
                  pConcreteState));
      case StatementEdge ->
          result.addAll(
              handleStatement(
                  pCFAEdge, ((AStatementEdge) pCFAEdge).getStatement(), pConcreteState));
      case AssumeEdge ->
          result.addAll(handleAssumeStatement((AssumeEdge) pCFAEdge, pConcreteState));
      default -> {}
    }

    if (pCFAEdge.getEdgeType() == CFAEdgeType.BlankEdge
        || !AutomatonGraphmlCommon.handleAsEpsilonEdge(pCFAEdge)) {
      List<AExpressionStatement> parameterAssumptions =
          handleFunctionEntry(pCFAEdge, pConcreteState);
      result.addAll(parameterAssumptions);
    }

    return result.build();
  }

  private String handleAssumeComment(AssumeEdge pCfaEdge, ConcreteState pConcreteState) {
    if (pCfaEdge instanceof CAssumeEdge cAssumeEdge) {
      return handleAssumeComment(cAssumeEdge, pConcreteState);
    }
    return "";
  }

  private String handleAssumeComment(CAssumeEdge pCFAEdge, ConcreteState pConcreteState) {

    CExpression pCExpression = pCFAEdge.getExpression();
    String functionName = pCFAEdge.getPredecessor().getFunctionName();

    if (pCExpression instanceof CBinaryExpression binExp) {

      String result1 = handleAssumeOp(pCFAEdge, binExp.getOperand1(), functionName, pConcreteState);
      String result2 = handleAssumeOp(pCFAEdge, binExp.getOperand2(), functionName, pConcreteState);

      if (!result1.isEmpty() && !result2.isEmpty()) {
        return result1 + System.lineSeparator() + result2;
      } else if (!result1.isEmpty()) {
        return result1;
      } else if (!result2.isEmpty()) {
        return result2;
      }
    }

    return "";
  }

  private String handleAssumeOp(
      CFAEdge pCFAEdge, CExpression op, String pFunctionName, ConcreteState pConcreteState) {

    if (op instanceof CLiteralExpression) {
      /*boring expression*/
      return "";
    }

    if (op instanceof CLeftHandSide cLeftHandSide) {

      List<AExpressionStatement> assignments =
          handleAssignment(pCFAEdge, cLeftHandSide, pConcreteState);

      if (assignments.isEmpty()) {
        return "";
      } else {
        return Joiner.on(System.lineSeparator())
            .join(Iterables.transform(assignments, AExpressionStatement::toASTString));
      }

    } else {
      Object value = getValueObject(op, pFunctionName, pConcreteState);

      if (value != null) {
        return op.toASTString() + " == " + value;
      } else {
        return "";
      }
    }
  }

  private List<AExpressionStatement> handleAssumeStatement(
      AssumeEdge pCFAEdge, ConcreteState pConcreteState) {

    if (!(pCFAEdge instanceof CAssumeEdge cAssumeEdge)) {
      return ImmutableList.of();

    } else {
      CExpression pCExpression = cAssumeEdge.getExpression();

      if (!(pCExpression instanceof CBinaryExpression binExp)) {
        return ImmutableList.of();

      } else {

        CExpression op1 = binExp.getOperand1();
        CExpression op2 = binExp.getOperand2();

        ImmutableList.Builder<AExpressionStatement> result = ImmutableList.builder();
        if (op1 instanceof CLeftHandSide cLeftHandSide) {
          result.addAll(handleAssignment(pCFAEdge, cLeftHandSide, pConcreteState));
        }

        if (op2 instanceof CLeftHandSide cLeftHandSide) {
          result.addAll(handleAssignment(pCFAEdge, cLeftHandSide, pConcreteState));
        }
        return result.build();
      }
    }
  }

  private Object getValueObject(
      CExpression pOp1, String pFunctionName, ConcreteState pConcreteState) {
    LModelValueVisitor v = new LModelValueVisitor(pFunctionName, pConcreteState);
    return v.evaluateNumericalValue(pOp1);
  }

  private List<AExpressionStatement> handleFunctionEntry(
      CFAEdge pEdge, ConcreteState pConcreteState) {

    CFANode predecessor = pEdge.getPredecessor();

    // For the program entry function, we must be careful not to create
    // expressions before the global initializations
    if (predecessor.getNumEnteringEdges() <= 0) {
      return ImmutableList.of();
    }

    // Handle program entry function
    String function = predecessor.getFunctionName();
    while (!(predecessor instanceof FunctionEntryNode)) {
      if (predecessor.getNumEnteringEdges() != 1
          || !predecessor.getFunctionName().equals(function)) {
        return ImmutableList.of();
      }
      CFAEdge enteringEdge = predecessor.getEnteringEdge(0);
      if (!AutomatonGraphmlCommon.handleAsEpsilonEdge(enteringEdge)
          && !AutomatonGraphmlCommon.isMainFunctionEntry(enteringEdge)) {
        return ImmutableList.of();
      }
      predecessor = enteringEdge.getPredecessor();
    }

    FunctionEntryNode entryNode = (FunctionEntryNode) predecessor;
    String functionName = entryNode.getFunctionDefinition().getName();

    List<? extends AParameterDeclaration> parameterDeclarations = entryNode.getFunctionParameters();
    if (parameterDeclarations.isEmpty()) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<AExpressionStatement> result = ImmutableList.builder();
    for (AParameterDeclaration parameterDeclaration : parameterDeclarations) {
      result.addAll(handleDeclaration(parameterDeclaration, functionName, pConcreteState));
    }
    return result.build();
  }

  private List<AExpressionStatement> handleAssignment(
      CFAEdge pCFAEdge, CLeftHandSide pLeftHandSide, ConcreteState pConcreteState) {

    String functionName = pCFAEdge.getPredecessor().getFunctionName();
    Object value = getValueObject(pLeftHandSide, functionName, pConcreteState);

    if (value == null) {
      return ImmutableList.of();
    }

    Type expectedType = pLeftHandSide.getExpressionType();
    ValueLiterals valueAsCode =
        getValueAsCode(value, expectedType, pLeftHandSide, functionName, pConcreteState);

    return handleSimpleValueLiteralsAssumptions(valueAsCode, pLeftHandSide);
  }

  private List<AExpressionStatement> handleAssignment(
      CFAEdge pCFAEdge, CAssignment pAssignment, ConcreteState pConcreteState) {
    CLeftHandSide leftHandSide = pAssignment.getLeftHandSide();
    return handleAssignment(pCFAEdge, leftHandSide, pConcreteState);
  }

  private Object getValueObject(
      CLeftHandSide pLeftHandSide, String pFunctionName, ConcreteState pConcreteState) {

    LModelValueVisitor v = new LModelValueVisitor(pFunctionName, pConcreteState);
    return pLeftHandSide.accept(v);
  }

  private ValueLiterals getValueAsCode(
      Object pValue,
      Type pExpectedType,
      CLeftHandSide leftHandSide,
      String functionName,
      ConcreteState pConcreteState) {

    // TODO processing for other languages
    if (pExpectedType instanceof CType expectedType) {
      CType cType = expectedType.getCanonicalType();

      ValueLiteralsVisitor v = new ValueLiteralsVisitor(pValue, leftHandSide, pConcreteState);
      ValueLiterals valueLiterals = cType.accept(v);

      // resolve field references that lack an address
      if (isStructOrUnionType(cType) && leftHandSide instanceof CIdExpression cIdExpression) {
        v.resolveStruct(cType, valueLiterals, cIdExpression, functionName);
      }

      return valueLiterals;
    }

    return new ValueLiterals();
  }

  private List<AExpressionStatement> handleStatement(
      CFAEdge pCFAEdge, AStatement pStatement, ConcreteState pConcreteState) {

    if (pStatement instanceof CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement) {
      CAssignment assignmentStatement = cFunctionCallAssignmentStatement;
      return handleAssignment(pCFAEdge, assignmentStatement, pConcreteState);
    }

    if (pStatement instanceof CExpressionAssignmentStatement cExpressionAssignmentStatement) {
      CAssignment assignmentStatement = cExpressionAssignmentStatement;
      return handleAssignment(pCFAEdge, assignmentStatement, pConcreteState);
    }

    return ImmutableList.of();
  }

  private List<AExpressionStatement> handleDeclaration(
      ASimpleDeclaration dcl, String pFunctionName, ConcreteState pConcreteState) {

    if (dcl instanceof CSimpleDeclaration cDcl) {

      CType dclType = cDcl.getType();
      @Nullable Object value = getValueObject(cDcl, pFunctionName, pConcreteState);

      if (value == null) {
        return ImmutableList.of();
      }

      CIdExpression idExpression = new CIdExpression(dcl.getFileLocation(), cDcl);
      ValueLiterals valueAsCode =
          getValueAsCode(value, dclType, idExpression, pFunctionName, pConcreteState);
      CLeftHandSide leftHandSide = new CIdExpression(FileLocation.DUMMY, cDcl);
      return handleSimpleValueLiteralsAssumptions(valueAsCode, leftHandSide);
    }

    return ImmutableList.of();
  }

  private List<AExpressionStatement> handleSimpleValueLiteralsAssumptions(
      ValueLiterals pValueLiterals, CLeftHandSide pLValue) {

    Set<SubExpressionValueLiteral> subValues = pValueLiterals.getSubExpressionValueLiteral();
    Set<AExpressionStatement> statements = new LinkedHashSet<>();
    CBinaryExpressionBuilder expressionBuilder = new CBinaryExpressionBuilder(machineModel, logger);

    if (!pValueLiterals.hasUnknownValueLiteral()) {

      CExpression leftSide = getLeftAssumptionFromLhs(pLValue);
      CExpression rightSide = pValueLiterals.getExpressionValueLiteralAsCExpression();
      AExpressionStatement statement =
          buildEquationExpressionStatement(expressionBuilder, leftSide, rightSide);
      statements.add(statement);
    }

    for (SubExpressionValueLiteral subValueLiteral : subValues) {

      CExpression leftSide = getLeftAssumptionFromLhs(subValueLiteral.subExpression());
      CExpression rightSide = subValueLiteral.valueLiteralAsCExpression();
      AExpressionStatement statement =
          buildEquationExpressionStatement(expressionBuilder, leftSide, rightSide);
      statements.add(statement);
    }

    return FluentIterable.from(statements).filter(Predicates.notNull()).toList();
  }

  private @Nullable AExpressionStatement buildEquationExpressionStatement(
      CBinaryExpressionBuilder pBuilder, CExpression pLeftSide, CExpression pRightSide) {
    CExpression leftSide = pLeftSide;
    CExpression rightSide = pRightSide;

    final CType leftType = leftSide.getExpressionType().getCanonicalType();
    final CType rightType = rightSide.getExpressionType().getCanonicalType();

    if (leftType instanceof CVoidType && rightType instanceof CVoidType) {
      return null;
    }

    boolean equalTypes = leftType.equals(rightType);

    FluentIterable<Class<? extends CType>> acceptedTypes =
        FluentIterable.from(
            ImmutableList.of(CSimpleType.class, CArrayType.class, CPointerType.class));

    boolean leftIsAccepted =
        equalTypes
            || acceptedTypes.anyMatch(
                acceptedTypeClass -> acceptedTypeClass.isAssignableFrom(leftType.getClass()));

    boolean rightIsAccepted =
        equalTypes
            || acceptedTypes.anyMatch(
                acceptedTypeClass -> acceptedTypeClass.isAssignableFrom(rightType.getClass()));

    if (leftType instanceof CSimpleType && !rightIsAccepted) {
      if (rightType instanceof CVoidType) {
        if (rightSide instanceof CPointerExpression) {
          rightSide = castDereferencedPointerType((CPointerExpression) rightSide, leftType);
        } else {
          return null;
        }
      } else {
        rightSide = new CCastExpression(rightSide.getFileLocation(), leftType, rightSide);
      }
    } else if (!leftIsAccepted && rightType instanceof CSimpleType) {
      if (leftType instanceof CVoidType) {
        if (leftSide instanceof CPointerExpression) {
          leftSide = castDereferencedPointerType((CPointerExpression) leftSide, rightType);
        } else {
          return null;
        }
      } else {
        leftSide = new CCastExpression(leftSide.getFileLocation(), rightType, leftSide);
      }
    }

    CBinaryExpression assumption;
    try {
      if (rightSide instanceof CFloatLiteralExpression floatLiteral
          && floatLiteral.getValue().isNan()) {
        // NaN can be encoded as leftSide != leftSide (which is ONLY true for NaN)
        CBinaryExpression eqExpr =
            pBuilder.buildBinaryExpressionUnchecked(
                leftSide, leftSide, CBinaryExpression.BinaryOperator.EQUALS);
        assumption = pBuilder.negateExpressionAndSimplify(eqExpr);

      } else if (leftSide instanceof CFloatLiteralExpression floatLiteral
          && floatLiteral.getValue().isNan()) {
        // NaN can be encoded as rightSide != rightSide (which is ONLY true for NaN)
        CBinaryExpression eqExpr =
            pBuilder.buildBinaryExpressionUnchecked(
                rightSide, rightSide, CBinaryExpression.BinaryOperator.EQUALS);
        assumption = pBuilder.negateExpressionAndSimplify(eqExpr);

      } else {
        // normal assignments using equality
        assumption =
            pBuilder.buildBinaryExpressionUnchecked(
                leftSide, rightSide, CBinaryExpression.BinaryOperator.EQUALS);
      }

    } catch (UnrecognizedCodeException e1) {
      throw new IllegalArgumentException(e1);
    }

    return new CExpressionStatement(assumption.getFileLocation(), assumption);
  }

  private CExpression castDereferencedPointerType(
      CPointerExpression pDereference, final CType pTargetType) {
    CExpression inner = pDereference.getOperand();
    if (inner.getExpressionType().equals(pTargetType)) {
      return pDereference;
    }
    inner =
        new CCastExpression(
            pDereference.getFileLocation(), new CPointerType(false, false, pTargetType), inner);
    return new CPointerExpression(pDereference.getFileLocation(), pTargetType, inner);
  }

  private CExpression getLeftAssumptionFromLhs(CLeftHandSide pLValue) {

    // We represent structs and arrays as addresses. When we transform those to
    // assumptions, we have to resolve them.

    CType type = pLValue.getExpressionType().getCanonicalType();

    if (isStructOrUnionType(type) || type instanceof CArrayType) {
      if (pLValue instanceof CPointerExpression cPointerExpression) {
        return cPointerExpression.getOperand();
      }
      CUnaryExpression unaryExpression =
          new CUnaryExpression(
              pLValue.getFileLocation(),
              new CPointerType(false, false, type),
              pLValue,
              CUnaryExpression.UnaryOperator.AMPER);
      return unaryExpression;
    } else {
      return pLValue;
    }
  }

  private @Nullable Object getValueObject(
      CSimpleDeclaration pDcl, String pFunctionName, ConcreteState pConcreteState) {
    return new LModelValueVisitor(pFunctionName, pConcreteState).handleVariableDeclaration(pDcl);
  }

  private boolean isStructOrUnionType(CType rValueType) {

    rValueType = rValueType.getCanonicalType();

    if (rValueType instanceof CElaboratedType type) {
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    return rValueType instanceof CCompositeType type
        && type.getKind() != CComplexType.ComplexTypeKind.ENUM;
  }

  // TODO Move to Utility?
  private @Nullable FieldReference getFieldReference(
      CFieldReference pIastFieldReference, String pFunctionName) {

    List<String> fieldNameList = new ArrayList<>();
    CFieldReference reference = pIastFieldReference;
    fieldNameList.add(FIRST, reference.getFieldName());

    while (reference.getFieldOwner() instanceof CFieldReference
        && !reference.isPointerDereference()) {
      reference = (CFieldReference) reference.getFieldOwner();
      fieldNameList.add(FIRST, reference.getFieldName());
    }

    if (reference.getFieldOwner() instanceof CIdExpression idExpression) {
      if (ForwardingTransferRelation.isGlobal(idExpression)) {
        return new FieldReference(idExpression.getName(), fieldNameList);
      } else {
        return new FieldReference(idExpression.getName(), pFunctionName, fieldNameList);
      }
    } else {
      return null;
    }
  }

  private class LModelValueVisitor implements CLeftHandSideVisitor<@Nullable Object, NoException> {

    private final String functionName;
    private final AddressValueVisitor addressVisitor;
    private final ConcreteState concreteState;

    LModelValueVisitor(String pFunctionName, ConcreteState pConcreteState) {
      functionName = pFunctionName;
      addressVisitor = new AddressValueVisitor(this);
      concreteState = pConcreteState;
    }

    private Address getAddress(CSimpleDeclaration dcl) {
      return addressVisitor.getAddress(dcl);
    }

    private @Nullable Number evaluateNumericalValue(CExpression exp) {

      Value addressV;
      try {
        ModelExpressionValueVisitor v =
            new ModelExpressionValueVisitor(
                functionName, machineModel, new LogManagerWithoutDuplicates(logger));
        addressV = exp.accept(v);
      } catch (ArithmeticException e) {
        logger.logDebugException(e);
        logger.log(
            Level.WARNING,
            "The expression "
                + exp.toASTString()
                + "could not be correctly evaluated while calculating the concrete values "
                + "in the counterexample path.");
        return null;
      } catch (UnrecognizedCodeException e1) {
        throw new IllegalArgumentException(e1);
      }

      if (addressV.isUnknown() && !addressV.isNumericValue()) {
        return null;
      }

      return addressV.asNumericValue().getNumber();
    }

    private Address evaluateNumericalValueAsAddress(CExpression exp) {
      Number result = evaluateNumericalValue(exp);
      return result == null ? Address.getUnknownAddress() : Address.valueOf(result);
    }

    /*This method evaluates the address of the lValue, not the address the expression evaluates to*/
    private Address evaluateAddress(CLeftHandSide pExp) {
      return pExp.accept(addressVisitor);
    }

    @Override
    public @Nullable Object visit(CArraySubscriptExpression pIastArraySubscriptExpression) {

      Address valueAddress = evaluateAddress(pIastArraySubscriptExpression);
      if (valueAddress.isUnknown()) {
        return null;
      }

      CType type = pIastArraySubscriptExpression.getExpressionType().getCanonicalType();

      /*The evaluation of an array or a struct is its address*/
      if (type instanceof CArrayType || isStructOrUnionType(type)) {
        if (valueAddress.isSymbolic()) {
          return null;
        }
        return valueAddress.getAddressValue();
      }

      return concreteState.getValueFromMemory(pIastArraySubscriptExpression, valueAddress);
    }

    @Override
    public @Nullable Object visit(CFieldReference pIastFieldReference) {

      Address address = evaluateAddress(pIastFieldReference);
      if (address.isUnknown()) {
        return lookupReference(pIastFieldReference);
      }

      CType type = pIastFieldReference.getExpressionType().getCanonicalType();

      // The evaluation of an array or a struct is its address
      if (type instanceof CArrayType || isStructOrUnionType(type)) {
        if (address.isSymbolic()) {
          return null;
        }
        return address.getAddressValue();
      }

      @Nullable Object value = concreteState.getValueFromMemory(pIastFieldReference, address);
      if (value == null) {
        return lookupReference(pIastFieldReference);
      }

      return value;
    }

    private @Nullable Object lookupReference(CFieldReference pIastFieldReference) {

      /* Fieldreferences are sometimes represented as variables,
      e.g. a.b.c in main is main::a$b$c */
      FieldReference fieldReference = getFieldReference(pIastFieldReference, functionName);

      if (fieldReference != null && concreteState.hasValueForLeftHandSide(fieldReference)) {

        return concreteState.getVariableValue(fieldReference);
      }

      return null;
    }

    private Optional<BigInteger> getFieldOffset(CFieldReference fieldReference) {
      CType fieldOwnerType = fieldReference.getFieldOwner().getExpressionType().getCanonicalType();
      return AssumptionToEdgeAllocator.getFieldOffset(
          fieldOwnerType, fieldReference.getFieldName(), machineModel);
    }

    @Override
    public Object visit(CIdExpression pCIdExpression) {

      CSimpleDeclaration dcl = pCIdExpression.getDeclaration();

      Address address = evaluateAddress(pCIdExpression);

      if (address.isUnknown()) {
        return lookupVariable(dcl);
      }

      CType type = pCIdExpression.getExpressionType().getCanonicalType();

      // The evaluation of an array or a struct is its address
      if (type instanceof CArrayType || isStructOrUnionType(type)) {
        if (address.isSymbolic()) {
          return lookupVariable(dcl);
        }
        return address.getAddressValue();
      }

      @Nullable Object value = concreteState.getValueFromMemory(pCIdExpression, address);

      if (value == null) {
        return lookupVariable(dcl);
      }

      return value;
    }

    private @Nullable Object handleVariableDeclaration(CSimpleDeclaration pDcl) {

      // These declarations don't evaluate to a value //TODO Assumption
      if (pDcl instanceof CFunctionDeclaration || pDcl instanceof CTypeDeclaration) {
        return null;
      }

      CIdExpression representingIdExpression = new CIdExpression(pDcl.getFileLocation(), pDcl);
      return this.visit(representingIdExpression);
    }

    private @Nullable Object lookupVariable(CSimpleDeclaration pVarDcl) {
      IDExpression varName = getIDExpression(pVarDcl);

      if (concreteState.hasValueForLeftHandSide(varName)) {
        return concreteState.getVariableValue(varName);
      } else {
        return null;
      }
    }

    // TODO Move to util
    private IDExpression getIDExpression(CSimpleDeclaration pDcl) {

      // TODO use original name?
      String name = pDcl.getName();

      if (pDcl instanceof CDeclaration cDeclaration && cDeclaration.isGlobal()) {
        return new IDExpression(name);
      } else {
        return new IDExpression(name, functionName);
      }
    }

    @Override
    public @Nullable Object visit(CPointerExpression pPointerExpression) {

      /*Quick jump to the necessary method.
       * the address of a dereference is the evaluation of its operand*/
      Address address = evaluateAddress(pPointerExpression);

      if (address.isUnknown()) {
        return null;
      }

      CType type = pPointerExpression.getExpressionType().getCanonicalType();

      /*The evaluation of an array or a struct is its address*/
      if (type instanceof CArrayType || isStructOrUnionType(type)) {
        if (address.isSymbolic()) {
          return null;
        }

        return address.getAddressValue();
      }

      return concreteState.getValueFromMemory(pPointerExpression, address);
    }

    boolean isStructOrUnionType(CType rValueType) {

      rValueType = rValueType.getCanonicalType();

      if (rValueType instanceof CElaboratedType type) {
        return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
      }

      return rValueType instanceof CCompositeType type
          && type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    private class AddressValueVisitor implements CLeftHandSideVisitor<Address, NoException> {

      private final LModelValueVisitor valueVisitor;

      AddressValueVisitor(LModelValueVisitor pValueVisitor) {
        valueVisitor = pValueVisitor;
      }

      Address getAddress(CSimpleDeclaration dcl) {
        IDExpression name = getIDExpression(dcl);
        if (concreteState.hasAddressOfVariable(name)) {
          return concreteState.getVariableAddress(name);
        }
        return Address.getUnknownAddress();
      }

      @Override
      public Address visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
        CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();

        // This works because arrays and structs evaluate to their addresses
        Address address = evaluateNumericalValueAsAddress(arrayExpression);

        if (address.isUnknown()
            || address.isSymbolic()
            || !pIastArraySubscriptExpression.getExpressionType().hasKnownConstantSize()) {
          return Address.getUnknownAddress();
        }

        CExpression subscriptCExpression = pIastArraySubscriptExpression.getSubscriptExpression();

        Number subscriptValueNumber = evaluateNumericalValue(subscriptCExpression);

        if (subscriptValueNumber == null) {
          return Address.getUnknownAddress();
        }

        final BigDecimal subscriptValue;
        if (subscriptValueNumber instanceof Rational rational) {
          subscriptValue =
              new BigDecimal(rational.getNum()).divide(new BigDecimal(rational.getDen()));
        } else {
          subscriptValue = new BigDecimal(subscriptValueNumber.toString());
        }

        BigDecimal typeSize =
            new BigDecimal(
                machineModel.getSizeof(
                    pIastArraySubscriptExpression.getExpressionType().getCanonicalType()));

        BigDecimal subscriptOffset = subscriptValue.multiply(typeSize);

        return address.addOffset(subscriptOffset);
      }

      @Override
      public Address visit(CFieldReference pIastFieldReference) {
        CExpression fieldOwner = pIastFieldReference.getFieldOwner();

        // This works because arrays and structs evaluate to their addresses.
        Address fieldOwnerAddress = evaluateNumericalValueAsAddress(fieldOwner);
        if (fieldOwnerAddress.isUnknown() || fieldOwnerAddress.isSymbolic()) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        Optional<BigInteger> fieldOffset = getFieldOffset(pIastFieldReference);
        if (!fieldOffset.isPresent()) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        Address address = fieldOwnerAddress.addOffset(fieldOffset.orElseThrow());
        if (address.isUnknown()) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        return address;
      }

      private Address lookupReferenceAddress(CFieldReference pIastFieldReference) {
        /* Fieldreferences are sometimes represented as variables,
        e.g. a.b.c in main is main::a$b$c */
        FieldReference fieldReferenceName = getFieldReference(pIastFieldReference, functionName);

        if (fieldReferenceName != null) {
          if (concreteState.hasAddressOfVariable(fieldReferenceName)) {
            return concreteState.getVariableAddress(fieldReferenceName);
          }
        }

        return Address.getUnknownAddress();
      }

      @Override
      public Address visit(CIdExpression pIastIdExpression) {
        return getAddress(pIastIdExpression.getDeclaration());
      }

      @Override
      public Address visit(CPointerExpression pPointerExpression) {
        /*The address of a pointer dereference is the evaluation of its operand*/
        return valueVisitor.evaluateNumericalValueAsAddress(pPointerExpression.getOperand());
      }

      @Override
      public Address visit(CComplexCastExpression pComplexCastExpression) {
        // TODO Implement complex Cast Expression
        return Address.getUnknownAddress();
      }
    }

    private class ModelExpressionValueVisitor extends AbstractExpressionValueVisitor {

      ModelExpressionValueVisitor(
          String pFunctionName, MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {
        super(pFunctionName, pMachineModel, pLogger);
      }

      @Override
      public Value visit(CCastExpression cast) throws UnrecognizedCodeException {

        if (concreteState
            .getAnalysisConcreteExpressionEvaluation()
            .shouldEvaluateExpressionWithThisEvaluator(cast)) {
          Value op = cast.getOperand().accept(this);

          if (op.isUnknown()) {
            return op;
          }

          return concreteState.getAnalysisConcreteExpressionEvaluation().evaluate(cast, op);
        }

        return super.visit(cast);
      }

      @Override
      public Value visit(CBinaryExpression binaryExp) throws UnrecognizedCodeException {

        if (concreteState
            .getAnalysisConcreteExpressionEvaluation()
            .shouldEvaluateExpressionWithThisEvaluator(binaryExp)) {
          Value op1 = binaryExp.getOperand1().accept(this);

          if (op1.isUnknown()) {
            return op1;
          }

          Value op2 = binaryExp.getOperand2().accept(this);

          if (op2.isUnknown()) {
            return op2;
          }

          return concreteState
              .getAnalysisConcreteExpressionEvaluation()
              .evaluate(binaryExp, op1, op2);
        }

        CExpression lVarInBinaryExp = binaryExp.getOperand1();
        CExpression rVarInBinaryExp = binaryExp.getOperand2();
        CType lVarInBinaryExpType = lVarInBinaryExp.getExpressionType().getCanonicalType();
        CType rVarInBinaryExpType = rVarInBinaryExp.getExpressionType().getCanonicalType();

        boolean lVarIsAddress =
            lVarInBinaryExpType instanceof CPointerType
                || lVarInBinaryExpType instanceof CArrayType;
        boolean rVarIsAddress =
            rVarInBinaryExpType instanceof CPointerType
                || rVarInBinaryExpType instanceof CArrayType;

        CExpression address = null;
        CExpression pointerOffset = null;
        CType addressType = null;

        if (lVarIsAddress && rVarIsAddress) {
          return Value.UnknownValue.getInstance();
        } else if (lVarIsAddress) {
          address = lVarInBinaryExp;
          pointerOffset = rVarInBinaryExp;
          addressType = lVarInBinaryExpType;
        } else if (rVarIsAddress) {
          address = rVarInBinaryExp;
          pointerOffset = lVarInBinaryExp;
          addressType = rVarInBinaryExpType;
        } else {
          if (assumeLinearArithmetics) {
            return switch (binaryExp.getOperator()) {
              case MULTIPLY -> {
                // Multiplication with constants is sometimes supported
                if (allowMultiplicationWithConstants
                    && (lVarInBinaryExp instanceof ALiteralExpression
                        || rVarInBinaryExp instanceof ALiteralExpression)) {
                  yield super.visit(binaryExp);
                }
                yield Value.UnknownValue.getInstance();
              }
              case DIVIDE, MODULO -> {
                // Division and modulo with constants are sometimes supported
                if (allowDivisionAndModuloByConstants
                    && rVarInBinaryExp instanceof ALiteralExpression) {
                  yield super.visit(binaryExp);
                }
                yield Value.UnknownValue.getInstance();
              }
              case BINARY_AND, BINARY_OR, BINARY_XOR, SHIFT_LEFT, SHIFT_RIGHT ->
                  Value.UnknownValue.getInstance();
              default -> super.visit(binaryExp);
            };
          } else {
            return super.visit(binaryExp);
          }
        }

        BinaryOperator binaryOperator = binaryExp.getOperator();

        CType elementType =
            addressType instanceof CPointerType
                ? ((CPointerType) addressType).getType().getCanonicalType()
                : ((CArrayType) addressType).getType().getCanonicalType();

        return switch (binaryOperator) {
          case PLUS, MINUS -> {
            Value addressValueV = address.accept(this);

            Value offsetValueV = pointerOffset.accept(this);

            if (addressValueV.isUnknown()
                || offsetValueV.isUnknown()
                || !addressValueV.isNumericValue()
                || !offsetValueV.isNumericValue()) {
              yield Value.UnknownValue.getInstance();
            }

            Number addressValueNumber = addressValueV.asNumericValue().getNumber();
            BigDecimal addressValue = new BigDecimal(addressValueNumber.toString());
            // Because address and offset value may be interchanged, use BigDecimal for both
            Number offsetValueNumber = offsetValueV.asNumericValue().getNumber();
            BigDecimal offsetValue = new BigDecimal(offsetValueNumber.toString());
            BigDecimal typeSize = new BigDecimal(machineModel.getSizeof(elementType));
            BigDecimal pointerOffsetValue = offsetValue.multiply(typeSize);

            yield switch (binaryOperator) {
              case PLUS -> new NumericValue(addressValue.add(pointerOffsetValue));
              case MINUS -> {
                if (lVarIsAddress) {
                  yield new NumericValue(addressValue.subtract(pointerOffsetValue));
                } else {
                  throw new UnrecognizedCodeException(
                      "Expected pointer arithmetic "
                          + " with + or - but found "
                          + binaryExp.toASTString(),
                      binaryExp);
                }
              }
              default -> throw new AssertionError();
            };
          }
          default -> Value.UnknownValue.getInstance();
        };
      }

      @Override
      public Value visit(CUnaryExpression pUnaryExpression) throws UnrecognizedCodeException {

        if (concreteState
            .getAnalysisConcreteExpressionEvaluation()
            .shouldEvaluateExpressionWithThisEvaluator(pUnaryExpression)) {

          Value operand = pUnaryExpression.getOperand().accept(this);

          if (operand.isUnknown()
              && (pUnaryExpression.getOperator() == UnaryOperator.MINUS
                  || pUnaryExpression.getOperator() == UnaryOperator.TILDE)) {
            return operand;
          }

          return concreteState
              .getAnalysisConcreteExpressionEvaluation()
              .evaluate(pUnaryExpression, operand);
        }

        if (pUnaryExpression.getOperator() == UnaryOperator.AMPER) {
          return handleAmper(pUnaryExpression.getOperand());
        }

        return super.visit(pUnaryExpression);
      }

      private Value handleAmper(CExpression pOperand) {
        if (pOperand instanceof CLeftHandSide cLeftHandSide) {

          Address address = evaluateAddress(cLeftHandSide);

          if (address.isConcrete()) {
            return new NumericValue(address.getAddressValue());
          }
        } else if (pOperand instanceof CCastExpression cCastExpression) {
          return handleAmper(cCastExpression.getOperand());
        }

        return Value.UnknownValue.getInstance();
      }

      @Override
      protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
          throws UnrecognizedCodeException {
        Object value = LModelValueVisitor.this.visit(pCPointerExpression);

        if (!(value instanceof Number number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue(number);
      }

      @Override
      protected Value evaluateCIdExpression(CIdExpression pCIdExpression)
          throws UnrecognizedCodeException {
        Object value = LModelValueVisitor.this.visit(pCIdExpression);
        if (!(value instanceof Number number)) {
          return Value.UnknownValue.getInstance();
        }
        return new NumericValue(number);
      }

      @Override
      protected Value evaluateJIdExpression(JIdExpression pVarName) {
        return Value.UnknownValue.getInstance();
      }

      @Override
      protected Value evaluateCFieldReference(CFieldReference pLValue)
          throws UnrecognizedCodeException {
        Object value = LModelValueVisitor.this.visit(pLValue);

        if (!(value instanceof Number number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue(number);
      }

      @Override
      protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
          throws UnrecognizedCodeException {
        Object value = LModelValueVisitor.this.visit(pLValue);

        if (!(value instanceof Number number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue(number);
      }

      @Override
      public Value visit(JClassLiteralExpression pJClassLiteralExpression) throws NoException {
        return Value.UnknownValue.getInstance();
      }
    }

    @Override
    public Object visit(CComplexCastExpression pComplexCastExpression) {
      // TODO Auto-generated method stub
      return null;
    }
  }

  private class ValueLiteralsVisitor extends DefaultCTypeVisitor<ValueLiterals, NoException> {

    private final Object value;
    private final CExpression exp;
    private final ConcreteState concreteState;

    ValueLiteralsVisitor(Object pValue, CExpression pExp, ConcreteState pConcreteState) {
      value = pValue;
      exp = pExp;
      concreteState = pConcreteState;
    }

    @Override
    public ValueLiterals visitDefault(CType pT) {
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CPointerType pointerType) {
      Address address = Address.valueOf(value);
      if (address.isUnknown()) {
        return createUnknownValueLiterals();
      }
      ValueLiteral valueLiteral = ExplicitValueLiteral.valueOf(address, machineModel);
      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);
      pointerType.accept(new ValueLiteralVisitor(address, valueLiterals, exp));
      return valueLiterals;
    }

    @Override
    public ValueLiterals visit(CArrayType arrayType) {
      Address address = Address.valueOf(value);
      if (address.isUnknown()) {
        return createUnknownValueLiterals();
      }

      ValueLiteral valueLiteral = ExplicitValueLiteral.valueOf(address, machineModel);
      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);
      arrayType.accept(new ValueLiteralVisitor(address, valueLiterals, exp));
      return valueLiterals;
    }

    @Override
    public ValueLiterals visit(CElaboratedType pT) {
      CType realType = pT.getRealType();
      if (realType != null) {
        return realType.accept(this);
      }
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CEnumType pT) {

      /*We don't need to resolve enum types */
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CFunctionType pT) {

      // TODO Implement function resolving for comments
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CSimpleType simpleType) {
      return new ValueLiterals(getValueLiteral(simpleType, value));
    }

    @Override
    public ValueLiterals visit(CBitFieldType pCBitFieldType) {
      return pCBitFieldType.getType().accept(this);
    }

    @Override
    public ValueLiterals visit(CProblemType pT) {
      return createUnknownValueLiterals();
    }

    @Override
    public ValueLiterals visit(CTypedefType pT) {
      return pT.getRealType().accept(this);
    }

    @Override
    public ValueLiterals visit(CCompositeType compType) {

      if (compType.getKind() == ComplexTypeKind.ENUM) {
        return createUnknownValueLiterals();
      }

      Address address = Address.valueOf(value);

      if (address.isUnknown()) {
        return createUnknownValueLiterals();
      }

      ValueLiteral valueLiteral = ExplicitValueLiteral.valueOf(address, machineModel);
      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);
      compType.accept(new ValueLiteralVisitor(address, valueLiterals, exp));
      return valueLiterals;
    }

    /**
     * Convert an integer value to floating point
     *
     * <p>Will fail and return the original value if converting would lose precision.
     */
    private Number convertToFloat(Number pNumber, CType pType) {
      // Calculate the target precision
      Format precision = FloatValue.Format.fromCType(machineModel, pType);

      // Convert to floating-point
      BigInteger integerValue = new NumericValue(pNumber).bigIntegerValue();
      FloatValue floatValue = FloatValue.fromInteger(precision, integerValue);

      // Check that no precision was lost
      Optional<BigInteger> maybeInteger = floatValue.toInteger();
      if (maybeInteger.isEmpty() || !maybeInteger.orElseThrow().equals(integerValue)) {
        return pNumber;
      } else {
        return floatValue;
      }
    }

    ValueLiteral getValueLiteral(CSimpleType pSimpleType, Object pValue) {
      CBasicType basicType = pSimpleType.getCanonicalType().getType();
      if (pValue instanceof FloatingPointNumber pFloatingPointNumber) {
        // Unlike other floating point values, FloatingPointNumbers do not implement the Number
        // interface and need to be converted first
        pValue = FloatValue.fromFloatingPointNumber(pFloatingPointNumber);
      }
      if (pValue instanceof Number pNumber) {
        NumericValue numericValue = new NumericValue(pNumber);
        switch (basicType) {
          case BOOL, CHAR, INT, INT128 -> {
            Preconditions.checkArgument(
                numericValue.hasIntegerType(),
                "Expecting an integer value, but `%s` has type `%s`.",
                pNumber,
                pNumber.getClass().getSimpleName());
            return handleIntegerNumbers(pNumber, pSimpleType);
          }
          case FLOAT, DOUBLE, FLOAT128 -> {
            // The value may have any type that implements the Number interface. We accept integers,
            // floats and rationals. The type check here makes sure that the class is known to our
            // implementation
            Preconditions.checkArgument(
                numericValue.hasFloatType()
                    || numericValue.hasIntegerType()
                    || numericValue.getNumber() instanceof Rational,
                "Expecting a floating-point value, but `%s` has unknown type `%s`.",
                pNumber,
                pNumber.getClass().getSimpleName());

            // If we were handed an integer, try converting it to float first
            if (numericValue.hasIntegerType()) {
              pNumber = convertToFloat(pNumber, pSimpleType);
            }
            return handleFloatingPointNumbers(pNumber, pSimpleType);
          }
          default ->
              throw new AssertionError(String.format("Value has unknown type `%s`", basicType));
        }
      }
      throw new AssertionError("Values must implement the Number interface.");
    }

    private ValueLiterals createUnknownValueLiterals() {
      return new ValueLiterals();
    }

    private ValueLiteral handleFloatingPointNumbers(Object pValue, CSimpleType pType) {
      if (pValue instanceof Rational rationalValue) {
        FloatValue.Format format = FloatValue.Format.fromCType(machineModel, pType);
        return ExplicitValueLiteral.valueOf(
            FloatValue.fromRational(format, rationalValue), machineModel, pType);
      } else if (pValue instanceof Double doubleValue) {
        return ExplicitValueLiteral.valueOf(
            FloatValue.fromDouble(doubleValue), machineModel, pType);
      } else if (pValue instanceof Float floatValue) {
        return ExplicitValueLiteral.valueOf(FloatValue.fromFloat(floatValue), machineModel, pType);
      } else if (pValue instanceof FloatValue floatValue) {
        return ExplicitValueLiteral.valueOf(floatValue, machineModel, pType);
      } else if (pValue instanceof FloatingPointNumber floatingPointNumber) {
        return ExplicitValueLiteral.valueOf(
            FloatValue.fromFloatingPointNumber(floatingPointNumber), machineModel, pType);
      }
      throw new UnsupportedOperationException(
          String.format(
              "Can't handle the value `%s` of type `%s` as a floating point number.",
              pValue, pValue.getClass().getSimpleName()));
    }

    void resolveStruct(
        CType type, ValueLiterals pValueLiterals, CIdExpression pOwner, String pFunctionName) {

      ValueLiteralStructResolver v =
          new ValueLiteralStructResolver(pValueLiterals, pFunctionName, pOwner);
      type.accept(v);
    }

    private ValueLiteral handleIntegerNumbers(Object pValue, CSimpleType pType) {
      if (pValue instanceof Number pNumber) {
        BigInteger beforeCast = new NumericValue(pNumber).getIntegerValue();
        return handlePotentialIntegerOverflow(beforeCast, pType);
      }
      throw new AssertionError("Values must implement the Number interface.");
    }

    /**
     * Creates a value literal for the given value or computes its wrap-around if it does not fit
     * into the specified type.
     *
     * @param pIntegerValue the value.
     * @param pType the type.
     * @return the value literal.
     */
    private ValueLiteral handlePotentialIntegerOverflow(
        BigInteger pIntegerValue, CSimpleType pType) {

      BigInteger lowerInclusiveBound = machineModel.getMinimalIntegerValue(pType);
      BigInteger upperInclusiveBound = machineModel.getMaximalIntegerValue(pType);

      assert lowerInclusiveBound.compareTo(upperInclusiveBound) < 0;

      if (pIntegerValue.compareTo(lowerInclusiveBound) < 0
          || pIntegerValue.compareTo(upperInclusiveBound) > 0) {
        if (assumeLinearArithmetics) {
          return UnknownValueLiteral.getInstance();
        }
        LogManagerWithoutDuplicates logManager =
            logger instanceof LogManagerWithoutDuplicates logManagerWithoutDuplicates
                ? logManagerWithoutDuplicates
                : new LogManagerWithoutDuplicates(logger);
        Value castValue =
            AbstractExpressionValueVisitor.castCValue(
                new NumericValue(pIntegerValue),
                pType,
                machineModel,
                logManager,
                FileLocation.DUMMY);
        if (castValue.isUnknown()) {
          return UnknownValueLiteral.getInstance();
        }

        Number number = castValue.asNumericValue().getNumber();
        final BigInteger valueAsBigInt;
        if (number instanceof BigInteger bigInteger) {
          valueAsBigInt = bigInteger;
        } else {
          valueAsBigInt = BigInteger.valueOf(number.longValue());
        }
        pType = enlargeTypeIfValueIsMinimalValue(pType, valueAsBigInt);
        return ExplicitValueLiteral.valueOf(valueAsBigInt, pType);
      }

      return ExplicitValueLiteral.valueOf(pIntegerValue, pType);
    }

    private CSimpleType enlargeTypeIfValueIsMinimalValue(
        CSimpleType pType, final BigInteger valueAsBigInt) {
      // In C there are no negative literals, so to represent the minimal value of an integer
      // type, we need that number as positive literal of the next larger type,
      // and then negate it.
      // For example for LONG_MIN we want to have -9223372036854775808UL, so the literal is
      // of type unsigned long and negated. This is only important when exporting the value
      // e.g. inside a witness, since EclipseCDT will not like -9223372036854775808L.
      if (valueAsBigInt.abs().compareTo(machineModel.getMaximalIntegerValue(pType)) > 0
          && valueAsBigInt.compareTo(BigInteger.ZERO) < 0
          && pType.getType().isIntegerType()) {
        while (valueAsBigInt.abs().compareTo(machineModel.getMaximalIntegerValue(pType)) > 0
            && !nextLargerIntegerTypeIfPossible(pType).equals(pType)) {
          pType = nextLargerIntegerTypeIfPossible(pType);
        }
      }
      return pType;
    }

    private CSimpleType nextLargerIntegerTypeIfPossible(CSimpleType pType) {
      if (pType.hasSignedSpecifier()) {
        return new CSimpleType(
            pType.isConst(),
            pType.isVolatile(),
            pType.getType(),
            pType.hasLongSpecifier(),
            pType.hasShortSpecifier(),
            false,
            true,
            pType.hasComplexSpecifier(),
            pType.hasImaginarySpecifier(),
            pType.hasLongLongSpecifier());
      } else {
        if (pType.getType() == CBasicType.INT) {
          if (pType.hasShortSpecifier()) {
            return CNumericTypes.SIGNED_INT;
          } else if (pType.hasLongSpecifier()) {
            return CNumericTypes.SIGNED_LONG_LONG_INT;
          } else if (pType.hasLongLongSpecifier()) {
            // fall through, this is already the largest type
          } else {
            // if it had neither specifier it is a plain (unsigned) int
            return CNumericTypes.SIGNED_LONG_INT;
          }
        }

        // just log and do not throw an exception in order to not break things
        logger.logf(Level.WARNING, "Cannot find next larger type for %s", pType);
        return pType;
      }
    }

    /** Resolves all subexpressions that can be resolved. Stops at duplicate memory location. */
    private class ValueLiteralVisitor extends DefaultCTypeVisitor<Void, NoException> {

      /*Contains references already visited, to avoid descending indefinitely.
       *Shares a reference with all instanced Visitors resolving the given type.*/
      private final Set<Pair<CType, Address>> visited;

      /*
       * Contains the address of the super type of the visited type.
       *
       */
      private final Address address;
      private final ValueLiterals valueLiterals;

      private final CExpression subExpression;

      ValueLiteralVisitor(Address pAddress, ValueLiterals pValueLiterals, CExpression pSubExp) {
        address = pAddress;
        valueLiterals = pValueLiterals;
        visited = new HashSet<>();
        subExpression = pSubExp;
      }

      private ValueLiteralVisitor(
          Address pAddress,
          ValueLiterals pValueLiterals,
          CExpression pSubExp,
          Set<Pair<CType, Address>> pVisited) {
        address = pAddress;
        valueLiterals = pValueLiterals;
        visited = pVisited;
        subExpression = pSubExp;
      }

      @Override
      public @Nullable Void visitDefault(CType pT) {
        return null;
      }

      @Override
      public @Nullable Void visit(CTypedefType pT) {
        return pT.getRealType().accept(this);
      }

      @Override
      public @Nullable Void visit(CElaboratedType pT) {
        CType realType = pT.getRealType();
        return realType == null ? null : realType.getCanonicalType().accept(this);
      }

      @Override
      public @Nullable Void visit(CEnumType pT) {
        return null;
      }

      @Override
      public @Nullable Void visit(CBitFieldType pCBitFieldType) {
        return pCBitFieldType.getType().accept(this);
      }

      @Override
      public @Nullable Void visit(CCompositeType compType) {

        // TODO handle enums and unions

        if (compType.getKind() == ComplexTypeKind.STRUCT) {
          handleStruct(compType);
        }

        return null;
      }

      private void handleStruct(CCompositeType pCompType) {
        Address fieldAddress = address;
        if (!fieldAddress.isConcrete()) {
          return;
        }

        Map<CCompositeTypeMemberDeclaration, BigInteger> bitOffsets =
            machineModel.getAllFieldOffsetsInBits(pCompType);

        for (Map.Entry<CCompositeTypeMemberDeclaration, BigInteger> memberBitOffset :
            bitOffsets.entrySet()) {
          CCompositeTypeMemberDeclaration memberType = memberBitOffset.getKey();
          Optional<BigInteger> memberOffset = bitsToByte(memberBitOffset.getValue(), machineModel);
          // TODO this looses values of bit fields
          if (memberOffset.isPresent()) {
            handleMemberField(memberType, address.addOffset(memberOffset.orElseThrow()));
          }
        }
      }

      private void handleMemberField(CCompositeTypeMemberDeclaration pType, Address fieldAddress) {
        CType expectedType = pType.getType().getCanonicalType();

        assert isStructOrUnionType(subExpression.getExpressionType().getCanonicalType());

        CExpression subExp;
        boolean isPointerDeref;

        if (subExpression instanceof CPointerExpression cPointerExpression) {
          // *a.b <=> a->b
          subExp = cPointerExpression.getOperand();
          isPointerDeref = true;
        } else {
          subExp = subExpression;
          isPointerDeref = false;
        }

        CFieldReference fieldReference =
            new CFieldReference(
                subExp.getFileLocation(), expectedType, pType.getName(), subExp, isPointerDeref);

        @Nullable Object fieldValue;

        // Arrays and structs are represented as addresses
        if (expectedType instanceof CArrayType || isStructOrUnionType(expectedType)) {
          fieldValue = fieldAddress;
        } else {
          fieldValue = concreteState.getValueFromMemory(fieldReference, fieldAddress);
        }

        if (fieldValue == null) {
          return;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = Address.getUnknownAddress();

        if (expectedType instanceof CSimpleType cSimpleType) {
          valueLiteral = getValueLiteral(cSimpleType, fieldValue);
        } else {
          valueAddress = Address.valueOf(fieldValue);

          if (valueAddress.isUnknown()) {
            return;
          }

          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress, machineModel);
        }

        Pair<CType, Address> visits = Pair.of(expectedType, fieldAddress);

        if (visited.contains(visits)) {
          return;
        }

        if (!valueLiteral.isUnknown()) {
          visited.add(visits);
          valueLiterals.addSubExpressionValueLiteral(
              new SubExpressionValueLiteral(valueLiteral, fieldReference));
        }

        if (valueAddress != null) {
          ValueLiteralVisitor v =
              new ValueLiteralVisitor(valueAddress, valueLiterals, fieldReference, visited);
          expectedType.accept(v);
        }
      }

      @Override
      public @Nullable Void visit(CArrayType arrayType) {
        if (!address.isConcrete()) {
          return null;
        }
        // For the bound check in handleArraySubscript() we need a statically known size,
        // otherwise we would loop infinitely.
        // TODO in principle we could extract the runtime size from the state?
        if (!arrayType.hasKnownConstantSize()) {
          return null;
        }

        CType expectedType = arrayType.getType().getCanonicalType();
        int subscript = 0;
        boolean memoryHasValue = true;
        while (memoryHasValue) {
          memoryHasValue = handleArraySubscript(address, subscript, expectedType, arrayType);
          subscript++;
        }
        return null;
      }

      private boolean handleArraySubscript(
          Address pArrayAddress, int pSubscript, CType pExpectedType, CArrayType pArrayType) {
        BigInteger typeSize = machineModel.getSizeof(pExpectedType);
        BigInteger subscriptOffset = BigInteger.valueOf(pSubscript).multiply(typeSize);

        // Check if we are already out of array bound
        if (machineModel.getSizeof(pArrayType).compareTo(subscriptOffset) <= 0) {
          return false;
        }

        Address arrayAddressWithOffset = pArrayAddress.addOffset(subscriptOffset);

        BigInteger subscript = BigInteger.valueOf(pSubscript);
        CIntegerLiteralExpression litExp =
            new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, subscript);
        CArraySubscriptExpression arraySubscript =
            new CArraySubscriptExpression(
                subExpression.getFileLocation(), pExpectedType, subExpression, litExp);

        @Nullable Object concreteValue;

        if (isStructOrUnionType(pExpectedType) || pExpectedType instanceof CArrayType) {
          // Arrays and structs are represented as addresses
          concreteValue = arrayAddressWithOffset;
        } else {
          concreteValue = concreteState.getValueFromMemory(arraySubscript, arrayAddressWithOffset);
        }

        if (concreteValue == null) {
          return false;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = Address.getUnknownAddress();

        if (pExpectedType instanceof CSimpleType cSimpleType) {
          valueLiteral = getValueLiteral(cSimpleType, concreteValue);
        } else {
          valueAddress = Address.valueOf(concreteValue);

          if (valueAddress.isUnknown()) {
            return false;
          }

          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress, machineModel);
        }

        if (!valueLiteral.isUnknown()) {
          SubExpressionValueLiteral subExpressionValueLiteral =
              new SubExpressionValueLiteral(valueLiteral, arraySubscript);
          valueLiterals.addSubExpressionValueLiteral(subExpressionValueLiteral);
        }

        if (!valueAddress.isUnknown()) {
          Pair<CType, Address> visits = Pair.of(pExpectedType, valueAddress);

          if (visited.contains(visits)) {
            return false;
          }

          visited.add(visits);

          ValueLiteralVisitor v =
              new ValueLiteralVisitor(valueAddress, valueLiterals, arraySubscript, visited);
          pExpectedType.accept(v);
        }

        // the check if the array continued was performed at an earlier stage in this function
        return true;
      }

      @Override
      public @Nullable Void visit(CPointerType pointerType) {

        CType expectedType = pointerType.getType().getCanonicalType();

        CPointerExpression pointerExp =
            new CPointerExpression(subExpression.getFileLocation(), expectedType, subExpression);

        @Nullable Object concreteValue;

        if (isStructOrUnionType(expectedType) || expectedType instanceof CArrayType) {
          // Arrays and structs are represented as addresses
          concreteValue = address;
        } else {
          concreteValue = concreteState.getValueFromMemory(pointerExp, address);
        }

        if (concreteValue == null) {
          return null;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = Address.getUnknownAddress();

        if (expectedType instanceof CSimpleType cSimpleType) {
          valueLiteral = getValueLiteral(cSimpleType, concreteValue);
        } else {
          valueAddress = Address.valueOf(concreteValue);

          if (valueAddress.isUnknown()) {
            return null;
          }

          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress, machineModel);
        }

        if (!valueLiteral.isUnknown()) {

          SubExpressionValueLiteral subExpressionValueLiteral =
              new SubExpressionValueLiteral(valueLiteral, pointerExp);

          valueLiterals.addSubExpressionValueLiteral(subExpressionValueLiteral);
        }

        if (!valueAddress.isUnknown()) {

          Pair<CType, Address> visits = Pair.of(expectedType, valueAddress);

          if (visited.contains(visits)) {
            return null;
          }

          /*Tell all instanced visitors that you visited this memory location*/
          visited.add(visits);

          ValueLiteralVisitor v =
              new ValueLiteralVisitor(valueAddress, valueLiterals, pointerExp, visited);
          expectedType.accept(v);
        }

        return null;
      }
    }

    /*Resolve structs or union fields that are stored in the variable environment*/
    private class ValueLiteralStructResolver extends DefaultCTypeVisitor<Void, NoException> {

      private final ValueLiterals valueLiterals;
      private final String functionName;
      private final CExpression prevSub;

      ValueLiteralStructResolver(
          ValueLiterals pValueLiterals, String pFunctionName, CFieldReference pPrevSub) {
        valueLiterals = pValueLiterals;
        functionName = pFunctionName;
        prevSub = pPrevSub;
      }

      ValueLiteralStructResolver(
          ValueLiterals pValueLiterals, String pFunctionName, CIdExpression pOwner) {
        valueLiterals = pValueLiterals;
        functionName = pFunctionName;
        prevSub = pOwner;
      }

      @Override
      public @Nullable Void visitDefault(CType pT) {
        return null;
      }

      @Override
      public @Nullable Void visit(CElaboratedType type) {
        CType realType = type.getRealType();
        return realType == null ? null : realType.getCanonicalType().accept(this);
      }

      @Override
      public @Nullable Void visit(CTypedefType pType) {
        return pType.getRealType().accept(this);
      }

      @Override
      public @Nullable Void visit(CBitFieldType pCBitFieldType) {
        return pCBitFieldType.getType().accept(this);
      }

      @Override
      public @Nullable Void visit(CCompositeType compType) {

        if (compType.getKind() == ComplexTypeKind.ENUM) {
          return null;
        }

        for (CCompositeTypeMemberDeclaration memberType : compType.getMembers()) {
          handleField(memberType.getName(), memberType.getType());
        }

        return null;
      }

      private void handleField(String pFieldName, CType pMemberType) {

        // Can't have pointer dereferences here.
        CFieldReference reference =
            new CFieldReference(prevSub.getFileLocation(), pMemberType, pFieldName, prevSub, false);

        FieldReference fieldReferenceName = getFieldReference(reference, functionName);

        if (concreteState.hasValueForLeftHandSide(fieldReferenceName)) {
          Object referenceValue = concreteState.getVariableValue(fieldReferenceName);
          addStructSubexpression(referenceValue, reference);
        }

        ValueLiteralStructResolver resolver =
            new ValueLiteralStructResolver(valueLiterals, functionName, reference);

        pMemberType.accept(resolver);
      }

      private void addStructSubexpression(Object pFieldValue, CFieldReference reference) {
        CType realType = reference.getExpressionType();
        ValueLiteral valueLiteral;

        if (realType instanceof CSimpleType cSimpleType) {
          valueLiteral = getValueLiteral(cSimpleType, pFieldValue);
        } else {
          Address valueAddress = Address.valueOf(pFieldValue);
          if (valueAddress.isUnknown()) {
            return;
          }
          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress, machineModel);
        }

        if (valueLiteral.isUnknown()) {
          return;
        }

        SubExpressionValueLiteral subExpression =
            new SubExpressionValueLiteral(valueLiteral, reference);
        valueLiterals.addSubExpressionValueLiteral(subExpression);
      }
    }
  }

  private static final class ValueLiterals {

    /*Contains values for possible sub expressions */
    private final List<SubExpressionValueLiteral> subExpressionValueLiterals = new ArrayList<>();

    private final ValueLiteral expressionValueLiteral;

    ValueLiterals() {
      expressionValueLiteral = UnknownValueLiteral.getInstance();
    }

    ValueLiterals(ValueLiteral valueLiteral) {
      expressionValueLiteral = valueLiteral;
    }

    CExpression getExpressionValueLiteralAsCExpression() {
      return expressionValueLiteral.getValueLiteral();
    }

    void addSubExpressionValueLiteral(SubExpressionValueLiteral code) {
      subExpressionValueLiterals.add(code);
    }

    boolean hasUnknownValueLiteral() {
      return expressionValueLiteral.isUnknown();
    }

    Set<SubExpressionValueLiteral> getSubExpressionValueLiteral() {
      return ImmutableSet.copyOf(subExpressionValueLiterals);
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("ValueLiteral : ");
      result.append(expressionValueLiteral.toString());
      result.append(", SubValueLiterals : ");
      result.append(Joiner.on(", ").join(subExpressionValueLiterals));
      return result.toString();
    }
  }

  private sealed interface ValueLiteral {

    CExpression getValueLiteral();

    boolean isUnknown();

    ValueLiteral addCast(CSimpleType pType);
  }

  private enum UnknownValueLiteral implements ValueLiteral {
    INSTANCE;

    static UnknownValueLiteral getInstance() {
      return INSTANCE;
    }

    @Override
    public CLiteralExpression getValueLiteral() {
      throw new UnsupportedOperationException("Can't get the value code of an unknown value");
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public ValueLiteral addCast(CSimpleType pType) {
      throw new UnsupportedOperationException("Can't get the value code of an unknown value");
    }

    @Override
    public String toString() {
      return "UNKNOWN";
    }
  }

  private static sealed class ExplicitValueLiteral implements ValueLiteral {

    private final CLiteralExpression explicitValueLiteral;

    ExplicitValueLiteral(CLiteralExpression pValueLiteral) {
      explicitValueLiteral = pValueLiteral;
    }

    static ValueLiteral valueOf(Address address, MachineModel pMachineModel) {
      if (address.isUnknown() || address.isSymbolic()) {
        return UnknownValueLiteral.getInstance();
      }

      BigInteger value = address.getAddressValue();
      CSimpleType type = CNumericTypes.LONG_LONG_INT;

      BigInteger upperInclusiveBound = pMachineModel.getMaximalIntegerValue(type);
      if (upperInclusiveBound.compareTo(value) < 0) {
        type = CNumericTypes.UNSIGNED_LONG_LONG_INT;
      }

      CLiteralExpression lit = new CIntegerLiteralExpression(FileLocation.DUMMY, type, value);
      return new ExplicitValueLiteral(lit);
    }

    @Override
    public ValueLiteral addCast(CSimpleType pType) {
      CExpression castedValue = getValueLiteral();
      CCastExpression castExpression =
          new CCastExpression(castedValue.getFileLocation(), pType, castedValue);
      return new CastedExplicitValueLiteral(explicitValueLiteral, castExpression);
    }

    static ValueLiteral valueOf(BigInteger value, CSimpleType pType) {
      CIntegerLiteralExpression literal =
          new CIntegerLiteralExpression(FileLocation.DUMMY, pType, value);
      return new ExplicitValueLiteral(literal);
    }

    static ValueLiteral valueOf(FloatValue value, MachineModel pMachineModel, CSimpleType pType) {

      Format targetFormat = FloatValue.Format.fromCType(pMachineModel, pType);
      if (!targetFormat.equals(value.getFormat())
          && value.withPrecision(targetFormat).withPrecision(value.getFormat()).equals(value)) {
        // If the precision of the value doesn't match the type, convert it to the new precision if
        // this can be done without rounding
        // FIXME Find out why the precision doesn't match the type
        value = value.withPrecision(targetFormat);
      }

      CFloatLiteralExpression literal =
          new CFloatLiteralExpression(FileLocation.DUMMY, pMachineModel, pType, value);
      return new ExplicitValueLiteral(literal);
    }

    @Override
    public CExpression getValueLiteral() {
      return explicitValueLiteral;
    }

    @Override
    public boolean isUnknown() {
      return false;
    }

    @Override
    public String toString() {
      return explicitValueLiteral.toASTString();
    }
  }

  private static final class CastedExplicitValueLiteral extends ExplicitValueLiteral {

    private final CCastExpression castExpression;

    CastedExplicitValueLiteral(CLiteralExpression pValueLiteral, CCastExpression exp) {
      super(pValueLiteral);
      castExpression = exp;
    }

    @Override
    public CExpression getValueLiteral() {
      return castExpression;
    }
  }

  private record SubExpressionValueLiteral(ValueLiteral valueLiteral, CLeftHandSide subExpression) {

    CExpression valueLiteralAsCExpression() {
      return valueLiteral().getValueLiteral();
    }
  }

  private static Optional<BigInteger> getFieldOffset(
      CType ownerType, String fieldName, MachineModel pMachineModel) {

    if (ownerType instanceof CElaboratedType cElaboratedType) {
      CType realType = cElaboratedType.getRealType();
      if (realType == null) {
        return Optional.empty();
      }

      return getFieldOffset(realType.getCanonicalType(), fieldName, pMachineModel);
    } else if (ownerType instanceof CCompositeType cCompositeType) {
      BigInteger fieldOffsetInBits = pMachineModel.getFieldOffsetInBits(cCompositeType, fieldName);
      return bitsToByte(fieldOffsetInBits, pMachineModel); // TODO this looses values of bit fields
    } else if (ownerType instanceof CPointerType cPointerType) {

      /* We do not explicitly transform x->b,
      so when we try to get the field b the ownerType of x
      is a pointer type.*/

      CType type = cPointerType.getType().getCanonicalType();
      return getFieldOffset(type, fieldName, pMachineModel);
    }

    throw new AssertionError();
  }

  private static Optional<BigInteger> bitsToByte(BigInteger bits, MachineModel pMachineModel) {
    BigInteger charSizeInBits = BigInteger.valueOf(pMachineModel.getSizeofCharInBits());
    BigInteger[] divAndRemainder = bits.divideAndRemainder(charSizeInBits);
    if (divAndRemainder[1].equals(BigInteger.ZERO)) {
      return Optional.of(divAndRemainder[0]);
    }
    return Optional.empty();
  }
}
