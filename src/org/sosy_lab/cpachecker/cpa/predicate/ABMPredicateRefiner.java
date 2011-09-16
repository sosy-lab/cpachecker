/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.abm.AbstractABMBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.collect.Lists;


/**
 * Implements predicate refinements when using ABM.
 * It is based on the {@link AbstractABMBasedRefiner} and delegates the work to
 * a {@link ExtendedPredicateRefiner}, which is a small extension of the regular
 * {@link PredicateRefiner}.
 *
 * So the hierarchy is as follows:
 *
 *               AbstractARTBasedRefiner
 *                         ^
 *                         |
 *           +-------------+-------------+
 *           |                           |
 * AbstractABMBasedRefiner   AbstractInterpolationBasedRefiner
 *           ^                           ^
 *           |                           |
 *           |                    PredicateRefiner
 *           |                           ^
 *           |                           |
 *   ABMPredicateRefiner ---> ExtendedPredicateRefiner
 *
 * Here ^ means inheritance and -> means reference.
 */
public final class ABMPredicateRefiner extends AbstractABMBasedRefiner {

  private final PredicateRefiner refiner;

  public ABMPredicateRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
    this.refiner = new ExtendedPredicateRefiner(pCpa);
  }

  @Override
  protected final CounterexampleInfo performRefinement0(ARTReachedSet pReached, Path pPath)
      throws CPAException, InterruptedException {

    return refiner.performRefinement(pReached, pPath);
  }

  /**
   * This is a small extension of PredicateRefiner that overrides
   * {@link #getFormulasForPath(List, ARTElement)} so that it respects ABM.
   */
  static final class ExtendedPredicateRefiner extends PredicateRefiner {

    final Timer ssaRenamingTimer = new Timer();

    private final PathFormulaManager pfmgr;

    private ExtendedPredicateRefiner(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
      super(pCpa);

      this.pfmgr = predicateCpa.getPathFormulaManager();

      if (!(predicateCpa instanceof ABMPredicateCPA)) {
        throw new InvalidConfigurationException(ABMPredicateRefiner.class.getSimpleName() + " needs an ABMPredicateCPA");
      }
      ((ABMPredicateCPA)predicateCpa).getABMStats().addRefiner(this);
    }

    @Override
    protected final CounterexampleInfo performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException, InterruptedException {
      CounterexampleInfo counterexample = super.performRefinement(pReached, pPath);

      if (counterexample.isSpurious()) {
        lastErrorPath = null; // TODO why this?
      }

      return counterexample;
    }

    @Override
    protected final List<Formula> getFormulasForPath(List<Pair<ARTElement, CFANode>> pPath, ARTElement initialElement) throws CPATransferException {
      // the elements in the path are not expanded, so they contain the path formulas
      // with the wrong indices
      // we need to re-create all path formulas in the flattened ART

      ssaRenamingTimer.start();
      try {
        return computeBlockFormulas(initialElement);

      } finally {
        ssaRenamingTimer.stop();
      }
    }

    private List<Formula> computeBlockFormulas(ARTElement pRoot) throws CPATransferException {

      Map<ARTElement, PathFormula> formulas = new HashMap<ARTElement, PathFormula>();
      List<Formula> abstractionFormulas = Lists.newArrayList();
      Deque<ARTElement> todo = new ArrayDeque<ARTElement>();

      // initialize
      assert pRoot.getParents().isEmpty();
      formulas.put(pRoot, pfmgr.makeEmptyPathFormula());
      todo.addAll(pRoot.getChildren());

      // iterate over all elements in the ART with BFS
      outer: while (!todo.isEmpty()) {
        ARTElement currentElement = todo.pollFirst();
        if (formulas.containsKey(currentElement)) {
          continue; // already handled
        }

        // collect formulas for current location
        List<PathFormula> currentFormulas = Lists.newArrayListWithExpectedSize(currentElement.getParents().size());
        for (ARTElement parentElement : currentElement.getParents()) {
          PathFormula parentFormula = formulas.get(parentElement);
          if (parentFormula == null) {
            // parent not handled yet, re-queue current element
            todo.addLast(currentElement);
            continue outer;

          } else {
            CFAEdge edge = parentElement.getEdgeToChild(currentElement);
            PathFormula currentFormula = pfmgr.makeAnd(parentFormula, edge);
            currentFormulas.add(currentFormula);
          }
        }
        assert currentFormulas.size() >= 1;

        PredicateAbstractElement predicateElement = extractElementByType(currentElement, PredicateAbstractElement.class);
        if (predicateElement.isAbstractionElement()) {
          // abstraction element
          PathFormula currentFormula = getOnlyElement(currentFormulas);
          abstractionFormulas.add(currentFormula.getFormula());

          // start new block with empty formula
          assert todo.isEmpty() : "todo should be empty because of the special ART structure";
          formulas.clear(); // free some memory

          formulas.put(currentElement, pfmgr.makeEmptyPathFormula(currentFormula));

        } else {
          // merge the formulas
          Iterator<PathFormula> it = currentFormulas.iterator();
          PathFormula currentFormula = it.next();
          while (it.hasNext()) {
            currentFormula = pfmgr.makeOr(currentFormula, it.next());
          }

          formulas.put(currentElement, currentFormula);
        }

        todo.addAll(currentElement.getChildren());
      }
      return abstractionFormulas;
    }
  }
}