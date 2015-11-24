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
import static org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView.IS_POINTER_SIGNED;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;

/**
 * A manager for pointer target sets.
 */
public class PointerTargetSetManager {

  private static final String UNITED_BASE_UNION_TAG_PREFIX = "__VERIFIER_base_union_of_";
  private static final String UNITED_BASE_FIELD_NAME_PREFIX = "__VERIFIER_united_base_field";

  private static final String FAKE_ALLOC_FUNCTION_NAME = "__VERIFIER_fake_alloc";

  /**
   * Returns whether a {@code CType} is a fake base type or not.
   * <p/>
   * A fake base type is an array of void.
   *
   * @param type The type to be checked.
   * @return Whether the type is a fake base type or not.
   */
  static boolean isFakeBaseType(final CType type) {
    return type instanceof CArrayType
        && ((CArrayType) type).getType() instanceof CVoidType;
  }

  /**
   * Returns a fake base type of a given size, i.e. an array of {@code size}
   * voids.
   *
   * @param size The size of the fake base type.
   * @return An array of {@code size} voids.
   */
  static CType getFakeBaseType(int size) {
    return CTypeUtils.simplifyType(
        new CArrayType(false, false, CVoidType.VOID,
            new CIntegerLiteralExpression(FileLocation.DUMMY,
                CNumericTypes.SIGNED_CHAR, BigInteger.valueOf(size))));
  }

  /**
   * Returns a name for united field bases with a specified index.
   *
   * @param index The index of the united field base.
   * @return A name for the united field base.
   */
  private static String getUnitedFieldBaseName(final int index) {
    return UNITED_BASE_FIELD_NAME_PREFIX + index;
  }

  private final ShutdownNotifier shutdownNotifier;
  private final FormulaEncodingWithPointerAliasingOptions options;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView bfmgr;
  private final FunctionFormulaManagerView ffmgr;
  private final TypeHandlerWithPointerAliasing typeHandler;

  /**
   * Creates a new PointerTargetSetManager.
   *
   * @param options Additional configuration options.
   * @param formulaManager The manager for SMT formulae.
   * @param typeHandler A type handler for certain types.
   * @param shutdownNotifier A notifier for external shutdowns to stop
   *                         long-running algorithms.
   */
  public PointerTargetSetManager(
      final FormulaEncodingWithPointerAliasingOptions options,
      final FormulaManagerView formulaManager,
      final TypeHandlerWithPointerAliasing typeHandler,
      final ShutdownNotifier shutdownNotifier) {
    this.options = options;
    this.formulaManager = formulaManager;
    bfmgr = this.formulaManager.getBooleanFormulaManager();
    ffmgr = this.formulaManager.getFunctionFormulaManager();
    this.typeHandler = typeHandler;
    this.shutdownNotifier = shutdownNotifier;
  }

  /**
   * Merges two {@link PointerTargetSet}s into one.
   *
   * @param pts1 The first {@code PointerTargetSet}.
   * @param pts2 The second {@code PointerTargetSet}.
   * @param resultSSA The map of SSA indices.
   * @param converter The converter for C code to SMT formulae.
   * @return The merged {@code PointerTargetSet}s.
   * @throws InterruptedException If the algorithms gets interrupted by an
   *                              external shutdown.
   */
  public MergeResult<PointerTargetSet> mergePointerTargetSets(
      final PointerTargetSet pts1,
      final PointerTargetSet pts2,
      final SSAMapBuilder resultSSA,
      final CtoFormulaConverter converter) throws InterruptedException {

    if (pts1.isEmpty() && pts2.isEmpty()) {
      return MergeResult.trivial(PointerTargetSet.emptyPointerTargetSet(),
          bfmgr);
    }

    final CopyOnWriteSortedMap<String, CType> basesOnlyPts1 =
        CopyOnWriteSortedMap.copyOf(
            PathCopyingPersistentTreeMap.<String, CType>of());
    final CopyOnWriteSortedMap<String, CType> basesOnlyPts2 =
        CopyOnWriteSortedMap.copyOf(
            PathCopyingPersistentTreeMap.<String, CType>of());

    PersistentSortedMap<String, CType> mergedBases = merge(
        pts1.getBases(), pts2.getBases(), Equivalence.equals(),
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
          public void differingValues(String key, CType leftValue,
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
        CopyOnWriteSortedMap.copyOf(
            PathCopyingPersistentTreeMap.<CompositeField, Boolean>of());
    final CopyOnWriteSortedMap<CompositeField, Boolean> fieldsOnlyPts2 =
        CopyOnWriteSortedMap.copyOf(
            PathCopyingPersistentTreeMap.<CompositeField, Boolean>of());

    PersistentSortedMap<CompositeField, Boolean> mergedFields = merge(
        pts1.getFields(), pts2.getFields(), Equivalence.equals(),
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
        merge(pts1.getTargets(), pts2.getTargets(),
            PointerTargetSetManager.<String, PointerTarget>mergeOnConflict());
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

    final PersistentSortedMap<String ,DeferredAllocationPool> mergedAllocations =
        mergeDeferredAllocationPools(pts1, pts2);
    shutdownNotifier.shutdownIfNecessary();

    final String lastBase;
    final BooleanFormula mergeFormula;
    if (pts1.getLastBase() == null
        || pts2.getLastBase() == null
        || pts1.getLastBase().equals(pts2.getLastBase())) {
      // Trivial case: either no allocations on one branch at all, or no
      // difference. Just take the first non-null value, the second is either
      // equal or null.
      lastBase =
          (pts1.getLastBase() != null) ? pts1.getLastBase() : pts2.getLastBase();
      mergeFormula = bfmgr.makeBoolean(true);
    } else if (basesOnlyPts1.isEmpty()) {
      assert pts2.getBases().keySet().containsAll(pts1.getBases().keySet());
      // One branch has a strict superset of the allocations of the other.
      lastBase = pts2.getLastBase();
      mergeFormula = bfmgr.makeBoolean(true);
    } else if (basesOnlyPts2.isEmpty()) {
      assert pts1.getBases().keySet().containsAll(pts2.getBases().keySet());
      // One branch has a strict superset of the allocations of the other.
      lastBase = pts1.getLastBase();
      mergeFormula = bfmgr.makeBoolean(true);
    } else {
      // Otherwise we have no possibility to determine which base to use as
      // lastBase, so we create an additional fake one.
      final CType fakeType = getFakeBaseType(0);
      final String fakeName = DynamicMemoryHandler.makeAllocVariableName(
          FAKE_ALLOC_FUNCTION_NAME, fakeType, resultSSA, converter);
      mergedBases = mergedBases.putAndCopy(fakeName, fakeType);
      lastBase = fakeName;
      mergeFormula = formulaManager.makeAnd(
          getNextBaseAddressInequality(fakeName, pts1.getBases(),
              pts2.getLastBase()),
          getNextBaseAddressInequality(fakeName, pts2.getBases(),
              pts2.getLastBase()));
    }

    PointerTargetSet result = new PointerTargetSet(mergedBases, lastBase,
        mergedFields, mergedAllocations, mergedTargets);

    final List<Pair<CCompositeType, String>> sharedFields = new ArrayList<>();
    final BooleanFormula mergeFormula2 = makeValueImportConstraints(
        basesOnlyPts1.getSnapshot(), sharedFields, resultSSA, pts2);
    final BooleanFormula mergeFormula1 = makeValueImportConstraints(
        basesOnlyPts2.getSnapshot(), sharedFields, resultSSA, pts1);

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
   * @param pts1 The first pool.
   * @param pts2 The second pool.
   * @return A merged {@code DeferredAllocationPool} with the content of both
   *         parameters.
   */
  private PersistentSortedMap<String, DeferredAllocationPool>
  mergeDeferredAllocationPools(final PointerTargetSet pts1,
      final PointerTargetSet pts2) {
    final Map<DeferredAllocationPool, DeferredAllocationPool> mergedPools =
        new HashMap<>();
    final MergeConflictHandler<String, DeferredAllocationPool>
        mergeConflictHandler = new MergeConflictHandler<String,
        DeferredAllocationPool>() {
      @Override
      public DeferredAllocationPool resolveConflict(String key,
          DeferredAllocationPool a, DeferredAllocationPool b) {
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

    PersistentSortedMap<String, DeferredAllocationPool> mergedAllocations =
        merge(pts1.getDeferredAllocations(), pts2.getDeferredAllocations(),
            mergeConflictHandler);
    for (final DeferredAllocationPool merged : mergedPools.keySet()) {
      for (final String pointerVariable : merged.getPointerVariables()) {
        mergedAllocations = mergedAllocations.putAndCopy(pointerVariable,
            merged);
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
     * @param key Not used in the algorithm.
     * @param type1 The first type to merge.
     * @param type2 The second type to merge.
     * @return A conflict resolving C type.
     */
    @Override
    public CType resolveConflict(final String key, final CType type1,
        final CType type2) {
      if (isFakeBaseType(type1)) {
        return type2;
      } else if (isFakeBaseType(type2)) {
        return type1;
      }

      int currentFieldIndex = 0;
      final ImmutableList.Builder<CCompositeTypeMemberDeclaration>
          membersBuilder = ImmutableList.builder();
      if (type1 instanceof CCompositeType) {
        final CCompositeType compositeType1 = (CCompositeType) type1;

        if (compositeType1.getKind() == ComplexTypeKind.UNION
            && !compositeType1.getMembers().isEmpty()
            && compositeType1.getMembers().get(0).getName().equals(
                getUnitedFieldBaseName(0))) {
          membersBuilder.addAll(compositeType1.getMembers());
          currentFieldIndex += compositeType1.getMembers().size();
        } else {
          membersBuilder.add(
              new CCompositeTypeMemberDeclaration(compositeType1,
                  getUnitedFieldBaseName(currentFieldIndex)));
          currentFieldIndex++;
        }

      } else {
        membersBuilder.add(
            new CCompositeTypeMemberDeclaration(type1,
                getUnitedFieldBaseName(currentFieldIndex)));
        currentFieldIndex++;
      }

      if (type2 instanceof CCompositeType) {
        final CCompositeType compositeType2 = (CCompositeType) type2;

        if (compositeType2.getKind() == ComplexTypeKind.UNION
            && !compositeType2.getMembers().isEmpty()
            && compositeType2.getMembers().get(0).getName().equals(
                getUnitedFieldBaseName(0))) {
          for (CCompositeTypeMemberDeclaration memberDeclaration
              : compositeType2.getMembers()) {
            membersBuilder.add(
                new CCompositeTypeMemberDeclaration(compositeType2,
                    getUnitedFieldBaseName(currentFieldIndex)));
            currentFieldIndex++;
          }
        } else {
          membersBuilder.add(
              new CCompositeTypeMemberDeclaration(compositeType2,
                  getUnitedFieldBaseName(currentFieldIndex)));
          currentFieldIndex++;
        }

      } else {
        membersBuilder.add(
            new CCompositeTypeMemberDeclaration(type2,
                getUnitedFieldBaseName(currentFieldIndex)));
        currentFieldIndex++;
      }

      String name = UNITED_BASE_UNION_TAG_PREFIX
          + type1.toString().replace(' ', '_')
          + "_and_"
          + type2.toString().replace(' ', '_');
      return new CCompositeType(false, false, ComplexTypeKind.UNION,
          membersBuilder.build(), name, name);
    }
  }

  /**
   * Gives a handler for merge conflicts.
   *
   * @param <K> The type of the keys in the merge conflict handler.
   * @param <T> The type of the list entries in the merge conflict handler.
   * @return A handler for merge conflicts.
   */
  private static <K, T> MergeConflictHandler<K, PersistentList<T>>
  mergeOnConflict() {
    return new MergeConflictHandler<K, PersistentList<T>>() {
      @Override
      public PersistentList<T> resolveConflict(K key, PersistentList<T> list1,
          PersistentList<T> list2) {
        return DeferredAllocationPool.mergeLists(list1, list2);
      }
    };
  }

  /**
   * Create constraint that imports the old value of a variable into the
   * memory handled with UFs.
   *
   * @param newBases A map of new bases.
   * @param sharedFields A list of shared fields.
   * @param ssa The SSA map.
   * @param pts The underlying PointerTargetSet
   * @return A boolean formula for the import constraint.
   */
  private BooleanFormula makeValueImportConstraints(
      final PersistentSortedMap<String, CType> newBases,
      final List<Pair<CCompositeType, String>> sharedFields,
      final SSAMapBuilder ssa,
      final PointerTargetSet pts) {

    BooleanFormula mergeFormula = bfmgr.makeBoolean(true);
    for (final Map.Entry<String, CType> base : newBases.entrySet()) {
      if (!options.isDynamicAllocVariableName(base.getKey())
          && !CTypeUtils.containsArray(base.getValue())) {
        final FormulaType<?> formulaType = typeHandler.getFormulaTypeFromCType(
            CTypeUtils.getBaseType(base.getValue()));
        mergeFormula = bfmgr.and(mergeFormula,
            makeValueImportConstraints(formulaManager.makeVariable(formulaType, PointerTargetSet.getBaseName(base.getKey())), base.getKey(), base.getValue(), sharedFields, ssa, pts));
      }
    }

    return mergeFormula;
  }

  /**
   * Create constraint that imports the old value of a variable into the
   * memory handled with UFs.
   *
   * @param address The formula for the address.
   * @param variablePrefix A prefix for variables.
   * @param variableType The type of the variable.
   * @param sharedFields A list of shared fields.
   * @param ssa The SSA map.
   * @param pts The underlying PointerTargetSet.
   * @return A boolean formula for the import constraint.
   */
  private BooleanFormula makeValueImportConstraints(
      final Formula address,
      final String variablePrefix,
      final CType variableType,
      final List<Pair<CCompositeType, String>> sharedFields,
      final SSAMapBuilder ssa,
      final PointerTargetSet pts) {
    assert !CTypeUtils.containsArray(variableType) : "Array access can't be "
        + "encoded as a variable";

    BooleanFormula result = bfmgr.makeBoolean(true);

    if (variableType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) variableType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not "
          + "composite: " + compositeType;

      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration
          : compositeType.getMembers()) {
        final String name = memberDeclaration.getName();
        final CType type = CTypeUtils.simplifyType(memberDeclaration.getType());
        final String prefix = variablePrefix
            + CToFormulaConverterWithHeapArray.FIELD_NAME_SEPARATOR + name;

        if (ssa.getIndex(prefix) > 0) {
          sharedFields.add(Pair.of(compositeType, name));

          result = bfmgr.and(result,
              makeValueImportConstraints(
                  formulaManager.makePlus(address,
                      formulaManager.makeNumber(
                          typeHandler.getPointerType(), offset),
                      IS_POINTER_SIGNED),
                  prefix, type, sharedFields, ssa, pts));
        }

        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += typeHandler.getSizeof(type);
        }
      }
    } else {
      if (ssa.getIndex(variablePrefix) > 0) {
        final FormulaType<?> variableFormulaType =
            typeHandler.getFormulaTypeFromCType(variableType);
        result = bfmgr.and(result,
            formulaManager.makeEqual(makeDereference(variableType, address, ssa),
                formulaManager.makeVariable(
                    variableFormulaType, variablePrefix,
                    ssa.getIndex(variablePrefix))));
      }
    }

    return result;
  }

  /**
   * Creates a formula for a dereference of a type.
   *
   * @param type The type to be dereferenced.
   * @param address The formula of the type's address.
   * @param ssa The SSA map.
   * @return A formula for the dereference of the type.
   */
  private Formula makeDereference(final CType type,
      final Formula address,
      final SSAMapBuilder ssa) {
    final String ufName = CToFormulaConverterWithHeapArray.getUFName(type);
    final int index = ssa.getIndex(ufName);
    final FormulaType<?> returnType = typeHandler.getFormulaTypeFromCType(type);
    // Todo: Change this for array formula manager
    return ffmgr.declareAndCallUninterpretedFunction(ufName, index, returnType,
        address);
  }

  /**
   * The method is used to speed up {@code sizeof} computation by caching sizes
   * of declared composite types.
   *
   * @param type The type to determine the size of.
   * @return The size of a given type.
   */
  int getSize(CType type) {
    return typeHandler.getSizeof(type);
  }

  /**
   * The method is used to speed up member offset computation for declared
   * composite types.
   *
   * @param compositeType The composite type.
   * @param memberName The name of the member of the composite type.
   * @return The offset of the member in the composite type.
   */
  public int getOffset(final CCompositeType compositeType,
      final String memberName) {
    return typeHandler.getOffset(compositeType, memberName);
  }

  /**
   * Gets the next base address.
   *
   * @param newBase Not used.
   * @param bases A map of existing bases.
   * @param lastBase The name of the last added base.
   * @return A formula for the next base address.
   */
  BooleanFormula getNextBaseAddressInequality(final String newBase,
      final PersistentSortedMap<String, CType> bases,
      final String lastBase) {
    final FormulaType<?> pointerType = typeHandler.getPointerType();
    final Formula newBaseFormula = formulaManager.makeVariable(pointerType,
        PointerTargetSet.getBaseName(lastBase));

    if (lastBase != null) {
      final int lastSize = typeHandler.getSizeof(bases.get(lastBase));
      final Formula rhs = formulaManager.makePlus(
          formulaManager.makeVariable(pointerType,
              PointerTargetSet.getBaseName(lastBase)),
          formulaManager.makeNumber(pointerType, lastSize),
          IS_POINTER_SIGNED);
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
   * Adds pointer targets for every used (tracked) (sub)field of the newly
   * allocated base.
   *
   * @param base The name of the base.
   * @param targetType The type of the target.
   * @param containerType The type of the container, might be {@code null}.
   * @param properOffset The offset.
   * @param containerOffset The offset in the container.
   * @param targets The map of available targets.
   * @return The new map of targets.
   */
  @CheckReturnValue
  private static PersistentSortedMap<String, PersistentList<PointerTarget>>
  addToTarget(final String base,
      final CType targetType,
      final @Nullable CType containerType,
      final int properOffset,
      final int containerOffset,
      final PersistentSortedMap<String, PersistentList<PointerTarget>> targets) {

    final String type = CTypeUtils.typeToString(targetType);
    PersistentList<PointerTarget> targetsForType = firstNonNull(
        targets.get(type), PersistentLinkedList.<PointerTarget>of());

    return targets.putAndCopy(type, targetsForType.with(
        new PointerTarget(base, containerType, properOffset, containerOffset)));
  }

  /**
   * Recursively adds pointer targets for every used (tracked) (sub)field of the
   * newly allocated base.
   *
   * Note: the recursion doesn't proceed on unused (untracked) (sub)fields.
   *
   * @param base the name of the newly allocated base variable
   * @param currentType type of the allocated base or the next added pointer
   *                    target
   * @param containerType either {@code null} or the type of the innermost
   *                      container of the next added pointer target
   * @param properOffset either {@code 0} or the offset of the next added
   *                     pointer target in its innermost container
   * @param containerOffset either {@code 0} or the offset of the innermost
   *                        container (relative to the base adddress)
   * @param targets The list of targets where the new targets should be added to
   * @param fields The set of "shared" fields that are accessed directly via
   *               pointers.
   * @return The targets map together with all the added targets.
   */
  @CheckReturnValue
  PersistentSortedMap<String, PersistentList<PointerTarget>> addToTargets(
      final String base,
      final CType currentType,
      final @Nullable CType containerType,
      final int properOffset,
      final int containerOffset,
      PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
      final PersistentSortedMap<CompositeField, Boolean> fields) {

    final CType type = CTypeUtils.simplifyType(currentType);
    assert !(type instanceof CElaboratedType) : "Unresolved elaborated type "
        + type + " for base " + base;

    if (type instanceof CArrayType) {
      final CArrayType arrayType = (CArrayType) type;
      Integer length = CTypeUtils.getArrayLength(arrayType);

      if (length == null) {
        length = options.defaultArrayLength();
      } else if (length > options.maxArrayLength()) {
        length = options.maxArrayLength();
      }

      int offset = 0;
      for (int i = 0; i < length; ++i) {
        targets = addToTargets(base, arrayType.getType(), arrayType, offset,
            containerOffset + properOffset, targets, fields);
        offset += getSize(arrayType.getType());
      }

    } else if (type instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) type;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not "
          + "composite: " + compositeType;

      final String typeName = CTypeUtils.typeToString(compositeType);
      typeHandler.addCompositeTypeToCache(compositeType);

      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration
          : compositeType.getMembers()) {
        if (fields.containsKey(CompositeField.of(
            typeName, memberDeclaration.getName()))) {
          targets = addToTargets(base, memberDeclaration.getType(),
              compositeType, offset, containerOffset + properOffset, targets,
              fields);
        }

        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += getSize(memberDeclaration.getType());
        }
      }

    } else {
      targets = addToTarget(base, type, containerType, properOffset,
          containerOffset, targets);
    }

    return targets;
  }

  /**
   * Compute all targets for a given set of bases and fields, and add them to a
   * map.
   *
   * @param targets
   * @param bases
   * @param fields
   * @return
   */
  @CheckReturnValue
  private PersistentSortedMap<String, PersistentList<PointerTarget>>
  addAllTargets(
      PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
      final PersistentSortedMap<String, CType> bases,
      final PersistentSortedMap<CompositeField, Boolean> fields) {
    for (final Map.Entry<String, CType> entry : bases.entrySet()) {
      String name = entry.getKey();
      CType type = CTypeUtils.simplifyType(entry.getValue());
      targets = addToTargets(name, type, null, 0, 0, targets, fields);
    }
    return targets;
  }

}
