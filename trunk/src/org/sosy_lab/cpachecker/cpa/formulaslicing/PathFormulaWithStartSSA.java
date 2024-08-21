// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Objects;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public final class PathFormulaWithStartSSA {
  private final PathFormula pathFormula;
  private final SSAMap startMap;

  public PathFormulaWithStartSSA(PathFormula pPathFormula, SSAMap pStartMap) {
    pathFormula = pPathFormula;
    startMap = pStartMap;
  }

  public SSAMap getStartMap() {
    return startMap;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    PathFormulaWithStartSSA that = (PathFormulaWithStartSSA) pO;
    return Objects.equals(pathFormula, that.pathFormula) && Objects.equals(startMap, that.startMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pathFormula, startMap);
  }
}
