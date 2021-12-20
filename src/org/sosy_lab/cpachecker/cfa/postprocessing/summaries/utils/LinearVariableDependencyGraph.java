// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.util.Pair;

public class LinearVariableDependencyGraph {

  private List<LinearVariableDependency> variableDependencies = new ArrayList<>();

  public LinearVariableDependencyGraph() {}

  public LinearVariableDependencyGraph(LinearVariableDependency pVariableDependency) {
    this.putDependency(pVariableDependency);
  }

  public LinearVariableDependencyGraph(List<LinearVariableDependency> pVariableDependencies) {
    for (LinearVariableDependency l : pVariableDependencies) {
      this.putDependency(l);
    }
  }

  public LinearVariableDependencyGraph(Set<LinearVariableDependency> pVariableDependencies) {
    for (LinearVariableDependency l : pVariableDependencies) {
      this.putDependency(l);
    }
  }

  public List<LinearVariableDependency> getDependencies() {
    return this.variableDependencies;
  }

  private Optional<Integer> findDependencyIndex(LinearVariableDependency pVariableDependency) {
    for (Integer i = 0; i < this.variableDependencies.size(); i++) {
      if (pVariableDependency.getDependentVariable()
          == this.variableDependencies.get(i).getDependentVariable()) {
        return Optional.of(i);
      }
    }
    return Optional.empty();
  }

  private Optional<LinearVariableDependency> findDependency(
      LinearVariableDependency pVariableDependency) {
    for (LinearVariableDependency l : this.variableDependencies) {
      if (pVariableDependency.getDependentVariable() == l.getDependentVariable()) {
        return Optional.of(pVariableDependency);
      }
    }
    return Optional.empty();
  }

  public void putDependency(LinearVariableDependency pVariableDependency) {
    // TODO: Improve this to make all the things consistent by adding the variables which appear in
    // the dependency
    Optional<Integer> optionalIndex = findDependencyIndex(pVariableDependency);
    if (optionalIndex.isPresent()) {
      this.variableDependencies.set(optionalIndex.get(), pVariableDependency);
    } else {
      this.variableDependencies.add(pVariableDependency);
    }
  }

  public boolean modifyDependencies(
      LinearVariableDependency pLinearVariableDependency, ABinaryOperator pOperator) {
    Optional<LinearVariableDependency> existingLinearVariableDependency =
        findDependency(pLinearVariableDependency);
    if (existingLinearVariableDependency.isPresent()) {
      return existingLinearVariableDependency
          .get()
          .modifyDependency(pLinearVariableDependency, pOperator);
    } else {
      this.putDependency(pLinearVariableDependency);
      return true;
    }
  }

  public LinearVariableDependencyMatrix asMatrix() {
    List<AVariableDeclaration> variableOrdering = new ArrayList<>();

    // Order the variables in order to construct the Matrix according to the length of the
    // dependencies
    List<Pair<LinearVariableDependency, Integer>> dependeciesAndSize = new ArrayList<>();
    for (LinearVariableDependency s : variableDependencies) {
      dependeciesAndSize.add(Pair.of(s, s.getVariableDependencies().getFirst().keySet().size()));
    }

    dependeciesAndSize.sort(Comparator.comparing(p1 -> p1.getSecond()));
    for (Pair<LinearVariableDependency, Integer> d : dependeciesAndSize) {
      variableOrdering.add(d.getFirst().getDependentVariable());
    }

    return new LinearVariableDependencyMatrix(this, variableOrdering);
  }

}
