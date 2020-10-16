// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TAFormulaEncodingProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings.TAFormulaEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Candidate invariant that represents the formula encoding of a timed automaton network. */
public class TimedAutomatonCandidateInvariant implements CandidateInvariant {
  private final TAFormulaEncoding encoding;

  public TimedAutomatonCandidateInvariant(
      Configuration pConfig, CFA pCfa, FormulaManagerView pFmgr) {
    try {
      encoding = TAFormulaEncodingProvider.getEncoding(pConfig, pCfa, pFmgr);
    } catch (InvalidConfigurationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext) {
    return pFMGR.getBooleanFormulaManager().makeFalse();
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws InterruptedException {
    return pFMGR.getBooleanFormulaManager().makeTrue();
  }

  public Collection<BooleanFormula> getFormulas(Iterable<AbstractState> pReachedSet) {
    return encoding.getFormulaFromReachedSet(pReachedSet);
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    return;
  }

  @Override
  public String toString() {
    return "No target locations reachable";
  }

  @Override
  public boolean appliesTo(CFANode pLocation) {
    return true;
  }

  @Override
  public FluentIterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    throw new UnsupportedOperationException();
  }
}
