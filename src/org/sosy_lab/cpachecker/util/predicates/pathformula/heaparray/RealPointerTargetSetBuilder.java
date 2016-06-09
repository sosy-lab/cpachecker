/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

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
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.DeferredAllocationPool;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetPattern;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet.CompositeField;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Actual builder implementation for PointerTargetSet.
 *
 * Its state starts with an existing set, but may be changed later. It supports read access, but it
 * is not recommended to use instances of this class except for the short period of time while
 * creating a new set.
 *
 * This class is not thread-safe.
 */
class RealPointerTargetSetBuilder implements PointerTargetSetBuilder {

  private final FormulaManagerView formulaManager;
  private final PointerTargetSetManagerHeapArray ptsMgs;
  private final FormulaEncodingWithPointerAliasingOptions options;

  // These fields all exist in PointerTargetSet and are documented there.
  private PersistentSortedMap<String, CType> bases;
  private String lastBase;
  private PersistentSortedMap<CompositeField, Boolean> fields;
  private PersistentSortedMap<String, DeferredAllocationPool> deferredAllocations;
  private PersistentSortedMap<String, PersistentList<PointerTarget>> targets;

  // Used in addEssentialFields()
  private final Predicate<Pair<CCompositeType, String>> isNewFieldPredicate =
      new Predicate<Pair<CCompositeType, String>>() {
        @Override
        public boolean apply(Pair<CCompositeType, String> pField) {
          final String type = CTypeUtils.typeToString(pField.getFirst());
          final CompositeField compositeField = CompositeField.of(type, pField.getSecond());
          return !fields.containsKey(compositeField);
        }
      };

  // Used in addEssentialFields()
  private static final Function<Pair<CCompositeType, String>, Triple<CCompositeType, String, CType>>
      typeFieldFunction =
          pField -> {
            final CCompositeType fieldComposite = pField.getFirst();
            final String fieldName = pField.getSecond();
            for (final CCompositeTypeMemberDeclaration declaration : fieldComposite.getMembers()) {
              if (declaration.getName().equals(fieldName)) {
                return Triple.of(
                    fieldComposite, fieldName, CTypeUtils.simplifyType(declaration.getType()));
              }
            }
            throw new AssertionError(
                "Tried to start tracking a non-existent field "
                    + fieldName
                    + " in composite type "
                    + fieldComposite);
          };

  // Used in addEssentialFields()
  private static final Comparator<Triple<CCompositeType, String, CType>>
      simpleTypedFieldsFirstComparator =
          (pField1, pField2) -> {
            final int isField1Simple = pField1.getThird() instanceof CCompositeType ? 1 : 0;
            final int isField2Simple = pField2.getThird() instanceof CCompositeType ? 1 : 0;
            return isField1Simple - isField2Simple;
          };

  /**
   * Creates a new RealPointerTargetSetBuilder.
   *
   * @param pPointerTargetSet        The underlying PointerTargetSet
   * @param pFormulaManagerView      The formula manager for SMT formulae
   * @param pPointerTargetSetManager The PointerTargetSetManager
   * @param pOptions                 Additional configuration options.
   */
  RealPointerTargetSetBuilder(
      final PointerTargetSet pPointerTargetSet,
      final FormulaManagerView pFormulaManagerView,
      final PointerTargetSetManagerHeapArray pPointerTargetSetManager,
      final FormulaEncodingWithPointerAliasingOptions pOptions) {
    bases = pPointerTargetSet.getBases();
    lastBase = pPointerTargetSet.getLastBase();
    fields = pPointerTargetSet.getFields();
    deferredAllocations = pPointerTargetSet.getDeferredAllocations();
    targets = pPointerTargetSet.getTargets();
    formulaManager = pFormulaManagerView;
    ptsMgs = pPointerTargetSetManager;
    options = pOptions;
  }

  /**
   * Recursively adds pointer targets for every used (tracked) (sub)field of the newly allocated
   * base.
   *
   * Note: The recursion doesn't proceed on unused (untracked) (sub)fields.
   *
   * @param pName The name of the newly allocated base variable.
   * @param pType The type of the allocated base or the next added pointer target
   */
  private void addTargets(final String pName, CType pType) {
    targets = ptsMgs.addToTargets(pName, pType, null, 0, 0, targets, fields);
  }

  /**
   * Returns a boolean formula for a prepared base of a pointer.
   *
   * @param pName The name of the variable.
   * @param pType The type of the variable.
   * @return A boolean formula representing the base.
   */
  @Override
  public BooleanFormula prepareBase(final String pName, CType pType) {
    pType = CTypeUtils.simplifyType(pType);
    if (bases.containsKey(pName)) {
      // The base has already been added
      return formulaManager.getBooleanFormulaManager().makeBoolean(true);
    }
    bases = bases.putAndCopy(pName, pType); // To get proper inequalities
    final BooleanFormula nextInequality = ptsMgs.getNextBaseAddressInequality(
        pName, bases, lastBase);
    // If typoe is incomplete, we can use a dummy size here because it is only used for the fake
    // base.
    int size = pType.isIncomplete() ? 0 : ptsMgs.getSize(pType);
    // To prevent adding spurious targets when merging
    bases = bases.putAndCopy(pName, PointerTargetSetManagerHeapArray.getFakeBaseType(size));
    lastBase = pName;
    return nextInequality;
  }

  /**
   * Shares a base of a pointer.
   *
   * @param pName The variable name.
   * @param pType The type of the variable.
   */
  @Override
  public void shareBase(final String pName, CType pType) {
    pType = CTypeUtils.simplifyType(pType);
    if (pType instanceof CElaboratedType) {
      // This is the declaration of a variable of an incomplete struct type.
      // We can't access the contents of this variable anyway, so we don't add
      // targets
      assert ((CElaboratedType) pType).getRealType() == null : "Elaborated type " + pType + " "
          + "that was not simplified but could have been.";
    } else {
      addTargets(pName, pType);
    }

    bases = bases.putAndCopy(pName, pType);
  }

  /**
   * Adds the newly allocated base of the given type for tracking along with all its tracked
   * (sub)fields (if it is a structure/union) or all its elements (it it is an array).
   *
   * @param pName The name of the base
   * @param pType The type of the base
   * @return A formula representing the base
   */
  @Override
  public BooleanFormula addBase(final String pName, CType pType) {
    pType = CTypeUtils.simplifyType(pType);
    if (bases.containsKey(pName)) {
      // The base has already been added
      return formulaManager.getBooleanFormulaManager().makeBoolean(true);
    }

    addTargets(pName, pType);
    bases = bases.putAndCopy(pName, pType);

    final BooleanFormula nextInequality = ptsMgs.getNextBaseAddressInequality(
        pName, bases, lastBase);
    lastBase = pName;
    if (!options.trackFunctionPointers() && CTypes.isFunctionPointer(pType)) {
      // Avoid adding constraints about function addresses,
      // otherwise we might track facts about function pointers for code like "if (p == &f)".
      return formulaManager.getBooleanFormulaManager().makeBoolean(true);
    } else {
      return nextInequality;
    }
  }

  /**
   * Returns, whether a field of a composite type is tracked or not.
   *
   * @param pCompositeType The composite type.
   * @param pFieldName     The name of the field in the composite type.
   * @return True, if the field is already tracked, false otherwise.
   */
  @Override
  public boolean tracksField(
      final CCompositeType pCompositeType,
      final String pFieldName) {
    return fields.containsKey(CompositeField.of(
        CTypeUtils.typeToString(pCompositeType), pFieldName));
  }

  /**
   * Recursively adds pointer targets for the given base variable when the newly used field is added
   * for tracking.
   *
   * @param pBase            The base variable
   * @param pCurrentType     The type of the base variable or of the next sub field
   * @param pProperOffset    Either {@code 0} or the offset of the next sub field in its innermost
   *                         container
   * @param pContainerOffset Either {@code 0} or the offset of the innermost container relative to
   *                         the base address
   * @param pComposite       The composite of the newly used field
   * @param pMemberName      The name of the newly used field
   */
  private void addTargets(
      final String pBase,
      final CType pCurrentType,
      final int pProperOffset,
      final int pContainerOffset,
      final String pComposite,
      final String pMemberName) {

    final CType type = CTypeUtils.simplifyType(pCurrentType);
    if (type instanceof CElaboratedType) {
      // unresolved struct type won't have any targets, do nothing
    } else if (type instanceof CArrayType) {
      final CArrayType arrayType = (CArrayType) type;
      Integer length = CTypeUtils.getArrayLength(arrayType);

      if (length == null) {
        length = options.defaultArrayLength();
      }

      int offset = 0;
      for (int i = 0; i < length; ++i) {
        addTargets(pBase, arrayType.getType(), offset, pContainerOffset + pProperOffset,
            pComposite, pMemberName);
        offset += ptsMgs.getSize(arrayType.getType());
      }
    } else if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: "
          + compositeType;

      final String typeName = CTypeUtils.typeToString(compositeType);
      int offset = 0;
      final boolean isTargetComposite = typeName.equals(pComposite);

      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (fields.containsKey(CompositeField.of(typeName,  memberDeclaration.getName()))) {
          addTargets(pBase, memberDeclaration.getType(), offset, pContainerOffset + pProperOffset,
              pComposite, pMemberName);
        }

        if (isTargetComposite && memberDeclaration.getName().equals(pMemberName)) {
          targets = ptsMgs.addToTargets(pBase, memberDeclaration.getType(), compositeType,
              offset, pContainerOffset + pProperOffset, targets, fields);
        }

        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += ptsMgs.getSize(memberDeclaration.getType());
        }
      }
    }
  }

  /**
   * Adds a field of a composite type to the tracking.
   *
   * @param pCompositeType The composite type with the field in it.
   * @param pFieldName     The name of the field in the composite type.
   * @return True, if the addition of the target was successful, false otherwise
   */
  @Override
  public boolean addField(
      final CCompositeType pCompositeType,
      final String pFieldName) {
    final String type = CTypeUtils.typeToString(pCompositeType);
    final CompositeField field = CompositeField.of(type, pFieldName);
    if (fields.containsKey(field)) {
      return true; // The field has already been added
    }

    final PersistentSortedMap<String, PersistentList<PointerTarget>> old = targets;
    for (final PersistentSortedMap.Entry<String, CType> baseEntry : bases.entrySet()) {
      addTargets(baseEntry.getKey(), baseEntry.getValue(), 0, 0, type, pFieldName);
    }
    fields = fields.putAndCopy(field, true);

    return old != targets;
  }

  /**
   * Should be used to remove the newly added field if it didn't turn out to correspond to any
   * actual pointer target.
   *
   * This can happen if we try to track a field of a composite that has no corresponding allocated
   * bases.
   *
   * @param pCompositeType The composite type which's field should be removed.
   * @param pFieldName     The name of the field that should be removed.
   */
  private void shallowRemoveField(
      final CCompositeType pCompositeType,
      final String pFieldName) {
    final String type = CTypeUtils.typeToString(pCompositeType);
    final CompositeField field = CompositeField.of(type, pFieldName);
    fields = fields.removeAndCopy(field);
  }

  /**
   * Used to start tracking for fields that were used in some expression or an assignment LHS.
   *
   * Each field is added for tracking only if it's present in some currently allocated object; an
   * inner structure/union field is added only if the field corresponding to the inner composite
   * itself is already tracked; also, a field corresponding to an inner composite is added only if
   * any fields of that composite are already tracked. The latter two optimizations cause problems
   * when adding an inner composite field along with the corresponding containing field e.g.:
   *
   * <p>{@code pouter->inner.f = /*...* /;}</p>
   *
   * Here {@code inner.f} is not added because inner is not yet tracked and {@code outer.inner} is
   * not added because no fields in structure <tt>inner</tt> are tracked. The issue is solved by
   * grouping the requested fields into chunks by their nesting and avoid optimizations when adding
   * fields of the same chunk.
   *
   * @param pFields The fields that should be tracked.
   */
  @Override
  public void addEssentialFields(
      final List<Pair<CCompositeType, String>> pFields) {
    final List<Triple<CCompositeType, String, CType>> typedFields = from(pFields)
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

  /**
   * Adds a pointer variable to the pool of tracked deferred allocations.
   *
   * @param pPointerVariable The name of the pointer variable.
   * @param pIsZeroing       A flag indicating if the variable is zeroing.
   * @param pSize            The size of the memory.
   * @param pBaseVariable    The name of the base variable.
   */
  private void addDeferredAllocation(
      final String pPointerVariable,
      final boolean pIsZeroing,
      final CIntegerLiteralExpression pSize,
      final String pBaseVariable) {
    deferredAllocations = deferredAllocations.putAndCopy(pPointerVariable,
        new DeferredAllocationPool(pPointerVariable, pIsZeroing, pSize, pBaseVariable));
  }

  /**
   * Adds a temporary deferred allocation to the tracking pool.
   *
   * @param pIsZeroing    A flag indicating if the variable is zeroing.
   * @param pSize         The size of the memory.
   * @param pBaseVariable The name of the base variable.
   */
  @Override
  public void addTemporaryDeferredAllocation(
      final boolean pIsZeroing,
      final CIntegerLiteralExpression pSize,
      final String pBaseVariable) {
    addDeferredAllocation(pBaseVariable, pIsZeroing, pSize, pBaseVariable);
  }

  /**
   * Adds a pointer to the tracking of deferred memory allocations.
   *
   * @param pNewPointerVariable      The new pointer variable.
   * @param pOriginalPointerVariable The original pointer variable.
   */
  @Override
  public void addDeferredAllocationPointer(
      final String pNewPointerVariable,
      final String pOriginalPointerVariable) {
    final DeferredAllocationPool newDeferredAllocationPool =
        deferredAllocations.get(pOriginalPointerVariable).addPointerVariable(pNewPointerVariable);

    for (final String pointerVariable : newDeferredAllocationPool.getPointerVariables()) {
      deferredAllocations = deferredAllocations.putAndCopy(
          pointerVariable, newDeferredAllocationPool);
    }

    assert deferredAllocations.get(pNewPointerVariable) == newDeferredAllocationPool;
  }

  /**
   * Removes pointer to a deferred memory allocation from tracking.
   *
   * @param pOldVariable The variable to be removed.
   * @return Whether the removed variable was the only pointer to the corresponding referred
   * allocation.
   */
  @Override
  public boolean removeDeferredAllocatinPointer(final String pOldVariable) {
    final DeferredAllocationPool newPool =
        deferredAllocations.get(pOldVariable).removePointerVariable(pOldVariable);

    deferredAllocations = deferredAllocations.removeAndCopy(pOldVariable);
    if (!newPool.getPointerVariables().isEmpty()) {
      for (final String variable : newPool.getPointerVariables()) {
        deferredAllocations = deferredAllocations.putAndCopy(variable, newPool);
      }

      return false;
    } else {
      return true;
    }
  }

  /**
   * Removes a variable from the pool of deferred allocations and returns the pool without the
   * variable.
   *
   * @param pAllocatedVariable The name of the variable to be removed.
   * @return The deferred allocation pool without the variable.
   */
  @Override
  public DeferredAllocationPool removeDeferredAllocation(
      final String pAllocatedVariable) {
    final DeferredAllocationPool allocationPool = deferredAllocations.get(pAllocatedVariable);
    for (final String variable : allocationPool.getPointerVariables()) {
      deferredAllocations = deferredAllocations.removeAndCopy(variable);
    }

    return allocationPool;
  }

  /**
   * Returns a set of all deferred allocation variables.
   *
   * @return The set of deferred allocation variables.
   */
  @Override
  public SortedSet<String> getDeferredAllocationVariables() {
    return deferredAllocations.keySet();
  }

  /**
   * Checks, if a variable is a temporary deferred allocation pointer.
   *
   * @param pPointer The variable name.
   * @return True, if the variable is a temporary deferred allocation pointer, false otherwise.
   */
  @Override
  public boolean isTemporaryDeferredAllocationPointer(final String pPointer) {
    final DeferredAllocationPool allocationPool = deferredAllocations.get(pPointer);
    assert allocationPool == null || allocationPool.getBaseVariables().size() >= 1
        : "Inconsistent deferred allocation pool: no bases";
    return allocationPool != null && allocationPool.getBaseVariables().get(0).equals(pPointer);
  }

  /**
   * Checks, if a variable is a deferred allocation pointer.
   *
   * @param pVariable The variable name.
   * @return True, if the variable is a deferred allocation pointer, false otherwise.
   */
  @Override
  public boolean isDeferredAllocationPointer(final String pVariable) {
    return deferredAllocations.containsKey(pVariable);
  }

  /**
   * Returns, if a variable is the actual base of a pointer.
   *
   * @param pName The name of the variable.
   * @return True, if the variable is an actual base, false otherwise.
   */
  @Override
  public boolean isActualBase(final String pName) {
    return bases.containsKey(pName)
        && !PointerTargetSetManagerHeapArray.isFakeBaseType(bases.get(pName));
  }

  /**
   * Returns, if a variable name is a prepared base.
   *
   * @param pName The name of the variable.
   * @return True, if the variable is a prepared base, false otherwise.
   */
  @Override
  public boolean isPreparedBase(final String pName) {
    return bases.containsKey(pName);
  }

  /**
   * Checks, if a variable is a base of a pointer.
   *
   * @param pName The name of the variable.
   * @param pType The type of the variable.
   * @return True, if the variable is a base, false otherwise.
   */
  @Override
  public boolean isBase(final String pName, CType pType) {
    pType = CTypeUtils.simplifyType(pType);
    final CType baseType = bases.get(pName);
    return baseType != null && baseType.equals(pType);
  }

  /**
   * Returns a set of all pointer bases.
   *
   * @return A set of all pointer bases.
   */
  @Override
  public SortedSet<String> getAllBases() {
    return bases.keySet();
  }

  /**
   * Gets a list of all targets of a pointer type.
   *
   * @param pType The type of the pointer variable.
   * @return A list of all targets of a pointer type.
   */
  @Override
  public PersistentList<PointerTarget> getAllTargets(final CType pType) {
    return firstNonNull(targets.get(CTypeUtils.typeToString(pType)), PersistentLinkedList.<PointerTarget>of());
  }

  /**
   * Gets all matching targets of a pointer target pattern.
   *
   * @param pType          The type of the pointer variable.
   * @param pTargetPattern The pointer target pattern.
   * @return A list of matching pointer targets.
   */
  @Override
  public Iterable<PointerTarget> getMatchingTargets(
      final CType pType,
      final PointerTargetPattern pTargetPattern) {
    return from(getAllTargets(pType)).filter(pTargetPattern);
  }

  /**
   * Gets all spurious targets of a pointer target pattern.
   *
   * @param pType          The type of the pointer variable.
   * @param pTargetPattern The pointer target pattern.
   * @return A list of spurious pointer targets.
   */
  @Override
  public Iterable<PointerTarget> getSpuriousTargets(
      final CType pType,
      final PointerTargetPattern pTargetPattern) {
    return from(getAllTargets(pType)).filter(not(pTargetPattern));
  }

  /**
   * Returns an immutable PointerTargetSet with all the changes made to the builder.
   *
   * @return A PointerTargetSet with all changes made to the builder.
   */
  @Override
  public PointerTargetSet build() {
    PointerTargetSet result = new PointerTargetSet(bases, lastBase, fields, deferredAllocations,
        targets);
    if (result.isEmpty()) {
      return PointerTargetSet.emptyPointerTargetSet();
    } else {
      return result;
    }
  }
}
