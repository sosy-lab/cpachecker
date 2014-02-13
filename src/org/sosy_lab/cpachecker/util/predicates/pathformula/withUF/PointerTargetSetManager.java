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

import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.VOID;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.MapMerger.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.MapMerger;
import org.sosy_lab.cpachecker.util.predicates.pathformula.MapMerger.ConflictHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.CompositeField;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTarget;

import com.google.common.collect.ImmutableList;


public class PointerTargetSetManager {

  private static final String UNITED_BASE_UNION_TAG_PREFIX = "__VERIFIER_base_union_of_";
  private static final String UNITED_BASE_FIELD_NAME_PREFIX = "__VERIFIER_united_base_field";

  private static final String FAKE_ALLOC_FUNCTION_NAME = "__VERIFIER_fake_alloc";

  static final CType getFakeBaseType(int size) {
    return CTypeUtils.simplifyType(new CArrayType(false, false, CNumericTypes.VOID, new CIntegerLiteralExpression(null,
                                                                                        CNumericTypes.SIGNED_CHAR,
                                                                                        BigInteger.valueOf(size))));
  }

  static final boolean isFakeBaseType(final CType type) {
    return type instanceof CArrayType && ((CArrayType) type).getType().equals(VOID);
  }

  private static final String getUnitedFieldBaseName(final int index) {
    return UNITED_BASE_FIELD_NAME_PREFIX + index;
  }


  private final FormulaEncodingWithUFOptions options;
  private final MachineModel machineModel;
  private final FormulaManagerView formulaManager;

  private final CSizeofVisitor sizeofVisitor;

  public PointerTargetSetManager(FormulaEncodingWithUFOptions pOptions, MachineModel pMachineModel,
      FormulaManagerView pFormulaManager) {
    options = pOptions;
    machineModel = pMachineModel;
    formulaManager = pFormulaManager;

    sizeofVisitor = new CSizeofVisitor(pMachineModel, pOptions);
  }

  public Triple<PointerTargetSet,
                BooleanFormula,
                Pair<PersistentSortedMap<String, CType>, PersistentSortedMap<String, CType>>>
    merge(final PointerTargetSet pts1, final PointerTargetSet pts2) {

    final boolean reverseBases = pts2.bases.size() > pts1.bases.size();
    Triple<PersistentSortedMap<String, CType>,
           PersistentSortedMap<String, CType>,
           PersistentSortedMap<String, CType>> mergedBases =
      !reverseBases ? mergeSortedSets(pts1.bases, pts2.bases, BaseUnitingConflictHandler.INSTANCE) :
                      mergeSortedSets(pts2.bases, pts1.bases, BaseUnitingConflictHandler.INSTANCE);

    final boolean reverseFields = pts2.fields.size() > pts1.fields.size();
    final Triple<PersistentSortedMap<CompositeField, Boolean>,
                 PersistentSortedMap<CompositeField, Boolean>,
                 PersistentSortedMap<CompositeField, Boolean>> mergedFields =
      !reverseFields ? mergeSortedSets(pts1.fields, pts2.fields, MapMerger.<CompositeField, Boolean>getExceptionOnConflictHandler()) :
                      mergeSortedSets(pts2.fields, pts1.fields, MapMerger.<CompositeField, Boolean>getExceptionOnConflictHandler());

    final boolean reverseTargets = pts2.targets.size() > pts1.targets.size();
    final PersistentSortedMap<String, PersistentList<PointerTarget>> mergedTargets =
      !reverseTargets ? mergeSortedMaps(pts1.targets, pts2.targets,PointerTargetSetManager.<String, PointerTarget>mergeOnConflict()) :
                        mergeSortedMaps(pts2.targets, pts1.targets, PointerTargetSetManager.<String, PointerTarget>mergeOnConflict());

    final PointerTargetSetBuilder builder1 = PointerTargetSet.emptyPointerTargetSet(
                                                                               machineModel,
                                                                               options,
                                                                               formulaManager).builder(),
                                  builder2 = PointerTargetSet.emptyPointerTargetSet(
                                                                               machineModel,
                                                                               options,
                                                                               formulaManager).builder();
    if (reverseBases == reverseFields) {
      builder1.setFields(mergedFields.getFirst());
      builder2.setFields(mergedFields.getSecond());
    } else {
      builder1.setFields(mergedFields.getSecond());
      builder2.setFields(mergedFields.getFirst());
    }

    for (final Map.Entry<String, CType> entry : mergedBases.getSecond().entrySet()) {
      builder1.addBase(entry.getKey(), entry.getValue());
    }

    for (final Map.Entry<String, CType> entry : mergedBases.getFirst().entrySet()) {
      builder2.addBase(entry.getKey(), entry.getValue());
    }

    final PersistentSortedMap<String, DeferredAllocationPool> mergedDeferredAllocations =
        mergeDeferredAllocationPools(pts1, pts2);

    final String lastBase;
    final BooleanFormula basesMergeFormula;
    if (pts1.lastBase == null && pts2.lastBase == null ||
        pts1.lastBase != null && (pts2.lastBase == null || pts1.lastBase.equals(pts2.lastBase))) {
      // The next check doesn't really hold anymore due to possible base unions, but these cases are suspicious
      assert pts1.lastBase == null ||
             pts2.lastBase == null ||
             isFakeBaseType(pts1.bases.get(pts1.lastBase)) ||
             isFakeBaseType(pts2.bases.get(pts2.lastBase)) ||
             pts1.bases.get(pts1.lastBase).equals(pts2.bases.get(pts2.lastBase));
      lastBase = pts1.lastBase;
      basesMergeFormula = formulaManager.getBooleanFormulaManager().makeBoolean(true);
      // Nothing to do, as there were no divergence with regard to base allocations
    } else if (pts1.lastBase == null && pts2.lastBase != null) {
      lastBase = pts2.lastBase;
      basesMergeFormula = formulaManager.getBooleanFormulaManager().makeBoolean(true);
    } else {
      final CType fakeBaseType = getFakeBaseType(0);
      final String fakeBaseName = FAKE_ALLOC_FUNCTION_NAME +
                                  CToFormulaWithUFConverter.getUFName(fakeBaseType) +
                                  CToFormulaWithUFConverter.FRESH_INDEX_SEPARATOR +
                                  PointerTargetSetBuilder.getNextDynamicAllocationIndex();
      mergedBases =
        Triple.of(mergedBases.getFirst(),
                  mergedBases.getSecond(),
                  mergedBases.getThird().putAndCopy(fakeBaseName, fakeBaseType));
      lastBase = fakeBaseName;
      basesMergeFormula = formulaManager.makeAnd(pts1.getNextBaseAddressInequality(fakeBaseName, pts1.lastBase),
                                                 pts2.getNextBaseAddressInequality(fakeBaseName, pts2.lastBase));
    }

    final ConflictHandler<String, PersistentList<PointerTarget>> conflictHandler =
                                                           PointerTargetSetManager.<String, PointerTarget>destructiveMergeOnConflict();

    final PointerTargetSet result  =
      new PointerTargetSet(machineModel,
                           sizeofVisitor,
                           options,
                           mergedBases.getThird(),
                           lastBase,
                           mergedFields.getThird(),
                           mergedDeferredAllocations,
                           mergeSortedMaps(
                             mergeSortedMaps(mergedTargets,
                                             builder1.targets,
                                             conflictHandler),
                             builder2.targets,
                             conflictHandler),
                           formulaManager);
    return Triple.of(result,
                     basesMergeFormula,
                     !reverseBases ? Pair.of(mergedBases.getFirst(), mergedBases.getSecond()) :
                                     Pair.of(mergedBases.getSecond(), mergedBases.getFirst()));
  }

  private PersistentSortedMap<String, DeferredAllocationPool> mergeDeferredAllocationPools(final PointerTargetSet pts1,
      final PointerTargetSet pts2) {
    final Map<DeferredAllocationPool, DeferredAllocationPool> mergedDeferredAllocationPools = new HashMap<>();
    final boolean reverseDeferredAllocations = pts2.deferredAllocations.size() > pts1.deferredAllocations.size();
    final ConflictHandler<String, DeferredAllocationPool> deferredAllocationMergingConflictHandler =
      new ConflictHandler<String, DeferredAllocationPool>() {
      @Override
      public DeferredAllocationPool resolveConflict(String key, DeferredAllocationPool a, DeferredAllocationPool b) {
        final DeferredAllocationPool result = a.mergeWith(b);
        final DeferredAllocationPool oldResult = mergedDeferredAllocationPools.get(result);
        if (oldResult == null) {
          mergedDeferredAllocationPools.put(result, result);
          return result;
        } else {
          final DeferredAllocationPool newResult = oldResult.mergeWith(result);
          mergedDeferredAllocationPools.put(newResult, newResult);
          return newResult;
        }
      }
    };
    PersistentSortedMap<String, DeferredAllocationPool> mergedDeferredAllocations =
      !reverseDeferredAllocations ?
        mergeSortedMaps(pts1.deferredAllocations, pts2.deferredAllocations, deferredAllocationMergingConflictHandler) :
        mergeSortedMaps(pts2.deferredAllocations, pts1.deferredAllocations, deferredAllocationMergingConflictHandler);
    for (final DeferredAllocationPool merged : mergedDeferredAllocationPools.keySet()) {
      for (final String pointerVariable : merged.getPointerVariables()) {
        mergedDeferredAllocations = mergedDeferredAllocations.putAndCopy(pointerVariable, merged);
      }
    }
    return mergedDeferredAllocations;
  }

  private static enum BaseUnitingConflictHandler implements ConflictHandler<String, CType> {
    INSTANCE;

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
                                                                 getUnitedFieldBaseName(currentFieldIndex++)));
        }
      } else {
        membersBuilder.add(new CCompositeTypeMemberDeclaration(type1,
                                                               getUnitedFieldBaseName(currentFieldIndex++)));
      }
      if (type2 instanceof CCompositeType) {
        final CCompositeType compositeType2 = (CCompositeType) type2;
        if (compositeType2.getKind() == ComplexTypeKind.UNION &&
            !compositeType2.getMembers().isEmpty() &&
            compositeType2.getMembers().get(0).getName().equals(getUnitedFieldBaseName(0))) {
          for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType2.getMembers()) {
            membersBuilder.add(new CCompositeTypeMemberDeclaration(memberDeclaration.getType(),
                                                                   getUnitedFieldBaseName(currentFieldIndex++)));
          }
        } else {
          membersBuilder.add(new CCompositeTypeMemberDeclaration(compositeType2,
                                                                 getUnitedFieldBaseName(currentFieldIndex++)));
        }
      } else {
        membersBuilder.add(new CCompositeTypeMemberDeclaration(type2,
                                                               getUnitedFieldBaseName(currentFieldIndex++)));
      }
      return new CCompositeType(false,
                                false,
                                ComplexTypeKind.UNION,
                                membersBuilder.build(),
                                UNITED_BASE_UNION_TAG_PREFIX + type1.toString().replace(' ', '_') + "_and_" +
                                                               type2.toString().replace(' ', '_'));
    }
  }

  private static <K, T> ConflictHandler<K, PersistentList<T>> mergeOnConflict() {
    return new ConflictHandler<K, PersistentList<T>>() {
      @Override
      public PersistentList<T> resolveConflict(K key, PersistentList<T> list1, PersistentList<T> list2) {
        return DeferredAllocationPool.mergeLists(list1, list2);
      }
    };
  }

  private static <K, T> ConflictHandler<K, PersistentList<T>> destructiveMergeOnConflict() {
    return new ConflictHandler<K, PersistentList<T>>() {
      @Override
      public PersistentList<T> resolveConflict(K key, PersistentList<T> list1, PersistentList<T> list2) {
        return list1.withAll(list2);
      }
    };
  }

}
