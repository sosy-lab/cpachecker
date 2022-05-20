// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.translators;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public abstract class AbstractRequirementsTranslator<T extends AbstractState> {

  private final Class<T> abstractStateClass;

  protected AbstractRequirementsTranslator(final Class<T> pAbstractStateClass) {
    abstractStateClass = pAbstractStateClass;
  }

  protected T extractRequirement(final AbstractState pState) {
    return AbstractStates.extractStateByType(pState, abstractStateClass);
  }

  protected Collection<T> extractRequirements(final Collection<AbstractState> pStates) {
    Collection<T> requirements = new ArrayList<>(pStates.size());
    for (AbstractState state : pStates) {
      requirements.add(extractRequirement(state));
    }
    return requirements;
  }

  protected abstract Pair<List<String>, String> convertToFormula(final T requirement, final SSAMap indices, final @Nullable Collection<String> pRequiredVars)
      throws CPAException;

  public Pair<Pair<List<String>, String>, Pair<List<String>, String>> convertRequirements(
      final AbstractState pre, final Collection<? extends AbstractState> post, final SSAMap postIndices,
      final @Nullable Collection<String> pInputVariables, final @Nullable Collection<String> pOutputVariables)
          throws CPAException {

    Pair<List<String>, String> formulaPre = convertToFormula(extractRequirement(pre), SSAMap.emptySSAMap(), pInputVariables);
    formulaPre = Pair.of(formulaPre.getFirst(), renameDefine(formulaPre.getSecond(), "pre"));

    if (post.isEmpty()) {
      return Pair.of(formulaPre, Pair.of(ImmutableList.of(), "(define-fun post () Bool false)"));
    }

    List<String> list = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    String definition;
    Pair<List<String>, String> formula;
    int BracketCounter = 0;
    int amount = post.size();
    int index;

    sb.append("(define-fun post () Bool ");

    for (AbstractState state : post){
      formula = convertToFormula(extractRequirement(state), postIndices, pOutputVariables);
      list.addAll(formula.getFirst());

      if (BracketCounter != amount-1) {
        sb.append("(or ");
        BracketCounter++;
      }
      // distinguish between (define-fun name () Bool (f)) and (define-fun name () Bool var)
      index = formula.getSecond().indexOf("(", formula.getSecond().indexOf(")"));
      if (index>0){
        definition = formula.getSecond().substring(index, formula.getSecond().length()-1);
      } else {
        definition = formula.getSecond().substring(formula.getSecond().lastIndexOf(" "), formula.getSecond().length()-1);
      }
      sb.append(definition);
    }
    for (int i=0; i<BracketCounter+1; i++) {
      sb.append(")");
    }

    return Pair.of(formulaPre, Pair.of(list, sb.toString()));
  }

  public static String renameDefine(final String define, final String newName) {
    int start = define.indexOf(" ") + 1;
    int end = define.indexOf(" ", start);
    return define.substring(0, start) + newName + define.substring(end);
  }
}
