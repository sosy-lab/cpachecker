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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.implicitCastToPointer;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.IsRelevantLhsVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * Implements a handler for assignments.
 */
class AssignmentHandler {

  private final FormulaEncodingWithPointerAliasingOptions options;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;
  private final String function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final MemoryRegionManager regionMgr;

  /**
   * Creates a new AssignmentHandler.
   *
   * @param pConv The C to SMT formula converter.
   * @param pEdge The current edge of the CFA (for logging purposes).
   * @param pFunction The name of the current function.
   * @param pSsa The SSA map.
   * @param pPts The underlying set of pointer targets.
   * @param pConstraints Additional constraints.
   * @param pErrorConditions Additional error conditions.
   */
  AssignmentHandler(CToFormulaConverterWithPointerAliasing pConv, CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts, Constraints pConstraints, ErrorConditions pErrorConditions,
      MemoryRegionManager pRegionMgr) {
    conv = pConv;

    typeHandler = pConv.typeHandler;
    options = conv.options;
    fmgr = conv.fmgr;
    bfmgr = conv.bfmgr;

    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
    regionMgr = pRegionMgr;
  }

  /**
   * Creates a formula to handle assignments.
   *
   * @param lhs The left hand side of an assignment.
   * @param lhsForChecking The left hand side of an assignment to check.
   * @param rhs Either {@code null} or the right hand side of the assignment.
   * @param batchMode A flag indicating batch mode.
   * @param destroyedRegions Either {@code null} or a set of destroyed memory regions.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  BooleanFormula handleAssignment(
      final CLeftHandSide lhs,
      final CLeftHandSide lhsForChecking,
      final CType lhsType,
      final @Nullable CRightHandSide rhs,
      final boolean batchMode,
      final @Nullable Set<MemoryRegion> destroyedRegions)
      throws UnrecognizedCCodeException, InterruptedException {
    if (!conv.isRelevantLeftHandSide(lhsForChecking)) {
      // Optimization for unused variables and fields
      return conv.bfmgr.makeTrue();
    }

    /*if (rhs instanceof CExpression && !((CExpression)rhs).accept(new IsRelevantLhsVisitor(conv))) {
      //remove global variable assignments
      return conv.bfmgr.makeBoolean(true);
    }*/

    final CType rhsType =
        rhs != null ? typeHandler.getSimplifiedType(rhs) : CNumericTypes.SIGNED_CHAR;

    // RHS handling
    final CExpressionVisitorWithPointerAliasing rhsVisitor = newExpressionVisitor();

    final Expression rhsExpression;
    if (rhs == null || (rhs instanceof CExpression && !((CExpression)rhs).accept(new IsRelevantLhsVisitor(conv)))) {
      rhsExpression = Value.nondetValue();
    } else {
      rhsExpression = createRHSExpression(rhs, lhsType, rhsVisitor);
    }

    pts.addEssentialFields(rhsVisitor.getInitializedFields());
    pts.addEssentialFields(rhsVisitor.getUsedFields());
    final List<Pair<CCompositeType, String>> rhsAddressedFields = rhsVisitor.getAddressedFields();
    final Map<String, CType> rhsLearnedPointersTypes = rhsVisitor.getLearnedPointerTypes();

    // LHS handling
    final CExpressionVisitorWithPointerAliasing lhsVisitor = newExpressionVisitor();
    final Location lhsLocation = lhs.accept(lhsVisitor).asLocation();
    final Map<String, CType> lhsLearnedPointerTypes = lhsVisitor.getLearnedPointerTypes();
    pts.addEssentialFields(lhsVisitor.getInitializedFields());
    pts.addEssentialFields(lhsVisitor.getUsedFields());
    // the pattern matching possibly aliased locations
    final PointerTargetPattern pattern = lhsLocation.isUnaliasedLocation()
        ? null
        : PointerTargetPattern.forLeftHandSide(lhs, typeHandler, edge, pts);

    if (conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) {
      DynamicMemoryHandler memoryHandler = new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);
      memoryHandler.handleDeferredAllocationsInAssignment(
          lhs,
          rhs,
          rhsExpression,
          lhsType,
          lhsVisitor,
          lhsLearnedPointerTypes,
          rhsLearnedPointersTypes);
    }

    final BooleanFormula result =
        makeAssignment(lhsType,
                          rhsType,
                          lhsLocation,
                          rhsExpression,
                          pattern,
                          batchMode,
                          destroyedRegions);

    for (final Pair<CCompositeType, String> field : rhsAddressedFields) {
      pts.addField(field.getFirst(), field.getSecond());
    }
    return result;
  }

  private Expression createRHSExpression(
      CRightHandSide pRhs, CType pLhsType, CExpressionVisitorWithPointerAliasing pRhsVisitor)
      throws UnrecognizedCCodeException {
    if (pRhs == null) {
      return Value.nondetValue();
    }
    CRightHandSide r = pRhs;
    if (r instanceof CExpression) {
      r = conv.convertLiteralToFloatIfNecessary((CExpression) r, pLhsType);
    }
    return r.accept(pRhsVisitor);
  }

  private CExpressionVisitorWithPointerAliasing newExpressionVisitor() {
    return new CExpressionVisitorWithPointerAliasing(
        conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
  }

  BooleanFormula handleAssignment(
      final CLeftHandSide lhs,
      final CLeftHandSide lhsForChecking,
      final @Nullable CRightHandSide rhs,
      final boolean batchMode,
      final @Nullable Set<MemoryRegion> destroyedRegions)
      throws UnrecognizedCCodeException, InterruptedException {
    return handleAssignment(
        lhs, lhsForChecking, typeHandler.getSimplifiedType(lhs), rhs, batchMode, destroyedRegions);
  }

  /**
   * Handles initialization assignments.
   *
   * @param variable The declared variable.
   * @param declarationType The type of the declared variable.
   * @param assignments A list of assignment statements.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException It the execution was interrupted.
   */
  BooleanFormula handleInitializationAssignments(
      final CLeftHandSide variable, final CType declarationType, final List<CExpressionAssignmentStatement> assignments) throws UnrecognizedCCodeException, InterruptedException {
    if (options.useQuantifiersOnArrays()
        && (declarationType instanceof CArrayType)
        && !assignments.isEmpty()) {
      return handleInitializationAssignmentsWithQuantifier(variable, assignments, false);
    } else {
      return handleInitializationAssignmentsWithoutQuantifier(variable, assignments);
    }
  }

  /**
   * Handles initialization assignments.
   *
   * @param variable The left hand side of the variable.
   * @param assignments A list of assignment statements.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException It the execution was interrupted.
   */
  private BooleanFormula handleInitializationAssignmentsWithoutQuantifier(
      final CLeftHandSide variable, final List<CExpressionAssignmentStatement> assignments)
      throws UnrecognizedCCodeException, InterruptedException {
    CExpressionVisitorWithPointerAliasing lhsVisitor = newExpressionVisitor();
    final Location lhsLocation = variable.accept(lhsVisitor).asLocation();
    final Set<MemoryRegion> updatedRegions = new HashSet<>();
    BooleanFormula result = conv.bfmgr.makeTrue();
    for (CExpressionAssignmentStatement assignment : assignments) {
      final CLeftHandSide lhs = assignment.getLeftHandSide();
      result = conv.bfmgr.and(result, handleAssignment(lhs, lhs,
                                                       assignment.getRightHandSide(),
                                                       lhsLocation.isAliased(), // Defer index update for UFs, but not for variables
                                                       updatedRegions));
    }
    if (lhsLocation.isAliased()) {
      finishAssignments(
          typeHandler.getSimplifiedType(variable),
          lhsLocation.asAliased(),
          PointerTargetPattern.forLeftHandSide(variable, typeHandler, edge, pts),
          updatedRegions);
    }
    return result;
  }

  /**
   * Handles an initialization assignments, i.e. an assignment with a C initializer, with using a
   * quantifier over the resulting SMT array.
   *
   * <p>If we cannot make an assignment of the form {@code <variable> = <value>}, we fall back to
   * the normal initialization in
   * {@link #handleInitializationAssignmentsWithoutQuantifier(CLeftHandSide, List)}.
   *
   * @param pLeftHandSide The left hand side of the statement. Needed for fallback scenario.
   * @param pAssignments A list of assignment statements.
   * @param pUseOldSSAIndices A flag indicating whether we will reuse SSA indices or not.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   * @see #handleInitializationAssignmentsWithoutQuantifier(CLeftHandSide, List)
   */
  private BooleanFormula handleInitializationAssignmentsWithQuantifier(
      final CLeftHandSide pLeftHandSide,
      final List<CExpressionAssignmentStatement> pAssignments,
      final boolean pUseOldSSAIndices)
      throws UnrecognizedCCodeException, InterruptedException {

    assert pAssignments.size() > 0 : "Cannot handle initialization assignments without an "
        + "assignment right hand side.";

    final CType lhsType = typeHandler.getSimplifiedType(pAssignments.get(0).getLeftHandSide());
    final CType rhsType = typeHandler.getSimplifiedType(pAssignments.get(0).getRightHandSide());

    final CExpressionVisitorWithPointerAliasing rhsVisitor = newExpressionVisitor();
    final Expression rhsValue = pAssignments.get(0).getRightHandSide().accept(rhsVisitor);

    final CExpressionVisitorWithPointerAliasing lhsVisitor = newExpressionVisitor();
    final Location lhsLocation = pLeftHandSide.accept(lhsVisitor).asLocation();

    if (!rhsValue.isValue()
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
      return handleInitializationAssignmentsWithoutQuantifier(pLeftHandSide, pAssignments);
    } else {
      MemoryRegion region = lhsLocation.asAliased().getMemoryRegion();
      if(region == null) {
        region = regionMgr.makeMemoryRegion(lhsType);
      }
      final String targetName = regionMgr.getPointerAccessName(region);
      final FormulaType<?> targetType = conv.getFormulaTypeFromCType(lhsType);
      final int oldIndex = conv.getIndex(targetName, lhsType, ssa);
      final int newIndex =
          pUseOldSSAIndices
              ? conv.getIndex(targetName, lhsType, ssa)
              : conv.getFreshIndex(targetName, lhsType, ssa);

      final Formula counter =
          fmgr.makeVariable(conv.voidPointerFormulaType, targetName + "@" + oldIndex + "@counter");
      final BooleanFormula rangeConstraint =
          fmgr.makeElementIndexConstraint(
              counter, lhsLocation.asAliased().getAddress(), pAssignments.size(), false);

      final Formula newDereference =
          conv.ptsMgr.makePointerDereference(targetName, targetType, newIndex, counter);
      final Formula rhs =
          conv.makeCast(rhsType, lhsType, rhsValue.asValue().getValue(), constraints, edge);

      final BooleanFormula assignNewValue = fmgr.assignment(newDereference, rhs);

      final BooleanFormula copyOldValue;
      if (options.useArraysForHeap()) {
        final ArrayFormulaManagerView afmgr = fmgr.getArrayFormulaManager();
        final ArrayFormula<?, ?> newArray =
            afmgr.makeArray(targetName, newIndex, conv.voidPointerFormulaType, targetType);
        final ArrayFormula<?, ?> oldArray =
            afmgr.makeArray(targetName, oldIndex, conv.voidPointerFormulaType, targetType);
        copyOldValue = fmgr.makeEqual(newArray, oldArray);

      } else {
        copyOldValue =
            fmgr.assignment(
                newDereference,
                conv.ptsMgr.makePointerDereference(targetName, targetType, oldIndex, counter));
      }

      return fmgr.getQuantifiedFormulaManager()
          .forall(
              counter,
              bfmgr.and(
                  bfmgr.implication(rangeConstraint, assignNewValue),
                  bfmgr.implication(bfmgr.not(rangeConstraint), copyOldValue)));
    }
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
      final List<CExpressionAssignmentStatement> pAssignments,
      final CExpressionVisitorWithPointerAliasing pRhsVisitor)
      throws UnrecognizedCCodeException {
    Expression tmp = null;
    for (CExpressionAssignmentStatement assignment : pAssignments) {
      if (tmp == null) {
        tmp = assignment.getRightHandSide().accept(pRhsVisitor);
      }
      if (!tmp.equals(assignment.getRightHandSide().accept(pRhsVisitor))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Creates a formula for an assignment.
   *
   * @param lvalueType The type of the lvalue.
   * @param rvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  BooleanFormula makeAssignment(
      CType lvalueType,
      final CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final @Nullable PointerTargetPattern pattern,
      final boolean useOldSSAIndices,
      @Nullable Set<MemoryRegion> updatedRegions)
      throws UnrecognizedCCodeException, InterruptedException {
    // Its a definite value assignment, a nondet assignment (SSA index update) or a nondet assignment among other
    // assignments to the same UF version (in this case an absense of aliasing should be somehow guaranteed, as in the
    // case of initialization assignments)
    //assert rvalue != null || !useOldSSAIndices || updatedRegions != null; // otherwise the call is useless
    checkNotNull(rvalue);

    checkIsSimplified(lvalueType);

    if (lvalue.isAliased() && !isSimpleType(lvalueType) && updatedRegions == null) {
      updatedRegions = new HashSet<>();
    } else {
      updatedRegions = null;
    }
    Set<Variable> updatedVariables = null;
    if (!lvalue.isAliased() && !isSimpleType(lvalueType)) {
      updatedVariables = new HashSet<>();
    }

    final BooleanFormula result = makeDestructiveAssignment(lvalueType, rvalueType,
                                                            lvalue, rvalue,
                                                            useOldSSAIndices,
                                                            updatedRegions,
                                                            updatedVariables);

    if (!useOldSSAIndices) {
      if (lvalue.isAliased()) {
        if (updatedRegions == null) {
          assert isSimpleType(lvalueType) : "Should be impossible due to the first if statement";
          MemoryRegion region = lvalue.asAliased().getMemoryRegion();
          if(region == null) {
            region = regionMgr.makeMemoryRegion(lvalueType);
          }
          updatedRegions = Collections.singleton(region);
        }
        finishAssignments(lvalueType, lvalue.asAliased(), pattern, updatedRegions);
      } else { // Unaliased lvalue
        if (updatedVariables == null) {
          assert isSimpleType(lvalueType) : "Should be impossible due to the first if statement";
          updatedVariables = Collections.singleton(Variable.create(lvalue.asUnaliased().getVariableName(), lvalueType));
        }
        for (final Variable variable : updatedVariables) {
          final String name = variable.getName();
          final CType type = variable.getType();
          conv.makeFreshIndex(name, type, ssa); // increment index in SSAMap
        }
      }
    }
    return result;
  }

  private void finishAssignments(
      CType lvalueType,
      final AliasedLocation lvalue,
      final PointerTargetPattern pattern,
      final Set<MemoryRegion> updatedRegions)
      throws InterruptedException {
    MemoryRegion region = lvalue.getMemoryRegion();
    if(region == null) {
      region = regionMgr.makeMemoryRegion(lvalueType);
    }
    //Michael suggestion to switch the order.
    //Important in case when we do not sure what update, add retention to the lvalueType
    addRetentionForAssignment(region,
                              lvalueType,
                              lvalue.getAddress(),
                              pattern, updatedRegions);
    updateSSA(updatedRegions, ssa);
  }

  /**
   * Creates a formula for a destructive assignment.
   *
   * @param lvalueType The type of the lvalue.
   * @param rvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @param updatedVariables Either {@code null} or a set of updated variables.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private BooleanFormula makeDestructiveAssignment(
      CType lvalueType,
      CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions,
      final @Nullable Set<Variable> updatedVariables)
      throws UnrecognizedCCodeException {
    checkIsSimplified(lvalueType);
    checkIsSimplified(rvalueType);
    BooleanFormula result;

    if (lvalueType instanceof CArrayType) {
      Preconditions.checkArgument(lvalue.isAliased(),
                                  "Array elements are always aliased (i.e. can't be encoded with variables)");
      final CArrayType lvalueArrayType = (CArrayType) lvalueType;
      final CType lvalueElementType = checkIsSimplified(lvalueArrayType.getType());

      if (rvalue.isNondetValue()) {
        //This is fix for races, global assignements are replaced by nondet
        return bfmgr.makeBoolean(true);
      }
      //tmpFix
      // There are only two cases of assignment to an array
      Preconditions.checkArgument(
          // Initializing array with a value (possibly nondet), useful for stack declarations and memset implementation
          (rvalue.isValue() && isSimpleType(rvalueType))
              ||
              // Array assignment (needed for structure assignment implementation)
              // Only possible from another array of the same type
              (rvalue.asLocation().isAliased()
                  && rvalueType instanceof CArrayType
                  && checkIsSimplified(((CArrayType) rvalueType).getType())
                      .equals(lvalueElementType)),
          "Impossible array assignment due to incompatible types: assignment of %s to %s",
          rvalueType,
          lvalueType);

      OptionalInt lvalueLength = lvalueArrayType.getLengthAsInt();
      // Try to fix the length if it's unknown (or too big)
      // Also ignore the tail part of very long arrays to avoid very large formulae (imprecise!)
      if (!lvalueLength.isPresent() && rvalue.isLocation()) {
        lvalueLength = ((CArrayType) rvalueType).getLengthAsInt();
      }
      int length =
          lvalueLength.isPresent()
              ? Integer.min(options.maxArrayLength(), lvalueLength.getAsInt())
              : options.defaultArrayLength();

      result = bfmgr.makeTrue();
      int offset = 0;
      for (int i = 0; i < length; ++i) {
        final Pair<AliasedLocation, CType> newLvalue = shiftArrayLvalue(lvalue.asAliased(), offset, lvalueElementType);
        final Pair<? extends Expression, CType> newRvalue =
                                                       shiftArrayRvalue(rvalue, rvalueType, offset, lvalueElementType);

        result = bfmgr.and(result,
                           makeDestructiveAssignment(newLvalue.getSecond(),
                                                     newRvalue.getSecond(),
                                                     newLvalue.getFirst(),
                                                     newRvalue.getFirst(),
                                                     useOldSSAIndices,
                                                     updatedRegions,
                                                     updatedVariables));
         offset += conv.getBitSizeof(lvalueArrayType.getType());
      }
      return result;
    } else if (lvalueType instanceof CCompositeType) {
      final CCompositeType lvalueCompositeType = (CCompositeType) lvalueType;
      assert lvalueCompositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + lvalueCompositeType;
      // There are two cases of assignment to a structure/union
      if (!(
          // Initialization with a value (possibly nondet), useful for stack declarations and memset implementation
          (rvalue.isValue() && isSimpleType(rvalueType))
              ||
              // Structure assignment
              rvalueType.equals(lvalueType))) {
        throw new UnrecognizedCCodeException("Impossible structure assignment due to incompatible types:"
            + " assignment of " + rvalue + " with type "+ rvalueType + " to " + lvalue + " with type "+ lvalueType, edge);
      }
      result = bfmgr.makeTrue();
      for (final CCompositeTypeMemberDeclaration memberDeclaration : lvalueCompositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final int offset = typeHandler.getBitOffset(lvalueCompositeType, memberName);
        final CType newLvalueType = typeHandler.getSimplifiedType(memberDeclaration);
        // Optimizing away the assignments from uninitialized fields
        if (conv.isRelevantField(lvalueCompositeType, memberName)
            && (
                // Assignment to a variable, no profit in optimizing it
                !lvalue.isAliased()
                || // That's not a simple assignment, check the nested composite
                !isSimpleType(newLvalueType)
                || // This is initialization, so the assignment is mandatory
                rvalue.isValue()
                || // The field is tracked as essential
                pts.tracksField(lvalueCompositeType, memberName)
                || // The variable representing the RHS was used somewhere (i.e. has SSA index)
                (!rvalue.isAliasedLocation()
                    && conv.hasIndex(
                        rvalue.asUnaliasedLocation().getVariableName()
                            + CToFormulaConverterWithPointerAliasing.FIELD_NAME_SEPARATOR
                            + memberName,
                        newLvalueType,
                        ssa)))) {
          final Pair<? extends Location, CType> newLvalue =
                                         shiftCompositeLvalue(lvalueType, lvalue, offset, memberName, memberDeclaration.getType());
          final Pair<? extends Expression, CType> newRvalue =
                             shiftCompositeRvalue(rvalue, offset, memberName, rvalueType, memberDeclaration.getType());

          result = bfmgr.and(result,
                             makeDestructiveAssignment(newLvalue.getSecond(),
                                                       newRvalue.getSecond(),
                                                       newLvalue.getFirst(),
                                                       newRvalue.getFirst(),
                                                       useOldSSAIndices,
                                                       updatedRegions,
                                                       updatedVariables));
        }
      }
      return result;
    } else { // Simple assignment
      return makeSimpleDestructiveAssignment(lvalueType,
                                             rvalueType,
                                             lvalue,
                                             rvalue,
                                             useOldSSAIndices,
                                             updatedRegions,
                                             updatedVariables);
    }
  }

  /**
   * Creates a formula for a simple destructive assignment.
   *
   * @param lvalueType The type of the lvalue.
   * @param rvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @param updatedVariables Either {@code null} or a set of updated variables.
   * @return A formula for the assignment.
   * @throws UnrecognizedCCodeException If the C code was unrecognizable.
   */
  private BooleanFormula makeSimpleDestructiveAssignment(
      CType lvalueType,
      CType rvalueType,
      final Location lvalue,
      Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions,
      final @Nullable Set<Variable> updatedVariables)
      throws UnrecognizedCCodeException {
    checkIsSimplified(lvalueType);
    checkIsSimplified(rvalueType);
    // Arrays and functions are implicitly converted to pointers
    rvalueType = implicitCastToPointer(rvalueType);

    Preconditions.checkArgument(isSimpleType(lvalueType),
                                "To assign to/from arrays/structures/unions use makeDestructiveAssignment");
    Preconditions.checkArgument(isSimpleType(rvalueType),
                                "To assign to/from arrays/structures/unions use makeDestructiveAssignment");

    assert !(lvalueType instanceof CFunctionType) : "Can't assign to functions";

    String targetName;
    MemoryRegion region;
    if(!lvalue.isAliased()) {
      targetName = lvalue.asUnaliased().getVariableName();
      region = null;
    } else {
      region = lvalue.asAliased().getMemoryRegion();
      if(region == null) {
        region = regionMgr.makeMemoryRegion(lvalueType);
      }
      targetName = regionMgr.getPointerAccessName(region);
    }
    final FormulaType<?> targetType = conv.getFormulaTypeFromCType(lvalueType);
    final int oldIndex = conv.getIndex(targetName, lvalueType, ssa);
    final int newIndex = useOldSSAIndices ?
            conv.getIndex(targetName, lvalueType, ssa) :
            conv.getFreshIndex(targetName, lvalueType, ssa);
    final BooleanFormula result;

    final Optional<Formula> value = getValueFormula(rvalueType, rvalue);
    final Formula rhs =
        value.isPresent()
            ? conv.makeCast(rvalueType, lvalueType, value.get(), constraints, edge)
            : null;
    if (!lvalue.isAliased()) { // Unaliased LHS
      if (rhs != null) {
        result = fmgr.assignment(fmgr.makeVariable(targetType, targetName, newIndex), rhs);
      } else {
        result = bfmgr.makeTrue();
      }

      if (updatedVariables != null) {
        updatedVariables.add(Variable.create(targetName, lvalueType));
      }
    } else { // Aliased LHS
      if (rhs != null) {
        final Formula address = lvalue.asAliased().getAddress();
        result =
            conv.ptsMgr.makePointerAssignment(
                targetName, targetType, oldIndex, newIndex, address, rhs);
      } else {
        result = bfmgr.makeTrue();
      }

      if (updatedRegions != null) {
        updatedRegions.add(region);
      }
    }

    return result;
  }

  private Optional<Formula> getValueFormula(CType pRValueType, Expression pRValue)
      throws AssertionError {
    switch (pRValue.getKind()) {
      case ALIASED_LOCATION:
        MemoryRegion region = pRValue.asAliasedLocation().getMemoryRegion();
        if(region == null) {
          region = regionMgr.makeMemoryRegion(pRValueType);
        }
        return Optional.of(
            conv.makeDereference(
                pRValueType, pRValue.asAliasedLocation().getAddress(), ssa, errorConditions, region));
      case UNALIASED_LOCATION:
        return Optional.of(
            conv.makeVariable(pRValue.asUnaliasedLocation().getVariableName(), pRValueType, ssa));
      case DET_VALUE:
        return Optional.of(pRValue.asValue().getValue());
      case NONDET:
        return Optional.empty();
      default:
        throw new AssertionError();
    }
  }

  /**
   * Add terms to the {@link #constraints} object that specify that unwritten heap cells
   * keep their value when the SSA index is updated.
   *
   * @param lvalueType The LHS type of the current assignment.
   * @param startAddress The start address of the written heap region.
   * @param pattern The pattern matching the (potentially) written heap cells.
   * @param regionsToRetain The set of regions which were affected by the assignment.
   */
  private void addRetentionForAssignment(
      MemoryRegion region,
      CType lvalueType,
      final Formula startAddress,
      final PointerTargetPattern pattern,
      final Set<MemoryRegion> regionsToRetain)
      throws InterruptedException {
    checkNotNull(lvalueType);
    checkNotNull(startAddress);
    checkNotNull(pattern);
    checkNotNull(regionsToRetain);

    if (options.useArraysForHeap()) {
      // not necessary for heap-array encoding
      return;
    }

    checkIsSimplified(lvalueType);
    final int size = conv.getBitSizeof(lvalueType);

    if (options.useQuantifiersOnArrays()) {
      addRetentionConstraintsWithQuantifiers(
          lvalueType, pattern, startAddress, size, regionsToRetain);
    } else {
      addRetentionConstraintsWithoutQuantifiers(
          region, lvalueType, pattern, startAddress, size, regionsToRetain);
    }
  }

  /**
   * Add retention constraints as specified by
   * {@link #addRetentionForAssignment(MemoryRegion, CType, Formula, PointerTargetPattern, Set)}
   * with the help of quantifiers.
   * Such a constraint is simply {@code forall i : !matches(i) => retention(i)}
   * where {@code matches(i)} specifies whether address {@code i} was written.
   */
  private void addRetentionConstraintsWithQuantifiers(
      final CType lvalueType,
      final PointerTargetPattern pattern,
      final Formula startAddress,
      final int size,
      final Set<MemoryRegion> regions) {

    for (final MemoryRegion region : regions) {
      final String ufName = regionMgr.getPointerAccessName(region);
      final int oldIndex = conv.getIndex(ufName, region.getType(), ssa);
      final int newIndex = conv.getFreshIndex(ufName, region.getType(), ssa);
      final FormulaType<?> targetType = conv.getFormulaTypeFromCType(region.getType());

      // forall counter : !condition => retentionConstraint
      // is equivalent to:
      // forall counter : condition || retentionConstraint

      final Formula counter = fmgr.makeVariable(conv.voidPointerFormulaType, ufName + "@counter");
      final BooleanFormula updateCondition;
      if (isSimpleType(lvalueType)) {
        updateCondition = fmgr.makeEqual(counter, startAddress);
      } else if (pattern.isExact()) {
        // TODO Is this branch necessary? startAddress and targetAddress should be equivalent.
        final Formula targetAddress = conv.makeFormulaForTarget(pattern.asPointerTarget());
        updateCondition = fmgr.makeElementIndexConstraint(counter, targetAddress, size, false);
      } else {
        updateCondition = fmgr.makeElementIndexConstraint(counter, startAddress, size, false);
      }

      final BooleanFormula body =
          bfmgr.or(
              updateCondition,
              conv.makeRetentionConstraint(ufName, oldIndex, newIndex, targetType, counter));

      constraints.addConstraint(fmgr.getQuantifiedFormulaManager().forall(counter, body));
    }
  }

  /**
   * Add retention constraints as specified by
   * {@link #addRetentionForAssignment(MemoryRegion, CType, Formula, PointerTargetPattern, Set)}
   * in a bounded way by manually iterating over all possibly written heap cells
   * and adding a constraint for each of them.
   */
  private void addRetentionConstraintsWithoutQuantifiers(
      MemoryRegion region,
      CType lvalueType,
      final PointerTargetPattern pattern,
      final Formula startAddress,
      final int size,
      final Set<MemoryRegion> regionsToRetain)
      throws InterruptedException {

    checkNotNull(region);
    if (isSimpleType(lvalueType)) {
      addSimpleTypeRetentionConstraints(pattern, ImmutableSet.of(region), startAddress);

    } else if (pattern.isExact()) {
      addExactRetentionConstraints(pattern.withRange(size), regionsToRetain);

    } else if (pattern.isSemiExact()) {
      // For semiexact retention constraints we need the first element type of the composite
      if (lvalueType instanceof CArrayType) {
        lvalueType = checkIsSimplified(((CArrayType) lvalueType).getType());
        region = regionMgr.makeMemoryRegion(lvalueType);
      } else { // CCompositeType
        CCompositeTypeMemberDeclaration memberDeclaration = ((CCompositeType) lvalueType).getMembers().get(0);
        region = regionMgr.makeMemoryRegion(lvalueType, checkIsSimplified(memberDeclaration.getType()), memberDeclaration.getName());
      }
      //for lvalueType
      addSemiexactRetentionConstraints(pattern, region, startAddress, size, regionsToRetain);

    } else { // Inexact pointer target pattern
      addInexactRetentionConstraints(startAddress, size, regionsToRetain);
    }
  }

  /**
   * Create formula constraints that retain values from the current SSA index to the next one.
   * @param regions The set of regions for which constraints should be created.
   * @param targetLookup A function that gives the PointerTargets for a type for which constraints should be created.
   * @param constraintConsumer A function that accepts a Formula with the address of the current target and the respective constraint.
   */
  private void makeRetentionConstraints(
      final Set<MemoryRegion> regions,
      final Function<MemoryRegion, ? extends Iterable<PointerTarget>> targetLookup,
      final BiConsumer<Formula, BooleanFormula> constraintConsumer)
      throws InterruptedException {

    for (final MemoryRegion region : regions) {
      final String ufName = regionMgr.getPointerAccessName(region);
      final int oldIndex = conv.getIndex(ufName, region.getType(), ssa);
      final int newIndex = conv.getFreshIndex(ufName, region.getType(), ssa);
      final FormulaType<?> targetType = conv.getFormulaTypeFromCType(region.getType());

      for (final PointerTarget target : targetLookup.apply(region)) {
        regionMgr.addTargetToStats(edge, ufName, target);
        conv.shutdownNotifier.shutdownIfNecessary();
        final Formula targetAddress = conv.makeFormulaForTarget(target);
        constraintConsumer.accept(
            targetAddress,
            conv.makeRetentionConstraint(ufName, oldIndex, newIndex, targetType, targetAddress));
      }
    }
  }

  /**
   * Add retention constraints without quantifiers for writing a simple (non-composite) type.
   *
   * All heap cells where the pattern does not match retained,
   * and if the pattern is not exact there are also conditional constraints
   * for cells that might be matched by the pattern.
   */
  private void addSimpleTypeRetentionConstraints(
      final PointerTargetPattern pattern, final Set<MemoryRegion> regions, final Formula startAddress)
      throws InterruptedException {
    if (!pattern.isExact()) {
      makeRetentionConstraints(
          regions,
          region -> pts.getMatchingTargets(region, pattern),
          (targetAddress, constraint) -> {
            final BooleanFormula updateCondition = fmgr.makeEqual(targetAddress, startAddress);
            constraints.addConstraint(bfmgr.or(updateCondition, constraint));
          });
    }

    addExactRetentionConstraints(pattern, regions);
  }

  /**
   * Add retention constraints without quantifiers for the case where the written memory region
   * is known exactly.
   * All heap cells where the pattern does not match retained.
   */
  private void addExactRetentionConstraints(
      final Predicate<PointerTarget> pattern, final Set<MemoryRegion> regions) throws InterruptedException {
    makeRetentionConstraints(
        regions,
        region -> pts.getNonMatchingTargets(region, pattern),
        (targetAddress, constraint) -> constraints.addConstraint(constraint));
  }

  /**
   * Add retention constraints without quantifiers for the case where some information is known
   * about the written memory region.
   * For each of the potentially written target candidates we add retention constraints
   * under the condition that it was this target that was actually written.
   */
  private void addSemiexactRetentionConstraints(
      final PointerTargetPattern pattern,
      final MemoryRegion firstElementRegion,
      final Formula startAddress,
      final int size,
      final Set<MemoryRegion> regions)
      throws InterruptedException {
    for (final PointerTarget target : pts.getMatchingTargets(firstElementRegion, pattern)) {
      final Formula candidateAddress = conv.makeFormulaForTarget(target);
      final BooleanFormula negAntecedent =
          bfmgr.not(fmgr.makeEqual(candidateAddress, startAddress));
      final Predicate<PointerTarget> exact =
          PointerTargetPattern.forRange(target.getBase(), target.getOffset(), size);

      List<BooleanFormula> consequent = new ArrayList<>();
      makeRetentionConstraints(
          regions,
          region -> pts.getNonMatchingTargets(region, exact),
          (targetAddress, constraint) -> consequent.add(constraint));
      constraints.addConstraint(bfmgr.or(negAntecedent, bfmgr.and(consequent)));
    }
  }

  /**
   * Add retention constraints without quantifiers for the case where nothing is known
   * about the written memory region.
   * For every heap cell we add a conditional constraint to retain it.
   */
  private void addInexactRetentionConstraints(
      final Formula startAddress, final int size, final Set<MemoryRegion> regions)
      throws InterruptedException {
    makeRetentionConstraints(
        regions,
        region -> pts.getAllTargets(region),
        (targetAddress, constraint) -> {
          final BooleanFormula updateCondition =
              fmgr.makeElementIndexConstraint(targetAddress, startAddress, size, false);
          constraints.addConstraint(bfmgr.or(updateCondition, constraint));
        });
  }

  /**
   * Updates the SSA map.
   *
   * @param regions A set of regions that should be added to the SSA map.
   * @param ssa The current SSA map.
   */
  private void updateSSA(final Set<MemoryRegion> regions, final SSAMapBuilder ssa) {
    for (final MemoryRegion region : regions) {
      final String ufName = regionMgr.getPointerAccessName(region);
      conv.makeFreshIndex(ufName, region.getType(), ssa);
    }
  }

  /**
   * Shifts the array's lvalue.
   *
   * @param lvalue The lvalue location.
   * @param offset The offset of the shift.
   * @param lvalueElementType The type of the lvalue element.
   * @return A tuple of location and type after the shift.
   */
  private Pair<AliasedLocation, CType> shiftArrayLvalue(final AliasedLocation lvalue,
                                                        final int offset,
                                                        final CType lvalueElementType) {
    final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
    final AliasedLocation newLvalue = Location.ofAddress(fmgr.makePlus(lvalue.getAddress(), offsetFormula));
    return Pair.of(newLvalue, lvalueElementType);
  }

  /**
   * Shifts the array's rvalue.
   *
   * @param rvalue The rvalue expression.
   * @param rvalueType The type of the rvalue.
   * @param offset The offset of the shift.
   * @param lvalueElementType The type of the lvalue element.
   * @return A tuple of expression and type after the shift.
   */
  private Pair<? extends Expression, CType> shiftArrayRvalue(final Expression rvalue,
                                                             final CType rvalueType,
                                                             final int offset,
                                                             final CType lvalueElementType) {
    // Support both initialization (with a value or nondet) and assignment (from another array location)
    switch (rvalue.getKind()) {
    case ALIASED_LOCATION: {
      assert rvalueType instanceof CArrayType : "Non-array rvalue in array assignment";
      final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
      final AliasedLocation newRvalue = Location.ofAddress(fmgr.makePlus(rvalue.asAliasedLocation().getAddress(),
                                                           offsetFormula));
      final CType newRvalueType = checkIsSimplified(((CArrayType) rvalueType).getType());
      return Pair.of(newRvalue, newRvalueType);
    }
    case DET_VALUE: {
      return Pair.of(rvalue, rvalueType);
    }
    case NONDET: {
      final CType newLvalueType = isSimpleType(lvalueElementType) ? lvalueElementType : CNumericTypes.SIGNED_CHAR;
      return Pair.of(Value.nondetValue(), newLvalueType);
    }
    case UNALIASED_LOCATION: {
      throw new AssertionError("Array locations should always be aliased");
    }
    default: throw new AssertionError();
    }
  }

  /**
   * Shifts the composite lvalue.
   *
   * @param lvalue The lvalue location.
   * @param offset The offset of the shift.
   * @param memberName The name of the member.
   * @param memberType The type of the member.
   * @return A tuple of location and type after the shift.
   */
  private Pair<? extends Location, CType> shiftCompositeLvalue(final CType lvalueType,
                                                               final Location lvalue,
                                                               final int offset,
                                                               final String memberName,
                                                               final CType memberType) {
    final CType newLvalueType = checkIsSimplified(memberType);
    if (lvalue.isAliased()) {
      final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
      final MemoryRegion region = regionMgr.makeMemoryRegion(lvalueType, newLvalueType, memberName);
      final AliasedLocation newLvalue = Location.ofAddressWithRegion(fmgr.makePlus(lvalue.asAliased().getAddress(),
                                                                         offsetFormula), region);
      return Pair.of(newLvalue, newLvalueType);

    } else {
      final UnaliasedLocation newLvalue = Location.ofVariableName(lvalue.asUnaliased().getVariableName() +
                                                                  CToFormulaConverterWithPointerAliasing.FIELD_NAME_SEPARATOR + memberName);
      return Pair.of(newLvalue, newLvalueType);
    }

  }

  /**
   * Shifts the composite rvalue.
   *
   * @param rvalue The rvalue expression.
   * @param offset The offset of the shift.
   * @param memberName The name of the member.
   * @param rvalueType The type of the rvalue.
   * @param memberType The type of the member.
   * @return A tuple of expression and type after the shift.
   */
  private Pair<? extends Expression, CType> shiftCompositeRvalue(final Expression rvalue,
                                                                 final int offset,
                                                                 final String memberName,
                                                                 final CType rvalueType,
                                                                 final CType memberType) {
    // Support both structure assignment and initialization with a value (or nondet)
    final CType newLvalueType = checkIsSimplified(memberType);
    switch (rvalue.getKind()) {
    case ALIASED_LOCATION: {
      final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
      final MemoryRegion region = regionMgr.makeMemoryRegion(rvalueType, newLvalueType, memberName);
      final AliasedLocation newRvalue = Location.ofAddressWithRegion(fmgr.makePlus(rvalue.asAliasedLocation().getAddress(),
                                                                         offsetFormula), region);
      return Pair.of(newRvalue, newLvalueType);
    }
    case UNALIASED_LOCATION: {
      final UnaliasedLocation newRvalue = Location.ofVariableName(rvalue.asUnaliasedLocation().getVariableName() +
                                                                  CToFormulaConverterWithPointerAliasing.FIELD_NAME_SEPARATOR +
                                                                  memberName);
      return Pair.of(newRvalue, newLvalueType);
    }
    case DET_VALUE: {
      return Pair.of(rvalue, rvalueType);
    }
    case NONDET: {
      final CType newRvalueType = isSimpleType(newLvalueType) ? newLvalueType : CNumericTypes.SIGNED_CHAR;
      return Pair.of(Value.nondetValue(), newRvalueType);
    }
    default: throw new AssertionError();
    }
  }
}
