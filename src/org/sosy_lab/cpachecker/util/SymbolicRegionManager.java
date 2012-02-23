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
package org.sosy_lab.cpachecker.util;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;


public class SymbolicRegionManager implements RegionManager {

  private static class SymbolicRegion implements Region {

    private final Formula f;

    public SymbolicRegion(Formula pF) {
      f = pF;
    }

    @Override
    public boolean isTrue() {
      return f.isTrue();
    }

    @Override
    public boolean isFalse() {
      return f.isFalse();
    }
  }

  private final FormulaManager fmgr;
  private final Solver solver;

  public SymbolicRegionManager(FormulaManager pFmgr, Solver pSolver) {
    fmgr = pFmgr;
    solver = pSolver;
  }

  @Override
  public boolean entails(Region pF1, Region pF2) {
    SymbolicRegion r1 = (SymbolicRegion)pF1;
    SymbolicRegion r2 = (SymbolicRegion)pF2;

    return solver.implies(r1.f, r2.f);
  }

  @Override
  public Region makeTrue() {
    return new SymbolicRegion(fmgr.makeTrue());
  }

  @Override
  public Region makeFalse() {
    return new SymbolicRegion(fmgr.makeFalse());
  }

  @Override
  public Region makeNot(Region pF) {
    SymbolicRegion r = (SymbolicRegion)pF;
    return new SymbolicRegion(fmgr.makeNot(r.f));
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    SymbolicRegion r1 = (SymbolicRegion)pF1;
    SymbolicRegion r2 = (SymbolicRegion)pF2;
    return new SymbolicRegion(fmgr.makeAnd(r1.f, r2.f));
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    SymbolicRegion r1 = (SymbolicRegion)pF1;
    SymbolicRegion r2 = (SymbolicRegion)pF2;
    return new SymbolicRegion(fmgr.makeOr(r1.f, r2.f));
  }

  @Override
  public Region makeExists(Region pF1, Region pF2) {
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

}
