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

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.DeferredAllocationPool;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;


public class StatementToFormulaWithUFVisitor extends StatementToFormulaVisitor {

  public StatementToFormulaWithUFVisitor(final ExpressionToFormulaWithUFVisitor delegate,
                                         final LvalueToPointerTargetPatternVisitor lvalueVisitor,
                                         final CToFormulaWithUFConverter conv,
                                         final ErrorConditions errorConditions,
                                         final PointerTargetSetBuilder pts) {
    super(delegate);
    this.delegate = delegate;
    this.lvalueVisitor = lvalueVisitor;
    this.conv = conv;
    this.errorConditions = errorConditions;
    this.pts = pts;
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

//  private static void addBases(final List<Pair<String, CType>> bases, final PointerTargetSetBuilder pts) {
//    for (final Pair<String, CType> base : bases) {
//      pts.addBase(base.getFirst(), base.getSecond());
//    }
//  }
//
//  private static void addFields(final List<Pair<CCompositeType, String>> fields, final PointerTargetSetBuilder pts) {
//    for (final Pair<CCompositeType, String> field : fields) {
//      pts.addField(field.getFirst(), field.getSecond());
//    }
//  }

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
                                             new CIntegerLiteralExpression(null,
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
                                                     final Object lhsTarget,
                                                     final Object rhsTarget,
                                                     final Map<String, CType> lhsUsedDeferredAllocationPointers,
                                                     final Map<String, CType> rhsUsedDeferredAllocationPointers)
  throws UnrecognizedCCodeException {
    boolean passed = false;
    for (final Map.Entry<String, CType> usedPointer : rhsUsedDeferredAllocationPointers.entrySet()) {
      boolean handled = false;
      if (!usedPointer.getValue().equals(CPointerType.POINTER_TO_VOID)) {
        handleDeferredAllocationTypeRevelation(usedPointer.getKey(), usedPointer.getValue());
        handled = true;
      } else if (rhs instanceof CExpression && ExpressionToFormulaWithUFVisitor.isSimpleTarget((CExpression) rhs)) {
        assert rhsTarget instanceof String &&
               ((String) rhsTarget).equals(usedPointer.getKey()) &&
               rhsUsedDeferredAllocationPointers.size() == 1 :
               "Wrong assumptions on deferred allocations tracking: rhs is not a single pointer";
        final CType lhsType = PointerTargetSet.simplifyType(lhs.getExpressionType());
        if (lhsType.equals(CPointerType.POINTER_TO_VOID) &&
            ExpressionToFormulaWithUFVisitor.isSimpleTarget(lhs) &&
            lhsTarget instanceof String) {
          final Map.Entry<String, CType> lhsUsedPointer = !lhsUsedDeferredAllocationPointers.isEmpty() ?
                                                       lhsUsedDeferredAllocationPointers.entrySet().iterator().next() :
                                                       null;
          assert lhsUsedDeferredAllocationPointers.size() <= 1 &&
                 lhsTarget instanceof String &&
                 (lhsUsedPointer == null || ((String) lhsTarget).equals(lhsUsedPointer.getKey())) :
                 "Wrong assumptions on deferred allocations tracking: unrecognized lhs";
          if (lhsUsedPointer != null) {
            handleDeferredAllocationPointerRemoval(lhsUsedPointer.getKey(), false);
          }
          pts.addDeferredAllocationPointer((String) lhsTarget, usedPointer.getKey());
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
      } else if (ExpressionToFormulaWithUFVisitor.isSimpleTarget(lhs)) {
        assert lhsTarget instanceof String &&
               ((String) lhsTarget).equals(usedPointer.getKey()) &&
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

  BooleanFormula handleAssignment(final CLeftHandSide lhs, final CRightHandSide rhs)
  throws UnrecognizedCCodeException {
    final CType lhsType = PointerTargetSet.simplifyType(lhs.getExpressionType());
    final CType rhsType = rhs != null ? PointerTargetSet.simplifyType(rhs.getExpressionType()) :
                            CNumericTypes.SIGNED_CHAR;

    final ImmutableList<Pair<CCompositeType, String>> rhsUsedFields;
    final ImmutableMap<String, CType> rhsUsedDeferredAllocationPointers;
    final Formula rhsFormula;
    delegate.reset();
    if (rhs != null &&
        (!(rhs instanceof CFunctionCallExpression) ||
         !(((CFunctionCallExpression) rhs).getFunctionNameExpression() instanceof CIdExpression) ||
         !isNondetFunctionName(
            ((CIdExpression)((CFunctionCallExpression) rhs).getFunctionNameExpression()).getName()))) {
      rhsFormula = rhs.accept(this);
      // addBases(delegate.getSharedBases(), pts);
      addEssentialFields(delegate.getInitializedFields(), pts);
      rhsUsedFields = delegate.getUsedFields();
      rhsUsedDeferredAllocationPointers = delegate.getUsedDeferredAllocationPointers();
    } else {
      rhsFormula = null;
      rhsUsedFields = ImmutableList.<Pair<CCompositeType,String>>of();
      rhsUsedDeferredAllocationPointers = ImmutableMap.<String, CType>of();
    }
    final String rhsName = delegate.getLastTarget() instanceof String ? (String) delegate.getLastTarget() : null;
    final Object rhsObject = !(rhsType instanceof CCompositeType) || rhsName == null ? rhsFormula : rhsName;

    delegate.reset();
    lhs.accept(delegate);
    // addBases(delegate.getSharedBases(), pts);
    addEssentialFields(delegate.getInitializedFields(), pts);
    final ImmutableList<Pair<CCompositeType, String>> lhsUsedFields = delegate.getUsedFields();
    final Object lastTarget = delegate.getLastTarget();
    assert lastTarget instanceof String || lastTarget instanceof Formula;
    final PointerTargetPattern pattern = lastTarget instanceof String ? null : lhs.accept(lvalueVisitor);

    boolean isAllocation = false;
    if ((conv.options.revealAllocationTypeFromLHS() || conv.options.deferUntypedAllocations()) &&
        rhs instanceof CFunctionCallExpression &&
        rhsObject instanceof Formula) {
      final Set<String> rhsVariables = conv.fmgr.extractVariables(rhsFormula);
      for (String variable : rhsVariables) {
        if (PointerTargetSet.isBaseName(variable)) {
          variable = PointerTargetSet.getBase(variable);
        }
        if (pts.isTemporaryDeferredAllocationPointer(variable)) {
          if (!isAllocation) {
            if (ExpressionToFormulaWithUFVisitor.isRevealingType(lhsType)) {
              handleDeferredAllocationTypeRevelation(variable, lhsType);
            } else if (lhsType.equals(CPointerType.POINTER_TO_VOID) &&
                       ExpressionToFormulaWithUFVisitor.isSimpleTarget(lhs) &&
                       lastTarget instanceof String) {
              if (pts.isDeferredAllocationPointer((String) lastTarget)) {
                handleDeferredAllocationPointerRemoval((String) lastTarget, false);
              }
              pts.addDeferredAllocationPointer((String) lastTarget, variable);
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

    if (conv.options.deferUntypedAllocations() && !isAllocation) {
      handleDeferredAllocationsInAssignment(lhs,
                                            rhs,
                                            lastTarget,
                                            rhsName,
                                            delegate.getUsedDeferredAllocationPointers(),
                                            rhsUsedDeferredAllocationPointers);
    }

    final BooleanFormula result =
      conv.makeAssignment(lhsType,
                          rhsType,
                          lastTarget,
                          rhsObject,
                          pattern,
                          true,
                          false,
                          null,
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

  @Override
  public BooleanFormula visit(final CAssignment e) throws UnrecognizedCCodeException {
    final CLeftHandSide lhs = e.getLeftHandSide();

    // Optimization for unused variables and fields
    if (lhs.accept(isRelevantLhsVisitor)) {
      return handleAssignment(lhs, e.getRightHandSide());
    } else {
      return conv.bfmgr.makeBoolean(true);
    }
  }

  public BooleanFormula visitComplexInitialization(final CDeclaration declaration, final List<?> initializerList)
  throws UnrecognizedCCodeException {
    final CType type = PointerTargetSet.simplifyType(declaration.getType());
    final String lhs = declaration.getQualifiedName();
    if (!pts.isActualBase(declaration.getQualifiedName()) && !PointerTargetSet.containsArray(type)) {
      declareSharedBase(declaration, false);
      return conv.makeAssignment(type,
                                 type,
                                 lhs,
                                 initializerList,
                                 null,
                                 true,
                                 false,
                                 null,
                                 edge,
                                 ssa,
                                 constraints,
                                 pts);
    } else {
      final PointerTargetPattern pattern = new PointerTargetPattern(lhs, 0, 0);
      final CType baseType = PointerTargetSet.getBaseType(type);
      final BooleanFormula result = conv.makeAssignment(type,
                                                        type,
                                                        conv.makeConstant(Variable.create(
                                                                            PointerTargetSet.getBaseName(lhs),
                                                                            baseType),
                                                                          ssa,
                                                                          pts),
                                                        initializerList,
                                                        pattern,
                                                        false,
                                                        false,
                                                        null,
                                                        edge,
                                                        ssa,
                                                        constraints,
                                                        pts);
      conv.addPreFilledBase(lhs, type, false, true, constraints, pts);
      return result;
    }
  }

  public BooleanFormula visitAssume(final CExpression e, final boolean truthAssumtion)
  throws UnrecognizedCCodeException {
    delegate.reset();

    BooleanFormula result = conv.toBooleanFormula(e.accept(delegate));

    if (conv.options.deferUntypedAllocations()) {
      handleDeferredAllocationsInAssume(e, delegate.getUsedDeferredAllocationPointers());
    }

    if (!truthAssumtion) {
      result = conv.bfmgr.not(result);
    }
    // addBases(delegate.getSharedBases(), pts);
    addEssentialFields(delegate.getInitializedFields(), pts);
    addEssentialFields(delegate.getUsedFields(), pts);
    return result;
  }

  private CInitializerList stringLiteralToInitializerList(final CStringLiteralExpression e,
                                                          final CArrayType type) {
    Integer length = PointerTargetSet.getArrayLength(type);
    final String s = e.getContentString();
    if (length == null) {
      length = s.length() + 1;
    }
    assert length >= s.length();
    // http://stackoverflow.com/a/6915917
    // As the C99 Draft Specification's 32nd Example in §6.7.8 (p. 130) states
    // char s[] = "abc", t[3] = "abc"; is identical to: char s[] = { 'a', 'b', 'c', '\0' }, t[] = { 'a', 'b', 'c' };
    final boolean zeroTerminated = length >= s.length() + 1;
    final List<CInitializer> initializers = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      initializers.add(new CInitializerExpression(
                             e.getFileLocation(),
                             new CCharLiteralExpression(e.getFileLocation(),
                                                        CNumericTypes.SIGNED_CHAR,
                                                        s.charAt(i))));
    }
    if (zeroTerminated) {
      initializers.add(new CInitializerExpression(
                             e.getFileLocation(),
                             new CCharLiteralExpression(e.getFileLocation(), CNumericTypes.SIGNED_CHAR, '\0')));
    }
    return new CInitializerList(e.getFileLocation(), initializers);
  }

  public Object visitInitializer(CType type, CInitializer topInitializer, final boolean isAutomatic)
  throws UnrecognizedCCodeException {
    type = PointerTargetSet.simplifyType(type);
    final Formula zero = conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(CNumericTypes.SIGNED_CHAR, pts), 0);
    if (type instanceof CArrayType) {
      if (topInitializer instanceof CInitializerExpression &&
          ((CArrayType) type).getType() instanceof CSimpleType &&
          ((CSimpleType) ((CArrayType) type).getType()).getType() == CBasicType.CHAR &&
          ((CInitializerExpression) topInitializer).getExpression() instanceof CStringLiteralExpression) {
        topInitializer = stringLiteralToInitializerList(
          (CStringLiteralExpression) ((CInitializerExpression) topInitializer).getExpression(),
          (CArrayType) type);
      }
      assert topInitializer instanceof CInitializerList : "Wrong array initializer";
      final CInitializerList initializerList = (CInitializerList) topInitializer;
      final CType elementType = PointerTargetSet.simplifyType(((CArrayType) type).getType());
      Integer length = PointerTargetSet.getArrayLength((CArrayType)type);
      if (length == null) {
        length = initializerList.getInitializers().size();
      }
      if (length == null) {
        throw new UnrecognizedCCodeException("Can't evaluate array size for initialization", edge, initializerList);
      }
      assert length >= initializerList.getInitializers().size() : "Initializer is larger than the array";
      final List<Object> result = new ArrayList<>(length);
      for (int i = 0; i < length; ++i) {
        if (isAutomatic) {
          result.add(zero);
        } else {
          result.add(null);
        }
      }
      int index = 0;
      for (final CInitializer initializer : initializerList.getInitializers()) {
        if (!(initializer instanceof CDesignatedInitializer)) {
          result.set(index, visitInitializer(elementType, initializer, isAutomatic));
        } else {
          final CDesignatedInitializer designatedInitializer = (CDesignatedInitializer) initializer;
          final List<CDesignator> designators = designatedInitializer.getDesignators();
          final CDesignator designator;
          if (designators.size() > 1) {
            conv.logger.logfOnce(Level.WARNING,
                                 "Nested designators are unsupported: %s in line %d",
                                 designatedInitializer,
                                 designatedInitializer.getFileLocation().getStartingLineNumber());
            continue;
          } else {
            designator = Iterables.getOnlyElement(designators);
          }
          final Object rhs = visitInitializer(elementType, designatedInitializer.getRightHandSide(), isAutomatic);
          if (designator instanceof CArrayRangeDesignator) {
            final Integer floor = tryEvaluateExpression(((CArrayRangeDesignator) designator).getFloorExpression());
            final Integer ceiling = tryEvaluateExpression(((CArrayRangeDesignator) designator).getFloorExpression());
            if (floor != null && ceiling != null) {
              for (int i = floor; i <= ceiling; i++) {
                result.set(i, rhs);
              }
              index = ceiling;
            } else {
              throw new UnrecognizedCCodeException("Can't evaluate array range designator bounds", edge, designator);
            }
          } else if (designator instanceof CArrayDesignator) {
            final Integer subscript = tryEvaluateExpression(((CArrayDesignator) designator).getSubscriptExpression());
            if (subscript != null) {
              index = subscript;
              result.set(index, rhs);
            } else {
              throw new UnrecognizedCCodeException("Can't evaluate array designator subscript", edge, designator);
            }
          }
        }
        ++index;
      }
      return result;
    } else if (type instanceof CCompositeType && ((CCompositeType) type).getKind() == ComplexTypeKind.STRUCT) {
      assert topInitializer instanceof CInitializerList : "Wrong structure initializer";
      final CInitializerList initializerList = (CInitializerList) topInitializer;
      final CCompositeType compositeType = (CCompositeType) type;
      final int size = compositeType.getMembers().size();
      final Map<String, Pair<Integer, CType>> members = new HashMap<>(size);
      final List<CType> memberTypes = new ArrayList<>(size);
      int index = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final CType memberType = PointerTargetSet.simplifyType(memberDeclaration.getType());
        members.put(memberDeclaration.getName(), Pair.of(index, memberType));
        memberTypes.add(memberType);
        index++;
      }
      final List<Object> result = new ArrayList<>(size);
      for (int i = 0; i < size; ++i) {
        if (isAutomatic) {
          result.add(zero);
        } else {
          result.add(null);
        }
      }
      index = 0;
      for (final CInitializer initializer : initializerList.getInitializers()) {
        if (!(initializer instanceof CDesignatedInitializer)) {
          result.set(index, visitInitializer(memberTypes.get(index), initializer, isAutomatic));
        } else {
          final CDesignatedInitializer designatedInitializer = (CDesignatedInitializer) initializer;
          final List<CDesignator> designators = designatedInitializer.getDesignators();
          final CDesignator designator;
          if (designators.size() > 1) {
            conv.logger.logfOnce(Level.WARNING,
                                 "Nested designators are unsupported: %s in line %d",
                                 designatedInitializer,
                                 designatedInitializer.getFileLocation().getStartingLineNumber());
            continue;
          } else {
            designator = Iterables.getOnlyElement(designators);
          }
          if (designator instanceof CFieldDesignator) {
            final Pair<Integer, CType> indexType = members.get(((CFieldDesignator) designator).getFieldName());
            final Object rhs = visitInitializer(indexType.getSecond(),
                                                designatedInitializer.getRightHandSide(),
                                                isAutomatic);
            result.set(indexType.getFirst(), rhs);
          } else {
            throw new UnrecognizedCCodeException("Wrong designator", edge, designator);
          }
        }
        index++;
      }
      return result;
    } else if (type instanceof CCompositeType && ((CCompositeType) type).getKind() == ComplexTypeKind.UNION) {
      if (topInitializer instanceof CInitializerList  &&
          ((CInitializerList) topInitializer).getInitializers().size() <= 1) {
        final CCompositeType compositeType = (CCompositeType) type;
        final int membersCount = compositeType.getMembers().size();
        final List<Object> result = new ArrayList<>(membersCount);
        for (int i = 0; i < membersCount; ++i) {
          result.add(null);
        }
        if (((CInitializerList) topInitializer).getInitializers().size() == 0) {
          result.set(0, visitInitializer(compositeType.getMembers().get(0).getType(),
                                         topInitializer,
                                         isAutomatic));
        } else {
          topInitializer = ((CInitializerList) topInitializer).getInitializers().get(0);
          if (!(topInitializer instanceof CDesignatedInitializer)) {
            result.set(0, visitInitializer(compositeType.getMembers().get(0).getType(),
                                           topInitializer,
                                           isAutomatic));
          } else {
            final CDesignatedInitializer designatedInitializer = (CDesignatedInitializer) topInitializer;
            final CDesignator designator = designatedInitializer.getDesignators().get(0);
            if (designator instanceof CFieldDesignator) {
              final String fieldName = ((CFieldDesignator) designator).getFieldName();
              int index = 0;
              for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
                if (memberDeclaration.getName().equals(fieldName)) {
                  final CType memberType = PointerTargetSet.simplifyType(memberDeclaration.getType());
                  final CInitializer newInitializer;
                  if (designatedInitializer.getDesignators().size() == 1) {
                    newInitializer = designatedInitializer.getRightHandSide();
                  } else {
                    newInitializer = new CDesignatedInitializer(designatedInitializer.getFileLocation(),
                                                                designatedInitializer.getDesignators().subList(
                                                                  1,
                                                                  designatedInitializer.getDesignators().size()),
                                                                designatedInitializer.getRightHandSide());
                  }
                  result.set(index, visitInitializer(memberType, newInitializer, isAutomatic));
                  break;
                }
                index++;
              }
              if (index >= compositeType.getMembers().size()) {
                throw new UnrecognizedCCodeException("Unrecognized field designator", edge, designator);
              }
            } else {
              throw new UnrecognizedCCodeException("Wrong designator: field designator expected", edge, designator);
            }
          }
        }
        return result;
      } else {
        throw new UnrecognizedCCodeException("Wrong union initializer: one-element list expected",
                                             edge,
                                             topInitializer);
      }
    } else {
      assert topInitializer instanceof CInitializerExpression : "Unrecognized initializer";
      final CExpression initializerExpression = ((CInitializerExpression) topInitializer).getExpression();
      final Formula initializer = initializerExpression.accept(this);
      final CType initializerExpressionType = CToFormulaWithUFConverter.implicitCastToPointer(
                                                                          initializerExpression.getExpressionType());
      return conv.makeCast(initializerExpressionType, type, initializer, edge);
    }
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
  public Formula visit(final CFunctionCallExpression e) throws UnrecognizedCCodeException {
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
        return conv.makeFreshVariable(functionName, returnType, ssa, pts);
      } else if ((conv.options.isSuccessfulAllocFunctionName(functionName) ||
                  conv.options.isSuccessfulZallocFunctionName(functionName)) &&
                  parameters.size() >= 1) {
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
        if (newType != null) {
          final CType newBaseType = PointerTargetSet.getBaseType(newType);
          final String newBase = conv.makeAllocVariableName(functionName, newType, newBaseType);
          return conv.makeAllocation(conv.options.isSuccessfulZallocFunctionName(functionName),
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
          return conv.makeConstant(PointerTargetSet.getBaseName(newBase), CPointerType.POINTER_TO_VOID, ssa, pts);
        }
      } else if ((conv.options.isMemoryAllocationFunction(functionName) ||
                  conv.options.isMemoryAllocationFunctionWithZeroing(functionName)) &&
                  parameters.size() >= 1) {
        final String delegateFunctionName = !conv.options.isMemoryAllocationFunctionWithZeroing(functionName) ?
                                              conv.options.getSuccessfulAllocFunctionName() :
                                              conv.options.getSuccessfulZallocFunctionName();
        final CExpression delegateFuncitonNameExpression = new CIdExpression(functionNameExpression.getFileLocation(),
                                                                             functionNameExpression.getExpressionType(),
                                                                             delegateFunctionName,
                                                                             ((CIdExpression) functionNameExpression)
                                                                               .getDeclaration());
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
                                        visit(delegateCall),
                                        conv.nullPointer);
        } else {
          return visit(delegateCall);
        }

      } else if (conv.options.isMemoryFreeFunction(functionName)
          && parameters.size() == 1) {

        final Formula operand = parameters.get(0).accept(delegate);
        BooleanFormula validFree = conv.fmgr.makeEqual(operand, conv.nullPointer);

        for (String base : pts.getAllBases()) {
          Formula baseF = conv.makeConstant(PointerTargetSet.getBaseName(base), CPointerType.POINTER_TO_VOID, ssa, pts);
          validFree = conv.bfmgr.or(validFree,
              conv.fmgr.makeEqual(operand, baseF)
              );
        }
        errorConditions.addInvalidFreeCondition(conv.bfmgr.not(validFree));
        return null; // free does not return anything, so nondet is ok

      } else if (conv.options.isNondetFunction(functionName)) {
        return null; // Nondet
      } else if (conv.options.isExternModelFunction(functionName)) {
        assert parameters.size() > 0 : "No external model given!";
        // the parameter comes in C syntax (with ")
        final String fileName = parameters.get(0).toASTString().replaceAll("\"", "");
        final File modelFile = new File(fileName);
        final BooleanFormula externalModel = loadExternalFormula(modelFile);
        final FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(returnType, pts);
        return conv.bfmgr.ifThenElse(externalModel,
                                     conv.fmgr.makeNumber(returnFormulaType, 1),
                                     conv.fmgr.makeNumber(returnFormulaType, 0));
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
                                                function) +
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
      return conv.makeConstant(CToFormulaWithUFConverter.UF_NAME_PREFIX + functionName, returnType, ssa, pts);
    } else {
      final CFunctionDeclaration functionDeclaration = e.getDeclaration();
      if (functionDeclaration == null) {
        if (functionNameExpression instanceof CIdExpression) {
          // This happens only if there are undeclared functions.
          conv.logger.logfOnce(Level.WARNING, "Cannot get declaration of function %s, ignoring calls to it.", functionNameExpression);
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

        final Formula argument = parameter.accept(this);
        arguments.add(conv.makeCast(parameter.getExpressionType(), parameterType, argument, edge));
      }
      assert !parameterTypesIterator.hasNext() && !parametersIterator.hasNext();

      final FormulaType<?> resultFormulaType = conv.getFormulaTypeFromCType(resultType, pts);
      return conv.ffmgr.createFuncAndCall(CToFormulaWithUFConverter.UF_NAME_PREFIX + functionName,
                                          resultFormulaType,
                                          arguments);
    }
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

  @SuppressWarnings("hiding")
  private final CToFormulaWithUFConverter conv;
  private final ErrorConditions errorConditions;
  private final PointerTargetSetBuilder pts;
  @SuppressWarnings("hiding")
  private final ExpressionToFormulaWithUFVisitor delegate;
  private final LvalueToPointerTargetPatternVisitor lvalueVisitor;
  private final IsRelevantLhsVisitor isRelevantLhsVisitor;
}
