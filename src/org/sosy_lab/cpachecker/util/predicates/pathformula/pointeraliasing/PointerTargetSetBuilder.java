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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet.CompositeField;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;


public interface PointerTargetSetBuilder {

  BooleanFormula prepareBase(String name, CType type);

  void shareBase(String name, CType type);

  /**
   * Adds the newly allocated base of the given type for tracking along with all its tracked (sub)fields
   * (if its a structure/union) or all its elements (if its an array).
   */
  BooleanFormula addBase(String name, CType type);

  boolean tracksField(CCompositeType compositeType, String fieldName);

  boolean addField(CCompositeType composite, String fieldName);

  void addEssentialFields(final List<Pair<CCompositeType, String>> fields);

  void addTemporaryDeferredAllocation(boolean isZeroing,
      CIntegerLiteralExpression size,
      String baseVariable);

  void addDeferredAllocationPointer(String newPointerVariable,
      String originalPointerVariable);

  /**
   * Removes pointer to a deferred memory allocation from tracking.
   * @return whether the removed variable was the only pointer to the corresponding referred allocation
   */
  boolean removeDeferredAllocatinPointer(String oldPointerVariable);

  DeferredAllocationPool removeDeferredAllocation(String allocatedPointerVariable);

  SortedSet<String> getDeferredAllocationVariables();

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
  final static class RealPointerTargetSetBuilder implements PointerTargetSetBuilder {

    private final FormulaManagerView formulaManager;
    private final PointerTargetSetManager ptsMgr;
    private final FormulaEncodingWithPointerAliasingOptions options;

    // These fields all exist in PointerTargetSet and are documented there.
    private PersistentSortedMap<String, CType> bases;
    private String lastBase;
    private PersistentSortedMap<CompositeField, Boolean> fields;
    private PersistentSortedMap<String, DeferredAllocationPool> deferredAllocations;
    private PersistentSortedMap<String, PersistentList<PointerTarget>> targets;

    // Used in addEssentialFields()
    private final Predicate<Pair<CCompositeType, String>> isNewFieldPredicate =
      new Predicate<Pair<CCompositeType, String>> () {
      @Override
      public boolean apply(Pair<CCompositeType, String> field) {
        final String type = CTypeUtils.typeToString(field.getFirst());
        final CompositeField compositeField = CompositeField.of(type, field.getSecond());
        return !fields.containsKey(compositeField);
      }
    };

    // Used in addEssentialFields()
    private static final Function<Pair<CCompositeType, String>, Triple<CCompositeType, String, CType>>
      typeFieldFunction = new Function<Pair<CCompositeType, String>, Triple<CCompositeType, String, CType>>() {
        @Override
        public Triple<CCompositeType, String, CType> apply(Pair<CCompositeType, String> field) {
          final CCompositeType fieldComposite = field.getFirst();
          final String fieldName = field.getSecond();
          for (final CCompositeTypeMemberDeclaration declaration : fieldComposite.getMembers()) {
            if (declaration.getName().equals(fieldName)) {
              return Triple.of(fieldComposite, fieldName, CTypeUtils.simplifyType(declaration.getType()));
            }
          }
          throw new AssertionError("Tried to start tracking for a non-existent field " + fieldName +
                                   " in composite type " + fieldComposite);
        }
      };

    // Used in addEssentialFields()
    private static final Comparator<Triple<CCompositeType, String, CType>>
      simpleTypedFieldsFirstComparator = new Comparator<Triple<CCompositeType, String, CType>>() {
        @Override
        public int compare(Triple<CCompositeType, String, CType> field1, Triple<CCompositeType, String, CType> field2) {
          final int isField1Simple = field1.getThird() instanceof CCompositeType ? 1 : 0;
          final int isField2Simple = field2.getThird() instanceof CCompositeType ? 1 : 0;
          return isField1Simple - isField2Simple;
        }
      };

    public RealPointerTargetSetBuilder(final PointerTargetSet pointerTargetSet,
        final FormulaManagerView pFormulaManager,
        final PointerTargetSetManager pPtsMgr,
        final FormulaEncodingWithPointerAliasingOptions pOptions) {
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
     * @param name the name of the newly allocated base variable
     * @param type type of the allocated base or the next added pointer target
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

      if (type instanceof CElaboratedType) {
        assert ((CElaboratedType) type).getRealType() == null : "Elaborated type " + type + " that was not simplified but could have been.";
        // This is the declaration of a variable of an incomplete struct type.
        // We can't access the contents of this variable anyway,
        // so we don't add targets.

      } else {
        addTargets(name, type);
      }

      bases = bases.putAndCopy(name, type);
    }

    /**
     * Adds the newly allocated base of the given type for tracking along with all its tracked (sub)fields
     * (if its a structure/union) or all its elements (if its an array).
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
     * @param containerOffset either {@code 0} or the offset of the innermost container relative to the base address
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
      if (cType instanceof CElaboratedType) {
        // unresolved struct type won't have any targets, do nothing

      } else if (cType instanceof CArrayType) {
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
     */
    private void shallowRemoveField(final CCompositeType composite, final String fieldName) {
      final String type = CTypeUtils.typeToString(composite);
      final CompositeField field = CompositeField.of(type, fieldName);
      fields = fields.removeAndCopy(field);
    }

    /**
     * Used to start tracking for fields that were used in some expression or an assignment LHS.
     * Each field is added for tracking only if it's present in some currently allocated object;
     * an inner structure/union field is added only if the field corresponding to the inner
     * composite itself is already tracked; also, a field corresponding to an inner composite is added
     * only if any fields of that composite are already tracked. The latter two optimizations cause problems
     * when adding an inner composite field along with the corresponding containing field e.g.:
     *
     * <p>{@code pouter->inner.f = /*...* /;}</p>
     *
     * Here {@code inner.f} is not added because inner is not yet tracked and {@code outer.inner}
     * is not added because no fields in structure <tt>inner</tt> are tracked. The issue is solved by grouping the
     * requested fields into chunks by their nesting and avoid optimizations when adding fields of the same chunk.
     *
     */
    @Override
    public void addEssentialFields(final List<Pair<CCompositeType, String>> fields) {
      final List<Triple<CCompositeType, String, CType>> typedFields =
        FluentIterable.from(fields)
                      .filter(isNewFieldPredicate)
                      .transform(typeFieldFunction)
                      .toSortedList(simpleTypedFieldsFirstComparator);
      if (typedFields.isEmpty()) {
        return;
      }
      final Set<Triple<CCompositeType, String, CType>> done = new HashSet<>();
      final List<Triple<CCompositeType, String, CType>> currentChain = new ArrayList<>();
      for (final Triple<CCompositeType, String, CType> field : typedFields) {
        if (!done.contains(field)) {
          currentChain.clear();

          Triple<CCompositeType, String, CType> current = field;
          do {
            currentChain.add(current);
            for (final Triple<CCompositeType, String, CType> parentField : typedFields) {
              if (!done.contains(parentField) && parentField.getThird().equals(current.getFirst())) {
                current = parentField;
                break;
              }
            }
          } while (current != currentChain.get(currentChain.size() - 1));

          boolean useful = false;
          for (int i = currentChain.size() - 1; i >= 0; i--) {
            final Triple<CCompositeType, String, CType> f = currentChain.get(i);
            done.add(f);
            useful |= addField(f.getFirst(), f.getSecond());
          }

          if (!useful) {
            for (final Triple<CCompositeType, String, CType> f : currentChain) {
              shallowRemoveField(f.getFirst(), f.getSecond());
            }
          }
        }
      }
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
      assert deferredAllocations.get(newPointerVariable) == newDeferredAllocationPool;
    }

    /**
     * Removes pointer to a deferred memory allocation from tracking.
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
    public SortedSet<String> getDeferredAllocationVariables() {
      return deferredAllocations.keySet();
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
    public void addEssentialFields(List<Pair<CCompositeType, String>> pFields) {
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
    public SortedSet<String> getDeferredAllocationVariables() {
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
