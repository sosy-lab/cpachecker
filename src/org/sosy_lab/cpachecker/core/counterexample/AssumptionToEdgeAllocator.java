/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import javax.annotation.Nullable;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
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
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;

/**
 * Creates assumption along an error path based on a given {@link CFAEdge} edge
 * and a given {@link ConcreteState} state.
 */
@Options(prefix="counterexample.export.assumptions")
public class AssumptionToEdgeAllocator {

  private final LogManager logger;
  private final MachineModel machineModel;

  private static final int FIRST = 0;

  @Option(secure=true, description=
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

  @Option(secure=true, description=
      "If the option assumeLinearArithmetics is set, this option can be used to"
      + " allow multiplication between operands with at least one constant.")
  private boolean allowMultiplicationWithConstants = false;

  @Option(secure=true, description=
      "If the option assumeLinearArithmetics is set, this option can be used to"
      + " allow division and modulo by constants.")
  private boolean allowDivisionAndModuloByConstants = false;

  @Option(secure=true, description=
      "Whether or not to include concrete address values.")
  private boolean includeConstantsForPointers = true;

  /**
   * Creates an instance of the allocator that takes an {@link CFAEdge} edge
   * along an error path and a {@link ConcreteState} state that contains the concrete
   * values of the variables and of the memory at that edge and creates concrete assumptions
   * for the variables at the given edge.
   *
   * @param pConfig the configuration.
   * @param pLogger logger for logging purposes.
   * @param pMachineModel the machine model that holds for the error path of the given edge.
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  public AssumptionToEdgeAllocator(
      Configuration pConfig,
      LogManager pLogger,
      MachineModel pMachineModel) throws InvalidConfigurationException {

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
   * @param pConcreteState a state that contains the concrete values of the variables at the given edge.
   *
   * @return An {@link CFAEdgeWithAssumptions} edge that contains concrete values for variables
   *          represented as assumptions
   */
  public CFAEdgeWithAssumptions allocateAssumptionsToEdge(CFAEdge pCFAEdge, ConcreteState pConcreteState) {

    Collection<AExpressionStatement> assignmentsAtEdge = createAssignmentsAtEdge(pCFAEdge, pConcreteState);
    String comment = createComment(pCFAEdge, pConcreteState);

    return new CFAEdgeWithAssumptions(pCFAEdge, assignmentsAtEdge, comment);
  }


  private String createComment(CFAEdge pCfaEdge, ConcreteState pConcreteState) {
    if (pCfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      return handleAssumeComment((AssumeEdge) pCfaEdge, pConcreteState);
    } else if (pCfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return handleDclComment((ADeclarationEdge)pCfaEdge, pConcreteState);
    } else if(pCfaEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge) {
      return handleReturnStatementComment((AReturnStatementEdge) pCfaEdge, pConcreteState);
    }

    return "";
  }

  private String handleReturnStatementComment(AReturnStatementEdge pCfaEdge, ConcreteState pConcreteState) {
    Optional<? extends AExpression> returnExpression = pCfaEdge.getExpression();
    if (returnExpression.isPresent() && returnExpression.get() instanceof CExpression) {
      CExpression returnExp = (CExpression) returnExpression.get();

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

      return returnExp.toASTString() + " = " + value.toString();
    }

    return "";
  }

  private String handleDclComment(ADeclarationEdge pCfaEdge, ConcreteState pConcreteState) {

    if (pCfaEdge instanceof CDeclarationEdge) {
      return getCommentOfDclAddress((CSimpleDeclaration) pCfaEdge.getDeclaration(), pCfaEdge, pConcreteState);
    }

    return "";
  }

  private String getCommentOfDclAddress(CSimpleDeclaration dcl, CFAEdge edge, ConcreteState pConcreteState) {

    String functionName = edge.getPredecessor().getFunctionName();

    /* function name may be null*/
    LModelValueVisitor v = new LModelValueVisitor(functionName, pConcreteState);
    Address address = v.getAddress(dcl);

    // concrete addresses already assumptions
    if (address.isUnknown() || address.isConcrete()) {
      return "";
    }

    return "&" + dcl.getName()
        + " == " + address.getCommentRepresentation();
  }

  @Nullable
  private Collection<AExpressionStatement> createAssignmentsAtEdge(CFAEdge pCFAEdge, ConcreteState pConcreteState) {

    Set<AExpressionStatement> result = new LinkedHashSet<>();

    // Get all Assumptions of this edge
    if (pCFAEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      List<AExpressionStatement> dclAssumptions = handleDeclaration(((ADeclarationEdge) pCFAEdge).getDeclaration(),
          pCFAEdge.getPredecessor().getFunctionName(),
          pConcreteState);
      result.addAll(dclAssumptions);
    } else if (pCFAEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      List<AExpressionStatement> stmtAssumptions =
          handleStatement(pCFAEdge, ((AStatementEdge) pCFAEdge).getStatement(), pConcreteState);
      result.addAll(stmtAssumptions);
    }

    if (pCFAEdge.getEdgeType() == CFAEdgeType.BlankEdge
        || !AutomatonGraphmlCommon.handleAsEpsilonEdge(pCFAEdge)) {
      List<AExpressionStatement> parameterAssumptions =
          handleFunctionEntry(pCFAEdge, pConcreteState);
      result.addAll(parameterAssumptions);
    }

    return result;
  }

  private String handleAssumeComment(AssumeEdge pCfaEdge, ConcreteState pConcreteState) {

    if (pCfaEdge instanceof CAssumeEdge) {
      return handleAssumeComment((CAssumeEdge) pCfaEdge, pConcreteState);
    }

    return "";
  }

  private String handleAssumeComment(CAssumeEdge pCFAEdge, ConcreteState pConcreteState) {

    CExpression pCExpression = pCFAEdge.getExpression();

    String functionName = pCFAEdge.getPredecessor().getFunctionName();

    if (pCExpression instanceof CBinaryExpression) {

      CBinaryExpression binExp = ((CBinaryExpression) pCExpression);

      CExpression op1 = binExp.getOperand1();
      CExpression op2 = binExp.getOperand2();

      String result1 = handleAssumeOp(pCFAEdge, op1, functionName, pConcreteState);

      String result2 = handleAssumeOp(pCFAEdge, op2, functionName, pConcreteState);

      if (!result1.isEmpty() && !result2.isEmpty()) {
        return result1 + System.lineSeparator() + result2;
      } else if (!result1.isEmpty()) {
        return result1;
      } else if (!result2.isEmpty()) {
        return result2;
      }

      return "";
    }

    return "";
  }

  private String handleAssumeOp(CFAEdge pCFAEdge, CExpression op, String pFunctionName, ConcreteState pConcreteState) {

    if (op instanceof CLiteralExpression) {
      /*boring expression*/
      return "";
    }

    if (op instanceof CLeftHandSide) {

      List<AExpressionStatement> assignments = handleAssignment(pCFAEdge, (CLeftHandSide) op, pConcreteState);

      if (assignments.size() == 0) {
        return "";
      } else {

        List<String> result = new ArrayList<>(assignments.size());

        for (AExpressionStatement assignment : assignments) {
          result.add(assignment.toASTString());
        }

        return Joiner.on(System.lineSeparator()).join(result);
      }

    } else {
      Object value = getValueObject(op, pFunctionName, pConcreteState);

      if (value != null) {
        return op.toASTString() + " == " + value.toString();
      } else {
        return "";
      }
    }
  }

  private Object getValueObject(CExpression pOp1, String pFunctionName, ConcreteState pConcreteState) {

    LModelValueVisitor v = new LModelValueVisitor(pFunctionName, pConcreteState);

    return v.evaluateNumericalValue(pOp1);
  }

  private List<AExpressionStatement> handleFunctionEntry(
      CFAEdge pEdge, ConcreteState pConcreteState) {

    CFANode predecessor = pEdge.getPredecessor();

    // For the program entry function, we must be careful not to create
    // expressions before the global initializations
    if (predecessor.getNumEnteringEdges() <= 0) {
      return Collections.emptyList();
    }

    // Handle program entry function
    String function = predecessor.getFunctionName();
    while (!(predecessor instanceof FunctionEntryNode)) {
      if (predecessor.getNumEnteringEdges() != 1
          || !predecessor.getFunctionName().equals(function)) {
        return Collections.emptyList();
      }
      CFAEdge enteringEdge = predecessor.getEnteringEdge(0);
      if (!AutomatonGraphmlCommon.handleAsEpsilonEdge(enteringEdge)) {
        return Collections.emptyList();
      }
      predecessor = enteringEdge.getPredecessor();
    }

    FunctionEntryNode entryNode = (FunctionEntryNode) predecessor;

    String functionName = entryNode.getFunctionDefinition().getName();

    List<? extends AParameterDeclaration> parameterDeclarations = entryNode.getFunctionParameters();
    if (parameterDeclarations.isEmpty()) {
      return Collections.emptyList();
    }
    List<AExpressionStatement> result = new ArrayList<>(parameterDeclarations.size());
    for (AParameterDeclaration parameterDeclaration : parameterDeclarations) {
      result.addAll(handleDeclaration(parameterDeclaration, functionName, pConcreteState));
    }
    return result;
  }

  @Nullable
  private List<AExpressionStatement> handleAssignment(CFAEdge pCFAEdge, CLeftHandSide pLeftHandSide, ConcreteState pConcreteState) {

    String functionName = pCFAEdge.getPredecessor().getFunctionName();

    Object value = getValueObject(pLeftHandSide, functionName, pConcreteState);

    if (value == null) {
      return Collections.emptyList();
    }

    Type expectedType = pLeftHandSide.getExpressionType();
    ValueLiterals valueAsCode = getValueAsCode(value, expectedType, pLeftHandSide, functionName, pConcreteState);

    return handleSimpleValueLiteralsAssumptions(valueAsCode, pLeftHandSide);
  }

  private List<AExpressionStatement> handleAssignment(CFAEdge pCFAEdge, CAssignment pAssignment, ConcreteState pConcreteState) {
    CLeftHandSide leftHandSide = pAssignment.getLeftHandSide();
    return handleAssignment(pCFAEdge, leftHandSide, pConcreteState);
  }

  private Object getValueObject(CLeftHandSide pLeftHandSide, String pFunctionName, ConcreteState pConcreteState) {

    LModelValueVisitor v = new LModelValueVisitor(pFunctionName, pConcreteState);
    return pLeftHandSide.accept(v);
  }

  private ValueLiterals getValueAsCode(Object pValue,
      Type pExpectedType,
      CLeftHandSide leftHandSide,
      String functionName,
      ConcreteState pConcreteState) {

    // TODO processing for other languages
    if (pExpectedType instanceof CType) {
      CType cType = ((CType) pExpectedType).getCanonicalType();

      ValueLiteralsVisitor v = new ValueLiteralsVisitor(pValue, leftHandSide, pConcreteState);
      ValueLiterals valueLiterals = cType.accept(v);

      // resolve field references that lack a address
      if (isStructOrUnionType(cType) && leftHandSide instanceof CIdExpression) {
        v.resolveStruct(cType, valueLiterals, (CIdExpression) leftHandSide, functionName);
      }

      return valueLiterals;
    }

    return new ValueLiterals();
  }

  private List<AExpressionStatement> handleStatement(CFAEdge pCFAEdge, AStatement pStatement, ConcreteState pConcreteState) {

    if (pStatement instanceof CFunctionCallAssignmentStatement) {
      CAssignment assignmentStatement =
          ((CFunctionCallAssignmentStatement) pStatement);
      return handleAssignment(pCFAEdge, assignmentStatement, pConcreteState);
    }

    if (pStatement instanceof CExpressionAssignmentStatement) {
      CAssignment assignmentStatement =
          ((CExpressionAssignmentStatement) pStatement);
      return handleAssignment(pCFAEdge, assignmentStatement, pConcreteState);
    }

    return Collections.emptyList();
  }

  private List<AExpressionStatement> handleDeclaration(ASimpleDeclaration dcl, String pFunctionName, ConcreteState pConcreteState) {

    if (dcl instanceof CSimpleDeclaration) {

      CSimpleDeclaration cDcl = (CSimpleDeclaration) dcl;

      CType dclType = cDcl.getType();

      Object value = getValueObject(cDcl, pFunctionName, pConcreteState);

      if (value == null) {
        return Collections.emptyList();
      }

      CIdExpression idExpression = new CIdExpression(dcl.getFileLocation(), cDcl);

      ValueLiterals valueAsCode =  getValueAsCode(value, dclType, idExpression, pFunctionName, pConcreteState);

      CLeftHandSide leftHandSide = new CIdExpression(FileLocation.DUMMY, cDcl);

      return handleSimpleValueLiteralsAssumptions(valueAsCode, leftHandSide);
    }

    return Collections.emptyList();
  }

  private List<AExpressionStatement> handleSimpleValueLiteralsAssumptions(ValueLiterals pValueLiterals, CLeftHandSide pLValue) {

    Set<SubExpressionValueLiteral> subValues = pValueLiterals.getSubExpressionValueLiteral();

    Set<AExpressionStatement> statements = Sets.newLinkedHashSet();

    CBinaryExpressionBuilder expressionBuilder = new CBinaryExpressionBuilder(machineModel, logger);

    if (!pValueLiterals.hasUnknownValueLiteral()) {

      CExpression leftSide = getLeftAssumptionFromLhs(pLValue);
      CExpression rightSide = pValueLiterals.getExpressionValueLiteralAsCExpression();

      AExpressionStatement statement =
          buildEquationExpressionStatement(expressionBuilder, leftSide, rightSide);
      statements.add(statement);
    }

    for (SubExpressionValueLiteral subValueLiteral : subValues) {

      CExpression leftSide = getLeftAssumptionFromLhs(subValueLiteral.getSubExpression());
      CExpression rightSide = subValueLiteral.getValueLiteralAsCExpression();

      AExpressionStatement statement =
          buildEquationExpressionStatement(expressionBuilder, leftSide, rightSide);
      statements.add(statement);
    }

    return FluentIterable.from(statements).filter(Predicates.notNull()).toList();
  }

  private @Nullable AExpressionStatement buildEquationExpressionStatement(
      CBinaryExpressionBuilder pBuilder,
      CExpression pLeftSide,
      CExpression pRightSide) {
    CExpression leftSide = pLeftSide;
    CExpression rightSide = pRightSide;

    final CType leftType = leftSide.getExpressionType().getCanonicalType();
    final CType rightType = rightSide.getExpressionType().getCanonicalType();

    if (leftType instanceof CVoidType && rightType instanceof CVoidType) {
      return null;
    }

    boolean equalTypes = leftType.equals(rightType);

    FluentIterable<Class<? extends CType>> acceptedTypes =
        FluentIterable.from(Collections.<Class<? extends CType>>singleton(CSimpleType.class));
    if (includeConstantsForPointers) {
      acceptedTypes = acceptedTypes.append(
          Arrays.asList(
              CArrayType.class,
              CPointerType.class));
    }

    boolean leftIsAccepted = equalTypes || acceptedTypes.anyMatch(
        pArg0 -> pArg0.isAssignableFrom(leftType.getClass()));

    boolean rightIsAccepted = equalTypes || acceptedTypes.anyMatch(
        pArg0 -> pArg0.isAssignableFrom(rightType.getClass()));

    if (!includeConstantsForPointers && (!leftIsAccepted || !rightIsAccepted)) {
      return null;
    }

    if (leftType instanceof CSimpleType && !rightIsAccepted) {
      rightSide = new CCastExpression(rightSide.getFileLocation(), leftType, rightSide);
    } else if (!leftIsAccepted && rightType instanceof CSimpleType) {
      leftSide = new CCastExpression(leftSide.getFileLocation(), rightType, leftSide);
    }

    CBinaryExpression assumption =
        pBuilder.buildBinaryExpressionUnchecked(
            leftSide,
            rightSide,
            CBinaryExpression.BinaryOperator.EQUALS);

    return new CExpressionStatement(assumption.getFileLocation(), assumption);
  }

  private CExpression getLeftAssumptionFromLhs(CLeftHandSide pLValue) {

    // We represent structs and arrays as addresses. When we transform those to
    // assumptions, we have to resolve them.

    CType type = pLValue.getExpressionType().getCanonicalType();

    if (isStructOrUnionType(type) || type instanceof CArrayType) {
      if (pLValue instanceof CPointerExpression) {
        return ((CPointerExpression) pLValue).getOperand();
      }
      CUnaryExpression unaryExpression = new CUnaryExpression(
          pLValue.getFileLocation(), type, pLValue,
          CUnaryExpression.UnaryOperator.AMPER);
      return unaryExpression;
    } else {
      return pLValue;
    }
  }

  private Object getValueObject(CSimpleDeclaration pDcl, String pFunctionName, ConcreteState pConcreteState) {
    return new LModelValueVisitor(pFunctionName, pConcreteState).handleVariableDeclaration(pDcl);
  }

  private boolean isStructOrUnionType(CType rValueType) {

    rValueType = rValueType.getCanonicalType();

    if (rValueType instanceof CElaboratedType) {
      CElaboratedType type = (CElaboratedType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    if (rValueType instanceof CCompositeType) {
      CCompositeType type = (CCompositeType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    return false;
  }

  //TODO Move to Utility?
  private FieldReference getFieldReference(CFieldReference pIastFieldReference,
      String pFunctionName) {

    List<String> fieldNameList = new ArrayList<>();

    CFieldReference reference = pIastFieldReference;

    fieldNameList.add(FIRST, reference.getFieldName());

    while (reference.getFieldOwner() instanceof CFieldReference
        && !reference.isPointerDereference()) {
      reference = (CFieldReference) reference.getFieldOwner();
      fieldNameList.add(FIRST, reference.getFieldName());
    }

    if (reference.getFieldOwner() instanceof CIdExpression) {

      CIdExpression idExpression = (CIdExpression) reference.getFieldOwner();

      if (ForwardingTransferRelation.isGlobal(idExpression)) {
        return new FieldReference(idExpression.getName(), fieldNameList);
      } else {
        return new FieldReference(idExpression.getName(), pFunctionName, fieldNameList);
      }
    } else {
      return null;
    }
  }

  private class LModelValueVisitor implements CLeftHandSideVisitor<Object, RuntimeException> {

    private final String functionName;
    private final AddressValueVisitor addressVisitor;
    private final ConcreteState concreteState;

    public LModelValueVisitor(String pFunctionName, ConcreteState pConcreteState) {
      functionName = pFunctionName;
      addressVisitor = new AddressValueVisitor(this);
      concreteState = pConcreteState;
    }

    private Address getAddress(CSimpleDeclaration dcl) {
      return addressVisitor.getAddress(dcl);
    }

    private Number evaluateNumericalValue(CExpression exp) {

      Value addressV;
      try {
        ModelExpressionValueVisitor v = new ModelExpressionValueVisitor(functionName, machineModel, new LogManagerWithoutDuplicates(logger));
        addressV = exp.accept(v);
      } catch (ArithmeticException e) {
        logger.logDebugException(e);
        logger.log(Level.WARNING, "The expression " + exp.toASTString() +
            "could not be correctly evaluated while calculating the concrete values "
            + "in the counterexample path.");
        return null;
      } catch (UnrecognizedCCodeException e1) {
        throw new IllegalArgumentException(e1);
      }

      if (addressV.isUnknown() && !addressV.isNumericValue()) {
        return null;
      }

      return addressV.asNumericValue().getNumber();
    }

    private Address evaluateNumericalValueAsAddress(CExpression exp) {

      Number result = evaluateNumericalValue(exp);

      if (result == null) {
        return Address.getUnknownAddress();
      }

      return Address.valueOf(result);
    }

    /*This method evaluates the address of the lValue, not the address the expression evaluates to*/
    private Address evaluateAddress(CLeftHandSide pExp) {
      return pExp.accept(addressVisitor);
    }

    @Override
    public Object visit(CArraySubscriptExpression pIastArraySubscriptExpression) {

      Address valueAddress = evaluateAddress(pIastArraySubscriptExpression);

      if (valueAddress.isUnknown()) {
        return null;
      }

      CType type = pIastArraySubscriptExpression.getExpressionType().getCanonicalType();

      /*The evaluation of an array or a struct is its address*/
      if (type instanceof CArrayType || isStructOrUnionType(type)) {

        if(valueAddress.isSymbolic()) {
          return null;
        }

        return valueAddress.getAddressValue();
      }

      Object value = concreteState.getValueFromMemory(pIastArraySubscriptExpression,
          valueAddress);

      return value;
    }

    @Override
    public Object visit(CFieldReference pIastFieldReference) {

      Address address = evaluateAddress(pIastFieldReference);

      if (address.isUnknown()) {
        return lookupReference(pIastFieldReference);
      }

      CType type = pIastFieldReference.getExpressionType().getCanonicalType();

      /* The evaluation of an array or a struct is its address */
      if (type instanceof CArrayType || isStructOrUnionType(type)) {

        if (address.isSymbolic()) {
          return null;
        }

        return address.getAddressValue();
      }

      Object value = concreteState.getValueFromMemory(pIastFieldReference, address);

      if (value == null) {
        return lookupReference(pIastFieldReference);
      }

      return value;
    }

    private Object lookupReference(CFieldReference pIastFieldReference) {

      /* Fieldreferences are sometimes represented as variables,
         e.g a.b.c in main is main::a$b$c */
      FieldReference fieldReference = getFieldReference(pIastFieldReference, functionName);

      if (fieldReference != null &&
          concreteState.hasValueForLeftHandSide(fieldReference)) {

        return concreteState.getVariableValue(fieldReference);
      }

      return null;
    }

    private BigInteger getFieldOffsetInBits(CFieldReference fieldReference) {
      CType fieldOwnerType = fieldReference.getFieldOwner().getExpressionType().getCanonicalType();
      return AssumptionToEdgeAllocator.getFieldOffsetInBits(
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

      /* The evaluation of an array or a struct is its address */
      if (type instanceof CArrayType || isStructOrUnionType(type)) {
        if (address.isSymbolic()) {
          return lookupVariable(dcl);
        }
        return address.getAddressValue();
      }

      Object value = concreteState.getValueFromMemory(pCIdExpression, address);

      if (value == null) {
        return lookupVariable(dcl);
      }

      return value;
    }

    @Nullable
    private Object handleVariableDeclaration(CSimpleDeclaration pDcl) {

      // These declarations don't evaluate to a value //TODO Assumption
      if (pDcl instanceof CFunctionDeclaration || pDcl instanceof CTypeDeclaration) { return null; }

      CIdExpression representingIdExpression = new CIdExpression(pDcl.getFileLocation(), pDcl);
      return this.visit(representingIdExpression);
    }

    private Object lookupVariable(CSimpleDeclaration pVarDcl) {
      IDExpression varName = getIDExpression(pVarDcl);

      if (concreteState.hasValueForLeftHandSide(varName)) {
        return concreteState.getVariableValue(varName);
      } else {
        return null;
      }
    }

    //TODO Move to util
    private IDExpression getIDExpression(CSimpleDeclaration pDcl) {

      //TODO use original name?
      String name = pDcl.getName();

      if (pDcl instanceof CDeclaration && ((CDeclaration) pDcl).isGlobal()) {
        return new IDExpression(name);
      } else {
        return new IDExpression(name, functionName);
      }
    }

    @Override
    public Object visit(CPointerExpression pPointerExpression) {

      /*Quick jump to the necessary method.
       * the address of a dereference is the evaluation of its operand*/
      Address address = evaluateAddress(pPointerExpression);

      if (address.isUnknown()) {
        return null;
      }

      CType type = pPointerExpression.getExpressionType().getCanonicalType();

      /*The evaluation of an array or a struct is its address*/
      if (type instanceof CArrayType || isStructOrUnionType(type)) {
        if(address.isSymbolic()) {
          return null;
        }

        return address.getAddressValue();
      }

      return concreteState.getValueFromMemory(pPointerExpression, address);
    }

    boolean isStructOrUnionType(CType rValueType) {

      rValueType = rValueType.getCanonicalType();

      if (rValueType instanceof CElaboratedType) {
        CElaboratedType type = (CElaboratedType) rValueType;
        return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
      }

      if (rValueType instanceof CCompositeType) {
        CCompositeType type = (CCompositeType) rValueType;
        return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
      }

      return false;
    }

    private class AddressValueVisitor implements CLeftHandSideVisitor<Address, RuntimeException> {

      private final LModelValueVisitor valueVisitor;

      public AddressValueVisitor(LModelValueVisitor pValueVisitor) {
        valueVisitor = pValueVisitor;
      }

      public Address getAddress(CSimpleDeclaration dcl) {

        IDExpression name = getIDExpression(dcl);

        if (concreteState.hasAddressOfVaribable(name)) {
          return concreteState.getVariableAddress(name);
        }

        return Address.getUnknownAddress();
      }

      @Override
      public Address visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
        CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();

        // This works because arrays and structs evaluate to their addresses
        Address address = evaluateNumericalValueAsAddress(arrayExpression);

        if (address.isUnknown() || address.isSymbolic()) {
          return Address.getUnknownAddress();
        }

        CExpression subscriptCExpression = pIastArraySubscriptExpression.getSubscriptExpression();

        Number subscriptValueNumber = evaluateNumericalValue(subscriptCExpression);

        if (subscriptValueNumber == null) {
          return Address.getUnknownAddress();
        }

        final BigDecimal subscriptValue;
        if (subscriptValueNumber instanceof Rational) {
          Rational rational = (Rational) subscriptValueNumber;
          subscriptValue = new BigDecimal(rational.getNum()).divide(new BigDecimal(rational.getDen()));
        } else {
          subscriptValue = new BigDecimal(subscriptValueNumber.toString());
        }

        BigDecimal typeSize =
            BigDecimal.valueOf(
                machineModel.getBitSizeof(
                    pIastArraySubscriptExpression.getExpressionType().getCanonicalType()));

        BigDecimal subscriptOffset = subscriptValue.multiply(typeSize);

        return address.addOffset(subscriptOffset);
      }

      @Override
      public Address visit(CFieldReference pIastFieldReference) {

        CExpression fieldOwner = pIastFieldReference.getFieldOwner();

        //This works because arrays and structs evaluate to their addresses.
        Address fieldOwnerAddress = evaluateNumericalValueAsAddress(fieldOwner);

        if (fieldOwnerAddress.isUnknown() || fieldOwnerAddress.isSymbolic()) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        BigInteger fieldOffset = getFieldOffsetInBits(pIastFieldReference);

        if (fieldOffset == null) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        Address address = fieldOwnerAddress.addOffset(new BigDecimal(fieldOffset));

        if (address.isUnknown()) {
          return lookupReferenceAddress(pIastFieldReference);
        }

        return address;
      }

      private Address lookupReferenceAddress(CFieldReference pIastFieldReference) {
        /* Fieldreferences are sometimes represented as variables,
        e.g a.b.c in main is main::a$b$c */
        FieldReference fieldReferenceName = getFieldReference(pIastFieldReference, functionName);

        if (fieldReferenceName != null) {
          if (concreteState.hasAddressOfVaribable(fieldReferenceName)) {
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

      public ModelExpressionValueVisitor(String pFunctionName, MachineModel pMachineModel,
          LogManagerWithoutDuplicates pLogger) {
        super(pFunctionName, pMachineModel, pLogger);
      }

      @Override
      public Value visit(CCastExpression cast) throws UnrecognizedCCodeException {

        if (concreteState.getAnalysisConcreteExpressionEvaluation().shouldEvaluateExpressionWithThisEvaluator(cast)) {
          Value op = cast.getOperand().accept(this);

          if (op.isUnknown()) {
            return op;
          }

          return concreteState.getAnalysisConcreteExpressionEvaluation().evaluate(cast, op);
        }

        return super.visit(cast);
      }

      @Override
      public Value visit(CBinaryExpression binaryExp) throws UnrecognizedCCodeException {

        if (concreteState.getAnalysisConcreteExpressionEvaluation().shouldEvaluateExpressionWithThisEvaluator(binaryExp)) {
          Value op1 = binaryExp.getOperand1().accept(this);

          if (op1.isUnknown()) {
            return op1;
          }

          Value op2 = binaryExp.getOperand2().accept(this);

          if (op2.isUnknown()) {
            return op2;
          }

          return concreteState.getAnalysisConcreteExpressionEvaluation().evaluate(binaryExp, op1, op2);
        }

        CExpression lVarInBinaryExp = binaryExp.getOperand1();
        CExpression rVarInBinaryExp = binaryExp.getOperand2();
        CType lVarInBinaryExpType = lVarInBinaryExp.getExpressionType().getCanonicalType();
        CType rVarInBinaryExpType = rVarInBinaryExp.getExpressionType().getCanonicalType();

        boolean lVarIsAddress = lVarInBinaryExpType instanceof CPointerType
            || lVarInBinaryExpType instanceof CArrayType;
        boolean rVarIsAddress = rVarInBinaryExpType instanceof CPointerType
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
            switch (binaryExp.getOperator()) {
            case MULTIPLY:
              // Multiplication with constants is sometimes supported
              if (allowMultiplicationWithConstants
                  && (lVarInBinaryExp instanceof ALiteralExpression
                      || rVarInBinaryExp instanceof ALiteralExpression)) {
                return super.visit(binaryExp);
              }
              return Value.UnknownValue.getInstance();
            case DIVIDE:
            case MODULO:
              // Division and modulo with constants are sometimes supported
              if (allowDivisionAndModuloByConstants && rVarInBinaryExp instanceof ALiteralExpression) {
                break;
              }
              //$FALL-THROUGH$
            case BINARY_AND:
            case BINARY_OR:
            case BINARY_XOR:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
              return Value.UnknownValue.getInstance();
            default:
              break;
            }
          }
          return super.visit(binaryExp);
        }

        BinaryOperator binaryOperator = binaryExp.getOperator();

        CType elementType = addressType instanceof CPointerType ?
            ((CPointerType)addressType).getType().getCanonicalType() :
                            ((CArrayType)addressType).getType().getCanonicalType();

        switch (binaryOperator) {
        case PLUS:
        case MINUS: {

          Value addressValueV = address.accept(this);

          Value offsetValueV = pointerOffset.accept(this);

          if (addressValueV.isUnknown() || offsetValueV.isUnknown()
              || !addressValueV.isNumericValue() || !offsetValueV.isNumericValue()) {
            return Value.UnknownValue
              .getInstance();
          }

          Number addressValueNumber = addressValueV.asNumericValue().getNumber();

          BigDecimal addressValue = new BigDecimal(addressValueNumber.toString());

          // Because address and offset value may be interchanged, use BigDecimal for both
          Number offsetValueNumber = offsetValueV.asNumericValue().getNumber();

          BigDecimal offsetValue = new BigDecimal(offsetValueNumber.toString());

              BigDecimal typeSize = BigDecimal.valueOf(getBitSizeof(elementType));

          BigDecimal pointerOffsetValue = offsetValue.multiply(typeSize);

          switch (binaryOperator) {
          case PLUS:
            return new NumericValue(addressValue.add(pointerOffsetValue));
          case MINUS:
            if (lVarIsAddress) {
              return new NumericValue(addressValue.subtract(pointerOffsetValue));
            } else {
              throw new UnrecognizedCCodeException("Expected pointer arithmetic "
                  + " with + or - but found " + binaryExp.toASTString(), binaryExp);
            }
          default:
            throw new AssertionError();
          }
        }

        default:
          return Value.UnknownValue.getInstance();
        }
      }

      @Override
      public Value visit(CUnaryExpression pUnaryExpression) throws UnrecognizedCCodeException {

        if (concreteState.getAnalysisConcreteExpressionEvaluation().shouldEvaluateExpressionWithThisEvaluator(pUnaryExpression)) {

          Value operand = pUnaryExpression.getOperand().accept(this);

          if (operand.isUnknown() && (pUnaryExpression.getOperator() == UnaryOperator.MINUS
              || pUnaryExpression.getOperator() == UnaryOperator.TILDE)) {
            return operand;
          }

          return concreteState.getAnalysisConcreteExpressionEvaluation().evaluate(pUnaryExpression, operand);
        }

        if (pUnaryExpression.getOperator() == UnaryOperator.AMPER) {

          CExpression operand = pUnaryExpression.getOperand();

          return handleAmper(operand);
        }

        return super.visit(pUnaryExpression);
      }

      private Value handleAmper(CExpression pOperand) {
        if (pOperand instanceof CLeftHandSide) {

          Address address = evaluateAddress((CLeftHandSide) pOperand);

          if (address.isConcrete()) {
            return new NumericValue(address.getAddressValue());
          }
        } else if (pOperand instanceof CCastExpression) {
          return handleAmper(((CCastExpression) pOperand).getOperand());
        }

        return Value.UnknownValue.getInstance();
      }

      @Override
      protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
          throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pCPointerExpression);

        if (value == null || !(value instanceof Number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((Number) value);
      }

      @Override
      protected Value evaluateCIdExpression(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {

        Object value = LModelValueVisitor.this.visit(pCIdExpression);

        if (value == null || !(value instanceof Number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((Number)value);
      }

      @Override
      protected Value evaluateJIdExpression(JIdExpression pVarName) {
        return Value.UnknownValue.getInstance();
      }

      @Override
      protected Value evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pLValue);

        if (value == null || !(value instanceof Number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((Number)value);
      }

      @Override
      protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
          throws UnrecognizedCCodeException {
        Object value = LModelValueVisitor.this.visit(pLValue);

        if (value == null || !(value instanceof Number)) {
          return Value.UnknownValue.getInstance();
        }

        return new NumericValue((Number) value);
      }
    }

    @Override
    public Object visit(CComplexCastExpression pComplexCastExpression) {
      // TODO Auto-generated method stub
      return null;
    }
  }

  private class ValueLiteralsVisitor extends DefaultCTypeVisitor<ValueLiterals, RuntimeException> {

    private final Object value;
    private final CExpression exp;
    private final ConcreteState concreteState;

    public ValueLiteralsVisitor(Object pValue, CExpression pExp, ConcreteState pConcreteState) {
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

      if(address.isUnknown()) {
        return createUnknownValueLiterals();
      }

      ValueLiteral valueLiteral = ExplicitValueLiteral.valueOf(address, machineModel);

      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);

      ValueLiteralVisitor v = new ValueLiteralVisitor(address, valueLiterals, exp);

      pointerType.accept(v);

      return valueLiterals;
    }

    @Override
    public ValueLiterals visit(CArrayType arrayType) {
      Address address = Address.valueOf(value);

      ValueLiteral valueLiteral;

      if (address.isUnknown()) {
        return createUnknownValueLiterals();
      }

      valueLiteral = ExplicitValueLiteral.valueOf(address, machineModel);

      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);

      ValueLiteralVisitor v = new ValueLiteralVisitor(address, valueLiterals, exp);

      arrayType.accept(v);

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
    public ValueLiterals visit(CBitFieldType pCBitFieldType) throws RuntimeException {
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

      if(address.isUnknown()) {
        return createUnknownValueLiterals();
      }

      ValueLiteral valueLiteral = ExplicitValueLiteral.valueOf(address, machineModel);

      ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);

      ValueLiteralVisitor v = new ValueLiteralVisitor(address, valueLiterals, exp);

      compType.accept(v);

      return valueLiterals;
    }

    protected ValueLiteral getValueLiteral(CSimpleType pSimpleType, Object pValue) {
      CSimpleType simpleType = pSimpleType.getCanonicalType();
      CBasicType basicType = simpleType.getType();

      switch (basicType) {
        case BOOL:
        case CHAR:
        case INT:
          return handleIntegerNumbers(pValue, simpleType);
        case FLOAT:
        case DOUBLE:
          if (assumeLinearArithmetics) {
            break;
          }
          return handleFloatingPointNumbers(pValue, simpleType);
        default:
          break;
      }

      return UnknownValueLiteral.getInstance();
    }

    private ValueLiterals createUnknownValueLiterals() {
      return new ValueLiterals();
    }

    private ValueLiteral handleFloatingPointNumbers(Object pValue, CSimpleType pType) {

      if (pValue instanceof Rational) {
        double val = ((Rational) pValue).doubleValue();
        if (Double.isInfinite(val) || Double.isNaN(val)) {
          // TODO return correct value
          return UnknownValueLiteral.getInstance();
        }
        return ExplicitValueLiteral.valueOf(new BigDecimal(val), pType);

      } else if (pValue instanceof Double) {
        double doubleValue = ((Double)pValue).doubleValue();
        if (Double.isInfinite(doubleValue) || Double.isNaN(doubleValue)) {
          // TODO return correct value
          return UnknownValueLiteral.getInstance();
        }
        return ExplicitValueLiteral.valueOf(BigDecimal.valueOf(doubleValue), pType);
      } else if (pValue instanceof Float) {
        float floatValue = ((Float)pValue).floatValue();
        if (Float.isInfinite(floatValue) || Double.isNaN(floatValue)) {
          // TODO return correct value
          return UnknownValueLiteral.getInstance();
        }
        return ExplicitValueLiteral.valueOf(BigDecimal.valueOf(floatValue), pType);
      }


      String value = pValue.toString();
      BigDecimal val;

      //TODO support rationals
      try {
        val = new BigDecimal(value);
      } catch (NumberFormatException e) {

        logger.log(Level.INFO, "Can't parse " + value + " as value for the counter-example path.");
        return UnknownValueLiteral.getInstance();
      }

      return ExplicitValueLiteral.valueOf(val, pType);
    }

    public void resolveStruct(CType type, ValueLiterals pValueLiterals,
        CIdExpression pOwner, String pFunctionName) {

      ValueLiteralStructResolver v = new ValueLiteralStructResolver(pValueLiterals, pFunctionName, pOwner);
      type.accept(v);
    }

    private ValueLiteral handleIntegerNumbers(Object pValue, CSimpleType pType) {

      String value = pValue.toString();

      if (value.matches("((-)?)\\d*")) {
        BigInteger integerValue = new BigInteger(value);

        return handlePotentialIntegerOverflow(integerValue, pType);
      } else {
        String[] numberParts = value.split("\\.");

        if (numberParts.length == 2 &&
            numberParts[1].matches("0*") &&
            numberParts[0].matches("((-)?)\\d*")) {

          BigInteger integerValue = new BigInteger(numberParts[0]);

          return handlePotentialIntegerOverflow(integerValue, pType);
        }
      }

      ValueLiteral valueLiteral = handleFloatingPointNumbers(pValue, pType);

      if (valueLiteral.isUnknown()) {
        return valueLiteral;
      } else {
        return valueLiteral.addCast(pType);
      }
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
            logger instanceof LogManagerWithoutDuplicates
                ? (LogManagerWithoutDuplicates) logger
                : new LogManagerWithoutDuplicates(logger);
        Value value =
            ExpressionValueVisitor.castCValue(
                new NumericValue(pIntegerValue),
                pType,
                machineModel,
                logManager,
                FileLocation.DUMMY);
        Number number = value.asNumericValue().getNumber();
        final BigInteger valueAsBigInt;
        if (number instanceof BigInteger) {
          valueAsBigInt = (BigInteger) number;
        } else {
          valueAsBigInt = BigInteger.valueOf(number.longValue());
        }
        return ExplicitValueLiteral.valueOf(valueAsBigInt, pType);
      }

      return ExplicitValueLiteral.valueOf(pIntegerValue, pType);
    }

    /**
     * Resolves all subexpressions that can be resolved.
     * Stops at duplicate memory location.
     */
    private class ValueLiteralVisitor extends DefaultCTypeVisitor<Void, RuntimeException> {

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

      public ValueLiteralVisitor(Address pAddress, ValueLiterals pValueLiterals, CExpression pSubExp) {
        address = pAddress;
        valueLiterals = pValueLiterals;
        visited = new HashSet<>();
        subExpression = pSubExp;
      }

      private ValueLiteralVisitor(Address pAddress, ValueLiterals pValueLiterals,
          CExpression pSubExp, Set<Pair<CType, Address>> pVisited) {
        address = pAddress;
        valueLiterals = pValueLiterals;
        visited = pVisited;
        subExpression = pSubExp;
      }

      @Override
      public Void visitDefault(CType pT) {
        return null;
      }

      @Override
      public Void visit(CTypedefType pT) {
        return pT.getRealType().accept(this);
      }

      @Override
      public Void visit(CElaboratedType pT) {

        CType realType = pT.getRealType();

        if (realType == null) {
          return null;
        }

        return realType.getCanonicalType().accept(this);
      }

      @Override
      public Void visit(CEnumType pT) {
        return null;
      }

      @Override
      public Void visit(CBitFieldType pCBitFieldType) throws RuntimeException {
        return pCBitFieldType.getType().accept(this);
      }

      @Override
      public Void visit(CCompositeType compType) {

        if (compType.getKind() == ComplexTypeKind.ENUM) {
          // TODO Enum
        }

        if (compType.getKind() == ComplexTypeKind.UNION) {
          //TODO Union
        }

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

        Map<CCompositeTypeMemberDeclaration, BigInteger> offsets =
            getFieldOffsetsInBits(pCompType, machineModel, Function.identity());

        for (Map.Entry<CCompositeTypeMemberDeclaration, BigInteger> memberOffset :
            offsets.entrySet()) {
          CCompositeTypeMemberDeclaration memberType = memberOffset.getKey();
          handleMemberField(memberType, address.addOffset(memberOffset.getValue()));
        }
      }

      private void handleMemberField(CCompositeTypeMemberDeclaration pType, Address fieldAddress) {
        CType expectedType = pType.getType().getCanonicalType();

        assert isStructOrUnionType(subExpression.getExpressionType().getCanonicalType());

        CExpression subExp;
        boolean isPointerDeref;

        if (subExpression instanceof CPointerExpression) {
          // *a.b <=> a->b
          subExp = ((CPointerExpression) subExpression).getOperand();
          isPointerDeref = true;
        } else {
          subExp = subExpression;
          isPointerDeref = false;
        }

        CFieldReference fieldReference = new CFieldReference(
            subExp.getFileLocation(), expectedType, pType.getName(), subExp,
            isPointerDeref);

        Object fieldValue;

        // Arrays and structs are represented as addresses
        if (expectedType instanceof CArrayType
            || isStructOrUnionType(expectedType)) {
          fieldValue = fieldAddress;
        } else {
          fieldValue = concreteState.getValueFromMemory(fieldReference, fieldAddress);
        }

        if (fieldValue == null) {
          return;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = Address.getUnknownAddress();

        if (expectedType instanceof CSimpleType) {
          valueLiteral = getValueLiteral(((CSimpleType) expectedType), fieldValue);
        } else {
          valueAddress = Address.valueOf(fieldValue);

          if(valueAddress.isUnknown()) {
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
          SubExpressionValueLiteral subExpression = new SubExpressionValueLiteral(valueLiteral, fieldReference);
          valueLiterals.addSubExpressionValueLiteral(subExpression);
        }

        if (valueAddress != null) {
          ValueLiteralVisitor v =
              new ValueLiteralVisitor(valueAddress, valueLiterals, fieldReference, visited);
          expectedType.accept(v);
        }
      }

      @Override
      public Void visit(CArrayType arrayType) {

        CType expectedType = arrayType.getType().getCanonicalType();

        int subscript = 0;

        boolean memoryHasValue = true;
        while (memoryHasValue) {
          memoryHasValue = handleArraySubscript(address, subscript, expectedType, arrayType);
          subscript++;
        }

        return null;
      }

      private boolean handleArraySubscript(Address pArrayAddress,
          int pSubscript, CType pExpectedType,
          CArrayType pArrayType) {
        if (!pArrayAddress.isConcrete()) {
          return false;
        }

        int typeSize = machineModel.getBitSizeof(pExpectedType);
        int subscriptOffset = pSubscript * typeSize;

        // Check if we are already out of array bound, if we have an array length.
        // FIXME Imprecise due to imprecise getSizeOf method
        if (!pArrayType.isIncomplete()
            && machineModel.getBitSizeof(pArrayType) <= subscriptOffset) {
          return false;
        }
        if (pArrayType.getLength() == null) {
          return false;
        }

        Address address = pArrayAddress.addOffset(BigInteger.valueOf(subscriptOffset));

        BigInteger subscript = BigInteger.valueOf(pSubscript);
        CIntegerLiteralExpression litExp =
            new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, subscript);
        CArraySubscriptExpression arraySubscript =
            new CArraySubscriptExpression(subExpression.getFileLocation(), pExpectedType, subExpression, litExp);

        Object value;

        if (isStructOrUnionType(pExpectedType) || pExpectedType instanceof CArrayType) {
          // Arrays and structs are represented as addresses
          value = address;
        } else {
          value = concreteState.getValueFromMemory(arraySubscript, address);
        }

        if (value == null) {
          return false;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = Address.getUnknownAddress();

        if (pExpectedType instanceof CSimpleType) {
          valueLiteral = getValueLiteral(((CSimpleType) pExpectedType), value);
        } else {
          valueAddress = Address.valueOf(value);

          if(valueAddress.isUnknown()) {
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

          ValueLiteralVisitor v = new ValueLiteralVisitor(valueAddress, valueLiterals, arraySubscript, visited);
          pExpectedType.accept(v);
        }

        // the check if the array continued was performed at an earlier stage in this function
        return true;
      }

      @Override
      public Void visit(CPointerType pointerType) {

        CType expectedType = pointerType.getType().getCanonicalType();

        CPointerExpression pointerExp = new CPointerExpression(subExpression.getFileLocation(), expectedType, subExpression);

        Object value;

        if (isStructOrUnionType(expectedType) || expectedType instanceof CArrayType) {
          // Arrays and structs are represented as addresses

          value = address;
        } else {
          value = concreteState.getValueFromMemory(pointerExp, address);
        }

        if (value == null) {
          return null;
        }

        ValueLiteral valueLiteral;
        Address valueAddress = Address.getUnknownAddress();

        if (expectedType instanceof CSimpleType) {
          valueLiteral = getValueLiteral(((CSimpleType) expectedType), value);
        } else {
          valueAddress = Address.valueOf(value);

          if(valueAddress.isUnknown()) {
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

          ValueLiteralVisitor v = new ValueLiteralVisitor(valueAddress, valueLiterals, pointerExp, visited);
          expectedType.accept(v);

        }

        return null;
      }
    }

    /*Resolve structs or union fields that are stored in the variable environment*/
    private class ValueLiteralStructResolver extends DefaultCTypeVisitor<Void, RuntimeException> {

      private final ValueLiterals valueLiterals;
      private final String functionName;
      private final CExpression prevSub;

      public ValueLiteralStructResolver(ValueLiterals pValueLiterals,
          String pFunctionName, CFieldReference pPrevSub) {
        valueLiterals = pValueLiterals;
        functionName = pFunctionName;
        prevSub = pPrevSub;
      }

      public ValueLiteralStructResolver(ValueLiterals pValueLiterals, String pFunctionName, CIdExpression pOwner) {
        valueLiterals = pValueLiterals;
        functionName = pFunctionName;
        prevSub = pOwner;
      }

      @Override
      public Void visitDefault(CType pT) {
        return null;
      }

      @Override
      public Void visit(CElaboratedType type) {

        CType realType = type.getRealType();

        if (realType == null) {
          return null;
        }

        return realType.getCanonicalType().accept(this);
      }

      @Override
      public Void visit(CTypedefType pType) {
        return pType.getRealType().accept(this);
      }

      @Override
      public Void visit(CBitFieldType pCBitFieldType) throws RuntimeException {
        return pCBitFieldType.getType().accept(this);
      }

      @Override
      public Void visit(CCompositeType compType) {

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
            new ValueLiteralStructResolver(valueLiterals,
                functionName, reference);

        pMemberType.accept(resolver);
      }

      private void addStructSubexpression(Object pFieldValue, CFieldReference reference) {

        CType realType = reference.getExpressionType();

        ValueLiteral valueLiteral;
        Address valueAddress = Address.getUnknownAddress();

        if (realType instanceof CSimpleType) {
          valueLiteral = getValueLiteral(((CSimpleType) realType), pFieldValue);
        } else {
          valueAddress = Address.valueOf(pFieldValue);

          if(valueAddress.isUnknown()) {
            return;
          }

          valueLiteral = ExplicitValueLiteral.valueOf(valueAddress, machineModel);
        }


        if (valueLiteral.isUnknown()) {
          return;
        }

        SubExpressionValueLiteral subExpression = new SubExpressionValueLiteral(valueLiteral, reference);
        valueLiterals.addSubExpressionValueLiteral(subExpression);
      }
    }

  }

  private final static class ValueLiterals {

    /*Contains values for possible sub expressions */
    private final List<SubExpressionValueLiteral> subExpressionValueLiterals = new ArrayList<>();

    private final ValueLiteral expressionValueLiteral;

    public ValueLiterals() {
      expressionValueLiteral = UnknownValueLiteral.getInstance();
    }

    public ValueLiterals(ValueLiteral valueLiteral) {
      expressionValueLiteral = valueLiteral;
    }

    public CExpression getExpressionValueLiteralAsCExpression() {
      return expressionValueLiteral.getValueLiteral();
    }

    public void addSubExpressionValueLiteral(SubExpressionValueLiteral code) {
      subExpressionValueLiterals.add(code);
    }

    public boolean hasUnknownValueLiteral() {
      return expressionValueLiteral.isUnknown();
    }

    public Set<SubExpressionValueLiteral> getSubExpressionValueLiteral() {
      return ImmutableSet.copyOf(subExpressionValueLiterals);
    }

    @Override
    public String toString() {

      StringBuilder result = new StringBuilder();

      result.append("ValueLiteral : ");
      result.append(expressionValueLiteral.toString());
      result.append(", SubValueLiterals : ");
      Joiner joiner = Joiner.on(", ");
      result.append(joiner.join(subExpressionValueLiterals));

      return result.toString();
    }
  }

  private static interface ValueLiteral {

    public CExpression getValueLiteral();
    public boolean isUnknown();

    public ValueLiteral addCast(CSimpleType pType);
  }

  private static class UnknownValueLiteral implements ValueLiteral {

    private static final UnknownValueLiteral instance = new UnknownValueLiteral();

    private UnknownValueLiteral() {}

    public static UnknownValueLiteral getInstance() {
      return instance;
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

  private static class ExplicitValueLiteral implements ValueLiteral {

    private final CLiteralExpression explicitValueLiteral;

    protected ExplicitValueLiteral(CLiteralExpression pValueLiteral) {
      explicitValueLiteral = pValueLiteral;
    }

    public static ValueLiteral valueOf(Address address, MachineModel pMachineModel) {

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

      CCastExpression castExpression = new CCastExpression(castedValue.getFileLocation(), pType, castedValue);
      return new CastedExplicitValueLiteral(explicitValueLiteral, castExpression);
    }

    public static ValueLiteral valueOf(BigInteger value, CSimpleType pType) {
      CIntegerLiteralExpression literal = new CIntegerLiteralExpression(FileLocation.DUMMY, pType, value);
      return new ExplicitValueLiteral(literal);
    }

    public static ValueLiteral valueOf(BigDecimal value, CSimpleType pType) {

      CFloatLiteralExpression literal = new CFloatLiteralExpression(FileLocation.DUMMY, pType, value);
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

    protected CastedExplicitValueLiteral(CLiteralExpression pValueLiteral, CCastExpression exp) {
      super(pValueLiteral);
      castExpression = exp;
    }

    @Override
    public CExpression getValueLiteral() {
      return castExpression;
    }
  }

  private static final class SubExpressionValueLiteral {

    private final ValueLiteral valueLiteral;
    private final CLeftHandSide subExpression;

    private SubExpressionValueLiteral(ValueLiteral pValueLiteral, CLeftHandSide pSubExpression) {
      valueLiteral = pValueLiteral;
      subExpression = pSubExpression;
    }

    public CExpression getValueLiteralAsCExpression() {
      return valueLiteral.getValueLiteral();
    }

    public CLeftHandSide getSubExpression() {
      return subExpression;
    }
  }

  private static @Nullable BigInteger getFieldOffsetInBits(
      CCompositeType ownerType, String fieldName, MachineModel pMachineModel) {
    return getFieldOffsetsInBits(ownerType, pMachineModel, CCompositeTypeMemberDeclaration::getName)
        .get(fieldName);
  }

  private static <K> Map<K, BigInteger> getFieldOffsetsInBits(
      CCompositeType ownerType,
      MachineModel pMachineModel,
      Function<CCompositeTypeMemberDeclaration, K> pKeyFunction) {

    List<CCompositeTypeMemberDeclaration> membersOfType = ownerType.getMembers();
    Map<K, BigInteger> result = Maps.newLinkedHashMapWithExpectedSize(membersOfType.size());

    BigInteger bitOffset = BigInteger.ZERO;

    if (ownerType.getKind() == ComplexTypeKind.STRUCT) {
      BigInteger sizeOfConsecutiveBitFields = BigInteger.ZERO;
      BaseSizeofVisitor sizeofVisitor = new BaseSizeofVisitor(pMachineModel);

      for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
        result.put(pKeyFunction.apply(typeMember), bitOffset.add(sizeOfConsecutiveBitFields));

        CType type = typeMember.getType();
        if (type.isIncomplete()) {
          return result;
        }

        BigInteger fieldSizeInBits = BigInteger.valueOf(pMachineModel.getBitSizeof(type));

        if (typeMember.getType() instanceof CBitFieldType) {
          sizeOfConsecutiveBitFields = sizeOfConsecutiveBitFields.add(fieldSizeInBits);
        } else {
          bitOffset = bitOffset.add(sizeOfConsecutiveBitFields);
          sizeOfConsecutiveBitFields = BigInteger.ZERO;
          int byteSize = sizeofVisitor.calculateByteSize(bitOffset.intValue());
          BigInteger padding =
              BigInteger.valueOf(pMachineModel.getPadding(byteSize, typeMember.getType()));
          bitOffset = bitOffset.add(padding);
          bitOffset = bitOffset.add(fieldSizeInBits);
        }
      }
    } else if (ownerType.getKind() == ComplexTypeKind.UNION) {
      for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
        result.put(pKeyFunction.apply(typeMember), BigInteger.ZERO);
      }
    }
    return result;
  }

  private static @Nullable BigInteger getFieldOffsetInBits(
      CType ownerType, String fieldName, MachineModel pMachineModel) {

    if (ownerType instanceof CElaboratedType) {

      CType realType = ((CElaboratedType) ownerType).getRealType();

      if (realType == null) {
        return null;
      }

      return getFieldOffsetInBits(realType.getCanonicalType(), fieldName, pMachineModel);
    } else if (ownerType instanceof CCompositeType) {
      return getFieldOffsetInBits((CCompositeType) ownerType, fieldName, pMachineModel);
    } else if (ownerType instanceof CPointerType) {

      /* We do not explicitly transform x->b,
      so when we try to get the field b the ownerType of x
      is a pointer type.*/

      CType type = ((CPointerType) ownerType).getType().getCanonicalType();

      return getFieldOffsetInBits(type, fieldName, pMachineModel);
    }

    throw new AssertionError();
  }

}
