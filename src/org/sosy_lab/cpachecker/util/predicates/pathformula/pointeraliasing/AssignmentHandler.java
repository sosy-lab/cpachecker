// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.IsRelevantWithHavocAbstractionVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceFieldAccessModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceIndexVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceResolved;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

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

  private final CSimpleType sizeType;

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

    sizeType = conv.machineModel.getPointerEquivalentSimpleType();
  }

  record ArraySliceSpan(long lhsBitOffset, long rhsBitOffset, long bitSize) {}

  record ArraySliceSpanLhs(ArraySliceExpression actual, CType targetType) {
    ArraySliceSpanLhs(ArraySliceExpression actual, CType targetType) {
      checkNotNull(actual);
      checkNotNull(targetType);
      this.actual = actual;
      this.targetType = targetType;
    }
  }

  record ArraySliceSpanRhs(ArraySliceSpan span, Optional<ArraySliceExpression> actual) {
    ArraySliceSpanRhs(ArraySliceSpan span, Optional<ArraySliceExpression> actual) {
      checkNotNull(span);
      checkNotNull(actual);
      this.span = span;
      this.actual = actual;
    }
  }

  record ArraySliceSpanAssignment(ArraySliceSpanLhs lhs, ArraySliceSpanRhs rhs) {
    ArraySliceSpanAssignment(ArraySliceSpanLhs lhs, ArraySliceSpanRhs rhs) {
      checkNotNull(lhs);
      checkNotNull(rhs);
      this.lhs = lhs;
      this.rhs = rhs;
    }
  }

  record ArraySliceSpanResolved(ArraySliceSpan span, Optional<ArraySliceResolved> actual) {
    ArraySliceSpanResolved(ArraySliceSpan span, Optional<ArraySliceResolved> actual) {
      checkNotNull(span);
      checkNotNull(actual);
      this.span = span;
      this.actual = actual;
    }
  }

  record ArraySliceAssignment(
      ArraySliceExpression lhs,
      Optional<CLeftHandSide> relevancyLhs,
      Optional<ArraySliceExpression> rhs) {
    ArraySliceAssignment(
        ArraySliceExpression lhs,
        Optional<CLeftHandSide> relevancyLhs,
        Optional<ArraySliceExpression> rhs) {
      checkNotNull(lhs);
      checkNotNull(relevancyLhs);
      checkNotNull(rhs);
      this.lhs = lhs;
      this.relevancyLhs = relevancyLhs;
      this.rhs = rhs;
    }
  }

  enum AssignmentConversionType {
    CAST,
    REINTERPRET
  }

  record AssignmentOptions(
      boolean useOldSSAIndicesIfAliased,
      AssignmentConversionType conversionType,
      boolean forceQuantifiers,
      boolean forcePointerAssignment) {
    AssignmentOptions(
        boolean useOldSSAIndicesIfAliased,
        AssignmentConversionType conversionType,
        boolean forceQuantifiers,
        boolean forcePointerAssignment) {
      checkNotNull(conversionType);
      this.useOldSSAIndicesIfAliased = useOldSSAIndicesIfAliased;
      this.conversionType = conversionType;
      this.forceQuantifiers = forceQuantifiers;
      this.forcePointerAssignment = forcePointerAssignment;
    }
  }

  BooleanFormula handleSliceAssignments(
      List<ArraySliceAssignment> pAssignments, final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {

    List<ArraySliceAssignment> assignments = new ArrayList<>(pAssignments);

    // apply relevancy of left-hand side and make canonical
    assignments =
        assignments.stream()
            .filter(
                assignment ->
                    assignment
                        .relevancyLhs
                        .map(relevancyLhs -> conv.isRelevantLeftHandSide(relevancyLhs))
                        .orElse(true))
            .map(
                assignment ->
                    new ArraySliceAssignment(
                        assignment.lhs.constructCanonical(),
                        assignment.relevancyLhs,
                        assignment.rhs().map(rhsSlice -> rhsSlice.constructCanonical())))
            .toList();

    // apply Havoc abstraction: if Havoc abstraction is turned on
    // and rhs is not relevant, make it nondeterministic
    if (conv.options.useHavocAbstraction()) {
      assignments =
          assignments.stream()
              .map(
                  assignment -> {
                    // the Havoc relevant visitor does not care about subscripts and fields,
                    // we can just test for relevancy of the base
                    if (assignment.rhs.isEmpty()) {
                      // already nondeterministic
                      return assignment;
                    }
                    IsRelevantWithHavocAbstractionVisitor havocVisitor = new IsRelevantWithHavocAbstractionVisitor(conv);
                    if (assignment.rhs.get().getBase().accept(havocVisitor)) {
                      // relevant
                      return assignment;
                    }
                    // havoc by making rhs nondeterministic
                    return new ArraySliceAssignment(
                        assignment.lhs, assignment.relevancyLhs, Optional.empty());
                  })
              .toList();
    }

    // generate lhs and rhs bases

    Map<CRightHandSide, ArraySliceResolved> resolvedLhsBases = new HashMap<>();
    Map<CRightHandSide, ArraySliceResolved> resolvedRhsBases = new HashMap<>();
    List<CompositeField> rhsAddressedFields = new ArrayList<>();

    for (ArraySliceAssignment assignment : assignments) {
      // resolve lhs base

      final CExpressionVisitorWithPointerAliasing lhsBaseVisitor =
          new CExpressionVisitorWithPointerAliasing(
              conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);

      CRightHandSide lhsBase = assignment.lhs.getBase();
      ArraySliceResolved resolvedLhsBase = resolveBase(lhsBase, lhsBaseVisitor);

      // add initialized and used fields of lhs to pointer-target set as essential
      pts.addEssentialFields(lhsBaseVisitor.getInitializedFields());
      pts.addEssentialFields(lhsBaseVisitor.getUsedFields());

      if (assignmentOptions.forcePointerAssignment()) {
        // if the force pointer assignment option is used, lhs must be an array
        // interpret it as a pointer instead
        CType lhsPointerType = CTypes.adjustFunctionOrArrayType(resolvedLhsBase.type());
        resolvedLhsBase = new ArraySliceResolved(resolvedLhsBase.expression(), lhsPointerType);
      }

      resolvedLhsBases.put(lhsBase, resolvedLhsBase);

      if (assignment.rhs.isEmpty()) {
        // no resolution of rhs base or deferred memory handling
        continue;
      }
      ArraySliceExpression rhs = assignment.rhs.get();
      // resolve rhs base
      CRightHandSide rhsBase = rhs.getBase();
      final CExpressionVisitorWithPointerAliasing rhsBaseVisitor =
          new CExpressionVisitorWithPointerAliasing(
              conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
      ArraySliceResolved resolvedRhsBase = resolveBase(rhsBase, rhsBaseVisitor);

      // add initialized and used fields of rhs to pointer-target set as essential
      pts.addEssentialFields(rhsBaseVisitor.getInitializedFields());
      pts.addEssentialFields(rhsBaseVisitor.getUsedFields());

      // prepare to add addressed fields of rhs to pointer-target set after assignment
      rhsAddressedFields.addAll(rhsBaseVisitor.getAddressedFields());

      resolvedRhsBases.put(rhsBase, resolvedRhsBase);

      // apply the deferred memory handler: if there is a malloc with void* type, the allocation
      // can be deferred until the assignment that uses the value; the allocation type can then be
      // inferred from assignment lhs type
      if (conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) {

        // the deferred memory handler does not care about actual subscript values, we can use dummy resolved expressions
        // it is necessary that there are no modifiers after CFunctionCallExpression base in assignments
        CRightHandSide lhsDummy = assignment.lhs.getDummyResolvedExpression(sizeType);
        CRightHandSide rhsDummy = rhs.getDummyResolvedExpression(sizeType);
        CType lhsType = typeHandler.getSimplifiedType(lhsDummy);

        if (assignmentOptions.forcePointerAssignment()) {
          // if the force pointer assignment option is used, lhs must be an array
          // interpret it as a pointer instead
          lhsType = CTypes.adjustFunctionOrArrayType(lhsType);
        }

        // we have everything we need, call memory handler
        // rhs expression is only used when rhs is CFunctionCallExpression which can have no modifiers in assignments
        // so we can substitute resolvedRhsBase.expression()
        DynamicMemoryHandler memoryHandler =
          new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);
        memoryHandler.handleDeferredAllocationsInAssignment(
            (CLeftHandSide) lhsDummy,
            rhsDummy,
            resolvedRhsBase.expression(),
            lhsType,
            lhsBaseVisitor.getLearnedPointerTypes(),
            rhsBaseVisitor.getLearnedPointerTypes());
      }
    }


    // make span assignments from assignments and convert them to progenitor span assignments
    List<ArraySliceSpanAssignment> progenitorSpanAssignments = new ArrayList<>();

    for (ArraySliceAssignment assignment : assignments) {
        // to initialize the span size, we need to know the type after potential casting
        // this is usually the type of lhs, but if pointer assignment is being forced,
        // it must be adjusted to pointer

        CType targetType =
            typeHandler.simplifyType(assignment.lhs.getFullExpressionType());
        if (assignmentOptions.forcePointerAssignment) {
          targetType = CTypes.adjustFunctionOrArrayType(targetType);
        }

        ArraySliceSpanLhs spanLhs = new ArraySliceSpanLhs(assignment.lhs, targetType);

        long targetBitSize = typeHandler.getBitSizeof(targetType);
        ArraySliceSpanRhs spanRhs =
            new ArraySliceSpanRhs(
                new ArraySliceSpan(0, 0, targetBitSize), assignment.rhs);

        // construct span assignment
        ArraySliceSpanAssignment spanAssignment =
            new ArraySliceSpanAssignment(spanLhs, spanRhs);

        // convert span assignment to progenitor
        progenitorSpanAssignments.add(convertSliceAssignmentToProgenitor(spanAssignment));
    }

    // generate simple slice assignments to resolve assignments to structures and arrays
    Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> simpleAssignmentMultimap =
        ArrayListMultimap.create();

    for (ArraySliceSpanAssignment spanAssignment : progenitorSpanAssignments) {
      if (assignmentOptions.forcePointerAssignment) {
        // actual assignment type is pointer, which is already simple
        simpleAssignmentMultimap.put(spanAssignment.lhs, spanAssignment.rhs);
      } else {
        generateSimpleSliceAssignments(spanAssignment, simpleAssignmentMultimap);
      }
    }

    // hand over to quantifier handler
    AssignmentQuantifierHandler assignmentQuantifierHandler =
        new AssignmentQuantifierHandler(
            conv,
            edge,
            function,
            ssa,
            pts,
            constraints,
            errorConditions,
            regionMgr,
            assignmentOptions,
            resolvedLhsBases,
            resolvedRhsBases);

    BooleanFormula result =
        assignmentQuantifierHandler.assignSimpleSlices(simpleAssignmentMultimap);

    // add addressed fields of rhs to pointer-target set
    for (final CompositeField field : rhsAddressedFields) {
      pts.addField(field);
    }

    return result;
  }

  private ArraySliceResolved resolveBase(
      CRightHandSide base, CExpressionVisitorWithPointerAliasing visitor)
      throws UnrecognizedCodeException {
    CType lhsBaseType = typeHandler.getSimplifiedType(base);
    Expression lhsBaseExpression = base.accept(visitor);
    return new ArraySliceResolved(lhsBaseExpression, lhsBaseType);
  }

  private ArraySliceSpanAssignment convertSliceAssignmentToProgenitor(
      ArraySliceSpanAssignment assignment) {

    // we assume assignment is already canonical

    // split the canonical lhs modifiers into progenitor modifiers and trailing field accesses
    // e.g. with (*x).a.b[0].c.d, split into (*x).a.b[0] and .c.d
    // the head is the progenitor from which we will be assigning to span

    ImmutableList<ArraySliceModifier> lhsModifiers = assignment.lhs.actual.getModifiers();

    // iterate in reverse to split to head and trailing
    List<ArraySliceModifier> progenitorModifiers = new ArrayList<>();

    List<ArraySliceFieldAccessModifier> trailingFieldAccesses = new ArrayList<>();

    boolean stillTrailing = true;

    for (ArraySliceModifier modifier : Lists.reverse(lhsModifiers)) {
      if (stillTrailing && modifier instanceof ArraySliceFieldAccessModifier accessModifier) {
        // add at the start of trailing
        trailingFieldAccesses.add(0, accessModifier);
      } else {
        // we are no longer trailing, add to the start of head
        stillTrailing = false;
        progenitorModifiers.add(0, modifier);
      }
    }

    // construct the progenitor lhs
    ArraySliceExpression progenitorLhs =
        new ArraySliceExpression(assignment.lhs.actual.getBase(), ImmutableList.copyOf(progenitorModifiers));

    // compute the full bit offset from progenitor
    // the parent type of first field access is the progenitor type
    CType parentType = progenitorLhs.getFullExpressionType();
    long bitOffsetFromProgenitor = 0;

    for (ArraySliceFieldAccessModifier access : trailingFieldAccesses) {
      // field access, parent must be composite
      CCompositeType parentCompositeType = (CCompositeType) parentType;

      // add current field access to bit offset from progenitor
      bitOffsetFromProgenitor += typeHandler.getBitOffset(parentCompositeType, access.field());

      // compute the parent type of next access, which is the simplified type of this accessed field
      parentType = typeHandler.getSimplifiedType(access.field());
    }

    ArraySliceSpan originalSpan = assignment.rhs.span;
    ArraySliceSpan spanFromProgenitor = new ArraySliceSpan(
        bitOffsetFromProgenitor + originalSpan.lhsBitOffset,
        originalSpan.rhsBitOffset,
        originalSpan.bitSize
        );

    // now construct the new progenitor assignment with lhs and span modified accordingly
    // rhs does not change, so target type does not change as well
    return new ArraySliceSpanAssignment(
        new ArraySliceSpanLhs(progenitorLhs, assignment.lhs.targetType),
        new ArraySliceSpanRhs(spanFromProgenitor, assignment.rhs.actual));
  }

  private void generateSimpleSliceAssignments(
      ArraySliceSpanAssignment assignment,
      Multimap<ArraySliceSpanLhs, ArraySliceSpanRhs> simpleAssignmentMultimap) {

    CType lhsType = typeHandler.simplifyType(assignment.lhs.actual.getFullExpressionType());

    // if rhs type is nondet, treat is as lhs type
    CType rhsType =
        assignment
            .rhs
            .actual
            .map(rhsSlice -> typeHandler.simplifyType(rhsSlice.getFullExpressionType()))
            .orElse(lhsType);

    if (lhsType instanceof CCompositeType lhsCompositeType) {

      ArraySliceSpan originalSpan = assignment.rhs.span;

        for (CCompositeTypeMemberDeclaration lhsMember : lhsCompositeType.getMembers()) {
          long lhsMemberBitOffset = typeHandler.getBitOffset(lhsCompositeType, lhsMember);
          long lhsMemberBitSize = typeHandler.getBitSizeof(lhsMember.getType());
        final ArraySliceExpression lhsMemberSlice =
            assignment.lhs.actual.withFieldAccess(lhsMember);

          Range<Long> lhsOriginalRange = Range.closedOpen(
              originalSpan.lhsBitOffset,
              originalSpan.lhsBitOffset + originalSpan.bitSize
              );
          Range<Long> lhsMemberRange = Range.closedOpen(lhsMemberBitOffset, lhsMemberBitOffset + lhsMemberBitSize);
          if (!lhsOriginalRange.isConnected(lhsMemberRange)) {
            // the span does not cover this member
            continue;
          }

          Range<Long> lhsIntersectionRange = lhsOriginalRange.intersection(lhsMemberRange);
          if (lhsIntersectionRange.isEmpty()) {
            // the span does not cover this member
            continue;
          }

          // create the assignment to member which is referenced to the member
          long intersectionMemberReferencedLhsBitOffset = lhsIntersectionRange.lowerEndpoint() - lhsMemberBitOffset;
          long intersectionBitSize = lhsIntersectionRange.upperEndpoint() - lhsIntersectionRange.lowerEndpoint();

          ArraySliceSpanAssignment memberAssignment;

        // go into rhs as well if bit offsets and types are the same
        if (originalSpan.lhsBitOffset == originalSpan.rhsBitOffset && lhsType.equals(rhsType)) {
          // types and offsets are equal, go into rhs as well

          // the offsets will remain the same for lhs and rhs
          ArraySliceSpan memberSpan =
              new ArraySliceSpan(
                  intersectionMemberReferencedLhsBitOffset,
                  intersectionMemberReferencedLhsBitOffset,
                  intersectionBitSize);

          // go into rhs if not nondet
          final Optional<ArraySliceExpression> memberRhsSlice =
              assignment.rhs.actual.map(rhsSlice -> rhsSlice.withFieldAccess(lhsMember));

          ArraySliceSpanRhs memberRhs = new ArraySliceSpanRhs(memberSpan, memberRhsSlice);

          // target type is now member type
          CType memberTargetType = typeHandler.getSimplifiedType(lhsMember);

          memberAssignment =
              new ArraySliceSpanAssignment(
                  new ArraySliceSpanLhs(lhsMemberSlice, memberTargetType), memberRhs);

          } else {
          // types or offsets are not equal, do not go into rhs, just get the right spans

          // the rhs offset is still referenced to rhs which does not change, but the intersection
          // may start after original, so add intersection lhs bit offset and subtract original
          // lhs bit offset
          long intersectionRhsBitOffset =
              originalSpan.rhsBitOffset
                  + lhsIntersectionRange.lowerEndpoint()
                  - lhsOriginalRange.lowerEndpoint();

          ArraySliceSpan memberSpan =
              new ArraySliceSpan(
                  intersectionMemberReferencedLhsBitOffset,
                  intersectionRhsBitOffset,
                  intersectionBitSize);
          ArraySliceSpanRhs memberRhs = new ArraySliceSpanRhs(memberSpan, assignment.rhs.actual);

          // target type does not change
          memberAssignment =
              new ArraySliceSpanAssignment(
                  new ArraySliceSpanLhs(lhsMemberSlice, assignment.lhs.targetType), memberRhs);
          }
        generateSimpleSliceAssignments(memberAssignment, simpleAssignmentMultimap);
      }
    } else if (lhsType instanceof CArrayType lhsArrayType) {
      @Nullable CExpression lhsArrayLength = lhsArrayType.getLength();
      if (lhsArrayLength == null) {
        // TODO: add flexible array member assignment, tracking the length from malloc
        conv.logger.logfOnce(
            Level.WARNING,
            "%s: Ignoring slice assignment to flexible array member %s as they are not well-supported",
            edge.getFileLocation(),
            lhsArrayType);
        return;
      }

      ArraySliceSpan originalSpan = assignment.rhs.span;

      if (!lhsType.equals(rhsType)) {
        // we currently do not assign to array types from different types as that would ideally
        // require spans
        // to support quantification, which would be problematic
        // it should be only required for cases of unions containing arrays
        conv.logger.logfOnce(
            Level.WARNING,
            "%s: Ignoring assignment to array type %s from other type %s",
            edge.getFileLocation(),
            lhsArrayType,
            rhsType);
        return;
      }
      if (originalSpan.lhsBitOffset != 0
          || originalSpan.rhsBitOffset != 0
          || originalSpan.bitSize != typeHandler.getBitSizeof(lhsArrayType)) {
        // we currently do not assign for non-full spans as it would not be trivial
        conv.logger.logfOnce(
            Level.WARNING,
            "%s: Ignoring assignment to array type %s with non-full span",
            edge.getFileLocation(),
            lhsArrayType);
        return;
      }

      // add an assignment of every element of array using a quantified variable
      ArraySliceIndexVariable indexVariable = new ArraySliceIndexVariable(lhsArrayType.getLength());
      ArraySliceExpression elementLhs = assignment.lhs.actual.withIndex(indexVariable);
      Optional<ArraySliceExpression> elementRhs =
          assignment.rhs.actual.map(rhsSlice -> rhsSlice.withIndex(indexVariable));

      CType elementType = typeHandler.simplifyType(lhsArrayType.getType());
      // full span
      ArraySliceSpan elementSpan =
          new ArraySliceSpan(0, 0, typeHandler.getBitSizeof(elementType));
      ArraySliceSpanRhs elementSpanRhs = new ArraySliceSpanRhs(elementSpan, elementRhs);
      // target type is now element type
      ArraySliceSpanAssignment elementAssignment =
          new ArraySliceSpanAssignment(
              new ArraySliceSpanLhs(elementLhs, elementType), elementSpanRhs);
      generateSimpleSliceAssignments(elementAssignment, simpleAssignmentMultimap);

    } else {
      // already simple, just add the assignment to simple assignments
      simpleAssignmentMultimap.put(assignment.lhs, assignment.rhs);
    }
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

    // cast normally, use old SSA indices if aliased
    AssignmentOptions assignmentOptions =
        new AssignmentOptions(true, AssignmentConversionType.CAST, false, false);

    if (options.useQuantifiersOnArrays()
        && (declarationType instanceof CArrayType arrayType)
        && !assignments.isEmpty()) {
      // try to make a single slice assignment out of the assignments

      OptionalInt arrayLength = arrayType.getLengthAsInt();

      CExpressionAssignmentStatement firstAssignment = assignments.get(0);

      // we can visit lhs and rhs multiple times without side effects
      // as there is no CFunctionCallExpression visit possible
      final CExpressionVisitorWithPointerAliasing rhsVisitor =
          new CExpressionVisitorWithPointerAliasing(
              conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
      final Expression rhsValue = firstAssignment.getRightHandSide().accept(rhsVisitor);

      final CExpressionVisitorWithPointerAliasing lhsVisitor =
          new CExpressionVisitorWithPointerAliasing(
              conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
      final Location lhsLocation = variable.accept(lhsVisitor).asLocation();

      if (arrayLength.isPresent()
          && arrayLength.getAsInt() == assignments.size()
          && rhsValue.isValue()
          && checkEqualityOfInitializers(assignments, rhsVisitor)
          && lhsLocation.isAliased()) {
        // there is an initializer for every array element and all of them are the same
        // make a single slice assignment over the array length
        CArraySubscriptExpression firstAssignmentLeftSide =
            (CArraySubscriptExpression) firstAssignment.getLeftHandSide();
        CLeftHandSide wholeAssignmentLeftSide =
            (CLeftHandSide) firstAssignmentLeftSide.getArrayExpression();

        ArraySliceExpression sliceLhs =
            new ArraySliceExpression(wholeAssignmentLeftSide)
                .withIndex(new ArraySliceIndexVariable(arrayType.getLength()));
        ArraySliceExpression sliceRhs =
            new ArraySliceExpression(firstAssignment.getRightHandSide());
        ArraySliceAssignment sliceAssignment =
            new ArraySliceAssignment(
                sliceLhs, Optional.of(firstAssignmentLeftSide), Optional.of(sliceRhs));
        return handleSliceAssignments(ImmutableList.of(sliceAssignment), assignmentOptions);
      }
    }

    // normal initializer handling, build all initialization assignments

    ImmutableList.Builder<ArraySliceAssignment> builder =
        ImmutableList.<ArraySliceAssignment>builder();
    for (CExpressionAssignmentStatement assignment : assignments) {
      ArraySliceExpression lhs = new ArraySliceExpression(assignment.getLeftHandSide());
      ArraySliceExpression rhs = new ArraySliceExpression(assignment.getRightHandSide());
      builder.add(
          new ArraySliceAssignment(
              lhs, Optional.of(assignment.getLeftHandSide()), Optional.of(rhs)));
    }
    return handleSliceAssignments(builder.build(), assignmentOptions);
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
}
