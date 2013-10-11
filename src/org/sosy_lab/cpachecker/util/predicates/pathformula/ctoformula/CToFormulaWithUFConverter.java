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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.RationalFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PathFormulaWithUF;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class CToFormulaWithUFConverter extends CtoFormulaConverter {

  public static CToFormulaWithUFConverter create(final Configuration config,
                                                 final FormulaManagerView fmgr,
                                                 final MachineModel machineModel,
                                                 final LogManager logger)
  throws InvalidConfigurationException {
    return new CToFormulaWithUFConverter(config, fmgr, machineModel, logger);
  }

  CToFormulaWithUFConverter(final Configuration config,
                            final FormulaManagerView formulaManagerView,
                            final MachineModel machineModel,
                            final LogManager logger)
  throws InvalidConfigurationException {
    super(config, formulaManagerView, machineModel, logger);
    rfmgr = formulaManagerView.getRationalFormulaManager();
  }

  private static String getUFName(final CType type) {
    return "*" + PointerTargetSet.typeToString(type).replace(' ', '_');
  }

  static String getReturnVarName() {
    return RETURN_VARIABLE_NAME;
  }

  @Override
  @Deprecated
  public FormulaType<?> getFormulaTypeFromCType(final CType type) {
    // throw new UnsupportedOperationException("Use the method with pts argument instead");
    return super.getFormulaTypeFromCType(type);
  }

  public FormulaType<?> getFormulaTypeFromCType(final CType type, @Nullable final PointerTargetSetBuilder pts) {
    final int size = pts != null ? pts.getSize(type) : super.getSizeof(type);
    final int bitsPerByte = machineModel.getSizeofCharInBits();
    return efmgr.getFormulaType(size * bitsPerByte);
  }

  private void checkSsaSavedType(final String name, final CType type, final SSAMapBuilder ssa) {
    CType ssaSavedType = ssa.getType(name);
    if (ssaSavedType != null) {
      ssaSavedType = PointerTargetSet.simplifyType(ssaSavedType);
    }
    if (ssaSavedType != null &&
        !ssaSavedType.equals(PointerTargetSet.simplifyType(type)) /*&&
        (!(type instanceof CPointerType) ||
         !ssaSavedType.equals(PointerTargetSet.simplifyType(((CPointerType) type).getType())))*/) {
      logger.logf(Level.FINEST,
                  "Variable %s was found with multiple types! (Type1: %s, Type2: %s)",
                  name,
                  ssaSavedType,
                  type);
    }
  }

  @Override
  int getIndex(final String name, final CType type, final SSAMapBuilder ssa) {
    int index = ssa.getIndex(name);
    if (index <= 0) {
      logger.log(Level.ALL, "WARNING: Auto-instantiating variable:", name);
      index = 1;
      checkSsaSavedType(name, type, ssa);
      ssa.setIndex(name, type, index);
    } else {
      checkSsaSavedType(name, type, ssa);
    }
    return index;
  }

  @Override
  @Deprecated
  Formula makeConstant(final String name, final CType type, final SSAMapBuilder ssa) {
    throw new UnsupportedOperationException("Use the method with pts argument instead");
  }

  @Override
  @Deprecated
  Formula makeConstant(final Variable var, final SSAMapBuilder ssa) {
    throw new UnsupportedOperationException("Use the method with pts argument instead");
  }

  Formula makeConstant(final String name,
                       final CType type,
                       final SSAMapBuilder ssa,
                       final PointerTargetSetBuilder pts) {
    return fmgr.makeVariable(getFormulaTypeFromCType(type, pts), name);
  }

  Formula makeConstant(final Variable var, final SSAMapBuilder ssa, final PointerTargetSetBuilder pts) {
    return makeConstant(var.getName(), var.getType(), ssa, pts);
  }

  @Override
  @Deprecated
  Formula makeVariable(final String name, final CType type, final SSAMapBuilder ssa) {
    throw new UnsupportedOperationException("Use the method with pts argument instead");
  }

  @Override
  @Deprecated
  Formula makeVariable(final Variable var, final SSAMapBuilder ssa) {
    throw new UnsupportedOperationException("Use the method with pts argument instead");
  }

  Formula makeVariable(final String name,
                       final CType type,
                       final SSAMapBuilder ssa,
                       final PointerTargetSetBuilder pts) {
    final int index = getIndex(name, type, ssa);
    return fmgr.makeVariable(getFormulaTypeFromCType(type, pts), name, index);
  }

  Formula makeVariable(final Variable var, final SSAMapBuilder ssa, final PointerTargetSetBuilder pts) {
    return makeVariable(var.getName(), var.getType(), ssa, pts);
  }

  @Override
  @Deprecated
  Formula makeFreshVariable(final String name, final CType type, final SSAMapBuilder ssa) {
    throw new UnsupportedOperationException("Use the method with pts argument instead");
  }

  Formula makeFreshVariable(final String name,
                            final CType type,
                            final SSAMapBuilder ssa,
                            final PointerTargetSetBuilder pts) {
    final int oldIndex = getIndex(name, type, ssa);
    final int newIndex = oldIndex + 1;
    ssa.setIndex(name, type, newIndex);
    return fmgr.makeVariable(getFormulaTypeFromCType(type, pts), name, newIndex);
  }

  Formula makeDereferece(CType type,
                         final Formula address,
                         final SSAMapBuilder ssa,
                         final PointerTargetSetBuilder pts) {
    type = PointerTargetSet.simplifyType(type);
    final String ufName = getUFName(type);
    final int index = getIndex(ufName, type, ssa);
    final FormulaType<?> returnType = getFormulaTypeFromCType(type, pts);
    return ffmgr.createFuncAndCall(ufName, index, returnType, ImmutableList.of(address));
  }

  void addSharingConstraints(final CFAEdge cfaEdge,
                             final Formula address,
                             final Variable base,
                             final List<Pair<CCompositeType, String>> fields,
                             final SSAMapBuilder ssa,
                             final Constraints constraints,
                             final PointerTargetSetBuilder pts) throws UnrecognizedCCodeException {
    final CType baseType = PointerTargetSet.simplifyType(base.getType());
    if (baseType instanceof CArrayType) {
      throw new UnrecognizedCCodeException("Array access can't be encoded as a varaible", cfaEdge);
    } else if (baseType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) baseType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final CType memberType = PointerTargetSet.simplifyType(memberDeclaration.getType());
        final Variable newBase = Variable.create(base.getName() + BaseVisitor.NAME_SEPARATOR + memberName,
                                                 memberType);
        if (getIndex(newBase.getName(), newBase.getType(), ssa) > 1) {
          fields.add(Pair.of(compositeType, memberName));
          addSharingConstraints(cfaEdge,
                                fmgr.makePlus(address, fmgr.makeNumber(pts.getPointerType(), offset)),
                                newBase,
                                fields,
                                ssa,
                                constraints,
                                pts);
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += pts.getSize(memberType);
        }
      }
    } else {
      constraints.addConstraint(fmgr.makeEqual(makeDereferece(baseType, address, ssa, pts),
                                               makeVariable(base, ssa, pts)));
    }
  }

  boolean isCompositeType(CType type) {
    type = PointerTargetSet.simplifyType(type);
    assert !(type instanceof CElaboratedType) : "Unresolved elaborated type";
    assert !(type instanceof CCompositeType) || ((CCompositeType) type).getKind() == ComplexTypeKind.STRUCT ||
                                                ((CCompositeType) type).getKind() == ComplexTypeKind.UNION :
           "Enums are not composite";
    return type instanceof CArrayType || type instanceof CCompositeType;
  }

  private void addRetentionConstraints(final PointerTargetPattern pattern,
                                       final CType lvalueType,
                                       final String ufName,
                                       final int oldIndex,
                                       final int newIndex,
                                       final FormulaType<?> returnType,
                                       final Formula lvalue,
                                       final Constraints constraints,
                                       final PointerTargetSetBuilder pts) {
    if (!pattern.isExact()) {
      for (final PointerTarget target : pts.getMatchingTargets(lvalueType, pattern)) {
        final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(pts.getPointerType(), target.getBase()),
                                                    fmgr.makeNumber(pts.getPointerType(), target.getOffset()));
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
      final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(pts.getPointerType(), target.getBase()),
                                                  fmgr.makeNumber(pts.getPointerType(), target.getOffset()));
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
                                                final List<CType> types,
                                                final SSAMapBuilder ssa,
                                                final Constraints constraints,
                                                final PointerTargetSetBuilder pts) {
    final PointerTargetPattern exact = new PointerTargetPattern();
    for (final PointerTarget target : pts.getMatchingTargets(firstElementType, pattern)) {
      final Formula candidateAddress = fmgr.makePlus(fmgr.makeVariable(pts.getPointerType(), target.getBase()),
                                                     fmgr.makeNumber(pts.getPointerType(), target.getOffset()));
      final BooleanFormula negAntecedent = bfmgr.not(fmgr.makeEqual(candidateAddress, startAddress));
      exact.setBase(target.getBase());
      exact.setRange(target.getOffset(), size);
      BooleanFormula consequent = bfmgr.makeBoolean(true);
      for (final CType type : types) {
        final String ufName = getUFName(type);
        final int oldIndex = getIndex(ufName, type, ssa);
        final int newIndex = oldIndex + 1;
        final FormulaType<?> returnType = getFormulaTypeFromCType(type, pts);
        for (final PointerTarget spurious : pts.getSpuriousTargets(type, exact)) {
          final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(pts.getPointerType(), spurious.getBase()),
                                                      fmgr.makeNumber(pts.getPointerType(), spurious.getOffset()));
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
                                              final List<CType> types,
                                              final SSAMapBuilder ssa,
                                              final Constraints constraints,
                                              final PointerTargetSetBuilder pts) {
    final PointerTargetPattern any = new PointerTargetPattern();
    for (final CType type : types) {
      final String ufName = getUFName(type);
      final int oldIndex = getIndex(ufName, type, ssa);
      final int newIndex = oldIndex + 1;
      final FormulaType<?> returnType = getFormulaTypeFromCType(type, pts);
      for (final PointerTarget spurious : pts.getSpuriousTargets(type, any)) {
        final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(pts.getPointerType(), spurious.getBase()),
                                      fmgr.makeNumber(pts.getPointerType(), spurious.getOffset()));
        final Formula endAddress = fmgr.makePlus(startAddress, fmgr.makeNumber(pts.getPointerType(), size));
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

  private void updateSSA(final List<CType> types,
                        final SSAMapBuilder ssa) {
    for (final CType type : types) {
      final String ufName = getUFName(type);
      final int newIndex = getIndex(ufName, type, ssa) + 1;
      ssa.setIndex(ufName, type, newIndex);
    }
  }

  /**
   * Returns a <code>BooleanFormula</code> (equality) representing the specified assignment.
   * Adds the corresponding memory retention constraints to the <code>constraints</code> argument.
   * This function can be used for any type of assignment:
   * structure assignments, structure/array initializations, assignments to pure variables as well as updating
   * uninterpreted function versions. All these assignments are supported by this function.
   * @param lvalueType &mdash; type of the left hand side (can be composite e.g. structure/array)
   * <p>
   * @param rvalueType &mdash; type of the right hand side (can be composite). The special case is when
   * <code>rvalueType</code> is simple, but <code>lvalueType</code>
   * is composite. This is treated as initialization (filling) of left hand side composite with the same simple
   * value (assigning the same value to every structure field / array element).
   * </p>
   * <p>
   * @param lvalue &mdash; the left hand side. There are two possibilities:
   * <ul>
   * <li><code>Formula</code> representing address for assignment;</li>
   * <li><code>String</code> representing pure variable name prefix (without field name or SSA index)</li>
   * </ul>
   * </p>
   * <p>
   * @param rvalue &mdash; the right hand side. There are five possibilities:
   * <ul>
   * <li><code>null</code> -- a non-determined value</li>
   * <li>a <code>Formula</code> representing a simple value -- a simple value to assign to the variable or to
   * initialize the composite with</li>
   * <li>a <code>Formula</code> representing composite address -- starting address of a composite for structure
   * assignment
   * <li>a <code>String</code> representing pure variable name prefix -- for pure structure assignment
   * <li> nested lists of formulas for structure initialization</li>
   * </ul>
   * </p>
   * @param pattern &mdash; pointer target pattern to match possible tracked addresses that can be represented by the
   * <code>lvalue</code> formula. Must be <code>null</code> in case of assignment to a pure variable.
   * @param batch If equal to <code>true</code>, only <code>BooleanFormula</code> representing the assignment
   * will be returned. The <code>constraints</code> will remain unchanged.
   * Useful for optimizing independent successive (kind of parallel) assignments.
   * @param ssa &mdash; the SSA map
   * @param constraints &mdash; the <code>Constraints</code> object to add resulting constraints if necessary
   * @param pts &mdash; the pointer target set with all tracked memory addresses
   * @return The boolean formula (equality) representing the assignment specified
   */
  BooleanFormula makeAssignment(@Nonnull CType lvalueType,
                                @Nonnull CType rvalueType,
                                final @Nonnull Object lvalue,
                                      @Nullable Object rvalue,
                                final @Nullable PointerTargetPattern pattern,
                                final boolean batch,
                                List<CType> types,
                                final @Nonnull SSAMapBuilder ssa,
                                final @Nonnull Constraints constraints,
                                final @Nonnull PointerTargetSetBuilder pts) {
    Preconditions.checkArgument(lvalue instanceof Formula || lvalue instanceof String, "Illegal left hand side");
    Preconditions.checkArgument(rvalue == null || rvalue instanceof String || rvalue instanceof Formula ||
                                rvalue instanceof List,
                                "Illegal right hand side");
    lvalueType = PointerTargetSet.simplifyType(lvalueType);
    rvalueType = PointerTargetSet.simplifyType(rvalueType);
    BooleanFormula result;
    if (lvalueType instanceof CArrayType) {
      Preconditions.checkArgument(lvalue instanceof Formula && pattern != null,
                                  "Array elements can't be represented as variables, but pure LHS is given");
      Preconditions.checkArgument(rvalue == null || rvalue instanceof Formula || rvalue instanceof List,
                                  "Array elements can't be represented as variables, but pure RHS is given");
      final CArrayType lvalueArrayType = (CArrayType) lvalueType;
      final CType lvalueElementType = PointerTargetSet.simplifyType(lvalueArrayType.getType());
      if (!(rvalueType instanceof CArrayType) ||
          PointerTargetSet.simplifyType(((CArrayType) rvalueType).getType())
            .equals(lvalueElementType.getCanonicalType())) {
        Integer length = lvalueArrayType.getLength().accept(pts.getEvaluatingVisitor());
        if (length == null) {
          length = PointerTargetSet.DEFAULT_ARRAY_LENGTH;
        }
        if (!(rvalue instanceof List) || ((List<?>) rvalue).size() >= length) {
          final Iterator<?> rvalueIterator = rvalue instanceof List ? ((List<?>) rvalue).iterator() : null;
          if (!batch) {
            types = new ArrayList<>();
          }
          result = bfmgr.makeBoolean(true);
          int offset = 0;
          for (int i = 0; i < length; ++i) {
            final CType newRvalueType = rvalueType instanceof CArrayType ?
                                          PointerTargetSet.simplifyType(((CArrayType) rvalueType).getType()) :
                                          rvalueType;
            final Formula offsetFormula = fmgr.makeNumber(pts.getPointerType(), offset);
            final Formula newLvalue = fmgr.makePlus((Formula) lvalue, offsetFormula);
            final Object newRvalue = rvalue == null ? null :
                                     rvalue instanceof Formula ?
                                       rvalueType instanceof CArrayType ?
                                         fmgr.makePlus((Formula) rvalue, offsetFormula) :
                                         rvalue :
                                     rvalueIterator.next();
            result = bfmgr.and(result,
                               makeAssignment(lvalueElementType,
                                              newRvalueType,
                                              newLvalue,
                                              newRvalue,
                                              pattern,
                                              true,
                                              types,
                                              ssa,
                                              constraints,
                                              pts));
            offset += pts.getSize(lvalueArrayType.getType());
          }
          if (!batch) {
            if (pattern.isExact()) {
              pattern.setRange(offset);
              for (final CType type : types) {
                final String ufName = getUFName(type);
                final int oldIndex = getIndex(ufName, type, ssa);
                final int newIndex = oldIndex + 1;
                final FormulaType<?> returnType = getFormulaTypeFromCType(type, pts);
                addRetentionConstraints(pattern, type, ufName, oldIndex, newIndex, returnType, null, constraints, pts);
              }
            } else if (pattern.isSemiexact()) {
              addSemiexactRetentionConstraints(pattern, lvalueElementType, (Formula) lvalue, offset, types, ssa,
                                               constraints, pts);
            } else {
              addInexactRetentionConstraints((Formula) lvalue, offset, types, ssa, constraints, pts);
            }
            updateSSA(types, ssa);
          }
          return result;
        } else {
          throw new IllegalArgumentException("Wrong array initializer");
        }
      } else {
        throw new IllegalArgumentException("Assigning incompatible array");
      }
    } else if (lvalueType instanceof CCompositeType) {
      final CCompositeType lvalueCompositeType = (CCompositeType) lvalueType;
      assert lvalueCompositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + lvalueCompositeType;
      if (!(rvalueType instanceof CCompositeType) ||
          rvalueType.getCanonicalType().equals(lvalueType.getCanonicalType())) {
        if (!(rvalue instanceof List) || ((List<?>) rvalue).size() >= lvalueCompositeType.getMembers().size()) {
          final Iterator<?> rvalueIterator = rvalue instanceof List ? ((List<?>) rvalue).iterator() : null;
          if (!batch) {
            types = new ArrayList<>();
          }
          result = bfmgr.makeBoolean(true);
          int offset = 0;
          for (final CCompositeTypeMemberDeclaration memberDeclaration : lvalueCompositeType.getMembers()) {
            final String memberName = memberDeclaration.getName();
            if (lvalue instanceof String || (pts.tracksField(lvalueCompositeType, memberName))) {
              final CType newLvalueType = PointerTargetSet.simplifyType(memberDeclaration.getType());
              final CType newRvalueType = rvalueType instanceof CCompositeType ? newLvalueType : rvalueType;
              final Formula offsetFormula = fmgr.makeNumber(pts.getPointerType(), offset);
              final Object newLvalue = lvalue instanceof Formula ?
                                         fmgr.makePlus((Formula) lvalue, offsetFormula) :
                                         lvalue + BaseVisitor.NAME_SEPARATOR + memberName;
              final Object newRvalue = rvalue == null ? null:
                                       rvalue instanceof String ? rvalue + BaseVisitor.NAME_SEPARATOR + memberName :
                                       rvalue instanceof Formula ?
                                         rvalueType instanceof CCompositeType ?
                                           fmgr.makePlus((Formula) rvalue, offsetFormula) :
                                           rvalue :
                                       rvalueIterator.next();
              result = bfmgr.and(result,
                                 makeAssignment(newLvalueType,
                                                newRvalueType,
                                                newLvalue,
                                                newRvalue,
                                                pattern,
                                                lvalue instanceof Formula,
                                                types,
                                                ssa,
                                                constraints,
                                                pts));
            }
            if (lvalueCompositeType.getKind() == ComplexTypeKind.STRUCT) {
              offset += pts.getSize(memberDeclaration.getType());
            }
          }
          if (!batch && pattern != null) {
            if (pattern.isExact()) {
              pattern.setRange(offset);
              for (final CType type : types) {
                final String ufName = getUFName(type);
                final int oldIndex = getIndex(ufName, type, ssa);
                final int newIndex = oldIndex + 1;
                final FormulaType<?> returnType = getFormulaTypeFromCType(type, pts);
                addRetentionConstraints(pattern, type, ufName, oldIndex, newIndex, returnType, null, constraints, pts);
              }
            } else if (pattern.isSemiexact()) {
              addSemiexactRetentionConstraints(pattern,
                                               PointerTargetSet.simplifyType(
                                                 lvalueCompositeType.getMembers().get(0).getType()),
                                               (Formula) lvalue,
                                               offset,
                                               types,
                                               ssa,
                                               constraints,
                                               pts);
            } else {
              addInexactRetentionConstraints((Formula) lvalue, offset, types, ssa, constraints, pts);
            }
            updateSSA(types, ssa);
          }
          return result;
        } else {
          throw new IllegalArgumentException("Wrong composite initializer");
        }
      } else {
        throw new IllegalArgumentException("Assigning incompatible composite");
      }
    } else {
      // This happens within recursive call for pure structure field assignment
      if (rvalue instanceof String) {
        assert ((String) rvalue).contains("$");
        rvalue = makeVariable((String) rvalue, rvalueType, ssa, pts);
      }
      assert rvalue == null || rvalue instanceof Formula : "Illegal right hand side";
      assert !(lvalueType instanceof CFunctionType) : "Can't assign to functions";
      final String targetName = lvalue instanceof String ? (String) lvalue : getUFName(lvalueType);
      final FormulaType<?> targetType = getFormulaTypeFromCType(lvalueType, pts);
      final int oldIndex = getIndex(targetName, lvalueType, ssa);
      final int newIndex = oldIndex + 1;
      if (rvalue != null) {
        if (rvalueType instanceof CArrayType) {
          rvalueType = new CPointerType(false, false,
                                        PointerTargetSet.simplifyType(((CArrayType) rvalueType).getType()));
        }
        final Formula rhs = makeCast(rvalueType, lvalueType, (Formula) rvalue);
        if (lvalue instanceof String) {
          result = fmgr.makeEqual(fmgr.makeVariable(targetType, targetName, newIndex), rhs);
        } else {
          final Formula lhs = ffmgr.createFuncAndCall(targetName,
                                                      newIndex,
                                                      targetType,
                                                      ImmutableList.of((Formula) lvalue));
          result = fmgr.makeEqual(lhs, rhs);
        }
      } else {
        result = bfmgr.makeBoolean(true);
      }
      if (!batch) {
        if (lvalue instanceof Formula) {
          addRetentionConstraints(pattern,
                                  lvalueType,
                                  targetName,
                                  oldIndex,
                                  newIndex,
                                  targetType,
                                  (Formula) lvalue,
                                  constraints,
                                  pts);
        }
        ssa.setIndex(targetName, lvalueType, newIndex);
      }
      return result;
    }
  }

  @Override
  <T extends Formula> BooleanFormula toBooleanFormula(T f) {
    // If this is not a predicate, make it a predicate by adding a "!= 0"
    if (!(f instanceof BooleanFormula)) {
      T zero = fmgr.makeNumber(fmgr.getFormulaType(f), 0);
      return bfmgr.not(fmgr.makeEqual(f, zero));
    } else {
      return (BooleanFormula) f;
    }
  }

  @Override
  public PathFormulaWithUF makeAnd(final PathFormula oldFormula, final CFAEdge edge) throws CPATransferException {
    if (oldFormula instanceof PathFormulaWithUF) {
      return makeAnd((PathFormulaWithUF) oldFormula, edge);
    } else {
      throw new CPATransferException("CToFormulaWithUF converter requires PathFormulaIWhtUF");
    }
  }

  public PathFormulaWithUF makeAnd(final PathFormulaWithUF oldFormula, final CFAEdge edge) throws CPATransferException {

    if (edge.getEdgeType() == CFAEdgeType.BlankEdge) {
      return oldFormula;
    }

    final String function = edge.getPredecessor() != null ? edge.getPredecessor().getFunctionName() : null;
    final SSAMapBuilder ssa = oldFormula.getSsa().builder();
    final Constraints constraints = new Constraints(bfmgr);
    final PointerTargetSetBuilder pts = oldFormula.getPointerTargetSet().builder();
    final StatementToFormulaWithUFVisitor statementVisitor = getStatementToFormulaWithUFVisitor(edge,
                                                                                                function,
                                                                                                ssa,
                                                                                                constraints,
                                                                                                pts);

    BooleanFormula edgeFormula = createFormulaForEdge(edge, statementVisitor);
    edgeFormula = bfmgr.and(edgeFormula, constraints.get());

    final SSAMap newSsa = ssa.build();
    final PointerTargetSet newPts = pts.build();
    final BooleanFormula newFormula = bfmgr.and(oldFormula.getFormula(), edgeFormula);
    int newLength = oldFormula.getLength() + 1;
    return new PathFormulaWithUF(newFormula, newSsa, newPts, newLength);
  }

  private ExpressionToFormulaWithUFVisitor getExpressionToFormulaWithUFVisitor(final CFAEdge cfaEdge,
                                                                               final String function,
                                                                               final SSAMapBuilder ssa,
                                                                               final Constraints constraints,
                                                                               final PointerTargetSetBuilder pts) {
    return new ExpressionToFormulaWithUFVisitor(this, cfaEdge, function, ssa, constraints, pts);
  }

  private LvalueToPointerTargetPatternVisitor getLvalueToPointerTargetPatternVisitor(
    final CFAEdge cfaEdge,
    final PointerTargetSetBuilder pts) {
    return new LvalueToPointerTargetPatternVisitor(this, cfaEdge, pts);
  }

  private StatementToFormulaWithUFVisitor getStatementToFormulaWithUFVisitor(final CFAEdge cfaEdge,
                                                                             final String function,
                                                                             final SSAMapBuilder ssa,
                                                                             final Constraints constraints,
                                                                             final PointerTargetSetBuilder pts) {
    final ExpressionToFormulaWithUFVisitor delegate = getExpressionToFormulaWithUFVisitor(cfaEdge,
                                                                                          function,
                                                                                          ssa,
                                                                                          constraints,
                                                                                          pts);
    final LvalueToPointerTargetPatternVisitor lvalueVisitor = getLvalueToPointerTargetPatternVisitor(cfaEdge, pts);
    return new StatementToFormulaWithUFVisitor(delegate, lvalueVisitor);
  }

  protected BooleanFormula makeReturn(final CExpression resultExpression,
                                      final CReturnStatementEdge returnEdge,
                                      final StatementToFormulaWithUFVisitor statementVisitor)
  throws CPATransferException {
    if (resultExpression == null) {
      // this is a return from a void function, do nothing
      return bfmgr.makeBoolean(true);
    } else {
      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      final String returnVariableName = getReturnVarName();
      final CFunctionDeclaration functionDeclaration = ((CFunctionEntryNode) returnEdge.getSuccessor()
                                                                                       .getEntryNode())
                                                                                         .getFunctionDefinition();
      final CType returnType = PointerTargetSet.simplifyType(functionDeclaration.getType().getReturnType());
      final CVariableDeclaration returnVariableDeclaration =
        new CVariableDeclaration(functionDeclaration.getFileLocation(),
                                 false,
                                 CStorageClass.AUTO,
                                 returnType,
                                 returnVariableName,
                                 returnVariableName,
                                 scoped(returnVariableName, statementVisitor.getFuncitonName()),
                                 null);
      final CExpressionAssignmentStatement assignment =
        new CExpressionAssignmentStatement(resultExpression.getFileLocation(),
                                           new CIdExpression(resultExpression.getFileLocation(),
                                                             returnType,
                                                             returnVariableName,
                                                             returnVariableDeclaration),
                                           resultExpression);
      final BooleanFormula result = assignment.accept(statementVisitor);
      if (PointerTargetSet.containsArray(returnType)) {
        statementVisitor.forceShared(returnVariableDeclaration);
      }
      return result;
    }
  }

  private static String getLogMessage(final String msg, final CFAEdge edge) {
    return "Line " + edge.getLineNumber()
            + ": " + msg
            + ": " + edge.getDescription();
  }

  private void logDebug(final String msg, final CFAEdge edge) {
    if (logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, getLogMessage(msg, edge));
    }
  }

  private BooleanFormula makeDeclaration(final CDeclarationEdge declarationEdge,
                                         final StatementToFormulaWithUFVisitor statementVisitor)
  throws CPATransferException {

    if (declarationEdge.getDeclaration() instanceof CTypeDeclaration &&
        ((CTypeDeclaration) declarationEdge.getDeclaration()).getType() instanceof CCompositeType) {
      statementVisitor.declareCompositeType(
          (CCompositeType) ((CTypeDeclaration) declarationEdge.getDeclaration()).getType());
    }

    if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // function declaration, typedef etc.
      logDebug("Ignoring declaration", declarationEdge);
      return bfmgr.makeBoolean(true);
    }

    final CVariableDeclaration declaration = (CVariableDeclaration) declarationEdge.getDeclaration();

    // makeFreshIndex(variableName, declaration.getType(), ssa); // TODO: Make sure about
                                                                 // correctness of SSA indices without this trick!

    // if there is an initializer associated to this variable,
    // take it into account
    final CType declarationType = PointerTargetSet.simplifyType(declaration.getType());
    final CInitializer initializer = declaration.getInitializer();
    if (initializer instanceof CInitializerExpression || initializer == null) {
      final CExpressionAssignmentStatement assignment = new CExpressionAssignmentStatement(
        declaration.getFileLocation(),
        new CIdExpression(declaration.getFileLocation(), declarationType, declaration.getName(), declaration),
        initializer != null ? ((CInitializerExpression) initializer).getExpression() : null);
      final BooleanFormula result = assignment.accept(statementVisitor);
      if (PointerTargetSet.containsArray(declarationType)) {
        statementVisitor.forceShared(declaration);
      }
      return result;
    } else if (initializer instanceof CInitializerList) {
      final Object initializerList = statementVisitor.visitInitializer(declarationType,
                                                                       initializer,
                                                                       !declaration.isGlobal());
      assert initializerList instanceof List : "Wrong initializer";
      return statementVisitor.visitComplexInitialization(declaration,
                                                         (List<?>) initializerList);
    } else {
      throw new UnrecognizedCCodeException("Unrecognized initializer", declarationEdge, initializer);
    }
  }

  private BooleanFormula makeAssume(final CAssumeEdge assume,
                                    final StatementToFormulaWithUFVisitor statementVisitor)
  throws CPATransferException {

    return statementVisitor.visitAssume(assume.getExpression(), assume.getTruthAssumption());
  }

  private BooleanFormula makeFunctionCall(final CFunctionCallEdge edge,
                                          final StatementToFormulaWithUFVisitor statementVisitor)
  throws CPATransferException {

    final List<CExpression> arguments = edge.getArguments();
    final CFunctionEntryNode entryNode = edge.getSuccessor();
    final List<CParameterDeclaration> parameters = entryNode.getFunctionParameters();

    if (entryNode.getFunctionDefinition().getType().takesVarArgs()) {
      if (parameters.size() > arguments.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
                                             "not match function definition",
                                             edge);
      }
      if (!SAFE_VAR_ARG_FUNCTIONS.contains(entryNode.getFunctionName())) {
        log(Level.WARNING, "Ignoring parameters passed as varargs to function " +
                           entryNode.getFunctionName() +
                           " in line " +
                           edge.getLineNumber());
      }
    } else {
      if (parameters.size() != arguments.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
                                             "not match function definition",
                                             edge);
      }
    }

    int i = 0;
    BooleanFormula result = bfmgr.makeBoolean(true);
    for (CParameterDeclaration formalParameter : parameters) {
      final CExpression argument = arguments.get(i++);
      final CType parameterType = PointerTargetSet.simplifyType(formalParameter.getType());
      final CExpressionAssignmentStatement assignmentStatement = new CExpressionAssignmentStatement(
        argument.getFileLocation(),
        new CIdExpression(argument.getFileLocation(), parameterType, formalParameter.getName(), formalParameter),
        argument);
      final BooleanFormula assignment = assignmentStatement.accept(statementVisitor);
      result = bfmgr.and(result, assignment);
      if (PointerTargetSet.containsArray(parameterType)) {
        statementVisitor.forceShared(formalParameter);
      }
    }

    return result;
  }

  protected BooleanFormula makeExitFunction(final CFunctionSummaryEdge summaryEdge,
                                            final StatementToFormulaWithUFVisitor statementVisitor)
  throws CPATransferException {

    final CFunctionCall returnExpression = summaryEdge.getExpression();
    if (returnExpression instanceof CFunctionCallStatement) {
      // this should be a void return, just do nothing...
      return bfmgr.makeBoolean(true);
    } else if (returnExpression instanceof CFunctionCallAssignmentStatement) {
      final CFunctionCallAssignmentStatement expression = (CFunctionCallAssignmentStatement) returnExpression;
      final String returnVariableName = getReturnVarName();
      final CFunctionCallExpression functionCallExpression = expression.getRightHandSide();
      final CType returnType = getReturnType(functionCallExpression, summaryEdge);
      final CIdExpression rhs = new CIdExpression(functionCallExpression.getFileLocation(),
                                                  returnType,
                                                  returnVariableName,
                                                  new CVariableDeclaration(functionCallExpression.getDeclaration()
                                                                                                 .getFileLocation(),
                                                                           false,
                                                                           CStorageClass.AUTO,
                                                                           returnType,
                                                                           returnVariableName,
                                                                           returnVariableName,
                                                                           scoped(returnVariableName,
                                                                                  statementVisitor
                                                                                    .getFuncitonName()),
                                                                           null));
      CExpression lhs = expression.getLeftHandSide();

      BooleanFormula assignment = statementVisitor.visit(
        new CExpressionAssignmentStatement(functionCallExpression.getFileLocation(), lhs, rhs));

      return assignment;
    } else {
      throw new UnrecognizedCCodeException("Unknown function exit expression", summaryEdge, returnExpression.asStatement());
    }
  }

  private BooleanFormula createFormulaForEdge(final CFAEdge edge,
                                              final StatementToFormulaWithUFVisitor statementVisitor) throws CPATransferException {
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      final CStatementEdge statementEdge = (CStatementEdge) edge;
      return statementEdge.getStatement().accept(statementVisitor);
    }
    case ReturnStatementEdge: {
      final CReturnStatementEdge returnEdge = (CReturnStatementEdge) edge;
      return makeReturn(returnEdge.getExpression(), returnEdge, statementVisitor);
    }

    case DeclarationEdge: {
      final CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
      return makeDeclaration(declarationEdge, statementVisitor);
    }
    case AssumeEdge: {
      return makeAssume((CAssumeEdge) edge, statementVisitor);
    }

    case BlankEdge: {
      assert false : "Handled above";
      return bfmgr.makeBoolean(true);
    }

    case FunctionCallEdge: {
      return makeFunctionCall((CFunctionCallEdge) edge, statementVisitor);
    }

    case FunctionReturnEdge: {
      // get the expression from the summary edge
      final CFunctionSummaryEdge summaryEdge = ((CFunctionReturnEdge) edge).getSummaryEdge();
      return makeExitFunction(summaryEdge, statementVisitor);
    }

    case MultiEdge: {
      BooleanFormula multiEdgeFormula = bfmgr.makeBoolean(true);
      for (CFAEdge singleEdge : (MultiEdge)edge) {
        if (singleEdge instanceof BlankEdge) {
          continue;
        }
        multiEdgeFormula = bfmgr.and(multiEdgeFormula, createFormulaForEdge(singleEdge, statementVisitor));
      }
      return multiEdgeFormula;
    }

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private final RationalFormulaManagerView rfmgr;

  String successfulAllocFunctionName = "__VERIFIER_successful_alloc";
  String successfulZallocFunctionName = "__VERIFIER_successful_zalloc";
  String MemsetFunctionName = "__VERIFIER_memset";

  @SuppressWarnings("hiding")
  private static final Set<String> SAFE_VAR_ARG_FUNCTIONS = ImmutableSet.of("printf", "printk");

  private static final String RETURN_VARIABLE_NAME = "__retval__";
}
