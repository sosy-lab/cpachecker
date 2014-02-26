/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.CTypeUtils.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


class AssignmentHandler {

  private final FormulaEncodingWithUFOptions options;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FunctionFormulaManagerView ffmgr;

  private final CToFormulaWithUFConverter conv;
  private final CFAEdge edge;
  private final String function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;

  AssignmentHandler(CToFormulaWithUFConverter pConv, CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts, Constraints pConstraints, ErrorConditions pErrorConditions) {
    conv = pConv;

    options = conv.options;
    fmgr = conv.fmgr;
    bfmgr = conv.bfmgr;
    ffmgr = conv.ffmgr;

    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
  }

  BooleanFormula handleAssignment(final CLeftHandSide lhs,
                                  final @Nullable CRightHandSide rhs,
                                  final boolean batchMode,
                                  final @Nullable Set<CType> destroyedTypes)
  throws UnrecognizedCCodeException {
    if (!conv.isRelevantLeftHandSide(lhs)) {
      // Optimization for unused variables and fields
      return conv.bfmgr.makeBoolean(true);
    }

    final CType lhsType = CTypeUtils.simplifyType(lhs.getExpressionType());
    final CType rhsType = rhs != null ? CTypeUtils.simplifyType(rhs.getExpressionType()) :
                                        CNumericTypes.SIGNED_CHAR;

    // RHS handling
    final ImmutableList<Pair<CCompositeType, String>> rhsUsedFields;
    final ImmutableMap<String, CType> rhsUsedDeferredAllocationPointers;
    final Expression rhsExpression;
    // RHS is neither null nor a nondet() function call
    if (rhs != null &&
        (!(rhs instanceof CFunctionCallExpression) ||
         !(((CFunctionCallExpression) rhs).getFunctionNameExpression() instanceof CIdExpression) ||
         !conv.options.isNondetFunction(((CIdExpression)((CFunctionCallExpression) rhs).getFunctionNameExpression()).getName()))) {
      StatementToFormulaWithUFVisitor rhsVisitor = new StatementToFormulaWithUFVisitor(conv, edge, function, ssa, constraints, errorConditions, pts);
      rhsExpression = rhs.accept(rhsVisitor);
      pts.addEssentialFields(rhsVisitor.getInitializedFields());
      rhsUsedFields = rhsVisitor.getUsedFields();
      rhsUsedDeferredAllocationPointers = rhsVisitor.getUsedDeferredAllocationPointers();
    } else { // RHS is nondet
      rhsExpression = Value.nondetValue();
      rhsUsedFields = ImmutableList.<Pair<CCompositeType,String>>of();
      rhsUsedDeferredAllocationPointers = ImmutableMap.<String, CType>of();
    }

    // LHS handling
    ExpressionToFormulaWithUFVisitor lhsVisitor = new ExpressionToFormulaWithUFVisitor(conv, edge, function, ssa, constraints, errorConditions, pts);
    final Location lhsLocation = lhs.accept(lhsVisitor).asLocation();
    ImmutableMap<String, CType> lhsUsedDeferredAllocationPointers = lhsVisitor.getUsedDeferredAllocationPointers();
    pts.addEssentialFields(lhsVisitor.getInitializedFields());
    final ImmutableList<Pair<CCompositeType, String>> lhsUsedFields = lhsVisitor.getUsedFields();
    // the pattern matching possibly aliased locations
    LvalueToPointerTargetPatternVisitor lvalueVisitor = new LvalueToPointerTargetPatternVisitor(conv, edge, pts);
    final PointerTargetPattern pattern = lhsLocation.isUnaliasedLocation() ? null : lhs.accept(lvalueVisitor);

    if (conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) {
      DynamicMemoryHandler memoryHandler = new DynamicMemoryHandler(conv, edge, ssa, pts, constraints, errorConditions);
      memoryHandler.handleDeferredAllocationsInAssignment(lhs, rhs,
          lhsLocation, rhsExpression, lhsType,
          lhsUsedDeferredAllocationPointers, rhsUsedDeferredAllocationPointers);
    }

    final BooleanFormula result =
        makeAssignment(lhsType,
                          rhsType,
                          lhsLocation,
                          rhsExpression,
                          pattern,
                          batchMode,
                          destroyedTypes);

    pts.addEssentialFields(lhsUsedFields);
    pts.addEssentialFields(rhsUsedFields);
    return result;
  }

  public BooleanFormula handleInitializationAssignments(final CLeftHandSide variable,
                                                        final List<CExpressionAssignmentStatement> assignments)
                                                            throws UnrecognizedCCodeException {
    ExpressionToFormulaWithUFVisitor lhsVisitor = new ExpressionToFormulaWithUFVisitor(conv, edge, function, ssa, constraints, errorConditions, pts);
    final Location lhsLocation = variable.accept(lhsVisitor).asLocation();
    final Set<CType> updatedTypes = new HashSet<>();
    BooleanFormula result = conv.bfmgr.makeBoolean(true);
    for (CExpressionAssignmentStatement assignment : assignments) {
      final CLeftHandSide lhs = assignment.getLeftHandSide();
      result = conv.bfmgr.and(result, handleAssignment(lhs,
                                                       assignment.getRightHandSide(),
                                                       lhsLocation.isAliased(), // Defer index update for UFs, but not for variables
                                                       updatedTypes));
    }
    if (lhsLocation.isAliased()) {
      LvalueToPointerTargetPatternVisitor lvalueVisitor = new LvalueToPointerTargetPatternVisitor(conv, edge, pts);
      finishAssignments(CTypeUtils.simplifyType(variable.getExpressionType()),
                             lhsLocation.asAliased(),
                             variable.accept(lvalueVisitor),
                             updatedTypes);
    }
    return result;
  }

  BooleanFormula makeAssignment(@Nonnull CType lvalueType,
                                final @Nonnull CType rvalueType,
                                final @Nonnull Location lvalue,
                                final @Nonnull Expression rvalue,
                                final @Nullable PointerTargetPattern pattern,
                                final boolean useOldSSAIndices,
                                      @Nullable Set<CType> updatedTypes)
  throws UnrecognizedCCodeException {
    // Its a definite value assignment, a nondet assignment (SSA index update) or a nondet assignment among other
    // assignments to the same UF version (in this case an absense of aliasing should be somehow guaranteed, as in the
    // case of initialization assignments)
    //assert rvalue != null || !useOldSSAIndices || updatedTypes != null; // otherwise the call is useless
    checkNotNull(rvalue);

    lvalueType = CTypeUtils.simplifyType(lvalueType);

    if (lvalue.isAliased() && !isSimpleType(lvalueType) && updatedTypes == null) {
      updatedTypes = new HashSet<>();
    } else {
      updatedTypes = null;
    }
    Set<Variable> updatedVariables = null;
    if (!lvalue.isAliased() && !isSimpleType(lvalueType)) {
      updatedVariables = new HashSet<>();
    }

    final BooleanFormula result = makeDestructiveAssignment(lvalueType, rvalueType,
                                                            lvalue, rvalue,
                                                            useOldSSAIndices,
                                                            updatedTypes,
                                                            updatedVariables);

    if (!useOldSSAIndices) {
      if (lvalue.isAliased()) {
        addRetentionForAssignment(lvalueType,
                                  lvalue.asAliased().getAddress(),
                                  pattern, updatedTypes);
        if (updatedTypes == null) {
          assert isSimpleType(lvalueType) : "Should be impossible due to the first if statement";
          updatedTypes = Collections.singleton(lvalueType);
        }
        updateSSA(updatedTypes, ssa);
      } else { // Unaliased lvalue
        if (updatedVariables == null) {
          assert isSimpleType(lvalueType) : "Should be impossible due to the first if statement";
          updatedVariables = Collections.singleton(Variable.create(lvalue.asUnaliased().getVariableName(), lvalueType));
        }
        for (final Variable variable : updatedVariables) {
          final String name = variable.getName();
          final CType type = variable.getType();
          ssa.setIndex(name, type, conv.getIndex(name, type, ssa) + 1);
        }
      }
    }
    return result;
  }

  void finishAssignments(@Nonnull CType lvalueType,
                         final @Nonnull AliasedLocation lvalue,
                         final @Nonnull PointerTargetPattern pattern,
                         final @Nonnull Set<CType> updatedTypes) {
    addRetentionForAssignment(lvalueType,
                              lvalue.asAliased().getAddress(),
                              pattern, updatedTypes);
    updateSSA(updatedTypes, ssa);
  }

  private BooleanFormula makeDestructiveAssignment(@Nonnull CType lvalueType,
                                                   @Nonnull CType rvalueType,
                                                   final @Nonnull  Location lvalue,
                                                   final @Nonnull  Expression rvalue,
                                                   final boolean useOldSSAIndices,
                                                   final @Nullable Set<CType> updatedTypes,
                                                   final @Nullable Set<Variable> updatedVariables)
  throws UnrecognizedCCodeException {
    lvalueType = CTypeUtils.simplifyType(lvalueType);
    rvalueType = CTypeUtils.simplifyType(rvalueType);
    BooleanFormula result;

    if (lvalueType instanceof CArrayType) {
      Preconditions.checkArgument(lvalue.isAliased(),
                                  "Array elements are always aliased (i.e. can't be encoded with variables)");
      final CArrayType lvalueArrayType = (CArrayType) lvalueType;
      final CType lvalueElementType = CTypeUtils.simplifyType(lvalueArrayType.getType());

      // There are only two cases of assignment to an array
      Preconditions.checkArgument(
        // Initializing array with a value (possibly nondet), useful for stack declarations and memset implementation
        rvalue.isValue() && isSimpleType(rvalueType) ||
        // Array assignment (needed for structure assignment implementation)
        // Only possible from another array of the same type
        rvalue.asLocation().isAliased() &&
        rvalueType instanceof CArrayType &&
        CTypeUtils.simplifyType(((CArrayType) rvalueType).getType()).equals(lvalueElementType),
        "Impossible array assignment due to incompatible types: assignment of %s to %s",
        rvalueType, lvalueType);

      Integer length = CTypeUtils.getArrayLength(lvalueArrayType);
      // Try to fix the length if it's unknown (or too big)
      // Also ignore the tail part of very long arrays to avoid very large formulae (imprecise!)
      if (length == null || length > options.maxArrayLength()) {
        final Integer rLength;
        if (rvalue.isLocation() &&
            (rLength = CTypeUtils.getArrayLength((CArrayType) rvalueType)) != null &&
            rLength <= options.maxArrayLength()) {
          length = rLength;
        } else {
          length = options.defaultArrayLength();
        }
      }

      result = bfmgr.makeBoolean(true);
      int offset = 0;
      for (int i = 0; i < length; ++i) {
        final Pair<AliasedLocation, CType> newLvalue = shiftArrayLvalue(lvalue.asAliased(), offset, lvalueElementType);
        final Pair<? extends Expression, CType> newRvalue =
                                                       shiftArrayRvalue(rvalue, rvalueType, offset, lvalueElementType);

        result = bfmgr.and(result,
                           makeDestructiveAssignment(newLvalue.getSecond(),
                                                     newRvalue.getSecond(),
                                                     newLvalue.getFirst(),
                                                     newRvalue.getFirst(),
                                                     useOldSSAIndices,
                                                     updatedTypes,
                                                     updatedVariables));
         offset += conv.getSizeof(lvalueArrayType.getType());
      }
      return result;
    } else if (lvalueType instanceof CCompositeType) {
      final CCompositeType lvalueCompositeType = (CCompositeType) lvalueType;
      assert lvalueCompositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + lvalueCompositeType;
      // There are two cases of assignment to a structure/union
      Preconditions.checkArgument(
          // Initialization with a value (possibly nondet), useful for stack declarations and memset implementation
          rvalue.isValue() && isSimpleType(rvalueType) ||
          // Structure assignment
          rvalueType.equals(lvalueType),
          "Impossible structure assignment due to incompatible types: assignment of %s to %s",
          rvalueType, lvalueType);
      result = bfmgr.makeBoolean(true);
      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : lvalueCompositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final CType newLvalueType = CTypeUtils.simplifyType(memberDeclaration.getType());
        // Optimizing away the assignments from uninitialized fields
        if (conv.isRelevantField(lvalueCompositeType, memberName) &&
             (!lvalue.isAliased() || // Assignment to a variable, no profit in optimizing it
              !isSimpleType(newLvalueType) || // That's not a simple assignment, check the nested composite
               rvalue.isValue() || // This is initialization, so the assignment is mandatory
               pts.tracksField(lvalueCompositeType, memberName) || // The field is tracked as essential
               // The variable representing the RHS was used somewhere (i.e. has SSA index)
               !rvalue.asLocation().isAliased() &&
                 conv.hasIndex(rvalue.asLocation().asUnaliased().getVariableName() +
                            CToFormulaWithUFConverter.FIELD_NAME_SEPARATOR +
                            memberName,
                          newLvalueType,
                          ssa))) {
          final Pair<? extends Location, CType> newLvalue =
                                         shiftCompositeLvalue(lvalue, offset, memberName, memberDeclaration.getType());
          final Pair<? extends Expression, CType> newRvalue =
                             shiftCompositeRvalue(rvalue, offset, memberName, rvalueType, memberDeclaration.getType());

          result = bfmgr.and(result,
                             makeDestructiveAssignment(newLvalue.getSecond(),
                                                       newRvalue.getSecond(),
                                                       newLvalue.getFirst(),
                                                       newRvalue.getFirst(),
                                                       useOldSSAIndices,
                                                       updatedTypes,
                                                       updatedVariables));
        }

        if (lvalueCompositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += conv.getSizeof(memberDeclaration.getType());
        }
      }
      return result;
    } else { // Simple assignment
      return makeSimpleDestructiveAssignment(lvalueType,
                                             rvalueType,
                                             lvalue,
                                             rvalue,
                                             useOldSSAIndices,
                                             updatedTypes,
                                             updatedVariables);
    }
  }

  private BooleanFormula makeSimpleDestructiveAssignment(@Nonnull CType lvalueType,
                                                         @Nonnull CType rvalueType,
                                                         final @Nonnull Location lvalue,
                                                               @Nonnull Expression rvalue,
                                                         final boolean useOldSSAIndices,
                                                         final @Nullable Set<CType> updatedTypes,
                                                         final @Nullable Set<Variable> updatedVariables)
  throws UnrecognizedCCodeException {
    lvalueType = CTypeUtils.simplifyType(lvalueType);
    rvalueType = CTypeUtils.simplifyType(rvalueType);
    rvalueType = implicitCastToPointer(rvalueType); // Arrays and functions are implicitly converted to pointers

    Preconditions.checkArgument(isSimpleType(lvalueType),
                                "To assign to/from arrays/structures/unions use makeDestructiveAssignment");
    Preconditions.checkArgument(isSimpleType(rvalueType),
                                "To assign to/from arrays/structures/unions use makeDestructiveAssignment");

    final Formula value;
    switch (rvalue.getKind()) {
    case ALIASED_LOCATION:
      value = conv.makeDereference(rvalueType, rvalue.asAliasedLocation().getAddress(), ssa, errorConditions);
      break;
    case UNALIASED_LOCATION:
      value = conv.makeVariable(rvalue.asUnaliasedLocation().getVariableName(), rvalueType, ssa);
      break;
    case DET_VALUE:
      value = rvalue.asValue().getValue();
      break;
    case NONDET:
      value = null;
      break;
    default: throw new AssertionError();
    }

    assert !(lvalueType instanceof CFunctionType) : "Can't assign to functions";

    final String targetName = !lvalue.isAliased() ? lvalue.asUnaliased().getVariableName() : CToFormulaWithUFConverter.getUFName(lvalueType);
    final FormulaType<?> targetType = conv.getFormulaTypeFromCType(lvalueType);
    final int oldIndex = conv.getIndex(targetName, lvalueType, ssa);
    final int newIndex = !useOldSSAIndices ? oldIndex + 1 : oldIndex;
    final BooleanFormula result;

    rvalueType = implicitCastToPointer(rvalueType);
    final Formula rhs = value != null ? conv.makeCast(rvalueType, lvalueType, value, edge) : null;
    if (!lvalue.isAliased()) { // Unaliased LHS
      if (rhs != null) {
        result = fmgr.makeEqual(fmgr.makeVariable(targetType, targetName, newIndex), rhs);
      } else {
        result = bfmgr.makeBoolean(true);
      }

      if (updatedVariables != null) {
        updatedVariables.add(Variable.create(targetName, lvalueType));
      }
    } else { // Aliased LHS
      final Formula lhs = ffmgr.createFuncAndCall(targetName,
                                                  newIndex,
                                                  targetType,
                                                  ImmutableList.of(lvalue.asAliased().getAddress()));
      if (rhs != null) {
        result = fmgr.makeEqual(lhs, rhs);
      } else {
        result = bfmgr.makeBoolean(true);
      }

      if (updatedTypes != null) {
        updatedTypes.add(lvalueType);
      }
    }

    return result;
  }

  private void addRetentionForAssignment(@Nonnull CType lvalueType,
                                         final @Nullable Formula startAddress,
                                         final @Nonnull PointerTargetPattern pattern,
                                         final Set<CType> typesToRetain) {
    lvalueType = CTypeUtils.simplifyType(lvalueType);
    final int size = conv.getSizeof(lvalueType);
    if (isSimpleType(lvalueType)) {
      Preconditions.checkArgument(startAddress != null,
                                  "Start address is mandatory for assigning to lvalues of simple types");
      final String ufName = CToFormulaWithUFConverter.getUFName(lvalueType);
      final int oldIndex = conv.getIndex(ufName, lvalueType, ssa);
      final int newIndex = oldIndex + 1;
      final FormulaType<?> targetType = conv.getFormulaTypeFromCType(lvalueType);
      addRetentionConstraints(pattern,
                              lvalueType,
                              ufName,
                              oldIndex,
                              newIndex,
                              targetType,
                              startAddress);
    } else if (pattern.isExact()) {
      pattern.setRange(size);
      for (final CType type : typesToRetain) {
        final String ufName = CToFormulaWithUFConverter.getUFName(type);
        final int oldIndex = conv.getIndex(ufName, type, ssa);
        final int newIndex = oldIndex + 1;
        final FormulaType<?> targetType = conv.getFormulaTypeFromCType(type);
        addRetentionConstraints(pattern, type, ufName, oldIndex, newIndex, targetType, null);
      }
    } else if (pattern.isSemiexact()) {
      Preconditions.checkArgument(startAddress != null,
                                  "Start address is mandatory for semiexact pointer target patterns");
      // For semiexact retention constraints we need the first element type of the composite
      if (lvalueType instanceof CArrayType) {
        lvalueType = CTypeUtils.simplifyType(((CArrayType) lvalueType).getType());
      } else { // CCompositeType
        lvalueType = CTypeUtils.simplifyType(((CCompositeType) lvalueType).getMembers().get(0).getType());
      }
      addSemiexactRetentionConstraints(pattern, lvalueType, startAddress, size, typesToRetain);
    } else { // Inexact pointer target pattern
      Preconditions.checkArgument(startAddress != null,
                                  "Start address is mandatory for inexact pointer target patterns");
      addInexactRetentionConstraints(startAddress, size, typesToRetain);
    }
  }

  private void addRetentionConstraints(final PointerTargetPattern pattern,
                                       final CType lvalueType,
                                       final String ufName,
                                       final int oldIndex,
                                       final int newIndex,
                                       final FormulaType<?> returnType,
                                       final Formula lvalue) {
    if (!pattern.isExact()) {
      for (final PointerTarget target : pts.getMatchingTargets(lvalueType, pattern)) {
        final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(conv.voidPointerFormulaType, target.getBaseName()),
                                                    fmgr.makeNumber(conv.voidPointerFormulaType, target.getOffset()));
        final BooleanFormula updateCondition = fmgr.makeEqual(targetAddress, lvalue);
        final BooleanFormula retention = fmgr.makeEqual(ffmgr.createFuncAndCall(ufName,
                                                                                newIndex,
                                                                                returnType,
                                                                                ImmutableList.of(targetAddress)),
                                                        ffmgr.createFuncAndCall(ufName,
                                                                                oldIndex,
                                                                                returnType,
                                                                                ImmutableList.of(targetAddress)));
       constraints.addConstraint(bfmgr.or(updateCondition, retention));
      }
    }
    for (final PointerTarget target : pts.getSpuriousTargets(lvalueType, pattern)) {
      final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(conv.voidPointerFormulaType, target.getBaseName()),
                                                  fmgr.makeNumber(conv.voidPointerFormulaType, target.getOffset()));
      constraints.addConstraint(fmgr.makeEqual(ffmgr.createFuncAndCall(ufName,
                                                                       newIndex,
                                                                       returnType,
                                                                       ImmutableList.of(targetAddress)),
                                               ffmgr.createFuncAndCall(ufName,
                                                                       oldIndex,
                                                                       returnType,
                                                                       ImmutableList.of(targetAddress))));
    }
  }

  private void addSemiexactRetentionConstraints(final PointerTargetPattern pattern,
                                                final CType firstElementType,
                                                final Formula startAddress,
                                                final int size,
                                                final Set<CType> types) {
    final PointerTargetPattern exact = new PointerTargetPattern();
    for (final PointerTarget target : pts.getMatchingTargets(firstElementType, pattern)) {
      final Formula candidateAddress = fmgr.makePlus(fmgr.makeVariable(conv.voidPointerFormulaType, target.getBaseName()),
                                                     fmgr.makeNumber(conv.voidPointerFormulaType, target.getOffset()));
      final BooleanFormula negAntecedent = bfmgr.not(fmgr.makeEqual(candidateAddress, startAddress));
      exact.setBase(target.getBase());
      exact.setRange(target.getOffset(), size);
      BooleanFormula consequent = bfmgr.makeBoolean(true);
      for (final CType type : types) {
        final String ufName = CToFormulaWithUFConverter.getUFName(type);
        final int oldIndex = conv.getIndex(ufName, type, ssa);
        final int newIndex = oldIndex + 1;
        final FormulaType<?> returnType = conv.getFormulaTypeFromCType(type);
        for (final PointerTarget spurious : pts.getSpuriousTargets(type, exact)) {
          final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(conv.voidPointerFormulaType, spurious.getBaseName()),
                                                      fmgr.makeNumber(conv.voidPointerFormulaType, spurious.getOffset()));
          consequent = bfmgr.and(consequent, fmgr.makeEqual(ffmgr.createFuncAndCall(ufName,
                                                                                    newIndex,
                                                                                    returnType,
                                                                                    ImmutableList.of(targetAddress)),
                                                            ffmgr.createFuncAndCall(ufName,
                                                                                    oldIndex,
                                                                                    returnType,
                                                                                    ImmutableList.of(targetAddress))));
        }
      }
      constraints.addConstraint(bfmgr.or(negAntecedent, consequent));
    }
  }

  private void addInexactRetentionConstraints(final Formula startAddress,
                                              final int size,
                                              final Set<CType> types) {
    final PointerTargetPattern any = new PointerTargetPattern();
    for (final CType type : types) {
      final String ufName = CToFormulaWithUFConverter.getUFName(type);
      final int oldIndex = conv.getIndex(ufName, type, ssa);
      final int newIndex = oldIndex + 1;
      final FormulaType<?> returnType = conv.getFormulaTypeFromCType(type);
      for (final PointerTarget spurious : pts.getSpuriousTargets(type, any)) {
        final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(conv.voidPointerFormulaType, spurious.getBaseName()),
                                      fmgr.makeNumber(conv.voidPointerFormulaType, spurious.getOffset()));
        final Formula endAddress = fmgr.makePlus(startAddress, fmgr.makeNumber(conv.voidPointerFormulaType, size));
        constraints.addConstraint(bfmgr.or(bfmgr.and(fmgr.makeLessOrEqual(startAddress, targetAddress, false),
                                                     fmgr.makeLessOrEqual(targetAddress, endAddress,false)),
                                           fmgr.makeEqual(ffmgr.createFuncAndCall(ufName,
                                                                                  newIndex,
                                                                                  returnType,
                                                                                  ImmutableList.of(targetAddress)),
                                           ffmgr.createFuncAndCall(ufName,
                                                                   oldIndex,
                                                                   returnType,
                                                                   ImmutableList.of(targetAddress)))));
      }
    }
  }

  private void updateSSA(final @Nonnull Set<CType> types, final SSAMapBuilder ssa) {
    for (final CType type : types) {
      final String ufName = CToFormulaWithUFConverter.getUFName(type);
      final int newIndex = conv.getIndex(ufName, type, ssa) + 1;
      ssa.setIndex(ufName, type, newIndex);
    }
  }

  private Pair<AliasedLocation, CType> shiftArrayLvalue(final AliasedLocation lvalue,
                                                        final int offset,
                                                        final CType lvalueElementType) {
    final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
    final AliasedLocation newLvalue = Location.ofAddress(fmgr.makePlus(lvalue.getAddress(), offsetFormula));
    return Pair.of(newLvalue, lvalueElementType);
  }

  private Pair<? extends Expression, CType> shiftArrayRvalue(final Expression rvalue,
                                                             final CType rvalueType,
                                                             final int offset,
                                                             final CType lvalueElementType) {
    // Support both initialization (with a value or nondet) and assignment (from another array location)
    switch(rvalue.getKind()) {
    case ALIASED_LOCATION: {
      assert rvalueType instanceof CArrayType : "Non-array rvalue in array assignment";
      final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
      final AliasedLocation newRvalue = Location.ofAddress(fmgr.makePlus(rvalue.asAliasedLocation().getAddress(),
                                                           offsetFormula));
      final CType newRvalueType = CTypeUtils.simplifyType(((CArrayType) rvalueType).getType());
      return Pair.of(newRvalue, newRvalueType);
    }
    case DET_VALUE: {
      return Pair.of(rvalue, rvalueType);
    }
    case NONDET: {
      final CType newLvalueType = isSimpleType(lvalueElementType) ? lvalueElementType : CNumericTypes.SIGNED_CHAR;
      return Pair.of(Value.nondetValue(), newLvalueType);
    }
    case UNALIASED_LOCATION: {
      throw new AssertionError("Array locations should always be aliased");
    }
    default: throw new AssertionError();
    }
  }

  private Pair<? extends Location, CType> shiftCompositeLvalue(final Location lvalue,
                                                               final int offset,
                                                               final String memberName,
                                                               final CType memberType) {
    final CType newLvalueType = CTypeUtils.simplifyType(memberType);
    if (lvalue.isAliased()) {
      final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
      final AliasedLocation newLvalue = Location.ofAddress(fmgr.makePlus(lvalue.asAliased().getAddress(),
                                                                         offsetFormula));
      return Pair.of(newLvalue, newLvalueType);

    } else {
      final UnaliasedLocation newLvalue = Location.ofVariableName(lvalue.asUnaliased().getVariableName() +
                                                                  CToFormulaWithUFConverter.FIELD_NAME_SEPARATOR + memberName);
      return Pair.of(newLvalue, newLvalueType);
    }

  }

  private Pair<? extends Expression, CType> shiftCompositeRvalue(final Expression rvalue,
                                                                 final int offset,
                                                                 final String memberName,
                                                                 final CType rvalueType,
                                                                 final CType memberType) {
    // Support both structure assignment and initialization with a value (or nondet)
    final CType newLvalueType = CTypeUtils.simplifyType(memberType);
    switch (rvalue.getKind()) {
    case ALIASED_LOCATION: {
      final Formula offsetFormula = fmgr.makeNumber(conv.voidPointerFormulaType, offset);
      final AliasedLocation newRvalue = Location.ofAddress(fmgr.makePlus(rvalue.asAliasedLocation().getAddress(),
                                                                         offsetFormula));
      return Pair.of(newRvalue, newLvalueType);
    }
    case UNALIASED_LOCATION: {
      final UnaliasedLocation newRvalue = Location.ofVariableName(rvalue.asUnaliasedLocation().getVariableName() +
                                                                  CToFormulaWithUFConverter.FIELD_NAME_SEPARATOR +
                                                                  memberName);
      return Pair.of(newRvalue, newLvalueType);
    }
    case DET_VALUE: {
      return Pair.of(rvalue, rvalueType);
    }
    case NONDET: {
      final CType newRvalueType = isSimpleType(newLvalueType) ? newLvalueType : CNumericTypes.SIGNED_CHAR;
      return Pair.of(Value.nondetValue(), newRvalueType);
    }
    default: throw new AssertionError();
    }
  }
}
