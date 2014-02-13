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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.Model.TermType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.CToFormulaWithUFConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.CTypeUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.FormulaEncodingWithUFOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PathFormulaWithUF;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTarget;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 *
 * This class inherits from CtoFormulaConverter to import the stuff there.
 */
@Options(prefix="cpa.predicate")
public class PathFormulaManagerImpl implements PathFormulaManager {

  @Option(description = "Handle aliasing of pointers. "
      + "This adds disjunctions to the formulas, so be careful when using cartesian abstraction.")
  private boolean handlePointerAliasing = true;

  private static final String BRANCHING_PREDICATE_NAME = "__ART__";
  private static final Pattern BRANCHING_PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + BRANCHING_PREDICATE_NAME + "(?=\\d+$)");

  private static final String NONDET_VARIABLE = "__nondet__";
  private static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";
  private static final CType NONDET_TYPE = CNumericTypes.INT;
  private final FormulaType<?> NONDET_FORMULA_TYPE;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FunctionFormulaManagerView ffmgr;
  private final CtoFormulaConverter converter;
  private final LogManager logger;

  @Option(description="add special information to formulas about non-deterministic functions")
  private boolean useNondetFlags = false;

  @Deprecated
  public PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, MachineModel pMachineModel)
          throws InvalidConfigurationException {
    this(pFmgr, config, pLogger, pMachineModel, Optional.<VariableClassification>absent());
  }

  public PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, CFA pCfa)
          throws InvalidConfigurationException {
    this(pFmgr, config, pLogger, pCfa.getMachineModel(), pCfa.getVarClassification());
  }

  public PathFormulaManagerImpl(FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger, MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification)
          throws InvalidConfigurationException {
    config.inject(this, PathFormulaManagerImpl.class);

    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    ffmgr = fmgr.getFunctionFormulaManager();
    logger = pLogger;

    converter = createConverter(pFmgr, config, pLogger, pMachineModel, pVariableClassification);

    if (!handlePointerAliasing) {
      NONDET_FORMULA_TYPE = converter.getFormulaTypeFromCType(NONDET_TYPE);
      logger.log(Level.WARNING, "Handling of pointer aliasing is disabled, analysis is unsound if aliased pointers exist.");
    } else {
      NONDET_FORMULA_TYPE = ((CToFormulaWithUFConverter) converter).getFormulaTypeFromCType(NONDET_TYPE, null);
    }
  }

  private CtoFormulaConverter createConverter(FormulaManagerView pFmgr, Configuration config, LogManager pLogger,
      MachineModel pMachineModel, Optional<VariableClassification> pVariableClassification)
          throws InvalidConfigurationException {
    if (handlePointerAliasing) {
      final FormulaEncodingWithUFOptions options = new FormulaEncodingWithUFOptions(config);
      return new CToFormulaWithUFConverter(options, pFmgr, pMachineModel, pVariableClassification, pLogger);

    } else {
      final FormulaEncodingOptions options = new FormulaEncodingOptions(config);
      return new CtoFormulaConverter(options, pFmgr, pMachineModel, pLogger);
    }
  }

  @Override
  public Pair<PathFormula, ErrorConditions> makeAndWithErrorConditions(PathFormula pOldFormula,
                             final CFAEdge pEdge) throws CPATransferException {
    Pair<PathFormula, ErrorConditions> result = converter.makeAnd(pOldFormula, pEdge);

    if (useNondetFlags) {
      PathFormula pf = result.getFirst();
      SSAMapBuilder ssa = pf.getSsa().builder();

      int lNondetIndex = ssa.getIndex(NONDET_VARIABLE);
      int lFlagIndex = ssa.getIndex(NONDET_FLAG_VARIABLE);

      if (lNondetIndex != lFlagIndex) {
        if (lFlagIndex < 0) {
          lFlagIndex = 1; // ssa indices start with 2, so next flag that is generated also uses index 2
        }

        BooleanFormula edgeFormula = pf.getFormula();

        for (int lIndex = lFlagIndex + 1; lIndex <= lNondetIndex; lIndex++) {
          Formula nondetVar = fmgr.makeVariable(NONDET_FORMULA_TYPE, NONDET_FLAG_VARIABLE, lIndex);
          BooleanFormula lAssignment = fmgr.assignment(nondetVar, fmgr.makeNumber(NONDET_FORMULA_TYPE, 1));
          edgeFormula = bfmgr.and(edgeFormula, lAssignment);
        }

        // update ssa index of nondet flag
        //setSsaIndex(ssa, Variable.create(NONDET_FLAG_VARIABLE, getNondetType()), lNondetIndex);
        ssa.setIndex(NONDET_FLAG_VARIABLE, NONDET_TYPE, lNondetIndex);

        result = Pair.of(new PathFormula(edgeFormula, ssa.build(), pf.getLength()),
                         result.getSecond());
      }
    }

    return result;
  }

  @Override
  public PathFormula makeAnd(PathFormula pOldFormula, CFAEdge pEdge) throws CPATransferException {
    return makeAndWithErrorConditions(pOldFormula, pEdge).getFirst();
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    if (!handlePointerAliasing) {
      return new PathFormula(bfmgr.makeBoolean(true), SSAMap.emptySSAMap(), 0);
    } else {
      return ((CToFormulaWithUFConverter)converter).makeEmptyPathFormula();
    }
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    if (!handlePointerAliasing) {
      return new PathFormula(bfmgr.makeBoolean(true), oldFormula.getSsa(), 0);
    } else {
      return new PathFormulaWithUF(bfmgr.makeBoolean(true),
                                   oldFormula.getSsa(),
                                   ((PathFormulaWithUF) oldFormula).getPointerTargetSet(), 0);
    }
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula oldFormula, SSAMap m) {
    if (!handlePointerAliasing) {
      return new PathFormula(oldFormula.getFormula(), m, oldFormula.getLength());
    } else {
      return new PathFormulaWithUF(oldFormula.getFormula(),
                                   m,
                                   ((PathFormulaWithUF) oldFormula).getPointerTargetSet(),
                                   oldFormula.getLength());
    }
  }

  @Override
  public PathFormula makeOr(final PathFormula pathFormula1, final PathFormula pathFormula2) {

    final BooleanFormula formula1 = pathFormula1.getFormula();
    final BooleanFormula formula2 = pathFormula2.getFormula();
    final SSAMap ssa1 = pathFormula1.getSsa();
    final SSAMap ssa2 = pathFormula2.getSsa();

    if (!handlePointerAliasing) {
      final Pair<Pair<BooleanFormula, BooleanFormula>, SSAMap> mergeResult = mergeSSAMaps(ssa2, ssa1);

      // Do not swap these two lines, that makes a huge difference in performance!
      final BooleanFormula newFormula2 = bfmgr.and(formula2, mergeResult.getFirst().getFirst());
      final BooleanFormula newFormula1 = bfmgr.and(formula1, mergeResult.getFirst().getSecond());

      final BooleanFormula newFormula = bfmgr.or(newFormula1, newFormula2);
      final SSAMap newSsa = mergeResult.getSecond();

      final int newLength = Math.max(pathFormula1.getLength(), pathFormula2.getLength());

      return new PathFormula(newFormula, newSsa, newLength);
    } else {
      final PointerTargetSet pts1 = ((PathFormulaWithUF) pathFormula1).getPointerTargetSet();
      final PointerTargetSet pts2 = ((PathFormulaWithUF) pathFormula2).getPointerTargetSet();

      final Triple<Triple<BooleanFormula, BooleanFormula, BooleanFormula>, SSAMap, PointerTargetSet> mergeResult =
        mergeSSAMapsAndPointerTargetSets(ssa1, pts1, ssa2, pts2);

      // (?) Do not swap these two lines, that makes a huge difference in performance (?) !
      final BooleanFormula newFormula1 = bfmgr.and(formula1, mergeResult.getFirst().getFirst());
      final BooleanFormula newFormula2 = bfmgr.and(formula2, mergeResult.getFirst().getSecond());
      final BooleanFormula newFormula = bfmgr.and(bfmgr.or(newFormula1, newFormula2),
                                                           mergeResult.getFirst().getThird());
      final SSAMap newSSA = mergeResult.getSecond();
      final PointerTargetSet newPTS = mergeResult.getThird();
      final int newLength = Math.max(pathFormula1.getLength(), pathFormula2.getLength());

      return new PathFormulaWithUF(newFormula, newSSA, newPTS, newLength);
    }
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    BooleanFormula otherFormula =  fmgr.instantiate(pOtherFormula, ssa);
    BooleanFormula resultFormula = bfmgr.and(pPathFormula.getFormula(), otherFormula);
    if (!handlePointerAliasing) {
      return new PathFormula(resultFormula, ssa, pPathFormula.getLength());
    } else {
      final PointerTargetSet pts = ((PathFormulaWithUF) pPathFormula).getPointerTargetSet();
      return new PathFormulaWithUF(resultFormula, ssa, pts, pPathFormula.getLength());
    }
  }

  /**
   * builds a formula that represents the necessary variable assignments
   * to "merge" the two ssa maps. That is, for every variable X that has two
   * different ssa indices i and j in the maps, creates a new formula
   * (X_k = X_i) | (X_k = X_j), where k is a fresh ssa index.
   * Returns the formula described above, plus a new SSAMap that is the merge
   * of the two.
   *
   * @param ssa1 an SSAMap
   * @param ssa2 an SSAMap
   * @return A pair (Formula, SSAMap)
   */
  private Pair<Pair<BooleanFormula, BooleanFormula>, SSAMap> mergeSSAMaps(
      SSAMap ssa1, SSAMap ssa2) {
    Pair<SSAMap, List<Triple<String, Integer, Integer>>> result = SSAMap.merge(ssa1, ssa2);
    SSAMap resultSSA = result.getFirst();
    List<Triple<String, Integer, Integer>> varDifferences = result.getSecond();

    BooleanFormula mt1 = bfmgr.makeBoolean(true);
    BooleanFormula mt2 = bfmgr.makeBoolean(true);

    for (Triple<String, Integer, Integer> difference : varDifferences) {
      String name = difference.getFirst();
      int i1 = Objects.firstNonNull(difference.getSecond(), 1);
      int i2 = Objects.firstNonNull(difference.getThird(), 1);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        BooleanFormula t;

        if (useNondetFlags && name.equals(NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i2, 1), i1);
        } else {
          t = makeSSAMerger(name, resultSSA.getType(name), Math.max(i2, 1), i1);
        }

        mt2 = bfmgr.and(mt2, t);

      } else if (i2 > 1) {
        assert i1 < i2;
        // i1:smaller, i2:bigger
        // => need correction term for i1
        BooleanFormula t;

        if (useNondetFlags && name.equals(NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i1, 1), i2);
        } else {
          t = makeSSAMerger(name, resultSSA.getType(name), Math.max(i1, 1), i2);
        }

        mt1 = bfmgr.and(mt1, t);
      }
    }

    return Pair.of(Pair.of(mt1, mt2), resultSSA);
  }

  private Triple<Triple<BooleanFormula, BooleanFormula, BooleanFormula>, SSAMap, PointerTargetSet>
    mergeSSAMapsAndPointerTargetSets(final SSAMap ssa1,
                                     final PointerTargetSet pts1,
                                     final SSAMap ssa2,
                                     final PointerTargetSet pts2) {
    final Pair<SSAMap, List<Triple<String, Integer, Integer>>> ssaMergeResult = SSAMap.merge(ssa1, ssa2);
    final SSAMap resultSSA = ssaMergeResult.getFirst();
    final List<Triple<String, Integer, Integer>> symbolDifferences = ssaMergeResult.getSecond();

    BooleanFormula mergeFormula1 = bfmgr.makeBoolean(true);
    BooleanFormula mergeFormula2 = bfmgr.makeBoolean(true);

    for (final Triple<String, Integer, Integer> symbolDifference : symbolDifferences) {
      final String symbolName = symbolDifference.getFirst();
      final int index1 = Objects.firstNonNull(symbolDifference.getSecond(), 1);
      final int index2 = Objects.firstNonNull(symbolDifference.getThird(), 1);

      BooleanFormula mergeFormula = bfmgr.makeBoolean(true);
      if (index1 > index2 && index1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2

        if (useNondetFlags && index2 > 0 && symbolName.equals(NONDET_FLAG_VARIABLE)) {
          mergeFormula = makeNondetFlagMerger(index2, index1);
        } else if (index2 > 0 && !symbolName.startsWith(CToFormulaWithUFConverter.UF_NAME_PREFIX)) {
          mergeFormula = makeNondetMiddleVariableMerger(symbolName,
                                                        resultSSA.getType(symbolName),
                                                        index2,
                                                        index1,
                                                        pts2);
        } else if (index2 > 0) {
          final CType symbolType = resultSSA.getType(symbolName);
          mergeFormula = makeNondetMiddleUFMerger(CToFormulaWithUFConverter.getUFName(symbolType),
                                                  symbolType,
                                                  index2,
                                                  index1,
                                                  pts2);
        }

        mergeFormula2 = bfmgr.and(mergeFormula2, mergeFormula);

      } else if (index2 > 1) {
        assert index1 < index2;
        // i1:smaller, i2:bigger
        // => need correction term for i1

        if (useNondetFlags && index1 > 0 && symbolName.equals(NONDET_FLAG_VARIABLE)) {
          mergeFormula = makeNondetFlagMerger(index1, index2);
        } else if (index1 > 0 && !symbolName.startsWith(CToFormulaWithUFConverter.UF_NAME_PREFIX)) {
          mergeFormula = makeNondetMiddleVariableMerger(symbolName,
                                                        resultSSA.getType(symbolName),
                                                        index1,
                                                        index2,
                                                        pts1);
        } else if (index2 > 0) {
          final CType symbolType = resultSSA.getType(symbolName);
          mergeFormula = makeNondetMiddleUFMerger(CToFormulaWithUFConverter.getUFName(symbolType),
                                                  symbolType,
                                                  index1,
                                                  index2,
                                                  pts1);
        }

        mergeFormula1 = bfmgr.and(mergeFormula1, mergeFormula);
      }
    }

    final Triple<PointerTargetSet,
                 BooleanFormula,
                 Pair<PersistentSortedMap<String, CType>, PersistentSortedMap<String, CType>>>
      ptsMergeResult = pts1.mergeWith(pts2);

    final List<Pair<CCompositeType, String>> sharedFields = new ArrayList<>();
    for (final Map.Entry<String, CType> baseFromPTS1 : ptsMergeResult.getThird().getFirst().entrySet()) {
      if (!((CToFormulaWithUFConverter) converter).isDynamicAllocVariableName(baseFromPTS1.getKey()) &&
          !CTypeUtils.containsArray(baseFromPTS1.getValue())) {
        final FormulaType<?> baseFormulaType = ((CToFormulaWithUFConverter) converter)
                                                 .getFormulaTypeFromCType(
                                                   CTypeUtils.getBaseType(baseFromPTS1.getValue()), pts1);
        mergeFormula2 = bfmgr.and(mergeFormula2, makeSharingConstraints(fmgr.makeVariable(baseFormulaType,
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
      if (!((CToFormulaWithUFConverter) converter).isDynamicAllocVariableName(baseFromPTS2.getKey()) &&
          !CTypeUtils.containsArray(baseFromPTS2.getValue())) {
        final FormulaType<?> baseFormulaType = ((CToFormulaWithUFConverter) converter)
                                                 .getFormulaTypeFromCType(
                                                   CTypeUtils.getBaseType(baseFromPTS2.getValue()), pts1);
        mergeFormula1 = bfmgr.and(mergeFormula1, makeSharingConstraints(fmgr.makeVariable(baseFormulaType,
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
      final PointerTargetSetBuilder resultPTSBuilder = resultPTS.builder();
      for (final Pair<CCompositeType, String> sharedField : sharedFields) {
        resultPTSBuilder.addField(sharedField.getFirst(), sharedField.getSecond());
      }
      resultPTS = resultPTSBuilder.build();
    }

    return Triple.of(Triple.of(mergeFormula1, mergeFormula2, ptsMergeResult.getSecond()), resultSSA, resultPTS);
  }

  private BooleanFormula makeNondetMiddleVariableMerger(final String variableName,
                                                        final CType variableType,
                                                        final int oldIndex,
                                                        final int newIndex,
                                                        final PointerTargetSet pts) {
    assert oldIndex < newIndex;

    final FormulaType<?> variableFormulaType = ((CToFormulaWithUFConverter) converter)
                                                 .getFormulaTypeFromCType(variableType, pts);
    final Formula oldVariable = fmgr.makeVariable(variableFormulaType, variableName, oldIndex);
    final Formula newVariable = fmgr.makeVariable(variableFormulaType, variableName, newIndex);

    return fmgr.makeEqual(newVariable, oldVariable);
  }

  private BooleanFormula makeNondetMiddleUFMerger(final String functionName,
                                                  final CType returnType,
                                                  final int oldIndex,
                                                  final int newIndex,
                                                  final PointerTargetSet pts) {
    assert oldIndex < newIndex;

    final FormulaType<?> returnFormulaType =  ((CToFormulaWithUFConverter) converter)
                                                .getFormulaTypeFromCType(returnType, pts);
    BooleanFormula result = bfmgr.makeBoolean(true);
    for (final PointerTarget target : pts.getAllTargets(returnType)) {
      final Formula targetAddress = fmgr.makePlus(fmgr.makeVariable(pts.getPointerType(), target.getBaseName()),
                                                  fmgr.makeNumber(pts.getPointerType(), target.getOffset()));

      final BooleanFormula retention = fmgr.makeEqual(ffmgr.createFuncAndCall(functionName,
                                                                              newIndex,
                                                                              returnFormulaType,
                                                                              ImmutableList.of(targetAddress)),
                                                      ffmgr.createFuncAndCall(functionName,
                                                                              oldIndex,
                                                                              returnFormulaType,
                                                                              ImmutableList.of(targetAddress)));
      result = fmgr.makeAnd(result, retention);
    }

    return result;
  }

  private Formula makeDereferece(final CType type,
                                 final Formula address,
                                 final SSAMap ssa,
                                 final PointerTargetSet pts) {
    final String ufName = CToFormulaWithUFConverter.getUFName(type);
    final int index = ssa.getIndex(ufName);
    final FormulaType<?> returnType = ((CToFormulaWithUFConverter) converter).getFormulaTypeFromCType(type, pts);
    return ffmgr.createFuncAndCall(ufName, index, returnType, ImmutableList.of(address));
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
                                       fmgr.makePlus(address, fmgr.makeNumber(pts.getPointerType(), offset)),
                                       newPrefix,
                                       memberType,
                                       sharedFields,
                                       ssa,
                                       pts));
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += pts.getSize(memberType);
        }
      }
    } else {
      if (ssa.getIndex(variablePrefix) > 0) {
        final FormulaType<?> variableFormulaType = ((CToFormulaWithUFConverter) converter)
                                                     .getFormulaTypeFromCType(variableType, pts);
        result = bfmgr.and(result, fmgr.makeEqual(makeDereferece(variableType, address, ssa, pts),
                                                  fmgr.makeVariable(variableFormulaType,
                                                                    variablePrefix,
                                                                    ssa.getIndex(variablePrefix))));
      }
    }

    return result;
  }

  private BooleanFormula makeNondetFlagMerger(int iSmaller, int iBigger) {
    return makeMerger(NONDET_FLAG_VARIABLE, iSmaller, iBigger, fmgr.makeNumber(NONDET_FORMULA_TYPE, 0));
  }

  private BooleanFormula makeMerger(String var, int iSmaller, int iBigger, Formula pInitialValue) {
    assert iSmaller < iBigger;

    BooleanFormula lResult = bfmgr.makeBoolean(true);
    FormulaType<Formula> type = fmgr.getFormulaType(pInitialValue);

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentVar = fmgr.makeVariable(type, var, i);
      BooleanFormula e = fmgr.makeEqual(currentVar, pInitialValue);
      lResult = bfmgr.and(lResult, e);
    }

    return lResult;
  }

  // creates the mathsat terms
  // (var@iSmaller = var@iSmaller+1; ...; var@iSmaller = var@iBigger)
  private BooleanFormula makeSSAMerger(String name, CType type, int iSmaller, int iBigger) {
    FormulaType<?> t = converter.getFormulaTypeFromCType(type);
    return makeMerger(name, iSmaller, iBigger,
        fmgr.makeVariable(t, name, iSmaller));
  }

  @Override
  public PathFormula makeFormulaForPath(List<CFAEdge> pPath) throws CPATransferException {
    PathFormula pathFormula = makeEmptyPathFormula();
    for (CFAEdge edge : pPath) {
      pathFormula = makeAnd(pathFormula, edge);
    }
    return pathFormula;
  }


  /**
   * Build a formula containing a predicate for all branching situations in the
   * ARG. If a satisfying assignment is created for this formula, it can be used
   * to find out which paths in the ARG are feasible.
   *
   * This method may be called with an empty set, in which case it does nothing
   * and returns the formula "true".
   *
   * @param elementsOnPath The ARG states that should be considered.
   * @return A formula containing a predicate for each branching.
   * @throws CPATransferException
   */
  @Override
  public BooleanFormula buildBranchingFormula(Iterable<ARGState> elementsOnPath) throws CPATransferException {
    // build the branching formula that will help us find the real error path
    BooleanFormula branchingFormula = bfmgr.makeBoolean(true);
    for (final ARGState pathElement : elementsOnPath) {

      if (pathElement.getChildren().size() > 1) {
        if (pathElement.getChildren().size() > 2) {
          // can't create branching formula
          if (from(pathElement.getChildren()).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            // We expect this situation of one of the children is a target state created by PredicateCPA.
            continue;
          } else {
            logger.log(Level.WARNING, "ARG branching with more than two outgoing edges");
            return bfmgr.makeBoolean(true);
          }
        }

        FluentIterable<CFAEdge> outgoingEdges = from(pathElement.getChildren()).transform(
            new Function<ARGState, CFAEdge>() {
              @Override
              public CFAEdge apply(ARGState child) {
                return pathElement.getEdgeToChild(child);
              }
        });
        if (!outgoingEdges.allMatch(Predicates.instanceOf(AssumeEdge.class))) {
          if (from(pathElement.getChildren()).anyMatch(AbstractStates.IS_TARGET_STATE)) {
            // We expect this situation of one of the children is a target state created by PredicateCPA.
            continue;
          } else {
            logger.log(Level.WARNING, "ARG branching without AssumeEdge");
            return bfmgr.makeBoolean(true);
          }
        }

        AssumeEdge edge = null;
        for (CFAEdge currentEdge : outgoingEdges) {
          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            edge = (AssumeEdge)currentEdge;
            break;
          }
        }
        assert edge != null;
        BooleanFormula pred = bfmgr.makeVariable(BRANCHING_PREDICATE_NAME + pathElement.getStateId(), 0);

        // create formula by edge, be sure to use the correct SSA indices!
        // TODO the class PathFormulaManagerImpl should not depend on PredicateAbstractState,
        // it is used without PredicateCPA as well.
        PathFormula pf;
        PredicateAbstractState pe = AbstractStates.extractStateByType(pathElement, PredicateAbstractState.class);
        if (pe == null) {
          logger.log(Level.WARNING, "Cannot find precise error path information without PredicateCPA");
          return bfmgr.makeBoolean(true);
        } else {
          pf = pe.getPathFormula();
        }
        pf = this.makeEmptyPathFormula(pf); // reset everything except SSAMap
        pf = this.makeAnd(pf, edge);        // conjunct with edge

        BooleanFormula equiv = bfmgr.equivalence(pred, pf.getFormula());
        branchingFormula = bfmgr.and(branchingFormula, equiv);
      }
    }
    return branchingFormula;
  }

  /**
   * Extract the information about the branching predicates created by
   * {@link #buildBranchingFormula(Set)} from a satisfying assignment.
   *
   * A map is created that stores for each ARGState (using its element id as
   * the map key) which edge was taken (the positive or the negated one).
   *
   * @param model A satisfying assignment that should contain values for branching predicates.
   * @return A map from ARG state id to a boolean value indicating direction.
   */
  @Override
  public Map<Integer, Boolean> getBranchingPredicateValuesFromModel(Model model) {
    if (model.isEmpty()) {
      logger.log(Level.WARNING, "No satisfying assignment given by solver!");
      return Collections.emptyMap();
    }

    Map<Integer, Boolean> preds = Maps.newHashMap();
    for (AssignableTerm a : model.keySet()) {
      if (a instanceof Model.Variable && a.getType() == TermType.Boolean) {

        String name = BRANCHING_PREDICATE_NAME_PATTERN.matcher(a.getName()).replaceFirst("");
        if (!name.equals(a.getName())) {
          // pattern matched, so it's a variable with __ART__ in it

          // no NumberFormatException because of RegExp match earlier
          Integer nodeId = Integer.parseInt(name);

          assert !preds.containsKey(nodeId);


          Boolean value = (Boolean)model.get(a);
          preds.put(nodeId, value);
        }
      }
    }
    return preds;
  }
}
