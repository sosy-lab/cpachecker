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
import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing.getFieldAccessName;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.implicitCastToPointer;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.isSimpleType;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
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
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.IsRelevantWithHavocAbstractionVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceFieldAccessModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceIndexVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceSplitExpression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceSubscriptModifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.ArraySliceExpression.ArraySliceTail;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.Expression.Kind;
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
    return handleAssignment(lhs, lhsForChecking, lhsType, rhs, useOldSSAIndicesIfAliased, false);
  }
  /**
   * Creates a formula to handle assignments.
   *
   * @param lhs The left hand side of an assignment.
   * @param lhsForChecking The left hand side of an assignment to check.
   * @param rhs Either {@code null} or the right hand side of the assignment.
   * @param useOldSSAIndicesIfAliased A flag indicating whether we can use old SSA indices for
   *     aliased locations (because the location was not used before)
   * @param reinterpretInsteadOfCasting A flag indicating whether we should reinterpret the
   *     right-hand side type, preserving the bit-vector representation, to the left-hand side type
   *     instead of casting it according to C rules
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If the execution was interrupted.
   */
  BooleanFormula handleAssignment(
      final CLeftHandSide lhs,
      final CLeftHandSide lhsForChecking,
      final CType lhsType,
      final @Nullable CRightHandSide rhs,
      final boolean useOldSSAIndicesIfAliased,
      final boolean reinterpretInsteadOfCasting)
      throws UnrecognizedCodeException, InterruptedException {
    // TODO: remake the parameters
    if (!conv.isRelevantLeftHandSide(lhsForChecking)) {
      // Optimization for unused variables and fields
      return conv.bfmgr.makeTrue();
    }

    final ArraySliceExpression lhsSlice = new ArraySliceExpression(lhs);
    final ArraySliceRhs rhsSlice;
    if (rhs != null) {
      if (rhs instanceof CExpression rhsExpression) {
      rhsSlice = new ArraySliceExpressionRhs(new ArraySliceExpression(rhsExpression));
      } else if (rhs instanceof CFunctionCallExpression rhsCall) {
        rhsSlice = new ArraySliceCallRhs(rhsCall);
      } else {
        assert(false);
        rhsSlice = null;
      }
    } else {
      rhsSlice = new ArraySliceNondetRhs(lhsType);
    }

    ArraySliceAssignment assignment = new ArraySliceAssignment(lhsSlice, rhsSlice);

    AssignmentOptions assignmentOptions =
        new AssignmentOptions(
            useOldSSAIndicesIfAliased,
            reinterpretInsteadOfCasting
                ? AssignmentConversionType.REINTERPRET
                : AssignmentConversionType.CAST);

    return handleSliceAssignment(assignment, assignmentOptions);
  }

  private record ArraySlicePartSpan(
      CType originalLhsType, long lhsBitOffset, long rhsBitOffset, long bitSize) {
    ArraySlicePartSpan(CType originalLhsType, long lhsBitOffset, long rhsBitOffset, long bitSize) {
      checkIsSimplified(originalLhsType);
      this.originalLhsType = originalLhsType;
      this.lhsBitOffset = lhsBitOffset;
      this.rhsBitOffset = rhsBitOffset;
      this.bitSize = bitSize;
    }
  }

  sealed interface ArraySliceRhs
      permits ArraySliceExpressionRhs, ArraySliceCallRhs, ArraySliceNondetRhs {
    @Nullable CRightHandSide getDummyResolved(CType sizeType);
  }

  record ArraySliceExpressionRhs(ArraySliceExpression expression) implements ArraySliceRhs {
    ArraySliceExpressionRhs(ArraySliceExpression expression) {
      checkNotNull(expression);
      this.expression = expression;
    }

    @Override
    public CRightHandSide getDummyResolved(CType sizeType) {
      // return dummy resolved
      return expression.getDummyResolvedExpression(sizeType);
    }
  }

  record ArraySliceCallRhs(CFunctionCallExpression call) implements ArraySliceRhs {
    ArraySliceCallRhs(CFunctionCallExpression call) {
      checkNotNull(call);
      this.call = call;
    }

    @Override
    public CRightHandSide getDummyResolved(CType sizeType) {
      // there is no resolving to be done, just return the call
      return call;
    }
  }

  record ArraySliceNondetRhs(CType type) implements ArraySliceRhs {
    ArraySliceNondetRhs(CType type) {
      checkNotNull(type);
      this.type = type;
    }

    @Override
    public @Nullable CRightHandSide getDummyResolved(CType sizeType) {
      // there is no right-hand side, return null
      return null;
    }
  }

  private record ArraySliceSpanRhs(Optional<ArraySlicePartSpan> span, ArraySliceRhs actual) {
    ArraySliceSpanRhs(Optional<ArraySlicePartSpan> span, ArraySliceRhs actual) {
      checkNotNull(span);
      checkNotNull(actual);
      this.span = span;
      this.actual = actual;
    }
  }

  private record ArraySliceSpanAssignment(
      ArraySliceExpression lhs, ImmutableList<ArraySliceSpanRhs> rhsList) {
    ArraySliceSpanAssignment(ArraySliceExpression lhs, ImmutableList<ArraySliceSpanRhs> rhsList) {
      checkNotNull(lhs);
      checkNotNull(rhsList);
      this.lhs = lhs;
      this.rhsList = rhsList;
    }
  }

  private record ArraySliceResolved(Expression expression, CType type) {

    private ArraySliceResolved(Expression expression, CType type) {
      checkNotNull(expression);
      checkIsSimplified(type);
      this.expression = expression;
      this.type = type;
    }
  }

  private record ArraySliceSpanResolved(
      Optional<ArraySlicePartSpan> span, ArraySliceResolved actual) {
    ArraySliceSpanResolved(Optional<ArraySlicePartSpan> span, ArraySliceResolved actual) {
      checkNotNull(span);
      checkNotNull(actual);
      this.span = span;
      this.actual = actual;
    }
  }

  record ArraySliceAssignment(ArraySliceExpression lhs, ArraySliceRhs rhs) {
    ArraySliceAssignment(ArraySliceExpression lhs, ArraySliceRhs rhs) {
      checkNotNull(lhs);
      checkNotNull(rhs);
      this.lhs = lhs;
      this.rhs = rhs;
    }
  }

  enum AssignmentConversionType {
    CAST,
    REINTERPRET
  }

  record AssignmentOptions(boolean useOldSSAIndices, AssignmentConversionType conversionType) {
    AssignmentOptions(boolean useOldSSAIndices, AssignmentConversionType conversionType) {
      checkNotNull(conversionType);
      this.useOldSSAIndices = useOldSSAIndices;
      this.conversionType = conversionType;
    }
  }

  BooleanFormula handleSliceAssignment(
      ArraySliceAssignment assignment, final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {
    return handleSliceAssignments(ImmutableList.of(assignment), assignmentOptions);
  }

  BooleanFormula handleSliceAssignments(
      List<ArraySliceAssignment> assignments, final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {

    CSimpleType sizeType = conv.machineModel.getPointerEquivalentSimpleType();

    // apply Havoc abstraction: if Havoc abstraction is turned on
    // and rhs is not relevant, make it nondeterministic
    List<ArraySliceAssignment> appliedHavocAssignments = new ArrayList<>();
    if (conv.options.useHavocAbstraction()) {
      for (ArraySliceAssignment assignment : assignments) {
        // the Havoc relevant visitor does not care about the value of subscripts, we can
        // just convert to dummy resolved expression and test with it
        @Nullable CRightHandSide dummyResolvedRhs = assignment.rhs.getDummyResolved(sizeType);

        if (dummyResolvedRhs == null || dummyResolvedRhs.accept(new IsRelevantWithHavocAbstractionVisitor(conv))) {
          // either already nondeterministic or relevant, retain
          appliedHavocAssignments.add(assignment);
        } else {
          // make rhs nondeterministic
          appliedHavocAssignments.add(
              new ArraySliceAssignment(
                  assignment.lhs,
                  new ArraySliceNondetRhs(assignment.lhs.getResolvedExpressionType(sizeType))));
        }
          continue;
      }
    } else {
      appliedHavocAssignments.addAll(assignments);
    }

    // apply the deferred memory handler: if there is a malloc with void* type, the allocation can
    // be deferred until the assignment that uses the value; the allocation type can then be
    // inferred from assignment lhs type
    if (conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) {

      DynamicMemoryHandler memoryHandler =
          new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions, regionMgr);



      for (ArraySliceAssignment assignment : assignments) {
        // the memory handler does not care about the value of subscripts, we can use dummy
        // resolutions, visit them, and get the learned pointer types
        final CExpressionVisitorWithPointerAliasing lhsVisitor = newExpressionVisitor();
        final CExpressionVisitorWithPointerAliasing rhsVisitor = newExpressionVisitor();
        // we do not need the lhs expression, but need the visitor that has visited it
        CExpression dummyResolvedLhs = assignment.lhs.getDummyResolvedExpression(sizeType);
        dummyResolvedLhs.accept(lhsVisitor);
        final @Nullable CRightHandSide dummyResolvedRhs = assignment.rhs.getDummyResolved(sizeType);
        // the dynamic memory handler expects null CExpression and nondet Expression on nondet rhs
        final Expression rhsExpression = dummyResolvedRhs != null ? dummyResolvedRhs.accept(rhsVisitor) : Value.nondetValue();

        final Map<String, CType> lhsLearnedPointerTypes = lhsVisitor.getLearnedPointerTypes();
        final Map<String, CType> rhsLearnedPointerTypes = lhsVisitor.getLearnedPointerTypes();

        // we have everything we need, call memory handler
        memoryHandler.handleDeferredAllocationsInAssignment(
            (CLeftHandSide) dummyResolvedLhs,
            dummyResolvedRhs,
            rhsExpression,
            typeHandler.getSimplifiedType(dummyResolvedLhs),
            lhsLearnedPointerTypes,
            rhsLearnedPointerTypes);
      }
    }

    // generate simple slice assignments to resolve assignments to structures and arrays
    List<ArraySliceAssignment> simpleAssignments = new ArrayList<>();

    for (ArraySliceAssignment assignment : assignments) {
      generateSimpleSliceAssignments(assignment, simpleAssignments);
    }
    // hand over
    return handleSimpleSliceAssignments(simpleAssignments, assignmentOptions);
  }


  private void generateSimpleSliceAssignments(
      ArraySliceAssignment assignment, List<ArraySliceAssignment> simpleAssignments) {
    boolean isExpressionRhs = assignment.rhs instanceof ArraySliceExpressionRhs;
    boolean isNondetRhs = assignment.rhs instanceof ArraySliceNondetRhs;
    if (!isExpressionRhs && !isNondetRhs) {
      // call rhs, cannot be simplified, just pretend that it is a simple assignment
      simpleAssignments.add(assignment);
      return;
    }

    CSimpleType sizeType = conv.machineModel.getPointerEquivalentSimpleType();

    CType lhsType = assignment.lhs.getResolvedExpressionType(sizeType);

    if (lhsType instanceof CCompositeType lhsCompositeType) {
      // add an assignment for each member
      for (CCompositeTypeMemberDeclaration member : lhsCompositeType.getMembers()) {
        final ArraySliceExpression memberLhs = assignment.lhs.withFieldAccess(member);

        final ArraySliceRhs memberRhs;
        if (isExpressionRhs) {
          memberRhs = new ArraySliceExpressionRhs(((ArraySliceExpressionRhs)assignment.rhs).expression.withFieldAccess(member));
        } else {
          // all nondet rhs are the same
          memberRhs = assignment.rhs;
        }
        ArraySliceAssignment memberAssignment = new ArraySliceAssignment(memberLhs, memberRhs);
        generateSimpleSliceAssignments(memberAssignment, simpleAssignments);
      }
    } else if (lhsType instanceof CArrayType lhsArrayType) {
      @Nullable CExpression lhsArrayLength = lhsArrayType.getLength();
      if (lhsArrayLength != null) {
        // add an assignment of every element of array using a quantified variable
        ArraySliceIndexVariable indexVariable =
            new ArraySliceIndexVariable(lhsArrayType.getLength());
        ArraySliceExpression elementLhs = assignment.lhs.withIndex(indexVariable);
        final ArraySliceRhs elementRhs;
        if (isExpressionRhs) {
          elementRhs =
              new ArraySliceExpressionRhs(
                  ((ArraySliceExpressionRhs) assignment.rhs).expression.withIndex(indexVariable));
        } else {
          // all nondet rhs are the same
          elementRhs = assignment.rhs;
        }
        ArraySliceAssignment elementAssignment = new ArraySliceAssignment(elementLhs, elementRhs);
        generateSimpleSliceAssignments(elementAssignment, simpleAssignments);
      } else {
        // TODO: add slice flexible array member assignment, tracking the length from malloc
        conv.logger.logfOnce(
            Level.WARNING,
            "%s: Ignoring slice assignment to flexible array member %s as they are not currently well-supported",
            edge.getFileLocation(),
            lhsArrayType);
      }
    } else {
      // already simple, just add the assignment to simple assignments
      simpleAssignments.add(assignment);
    }
  }

  private BooleanFormula handleSimpleSliceAssignments(
      final List<ArraySliceAssignment> assignments, final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {

    // handle unions here

    // the caller is responsible to make sure that for fields in assignments, unionized fields have
    // the same representation in memory assigned where they overlap, we only add span assignments
    // for unionized fields that are not assigned to

    // add the given assignments to span assignments; also generate alternative assignments for
    // every union field, use a LinkedHashMultimap to preseve ordering

    List<ArraySliceSpanAssignment> spanAssignments = new ArrayList<>();
    Multimap<ArraySliceExpression, ArraySliceSpanRhs> alternativeAssignmentMultimap =
        LinkedHashMultimap.create();

    for (ArraySliceAssignment assignment : assignments) {
      spanAssignments.add(
          new ArraySliceSpanAssignment(
              assignment.lhs,
              ImmutableList.of(new ArraySliceSpanRhs(Optional.empty(), assignment.rhs))));
      generateAlternativeSpanAssignments(assignment, alternativeAssignmentMultimap);
    }

    // remove all alternative assignments for fields that are normally assigned
    List<Entry<ArraySliceExpression, ArraySliceSpanRhs>> alternativeAssignmentsToRemove =
        new ArrayList<>();

    for (Entry<ArraySliceExpression, ArraySliceSpanRhs> entry :
        alternativeAssignmentMultimap.entries()) {
      if (assignments.contains(new ArraySliceAssignment(entry.getKey(), entry.getValue().actual))) {
        alternativeAssignmentsToRemove.add(entry);
      }
    }
    alternativeAssignmentMultimap.removeAll(alternativeAssignmentsToRemove);

    // and add alternative assignments to the the span assignments
    for (Entry<ArraySliceExpression, Collection<ArraySliceSpanRhs>> entry :
        alternativeAssignmentMultimap.asMap().entrySet()) {
      ArraySliceExpression lhs = entry.getKey();
      // TODO: for the least amount of assignments, we should find the minimal amount of span
      // intervals which fully cover the original spans
      // we will just add every span assignment, it should result in the same result provided that
      // the caller has fulfilled the same representation condition
      ImmutableList.Builder<ArraySliceSpanRhs> builder = ImmutableList.builder();
      builder.addAll(entry.getValue());
      spanAssignments.add(new ArraySliceSpanAssignment(lhs, builder.build()));
    }

    // hand off the span assignments

    if (options.useQuantifiersOnArrays()) {
      return handleSimpleSliceAssignmentsWithQuantifiers(spanAssignments, assignmentOptions);
    } else {
      return handleSimpleSliceAssignmentsWithoutQuantifiers(spanAssignments, assignmentOptions);
    }
  }

  private void generateAlternativeSpanAssignments(
      ArraySliceAssignment assignment,
      Multimap<ArraySliceExpression, ArraySliceSpanRhs> spanAssignmentMultimap) {

    CSimpleType sizeType = conv.machineModel.getPointerEquivalentSimpleType();

    // split the lhs into a base part followed by field accesses
    // e.g. with (*x).a.b.c.d, split into (*x) and .a.b.c.d

    final ArraySliceSplitExpression splitLhs = assignment.lhs.getSplit();
    final ArraySliceExpression head = splitLhs.head();
    final CType headType = head.getResolvedExpressionType(sizeType);
    final ArraySliceTail tail = splitLhs.tail();

    // look at all accesses above the lowest one
    // e.g. with .a.b.c.d, look at (no field access), .a, .a.b, .a.b.c

    List<ArraySliceTail> alternativeUnionAccesses = new ArrayList<>();
    CType parentType = headType;
    for (int numAccesses = 0; numAccesses < tail.list().size(); ++numAccesses) {

      CCompositeTypeMemberDeclaration currentMember = tail.list().get(numAccesses);
      CCompositeType parentCompositeType = (CCompositeType) parentType;

      if (parentCompositeType.getKind() == CComplexType.ComplexTypeKind.UNION) {
        // add all members other than the currently assigned one to alternative union accesses
        // e.g. let's say that .a is a union { int b; int x; struct my_struct y; },
        // add alternative union accesses .a.x, .a.y

        ImmutableList<CCompositeTypeMemberDeclaration> withoutMember =
            tail.list().subList(0, numAccesses);

        for (CCompositeTypeMemberDeclaration member : parentCompositeType.getMembers()) {
          if (currentMember.getName().equals(member.getName())) {
            // do not add the current field as an alternative
            continue;
          }

          ImmutableList.Builder<CCompositeTypeMemberDeclaration> builder = ImmutableList.<CCompositeTypeMemberDeclaration>builder();
          builder.addAll(withoutMember);
          builder.add(member);

          alternativeUnionAccesses.add(new ArraySliceTail(builder.build()));
        }
      }

      parentType = typeHandler.getSimplifiedType(currentMember);
    }

    // now that we have the alternative union accesses, we need to generate simple accesses
    // e.g. we have .a.x, .a.y and the type of y is struct my_struct { int s; float t; }
    // we want to generate .a.x, .a.y.s, .a.y.t

    List<ArraySliceTail> simpleAlternativeTails = new ArrayList<>();
    for (ArraySliceTail alternativeTail : alternativeUnionAccesses) {
      generateSimpleTails(headType, alternativeTail, simpleAlternativeTails);
    }

    // now that we have the simple alternative union accesses, we should resolve their offsets
    // for easy computation, we will compute the start offset and offset after end for both the
    // original assignment lhs type and the alternative union access type; that will allow us to
    // compute the bits that need to be copied as an intersection of the intervals

    long originalTailBitOffset = computeTailBitOffset(headType, tail);
    long originalTailBitSize = computeTailBitSize(headType, tail);
    long originalTailBitAfterEnd = originalTailBitOffset + originalTailBitSize;

    for (ArraySliceTail alternativeTail : simpleAlternativeTails) {
      long alternativeTailBitOffset = computeTailBitOffset(headType, alternativeTail);
      long alternativeTailBitSize = computeTailBitSize(headType, alternativeTail);
      long alternativeTailBitAfterEnd = alternativeTailBitOffset + alternativeTailBitSize;

      long copyBitOffset = Long.max(originalTailBitOffset, alternativeTailBitOffset);
      long copyBitAfterEnd = Long.min(originalTailBitAfterEnd, alternativeTailBitAfterEnd);

      if (copyBitOffset >= copyBitAfterEnd) {
        // nothing to copy
        continue;
      }

      long copyBitSize = copyBitAfterEnd - copyBitOffset;
      long copyBitOffsetFromOriginal = copyBitOffset - originalTailBitOffset;
      long copyBitOffsetFromAlternative = copyBitOffset - alternativeTailBitOffset;

      // assign the appropriate part of original rhs to alternative lhs
      ArraySliceExpression alternativeLhs = ArraySliceExpression.fromSplit(new ArraySliceSplitExpression(head, alternativeTail));

      ArraySlicePartSpan span =
          new ArraySlicePartSpan(
              typeHandler.simplifyType(assignment.lhs.getResolvedExpressionType(sizeType)),
              copyBitOffsetFromAlternative,
              copyBitOffsetFromOriginal,
              copyBitSize);
      spanAssignmentMultimap.put(
          alternativeLhs,
          new ArraySliceSpanRhs(
              Optional.of(span),
              assignment.rhs));
    }

    // we have added the needed span assignments to spanAssignmentMultimap
  }

  private long computeTailBitSize(CType headType, ArraySliceTail tail) {
    if (tail.list().isEmpty()) {
      return typeHandler.getBitSizeof(headType);
    }
    return typeHandler.getBitSizeof(
        typeHandler.getSimplifiedType(tail.list().get(tail.list().size() - 1)));
  }

  private long computeTailBitOffset(CType headType, ArraySliceTail tail) {
    long bitOffset = 0;
    CType parentType = headType;
    for (CCompositeTypeMemberDeclaration field : tail.list()) {
      bitOffset += typeHandler.getBitOffset((CCompositeType) parentType, field);
      parentType = typeHandler.getSimplifiedType(field);
    }
    return bitOffset;
  }

  private void generateSimpleTails(
      CType headType, ArraySliceTail trailing, List<ArraySliceTail> simpleTrailing) {
    if (trailing.list().isEmpty()) {
      // just add the trailing if head type is simple
      // TODO: handle arrays in simple tail generation
      if (CTypeUtils.isSimpleType(headType)) {
        simpleTrailing.add(trailing);
      }
      return;
    }

    CCompositeTypeMemberDeclaration lastAccess = trailing.list().get(trailing.list().size() - 1);
    CType lastType = typeHandler.getSimplifiedType(lastAccess);

    if (lastType instanceof CCompositeType lastCompositeType) {
      // add an assignment for each member
      for (CCompositeTypeMemberDeclaration member : lastCompositeType.getMembers()) {
        ImmutableList.Builder<CCompositeTypeMemberDeclaration> builder =
            ImmutableList.<CCompositeTypeMemberDeclaration>builder();
        builder.addAll(trailing.list());
        builder.add(member);
        // TODO: a simple tail here could be an array...
        generateSimpleTails(headType, new ArraySliceTail(builder.build()), simpleTrailing);
      }
    } else {
      // just add the assignment to simple assignments if its type is simple
      // TODO: handle arrays in simple tail generation
      if (CTypeUtils.isSimpleType(lastType)) {
        simpleTrailing.add(trailing);
      }
    }
  }

  private BooleanFormula handleSimpleSliceAssignmentsWithoutQuantifiers(
      final List<ArraySliceSpanAssignment> assignments, final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException, InterruptedException {

    // unroll the variables for every assignment

    BooleanFormula result = null;

    for (ArraySliceSpanAssignment assignment : assignments) {

      // get all quantifier variables that are used by at least one side
      HashSet<ArraySliceIndexVariable> quantifierVariableSet = new LinkedHashSet<>();
      quantifierVariableSet.addAll(assignment.lhs.getUnresolvedIndexVariables());
      for (ArraySliceSpanRhs rhs : assignment.rhsList) {
        if (rhs.actual instanceof ArraySliceExpressionRhs expressionRhs) {
          quantifierVariableSet.addAll(expressionRhs.expression.getUnresolvedIndexVariables());
        }
      }
      List<ArraySliceIndexVariable> quantifierVariables = new ArrayList<>(quantifierVariableSet);

      result =
          nullableAnd(
              result,
              unrollSliceAssignment(
                  assignment,
                  assignmentOptions,
                  quantifierVariables,
                  new HashMap<>(),
                  null));
    }

    return nullToTrue(result);
  }

  private BooleanFormula unrollSliceAssignment(
      ArraySliceSpanAssignment assignment,
      AssignmentOptions assignmentOptions,
      List<ArraySliceIndexVariable> quantifierVariables,
      Map<ArraySliceIndexVariable, Long> unrolledVariables,
      @Nullable BooleanFormula condition)
      throws UnrecognizedCodeException, InterruptedException {

    // the recursive unrolling is probably slow, but will serve well for now

    CSimpleType sizeType = conv.machineModel.getPointerEquivalentSimpleType();

    if (quantifierVariables.isEmpty()) {
      // already unrolled, resolve the indices in array slice expressions
      // TODO: add fields for UF
      final CExpressionVisitorWithPointerAliasing visitor = newExpressionVisitor();

      ArraySliceExpression lhs = assignment.lhs;
      while (!lhs.isResolved()) {
        long unrolledIndex = unrolledVariables.get(lhs.getFirstIndex());
        lhs = lhs.resolveFirstIndex(sizeType, unrolledIndex);
      }
      CExpression lhsBase = lhs.getResolvedExpression();
      Expression lhsExpression = lhsBase.accept(visitor);
      ArraySliceResolved lhsVisited =
          new ArraySliceResolved(lhsExpression, typeHandler.getSimplifiedType(lhsBase));

      ImmutableList.Builder<ArraySliceSpanResolved> builder = ImmutableList.builder();

      for (ArraySliceSpanRhs rhs : assignment.rhsList) {
        final CRightHandSide rhsResolved;
        if (rhs.actual instanceof ArraySliceNondetRhs nondetRhs) {
          builder.add(
              new ArraySliceSpanResolved(
                  rhs.span, new ArraySliceResolved(Value.nondetValue(), nondetRhs.type)));
          continue;
        } else if (rhs.actual instanceof ArraySliceCallRhs callRhs) {
          rhsResolved = callRhs.call;
        } else if (rhs.actual instanceof ArraySliceExpressionRhs expressionRhs) {
          // resolve all indices
          ArraySliceExpression rhsSliceExpression = expressionRhs.expression;
          while (!rhsSliceExpression.isResolved()) {
            long unrolledIndex = unrolledVariables.get(rhsSliceExpression.getFirstIndex());
            rhsSliceExpression = rhsSliceExpression.resolveFirstIndex(sizeType, unrolledIndex);
          }
          rhsResolved = rhsSliceExpression.getResolvedExpression();
        } else {
          assert (false);
          continue;
        }

        Expression rhsExpression = rhsResolved.accept(visitor);
        builder.add(
            new ArraySliceSpanResolved(
                rhs.span,
                new ArraySliceResolved(rhsExpression, typeHandler.getSimplifiedType(rhsResolved))));
      }

      return makeSliceAssignment(
          lhsVisited, builder.build(), assignmentOptions, nullToTrue(condition), false);
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
              assignment,
              assignmentOptions,
              quantifierVariables,
              unrolledVariables,
              unrolledCondition);
      result = nullableAnd(result, recursionResult);
    }

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
      List<ArraySliceSpanAssignment> assignments) {
    // remove duplicates, but preserve ordering so quantification is deterministic
    Set<ArraySliceIndexVariable> indexVariableSet = new LinkedHashSet<>();

    for (ArraySliceSpanAssignment assignment : assignments) {
      indexVariableSet.addAll(assignment.lhs().getUnresolvedIndexVariables());
      for (ArraySliceSpanRhs rhs : assignment.rhsList) {
        // only expression rhs can have unresolved index variables
        if (rhs.actual instanceof ArraySliceExpressionRhs expressionRhs) {
          indexVariableSet.addAll(expressionRhs.expression.getUnresolvedIndexVariables());
        }
      }
    }
    // convert to list
    return ImmutableList.copyOf(indexVariableSet);
  }

  private BooleanFormula handleSimpleSliceAssignmentsWithQuantifiers(
      final List<ArraySliceSpanAssignment> assignments, final AssignmentOptions assignmentOptions)
      throws UnrecognizedCodeException {

    // get all index variables
    List<ArraySliceIndexVariable> indexVariables = resolveAllIndexVariables(assignments);

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
    for (ArraySliceSpanAssignment assignment : assignments) {

      // TODO: add fields handling for UF
      CExpressionVisitorWithPointerAliasing visitor =
          new CExpressionVisitorWithPointerAliasing(
              conv, edge, function, ssa, constraints, errorConditions, pts, regionMgr);
      ArraySliceResolved lhsResolved =
          resolveSliceExpression(assignment.lhs, quantifiedVariableFormulaMap, visitor);
      if (lhsResolved == null) {
        // TODO: only used for ignoring assignments to bit-fields which should be handled properly
        // do not perform this assignment, but others can be done
        continue;
      }

      ImmutableList.Builder<ArraySliceSpanResolved> builder = ImmutableList.builder();
      for (ArraySliceSpanRhs rhs : assignment.rhsList) {
        ArraySliceResolved rhsResolved =
            resolveRhs(rhs.actual, quantifiedVariableFormulaMap, visitor);

        if (rhsResolved == null) {
          // TODO: only used for ignoring assignments to bit-fields which should be handled properly
          // do not perform this assignment, but others can be done
          continue;
        }

        builder.add(new ArraySliceSpanResolved(rhs.span, rhsResolved));
      }

      // TODO: add updatedRegions handling for UF

      // after cast/reinterpretation, lhs and rhs have the lhs type
      BooleanFormula assignmentResult =
          makeSliceAssignment(
              lhsResolved, builder.build(), assignmentOptions, conditionFormula, true);
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

  private CType getIntegerTypeReinterpretation(CType type) {

    // TODO: unify with memory fns and put into MachineModel
    if (!(type instanceof CSimpleType)) {
      return type;
    }

    CSimpleType simpleType = (CSimpleType) type;

    CBasicType basicType = simpleType.getType();

    // we need to construct the corresponding integer type with the same byte size
    // as the floating-point type so that we can populate it

    if (basicType == CBasicType.FLOAT) {
      // TODO: integer type with the same sizeof as float should be resolved by a reverse lookup
      // into the machine model
      // we use unsigned int in the meantime
      return CNumericTypes.UNSIGNED_INT;
    } else if (basicType == CBasicType.DOUBLE) {
      // TODO: integer type with the same sizeof as float should be resolved by a reverse lookup
      // into the machine model
      // we use unsigned long long in the meantime
      return CNumericTypes.UNSIGNED_LONG_LONG_INT;
    }
    return type;
  }

  private BooleanFormula makeSliceAssignment(
      ArraySliceResolved lhsResolved,
      ImmutableList<ArraySliceSpanResolved> rhsList,
      AssignmentOptions assignmentOptions,
      BooleanFormula conditionFormula,
      boolean useQuantifiers)
      throws UnrecognizedCodeException {

    if (lhsResolved.expression.isNondetValue()) {
      // only because of CExpressionVisitorWithPointerAliasing.visit(CFieldReference)
      conv.logger.logfOnce(
          Level.WARNING,
          "%s: Ignoring assignment to %s because bit fields are currently not fully supported",
          edge.getFileLocation(),
          lhsResolved.type);
      return bfmgr.makeTrue();
    }

    if (rhsList.isEmpty()) {
      // nothing to do
      return bfmgr.makeTrue();
    }

    final Expression rhsResult;
    final CType rhsType;

    ArraySliceSpanResolved firstRhs = rhsList.get(0);

    if (firstRhs.span.isEmpty()) {
      verify(rhsList.size() == 1);
      // normal assignment
      // convert normally
      CType rhsPerhapsPointerType = implicitCastToPointer(firstRhs.actual.type);
      if (rhsPerhapsPointerType instanceof CPointerType) {
        // do not convert RHS expression here, leave it to handleDestructiveAssignment
        rhsResult = firstRhs.actual.expression;
        rhsType = firstRhs.actual.type;
      } else {
        // convert RHS expression
        rhsResult =
            convertRhsExpression(
                assignmentOptions.conversionType, lhsResolved.type, firstRhs.actual);
        rhsType = lhsResolved.type;
      }

    } else {
      // TODO: float handling is somewhat wonky
      // put together the rhs expressions from spans

      long lhsBitSize = typeHandler.getBitSizeof(lhsResolved.type);
      Formula wholeRhsFormula = null;
      boolean forceNondet = false;
      for (ArraySliceSpanResolved rhs : rhsList) {

        CType rhsPerhapsPointerType = implicitCastToPointer(rhs.actual.type);
        if (rhsPerhapsPointerType instanceof CPointerType) {
          // TODO: handle pointer rhs type in union
          // force span lhs nondet for now
          forceNondet = true;
          break;
        }

        ArraySlicePartSpan rhsSpan = rhs.span.get();
        // convert RHS expression to original LHS type
        Expression convertedRhs =
            convertRhsExpression(
                assignmentOptions.conversionType, rhsSpan.originalLhsType, rhs.actual);
        Optional<Formula> optionalConvertedRhsFormula =
            getValueFormula(rhsSpan.originalLhsType, convertedRhs);
        if (optionalConvertedRhsFormula.isEmpty()) {
          // no RHS formula due to nondet expression
          // force span lhs nondet
          forceNondet = true;
          break;
        }

        Formula convertedRhsFormula = getValueFormula(rhsSpan.originalLhsType, convertedRhs).get();
        // reinterpret to integer version of original lhs type
        Formula reinterpretedRhsFormula =
            conv.makeValueReinterpretation(
                rhsSpan.originalLhsType,
                getIntegerTypeReinterpretation(rhsSpan.originalLhsType),
                convertedRhsFormula);
        if (reinterpretedRhsFormula != null) {
          convertedRhsFormula = reinterpretedRhsFormula;
        }

        // extract the interesting part and dimension to new lhs type, shift left
        // this will make the formula type integer version of new lhs type
        Formula extractedFormula =
            fmgr.makeExtract(
                convertedRhsFormula,
                (int) (rhsSpan.rhsBitOffset + rhsSpan.bitSize - 1),
                (int) rhsSpan.rhsBitOffset);

        long numExtendBits = lhsBitSize - rhsSpan.bitSize;

        Formula extendedFormula = fmgr.makeExtend(extractedFormula, (int) numExtendBits, false);

        Formula shiftedFormula =
            fmgr.makeShiftLeft(
                extendedFormula,
                fmgr.makeNumber(
                    FormulaType.getBitvectorTypeWithSize((int) lhsBitSize), rhsSpan.lhsBitOffset));

        // bit-or with other parts
        if (wholeRhsFormula != null) {
          wholeRhsFormula = fmgr.makeOr(wholeRhsFormula, shiftedFormula);
        } else {
          wholeRhsFormula = shiftedFormula;
        }
      }

      // the RHS type is now definitely the type of LHS
      rhsType = lhsResolved.type;
      if (forceNondet) {
        // force RHS result to be nondeterministic
        rhsResult = Value.nondetValue();
      } else {
        // reinterpret to LHS type
        Formula reinterpretedWholeRhsFormula =
            conv.makeValueReinterpretation(
                getIntegerTypeReinterpretation(lhsResolved.type), lhsResolved.type, wholeRhsFormula);
        if (reinterpretedWholeRhsFormula != null) {
          wholeRhsFormula = reinterpretedWholeRhsFormula;
        }
        rhsResult = Value.ofValue(wholeRhsFormula);
      }
    }

    // TODO: currently cannot be simple due to function calls

    return makeDestructiveAssignment(
        lhsResolved.type,
        rhsType,
        (Location) lhsResolved.expression,
        rhsResult,
        assignmentOptions.useOldSSAIndices && lhsResolved.expression.isAliasedLocation(),
        null,
        conditionFormula,
        useQuantifiers);
  }

  private Expression convertRhsExpression(
      AssignmentConversionType conversionType, CType lhsType, ArraySliceResolved rhsResolved)
      throws UnrecognizedCodeException {

    // convert only if necessary, the types are already simplified
    if (lhsType.equals(rhsResolved.type)) {
      return rhsResolved.expression;
    }

    Optional<Formula> optionalRhsFormula =
        getValueFormula(rhsResolved.type, rhsResolved.expression);
    if (optionalRhsFormula.isEmpty()) {
      // nondeterministic RHS expression has no formula, do not convert
      return rhsResolved.expression;
    }
    Formula rhsFormula =
        getValueFormula(rhsResolved.type, rhsResolved.expression).orElseThrow();
    switch (conversionType) {
      case CAST:
        // cast rhs from rhs type to lhs type
        Formula castRhsFormula =
            conv.makeCast(rhsResolved.type, lhsType, rhsFormula, constraints, edge);
      return Value.ofValue(castRhsFormula);
      case REINTERPRET:
        if (lhsType instanceof CBitFieldType) {
          // cannot reinterpret to bit-field type
          conv.logger.logfOnce(
              Level.WARNING,
              "%s: Making assignment from %s to %s nondeterministic because reinterpretation to bitfield is not supported",
              edge.getFileLocation(),
              rhsResolved.type,
              lhsType);
          return Value.nondetValue();
        }

        // reinterpret rhs from rhs type to lhs type
        Formula reinterpretedRhsFormula =
            conv.makeValueReinterpretation(rhsResolved.type, lhsType, rhsFormula);

        // makeValueReinterpretation returns null if no reinterpretation happened
        if (reinterpretedRhsFormula != null) {
          return Value.ofValue(reinterpretedRhsFormula);
        }
        break;
      default:
        assert (false);
    }
    return rhsResolved.expression;
  }

  private @Nullable ArraySliceResolved resolveRhs(
      final ArraySliceRhs rhs,
      final Map<ArraySliceIndexVariable, Formula> quantifiedVariableFormulaMap,
      CExpressionVisitorWithPointerAliasing visitor)
      throws UnrecognizedCodeException {

    if (rhs instanceof ArraySliceNondetRhs nondetRhs) {
      return new ArraySliceResolved(Value.nondetValue(), nondetRhs.type);
    }
    if (rhs instanceof ArraySliceCallRhs callRhs) {
      Expression rhsExpression = callRhs.call.accept(visitor);
      return new ArraySliceResolved(rhsExpression, typeHandler.getSimplifiedType(callRhs.call));
    }

    return resolveSliceExpression(
        ((ArraySliceExpressionRhs) rhs).expression, quantifiedVariableFormulaMap, visitor);
  }

  private @Nullable ArraySliceResolved resolveSliceExpression(
      final ArraySliceExpression sliceExpression,
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
        return null;
      }
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
    CPointerType basePointerType = (CPointerType) CTypes.adjustFunctionOrArrayType(base.type);
    final CType elementType = typeHandler.simplifyType(basePointerType.getType());

    // perform pointer arithmetic, we have array[base] and want array[base + i]
    // the quantified variable i must be multiplied by the sizeof the element type

    Formula baseAddress = base.expression.asAliasedLocation().getAddress();
    final Formula sizeofElement =
        conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.getSizeof(elementType));

    final Formula adjustedAddress =
        conv.fmgr.makePlus(baseAddress, conv.fmgr.makeMultiply(quantifiedVariableFormula, sizeofElement));

    // return the resolved formula with adjusted address and array element type
    return new ArraySliceResolved(AliasedLocation.ofAddress(adjustedAddress), elementType);
  }

  private @Nullable ArraySliceResolved resolveFieldAccessModifier(
      ArraySliceResolved base, ArraySliceFieldAccessModifier modifier) {

    // the base type must be a composite type to have fields
    CCompositeType baseType = (CCompositeType) base.type;
    final String fieldName = modifier.field().getName();
    CType fieldType = typeHandler.getSimplifiedType(modifier.field());

    // we will increase the base address by field offset

    Formula baseAddress = base.expression.asAliasedLocation().getAddress();

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


  private Expression createRHSExpression(
      CRightHandSide pRhs,
      CType pLhsType,
      CExpressionVisitorWithPointerAliasing pRhsVisitor,
      boolean reinterpretInsteadOfCasting)
      throws UnrecognizedCodeException {
    if (pRhs == null) {
      return Value.nondetValue();
    }
    CRightHandSide r = pRhs;

    // cast if we are supposed to cast and it is necessary
    if (!reinterpretInsteadOfCasting && (r instanceof CExpression)) {
      r = conv.convertLiteralToFloatIfNecessary((CExpression) r, pLhsType);
    }
    Expression rhsExpression = r.accept(pRhsVisitor);
    CType rhsType = r.getExpressionType();

    if (!reinterpretInsteadOfCasting) {
      // return if we are not supposed to reinterpret
      return rhsExpression;
    }

    // perform reinterpretation

    if (rhsExpression.getKind() == Kind.NONDET) {
      // nondeterministic value does not correspond to a formula
      // and is the same for every type, just return it
      return rhsExpression;
    }

    Formula rhsFormula = getValueFormula(rhsType, rhsExpression).orElseThrow();

    Formula reinterpretedRhsFormula =
        conv.makeValueReinterpretation(r.getExpressionType(), pLhsType, rhsFormula);
    // makeValueReinterpretation returns null if no reinterpretation happened
    // return the original expression
    if (reinterpretedRhsFormula == null) {
      return rhsExpression;
    }

    return Value.ofValue(reinterpretedRhsFormula);
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

  BooleanFormula handleAssignment(
      final CLeftHandSide lhs,
      final CLeftHandSide lhsForChecking,
      final @Nullable CRightHandSide rhs,
      final boolean useOldSSAIndicesIfAliased,
      final boolean reinterpretInsteadOfCasting)
      throws UnrecognizedCodeException, InterruptedException {
    return handleAssignment(
        lhs,
        lhsForChecking,
        typeHandler.getSimplifiedType(lhs),
        rhs,
        useOldSSAIndicesIfAliased,
        reinterpretInsteadOfCasting);
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
        : "Cannot handle initialization assignments without an assignment right hand side.";

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
   * @param condition Either {@code null} or a condition which determines if the assignment is
   *     actually done. In case of {@code null}, the assignmment is always done.
   * @param useQuantifiers If the quantifier assignment version should be used.
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
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

  private BooleanFormula makeDestructiveArrayAssignment(
      CArrayType lvalueArrayType,
      CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions,
      @Nullable BooleanFormula condition,
      boolean useQuantifiers)
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

  private BooleanFormula makeDestructiveCompositeAssignment(
      final CCompositeType lvalueCompositeType,
      CType rvalueType,
      final Location lvalue,
      final Expression rvalue,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions,
      @Nullable BooleanFormula condition,
      boolean useQuantifiers)
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
   * Creates a formula for a simple destructive assignment.
   *
   * @param lvalueType The type of the lvalue.
   * @param pRvalueType The type of the rvalue.
   * @param lvalue The location of the lvalue.
   * @param rvalue The rvalue expression.
   * @param useOldSSAIndices A flag indicating if we should use the old SSA indices or not.
   * @param updatedRegions Either {@code null} or a set of updated regions.
   * @param condition Either {@code null} or a condition which determines if the assignment is
   *     actually done. In case of {@code null}, the assignmment is always done.
   * @return A formula for the assignment.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  private BooleanFormula makeSimpleDestructiveAssignment(
      CType lvalueType,
      final CType pRvalueType,
      final Location lvalue,
      Expression rvalue,
      final boolean useOldSSAIndices,
      final @Nullable Set<MemoryRegion> updatedRegions,
      @Nullable BooleanFormula condition,
      boolean useQuantifiers)
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

      Formula newVariable = fmgr.makeVariable(targetType, targetName, newIndex);

      if (rhs != null) {
        result = fmgr.assignment(newVariable, rhs);
      } else {
        result = bfmgr.makeTrue();
      }

      // if we need to make the assignment conditional, add the condition
      // either the condition holds and the assignment should be done,
      // or the condition does not hold and the previous value should be copied
      if (condition != null) {
        final int oldIndex = conv.getIndex(targetName, lvalueType, ssa);
        Formula oldVariable = fmgr.makeVariable(targetType, targetName, oldIndex);

        BooleanFormula retainmentAssignment = fmgr.assignment(newVariable, oldVariable);

        result = conv.bfmgr.ifThenElse(condition, result, retainmentAssignment);
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

      final Formula address = lvalue.asAliased().getAddress();

      if (rhs != null) {
        // use the special quantifier version of pointer assignment if requested

        if (useQuantifiers) {
          result =
              conv.ptsMgr.makeQuantifiedPointerAssignment(
                  targetName, targetType, oldIndex, newIndex, address, condition, rhs);
        } else {
          result =
              conv.ptsMgr.makePointerAssignment(
                  targetName, targetType, oldIndex, newIndex, address, rhs);
        }
      } else {
        result = bfmgr.makeTrue();
      }

      // if we need to make the assignment conditional, add the condition
      // either the condition holds and the assignment should be done,
      // or the condition does not hold and the previous value should be copied
      if (!useQuantifiers && condition != null) {
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
      final CFieldReference fieldReference,
      final @Nullable BooleanFormula condition)
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
            updatedRegions,
            condition);
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
            updatedRegions,
            condition);
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
      final Set<MemoryRegion> updatedRegions,
      final @Nullable BooleanFormula condition)
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
        long offset = 0;
        int targetSize = Ints.checkedCast(typeHandler.getBitSizeof(newLhsType));
        Formula rhsFormula = null;

        for (CCompositeTypeMemberDeclaration innerMember :
            ((CCompositeType) rhsType).getMembers()) {
          int innerMemberSize = Ints.checkedCast(typeHandler.getBitSizeof(innerMember.getType()));

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
                          innerMemberFieldReference, innerMember.getType(), expVisitor, false))
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
            updatedRegions,
            condition,
            false));
  }

  private void addAssignmentsForOtherFieldsOfUnionForLhsCompositeType(
      final CExpression newLhs,
      final CCompositeType newLhsType,
      final CType rhsType,
      final Expression rhsExpression,
      final CExpressionVisitorWithPointerAliasing lhsVisitor,
      CCompositeTypeMemberDeclaration member,
      final boolean useOldSSAIndices,
      final Set<MemoryRegion> updatedRegions,
      final @Nullable BooleanFormula condition)
      throws AssertionError, UnrecognizedCodeException {
    // Use different name in this block as newLhsType is confusing. newLhsType was computed as
    // member.getType() -> call it memberType here (note we will also have an innerMember)
    final CCompositeType memberType = newLhsType;
    // newLhs is a CFieldReference to member:
    final CExpression memberCFieldReference = newLhs;
    final int rhsSize = Ints.checkedCast(typeHandler.getBitSizeof(rhsType));

    // for each innerMember of member we need to add a (destructive!) constraint like:
    // union.member.innerMember := treatAsMemberTypeAndExtractInnerMemberValue(rhsExpression);
    for (CCompositeTypeMemberDeclaration innerMember : memberType.getMembers()) {
      int fieldOffset = Ints.checkedCast(typeHandler.getBitOffset(memberType, innerMember));
      if (fieldOffset >= rhsSize) {
        // nothing to fill anymore
        break;
      }
      // don't try later to extract a too big chunk of bits
      int fieldSize =
          Math.min(
              Ints.checkedCast(typeHandler.getBitSizeof(innerMember.getType())),
              rhsSize - fieldOffset);
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
              updatedRegions,
              condition,
              false));
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
