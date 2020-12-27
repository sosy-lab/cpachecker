// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2019  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.implicitCastToPointer;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
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
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/** Implements a handler for assignments. */
@SuppressWarnings("unused") // TODO fix unused variables
class AssignmentHandlerBackwards extends AssignmentHandler {

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
  AssignmentHandlerBackwards(
      CToFormulaConverterWithPointerAliasing pConv,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions,
      MemoryRegionManager pRegionMgr) {
    super(pConv, pEdge, pFunction, pSsa, pPts, pConstraints, pErrorConditions, pRegionMgr);
  }

  /**
   * Creates a formula to handle assignments.
   *
   * @param lhs The left hand side of an assignment.
   * @param lhsForChecking The left hand side of an assignment to check.
   * @param rhs Either {@code null} or the right hand side of the assignment.
   * @param useOldSSAIndicesIfAliased A flag indicating whether we can use old SSA indices for
   *        aliased locations (because the location was not used before)
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  @Override
  public BooleanFormula handleAssignment(
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
    // final boolean useOldSSAIndices = useOldSSAIndicesIfAliased && lhsLocation.isAliased();
    final boolean useOldSSAIndices = useOldSSAIndicesIfAliased && lhsLocation.isAliased();

    final Map<String, CType> lhsLearnedPointerTypes = lhsVisitor.getLearnedPointerTypes();
    pts.addEssentialFields(lhsVisitor.getInitializedFields());
    pts.addEssentialFields(lhsVisitor.getUsedFields());
    // the pattern matching possibly aliased locations

    // making new variable
    final String targetName;
    final int oldIndex;
    final Formula lhsFormula;
    // final FormulaType<?> targetType = conv.getFormulaTypeFromCType(lhsType);
    final CType rhsType =
        rhs != null ? typeHandler.getSimplifiedType(rhs) : CNumericTypes.SIGNED_CHAR;
    // necessary only for update terms for new UF indices
    Set<MemoryRegion> updatedRegions =
        useOldSSAIndices || options.useArraysForHeap() ? null : new HashSet<>();


    if (!lhsLocation.isAliased()) {

      assert !useOldSSAIndices;
      targetName = lhsLocation.asUnaliased().getVariableName();
      // oldIndex = conv.getIndex(targetName, lhsType, ssa);
      lhsFormula = conv.makeFreshVariable(targetName, lhsType, ssa);
      // final Formula lhsFormula = fmgr.makeVariable(targetType, targetName, oldIndex);
      int newIndex = conv.getFreshIndex(targetName, lhsType, ssa);

    } else {

      lhsFormula = null;

      MemoryRegion region = lhsLocation.asAliased().getMemoryRegion();
      if (region == null) {
        region = regionMgr.makeMemoryRegion(lhsType);
      }
      targetName = regionMgr.getPointerAccessName(region);
      oldIndex = conv.getIndex(targetName, lhsType, ssa);
      final int newIndex;
      if (useOldSSAIndices) {
        assert updatedRegions == null : "Returning updated regions is only for new indices";
        newIndex = oldIndex;

      } else if (options.useArraysForHeap()) {
        assert updatedRegions == null : "Return updated regions is only for UF encoding";
        newIndex = conv.makeFreshIndex(targetName, lhsType, ssa);

      } else {
        assert updatedRegions != null : "UF encoding needs to update regions for new indices";
        newIndex = conv.getFreshIndex(targetName, lhsType, ssa);
        updatedRegions.add(region);
        // For UFs, we use a new index without storing it such that we use the same index
        // for multiple writes that are part of the same assignment.
        // The new index will be stored in the SSAMap later.

      }

    }

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



    if (conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) {
      DynamicMemoryHandler memoryHandler =
          new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);
      memoryHandler.handleDeferredAllocationsInAssignment(
          lhs,
          rhs,
          rhsExpression,
          lhsType,
          lhsLearnedPointerTypes,
          rhsLearnedPointersTypes);
    }

    if (lhsLocation.isUnaliasedLocation() && lhs instanceof CFieldReference) {
      CFieldReference fieldReference = (CFieldReference) lhs;
      CExpression fieldOwner = fieldReference.getFieldOwner();
      CType ownerType = typeHandler.getSimplifiedType(fieldOwner);
      if (!fieldReference.isPointerDereference() && ownerType instanceof CCompositeType) {
        if (((CCompositeType) ownerType).getKind() == ComplexTypeKind.UNION) {
          addAssignmentsForOtherFieldsOfUnion(
              lhsType,
              (CCompositeType) ownerType,
              lhsFormula,
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
                lhsFormula,
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

    final BooleanFormula result =
        makeDestructiveAssignment(
            lhsType,
            rhsType,
            lhsLocation,
            lhsFormula,
            rhsExpression,
            useOldSSAIndices,
            updatedRegions);

    return result;
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
      final Formula lhsFormula,
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
          (CArrayType) lvalueType,
          rvalueType,
          lvalue,
          lhsFormula,
          rvalue,
          useOldSSAIndices,
          updatedRegions);

    } else if (lvalueType instanceof CCompositeType) {
      final CCompositeType lvalueCompositeType = (CCompositeType) lvalueType;
      return makeDestructiveCompositeAssignment(
          lvalueCompositeType,
          rvalueType,
          lvalue,
          lhsFormula,
          rvalue,
          useOldSSAIndices,
          updatedRegions);

    } else { // Simple assignment
      return makeSimpleDestructiveAssignment(
          lvalueType,
          rvalueType,
          lvalue,
          lhsFormula,
          rvalue,
          useOldSSAIndices,
          updatedRegions);
    }
  }

  private BooleanFormula makeDestructiveArrayAssignment(
      CArrayType lvalueArrayType,
      CType rvalueType,
      final Location lvalue,
      final Formula lhsFormula,
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
          "Impossible array assignment due to incompatible types: assignment of %s with type %s to %s with type %s",
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
            AliasedLocation
                .ofAddress(fmgr.makePlus(rvalue.asAliasedLocation().getAddress(), offsetFormula));
      }

      result =
          bfmgr.and(
              result,
              makeDestructiveAssignment(
                  lvalueElementType,
                  newRvalueType,
                  newLvalue,
                  lhsFormula,
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
      final Formula lhsFormula,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions)
      throws UnrecognizedCodeException {
    // There are two cases of assignment to a structure/union
    // - Initialization with a value (possibly nondet), useful for stack declarations and memset
    // - Structure assignment
    checkArgument(
        (rvalue.isValue() && isSimpleType(rvalueType)) || rvalueType.equals(lvalueCompositeType),
        "Impossible assignment due to incompatible types: assignment of %s with type %s to %s with type %s",
        rvalue,
        rvalueType,
        lvalue,
        lvalueCompositeType);

    BooleanFormula result = bfmgr.makeTrue();
    for (final CCompositeTypeMemberDeclaration memberDeclaration : lvalueCompositeType
        .getMembers()) {
      final CType newLvalueType = typeHandler.getSimplifiedType(memberDeclaration);
      // Optimizing away the assignments from uninitialized fields
      if (conv.isRelevantField(lvalueCompositeType, memberDeclaration) && (
      // Assignment to a variable, no profit in optimizing it
      !lvalue.isAliased() || // That's not a simple assignment, check the nested composite
          !isSimpleType(newLvalueType) || // This is initialization, so the assignment is mandatory
          rvalue.isValue() || // The field is tracked as essential
          pts.tracksField(CompositeField.of(lvalueCompositeType, memberDeclaration)) || // The
                                                                                        // variable
                                                                                        // representing
                                                                                        // the RHS
                                                                                        // was used
                                                                                        // somewhere
                                                                                        // (i.e. has
                                                                                        // SSA
                                                                                        // index)
          (!rvalue.isAliasedLocation()
              && conv.hasIndex(
                  getFieldAccessName(
                      rvalue.asUnaliasedLocation().getVariableName(),
                      memberDeclaration),
                  newLvalueType,
                  ssa)))) {

        final OptionalLong offset = typeHandler.getOffset(lvalueCompositeType, memberDeclaration);
        if (!offset.isPresent()) {
          continue; // TODO this looses values of bit fields
        }
        final Formula offsetFormula =
            fmgr.makeNumber(conv.voidPointerFormulaType, offset.orElseThrow());

        // Handle new LHS
        final Location newLvalue;
        if (lvalue.isAliased()) {
          final MemoryRegion region =
              regionMgr.makeMemoryRegion(lvalueCompositeType, memberDeclaration);
          newLvalue =
              AliasedLocation.ofAddressWithRegion(
                  fmgr.makePlus(lvalue.asAliased().getAddress(), offsetFormula),
                  region);

        } else {
          newLvalue =
              UnaliasedLocation.ofVariableName(
                  getFieldAccessName(lvalue.asUnaliased().getVariableName(), memberDeclaration));
        }

        // making new variable
        final String targetName;
        final int oldIndex;
        final FormulaType<?> targetType = conv.getFormulaTypeFromCType(newLvalueType);
        final Formula newlhsFormula;
        if (!newLvalue.isAliased()) {
          assert !useOldSSAIndices;

          targetName = newLvalue.asUnaliased().getVariableName();
          oldIndex = conv.getIndex(targetName, newLvalueType, ssa);
          newlhsFormula = conv.makeFreshVariable(targetName, newLvalueType, ssa);
          // final Formula lhsFormula = fmgr.makeVariable(targetType, targetName, oldIndex);
          int newIndex = conv.getFreshIndex(targetName, newLvalueType, ssa);
        } else {

          newlhsFormula = null;

          MemoryRegion region = newLvalue.asAliased().getMemoryRegion();
          if (region == null) {
            // should never happen as memory region is already made above
            region = regionMgr.makeMemoryRegion(newLvalueType);
          }
          targetName = regionMgr.getPointerAccessName(region);
          oldIndex = conv.getIndex(targetName, newLvalueType, ssa);
          final int newIndex;
          if (useOldSSAIndices) {
            assert updatedRegions == null : "Returning updated regions is only for new indices";
            newIndex = oldIndex;

          } else if (options.useArraysForHeap()) {
            assert updatedRegions == null : "Return updated regions is only for UF encoding";
            newIndex = conv.makeFreshIndex(targetName, newLvalueType, ssa);

          } else {
            assert updatedRegions != null : "UF encoding needs to update regions for new indices";
            newIndex = conv.getFreshIndex(targetName, newLvalueType, ssa);
            updatedRegions.add(region);
            // For UFs, we use a new index without storing it such that we use the same index
            // for multiple writes that are part of the same assignment.
            // The new index will be stored in the SSAMap later.

          }
        }

        // handle new RHS
        final CType newRvalueType;
        final Expression newRvalue;
        if (rvalue.isLocation()) {
          newRvalueType = newLvalueType;
          if (rvalue.isAliasedLocation()) {
            final MemoryRegion region = regionMgr.makeMemoryRegion(rvalueType, memberDeclaration);
            newRvalue =
                AliasedLocation.ofAddressWithRegion(
                    fmgr.makePlus(rvalue.asAliasedLocation().getAddress(), offsetFormula),
                    region);
          } else {
            newRvalue =
                UnaliasedLocation.ofVariableName(
                    getFieldAccessName(
                        rvalue.asUnaliasedLocation().getVariableName(),
                        memberDeclaration));
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
                    newlhsFormula,
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
      final Formula lhsFormula,
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
      // final int oldIndex = conv.getIndex(targetName, lvalueType, ssa);

      if (rhs != null) {
        result = fmgr.assignment(lhsFormula, rhs);
      } else {
        result = bfmgr.makeTrue();
      }

    } else { // Aliased LHS
      MemoryRegion region = lvalue.asAliased().getMemoryRegion();
      if (region == null) {
        region = regionMgr.makeMemoryRegion(lvalueType);
      }
      final String targetName = regionMgr.getPointerAccessName(region);
      int oldIndex = conv.getIndex(targetName, lvalueType, ssa);

      if (useOldSSAIndices) {
        assert updatedRegions == null : "Returning updated regions is only for new indices";

      } else if (options.useArraysForHeap()) {
        assert updatedRegions == null : "Return updated regions is only for UF encoding";
        if (rhs == null) {
          // For arrays, we always need to add a term that connects oldIndex with newIndex
          String nondetName =
              "__nondet_value_" + CTypeUtils.typeToString(rvalueType).replace(' ', '_');
          rhs = conv.makeNondet(nondetName, rvalueType, ssa, constraints);
          rhs = conv.makeCast(rvalueType, lvalueType, rhs, constraints, edge);
        }
        // conv.makeFreshIndex(targetName, lvalueType, ssa);

      } else {
        assert updatedRegions != null : "UF encoding needs to update regions for new indices";
        updatedRegions.add(region);
        // For UFs, we use a new index without storing it such that we use the same index
        // for multiple writes that are part of the same assignment.
        // The new index will be stored in the SSAMap later.
        // conv.getFreshIndex(targetName, lvalueType, ssa);
      }

      final int newIndex;
      if (useOldSSAIndices) {
        // in backwards analysis the lhs has the lower index than the rhs
        newIndex = oldIndex;
      } else {
        newIndex = oldIndex + 1;
      }

      if (rhs != null) {
        final Formula address = lvalue.asAliased().getAddress();
        result =
            conv.ptsMgr.makePointerAssignment(
                targetName,
                targetType,
                newIndex - 1,
                oldIndex - 1,
                address,
                rhs);
      } else {
        result = bfmgr.makeTrue();
      }

    }

    return result;
  }

  private void addAssignmentsForOtherFieldsOfUnion(
      final CType lhsType,
      final CCompositeType ownerType,
      final Formula lhsFormula,
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

      // Here create new lhsFormula and make new variable?
      final String targetName = newLhsLocation.asUnaliased().getVariableName();
      final int oldIndex = conv.getIndex(targetName, newLhsType, ssa);
      // final FormulaType<?> targetType = conv.getFormulaTypeFromCType(newLhsType);
      final Formula newLhsFormula = conv.makeFreshVariable(targetName, newLhsType, ssa);
      int newIndex = conv.getFreshIndex(targetName, newLhsType, ssa);

      if (CTypeUtils.isSimpleType(newLhsType)) {
        final Expression newRhsExpression;
        final CType newRhsType = newLhsType;
        if (CTypeUtils.isSimpleType(rhsType) && !rhsExpression.isNondetValue()) {
          Formula rhsFormula = getValueFormula(rhsType, rhsExpression).orElseThrow();
          rhsFormula = conv.makeCast(rhsType, lhsType, rhsFormula, constraints, edge);
          rhsFormula = conv.makeValueReinterpretation(lhsType, newLhsType, rhsFormula);
          newRhsExpression = rhsFormula == null ? Value.nondetValue() : Value.ofValue(rhsFormula);
        } else if (rhsType instanceof CCompositeType) {
          // reinterpret compositetype as bitvector; concatenate its fields appropriately in case of
          // struct
          if (((CCompositeType) rhsType).getKind() == ComplexTypeKind.STRUCT) {
            CExpressionVisitorWithPointerAliasing expVisitor = newExpressionVisitor();
            int offset = 0;
            int targetSize = typeHandler.getBitSizeof(newLhsType);
            Formula rhsFormula = null;

            for (CCompositeTypeMemberDeclaration innerMember : ((CCompositeType) rhsType)
                .getMembers()) {
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
                          innerMemberFieldReference,
                          innerMember.getType(),
                          expVisitor)).orElseThrow();
              if (!(memberFormula instanceof BitvectorFormula)) {
                CType interType = TypeUtils.createTypeWithLength(innerMemberSize);
                memberFormula =
                    conv.makeCast(
                        innerMember.getType(),
                        interType,
                        memberFormula,
                        constraints,
                        edge);
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
                memberFormula =
                    fmgr.makeExtend(memberFormula, targetSize - innerMemberSize, lhsSigned);
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
            newRhsExpression = rhsFormula == null ? Value.nondetValue() : Value.ofValue(rhsFormula);

            // make assignment to lhs
          } else {
            throw new UnsupportedCodeException(
                "Assignment of complex Unions via nested Struct-Members not supported",
                edge);
          }
        } else {
          newRhsExpression = Value.nondetValue();
        }
        constraints.addConstraint(
            makeDestructiveAssignment(
                newLhsType,
                newRhsType,
                newLhsLocation,
                newLhsFormula,
                newRhsExpression,
                useOldSSAIndices,
                updatedRegions));
      }

      if (newLhsType instanceof CCompositeType
          && CTypeUtils.isSimpleType(rhsType)
          && !rhsExpression.isNondetValue()) {
        // Use different name in this block as newLhsType is confusing. newLhsType was computed as
        // member.getType() -> call it memberType here (note we will also have an innerMember)
        final CType memberType = newLhsType;
        // newLhs is a CFieldReference to member:
        final CExpression memberCFieldReference = newLhs;
        final int rhsSize = typeHandler.getBitSizeof(rhsType);

        // for each innerMember of member we need to add a (destructive!) constraint like:
        // union.member.innerMember := treatAsMemberTypeAndExtractInnerMemberValue(rhsExpression);
        for (CCompositeTypeMemberDeclaration innerMember : ((CCompositeType) memberType)
            .getMembers()) {

          int fieldOffset =
              (int) typeHandler.getBitOffset(((CCompositeType) memberType), innerMember);
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
            // cast before was to memberType - but does this make sence?
            rhsFormula =
                conv.makeCast(rhsType, innerMember.getType(), rhsFormula, constraints, edge);
            rhsFormula = conv.makeValueReinterpretation(rhsType, innerMember.getType(), rhsFormula);
          }
          assert rhsFormula == null || rhsFormula instanceof BitvectorFormula;

          boolean rhsSigned = false;
          if (!(rhsType instanceof CPointerType)) {
            rhsSigned = ((CSimpleType) rhsType).isSigned();
          }
          // "AndExtractInnerMemberValue"
          if (rhsFormula != null) {
            rhsFormula = fmgr.makeExtract(rhsFormula, endIndex, startIndex, rhsSigned);
          }
          Expression newRhsExpression =
              rhsFormula == null ? Value.nondetValue() : Value.ofValue(rhsFormula);

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

          // hier nochmal neue lhsFormula?
          // Here create new lhsFormula and make new variable?
          final String innerTargetName = innerMemberLocation.asUnaliased().getVariableName();
          final int innerOldIndex = conv.getIndex(innerTargetName, innerMember.getType(), ssa);
          // final FormulaType<?> targetType = conv.getFormulaTypeFromCType(newLhsType);
          final Formula innerLhsFormula =
              conv.makeFreshVariable(innerTargetName, innerMember.getType(), ssa);
          int innerNewIndex = conv.getFreshIndex(innerTargetName, innerMember.getType(), ssa);

          constraints.addConstraint(
              makeDestructiveAssignment(
                  innerMember.getType(),
                  innerMember.getType(),
                  innerMemberLocation,
                  innerLhsFormula,
                  newRhsExpression,
                  useOldSSAIndices,
                  updatedRegions));
        }
      }
    }
  }
}
