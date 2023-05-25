// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.implicitCastToPointer;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SMTHeap.SMTAddressValue;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.ResolvedSlice;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * Handler for low-level slice assignments with left-hand sides and partial right-hand sides already
 * resolved to an expression.
 *
 * <p>Normal code should use {@link AssignmentHandler} for assignments.
 *
 * @see SliceExpression
 * @see AssignmentHandler
 * @see AssignmentQuantifierHandler
 */
class AssignmentFormulaHandler {

  /**
   * Determines the span of a partial assignment. The only relevant part is {@link #bitSize} bits
   * long. Its least-significant-bit offset on the left-hand side is {@link #lhsBitOffset}. On the
   * right-hand side, the offset {@link #rhsTargetBitOffset} pertains to the value after
   * casting/reinterpreting to target type.
   */
  record PartialSpan(long lhsBitOffset, long rhsTargetBitOffset, long bitSize) {
    PartialSpan {
      checkArgument(bitSize > 0);
    }

    /**
     * Returns whether the span is full, i.e., starts at zero both at left-hand side and
     * right-hand-side and its bit size equals the parameter bit size.
     */
    boolean isFullSpan(long fullBitSize) {
      return lhsBitOffset == 0 && rhsTargetBitOffset == 0 && bitSize == fullBitSize;
    }
  }

  /**
   * Resolved right-hand side of assignment with partial span that determines how it should be
   * mapped onto left-hand side after casting/reinterpreting to targett type.
   */
  record ResolvedPartialAssignmentRhs(PartialSpan span, Optional<ResolvedSlice> actual) {
    ResolvedPartialAssignmentRhs {
      checkNotNull(span);
      checkNotNull(actual);
    }
  }

  private final FormulaEncodingWithPointerAliasingOptions options;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final BitvectorFormulaManagerView bvmgr;

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final MemoryRegionManager regionMgr;

  private final AddressHandler addressHandler;

  /**
   * Creates a new AssignmentFormulaHandler.
   *
   * @param pConv The C to SMT formula converter.
   * @param pEdge The current edge of the CFA (for logging purposes).
   * @param pSsa The SSA map.
   * @param pPts The underlying set of pointer targets.
   * @param pConstraints Additional constraints.
   * @param pErrorConditions Additional error conditions.
   */
  AssignmentFormulaHandler(
      CToFormulaConverterWithPointerAliasing pConv,
      CFAEdge pEdge,
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
    bvmgr = fmgr.getBitvectorFormulaManager();

    edge = pEdge;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    regionMgr = pRegionMgr;

    addressHandler = new AddressHandler(pConv, pSsa, pConstraints, pErrorConditions, pRegionMgr);
  }

  /**
   * Uses the underlying heap to make the slice assignment to the left-hand side from the list of
   * partial right-hand sides.
   *
   * @param lhsResolved Already resolved left-hand side.
   * @param targetType Original target type to which each partial right-hand side is cast or
   *     reintepreted before further processing.
   * @param rhsList List of already resolved partial right-hand sides.
   * @param assignmentOptions Assignment options.
   * @param conditionFormula An SMT condition for the assignment to be made; if it does not hold,
   *     the previous value is retained.
   * @param useQuantifiers Whether quantifiers are used, which may impact handling in heaps.
   * @param pattern Pointer-target pattern used for UFs finishing. If null, UF finishing is not
   *     used.
   * @return The Boolean formula describing the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assignment making.
   */
  BooleanFormula assignResolvedSlice(
      ResolvedSlice lhsResolved,
      CType targetType,
      List<ResolvedPartialAssignmentRhs> rhsList,
      AssignmentOptions assignmentOptions,
      BooleanFormula conditionFormula,
      boolean useQuantifiers,
      @Nullable PointerTargetPattern pattern)
      throws UnrecognizedCodeException, InterruptedException {

    CType lhsType = lhsResolved.type();

    if (lhsResolved.expression().isNondetValue()) {
      // can only occur due to non-supported bit-fields, warn and return
      conv.logger.logfOnce(
          Level.WARNING,
          "%s: Ignoring assignment to %s because bit fields are currently not fully supported",
          edge.getFileLocation(),
          lhsType);
      return bfmgr.makeTrue();
    }

    // construct the RHS expression
    ResolvedSlice rhsResult =
        resolveCompleteRhs(lhsResolved, targetType, rhsList, assignmentOptions);

    // determine whether it is desired to use old SSA indices because we are doing
    // an initialization assignment
    // unaliased locations do not get any improvement from using old SSA indices
    final Location lhsLocation = lhsResolved.expression().asLocation();
    final boolean useOldSSAIndices =
        assignmentOptions.useOldSSAIndicesIfAliased() && lhsLocation.isAliased();

    // if we are using UF heap and this is not first assignment (which can be done with old SSA
    // indices), we need to get the updated regions from assignment for finishing assignments
    Set<MemoryRegion> updatedRegions =
        useOldSSAIndices || options.useArraysForHeap() ? null : new HashSet<>();

    // perform the actual destructive assignment
    // the RHS result contains both the RHS expression and RHS type
    BooleanFormula result =
        makeSimpleDestructiveAssignment(
            lhsType,
            rhsResult.type(),
            lhsLocation,
            rhsResult.expression(),
            assignmentOptions.useOldSSAIndicesIfAliased()
                && lhsResolved.expression().isAliasedLocation(),
            updatedRegions,
            conditionFormula,
            useQuantifiers);

    // we are using UF heap and have a pointer-target pattern, we need to finish the assignments
    // otherwise, the heap with new SSA index would only contain
    // the new assignment and not retain any other assignments
    if (pattern != null) {
      finishAssignmentsForUF(lhsResolved.type(), lhsLocation.asAliased(), pattern, updatedRegions);
    }

    return result;
  }

  /**
   * Constructs the complete right-hand-side expression from partial resolved right-hand-sides and
   * previous resolved left-hand-side, together with the its type to be considered.
   *
   * <p>This is done by first casting / reinterpreting (according to {@link AssignmentOptions}) each
   * partial RHS to the target type. After that, the interesting part of RHS is extracted and
   * inserted to the correct place in LHS-sized formula. Since there might be some parts of LHS that
   * do not correspond to any partial RHS, these are copied from previous LHS value.
   *
   * <p>The resolved RHS type only differs from the resolved LHS type in the special case of a
   * full-span array-or-function-to-pointer assignment, in which case the original type is returned
   * within the result.
   *
   * @param lhs Resolved left-hand side.
   * @param targetType Original target type to which each partial right-hand side is cast or
   *     reintepreted before further processing.
   * @param rhsList List of partial resolved right-hand-sides.
   * @param assignmentOptions Assignment options.
   * @return Resolved right-hand-side expression to be assigned.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  private ResolvedSlice resolveCompleteRhs(
      ResolvedSlice lhs,
      CType targetType,
      List<ResolvedPartialAssignmentRhs> rhsList,
      AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException {

    // in this function, we consider "RHS formula" to be the resolved formula of a single part
    // before any span consideration; "partial RHS formula" to be such a formula after extracting
    // and inserting into an LHS-sized formula, the other bits being zeroed; and "whole RHS formula"
    // to be a bit-or of all such partial RHS formulas

    CType lhsType = lhs.type();
    long targetBitSize = typeHandler.getBitSizeof(targetType);
    long lhsBitSize = typeHandler.getBitSizeof(lhsType);

    // handle the special case of full-span array-or-function-to-pointer assignment
    if (lhs.type() instanceof CPointerType && rhsList.size() == 1) {
      // we now know it is a something-to-pointer assignment
      final ResolvedPartialAssignmentRhs rhs = rhsList.get(0);
      final PartialSpan rhsSpan = rhs.span();

      if (rhsSpan.isFullSpan(lhsBitSize)
          && lhsBitSize == targetBitSize
          && rhs.actual().isPresent()) {
        // we now know it is a full-span something-to-pointer assignment
        ResolvedSlice rhsResolvedSlice = rhs.actual().orElseThrow();
        if (rhsResolvedSlice.type() instanceof CArrayType
            || rhsResolvedSlice.type() instanceof CFunctionType) {
          // OK, this is the special case, full-span array-or-function-to-pointer assignment
          // return the RHS resolved slice, which has the array type
          return rhsResolvedSlice;
        }
      }
    }
    // not the special case, the returned RHS type is the LHS type
    final CType resultType = lhs.type();

    // track ranges of LHS which have been already filled
    RangeSet<Long> lhsRangeSet = TreeRangeSet.create();

    // initialize the complete formula to null as it is not trivial
    // to create a zero-filled formula of given C type
    BitvectorFormula completeRhsFormula = null;

    // for each partial RHS, insert the part into the correct place in the complete formula
    for (ResolvedPartialAssignmentRhs rhs : rhsList) {

      PartialSpan rhsSpan = rhs.span();

      // add to lhs range set
      long lhsOffset = rhsSpan.lhsBitOffset();
      long lhsAfterEnd = rhsSpan.lhsBitOffset() + rhsSpan.bitSize();
      lhsRangeSet.add(Range.closedOpen(lhsOffset, lhsAfterEnd));

      // if resolved RHS is nondet, treat it as a nondet value with target type
      // this means there is now only one way to represent a nondet value
      ResolvedSlice rhsResolved =
          rhs.actual().orElse(new ResolvedSlice(Value.nondetValue(), targetType));

      final Optional<BitvectorFormula> partialRhsFormula;

      if (assignmentOptions.conversionType() == AssignmentOptions.ConversionType.BYTE_REPEAT) {
        // treat repeat conversion specially to avoid constructing target-type-sized bitvector
        // we only need the LHS-sized bitvector after span extraction
        partialRhsFormula = convertByteRepeatRhs(lhs.type(), rhs.span(), rhsResolved);
      } else {
        // convert RHS expression to target type
        Expression targetTypeRhsExpression =
            convertResolved(assignmentOptions.conversionType(), targetType, rhsResolved);

        // get the RHS formula for low-level manipulation
        Optional<Formula> rhsFormula =
            addressHandler.getOptionalValueFormula(targetTypeRhsExpression, targetType, false);
        if (rhsFormula.isEmpty()) {
          // nondet partial formula, make whole assignment nondet
          partialRhsFormula = Optional.empty();
        } else {
          // handle full assignments without complications: reinterpret from targetType to
          // lhsResolved.type and return the resulting expression
          if (rhsSpan.lhsBitOffset() == 0
              && rhsSpan.rhsTargetBitOffset() == 0
              && rhsSpan.bitSize() == lhsBitSize
              && lhsBitSize == targetBitSize) {
            return new ResolvedSlice(
                convertResolved(
                    AssignmentOptions.ConversionType.REINTERPRET,
                    lhs.type(),
                    new ResolvedSlice(targetTypeRhsExpression, targetType)),
                resultType);
          }

          // construct the partial RHS formula in a separate function
          partialRhsFormula =
              Optional.of(
                  constructPartialRhsFormula(
                      lhsBitSize, targetType, rhsSpan, rhsFormula.orElseThrow()));
        }
      }

      if (partialRhsFormula.isEmpty()) {
        // nondet rhs part, make the whole rhs result nondeterministic
        return new ResolvedSlice(Value.nondetValue(), resultType);
      }

      // bit-or with other parts, all of them are LHS-sized
      completeRhsFormula =
          (completeRhsFormula != null)
              ? fmgr.makeOr(completeRhsFormula, partialRhsFormula.orElseThrow())
              : partialRhsFormula.orElseThrow();
    }

    // completeRhsFormula now contains all partial RHS and is LHS-sized (or null)
    // however, there is a possibility that some ranges of bits were not assigned
    // so we need to retain those bits from previous LHS

    // to find out which ranges should be retained, we complement the assigned ranges
    // and union with the range encompassing completeRhsFormula [0, lhsBitSize)
    RangeSet<Long> retainedRangeSet =
        lhsRangeSet.complement().subRangeSet(Range.closedOpen(0L, lhsBitSize));

    if (!retainedRangeSet.isEmpty()) {
      // there are some retained bits from previous LHS
      // get previous LHS formula
      Optional<Formula> previousLhsFormula =
          addressHandler.getOptionalValueFormula(lhs.expression(), lhs.type(), false);
      if (previousLhsFormula.isEmpty()) {
        // some bits from previous LHS are retained in current RHS, but previous LHS is nondet
        // make current RHS nondet as well
        return new ResolvedSlice(Value.nondetValue(), resultType);
      }

      // reinterpret the previous LHS formula to bitvector
      BitvectorFormula bitvectorPreviousLhsFormula =
          conv.makeValueReinterpretationToBitvector(lhs.type(), previousLhsFormula.orElseThrow());
      // for all retained ranges, retain previous LHS values in the complete RHS formula
      for (Range<Long> retainedRange : retainedRangeSet.asRanges()) {
        if (retainedRange.isEmpty()) {
          continue;
        }
        // construct the partial RHS formula in a separate function
        BitvectorFormula partialRhsFormula =
            constructPartialRhsFromPreviousLhsFormula(
                bitvectorPreviousLhsFormula, lhsBitSize, retainedRange);
        // bit-or with other parts, all of them are LHS-sized
        completeRhsFormula =
            (completeRhsFormula != null)
                ? bvmgr.or(completeRhsFormula, partialRhsFormula)
                : partialRhsFormula;
      }
    }

    // the complete RHS formula is now definitely non-null as long as the type is non-zero-sized
    assert (completeRhsFormula != null);

    // reinterpret from LHS-sized bitvector to the actual LHS type
    Formula targetTypeCompleteRhsFormula =
        conv.makeValueReinterpretationFromBitvector(lhs.type(), completeRhsFormula);

    // return the complete RHS formula
    return new ResolvedSlice(Value.ofValue(targetTypeCompleteRhsFormula), resultType);
  }

  /**
   * Construct an left-hand-side-sized bitvector formula containing the part of given
   * right-hand-side formula in a given place, as determined by {@code rhsSpan}. All other bits are
   * zeroed.
   *
   * @param lhsBitSize LHS bit size.
   * @param targetType RHS target type. It is assumed the RHS formula was already cast /
   *     reinterpreted to this type.
   * @param span Span which gives the offsets and size of the interesting part of RHS.
   * @param rhsFormula Supplied RHS formula. Does not need to be a bitvector formula.
   * @return LHS-sized bitvector formula containing the part of {@code rhsFormula} as determined by
   *     {@code span}, all other bits filled with zeros.
   */
  private BitvectorFormula constructPartialRhsFormula(
      long lhsBitSize, CType targetType, PartialSpan span, Formula rhsFormula) {

    // make the formula a bitvector formula
    BitvectorFormula bitvectorRhsFormula =
        conv.makeValueReinterpretationToBitvector(targetType, rhsFormula);

    // extract the interesting part
    BitvectorFormula extractedFormula =
        bvmgr.extract(
            bitvectorRhsFormula,
            (int) (span.rhsTargetBitOffset() + span.bitSize() - 1),
            (int) span.rhsTargetBitOffset());

    // extend to LHS type size
    long numExtendBits = lhsBitSize - span.bitSize();
    BitvectorFormula extendedFormula = bvmgr.extend(extractedFormula, (int) numExtendBits, false);

    // shift left by span.lhsBitOffset()
    BitvectorFormula shiftedFormula =
        bvmgr.shiftLeft(
            extendedFormula,
            bvmgr.makeBitvector(
                FormulaType.getBitvectorTypeWithSize((int) lhsBitSize), span.lhsBitOffset()));

    // return the result
    return shiftedFormula;
  }

  /**
   * Construct an left-hand-side-sized bitvector formula retaining a range of bits in previous LHS
   * formula, setting all other bits to zero.
   *
   * @param bitvectorPreviousLhsFormula Bitvector formula giving the previous LHS value.
   * @param lhsBitSize LHS bit size.
   * @param retainedRange The range determining the bits to retain. The range is assumed to be
   *     closed on bottom and open on top, i.e. [lsb, msb+1).
   * @return LHS-sized bitvector formula
   */
  private BitvectorFormula constructPartialRhsFromPreviousLhsFormula(
      BitvectorFormula bitvectorPreviousLhsFormula, long lhsBitSize, Range<Long> retainedRange) {

    // compute the retained bit offset, size, lsb, msb
    long retainedLsb = retainedRange.lowerEndpoint();
    long retainedBitSize = retainedRange.upperEndpoint() - retainedRange.lowerEndpoint();
    long retainedMsb = retainedRange.upperEndpoint() - 1;

    // extract the range [lsb, msb]
    BitvectorFormula extractedFormula =
        bvmgr.extract(bitvectorPreviousLhsFormula, (int) retainedMsb, (int) retainedLsb);

    // extend back to LHS bit size
    long numExtendBits = lhsBitSize - retainedBitSize;
    BitvectorFormula extendedFormula = bvmgr.extend(extractedFormula, (int) numExtendBits, false);

    // shift left so that lsb is in its correct place again
    BitvectorFormula shiftedFormula =
        bvmgr.shiftLeft(
            extendedFormula,
            fmgr.makeNumber(FormulaType.getBitvectorTypeWithSize((int) lhsBitSize), retainedLsb));

    // return result
    return shiftedFormula;
  }

  /**
   * Convert an expression from resolved array slice to another type.
   *
   * @param conversionType Type of conversion (cast / reinterpret).
   * @param toType The type we are converting to.
   * @param resolved Resolved array slice containing the expression to convert and type we are
   *     converting from.
   * @return Expression from {@code resolved} converted to type {@code toType}.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  private Expression convertResolved(
      AssignmentOptions.ConversionType conversionType, CType toType, ResolvedSlice resolved)
      throws UnrecognizedCodeException {
    // BYTE_REPEAT must be done in convertByteRepeatRhs
    checkArgument(conversionType != AssignmentOptions.ConversionType.BYTE_REPEAT);

    final CType fromType = resolved.type();

    if (toType.equals(fromType)) {
      return resolved.expression();
    }

    Optional<Formula> optionalRhsFormula =
        addressHandler.getOptionalValueFormula(resolved.expression(), fromType, false);
    if (optionalRhsFormula.isEmpty()) {
      // nondeterministic RHS expression has no formula, do not convert
      return resolved.expression();
    }
    Formula rhsFormula =
        addressHandler
            .getOptionalValueFormula(resolved.expression(), fromType, false)
            .orElseThrow();
    return switch (conversionType) {
      case CAST ->
      // cast rhs from rhs type to lhs type
      Value.ofValue(conv.makeCast(fromType, toType, rhsFormula, constraints, edge));
      case REINTERPRET -> {
        // reinterpret rhs from rhs type to lhs type
        final @Nullable Formula reinterpretedFormula =
            conv.makeValueReinterpretation(resolved.type(), toType, rhsFormula);
        // makeValueReinterpretation returns null if reintepretation was not possible, throw in that
        // case
        if (reinterpretedFormula == null) {
          throw new UnrecognizedCodeException(
              String.format(
                  "Reinterpretation from %s to %s not supported", resolved.type(), toType),
              edge);
        }
        yield Value.ofValue(reinterpretedFormula);
      }
      case BYTE_REPEAT -> throw new AssertionError();
    };
  }

  /**
   * Converts right-hand side byte formula to left-hand-side formula by treating the byte as
   * infinitely repeating and selecting the correct span.
   *
   * <p>Note that we are skipping the target type entirely; we can do this as we are not casting.
   *
   * @param lhsType Left-hand-side type we are converting to.
   * @param span Partial span to use when extracting from "infinitely-repeating" byte.
   * @param rhs Resolved array slice containing the expression to convert and type we are converting
   *     from.
   * @return Bitvector formula with left-hand-side type size or empty Optional if the value is
   *     nondeterministic.
   */
  private Optional<BitvectorFormula> convertByteRepeatRhs(
      CType lhsType, PartialSpan span, ResolvedSlice rhs) {

    final CType rhsType = rhs.type();
    Optional<Formula> rhsFormula =
        addressHandler.getOptionalValueFormula(rhs.expression(), rhsType, false);
    if (rhsFormula.isEmpty()) {
      return Optional.empty();
    }

    // reinterpret as bitvector
    final BitvectorFormula bitvectorFormula =
        conv.makeValueReinterpretationToBitvector(rhsType, rhsFormula.orElseThrow());

    final long fromBitSizeof = conv.getBitSizeof(rhsType);
    // the type which we are repeating must be byte-sized
    // addressing would not make any sense otherwise
    verify(fromBitSizeof == conv.machineModel.getSizeofCharInBits());

    // get total starting bit offset
    final long totalExtractBitOffset = span.lhsBitOffset - span.rhsTargetBitOffset;
    // get starting bit offset within byte; since all RHS bytes are the same, no information is lost
    // due to Math.floorMod, this is always non-negative
    final long extractBitInByteOffset =
        Math.floorMod(totalExtractBitOffset, conv.machineModel.getSizeofCharInBits());

    final long minimumRepeatedBitSize = extractBitInByteOffset + span.bitSize;

    // repeat rhs as necessary to get the minimum-size bitvector formula to extract from
    long numRepeats = (minimumRepeatedBitSize / fromBitSizeof);
    if (minimumRepeatedBitSize % fromBitSizeof != 0) {
      numRepeats += 1;
    }
    BitvectorFormula repeatedFormula = bvmgr.makeBitvector(0, 0);
    for (long i = 0; i < numRepeats; ++i) {
      repeatedFormula = bvmgr.concat(repeatedFormula, bitvectorFormula);
    }

    final long extractLsb = extractBitInByteOffset;
    final long extractMsb = extractBitInByteOffset + span.bitSize - 1;

    final BitvectorFormula extractedFormula =
        bvmgr.extract(repeatedFormula, (int) extractMsb, (int) extractLsb);

    // do not convert from bitvector type
    return Optional.of(extractedFormula);
  }

  /**
   * Finish assignments for uninterpreted function heap.
   *
   * <p>Needed when using uninterpreted function heap with unrolled assignments as after an
   * assignment, the heap with new SSA index would only contain the new assignment and not retain
   * any other assignments
   *
   * @param lvalueType The LHS type of the current assignment.
   * @param lvalue The written-to aliased location.
   * @param pattern The pattern matching the (potentially) written heap cells.
   * @param updatedRegions The set of regions which were affected by the assignment.
   * @throws InterruptedException If a shutdown was requested during finishing assignments.
   */
  void finishAssignmentsForUF(
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
    final long size = conv.getSizeof(lvalueType);

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
      final long size,
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
      final long size,
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
      final long size,
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
      final Formula startAddress, final long size, final Set<MemoryRegion> regions)
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
   * Creates a formula for a simple destructive assignment.
   *
   * @param lvalueType The type of the lvalue.
   * @param pRvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @param condition A condition which determines if the assignment is actually done.
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  private BooleanFormula makeSimpleDestructiveAssignment(
      final CType lvalueType,
      final CType pRvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions,
      final BooleanFormula condition,
      final boolean useQuantifiers)
      throws UnrecognizedCodeException {
    // Arrays and functions are implicitly converted to pointers
    CType rvalueType = implicitCastToPointer(pRvalueType);

    checkArgument(isSimpleType(lvalueType));
    checkArgument(isSimpleType(rvalueType));
    assert !(lvalueType instanceof CFunctionType) : "Can't assign to functions";

    final FormulaType<?> targetType = conv.getFormulaTypeFromCType(lvalueType);
    BooleanFormula result;

    Formula rhs;
    if (pRvalueType instanceof CArrayType && rvalue.isAliasedLocation()) {
      // When assigning an array to a pointer, the address of the array is taken
      rhs = rvalue.asAliasedLocation().getAddress();
    } else {
      final Optional<Formula> value =
          addressHandler.getOptionalValueFormula(rvalue, rvalueType, false);
      rhs =
          value.isPresent()
              ? conv.makeCast(rvalueType, lvalueType, value.orElseThrow(), constraints, edge)
              : null;
    }

    if (!lvalue.isAliased()) { // Unaliased LHS
      assert !useOldSSAIndices;

      final String targetName = lvalue.asUnaliased().getVariableName();
      final int newIndex = conv.makeFreshIndex(targetName, lvalueType, ssa);

      Formula newVariable = fmgr.makeVariable(targetType, targetName, newIndex);

      if (rhs != null) {
        result = fmgr.assignment(newVariable, rhs);
      } else {
        result = bfmgr.makeTrue();
      }

      // add the condition
      // either the condition holds and the assignment should be done,
      // or the condition does not hold and the previous value should be copied
      final int oldIndex = conv.getIndex(targetName, lvalueType, ssa);
      Formula oldVariable = fmgr.makeVariable(targetType, targetName, oldIndex);

      BooleanFormula retainmentAssignment = fmgr.assignment(newVariable, oldVariable);

      result = conv.bfmgr.ifThenElse(condition, result, retainmentAssignment);

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

      final Formula address = lvalue.asAliased().getAddress();

      if (rhs != null) {
        // use the special quantifier version of pointer assignment if requested

        if (useQuantifiers) {
          result =
              conv.ptsMgr.makeQuantifiedPointerAssignment(
                  targetName,
                  targetType,
                  oldIndex,
                  newIndex,
                  condition,
                  new SMTAddressValue<>(address, rhs));
        } else {
          result =
              conv.ptsMgr.makePointerAssignment(
                  targetName,
                  targetType,
                  oldIndex,
                  newIndex,
                  new SMTHeap.SMTAddressValue<>(address, rhs));
        }
      } else {
        result = bfmgr.makeTrue();
      }

      // add the condition
      // either the condition holds and the assignment should be done,
      // or the condition does not hold and the previous value should be copied
      if (!useQuantifiers) {
        BooleanFormula retainmentAssignment =
            conv.ptsMgr.makeIdentityPointerAssignment(targetName, targetType, oldIndex, newIndex);
        BooleanFormula makeNewAssignment = conv.bfmgr.and(condition, result);
        BooleanFormula retainOldAssignment =
            conv.bfmgr.and(conv.bfmgr.not(condition), retainmentAssignment);

        result = conv.bfmgr.or(makeNewAssignment, retainOldAssignment);
      }
    }

    return result;
  }

  /**
   * Creates a formula for array destructive assignment.
   *
   * <p>Deprecated as non-simple destructive assignments have been superseded by simplifying slice
   * expressions in {@link AssignmentHandler}.
   *
   * @param lvalueArrayType The type of the lvalue.
   * @param rvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @param condition A condition which determines if the assignment is actually done.
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Deprecated
  private BooleanFormula makeDestructiveArrayAssignment(
      final CArrayType lvalueArrayType,
      final CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions,
      final BooleanFormula condition,
      final boolean useQuantifiers)
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
    long offset = 0;
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
                  updatedRegions,
                  condition,
                  useQuantifiers));
      offset += conv.getSizeof(lvalueArrayType.getType());
    }
    return result;
  }

  /**
   * Creates a formula for composite destructive assignment.
   *
   * <p>Deprecated as non-simple destructive assignments have been superseded by simplifying slice
   * expressions in {@link AssignmentHandler}.
   *
   * @param lvalueCompositeType The type of the lvalue.
   * @param rvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @param condition A condition which determines if the assignment is actually done.
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Deprecated
  private BooleanFormula makeDestructiveCompositeAssignment(
      final CCompositeType lvalueCompositeType,
      final CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions,
      final BooleanFormula condition,
      final boolean useQuantifiers)
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
                    updatedRegions,
                    condition,
                    useQuantifiers));
      }
    }
    return result;
  }

  /**
   * Creates a formula for a destructive assignment.
   *
   * <p>Deprecated as non-simple destructive assignments have been superseded by simplifying slice
   * expressions in {@link AssignmentHandler}.
   *
   * @param lvalueType The type of the lvalue.
   * @param rvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @param condition A condition which determines if the assignment is actually done.
   * @param useQuantifiers If the quantifier assignment version should be used.
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  @Deprecated
  BooleanFormula makeDestructiveAssignment(
      CType lvalueType,
      CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions,
      final BooleanFormula condition,
      boolean useQuantifiers)
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
          rvalue,
          useOldSSAIndices,
          updatedRegions,
          condition,
          useQuantifiers);

    } else if (lvalueType instanceof CCompositeType lvalueCompositeType) {
      return makeDestructiveCompositeAssignment(
          lvalueCompositeType,
          rvalueType,
          lvalue,
          rvalue,
          useOldSSAIndices,
          updatedRegions,
          condition,
          useQuantifiers);

    } else { // Simple assignment
      return makeSimpleDestructiveAssignment(
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
}
