// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Instances of this class should hold a state formula (the result of an abstraction computation) in
 * several representations: First, as an abstract region (usually this would be a BDD). Second, as a
 * symbolic formula. Third, again as a symbolic formula, but this time all variables have names
 * which include their SSA index at the time of the abstraction computation.
 *
 * <p>Additionally the formula for the block immediately before the abstraction computation is
 * stored (this also has SSA indices as it is a path formula, even if it is not of the type
 * PathFormula).
 *
 * <p>Abstractions are not considered equal even if they have the same formula.
 */
public class AbstractionFormula implements Serializable {

  private static final long serialVersionUID = -7756517128231447937L;
  private @Nullable final transient Region region; // Null after de-serializing from proof
  private final transient BooleanFormula formula;
  private final BooleanFormula instantiatedFormula;

  /**
   * The formula of the block directly before this abstraction. (This formula was used to create
   * this abstraction).
   */
  private final PathFormula blockFormula;

  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final transient int id = idGenerator.getFreshId();
  private final transient FormulaManagerView fMgr;
  private final transient ImmutableSet<Integer> idsOfStoredAbstractionReused;

  public AbstractionFormula(
      FormulaManagerView mgr,
      Region pRegion,
      BooleanFormula pFormula,
      BooleanFormula pInstantiatedFormula,
      PathFormula pBlockFormula,
      Set<Integer> pIdOfStoredAbstractionReused) {
    fMgr = checkNotNull(mgr);
    region = checkNotNull(pRegion);
    formula = checkNotNull(pFormula);
    instantiatedFormula = checkNotNull(pInstantiatedFormula);
    blockFormula = checkNotNull(pBlockFormula);
    idsOfStoredAbstractionReused = ImmutableSet.copyOf(pIdOfStoredAbstractionReused);
  }

  /**
   * create a copy with the same formula information, but a new unique object-id.
   *
   * <p>As we do not override {@link Object#equals} in AbstractionFormula, the new copy will not be
   * "equal" to the old instance.
   */
  public AbstractionFormula copyOf() {
    return new AbstractionFormula(
        fMgr, region, formula, instantiatedFormula, blockFormula, idsOfStoredAbstractionReused);
  }

  public boolean isReusedFromStoredAbstraction() {
    return !idsOfStoredAbstractionReused.isEmpty();
  }

  public boolean isTrue() {
    return fMgr.getBooleanFormulaManager().isTrue(formula);
  }

  public boolean isFalse() {
    return fMgr.getBooleanFormulaManager().isFalse(formula);
  }

  public @Nullable Region asRegion() {
    return region;
  }

  /** Returns the formula representation where all variables do not have SSA indices. */
  public BooleanFormula asFormula() {
    return formula;
  }

  public BooleanFormula asFormulaFromOtherSolver(FormulaManagerView pMgr) {
    if (pMgr == fMgr) {
      return formula;
    }
    return pMgr.translateFrom(formula, fMgr);
  }

  public ExpressionTree<Object> asExpressionTree(CFANode pLocation) throws InterruptedException {
    return ExpressionTrees.fromFormula(asFormula(), fMgr, pLocation);
  }

  /** Returns the formula representation where all variables DO have SSA indices. */
  public BooleanFormula asInstantiatedFormula() {
    return instantiatedFormula;
  }

  public PathFormula getBlockFormula() {
    return blockFormula;
  }

  public int getId() {
    return id;
  }

  public ImmutableSet<Integer> getIdsOfStoredAbstractionReused() {
    return idsOfStoredAbstractionReused;
  }

  @Override
  public String toString() {
    // we print the formula only when it is small
    String abs = "";
    if (isTrue()) {
      abs = ": true";
    } else if (isFalse()) {
      abs = ": false";
    }
    return "ABS" + id + abs;
  }

  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  /**
   * javadoc to remove unused parameter warning
   *
   * @param in an input stream
   */
  @SuppressWarnings("UnusedVariable") // parameter is required by API
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = 2349286L;
    private final String instantiatedFormulaDump;
    private final PathFormula blockFormula;

    public SerializationProxy(AbstractionFormula pAbstractionFormula) {
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      instantiatedFormulaDump =
          mgr.dumpFormula(pAbstractionFormula.asInstantiatedFormula()).toString();
      blockFormula = pAbstractionFormula.getBlockFormula();
    }

    private Object readResolve() {
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      BooleanFormula instantiatedFormula = mgr.parse(instantiatedFormulaDump);
      BooleanFormula notInstantiated = mgr.uninstantiate(instantiatedFormula);
      return new AbstractionFormula(
          mgr,
          GlobalInfo.getInstance().getAbstractionManager().convertFormulaToRegion(notInstantiated),
          notInstantiated,
          instantiatedFormula,
          blockFormula,
          ImmutableSet.of());
    }
  }
}
