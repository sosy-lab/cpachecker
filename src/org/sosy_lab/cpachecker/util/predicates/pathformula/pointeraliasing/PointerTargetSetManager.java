// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.PersistentSortedMaps.merge;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils.checkIsSimplified;

import com.google.common.base.Equivalence;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CheckReturnValue;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.RealPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/** A manager for pointer target sets. */
class PointerTargetSetManager {

  private static final String UNITED_BASE_UNION_TAG_PREFIX = "__VERIFIER_base_union_of_";
  private static final String UNITED_BASE_FIELD_NAME_PREFIX = "__VERIFIER_united_base_field";

  /**
   * Returns a fake base type of a given size, i.e. an array of {@code size} voids.
   *
   * @param size The size of the fake base type.
   * @return An array of {@code size} voids.
   */
  static CType getFakeBaseType(long size) {
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
   *
   * <p>A fake base type is an array of void.
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

  private final CToFormulaConverterWithPointerAliasing conv;

  private final FormulaEncodingWithPointerAliasingOptions options;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView bfmgr;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final MemoryRegionManager regionMgr;
  private final SMTHeap heap;
  /**
   * Creates a new PointerTargetSetManager.
   *
   * @param pOptions Additional configuration options.
   * @param pFormulaManager The manager for SMT formulae.
   * @param pTypeHandler A type handler for certain types.
   * @param pShutdownNotifier A notifier for external shutdowns to stop long-running algorithms.
   */
  PointerTargetSetManager(
      CToFormulaConverterWithPointerAliasing pConv,
      FormulaEncodingWithPointerAliasingOptions pOptions,
      FormulaManagerView pFormulaManager,
      TypeHandlerWithPointerAliasing pTypeHandler,
      ShutdownNotifier pShutdownNotifier,
      MemoryRegionManager pRegionMgr) {
    conv = pConv;
    options = pOptions;
    formulaManager = pFormulaManager;
    bfmgr = formulaManager.getBooleanFormulaManager();
    typeHandler = pTypeHandler;
    shutdownNotifier = pShutdownNotifier;
    regionMgr = pRegionMgr;

    if (pOptions.useByteArrayForHeap()) {
      heap =
          new SMTHeapWithByteArray(
              pFormulaManager, pTypeHandler.getPointerType(), pConv.machineModel);
    } else if (pOptions.useArraysForHeap()) {
      heap = new SMTHeapWithArrays(pFormulaManager, pTypeHandler.getPointerType());
    } else {
      heap = new SMTHeapWithUninterpretedFunctionCalls(pFormulaManager);
    }
  }

  /**
   * Make a formula that represents a pointer access.
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
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
    return heap.makePointerDereference(targetName, targetType, ssaIndex, address);
  }

  /**
   * Make a formula that represents a pointer access.
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param targetType The formula type of the value
   * @param address The address to access
   * @return A formula representing {@code targetName[address]}
   */
  <I extends Formula, E extends Formula> E makePointerDereference(
      final String targetName, final FormulaType<E> targetType, final I address) {
    return heap.makePointerDereference(targetName, targetType, address);
  }

  /**
   * Create a formula that represents an assignment to a value via a pointer.
   *
   * @param targetName The name of the pointer access symbol as returned by {@link
   *     MemoryRegionManager#getPointerAccessName(MemoryRegion)}
   * @param pTargetType The formula type of the value
   * @param oldIndex The old SSA index for targetName
   * @param newIndex The new SSA index for targetName
   * @param address The address where the value should be written
   * @param value The value to write
   * @return A formula representing an assignment of the form {@code targetName@newIndex[address] =
   *     value}
   */
  <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      final String targetName,
      final FormulaType<?> pTargetType,
      final int oldIndex,
      final int newIndex,
      final I address,
      final E value) {
    return heap.makePointerAssignment(targetName, pTargetType, oldIndex, newIndex, address, value);
  }

  /**
   * Merges two {@link PointerTargetSet}s into one.
   *
   * <p>This can modify the given SSAMap in one case: if one of the PointerTargetSets contains a
   * base for a variable, and the other does not, there will of course be a base for this variable
   * in the result, and this variable will be deleted from the SSAMap.
   *
   * @param pts1 The first {@code PointerTargetSet}.
   * @param pts2 The second {@code PointerTargetSet}.
   * @param ssa The map of SSA indices.
   * @return The merged {@code PointerTargetSet}s.
   * @throws InterruptedException If the algorithms gets interrupted by an external shutdown.
   */
  MergeResult<PointerTargetSet> mergePointerTargetSets(
      final PointerTargetSet pts1, final PointerTargetSet pts2, final SSAMapBuilder ssa)
      throws InterruptedException {

    if (pts1.isEmpty() && pts2.isEmpty()) {
      return MergeResult.trivial(PointerTargetSet.emptyPointerTargetSet(), bfmgr);
    }

    final Constraints mergeConstraints1 = new Constraints(bfmgr);
    final Constraints mergeConstraints2 = new Constraints(bfmgr);
    PersistentList<Formula> highestAllocatedAddresses1 = pts1.getHighestAllocatedAddresses();
    PersistentList<Formula> highestAllocatedAddresses2 = pts2.getHighestAllocatedAddresses();

    // Handle bases

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
                } else if (isFakeBaseType(pRightValue)
                    && !(pLeftValue instanceof CElaboratedType)) {
                  basesOnlyPts1.put(pKey, pLeftValue);
                }
              }
            });
    shutdownNotifier.shutdownIfNecessary();

    // For all bases only in pts1, add required constraints regarding pts2 and vice-versa.
    // basesOnlyPtsX may contain bases for with ptsY has a fake base, but for these ptsY also had
    // constraints created already, and duplicate constraints would contradict. So we filter these.
    highestAllocatedAddresses2 =
        makeBaseAddressConstraintsForMergedBases(
            basesOnlyPts1.getSnapshot(),
            pts2.getBases().keySet(),
            highestAllocatedAddresses2,
            mergeConstraints2);
    highestAllocatedAddresses1 =
        makeBaseAddressConstraintsForMergedBases(
            basesOnlyPts2.getSnapshot(),
            pts1.getBases().keySet(),
            highestAllocatedAddresses1,
            mergeConstraints1);

    // Handle fields

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

    // Handle targets

    PersistentSortedMap<String, PersistentList<PointerTarget>> mergedTargets =
        merge(
            pts1.getTargets(), pts2.getTargets(), (key, list1, list2) -> mergeLists(list1, list2));
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

    // Handle allocation metadata

    final PersistentList<Formula> highestAllocatedAddresses =
        mergeLists(highestAllocatedAddresses1, highestAllocatedAddresses2);

    int allocationCount = Math.max(pts1.getAllocationCount(), pts2.getAllocationCount());

    PointerTargetSet resultPTS =
        new PointerTargetSet(
            mergedBases,
            mergedFields,
            mergedDeferredAllocations,
            mergedTargets,
            highestAllocatedAddresses,
            allocationCount);

    // Handle value-import constraints

    final List<CompositeField> sharedFields = new ArrayList<>();
    makeValueImportConstraints(basesOnlyPts1.getSnapshot(), sharedFields, ssa, mergeConstraints2);
    makeValueImportConstraints(basesOnlyPts2.getSnapshot(), sharedFields, ssa, mergeConstraints1);

    if (!sharedFields.isEmpty()) {
      final PointerTargetSetBuilder resultPTSBuilder =
          new RealPointerTargetSetBuilder(resultPTS, typeHandler, this, options, regionMgr);
      for (final CompositeField sharedField : sharedFields) {
        resultPTSBuilder.addField(sharedField);
      }
      resultPTS = resultPTSBuilder.build();
    }

    return new MergeResult<>(
        resultPTS, mergeConstraints1.get(), mergeConstraints2.get(), bfmgr.makeTrue());
  }

  /** A handler for merge conflicts that appear when merging bases. */
  private enum BaseUnitingConflictHandler implements MergeConflictHandler<String, CType> {
    INSTANCE;

    /**
     * Resolves a merge conflict between two types and returns the resolved type.
     *
     * <p>We build up a new union-type containing all given types, except for fake-types.
     *
     * @param key Not used in the algorithm.
     * @param type1 The first type to merge.
     * @param type2 The second type to merge.
     * @return A conflict resolving C type.
     */
    @Override
    public CType resolveConflict(final String key, final CType type1, final CType type2) {
      if (isFakeBaseType(type1) || type1.isIncomplete()) {
        return type2;
      } else if (isFakeBaseType(type2) || type2.isIncomplete()) {
        return type1;
      }
      int currentFieldIndex = 0;
      final ImmutableList.Builder<CCompositeTypeMemberDeclaration> membersBuilder =
          ImmutableList.builder();
      final Set<CType> seenMembers = new HashSet<>();
      if (isAlreadyMergedCompositeType(type1)) {
        // if already a merged type, just copy the inner types, without creating new base-names
        for (CCompositeTypeMemberDeclaration innerType : ((CCompositeType) type1).getMembers()) {
          membersBuilder.add(innerType);
          seenMembers.add(innerType.getType());
          currentFieldIndex++;
        }
      } else {
        membersBuilder.add(
            new CCompositeTypeMemberDeclaration(type1, getUnitedFieldBaseName(currentFieldIndex)));
        seenMembers.add(type1);
        currentFieldIndex++;
      }
      if (isAlreadyMergedCompositeType(type2)) {
        // if already a merged type, just copy the inner types, if needed
        for (CCompositeTypeMemberDeclaration innerType : ((CCompositeType) type2).getMembers()) {
          if (seenMembers.add(innerType.getType())) {
            membersBuilder.add(
                new CCompositeTypeMemberDeclaration(
                    innerType.getType(), getUnitedFieldBaseName(currentFieldIndex)));
            currentFieldIndex++;
          }
        }
      } else {
        if (seenMembers.add(type2)) {
          membersBuilder.add(
              new CCompositeTypeMemberDeclaration(
                  type2, getUnitedFieldBaseName(currentFieldIndex)));
        }
      }

      ImmutableList<CCompositeTypeMemberDeclaration> members = membersBuilder.build();
      String varName =
          UNITED_BASE_UNION_TAG_PREFIX
              + Joiner.on("_and_")
                  .join(
                      Iterables.transform(members, m -> m.getType().toString().replace(" ", "_")));
      return new CCompositeType(false, false, ComplexTypeKind.UNION, members, varName, varName);
    }

    /**
     * check whether the given type was already build by a previous merge of other types.
     *
     * <p>We check for UNION-type with special fieldnames.
     */
    private static boolean isAlreadyMergedCompositeType(final CType type) {
      if (type instanceof CCompositeType) {
        final CCompositeType compositeType = (CCompositeType) type;
        return compositeType.getKind() == ComplexTypeKind.UNION
            && !compositeType.getMembers().isEmpty()
            && compositeType.getMembers().get(0).getName().equals(getUnitedFieldBaseName(0));
      }
      return false;
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
   * Create base-address constraints specifically for the case where pointer-target sets are merged.
   * At first, one would think that this is not necessary, because on paths where a base was not
   * created, it will also not be used, and thus its address does not matter. However, this is not
   * true for local variables: As discussed in #987, a local variable can reappear later (if we
   * reenter the respective function) but we would never add a constraint again (because the base
   * already exists). If we do not add the constraints on merge, there would be paths where the base
   * is used but no constraint was added.
   *
   * @param pBases The bases for which to create constraints.
   * @param pIgnoredBases Set of bases that will be ignored.
   * @param highestAllocatedAddresses the previously highest allocated addresses as base for the new
   *     base constraints
   * @param pConstraints Where the constraint(s) should be added.
   * @return The now highest allocated addresses to use for future base constraints
   */
  private PersistentList<Formula> makeBaseAddressConstraintsForMergedBases(
      final Map<String, CType> pBases,
      final Set<String> pIgnoredBases,
      PersistentList<Formula> highestAllocatedAddresses,
      final Constraints pConstraints) {

    for (Map.Entry<String, CType> base : pBases.entrySet()) {
      final String baseName = base.getKey();

      // We do not need constraints for bases resulting from dynamic memory allocation,
      // because these bases cannot occur later again.
      if (!pIgnoredBases.contains(baseName)
          && !DynamicMemoryHandler.isAllocVariableName(baseName)) {
        highestAllocatedAddresses =
            makeBaseAddressConstraints(
                baseName, base.getValue(), null, highestAllocatedAddresses, pConstraints);
      }
    }

    return highestAllocatedAddresses;
  }

  /**
   * Create the constraints that are required for a new base:
   *
   * <ul>
   *   <li>for inequality between a new base and existing bases (to prevent overlapping)
   *   <li>for alignment
   * </ul>
   *
   * @param pNewBase The name of the new base.
   * @param pType The type of the new base.
   * @param pAllocationSize An optional expression for the size in bytes of the new base.
   * @param pHighestAllocatedAddresses list of addresses for which this method will ensure that the
   *     new base does not overlap
   * @param pConstraints Where the constraints about addresses will be added to.
   * @return list of addresses as base for allocation of the next base, nothing should overlap with
   *     these
   */
  PersistentList<Formula> makeBaseAddressConstraints(
      final String pNewBase,
      final CType pType,
      final @Nullable Formula pAllocationSize,
      final PersistentList<Formula> pHighestAllocatedAddresses,
      final Constraints pConstraints) {

    if (!options.trackFunctionPointers() && CTypes.isFunctionPointer(pType)) {
      // Avoid adding constraints about function addresses,
      // otherwise we might track facts about function pointers for code like "if (p == &f)".
      return pHighestAllocatedAddresses;
    }

    final FormulaType<?> pointerType = typeHandler.getPointerType();
    final Formula newBaseFormula =
        formulaManager.makeVariableWithoutSSAIndex(
            pointerType, PointerTargetSet.getBaseName(pNewBase));

    // Create constraints for the new base address and store them
    if (pHighestAllocatedAddresses.isEmpty()) {
      pConstraints.addConstraint(makeGreaterZero(newBaseFormula));
    } else {
      for (Formula oldAddress : pHighestAllocatedAddresses) {
        pConstraints.addConstraint(
            formulaManager.makeGreaterThan(newBaseFormula, oldAddress, true));
      }
    }

    // Add alignment constraint
    // For incomplete types, better not add constraints (imprecise) than a wrong one (unsound).
    if (!pType.isIncomplete()) {
      pConstraints.addConstraint(
          formulaManager.makeModularCongruence(
              newBaseFormula,
              formulaManager.makeNumber(pointerType, 0L),
              typeHandler.getAlignof(pType),
              false));
    }

    final long typeSize =
        pType.hasKnownConstantSize()
            ? typeHandler.getSizeof(pType)
            : options.defaultAllocationSize();
    final Formula typeSizeF = formulaManager.makeNumber(pointerType, typeSize);
    final Formula newBasePlusTypeSize = formulaManager.makePlus(newBaseFormula, typeSizeF);

    // Prepare highestAllocatedAddresses which we will use for the constraints of the next base.
    // We have two ways to compute the size: sizeof(type) and the allocationSize (e.g., the
    // argument to malloc).
    // We need both here: Only the allocation size is correct for allocations with dynamic size,
    // and only the size of the type takes into account the default array length that we assume
    // elsewhere and we must thus ensure here, too.
    // Furthermore, in case of linear approximation we need a constraint that the size is
    // positive, and using the type size takes care of this automatically.
    // In cases where the precise size is known and correct, we need only one of the constraints.
    // We use the one with typeSize instead of allocationSize, because the latter would add one
    // multiplication to the formula.
    // We also need to ensure that the highest allocated address (both variants) is greater than
    // zero to prevent overflows with bitvector arithmetic.

    pConstraints.addConstraint(makeGreaterZero(newBasePlusTypeSize));
    PersistentList<Formula> highestAllocatedAddresses =
        PersistentLinkedList.of(newBasePlusTypeSize);

    if (pAllocationSize != null
        && !pAllocationSize.equals(formulaManager.makeNumber(pointerType, typeSize))) {
      Formula basePlusAllocationSize = formulaManager.makePlus(newBaseFormula, pAllocationSize);
      pConstraints.addConstraint(makeGreaterZero(basePlusAllocationSize));

      highestAllocatedAddresses = highestAllocatedAddresses.with(basePlusAllocationSize);
    }

    return highestAllocatedAddresses;
  }

  private BooleanFormula makeGreaterZero(Formula f) {
    return formulaManager.makeGreaterThan(
        f, formulaManager.makeNumber(typeHandler.getPointerType(), 0L), true);
  }

  /**
   * Create constraint that imports the old value of a variable into the memory handled with UFs.
   *
   * @param newBases A map of new bases.
   * @param sharedFields A list of shared fields.
   * @param ssaBuilder The SSA map.
   * @param constraints Where the import constraint(s) should be added.
   */
  private void makeValueImportConstraints(
      final PersistentSortedMap<String, CType> newBases,
      final List<CompositeField> sharedFields,
      final SSAMapBuilder ssaBuilder,
      final Constraints constraints) {
    for (final Map.Entry<String, CType> base : newBases.entrySet()) {
      if (!options.isDynamicAllocVariableName(base.getKey())
          && !CTypeUtils.containsArrayOutsideFunctionParameter(base.getValue())) {
        final Formula baseVar = conv.makeBaseAddress(base.getKey(), base.getValue());
        conv.addValueImportConstraints(
            baseVar, base.getKey(), base.getValue(), sharedFields, ssaBuilder, constraints, null);
      }
    }
  }

  /**
   * Recursively adds pointer targets for every used (tracked) (sub)field of the newly allocated
   * base.
   *
   * <p>Note: the recursion doesn't proceed on unused (untracked) (sub)fields.
   *
   * @param base the name of the newly allocated base variable
   * @param cType type of the allocated base or the next added pointer target
   * @param containerType either {@code null} or the type of the innermost container of the next
   *     added pointer target
   * @param properOffset either {@code 0} or the offset of the next added pointer target in its
   *     innermost container
   * @param containerOffset either {@code 0} or the offset of the innermost container (relative to
   *     the base adddress)
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
      final long properOffset,
      final long containerOffset,
      PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
      final PersistentSortedMap<CompositeField, Boolean> fields) {
    checkIsSimplified(cType);
    /* Remove assertion: it fails on a correct code (gcc compiles it)
     * struct A;
     * ...
     * struct A *var;
     * var = kmalloc(16);
     */
    // assert !(cType instanceof CElaboratedType) : "Unresolved elaborated type " + cType  + " for
    // base " + base;
    if (cType instanceof CArrayType) {
      final CArrayType arrayType = (CArrayType) cType;
      final int length = CTypeUtils.getArrayLength(arrayType, options);
      long offset = 0;
      for (int i = 0; i < length; ++i) {
        // TODO: create region with arrayType.getType()
        targets =
            addToTargets(
                base,
                null,
                arrayType.getType(),
                arrayType,
                offset,
                containerOffset + properOffset,
                targets,
                fields);
        offset += typeHandler.getSizeof(arrayType.getType());
      }
    } else if (cType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) cType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM
          : "Enums are not composite: " + compositeType;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final OptionalLong offset = typeHandler.getOffset(compositeType, memberDeclaration);
        if (!offset.isPresent()) {
          continue; // TODO this looses values of bit fields
        }
        if (fields.containsKey(CompositeField.of(compositeType, memberDeclaration))) {
          MemoryRegion newRegion = regionMgr.makeMemoryRegion(compositeType, memberDeclaration);
          targets =
              addToTargets(
                  base,
                  newRegion,
                  memberDeclaration.getType(),
                  compositeType,
                  offset.orElseThrow(),
                  containerOffset + properOffset,
                  targets,
                  fields);
        }
      }
    } else {
      MemoryRegion newRegion = region;
      if (newRegion == null) {
        newRegion = regionMgr.makeMemoryRegion(cType);
      }
      String regionName = regionMgr.getPointerAccessName(newRegion);
      PersistentList<PointerTarget> targetsForRegion =
          targets.getOrDefault(regionName, PersistentLinkedList.of());
      targets =
          targets.putAndCopy(
              regionName,
              targetsForRegion.with(
                  new PointerTarget(base, containerType, properOffset, containerOffset)));
    }

    return targets;
  }

  /**
   * Compute all targets for a given set of bases and fields, and add them to a map.
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
