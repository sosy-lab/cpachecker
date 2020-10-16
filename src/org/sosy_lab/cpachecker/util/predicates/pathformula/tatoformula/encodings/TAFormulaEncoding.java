// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface TAFormulaEncoding {
  BooleanFormula getInitialFormula(CFANode initialNode, int pInitialIndex);

  Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula predecessor, int pLastReachedIndex, CFAEdge edge);

  Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula predecessor, int pLastReachedIndex);

  Collection<BooleanFormula> getFormulaFromReachedSet(Iterable<AbstractState> pReachedSet);
}
