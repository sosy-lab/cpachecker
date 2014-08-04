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

import static com.google.common.base.Predicates.instanceOf;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;
import org.sosy_lab.cpachecker.util.invariants.GraphUtil;
import org.sosy_lab.cpachecker.util.invariants.choosers.SingleLoopTemplateChooser;
import org.sosy_lab.cpachecker.util.invariants.choosers.TemplateChooser;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplatePathFormulaBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

public class SingleLoopNetworkBuilder implements NetworkBuilder {

  private final ARGPath cePath;
  private final LogManager logger;
  private final TemplatePathFormulaBuilder tpfb;
  private final Loop loop;
  private final CFANode root;
  private final CFANode loopHead;
  private final CFANode error;
  private final PathFormula entryFormula;
  private final PathFormula loopFormulaHead;
  private final PathFormula loopFormulaTail;
  private final PathFormula loopFormula;
  private final PathFormula exitFormulaHead;
  private final PathFormula exitFormulaTail;
  private final PathFormula exitFormula;
  private final TemplateChooser chooser;

  public SingleLoopNetworkBuilder(ARGPath pPath, LogManager pLogger) throws RefinementFailedException {
    cePath = pPath;
    logger = pLogger;
    tpfb = new TemplatePathFormulaBuilder();

    // If there is just one loop, get a hold of it. Otherwise throw exception.
    loop = getSingleLoopOrDie();

    // function edges do not count as incoming edges
    Iterable<CFAEdge> incomingEdges = Iterables.filter(loop.getIncomingEdges(),
                                                       Predicates.not(instanceOf(CFunctionReturnEdge.class)));

    // Check that there is just one incoming edge into the loop,
    // and that the loop has just one loop head.
    if (Iterables.size(incomingEdges) > 1) {
      logger.log(Level.FINEST, "Could not use invariant generation for proving program safety, loop has too many incoming edges", incomingEdges);
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, cePath);
    }

    if (loop.getLoopHeads().size() > 1) {
      logger.log(Level.FINEST, "Could not use invariant generation for proving program safety, loop has too many loop heads.");
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, cePath);
    }

    // There is only one loop head. Grab it.
    loopHead = Iterables.getOnlyElement(loop.getLoopHeads());

    // Check that the loop head is unambiguous.
    assert loopHead.equals(Iterables.getOnlyElement(incomingEdges).getSuccessor());

    // At this point, we deem the program suitable for application of our method, and
    // we proceed to apply it.
    logger.log(Level.FINEST, "Constructing single loop network builder.");

    // Get root location.
    root = AbstractStates.extractLocation(cePath.getFirst().getFirst());

    // Get error location
    error = AbstractStates.extractLocation(cePath.getLast().getFirst());

    // Get entry path formula, from root node up to loop head,
    // loop formula, from loop head back to loop head, and
    // exit formula, from loop head to error location.
    entryFormula = buildEntryFormula(cePath, loopHead);

    List<PathFormula> loopFormulas = buildLoopFormulas(loop);
    loopFormula = loopFormulas.get(0);
    loopFormulaHead = loopFormulas.get(1);
    loopFormulaTail = loopFormulas.get(2);

    Pair<PathFormula, PathFormula> exitHeadAndTail = buildExitFormulaHeadAndTail(cePath, loopHead);
    exitFormulaHead = exitHeadAndTail.getFirst();
    exitFormulaTail = exitHeadAndTail.getSecond();
    exitFormula = buildExitFormula(cePath, loopHead);

    logger.log(Level.ALL, "\nEntry, loop, and exit formulas:\nEntry: ", entryFormula, "\nLoop: ", loopFormula, "\nExit: ", exitFormula);

    // Choose an invariant template for the loop head.
    chooser = buildChooser(entryFormula, loopFormula, loopFormulaHead, loopFormulaTail, exitFormula, exitFormulaHead, exitFormulaTail);

  }

  /*
   * Form a sorted set of all the CFANodes in the counterexample path.
   */
  private Loop getSingleLoopOrDie() throws RefinementFailedException {
    // If we are going to use the CFAUtils.findLoops method, we have to essentially "cut" the counterexample path
    // out of the CFA it belongs to; i.e. for each node in the path delete all those of its edges that point to
    // nodes not in the path. Otherwise CFAUtils.findLoops will have an error.
    SortedSet<CFANode> nodes = new TreeSet<>(getAllNodes());
    // Now ask CFAUtils to find any and all the loops in the counterexample path.
    try {
      Collection<Loop> loops = CFAUtils.findLoops(nodes, Language.C);

      if (loops.size() > 1) {
        // there are too many loops
        logger.log(Level.FINEST, "Could not use invariant generation for proving program safety, program has too many loops.");
        throw new RefinementFailedException(Reason.InvariantRefinementFailed, cePath);
      }

      if (loops.isEmpty()) {
        // invariant generation is unnecessary, program has no loops
        logger.log(Level.FINEST, "Could not use invariant generation for proving program safety, program has no loops.");
        throw new RefinementFailedException(Reason.InvariantRefinementFailed, cePath);
      }

      // There is just one loop. Get a hold of it.
      Loop loop = Iterables.getOnlyElement(loops);
      return loop;

    } catch (ParserException e) {
      logger.logUserException(Level.WARNING, e, "Could not analyze loop structure of program.");
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, cePath);
    }

  }

  private Set<CFANode> getAllNodes() {
    Pair<ARGState, CFAEdge> rootPair = cePath.getFirst();
    ARGState ae = rootPair.getFirst();
    CFANode root = AbstractStates.extractLocation(ae);
    return CFATraversal.dfs().collectNodesReachableFrom(root);
  }

  private PathFormula buildEntryFormula(ARGPath pPath, CFANode loopHead) {
    // gather CFAEdges until hit ARGState whose location is loopHead
    Vector<CFAEdge> edges = new Vector<>();

    for (Pair<ARGState, CFAEdge> pair : pPath) {
      ARGState ae = pair.getFirst();
      CFAEdge edge = pair.getSecond();
      CFANode loc = AbstractStates.extractLocation(ae);
      if (loc == loopHead) {
        break;
      } else {
        edges.add(edge);
      }
    }
    // build path formula for these edges
    PathFormula entryFormula = tpfb.buildPathFormula(edges);
    return entryFormula;
  }

  private List<PathFormula> buildLoopFormulas(Loop loop) {
    logger.log(Level.ALL, "Loop:\n",loop);
    Vector<CFANode> loopNodes = new Vector<>(loop.getLoopNodes());
    Vector<CFAEdge> loopEdges = GraphUtil.makeEdgeLoop(loopNodes, logger);
    logger.log(Level.ALL, "Sequence of edges in loop:\n",loopEdges);
    // head:
    Vector<CFAEdge> loopHead = new Vector<>(1);
    loopHead.add(loopEdges.get(0));
    // tail:
    Vector<CFAEdge> loopTail = new Vector<>(loopEdges.size()-1);
    loopTail.addAll(loopEdges);
    loopTail.remove(0);
    // Build path formulas.
    PathFormula loopFormula = tpfb.buildPathFormula(loopEdges);
    PathFormula loopFormulaHead = tpfb.buildPathFormula(loopHead);
    PathFormula loopFormulaTail = tpfb.buildPathFormula(loopTail);
    List<PathFormula> three = new Vector<>(3);
    three.add(loopFormula);
    three.add(loopFormulaHead);
    three.add(loopFormulaTail);
    return three;
  }

  private PathFormula buildExitFormula(ARGPath pPath, CFANode loopHead) {
    // gather CFAEdges from ARGState whose location is loopHead to end of path
    Vector<CFAEdge> edges = new Vector<>();

    boolean begun = false;
    int N = pPath.size() - 1; // we ignore last pair, since last edge is useless, hence " - 1"
    for (int i = 0; i < N; i++) {
      Pair<ARGState, CFAEdge> pair = pPath.get(i);
      if (begun) {
        CFAEdge edge = pair.getSecond();
        edges.add(edge);
      } else {
        ARGState ae = pair.getFirst();
        CFANode loc = AbstractStates.extractLocation(ae);
        if (loc == loopHead) {
          begun = true;
          CFAEdge edge = pair.getSecond();
          edges.add(edge);
        }
      }
    }
    // build path formula for these edges
    PathFormula exitFormula = tpfb.buildPathFormula(edges);
    return exitFormula;
  }

  private Pair<PathFormula, PathFormula> buildExitFormulaHeadAndTail(ARGPath pPath, CFANode loopHead) {
    // Like buildExitFormula method, only returns the formula for the exit path in two parts:
    // the "head", being the first edge, and the "tail", being the remainder of the path.
    CFAEdge headEdge = null;
    Vector<CFAEdge> tailEdges = new Vector<>();

    boolean begun = false;
    int N = pPath.size() - 1; // we ignore last pair, since last edge is useless, hence " - 1"
    for (int i = 0; i < N; i++) {
      Pair<ARGState, CFAEdge> pair = pPath.get(i);
      if (begun) {
        CFAEdge edge = pair.getSecond();
        tailEdges.add(edge);
      } else {
        ARGState ae = pair.getFirst();
        CFANode loc = AbstractStates.extractLocation(ae);
        if (loc == loopHead) {
          begun = true;
          CFAEdge edge = pair.getSecond();
          headEdge = edge;
        }
      }
    }
    // build path formula for these edges
    PathFormula headFormula = tpfb.buildPathFormula(headEdge);
    PathFormula tailFormula = tpfb.buildPathFormula(tailEdges);
    Pair<PathFormula, PathFormula> exitFormulae = Pair.<PathFormula, PathFormula>of(headFormula, tailFormula);
    return exitFormulae;
  }

  private TemplateChooser buildChooser(
      PathFormula pEntryFormula,
      PathFormula pLoopFormula, PathFormula pLoopFormulaHead, PathFormula pLoopFormulaTail,
      PathFormula pExitFormula, PathFormula pExitHead, PathFormula pExitTail) {
    // Pull the Formulas out of the PathFormulas, cast them into TemplateFormulas,
    // construct a TemplateChooser, and return it.
    TemplateFormula entryFormula = (TemplateFormula) pEntryFormula.getFormula();
    TemplateFormula loopFormula = (TemplateFormula) pLoopFormula.getFormula();
    TemplateFormula loopFormulaHead = (TemplateFormula) pLoopFormulaHead.getFormula();
    TemplateFormula loopFormulaTail = (TemplateFormula) pLoopFormulaTail.getFormula();
    TemplateFormula exitFormula = (TemplateFormula) pExitFormula.getFormula();
    TemplateFormula exitFormulaHead = (TemplateFormula) pExitHead.getFormula();
    TemplateFormula exitFormulaTail = (TemplateFormula) pExitTail.getFormula();
    TemplateChooser chooser = new SingleLoopTemplateChooser(
        logger, entryFormula,
        loopFormula, loopFormulaHead, loopFormulaTail,
        exitFormula, exitFormulaHead, exitFormulaTail);
    return chooser;
  }

  /**
   * Build the Network object for a simple loop network, with root node R and loop head L.
   * @param pRoot the root node R.
   * @param pLoopHead the loop head node L.
   * @param pInvTemp the invariant template to put at L.
   * @param pEntryFormula the path formula from R to L.
   * @param pLoopFormula the path formula from L back to L.
   * @return
   */
  private TemplateNetwork buildSimpleLoopNetwork(CFANode pRoot, CFANode pLoopHead, CFANode pError,
      Template pInvTemp, PathFormula pEntryFormula, PathFormula pLoopFormula,
      PathFormula pExitFormula) {

    // Locations:
    Location root = new Location(pRoot);
    Location loopHead = new Location(pLoopHead);
    Location error = new Location(pError);

    // Template map:
    TemplateMap tmap = new TemplateMap();
    tmap.put(root, Template.makeTrueTemplate());
    tmap.put(loopHead, pInvTemp);
    tmap.put(error, Template.makeFalseTemplate());

    // Path formulas:
    TemplateFormula entryFormula = (TemplateFormula)pEntryFormula.getFormula();
    TemplateFormula loopFormula = (TemplateFormula)pLoopFormula.getFormula();
    TemplateFormula exitFormula = (TemplateFormula)pExitFormula.getFormula();

    // Transitions:
    Transition entryTrans = new Transition(tmap, root, entryFormula, loopHead);
    Transition loopTrans = new Transition(tmap, loopHead, loopFormula, loopHead);
    Transition exitTrans = new Transition(tmap, loopHead, exitFormula, error);

    // Construct and return program.
    TemplateNetwork tnet = new TemplateNetwork(tmap, entryTrans, loopTrans, exitTrans);
    return tnet;

  }

  @Override
  public TemplateNetwork nextNetwork() {
    Template invTemp = chooser.chooseNextTemplate();
    // If we have run out of templates, then return the null network.
    if (invTemp == null) { return null; }

    logger.log(Level.ALL, "\nChosen invariant template for loop head:\n", invTemp);

    TemplateNetwork tnet = buildSimpleLoopNetwork(root, loopHead, error, invTemp, entryFormula, loopFormula, exitFormula);
    return tnet;
  }

}
