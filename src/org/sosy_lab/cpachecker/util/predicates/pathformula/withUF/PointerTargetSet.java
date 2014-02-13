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
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;

public class PointerTargetSet implements Serializable {

  /**
   * The objects of the class are used to keep the set of currently tracked fields in a {@link PersistentSortedMap}.
   * Objects of {@link CompositeField} are used as keys and place-holders of type {@link Boolean} are used as values.
   * <p>
   * This allows one to check if a particular field is tracked using a temporary object of {@link CompositeField} and
   * keep the set of currently tracked fields in rather simple way (no special-case merging is required).
   * </p>
   */
  public static class CompositeField implements Comparable<CompositeField> {
    private CompositeField(final String compositeType, final String fieldName) {
      this.compositeType = compositeType;
      this.fieldName = fieldName;
    }

    public static CompositeField of(final @Nonnull String compositeType, final @Nonnull String fieldName) {
      return new CompositeField(compositeType, fieldName);
    }

//    public String compositeType() {
//      return compositeType;
//    }

//    public String fieldName() {
//      return fieldName;
//    }

    @Override
    public String toString() {
      return compositeType + "." + fieldName;
    }

    @Override
    public int compareTo(final CompositeField other) {
      final int result = this.compositeType.compareTo(other.compositeType);
      if (result != 0) {
        return result;
      } else {
        return this.fieldName.compareTo(other.fieldName);
      }
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      } else if (!(obj instanceof CompositeField)) {
        return false;
      } else {
        CompositeField other = (CompositeField) obj;
        return compositeType.equals(other.compositeType) && fieldName.equals(other.fieldName);
      }
    }

    @Override
    public int hashCode() {
      return compositeType.hashCode() * 17 + fieldName.hashCode();
    }

    private final String compositeType;
    private final String fieldName;
  }

  public static String getBaseName(final String name){
    return BASE_PREFIX + name;
  }

  static boolean isBaseName(final String name) {
    return name.startsWith(BASE_PREFIX);
  }

  static String getBase(final String baseName) {
    return baseName.replaceFirst(BASE_PREFIX, "");
  }

  /**
   * The method is used to speed up {@code sizeof} computation by caching sizes of declared composite types.
   * @param cType
   * @return
   */
  public int getSize(CType cType) {
    cType = CTypeUtils.simplifyType(cType);
    if (cType instanceof CCompositeType) {
      if (sizes.contains(cType)) {
        return sizes.count(cType);
      } else {
        return cType.accept(sizeofVisitor);
      }
    } else {
      return cType.accept(sizeofVisitor);
    }
  }

  public Iterable<PointerTarget> getMatchingTargets(final CType type,
                                                    final PointerTargetPattern pattern) {
    return from(getAllTargets(type)).filter(pattern);
  }

  public Iterable<PointerTarget> getSpuriousTargets(final CType type,
                                                    final PointerTargetPattern pattern) {
    return from(getAllTargets(type)).filter(not(pattern));
  }

  public PersistentList<PointerTarget> getAllTargets(final CType type) {
    return firstNonNull(targets.get(CTypeUtils.typeToString(type)),
                        PersistentLinkedList.<PointerTarget>of());
  }

  /**
   * Builder for PointerTargetSet. Its state starts with an existing set, but may be
   * changed later. It supports read access, but it is not recommended to use
   * instances of this class except for the short period of time
   * while creating a new set.
   *
   * This class is not thread-safe.
   */
  public final static class PointerTargetSetBuilder extends PointerTargetSet {

    private PointerTargetSetBuilder(final PointerTargetSet pointerTargetSet) {
      super(pointerTargetSet.machineModel,
            pointerTargetSet.sizeofVisitor,
            pointerTargetSet.options,
            pointerTargetSet.bases,
            pointerTargetSet.lastBase,
            pointerTargetSet.fields,
            pointerTargetSet.deferredAllocations,
            pointerTargetSet.targets,
            pointerTargetSet.formulaManager);
    }

    /**
     * Adds the declared composite type to the cache saving its size as well as the offset of every
     * member of the composite.
     * @param compositeType
     */
    public void addCompositeType(CCompositeType compositeType) {
      compositeType = (CCompositeType) CTypeUtils.simplifyType(compositeType);
      if (offsets.containsKey(compositeType)) {
        // Support for empty structs though it's a GCC extension
        assert sizes.contains(compositeType) || Integer.valueOf(0).equals(compositeType.accept(sizeofVisitor)) :
          "Illegal state of PointerTargetSet: no size for type:" + compositeType;
        return; // The type has already been added
      }

      final Integer size = compositeType.accept(sizeofVisitor);

      assert size != null : "Can't evaluate size of a composite type: " + compositeType;

      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;

      final Multiset<String> members = HashMultiset.create();
      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        members.setCount(memberDeclaration.getName(), offset);
        final CType memberType = CTypeUtils.simplifyType(memberDeclaration.getType());
        final CCompositeType memberCompositeType;
        if (memberType instanceof CCompositeType) {
          memberCompositeType = (CCompositeType) memberType;
          if (memberCompositeType.getKind() == ComplexTypeKind.STRUCT ||
              memberCompositeType.getKind() == ComplexTypeKind.UNION) {
            if (!offsets.containsKey(memberCompositeType)) {
              assert !sizes.contains(memberCompositeType) :
                "Illegal state of PointerTargetSet: size for type:" + memberCompositeType;
              addCompositeType(memberCompositeType);
            }
          }
        } else {
          memberCompositeType = null;
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          if (memberCompositeType != null) {
            offset += sizes.count(memberCompositeType);
          } else {
            offset += memberDeclaration.getType().accept(sizeofVisitor);
          }
        }
      }

      assert compositeType.getKind() != ComplexTypeKind.STRUCT || offset == size :
             "Incorrect sizeof or offset of the last member: " + compositeType;

      sizes.setCount(compositeType, size);
      offsets.put(compositeType, members);
    }

    private void addTarget(final String base,
                           final CType targetType,
                           final @Nullable CType containerType,
                           final int properOffset,
                           final int containerOffset) {
      final String type = CTypeUtils.typeToString(targetType);
      PersistentList<PointerTarget> targetsForType = firstNonNull(targets.get(type),
                                                                  PersistentLinkedList.<PointerTarget>of());
      targets = targets.putAndCopy(type, targetsForType.with(new PointerTarget(base,
                                                                               containerType,
                                                                               properOffset,
                                                                               containerOffset)));
      flag = true;
    }

  /**
   * Recursively adds pointer targets for every used (tracked) (sub)field of the newly allocated base.
   * (actual recursive implementation).
   * @param base the name of the newly allocated base variable
   * @param currentType type of the allocated base or the next added pointer target
   * @param containerType either {@code null} or the type of the innermost container of the next added pointer target
   * @param properOffset either {@code 0} or the offset of the next added pointer target in its innermost container
   * @param containerOffset either {@code 0} or the offset of the innermost container (relative to the base adddress)
   */
    private void addTargets(final String base,
                            final CType currentType,
                            final @Nullable CType containerType,
                            final int properOffset,
                            final int containerOffset) {
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
          addTargets(base, arrayType.getType(), arrayType, offset, containerOffset + properOffset);
          offset += getSize(arrayType.getType());
        }
      } else if (cType instanceof CCompositeType) {
        final CCompositeType compositeType = (CCompositeType) cType;
        assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
        final String type = CTypeUtils.typeToString(compositeType);
        addCompositeType(compositeType);
        int offset = 0;
        for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
          if (fields.containsKey(CompositeField.of(type, memberDeclaration.getName()))) {
            addTargets(base, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset);
          }
          if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
            offset += getSize(memberDeclaration.getType());
          }
        }
      } else {
        addTarget(base, cType, containerType, properOffset, containerOffset);
      }
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
      addTargets(name, type, null, 0, 0);
    }

    public BooleanFormula prepareBase(final String name, CType type) {
      type = CTypeUtils.simplifyType(type);
      if (bases.containsKey(name)) {
        // The base has already been added
        return formulaManager.getBooleanFormulaManager().makeBoolean(true);
      }
      bases = bases.putAndCopy(name, type); // To get proper inequalities
      final BooleanFormula nextInequality = getNextBaseAddressInequality(name, lastBase);
      bases = bases.putAndCopy(name, PointerTargetSetManager.getFakeBaseType(getSize(type))); // To prevent adding spurious targets when merging
      lastBase = name;
      return nextInequality;
    }

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
    public BooleanFormula addBase(final String name, CType type) {
      type = CTypeUtils.simplifyType(type);
      if (bases.containsKey(name)) {
        // The base has already been added
        return formulaManager.getBooleanFormulaManager().makeBoolean(true);
      }

      addTargets(name, type);
      bases = bases.putAndCopy(name, type);

      final BooleanFormula nextInequality = getNextBaseAddressInequality(name, lastBase);
      lastBase = name;
      return nextInequality;
    }

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
          offset += getSize(arrayType.getType());
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
            addTargets(base, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset);
          }
          if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
            offset += getSize(memberDeclaration.getType());
          }
        }
      }
    }

    public boolean addField(final CCompositeType composite, final String fieldName) {
      final String type = CTypeUtils.typeToString(composite);
      final CompositeField field = CompositeField.of(type, fieldName);
      if (fields.containsKey(field)) {
        return true; // The field has already been added
      }
      flag = false;
      for (final PersistentSortedMap.Entry<String, CType> baseEntry : bases.entrySet()) {
        addTargets(baseEntry.getKey(), baseEntry.getValue(), null, 0, 0, type, fieldName);
      }
      fields = fields.putAndCopy(field, true);
      return flag;
    }

    /**
     * Should be used to remove the newly added field if it didn't turn out to correspond to any actual pointer target.
     * This can happen if we try to track a field of a composite that has no corresponding allocated bases.
     * @param composite
     * @param fieldName
     */
    public void shallowRemoveField(final CCompositeType composite, final String fieldName) {
      final String type = CTypeUtils.typeToString(composite);
      final CompositeField field = CompositeField.of(type, fieldName);
      fields = fields.removeAndCopy(field);
    }

    void setFields(PersistentSortedMap<CompositeField, Boolean> fields) {
      checkState(this.fields.isEmpty());
      this.fields = checkNotNull(fields);
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

    public void addTemporaryDeferredAllocation(final boolean isZeroing,
                                               final CIntegerLiteralExpression size,
                                               final String baseVariable) {
      addDeferredAllocation(baseVariable, isZeroing, size, baseVariable);
    }

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

    public DeferredAllocationPool removeDeferredAllocation(final String allocatedPointerVariable) {
      final DeferredAllocationPool deferredAllocationPool = deferredAllocations.get(allocatedPointerVariable);
      for (final String pointerVariable : deferredAllocationPool.getPointerVariables()) {
        deferredAllocations = deferredAllocations.removeAndCopy(pointerVariable);
      }
      return deferredAllocationPool;
    }

    public Collection<String> getDeferredAllocationVariables() {
      return ImmutableSet.copyOf(deferredAllocations.keySet());
    }

    public static int getNextDynamicAllocationIndex() {
      return dynamicAllocationCounter++;
    }

    /**
     * The method is used to speed up member offset computation for declared composite types.
     * @param compositeType
     * @param memberName
     * @return
     */
    public int getOffset(CCompositeType compositeType, final String memberName) {
      compositeType = (CCompositeType) CTypeUtils.simplifyType(compositeType);
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      Multiset<String> multiset = offsets.get(compositeType);
      if (multiset == null) {
        addCompositeType(compositeType);
        multiset = offsets.get(compositeType);
        assert multiset != null : "Failed adding composite type to cache: " + compositeType;
      }
      return multiset.count(memberName);
    }

    /**
     * Returns an immutable PointerTargetSet with all the changes made to the builder.
     */
    public PointerTargetSet build() {
      return new PointerTargetSet(machineModel,
                                  sizeofVisitor,
                                  options,
                                  bases,
                                  lastBase,
                                  fields,
                                  deferredAllocations,
                                  targets,
                                  formulaManager);
    }

    private boolean flag; // Used by addField() to detect essential additions

    private static final long serialVersionUID = 5692271309582052121L;
  }

  public boolean isTemporaryDeferredAllocationPointer(final String pointerVariable) {
    final DeferredAllocationPool deferredAllocationPool = deferredAllocations.get(pointerVariable);
    assert deferredAllocationPool == null || deferredAllocationPool.getBaseVariables().size() >= 1 :
           "Inconsistent deferred alloction pool: no bases";
    return deferredAllocationPool != null && deferredAllocationPool.getBaseVariables().get(0).equals(pointerVariable);
  }

  public boolean isDeferredAllocationPointer(final String pointerVariable) {
    return deferredAllocations.containsKey(pointerVariable);
  }

  public FormulaType<?> getPointerType() {
    return pointerType;
  }

  protected BooleanFormula getNextBaseAddressInequality(final String newBase,
                                                        final String lastBase) {
    final FormulaManagerView fm = formulaManager;
    final Formula newBaseFormula = fm.makeVariable(pointerType, getBaseName(newBase));
    if (lastBase != null) {
      final Integer lastSize = getSize(bases.get(lastBase));
      final Formula rhs = fm.makePlus(fm.makeVariable(pointerType, getBaseName(lastBase)),
                                      fm.makeNumber(pointerType, lastSize));
      // The condition rhs > 0 prevents overflows in case of bit-vector encoding
      return fm.makeAnd(fm.makeGreaterThan(rhs, fm.makeNumber(pointerType, 0L), true),
                        fm.makeGreaterOrEqual(newBaseFormula, rhs, true));
    } else {
      return fm.makeGreaterThan(newBaseFormula, fm.makeNumber(pointerType, 0L), true);
    }
  }

  public boolean isActualBase(final String name) {
    return bases.containsKey(name) && !PointerTargetSetManager.isFakeBaseType(bases.get(name));
  }

  public boolean isPreparedBase(final String name) {
    return bases.containsKey(name);
  }

  public boolean isBase(final String name, CType type) {
    type = CTypeUtils.simplifyType(type);
    final CType baseType = bases.get(name);
    return baseType != null && baseType.equals(type);
  }

  public SortedSet<String> getAllBases() {
    return bases.keySet();
  }

  public static final PointerTargetSet emptyPointerTargetSet(final MachineModel machineModel,
                                                             final FormulaEncodingWithUFOptions options,
                                                             final FormulaManagerView formulaManager) {
    return new PointerTargetSet(machineModel,
                                new CSizeofVisitor(machineModel, options),
                                options,
                                PathCopyingPersistentTreeMap.<String, CType>of(),
                                null,
                                PathCopyingPersistentTreeMap.<CompositeField, Boolean>of(),
                                PathCopyingPersistentTreeMap.<String, DeferredAllocationPool>of(),
                                PathCopyingPersistentTreeMap.<String, PersistentList<PointerTarget>>of(),
                                formulaManager);
  }

  @Override
  public String toString() {
    return joiner.join(bases.entrySet()) + " " + joiner.join(fields.entrySet());
  }

  @Override
  public int hashCode() {
    return (31 + bases.keySet().hashCode()) * 31 + fields.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof PointerTargetSet)) {
      return false;
    } else {
      PointerTargetSet other = (PointerTargetSet) obj;
      return bases.equals(other.bases) && fields.equals(other.fields);
    }
  }

  PointerTargetSet(final MachineModel machineModel,
                           final CSizeofVisitor sizeofVisitor,
                           final FormulaEncodingWithUFOptions options,
                           final PersistentSortedMap<String, CType> bases,
                           final String lastBase,
                           final PersistentSortedMap<CompositeField, Boolean> fields,
                           final PersistentSortedMap<String, DeferredAllocationPool> deferredAllocations,
                           final PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
                           final FormulaManagerView formulaManager) {
    this.machineModel = machineModel;
    this.sizeofVisitor = sizeofVisitor;

    this.options = options;
    this.formulaManager = formulaManager;

    this.bases = bases;
    this.lastBase = lastBase;
    this.fields = fields;

    this.deferredAllocations = deferredAllocations;

    this.targets = targets;

    final int pointerSize = machineModel.getSizeofPtr();
    final int bitsPerByte = machineModel.getSizeofCharInBits();
    this.pointerType = this.formulaManager.getBitvectorFormulaManager()
                                          .getFormulaType(pointerSize * bitsPerByte);
  }

  /**
   * Returns a PointerTargetSetBuilder that is initialized with the current PointerTargetSet.
   */
  public PointerTargetSetBuilder builder() {
    return new PointerTargetSetBuilder(this);
  }

  private static final Joiner joiner = Joiner.on(" ");

  protected final FormulaEncodingWithUFOptions options;
  protected final MachineModel machineModel;
  protected final CSizeofVisitor sizeofVisitor;

  protected final FormulaManagerView formulaManager;

  /*
   * Use Multiset<String> instead of Map<String, Integer> because it is more
   * efficient. The integer value is stored as the number of instances of any
   * element in the Multiset. So instead of calling map.get(key) we just use
   * Multiset.count(key). This is better because the Multiset internally uses
   * modifiable integers instead of the immutable Integer class.
   */
  private static final Multiset<CCompositeType> sizes = HashMultiset.create();
  private static final Map<CCompositeType, Multiset<String>> offsets = new HashMap<>();

  // The following fields are modified in the derived class only

  // The set of known memory objects.
  // This includes allocated memory regions and global/local structs/arrays.
  // The key of the map is the name of the base (without the BASE_PREFIX).
  // There are also "fake" bases in the map for variables that have their address
  // taken somewhere but are not yet tracked.
  protected /*final*/ PersistentSortedMap<String, CType> bases;

  // The last added memory region (used to create the chain of inequalities between bases).
  protected /*final*/ String lastBase;

  // The set of "shared" fields that are accessed directly via pointers,
  // so they are represented with UFs instead of as variables.
  protected /*final*/ PersistentSortedMap<CompositeField, Boolean> fields;

  protected /*final*/ PersistentSortedMap<String, DeferredAllocationPool> deferredAllocations;

  // The complete set of tracked memory locations.
  // The map key is the type of the memory location.
  // This set of locations is used to restore the values of the memory-access UF
  // when the SSA index is used (i.e, to create the *int@3(i) = *int@2(i) terms
  // for all values of i from this map).
  // This means that when a location is not present in this map,
  // its value is not tracked and might get lost.
  protected /*final*/ PersistentSortedMap<String, PersistentList<PointerTarget>> targets;

  private final FormulaType<?> pointerType;

  // The counter that guarantees a unique name for each allocated memory region.
  private static int dynamicAllocationCounter = 0;

  private static final String BASE_PREFIX = "__ADDRESS_OF_";

  private static final long serialVersionUID = 2102505458322248624L;
}
