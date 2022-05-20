// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicateAbstractionStrategy implements AbstractionStrategy {

  private final Map<PredicateAbstractionManager, SetMultimap<CFANode, AbstractionPredicate>>
      precision = new LinkedHashMap<>();

  private final Multimap<PredicateAbstractionManager, AbstractionPredicate> globalPrecision =
      LinkedHashMultimap.create();

  private final SetMultimap<FormulaType<Formula>, Formula> seenVariables =
      LinkedHashMultimap.create();

  private final Optional<VariableClassification> variableClassification;

  public PredicateAbstractionStrategy(Optional<VariableClassification> pVarClassification) {
    variableClassification = Objects.requireNonNull(pVarClassification);
  }

  private Multimap<CFANode, AbstractionPredicate> getPrecision(PredicateAbstractionManager pPam) {
    SetMultimap<CFANode, AbstractionPredicate> pamPrecision = precision.get(pPam);
    if (pamPrecision == null) {
      pamPrecision = HashMultimap.create();
      precision.put(pPam, pamPrecision);
    }
    return pamPrecision;
  }

  private Collection<AbstractionPredicate> getPrecision(
      PredicateAbstractionManager pPam, CFANode pLocation) {
    Collection<AbstractionPredicate> localPrec = getPrecision(pPam).get(pLocation);
    Collection<AbstractionPredicate> globalPrec = globalPrecision.get(pPam);
    if (globalPrec.isEmpty()) {
      return localPrec;
    }
    if (localPrec.isEmpty()) {
      return globalPrec;
    }
    List<AbstractionPredicate> combined = new ArrayList<>(localPrec.size() + globalPrec.size());
    combined.addAll(localPrec);
    combined.addAll(globalPrec);
    return combined;
  }

  @Override
  public BooleanFormula performAbstraction(
      PredicateAbstractionManager pPam, CFANode pLocation, BooleanFormula pFormula)
      throws InterruptedException, SolverException {
    return pPam.computeAbstraction(pFormula, getPrecision(pPam, pLocation));
  }

  @Override
  public void refinePrecision(
      PredicateAbstractionManager pPam, CFANode pLocation, Iterable<BooleanFormula> pPredicates) {
    Multimap<CFANode, AbstractionPredicate> pamPrecision = getPrecision(pPam);
    for (BooleanFormula pPredicate : pPredicates) {
      pamPrecision.put(pLocation, pPam.getPredicateFor(pPredicate));
    }
  }

  @Override
  public void refinePrecision(
      PredicateAbstractionManager pPam, Iterable<BooleanFormula> pPredicates) {
    for (BooleanFormula pPredicate : pPredicates) {
      globalPrecision.put(pPam, pPam.getPredicateFor(pPredicate));
    }
  }

  @Override
  public void refinePrecision(
      PredicateAbstractionManager pPam,
      CFANode pLocation,
      FormulaManagerView pFMGR,
      Set<Formula> pVariables) {
    if (pVariables.isEmpty()) {
      return;
    }
    Multimap<CFANode, AbstractionPredicate> pamPrecision = getPrecision(pPam);
    for (Formula variable : pVariables) {
      FormulaType<Formula> variableType = pFMGR.getFormulaType(variable);
      Set<Formula> seenVariablesOfSameType = seenVariables.get(variableType);
      if (!seenVariablesOfSameType.contains(variable)) {
        String variableName = pFMGR.extractVariableNames(variable).iterator().next();
        for (Formula previouslySeenVariable : seenVariablesOfSameType) {
          String previouslySeenVariableName =
              pFMGR.extractVariableNames(previouslySeenVariable).iterator().next();
          if (isLeqRelevant(variableName, previouslySeenVariableName)) {
            BooleanFormula leq = pFMGR.makeLessOrEqual(variable, previouslySeenVariable, true);
            pamPrecision.put(pLocation, pPam.getPredicateFor(leq));
            BooleanFormula geq = pFMGR.makeGreaterOrEqual(variable, previouslySeenVariable, true);
            pamPrecision.put(pLocation, pPam.getPredicateFor(geq));
          }
        }
        seenVariables.put(variableType, variable);
      }
    }
  }

  private boolean isLeqRelevant(String pVar1, String pVar2) {
    if (!variableClassification.isPresent()) {
      return true;
    }
    VariableClassification varClassification = variableClassification.orElseThrow();
    Set<String> intAddVars = varClassification.getIntAddVars();
    if (intAddVars.contains(pVar1) && intAddVars.contains(pVar2)) {
      return true;
    }
    for (Partition partition :
        Iterables.concat(
            varClassification.getIntEqualPartitions(), varClassification.getIntBoolPartitions())) {
      Set<String> partitionVariables = partition.getVars();
      if (partitionVariables.contains(pVar1)) {
        return partitionVariables.contains(pVar2);
      }
      if (partitionVariables.contains(pVar2)) {
        return partitionVariables.contains(pVar1);
      }
    }
    return true;
  }
}
