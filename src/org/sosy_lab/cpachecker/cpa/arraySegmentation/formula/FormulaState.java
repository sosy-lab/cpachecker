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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.formula;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Auxilliary CPA-Abstract-State that contains a partially computed formula used for
 * Non-interference evaluation
 */
public class FormulaState implements AbstractState, Cloneable, Serializable,
    LatticeAbstractState<FormulaState>, Graphable {

  private static final long serialVersionUID = -8064100702131561328L;

  /**
   * The path formula for the path from the last abstraction node to this node. it is set to true on
   * a new abstraction location and updated with a new non-abstraction location
   */
  protected PathFormula path1;

  private FormulaRelation pr;

  public FormulaRelation getPr() {
    return pr;
  }

  public SSAMap whilebefore = null;
  public PathFormula formulabefore = null;

  // protected SSAMap whileafter=null;

  public FormulaState(PathFormula pPath1, FormulaRelation pPr) {
    this.path1 = pPath1;

    this.pr = pPr;
  }

  public PathFormula getPathFormula() {

    return path1;

  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    // sb.append("\\n");
    // sb.append("[Formula]=");
    // sb.append(path1.getFormula().toString());
    // sb.append("\\n");
    // sb.append("[Formula2]=");
    // sb.append(path2.toString());
    // sb.append("\\n");
    sb.append("}");
    // sb.append("\\n");
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public boolean isUnsat() {
    BooleanFormula bf1 = this.path1.getFormula();
    Solver solver = pr.getSolver();
    boolean result = false;
    try {
      result = solver.isUnsat(bf1);
    } catch (SolverException e) {
      // TODO Auto-generated catch block

    } catch (InterruptedException e) {
      // TODO Auto-generated catch block

    }
    return result;
  }

  public boolean isCovered(FormulaState pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null) {
      return false;
    }
    // TaggedFormulaManager fm = pr.getFormulaManager();
    // BooleanFormulaManagerView bm = fm.getBooleanFormulaManager();
    BooleanFormula bf1 = this.path1.getFormula();
    BooleanFormula bf2 = pOther.path1.getFormula();
    Solver solver = pr.getSolver();
    boolean result = false;
    try {
      // boolean result1=solver.isUnsat(bf1);
      // boolean result2=solver.isUnsat(bf2);

      result = solver.implies(bf1, bf2);

    } catch (SolverException e) {
      // TODO Auto-generated catch block

    } catch (InterruptedException e) {
      // TODO Auto-generated catch block

    }

    return result;
  }

  public boolean isEqual(FormulaState pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null) {
      return false;
    }
    return this.isCovered(pOther) && pOther.isCovered(this);
  }

  public PathFormula formulajoin(PathFormula p1, PathFormula q1) {
    FormulaManagerView fm = pr.getFormulaManager();
    // BooleanFormulaManagerView bm = fm.getBooleanFormulaManager();

    List<MapsDifference.Entry<String, Integer>> symbolDifferences1 = new ArrayList<>();
    SSAMap newmap1 =
        SSAMap.merge(
            p1.getSsa(),
            q1.getSsa(),
            MapsDifference.collectMapsDifferenceTo(symbolDifferences1));

    BooleanFormula p1add = p1.getFormula();
    BooleanFormula q1add = q1.getFormula();
    int length1 = p1.getLength();
    int length2 = q1.getLength();

    for (MapsDifference.Entry<String, Integer> symbolDifference : symbolDifferences1) {
      String v = symbolDifference.getKey();
      int index1 = symbolDifference.getLeftValue().orElse(1);
      int index2 = symbolDifference.getRightValue().orElse(1);
      if (index1 < index2) {
        p1add = fm.makeAnd(p1add, pr.makeEqual(v, v, index2, index1));
        length1++;
      }
      if (index1 > index2) {
        q1add = fm.makeAnd(q1add, pr.makeEqual(v, v, index1, index2));
        length2++;
      }
    }

    BooleanFormula newp1 = fm.makeOr(p1add, q1add);

    PathFormula newpf =
        new PathFormula(
            newp1,
            newmap1,
            PointerTargetSet.emptyPointerTargetSet(),
            Math.max(length1, length2));
    return newpf;
  }

  @Override
  public FormulaState join(FormulaState pOther) {
    // Strongest Post Condition
    FormulaState merge;

    // TaggedFormulaManager fm = pr.getFormulaManager();
    // BooleanFormulaManagerView bm = fm.getBooleanFormulaManager();

    if (isCovered(pOther) && pOther.isCovered(this)) {
      return pOther;
    }

    PathFormula p1 = this.getPathFormula();

    PathFormula q1 = pOther.getPathFormula();
    PathFormula newpf = formulajoin(p1, q1);


    merge = new FormulaState(newpf, pr);

    if (this.formulabefore != null && pOther.formulabefore != null) {
      merge.formulabefore = formulajoin(this.formulabefore, pOther.formulabefore);

      SSAMap map1 = merge.formulabefore.getSsa();
      SSAMapBuilder mb1 = map1.builder();
      SSAMap newmap1 = mb1.build();
      merge.whilebefore = newmap1;
    } else {
      if (this.formulabefore != null) {
        merge.whilebefore = this.whilebefore;
        merge.formulabefore = this.formulabefore;

      } else {
        merge.whilebefore = pOther.whilebefore;
        merge.formulabefore = pOther.formulabefore;

      }
    }

    return merge;
  }

  @Override
  public boolean isLessOrEqual(FormulaState pOther) throws CPAException, InterruptedException {
    boolean result = this.isCovered(pOther);
    return result;
  }

  @Override
  public String toString() {
    return path1.toString();

  }

  @Override
  public FormulaState clone() {
    try {
      super.clone();
    } catch (CloneNotSupportedException e) {
    }

    FormulaState result = new FormulaState(path1, pr);
    result.whilebefore = this.whilebefore;
    result.formulabefore = this.formulabefore;
    return result;
  }

  @Override
  public int hashCode() {
    return Objects.hash(formulabefore, path1, whilebefore);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FormulaState other = (FormulaState) obj;
    return Objects.equals(formulabefore, other.formulabefore)
        && Objects.equals(path1, other.path1);
  }


}
