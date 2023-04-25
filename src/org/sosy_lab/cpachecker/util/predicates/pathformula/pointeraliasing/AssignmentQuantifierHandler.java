// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.PartialSpan;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.ResolvedPartialAssignmentRhs;
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

  /** Stores partial assignment left-hand side and all right-hand sides. */
  record PartialAssignment(PartialAssignmentLhs lhs, ImmutableList<PartialAssignmentRhs> rhsList) {
    PartialAssignment {
      checkNotNull(lhs);
      checkNotNull(rhsList);
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

  private final AddressHandler addressHandler;

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

  /**
   * Unsigned type capable of containing all pointer values. Used for constructing quantification
   * conditions. Encoded quantified variables are of its formula type.
   */
  private final CSimpleType pointerAsUnsignedIntType;

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

    addressHandler = new AddressHandler(pConv, pSsa, pConstraints, pErrorConditions, pRegionMgr);

    assignmentOptions = pAssignmentOptions;
    resolvedLhsBases = pResolvedLhsBases;
    resolvedRhsBases = pResolvedRhsBases;

    pointerAsUnsignedIntType = conv.machineModel.getPointerAsUnsignedIntType();
  }

  /**
   * Performs simple slice assignments and returns the resulting Boolean formula.
   *
   * @param assignmentMultimap The multimap containing the simple partial slice assignments. Each
   *     LHS can have multiple partial RHS from which to assign. The full expression types of LHS
   *     and RHS cannot be array or composite types. The multimap should preserve order of addition
   *     for deterministic order of quantification.
   * @return The Boolean formula describing to assignments.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assigning.
   */
  BooleanFormula assignSimpleSlices(
      final Multimap<PartialAssignmentLhs, PartialAssignmentRhs> assignmentMultimap)
      throws UnrecognizedCodeException, InterruptedException {

    // the result is a conjunction of assignments
    BooleanFormula result = bfmgr.makeTrue();

    // assign each left-hand side separately
    // we do not want to assign multiple left-hand sides at once as we would potentially need to
    // unroll variables that only occur in one assignment, which would result in a large amount
    // of unnecessary formula duplication
    for (Entry<PartialAssignmentLhs, Collection<PartialAssignmentRhs>> entry :
        assignmentMultimap.asMap().entrySet()) {

      final PartialAssignmentLhs lhs = entry.getKey();
      final ImmutableList<PartialAssignmentRhs> rhsList = ImmutableList.copyOf(entry.getValue());

      // get the variables that we need to quantify (encode or unroll)
      // each variable can be present at both sides, potentially even in multiple modifiers at each
      // side, so we use a set to remove duplicates
      // ImmutableSet also guarantees deterministic order of quantification, so there should not be
      // problems with run-to-run variance due to different order of quantification between runs
      final ImmutableSet.Builder<SliceVariable> variablesToQuantifyBuilder = ImmutableSet.builder();

      variablesToQuantifyBuilder.addAll(lhs.actual().getPresentVariables());
      for (PartialAssignmentRhs rhs : rhsList) {
        if (rhs.actual().isPresent()) {
          variablesToQuantifyBuilder.addAll(rhs.actual().get().getPresentVariables());
        }
      }

      // hand over to recursive quantification
      // initially, the condition for assignment to actually occur is true
      final PartialAssignment assignment = new PartialAssignment(lhs, rhsList);

      // check that the assignment is supported; this must be done with a simple partial assignment,
      // but can be done before quantification, so we will do it here to avoid potential costs of
      // checking multiple times after unrolling
      checkAssignmentSupported(assignment);

      BooleanFormula assignmentResult =
          quantifyAssignments(
              assignment, variablesToQuantifyBuilder.build().asList(), bfmgr.makeTrue());

      // conjunct the assignment formulas
      result = bfmgr.and(result, assignmentResult);
    }

    return result;
  }

  /**
   * Checks whether an assignment is supported.
   *
   * @see AssignmentOptions.ConversionType#BYTE_REPEAT
   * @param assignment The assignment to check.
   * @throws UnrecognizedCodeException Thrown if conversion type is {@link
   *     AssignmentOptions.ConversionType#BYTE_REPEAT} and the full left-hand side type is void,
   *     which signifies incomplete type discovery. Also thrown if conversion type is {@link
   *     AssignmentOptions.ConversionType#BYTE_REPEAT} and there are bitfields within the left-hand
   *     side and it cannot be determined that rhs is either all-ones or all-zeros, so bitfield
   *     value would be heavily implementation-defined.
   */
  private void checkAssignmentSupported(PartialAssignment assignment)
      throws UnrecognizedCodeException {

    // assignment is always supported when not using BYTE_REPEAT conversion type
    if (assignmentOptions.conversionType() != AssignmentOptions.ConversionType.BYTE_REPEAT) {
      return;
    }

    // make sure that the full expression type is not void; this is not permitted
    final CType fullExpressionType =
        typeHandler.simplifyType(assignment.lhs.actual.getFullExpressionType());

    if (fullExpressionType instanceof CVoidType) {
      throw new UnrecognizedCodeException("Unsupported assignment to void", edge);
    }

    // make sure that if the destination is a bitfield, the rhs definitely sets every bit to zero or
    // one; otherwise, behavior would be heavily implementation-defined

    if (fullExpressionType instanceof CBitFieldType) {
        for (PartialAssignmentRhs rhs : assignment.rhsList) {
          if (rhs.actual.isEmpty()) {
            // nondet, skip
            continue;
          }
        SliceExpression rhsSlice = rhs.actual.get();

        // it is the caller's responsibility to ensure the rhs base is a cast to unsigned char
        CCastExpression base = (CCastExpression) rhsSlice.base();
        verify(
            rhsSlice.modifiers().isEmpty()
                && base.getCastType().equals(CNumericTypes.UNSIGNED_CHAR));

        CExpression baseUnderlying = base.getOperand();

        if (!(baseUnderlying instanceof CIntegerLiteralExpression)) {
          throw new UnrecognizedCodeException(
              "Non-literal byte repeat value not supported for bitfields", edge);
        }

        // determine the value of literal
        final CIntegerLiteralExpression setValueLiteral = (CIntegerLiteralExpression) baseUnderlying;

        // make sure it is either all-zeros or all-ones
        int unsignedCharAllOnes =
            conv.machineModel.getMaximalIntegerValue(CNumericTypes.UNSIGNED_CHAR).intValue();
        final int setByte = setValueLiteral.getValue().intValue() & unsignedCharAllOnes;
        if (setByte != 0 && setByte != unsignedCharAllOnes) {
          throw new UnrecognizedCodeException(
              "Only all-zeros and all-ones byte repeat values supported for bitfields", edge);
        }
      }
    }
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
   * @param assignment The simple partial slice assignment.
   * @param variablesToQuantify Remaining variables that need to be quantified. Each variable which
   *     still needs to be quantified must appear in this list exactly once.
   * @param condition Boolean formula condition for the assignment to actually occur.
   * @return The Boolean formula describing the assignments.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assigning.
   */
  private BooleanFormula quantifyAssignments(
      final PartialAssignment assignment,
      final List<SliceVariable> variablesToQuantify,
      final BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    if (variablesToQuantify.isEmpty()) {
      // all variables have been quantified, perform quantified assignment
      return assignSimpleSlicesWithResolvedIndexing(assignment, condition);
    }

    // not all variables have been quantified, get the variable to quantify
    final SliceVariable variableToQuantify = variablesToQuantify.iterator().next();

    // make a sublist without the variable to quantify
    final List<SliceVariable> nextVariablesToQuantify =
        variablesToQuantify.subList(1, variablesToQuantify.size());

    // get the variable slice size (the assignment is done for all i where 0 <= i < sliceSize)
    final CExpression sliceSize = variableToQuantify.getSliceSize();
    // cast it to unsigned type capable of storing all pointer values to get a proper formula
    final CExpression sliceSizeCastToCapableType =
        new CCastExpression(FileLocation.DUMMY, pointerAsUnsignedIntType, sliceSize);
    // visit it to get the formula
    final CExpressionVisitorWithPointerAliasing indexSizeVisitor =
        new CExpressionVisitorWithPointerAliasing(
            conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
    final Expression sliceSizeExpression = sliceSizeCastToCapableType.accept(indexSizeVisitor);
    final Formula sliceSizeFormula =
        indexSizeVisitor.asValueFormula(sliceSizeExpression, pointerAsUnsignedIntType);

    // TODO: should we add fields to UF from index visitor?

    // decide whether to encode or unroll the quantifier
    // the functions are recursive and return the result of completed assignment
    if (shouldEncode()) {
      return encodeQuantifier(
          assignment,
          nextVariablesToQuantify,
          condition,
          variableToQuantify,
          sliceSizeFormula);
    } else {
      return unrollQuantifier(
          assignment,
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
    return options.useQuantifiersOnArrays() || assignmentOptions.forceEncodingQuantifiers();
  }

  /**
   * Encodes the quantifier for the given slice variable in the SMT solver theory of quantifiers and
   * calls {@link #quantifyAssignments(PartialAssignment, List, BooleanFormula)} recursively.
   *
   * @param assignment The the simple partial slice assignment to quantify.
   * @param nextVariablesToQuantify Remaining variables that need to be quantified, without the one
   *     to currently encode. Each variable which still needs to be quantified, except the one to
   *     currently encode, must appear in this list exactly once.
   * @param condition Boolean formula condition for the assignment to actually occur.
   * @param variableToEncode The variable to be encoded here.
   * @param sliceSizeFormula The formula for slice size. The assignment should occur iff {@code 0 <=
   *     i < sliceSizeFormula} where {@code i} is the encoded variable.
   * @return The Boolean formula describing the assignment.
   */
  private BooleanFormula encodeQuantifier(
      PartialAssignment assignment,
      List<SliceVariable> nextVariablesToQuantify,
      BooleanFormula condition,
      SliceVariable variableToEncode,
      Formula sliceSizeFormula)
      throws UnrecognizedCodeException, InterruptedException {

    // the encoded quantified variable should be of pointerAsUnsignedIntType
    final FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(pointerAsUnsignedIntType);

    // create encoded quantified variable
    final Formula encodedVariable =
        fmgr.makeVariableWithoutSSAIndex(
            sizeFormulaType, ENCODED_VARIABLE_PREFIX + NEXT_ENCODED_VARIABLE_NUMBER++);

    // resolve the quantifier in assignment
    // for every (LHS or RHS) slice, we replace it with a slice that has unresolved indexing
    // by variableToUnroll replaced by resolved indexing by indexFormula

    final PartialAssignment nextAssignment =
        mapAssignmentSlices(
            assignment, slice -> slice.resolveVariable(variableToEncode, encodedVariable));

    // create the condition for quantifier
    // the quantified variable condition holds when 0 <= index < size
    // the formula is unsigned, we will just do an unsigned less-than comparison
    final BooleanFormula nextCondition =
        bfmgr.and(condition, fmgr.makeLessThan(encodedVariable, sliceSizeFormula, false));

    // recurse and get the assignment result
    final BooleanFormula assignmentResult =
        quantifyAssignments(nextAssignment, nextVariablesToQuantify, nextCondition);

    // add quantifier around the recursion result
    return fmgr.getQuantifiedFormulaManager().forall(encodedVariable, assignmentResult);
  }

  /**
   * Unrolls the quantifier for the given slice variable in the and calls {@link
   * #quantifyAssignments(PartialAssignment, List, BooleanFormula)} recursively.
   *
   * <p>This is unsound if the length of unrolling is not sufficient. If UFs are used, it also may
   * be unsound due to other assignments within the same aliased location not being retained.
   *
   * @param assignment The simple partial slice assignment to unroll.
   * @param nextVariablesToQuantify Remaining variables that need to be quantified, without the one
   *     to currently unroll. Each variable which still needs to be quantified, except the one to
   *     currently unroll, must appear in this list exactly once.
   * @param condition Boolean formula condition for the assignment to actually occur.
   * @param variableToUnroll The variable to be unrolled here.
   * @param sliceSizeFormula The formula for slice size. The assignment should occur iff {@code 0 <=
   *     i < sliceSizeFormula} where {@code i} is the unrolled variable.
   * @return The Boolean formula describing the assignment.
   */
  private BooleanFormula unrollQuantifier(
      PartialAssignment assignment,
      List<SliceVariable> nextVariablesToQuantify,
      BooleanFormula condition,
      SliceVariable variableToUnroll,
      Formula sliceSizeFormula)
      throws UnrecognizedCodeException, InterruptedException {

    // the unrolled index should be of pointerAsUnsignedIntType
    final FormulaType<?> sizeFormulaType = conv.getFormulaTypeFromCType(pointerAsUnsignedIntType);

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
            unrollingSize);
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
          unrollingSize);
    }

    // the result will be a conjunction of unrolled assignment results
    BooleanFormula result = bfmgr.makeTrue();

    // for all 0 <= i < unrollingSize, perform assignments with the variable formula set to i
    for (long i = 0; i < unrollingSize; ++i) {
      // construct the index formula
      final Formula indexFormula = conv.fmgr.makeNumber(sizeFormulaType, i);

      // perform the unrolled assignments conditionally
      // the variable condition holds when 0 <= i < size
      // the formula is unsigned, we will just do an unsigned less-than comparison
      final BooleanFormula nextCondition =
          bfmgr.and(condition, fmgr.makeLessThan(indexFormula, sliceSizeFormula, false));

      // resolve the quantifier in assignment
      // for every (LHS or RHS) slice, we replace it with a slice that has unresolved indexing
      // by variableToUnroll replaced by resolved indexing by indexFormula
      final PartialAssignment nextAssignment =
          mapAssignmentSlices(
              assignment, slice -> slice.resolveVariable(variableToUnroll, indexFormula));

      // quantify recursively
      final BooleanFormula recursionResult =
          quantifyAssignments(nextAssignment, nextVariablesToQuantify, nextCondition);

      // result is conjunction of unrolled assignment results
      result = bfmgr.and(result, recursionResult);
    }

    return result;
  }

  /**
   * Apply a given function to every slice in partial slice assignment.
   *
   * <p>Used to replace a quantified variable in every slice with its resolved formula.
   *
   * @param assignment Partial slice assignment.
   * @param sliceMappingFunction A function to apply to every {@code ArraySliceExpression} in the
   *     assignment.
   * @return Partial slice assignment with the function applied, with no other changes.
   */
  private PartialAssignment mapAssignmentSlices(
      final PartialAssignment assignment,
      final Function<SliceExpression, SliceExpression> sliceMappingFunction) {

    // apply the function to the LHS slice
    final SliceExpression mappedLhsSlice =
        sliceMappingFunction.apply(assignment.lhs.actual);
    // construct the whole LHS
    final PartialAssignmentLhs mappedLhs =
        new PartialAssignmentLhs(mappedLhsSlice, assignment.lhs.targetType);

    ImmutableList.Builder<PartialAssignmentRhs> mappedRhsListBuilder = ImmutableList.builder();

    // iterate over all RHS
    for (PartialAssignmentRhs rhs : assignment.rhsList) {
      // apply the function to the RHS slice if it exists
      // (if it does not, it is taken as nondet)
      final Optional<SliceExpression> mappedRhsSlice =
          rhs.actual().map(rhsSlice -> sliceMappingFunction.apply(rhsSlice));
      // construct the whole RHS and put the result into the new multimap
      final PartialAssignmentRhs mappedRhs = new PartialAssignmentRhs(rhs.span(), mappedRhsSlice);
      mappedRhsListBuilder.add(mappedRhs);
    }
    return new PartialAssignment(mappedLhs, mappedRhsListBuilder.build());
  }

  /**
   * Performs simple slice assignment and returns the resulting Boolean formula. All indexing
   * modifiers in the assignment must be already resolved.
   *
   * @param assignment The simple partial slice assignment. All indexing modifiers must be resolved.
   * @param condition Boolean formula condition for the assignment to actually occur.
   * @return The Boolean formula describing the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assigning.
   */
  private BooleanFormula assignSimpleSlicesWithResolvedIndexing(
      final PartialAssignment assignment, final BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    // construct a formula handler
    final AssignmentFormulaHandler assignmentFormulaHandler =
        new AssignmentFormulaHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);


      final SliceExpression lhsSlice = assignment.lhs.actual();
      final CType targetType = assignment.lhs.targetType();

      // resolve the LHS by getting the resolved base and resolving modifiers over it
      final ResolvedSlice lhsResolvedBase = resolvedLhsBases.get(lhsSlice.base());
      final ResolvedSlice lhsResolved = applySliceModifiersToResolvedBase(lhsResolvedBase, lhsSlice);

      final List<ResolvedPartialAssignmentRhs> rhsResolvedList = new ArrayList<>();

      // resolve each RHS and collect them into a list
      for (PartialAssignmentRhs rhs : assignment.rhsList) {

        // make nondet RHS into nondet resolved
        if (rhs.actual().isEmpty()) {
          rhsResolvedList.add(new ResolvedPartialAssignmentRhs(rhs.span(), Optional.empty()));
          continue;
        }

        // resolve the RHS by getting the resolved base and resolving modifiers over it
        final SliceExpression rhsSlice = rhs.actual().get();
        final ResolvedSlice rhsResolvedBase = resolvedRhsBases.get(rhsSlice.base());
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
                  (CLeftHandSide) lhsSlice.getDummyResolvedExpression(), typeHandler, edge, pts)
              : null;

      // make the actual assignment
      return assignmentFormulaHandler.assignResolvedSlice(
                  lhsResolved,
                  targetType,
                  rhsResolvedList,
                  assignmentOptions,
                  condition,
                  false,
                  pattern);
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
    checkNotNull(resolvedBase);
    checkNotNull(slice);

    ResolvedSlice resolved = resolvedBase;

    // needed for correct dereferencing in the subscript modifier
    boolean resolvedIsFunctionParameter =
        (slice.base() instanceof CIdExpression idBase)
            && idBase.getDeclaration() instanceof CParameterDeclaration;

    // apply the modifiers now
    for (SliceModifier modifier : slice.modifiers()) {
      if (modifier instanceof SliceIndexModifier subscriptModifier) {
        resolved = applySubscriptModifier(resolved, subscriptModifier, resolvedIsFunctionParameter);
      } else if (modifier instanceof SliceFieldAccessModifier fieldAccessModifier) {
        resolved = applyFieldAccessModifier(resolved, fieldAccessModifier);
      } else {
        throw new IllegalStateException("Cannot apply unresolved modifier to resolved slice");
      }
      // resolved can no longer be function parameter
      resolvedIsFunctionParameter = false;
    }

    return resolved;
  }

  /**
   * Applies a field access modifier to the resolved base.
   *
   * @param resolved Resolved base.
   * @param modifier Field access modifier to apply.
   * @return Resolved slice with applied field access modifier.
   */
  private ResolvedSlice applyFieldAccessModifier(
      ResolvedSlice resolved, SliceFieldAccessModifier modifier) {

    // the base type must be a composite type to have fields
    CCompositeType baseType = (CCompositeType) resolved.type();
    CType fieldType = conv.typeHandler.getSimplifiedType(modifier.field());

    // add field to essential fields for uninterpreted functions
    pts.addEssentialFields(ImmutableList.of(CompositeField.of(baseType, modifier.field())));

    // handle depending on type of base, normally should be either unaliased or aliased
    // also can be nondet due to bitfields that we do not currently handle
    Expression base = resolved.expression();

    if (base.isUnaliasedLocation()) {
      // just return the field-accessed name
      UnaliasedLocation resultLocation =
          UnaliasedLocation.ofVariableName(
              getFieldAccessName(
                  resolved.expression().asUnaliasedLocation().getVariableName(), modifier.field()));
      return new ResolvedSlice(resultLocation, fieldType);
    } else if (base.isAliasedLocation()) {
      // no dereference needed, base type is composite, not pointer-like
      // adjust via AddressHandler
      Expression adjustedExpression =
          addressHandler.applyFieldOffset(
              base.asAliasedLocation(), CompositeField.of(baseType, modifier.field()));
      return new ResolvedSlice(adjustedExpression, fieldType);
    } else if (base.isNondetValue()) {
      // should only happen due to bitfields that we do not currently handle
      // silently pass through with the new type
      return new ResolvedSlice(Value.nondetValue(), fieldType);
    } else {
      // should never happen
      throw new AssertionError();
    }
  }

  /**
   * Applies a subscript modifier to the resolved base.
   *
   * @param resolved Resolved base.
   * @param modifier Subscript modifier to apply.
   * @param resolvedIsFunctionParameter Whether the resolved base is a function parameter. Needed
   *     for correct dereferencing of the base.
   * @return Resolved slice with applied subscript modifier.
   */
  private ResolvedSlice applySubscriptModifier(
      ResolvedSlice resolved, SliceIndexModifier modifier, boolean resolvedIsFunctionParameter) {

    // all subscript modifiers must be already resolved here
    SliceFormulaIndexModifier resolvedModifier = (SliceFormulaIndexModifier) modifier;

    // get the array element type
    CType baseType = typeHandler.simplifyType(resolved.type());
    CPointerType basePointerType = (CPointerType) CTypes.adjustFunctionOrArrayType(baseType);
    final CType elementType = typeHandler.simplifyType(basePointerType.getType());

    // get the base
    final Expression base = resolved.expression();

    // compute whether the base is direct-access, base type can never be composite here
    // see CExpressionVisitorWithPointerAliasing#dereference(CExpression, Expression)
    // for the direct access reasoning
    final boolean directAccess =
        resolved.type() instanceof CArrayType && !resolvedIsFunctionParameter;
    // subscript is the encoded modifier variable
    final Formula subscript = resolvedModifier.encodedVariable();
    // use addressHandler to apply subscript
    // the differentiation based on type of expression will be handled in it
    Expression adjustedExpression =
        addressHandler.applySubscriptOffset(baseType, base, directAccess, elementType, subscript);
    return new ResolvedSlice(adjustedExpression, elementType);
  }

}
