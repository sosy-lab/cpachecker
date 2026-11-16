// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAIndexProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula.AcslTypeHelper.BinaryTermData;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class AcslTermToFormulaVisitor extends SSAIndexProvider<AcslType>
    implements AcslTermVisitor<Formula, NoException> {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final SSAMapBuilder currentSsa;
  private final Optional<SSAMap>
      functionEntrySsa; // Optional SSA map for function-entry state (\old)
  private final CToFormulaConverterWithPointerAliasing ctoFormulaConverter;
  private final MachineModel machineModel;
  private final AcslTypeHelper typeHelper;
  private final PointerTargetSetBuilder
      ptsb; // needed for CRightHandSideVisitor to convert AcslCExpressions
  private final PointerTargetSet
      originalPts; // copy to ensure we do not accidentally modify the original pts
  private final Constraints
      constraints; // needed for CRightHandSideVisitor to convert AcslCExpressions

  public AcslTermToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter,
      MachineModel pMachineModel,
      PointerTargetSetBuilder pPtsb,
      Constraints pCon) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = Optional.empty();
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.machineModel = pMachineModel;
    this.typeHelper = new AcslTypeHelper(pMachineModel, pFmgr, pCtoFormulaConverter);
    this.ptsb = pPtsb;
    this.originalPts = pPtsb.build();
    this.constraints = pCon;
  }

  public AcslTermToFormulaVisitor(
      FormulaManagerView pFmgr,
      SSAMapBuilder pCurrentSsa,
      SSAMap pFunctionEntrySsa,
      CToFormulaConverterWithPointerAliasing pCtoFormulaConverter,
      MachineModel pMachineModel,
      PointerTargetSetBuilder pPtsb,
      Constraints pCon) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    checkNotNull(pMachineModel);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = Optional.ofNullable(pFunctionEntrySsa);
    this.ctoFormulaConverter = pCtoFormulaConverter;
    this.machineModel = pMachineModel;
    this.typeHelper = new AcslTypeHelper(pMachineModel, pFmgr, pCtoFormulaConverter);
    this.ptsb = pPtsb;
    this.originalPts = pPtsb.build();
    this.constraints = pCon;
  }

  @Override
  public Formula visit(AcslUnaryTerm pAcslUnaryTerm) {
    // TODO check negation vs bitwise complementation according to ACSL standard, see Issue #1640
    return switch (pAcslUnaryTerm.getOperator()) {
      case SIZEOF -> throw new UnsupportedOperationException("Not yet implemented");
      // unary plus should not change the value
      case PLUS -> pAcslUnaryTerm.getOperand().accept(this);
      case MINUS ->
          unaryToCExpression(pAcslUnaryTerm)
              .map(this::convertCExpression)
              .orElseGet(() -> fmgr.makeNegate(pAcslUnaryTerm.getOperand().accept(this)));
      case POINTER_DEREFERENCE, ADDRESS_OF ->
          convertCExpression(toCExpressionOrFail(pAcslUnaryTerm));
      case NEGATION -> fmgr.makeNot(pAcslUnaryTerm.getOperand().accept(this));
    };
  }

  @Override
  public Formula visit(AcslStringLiteralTerm pAcslStringLiteralTerm) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslRealLiteralTerm pAcslRealLiteralTerm) {
    return fmgr.getRationalFormulaManager().makeNumber(pAcslRealLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslCharLiteralTerm pAcslCharLiteralTerm) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslIntegerLiteralTerm pAcslIntegerLiteralTerm) {
    return fmgr.getIntegerFormulaManager().makeNumber(pAcslIntegerLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslBooleanLiteralTerm pAcslBooleanLiteralTerm) {
    return fmgr.getBooleanFormulaManager().makeBoolean(pAcslBooleanLiteralTerm.getValue());
  }

  @Override
  public Formula visit(AcslBinaryTerm pAcslBinaryTerm) {
    Optional<CExpression> cExpression = binaryToCExpression(pAcslBinaryTerm);
    if (cExpression.isPresent()) {
      return convertCExpression(cExpression.orElseThrow());
    }

    AcslType operand1Type = pAcslBinaryTerm.getOperand1().getExpressionType();
    AcslType operand2Type = pAcslBinaryTerm.getOperand2().getExpressionType();
    AcslType termType = pAcslBinaryTerm.getExpressionType();

    Formula operand1Formula = pAcslBinaryTerm.getOperand1().accept(this);
    Formula operand2Formula = pAcslBinaryTerm.getOperand2().accept(this);

    BinaryTermData result =
        typeHelper.handleBinaryTerm(
            termType, operand1Type, operand2Type, operand1Formula, operand2Formula);

    Boolean signed = result.signed();
    operand1Formula = result.f1();
    operand2Formula = result.f2();

    AcslBinaryTermOperator operator = pAcslBinaryTerm.getOperator();
    boolean bitvectors =
        operand1Formula instanceof BitvectorFormula && operand2Formula instanceof BitvectorFormula;
    boolean booleans =
        operand1Formula instanceof BooleanFormula && operand2Formula instanceof BooleanFormula;
    boolean isShift =
        operator == AcslBinaryTermOperator.SHIFT_LEFT
            || operator == AcslBinaryTermOperator.SHIFT_RIGHT;
    if ((isShift && !bitvectors)
        || (AcslBinaryTermOperator.isBitwiseOperator(operator) && !bitvectors && !booleans)) {
      throw new UnsupportedOperationException(
          "The operator " + operator + " needs bitvectors, not " + pAcslBinaryTerm.toASTString());
    }

    return switch (operator) {
      case BINARY_AND -> fmgr.makeAnd(operand1Formula, operand2Formula);
      case BINARY_OR -> fmgr.makeOr(operand1Formula, operand2Formula);
      // bitwise a -> b is the same as bitwise not a or b
      case BINARY_IMPLICATION -> fmgr.makeOr(fmgr.makeNot(operand1Formula), operand2Formula);
      // bitwise a <-> b is the same as bitwise not (a xor b)
      case BINARY_EQUIVALENT -> fmgr.makeNot(fmgr.makeXor(operand1Formula, operand2Formula));
      case BINARY_XOR -> fmgr.makeXor(operand1Formula, operand2Formula);
      case PLUS -> fmgr.makePlus(operand1Formula, operand2Formula);
      case MINUS -> fmgr.makeMinus(operand1Formula, operand2Formula);
      case MULTIPLY -> fmgr.makeMultiply(operand1Formula, operand2Formula);
      case DIVIDE -> fmgr.makeDivide(operand1Formula, operand2Formula, signed);
      case MODULO -> fmgr.makeRemainder(operand1Formula, operand2Formula, signed);
      case SHIFT_LEFT -> fmgr.makeShiftLeft(operand1Formula, operand2Formula);
      case SHIFT_RIGHT -> fmgr.makeShiftRight(operand1Formula, operand2Formula, signed);
    };
  }

  @Override
  public Formula visit(AcslIdTerm pAcslIdTerm) {
    AcslSimpleDeclaration variable = pAcslIdTerm.getDeclaration();
    if (variable instanceof AcslCVariableDeclaration) {
      // The C encoding knows the names and the memory of program variables
      return convertCExpression(toCExpressionOrFail(pAcslIdTerm));
    }
    String varName = variable.getName();
    // Quantifier variables do not need tracking in the SSA map, use index 1 for them
    int useIndex = varName.startsWith("ACSL#q") ? 1 : getIndex(varName, variable.getType());

    return fmgr.makeVariable(
        typeHelper.acslTypeToFormulaType(pAcslIdTerm.getExpressionType()), varName, useIndex);
  }

  @Override
  public Formula visit(AcslOldTerm pAcslOldTerm) {
    if (functionEntrySsa.isEmpty()) {
      throw new UnsupportedOperationException(
          "\\old is not available without a SSA map at function entry");
    }

    AcslTermToFormulaVisitor oldVisitor =
        new AcslTermToFormulaVisitor(
            fmgr,
            functionEntrySsa.orElseThrow().builder(),
            functionEntrySsa.orElseThrow(),
            ctoFormulaConverter,
            machineModel,
            ptsb,
            constraints);

    return pAcslOldTerm.getTerm().accept(oldVisitor);
  }

  @Override
  public Formula visit(AcslResultTerm pAcslResultTerm) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslAtTerm pAcslAtTerm) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Formula visit(AcslTernaryTerm pAcslTernaryTerm) {
    AcslPredicateToFormulaVisitor predicateVisitor =
        new AcslPredicateToFormulaVisitor(
            fmgr, this, functionEntrySsa, ctoFormulaConverter, machineModel, ptsb, constraints);
    BooleanFormula conditionFormula = pAcslTernaryTerm.getCondition().accept(predicateVisitor);
    Formula ifTrueFormula = pAcslTernaryTerm.getResultIfTrue().accept(this);
    Formula ifFalseFormula = pAcslTernaryTerm.getResultIfFalse().accept(this);

    return bfmgr.ifThenElse(conditionFormula, ifTrueFormula, ifFalseFormula);
  }

  @Override
  public Formula visit(AcslFunctionCallTerm pAcslFunctionCallTerm) {

    AcslFunctionDeclaration declaration = pAcslFunctionCallTerm.getDeclaration();
    FormulaType<?> returnType =
        typeHelper.acslTypeToFormulaType(declaration.getType().getReturnType());

    String functionName = "ACSL#" + declaration.getQualifiedName();

    List<Formula> params = new ArrayList<>();
    for (AcslTerm param : pAcslFunctionCallTerm.getParameterExpressions()) {
      params.add(param.accept(this));
    }

    // Calling declareAndCallUF multiple times with the same function name does not seem to create
    // any issues (see: AcslToFormulaVisitorsTest.testPredicateApplication(), where this also
    // happens)
    return fmgr.getFunctionFormulaManager().declareAndCallUF(functionName, returnType, params);
  }

  @Override
  public Formula visit(AcslArraySubscriptTerm pAcslArraySubscriptTerm) {
    return convertCExpression(toCExpressionOrFail(pAcslArraySubscriptTerm));
  }

  @Override
  public Formula visit(AcslCExpression pAcslCExpression) {
    return convertCExpression(pAcslCExpression.getCExpression());
  }

  Formula convertCExpression(CExpression pExpression) {
    try {
      Formula f = cExpressionToFormula(pExpression, ptsb);
      Verify.verify(
          ptsb.build().equals(originalPts)); // make sure we did not modify the original pts
      return f;
    } catch (UnrecognizedCodeException e) {
      throw new UnsupportedOperationException(e.getMessage(), e);
    }
  }

  /**
   * Returns the term as C expression, if it consists only of program variables, memory accesses,
   * literals, and operators of C. The C encoding converts such a term with the semantics of C,
   * which avoids conversions between bitvectors and integers.
   */
  Optional<CExpression> toCExpression(AcslTerm pTerm) {
    return switch (pTerm) {
      case AcslIdTerm id when id.getDeclaration() instanceof AcslCVariableDeclaration variable ->
          Optional.of(new CIdExpression(id.getFileLocation(), variable.getCVariableDeclaration()));
      case AcslIntegerLiteralTerm literal -> integerLiteral(literal);
      case AcslCharLiteralTerm literal ->
          Optional.of(
              new CCharLiteralExpression(
                  literal.getFileLocation(), CNumericTypes.CHAR, literal.getValue()));
      case AcslCExpression expression -> Optional.of(expression.getCExpression());
      case AcslArraySubscriptTerm subscript -> {
        Optional<CExpression> array = toCExpression(subscript.getArrayExpression());
        Optional<CExpression> index = toCExpression(subscript.getSubscriptExpression());
        Optional<CType> elementType = array.flatMap(AcslTermToFormulaVisitor::targetType);
        yield array.isPresent() && index.isPresent() && elementType.isPresent()
            ? Optional.of(
                new CArraySubscriptExpression(
                    subscript.getFileLocation(),
                    elementType.orElseThrow(),
                    array.orElseThrow(),
                    index.orElseThrow()))
            : Optional.empty();
      }
      case AcslUnaryTerm unary -> unaryToCExpression(unary);
      case AcslBinaryTerm binary -> binaryToCExpression(binary);
      default -> Optional.empty();
    };
  }

  private Optional<CExpression> integerLiteral(AcslIntegerLiteralTerm pLiteral) {
    for (CSimpleType type : ImmutableList.of(CNumericTypes.INT, CNumericTypes.LONG_LONG_INT)) {
      if (machineModel.getMinimalIntegerValue(type).compareTo(pLiteral.getValue()) <= 0
          && pLiteral.getValue().compareTo(machineModel.getMaximalIntegerValue(type)) <= 0) {
        return Optional.of(
            new CIntegerLiteralExpression(pLiteral.getFileLocation(), type, pLiteral.getValue()));
      }
    }
    return Optional.empty();
  }

  private Optional<CExpression> unaryToCExpression(AcslUnaryTerm pTerm) {
    Optional<CExpression> operand = toCExpression(pTerm.getOperand());
    if (operand.isEmpty()) {
      return Optional.empty();
    }
    CExpression cOperand = operand.orElseThrow();
    FileLocation location = pTerm.getFileLocation();
    return switch (pTerm.getOperator()) {
      case PLUS -> operand;
      case MINUS ->
          Optional.of(
              new CUnaryExpression(
                  location, cOperand.getExpressionType(), cOperand, UnaryOperator.MINUS));
      case POINTER_DEREFERENCE ->
          targetType(cOperand).map(type -> new CPointerExpression(location, type, cOperand));
      case ADDRESS_OF ->
          Optional.of(
              new CUnaryExpression(
                  location,
                  new CPointerType(CTypeQualifiers.NONE, cOperand.getExpressionType()),
                  cOperand,
                  UnaryOperator.AMPER));
      case NEGATION, SIZEOF -> Optional.empty();
    };
  }

  private Optional<CExpression> binaryToCExpression(AcslBinaryTerm pTerm) {
    Optional<BinaryOperator> operator =
        switch (pTerm.getOperator()) {
          case PLUS -> Optional.of(BinaryOperator.PLUS);
          case MINUS -> Optional.of(BinaryOperator.MINUS);
          case MULTIPLY -> Optional.of(BinaryOperator.MULTIPLY);
          case DIVIDE -> Optional.of(BinaryOperator.DIVIDE);
          case MODULO -> Optional.of(BinaryOperator.REMAINDER);
          case SHIFT_LEFT -> Optional.of(BinaryOperator.SHIFT_LEFT);
          case SHIFT_RIGHT -> Optional.of(BinaryOperator.SHIFT_RIGHT);
          case BINARY_AND -> Optional.of(BinaryOperator.BITWISE_AND);
          case BINARY_OR -> Optional.of(BinaryOperator.BITWISE_OR);
          case BINARY_XOR -> Optional.of(BinaryOperator.BITWISE_XOR);
          case BINARY_IMPLICATION, BINARY_EQUIVALENT -> Optional.empty();
        };
    return binaryToCExpression(pTerm.getOperand1(), pTerm.getOperand2(), operator);
  }

  /** Returns the C expression with the operator, if both operands are C expressions. */
  Optional<CExpression> binaryToCExpression(
      AcslTerm pOperand1, AcslTerm pOperand2, Optional<BinaryOperator> pOperator) {
    Optional<CExpression> operand1 = toCExpression(pOperand1);
    Optional<CExpression> operand2 = toCExpression(pOperand2);
    if (pOperator.isEmpty() || operand1.isEmpty() || operand2.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          new CBinaryExpressionBuilder(machineModel, LogManager.createNullLogManager())
              .buildBinaryExpression(
                  operand1.orElseThrow(), operand2.orElseThrow(), pOperator.orElseThrow()));
    } catch (UnrecognizedCodeException e) {
      return Optional.empty();
    }
  }

  private static Optional<CType> targetType(CExpression pPointer) {
    return switch (pPointer.getExpressionType().getCanonicalType()) {
      case CPointerType pointer -> Optional.of(pointer.getType());
      case CArrayType array -> Optional.of(array.getType());
      default -> Optional.empty();
    };
  }

  private CExpression toCExpressionOrFail(AcslTerm pTerm) {
    return toCExpression(pTerm)
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "The C encoding cannot convert " + pTerm.toASTString()));
  }

  /**
   * Returns the index of a variable in the ssa map, or creates one with value 1 for new variables
   *
   * @return the index of the variable
   */
  private int getIndex(String name, AcslType type) {
    Type existingType = currentSsa.getType(name);
    if (existingType != null && !type.equals(existingType)) {
      throw new IllegalArgumentException(
          "Variable "
              + name
              + " has conflicting types: "
              + currentSsa.getType(name)
              + " and "
              + type);
    }

    return super.getExistingOrNewIndex(name, type, currentSsa);
  }

  public Formula cExpressionToFormula(CExpression cExpr, PointerTargetSetBuilder pPts)
      throws UnrecognizedCodeException {

    CFAEdge dummyEdge =
        new BlankEdge(
            "",
            FileLocation.DUMMY,
            CFANode.newDummyCFANode("dummy-1"),
            CFANode.newDummyCFANode("dummy-2"),
            "Dummy Edge");

    ErrorConditions errorConditions = new ErrorConditions(bfmgr);

    // this has the side effect of sometimes adding to the constraints, if the CExpression contains
    // an array access
    CRightHandSideVisitor<Formula, UnrecognizedCodeException> exprVisitor =
        ctoFormulaConverter.createCRightHandSideVisitor(
            dummyEdge, "dummy-function-name", currentSsa, pPts, constraints, errorConditions);
    return cExpr.accept(exprVisitor);
  }
}
