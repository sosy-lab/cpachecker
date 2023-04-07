// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
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

  private final CSimpleType sizeType;

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

    sizeType = conv.machineModel.getPointerEquivalentSimpleType();
  }

  BooleanFormula handleSimpleSliceAssignments(
      final Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {

    LinkedHashSet<ArraySliceIndexVariable> variablesToQuantify = new LinkedHashSet<>();

    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> entry :
        assignmentMultimap.asMap().entrySet()) {
      variablesToQuantify.addAll(entry.getKey().actual().getUnresolvedIndexVariables());
      for (ArraySliceSpanRhs rhs : entry.getValue()) {
        if (rhs.actual() instanceof ArraySliceExpressionRhs expressionRhs) {
          variablesToQuantify.addAll(expressionRhs.expression().getUnresolvedIndexVariables());
        }
      }
    }

    return quantifySliceAssignment(
        assignmentMultimap,
        assignmentOptions,
        variablesToQuantify,
        ImmutableMap.of(),
        ImmutableMap.of(),
        bfmgr.makeTrue());
  }

  private boolean shouldUnroll(AssignmentOptions assignmentOptions) {
    return !options.useQuantifiersOnArrays() && !assignmentOptions.forceQuantifiers();
  }

  private BooleanFormula quantifySliceAssignment(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      AssignmentOptions assignmentOptions,
      LinkedHashSet<ArraySliceIndexVariable> variablesToQuantify,
      Map<ArraySliceIndexVariable, Long> unrolledVariables,
      Map<ArraySliceIndexVariable, Formula> encodedVariables,
      BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    // recursive quantification
    if (variablesToQuantify.isEmpty()) {
      // all variables are quantified
      // apply unrolled variables if necessary
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> unrolledAssignmentMultimap =
          applyUnrolledVariables(assignmentMultimap, assignmentOptions, unrolledVariables);
      // perform quantified assignment
      return performQuantifiedAssignment(
          unrolledAssignmentMultimap,
          assignmentOptions,
          unrolledVariables,
          encodedVariables,
          condition);
    }

    // get the variable to quantify
    ArraySliceIndexVariable variableToQuantify = variablesToQuantify.iterator().next();

    LinkedHashSet<ArraySliceIndexVariable> nextVariablesToQuantify =
        new LinkedHashSet<>(variablesToQuantify);
    nextVariablesToQuantify.remove(variableToQuantify);

    CExpression sliceSize = variableToQuantify.getSize();

    // we will perform the unrolled assignments conditionally, only if the index is smaller than the
    // actual size
    CExpression sliceSizeCastToSizeType =
        new CCastExpression(FileLocation.DUMMY, sizeType, sliceSize);

    final CExpressionVisitorWithPointerAliasing indexSizeVisitor =
        new CExpressionVisitorWithPointerAliasing(
            conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
    Expression sliceSizeExpression = sliceSizeCastToSizeType.accept(indexSizeVisitor);
    // TODO: add fields to UF from visitor

    Formula sliceSizeFormula = indexSizeVisitor.asValueFormula(sliceSizeExpression, sizeType);

    // decide whether to encode or unroll the quantifier
    if (shouldUnroll(assignmentOptions)) {
      return unrollQuantifier(
          assignmentMultimap,
          assignmentOptions,
          nextVariablesToQuantify,
          unrolledVariables,
          encodedVariables,
          condition,
          variableToQuantify,
          sliceSizeFormula);
    } else {
      return encodeQuantifier(
          assignmentMultimap,
          assignmentOptions,
          nextVariablesToQuantify,
          unrolledVariables,
          encodedVariables,
          condition,
          variableToQuantify,
          sliceSizeFormula);
    }
  }

  private BooleanFormula unrollQuantifier(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      AssignmentOptions assignmentOptions,
      LinkedHashSet<ArraySliceIndexVariable> nextVariablesToQuantify,
      Map<ArraySliceIndexVariable, Long> unrolledVariables,
      Map<ArraySliceIndexVariable, Formula> encodedVariables,
      BooleanFormula condition,
      ArraySliceIndexVariable variableToUnroll,
      Formula sliceSizeFormula)
      throws UnrecognizedCodeException, InterruptedException {

    CExpression sliceSize = variableToUnroll.getSize();


    FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(sizeType);
    Formula zeroFormula = conv.fmgr.makeNumber(sizeFormulaType, 0);
    boolean sizeTypeSigned = sizeType.getCanonicalType().isSigned();

    // overapproximate for long arrays
    long consideredArraySize = options.defaultArrayLength();

    if (sliceSize instanceof CIntegerLiteralExpression literalSliceSize) {
      consideredArraySize = ((CIntegerLiteralExpression) sliceSize).getValue().longValueExact();
      if (options.maxArrayLength() >= 0 && consideredArraySize > options.maxArrayLength()) {
        consideredArraySize = options.maxArrayLength();
      }
    }

    BooleanFormula result = bfmgr.makeTrue();

    for (long i = 0; i < consideredArraySize; ++i) {

      Formula indexFormula = conv.fmgr.makeNumber(sizeFormulaType, i);

      // the variable condition holds when 0 <= index < size
      BooleanFormula nextCondition =
          bfmgr.and(
              condition,
              fmgr.makeLessOrEqual(zeroFormula, indexFormula, sizeTypeSigned),
              fmgr.makeLessThan(indexFormula, sliceSizeFormula, sizeTypeSigned));

      // make a new map with added newly unrolled variable
      Map<ArraySliceIndexVariable, Long> nextUnrolledVariables = new HashMap<>(unrolledVariables);
      nextUnrolledVariables.put(variableToUnroll, i);

      // quantify recursively
      BooleanFormula recursionResult =
          quantifySliceAssignment(
              assignmentMultimap,
              assignmentOptions,
              nextVariablesToQuantify,
              nextUnrolledVariables,
              encodedVariables,
              nextCondition);
      result = bfmgr.and(result, recursionResult);
    }

    return result;
  }

  private ArraySliceExpression applyUnrolledVariables(
      ArraySliceExpression expression,
      Map<ArraySliceIndexVariable, Long> unrolledVariables) {

    while (!expression.isResolved()) {
      ArraySliceIndexVariable firstIndex = expression.getFirstIndex();
      Long unrolledIndex = unrolledVariables.get(firstIndex);
      // there were sometimes problems with index not found, so check for not null
      checkNotNull(
          unrolledIndex,
          "Could not get value of unrolled index %s for expression %s",
          firstIndex,
          expression);
      expression = expression.resolveFirstIndex(sizeType, unrolledIndex);
    }
    return expression;
  }

  private Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> applyUnrolledVariables(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      AssignmentOptions assignmentOptions,
      Map<ArraySliceIndexVariable, Long> unrolledVariables) {
    if (!shouldUnroll(assignmentOptions)) {
      return assignmentMultimap;
    }

    Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> unrolledAssignmentMultimap =
        LinkedHashMultimap.create();

    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> assignment :
        assignmentMultimap.asMap().entrySet()) {
      ArraySliceExpression unrolledLhsExpression =
          applyUnrolledVariables(assignment.getKey().actual(), unrolledVariables);
      ArraySliceSpanLhs unrolledLhs =
          new ArraySliceSpanLhs(unrolledLhsExpression, assignment.getKey().targetType());

      for (ArraySliceSpanRhs rhs : assignment.getValue()) {
        final ArraySliceSpanRhs unrolledRhs;
        if (rhs.actual() instanceof ArraySliceExpressionRhs expressionRhs) {
          ArraySliceExpression unrolledRhsExpression =
              applyUnrolledVariables(expressionRhs.expression(), unrolledVariables);
          unrolledRhs =
              new ArraySliceSpanRhs(rhs.span(), new ArraySliceExpressionRhs(unrolledRhsExpression));
        } else {
          unrolledRhs = rhs;
        }
        unrolledAssignmentMultimap.put(unrolledLhs, unrolledRhs);
      }
    }
    return unrolledAssignmentMultimap;
  }

  private record ArraySliceResolvedWithVisitor(
      ArraySliceResolved resolved, CExpressionVisitorWithPointerAliasing visitor) {

    ArraySliceResolvedWithVisitor(
        ArraySliceResolved resolved, CExpressionVisitorWithPointerAliasing visitor) {
      checkNotNull(resolved);
      checkNotNull(visitor);
      this.resolved = resolved;
      this.visitor = visitor;
    }
  }

  private BooleanFormula performQuantifiedAssignment(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      AssignmentOptions assignmentOptions,
      Map<ArraySliceIndexVariable, Long> unrolledVariables,
      Map<ArraySliceIndexVariable, Formula> encodedVariables,
      BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    // only resolve each lhs and rhs expression once

    LinkedHashSet<ArraySliceExpression> lhsSliceSet =
        assignmentMultimap.entries().stream()
            .map(entry -> entry.getKey().actual())
            .collect(Collectors.toCollection(LinkedHashSet::new));

    Map<ArraySliceExpression, ArraySliceResolvedWithVisitor> lhsResolutionMap = new HashMap<>();

    for (ArraySliceExpression lhs : lhsSliceSet) {
      final CExpressionVisitorWithPointerAliasing lhsVisitor =
          new CExpressionVisitorWithPointerAliasing(
              conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);

      ArraySliceResolved lhsResolved = resolveSliceExpression(lhs, encodedVariables, lhsVisitor);

      // add initialized and used fields of lhs to pointer-target set as essential
      pts.addEssentialFields(lhsVisitor.getInitializedFields());
      pts.addEssentialFields(lhsVisitor.getUsedFields());

      CType lhsFinalType = lhsResolved.type();

      if (assignmentOptions.forcePointerAssignment()) {
        // if the force pointer assignment option is used, lhs must be an array
        // interpret it as a pointer instead
        CType lhsPointerType = CTypes.adjustFunctionOrArrayType(lhsFinalType);
        lhsResolved = new ArraySliceResolved(lhsResolved.expression(), lhsPointerType);
      }

      lhsResolutionMap.put(lhs, new ArraySliceResolvedWithVisitor(lhsResolved, lhsVisitor));
    }

    Map<ArraySliceRhs, Optional<ArraySliceResolvedWithVisitor>> rhsResolutionMap = new HashMap<>();

    LinkedHashSet<ArraySliceRhs> rhsSliceSet =
        assignmentMultimap.entries().stream()
            .map(entry -> entry.getValue().actual())
            .collect(Collectors.toCollection(LinkedHashSet::new));

    List<CompositeField> rhsAddressedFields = new ArrayList<>();

    for (ArraySliceRhs rhs : rhsSliceSet) {
      final CExpressionVisitorWithPointerAliasing rhsVisitor =
          new CExpressionVisitorWithPointerAliasing(
              conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);

      final Optional<ArraySliceResolved> rhsResolved =
          resolveRhs(rhs, encodedVariables, rhsVisitor);

      // add initialized and used fields of rhs to pointer-target set as essential
      pts.addEssentialFields(rhsVisitor.getInitializedFields());
      pts.addEssentialFields(rhsVisitor.getUsedFields());

      // prepare to add addressed fields of rhs to pointer-target set after assignment
      rhsAddressedFields.addAll(rhsVisitor.getAddressedFields());

      // add to resolution map
      if (rhsResolved.isPresent()) {
        rhsResolutionMap.put(
            rhs, Optional.of(new ArraySliceResolvedWithVisitor(rhsResolved.get(), rhsVisitor)));
      } else {
        rhsResolutionMap.put(rhs, Optional.empty());
      }
    }

    BooleanFormula result = bfmgr.makeTrue();

    for (Entry<ArraySliceSpanLhs, Collection<ArraySliceSpanRhs>> assignment :
        assignmentMultimap.asMap().entrySet()) {

      ArraySliceExpression lhs = assignment.getKey().actual();
      CType targetType = assignment.getKey().targetType();

      ArraySliceResolvedWithVisitor lhsResolvedWithVisitor = lhsResolutionMap.get(lhs);
      ArraySliceResolved lhsResolved = lhsResolvedWithVisitor.resolved;

      List<ArraySliceSpanResolved> rhsResolvedList = new ArrayList<>();

      for (ArraySliceSpanRhs rhs : assignment.getValue()) {
        Optional<ArraySliceResolvedWithVisitor> rhsResolvedWithVisitor = rhsResolutionMap.get(rhs.actual());

        // apply the deferred memory handler: if there is a malloc with void* type, the allocation
        // can
        // be deferred until the assignment that uses the value; the allocation type can then be
        // inferred from assignment lhs type
        if (rhsResolvedWithVisitor.isPresent()
            && (conv.options.revealAllocationTypeFromLHS()
                || conv.options.deferUntypedAllocations())) {

        // we have everything we need, call memory handler
        DynamicMemoryHandler memoryHandler =
            new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);
          memoryHandler.handleDeferredAllocationsInAssignment(
              (CLeftHandSide) lhs.getDummyResolvedExpression(sizeType),
              rhs.actual().getDummyResolvedRightHandSide(sizeType).get(),
              rhsResolvedWithVisitor.get().resolved.expression(),
              lhsResolvedWithVisitor.resolved.type(),
              lhsResolvedWithVisitor.visitor.getLearnedPointerTypes(),
              rhsResolvedWithVisitor.get().visitor.getLearnedPointerTypes());
        }

        if (rhsResolvedWithVisitor.isPresent()) {
          rhsResolvedList.add(
              new ArraySliceSpanResolved(rhs.span(), rhsResolvedWithVisitor.get().resolved));
        } else {
          // we need to construct nondet rhs with target type
          // no other rhs are needed after that
          rhsResolvedList.add(
              new ArraySliceSpanResolved(
                  rhs.span(), new ArraySliceResolved(Value.nondetValue(), targetType)));
        }
      }

      // compute pointer-target set pattern if necessary for UFs finishing
      // UFs must be finished only if all three of the following conditions are met:
      // 1. UF heap is used
      // 2. lhs is in aliased location (unaliased location is assigned as a whole)
      // 3. using old SSA indices is not selected
      final PointerTargetPattern pattern =
          !options.useArraysForHeap()
                  && lhsResolved.expression().isAliasedLocation()
                  && !assignmentOptions.useOldSSAIndicesIfAliased()
              ? PointerTargetPattern.forLeftHandSide(
                  (CLeftHandSide) lhs.getDummyResolvedExpression(sizeType), typeHandler, edge, pts)
              : null;

      // make the actual assignment
      result =
          bfmgr.and(
              result,
              assignmentFormulaHandler.makeSliceAssignment(
                  lhsResolved,
                  targetType,
                  rhsResolvedList,
                  assignmentOptions,
                  condition,
                  false,
                  pattern));
    }

    // add addressed fields of rhs to pointer-target set
    for (final CompositeField field : rhsAddressedFields) {
      pts.addField(field);
    }

    return result;
  }

  private BooleanFormula encodeQuantifier(
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> assignmentMultimap,
      AssignmentOptions assignmentOptions,
      LinkedHashSet<ArraySliceIndexVariable> nextVariablesToQuantify,
      Map<ArraySliceIndexVariable, Long> unrolledVariables,
      Map<ArraySliceIndexVariable, Formula> encodedVariables,
      BooleanFormula condition,
      ArraySliceIndexVariable variableToEncode,
      Formula sliceSizeFormula)
      throws UnrecognizedCodeException, InterruptedException {

    // the quantified variables should be of the size type
    FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(sizeType);
    Formula zeroFormula = conv.fmgr.makeNumber(sizeFormulaType, 0);
    boolean sizeTypeIsSigned = sizeType.getCanonicalType().isSigned();

    // create encoded quantified variable
    final Formula encodedVariable =
        fmgr.makeVariableWithoutSSAIndex(
            sizeFormulaType, "__quantifier_" + nextQuantifierVariableNumber++);

    HashMap<ArraySliceIndexVariable, Formula> nextEncodedVariables =
        new HashMap<>(encodedVariables);
    nextEncodedVariables.put(variableToEncode, encodedVariable);

    // create the condition for quantifier
    // the quantified variable condition holds when 0 <= index < size
    BooleanFormula nextCondition =
        bfmgr.and(
            condition,
            fmgr.makeLessOrEqual(zeroFormula, encodedVariable, sizeTypeIsSigned),
            fmgr.makeLessThan(encodedVariable, sliceSizeFormula, sizeTypeIsSigned));

    // recurse
    BooleanFormula recursionResult =
        quantifySliceAssignment(
            assignmentMultimap,
            assignmentOptions,
            nextVariablesToQuantify,
            unrolledVariables,
            nextEncodedVariables,
            nextCondition);

    // add quantifier around the recursion result
    return fmgr.getQuantifiedFormulaManager().forall(encodedVariable, recursionResult);
  }

  private Optional<ArraySliceResolved> resolveRhs(
      final ArraySliceRhs rhs,
      final Map<ArraySliceIndexVariable, Formula> encodedVariables,
      CExpressionVisitorWithPointerAliasing visitor)
      throws UnrecognizedCodeException {

    if (rhs instanceof ArraySliceCallRhs callRhs) {
      Expression rhsExpression = callRhs.call().accept(visitor);
      return Optional.of(
          new ArraySliceResolved(rhsExpression, typeHandler.getSimplifiedType(callRhs.call())));
    } else if (rhs instanceof ArraySliceExpressionRhs expressionRhs) {
      // lhs must be simple, so not an array, therefore, array type rhs must be converted to
      // pointer
      ArraySliceResolved resolved =
          resolveSliceExpression(
              ((ArraySliceExpressionRhs) rhs).expression(), encodedVariables, visitor);
      CType rhsType = CTypes.adjustFunctionOrArrayType(resolved.type());
      return Optional.of(new ArraySliceResolved(resolved.expression(), rhsType));
    } else {
      assert (rhs instanceof ArraySliceNondetRhs);
      return Optional.empty();
    }

  }

  private @Nullable ArraySliceResolved resolveSliceExpression(
      final ArraySliceExpression sliceExpression,
      final Map<ArraySliceIndexVariable, Formula> encodedVariables,
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
        base = resolveSubscriptModifier(base, subscriptModifier, encodedVariables);
      } else {
        base = resolveFieldAccessModifier(base, (ArraySliceFieldAccessModifier) modifier);
      }
      if (base == null) {
        // TODO: only used for ignoring assignments to bit-fields which should be handled properly
        // TODO: also used for ignoring non-aliased locations which should be handled properly
        return null;
      }
    }

    return base;
  }

  private ArraySliceResolved resolveSubscriptModifier(
      ArraySliceResolved base,
      ArraySliceSubscriptModifier modifier,
      final Map<ArraySliceIndexVariable, Formula> encodedVariables) {

    // find the quantified variable formula, the caller is responsible for ensuring that it is in
    // the map

    Formula quantifiedVariableFormula = encodedVariables.get(modifier.index());
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
