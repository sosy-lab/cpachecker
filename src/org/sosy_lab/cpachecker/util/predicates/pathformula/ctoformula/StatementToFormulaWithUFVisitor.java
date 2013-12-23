/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitExpressionValueVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.util.Expression;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.util.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.util.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.DeferredAllocationPool;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


public class StatementToFormulaWithUFVisitor extends ExpressionToFormulaWithUFVisitor
                                             implements CStatementVisitor<BooleanFormula, UnrecognizedCCodeException>,
                                                        CRightHandSideVisitor<Expression, UnrecognizedCCodeException> {

  public StatementToFormulaWithUFVisitor(final LvalueToPointerTargetPatternVisitor lvalueVisitor,
                                         final CToFormulaWithUFConverter cToFormulaConverter,
                                         final CFAEdge cfaEdge,
                                         final String function,
                                         final SSAMapBuilder ssa,
                                         final Constraints constraints,
                                         final PointerTargetSetBuilder pts) {
    super(cToFormulaConverter, cfaEdge, function, ssa, constraints, pts);

    this.statementDelegate = new StatementToFormulaVisitor(delegate);
    this.lvalueVisitor = lvalueVisitor;
    this.isRelevantLhsVisitor = new IsRelevantLhsVisitor();
  }

  @Override
  public BooleanFormula visit(final CExpressionAssignmentStatement e) throws UnrecognizedCCodeException {
    return visit((CAssignment) e);
  }

  @Override
  public BooleanFormula visit(final CFunctionCallAssignmentStatement e) throws UnrecognizedCCodeException {
    return visit((CAssignment) e);
  }

  @Override
  public BooleanFormula visit(CExpressionStatement s) {
    return statementDelegate.visit(s);
  }

  @Override
  public BooleanFormula visit(CFunctionCallStatement exp) throws UnrecognizedCCodeException {
    // this is an external call
    // visit expression in order to print warnings if necessary
    visit(exp.getFunctionCallExpression());
    return conv.bfmgr.makeBoolean(true);
  }

  private static void addEssentialFields(final List<Pair<CCompositeType, String>> fields,
                                         final PointerTargetSetBuilder pts) {
    for (final Pair<CCompositeType, String> field : fields) {
      if (!pts.addField(field.getFirst(), field.getSecond())) {
        pts.shallowRemoveField(field.getFirst(), field.getSecond());
      }
    }
  }

  private boolean isNondetFunctionName(final String name) {
    return conv.options.isNondetFunction(name);
  }

  private CType refineType(final @Nonnull CType type, final @Nonnull CIntegerLiteralExpression sizeLiteral) {
    if (sizeLiteral.getValue() != null) {
      final int size = sizeLiteral.getValue().intValue();
      final int typeSize = pts.getSize(type);
      if (type instanceof CArrayType) {
        if (typeSize != size) {
          conv.logger.logf(Level.WARNING,
                           "Array size of the revealed type differs form the allocation size: %s : %d != %d",
                           type,
                           typeSize,
                           size);
        }
        return type;
      } else {
        final int n = size / typeSize;
        final int remainder = size % typeSize;
        if (n == 0 || remainder != 0) {
          conv.logger.logf(Level.WARNING,
                           "Can't refine allocation type, but the sizes differ: %s : %d != %d",
                           type,
                           typeSize,
                           size);
          return type;
        }
        return new CArrayType(false, false, type, new CIntegerLiteralExpression(sizeLiteral.getFileLocation(),
                                                                                sizeLiteral.getExpressionType(),
                                                                                BigInteger.valueOf(n)));
      }
    } else {
      return type;
    }
  }

  private CType getAllocationType(final @Nonnull CType type, final @Nullable CIntegerLiteralExpression sizeLiteral) {
    if (type instanceof CPointerType) {
      return sizeLiteral != null ? refineType(((CPointerType) type).getType(), sizeLiteral) :
                                   ((CPointerType) type).getType();
    } else if (type instanceof CArrayType) {
      return sizeLiteral != null ? refineType(type, sizeLiteral) : type;
    } else {
      throw new IllegalArgumentException("Either pointer or array type expected");
    }
  }

  private void handleDeferredAllocationTypeRevelation(final @Nonnull String pointerVariable,
                                                      final @Nonnull CType type)
  throws UnrecognizedCCodeException {
    final DeferredAllocationPool deferredAllocationPool = pts.removeDeferredAllocation(pointerVariable);
    for (final String baseVariable : deferredAllocationPool.getBaseVariables()) {
      conv.makeAllocation(deferredAllocationPool.wasAllocationZeroing(),
                          getAllocationType(type, deferredAllocationPool.getSize()),
                          baseVariable,
                          edge,
                          ssa,
                          constraints,
                          pts);
    }
  }

  private void handleDeferredAllocationPointerRemoval(final String pointerVariable, final boolean isReturn) {
    if (pts.removeDeferredAllocatinPointer(pointerVariable)) {
      conv.logger.logfOnce(Level.WARNING,
                           (!isReturn ? "Assignment to the" : "Destroying the") +
                             " void * pointer  %s produces garbage! (in the following line(s):\n %s)",
                           pointerVariable,
                           edge);
    }
  }

  private void handleDeferredAllocationPointerEscape(final String pointerVariable)
  throws UnrecognizedCCodeException {
    final DeferredAllocationPool deferredAllocationPool = pts.removeDeferredAllocation(pointerVariable);
    final CIntegerLiteralExpression size = deferredAllocationPool.getSize() != null ?
                                             deferredAllocationPool.getSize() :
                                             new CIntegerLiteralExpression(
                                                   null,
                                                   CNumericTypes.SIGNED_CHAR,
                                                   BigInteger.valueOf(conv.options.defaultAllocationSize()));
    conv.logger.logfOnce(Level.WARNING,
                         "The void * pointer %s to a deferred allocation escaped form tracking! " +
                           "Allocating array void[%d]. (in the following line(s):\n %s)",
                         pointerVariable,
                         size.getValue(),
                         edge);
    for (final String baseVariable : deferredAllocationPool.getBaseVariables()) {
      conv.makeAllocation(deferredAllocationPool.wasAllocationZeroing(),
                          new CArrayType(false,
                                         false,
                                         CNumericTypes.VOID,
                                         size),
                          baseVariable,
                          edge,
                          ssa,
                          constraints,
                          pts);
    }
  }

  private void handleDeferredAllocationsInAssignment(final CLeftHandSide lhs,
                                                     final CRightHandSide rhs,
                                                     final Location lhsLocation,
                                                     final Expression rhsExpression,
                                                     final Map<String, CType> lhsUsedDeferredAllocationPointers,
                                                     final Map<String, CType> rhsUsedDeferredAllocationPointers)
  throws UnrecognizedCCodeException {
    boolean passed = false;
    for (final Map.Entry<String, CType> usedPointer : rhsUsedDeferredAllocationPointers.entrySet()) {
      boolean handled = false;
      if (ExpressionToFormulaWithUFVisitor.isRevealingType(usedPointer.getValue())) {
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
        handled = true;
      } else if (rhs instanceof CExpression &&
                 // TODO: use rhsExpression.isUnaliasedLocation() instead?
                 ExpressionToFormulaWithUFVisitor.isUnaliasedLocation((CExpression) rhs)) {
        assert rhsExpression.isUnaliasedLocation() &&
               rhsExpression.asUnaliasedLocation().getVariableName().equals(usedPointer.getKey()) &&
               rhsUsedDeferredAllocationPointers.size() == 1 :
               "Wrong assumptions on deferred allocations tracking: rhs is not a single pointer";
        final CType lhsType = PointerTargetSet.simplifyType(lhs.getExpressionType());
        if (lhsType.equals(CPointerType.POINTER_TO_VOID) &&
            // TODO: is the following isUnaliasedLocation() check really needed?
            ExpressionToFormulaWithUFVisitor.isUnaliasedLocation(lhs) &&
            !lhsLocation.isAliased()) {
          final Map.Entry<String, CType> lhsUsedPointer = !lhsUsedDeferredAllocationPointers.isEmpty() ?
                                                       lhsUsedDeferredAllocationPointers.entrySet().iterator().next() :
                                                       null;
          assert lhsUsedDeferredAllocationPointers.size() <= 1 &&
                 rhsExpression.isUnaliasedLocation() &&
                 (lhsUsedPointer == null ||
                  (rhsExpression.asUnaliasedLocation().getVariableName()).equals(lhsUsedPointer.getKey())) :
                 "Wrong assumptions on deferred allocations tracking: unrecognized lhs";
          if (lhsUsedPointer != null) {
            handleDeferredAllocationPointerRemoval(lhsUsedPointer.getKey(), false);
          }
          pts.addDeferredAllocationPointer(lhsLocation.asUnaliased().getVariableName(), usedPointer.getKey());
          passed = true;
          handled = true;
        } else if (ExpressionToFormulaWithUFVisitor.isRevealingType(lhsType)) {
          handleDeferredAllocationTypeRevelation(usedPointer.getKey(), lhsType);
          handled = true;
        }
      }
      if (!handled) {
        handleDeferredAllocationPointerEscape(usedPointer.getKey());
      }
    }
    for (final Map.Entry<String, CType> usedPointer : lhsUsedDeferredAllocationPointers.entrySet()) {
      if (!usedPointer.getValue().equals(CPointerType.POINTER_TO_VOID)) {
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
      // TODO: use lhsExpression.isUnaliasedLoation() instead (?)
      } else if (ExpressionToFormulaWithUFVisitor.isUnaliasedLocation(lhs)) {
        assert !lhsLocation.isAliased() &&
               lhsLocation.asUnaliased().getVariableName().equals(usedPointer.getKey()) &&
               lhsUsedDeferredAllocationPointers.size() == 1 :
               "Wrong assumptions on deferred allocations tracking: lhs is not a single pointer";
        if (!passed) {
          handleDeferredAllocationPointerRemoval(usedPointer.getKey(), false);
        }
      } else {
        handleDeferredAllocationPointerEscape(usedPointer.getKey());
      }
    }
  }

  private void handleDeferredAllocationsInAssume(final CExpression e,
                                                 final Map<String, CType> usedDeferredAllocationPointers)
  throws UnrecognizedCCodeException {
    for (final Map.Entry<String, CType> usedPointer : usedDeferredAllocationPointers.entrySet()) {
      if (!usedPointer.getValue().equals(CPointerType.POINTER_TO_VOID)) {
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
      } else if (e instanceof CBinaryExpression) {
        final CBinaryExpression binaryExpression = (CBinaryExpression) e;
        switch (binaryExpression.getOperator()) {
        case EQUALS:
        case GREATER_EQUAL:
        case GREATER_THAN:
        case LESS_EQUAL:
        case LESS_THAN:
          final CType operand1Type = PointerTargetSet.simplifyType(binaryExpression.getOperand1().getExpressionType());
          final CType operand2Type = PointerTargetSet.simplifyType(binaryExpression.getOperand2().getExpressionType());
          CType type = null;
          if (ExpressionToFormulaWithUFVisitor.isRevealingType(operand1Type)) {
            type = operand1Type;
          } else if (ExpressionToFormulaWithUFVisitor.isRevealingType(operand2Type)) {
            type = operand2Type;
          }
          if (type != null) {
            handleDeferredAllocationTypeRevelation(usedPointer.getKey(), type);
          }
          break;
        }
      }
    }
  }

  /**
   * The function removes local void * pointers (deferred allocations)
   * declared in current function scope from tracking after returning from the function.
   */
  void handleDeferredAllocationInFunctionExit() {
    for (final String variable : pts.getDeferredAllocationVariables()) {
      final int position = variable.indexOf(CToFormulaWithUFConverter.SCOPE_SEPARATOR);
      if (position >= 0) { // Consider only local variables (in current function scope)
        final String variableFunction = variable.substring(0, position);
        if (function.equals(variableFunction)) {
          handleDeferredAllocationPointerRemoval(variable, true);
        }
      }
    }
  }

  BooleanFormula handleAssignment(final CLeftHandSide lhs, final CRightHandSide rhs,
                                  final boolean batchMode,
                                  final Set<CType> destroyedTypes)
  throws UnrecognizedCCodeException {
    final CType lhsType = PointerTargetSet.simplifyType(lhs.getExpressionType());
    final CType rhsType = rhs != null ? PointerTargetSet.simplifyType(rhs.getExpressionType()) :
                                        CNumericTypes.SIGNED_CHAR;

    // RHS handling
    final ImmutableList<Pair<CCompositeType, String>> rhsUsedFields;
    final ImmutableMap<String, CType> rhsUsedDeferredAllocationPointers;
    final Expression rhsExpression;
    reset();
    // RHS is neither null nor a nondet() function call
    if (rhs != null &&
        (!(rhs instanceof CFunctionCallExpression) ||
         !(((CFunctionCallExpression) rhs).getFunctionNameExpression() instanceof CIdExpression) ||
         !isNondetFunctionName(
            ((CIdExpression)((CFunctionCallExpression) rhs).getFunctionNameExpression()).getName()))) {
      rhsExpression = rhs.accept(this);
      addEssentialFields(getInitializedFields(), pts);
      rhsUsedFields = getUsedFields();
      rhsUsedDeferredAllocationPointers = getUsedDeferredAllocationPointers();
    } else { // RHS is nondet
      rhsExpression = null;
      rhsUsedFields = ImmutableList.<Pair<CCompositeType,String>>of();
      rhsUsedDeferredAllocationPointers = ImmutableMap.<String, CType>of();
    }

    // LHS handling
    reset();
    final Location lhsLocation = lhs.accept(this).asLocation();
    addEssentialFields(getInitializedFields(), pts);
    final ImmutableList<Pair<CCompositeType, String>> lhsUsedFields = getUsedFields();
    // the pattern matching possibly aliased locations
    final PointerTargetPattern pattern = lhsLocation.isUnaliasedLocation() ? null : lhs.accept(lvalueVisitor);

    // Handle allocations: reveal the actual type form the LHS type or defer the allocation until later
    boolean isAllocation = false;
    if ((conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) &&
        rhs instanceof CFunctionCallExpression &&
        rhsExpression.isValue()) {
      final Set<String> rhsVariables = conv.fmgr.extractVariables(rhsExpression.asValue().getValue());
      // Actually there is always either 1 variable (just address) or 2 variables (nondet + allocation address)
      for (String variable : rhsVariables) {
        if (PointerTargetSet.isBaseName(variable)) {
          variable = PointerTargetSet.getBase(variable);
        }
        if (pts.isTemporaryDeferredAllocationPointer(variable)) {
          if (!isAllocation) {
            // We can reveal the type from the LHS
            if (ExpressionToFormulaWithUFVisitor.isRevealingType(lhsType)) {
              handleDeferredAllocationTypeRevelation(variable, lhsType);
            // We can defer the allocation and start tracking the variable in the LHS
            } else if (lhsType.equals(CPointerType.POINTER_TO_VOID) &&
                       // TODO: remove the double-check (?)
                       ExpressionToFormulaWithUFVisitor.isUnaliasedLocation(lhs) &&
                       lhsLocation.isUnaliasedLocation()) {
              final String variableName = lhsLocation.asUnaliasedLocation().getVariableName();
              if (pts.isDeferredAllocationPointer(variableName)) {
                handleDeferredAllocationPointerRemoval(variableName, false);
              }
              pts.addDeferredAllocationPointer(variableName, variable); // Now we track the LHS
              // And not the RHS, because the LHS is its only alias
              handleDeferredAllocationPointerRemoval(variable, false);
            } else {
              handleDeferredAllocationPointerEscape(variable);
            }
            isAllocation = true;
          } else {
            throw new UnrecognizedCCodeException("Can't handle ambiguous allocation", edge, rhs);
          }
        }
      }
    }

    // Track currently deferred allocations
    if (conv.options.deferUntypedAllocations() && !isAllocation) {
      handleDeferredAllocationsInAssignment(lhs,
                                            rhs,
                                            lhsLocation,
                                            rhsExpression,
                                            getUsedDeferredAllocationPointers(),
                                            rhsUsedDeferredAllocationPointers);
    }

    final BooleanFormula result =
      conv.makeAssignment(lhsType,
                          rhsType,
                          lhsLocation,
                          rhsExpression,
                          pattern,
                          batchMode,
                          destroyedTypes,
                          edge,
                          ssa,
                          constraints,
                          pts);

    addEssentialFields(lhsUsedFields, pts);
    addEssentialFields(rhsUsedFields, pts);
    return result;
  }

  class IsRelevantLhsVisitor extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

    @Override
    public Boolean visit(final CArraySubscriptExpression e) {
      return e.getArrayExpression().accept(this);
    }

    @Override
    public Boolean visit(final CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Boolean visit(final CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Boolean visit(final CFieldReference e) {
      CType fieldOwnerType = PointerTargetSet.simplifyType(e.getFieldOwner().getExpressionType());
      if (fieldOwnerType instanceof CPointerType) {
        fieldOwnerType = ((CPointerType) fieldOwnerType).getType();
      }
      assert fieldOwnerType instanceof CCompositeType : "Field owner should have composite type";
      return conv.isRelevantField((CCompositeType) fieldOwnerType, e.getFieldName(), pts);
    }

    @Override
    public Boolean visit(final CIdExpression e) {
      return conv.isRelevantVariable(e.getDeclaration().getQualifiedName());
    }

    @Override
    public Boolean visit(CPointerExpression e) {
      return true;
    }

    @Override
    protected Boolean visitDefault(CExpression e) {
      throw new IllegalArgumentException("Undexpected left hand side: " + e.toString());
    }
  }

  public BooleanFormula visit(final CAssignment e) throws UnrecognizedCCodeException {
    final CLeftHandSide lhs = e.getLeftHandSide();

    // Optimization for unused variables and fields
    if (lhs.accept(isRelevantLhsVisitor)) {
      return handleAssignment(lhs, e.getRightHandSide(), false, null);
    } else {
      return conv.bfmgr.makeBoolean(true);
    }
  }

  public BooleanFormula handleInitializationAssignments(final CLeftHandSide variable,
                                                        final List<CExpressionAssignmentStatement> assignments)
  throws UnrecognizedCCodeException {
    final Set<CType> destroyedTypes = new HashSet<>();
    BooleanFormula result = conv.bfmgr.makeBoolean(true);
    for (CExpressionAssignmentStatement assignment : assignments) {
      final CLeftHandSide lhs = assignment.getLeftHandSide();
      if (lhs.accept(isRelevantLhsVisitor)) {
        result = conv.bfmgr.and(result, handleAssignment(lhs, assignment.getRightHandSide(), true, destroyedTypes));
      }
    }
    final Location lhsLocation = variable.accept(this).asLocation();
    if (lhsLocation.isAliased()) {
      conv.finishAssignments(PointerTargetSet.simplifyType(variable.getExpressionType()),
                             lhsLocation.asAliased(),
                             variable.accept(lvalueVisitor),
                             destroyedTypes,
                             edge, ssa, constraints, pts);
    }
    return result;
  }

  public BooleanFormula visitAssume(final CExpression e, final boolean truthAssumtion)
  throws UnrecognizedCCodeException {
    reset();

    final CType expressionType = PointerTargetSet.simplifyType(e.getExpressionType());
    BooleanFormula result = conv.toBooleanFormula(asValueFormula(e.accept(this),
                                                                 expressionType));

    if (conv.options.deferUntypedAllocations()) {
      handleDeferredAllocationsInAssume(e, getUsedDeferredAllocationPointers());
    }

    if (!truthAssumtion) {
      result = conv.bfmgr.not(result);
    }

    addEssentialFields(getInitializedFields(), pts);
    addEssentialFields(getUsedFields(), pts);
    return result;
  }

  private static Integer tryEvaluateExpression(CExpression e) {
    if (e instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression)e).getValue().intValue();
    }
    return null;
  }

  private static boolean isSizeof(final CExpression e) {
    return e instanceof CUnaryExpression && ((CUnaryExpression) e).getOperator() == UnaryOperator.SIZEOF ||
           e instanceof CTypeIdExpression && ((CTypeIdExpression) e).getOperator() == TypeIdOperator.SIZEOF;
  }

  private static boolean isSizeofMultilple(final CExpression e) {
    return e instanceof CBinaryExpression &&
           ((CBinaryExpression) e).getOperator() == BinaryOperator.MULTIPLY &&
           (isSizeof(((CBinaryExpression) e).getOperand1()) ||
            isSizeof(((CBinaryExpression) e).getOperand2()));
  }

  private static CType getSizeofType(CExpression e) {
    if (e instanceof CUnaryExpression &&
        ((CUnaryExpression) e).getOperator() == UnaryOperator.SIZEOF) {
      return PointerTargetSet.simplifyType(((CUnaryExpression) e).getOperand().getExpressionType());
    } else if (e instanceof CTypeIdExpression &&
               ((CTypeIdExpression) e).getOperator() == TypeIdOperator.SIZEOF) {
      return PointerTargetSet.simplifyType(((CTypeIdExpression) e).getType());
    } else {
      return null;
    }
  }

  @Override
  public Value visit(final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    final CExpression functionNameExpression = e.getFunctionNameExpression();
    final CType returnType = PointerTargetSet.simplifyType(e.getExpressionType());
    final List<CExpression> parameters = e.getParameterExpressions();

    // First let's handle special cases such as assumes, allocations, nondets, external models, etc.
    final String functionName;
    if (functionNameExpression instanceof CIdExpression) {
      functionName = ((CIdExpression) functionNameExpression).getName();
      if (functionName.equals(CToFormulaWithUFConverter.ASSUME_FUNCTION_NAME) && parameters.size() == 1) {
        final BooleanFormula condition = visitAssume(parameters.get(0), true);
        constraints.addConstraint(condition);
        return Value.ofValue(conv.makeFreshVariable(functionName, returnType, ssa, pts));

      } else if ((conv.options.isSuccessfulAllocFunctionName(functionName) ||
                  conv.options.isSuccessfulZallocFunctionName(functionName))) {
        return Value.ofValue(handleSucessfulMemoryAllocation(functionName, e));

      } else if ((conv.options.isMemoryAllocationFunction(functionName) ||
                  conv.options.isMemoryAllocationFunctionWithZeroing(functionName))) {
        return Value.ofValue(handleMemoryAllocation(e, (CIdExpression)functionNameExpression, functionName));

      } else if (conv.options.isMemoryFreeFunction(functionName)) {
        return Value.ofValue(handleMemoryFree(e, parameters));

      } else if (conv.options.isNondetFunction(functionName)) {
        return null; // Nondet

      } else if (conv.options.isExternModelFunction(functionName)) {
        return Value.ofValue(statementDelegate.handleExternModelFunction(e, parameters));

      } else if (CtoFormulaConverter.UNSUPPORTED_FUNCTIONS.containsKey(functionName)) {
        throw new UnsupportedCCodeException(CtoFormulaConverter.UNSUPPORTED_FUNCTIONS.get(functionName), edge, e);

      } else if (!CtoFormulaConverter.PURE_EXTERNAL_FUNCTIONS.contains(functionName)) {
        if (parameters.isEmpty()) {
          // function of arity 0
          conv.logger.logfOnce(Level.INFO,
                               "Assuming external function %s to be a constant function.",
                               functionName);
        } else {
          conv.logger.logfOnce(Level.INFO,
                               "Assuming external function %s to be a pure function.",
                               functionName);
        }
      }
    } else {
      conv.logger.logfOnce(Level.WARNING,
                           "Ignoring function call through function pointer %s",
                           e);
      functionName = "<func>{" +
                     CtoFormulaConverter.scoped(CtoFormulaConverter.exprToVarName(functionNameExpression),
                                                getDelegate().function) +
                     "}";
    }

    // Pure functions returning composites are unsupported, return null that'll be interpreted as nondet
    final CType resultType = PointerTargetSet.simplifyType(conv.getReturnType(e, edge));
    if (resultType instanceof CCompositeType ||
        PointerTargetSet.containsArray(resultType)) {
      conv.logger.logfOnce(Level.WARNING,
                           "Pure function %s returning a composite is treated as nondet.", e);
      return null;
    }

    // Now let's handle "normal" functions assumed to be pure
    if (parameters.isEmpty()) {
      // This is a function of arity 0 and we assume its constant.
      return Value.ofValue(conv.makeConstant(CToFormulaWithUFConverter.UF_NAME_PREFIX + functionName, returnType, pts));
    } else {
      final CFunctionDeclaration functionDeclaration = e.getDeclaration();
      if (functionDeclaration == null) {
        if (functionNameExpression instanceof CIdExpression) {
          // This happens only if there are undeclared functions.
          conv.logger.logfOnce(Level.WARNING, "Cannot get declaration of function %s, ignoring calls to it.",
                               functionNameExpression);
        }
        return null; // Nondet
      }

      if (functionDeclaration.getType().takesVarArgs()) {
        // Return nondet instead of an UF for vararg functions.
        // This is sound but slightly more imprecise (we loose the UF axioms).
        return null; // Nondet
      }

      final List<CType> parameterTypes = functionDeclaration.getType().getParameters();
      // functionName += "{" + parameterTypes.size() + "}";
      // add #arguments to function name to cope with vararg functions
      // TODO: Handled above?
      if (parameterTypes.size() != parameters.size()) {
        throw new UnrecognizedCCodeException("Function " + functionDeclaration + " received " +
                                             parameters.size() + " parameters instead of the expected " +
                                             parameterTypes.size(),
                                             edge,
                                             e);
      }

      final List<Formula> arguments = new ArrayList<>(parameters.size());
      final Iterator<CType> parameterTypesIterator = parameterTypes.iterator();
      final Iterator<CExpression> parametersIterator = parameters.iterator();
      while (parameterTypesIterator.hasNext() && parametersIterator.hasNext()) {
        final CType parameterType= parameterTypesIterator.next();
        CExpression parameter = parametersIterator.next();
        parameter = conv.makeCastFromArrayToPointerIfNecessary(parameter, parameterType);

        final Formula argument = asValueFormula(parameter.accept(this), parameterType);
        arguments.add(conv.makeCast(parameter.getExpressionType(), parameterType, argument, edge));
      }
      assert !parameterTypesIterator.hasNext() && !parametersIterator.hasNext();

      final FormulaType<?> resultFormulaType = conv.getFormulaTypeFromCType(resultType, pts);
      return Value.ofValue(conv.ffmgr.createFuncAndCall(CToFormulaWithUFConverter.UF_NAME_PREFIX + functionName,
                                                        resultFormulaType,
                                                        arguments));
    }
  }

  /**
   * Handle memory allocation functions that may fail (i.e., return null)
   * and that may or may not zero the memory.
   */
  private Formula handleMemoryAllocation(final CFunctionCallExpression e,
      final CIdExpression functionNameExpression, final String functionName) throws UnrecognizedCCodeException {
    final boolean isZeroing = conv.options.isMemoryAllocationFunctionWithZeroing(functionName);
    List<CExpression> parameters = e.getParameterExpressions();

    if (functionName.equals(CALLOC_FUNCTION) && parameters.size() == 2) {
      CExpression param0 = parameters.get(0);
      CExpression param1 = parameters.get(1);

      // Build expression for param0 * param1 as new parameter.
      CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(conv.machineModel, conv.logger);
      CBinaryExpression multiplication = builder.buildBinaryExpression(
          param0, param1, BinaryOperator.MULTIPLY);

      // Try to evaluate the multiplication if possible.
      Integer value0 = tryEvaluateExpression(param0);
      Integer value1 = tryEvaluateExpression(param1);
      if (value0 != null && value1 != null) {
        long result = ExplicitExpressionValueVisitor.calculateBinaryOperation(
            value0.longValue(), value1.longValue(), multiplication,
            conv.machineModel, conv.logger, edge);

        CExpression newParam = new CIntegerLiteralExpression(param0.getFileLocation(),
                                                 multiplication.getExpressionType(),
                                                 BigInteger.valueOf(result));
        parameters = Collections.singletonList(newParam);
      } else {
        parameters = Collections.<CExpression>singletonList(multiplication);
      }

    } else if (parameters.size() != 1) {
      if (parameters.size() > 1 && conv.options.hasSuperfluousParameters(functionName)) {
        parameters = Collections.singletonList(parameters.get(0));
      } else {
        throw new UnrecognizedCCodeException(
            String.format("Memory allocation function %s() called with %d parameters instead of 1",
                          functionName, parameters.size()), edge, e);
      }
    }

    final String delegateFunctionName = !isZeroing ?
                                          conv.options.getSuccessfulAllocFunctionName() :
                                          conv.options.getSuccessfulZallocFunctionName();
    final CExpression delegateFuncitonNameExpression = new CIdExpression(functionNameExpression.getFileLocation(),
                                                                         functionNameExpression.getExpressionType(),
                                                                         delegateFunctionName,
                                                                         functionNameExpression.getDeclaration());
    final CFunctionCallExpression delegateCall =
      new CFunctionCallExpression(e.getFileLocation(),
                                  CPointerType.POINTER_TO_VOID,
                                  delegateFuncitonNameExpression,
                                  parameters,
                                  e.getDeclaration());
    if (!conv.options.makeMemoryAllocationsAlwaysSucceed()) {
      final Formula nondet = conv.makeFreshVariable(functionName,
                                                    CPointerType.POINTER_TO_VOID,
                                                    ssa,
                                                    pts);
      return conv.bfmgr.ifThenElse(conv.bfmgr.not(conv.fmgr.makeEqual(nondet, conv.nullPointer)),
                                    visit(delegateCall).asValue().getValue(),
                                    conv.nullPointer);
    } else {
      return visit(delegateCall).asValue().getValue();
    }
  }

  /**
   * Handle memory allocation functions that cannot fail
   * (i.e., do not return NULL) and do not zero the memory.
   */
  private Formula handleSucessfulMemoryAllocation(final String functionName,
      final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    List<CExpression> parameters = e.getParameterExpressions();
    if (parameters.size() != 1) {
      if (parameters.size() > 1 && conv.options.hasSuperfluousParameters(functionName)) {
        parameters = Collections.singletonList(parameters.get(0));
      } else {
        throw new UnrecognizedCCodeException(
            String.format("Memory allocation function %s() called with %d parameters instead of 1",
                          functionName, parameters.size()), edge, e);
      }
    }

    final CExpression parameter = parameters.get(0);
    Integer size = null;
    final CType newType;
    if (isSizeof(parameter)) {
      newType = getSizeofType(parameter);
    } else if (isSizeofMultilple(parameter)) {
      final CBinaryExpression product = (CBinaryExpression) parameter;
      final CType operand1Type = getSizeofType(product.getOperand1());
      final CType operand2Type = getSizeofType(product.getOperand2());
      if (operand1Type != null) {
        newType = new CArrayType(false, false, operand1Type, product.getOperand2());
      } else if (operand2Type != null) {
        newType = new CArrayType(false, false, operand2Type, product.getOperand1());
      } else {
        throw new UnrecognizedCCodeException("Can't determine type for internal memory allocation", edge, e);
      }
    } else {
      size = tryEvaluateExpression(parameter);
      if (!conv.options.revealAllocationTypeFromLHS() && !conv.options.deferUntypedAllocations()) {
        final CExpression length;
        if (size == null) {
          size = conv.options.defaultAllocationSize();
          length = new CIntegerLiteralExpression(parameter.getFileLocation(),
                                                 parameter.getExpressionType(),
                                                 BigInteger.valueOf(size));
        } else {
          length = parameter;
        }
        newType = new CArrayType(false, false, CNumericTypes.VOID, length);
      } else {
        newType = null;
      }
    }
    Formula address;
    if (newType != null) {
      final CType newBaseType = PointerTargetSet.getBaseType(newType);
      final String newBase = conv.makeAllocVariableName(functionName, newType, newBaseType);
      address =  conv.makeAllocation(conv.options.isSuccessfulZallocFunctionName(functionName),
                                 newType,
                                 newBase,
                                 edge,
                                 ssa,
                                 constraints,
                                 pts);
    } else {
      final String newBase = conv.makeAllocVariableName(functionName,
                                                            CNumericTypes.VOID,
                                                            CPointerType.POINTER_TO_VOID);
      pts.addTemporaryDeferredAllocation(conv.options.isSuccessfulZallocFunctionName(functionName),
                                         size != null ? new CIntegerLiteralExpression(parameter.getFileLocation(),
                                                                                      parameter.getExpressionType(),
                                                                                      BigInteger.valueOf(size)) :
                                                        null,
                                         newBase);
      address = conv.makeConstant(PointerTargetSet.getBaseName(newBase), CPointerType.POINTER_TO_VOID, pts);
    }

    return address;
  }

  /**
   * Handle calls to free()
   */
  private Formula handleMemoryFree(final CFunctionCallExpression e,
      final List<CExpression> parameters) throws UnrecognizedCCodeException {
    if (parameters.size() != 1) {
      throw new UnrecognizedCCodeException(
          String.format("free() called with %d parameters", parameters.size()), edge, e);
    }

    return null; // free does not return anything, so nondet is ok
  }

  public String getFuncitonName() {
    return function;
  }

  public void declareSharedBase(final CDeclaration declaration, final boolean shareImmediately) {
    if (shareImmediately) {
      conv.addPreFilledBase(declaration.getQualifiedName(), declaration.getType(), false, false, constraints, pts);
    } else if (conv.isAddressedVariable(declaration.getQualifiedName())) {
      constraints.addConstraint(pts.prepareBase(declaration.getQualifiedName(), declaration.getType()));
    }
  }

  public void declareSharedBase(final CParameterDeclaration declaration, final boolean shareImmediately) {
    if (shareImmediately) {
      conv.addPreFilledBase(declaration.getQualifiedName(), declaration.getType(), false, false, constraints, pts);
    } else if (conv.isAddressedVariable(declaration.getQualifiedName())) {
      constraints.addConstraint(pts.prepareBase(declaration.getQualifiedName(), declaration.getType()));
    }
  }

  public void declareCompositeType(final CCompositeType compositeType) {
    pts.addCompositeType(compositeType);
  }

  private final StatementToFormulaVisitor statementDelegate;
  private final LvalueToPointerTargetPatternVisitor lvalueVisitor;
  private final IsRelevantLhsVisitor isRelevantLhsVisitor;

  private static final String CALLOC_FUNCTION = "calloc";
}
