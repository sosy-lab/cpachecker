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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.PrintStream;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

/**
 * Adaptor from FormulaManager/Solver to RegionManager in order to use Formulas
 * as Regions.
 * This class implements only a minimal set of methods on purpose.
 */
public class SymbolicRegionManager implements RegionManager {

  public static class SymbolicRegion implements Region {

    private final Formula f;

    public SymbolicRegion(Formula pF) {
      f = checkNotNull(pF);
    }

    @Override
    public boolean isTrue() {
      return f.isTrue();
    }

    @Override
    public boolean isFalse() {
      return f.isFalse();
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

  private final Solver solver;

  private final SymbolicRegion trueRegion;
  private final SymbolicRegion falseRegion;

  public SymbolicRegionManager(FormulaManager fmgr, Solver pSolver) {
    solver = pSolver;

    trueRegion = new SymbolicRegion(fmgr.makeTrue());
    falseRegion = new SymbolicRegion(fmgr.makeFalse());
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
    throw new UnsupportedOperationException();
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    throw new UnsupportedOperationException();
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
  public Region makeExists(Region pF1, Region... pF2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Region createPredicate() {
    throw new UnsupportedOperationException();
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
