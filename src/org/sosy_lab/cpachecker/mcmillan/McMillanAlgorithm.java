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
package org.sosy_lab.cpachecker.mcmillan;

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.DefaultInterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;

import com.google.common.collect.Lists;

public class McMillanAlgorithm implements Algorithm {

  private final LogManager logger;

  private final ExtendedFormulaManager fmgr;
  private final PathFormulaManager pfmgr;
  private final TheoremProver prover;
  private final InterpolationManager<Formula> imgr;

  private final List<Vertex> allNodes = new ArrayList<Vertex>(); // chronologically sorted

  public McMillanAlgorithm(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    logger = pLogger;

    MathsatFormulaManager mfmgr = MathsatFactory.createFormulaManager(config, logger);
    fmgr = new ExtendedFormulaManager(mfmgr, config, logger);
    pfmgr = new CachingPathFormulaManager(new PathFormulaManagerImpl(fmgr, config, logger));
    prover = new MathsatTheoremProver(mfmgr);
    imgr = new DefaultInterpolationManager(fmgr, pfmgr, prover, config, logger);

    prover.init();
  }

  public AbstractElement getInitialElement(CFANode location) {
    Vertex root = new Vertex(location, fmgr.makeTrue(), pfmgr.makeEmptyPathFormula());
    allNodes.add(root);
    return root;
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    unwind(pReachedSet);
    return true;
  }

  private void expand(Vertex v) throws CPATransferException {
    if (v.isLeaf() && !v.isCovered()) {
      CFANode loc = v.getLocation();
      for (CFAEdge edge : leavingEdges(loc)) {

        PathFormula newPathFormula = pfmgr.makeAnd(v.getPathFormula(), edge);
        Vertex w = new Vertex(v, edge.getSuccessor(), fmgr.makeTrue(), newPathFormula);
        allNodes.add(w);
      }
    }
  }

  private boolean refine(final Vertex v) throws CPAException, InterruptedException {
    if (v.isTarget() && !v.getStateFormula().isFalse()) {

      logger.log(Level.INFO, "Refinement on " + v);

      // build list of path elements/formulas in bottom-to-top order and reverse
      List<Vertex> path = new ArrayList<Vertex>();
      List<Formula> pathFormulas = new ArrayList<Formula>();
      {
        Vertex w = v;
        while (w.hasParent()) {
          pathFormulas.add(w.getPathFormula().getFormula());
          path.add(w);
          w = w.getParent();
        }
        path.add(w); // this is the root element of the ART
        // ignore path formula of root element, it is "true"
      }
      path = Lists.reverse(path);
      pathFormulas = Lists.reverse(pathFormulas);

      CounterexampleTraceInfo<Formula> cex = imgr.buildCounterexampleTrace(pathFormulas, Collections.<ARTElement>emptySet());

      if (!cex.isSpurious()) {
        return false; // real counterexample
      }

      logger.log(Level.INFO, "Refinement successful");

      path = path.subList(1, path.size()-1); // skip first and last element, itp is always true/false there
      assert cex.getPredicatesForRefinement().size() ==  path.size();

      for (Pair<Formula, Vertex> interpolationPoint : Pair.zipList(cex.getPredicatesForRefinement(), path)) {
        Formula itp = interpolationPoint.getFirst();
        Vertex w = interpolationPoint.getSecond();

        if (itp.isTrue()) {
          continue;
        }

        Formula stateFormula = w.getStateFormula();
        if (!implies(stateFormula, itp)) {
          w.setStateFormula(fmgr.makeAnd(stateFormula, itp)); // automatically uncovers nodes as needed
        }
      }

      // itp of last element is always false, set it
      v.setStateFormula(fmgr.makeFalse());
    }
    return true;
  }

  private void cover(Vertex v, Vertex w) {
    if (v.getLocation().equals(w.getLocation())
        && !v.isCovered()
        && !w.isCovered() // ???
        && !v.isAncestorOf(w)) {

      if (implies(v.getStateFormula(), w.getStateFormula())) {
        for (Vertex y : v.getSubtree()) {
          y.cleanCoverage();
        }
        v.setCoveredBy(w);
      }
    }
  }

  private void close(Vertex v) {
    for (Vertex w : allNodes) {
      if (w.isOlderThan(v) && w.getLocation().equals(v.getLocation())) {
        cover(v, w);
      }
    }
  }

  private boolean dfs(Vertex v) throws CPAException, InterruptedException {
    close(v);
    if (!v.isCovered()) {
      if (v.isTarget()) {
        if (!refine(v)) {
          return false;
        }

        Vertex w = v;
        close(w);
        while (w.hasParent()) {
          w = w.getParent();
          close(w);
        }
      }
      expand(v);
      for (Vertex w : v.getChildren()) {
        dfs(w);
      }
    }
    return true;
  }

  private void unwind(ReachedSet pReached) throws CPAException, InterruptedException {

    outer:
    while (true) {
      for (Vertex v : allNodes) {
        if (v.isLeaf() && !v.isCovered()) {

          Vertex w = v;
          while (w.hasParent()) {
            w = w.getParent();
            close(w);
          }

          if (!dfs(v)) {
            logger.log(Level.INFO, "Bug found");
            break outer;
          }

          continue outer;
        }
      }
      break outer;
    }

    for (Vertex v : allNodes) {
      pReached.add(v, SingletonPrecision.getInstance());
      pReached.popFromWaitlist();
    }
  }

  private boolean implies(Formula a, Formula b) {
    Formula f = fmgr.makeNot(fmgr.makeImplication(a, b));

    return prover.isUnsat(f);
  }
}
