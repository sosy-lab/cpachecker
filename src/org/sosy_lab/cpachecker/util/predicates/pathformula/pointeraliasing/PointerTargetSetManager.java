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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.PersistentSortedMaps.merge;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet.CompositeField;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.RealPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * A manager for pointer target sets.
 */
class PointerTargetSetManager {

  private static final String UNITED_BASE_UNION_TAG_PREFIX = "__VERIFIER_base_union_of_";
  private static final String UNITED_BASE_FIELD_NAME_PREFIX = "__VERIFIER_united_base_field";

  private static final String FAKE_ALLOC_FUNCTION_NAME = "__VERIFIER_fake_alloc";

  /**
   * Returns a fake base type of a given size, i.e. an array of {@code size} voids.
   *
   * @param size The size of the fake base type.
   * @return An array of {@code size} voids.
   */
  static CType getFakeBaseType(int size) {
    return checkIsSimplified(
        new CArrayType(
            false,
            false,
            CVoidType.VOID,
            new CIntegerLiteralExpression(
                FileLocation.DUMMY, CNumericTypes.SIGNED_CHAR, BigInteger.valueOf(size))));
  }

  /**
   * Returns whether a {@code CType} is a fake base type or not.
   * <p/>
   * A fake base type is an array of void.
   *
   * @param type The type to be checked.
   * @return Whether the type is a fake base type or not.
   */
  static boolean isFakeBaseType(final CType type) {
    return type instanceof CArrayType && ((CArrayType) type).getType() instanceof CVoidType;
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
  private final @Nullable ArrayFormulaManagerView afmgr;
  private final FunctionFormulaManagerView ffmgr;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final MemoryRegionManager regionMgr;

  /**
   * Creates a new PointerTargetSetManager.
   *
   * @param pOptions Additional configuration options.
   * @param pFormulaManager The manager for SMT formulae.
   * @param pTypeHandler A type handler for certain types.
   * @param pShutdownNotifier A notifier for external shutdowns to stop long-running algorithms.
   */
  PointerTargetSetManager(
      FormulaEncodingWithPointerAliasingOptions pOptions,
      FormulaManagerView pFormulaManager,
      TypeHandlerWithPointerAliasing pTypeHandler,
      ShutdownNotifier pShutdownNotifier,
      MemoryRegionManager pRegionMgr) {
    options = pOptions;
    formulaManager = pFormulaManager;
    bfmgr = formulaManager.getBooleanFormulaManager();
    afmgr = options.useArraysForHeap() ? formulaManager.getArrayFormulaManager() : null;
    ffmgr = formulaManager.getFunctionFormulaManager();
    typeHandler = pTypeHandler;
    shutdownNotifier = pShutdownNotifier;
    regionMgr = pRegionMgr;
  }

  /**
   * Make a formula that represents a pointer access.
   * @param targetName The name of the pointer access symbol as returned by {@link MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param targetType The formula type of the value
   * @param ssaIndex The SSA index for targetName
   * @param address The address to access
   * @return A formula representing {@code targetName@ssaIndex[address]}
   */
  <I extends Formula, V extends Formula> V makePointerDereference(
      final String targetName,
      final FormulaType<V> targetType,
      final int ssaIndex,
      final I address) {
    final FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(typeHandler.getPointerType().equals(addressType));
    if (options.useArraysForHeap()) {
      final ArrayFormula<I, V> arrayFormula =
          afmgr.makeArray(targetName, ssaIndex, addressType, targetType);
      return afmgr.select(arrayFormula, address);
    } else {
      return ffmgr.declareAndCallUninterpretedFunction(targetName, ssaIndex, targetType, address);
    }
  }

  /**
   * Make a formula that represents a pointer access.
   * @param targetName The name of the pointer access symbol as returned by {@link MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param targetType The formula type of the value
   * @param address The address to access
   * @return A formula representing {@code targetName[address]}
   */
  <I extends Formula, E extends Formula> E makePointerDereference(
      final String targetName,
      final FormulaType<E> targetType,
      final I address) {
    final FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(typeHandler.getPointerType().equals(addressType));
    if (options.useArraysForHeap()) {
      final ArrayFormula<I, E> arrayFormula = afmgr.makeArray(targetName, addressType, targetType);
      return afmgr.select(arrayFormula, address);
    } else {
      return ffmgr.declareAndCallUF(targetName, targetType, address);
    }
  }

  /**
   * Create a formula that represents an assignment to a value via a pointer.
   * @param targetName The name of the pointer access symbol as returned by {@link MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param pTargetType The formula type of the value
   * @param oldIndex The old SSA index for targetName
   * @param newIndex The new SSA index for targetName
   * @param address The address where the value should be written
   * @param value The value to write
   * @return A formula representing an assignment of the form {@code targetName@newIndex[address] = value}
   */
  <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      final String targetName,
      final FormulaType<?> pTargetType,
      final int oldIndex,
      final int newIndex,
      final I address,
      final E value) {
    FormulaType<E> targetType = formulaManager.getFormulaType(value);
    checkArgument(pTargetType.equals(targetType));
    FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(typeHandler.getPointerType().equals(addressType));
    if (options.useArraysForHeap()) {
      final ArrayFormula<I, E> oldFormula =
          afmgr.makeArray(
              targetName,
              oldIndex,
              addressType,
              targetType);
      final ArrayFormula<I, E> arrayFormula =
          afmgr.makeArray(
              targetName,
              newIndex,
              addressType,
              targetType);
      return formulaManager.makeEqual(arrayFormula, afmgr.store(oldFormula, address, value));
    } else {
      final Formula lhs =
          ffmgr.declareAndCallUninterpretedFunction(targetName, newIndex, targetType, address);
      return formulaManager.assignment(lhs, value);
    }
  }

  /**
   * Merges two {@link PointerTargetSet}s into one.
   *
   * @param pts1 The first {@code PointerTargetSet}.
   * @param pts2 The second {@code PointerTargetSet}.
   * @param resultSSA The map of SSA indices.
   * @param conv The converter for C code to SMT formulae.
   * @return The merged {@code PointerTargetSet}s.
   * @throws InterruptedException If the algorithms gets interrupted by an external shutdown.
   */
  MergeResult<PointerTargetSet> mergePointerTargetSets(
      final PointerTargetSet pts1,
      final PointerTargetSet pts2,
      final SSAMapBuilder resultSSA,
      final CtoFormulaConverter conv)
      throws InterruptedException {

    if (pts1.isEmpty() && pts2.isEmpty()) {
      return MergeResult.trivial(PointerTargetSet.emptyPointerTargetSet(), bfmgr);
    }

    final CopyOnWriteSortedMap<String, CType> basesOnlyPts1 =
        CopyOnWriteSortedMap.copyOf(PathCopyingPersistentTreeMap.<String, CType>of());
    final CopyOnWriteSortedMap<String, CType> basesOnlyPts2 =
        CopyOnWriteSortedMap.copyOf(PathCopyingPersistentTreeMap.<String, CType>of());

    PersistentSortedMap<String, CType> mergedBases =
        merge(
            pts1.getBases(),
            pts2.getBases(),
            Equivalence.equals(),
            BaseUnitingConflictHandler.INSTANCE,
            new MapsDifference.DefaultVisitor<String, CType>() {
              @Override
              public void leftValueOnly(String pKey, CType pLeftValue) {
                basesOnlyPts1.put(pKey, pLeftValue);
              }

              @Override
              public void rightValueOnly(String pKey, CType pRightValue) {
                basesOnlyPts2.put(pKey, pRightValue);
              }

              @Override
              public void differingValues(String pKey, CType pLeftValue, CType pRightValue) {
                if (isFakeBaseType(pLeftValue) && !(pRightValue instanceof CElaboratedType)) {
                  basesOnlyPts2.put(pKey, pRightValue);
                } else if (isFakeBaseType(pRightValue) && !(pLeftValue instanceof CElaboratedType)) {
                  basesOnlyPts1.put(pKey, pLeftValue);
                }
              }
            });
    shutdownNotifier.shutdownIfNecessary();


    final CopyOnWriteSortedMap<CompositeField, Boolean> fieldsOnlyPts1 =
        CopyOnWriteSortedMap.copyOf(PathCopyingPersistentTreeMap.<CompositeField, Boolean>of());
    final CopyOnWriteSortedMap<CompositeField, Boolean> fieldsOnlyPts2 =
        CopyOnWriteSortedMap.copyOf(PathCopyingPersistentTreeMap.<CompositeField, Boolean>of());

    PersistentSortedMap<CompositeField, Boolean> mergedFields =
        merge(
            pts1.getFields(),
            pts2.getFields(),
            Equivalence.equals(),
            PersistentSortedMaps.getExceptionMergeConflictHandler(),
            new MapsDifference.DefaultVisitor<CompositeField, Boolean>() {
              @Override
              public void leftValueOnly(CompositeField pKey, Boolean pLeftValue) {
                fieldsOnlyPts1.put(pKey, pLeftValue);
              }

              @Override
              public void rightValueOnly(CompositeField pKey, Boolean pRightValue) {
                fieldsOnlyPts2.put(pKey, pRightValue);
              }
            });
    shutdownNotifier.shutdownIfNecessary();

    PersistentSortedMap<String, PersistentList<PointerTarget>> mergedTargets =
      merge(pts1.getTargets(), pts2.getTargets(), mergeOnConflict());
    shutdownNotifier.shutdownIfNecessary();

    // Targets is always the cross product of bases and fields.
    // So when we merge the bases, fields, and targets by taking the union,
    // there are missing targets:
    // (b1+b2) x (f1+f2) != (t1+t2) == ((b1 x f1) + (b2 x f2))
    // The following targets are missing:
    // (b1 x f2) and (b2 x f1)
    // So we add exactly these targets:

    mergedTargets =
        addAllTargets(mergedTargets, basesOnlyPts2.getSnapshot(), fieldsOnlyPts1.getSnapshot());
    mergedTargets =
        addAllTargets(mergedTargets, basesOnlyPts1.getSnapshot(), fieldsOnlyPts2.getSnapshot());

    final PersistentList<Pair<String, DeferredAllocation>> mergedDeferredAllocations =
        mergeLists(pts1.getDeferredAllocations(), pts2.getDeferredAllocations());
    shutdownNotifier.shutdownIfNecessary();

    final String lastBase;
    final BooleanFormula basesMergeFormula;
    if (pts1.getLastBase() == null ||
        pts2.getLastBase() == null ||
        pts1.getLastBase().equals(pts2.getLastBase())) {
      // Trivial case: either no allocations on one branch at all, or no difference.
      // Just take the first non-null value, the second is either equal or null.
      lastBase = (pts1.getLastBase() != null) ? pts1.getLastBase() : pts2.getLastBase();
      basesMergeFormula = bfmgr.makeTrue();

    } else if (basesOnlyPts1.isEmpty()) {
      assert pts2.getBases().keySet().containsAll(pts1.getBases().keySet());
      // One branch has a strict superset of the allocations of the other.
      lastBase = pts2.getLastBase();
      basesMergeFormula = bfmgr.makeTrue();

    } else if (basesOnlyPts2.isEmpty()) {
      assert pts1.getBases().keySet().containsAll(pts2.getBases().keySet());
      // One branch has a strict superset of the allocations of the other.
      lastBase = pts1.getLastBase();
      basesMergeFormula = bfmgr.makeTrue();

    } else {
      // Otherwise we have no possibility to determine which base to use as lastBase,
      // so we create an additional fake one.
      final CType fakeBaseType = getFakeBaseType(0);
      final String fakeBaseName = DynamicMemoryHandler.makeAllocVariableName(
          FAKE_ALLOC_FUNCTION_NAME, fakeBaseType, resultSSA, conv);
      mergedBases = mergedBases.putAndCopy(fakeBaseName, fakeBaseType);
      lastBase = fakeBaseName;
      basesMergeFormula = formulaManager.makeAnd(getNextBaseAddressInequality(fakeBaseName, pts1.getBases(), pts1.getLastBase()),
                                                 getNextBaseAddressInequality(fakeBaseName, pts2.getBases(), pts2.getLastBase()));
    }

    PointerTargetSet resultPTS =
        new PointerTargetSet(
            mergedBases, lastBase, mergedFields, mergedDeferredAllocations, mergedTargets);

    final List<Pair<CCompositeType, String>> sharedFields = new ArrayList<>();
    final BooleanFormula mergeFormula2 =
        makeValueImportConstraints(basesOnlyPts1.getSnapshot(), sharedFields, resultSSA);
    final BooleanFormula mergeFormula1 =
        makeValueImportConstraints(basesOnlyPts2.getSnapshot(), sharedFields, resultSSA);

    if (!sharedFields.isEmpty()) {
      final PointerTargetSetBuilder resultPTSBuilder =
          new RealPointerTargetSetBuilder(resultPTS, formulaManager, typeHandler, this, options, regionMgr);
      for (final Pair<CCompositeType, String> sharedField : sharedFields) {
        resultPTSBuilder.addField(sharedField.getFirst(), sharedField.getSecond());
      }
      resultPTS = resultPTSBuilder.build();
    }

    return new MergeResult<>(resultPTS, mergeFormula1, mergeFormula2, basesMergeFormula);
  }

  /**
   * A handler for merge conflicts that appear when merging bases.
   */
  private static enum BaseUnitingConflictHandler implements MergeConflictHandler<String, CType> {
    INSTANCE;

    /**
     * Resolves a merge conflict between two types and returns the resolved type
     *
     * @param key   Not used in the algorithm.
     * @param type1 The first type to merge.
     * @param type2 The second type to merge.
     * @return A conflict resolving C type.
     */
    @Override
    public CType resolveConflict(final String key, final CType type1, final CType type2) {
      if (isFakeBaseType(type1)) {
        return type2;
      } else if (isFakeBaseType(type2)) {
        return type1;
      }
      int currentFieldIndex = 0;
      final ImmutableList.Builder<CCompositeTypeMemberDeclaration> membersBuilder =
        ImmutableList.<CCompositeTypeMemberDeclaration>builder();
      if (type1 instanceof CCompositeType) {
        final CCompositeType compositeType1 = (CCompositeType) type1;
        if (compositeType1.getKind() == ComplexTypeKind.UNION &&
            !compositeType1.getMembers().isEmpty() &&
            compositeType1.getMembers().get(0).getName().equals(getUnitedFieldBaseName(0))) {
          membersBuilder.addAll(compositeType1.getMembers());
          currentFieldIndex += compositeType1.getMembers().size();
        } else {
          membersBuilder.add(new CCompositeTypeMemberDeclaration(compositeType1,
                                                                 getUnitedFieldBaseName(currentFieldIndex)));
          currentFieldIndex++;
        }
      } else {
        membersBuilder.add(new CCompositeTypeMemberDeclaration(type1,
                                                               getUnitedFieldBaseName(currentFieldIndex)));
        currentFieldIndex++;
      }
      if (type2 instanceof CCompositeType) {
        final CCompositeType compositeType2 = (CCompositeType) type2;
        if (compositeType2.getKind() == ComplexTypeKind.UNION &&
            !compositeType2.getMembers().isEmpty() &&
            compositeType2.getMembers().get(0).getName().equals(getUnitedFieldBaseName(0))) {
          for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType2.getMembers()) {
            membersBuilder.add(new CCompositeTypeMemberDeclaration(memberDeclaration.getType(),
                                                                   getUnitedFieldBaseName(currentFieldIndex)));
            currentFieldIndex++;
          }
        } else {
          membersBuilder.add(new CCompositeTypeMemberDeclaration(compositeType2,
                                                                 getUnitedFieldBaseName(currentFieldIndex)));
        }
      } else {
        membersBuilder.add(new CCompositeTypeMemberDeclaration(type2,
                                                               getUnitedFieldBaseName(currentFieldIndex)));
      }


      String varName = UNITED_BASE_UNION_TAG_PREFIX
                       + type1.toString().replace(' ', '_')
                       + "_and_"
                       + type2.toString().replace(' ', '_');
      return new CCompositeType(false,
                                false,
                                ComplexTypeKind.UNION,
                                membersBuilder.build(),
                                varName,
                                varName);
    }
  }

  static <T> PersistentList<T> mergeLists(
      final PersistentList<T> list1, final PersistentList<T> list2) {
    if (list1 == list2) {
      return list1;
    }
    final int size1 = list1.size();
    final int size2 = list2.size();
    if (size1 == size2 && list1.equals(list2)) {
      return list1;
    }

    PersistentList<T> smallerList, biggerList;
    if (size1 > size2) {
      smallerList = list2;
      biggerList = list1;
    } else {
      smallerList = list1;
      biggerList = list2;
    }

    final Set<T> fromBigger = new HashSet<>(biggerList);
    PersistentList<T> result = biggerList;

    for (final T target : from(smallerList).filter(not(in(fromBigger)))) {
      result = result.with(target);
    }
    return result;
  }

  /**
   * Gives a handler for merge conflicts.
   *
   * @param <K> The type of the keys in the merge conflict handler.
   * @param <T> The type of the list entries in the merge conflict handler.
   * @return A handler for merge conflicts.
   */
  private static <K, T> MergeConflictHandler<K, PersistentList<T>> mergeOnConflict() {
    return (key, list1, list2) -> mergeLists(list1, list2);
  }

  /**
   * Create constraint that imports the old value of a variable into the memory handled with UFs.
   *
   * @param newBases A map of new bases.
   * @param sharedFields A list of shared fields.
   * @param ssa The SSA map.
   * @return A boolean formula for the import constraint.
   */
  private BooleanFormula makeValueImportConstraints(final PersistentSortedMap<String, CType> newBases,
      final List<Pair<CCompositeType, String>> sharedFields, final SSAMapBuilder ssa) {
    BooleanFormula mergeFormula = bfmgr.makeTrue();
    for (final Map.Entry<String, CType> base : newBases.entrySet()) {
      if (!options.isDynamicAllocVariableName(base.getKey())
          && !CTypeUtils.containsArrayOutsideFunctionParameter(base.getValue())) {
        final FormulaType<?> baseFormulaType = typeHandler.getFormulaTypeFromCType(
                                                   CTypeUtils.getBaseType(base.getValue()));
        mergeFormula = bfmgr.and(mergeFormula, makeValueImportConstraints(formulaManager.makeVariable(baseFormulaType,
                                                                                          PointerTargetSet.getBaseName(
                                                                                              base.getKey())),
                                                                        base.getKey(),
                                                                        base.getValue(),
                                                                        sharedFields,
                                                                        ssa, null));
      }
    }
    return mergeFormula;
  }

  /**
   * Create constraint that imports the old value of a variable into the memory handled with UFs.
   *
   * @param address        The formula for the address.
   * @param variablePrefix A prefix for variables.
   * @param variableType   The type of the variable.
   * @param sharedFields   A list of shared fields.
   * @param ssa  The SSA map.
   * @return A boolean formula for the import constraint.
   */
  private <I extends Formula >BooleanFormula makeValueImportConstraints(
      final I address,
      final String variablePrefix,
      final CType variableType,
      final List<Pair<CCompositeType, String>> sharedFields,
      final SSAMapBuilder ssa,
      final MemoryRegion region) {

    assert !CTypeUtils.containsArrayOutsideFunctionParameter(variableType)
        : "Array access can't be encoded as a variable";

    BooleanFormula result = bfmgr.makeTrue();

    if (variableType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) variableType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final int offset = typeHandler.getBitOffset(compositeType, memberName);
        final CType memberType = typeHandler.getSimplifiedType(memberDeclaration);
        final String newPrefix = variablePrefix + CToFormulaConverterWithPointerAliasing.FIELD_NAME_SEPARATOR + memberName;
        if (ssa.getIndex(newPrefix) > 0) {
          MemoryRegion newRegion = regionMgr.makeMemoryRegion(compositeType, memberType, memberName);
          sharedFields.add(Pair.of(compositeType, memberName));
          result = bfmgr.and(
              result,
              makeValueImportConstraints(
                  formulaManager.makePlus(
                      address,
                      formulaManager.makeNumber(
                          typeHandler.getPointerType(),
                          offset)
                  ),
                  newPrefix,
                  memberType,
                  sharedFields,
                  ssa,
                  newRegion
              )
          );
        }
      }
    } else {
      if (ssa.getIndex(variablePrefix) > 0) {
        MemoryRegion newRegion = region;
        if(newRegion == null) {
          newRegion = regionMgr.makeMemoryRegion(variableType);
        }
        final FormulaType<?> variableFormulaType = typeHandler.getFormulaTypeFromCType(variableType);
        result = bfmgr.and(result, formulaManager.makeEqual(makeDereference(variableType, address, ssa, newRegion),
                                                  formulaManager.makeVariable(variableFormulaType,
                                                                    variablePrefix,
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
  private <I extends Formula> Formula makeDereference(
      final CType type, final I address, final SSAMapBuilder ssa, MemoryRegion region) {
    final String ufName = regionMgr.getPointerAccessName(region);
    final int index = ssa.getIndex(ufName);
    final FormulaType<?> returnType = typeHandler.getFormulaTypeFromCType(type);
    return makePointerDereference(ufName, returnType, index, address);
  }

  /**
   * Gets the next base address.
   *
   * @param newBase The name of the next base.
   * @param bases A map of existing bases.
   * @param lastBase The name of the last added base.
   * @return A formula for the next base address.
   */
  BooleanFormula getNextBaseAddressInequality(
      final String newBase, final PersistentSortedMap<String, CType> bases, final String lastBase) {
    final FormulaType<?> pointerType = typeHandler.getPointerType();
    final Formula newBaseFormula = formulaManager.makeVariable(pointerType, PointerTargetSet.getBaseName(newBase));
    if (lastBase != null) {
      final CType lastType = bases.get(lastBase);
      final int lastSize =
          lastType.isIncomplete()
              ? options.defaultAllocationSize()
              : typeHandler.getSizeof(lastType);
      final Formula rhs = formulaManager.makePlus(formulaManager.makeVariable(pointerType, PointerTargetSet.getBaseName(lastBase)),
                                                  formulaManager.makeNumber(pointerType, lastSize * typeHandler.getBitsPerByte()));
      // The condition rhs > 0 prevents overflows in case of bit-vector encoding
      return formulaManager.makeAnd(formulaManager.makeGreaterThan(rhs, formulaManager.makeNumber(pointerType, 0L), true),
                                    formulaManager.makeGreaterOrEqual(newBaseFormula, rhs, true));
    } else {
      return formulaManager.makeGreaterThan(newBaseFormula, formulaManager.makeNumber(pointerType, 0L), true);
    }
  }

  /**
   * Adds pointer targets for every used (tracked) (sub)field of the newly allocated base.
   *
   * @param base The name of the base.
   * @param region The region of the target.
   * @param containerType The type of the container, might be {@code null}.
   * @param properOffset The offset.
   * @param containerOffset The offset in the container.
   * @param targets The map of available targets.
   * @param regionMgr The region manager.
   * @return The new map of targets.
   */
  @CheckReturnValue
  private static PersistentSortedMap<String, PersistentList<PointerTarget>> addToTarget(final String base,
                         final MemoryRegion region,
                         final @Nullable CType containerType,
                         final int properOffset,
                         final int containerOffset,
                         final PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
                         MemoryRegionManager regionMgr) {
    String regionName = regionMgr.getPointerAccessName(region);
    PersistentList<PointerTarget> targetsForRegion =
        targets.getOrDefault(regionName, PersistentLinkedList.of());
    return targets.putAndCopy(regionName, targetsForRegion.with(new PointerTarget(base,
                                                                             containerType,
                                                                             properOffset,
                                                                             containerOffset)));
  }

  /**
   * Recursively adds pointer targets for every used (tracked) (sub)field of the newly allocated base.
   *
   * Note: the recursion doesn't proceed on unused (untracked) (sub)fields.
   *
   * @param base the name of the newly allocated base variable
   * @param cType type of the allocated base or the next added pointer target
   * @param containerType either {@code null} or the type of the innermost container of the next added pointer target
   * @param properOffset either {@code 0} or the offset of the next added pointer target in its innermost container
   * @param containerOffset either {@code 0} or the offset of the innermost container (relative to the base adddress)
   * @param targets The list of targets where the new targets should be added to.
   * @param fields The set of "shared" fields that are accessed directly via pointers.
   * @return The targets map together with all the added targets.
   */
  @CheckReturnValue
  PersistentSortedMap<String, PersistentList<PointerTarget>> addToTargets(
      final String base,
      final @Nullable MemoryRegion region,
      final CType cType,
      final @Nullable CType containerType,
      final int properOffset,
      final int containerOffset,
      PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
      final PersistentSortedMap<CompositeField, Boolean> fields) {
    checkIsSimplified(cType);
    /* Remove assertion: it fails on a correct code (gcc compiles it)
     * struct A;
     * ...
     * struct A *var;
     * var = kmalloc(16);
     */
    //assert !(cType instanceof CElaboratedType) : "Unresolved elaborated type " + cType  + " for base " + base;
    if (cType instanceof CArrayType) {
      final CArrayType arrayType = (CArrayType) cType;
      final int length = CTypeUtils.getArrayLength(arrayType, options);
      int offset = 0;
      for (int i = 0; i < length; ++i) {
        //TODO: create region with arrayType.getType()
        targets = addToTargets(base, null, arrayType.getType(), arrayType, offset, containerOffset + properOffset, targets, fields);
        offset += typeHandler.getBitSizeof(arrayType.getType());
      }
    } else if (cType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) cType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      final String type = CTypeUtils.typeToString(compositeType);
      typeHandler.addCompositeTypeToCache(compositeType);
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final int offset = typeHandler.getBitOffset(compositeType, memberDeclaration.getName());
        if (fields.containsKey(CompositeField.of(type, memberDeclaration.getName()))) {
          MemoryRegion newRegion = regionMgr.makeMemoryRegion(compositeType, memberDeclaration.getType(), memberDeclaration.getName());
          targets = addToTargets(base, newRegion, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset, targets, fields);
        }
      }
    } else {
      MemoryRegion newRegion = region;
      if(newRegion == null) {
        newRegion = regionMgr.makeMemoryRegion(cType);
      }
      targets = addToTarget(base, newRegion, containerType, properOffset, containerOffset, targets, regionMgr);
    }

    return targets;
  }

  /**
   * Compute all targets for a given set of bases and fields,
   * and add them to a map.
   *
   * @param targets A map of existing targets
   * @param bases A set of bases
   * @param fields A set of fields
   * @return A map of existing targets
   */
  @CheckReturnValue
  private PersistentSortedMap<String, PersistentList<PointerTarget>> addAllTargets(
      PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
      final PersistentSortedMap<String, CType> bases,
      final PersistentSortedMap<CompositeField, Boolean> fields) {
    for (final Map.Entry<String, CType> entry : bases.entrySet()) {
      String name = entry.getKey();
      CType type = checkIsSimplified(entry.getValue());
      targets = addToTargets(name, null, type, null, 0, 0, targets, fields);
    }
    return targets;
  }
}
