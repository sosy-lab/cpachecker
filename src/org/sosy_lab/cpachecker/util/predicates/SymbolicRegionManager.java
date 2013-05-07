/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.*;

import java.io.PrintStream;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;

/**
 * Adaptor from FormulaManager/Solver to RegionManager in order to use Formulas
 * as Regions.
 * This class implements only a minimal set of methods on purpose.
 */
public class SymbolicRegionManager implements RegionManager {

  private static class SymbolicRegion implements Region {

    private final BooleanFormula f;
    private final BooleanFormulaManager bfmgr;

    private SymbolicRegion(BooleanFormulaManager bfmgr, BooleanFormula pF) {
      f = checkNotNull(pF);
      this.bfmgr = bfmgr;
    }

    @Override
    public boolean isTrue() {
      return bfmgr.isTrue(f);
    }

    @Override
    public boolean isFalse() {
      return bfmgr.isFalse(f);
    }

    @Override
    public String toString() {
      return f.toString();
    }

    @Override
    public boolean equals(Object pObj) {
      return pObj instanceof SymbolicRegion
          && f.equals(((SymbolicRegion)pObj).f);
    }

    @Override
    public int hashCode() {
      return f.hashCode();
    }
  }

  private final BooleanFormulaManager bfmgr;
  private final Solver solver;

  private final SymbolicRegion trueRegion;
  private final SymbolicRegion falseRegion;

  private int predicateCount = 0;

  public SymbolicRegionManager(FormulaManager fmgr, Solver pSolver) {
    solver = pSolver;
    bfmgr = fmgr.getBooleanFormulaManager();
    trueRegion = new SymbolicRegion(bfmgr,  bfmgr.makeBoolean(true));
    falseRegion = new SymbolicRegion(bfmgr,  bfmgr.makeBoolean(false));
  }

  @Override
  public Region fromFormula(BooleanFormula f, FormulaManagerView pFmgr,
      Function<BooleanFormula, Region> pAtomToRegion) {
    checkArgument(pFmgr.getBooleanFormulaManager() == bfmgr);
    return new SymbolicRegion(bfmgr, f);
  }

  BooleanFormula toFormula(Region r) {
    return ((SymbolicRegion)r).f;
  }

  @Override
  public boolean entails(Region pF1, Region pF2) {
    SymbolicRegion r1 = (SymbolicRegion)pF1;
    SymbolicRegion r2 = (SymbolicRegion)pF2;

    return solver.implies(r1.f, r2.f);
  }

  @Override
  public SymbolicRegion makeTrue() {
    return trueRegion;
  }

  @Override
  public SymbolicRegion makeFalse() {
    return falseRegion;
  }

  @Override
  public Region makeNot(Region pF) {
    SymbolicRegion r = (SymbolicRegion)pF;
    return new SymbolicRegion(r.bfmgr, r.bfmgr.not(r.f));
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    SymbolicRegion r1 = (SymbolicRegion)pF1;
    SymbolicRegion r2 = (SymbolicRegion)pF2;
    assert r1.bfmgr == r2.bfmgr;

    return new SymbolicRegion(r1.bfmgr, r1.bfmgr.and(r1.f, r2.f));
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    SymbolicRegion r1 = (SymbolicRegion)pF1;
    SymbolicRegion r2 = (SymbolicRegion)pF2;
    assert r1.bfmgr == r2.bfmgr;

    return new SymbolicRegion(r1.bfmgr, r1.bfmgr.or(r1.f, r2.f));
  }

  @Override
  public Region makeEqual(Region pF1, Region pF2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Region makeUnequal(Region pF1, Region pF2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Region makeIte(Region pF1, Region pF2, Region pF3) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Region makeExists(Region pF1, Region... pF2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Region createPredicate() {
    return new SymbolicRegion(bfmgr,
        bfmgr.makeVariable("__PREDICATE__" + predicateCount++));
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void printStatistics(PrintStream out) {
    // do nothing
  }

}
