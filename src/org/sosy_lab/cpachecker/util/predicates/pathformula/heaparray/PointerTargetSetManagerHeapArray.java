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
import static org.sosy_lab.common.collect.PersistentSortedMaps.merge;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.CopyOnWriteSortedMap;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;
import org.sosy_lab.common.collect.PersistentSortedMaps.MergeConflictHandler;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.DeferredAllocationPool;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet.CompositeField;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * A manager for pointer target sets.
 */
class PointerTargetSetManagerHeapArray extends PointerTargetSetManager {

  private static final String UNITED_BASE_UNION_TAG_PREFIX = "__VERIFIER_base_union_of_";
  private static final String UNITED_BASE_FIELD_NAME_PREFIX = "__VERIFIER_united_base_field";

  private static final String FAKE_ALLOC_FUNCTION_NAME = "__VERIFIER_fake_alloc";

  /**
   * Returns whether a {@code CType} is a fake base type or not.
   * <p/>
   * A fake base type is an array of void.
   *
   * @param pCType The type to be checked.
   * @return Whether the type is a fake base type or not.
   */
  static boolean isFakeBaseType(final CType pCType) {
    return pCType instanceof CArrayType && ((CArrayType) pCType).getType() instanceof CVoidType;
  }

  /**
   * Returns a fake base type of a given size, i.e. an array of {@code size} voids.
   *
   * @param pSize The size of the fake base type.
   * @return An array of {@code size} voids.
   */
  static CType getFakeBaseType(final int pSize) {
    return CTypeUtils.simplifyType(
        new CArrayType(false, false, CVoidType.VOID,
            new CIntegerLiteralExpression(FileLocation.DUMMY,
                CNumericTypes.SIGNED_CHAR, BigInteger.valueOf(pSize))));
  }

  /**
   * Returns a name for united field bases with a specified index.
   *
   * @param pIndex The index of the united field base.
   * @return A name for the united field base.
   */
  private static String getUnitedFieldBaseName(final int pIndex) {
    return UNITED_BASE_FIELD_NAME_PREFIX + pIndex;
  }

  private final ShutdownNotifier shutdownNotifier;
  private final FormulaEncodingWithPointerAliasingOptions options;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView bfmgr;
  private final ArrayFormulaManagerView afmgr;
  private final TypeHandlerWithPointerAliasing typeHandler;

  /**
   * Creates a new PointerTargetSetManager.
   *
   * @param pOptions            Additional configuration options.
   * @param pFormulaManagerView The manager for SMT formulae.
   * @param pTypeHandler        A type handler for certain types.
   * @param pShutdownNotifier   A notifier for external shutdowns to stop long-running algorithms.
   */
  PointerTargetSetManagerHeapArray(
      final FormulaEncodingWithPointerAliasingOptions pOptions,
      final FormulaManagerView pFormulaManagerView,
      final TypeHandlerWithPointerAliasing pTypeHandler,
      final ShutdownNotifier pShutdownNotifier) {
    super(pOptions, pFormulaManagerView, pTypeHandler, pShutdownNotifier);

    options = pOptions;
    formulaManager = pFormulaManagerView;
    bfmgr = formulaManager.getBooleanFormulaManager();
    afmgr = formulaManager.getArrayFormulaManager();
    typeHandler = pTypeHandler;
    shutdownNotifier = pShutdownNotifier;
  }

  /**
   * Merges two {@link PointerTargetSet}s into one.
   *
   * @param pPts1      The first {@code PointerTargetSet}.
   * @param pPts2      The second {@code PointerTargetSet}.
   * @param pResultSSA The map of SSA indices.
   * @param pConverter The converter for C code to SMT formulae.
   * @return The merged {@code PointerTargetSet}s.
   * @throws InterruptedException If the algorithms gets interrupted by an external shutdown.
   */
  @Override
  public MergeResult<PointerTargetSet> mergePointerTargetSets(
      final PointerTargetSet pPts1,
      final PointerTargetSet pPts2,
      final SSAMapBuilder pResultSSA,
      final CtoFormulaConverter pConverter) throws InterruptedException {

    if (pPts1.isEmpty() && pPts2.isEmpty()) {
      return MergeResult.trivial(PointerTargetSet.emptyPointerTargetSet(), bfmgr);
    }

    final CopyOnWriteSortedMap<String, CType> basesOnlyPts1 =CopyOnWriteSortedMap.copyOf(
        PathCopyingPersistentTreeMap.<String, CType>of());
    final CopyOnWriteSortedMap<String, CType> basesOnlyPts2 =CopyOnWriteSortedMap.copyOf(
        PathCopyingPersistentTreeMap.<String, CType>of());

    PersistentSortedMap<String, CType> mergedBases = merge(
        pPts1.getBases(), pPts2.getBases(), Equivalence.equals(),
        BaseUnitingConflictHandler.INSTANCE,
        new MapsDifference.DefaultVisitor<String, CType>() {
          @Override
          public void leftValueOnly(String key, CType value) {
            basesOnlyPts1.put(key, value);
          }

          @Override
          public void rightValueOnly(String key, CType value) {
            basesOnlyPts2.put(key, value);
          }

          @Override
          public void differingValues(
              String key, CType leftValue,
              CType rightValue) {
            if (isFakeBaseType(leftValue)
                && !(rightValue instanceof CElaboratedType)) {
              basesOnlyPts2.put(key, rightValue);
            } else if (isFakeBaseType(rightValue)
                && !(leftValue instanceof CElaboratedType)) {
              basesOnlyPts1.put(key, leftValue);
            }
          }
        });
    shutdownNotifier.shutdownIfNecessary();

    final CopyOnWriteSortedMap<CompositeField, Boolean> fieldsOnlyPts1 =
        CopyOnWriteSortedMap.copyOf(PathCopyingPersistentTreeMap.<CompositeField, Boolean>of());
    final CopyOnWriteSortedMap<CompositeField, Boolean> fieldsOnlyPts2 =
        CopyOnWriteSortedMap.copyOf(PathCopyingPersistentTreeMap.<CompositeField, Boolean>of());

    PersistentSortedMap<CompositeField, Boolean> mergedFields = merge(
        pPts1.getFields(), pPts2.getFields(), Equivalence.equals(),
        PersistentSortedMaps
            .<CompositeField, Boolean>getExceptionMergeConflictHandler(),
        new MapsDifference.DefaultVisitor<CompositeField, Boolean>() {
          @Override
          public void leftValueOnly(CompositeField key, Boolean value) {
            fieldsOnlyPts1.put(key, value);
          }

          @Override
          public void rightValueOnly(CompositeField key, Boolean value) {
            fieldsOnlyPts2.put(key, value);
          }
        });
    shutdownNotifier.shutdownIfNecessary();

    PersistentSortedMap<String, PersistentList<PointerTarget>> mergedTargets =
        merge(pPts1.getTargets(), pPts2.getTargets(),
            PointerTargetSetManagerHeapArray.<String, PointerTarget>mergeOnConflict());
    shutdownNotifier.shutdownIfNecessary();

    // Targets is always the cross product of bases and fields.
    // So when we merge the bases, fields, and targets by taking the union,
    // there are missing targets:
    //    (b1+b2) x (f1+f2) != (t1+t2) == ((b1 x f1) + (b2 x f2))
    // The following targets are missing:
    //    (b1 x f2) and (b2 x f1)
    // So we add exactly these targets:
    mergedTargets = addAllTargets(mergedTargets, basesOnlyPts2.getSnapshot(),
        fieldsOnlyPts1.getSnapshot());
    mergedTargets = addAllTargets(mergedTargets, basesOnlyPts1.getSnapshot(),
        fieldsOnlyPts2.getSnapshot());

    final PersistentSortedMap<String, DeferredAllocationPool> mergedAllocations =
        mergeDeferredAllocationPools(pPts1, pPts2);
    shutdownNotifier.shutdownIfNecessary();

    final String lastBase;
    final BooleanFormula mergeFormula;
    if (pPts1.getLastBase() == null
        || pPts2.getLastBase() == null
        || pPts1.getLastBase().equals(pPts2.getLastBase())) {
      // Trivial case: either no allocations on one branch at all, or no
      // difference. Just take the first non-null value, the second is either
      // equal or null.
      lastBase = (pPts1.getLastBase() != null) ? pPts1.getLastBase() : pPts2.getLastBase();
      mergeFormula = bfmgr.makeBoolean(true);
    } else if (basesOnlyPts1.isEmpty()) {
      assert pPts2.getBases().keySet().containsAll(pPts1.getBases().keySet());
      // One branch has a strict superset of the allocations of the other.
      lastBase = pPts2.getLastBase();
      mergeFormula = bfmgr.makeBoolean(true);
    } else if (basesOnlyPts2.isEmpty()) {
      assert pPts1.getBases().keySet().containsAll(pPts2.getBases().keySet());
      // One branch has a strict superset of the allocations of the other.
      lastBase = pPts1.getLastBase();
      mergeFormula = bfmgr.makeBoolean(true);
    } else {
      // Otherwise we have no possibility to determine which base to use as
      // lastBase, so we create an additional fake one.
      final CType fakeType = getFakeBaseType(0);
      final String fakeName = DynamicMemoryHandler.makeAllocVariableName(
          FAKE_ALLOC_FUNCTION_NAME, fakeType, pResultSSA, pConverter);
      mergedBases = mergedBases.putAndCopy(fakeName, fakeType);
      lastBase = fakeName;
      mergeFormula = formulaManager.makeAnd(
          getNextBaseAddressInequality(fakeName, pPts1.getBases(), pPts1.getLastBase()),
          getNextBaseAddressInequality(fakeName, pPts2.getBases(), pPts2.getLastBase()));
    }

    PointerTargetSet result = new PointerTargetSet(mergedBases, lastBase, mergedFields,
        mergedAllocations, mergedTargets);

    final List<Pair<CCompositeType, String>> sharedFields = new ArrayList<>();
    final BooleanFormula mergeFormula2 = makeValueImportConstraints(
        basesOnlyPts1.getSnapshot(), sharedFields, pResultSSA);
    final BooleanFormula mergeFormula1 = makeValueImportConstraints(
        basesOnlyPts2.getSnapshot(), sharedFields, pResultSSA);

    if (!sharedFields.isEmpty()) {
      final PointerTargetSetBuilder resultBuilder =
          new RealPointerTargetSetBuilder(result, formulaManager, this, options);
      for (final Pair<CCompositeType, String> sharedField : sharedFields) {
        resultBuilder.addField(sharedField.getFirst(), sharedField.getSecond());
      }
      result = resultBuilder.build();
    }

    return new MergeResult<>(result, mergeFormula1, mergeFormula2, mergeFormula);
  }

  /**
   * Merges two {@link DeferredAllocationPool}s into one.
   *
   * @param pPts1 The first pool.
   * @param pPts2 The second pool.
   * @return A merged {@code DeferredAllocationPool} with the content of both parameters.
   */
  private PersistentSortedMap<String, DeferredAllocationPool> mergeDeferredAllocationPools(
      final PointerTargetSet pPts1,
      final PointerTargetSet pPts2) {
    final Map<DeferredAllocationPool, DeferredAllocationPool> mergedPools = new HashMap<>();
    final MergeConflictHandler<String, DeferredAllocationPool> mergeConflictHandler =
        new MergeConflictHandler<String, DeferredAllocationPool>() {
      @Override
      public DeferredAllocationPool resolveConflict(
          String key,
          DeferredAllocationPool a,
          DeferredAllocationPool b) {
        final DeferredAllocationPool result = a.mergeWith(b);
        final DeferredAllocationPool oldResult = mergedPools.get(result);
        if (oldResult == null) {
          mergedPools.put(result, result);
          return result;
        } else {
          final DeferredAllocationPool newResult = oldResult.mergeWith(result);
          mergedPools.put(newResult, newResult);
          return newResult;
        }
      }
    };

    PersistentSortedMap<String, DeferredAllocationPool> mergedAllocations = merge(
        pPts1.getDeferredAllocations(), pPts2.getDeferredAllocations(), mergeConflictHandler);
    for (final DeferredAllocationPool merged : mergedPools.keySet()) {
      for (final String pointerVariable : merged.getPointerVariables()) {
        mergedAllocations = mergedAllocations.putAndCopy(pointerVariable, merged);
      }
    }

    return mergedAllocations;
  }

  /**
   * A handler for merge conflicts that appear when merging bases.
   */
  private enum BaseUnitingConflictHandler
      implements MergeConflictHandler<String, CType> {
    INSTANCE;

    /**
     * Resolves a merge conflict between two types and returns the resolved type
     *
     * @param pKey   Not used in the algorithm.
     * @param pType1 The first type to merge.
     * @param pType2 The second type to merge.
     * @return A conflict resolving C type.
     */
    @Override
    public CType resolveConflict(
        final String pKey, final CType pType1,
        final CType pType2) {
      if (isFakeBaseType(pType1)) {
        return pType2;
      } else if (isFakeBaseType(pType2)) {
        return pType1;
      }

      int currentFieldIndex = 0;
      final ImmutableList.Builder<CCompositeTypeMemberDeclaration> membersBuilder =
          ImmutableList.builder();
      if (pType1 instanceof CCompositeType) {
        final CCompositeType compositeType1 = (CCompositeType) pType1;

        if (compositeType1.getKind() == ComplexTypeKind.UNION
            && !compositeType1.getMembers().isEmpty()
            && compositeType1.getMembers().get(0).getName().equals(getUnitedFieldBaseName(0))) {
          membersBuilder.addAll(compositeType1.getMembers());
          currentFieldIndex += compositeType1.getMembers().size();
        } else {
          membersBuilder.add(new CCompositeTypeMemberDeclaration(compositeType1,
              getUnitedFieldBaseName(currentFieldIndex)));
          currentFieldIndex++;
        }

      } else {
        membersBuilder.add(new CCompositeTypeMemberDeclaration(pType1,
            getUnitedFieldBaseName(currentFieldIndex)));
        currentFieldIndex++;
      }

      if (pType2 instanceof CCompositeType) {
        final CCompositeType compositeType2 = (CCompositeType) pType2;

        if (compositeType2.getKind() == ComplexTypeKind.UNION
            && !compositeType2.getMembers().isEmpty()
            && compositeType2.getMembers().get(0).getName().equals(
            getUnitedFieldBaseName(0))) {
          for (@SuppressWarnings("unused") CCompositeTypeMemberDeclaration ignored
              : compositeType2.getMembers()) {
            membersBuilder.add(new CCompositeTypeMemberDeclaration(compositeType2,
                getUnitedFieldBaseName(currentFieldIndex)));
            currentFieldIndex++;
          }
        } else {
          membersBuilder.add(new CCompositeTypeMemberDeclaration(compositeType2,
              getUnitedFieldBaseName(currentFieldIndex)));
        }

      } else {
        membersBuilder.add(new CCompositeTypeMemberDeclaration(pType2,
            getUnitedFieldBaseName(currentFieldIndex)));
      }

      String name = UNITED_BASE_UNION_TAG_PREFIX
          + pType1.toString().replace(' ', '_')
          + "_and_"
          + pType2.toString().replace(' ', '_');
      return new CCompositeType(false, false, ComplexTypeKind.UNION, membersBuilder.build(),
          name, name);
    }
  }

  /**
   * Gives a handler for merge conflicts.
   *
   * @param <K> The type of the keys in the merge conflict handler.
   * @param <T> The type of the list entries in the merge conflict handler.
   * @return A handler for merge conflicts.
   */
  private static <K, T> MergeConflictHandler<K, PersistentList<T>> mergeOnConflict() {
    return new MergeConflictHandler<K, PersistentList<T>>() {
      @Override
      public PersistentList<T> resolveConflict(
          K pKey, PersistentList<T> pList1,
          PersistentList<T> pList2) {
        return DeferredAllocationPool.mergeLists(pList1, pList2);
      }
    };
  }

  /**
   * Create constraint that imports the old value of a variable into the memory handled with UFs.
   *
   * @param pNewBases      A map of new bases.
   * @param pSharedFields  A list of shared fields.
   * @param pSSAMapBuilder The SSA map.
   * @return A boolean formula for the import constraint.
   */
  private BooleanFormula makeValueImportConstraints(
      final PersistentSortedMap<String, CType> pNewBases,
      final List<Pair<CCompositeType, String>> pSharedFields,
      final SSAMapBuilder pSSAMapBuilder) {

    BooleanFormula mergeFormula = bfmgr.makeBoolean(true);
    for (final Map.Entry<String, CType> base : pNewBases.entrySet()) {
      if (!options.isDynamicAllocVariableName(base.getKey())
          && !CTypeUtils.containsArray(base.getValue())) {
        final FormulaType<?> formulaType = typeHandler.getFormulaTypeFromCType(
            CTypeUtils.getBaseType(base.getValue()));
        mergeFormula = bfmgr.and(mergeFormula,
            makeValueImportConstraints(
                formulaManager.makeVariable(
                    formulaType, PointerTargetSet.getBaseName(base.getKey())),
                base.getKey(), base.getValue(), pSharedFields, pSSAMapBuilder));
      }
    }

    return mergeFormula;
  }

  /**
   * Create constraint that imports the old value of a variable into the memory handled with UFs.
   *
   * @param pAddress        The formula for the address.
   * @param pVariablePrefix A prefix for variables.
   * @param pVariableType   The type of the variable.
   * @param pSharedFields   A list of shared fields.
   * @param pSSAMapBuilder  The SSA map.
   * @return A boolean formula for the import constraint.
   */
  private BooleanFormula makeValueImportConstraints(
      final Formula pAddress,
      final String pVariablePrefix,
      final CType pVariableType,
      final List<Pair<CCompositeType, String>> pSharedFields,
      final SSAMapBuilder pSSAMapBuilder) {
    assert !CTypeUtils.containsArray(pVariableType) : "Array access can't be encoded as a variable";

    BooleanFormula result = bfmgr.makeBoolean(true);

    if (pVariableType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) pVariableType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: "
          + compositeType;

      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final String name = memberDeclaration.getName();
        final CType type = CTypeUtils.simplifyType(memberDeclaration.getType());
        final String prefix = pVariablePrefix
            + CToFormulaConverterWithHeapArray.FIELD_NAME_SEPARATOR + name;

        if (pSSAMapBuilder.getIndex(prefix) > 0) {
          pSharedFields.add(Pair.of(compositeType, name));

          result = bfmgr.and(result,
              makeValueImportConstraints(
                  formulaManager.makePlus(pAddress,
                      formulaManager.makeNumber(
                          typeHandler.getPointerType(), offset)),
                  prefix, type, pSharedFields, pSSAMapBuilder));
        }

        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += typeHandler.getSizeof(type);
        }
      }
    } else {
      if (pSSAMapBuilder.getIndex(pVariablePrefix) > 0) {
        final FormulaType<?> variableFormulaType =
            typeHandler.getFormulaTypeFromCType(pVariableType);
        result = bfmgr.and(result,
            formulaManager.makeEqual(makeDereference(pVariableType, pAddress, pSSAMapBuilder),
                formulaManager.makeVariable(
                    variableFormulaType, pVariablePrefix,
                    pSSAMapBuilder.getIndex(pVariablePrefix))));
      }
    }

    return result;
  }

  /**
   * Creates a formula for a dereference of a type.
   *
   * @param pType          The type to be dereferenced.
   * @param pAddress       The formula of the type's address.
   * @param pSSAMapBuilder The SSA map.
   * @return A formula for the dereference of the type.
   */
  private Formula makeDereference(
      final CType pType,
      final Formula pAddress,
      final SSAMapBuilder pSSAMapBuilder) {
    final String ufName = CToFormulaConverterWithHeapArray.getArrayName(pType);
    final int index = pSSAMapBuilder.getIndex(ufName);
    final FormulaType<?> returnType = typeHandler.getFormulaTypeFromCType(pType);

    final ArrayFormula<?, ?> arrayFormula = afmgr.makeArray(ufName + "@" + index,
        FormulaType.IntegerType, returnType);
    return afmgr.select(arrayFormula, pAddress);
  }

  /**
   * The method is used to speed up {@code sizeof} computation by caching sizes of declared
   * composite types.
   *
   * @param pType The type to determine the size of.
   * @return The size of a given type.
   */
  @Override
  protected int getSize(CType pType) {
    return typeHandler.getSizeof(pType);
  }

  /**
   * The method is used to speed up member offset computation for declared composite types.
   *
   * @param pCompositeType The composite type.
   * @param pMemberName    The name of the member of the composite type.
   * @return The offset of the member in the composite type.
   */
  @Override
  public int getOffset(
      final CCompositeType pCompositeType,
      final String pMemberName) {
    return typeHandler.getOffset(pCompositeType, pMemberName);
  }

  /**
   * Gets the next base address.
   *
   * @param pNewBase  Not used.
   * @param pBases    A map of existing bases.
   * @param pLastBase The name of the last added base.
   * @return A formula for the next base address.
   */
  @Override
  protected BooleanFormula getNextBaseAddressInequality(
      final String pNewBase,
      final PersistentSortedMap<String, CType> pBases,
      final String pLastBase) {
    final FormulaType<?> pointerType = typeHandler.getPointerType();
    final Formula newBaseFormula = formulaManager.makeVariable(pointerType,
        PointerTargetSet.getBaseName(pNewBase));

    if (pLastBase != null) {
      final CType lastType = pBases.get(pLastBase);
      final int lastSize =
          lastType.isIncomplete()
              ? options.defaultAllocationSize()
              : typeHandler.getSizeof(lastType);
      final Formula rhs = formulaManager.makePlus(
          formulaManager.makeVariable(pointerType,
              PointerTargetSet.getBaseName(pLastBase)),
          formulaManager.makeNumber(pointerType, lastSize));
      // The condition rhs > 0 prevents overflows in case of bit-vector encoding
      return formulaManager.makeAnd(
          formulaManager.makeGreaterThan(rhs,
              formulaManager.makeNumber(pointerType, 0L), true),
          formulaManager.makeGreaterOrEqual(newBaseFormula, rhs, true));
    } else {
      return formulaManager.makeGreaterThan(newBaseFormula,
          formulaManager.makeNumber(pointerType, 0L), true);
    }
  }

  /**
   * Adds pointer targets for every used (tracked) (sub)field of the newly allocated base.
   *
   * @param pBase            The name of the base.
   * @param pTargetType      The type of the target.
   * @param pContainerType   The type of the container, might be {@code null}.
   * @param pProperOffset    The offset.
   * @param pContainerOffset The offset in the container.
   * @param pTargets         The map of available targets.
   * @return The new map of targets.
   */
  @CheckReturnValue
  private static PersistentSortedMap<String, PersistentList<PointerTarget>> addToTarget(
      final String pBase,
      final CType pTargetType,
      final @Nullable CType pContainerType,
      final int pProperOffset,
      final int pContainerOffset,
      final PersistentSortedMap<String, PersistentList<PointerTarget>> pTargets) {

    final String type = CTypeUtils.typeToString(pTargetType);
    PersistentList<PointerTarget> targetsForType = firstNonNull(
        pTargets.get(type), PersistentLinkedList.<PointerTarget>of());

    return pTargets.putAndCopy(type, targetsForType.with(
        new PointerTarget(pBase, pContainerType, pProperOffset, pContainerOffset)));
  }

  /**
   * Recursively adds pointer targets for every used (tracked) (sub)field of the newly allocated
   * base.
   *
   * Note: the recursion doesn't proceed on unused (untracked) (sub)fields.
   *
   * @param pBase            the name of the newly allocated base variable
   * @param pCurrentType     type of the allocated base or the next added pointer target
   * @param pContainerType   either {@code null} or the type of the innermost container of the next
   *                         added pointer target
   * @param pProperOffset    either {@code 0} or the offset of the next added pointer target in its
   *                         innermost container
   * @param pContainerOffset either {@code 0} or the offset of the innermost container (relative to
   *                         the base adddress)
   * @param pTargets         The list of targets where the new targets should be added to
   * @param pFields          The set of "shared" fields that are accessed directly via pointers.
   * @return The targets map together with all the added targets.
   */
  @CheckReturnValue
  @Override
  protected PersistentSortedMap<String, PersistentList<PointerTarget>> addToTargets(
      final String pBase,
      String region,
      final CType pCurrentType,
      final @Nullable CType pContainerType,
      final int pProperOffset,
      final int pContainerOffset,
      PersistentSortedMap<String, PersistentList<PointerTarget>> pTargets,
      final PersistentSortedMap<CompositeField, Boolean> pFields) {

    final CType type = CTypeUtils.simplifyType(pCurrentType);
    assert !(type instanceof CElaboratedType) : "Unresolved elaborated type " + type + " for base "
        + pBase;

    if (type instanceof CArrayType) {
      final CArrayType arrayType = (CArrayType) type;
      Integer length = CTypeUtils.getArrayLength(arrayType);

      if (length == null) {
        length = options.defaultArrayLength();
      }

      int offset = 0;
      for (int i = 0; i < length; ++i) {
        pTargets = addToTargets(pBase, null, arrayType.getType(), arrayType, offset,
            pContainerOffset + pProperOffset, pTargets, pFields);
        offset += getSize(arrayType.getType());
      }

    } else if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: "
          + compositeType;

      final String typeName = CTypeUtils.typeToString(compositeType);
      typeHandler.addCompositeTypeToCache(compositeType);

      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration
          : compositeType.getMembers()) {
        if (pFields.containsKey(CompositeField.of(typeName, memberDeclaration.getName()))) {
          pTargets = addToTargets(pBase, null, memberDeclaration.getType(), compositeType, offset,
              pContainerOffset + pProperOffset, pTargets, pFields);
        }

        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += getSize(memberDeclaration.getType());
        }
      }

    } else {
      pTargets = addToTarget(pBase, type, pContainerType, pProperOffset, pContainerOffset, pTargets);
    }

    return pTargets;
  }

  /**
   * Compute all targets for a given set of bases and fields, and add them to a map.
   *
   * @param pTargets A map of existing targets
   * @param pBases   A set of bases
   * @param pFields  A set of fields
   * @return A map of existing targets
   */
  @CheckReturnValue
  private PersistentSortedMap<String, PersistentList<PointerTarget>> addAllTargets(
      PersistentSortedMap<String, PersistentList<PointerTarget>> pTargets,
      final PersistentSortedMap<String, CType> pBases,
      final PersistentSortedMap<CompositeField, Boolean> pFields) {
    for (final Map.Entry<String, CType> entry : pBases.entrySet()) {
      String name = entry.getKey();
      CType type = CTypeUtils.simplifyType(entry.getValue());
      pTargets = addToTargets(name, null, type, null, 0, 0, pTargets, pFields);
    }
    return pTargets;
  }

}
