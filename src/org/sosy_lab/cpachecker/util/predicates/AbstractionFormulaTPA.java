// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * TODO: Add documentation
 */
public class AbstractionFormulaTPA extends AbstractionFormula implements Serializable {

  // Fields from AbstractionFormula
  private @Nullable
  final transient Region region; // Null after de-serializing from proof
  private final transient BooleanFormula formula;
  private final BooleanFormula instantiatedFormula;
  private final PathFormula blockFormula;

  private final List<AbstractionPredicate> transitionPredicateList = new ArrayList<>();
  private SSAMap blockFormulaSsaMap;

//  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
//  private final transient int id = idGenerator.getFreshId();
  private final transient FormulaManagerView fMgr;
  private final transient ImmutableSet<Integer> idsOfStoredAbstractionReused;

  // AbstractionFormulaTPA's fields
//  private final BooleanFormula tpaFormula;
  public AbstractionFormulaTPA(
      FormulaManagerView mgr,
      Region pRegion,
      BooleanFormula pFormula,
      BooleanFormula pInstantiatedFormula,
      PathFormula pBlockFormula,
      Set<Integer> pIdOfStoredAbstractionReused) {
    super(mgr, pRegion, pFormula, pInstantiatedFormula, pBlockFormula,
        pIdOfStoredAbstractionReused);

    fMgr = checkNotNull(mgr);
    region = checkNotNull(pRegion);
    formula = checkNotNull(pFormula);
    instantiatedFormula = checkNotNull(pInstantiatedFormula);
    blockFormula = checkNotNull(pBlockFormula);
    idsOfStoredAbstractionReused = ImmutableSet.copyOf(pIdOfStoredAbstractionReused);

//    printNewAbstractFormula(mgr, pRegion, pFormula, pInstantiatedFormula, pBlockFormula, pIdOfStoredAbstractionReused);
  }

  public void addTransitionPredicates(List<AbstractionPredicate> pTransitionPredicate) {
    transitionPredicateList.addAll(pTransitionPredicate);
  }

  public boolean isContainTransitionPredicate() {
    return !transitionPredicateList.isEmpty();
  }

  public SSAMap getBlockFormulaSsaMap() {
    return blockFormulaSsaMap;
  }

  public void setBlockFormulaSsaMap(SSAMap pBlockFormulaSsaMap) {
    blockFormulaSsaMap = pBlockFormulaSsaMap;
  }

  private void printNewAbstractFormula(
      FormulaManagerView mgr,
      Region pRegion,
      BooleanFormula pFormula,
      BooleanFormula pInstantiatedFormula,
      PathFormula pBlockFormula,
      Set<Integer> pIdOfStoredAbstractionReused) {
    BooleanFormula formulaWithPrimeVariable = null;
    System.out.println("CREATING NEW TPA ABSTRACT FORMULA");
    System.out.println("Region: " + pRegion.toString());
    System.out.println("Formula: " + pFormula.toString());
    System.out.println("Instantiated Formula: " + pInstantiatedFormula.toString());
    System.out.println("Block Formula: " + pBlockFormula);
    System.out.println("=============================================================================");
  }

  /**
   * create a copy with the same formula information, but a new unique object-id.
   *
   * <p>As we do not override {@link Object#equals} in AbstractionFormula, the new copy will not be
   * "equal" to the old instance.
   */
  @Override
  public AbstractionFormulaTPA copyOf() {
    return new AbstractionFormulaTPA(
        fMgr, region, formula, instantiatedFormula, blockFormula, idsOfStoredAbstractionReused);
  }
}
