/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.invariants.choosers.TemplateChooser;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplatePathFormulaBuilder;

public class PathProgramNetworkBuilder implements NetworkBuilder {

  private final MutableARGPath cePath;
  private final LogManager logger;
  private final TemplatePathFormulaBuilder tpfb;
  private final SortedSet<CFANode> nodeSet;

  @SuppressWarnings("unused")
  private final Collection<Loop> loops;
  @SuppressWarnings("unused")
  private TemplateChooser chooser;
  @SuppressWarnings("unused")
  private TemplateNetwork basicTnet;

  public PathProgramNetworkBuilder(MutableARGPath pPath, LogManager pLogger) throws RefinementFailedException {
    cePath = pPath;
    logger = pLogger;
    tpfb = new TemplatePathFormulaBuilder();
    basicTnet = buildNetworkWithoutTemplates();
    nodeSet = getNodeSet();
    loops = findLoops();
  }

  private Collection<Loop> findLoops() {
    Collection<Loop> loops;
    try {
      loops = CFAUtils.findLoops(nodeSet, Language.C);
    } catch (Exception e) {
      logger.log(Level.FINEST, "While constructing path program, could not detect all loops.");
      loops = null;
    }
    return loops;
  }

  private SortedSet<CFANode> getNodeSet() {
    SortedSet<CFANode> nodes = new TreeSet<>();
    for (Pair<ARGState, CFAEdge> pair : cePath) {
      ARGState ae = pair.getFirst();
      CFANode n = AbstractStates.extractLocation(ae);
      nodes.add(n);
    }
    return nodes;
  }

  /*
   * We construct the basic "path program", as specified in
   * Dirk Beyer, T.A. Henzinger, R. Majumdar, A. Rybalchenko. "Path Invariants". PLDI'07. 2007.
   */
  private TemplateNetwork buildNetworkWithoutTemplates() {
    // Construct the Vector of transitions.
    Vector<Transition> trans = new Vector<>();

    // We use an empty template map for now.
    TemplateMap tmap = new TemplateMap();

    // Go through the counterexample path, and form all the transitions in it.
    int N = cePath.size() - 1; // we ignore last pair, since last edge is useless, hence " - 1"
    Pair<ARGState, CFAEdge> pair;
    ARGState ae1, ae2;
    CFAEdge edge;
    TemplateFormula transitionFormula;
    Location l1, l2;
    CFANode node;
    Transition tran;
    for (int i = 0; i < N; i++) {

      // Get objects from the path.
      pair = cePath.get(i);
      ae1 = pair.getFirst();
      edge = pair.getSecond();
      ae2 = cePath.get(i+1).getFirst();

      // Build locations and transition formula.
      transitionFormula = (TemplateFormula)tpfb.buildPathFormula(edge).getFormula();
      node = AbstractStates.extractLocation(ae1);
      l1 = new Location(node);
      node = AbstractStates.extractLocation(ae2);
      l2 = new Location(node);

      // Build transition.
      tran = new Transition(tmap, l1, transitionFormula, l2);
      trans.add(tran);
    }
    return new TemplateNetwork(tmap, trans);
  }

  @Override
  public TemplateNetwork nextNetwork() {
    // TODO Auto-generated method stub
    return null;
  }

}
