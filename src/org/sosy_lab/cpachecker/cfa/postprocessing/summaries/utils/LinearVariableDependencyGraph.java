// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;

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
}
