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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.stream.Stream;
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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.IsRelevantWithHavocAbstractionVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.PartialSpan;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentQuantifierHandler.PartialAssignment;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentQuantifierHandler.PartialAssignmentLhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentQuantifierHandler.PartialAssignmentRhs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.ResolvedSlice;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceFieldAccessModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceVariable;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * High-level handler for assignments which should be used when assignments should be made.
 *
 * <p>Slice assignments are used, which means that after the LHS and RHS bases, quantified indexing
 * and further field accesses may be done. For example, with {@code int a[3], b[3]}, the function
 * {@code memcpy(&b, &a, size * sizeof(int))} can be encoded as {@code b[i] = a[i]} with slice
 * variable {@code 0 <= i < size}. Idiomatic loop assignments such as {@code for (i=0; i < size;
 * ++i) b[i] = a[i]} could also be transformed into slice assignments in the future.
 *
 * <p>The entry point is {@link #assign(List)}, which takes care of everything necessary for proper
 * assignment handling.
 *
 * <p>This class is mostly concerned with converting slice assignments to simple partial slice
 * assignments. Partial slice assignments allow for setting a single left-hand side from spans of
 * bits of right-hand sides (as documented in {@link PartialAssignment}). A partial assignment is
 * simple if its left-hand side type is not an array or a compound type. This allows for easier
 * assignment handling later on in {@link AssignmentQuantifierHandler}.
 *
 * <p>The class is also responsible for union handling by conversion of assignments to partial
 * assignments of progenitor type (i.e. type before trailing field accesses) before converting them
 * to simple partial assignments. This can increase soundness for some cases of unions when using
 * multiple heaps, but does not guarantee it in entirety.
 *
 * @see SliceExpression
 * @see AssignmentQuantifierHandler
 * @see AssignmentFormulaHandler
 * @see MemoryManipulationFunctionHandler
 */
class AssignmentHandler {

  /**
   * Stores the information about a single slice assignment. Both the left-hand side and right-hand
   * side are represented via slice expressions, allowing for assignments that contain quantified
   * variables.
   *
   * <p>The right-hand side is optional. If empty, it is taken to be nondeterministic.
   *
   * <p>If {@code relevancyLhs} is non-empty, the assignment is not performed if not relevant, as
   * decided by {@link CToFormulaConverterWithPointerAliasing#isRelevantLeftHandSide(CLeftHandSide,
   * Optional)}.
   *
   * <p>The quantified variables in LHS and RHS slice expressions must be unresolved. They will be
   * resolved later by {@link AssignmentQuantifierHandler}.
   */
  record SliceAssignment(
      SliceExpression lhs, Optional<CLeftHandSide> relevancyLhs, Optional<SliceExpression> rhs) {
    SliceAssignment {
      checkNotNull(lhs);
      checkNotNull(relevancyLhs);
      checkNotNull(rhs);
      checkArgument(!lhs.containsResolvedModifiers());
      checkArgument(rhs.isEmpty() || !rhs.orElseThrow().containsResolvedModifiers());
    }

    private SliceAssignment constructCanonical() {
      // make the slice expressions canonical
      return new SliceAssignment(
          lhs.constructCanonical(), relevancyLhs, rhs().map(SliceExpression::constructCanonical));
    }

    private boolean isRelevant(CToFormulaConverterWithPointerAliasing pConv) {
      // if relevancyLhs is empty in some assignment, treat it as relevant
      return relevancyLhs
          .map(
              presentRelevancyLhs ->
                  pConv.isRelevantLeftHandSide(presentRelevancyLhs, rhs.map(SliceExpression::base)))
          .orElse(true);
    }
  }

  /** A helper record for storing a partial assignment which has exactly one right-hand side. */
  private record SingleRhsPartialAssignment(PartialAssignmentLhs lhs, PartialAssignmentRhs rhs) {
    SingleRhsPartialAssignment {
      checkNotNull(lhs);
      checkNotNull(rhs);
    }
  }

  private final FormulaEncodingWithPointerAliasingOptions options;
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
      MemoryRegionManager pRegionMgr,
      AssignmentOptions pAssignmentOptions) {
    conv = pConv;

    typeHandler = pConv.typeHandler;
    options = conv.options;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
    regionMgr = pRegionMgr;

    assignmentOptions = pAssignmentOptions;
  }

  /**
   * Performs assignments to memory heap. Main assignment entry point.
   *
   * <p>The assignments are slice assignments, meaning that they support quantified indexing after a
   * {@link CRightHandSide} base on both the left-hand side and right-hand side.
   *
   * <p>This function handles all things the assignment entails, including computing relevancy,
   * assigning to unionized fields (if there are any), pointer-target set and dynamic memory handler
   * updating, quantifier unrolling or encoding, etc.
   *
   * @param pAssignments Slice assignments to perform.
   * @return The Boolean formula describing to assignments.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assigning.
   */
  BooleanFormula assign(List<SliceAssignment> pAssignments)
      throws UnrecognizedCodeException, InterruptedException {

    // apply LHS relevancy
    Stream<SliceAssignment> assignmentsStream =
        pAssignments.stream().filter(assignment -> assignment.isRelevant(conv));

    // make the slice expressions canonical, moving the trailing field accesses of bases to first
    // modifiers; this results in the same effective assignments, but the base encompasses more
    // fields which may be unionized, paving the way for assigning to relevant simple unionized
    // fields after converting to simple slice assignments
    assignmentsStream = assignmentsStream.map(SliceAssignment::constructCanonical);

    // apply Havoc abstraction: if Havoc abstraction is turned on
    // and LHS is not relevant, make it nondeterministic
    if (conv.options.useHavocAbstraction()) {
      assignmentsStream = assignmentsStream.map(this::applyHavocToAssignment);
    }

    final List<SliceAssignment> assignments = assignmentsStream.toList();

    // resolve LHS and RHS bases here, before converting to simple slice assignments
    // this is needed for two reasons:
    // 1. to avoid visiting LHS and RHS multiple times, which is especially important for
    // CFunctionCallExpression RHS as it may produce side effects
    // 2. to be able to update the DynamicMemoryHandler using the original assignments

    final Map<CRightHandSide, ResolvedSlice> lhsBaseResolutionMap = new HashMap<>();
    final Map<CRightHandSide, ResolvedSlice> rhsBaseResolutionMap = new HashMap<>();
    final List<CompositeField> rhsAddressedFields = new ArrayList<>();

    for (SliceAssignment assignment : assignments) {
      resolveAssignmentBases(
          assignment, lhsBaseResolutionMap, rhsBaseResolutionMap, rhsAddressedFields);
    }

    // note that after resolving the slice bases, we can no longer modify them,
    // otherwise, we would lose the resolution mapping

    // for better soundness, we want to assign not just to the LHS, but to fields that are unionized
    // with it as well; for that reason, we will convert the assignments to partial assignments and
    // move outward from trailing field accesses, so the LHS type of partial assignment is the
    // coarsest possible ("progenitor")
    final List<SingleRhsPartialAssignment> partialAssignments = new ArrayList<>();

    for (SliceAssignment assignment : assignments) {
      // to initialize the span size, we need to know the type after potential casting
      // this is usually the type of LHS, but if pointer assignment or array attachment is being
      // forced, it must be adjusted to pointer
      CType targetType = typeHandler.simplifyType(assignment.lhs.getFullExpressionType());
      if (assignmentOptions.forcePointerAssignmentOrArrayAttachment()) {
        targetType = CTypes.adjustFunctionOrArrayType(targetType);
      }

      // construct the partial assignment as if it is a full assignment from RHS (converted to
      // target type) to LHS
      final PartialAssignmentLhs partialLhs = new PartialAssignmentLhs(assignment.lhs, targetType);
      final long targetBitSize = typeHandler.getBitSizeof(targetType);
      final PartialAssignmentRhs partialRhs =
          new PartialAssignmentRhs(new PartialSpan(0, 0, targetBitSize), assignment.rhs);
      final SingleRhsPartialAssignment partialAssignment =
          new SingleRhsPartialAssignment(partialLhs, partialRhs);

      if (options.useByteArrayForHeap()) {
        // we do not need to convert to progenitor if we are assigning to the byte array, as all
        // types will read and write from the same array
        // however, that does not apply to unaliased locations, so we need to make sure that there
        // are no assignments to them if we want to skip the conversion to progenitor

        boolean assignmentToUnaliasedExists = false;
        for (ResolvedSlice lhsResolvedBase : lhsBaseResolutionMap.values()) {
          assignmentToUnaliasedExists =
              assignmentToUnaliasedExists || lhsResolvedBase.expression().isUnaliasedLocation();
        }

        if (!assignmentToUnaliasedExists) {
          // we do not need to convert to progenitor, unions are resolved implicitly by the byte
          // array
          partialAssignments.add(partialAssignment);
          continue;
        }
      }

      // convert span assignment to progenitor; this will retain the meaning of the assignment,
      // but the LHS type will be the coarsest possible
      final SingleRhsPartialAssignment progenitorPartialAssignment =
          convertPartialAssignmentToProgenitor(partialAssignment);

      partialAssignments.add(progenitorPartialAssignment);
    }

    // generate simple slice assignments to resolve assignments to structures and arrays
    // as we have converted to progenitors, this will recurse into unionized fields within
    // the same coarsest type, and thus resolve these unionized assignments correctly
    // note that one LHS can now correspond to multiple RHS, so we need a multimap
    final ListMultimap<PartialAssignmentLhs, PartialAssignmentRhs> simpleAssignmentMultimap =
        MultimapBuilder.linkedHashKeys().arrayListValues().build();

    for (SingleRhsPartialAssignment partialAssignment : partialAssignments) {
      if (assignmentOptions.forcePointerAssignmentOrArrayAttachment()) {
        // actual assignment type is pointer, which is already simple
        simpleAssignmentMultimap.put(partialAssignment.lhs(), partialAssignment.rhs());
      } else {
        generateSimplePartialAssignments(partialAssignment, simpleAssignmentMultimap);
      }
    }

    // hand over to quantifier handler
    final AssignmentQuantifierHandler assignmentQuantifierHandler =
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
            lhsBaseResolutionMap,
            rhsBaseResolutionMap);
    final BooleanFormula result =
        assignmentQuantifierHandler.assignSimpleSlices(simpleAssignmentMultimap);

    // add addressed fields of rhs to pointer-target set
    for (CompositeField field : rhsAddressedFields) {
      pts.addField(field);
    }

    return result;
  }

  /**
   * In this function, Havoc abstraction is applied onto an assignment.
   *
   * <p>It is determined whether the right side should be Havoc'd. If it should, the new right-hand
   * side is nondeterministic. Otherwise, it is retained.
   *
   * @param assignment The assignment to apply Havoc abstraction to.
   * @return The assignment with applied Havoc abstraction.
   */
  private SliceAssignment applyHavocToAssignment(SliceAssignment assignment) {
    // the Havoc relevant visitor does not care about subscripts and fields,
    // we can just test for relevancy of the base
    if (assignment.rhs.isEmpty()) {
      // already nondeterministic
      return assignment;
    }
    IsRelevantWithHavocAbstractionVisitor havocVisitor =
        new IsRelevantWithHavocAbstractionVisitor(conv);
    if (assignment.rhs.orElseThrow().base().accept(havocVisitor)) {
      // relevant
      return assignment;
    }
    // havoc by making rhs nondeterministic
    return new SliceAssignment(assignment.lhs, assignment.relevancyLhs, Optional.empty());
  }

  /**
   * Resolves bases of a slice assignment and puts the resolutions to the provided maps.
   *
   * <p>Also calls the DynamicMemoryHandler to handle deferred allocations as necessary.
   *
   * @param assignment The assignment to resolve bases of.
   * @param lhsBaseResolutionMap LHS base resolution map to which the LHS base resolutions will be
   *     put.
   * @param rhsBaseResolutionMap RHS base resolution map to which the RHS base resolutions will be
   *     put.
   * @param rhsAddressedFields A list into which RHS addressed fields determined by the RHS visitor
   *     are inserted.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during assigning.
   */
  private void resolveAssignmentBases(
      SliceAssignment assignment,
      Map<CRightHandSide, ResolvedSlice> lhsBaseResolutionMap,
      Map<CRightHandSide, ResolvedSlice> rhsBaseResolutionMap,
      List<CompositeField> rhsAddressedFields)
      throws UnrecognizedCodeException, InterruptedException {
    // resolve LHS base using visitor
    final CExpressionVisitorWithPointerAliasing lhsBaseVisitor =
        new CExpressionVisitorWithPointerAliasing(
            conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
    final CRightHandSide lhsBase = assignment.lhs.base();
    ResolvedSlice resolvedLhsBase = resolveBase(lhsBase, lhsBaseVisitor);

    // add initialized and used fields of LHS base to pointer-target set as essential
    // this is only needed for UF heap
    pts.addEssentialFields(lhsBaseVisitor.getInitializedFields());
    pts.addEssentialFields(lhsBaseVisitor.getUsedFields());

    if (assignmentOptions.forcePointerAssignmentOrArrayAttachment()) {
      // resolved LHS now may have array type, but it must be interpreted as pointer instead
      final CType lhsPointerType = CTypes.adjustFunctionOrArrayType(resolvedLhsBase.type());
      resolvedLhsBase = new ResolvedSlice(resolvedLhsBase.expression(), lhsPointerType);
    }

    // add LHS to resolution map
    lhsBaseResolutionMap.put(lhsBase, resolvedLhsBase);

    if (assignment.rhs.isEmpty()) {
      // assignment RHS is nondeterministic
      // no resolution of RHS base or deferred memory handling
      return;
    }
    final SliceExpression rhs = assignment.rhs.orElseThrow();

    // resolve RHS base using visitor
    final CRightHandSide rhsBase = rhs.base();
    final CExpressionVisitorWithPointerAliasing rhsBaseVisitor =
        new CExpressionVisitorWithPointerAliasing(
            conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
    final ResolvedSlice resolvedRhsBase = resolveBase(rhsBase, rhsBaseVisitor);

    // add initialized and used fields of RHS to pointer-target set as essential
    // this is only needed for UF heap
    pts.addEssentialFields(rhsBaseVisitor.getInitializedFields());
    pts.addEssentialFields(rhsBaseVisitor.getUsedFields());

    // prepare to add addressed fields of RHS to pointer-target set after assignment
    // this is only needed for UF heap
    rhsAddressedFields.addAll(rhsBaseVisitor.getAddressedFields());

    // add RHS to resolution map
    rhsBaseResolutionMap.put(rhsBase, resolvedRhsBase);

    // apply the deferred memory handler: if there is a malloc with void* type, the allocation
    // can be deferred until the assignment that uses the value; the allocation type can then be
    // inferred from assignment lhs type
    if (conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) {

      // the deferred memory handler does not care about actual subscript values, so we can use
      // dummy resolved expressions; it is necessary that there are no modifiers after
      // CFunctionCallExpression base in assignments
      final CRightHandSide lhsDummy = assignment.lhs.getDummyResolvedExpression();
      final CRightHandSide rhsDummy = rhs.getDummyResolvedExpression();
      CType lhsType = typeHandler.getSimplifiedType(lhsDummy);

      if (assignmentOptions.forcePointerAssignmentOrArrayAttachment()) {
        // lhsType may be an array but we have to interpret it as a pointer instead
        lhsType = CTypes.adjustFunctionOrArrayType(lhsType);
      }

      // we have everything we need, call memory handler
      // rhs expression is only used when rhs is CFunctionCallExpression which can have no
      // modifiers in assignments
      // so we can substitute resolvedRhsBase.expression()
      final DynamicMemoryHandler memoryHandler =
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

  /**
   * A helper function to resolve a base using a visitor, returning a {@link ResolvedSlice}.
   *
   * @param base The base to resolve.
   * @param visitor The visitor to use.
   * @return The resolved slice
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  private ResolvedSlice resolveBase(
      CRightHandSide base, CExpressionVisitorWithPointerAliasing visitor)
      throws UnrecognizedCodeException {
    CType lhsBaseType = typeHandler.getSimplifiedType(base);
    Expression lhsBaseExpression = base.accept(visitor);
    return new ResolvedSlice(lhsBaseExpression, lhsBaseType);
  }

  /**
   * Converts partial assignment to progenitor partial assignment.
   *
   * <p>The progenitor partial assignment has the same effect as the original assignment and retains
   * the same base, but LHS trailing field access modifiers are discarded and instead represented by
   * the partial assignment span. This means full LHS type is the coarsest possible, representing
   * assignment not only to the original field, but to unionized fields within the progenitor LHS
   * type as well.
   *
   * @param assignment The partial assignment to convert to progenitor.
   * @return Progenitor partial assignment, retaining the same base and effective assignment.
   */
  private SingleRhsPartialAssignment convertPartialAssignmentToProgenitor(
      SingleRhsPartialAssignment assignment) {

    // we assume assignment is already canonical and split the canonical LHS modifiers
    // into progenitor modifiers and trailing field accesses
    // e.g. with (*x).a.b[0].c.d, split into (*x).a.b[0] and .c.d
    // the head is the progenitor from which we will be assigning to span

    ImmutableList<SliceModifier> lhsModifiers = assignment.lhs().actual().modifiers();

    // split to head and trailing
    // Invariant: lhsModifiers.get(splitPoint) either does not exist or it and all further elements
    // are SliceFieldAccessModifier
    int splitPoint = lhsModifiers.size();
    while (splitPoint > 0 && lhsModifiers.get(splitPoint - 1) instanceof SliceFieldAccessModifier) {
      splitPoint--;
    }
    ImmutableList<SliceModifier> progenitorModifiers = lhsModifiers.subList(0, splitPoint);
    ImmutableList<SliceModifier> trailingFieldAccesses =
        lhsModifiers.subList(splitPoint, lhsModifiers.size());

    // construct the progenitor lhs
    SliceExpression progenitorLhs =
        new SliceExpression(assignment.lhs().actual().base(), progenitorModifiers);

    // compute the full bit offset from progenitor
    // the parent type of first field access is the progenitor type
    CType parentType = progenitorLhs.getFullExpressionType();
    long bitOffsetFromProgenitor = 0;
    for (SliceModifier modifier : trailingFieldAccesses) {
      SliceFieldAccessModifier access = (SliceFieldAccessModifier) modifier; // ensured above

      // field access, parent must be composite
      CCompositeType parentCompositeType = (CCompositeType) parentType;

      // add current field access to bit offset from progenitor
      bitOffsetFromProgenitor += typeHandler.getBitOffset(parentCompositeType, access.field());

      // compute the parent type of next access, which is the simplified type of this accessed field
      parentType = typeHandler.getSimplifiedType(access.field());
    }

    PartialSpan originalSpan = assignment.rhs().span();
    PartialSpan spanFromProgenitor =
        new PartialSpan(
            bitOffsetFromProgenitor + originalSpan.lhsBitOffset(),
            originalSpan.rhsTargetBitOffset(),
            originalSpan.bitSize());

    // now construct the new progenitor assignment with lhs and span modified accordingly
    // rhs does not change, so target type does not change as well
    return new SingleRhsPartialAssignment(
        new PartialAssignmentLhs(progenitorLhs, assignment.lhs().targetType()),
        new PartialAssignmentRhs(spanFromProgenitor, assignment.rhs().actual()));
  }

  /**
   * Generates simple partial assignments from a partial assignment recursively.
   *
   * <p>The simple partial assignments LHS and RHS have non-composite, non-array full types.
   *
   * @param assignment Assignment to generate simple assignments from.
   * @param simpleAssignmentMultimap The multimap to add the generated simple assignments to.
   */
  private void generateSimplePartialAssignments(
      SingleRhsPartialAssignment assignment,
      Multimap<PartialAssignmentLhs, PartialAssignmentRhs> simpleAssignmentMultimap) {

    final CType lhsType =
        typeHandler.simplifyType(assignment.lhs().actual().getFullExpressionType());

    // if rhs type is nondet, treat is as LHS type
    final CType rhsType =
        assignment
            .rhs()
            .actual()
            .map(rhsSlice -> typeHandler.simplifyType(rhsSlice.getFullExpressionType()))
            .orElse(lhsType);

    // hand off to the proper function depending on LHS type
    if (lhsType instanceof CArrayType lhsArrayType) {
      generatePartialAssignmentsForArrayType(
          assignment, simpleAssignmentMultimap, lhsArrayType, rhsType);
    } else if (lhsType instanceof CCompositeType lhsCompositeType) {
      for (CCompositeTypeMemberDeclaration lhsMember : lhsCompositeType.getMembers()) {
        generatePartialAssignmentsForCompositeMember(
            assignment, simpleAssignmentMultimap, lhsCompositeType, rhsType, lhsMember);
      }
    } else {
      // already simple, just add the assignment to simple assignments
      simpleAssignmentMultimap.put(assignment.lhs(), assignment.rhs());
    }
  }

  /**
   * Generates simple partial assignments from a partial assignment with LHS array type.
   *
   * <p>If LHS and RHS types are the same and the assignment span is complete, slices the
   * assignment, i.e., for {@code lhs = rhs}, creates {@code lhs[i] = rhs[i]}, where {@code 0 <= i <
   * size} and {@code size} is the size of the array.
   *
   * <p>If LHS is a flexible array member, the types of LHS and RHS are not the same, or the
   * assignment span is not complete, currently ignores the assignment.
   *
   * @param assignment Assignment to generate simple assignments from.
   * @param simpleAssignmentMultimap The multimap to add the generated simple assignments to.
   * @param lhsArrayType LHS array type.
   * @param rhsType RHS type.
   */
  private void generatePartialAssignmentsForArrayType(
      SingleRhsPartialAssignment assignment,
      Multimap<PartialAssignmentLhs, PartialAssignmentRhs> simpleAssignmentMultimap,
      CArrayType lhsArrayType,
      CType rhsType) {

    final @Nullable CExpression lhsArrayLength = lhsArrayType.getLength();
    if (lhsArrayLength == null) {
      // we currently do not assign to flexible array members as it is complex to implement
      conv.logger.logfOnce(
          Level.WARNING,
          "%s: Ignoring assignment to flexible array member %s as they are not well-supported",
          edge.getFileLocation(),
          lhsArrayType);
      return;
    }

    final PartialSpan originalSpan = assignment.rhs().span();

    if (!lhsArrayType.equals(rhsType)) {
      // we currently do not assign to array types from different types as that would ideally
      // require spans to support quantification, which would be problematic
      // it should be only required for cases of unions containing arrays
      conv.logger.logfOnce(
          Level.WARNING,
          "%s: Ignoring assignment to array type %s from other type %s",
          edge.getFileLocation(),
          lhsArrayType,
          rhsType);
      return;
    }
    if (originalSpan.lhsBitOffset() != 0
        || originalSpan.rhsTargetBitOffset() != 0
        || originalSpan.bitSize() != typeHandler.getBitSizeof(lhsArrayType)) {
      // we currently do not assign for incomplete spans as it would not be trivial
      conv.logger.logfOnce(
          Level.WARNING,
          "%s: Ignoring assignment to array type %s with incomplete span",
          edge.getFileLocation(),
          lhsArrayType);
      return;
    }

    // slice the assignment using a slice variable
    final SliceVariable indexVariable = new SliceVariable(lhsArrayType.getLength());
    final SliceExpression elementLhs = assignment.lhs().actual().withIndex(indexVariable);
    final Optional<SliceExpression> elementRhs =
        assignment.rhs().actual().map(rhsSlice -> rhsSlice.withIndex(indexVariable));

    // full span
    final CType elementType = typeHandler.simplifyType(lhsArrayType.getType());
    final PartialSpan elementSpan = new PartialSpan(0, 0, typeHandler.getBitSizeof(elementType));
    final PartialAssignmentRhs elementSpanRhs = new PartialAssignmentRhs(elementSpan, elementRhs);

    // target type is now element type
    final SingleRhsPartialAssignment elementAssignment =
        new SingleRhsPartialAssignment(
            new PartialAssignmentLhs(elementLhs, elementType), elementSpanRhs);
    generateSimplePartialAssignments(elementAssignment, simpleAssignmentMultimap);
  }

  /**
   * Generates simple partial assignments from a partial assignment with left-hand-side composite
   * type member.
   *
   * <p>If LHS span does not cover the member at least partially, returns. If LHS and RHS types and
   * offsets are the same, field-accesses both of them. If they are different, field-accesses only
   * LHS, "accessing" RHS by adjusting span. This preserves the ability to slice if possible.
   *
   * @param assignment Assignment to generate simple assignments from.
   * @param simpleAssignmentMultimap The multimap to add the generated simple assignments to.
   * @param lhsCompositeType LHS composite type.
   * @param rhsType RHS type.
   * @param lhsMember The LHS member to consider.
   */
  private void generatePartialAssignmentsForCompositeMember(
      SingleRhsPartialAssignment assignment,
      Multimap<PartialAssignmentLhs, PartialAssignmentRhs> simpleAssignmentMultimap,
      CCompositeType lhsCompositeType,
      CType rhsType,
      CCompositeTypeMemberDeclaration lhsMember) {
    final PartialSpan originalSpan = assignment.rhs().span();

    final long lhsMemberBitOffset = typeHandler.getBitOffset(lhsCompositeType, lhsMember);
    final long lhsMemberBitSize = typeHandler.getBitSizeof(lhsMember.getType());
    final SliceExpression lhsMemberSlice = assignment.lhs().actual().withFieldAccess(lhsMember);

    // compare LHS assignment range with member range
    final Range<Long> lhsOriginalRange = originalSpan.asLhsRange();
    final Range<Long> lhsMemberRange =
        Range.closedOpen(lhsMemberBitOffset, lhsMemberBitOffset + lhsMemberBitSize);
    if (!lhsOriginalRange.isConnected(lhsMemberRange)) {
      // the span does not cover this member
      return;
    }

    // get the intersection, it may be empty
    final Range<Long> lhsIntersectionRange = lhsOriginalRange.intersection(lhsMemberRange);
    if (lhsIntersectionRange.isEmpty()) {
      // the span does not cover this member
      return;
    }

    // get the member-referenced offset and member assignment size
    final long intersectionMemberReferencedLhsBitOffset =
        lhsIntersectionRange.lowerEndpoint() - lhsMemberBitOffset;
    final long memberAssignmentBitSize =
        lhsIntersectionRange.upperEndpoint() - lhsIntersectionRange.lowerEndpoint();

    // go into rhs as well if bit offsets and types are the same
    if (originalSpan.lhsBitOffset() == originalSpan.rhsTargetBitOffset()
        && lhsCompositeType.equals(rhsType)) {
      // types and offsets are equal, go into rhs as well

      // the offsets will remain the same for lhs and rhs
      final PartialSpan memberSpan =
          new PartialSpan(
              intersectionMemberReferencedLhsBitOffset,
              intersectionMemberReferencedLhsBitOffset,
              memberAssignmentBitSize);

      // go into rhs if not nondet
      final Optional<SliceExpression> memberRhsSlice =
          assignment.rhs().actual().map(rhsSlice -> rhsSlice.withFieldAccess(lhsMember));

      final PartialAssignmentRhs memberRhs = new PartialAssignmentRhs(memberSpan, memberRhsSlice);

      // target type is now member type
      final CType memberTargetType = typeHandler.getSimplifiedType(lhsMember);

      final SingleRhsPartialAssignment memberAssignment =
          new SingleRhsPartialAssignment(
              new PartialAssignmentLhs(lhsMemberSlice, memberTargetType), memberRhs);
      // we now have the member assignment, generate the simple assignments from it
      generateSimplePartialAssignments(memberAssignment, simpleAssignmentMultimap);

      // early return
      return;
    }

    // types or offsets are not equal, do not go into rhs, just get the right spans
    // the rhs offset is still referenced to rhs which does not change, but the intersection
    // may start after original, so add intersection lhs bit offset and subtract original
    // lhs bit offset
    final long intersectionRhsBitOffset =
        originalSpan.rhsTargetBitOffset()
            + lhsIntersectionRange.lowerEndpoint()
            - lhsOriginalRange.lowerEndpoint();

    final PartialSpan memberSpan =
        new PartialSpan(
            intersectionMemberReferencedLhsBitOffset,
            intersectionRhsBitOffset,
            memberAssignmentBitSize);
    final PartialAssignmentRhs memberRhs =
        new PartialAssignmentRhs(memberSpan, assignment.rhs().actual());

    // target type does not change
    final SingleRhsPartialAssignment memberAssignment =
        new SingleRhsPartialAssignment(
            new PartialAssignmentLhs(lhsMemberSlice, assignment.lhs().targetType()), memberRhs);

    // we now have the member assignment, generate the simple assignments from it
    generateSimplePartialAssignments(memberAssignment, simpleAssignmentMultimap);
  }

  /**
   * Assigns initialization assignments to the given variable.
   *
   * <p>If quantifier encoding should be used and is array initialization with all initializers
   * being the same, makes the initializer slicing, e.g. from <code> int a[3] = {2,2,2}; </code>,
   * makes initial slice assignment {@code a[i] = 2} for {@code 0 <= i < size} where {@code size =
   * 3}. Otherwise, just generates the assignments as normal.
   *
   * @param variable The declared variable.
   * @param declarationType The type of the declared variable.
   * @param assignments A list of assignment statements.
   * @return A boolean formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException It the execution was interrupted.
   */
  BooleanFormula initializationAssign(
      final CIdExpression variable,
      final CType declarationType,
      final List<CExpressionAssignmentStatement> assignments)
      throws UnrecognizedCodeException, InterruptedException {

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
          && arrayLength.orElseThrow() == assignments.size()
          && rhsValue.isValue()
          && checkEqualityOfInitializers(assignments, rhsVisitor)
          && lhsLocation.isAliased()) {
        // there is an initializer for every array element and all of them are the same
        // make a single slice assignment over the array length
        CArraySubscriptExpression firstAssignmentLeftSide =
            (CArraySubscriptExpression) firstAssignment.getLeftHandSide();
        CLeftHandSide wholeAssignmentLeftSide =
            (CLeftHandSide) firstAssignmentLeftSide.getArrayExpression();

        SliceExpression sliceLhs =
            new SliceExpression(wholeAssignmentLeftSide)
                .withIndex(new SliceVariable(arrayType.getLength()));
        SliceExpression sliceRhs = new SliceExpression(firstAssignment.getRightHandSide());
        SliceAssignment sliceAssignment =
            new SliceAssignment(
                sliceLhs, Optional.of(firstAssignmentLeftSide), Optional.of(sliceRhs));
        return assign(ImmutableList.of(sliceAssignment));
      }
    }

    // normal initializer handling, build all initialization assignments

    ImmutableList.Builder<SliceAssignment> builder = ImmutableList.<SliceAssignment>builder();
    for (CExpressionAssignmentStatement assignment : assignments) {
      SliceExpression lhs = new SliceExpression(assignment.getLeftHandSide());
      SliceExpression rhs = new SliceExpression(assignment.getRightHandSide());
      builder.add(
          new SliceAssignment(lhs, Optional.of(assignment.getLeftHandSide()), Optional.of(rhs)));
    }
    return assign(builder.build());
  }

  /**
   * Checks whether all assignments of an initializer have the same value.
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
