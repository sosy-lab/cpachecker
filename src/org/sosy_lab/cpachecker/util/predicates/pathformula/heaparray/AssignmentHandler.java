/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.implicitCastToPointer;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Variable;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.QuantifiedFormulaManagerView;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.IntegerFormulaManager;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implements a handler for assignments.
 */
class AssignmentHandler {

  private final FormulaEncodingWithPointerAliasingOptions options;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView bfmgr;
  private final ArrayFormulaManagerView afmgr;

  private final CToFormulaConverterWithHeapArray converter;
  private final CFAEdge edge;
  private final String function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;

  /**
   * Creates a new AssignmentHandler.
   *
   * @param pConverter               The C to SMT formula converter.
   * @param pCFAEdge                 The current edge of the CFA (for logging purposes).
   * @param pFunction                The name of the current function.
   * @param pSSAMapBuilder           The SSA map.
   * @param pPointerTargetSetBuilder The underlying set of pointer targets.
   * @param pConstraints             Additional constraints.
   * @param pErrorConditions         Additional error conditions.
   */
  AssignmentHandler(
      final CToFormulaConverterWithHeapArray pConverter,
      final CFAEdge pCFAEdge,
      final String pFunction,
      final SSAMapBuilder pSSAMapBuilder,
      final PointerTargetSetBuilder pPointerTargetSetBuilder,
      final Constraints pConstraints,
      final ErrorConditions pErrorConditions) {
    converter = pConverter;

    options = this.converter.options;
    formulaManager = this.converter.formulaManager;
    bfmgr = this.converter.bfmgr;
    afmgr = converter.afmgr;

    edge = pCFAEdge;
    function = pFunction;
    ssa = pSSAMapBuilder;
    pts = pPointerTargetSetBuilder;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
  }

  /**
   * Creates a formula to handle assignments.
   *
   * @param pLhs            The left hand side of an assignment.
   * @param pLhsForChecking The left hand side of an assignment to check.
   * @param pRhs            Either {@code null} or the right hand side of the assignment.
   * @param pBatchMode      A flag indicating batch mode.
   * @param pDestroyedTypes Either {@code null} or a set of destroyed types.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  BooleanFormula handleAssignment(
      final CLeftHandSide pLhs,
      final CLeftHandSide pLhsForChecking,
      final @Nullable CRightHandSide pRhs,
      final boolean pBatchMode,
      final @Nullable Set<CType> pDestroyedTypes)
      throws UnrecognizedCCodeException, InterruptedException {

    if (!converter.isRelevantLeftHandSide(pLhsForChecking)) {
      // Optimization for unused variables and fields
      return converter.bfmgr.makeBoolean(true);
    }

    final CType lhsType = CTypeUtils.simplifyType(pLhs.getExpressionType());
    final CType rhsType =
        pRhs != null ? CTypeUtils.simplifyType(pRhs.getExpressionType())
                     : CNumericTypes.SIGNED_CHAR;

    // RHS handling
    final CExpressionVisitorWithHeapArray rhsVisitor =
        new CExpressionVisitorWithHeapArray(converter, edge, function, ssa, constraints,
            errorConditions, pts);

    final Expression rhsExpression;
    if (pRhs == null) {
      rhsExpression = Value.nondetValue();
    } else {
      CRightHandSide r = pRhs;
      if (r instanceof CExpression) {
        r = converter.convertLiteralToFloatIfNecessary((CExpression) r, lhsType);
      }
      rhsExpression = r.accept(rhsVisitor);
    }

    pts.addEssentialFields(rhsVisitor.getInitializedFields());
    pts.addEssentialFields(rhsVisitor.getUsedFields());
    final List<Pair<CCompositeType, String>> rhsAddressedFields = rhsVisitor.getAddressedFields();
    final Map<String, CType> rhsUsedDeferredAllocationPointers =
        rhsVisitor.getUsedDeferredAllocationPointers();

    // LHS handling
    final CExpressionVisitorWithHeapArray lhsVisitor =
        new CExpressionVisitorWithHeapArray(converter, edge, function, ssa, constraints,
            errorConditions, pts);
    final Location lhsLocation = pLhs.accept(lhsVisitor).asLocation();
    final Map<String, CType> lhsUsedDeferredAllocationPointers =
        lhsVisitor.getUsedDeferredAllocationPointers();
    pts.addEssentialFields(lhsVisitor.getInitializedFields());
    pts.addEssentialFields(lhsVisitor.getUsedFields());

    if (converter.options.revealAllocationTypeFromLHS()
        || converter.options.deferUntypedAllocations()) {
      DynamicMemoryHandler memoryHandler =
          new DynamicMemoryHandler(converter, edge, ssa, pts, constraints, errorConditions);
      memoryHandler.handleDeferredAllocationsInAssignment(pLhs, pRhs, lhsLocation, rhsExpression,
          lhsType, lhsUsedDeferredAllocationPointers, rhsUsedDeferredAllocationPointers);
    }

    final BooleanFormula result =
        makeAssignment(lhsType, rhsType, lhsLocation, rhsExpression, pBatchMode, pDestroyedTypes);

    for (final Pair<CCompositeType, String> field : rhsAddressedFields) {
      pts.addField(field.getFirst(), field.getSecond());
    }

    return result;
  }

  /**
   * Handles initialization assignments.
   *
   * @param pVariable    The left hand side of the variable.
   * @param pAssignments A list of assignment statements.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       It the execution was interrupted.
   */
  BooleanFormula handleInitializationAssignments(
      final CLeftHandSide pVariable,
      final List<CExpressionAssignmentStatement> pAssignments)
      throws UnrecognizedCCodeException, InterruptedException {

    CExpressionVisitorWithHeapArray lhsVisitor =
        new CExpressionVisitorWithHeapArray(converter, edge, function,
            ssa, constraints, errorConditions, pts);
    final Location lhsLocation = pVariable.accept(lhsVisitor).asLocation();
    final Set<CType> updatedTypes = new HashSet<>();
    BooleanFormula result = converter.bfmgr.makeBoolean(true);
    for (CExpressionAssignmentStatement assignment : pAssignments) {
      final CLeftHandSide lhs = assignment.getLeftHandSide();
      result = converter.bfmgr.and(result,
          handleAssignment(lhs, lhs, assignment.getRightHandSide(),
              lhsLocation.isAliased(), updatedTypes));
    }

    if (lhsLocation.isAliased()) {
      updateSSA(updatedTypes, ssa);
    }

    return result;
  }

  /**
   * Handles an initialization assignments, i.e. an assignment with a C initializer, with using a
   * quantifier over the resulting SMT array.
   *
   * <p>If we cannot make an assignment of the form {@code <variable> = <value>}, we fall back to
   * the normal initialization in {@link #handleInitializationAssignments(CLeftHandSide,
   * List)}.</p>
   *
   * @param pLeftHandSide     The left hand side of the statement. Needed for fallback scenario.
   * @param pAssignments      A list of assignment statements.
   * @param pQfmgr            A formula manager with quantifier support.
   * @param pUseOldSSAIndices A flag indicating whether we will reuse SSA indices or not.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   * @see #handleInitializationAssignments(CLeftHandSide, List)
   */
  BooleanFormula handleInitializationAssignmentsWithQuantifier(
      final @Nonnull CLeftHandSide pLeftHandSide,
      final @Nonnull List<CExpressionAssignmentStatement> pAssignments,
      final @Nonnull QuantifiedFormulaManagerView pQfmgr,
      final boolean pUseOldSSAIndices)
      throws UnrecognizedCCodeException, InterruptedException {

    assert pAssignments.size() > 0 : "Cannot handle initialization assignments without an "
        + "assignment right hand side.";
    assert formulaManager.getIntegerFormulaManager() != null : "Need a formula manager for "
        + "integers to handle initialization assignments with quantifiers";

    final IntegerFormulaManagerView ifmgr = formulaManager.getIntegerFormulaManager();

    final CType lhsType = CTypeUtils.simplifyType(
        pAssignments.get(0).getLeftHandSide().getExpressionType());

    final CExpressionVisitorWithHeapArray rhsVisitor =
        new CExpressionVisitorWithHeapArray(converter, edge, function, ssa, constraints,
            errorConditions, pts);
    final Value rhsValue = pAssignments.get(0).getRightHandSide().accept(rhsVisitor).asValue();

    final CExpressionVisitorWithHeapArray lhsVisitor =
        new CExpressionVisitorWithHeapArray(
            converter, edge, function, ssa, constraints, errorConditions, pts);
    final Location lhsLocation = pLeftHandSide.accept(lhsVisitor).asLocation();

    if (rhsValue == null
        || !checkEqualityOfInitializers(pAssignments, rhsVisitor)
        || !lhsLocation.isAliased()) {
      // Fallback case, if we have no initialization of the form "<variable> = <value>"
      // Example code snippet
      // (cf. test/programs/simple/struct-initializer-for-composite-field_false-unreach-label.c)
      //    struct s { int x; };
      //    struct t { struct s s; };
      //    ...
      //    const struct s s = { .x = 1 };
      //    struct t t = { .s = s };
      return handleInitializationAssignments(pLeftHandSide, pAssignments);
    } else {
      final String targetName = CToFormulaConverterWithHeapArray.getArrayName(lhsType);
      final FormulaType<?> targetType = converter.getFormulaTypeFromCType(lhsType);
      final int oldIndex = converter.getIndex(targetName, lhsType, ssa);
      final int newIndex =
          pUseOldSSAIndices
              ? converter.getIndex(targetName, lhsType, ssa)
              : converter.getFreshIndex(targetName, lhsType, ssa);

      final IntegerFormula lowerBound =
          (IntegerFormula) ifmgr.unwrap(lhsLocation.asAliased().getAddress());
      final IntegerFormula upperBound =
          ifmgr.add(lowerBound, ifmgr.makeNumber(pAssignments.size()));

      final IntegerFormula counter = ifmgr.makeVariable(targetName + "@" + oldIndex + "@counter");
      final ArrayFormula<?, ?> newArray =
          afmgr.makeArray(targetName + "@" + newIndex, FormulaType.IntegerType, targetType);
      final ArrayFormula<?, ?> oldArray =
          afmgr.makeArray(targetName + "@" + oldIndex, FormulaType.IntegerType, targetType);

      final List<BooleanFormula> rangeConstraint =
          makeRangeConstraint(ifmgr, counter, lowerBound, upperBound);

      final BooleanFormula selectFormula =
          formulaManager.makeEqual(afmgr.select(newArray, counter), rhsValue.getValue());
      final BooleanFormula copyFormula =
          formulaManager.makeEqual(
              newArray, afmgr.store(oldArray, counter, afmgr.select(oldArray, counter)));

      return pQfmgr.forall(
          Collections.singletonList(counter),
          bfmgr.and(
              bfmgr.implication(bfmgr.and(rangeConstraint), selectFormula),
              bfmgr.implication(bfmgr.not(bfmgr.and(rangeConstraint)), copyFormula)));
    }
  }

  /**
   * Creates a list of {@code BooleanFormula}s that are range constraints for universal quantifiers.
   *
   * @param pIntegerFormulaManager A formula manager for integers.
   * @param pVariable The index variable of the quantifier.
   * @param pLowerBound The lower bound of the constraints.
   * @param pUpperBound The upper bound of the constraints.
   * @param <R> The type of the index variable.
   * @return A list of constraint formulae.
   */
  private <R extends IntegerFormula> List<BooleanFormula> makeRangeConstraint(
      final IntegerFormulaManager pIntegerFormulaManager,
      final R pVariable,
      final R pLowerBound,
      final R pUpperBound) {
    return ImmutableList.of(
        pIntegerFormulaManager.greaterOrEquals(pVariable, pLowerBound),
        pIntegerFormulaManager.lessOrEquals(pVariable, pUpperBound));
  }

  /**
   * Checks, whether all assignments of an initializer have the same value.
   *
   * @param pAssignments The list of assignments.
   * @param pRhsVisitor A visitor to evaluate the value of the right-hand side.
   * @return Whether all assignments of an initializer have the same value.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private boolean checkEqualityOfInitializers(
      final @Nonnull List<CExpressionAssignmentStatement> pAssignments,
      final @Nonnull CExpressionVisitorWithHeapArray pRhsVisitor)
      throws UnrecognizedCCodeException {
    Value tmp = null;
    for (CExpressionAssignmentStatement assignment : pAssignments) {
      if (tmp == null) {
        tmp = assignment.getRightHandSide().accept(pRhsVisitor).asValue();
      }
      if (!tmp.equals(assignment.getRightHandSide().accept(pRhsVisitor).asValue())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Creates a formula for an assignment.
   *
   * @param pLvalueType       The type of the lvalue.
   * @param pRvalueType       The type of the rvalue.
   * @param pLvalue           The location of the lvalue.
   * @param pRvalue           The rvalue expression.
   * @param pUseOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param pUpdatedTypes     Either {@code null} or a set of updated types.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException       If the execution was interrupted.
   */
  BooleanFormula makeAssignment(
      @Nonnull CType pLvalueType,
      final @Nonnull CType pRvalueType,
      final @Nonnull Location pLvalue,
      final @Nonnull Expression pRvalue,
      final boolean pUseOldSSAIndices,
      @Nullable Set<CType> pUpdatedTypes)
      throws UnrecognizedCCodeException, InterruptedException {

    checkNotNull(pRvalue);

    pLvalueType = CTypeUtils.simplifyType(pLvalueType);

    if (pLvalue.isAliased()
        && !isSimpleType(pLvalueType)
        && pUpdatedTypes == null) {
      pUpdatedTypes = new HashSet<>();
    } else {
      pUpdatedTypes = null;
    }
    Set<Variable> updatedVariables = null;
    if (!pLvalue.isAliased() && !isSimpleType(pLvalueType)) {
      updatedVariables = new HashSet<>();
    }

    final BooleanFormula result = makeDestructiveAssignment(pLvalueType, pRvalueType, pLvalue,
        pRvalue, pUseOldSSAIndices, pUpdatedTypes, updatedVariables);

    if (!pUseOldSSAIndices) {
      if (pLvalue.isAliased()) {
        if (pUpdatedTypes == null) {
          assert isSimpleType(pLvalueType) : "Should be impossible due to the first if statement";
          pUpdatedTypes = Collections.singleton(pLvalueType);
        }

        updateSSA(pUpdatedTypes, ssa);

      } else { // Unaliased lvalue
        if (updatedVariables == null) {
          assert isSimpleType(pLvalueType) : "Should be impossible due to the first if statement";
          updatedVariables = Collections.singleton(Variable.create(
              pLvalue.asUnaliased().getVariableName(), pLvalueType));
        }

        for (final Variable variable : updatedVariables) {
          final String name = variable.getName();
          final CType type = variable.getType();
          converter.makeFreshIndex(name, type, ssa); // increment index in SSAMap
        }
      }
    }
    return result;
  }

  /**
   * Creates a formula for a destructive assignment.
   *
   * @param pLvalueType       The type of the lvalue.
   * @param pRvalueType       The type of the rvalue.
   * @param pLvalue           The location of the lvalue.
   * @param pRvalue           The rvalue expression.
   * @param pUseOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param pUpdatedTypes     Either {@code null} or a set of updated types.
   * @param pUpdatedVariables Either {@code null} or a set of updated variables.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private BooleanFormula makeDestructiveAssignment(
      @Nonnull CType pLvalueType,
      @Nonnull CType pRvalueType,
      final @Nonnull Location pLvalue,
      final @Nonnull Expression pRvalue,
      final boolean pUseOldSSAIndices,
      final @Nullable Set<CType> pUpdatedTypes,
      final @Nullable Set<Variable> pUpdatedVariables)
      throws UnrecognizedCCodeException {

    pLvalueType = CTypeUtils.simplifyType(pLvalueType);
    pRvalueType = CTypeUtils.simplifyType(pRvalueType);
    BooleanFormula result;

    if (pLvalueType instanceof CArrayType) {
      Preconditions.checkArgument(pLvalue.isAliased(), "Array elements are always aliased " +
          "(i.e. can't be encoded with variables)");
      final CArrayType lvalueArrayType = (CArrayType) pLvalueType;
      final CType lvalueElementType = CTypeUtils.simplifyType(lvalueArrayType.getType());

      // There are only two cases of assignment to an array, either initializing
      // the array with a value (possibly nondet), which is useful for stack
      // declarations and memset implementation, or an array assignment (needed
      // for structure assignment implementation). This is only possible from
      // another array of the same type.
      Preconditions.checkArgument(pRvalue.isValue() && isSimpleType(pRvalueType)
              || pRvalue.asLocation().isAliased()
              && pRvalueType instanceof CArrayType
              && CTypeUtils.simplifyType(((CArrayType) pRvalueType).getType())
              .equals(lvalueElementType), "Impossible array assignment due to incompatible " +
          "types: assignment of %s to %s", pRvalueType,
          pLvalueType);

      Integer length = CTypeUtils.getArrayLength(lvalueArrayType);
      // Try to fix the length if it's unknown (or too big)
      // Also ignore the tail part of very long arrays to avoid very large
      // formulae (imprecise!)
      if (length == null) {
        final Integer rLength;
        if (pRvalue.isLocation()
            && (rLength = CTypeUtils.getArrayLength((CArrayType) pRvalueType)) != null) {
          length = rLength;
        } else {
          length = options.defaultArrayLength();
        }
      }

      result = bfmgr.makeBoolean(true);
      int offset = 0;
      for (int i = 0; i < length; ++i) {
        final Pair<AliasedLocation, CType> newLvalue =
            shiftArrayLvalue(pLvalue.asAliased(), offset, lvalueElementType);
        final Pair<? extends Expression, CType> newRvalue =
            shiftArrayRvalue(pRvalue, pRvalueType, offset, lvalueElementType);

        result = bfmgr.and(result,
            makeDestructiveAssignment(newLvalue.getSecond(),
                newRvalue.getSecond(), newLvalue.getFirst(),
                newRvalue.getFirst(), pUseOldSSAIndices, pUpdatedTypes,
                pUpdatedVariables));
        offset += converter.getSizeof(lvalueArrayType.getType());
      }

      return result;

    } else if (pLvalueType instanceof CCompositeType) {
      final CCompositeType lvalueCompositeType = (CCompositeType) pLvalueType;
      assert lvalueCompositeType.getKind() != ComplexTypeKind.ENUM : "Enums are"
          + " not composite: " + lvalueCompositeType;
      // There are two cases of assignment to a structure/union, either an
      // initialization with a value (possibly nondet) which is useful for stack
      // declarations and memset implementation, or a structure assignment.
      if (!(pRvalue.isValue() && isSimpleType(pRvalueType)
          || pRvalueType.equals(pLvalueType))) {
        throw new UnrecognizedCCodeException("Impossible structure assignment "
            + "due to incompatible types: assignment of " + pRvalue
            + " with type " + pRvalueType + " to " + pLvalue + " with type "
            + pLvalueType, edge);
      }

      result = bfmgr.makeBoolean(true);
      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration
          : lvalueCompositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final CType newLvalueType = CTypeUtils.simplifyType(memberDeclaration.getType());

        // Optimizing away the assignments from uninitialized fields
        if (converter.isRelevantField(lvalueCompositeType, memberName) &&
            (!pLvalue.isAliased() || // Assignment to a variable, no profit in optimizing it
                !isSimpleType(
                    newLvalueType) || // That's not a simple assignment, check the nested composite
                pRvalue.isValue() || // This is initialization, so the assignment is mandatory
                pts.tracksField(lvalueCompositeType,
                    memberName) || // The field is tracked as essential
                // The variable representing the RHS was used some where (i.e. has SSA index)
                !pRvalue.asLocation().isAliased() &&
                    converter.hasIndex(pRvalue.asLocation().asUnaliased().getVariableName()
                        + CToFormulaConverterWithHeapArray.FIELD_NAME_SEPARATOR
                        + memberName, newLvalueType, ssa))) {
          final Pair<? extends Location, CType> newLvalue =
              shiftCompositeLvalue(pLvalue, offset, memberName, memberDeclaration.getType());
          final Pair<? extends Expression, CType> newRvalue =
              shiftCompositeRvalue(pRvalue, offset, memberName, pRvalueType,
                  memberDeclaration.getType());

          result = bfmgr.and(result,
              makeDestructiveAssignment(newLvalue.getSecond(),
                  newRvalue.getSecond(), newLvalue.getFirst(),
                  newRvalue.getFirst(), pUseOldSSAIndices, pUpdatedTypes,
                  pUpdatedVariables));
        }

        if (lvalueCompositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += converter.getSizeof(memberDeclaration.getType());
        }
      }

      return result;

    } else { // Simple assignment
      return makeSimpleDestructiveAssignment(pLvalueType, pRvalueType, pLvalue,
          pRvalue, pUseOldSSAIndices, pUpdatedTypes, pUpdatedVariables);
    }
  }

  /**
   * Creates a formula for a simple destructive assignment.
   *
   * @param pLvalueType       The type of the lvalue.
   * @param pRvalueType       The type of the rvalue.
   * @param pLvalue           The location of the lvalue.
   * @param pRvalue           The rvalue expression.
   * @param pUseOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param pUpdatedTypes     Either {@code null} or a set of updated types.
   * @param pUpdatedVariables Either {@code null} or a set of updated variables.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private BooleanFormula makeSimpleDestructiveAssignment(
      @Nonnull CType pLvalueType,
      @Nonnull CType pRvalueType,
      final @Nonnull Location pLvalue,
      @Nonnull Expression pRvalue,
      final boolean pUseOldSSAIndices,
      final @Nullable Set<CType> pUpdatedTypes,
      final @Nullable Set<Variable> pUpdatedVariables)
      throws UnrecognizedCCodeException {

    pLvalueType = CTypeUtils.simplifyType(pLvalueType);
    pRvalueType = CTypeUtils.simplifyType(pRvalueType);
    // Arrays and functions are implicitly converted to pointers
    pRvalueType = implicitCastToPointer(pRvalueType);

    Preconditions.checkArgument(isSimpleType(pLvalueType), "To assign to/from "
        + "arrays/structures/unions use makeDestructiveAssignment");
    Preconditions.checkArgument(isSimpleType(pRvalueType), "To assign to/from "
        + "arrays/structures/unions use makeDestructiveAssignment");

    final Formula value;
    switch (pRvalue.getKind()) {
      case ALIASED_LOCATION:
        value = converter.makeDereference(pRvalueType,
            pRvalue.asAliasedLocation().getAddress(), ssa, errorConditions);
        break;
      case UNALIASED_LOCATION:
        value = converter.makeVariable(
            pRvalue.asUnaliasedLocation().getVariableName(), pRvalueType, ssa);
        break;
      case DET_VALUE:
        value = pRvalue.asValue().getValue();
        break;
      case NONDET:
        value = null;
        break;
      default:
        throw new AssertionError();
    }

    assert !(pLvalueType instanceof CFunctionType) : "Can't assign to functions";

    final String targetName = !pLvalue.isAliased()
                              ? pLvalue.asUnaliased().getVariableName()
                              : CToFormulaConverterWithHeapArray.getArrayName(pLvalueType);
    final FormulaType<?> targetType = converter.getFormulaTypeFromCType(pLvalueType);
    final int oldIndex = converter.getIndex(targetName, pLvalueType, ssa);
    final int newIndex = pUseOldSSAIndices
                         ? converter.getIndex(targetName, pLvalueType, ssa)
                         : converter.getFreshIndex(targetName, pLvalueType, ssa);
    final BooleanFormula result;

    pRvalueType = implicitCastToPointer(pRvalueType);
    final Formula rhs = value != null
                        ? converter.makeCast(pRvalueType, pLvalueType, value, constraints, edge)
                        : null;

    if (!pLvalue.isAliased()) { // Unaliased LHS
      if (rhs != null) {
        result = formulaManager.assignment(
            formulaManager.makeVariable(targetType, targetName, newIndex), rhs);
      } else {
        result = bfmgr.makeBoolean(true);
      }

      if (pUpdatedVariables != null) {
        pUpdatedVariables.add(Variable.create(targetName, pLvalueType));
      }
    } else { // Aliased LHS
      if (rhs != null) {
        final ArrayFormula<?, ?> oldFormula = afmgr.makeArray(targetName + "@" + oldIndex,
            FormulaType.IntegerType, targetType);
        final ArrayFormula<?, ?> arrayFormula = afmgr.makeArray(targetName + "@" + newIndex,
            FormulaType.IntegerType, targetType);
        result = formulaManager.makeEqual(arrayFormula, afmgr.store(oldFormula,
            pLvalue.asAliased().getAddress(), rhs));
      } else {
        result = bfmgr.makeBoolean(true);
      }

      if (pUpdatedTypes != null) {
        pUpdatedTypes.add(pLvalueType);
      }
    }

    return result;
  }

  /**
   * Updates the SSA map.
   *
   * @param pTypes         A set of types that should be added to the SSA map.
   * @param pSSAMapBuilder The current SSA map.
   */
  private void updateSSA(
      final @Nonnull Set<CType> pTypes,
      final SSAMapBuilder pSSAMapBuilder) {
    for (final CType type : pTypes) {
      final String ufName = CToFormulaConverterWithHeapArray.getArrayName(type);
      converter.makeFreshIndex(ufName, type, pSSAMapBuilder);
    }
  }

  /**
   * Shifts the array's lvalue.
   *
   * @param pLvalue            The lvalue location.
   * @param pOffset            The offset of the shift.
   * @param pLvalueElementType The type of the lvalue element.
   * @return A tuple of location and type after the shift.
   */
  private Pair<AliasedLocation, CType> shiftArrayLvalue(
      final AliasedLocation pLvalue,
      final int pOffset,
      final CType pLvalueElementType) {
    final Formula offsetFormula = formulaManager.makeNumber(
        converter.voidPointerFormulaType, pOffset);
    final AliasedLocation newLvalue = Location.ofAddress(
        formulaManager.makePlus(pLvalue.getAddress(), offsetFormula));
    return Pair.of(newLvalue, pLvalueElementType);
  }

  /**
   * Shifts the array's rvalue.
   *
   * @param pRvalue            The rvalue expression.
   * @param pRvalueType        The type of the rvalue.
   * @param pOffset            The offset of the shift.
   * @param pLvalueElementType The type of the lvalue element.
   * @return A tuple of expression and type after the shift.
   */
  private Pair<? extends Expression, CType> shiftArrayRvalue(
      final Expression pRvalue,
      final CType pRvalueType,
      final int pOffset,
      final CType pLvalueElementType) {
    // Support both initialization (with a value or nondet) and assignment
    // (from another array location)
    switch (pRvalue.getKind()) {
      case ALIASED_LOCATION: {
        assert pRvalueType instanceof CArrayType : "Non-array rvalue in array assignment";
        final Formula offsetFormula = formulaManager.makeNumber(
            converter.voidPointerFormulaType, pOffset);
        final AliasedLocation newRvalue = Location.ofAddress(
            formulaManager.makePlus(pRvalue.asAliasedLocation().getAddress(), offsetFormula));
        final CType newRvalueType = CTypeUtils.simplifyType(((CArrayType) pRvalueType).getType());
        return Pair.of(newRvalue, newRvalueType);
      }
      case DET_VALUE: {
        return Pair.of(pRvalue, pRvalueType);
      }
      case NONDET: {
        final CType newLvalueType = isSimpleType(pLvalueElementType)
                                    ? pLvalueElementType : CNumericTypes.SIGNED_CHAR;
        return Pair.of(Value.nondetValue(), newLvalueType);
      }
      case UNALIASED_LOCATION: {
        throw new AssertionError("Array locations should always be aliased");
      }
      default:
        throw new AssertionError();
    }
  }

  /**
   * Shifts the composite lvalue.
   *
   * @param pLvalue     The lvalue location.
   * @param pOffset     The offset of the shift.
   * @param pMemberName The name of the member.
   * @param pMemberType The type of the member.
   * @return A tuple of location and type after the shift.
   */
  private Pair<? extends Location, CType> shiftCompositeLvalue(
      final Location pLvalue,
      final int pOffset,
      final String pMemberName,
      final CType pMemberType) {
    final CType newLvalueType = CTypeUtils.simplifyType(pMemberType);
    if (pLvalue.isAliased()) {
      final Formula offsetFormula = formulaManager.makeNumber(
          converter.voidPointerFormulaType, pOffset);
      final AliasedLocation newLvalue = Location.ofAddress(
          formulaManager.makePlus(pLvalue.asAliased().getAddress(), offsetFormula));
      return Pair.of(newLvalue, newLvalueType);

    } else {
      final UnaliasedLocation newLvalue = Location.ofVariableName(
          pLvalue.asUnaliased().getVariableName()
              + CToFormulaConverterWithHeapArray.FIELD_NAME_SEPARATOR
              + pMemberName);
      return Pair.of(newLvalue, newLvalueType);
    }

  }

  /**
   * Shifts the composite rvalue.
   *
   * @param pRvalue     The rvalue expression.
   * @param pOffset     The offset of the shift.
   * @param pMemberName The name of the member.
   * @param pRvalueType The type of the rvalue.
   * @param pMemberType The type of the member.
   * @return A tuple of expression and type after the shift.
   */
  private Pair<? extends Expression, CType> shiftCompositeRvalue(
      final Expression pRvalue,
      final int pOffset,
      final String pMemberName,
      final CType pRvalueType,
      final CType pMemberType) {
    // Support both structure assignment and initialization with a value
    // (or nondet)
    final CType newLvalueType = CTypeUtils.simplifyType(pMemberType);
    switch (pRvalue.getKind()) {
      case ALIASED_LOCATION: {
        final Formula offsetFormula = formulaManager.makeNumber(
            converter.voidPointerFormulaType, pOffset);
        final AliasedLocation newRvalue = Location.ofAddress(
            formulaManager.makePlus(pRvalue.asAliasedLocation().getAddress(), offsetFormula));
        return Pair.of(newRvalue, newLvalueType);
      }
      case UNALIASED_LOCATION: {
        final UnaliasedLocation newRvalue = Location.ofVariableName(
            pRvalue.asUnaliasedLocation().getVariableName()
                + CToFormulaConverterWithHeapArray.FIELD_NAME_SEPARATOR
                + pMemberName);
        return Pair.of(newRvalue, newLvalueType);
      }
      case DET_VALUE: {
        return Pair.of(pRvalue, pRvalueType);
      }
      case NONDET: {
        final CType newRvalueType = isSimpleType(newLvalueType)
                                    ? newLvalueType : CNumericTypes.SIGNED_CHAR;
        return Pair.of(Value.nondetValue(), newRvalueType);
      }
      default:
        throw new AssertionError();
    }
  }
}
