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
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.VOID;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.MapMerger.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.MapMerger;
import org.sosy_lab.cpachecker.util.predicates.pathformula.MapMerger.ConflictHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
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
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView bfmgr;
  private final FunctionFormulaManagerView ffmgr;
  final CToFormulaWithUFTypeHandler typeHandler;

  public PointerTargetSetManager(FormulaEncodingWithUFOptions pOptions,
      FormulaManagerView pFormulaManager, CToFormulaWithUFTypeHandler pTypeHandler) {
    options = pOptions;
    formulaManager = pFormulaManager;
    bfmgr = formulaManager.getBooleanFormulaManager();
    ffmgr = formulaManager.getFunctionFormulaManager();
    typeHandler = pTypeHandler;
  }

  public Pair<Triple<BooleanFormula, BooleanFormula, BooleanFormula>, PointerTargetSet>
            mergePointerTargetSets(final PointerTargetSet pts1,
                                   final PointerTargetSet pts2,
                                   final SSAMap resultSSA) {

    BooleanFormula mergeFormula1 = bfmgr.makeBoolean(true);
    BooleanFormula mergeFormula2 = bfmgr.makeBoolean(true);

    final Triple<PointerTargetSet,
                 BooleanFormula,
                 Pair<PersistentSortedMap<String, CType>, PersistentSortedMap<String, CType>>>
      ptsMergeResult = merge(pts1, pts2);

    final List<Pair<CCompositeType, String>> sharedFields = new ArrayList<>();
    for (final Map.Entry<String, CType> baseFromPTS1 : ptsMergeResult.getThird().getFirst().entrySet()) {
      if (!options.isDynamicAllocVariableName(baseFromPTS1.getKey()) &&
          !CTypeUtils.containsArray(baseFromPTS1.getValue())) {
        final FormulaType<?> baseFormulaType = typeHandler.getFormulaTypeFromCType(
                                                   CTypeUtils.getBaseType(baseFromPTS1.getValue()));
        mergeFormula2 = bfmgr.and(mergeFormula2, makeSharingConstraints(formulaManager.makeVariable(baseFormulaType,
                                                                                          PointerTargetSet.getBaseName(
                                                                                            baseFromPTS1.getKey())),
                                                                        baseFromPTS1.getKey(),
                                                                        baseFromPTS1.getValue(),
                                                                        sharedFields,
                                                                        resultSSA,
                                                                        pts2));
      }
    }
    for (final Map.Entry<String, CType> baseFromPTS2 : ptsMergeResult.getThird().getSecond().entrySet()) {
      if (!options.isDynamicAllocVariableName(baseFromPTS2.getKey()) &&
          !CTypeUtils.containsArray(baseFromPTS2.getValue())) {
        final FormulaType<?> baseFormulaType = typeHandler.getFormulaTypeFromCType(
                                                   CTypeUtils.getBaseType(baseFromPTS2.getValue()));
        mergeFormula1 = bfmgr.and(mergeFormula1, makeSharingConstraints(formulaManager.makeVariable(baseFormulaType,
                                                                                          PointerTargetSet.getBaseName(
                                                                                              baseFromPTS2.getKey())),
                                                                        baseFromPTS2.getKey(),
                                                                        baseFromPTS2.getValue(),
                                                                        sharedFields,
                                                                        resultSSA,
                                                                        pts1));
      }
    }

    PointerTargetSet resultPTS = ptsMergeResult.getFirst();
    if (!sharedFields.isEmpty()) {
      final PointerTargetSetBuilder resultPTSBuilder = resultPTS.builder(this, options);
      for (final Pair<CCompositeType, String> sharedField : sharedFields) {
        resultPTSBuilder.addField(sharedField.getFirst(), sharedField.getSecond());
      }
      resultPTS = resultPTSBuilder.build();
    }

    return Pair.of(Triple.of(mergeFormula1, mergeFormula2, ptsMergeResult.getSecond()), resultPTS);
  }

  private Triple<PointerTargetSet,
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
                                                                               formulaManager).builder(this, options),
                                  builder2 = PointerTargetSet.emptyPointerTargetSet(
                                                                               formulaManager).builder(this, options);
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
      basesMergeFormula = formulaManager.makeAnd(pts1.getNextBaseAddressInequality(fakeBaseName, pts1.lastBase, typeHandler),
                                                 pts2.getNextBaseAddressInequality(fakeBaseName, pts2.lastBase, typeHandler));
    }

    final ConflictHandler<String, PersistentList<PointerTarget>> conflictHandler =
                                                           PointerTargetSetManager.<String, PointerTarget>destructiveMergeOnConflict();

    final PointerTargetSet result  =
      new PointerTargetSet(mergedBases.getThird(),
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

  private BooleanFormula makeSharingConstraints(final Formula address,
                                                final String variablePrefix,
                                                final CType variableType,
                                                final List<Pair<CCompositeType, String>> sharedFields,
                                                final SSAMap ssa,
                                                final PointerTargetSet pts) {

    assert !CTypeUtils.containsArray(variableType) : "Array access can't be encoded as a varaible";

    BooleanFormula result = bfmgr.makeBoolean(true);

    if (variableType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) variableType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final String memberName = memberDeclaration.getName();
        final CType memberType = CTypeUtils.simplifyType(memberDeclaration.getType());
        final String newPrefix = variablePrefix + CToFormulaWithUFConverter.FIELD_NAME_SEPARATOR + memberName;
        if (ssa.getIndex(newPrefix) > 0) {
          sharedFields.add(Pair.of(compositeType, memberName));
          result = bfmgr.and(result, makeSharingConstraints(
                                       formulaManager.makePlus(address, formulaManager.makeNumber(typeHandler.getPointerType(), offset)),
                                       newPrefix,
                                       memberType,
                                       sharedFields,
                                       ssa,
                                       pts));
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += typeHandler.getSizeof(memberType);
        }
      }
    } else {
      if (ssa.getIndex(variablePrefix) > 0) {
        final FormulaType<?> variableFormulaType = typeHandler.getFormulaTypeFromCType(variableType);
        result = bfmgr.and(result, formulaManager.makeEqual(makeDereferece(variableType, address, ssa),
                                                  formulaManager.makeVariable(variableFormulaType,
                                                                    variablePrefix,
                                                                    ssa.getIndex(variablePrefix))));
      }
    }

    return result;
  }

  private Formula makeDereferece(final CType type,
                                 final Formula address,
                                 final SSAMap ssa) {
    final String ufName = CToFormulaWithUFConverter.getUFName(type);
    final int index = ssa.getIndex(ufName);
    final FormulaType<?> returnType = typeHandler.getFormulaTypeFromCType(type);
    return ffmgr.createFuncAndCall(ufName, index, returnType, ImmutableList.of(address));
  }



  /**
   * The method is used to speed up {@code sizeof} computation by caching sizes of declared composite types.
   * @param cType
   * @return
   */
  public int getSize(CType cType) {
    return typeHandler.getSizeof(cType);
  }

  /**
   * The method is used to speed up member offset computation for declared composite types.
   * @param compositeType
   * @param memberName
   * @return
   */
  public int getOffset(CCompositeType compositeType, final String memberName) {
    return typeHandler.getOffset(compositeType, memberName);
  }

  @CheckReturnValue
  private static PersistentSortedMap<String, PersistentList<PointerTarget>> addToTarget(final String base,
                         final CType targetType,
                         final @Nullable CType containerType,
                         final int properOffset,
                         final int containerOffset,
                         final PersistentSortedMap<String, PersistentList<PointerTarget>> targets) {
    final String type = CTypeUtils.typeToString(targetType);
    PersistentList<PointerTarget> targetsForType = firstNonNull(targets.get(type),
                                                                PersistentLinkedList.<PointerTarget>of());
    return targets.putAndCopy(type, targetsForType.with(new PointerTarget(base,
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
   * @param currentType type of the allocated base or the next added pointer target
   * @param containerType either {@code null} or the type of the innermost container of the next added pointer target
   * @param properOffset either {@code 0} or the offset of the next added pointer target in its innermost container
   * @param containerOffset either {@code 0} or the offset of the innermost container (relative to the base adddress)
   * @param targets The list of targets where the new targets should be added to.
   * @param fields The set of "shared" fields that are accessed directly via pointers.
   * @return The targets map together with all the added targets.
   */
  @CheckReturnValue
  PersistentSortedMap<String, PersistentList<PointerTarget>> addToTargets(final String base,
                          final CType currentType,
                          final @Nullable CType containerType,
                          final int properOffset,
                          final int containerOffset,
                          PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
                          final PersistentSortedMap<CompositeField, Boolean> fields) {
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
        targets = addToTargets(base, arrayType.getType(), arrayType, offset, containerOffset + properOffset, targets, fields);
        offset += getSize(arrayType.getType());
      }
    } else if (cType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) cType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      final String type = CTypeUtils.typeToString(compositeType);
      typeHandler.addCompositeTypeToCache(compositeType);
      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (fields.containsKey(CompositeField.of(type, memberDeclaration.getName()))) {
          targets = addToTargets(base, memberDeclaration.getType(), compositeType, offset, containerOffset + properOffset, targets, fields);
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += getSize(memberDeclaration.getType());
        }
      }
    } else {
      targets = addToTarget(base, cType, containerType, properOffset, containerOffset, targets);
    }

    return targets;
  }
}
