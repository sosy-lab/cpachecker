// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAEncodingExtensionWrapper implements TAEncodingExtension {
  private final Iterable<TAEncodingExtension> extensions;
  private final BooleanFormulaManagerView bFmgr;

  public TAEncodingExtensionWrapper(
      FormulaManagerView pFmgr, Iterable<TAEncodingExtension> pExtensions) {
    bFmgr = pFmgr.getBooleanFormulaManager();
    extensions = pExtensions;
  }

  private BooleanFormula combineExtensions(
      Function<TAEncodingExtension, BooleanFormula> encodingFunction) {
    var extensionFormulas = from(extensions).transform(encodingFunction);
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());

    return extensionsFormula;
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return combineExtensions(ext -> makeAutomatonStep(pAutomaton, pLastReachedIndex));
  }

  @Override
  public BooleanFormula makeInitialFormula(TaDeclaration pAutomaton, int pInitialIndex) {
    return combineExtensions(ext -> makeInitialFormula(pAutomaton, pInitialIndex));
  }

  @Override
  public BooleanFormula makeFinalConditionForAutomaton(TaDeclaration pAutomaton, int pStep) {
    return combineExtensions(ext -> makeFinalConditionForAutomaton(pAutomaton, pStep));
  }

  @Override
  public BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    return combineExtensions(ext -> makeDiscreteStep(pAutomaton, pLastReachedIndex, pEdge));
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return combineExtensions(ext -> makeDelayTransition(pAutomaton, pLastReachedIndex));
  }

  @Override
  public BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return combineExtensions(ext -> makeIdleTransition(pAutomaton, pLastReachedIndex));
  }

  @Override
  public BooleanFormula makeStepFormula(int pLastReachedIndex) {
    return combineExtensions(ext -> makeStepFormula(pLastReachedIndex));
  }
}
