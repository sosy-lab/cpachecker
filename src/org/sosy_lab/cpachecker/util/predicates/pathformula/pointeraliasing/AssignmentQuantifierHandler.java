// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.AssignmentOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.PartialSpan;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.ResolvedPartialAssignmentRhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.ResolvedSlice;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceFieldAccessModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceFormulaIndexModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceIndexModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceVariable;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * Handles resolving assignments that have been already made simple. The quantified variables are
 * either unrolled or encoded before resolving the assignments and passing them to {@code
 * AssignmentFormulaHandler}.
 *
 * <p>By "unrolling", we mean replacing the quantified variable with discrete values it can take. As
 * this could make the formula too long or even infinitely long, the unrolling size is limited,
 * which does not preserve soundness.
 *
 * <p>By "encoding", we mean replacing the quantified variable with an SMT variable which is
 * quantified universally in the theory of quantifiers of the SMT solver. This is not supported by
 * all SMT solvers and may easily result in combinatorial explosion within the solver, but preserves
 * soundness.
 *
 * <p>Normal code should use {@link AssignmentHandler} for assignments instead which transforms
 * arbitrary assignments to simple assignments before using this handler.
 *
 * @see SliceExpression
 * @see AssignmentHandler
 * @see AssignmentFormulaHandler
 */
class AssignmentQuantifierHandler {

  /**
   * Left-hand side of an unresolved partial assignment. It stores both the slice expression and the
   * target type the right-hand sides should be cast/reinterpreted to. This is necessary because the
   * original cast target type is lost when making partial assignments simple.
   */
  record PartialAssignmentLhs(SliceExpression actual, CType targetType) {
    PartialAssignmentLhs {
      checkNotNull(actual);
      checkNotNull(targetType);
    }
  }

  /**
   * Right-hand side of an unresolved partial assignment. In addition to the slice expression, it
   * stores the span mapping the relevant part of right-hand side (after casting to target type) to
   * left-hand side.
   */
  record PartialAssignmentRhs(PartialSpan span, Optional<SliceExpression> actual) {
    PartialAssignmentRhs {
      checkNotNull(span);
      checkNotNull(actual);
    }
  }

  /** Prefix of SMT-encoded variable name, followed by variable number. */
  private static final String ENCODED_VARIABLE_PREFIX = "__quantifier_";

  /**
   * Next encoded variable number. Is static so that quantified variables can be differentiated in
   * SMT solver formula even when the quantifier handler is constructed for each assignment
   * separately.
   */
  private static int NEXT_ENCODED_VARIABLE_NUMBER = 0;

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

  /** Assignment options, used for each assignment within the constructed handler. */
  private final AssignmentOptions assignmentOptions;

  /**
   * Resolved left-hand-side bases of assignments. For each assignment to be handled, the
   * left-hand-side base must be present as a key in this map.
   */
  private final Map<CRightHandSide, ResolvedSlice> resolvedLhsBases;

  /**
   * Resolved left-hand-side bases of assignments. For each assignment to be handled, the base of
   * each deterministic right-hand-side part must be present as a key in this map.
   */
  private final Map<CRightHandSide, ResolvedSlice> resolvedRhsBases;

  /** Machine model pointer-equivalent size type, retained here for conciseness. */
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
   * @param pRegionMgr Memory region manager.
   * @param pAssignmentOptions Assignment options which will be used for each assignment within this
   *     handler.
   * @param pResolvedLhsBases Resolved left-hand-side bases.
   * @param pResolvedRhsBases Resolved right-hand-side bases.
   */
  AssignmentQuantifierHandler(
      CToFormulaConverterWithPointerAliasing pConv,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions,
      MemoryRegionManager pRegionMgr,
      AssignmentOptions pAssignmentOptions,
      Map<CRightHandSide, ResolvedSlice> pResolvedLhsBases,
      Map<CRightHandSide, ResolvedSlice> pResolvedRhsBases) {
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

    assignmentOptions = pAssignmentOptions;
    resolvedLhsBases = pResolvedLhsBases;
    resolvedRhsBases = pResolvedRhsBases;

    sizeType = conv.machineModel.getPointerSizedIntType();
  }

  /**
   * Performs simple slice assignments and returns the resulting Boolean formula.
   *
   * @param assignmentMultimap The multimap containing the simple slice assignments. Each LHS can
   *     have multiple partial RHS from which to assign. The full expression types of LHS and RHS
   *     cannot be array or composite types. The multimap should preserve order of addition for
   *     deterministic order of quantification.
   * @return The Boolean formula describing to assignments.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assigning.
   */
  BooleanFormula assignSimpleSlices(
      final Multimap<PartialAssignmentLhs, PartialAssignmentRhs> assignmentMultimap)
      throws UnrecognizedCodeException, InterruptedException {

    // get a set of variables that we need to quantify (encode or unroll)
    // each variable can be present in more locations, so we use a set to remove duplicates
    // as we want to have deterministic order of quantification, we use a LinkedHashSet
    final LinkedHashSet<SliceVariable> variablesToQuantify = new LinkedHashSet<>();
    for (Entry<PartialAssignmentLhs, Collection<PartialAssignmentRhs>> entry :
        assignmentMultimap.asMap().entrySet()) {
      variablesToQuantify.addAll(entry.getKey().actual().getPresentVariables());
      for (PartialAssignmentRhs rhs : entry.getValue()) {
        if (rhs.actual().isPresent()) {
          variablesToQuantify.addAll(rhs.actual().get().getPresentVariables());
        }
      }
    }

    // hand over to recursive quantification
    // initially, the condition for assignment to actually occur is true
    return quantifyAssignments(assignmentMultimap, variablesToQuantify, bfmgr.makeTrue());
  }

  /**
   * Recursively quantifies slice variables and performs the assignments when all variables are
   * quantified.
   *
   * <p>During each call with non-empty set of variables to quantify, one of the variables is
   * encoded or unrolled as selected. The requirements for the assignment to actually occur are
   * carried in the {@code condition} parameter: if it is not satisfied, the value is retained
   * instead. This is needed both for unrolling and encoding, as even with unrolling, the desired
   * variable range may not be statically known.
   *
   * @param assignmentMultimap The multimap containing the simple slice assignments.
   * @param variablesToQuantify Remaining variables that need to be quantified.
   * @param condition Boolean formula condition for the assignment to actually occur.
   * @return The Boolean formula describing the assignments.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assigning.
   */
  private BooleanFormula quantifyAssignments(
      final Multimap<PartialAssignmentLhs, PartialAssignmentRhs> assignmentMultimap,
      final LinkedHashSet<SliceVariable> variablesToQuantify,
      final BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    if (variablesToQuantify.isEmpty()) {
      // all variables have been quantified, perform quantified assignment
      return assignSimpleSlicesWithResolvedIndexing(assignmentMultimap, condition);
    }

    // not all variables have been quantified, get the variable to quantify
    final SliceVariable variableToQuantify = variablesToQuantify.iterator().next();

    // remove the variable which will be quantified from the next variables to quantify
    final LinkedHashSet<SliceVariable> nextVariablesToQuantify =
        new LinkedHashSet<>(variablesToQuantify);
    nextVariablesToQuantify.remove(variableToQuantify);

    // get the variable slice size (the assignment is done for all i where 0 <= i < sliceSize)
    final CExpression sliceSize = variableToQuantify.getSliceSize();
    // cast it to size type to get a proper formula
    final CExpression sliceSizeCastToSizeType =
        new CCastExpression(FileLocation.DUMMY, sizeType, sliceSize);
    // visit it to get the formula
    final CExpressionVisitorWithPointerAliasing indexSizeVisitor =
        new CExpressionVisitorWithPointerAliasing(
            conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
    final Expression sliceSizeExpression = sliceSizeCastToSizeType.accept(indexSizeVisitor);
    final Formula sliceSizeFormula = indexSizeVisitor.asValueFormula(sliceSizeExpression, sizeType);

    // TODO: should we add fields to UF from index visitor?

    // decide whether to encode or unroll the quantifier
    // the functions are recursive and return the result of completed assignment
    if (shouldEncode()) {
      return encodeQuantifier(
          assignmentMultimap,
          nextVariablesToQuantify,
          condition,
          variableToQuantify,
          sliceSizeFormula);
    } else {
      return unrollQuantifier(
          assignmentMultimap,
          nextVariablesToQuantify,
          condition,
          variableToQuantify,
          sliceSizeFormula);
    }
  }

  /**
   * Decides whether a variable should be encoded or unrolled.
   *
   * @return True if we should encode, false if we should unroll the variable.
   */
  private boolean shouldEncode() {
    // encode if the quantifiers are selected in global options or forced in assignment options
    // unroll otherwise
    // note that currently, all variables within the same assignment call behave the same,
    // but this behavior can be changed in the future if necessary: the handling is ready for it
    return options.useQuantifiersOnArrays() || assignmentOptions.forceQuantifiers();
  }

  /**
   * Encodes the quantifier for the given slice variable in the SMT solver theory of quantifiers and
   * calls {@link #quantifyAssignments(Multimap, LinkedHashSet, BooleanFormula)} recursively.
   *
   * @param assignmentMultimap The multimap containing the simple slice assignments.
   * @param nextVariablesToQuantify Remaining variables that need to be quantified, without the one
   *     to currently encode.
   * @param condition Boolean formula condition for the assignment to actually occur.
   * @param variableToEncode The variable to be encoded here.
   * @param sliceSizeFormula The formula for slice size. The assignment should occur iff {@code 0 <=
   *     i < sliceSizeFormula} where {@code i} is the encoded variable.
   * @return The Boolean formula describing the assignments.
   */
  private BooleanFormula encodeQuantifier(
      Multimap<PartialAssignmentLhs, PartialAssignmentRhs> assignmentMultimap,
      LinkedHashSet<SliceVariable> nextVariablesToQuantify,
      BooleanFormula condition,
      SliceVariable variableToEncode,
      Formula sliceSizeFormula)
      throws UnrecognizedCodeException, InterruptedException {

    // the quantified variable should be of size type
    final FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(sizeType);
    final Formula zeroFormula = conv.fmgr.makeNumber(sizeFormulaType, 0);
    final boolean sizeTypeIsSigned = sizeType.getCanonicalType().isSigned();

    // create encoded quantified variable
    final Formula encodedVariable =
        fmgr.makeVariableWithoutSSAIndex(
            sizeFormulaType, ENCODED_VARIABLE_PREFIX + NEXT_ENCODED_VARIABLE_NUMBER++);

    // resolve in assignments
    // for every (LHS or RHS) slice, we replace it with a slice that has unresolved indexing
    // by variableToUnroll replaced by resolved indexing by indexFormula
    final Multimap<PartialAssignmentLhs, PartialAssignmentRhs> nextAssignmentMultimap =
        mapAssignmentSlices(
            assignmentMultimap, slice -> slice.resolveVariable(variableToEncode, encodedVariable));

    // create the condition for quantifier
    // the quantified variable condition holds when 0 <= index < size
    // note that the size type may be signed, so we must do the less-or-equal constraint
    final BooleanFormula nextCondition =
        bfmgr.and(
            condition,
            fmgr.makeLessOrEqual(zeroFormula, encodedVariable, sizeTypeIsSigned),
            fmgr.makeLessThan(encodedVariable, sliceSizeFormula, sizeTypeIsSigned));

    // recurse and get the assignment result
    final BooleanFormula assignmentResult =
        quantifyAssignments(nextAssignmentMultimap, nextVariablesToQuantify, nextCondition);

    // add quantifier around the recursion result
    return fmgr.getQuantifiedFormulaManager().forall(encodedVariable, assignmentResult);
  }

  /**
   * Unrolls the quantifier for the given slice variable in the and calls {@link
   * #quantifyAssignments(Multimap, LinkedHashSet, BooleanFormula)} recursively.
   *
   * <p>This is unsound if the length of unrolling is not sufficient. If UFs are used, it also may
   * be unsound due to other assignments within the same aliased location not being retained.
   *
   * @param assignmentMultimap The multimap containing the simple slice assignments.
   * @param nextVariablesToQuantify Remaining variables that need to be quantified, without the one
   *     to currently encode.
   * @param condition Boolean formula condition for the assignment to actually occur.
   * @param variableToUnroll The variable to be unrolled here.
   * @param sliceSizeFormula The formula for slice size. The assignment should occur iff {@code 0 <=
   *     i < sliceSizeFormula} where {@code i} is the unrolled variable.
   * @return The Boolean formula describing the assignments.
   */
  private BooleanFormula unrollQuantifier(
      Multimap<PartialAssignmentLhs, PartialAssignmentRhs> assignmentMultimap,
      LinkedHashSet<SliceVariable> nextVariablesToQuantify,
      BooleanFormula condition,
      SliceVariable variableToUnroll,
      Formula sliceSizeFormula)
      throws UnrecognizedCodeException, InterruptedException {

    // the unrolled index should be of size type
    final FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(sizeType);
    final Formula zeroFormula = conv.fmgr.makeNumber(sizeFormulaType, 0);
    final boolean sizeTypeSigned = sizeType.getCanonicalType().isSigned();

    // limit the unrolling size to a reasonable number by default
    long unrollingSize = options.defaultArrayLength();

    // if the expression is a literal, we can get the exact slice size
    final CExpression sliceSize = variableToUnroll.getSliceSize();
    if (sliceSize instanceof CIntegerLiteralExpression literalSliceSize) {
      final long exactSliceSize = literalSliceSize.getValue().longValueExact();
      // decide whether the literal size is not longer than reasonable for instances where
      // we know the size exactly; note that the reasonable sizes may be different depending on
      // whether the slice size is a literal or not
      if (options.maxArrayLength() >= 0 && exactSliceSize > options.maxArrayLength()) {
        // unreasonable exact slice size, limit and warn
        unrollingSize = options.maxArrayLength();
        // warn just once for all literal unrollings to avoid polluting the output
        conv.logger.logfOnce(
            Level.WARNING,
            "Limiting unrolling of literal-length slice assignment to %s, soundness may be lost",
            options.maxArrayLength());
      } else {
        // reasonable exact slice size, soundness is guaranteed
        unrollingSize = exactSliceSize;
      }
    } else {
      // non-literal slice size expression, always potentially unsound
      // warn just once for all non-literal unrollings to avoid polluting the output
      conv.logger.logfOnce(
          Level.WARNING,
          "Limiting unrolling of non-literal-length slice assignment to %s, soundness may be lost",
          options.maxArrayLength());
    }

    // the result will be a conjunction of unrolled assignment results
    BooleanFormula result = bfmgr.makeTrue();

    // for all 0 <= i < unrollingSize, perform assignments with the variable formula set to i
    for (long i = 0; i < unrollingSize; ++i) {
      // construct the index formula
      final Formula indexFormula = conv.fmgr.makeNumber(sizeFormulaType, i);

      // perform the unrolled assignments conditionally
      // the variable condition holds when 0 <= i < size
      final BooleanFormula nextCondition =
          bfmgr.and(
              condition,
              fmgr.makeLessOrEqual(zeroFormula, indexFormula, sizeTypeSigned),
              fmgr.makeLessThan(indexFormula, sliceSizeFormula, sizeTypeSigned));

      // resolve the quantifier in assignments
      // for every (LHS or RHS) slice, we replace it with a slice that has unresolved indexing
      // by variableToUnroll replaced by resolved indexing by indexFormula
      final Multimap<PartialAssignmentLhs, PartialAssignmentRhs> nextAssignmentMultimap =
          mapAssignmentSlices(
              assignmentMultimap, slice -> slice.resolveVariable(variableToUnroll, indexFormula));

      // quantify recursively
      final BooleanFormula recursionResult =
          quantifyAssignments(nextAssignmentMultimap, nextVariablesToQuantify, nextCondition);

      // result is conjunction of unrolled assignment results
      result = bfmgr.and(result, recursionResult);
    }

    return result;
  }

  /**
   * Apply a given function to every slice in assignment multimap.
   *
   * <p>Used to replace a quantified variable in every slice with its resolved formula.
   *
   * @param assignmentMultimap Assignment multimap.
   * @param sliceMappingFunction A function to apply to every {@code ArraySliceExpression} in the
   *     multimap.
   * @return A new multimap with the function applied, with no other changes. Preserves ordering of
   *     {@code assignmentMultimap}.
   */
  private Multimap<PartialAssignmentLhs, PartialAssignmentRhs> mapAssignmentSlices(
      final Multimap<PartialAssignmentLhs, PartialAssignmentRhs> assignmentMultimap,
      final Function<SliceExpression, SliceExpression> sliceMappingFunction) {

    // LinkedHashMultimap to preserve ordering
    final Multimap<PartialAssignmentLhs, PartialAssignmentRhs> result = LinkedHashMultimap.create();

    // iterate over all LHS
    for (Entry<PartialAssignmentLhs, Collection<PartialAssignmentRhs>> assignment :
        assignmentMultimap.asMap().entrySet()) {
      // apply the function to the LHS slice
      final SliceExpression mappedLhsSlice =
          sliceMappingFunction.apply(assignment.getKey().actual());
      // construct the whole LHS
      final PartialAssignmentLhs mappedLhs =
          new PartialAssignmentLhs(mappedLhsSlice, assignment.getKey().targetType());

      // iterate over all RHS
      for (PartialAssignmentRhs rhs : assignment.getValue()) {
        // apply the function to the RHS slice if it exists
        // (if it does not, it is taken as nondet)
        final Optional<SliceExpression> resolvedRhsSlice =
            rhs.actual().map(rhsSlice -> sliceMappingFunction.apply(rhsSlice));
        // construct the whole RHS and put the result into the new multimap
        final PartialAssignmentRhs resolvedRhs = new PartialAssignmentRhs(rhs.span(), resolvedRhsSlice);
        result.put(mappedLhs, resolvedRhs);
      }
    }
    return result;
  }

  /**
   * Performs simple slice assignments and returns the resulting Boolean formula. All indexing
   * modifiers in the assignments must be already resolved.
   *
   * @param assignmentMultimap The multimap containing the simple slice assignments. All indexing
   *     modifiers must be resolved.
   * @param condition Boolean formula condition for the assignment to actually occur.
   * @return The Boolean formula describing the assignments.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assigning.
   */
  private BooleanFormula assignSimpleSlicesWithResolvedIndexing(
      final Multimap<PartialAssignmentLhs, PartialAssignmentRhs> assignmentMultimap,
      final BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    // construct a formula handler
    final AssignmentFormulaHandler assignmentFormulaHandler =
        new AssignmentFormulaHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);

    // the result is a conjunction of assignments
    BooleanFormula result = bfmgr.makeTrue();

    // for each assignment, perform it using the formula handler and conjunct the result
    for (Entry<PartialAssignmentLhs, Collection<PartialAssignmentRhs>> assignment :
        assignmentMultimap.asMap().entrySet()) {

      final SliceExpression lhsSlice = assignment.getKey().actual();
      final CType targetType = assignment.getKey().targetType();

      // resolve the LHS by getting the resolved base and resolving modifiers over it
      final ResolvedSlice lhsResolvedBase = resolvedLhsBases.get(lhsSlice.getBase());
      final ResolvedSlice lhsResolved = applySliceModifiersToResolvedBase(lhsResolvedBase, lhsSlice);

      // skip assignment if LHS is nondet
      if (lhsResolved.expression().isNondetValue()) {
        // should only happen when we cannot assign to aliased bitfields
        // TODO: implement aliased bitfields
        continue;
      }

      final List<ResolvedPartialAssignmentRhs> rhsResolvedList = new ArrayList<>();

      // resolve each RHS and collect them into a list
      for (PartialAssignmentRhs rhs : assignment.getValue()) {

        // make nondet RHS into nondet resolved
        if (rhs.actual().isEmpty()) {
          rhsResolvedList.add(new ResolvedPartialAssignmentRhs(rhs.span(), Optional.empty()));
          continue;
        }

        // resolve the RHS by getting the resolved base and resolving modifiers over it
        final SliceExpression rhsSlice = rhs.actual().get();
        final ResolvedSlice rhsResolvedBase = resolvedRhsBases.get(rhsSlice.getBase());
        ResolvedSlice rhsResolved = applySliceModifiersToResolvedBase(rhsResolvedBase, rhsSlice);

        // after resolving rhs, the rhs resolved type may be array even if we want to do
        // pointer assignment, signified by pointer target type
        // make rhs resolved target type into pointer in that case
        if (targetType instanceof CPointerType) {
          rhsResolved =
              new ResolvedSlice(
                  rhsResolved.expression(), CTypes.adjustFunctionOrArrayType(rhsResolved.type()));
        }
        // add resolved RHS to list
        rhsResolvedList.add(new ResolvedPartialAssignmentRhs(rhs.span(), Optional.of(rhsResolved)));
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
                  (CLeftHandSide) lhsSlice.getDummyResolvedExpression(sizeType), typeHandler, edge, pts)
              : null;

      // make the actual assignment
      result =
          bfmgr.and(
              result,
              assignmentFormulaHandler.assignResolvedSlice(
                  lhsResolved,
                  targetType,
                  rhsResolvedList,
                  assignmentOptions,
                  condition,
                  false,
                  pattern));
    }

    return result;
  }

  /**
   * Applies slice modifiers to resolved base.
   *
   * @param resolvedBase Supplied resolved base (it must be resolved externally).
   * @return Resolution result after applying modifiers.
   * @throws IllegalStateException If there are any unresolved modifiers.
   */
  private ResolvedSlice applySliceModifiersToResolvedBase(
      final ResolvedSlice resolvedBase, final SliceExpression slice) {
    assert (resolvedBase != null);

    ResolvedSlice resolved = resolvedBase;

    // needed for the subscript modifier
    boolean wasParameterId =
        (slice.getBase() instanceof CIdExpression idBase)
            && idBase.getDeclaration() instanceof CParameterDeclaration;

    // apply the modifiers now
    // TODO: deduplicate the resolution functions with CExpressionVisitorWithPointerAliasing
    for (SliceModifier modifier : slice.getModifiers()) {
      if (modifier instanceof SliceIndexModifier subscriptModifier) {
        resolved = applySubscriptModifier(resolved, subscriptModifier, wasParameterId);
      } else if (modifier instanceof SliceFieldAccessModifier fieldAccessModifier) {
        resolved = applyFieldAccessModifier(resolved, fieldAccessModifier);
      } else {
        throw new IllegalStateException("Cannot apply unresolved modifier to resolved slice");
      }
    }

    return resolved;
  }

  private ResolvedSlice applyFieldAccessModifier(
      ResolvedSlice resolved, SliceFieldAccessModifier modifier) {

    // the base type must be a composite type to have fields
    CCompositeType baseType = (CCompositeType) resolved.type();
    final String fieldName = modifier.field().getName();
    CType fieldType = conv.typeHandler.getSimplifiedType(modifier.field());

    // composite types may be aliased or unaliased, resolve in both cases
    if (resolved.expression().isUnaliasedLocation()) {
      UnaliasedLocation resultLocation =
          UnaliasedLocation.ofVariableName(
              getFieldAccessName(
                  resolved.expression().asUnaliasedLocation().getVariableName(), modifier.field()));
      return new ResolvedSlice(resultLocation, fieldType);
    }

    // aliased location
    // we will increase the base address by field offset
    Formula baseAddress = resolved.expression().asAliasedLocation().getAddress();

    // we must create a memory region for access
    final MemoryRegion region = regionMgr.makeMemoryRegion(baseType, modifier.field());

    final OptionalLong offset = conv.typeHandler.getOffset(baseType, fieldName);
    if (!offset.isPresent()) {
      // this loses assignments from/to aliased bitfields
      // TODO: implement aliased bitfields
      return new ResolvedSlice(Value.nondetValue(), fieldType);
    }

    final Formula offsetFormula =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, offset.orElseThrow());
    final Formula adjustedAdress = conv.fmgr.makePlus(baseAddress, offsetFormula);

    AliasedLocation adjustedLocation = AliasedLocation.ofAddressWithRegion(adjustedAdress, region);
    return new ResolvedSlice(adjustedLocation, fieldType);
  }

  private ResolvedSlice applySubscriptModifier(
      ResolvedSlice resolved, SliceIndexModifier modifier, boolean wasParameterId) {

    final AliasedLocation dereferenced;

    // dereference resolved
    // TODO: deduplicate with CExpressionVisitorWithPointerAliasing.dereference
    boolean shouldTreatAsDirectAccess =
        resolved.expression().isAliasedLocation()
            && (resolved.type() instanceof CCompositeType
                || (resolved.type() instanceof CArrayType && !wasParameterId));
    if (shouldTreatAsDirectAccess) {
      dereferenced = resolved.expression().asAliasedLocation();
    } else {
      dereferenced =
          AliasedLocation.ofAddress(
              asValueFormula(
                  resolved.expression(),
                  CTypeUtils.implicitCastToPointer(resolved.type()),
                  shouldTreatAsDirectAccess));
    }

    // all subscript modifiers must be already resolved here
    SliceFormulaIndexModifier resolvedModifier = (SliceFormulaIndexModifier) modifier;

    // get the array element type
    CPointerType basePointerType = (CPointerType) CTypes.adjustFunctionOrArrayType(resolved.type());
    final CType elementType = conv.typeHandler.simplifyType(basePointerType.getType());

    // get base array address, arrays must be always aliased
    Formula baseAddress = dereferenced.getAddress();

    // get size of array element
    final Formula sizeofElement =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.getSizeof(elementType));

    // perform pointer arithmetic, we have array[base] and want array[base + i]
    // the quantified variable i must be multiplied by the sizeof the element type
    final Formula adjustedAddress =
        conv.fmgr.makePlus(
            baseAddress, conv.fmgr.makeMultiply(resolvedModifier.encodedVariable(), sizeofElement));

    // return the resolved formula with adjusted address and array element type
    return new ResolvedSlice(AliasedLocation.ofAddress(adjustedAddress), elementType);
  }

  private Formula asValueFormula(final Expression e, final CType type, final boolean isSafe) {
    // TODO: deduplicate with CExpressionVisitorWithPointerAliasing.asValueFormula
    if (e.isNondetValue()) {
      throw new IllegalStateException();
    } else if (e.isValue()) {
      return e.asValue().getValue();
    } else if (e.isAliasedLocation()) {
      MemoryRegion region = e.asAliasedLocation().getMemoryRegion();
      if (region == null) {
        region = regionMgr.makeMemoryRegion(type);
      }
      return !isSafe
          ? conv.makeDereference(
              type, e.asAliasedLocation().getAddress(), ssa, errorConditions, region)
          : conv.makeSafeDereference(type, e.asAliasedLocation().getAddress(), ssa, region);
    } else { // Unaliased location
      return conv.makeVariable(e.asUnaliasedLocation().getVariableName(), type, ssa);
    }
  }
}
