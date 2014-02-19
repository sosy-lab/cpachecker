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

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.SortedSet;

import javax.annotation.Nullable;

import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.CompositeField;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.collect.ImmutableSet;


public interface PointerTargetSetBuilder {

  BooleanFormula prepareBase(String name, CType type);

  void shareBase(String name, CType type);

  /**
   * Adds the newly allocated base of the given type for tracking along with all its tracked (sub)fields
   * (if its a structure/union) or all its elements (if its an array).
   * @param name
   * @param type
   */
  BooleanFormula addBase(String name, CType type);

  boolean tracksField(CCompositeType compositeType, String fieldName);

  boolean addField(CCompositeType composite, String fieldName);

  /**
   * Should be used to remove the newly added field if it didn't turn out to correspond to any actual pointer target.
   * This can happen if we try to track a field of a composite that has no corresponding allocated bases.
   * @param composite
   * @param fieldName
   */
  void shallowRemoveField(CCompositeType composite, String fieldName);

  void addTemporaryDeferredAllocation(boolean isZeroing,
      CIntegerLiteralExpression size,
      String baseVariable);

  void addDeferredAllocationPointer(String newPointerVariable,
      String originalPointerVariable);

  /**
   * Removes pointer to a deferred memory allocation from tracking.
   * @param oldPointerVariable
   * @return whether the removed variable was the only pointer to the corresponding referred allocation
   */
  boolean removeDeferredAllocatinPointer(String oldPointerVariable);

  DeferredAllocationPool removeDeferredAllocation(String allocatedPointerVariable);

  Collection<String> getDeferredAllocationVariables();

  boolean isTemporaryDeferredAllocationPointer(String pointerVariable);

  boolean isDeferredAllocationPointer(String pointerVariable);

  boolean isActualBase(String name);

  boolean isPreparedBase(String name);

  boolean isBase(String name, CType type);

  SortedSet<String> getAllBases();

  PersistentList<PointerTarget> getAllTargets(CType type);

  Iterable<PointerTarget> getMatchingTargets(CType type,
      PointerTargetPattern pattern);

  Iterable<PointerTarget> getSpuriousTargets(CType type,
      PointerTargetPattern pattern);

  /**
   * Returns an immutable PointerTargetSet with all the changes made to the builder.
   */
  PointerTargetSet build();

  /**
   * Actual builder implementation for PointerTargetSet.
   * Its state starts with an existing set, but may be changed later.
   * It supports read access, but it is not recommended to use
   * instances of this class except for the short period of time
   * while creating a new set.
   *
   * This class is not thread-safe.
   */
  public final static class RealPointerTargetSetBuilder implements PointerTargetSetBuilder {

    // The counter that guarantees a unique name for each allocated memory region.
    private static int dynamicAllocationCounter = 0;

    private final FormulaManagerView formulaManager;
    private final PointerTargetSetManager ptsMgr;
    private final FormulaEncodingWithUFOptions options;

    // These fields all exist in PointerTargetSet and are documented there.
    private PersistentSortedMap<String, CType> bases;
    private String lastBase;
    private PersistentSortedMap<CompositeField, Boolean> fields;
    private PersistentSortedMap<String, DeferredAllocationPool> deferredAllocations;
    private PersistentSortedMap<String, PersistentList<PointerTarget>> targets;

    RealPointerTargetSetBuilder(final PointerTargetSet pointerTargetSet,
        final FormulaManagerView pFormulaManager,
        final PointerTargetSetManager pPtsMgr,
        final FormulaEncodingWithUFOptions pOptions) {
      bases = pointerTargetSet.bases;
      lastBase = pointerTargetSet.lastBase;
      fields = pointerTargetSet.fields;
      deferredAllocations = pointerTargetSet.deferredAllocations;
      targets = pointerTargetSet.targets;
      formulaManager = pFormulaManager;
      ptsMgr = pPtsMgr;
      options = pOptions;
    }


    /**
     * Recursively adds pointer targets for every used (tracked) (sub)field of the newly allocated base.
     *
     * Note: the recursion doesn't proceed on unused (untracked) (sub)fields.
     *
     * @param base the name of the newly allocated base variable
     * @param currentType type of the allocated base or the next added pointer target
     */
    private void addTargets(final String name, CType type) {
      targets = ptsMgr.addToTargets(name, type, null, 0, 0, targets, fields);
    }

    @Override
    public BooleanFormula prepareBase(final String name, CType type) {
      type = CTypeUtils.simplifyType(type);
      if (bases.containsKey(name)) {
        // The base has already been added
        return formulaManager.getBooleanFormulaManager().makeBoolean(true);
      }
      bases = bases.putAndCopy(name, type); // To get proper inequalities
      final BooleanFormula nextInequality = ptsMgr.getNextBaseAddressInequality(name, bases, lastBase);
      bases = bases.putAndCopy(name, PointerTargetSetManager.getFakeBaseType(ptsMgr.getSize(type))); // To prevent adding spurious targets when merging
      lastBase = name;
      return nextInequality;
    }

    @Override
    public void shareBase(final String name, CType type) {
      type = CTypeUtils.simplifyType(type);
//      Preconditions.checkArgument(bases.containsKey(name),
//                                  "The base should be prepared beforehead with prepareBase()");

      addTargets(name, type);
      bases = bases.putAndCopy(name, type);

      lastBase = name;
    }

    /**
     * Adds the newly allocated base of the given type for tracking along with all its tracked (sub)fields
     * (if its a structure/union) or all its elements (if its an array).
     * @param name
     * @param type
     */
    @Override
    public BooleanFormula addBase(final String name, CType type) {
      type = CTypeUtils.simplifyType(type);
      if (bases.containsKey(name)) {
        // The base has already been added
        return formulaManager.getBooleanFormulaManager().makeBoolean(true);
      }

      addTargets(name, type);
      bases = bases.putAndCopy(name, type);

      final BooleanFormula nextInequality = ptsMgr.getNextBaseAddressInequality(name, bases, lastBase);
      lastBase = name;
      return nextInequality;
    }

    @Override
    public boolean tracksField(final CCompositeType compositeType, final String fieldName) {
      return fields.containsKey(CompositeField.of(CTypeUtils.typeToString(compositeType), fieldName));
    }

    /**
     * Recursively adds pointer targets for the given base variable when the newly used field is added for tracking.
     * @param base the base variable
     * @param currentType the type of the base variable or of the next subfield
     * @param containerType either {@code null} or the type of the innermost container of the next considered subfield
     * @param properOffset either {@code 0} or the offset of the next subfield in its innermost container
     * @param containerOffset either {code 0} or the offset of the innermost container relative to the base address
     * @param composite the composite of the newly used field
     * @param memberName the name of the newly used field
     */
    private void addTargets(final String base,
                            final CType currentType,
                            final @Nullable CType containerType,
                            final int properOffset,
                            final int containerOffset,
                            final String composite,
                            final String memberName) {
      final CType cType = CTypeUtils.simplifyType(currentType);
      assert !(cType instanceof CElaboratedType) : "Unresolved elaborated type:" + cType;
      if (cType instanceof CArrayType) {
        final CArrayType arrayType = (CArrayType) cType;
        Integer length = CTypeUtils.getArrayLength(arrayType);
        if (length == null) {
          length = options.defaultArrayLength();
        } else if (length > options.maxArrayLength()) {
          length = options.maxArrayLength();
        }
        int offset = 0;
        for (int i = 0; i < length; ++i) {
          addTargets(base, arrayType.getType(), arrayType, offset, containerOffset + properOffset,
                     composite, memberName);
          offset += ptsMgr.getSize(arrayType.getType());
        }
      } else if (cType instanceof CCompositeType) {
        final CCompositeType compositeType = (CCompositeType) cType;
        assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
        final String type = CTypeUtils.typeToString(compositeType);
        int offset = 0;
        final boolean isTargetComposite = type.equals(composite);
        for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
          if (fields.containsKey(CompositeField.of(type, memberDeclaration.getName()))) {
            addTargets(base, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset,
                       composite, memberName);
          }
          if (isTargetComposite && memberDeclaration.getName().equals(memberName)) {
            targets = ptsMgr.addToTargets(base, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset, targets, fields);
          }
          if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
            offset += ptsMgr.getSize(memberDeclaration.getType());
          }
        }
      }
    }

    @Override
    public boolean addField(final CCompositeType composite, final String fieldName) {
      final String type = CTypeUtils.typeToString(composite);
      final CompositeField field = CompositeField.of(type, fieldName);
      if (fields.containsKey(field)) {
        return true; // The field has already been added
      }

      final PersistentSortedMap<String, PersistentList<PointerTarget>> oldTargets = targets;
      for (final PersistentSortedMap.Entry<String, CType> baseEntry : bases.entrySet()) {
        addTargets(baseEntry.getKey(), baseEntry.getValue(), null, 0, 0, type, fieldName);
      }
      fields = fields.putAndCopy(field, true);

      if (oldTargets != targets) {
        // Target added
        return true;
      }
      return false;
    }

    /**
     * Should be used to remove the newly added field if it didn't turn out to correspond to any actual pointer target.
     * This can happen if we try to track a field of a composite that has no corresponding allocated bases.
     * @param composite
     * @param fieldName
     */
    @Override
    public void shallowRemoveField(final CCompositeType composite, final String fieldName) {
      final String type = CTypeUtils.typeToString(composite);
      final CompositeField field = CompositeField.of(type, fieldName);
      fields = fields.removeAndCopy(field);
    }

    private void addDeferredAllocation(final String pointerVariable,
                                      final boolean isZeroing,
                                      final CIntegerLiteralExpression size,
                                      final String baseVariable) {
      deferredAllocations = deferredAllocations.putAndCopy(pointerVariable,
                                                           new DeferredAllocationPool(pointerVariable,
                                                                                      isZeroing,
                                                                                      size,
                                                                                      baseVariable));
    }

    @Override
    public void addTemporaryDeferredAllocation(final boolean isZeroing,
                                               final CIntegerLiteralExpression size,
                                               final String baseVariable) {
      addDeferredAllocation(baseVariable, isZeroing, size, baseVariable);
    }

    @Override
    public void addDeferredAllocationPointer(final String newPointerVariable,
                                             final String originalPointerVariable) {
      final DeferredAllocationPool newDeferredAllocationPool =
        deferredAllocations.get(originalPointerVariable).addPointerVariable(newPointerVariable);

      for (final String pointerVariable : newDeferredAllocationPool.getPointerVariables()) {
        deferredAllocations = deferredAllocations.putAndCopy(pointerVariable, newDeferredAllocationPool);
      }
      deferredAllocations = deferredAllocations.putAndCopy(newPointerVariable, newDeferredAllocationPool);
    }

    /**
     * Removes pointer to a deferred memory allocation from tracking.
     * @param oldPointerVariable
     * @return whether the removed variable was the only pointer to the corresponding referred allocation
     */
    @Override
    public boolean removeDeferredAllocatinPointer(final String oldPointerVariable) {
      final DeferredAllocationPool newDeferredAllocationPool =
        deferredAllocations.get(oldPointerVariable).removePointerVariable(oldPointerVariable);

      deferredAllocations = deferredAllocations.removeAndCopy(oldPointerVariable);
      if (!newDeferredAllocationPool.getPointerVariables().isEmpty()) {
        for (final String pointerVariable : newDeferredAllocationPool.getPointerVariables()) {
          deferredAllocations = deferredAllocations.putAndCopy(pointerVariable, newDeferredAllocationPool);
        }
        return false;
      } else {
        return true;
      }
    }

    @Override
    public DeferredAllocationPool removeDeferredAllocation(final String allocatedPointerVariable) {
      final DeferredAllocationPool deferredAllocationPool = deferredAllocations.get(allocatedPointerVariable);
      for (final String pointerVariable : deferredAllocationPool.getPointerVariables()) {
        deferredAllocations = deferredAllocations.removeAndCopy(pointerVariable);
      }
      return deferredAllocationPool;
    }

    @Override
    public Collection<String> getDeferredAllocationVariables() {
      return ImmutableSet.copyOf(deferredAllocations.keySet());
    }

    public static int getNextDynamicAllocationIndex() {
      return dynamicAllocationCounter++;
    }

    @Override
    public boolean isTemporaryDeferredAllocationPointer(final String pointerVariable) {
      final DeferredAllocationPool deferredAllocationPool = deferredAllocations.get(pointerVariable);
      assert deferredAllocationPool == null || deferredAllocationPool.getBaseVariables().size() >= 1 :
             "Inconsistent deferred alloction pool: no bases";
      return deferredAllocationPool != null && deferredAllocationPool.getBaseVariables().get(0).equals(pointerVariable);
    }

    @Override
    public boolean isDeferredAllocationPointer(final String pointerVariable) {
      return deferredAllocations.containsKey(pointerVariable);
    }

    @Override
    public boolean isActualBase(final String name) {
      return bases.containsKey(name) && !PointerTargetSetManager.isFakeBaseType(bases.get(name));
    }

    @Override
    public boolean isPreparedBase(final String name) {
      return bases.containsKey(name);
    }

    @Override
    public boolean isBase(final String name, CType type) {
      type = CTypeUtils.simplifyType(type);
      final CType baseType = bases.get(name);
      return baseType != null && baseType.equals(type);
    }

    @Override
    public SortedSet<String> getAllBases() {
      return bases.keySet();
    }

    @Override
    public PersistentList<PointerTarget> getAllTargets(final CType type) {
      return firstNonNull(targets.get(CTypeUtils.typeToString(type)),
                          PersistentLinkedList.<PointerTarget>of());
    }

    @Override
    public Iterable<PointerTarget> getMatchingTargets(final CType type,
        final PointerTargetPattern pattern) {
      return from(getAllTargets(type)).filter(pattern);
    }

    @Override
    public Iterable<PointerTarget> getSpuriousTargets(final CType type,
        final PointerTargetPattern pattern) {
      return from(getAllTargets(type)).filter(not(pattern));
    }

    /**
     * Returns an immutable PointerTargetSet with all the changes made to the builder.
     */
    @Override
    public PointerTargetSet build() {
      PointerTargetSet result = new PointerTargetSet(bases, lastBase, fields,
          deferredAllocations, targets);
      if (result.isEmpty()) {
        return PointerTargetSet.emptyPointerTargetSet();
      } else {
        return result;
      }
    }
  }


  /**
   * Dummy implementation of {@link PointerTargetSetBuilder}
   * that throws an exception on all methods except for {@link #build()},
   * where it returns an empty {@link PointerTargetSet}.
   */
  public static enum DummyPointerTargetSetBuilder implements PointerTargetSetBuilder {
    INSTANCE;

    @Override
    public BooleanFormula prepareBase(String pName, CType pType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void shareBase(String pName, CType pType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public BooleanFormula addBase(String pName, CType pType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean tracksField(CCompositeType pCompositeType, String pFieldName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addField(CCompositeType pComposite, String pFieldName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void shallowRemoveField(CCompositeType pComposite, String pFieldName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addTemporaryDeferredAllocation(boolean pIsZeroing, CIntegerLiteralExpression pSize, String pBaseVariable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addDeferredAllocationPointer(String pNewPointerVariable, String pOriginalPointerVariable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeDeferredAllocatinPointer(String pOldPointerVariable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DeferredAllocationPool removeDeferredAllocation(String pAllocatedPointerVariable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getDeferredAllocationVariables() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTemporaryDeferredAllocationPointer(String pPointerVariable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDeferredAllocationPointer(String pPointerVariable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isActualBase(String pName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPreparedBase(String pName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBase(String pName, CType pType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<String> getAllBases() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PersistentList<PointerTarget> getAllTargets(CType pType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<PointerTarget> getMatchingTargets(CType pType, PointerTargetPattern pPattern) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<PointerTarget> getSpuriousTargets(CType pType, PointerTargetPattern pPattern) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PointerTargetSet build() {
      return PointerTargetSet.emptyPointerTargetSet();
    }
  }
}
