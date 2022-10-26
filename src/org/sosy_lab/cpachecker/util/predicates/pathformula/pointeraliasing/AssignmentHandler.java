// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.implicitCastToPointer;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.IsRelevantWithHavocAbstractionVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/** Implements a handler for assignments. */
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
  AssignmentHandler(
      CToFormulaConverterWithPointerAliasing pConv,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions,
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
   * @param useOldSSAIndicesIfAliased A flag indicating whether we can use old SSA indices for
   *     aliased locations (because the location was not used before)
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  BooleanFormula handleAssignment(
      final CLeftHandSide lhs,
      final CLeftHandSide lhsForChecking,
      final CType lhsType,
      final @Nullable CRightHandSide rhs,
      final boolean useOldSSAIndicesIfAliased)
      throws UnrecognizedCodeException, InterruptedException {
    if (!conv.isRelevantLeftHandSide(lhsForChecking)) {
      // Optimization for unused variables and fields
      return conv.bfmgr.makeTrue();
    }

    final CType rhsType =
        rhs != null ? typeHandler.getSimplifiedType(rhs) : CNumericTypes.SIGNED_CHAR;

    // RHS handling
    final CExpressionVisitorWithPointerAliasing rhsVisitor = newExpressionVisitor();

    final Expression rhsExpression;

    if (conv.options.useHavocAbstraction()
        && (rhs == null || !rhs.accept(new IsRelevantWithHavocAbstractionVisitor(conv)))) {
      rhsExpression = Value.nondetValue();
    } else {
      rhsExpression = createRHSExpression(rhs, lhsType, rhsVisitor);
    }

    pts.addEssentialFields(rhsVisitor.getInitializedFields());
    pts.addEssentialFields(rhsVisitor.getUsedFields());
    final List<CompositeField> rhsAddressedFields = rhsVisitor.getAddressedFields();
    final Map<String, CType> rhsLearnedPointersTypes = rhsVisitor.getLearnedPointerTypes();

    // LHS handling
    final CExpressionVisitorWithPointerAliasing lhsVisitor = newExpressionVisitor();
    final Expression lhsExpression = lhs.accept(lhsVisitor);
    if (lhsExpression.isNondetValue()) {
      // only because of CExpressionVisitorWithPointerAliasing.visit(CFieldReference)
      conv.logger.logfOnce(
          Level.WARNING,
          "%s: Ignoring assignment to %s because bit fields are currently not fully supported",
          edge.getFileLocation(),
          lhs);
      return conv.bfmgr.makeTrue();
    }
    final Location lhsLocation = lhsExpression.asLocation();
    final boolean useOldSSAIndices = useOldSSAIndicesIfAliased && lhsLocation.isAliased();

    final Map<String, CType> lhsLearnedPointerTypes = lhsVisitor.getLearnedPointerTypes();
    pts.addEssentialFields(lhsVisitor.getInitializedFields());
    pts.addEssentialFields(lhsVisitor.getUsedFields());
    // the pattern matching possibly aliased locations

    if (conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) {
      DynamicMemoryHandler memoryHandler =
          new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);
      memoryHandler.handleDeferredAllocationsInAssignment(
          lhs, rhs, rhsExpression, lhsType, lhsLearnedPointerTypes, rhsLearnedPointersTypes);
    }

    // necessary only for update terms for new UF indices
    Set<MemoryRegion> updatedRegions =
        useOldSSAIndices || options.useArraysForHeap() ? null : new HashSet<>();

    final BooleanFormula result =
        makeDestructiveAssignment(
            lhsType, rhsType, lhsLocation, rhsExpression, useOldSSAIndices, updatedRegions);

    if (lhsLocation.isUnaliasedLocation() && lhs instanceof CFieldReference) {
      CFieldReference fieldReference = (CFieldReference) lhs;
      CExpression fieldOwner = fieldReference.getFieldOwner();
      CType ownerType = typeHandler.getSimplifiedType(fieldOwner);
      if (!fieldReference.isPointerDereference() && ownerType instanceof CCompositeType) {
        if (((CCompositeType) ownerType).getKind() == ComplexTypeKind.UNION) {
          addAssignmentsForOtherFieldsOfUnion(
              lhsType,
              (CCompositeType) ownerType,
              rhsType,
              rhsExpression,
              useOldSSAIndices,
              updatedRegions,
              fieldReference);
        }
        if (fieldOwner instanceof CFieldReference) {
          CFieldReference owner = (CFieldReference) fieldOwner;
          CType ownersOwnerType = typeHandler.getSimplifiedType(owner.getFieldOwner());
          if (ownersOwnerType instanceof CCompositeType
              && ((CCompositeType) ownersOwnerType).getKind() == ComplexTypeKind.UNION) {
            addAssignmentsForOtherFieldsOfUnion(
                ownersOwnerType,
                (CCompositeType) ownersOwnerType,
                ownerType,
                createRHSExpression(owner, ownerType, rhsVisitor),
                useOldSSAIndices,
                updatedRegions,
                owner);
          }
        }
      }
    }

    if (!useOldSSAIndices && !options.useArraysForHeap()) {
      if (lhsLocation.isAliased()) {
        final PointerTargetPattern pattern =
            PointerTargetPattern.forLeftHandSide(lhs, typeHandler, edge, pts);
        finishAssignmentsForUF(lhsType, lhsLocation.asAliased(), pattern, updatedRegions);
      } else { // Unaliased lvalue
        assert updatedRegions != null && updatedRegions.isEmpty();
      }
    }

    for (final CompositeField field : rhsAddressedFields) {
      pts.addField(field);
    }
    return result;
  }

  private Expression createRHSExpression(
      CRightHandSide pRhs, CType pLhsType, CExpressionVisitorWithPointerAliasing pRhsVisitor)
      throws UnrecognizedCodeException {
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
      final boolean useOldSSAIndicesIfAliased)
      throws UnrecognizedCodeException, InterruptedException {
    return handleAssignment(
        lhs, lhsForChecking, typeHandler.getSimplifiedType(lhs), rhs, useOldSSAIndicesIfAliased);
  }

  /**
   * Handles initialization assignments.
   *
   * @param variable The declared variable.
   * @param declarationType The type of the declared variable.
   * @param assignments A list of assignment statements.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException It the execution was interrupted.
   */
  BooleanFormula handleInitializationAssignments(
      final CIdExpression variable,
      final CType declarationType,
      final List<CExpressionAssignmentStatement> assignments)
      throws UnrecognizedCodeException, InterruptedException {
    if (options.useQuantifiersOnArrays()
        && (declarationType instanceof CArrayType)
        && !assignments.isEmpty()) {
      return handleInitializationAssignmentsWithQuantifier(variable, assignments, false);
    } else {
      return handleInitializationAssignmentsWithoutQuantifier(assignments);
    }
  }

  /**
   * Handles initialization assignments.
   *
   * @param assignments A list of assignment statements.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException It the execution was interrupted.
   */
  private BooleanFormula handleInitializationAssignmentsWithoutQuantifier(
      final List<CExpressionAssignmentStatement> assignments)
      throws UnrecognizedCodeException, InterruptedException {
    BooleanFormula result = conv.bfmgr.makeTrue();
    for (CExpressionAssignmentStatement assignment : assignments) {
      final CLeftHandSide lhs = assignment.getLeftHandSide();
      result =
          conv.bfmgr.and(result, handleAssignment(lhs, lhs, assignment.getRightHandSide(), true));
    }
    return result;
  }

  /**
   * Handles an initialization assignments, i.e. an assignment with a C initializer, with using a
   * quantifier over the resulting SMT array.
   *
   * <p>If we cannot make an assignment of the form {@code <variable> = <value>}, we fall back to
   * the normal initialization in {@link #handleInitializationAssignmentsWithoutQuantifier(List)}.
   *
   * @param pLeftHandSide The left hand side of the statement. Needed for fallback scenario.
   * @param pAssignments A list of assignment statements.
   * @param pUseOldSSAIndices A flag indicating whether we will reuse SSA indices or not.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   * @see #handleInitializationAssignmentsWithoutQuantifier(List)
   */
  private BooleanFormula handleInitializationAssignmentsWithQuantifier(
      final CIdExpression pLeftHandSide,
      final List<CExpressionAssignmentStatement> pAssignments,
      final boolean pUseOldSSAIndices)
      throws UnrecognizedCodeException, InterruptedException {

    assert !pAssignments.isEmpty()
        : "Cannot handle initialization assignments without an " + "assignment right hand side.";

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
      // (cf. test/programs/simple/struct-initializer-for-composite-field.c)
      //    struct s { int x; };
      //    struct t { struct s s; };
      //    ...
      //    const struct s s = { .x = 1 };
      //    struct t t = { .s = s };
      return handleInitializationAssignmentsWithoutQuantifier(pAssignments);
    } else {
      MemoryRegion region = lhsLocation.asAliased().getMemoryRegion();
      if (region == null) {
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
          fmgr.makeVariableWithoutSSAIndex(
              conv.voidPointerFormulaType, targetName + "__" + oldIndex + "__counter");
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
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  private boolean checkEqualityOfInitializers(
      final List<CExpressionAssignmentStatement> pAssignments,
      final CExpressionVisitorWithPointerAliasing pRhsVisitor)
      throws UnrecognizedCodeException {
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

  private void finishAssignmentsForUF(
      CType lvalueType,
      final AliasedLocation lvalue,
      final PointerTargetPattern pattern,
      final Set<MemoryRegion> updatedRegions)
      throws InterruptedException {
    MemoryRegion region = lvalue.getMemoryRegion();
    if (region == null) {
      region = regionMgr.makeMemoryRegion(lvalueType);
    }
    if (isSimpleType(lvalueType)) {
      assert updatedRegions.contains(region);
    }
    addRetentionForAssignment(region, lvalueType, lvalue.getAddress(), pattern, updatedRegions);
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
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  BooleanFormula makeDestructiveAssignment(
      CType lvalueType,
      CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions)
      throws UnrecognizedCodeException {
    checkIsSimplified(lvalueType);
    checkIsSimplified(rvalueType);
    checkArgument(
        !useOldSSAIndices || updatedRegions == null,
        "With old SSA indices returning updated regions does not make sense");

    if (lvalueType instanceof CArrayType) {
      return makeDestructiveArrayAssignment(
          (CArrayType) lvalueType, rvalueType, lvalue, rvalue, useOldSSAIndices, updatedRegions);

    } else if (lvalueType instanceof CCompositeType) {
      final CCompositeType lvalueCompositeType = (CCompositeType) lvalueType;
      return makeDestructiveCompositeAssignment(
          lvalueCompositeType, rvalueType, lvalue, rvalue, useOldSSAIndices, updatedRegions);

    } else { // Simple assignment
      return makeSimpleDestructiveAssignment(
          lvalueType, rvalueType, lvalue, rvalue, useOldSSAIndices, updatedRegions);
    }
  }

  private BooleanFormula makeDestructiveArrayAssignment(
      CArrayType lvalueArrayType,
      CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions)
      throws UnrecognizedCodeException {
    checkArgument(lvalue.isAliased(), "Array elements are always aliased");
    final CType lvalueElementType = lvalueArrayType.getType();

    OptionalInt lvalueLength = lvalueArrayType.getLengthAsInt();
    // Try to fix the length if it's unknown (or too big)
    // Also ignore the tail part of very long arrays to avoid very large formulae (imprecise!)
    if (!lvalueLength.isPresent() && rvalue.isLocation()) {
      lvalueLength = ((CArrayType) rvalueType).getLengthAsInt();
    }
    int length =
        lvalueLength.isPresent()
            ? Integer.min(options.maxArrayLength(), lvalueLength.orElseThrow())
            : options.defaultArrayLength();

    // There are two cases of assignment to an array
    // - Initialization with a value (possibly nondet), useful for stack declarations and memset
    // - Array assignment as part of a structure assignment
    final CType newRvalueType;
    if (rvalue.isValue()) {
      checkArgument(
          isSimpleType(rvalueType),
          "Impossible assignment of %s with type %s to array:",
          rvalue,
          rvalueType);
      if (rvalue.isNondetValue()) {
        newRvalueType =
            isSimpleType(lvalueElementType) ? lvalueElementType : CNumericTypes.SIGNED_CHAR;
      } else {
        newRvalueType = rvalueType;
      }

    } else {
      checkArgument(
          rvalue.asLocation().isAliased(),
          "Impossible assignment of %s with type %s to array:",
          rvalue,
          rvalueType);
      checkArgument(
          ((CArrayType) rvalueType).getType().equals(lvalueElementType),
          "Impossible array assignment due to incompatible types: assignment of %s with type %s to"
              + " %s with type %s",
          rvalue,
          rvalueType,
          lvalue,
          lvalueArrayType);
      newRvalueType = checkIsSimplified(((CArrayType) rvalueType).getType());
    }

    BooleanFormula result = bfmgr.makeTrue();
    int offset = 0;
    for (int i = 0; i < length; ++i) {
      final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
      final AliasedLocation newLvalue =
          AliasedLocation.ofAddress(fmgr.makePlus(lvalue.asAliased().getAddress(), offsetFormula));
      final Expression newRvalue;

      // Support both initialization (with a value or nondet) and assignment (from another array
      // location)
      if (rvalue.isValue()) {
        newRvalue = rvalue;
      } else {
        newRvalue =
            AliasedLocation.ofAddress(
                fmgr.makePlus(rvalue.asAliasedLocation().getAddress(), offsetFormula));
      }

      result =
          bfmgr.and(
              result,
              makeDestructiveAssignment(
                  lvalueElementType,
                  newRvalueType,
                  newLvalue,
                  newRvalue,
                  useOldSSAIndices,
                  updatedRegions));
      offset += conv.getSizeof(lvalueArrayType.getType());
    }
    return result;
  }

  private BooleanFormula makeDestructiveCompositeAssignment(
      final CCompositeType lvalueCompositeType,
      CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions)
      throws UnrecognizedCodeException {
    // There are two cases of assignment to a structure/union
    // - Initialization with a value (possibly nondet), useful for stack declarations and memset
    // - Structure assignment
    checkArgument(
        (rvalue.isValue() && isSimpleType(rvalueType)) || rvalueType.equals(lvalueCompositeType),
        "Impossible assignment due to incompatible types: assignment of %s with type %s to %s with"
            + " type %s",
        rvalue,
        rvalueType,
        lvalue,
        lvalueCompositeType);

    BooleanFormula result = bfmgr.makeTrue();
    for (final CCompositeTypeMemberDeclaration memberDeclaration :
        lvalueCompositeType.getMembers()) {
      final CType newLvalueType = typeHandler.getSimplifiedType(memberDeclaration);
      // Optimizing away the assignments from uninitialized fields
      if (conv.isRelevantField(lvalueCompositeType, memberDeclaration)
          && (
          // Assignment to a variable, no profit in optimizing it
          !lvalue.isAliased()
              || // That's not a simple assignment, check the nested composite
              !isSimpleType(newLvalueType)
              || // This is initialization, so the assignment is mandatory
              rvalue.isValue()
              || // The field is tracked as essential
              pts.tracksField(CompositeField.of(lvalueCompositeType, memberDeclaration))
              || // The variable representing the RHS was used somewhere (i.e. has SSA index)
              (!rvalue.isAliasedLocation()
                  && conv.hasIndex(
                      getFieldAccessName(
                          rvalue.asUnaliasedLocation().getVariableName(), memberDeclaration),
                      newLvalueType,
                      ssa)))) {

        final OptionalLong offset = typeHandler.getOffset(lvalueCompositeType, memberDeclaration);
        if (!offset.isPresent()) {
          continue; // TODO this looses values of bit fields
        }
        final Formula offsetFormula =
            fmgr.makeNumber(conv.voidPointerFormulaType, offset.orElseThrow());
        final Location newLvalue;
        if (lvalue.isAliased()) {
          final MemoryRegion region =
              regionMgr.makeMemoryRegion(lvalueCompositeType, memberDeclaration);
          newLvalue =
              AliasedLocation.ofAddressWithRegion(
                  fmgr.makePlus(lvalue.asAliased().getAddress(), offsetFormula), region);

        } else {
          newLvalue =
              UnaliasedLocation.ofVariableName(
                  getFieldAccessName(lvalue.asUnaliased().getVariableName(), memberDeclaration));
        }

        final CType newRvalueType;
        final Expression newRvalue;
        if (rvalue.isLocation()) {
          newRvalueType = newLvalueType;
          if (rvalue.isAliasedLocation()) {
            final MemoryRegion region = regionMgr.makeMemoryRegion(rvalueType, memberDeclaration);
            newRvalue =
                AliasedLocation.ofAddressWithRegion(
                    fmgr.makePlus(rvalue.asAliasedLocation().getAddress(), offsetFormula), region);
          } else {
            newRvalue =
                UnaliasedLocation.ofVariableName(
                    getFieldAccessName(
                        rvalue.asUnaliasedLocation().getVariableName(), memberDeclaration));
          }

        } else {
          newRvalue = rvalue;
          if (rvalue.isNondetValue()) {
            newRvalueType = isSimpleType(newLvalueType) ? newLvalueType : CNumericTypes.SIGNED_CHAR;
          } else {
            newRvalueType = rvalueType;
          }
        }

        result =
            bfmgr.and(
                result,
                makeDestructiveAssignment(
                    newLvalueType,
                    newRvalueType,
                    newLvalue,
                    newRvalue,
                    useOldSSAIndices,
                    updatedRegions));
      }
    }
    return result;
  }

  /**
   * Creates a formula for a simple destructive assignment.
   *
   * @param lvalueType The type of the lvalue.
   * @param pRvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  private BooleanFormula makeSimpleDestructiveAssignment(
      CType lvalueType,
      final CType pRvalueType,
      final Location lvalue,
      Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions)
      throws UnrecognizedCodeException {
    // Arrays and functions are implicitly converted to pointers
    CType rvalueType = implicitCastToPointer(pRvalueType);

    checkArgument(isSimpleType(lvalueType));
    checkArgument(isSimpleType(rvalueType));
    assert !(lvalueType instanceof CFunctionType) : "Can't assign to functions";

    final FormulaType<?> targetType = conv.getFormulaTypeFromCType(lvalueType);
    final BooleanFormula result;

    Formula rhs;
    if (pRvalueType instanceof CArrayType && rvalue.isAliasedLocation()) {
      // When assigning an array to a pointer, the address of the array is taken
      rhs = rvalue.asAliasedLocation().getAddress();
    } else {
      final Optional<Formula> value = getValueFormula(rvalueType, rvalue);
      rhs =
          value.isPresent()
              ? conv.makeCast(rvalueType, lvalueType, value.orElseThrow(), constraints, edge)
              : null;
    }

    if (!lvalue.isAliased()) { // Unaliased LHS
      assert !useOldSSAIndices;

      final String targetName = lvalue.asUnaliased().getVariableName();
      final int newIndex = conv.makeFreshIndex(targetName, lvalueType, ssa);

      if (rhs != null) {
        result = fmgr.assignment(fmgr.makeVariable(targetType, targetName, newIndex), rhs);
      } else {
        result = bfmgr.makeTrue();
      }

    } else { // Aliased LHS
      MemoryRegion region = lvalue.asAliased().getMemoryRegion();
      if (region == null) {
        region = regionMgr.makeMemoryRegion(lvalueType);
      }
      final String targetName = regionMgr.getPointerAccessName(region);

      final int oldIndex = conv.getIndex(targetName, lvalueType, ssa);
      final int newIndex;
      if (useOldSSAIndices) {
        assert updatedRegions == null : "Returning updated regions is only for new indices";
        newIndex = oldIndex;

      } else if (options.useArraysForHeap()) {
        assert updatedRegions == null : "Return updated regions is only for UF encoding";
        if (rhs == null) {
          // For arrays, we always need to add a term that connects oldIndex with newIndex
          String nondetName =
              "__nondet_value_" + CTypeUtils.typeToString(rvalueType).replace(' ', '_');
          rhs = conv.makeNondet(nondetName, rvalueType, ssa, constraints);
          rhs = conv.makeCast(rvalueType, lvalueType, rhs, constraints, edge);
        }
        newIndex = conv.makeFreshIndex(targetName, lvalueType, ssa);

      } else {
        assert updatedRegions != null : "UF encoding needs to update regions for new indices";
        updatedRegions.add(region);
        // For UFs, we use a new index without storing it such that we use the same index
        // for multiple writes that are part of the same assignment.
        // The new index will be stored in the SSAMap later.
        newIndex = conv.getFreshIndex(targetName, lvalueType, ssa);
      }

      if (rhs != null) {
        final Formula address = lvalue.asAliased().getAddress();
        result =
            conv.ptsMgr.makePointerAssignment(
                targetName, targetType, oldIndex, newIndex, address, rhs);
      } else {
        result = bfmgr.makeTrue();
      }
    }

    return result;
  }

  private Optional<Formula> getValueFormula(CType pRValueType, Expression pRValue)
      throws AssertionError {
    switch (pRValue.getKind()) {
      case ALIASED_LOCATION:
        MemoryRegion region = pRValue.asAliasedLocation().getMemoryRegion();
        if (region == null) {
          region = regionMgr.makeMemoryRegion(pRValueType);
        }
        return Optional.of(
            conv.makeDereference(
                pRValueType,
                pRValue.asAliasedLocation().getAddress(),
                ssa,
                errorConditions,
                region));
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

  private void addAssignmentsForOtherFieldsOfUnion(
      final CType lhsType,
      final CCompositeType ownerType,
      final CType rhsType,
      final Expression rhsExpression,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions,
      final CFieldReference fieldReference)
      throws UnrecognizedCodeException {
    final CExpressionVisitorWithPointerAliasing lhsVisitor = newExpressionVisitor();
    for (CCompositeTypeMemberDeclaration member : ownerType.getMembers()) {
      if (member.getName().equals(fieldReference.getFieldName())) {
        continue; // handled already as the main assignment
      }

      final CType newLhsType = member.getType();
      final CExpression newLhs =
          new CFieldReference(
              FileLocation.DUMMY,
              newLhsType,
              member.getName(),
              fieldReference.getFieldOwner(),
              false);
      final Location newLhsLocation = newLhs.accept(lhsVisitor).asLocation();
      assert newLhsLocation.isUnaliasedLocation();

      if (CTypeUtils.isSimpleType(newLhsType)) {
        addAssignmentsForOtherFieldsOfUnionForLhsSimpleType(
            lhsType,
            newLhsType,
            rhsType,
            rhsExpression,
            fieldReference,
            newLhsLocation,
            useOldSSAIndices,
            updatedRegions);
      }

      if (newLhsType instanceof CCompositeType
          && CTypeUtils.isSimpleType(rhsType)
          && !rhsExpression.isNondetValue()) {
        addAssignmentsForOtherFieldsOfUnionForLhsCompositeType(
            newLhs,
            (CCompositeType) newLhsType,
            rhsType,
            rhsExpression,
            lhsVisitor,
            member,
            useOldSSAIndices,
            updatedRegions);
      }
    }
  }

  private void addAssignmentsForOtherFieldsOfUnionForLhsSimpleType(
      final CType lhsType,
      final CType newLhsType,
      final CType rhsType,
      final Expression rhsExpression,
      final CFieldReference fieldReference,
      final Location newLhsLocation,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions)
      throws AssertionError, UnrecognizedCodeException, UnsupportedCodeException {
    final Expression newRhsExpression;
    if (CTypeUtils.isSimpleType(rhsType) && !rhsExpression.isNondetValue()) {
      Formula rhsFormula = getValueFormula(rhsType, rhsExpression).orElseThrow();
      rhsFormula = conv.makeCast(rhsType, lhsType, rhsFormula, constraints, edge);
      rhsFormula = conv.makeValueReinterpretation(lhsType, newLhsType, rhsFormula);
      newRhsExpression = Value.ofValueOrNondet(rhsFormula);
    } else if (rhsType instanceof CCompositeType) {
      // reinterpret compositetype as bitvector; concatenate its fields appropriately in case of
      // struct
      if (((CCompositeType) rhsType).getKind() == ComplexTypeKind.STRUCT) {
        CExpressionVisitorWithPointerAliasing expVisitor = newExpressionVisitor();
        int offset = 0;
        int targetSize = typeHandler.getBitSizeof(newLhsType);
        Formula rhsFormula = null;

        for (CCompositeTypeMemberDeclaration innerMember :
            ((CCompositeType) rhsType).getMembers()) {
          int innerMemberSize = typeHandler.getBitSizeof(innerMember.getType());

          CExpression innerMemberFieldReference =
              new CFieldReference(
                  FileLocation.DUMMY,
                  innerMember.getType(),
                  innerMember.getName(),
                  fieldReference,
                  false);
          Formula memberFormula =
              getValueFormula(
                      innerMember.getType(),
                      createRHSExpression(
                          innerMemberFieldReference, innerMember.getType(), expVisitor))
                  .orElseThrow();
          if (!(memberFormula instanceof BitvectorFormula)) {
            CType interType = TypeUtils.createTypeWithLength(innerMemberSize);
            memberFormula =
                conv.makeCast(innerMember.getType(), interType, memberFormula, constraints, edge);
            memberFormula =
                conv.makeValueReinterpretation(innerMember.getType(), interType, memberFormula);
          }
          assert memberFormula == null || memberFormula instanceof BitvectorFormula;

          if (memberFormula != null) {
            if (rhsFormula == null) {
              rhsFormula = fmgr.getBitvectorFormulaManager().makeBitvector(targetSize, 0);
            }

            boolean lhsSigned = false;
            if (!(newLhsType instanceof CPointerType)) {
              lhsSigned = ((CSimpleType) newLhsType).isSigned();
            }
            memberFormula = fmgr.makeExtend(memberFormula, targetSize - innerMemberSize, lhsSigned);
            memberFormula =
                fmgr.makeShiftLeft(
                    memberFormula,
                    fmgr.makeNumber(FormulaType.getBitvectorTypeWithSize(targetSize), offset));
            rhsFormula = fmgr.makePlus(rhsFormula, memberFormula);
          }

          offset += typeHandler.getBitSizeof(innerMember.getType());
        }

        if (rhsFormula != null) {
          CType fromType = TypeUtils.createTypeWithLength(targetSize);
          rhsFormula = conv.makeCast(fromType, newLhsType, rhsFormula, constraints, edge);
          rhsFormula = conv.makeValueReinterpretation(fromType, newLhsType, rhsFormula);
        }
        // make rhsexpression from constructed bitvector; perhaps cast to lhsType in advance?
        newRhsExpression = Value.ofValueOrNondet(rhsFormula);

        // make assignment to lhs
      } else {
        throw new UnsupportedCodeException(
            "Assignment of complex Unions via nested Struct-Members not supported", edge);
      }
    } else {
      newRhsExpression = Value.nondetValue();
    }
    final CType newRhsType = newLhsType;
    constraints.addConstraint(
        makeDestructiveAssignment(
            newLhsType,
            newRhsType,
            newLhsLocation,
            newRhsExpression,
            useOldSSAIndices,
            updatedRegions));
  }

  private void addAssignmentsForOtherFieldsOfUnionForLhsCompositeType(
      final CExpression newLhs,
      final CCompositeType newLhsType,
      final CType rhsType,
      final Expression rhsExpression,
      final CExpressionVisitorWithPointerAliasing lhsVisitor,
      CCompositeTypeMemberDeclaration member,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions)
      throws AssertionError, UnrecognizedCodeException {
    // Use different name in this block as newLhsType is confusing. newLhsType was computed as
    // member.getType() -> call it memberType here (note we will also have an innerMember)
    final CCompositeType memberType = newLhsType;
    // newLhs is a CFieldReference to member:
    final CExpression memberCFieldReference = newLhs;
    final int rhsSize = typeHandler.getBitSizeof(rhsType);

    // for each innerMember of member we need to add a (destructive!) constraint like:
    // union.member.innerMember := treatAsMemberTypeAndExtractInnerMemberValue(rhsExpression);
    for (CCompositeTypeMemberDeclaration innerMember : memberType.getMembers()) {
      int fieldOffset = (int) typeHandler.getBitOffset(memberType, innerMember);
      if (fieldOffset >= rhsSize) {
        // nothing to fill anymore
        break;
      }
      // don't try later to extract a too big chunk of bits
      int fieldSize =
          Math.min(typeHandler.getBitSizeof(innerMember.getType()), rhsSize - fieldOffset);
      assert fieldSize > 0;
      int startIndex = fieldOffset;
      int endIndex = fieldOffset + fieldSize - 1;

      // "treatAsMemberType"
      Formula rhsFormula = getValueFormula(rhsType, rhsExpression).orElseThrow();
      if (rhsType instanceof CPointerType) {
        // Do not break on Pointer-Handling
        CType rhsCasted = TypeUtils.createTypeWithLength(rhsSize);
        rhsFormula = conv.makeCast(rhsType, rhsCasted, rhsFormula, constraints, edge);
        rhsFormula = conv.makeValueReinterpretation(rhsType, rhsCasted, rhsFormula);
      } else {
        rhsFormula = conv.makeCast(rhsType, memberType, rhsFormula, constraints, edge);
        rhsFormula = conv.makeValueReinterpretation(rhsType, memberType, rhsFormula);
      }
      assert rhsFormula == null || rhsFormula instanceof BitvectorFormula;

      // "AndExtractInnerMemberValue"
      if (rhsFormula != null) {
        rhsFormula = fmgr.makeExtract(rhsFormula, endIndex, startIndex);
      }
      Expression newRhsExpression = Value.ofValueOrNondet(rhsFormula);

      // we need innerMember as location for the lvalue of makeDestructiveAssignment:
      final CExpression innerMemberCFieldReference =
          new CFieldReference(
              FileLocation.DUMMY,
              member.getType(),
              innerMember.getName(),
              memberCFieldReference,
              false);
      final Location innerMemberLocation =
          innerMemberCFieldReference.accept(lhsVisitor).asLocation();

      constraints.addConstraint(
          makeDestructiveAssignment(
              innerMember.getType(),
              innerMember.getType(),
              innerMemberLocation,
              newRhsExpression,
              useOldSSAIndices,
              updatedRegions));
    }
  }

  /**
   * Add terms to the {@link #constraints} object that specify that unwritten heap cells keep their
   * value when the SSA index is updated. Only used for the UF encoding.
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

    assert !options.useArraysForHeap();

    checkIsSimplified(lvalueType);
    final int size = conv.getSizeof(lvalueType);

    if (options.useQuantifiersOnArrays()) {
      addRetentionConstraintsWithQuantifiers(
          lvalueType, pattern, startAddress, size, regionsToRetain);
    } else {
      addRetentionConstraintsWithoutQuantifiers(
          region, lvalueType, pattern, startAddress, size, regionsToRetain);
    }
  }

  /**
   * Add retention constraints as specified by {@link #addRetentionForAssignment(MemoryRegion,
   * CType, Formula, PointerTargetPattern, Set)} with the help of quantifiers. Such a constraint is
   * simply {@code forall i : !matches(i) => retention(i)} where {@code matches(i)} specifies
   * whether address {@code i} was written.
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

      final Formula counter =
          fmgr.makeVariableWithoutSSAIndex(conv.voidPointerFormulaType, ufName + "__counter");
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
   * Add retention constraints as specified by {@link #addRetentionForAssignment(MemoryRegion,
   * CType, Formula, PointerTargetPattern, Set)} in a bounded way by manually iterating over all
   * possibly written heap cells and adding a constraint for each of them.
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
        CCompositeTypeMemberDeclaration memberDeclaration =
            ((CCompositeType) lvalueType).getMembers().get(0);
        region = regionMgr.makeMemoryRegion(lvalueType, memberDeclaration);
      }
      // for lvalueType
      addSemiexactRetentionConstraints(pattern, region, startAddress, size, regionsToRetain);

    } else { // Inexact pointer target pattern
      addInexactRetentionConstraints(startAddress, size, regionsToRetain);
    }
  }

  /**
   * Create formula constraints that retain values from the current SSA index to the next one.
   *
   * @param regions The set of regions for which constraints should be created.
   * @param targetLookup A function that gives the PointerTargets for a type for which constraints
   *     should be created.
   * @param constraintConsumer A function that accepts a Formula with the address of the current
   *     target and the respective constraint.
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
   * <p>All heap cells where the pattern does not match retained, and if the pattern is not exact
   * there are also conditional constraints for cells that might be matched by the pattern.
   */
  private void addSimpleTypeRetentionConstraints(
      final PointerTargetPattern pattern,
      final Set<MemoryRegion> regions,
      final Formula startAddress)
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
   * Add retention constraints without quantifiers for the case where the written memory region is
   * known exactly. All heap cells where the pattern does not match retained.
   */
  private void addExactRetentionConstraints(
      final Predicate<PointerTarget> pattern, final Set<MemoryRegion> regions)
      throws InterruptedException {
    makeRetentionConstraints(
        regions,
        region -> pts.getNonMatchingTargets(region, pattern),
        (targetAddress, constraint) -> constraints.addConstraint(constraint));
  }

  /**
   * Add retention constraints without quantifiers for the case where some information is known
   * about the written memory region. For each of the potentially written target candidates we add
   * retention constraints under the condition that it was this target that was actually written.
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
   * Add retention constraints without quantifiers for the case where nothing is known about the
   * written memory region. For every heap cell we add a conditional constraint to retain it.
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
   * Updates the SSA map for memory UFs.
   *
   * @param regions A set of regions that should be added to the SSA map.
   * @param pSsa The current SSA map.
   */
  private void updateSSA(final Set<MemoryRegion> regions, final SSAMapBuilder pSsa) {
    for (final MemoryRegion region : regions) {
      final String ufName = regionMgr.getPointerAccessName(region);
      conv.makeFreshIndex(ufName, region.getType(), pSsa);
    }
  }
}
