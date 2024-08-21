// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.translators;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

public class PredicateRequirementsTranslator
    extends AbstractRequirementsTranslator<PredicateAbstractState> {

  private final FormulaManagerView fmgr;
  private int counter;

  public PredicateRequirementsTranslator(FormulaManagerView pFmgr) {
    super(PredicateAbstractState.class);
    fmgr = pFmgr;
    counter = 0;
  }

  @Override
  protected Pair<List<String>, String> convertToFormula(
      final PredicateAbstractState pRequirement,
      final SSAMap pIndices,
      final @Nullable Collection<String> pRequiredVars)
      throws CPAException {

    if (!pRequirement.isAbstractionState()) {
      throw new CPAException(
          "The PredicateAbstractState "
              + pRequirement
              + " is not an abstractionState. Ensure that property"
              + " cpa.predicate.blk.alwaysAtExplicitNodes is set to true");
    }

    BooleanFormula formulaBool;
    if (pRequiredVars == null) {
      formulaBool = fmgr.instantiate(pRequirement.getAbstractionFormula().asFormula(), pIndices);
    } else {
      formulaBool =
          fmgr.instantiate(
              removeUnnecessaryClauses(
                  pRequirement.getAbstractionFormula().asFormula(),
                  getRelevantVariables(
                      pRequirement.getAbstractionFormula().asFormula(), pRequiredVars)),
              pIndices);
    }
    Pair<String, List<String>> pair = PredicatePersistenceUtils.splitFormula(fmgr, formulaBool);
    List<String> list = new ArrayList<>(pair.getSecond());
    List<String> removeFromList = new ArrayList<>();
    for (String stmt : list) {
      if (!stmt.startsWith("(declare") && !stmt.startsWith("(define")) {
        removeFromList.add(stmt);
      }
    }
    list.removeAll(removeFromList);

    String secReturn;
    String element = pair.getFirst();
    // element =(assert ...)
    element = element.substring(element.indexOf('t') + 1, element.length() - 1);
    secReturn = "(define-fun .defci" + counter++ + " () Bool " + element + ")";

    return Pair.of(list, secReturn);
  }

  private Set<String> getRelevantVariables(
      final BooleanFormula f, final Collection<String> pRequiredVars) {
    Map<String, Collection<String>> varDependentOn = new HashMap<>();

    Set<String> vars;
    for (BooleanFormula atom : fmgr.extractAtoms(f, false)) {
      vars = fmgr.extractVariableNames(atom);
      for (String var : vars) {
        if (!varDependentOn.containsKey(var)) {
          varDependentOn.put(var, new HashSet<>());
        }
        varDependentOn.get(var).addAll(vars);
      }
    }

    Set<String> relevantVars = new HashSet<>(pRequiredVars);
    boolean changed = true;

    while (changed) {
      changed = false;
      for (Entry<String, Collection<String>> dep : varDependentOn.entrySet()) {
        if (relevantVars.contains(dep.getKey()) && relevantVars.addAll(dep.getValue())) {
          changed = true;
        }
      }
    }

    return relevantVars;
  }

  private BooleanFormula removeUnnecessaryClauses(
      final BooleanFormula fOrigin, final Set<String> pRequiredVars) {
    RelevantFormulaDetector relevantFormulaDetector = new RelevantFormulaDetector(pRequiredVars);
    fmgr.visitRecursively(fOrigin, relevantFormulaDetector);
    if (relevantFormulaDetector.detectionSuccessful()) {
      return relevantFormulaDetector.getRelevantFormula();
    }
    return fOrigin;
  }

  private class RelevantFormulaDetector extends DefaultFormulaVisitor<TraversalProcess> {
    private final Set<BooleanFormula> clauses = new HashSet<>();
    private final Set<String> relevantVars;
    private boolean success = true;

    public RelevantFormulaDetector(final Set<String> pRelevantVars) {
      relevantVars = pRelevantVars;
    }

    public BooleanFormula getRelevantFormula() {
      Preconditions.checkState(success);
      if (clauses.isEmpty()) {
        return fmgr.getBooleanFormulaManager().makeTrue();
      }
      return fmgr.getBooleanFormulaManager().and(clauses);
    }

    public boolean detectionSuccessful() {
      return success;
    }

    @Override
    protected TraversalProcess visitDefault(final Formula pFormula) {
      if (!(pFormula instanceof BooleanFormula)) {
        success = false;
        return TraversalProcess.ABORT;
      }

      clauses.add((BooleanFormula) pFormula);
      return TraversalProcess.SKIP;
    }

    @Override
    public TraversalProcess visitFunction(
        final Formula pFormula, final List<Formula> args, final FunctionDeclaration<?> decl) {
      if (decl.getKind().equals(FunctionDeclarationKind.AND)) {
        return TraversalProcess.CONTINUE;
      }

      if (!Sets.intersection(fmgr.extractVariableNames(pFormula), relevantVars).isEmpty()) {
        if (!(pFormula instanceof BooleanFormula)) {
          success = false;
          return TraversalProcess.ABORT;
        }

        clauses.add((BooleanFormula) pFormula);
      }

      return TraversalProcess.SKIP;
    }
  }
}
