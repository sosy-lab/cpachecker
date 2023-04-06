// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceFieldAccessModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceIndexVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceResolved;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceSubscriptModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceCallRhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceExpressionRhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceNondetRhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceRhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceSpanAssignment;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceSpanLhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceSpanResolved;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.ArraySliceSpanRhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentHandler.AssignmentOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

class AssignmentQuantifierHandler {
  private int nextQuantifierVariableNumber = 0;

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

  private final AssignmentFormulaHandler assignmentFormulaHandler;

  /**
   * Creates a new AssignmentQuantifierHandler.
   *
   * @param pConv The C to SMT formula converter.
   * @param pEdge The current edge of the CFA (for logging purposes).
   * @param pFunction The name of the current function.
   * @param pSsa The SSA map.
   * @param pPts The underlying set of pointer targets.
   * @param pConstraints Additional constraints.
   * @param pErrorConditions Additional error conditions.
   */
  AssignmentQuantifierHandler(
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
    assignmentFormulaHandler =
        new AssignmentFormulaHandler(
            pConv, pEdge, pFunction, pSsa, pPts, pConstraints, pErrorConditions, pRegionMgr);
  }

  BooleanFormula handleSimpleSliceAssignments(
      final List<ArraySliceSpanAssignment> assignments, final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {

    // no union handling here now

    boolean canUseQuantifiers = true;

    Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap = LinkedHashMultimap.create();
    for (ArraySliceSpanAssignment assignment : assignments) {
      assignmentMultimap.put(assignment.lhs(), assignment.rhs());
      if (assignment.rhs().actual() instanceof ArraySliceCallRhs) {
        // call rhs precludes using quantifiers, as the call cannot be handled
        // in dynamic memory handler
        // this overrides even forcing quantifiers in assignment options
        canUseQuantifiers = false;
      }
    }

    // hand off the span assignments

    if (canUseQuantifiers
        && (options.useQuantifiersOnArrays() || assignmentOptions.forceQuantifiers())) {
      return handleSimpleSliceAssignmentsWithQuantifiers(assignmentMultimap, assignmentOptions);
    } else {
      return handleSimpleSliceAssignmentsWithoutQuantifiers(assignmentMultimap, assignmentOptions);
    }
  }

  private BooleanFormula handleSimpleSliceAssignmentsWithoutQuantifiers(
      final Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {

    // unroll the variables for every assignment

    BooleanFormula result = null;

    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> entry :
        assignmentMultimap.asMap().entrySet()) {
      ArraySliceSpanLhs lhs = entry.getKey();
      Collection<ArraySliceSpanRhs> rhsCollection = entry.getValue();

      // get all quantifier variables that are used by at least one side
      HashSet<ArraySliceIndexVariable> quantifierVariableSet = new LinkedHashSet<>();
      quantifierVariableSet.addAll(lhs.actual().getUnresolvedIndexVariables());
      for (ArraySliceSpanRhs rhs : rhsCollection) {
        if (rhs.actual() instanceof ArraySliceExpressionRhs expressionRhs) {
          quantifierVariableSet.addAll(expressionRhs.expression().getUnresolvedIndexVariables());
        }
      }
      List<ArraySliceIndexVariable> quantifierVariables = new ArrayList<>(quantifierVariableSet);

      result =
          nullableAnd(
              result,
              unrollSliceAssignment(
                  lhs,
                  rhsCollection,
                  assignmentOptions,
                  quantifierVariables,
                  new HashMap<>(),
                  null));
    }

    return nullToTrue(result);
  }

  private BooleanFormula unrollSliceAssignment(
      ArraySliceSpanLhs lhs,
      Collection<ArraySliceSpanRhs> rhsCollection,
      AssignmentOptions assignmentOptions,
      List<ArraySliceIndexVariable> quantifierVariables,
      Map<ArraySliceIndexVariable, Long> unrolledVariables,
      @Nullable BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    // the recursive unrolling is probably slow, but will serve well for now

    CSimpleType sizeType = conv.machineModel.getPointerEquivalentSimpleType();

    if (quantifierVariables.isEmpty()) {
      // already unrolled, resolve the indices in array slice expressions
      final CExpressionVisitorWithPointerAliasing lhsVisitor = newExpressionVisitor();

      ArraySliceExpression lhsSliceExpression = lhs.actual();
      while (!lhsSliceExpression.isResolved()) {
        ArraySliceIndexVariable firstLhsIndex = lhsSliceExpression.getFirstIndex();
        Long unrolledIndex = unrolledVariables.get(firstLhsIndex);
        // there were sometimes problems with index not found, so check for not null
        checkNotNull(
            unrolledIndex,
            "Could not get value of unrolled index %s for lhs %s",
            firstLhsIndex,
            lhsSliceExpression);
        lhsSliceExpression = lhsSliceExpression.resolveFirstIndex(sizeType, unrolledIndex);
      }
      CExpression lhsBase = lhsSliceExpression.getResolvedExpression();
      Expression lhsExpression = lhsBase.accept(lhsVisitor);
      CType lhsFinalType = typeHandler.getSimplifiedType(lhsBase);

      if (assignmentOptions.forcePointerAssignment()) {
        // if the force pointer assignment option is used, lhs must be an array
        // interpret it as a pointer instead
        lhsFinalType = CTypes.adjustFunctionOrArrayType((lhsFinalType));
      }

      ImmutableList.Builder<ArraySliceSpanResolved> builder = ImmutableList.builder();

      // only resolve each rhs CExpression once
      List<ArraySliceRhs> rhsSlices = new ArrayList<>();

      for (ArraySliceSpanRhs rhs : rhsCollection) {
        rhsSlices.add(rhs.actual());
      }

      Map<ArraySliceRhs, ArraySliceResolved> rhsResolutionMap = new HashMap<>();

      // add initialized and used fields of lhs to pointer-target set as essential
      pts.addEssentialFields(lhsVisitor.getInitializedFields());
      pts.addEssentialFields(lhsVisitor.getUsedFields());

      List<CompositeField> rhsAddressedFields = new ArrayList<>();

      for (ArraySliceRhs rhs : rhsSlices) {
        final CExpressionVisitorWithPointerAliasing rhsVisitor = newExpressionVisitor();

        final @Nullable CRightHandSide rhsBase;
        final ArraySliceResolved rhsResolved;
        if (rhs instanceof ArraySliceNondetRhs nondetRhs) {
          rhsResolved = new ArraySliceResolved(Value.nondetValue(), lhsFinalType);
          rhsBase = null;
        } else if (rhs instanceof ArraySliceCallRhs callRhs) {
          rhsBase = callRhs.call();
          Expression rhsExpression = callRhs.call().accept(rhsVisitor);
          rhsResolved =
              new ArraySliceResolved(rhsExpression, typeHandler.getSimplifiedType(callRhs.call()));
        } else if (rhs instanceof ArraySliceExpressionRhs expressionRhs) {
          // resolve all indices
          ArraySliceExpression rhsSliceExpression = expressionRhs.expression();
          while (!rhsSliceExpression.isResolved()) {
            ArraySliceIndexVariable firstIndex = rhsSliceExpression.getFirstIndex();
            Long unrolledIndex = unrolledVariables.get(firstIndex);
            // there were sometimes problems with index not found, so check for not null
            checkNotNull(
                unrolledIndex,
                "Could not get value of unrolled index %s for lhs %s and rhs %s",
                firstIndex,
                lhs,
                rhsSliceExpression);
            rhsSliceExpression = rhsSliceExpression.resolveFirstIndex(sizeType, unrolledIndex);
          }
          // lhs must be simple, so not an array, therefore, rhs array type must be converted to
          // pointer
          rhsBase = rhsSliceExpression.getResolvedExpression();
          CType rhsType = CTypes.adjustFunctionOrArrayType(typeHandler.getSimplifiedType(rhsBase));
          Expression rhsExpression = rhsBase.accept(rhsVisitor);
          rhsResolved = new ArraySliceResolved(rhsExpression, rhsType);
        } else {
          assert (false);
          rhsBase = null;
          rhsResolved = null;
        }
        // add initialized and used fields of rhs to pointer-target set as essential
        pts.addEssentialFields(rhsVisitor.getInitializedFields());
        pts.addEssentialFields(rhsVisitor.getUsedFields());

        // apply the deferred memory handler: if there is a malloc with void* type, the allocation
        // can
        // be deferred until the assignment that uses the value; the allocation type can then be
        // inferred from assignment lhs type
        if (rhsBase != null
            && rhsResolved != null
            && (conv.options.revealAllocationTypeFromLHS()
                || conv.options.deferUntypedAllocations())) {

          // we have everything we need, call memory handler
          DynamicMemoryHandler memoryHandler =
              new DynamicMemoryHandler(
                  conv, edge, ssa, pts, constraints, errorConditions, regionMgr);
          memoryHandler.handleDeferredAllocationsInAssignment(
              (CLeftHandSide) lhsBase,
              rhsBase,
              rhsResolved.expression(),
              lhsFinalType,
              lhsVisitor.getLearnedPointerTypes(),
              rhsVisitor.getLearnedPointerTypes());
        }

        rhsAddressedFields.addAll(rhsVisitor.getAddressedFields());

        // put into resolution map
        rhsResolutionMap.put(rhs, rhsResolved);
      }

      for (ArraySliceSpanRhs rhs : rhsCollection) {
        ArraySliceResolved rhsResolved = rhsResolutionMap.get(rhs.actual());
        assert (rhsResolved != null);
        builder.add(new ArraySliceSpanResolved(rhs.span(), rhsResolved));
      }

      // compute pointer-target set pattern if necessary for UFs finishing
      // UFs must be finished only if all three of the following conditions are met:
      // 1. UF heap is used
      // 2. lhs is in aliased location (unaliased location is assigned as a whole)
      // 3. using old SSA indices is not selected
      final PointerTargetPattern pattern =
          !options.useArraysForHeap()
                  && lhsExpression.isAliasedLocation()
                  && !assignmentOptions.useOldSSAIndicesIfAliased()
              ? PointerTargetPattern.forLeftHandSide(
                  (CLeftHandSide) lhsBase, typeHandler, edge, pts)
              : null;

      // make the actual assignment
      ArraySliceResolved lhsVisited = new ArraySliceResolved(lhsExpression, lhsFinalType);
      BooleanFormula result =
          assignmentFormulaHandler.makeSliceAssignment(
              lhsVisited,
              lhs.targetType(),
              builder.build(),
              assignmentOptions,
              nullToTrue(condition),
              false,
              pattern);

      // add addressed fields of rhs to pointer-target set
      for (final CompositeField field : rhsAddressedFields) {
        pts.addField(field);
      }

      return result;
    }

    // for better speed, work with the last variable in quantifierVariables
    // remove it from the list now and re-add it after recursion to avoid creating new lists

    ArraySliceIndexVariable unrolledIndex =
        quantifierVariables.remove(quantifierVariables.size() - 1);

    CExpression sliceSize = unrolledIndex.getSize();

    // overapproximate for long arrays
    long consideredArraySize = options.defaultArrayLength();

    if (sliceSize instanceof CIntegerLiteralExpression literalSliceSize) {
      consideredArraySize = ((CIntegerLiteralExpression) sliceSize).getValue().longValueExact();
      if (options.maxArrayLength() >= 0 && consideredArraySize > options.maxArrayLength()) {
        consideredArraySize = options.maxArrayLength();
      }
    }

    // TODO: unify the index handling with quantifier version
    // we will perform the unrolled assignments conditionally, only if the index is smaller than the
    // actual size
    CExpression indexSizeCCast = new CCastExpression(FileLocation.DUMMY, sizeType, sliceSize);

    final CExpressionVisitorWithPointerAliasing indexSizeVisitor = newExpressionVisitor();
    Expression indexSizeExpression = indexSizeCCast.accept(indexSizeVisitor);
    // TODO: add fields to UF from visitor

    Formula sizeFormula = indexSizeVisitor.asValueFormula(indexSizeExpression, sizeType);

    BooleanFormula result = null;

    FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(sizeType);
    Formula zeroFormula = conv.fmgr.makeNumber(sizeFormulaType, 0);
    boolean sizeTypeSigned = sizeType.getCanonicalType().isSigned();

    for (long i = 0; i < consideredArraySize; ++i) {

      Formula indexFormula = conv.fmgr.makeNumber(sizeFormulaType, i);

      // the variable condition holds when 0 <= index < size
      BooleanFormula unrolledCondition =
          bfmgr.and(
              fmgr.makeLessOrEqual(zeroFormula, indexFormula, sizeTypeSigned),
              fmgr.makeLessThan(indexFormula, sizeFormula, sizeTypeSigned));

      // we do not need to remove the index from unrolledVariables after recursion as it will be
      // overwritten before next use anyway
      unrolledVariables.put(unrolledIndex, i);

      // recursive unrolling
      BooleanFormula recursionResult =
          unrollSliceAssignment(
              lhs,
              rhsCollection,
              assignmentOptions,
              quantifierVariables,
              unrolledVariables,
              unrolledCondition);
      result = nullableAnd(result, recursionResult);
    }

    // re-add variable to quantified variable list
    quantifierVariables.add(unrolledIndex);

    return nullToTrue(result);
  }

  private BooleanFormula nullableAnd(@Nullable BooleanFormula a, @Nullable BooleanFormula b) {
    // TODO: this support function should be moved to some manager / utils
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return bfmgr.and(a, b);
  }

  private BooleanFormula nullToTrue(@Nullable BooleanFormula a) {
    // TODO: this support function should be moved to some manager / utils
    if (a == null) {
      return bfmgr.makeTrue();
    }
    return a;
  }

  private ImmutableList<ArraySliceIndexVariable> resolveAllIndexVariables(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap) {
    // remove duplicates, but preserve ordering so quantification is deterministic
    Set<ArraySliceIndexVariable> indexVariableSet = new LinkedHashSet<>();

    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> entry :
        assignmentMultimap.asMap().entrySet()) {
      indexVariableSet.addAll(entry.getKey().actual().getUnresolvedIndexVariables());
      for (ArraySliceSpanRhs rhs : entry.getValue()) {
        // only expression rhs can have unresolved index variables
        if (rhs.actual() instanceof ArraySliceExpressionRhs expressionRhs) {
          indexVariableSet.addAll(expressionRhs.expression().getUnresolvedIndexVariables());
        }
      }
    }
    // convert to list
    return ImmutableList.copyOf(indexVariableSet);
  }

  private BooleanFormula handleSimpleSliceAssignmentsWithQuantifiers(
      final Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {

    // get all index variables
    List<ArraySliceIndexVariable> indexVariables = resolveAllIndexVariables(assignmentMultimap);

    // the quantified variables should be of the size type
    final CSimpleType sizeType = conv.machineModel.getPointerEquivalentSimpleType();
    FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(sizeType);
    Formula zeroFormula = conv.fmgr.makeNumber(sizeFormulaType, 0);
    boolean sizeTypeSigned = sizeType.getCanonicalType().isSigned();

    // instantiate all index variables and create the condition for whether an element will be
    // assignment, depending on whether all quantified variable conditions hold for it

    BooleanFormula conditionFormula = null;

    // as we will be creating the quantifiers from this, use a LinkedHashMap to retain
    // predictable quantifier ordering
    Map<ArraySliceIndexVariable, Formula> quantifiedVariableFormulaMap = new LinkedHashMap<>();

    for (ArraySliceIndexVariable indexVariable : indexVariables) {

      // TODO: better naming of quantifiedVariable
      final Formula quantifiedVariableFormula =
          fmgr.makeVariableWithoutSSAIndex(
              sizeFormulaType, "__quantifier_" + nextQuantifierVariableNumber++);
      quantifiedVariableFormulaMap.put(indexVariable, quantifiedVariableFormula);

      // cast the index size expression to the size type to make sure there are no suprises
      // comparing
      CCastExpression indexSizeCCast =
          new CCastExpression(FileLocation.DUMMY, sizeType, indexVariable.getSize());

      final CExpressionVisitorWithPointerAliasing indexSizeVisitor = newExpressionVisitor();
      Expression indexSizeExpression = indexSizeCCast.accept(indexSizeVisitor);
      // TODO: add fields to UF from visitor

      Formula sizeFormula = indexSizeVisitor.asValueFormula(indexSizeExpression, sizeType);

      // the quantified variable condition holds when 0 <= index < size
      BooleanFormula quantifiedVariableCondition =
          bfmgr.and(
              fmgr.makeLessOrEqual(zeroFormula, quantifiedVariableFormula, sizeTypeSigned),
              fmgr.makeLessThan(quantifiedVariableFormula, sizeFormula, sizeTypeSigned));

      conditionFormula = nullableAnd(conditionFormula, quantifiedVariableCondition);
    }

    conditionFormula = nullToTrue(conditionFormula);

    // construct the result as a conjunction of assignments
    // make sure that there is no unnecessary tautology polluting the formula
    BooleanFormula assignmentSystem = null;

    // now that we have everything quantified, we can perform the assignments
    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> entry :
        assignmentMultimap.asMap().entrySet()) {

      // TODO: add fields handling for UF
      CExpressionVisitorWithPointerAliasing visitor =
          new CExpressionVisitorWithPointerAliasing(
              conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
      ArraySliceResolved lhsResolved =
          resolveSliceExpression(
              entry.getKey().actual(), Optional.empty(), quantifiedVariableFormulaMap, visitor);
      if (lhsResolved == null) {
        // TODO: only used for ignoring assignments to bit-fields which should be handled properly
        // do not perform this assignment, but others can be done
        continue;
      }

      ImmutableList.Builder<ArraySliceSpanResolved> builder = ImmutableList.builder();
      for (ArraySliceSpanRhs rhs : entry.getValue()) {
        ArraySliceResolved rhsResolved =
            resolveRhs(rhs.actual(), lhsResolved.type(), quantifiedVariableFormulaMap, visitor);

        if (rhsResolved == null) {
          // TODO: only used for ignoring assignments to bit-fields which should be handled properly
          // do not perform this assignment, but others can be done
          continue;
        }

        builder.add(new ArraySliceSpanResolved(rhs.span(), rhsResolved));
      }

      // TODO: add updatedRegions handling for UF

      // if there are no quantifiers, do not force array heap to use the quantified assignment
      // force UF heap to use the quantified assignment version, as it would not retain other
      // assignments otherwise
      // we do not want to use the technique of finishing assignments if we can use quantifiers
      // as quantified retainment is the most precise
      boolean isReallyQuantified =
          !options.useArraysForHeap() || !quantifiedVariableFormulaMap.isEmpty();

      // after cast/reinterpretation, lhs and rhs have the lhs type
      // do not provide a pointer-target set pattern as we do not want to finish assignments
      BooleanFormula assignmentResult =
          assignmentFormulaHandler.makeSliceAssignment(
              lhsResolved,
              entry.getKey().targetType(),
              builder.build(),
              assignmentOptions,
              conditionFormula,
              isReallyQuantified,
              null);
      assignmentSystem = nullableAnd(assignmentSystem, assignmentResult);
    }

    // add quantifiers around the assignment system
    BooleanFormula quantifiedAssignmentSystem = nullToTrue(assignmentSystem);

    for (Formula quantifiedVariableFormula : quantifiedVariableFormulaMap.values()) {
      quantifiedAssignmentSystem =
          fmgr.getQuantifiedFormulaManager()
              .forall(quantifiedVariableFormula, quantifiedAssignmentSystem);
    }

    // we are done
    return quantifiedAssignmentSystem;
  }

  private @Nullable ArraySliceResolved resolveRhs(
      final ArraySliceRhs rhs,
      final CType lhsType,
      final Map<ArraySliceIndexVariable, Formula> quantifiedVariableFormulaMap,
      CExpressionVisitorWithPointerAliasing visitor)
      throws UnrecognizedCodeException {

    if (rhs instanceof ArraySliceNondetRhs nondetRhs) {
      return new ArraySliceResolved(Value.nondetValue(), lhsType);
    }
    if (rhs instanceof ArraySliceCallRhs callRhs) {
      Expression rhsExpression = callRhs.call().accept(visitor);
      return new ArraySliceResolved(rhsExpression, typeHandler.getSimplifiedType(callRhs.call()));
    }

    return resolveSliceExpression(
        ((ArraySliceExpressionRhs) rhs).expression(),
        Optional.empty(),
        quantifiedVariableFormulaMap,
        visitor);
  }

  private @Nullable ArraySliceResolved resolveSliceExpression(
      final ArraySliceExpression sliceExpression,
      final Optional<CType> finalType,
      final Map<ArraySliceIndexVariable, Formula> quantifiedVariableFormulaMap,
      CExpressionVisitorWithPointerAliasing visitor)
      throws UnrecognizedCodeException {

    // TODO: handle UF field marking properly in this method

    CExpression baseCExpression = sliceExpression.getBaseExpression();
    CType baseType = typeHandler.getSimplifiedType(baseCExpression);

    // convert the base from C expression to SMT expression
    Expression baseExpression = baseCExpression.accept(visitor);

    ArraySliceResolved base = new ArraySliceResolved(baseExpression, baseType);

    // we have unresolved modifiers, that means there is some quantified array access
    // so the base must be an array and therefore represent an AliasedLocation

    for (ArraySliceModifier modifier : sliceExpression.getModifiers()) {
      if (modifier instanceof ArraySliceSubscriptModifier subscriptModifier) {
        base = resolveSubscriptModifier(base, subscriptModifier, quantifiedVariableFormulaMap);
      } else {
        base = resolveFieldAccessModifier(base, (ArraySliceFieldAccessModifier) modifier);
      }
      if (base == null) {
        // TODO: only used for ignoring assignments to bit-fields which should be handled properly
        // TODO: also used for ignoring non-aliased locations which should be handled properly
        return null;
      }
    }

    if (finalType.isPresent()) {
      // retype to final type
      base = new ArraySliceResolved(base.expression(), finalType.get());
    }

    return base;
  }

  private ArraySliceResolved resolveSubscriptModifier(
      ArraySliceResolved base,
      ArraySliceSubscriptModifier modifier,
      final Map<ArraySliceIndexVariable, Formula> quantifiedVariableFormulaMap) {

    // find the quantified variable formula, the caller is responsible for ensuring that it is in
    // the map

    Formula quantifiedVariableFormula = quantifiedVariableFormulaMap.get(modifier.index());
    checkNotNull(quantifiedVariableFormula);

    // get the array element type
    CPointerType basePointerType = (CPointerType) CTypes.adjustFunctionOrArrayType(base.type());
    final CType elementType = typeHandler.simplifyType(basePointerType.getType());

    // perform pointer arithmetic, we have array[base] and want array[base + i]
    // the quantified variable i must be multiplied by the sizeof the element type

    if (!base.expression().isAliasedLocation()) {
      // TODO: resolve for nonaliased location
      conv.logger.logfOnce(
          Level.WARNING,
          "%s: Ignoring resolution of subscript modifier for non-aliased expression %s with type %s",
          edge.getFileLocation(),
          base.expression(),
          base.type());
      return null;
    }

    Formula baseAddress = base.expression().asAliasedLocation().getAddress();
    final Formula sizeofElement =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.getSizeof(elementType));

    final Formula adjustedAddress =
        conv.fmgr.makePlus(
            baseAddress, conv.fmgr.makeMultiply(quantifiedVariableFormula, sizeofElement));

    // return the resolved formula with adjusted address and array element type
    return new ArraySliceResolved(AliasedLocation.ofAddress(adjustedAddress), elementType);
  }

  private @Nullable ArraySliceResolved resolveFieldAccessModifier(
      ArraySliceResolved base, ArraySliceFieldAccessModifier modifier) {

    // the base type must be a composite type to have fields
    CCompositeType baseType = (CCompositeType) base.type();
    final String fieldName = modifier.field().getName();
    CType fieldType = typeHandler.getSimplifiedType(modifier.field());

    if (!base.expression().isAliasedLocation()) {
      // TODO: resolve for nonaliased location
      conv.logger.logfOnce(
          Level.WARNING,
          "%s: Ignoring resolution of subscript modifier for non-aliased expression %s with type %s",
          edge.getFileLocation(),
          base.expression(),
          base.type());
      return null;
    }

    // we will increase the base address by field offset

    Formula baseAddress = base.expression().asAliasedLocation().getAddress();

    final OptionalLong offset = typeHandler.getOffset(baseType, fieldName);
    if (!offset.isPresent()) {
      // TODO This loses values of bit fields.
      return null;
    }

    final Formula offsetFormula =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, offset.orElseThrow());
    final Formula adjustedAdress = conv.fmgr.makePlus(baseAddress, offsetFormula);

    // TODO: add equal base address constraint

    // for field access, it is necessary to create a memory region for field access
    final MemoryRegion region = regionMgr.makeMemoryRegion(baseType, modifier.field());
    AliasedLocation resultLocation = AliasedLocation.ofAddressWithRegion(adjustedAdress, region);

    // return the resolved formula with adjusted address and field type
    return new ArraySliceResolved(resultLocation, fieldType);
  }

  CExpressionVisitorWithPointerAliasing newExpressionVisitor() {
    return new CExpressionVisitorWithPointerAliasing(
        conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
  }

  @Deprecated
  BooleanFormula makeDestructiveAssignment(
      CType lvalueType,
      CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions,
      final @Nullable BooleanFormula condition,
      boolean useQuantifiers)
      throws UnrecognizedCodeException {
    // TODO: remove this function
    return assignmentFormulaHandler.makeDestructiveAssignment(
        lvalueType,
        rvalueType,
        lvalue,
        rvalue,
        useOldSSAIndices,
        updatedRegions,
        condition,
        useQuantifiers);
  }

}
