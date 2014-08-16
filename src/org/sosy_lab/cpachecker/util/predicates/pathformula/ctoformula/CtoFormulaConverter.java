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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.DummyPointerTargetSetBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Class containing all the code that converts C code into a formula.
 */
public class CtoFormulaConverter {

  // list of functions that are pure (no side-effects from the perspective of this analysis)
  public static final Set<String> PURE_EXTERNAL_FUNCTIONS
      = ImmutableSet.of("abort", "exit", "__assert_fail", "__VERIFIER_error",
          "free", "kfree",
          "fprintf", "printf", "puts", "printk", "sprintf", "swprintf",
          "strcasecmp", "strchr", "strcmp", "strlen", "strncmp", "strrchr", "strstr"
          );

  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  public static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  //names for special variables needed to deal with functions
  protected static final String RETURN_VARIABLE_NAME = "__retval__";
  protected static final String PARAM_VARIABLE_NAME = "__param__";

  private static final Set<String> SAFE_VAR_ARG_FUNCTIONS = ImmutableSet.of(
      "printf", "printk"
      );

  private static final String SCOPE_SEPARATOR = "::";

  private final Map<String, Formula> stringLitToFormula = new HashMap<>();
  private int nextStringLitIndex = 0;

  final FormulaEncodingOptions options;
  protected final MachineModel machineModel;
  private final Optional<VariableClassification> variableClassification;
  final CtoFormulaTypeHandler typeHandler;

  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bfmgr;
  private final NumeralFormulaManagerView<NumeralFormula, RationalFormula> nfmgr;
  private final BitvectorFormulaManagerView efmgr;
  protected final FunctionFormulaManagerView ffmgr;
  protected final LogManagerWithoutDuplicates logger;
  protected final ShutdownNotifier shutdownNotifier;

  public static final int          VARIABLE_UNSET          = SSAMap.DEFAULT_DEFAULT_IDX;
  static final int                 VARIABLE_INSTANTIATED   = 1;
  static final int                 VARIABLE_UNINITIALIZED  = VARIABLE_INSTANTIATED + SSAMap.DEFAULT_INCREMENT;

  private final FunctionFormulaType<?> stringUfDecl;

  private final HashSet<CVariableDeclaration> globalDeclarations = new HashSet<>()
          ;
  public CtoFormulaConverter(FormulaEncodingOptions pOptions, FormulaManagerView fmgr,
      MachineModel pMachineModel, Optional<VariableClassification> pVariableClassification,
      LogManager logger, ShutdownNotifier pShutdownNotifier,
      CtoFormulaTypeHandler pTypeHandler) {

    this.fmgr = fmgr;
    this.options = pOptions;
    this.machineModel = pMachineModel;
    this.variableClassification = pVariableClassification;
    this.typeHandler = pTypeHandler;

    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.nfmgr = fmgr.getRationalFormulaManager();
    this.efmgr = fmgr.getBitvectorFormulaManager();
    this.ffmgr = fmgr.getFunctionFormulaManager();
    this.logger = new LogManagerWithoutDuplicates(logger);
    this.shutdownNotifier = pShutdownNotifier;

    stringUfDecl = ffmgr.createFunction(
            "__string__", typeHandler.getPointerType(), FormulaType.RationalType);
  }

  void logfOnce(Level level, CFAEdge edge, String msg, Object... args) {
    if (logger.wouldBeLogged(level)) {
      logger.logfOnce(level, "%s: %s: %s",
          edge.getFileLocation(),
          String.format(msg, args),
          edge.getDescription());
    }
  }

  /**
   * Returns the size in bytes of the given type.
   * Always use this method instead of machineModel.getSizeOf,
   * because this method can handle dereference-types.
   * @param pType the type to calculate the size of.
   * @return the size in bytes of the given type.
   */
  protected int getSizeof(CType pType) {
    return typeHandler.getSizeof(pType);
  }

  protected boolean isRelevantField(final CCompositeType compositeType,
                          final String fieldName) {
    return !variableClassification.isPresent() ||
           !options.ignoreIrrelevantVariables() ||
           variableClassification.get().getRelevantFields().containsEntry(compositeType, fieldName);
  }

  protected boolean isRelevantLeftHandSide(final CLeftHandSide lhs) {
    if (options.ignoreIrrelevantVariables() && variableClassification.isPresent()) {
      return lhs.accept(new IsRelevantLhsVisitor(this));
    } else {
      return true;
    }
  }

  protected final boolean isRelevantVariable(final CSimpleDeclaration var) {
    if (options.ignoreIrrelevantVariables() && variableClassification.isPresent()) {
      return var.getName().equals(RETURN_VARIABLE_NAME) ||
           variableClassification.get().getRelevantVariables().contains(var.getQualifiedName());
    }
    return true;
  }

  public FormulaType<?> getFormulaTypeFromCType(CType type) {
    return typeHandler.getFormulaTypeFromCType(type);
  }

  /** prefixes function to variable name
  * Call only if you are sure you have a local variable!
  */
  public static String scoped(String var, String function) {
    return (function + SCOPE_SEPARATOR + var).intern();
  }

  /**
   * This method eleminates all spaces from an expression's ASTString and returns
   * the new String.
   *
   * @param e the expression which should be named
   * @return the name of the expression
   */
  public static String exprToVarName(IAstNode e) {
    return e.toASTString().replaceAll("[ \n\t]", "");
  }

  /**
   * Produces a fresh new SSA index for an assignment
   * and updates the SSA map.
   */
  protected int makeFreshIndex(String name, CType type, SSAMapBuilder ssa) {
    return getIndex(name, type, ssa, true, true);
  }

  /**
   * Produces a fresh new SSA index for an assignment,
   * but does _not_ update the SSA map.
   */
  protected int getFreshIndex(String name, CType type, SSAMapBuilder ssa) {
    return getIndex(name, type, ssa, true, false);
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there
   * is none, it creates one with the value 1.
   *
   * @return the index of the variable
   */
  protected int getIndex(String name, CType type, SSAMapBuilder ssa) {
    return getIndex(name, type, ssa, false, true);
  }

  private int getIndex(String name, CType type, SSAMapBuilder ssa, boolean makeFresh, boolean set) {
    int idx = ssa.getIndex(name);
    checkSsaSavedType(name, type, ssa.getType(name));
    if (makeFresh) {
      if (idx > 0) {
        idx = ssa.getFreshIndex(name);
      } else {
        idx = VARIABLE_UNINITIALIZED; // AG - IMPORTANT!!! We must start from 2 and
        // not from 1, because this is an assignment,
        // so the SSA index must be fresh.
      }
      if (set) {
        ssa.setIndex(name, type, idx);
      }
    } else {
      if (idx <= 0) {
        logger.log(Level.ALL, "WARNING: Auto-instantiating variable:", name);
        idx = VARIABLE_INSTANTIATED;
        if (set) {
          ssa.setIndex(name, type, idx);
        }
      }
    }

    return idx;
  }

  protected void checkSsaSavedType(String name, CType type, CType t) {

    // Check if types match

    // Assert when a variable already exists, that it has the same type
    // TODO: Un-comment when parser and code-base is stable enough
//    Variable t;
//    assert
//         (t = ssa.getType(name)) == null
//      || CTypeUtils.equals(t, type)
//      : "Saving variables with mutliple types is not possible!";
    if (t != null && !areEqualWithMatchingPointerArray(t, type)) {

      if (getFormulaTypeFromCType(t) != getFormulaTypeFromCType(type)) {
        throw new UnsupportedOperationException(
            "Variable " + name + " used with types of different sizes! " +
                "(Type1: " + t + ", Type2: " + type + ")");
      } else {
        logger.logf(Level.FINEST, "Variable %s was found with multiple types!"
                + " (Type1: %s, Type2: %s)", name, t, type);
      }
    }
  }

  /**
   * Create a formula for a given variable, which is assumed to be constant.
   * This method does not handle scoping!
   */
  protected Formula makeConstant(String name, CType type) {
    return fmgr.makeVariable(getFormulaTypeFromCType(type), name);
  }

  /**
   * Create a formula for a given variable.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   *
   * This method does not update the index of the variable.
   */
  protected Formula makeVariable(String name, CType type, SSAMapBuilder ssa) {
    return makeVariable(name, type, ssa, false);
  }

  /**
   * Create a formula for a given variable with a fresh index if needed.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   *
   * This method does not update the index of the variable.
   */
  private Formula makeVariable(String name, CType type, SSAMapBuilder ssa, boolean makeFreshIndex) {
    int idx = getIndex(name, type, ssa, makeFreshIndex, true);
    return fmgr.makeVariable(this.getFormulaTypeFromCType(type), name, idx);
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand
   * side of an assignment.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   */
  protected Formula makeFreshVariable(String name, CType type, SSAMapBuilder ssa) {
    return makeVariable(name, type, ssa, true);
  }

  Formula makeStringLiteral(String literal) {
    Formula result = stringLitToFormula.get(literal);

    if (result == null) {
      // generate a new string literal. We generate a new UIf
      int n = nextStringLitIndex++;
      result = ffmgr.createUninterpretedFunctionCall(
          stringUfDecl, ImmutableList.of(nfmgr.makeNumber(n)));
      stringLitToFormula.put(literal, result);
    }

    return result;
  }

  /**
   * Used for implicit and explicit type casts between CTypes.
   * @param fromType the origin Type of the expression.
   * @param toType the type to cast into.
   * @param formula the formula of the expression.
   * @return the new formula after the cast.
   */
  protected Formula makeCast(final CType pFromType, final CType pToType, Formula formula, CFAEdge edge) throws UnrecognizedCCodeException {
    // UNDEFINED: Casting a numeric value into a value that can't be represented by the target type (either directly or via static_cast)

    CType fromType = pFromType.getCanonicalType();
    CType toType = pToType.getCanonicalType();

    if (fromType.equals(toType)) {
      return formula; // No cast required;
    }

    if (fromType instanceof CFunctionType) {
      // references to functions can be seen as function pointers
      fromType = new CPointerType(false, false, fromType);
    }

    final boolean fromIsPointer = fromType instanceof CPointerType;
    final boolean toIsPointer = toType instanceof CPointerType;
    final boolean fromCanBeHandledAsInt =
        (fromIsPointer ||
         fromType instanceof CEnumType ||
        (fromType instanceof CElaboratedType &&
            ((CElaboratedType)fromType).getKind() == ComplexTypeKind.ENUM));
    final boolean toCanBeHandledAsInt =
        (toIsPointer ||
         toType instanceof CEnumType ||
        (toType instanceof CElaboratedType &&
            ((CElaboratedType)toType).getKind() == ComplexTypeKind.ENUM));

    if (fromCanBeHandledAsInt || toCanBeHandledAsInt) {
      // See Enums/Pointers as Integers
      if (fromCanBeHandledAsInt) {
        fromType = fromIsPointer ? machineModel.getPointerEquivalentSimpleType() : CNumericTypes.INT;
        fromType = fromType.getCanonicalType();
      }

      if (toCanBeHandledAsInt) {
        toType = toIsPointer ? machineModel.getPointerEquivalentSimpleType() : CNumericTypes.INT;
        toType = toType.getCanonicalType();
      }
    }

    if (fromType instanceof CSimpleType) {
      CSimpleType sfromType = (CSimpleType)fromType;
      if (toType instanceof CSimpleType) {
        CSimpleType stoType = (CSimpleType)toType;
        return makeSimpleCast(sfromType, stoType, formula);
      }
    }

    if (fromType instanceof CPointerType ||
        toType instanceof CPointerType) {
      // Ignore casts between Pointer and right sized types
      if (getFormulaTypeFromCType(toType) == getFormulaTypeFromCType(fromType)) {
        return formula;
      }
    }

    if (getSizeof(fromType) == getSizeof(toType)) {
      // We can most likely just ignore this cast
      logger.logfOnce(Level.WARNING, "Ignoring cast from %s to %s.", fromType, toType);
      return formula;
    } else {
      throw new UnrecognizedCCodeException("Cast from " + pFromType + " to " + pToType + " not supported!", edge);
    }
  }

  protected CExpression makeCastFromArrayToPointerIfNecessary(CExpression exp, CType targetType) {
    if (exp.getExpressionType().getCanonicalType() instanceof CArrayType) {
      targetType = targetType.getCanonicalType();
      if (targetType instanceof CPointerType || targetType instanceof CSimpleType) {
        return makeCastFromArrayToPointer(exp);
      }
    }
    return exp;
  }

  private static CExpression makeCastFromArrayToPointer(CExpression arrayExpression) {
    // array-to-pointer conversion
    CArrayType arrayType = (CArrayType)arrayExpression.getExpressionType().getCanonicalType();
    CPointerType pointerType = new CPointerType(arrayType.isConst(),
        arrayType.isVolatile(), arrayType.getType());

    return new CUnaryExpression(arrayExpression.getFileLocation(), pointerType,
        arrayExpression, UnaryOperator.AMPER);
  }

  /**
   * Handles casts between simple types.
   * When the fromType is a signed type a bit-extension will be done,
   * on any other case it will be filled with 0 bits.
   */
  private Formula makeSimpleCast(CSimpleType pFromCType, CSimpleType pToCType, Formula pFormula) {
    final FormulaType<?> fromType = typeHandler.getFormulaTypeFromCType(pFromCType);
    final FormulaType<?> toType = typeHandler.getFormulaTypeFromCType(pToCType);

    final Formula ret;
    if (fromType == toType) {
      ret = pFormula;

    } else if (fromType.isBitvectorType() && toType.isBitvectorType()) {
      int fromSize = ((FormulaType.BitvectorType)fromType).getSize();
      int toSize = ((FormulaType.BitvectorType)toType).getSize();
      if (fromSize > toSize) {
        ret = fmgr.makeExtract(pFormula, toSize-1, 0);

      } else if (fromSize < toSize) {
        ret = fmgr.makeExtend(pFormula, (toSize - fromSize), machineModel.isSigned(pFromCType));

      } else {
        ret = pFormula;
      }

    } else {
      throw new IllegalArgumentException("Cast from " + pFromCType + " to " + pToCType
          + " needs theory conversion between " + fromType + " and " + toType);
    }

    assert fmgr.getFormulaType(ret) == toType : "types do not match: " + fmgr.getFormulaType(ret) + " vs " + toType;
    return ret;
  }

//  @Override
  public PathFormula makeAnd(PathFormula oldFormula,
      CFAEdge edge, ErrorConditions errorConditions)
      throws CPATransferException, InterruptedException {
    // this is where the "meat" is... We have to parse the statement
    // attached to the edge, and convert it to the appropriate formula

    String function = (edge.getPredecessor() != null)
                          ? edge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = oldFormula.getSsa().builder();
    Constraints constraints = new Constraints(bfmgr);
    PointerTargetSetBuilder pts = createPointerTargetSetBuilder(oldFormula.getPointerTargetSet());

    // param-constraints must be added _before_ handling the edge (some lines below),
    // because this edge could write a global value.
    if (edge.getPredecessor() instanceof CFunctionEntryNode) {
      addParameterConstraints(edge, function, ssa, pts, constraints, errorConditions, (CFunctionEntryNode)edge.getPredecessor());
      addGlobalAssignmentConstraints(edge, function, ssa, pts, constraints, errorConditions, PARAM_VARIABLE_NAME, false);
    }

    // handle the edge
    BooleanFormula edgeFormula = createFormulaForEdge(edge, function, ssa, pts, constraints, errorConditions);

    // result-constraints must be added _after_ handling the edge (some lines above),
    // because this edge could write a global value.
    if (edge.getSuccessor() instanceof FunctionExitNode) {
      addGlobalAssignmentConstraints(edge, function, ssa, pts, constraints, errorConditions, RETURN_VARIABLE_NAME, true);
    }

    edgeFormula = bfmgr.and(edgeFormula, constraints.get());

    SSAMap newSsa = ssa.build();
    PointerTargetSet newPts = pts.build();

    if (bfmgr.isTrue(edgeFormula)
        && (newSsa == oldFormula.getSsa())
        && newPts.equals(oldFormula.getPointerTargetSet())) {
      // formula is just "true" and rest is equal
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return oldFormula;
    }

    BooleanFormula newFormula = bfmgr.and(oldFormula.getFormula(), edgeFormula);
    int newLength = oldFormula.getLength() + 1;
    return new PathFormula(newFormula, newSsa, newPts, newLength);
  }


  /** this function is only executed, if the option useParameterVariables is used,
   * otherwise it does nothing.
   * create and add constraints about parameters: param1=tmp_param1, param2=tmp_param2, ...
   * The tmp-variables are also used before the function-entry as "argument-constraints". */
  private void addParameterConstraints(final CFAEdge edge, final String function,
                                       final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
                                       final Constraints constraints, final ErrorConditions errorConditions,
                                       final CFunctionEntryNode entryNode)
          throws UnrecognizedCCodeException, InterruptedException {

    if (options.useParameterVariables()) {
      for (CParameterDeclaration formalParam : entryNode.getFunctionParameters()) {

        // create expressions for each formal param: "f::x" --> "f::x__param__"
        CParameterDeclaration tmpParameterExpression = new CParameterDeclaration(
                formalParam.getFileLocation(), formalParam.getType(), formalParam.getName() + PARAM_VARIABLE_NAME);
        tmpParameterExpression.setQualifiedName(formalParam.getQualifiedName() + PARAM_VARIABLE_NAME);

        CIdExpression lhs = new CIdExpression(formalParam.getFileLocation(), formalParam);
        CIdExpression rhs = new CIdExpression(formalParam.getFileLocation(), tmpParameterExpression);

        // add assignment to constraints: "f::x" = "f::x__param__"
        BooleanFormula eq = makeAssignment(lhs, rhs, edge, function, ssa, pts, constraints, errorConditions);
        constraints.addConstraint(eq);
      }
    }
  }

  /** this function is only executed, if the option useParameterVariablesForGlobals is used,
   * otherwise it does nothing.
   * create and add constraints about a global variable: tmp_1_f==global1, tmp_2_f==global2, ...
   * @param tmpAsLHS if tmpAsLHS:  tmp_result1_f := global1
   *                 else          global1       := tmp_result1_f
   */
  private void addGlobalAssignmentConstraints(final CFAEdge edge, final String function,
                                              final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
                                              final Constraints constraints, final ErrorConditions errorConditions,
                                              final String tmpNamePart, final boolean tmpAsLHS)
          throws UnrecognizedCCodeException, InterruptedException {

    if (options.useParameterVariablesForGlobals()) {

      // make assignments: tmp_param1_f==global1, tmp_param2_f==global2, ...
      // function-name is important, because otherwise the name is not unique over several function-calls.
      for (final CVariableDeclaration decl : globalDeclarations) {
        final CParameterDeclaration tmpParameter = new CParameterDeclaration(
                decl.getFileLocation(), decl.getType(), decl.getName() + tmpNamePart + function);
        tmpParameter.setQualifiedName(decl.getQualifiedName() + tmpNamePart + function);

        final CIdExpression tmp = new CIdExpression(decl.getFileLocation(), tmpParameter);
        final CIdExpression glob = new CIdExpression(decl.getFileLocation(), decl);

        final BooleanFormula eq;
        if (tmpAsLHS) {
          eq = makeAssignment(tmp, glob, glob, edge, function, ssa, pts, constraints, errorConditions);
        } else {
          eq = makeAssignment(glob, glob, tmp, edge, function, ssa, pts, constraints, errorConditions);
        }
        constraints.addConstraint(eq);
      }

    }
  }

  /**
   * This helper method creates a formula for an CFA edge, given the current function, SSA map and constraints.
   *
   * @param edge the edge for which to create the formula
   * @param function the current scope
   * @param ssa the current SSA map
   * @param constraints the current constraints
   * @return the formula for the edge
   */
  private BooleanFormula createFormulaForEdge(
      final CFAEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws CPATransferException, InterruptedException {
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      return makeStatement((CStatementEdge) edge, function,
          ssa, pts, constraints, errorConditions);
    }

    case ReturnStatementEdge: {
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)edge;
      return makeReturn(returnEdge.getExpression(), returnEdge, function,
          ssa, pts, constraints, errorConditions);
    }

    case DeclarationEdge: {
      return makeDeclaration((CDeclarationEdge)edge, function, ssa, pts, constraints, errorConditions);
    }

    case AssumeEdge: {
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      return makePredicate(assumeEdge.getExpression(), assumeEdge.getTruthAssumption(),
          assumeEdge, function, ssa, pts, constraints, errorConditions);
    }

    case BlankEdge: {
      return bfmgr.makeBoolean(true);
    }

    case FunctionCallEdge: {
      return makeFunctionCall((CFunctionCallEdge)edge, function,
          ssa, pts, constraints, errorConditions);
    }

    case FunctionReturnEdge: {
      // get the expression from the summary edge
      CFunctionSummaryEdge ce = ((CFunctionReturnEdge)edge).getSummaryEdge();
      return makeExitFunction(ce, function,
          ssa, pts, constraints, errorConditions);
    }

    case MultiEdge: {
      List<BooleanFormula> multiEdgeFormulas = new ArrayList<>(((MultiEdge)edge).getEdges().size());

      // unroll the MultiEdge
      for (CFAEdge singleEdge : (MultiEdge)edge) {
        if (singleEdge instanceof BlankEdge) {
          continue;
        }
        multiEdgeFormulas.add(createFormulaForEdge(singleEdge, function, ssa, pts, constraints, errorConditions));
        shutdownNotifier.shutdownIfNecessary();
      }

      // Big conjunction at the end is better than creating a new conjunction
      // after each edge for some SMT solvers.
      return bfmgr.and(multiEdgeFormulas);
    }

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  protected BooleanFormula makeStatement(
      final CStatementEdge statement, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws CPATransferException, InterruptedException {

    CStatement stmt = statement.getStatement();
    if (stmt instanceof CAssignment) {
      CAssignment assignment = (CAssignment)stmt;
      return makeAssignment(assignment.getLeftHandSide(), assignment.getRightHandSide(),
          statement, function, ssa, pts, constraints, errorConditions);

    } else {
      if (stmt instanceof CFunctionCallStatement) {
        CRightHandSideVisitor<Formula, UnrecognizedCCodeException> ev = createCRightHandSideVisitor(
            statement, function, ssa, pts, constraints, errorConditions);
        CFunctionCallStatement callStmt = (CFunctionCallStatement)stmt;
        callStmt.getFunctionCallExpression().accept(ev);

      } else if (!(stmt instanceof CExpressionStatement)) {
        throw new UnrecognizedCCodeException("Unknown statement", statement, stmt);
      }

      // side-effect free statement, ignore
      return bfmgr.makeBoolean(true);
    }
  }

  protected BooleanFormula makeDeclaration(
      final CDeclarationEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws CPATransferException, InterruptedException {

    if (!(edge.getDeclaration() instanceof CVariableDeclaration)) {
      // struct prototype, function declaration, typedef etc.
      logfOnce(Level.FINEST, edge, "Ignoring declaration");
      return bfmgr.makeBoolean(true);
    }

    CVariableDeclaration decl = (CVariableDeclaration)edge.getDeclaration();
    final String varName = decl.getQualifiedName();

    if (!isRelevantVariable(decl)) {
      logger.logfOnce(Level.FINEST, "%s: Ignoring declaration of unused variable: %s",
          decl.getFileLocation(), decl.toASTString());
      return bfmgr.makeBoolean(true);
    }

    if (options.useParameterVariablesForGlobals() && decl.isGlobal()) {
      globalDeclarations.add(decl);
    }

    // if the var is unsigned, add the constraint that it should
    // be > 0
    //    if (((CSimpleType)spec).isUnsigned()) {
    //    long z = mathsat.api.msat_make_number(msatEnv, "0");
    //    long mvar = buildMsatVariable(var, idx);
    //    long t = mathsat.api.msat_make_gt(msatEnv, mvar, z);
    //    t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
    //    m1 = new MathsatFormula(t);
    //    }

    // just increment index of variable in SSAMap
    // (a declaration contains an implicit assignment, even without initializer)
    // In case of an existing initializer, we increment the index twice
    // (here and below) so that the index 2 only occurs for uninitialized variables.
    // DO NOT OMIT THIS CALL, even without an initializer!
    makeFreshIndex(varName, decl.getType(), ssa);

    // if there is an initializer associated to this variable,
    // take it into account
    BooleanFormula result = bfmgr.makeBoolean(true);

    if (decl.getInitializer() instanceof CInitializerList) {
      // If there is an initializer, all fields/elements not mentioned
      // in the initializer are set to 0 (C standard § 6.7.9 (21)

      int size = machineModel.getSizeof(decl.getType());
      if (size > 0) {
        Formula var = makeVariable(varName, decl.getType(), ssa);
        Formula zero = fmgr.makeNumber(getFormulaTypeFromCType(decl.getType()), 0L);
        result = bfmgr.and(result, fmgr.assignment(var, zero));
      }
    }

    for (CAssignment assignment : CInitializers.convertToAssignments(decl, edge)) {
      result = bfmgr.and(result, makeAssignment(assignment.getLeftHandSide(), assignment.getRightHandSide(), edge, function, ssa, pts, constraints, errorConditions));
    }

    return result;
  }

  protected BooleanFormula makeExitFunction(
      final CFunctionSummaryEdge ce, final String calledFunction,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws CPATransferException, InterruptedException {

    addGlobalAssignmentConstraints(ce, calledFunction, ssa, pts, constraints, errorConditions, RETURN_VARIABLE_NAME, false);

    CFunctionCall retExp = ce.getExpression();
    if (retExp instanceof CFunctionCallStatement) {
      // this should be a void return, just do nothing...
      return bfmgr.makeBoolean(true);

    } else if (retExp instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement exp = (CFunctionCallAssignmentStatement)retExp;
      CFunctionCallExpression funcCallExp = exp.getRightHandSide();

      String callerFunction = ce.getSuccessor().getFunctionName();

      final CIdExpression rhs = createReturnVariable(funcCallExp.getFileLocation(), funcCallExp.getDeclaration());

      return makeAssignment(exp.getLeftHandSide(), rhs, ce, callerFunction, ssa, pts, constraints, errorConditions);
    } else {
      throw new UnrecognizedCCodeException("Unknown function exit expression", ce, retExp);
    }
  }

  protected CType getReturnType(CFunctionCallExpression funcCallExp, CFAEdge edge) throws UnrecognizedCCodeException {
    // NOTE: When funCallExp.getExpressionType() does always return the return type of the function we don't
    // need this function. However I'm not sure because there can be implicit casts. Just to be safe.
    CType retType;
    CFunctionDeclaration funcDecl = funcCallExp.getDeclaration();
    if (funcDecl == null) {
      // Check if we have a function pointer here.
      CExpression functionNameExpression = funcCallExp.getFunctionNameExpression();
      CType expressionType = functionNameExpression.getExpressionType().getCanonicalType();
      if (expressionType instanceof CFunctionType) {
        CFunctionType funcPtrType = (CFunctionType)expressionType;
        retType = funcPtrType.getReturnType();
      } else if (expressionType instanceof CPointerType &&
                 ((CPointerType) expressionType).getType().getCanonicalType() instanceof CFunctionType) {
        CFunctionType funcPtrType = (CFunctionType) ((CPointerType) expressionType).getType().getCanonicalType();
        retType = funcPtrType.getReturnType();
      } else {
        throw new UnrecognizedCCodeException("Cannot handle function pointer call with unknown type " + expressionType, edge, funcCallExp);
      }
      assert retType != null;
    } else {
      retType = funcDecl.getType().getReturnType();
    }

    CType expType = funcCallExp.getExpressionType();
    if (!expType.getCanonicalType().equals(retType.getCanonicalType())) {
      // Bit ignore for now because we sometimes just get ElaboratedType instead of CompositeType
      logfOnce(Level.WARNING, edge,
          "Return type of function %s is %s, but result is used as type %s",
          funcDecl.getName(), retType, expType);
    }
    return expType;
  }


  protected BooleanFormula makeFunctionCall(
      final CFunctionCallEdge edge, final String callerFunction,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws CPATransferException, InterruptedException {

    List<CExpression> actualParams = edge.getArguments();

    CFunctionEntryNode fn = edge.getSuccessor();
    List<CParameterDeclaration> formalParams = fn.getFunctionParameters();

    if (fn.getFunctionDefinition().getType().takesVarArgs()) {
      if (formalParams.size() > actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }

      if (!SAFE_VAR_ARG_FUNCTIONS.contains(fn.getFunctionName())) {
        logfOnce(Level.WARNING, edge,
            "Ignoring parameters passed as varargs to function %s",
            fn.getFunctionName());
      }

    } else {
      if (formalParams.size() != actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }
    }

    int i = 0;
    BooleanFormula result = bfmgr.makeBoolean(true);
    for (CParameterDeclaration formalParam : formalParams) {
      CExpression paramExpression = actualParams.get(i++);
      CIdExpression lhs = new CIdExpression(paramExpression.getFileLocation(), formalParam);
      final CIdExpression paramLHS;
      if (options.useParameterVariables()) {
        // make assignments: tmp_param1==arg1, tmp_param2==arg2, ...
        CParameterDeclaration tmpParameter = new CParameterDeclaration(
                formalParam.getFileLocation(), formalParam.getType(), formalParam.getName() + PARAM_VARIABLE_NAME);
        tmpParameter.setQualifiedName(formalParam.getQualifiedName() + PARAM_VARIABLE_NAME);
        paramLHS = new CIdExpression(paramExpression.getFileLocation(), tmpParameter);
      } else {
        paramLHS = lhs;
      }

      BooleanFormula eq = makeAssignment(paramLHS, lhs, paramExpression, edge, callerFunction, ssa, pts, constraints, errorConditions);
      result = bfmgr.and(result, eq);
    }

    addGlobalAssignmentConstraints(edge, fn.getFunctionName(), ssa, pts, constraints, errorConditions, PARAM_VARIABLE_NAME, true);

    return result;
  }

  protected BooleanFormula makeReturn(final Optional<CExpression> rightExp,
      final CReturnStatementEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws CPATransferException, InterruptedException {
    if (!rightExp.isPresent()) {
      // this is a return from a void function, do nothing
      return bfmgr.makeBoolean(true);
    } else {

      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      final CFunctionDeclaration functionDeclaration =
          ((CFunctionEntryNode) edge.getSuccessor().getEntryNode()).getFunctionDefinition();
      final CIdExpression lhs = createReturnVariable(rightExp.get().getFileLocation(), functionDeclaration);

      return makeAssignment(lhs, rightExp.get(), edge, function, ssa, pts, constraints, errorConditions);
    }
  }

  private static CIdExpression createReturnVariable(final FileLocation fileLocation,
      final CFunctionDeclaration functionDeclaration) {
    final CVariableDeclaration returnVariableDeclaration = createReturnVariableDeclaration(functionDeclaration);
    final CIdExpression lhs = new CIdExpression(fileLocation,
                       returnVariableDeclaration);
    return lhs;
  }

  protected static final CVariableDeclaration createReturnVariableDeclaration(
      final CFunctionDeclaration functionDeclaration) {
    final String retVarName = RETURN_VARIABLE_NAME;
    final CType returnType = functionDeclaration.getType().getReturnType();

    return new CVariableDeclaration(functionDeclaration.getFileLocation(), false,
        CStorageClass.AUTO, returnType,
        retVarName, retVarName, scoped(retVarName, functionDeclaration.getName()), null);
  }

  /**
   * Creates formula for the given assignment.
   * @param lhs the left-hand-side of the assignment
   * @param rhs the right-hand-side of the assignment
   * @return the assignment formula
   * @throws UnrecognizedCCodeException
   * @throws InterruptedException
   */
  private BooleanFormula makeAssignment(
      final CLeftHandSide lhs, CRightHandSide rhs,
      final CFAEdge edge, final String function,
      final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
      final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {
    // lhs is used twice, also as lhsForChecking!
    return makeAssignment(lhs, lhs, rhs, edge, function, ssa, pts, constraints, errorConditions);
  }

  /**
   * Creates formula for the given assignment.
   * @param lhs the left-hand-side of the assignment
   * @param lhsForChecking a left-hand-side of the assignment (for most cases: lhs == lhsForChecking),
   *                       that is used to check, if the assignment is important.
   *                       If the assignment is not important, we return TRUE.
   * @param rhs the right-hand-side of the assignment
   * @return the assignment formula
   * @throws UnrecognizedCCodeException
   * @throws InterruptedException
   */
  protected BooleanFormula makeAssignment(
          final CLeftHandSide lhs, final CLeftHandSide lhsForChecking, CRightHandSide rhs,
          final CFAEdge edge, final String function,
          final SSAMapBuilder ssa, final PointerTargetSetBuilder pts,
          final Constraints constraints, final ErrorConditions errorConditions)
          throws UnrecognizedCCodeException, InterruptedException {

    if (!isRelevantLeftHandSide(lhsForChecking)) {
      // Optimization for unused variables and fields
      return bfmgr.makeBoolean(true);
    }

    CType lhsType = lhs.getExpressionType().getCanonicalType();

    if (lhsType instanceof CArrayType) {
      // Probably a (string) initializer, ignore assignments to arrays
      // as they cannot behandled precisely anyway.
      return bfmgr.makeBoolean(true);
    }

    if (rhs instanceof CExpression) {
      rhs = makeCastFromArrayToPointerIfNecessary((CExpression)rhs, lhsType);
    }

    Formula r = buildTerm(rhs, edge, function, ssa, pts, constraints, errorConditions);
    Formula l = buildLvalueTerm(lhs, edge, function, ssa, pts, constraints, errorConditions);
    r = makeCast(
          rhs.getExpressionType(),
          lhsType,
          r,
          edge);

    return fmgr.assignment(l, r);
  }

  Formula buildTerm(CRightHandSide exp, CFAEdge edge, String function,
      SSAMapBuilder ssa, PointerTargetSetBuilder pts,
      Constraints constraints, ErrorConditions errorConditions)
          throws UnrecognizedCCodeException {
    return exp.accept(createCRightHandSideVisitor(edge, function, ssa, pts, constraints, errorConditions));
  }

  Formula buildLvalueTerm(CLeftHandSide exp,
      CFAEdge edge, String function,
      SSAMapBuilder ssa, PointerTargetSetBuilder pts,
      Constraints constraints, ErrorConditions errorConditions) throws UnrecognizedCCodeException {
    return exp.accept(new LvalueVisitor(this, edge, function, ssa, pts, constraints, errorConditions));
  }

  <T extends Formula> T ifTrueThenOneElseZero(FormulaType<T> type, BooleanFormula pCond) {
    T one = fmgr.makeNumber(type, 1);
    T zero = fmgr.makeNumber(type, 0);
    return bfmgr.ifThenElse(pCond, one, zero);
  }

  protected final <T extends Formula> BooleanFormula toBooleanFormula(T pF) {
    // If this is not a predicate, make it a predicate by adding a "!= 0"
    assert !fmgr.getFormulaType(pF).isBooleanType();

    T zero = fmgr.makeNumber(fmgr.getFormulaType(pF), 0);

    if (bfmgr.isIfThenElse(pF)) {
      Triple<BooleanFormula, T, T> parts = bfmgr.splitIfThenElse(pF);

      T one = fmgr.makeNumber(fmgr.getFormulaType(pF), 1);

      if (parts.getSecond().equals(one) && parts.getThird().equals(zero)) {
        return parts.getFirst();
      } else if (parts.getSecond().equals(zero) && parts.getThird().equals(one)) {
        return bfmgr.not(parts.getFirst());
      }
    }

    return bfmgr.not(fmgr.makeEqual(pF, zero));
  }

  protected BooleanFormula makePredicate(CExpression exp, boolean isTrue, CFAEdge edge,
      String function, SSAMapBuilder ssa, PointerTargetSetBuilder pts, Constraints constraints, ErrorConditions errorConditions) throws UnrecognizedCCodeException, InterruptedException {

    Formula f = exp.accept(createCRightHandSideVisitor(edge, function, ssa, pts, constraints, errorConditions));
    BooleanFormula result = toBooleanFormula(f);

    if (!isTrue) {
      result = bfmgr.not(result);
    }
    return result;
  }

  public BooleanFormula makePredicate(CExpression exp, CFAEdge edge, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException, InterruptedException {
    PointerTargetSetBuilder pts = DummyPointerTargetSetBuilder.INSTANCE;
    Constraints constraints = new Constraints(bfmgr);
    ErrorConditions errorConditions = ErrorConditions.dummyInstance(bfmgr);
    BooleanFormula f = makePredicate(exp, true, edge, function, ssa, pts, constraints, errorConditions);
    return bfmgr.and(f, constraints.get());
  }

  protected PointerTargetSetBuilder createPointerTargetSetBuilder(PointerTargetSet pts) {
    return DummyPointerTargetSetBuilder.INSTANCE;
  }

  protected CRightHandSideVisitor<Formula, UnrecognizedCCodeException> createCRightHandSideVisitor(
      CFAEdge pEdge, String pFunction,
      SSAMapBuilder ssa, PointerTargetSetBuilder pts,
      Constraints constraints, ErrorConditions errorConditions) {
    return new ExpressionToFormulaVisitor(this, pEdge, pFunction, ssa, constraints);
  }

  /**
   * Creates a Formula which accesses the given bits.
   */
  private BitvectorFormula accessField(Pair<Integer, Integer> msb_Lsb, BitvectorFormula f) {
    return fmgr.makeExtract(f, msb_Lsb.getFirst(), msb_Lsb.getSecond());
  }

  /**
   * Creates a Formula which accesses the given Field
   */
  BitvectorFormula accessField(CFieldReference fExp, Formula f) throws UnrecognizedCCodeException {
    assert options.handleFieldAccess() : "Fieldaccess if only allowed with handleFieldAccess";
    assert f instanceof BitvectorFormula : "Fields need to be represented with bitvectors";
    // Get the underlaying structure
    Pair<Integer, Integer> msb_Lsb = getFieldOffsetMsbLsb(fExp);
    return accessField(msb_Lsb, (BitvectorFormula)f);
  }

  /**
   * Return the bitvector for a struct with the bits for one field replaced
   * by another bitvector, or left out completely.
   * @param fExp The field of the struct to replace.
   * @param pLVar The full struct.
   * @param pRightVariable The replacement bitvector, or nothing.
   * @return If pRightVariable is present, a formula of the same size as pLVar, but with some bits replaced.
   * If pRightVariable is not present, a formula that is smaller then pLVar (with the field bits missing).
   */
  Formula replaceField(CFieldReference fExp, Formula pLVar, Optional<Formula> pRightVariable) throws UnrecognizedCCodeException {
    assert options.handleFieldAccess() : "Fieldaccess if only allowed with handleFieldAccess";

    Pair<Integer, Integer> msb_Lsb = getFieldOffsetMsbLsb(fExp);

    int size = efmgr.getLength((BitvectorFormula) pLVar);
    assert size > msb_Lsb.getFirst() : "pLVar is too small";
    assert 0 <= msb_Lsb.getSecond() && msb_Lsb.getFirst() >= msb_Lsb.getSecond() : "msb_Lsb is invalid";

    // create a list with three formulas:
    // - prefix of struct (before the field)
    // - the replaced field
    // - suffix of struct (after the field)
    List<Formula> parts = new ArrayList<>(3);

    if (msb_Lsb.getFirst() + 1 < size) {
      parts.add(fmgr.makeExtract(pLVar, size - 1, msb_Lsb.getFirst() + 1));
    }

    if (pRightVariable.isPresent()) {
      assert efmgr.getLength((BitvectorFormula) pRightVariable.get()) == msb_Lsb.getFirst() + 1 - msb_Lsb.getSecond() : "The new formula has not the right size";
      parts.add(pRightVariable.get());
    }

    if (msb_Lsb.getSecond() > 0) {
      parts.add(fmgr.makeExtract(pLVar, msb_Lsb.getSecond() - 1, 0));
    }

    if (parts.isEmpty()) {
      // struct with no other fields, return empty bitvector
      return efmgr.makeBitvector(0, 0);
    }
    return fmgr.makeConcat(parts);
  }

  /**
   * Returns the offset of the given CFieldReference within the structure in bits.
   */
  private Pair<Integer, Integer> getFieldOffsetMsbLsb(CFieldReference fExp) throws UnrecognizedCCodeException {
    CExpression fieldRef = getRealFieldOwner(fExp);
    CCompositeType structType = (CCompositeType)fieldRef.getExpressionType().getCanonicalType();

    // f is now the structure, access it:
    int bitsPerByte = machineModel.getSizeofCharInBits();

    int offset;
    switch (structType.getKind()) {
    case UNION:
      offset = 0;
      break;
    case STRUCT:
      offset = getFieldOffset(structType, fExp.getFieldName()) * bitsPerByte;
      break;
    default:
      throw new UnrecognizedCCodeException("Unexpected field access", fExp);
    }

    int fieldSize = getSizeof(fExp.getExpressionType()) * bitsPerByte;

    // Crude hack for unions with zero-sized array fields produced by LDV
    // (ldv-consumption/32_7a_cilled_true_linux-3.8-rc1-32_7a-fs--ceph--ceph.ko-ldv_main7_sequence_infinite_withcheck_stateful.cil.out.c)
    if (fieldSize == 0 && structType.getKind() == ComplexTypeKind.UNION) {
      fieldSize = getSizeof(fieldRef.getExpressionType());
    }

    int lsb = offset;
    int msb = offset + fieldSize - 1;
    assert(lsb >= 0);
    assert(msb >= lsb);
    Pair<Integer, Integer> msb_Lsb = Pair.of(msb, lsb);
    return msb_Lsb;
  }

  /**
   * Returns the offset of the given field in the given struct in bytes.
   *
   * This function does not handle UNIONs or ENUMs!
   */
  private int getFieldOffset(CCompositeType structType, String fieldName) {
    int off = 0;
    for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
      if (member.getName().equals(fieldName)) {
        return off;
      }

      off += getSizeof(member.getType());
    }

    throw new AssertionError("field " + fieldName + " was not found in " + structType);
  }

  /**
   * We call this method for unsupported Expressions and just make a new Variable.
   */
  Formula makeVariableUnsafe(CExpression exp, String function, SSAMapBuilder ssa, boolean makeFresh) {
    if (makeFresh) {
      logger.logOnce(Level.WARNING, "Program contains array, or pointer (multiple level of indirection), or field (enable handleFieldAccess and handleFieldAliasing) access; analysis is imprecise in case of aliasing.");
    }
    logger.logfOnce(Level.FINEST, "%s: Unhandled expression treated as free variable: %s",
        exp.getFileLocation(), exp.toASTString());

    String var = scoped(exprToVarName(exp), function);
    return makeVariable(var, exp.getExpressionType(), ssa, makeFresh);
  }
}
