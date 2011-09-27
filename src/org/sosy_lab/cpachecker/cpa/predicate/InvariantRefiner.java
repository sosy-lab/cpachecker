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

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.AbstractARTBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.CFA.Loop;
import org.sosy_lab.cpachecker.util.invariants.GraphUtil;
import org.sosy_lab.cpachecker.util.invariants.balancer.Balancer;
import org.sosy_lab.cpachecker.util.invariants.balancer.Location;
import org.sosy_lab.cpachecker.util.invariants.balancer.TemplateMap;
import org.sosy_lab.cpachecker.util.invariants.balancer.TemplateNetwork;
import org.sosy_lab.cpachecker.util.invariants.balancer.Transition;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateChooser;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConstraint;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormulaManager;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplatePathFormulaBuilder;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTrue;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateChooser.TemplateChooserStrategy;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.predicate.refinement")
public class InvariantRefiner extends AbstractARTBasedRefiner {

  @Option(description="split arithmetic equalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  private final PredicateRefiner predicateRefiner;
  private final PredicateCPA predicateCpa;
  private final Configuration config;
  private final LogManager logger;
  private final PredicateAbstractionManager amgr;
  private final ExtendedFormulaManager emgr;
  private final TemplateFormulaManager tmgr;
  private final TemplatePathFormulaBuilder tpfb;
  private final TheoremProver prover;
  private final PathFormulaManagerImpl pfmgr;

  final Timer totalRefinement = new Timer();
  final Timer balancing = new Timer();
  final Timer refuting = new Timer();

  public InvariantRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);

    predicateRefiner = PredicateRefiner.create(pCpa);
    predicateCpa = this.getArtCpa().retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(getClass().getSimpleName() + " needs a PredicateCPA");
    }

    config = predicateCpa.getConfiguration();
    config.inject(this, InvariantRefiner.class);
    logger = predicateCpa.getLogger();

    amgr = predicateCpa.getPredicateManager();
    emgr = predicateCpa.getFormulaManager();

    tmgr = new TemplateFormulaManager();
    tpfb = new TemplatePathFormulaBuilder();

    prover = predicateCpa.getTheoremProver();

    pfmgr = new PathFormulaManagerImpl(emgr, config, logger);

  }

  public static InvariantRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    return new InvariantRefiner(pCpa);
  }

  @Override
  protected CounterexampleInfo performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException, InterruptedException {

    totalRefinement.start();

    // build the counterexample
    CounterexampleTraceInfo<Collection<AbstractionPredicate>> counterexample = buildCounterexampleTrace(pPath);

    if (counterexample != null) {
      // the counterexample path was spurious, and we refine
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      List<Pair<ARTElement, CFANode>> path = transformPath(pPath);
      predicateRefiner.performRefinement(pReached, path, counterexample);

      totalRefinement.stop();
      return CounterexampleInfo.spurious();
    } else {
      // the counterexample path might be feasible or it might not; we don't know
      totalRefinement.stop();
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
    }

  }

  private CounterexampleTraceInfo<Collection<AbstractionPredicate>> buildCounterexampleTrace(Path pPath) throws CPAException {

    CounterexampleTraceInfo<Collection<AbstractionPredicate>> ceti = null;

    // Invariant generation is currently only possible if there is a single loop.
    // This check can be weakened in the future.
    Multimap<String, Loop> loops = CFACreator.loops;
    if (loops.size() > 1) {
      // there are too many loops
      logger.log(Level.FINEST, "Could not use invariant generation for proving program safety, program has too many loops.");
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
    }

    if (loops.isEmpty()) {
      // invariant generation is unnecessary, program has no loops
      logger.log(Level.FINEST, "Could not use invariant generation for proving program safety, program has no loops.");
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
    }

    // There is just one loop. Get a hold of it.
    Loop loop = Iterables.getOnlyElement(loops.values());

    // function edges do not count as incoming edges
    Iterable<CFAEdge> incomingEdges = Iterables.filter(loop.getIncomingEdges(),
                                                       Predicates.not(instanceOf(FunctionReturnEdge.class)));

    // Check that there is just one incoming edge into the loop,
    // and that the loop has just one loop head.
    if (Iterables.size(incomingEdges) > 1) {
      logger.log(Level.FINEST, "Could not use invariant generation for proving program safety, loop has too many incoming edges", incomingEdges);
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
    }

    if (loop.getLoopHeads().size() > 1) {
      logger.log(Level.FINEST, "Could not use invariant generation for proving program safety, loop has too many loop heads.");
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
    }

    // There is only one loop head. Grab it.
    CFANode loopHead = Iterables.getOnlyElement(loop.getLoopHeads());

    // Check that the loop head is unambiguous.
    assert loopHead.equals(Iterables.getOnlyElement(incomingEdges).getSuccessor());

    // At this point, we deem the program suitable for application of our method, and
    // we proceed to apply it.
    logger.log(Level.FINEST, "Attempting to compute invariant.");

    // Get entry path formula, from root node up to loop head,
    // loop formula, from loop head back to loop head, and
    // exit formula, from loop head to error location.
    PathFormula entryFormula = buildEntryFormula(pPath, loopHead);
    PathFormula loopFormula = buildLoopFormula(loop);

    // See TODO note below re this quick fix.
    Pair<PathFormula,PathFormula> exitFormulas = buildExitFormula(pPath, loopHead);
    PathFormula exitFormula = exitFormulas.getFirst();
    PathFormula emgrExitFormula = exitFormulas.getSecond();

    logger.log(Level.ALL, "\nEntry, loop, and exit formulas:\nEntry: ", entryFormula, "\nLoop: ", loopFormula, "\nExit: ", exitFormula);
    logger.log(Level.ALL, "Full exit formula, from emgr:\n", emgrExitFormula);

    // Choose an invariant template for the loop head.
    TemplateFormula invTemp = chooseTemplate(entryFormula, loopFormula, exitFormula);
    logger.log(Level.ALL, "\nChosen invariant template for loop head:\n", invTemp);

    // Construct a Program object and attempt to balance it.
    CFANode root = AbstractElements.extractLocation(pPath.getFirst().getFirst());
    TemplateNetwork tnet = buildSimpleLoopNetwork(root, loopHead, invTemp, entryFormula, loopFormula);
    Balancer balancer = new Balancer(logger, pPath);

    balancing.start();
    boolean balanced = balancer.balance(tnet);
    balancing.stop();

    if (balanced) {
      // We managed to compute an invariant at the loop head.
      // Now we must check whether it refutes the counterexample path.
      TemplateFormula phi = tnet.getTemplateMap().get(loopHead);

      logger.log(Level.FINEST, "Computed invariant.");
      logger.log(Level.ALL, "Invariant:\n", phi);

      // We conjoin the invariant with the exit path, and check if this
      // is unsatisfiable. First the invariant needs to be preindexed.

      /**
       * Restore these three lines when possible.
      phi.preindex( (TemplateFormula)exitFormula.getFormula() );
      TemplateFormula phiAndExitTF = (TemplateFormula)tmgr.makeAnd(phi, exitFormula.getFormula());
      Formula phiAndExit = phiAndExitTF.translate(emgr.getDelegate());
      */
      // Quick fix:
      Formula phiAndExit;
      {
        Set<String> phiVars = phi.getAllVariables(VariableWriteMode.PLAIN);
        Integer I = new Integer(0);
        Map<String,Integer> map = new HashMap<String,Integer>();
          for (String s : phiVars) {
            map.put(s,I);
          }
        phi.preindex(map);
        Formula phiTrans = phi.translate(emgr.getDelegate());
        phiAndExit = emgr.makeAnd(phiTrans,emgrExitFormula.getFormula());
      }
      // end quick fix.

      logger.log(Level.ALL, "Invariant and exit formula conjunction:\n", phiAndExit);
      // Unindex phi.
      phi.unindex();

      prover.init();
      refuting.start();
      boolean refutes = prover.isUnsat(phiAndExit);
      refuting.stop();
      prover.reset();

      if (refutes) {
        // Invariant was strong enough.
        logger.log(Level.FINEST, "Invariant refuted counterexample path.");
        // Build a CounterexampleTraceInfo object.
        ceti = new CounterexampleTraceInfo<Collection<AbstractionPredicate>>();
        // Add to it the predicates for phi, at the loop head location.
        addPredicates(ceti, phi, pPath, loopHead);
      } else {
        // Invariant wasn't strong enough.
        logger.log(Level.FINEST, "Invariant did not refute counterexample path.");
      }

    } else {
      // The refinement failed.
      logger.log(Level.FINEST, "Could not compute invariant.");
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
    }

    return ceti;
  }

  private PathFormula buildEntryFormula(Path pPath, CFANode loopHead) {
    // gather CFAEdges until hit ARTElement whose location is loopHead
    Vector<CFAEdge> edges = new Vector<CFAEdge>();

    for (Pair<ARTElement, CFAEdge> pair : pPath) {
      ARTElement ae = pair.getFirst();
      CFAEdge edge = pair.getSecond();
      CFANode loc = AbstractElements.extractLocation(ae);
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

  private PathFormula buildLoopFormula(Loop loop) {
    logger.log(Level.ALL, "Loop:\n",loop);
    Vector<CFANode> loopNodes = new Vector<CFANode>(loop.getLoopNodes());
    Vector<CFAEdge> loopEdges = GraphUtil.makeEdgeLoop(loopNodes);
    logger.log(Level.ALL,"Sequence of edges in loop:\n",loopEdges);
    PathFormula loopFormula = tpfb.buildPathFormula(loopEdges);
    return loopFormula;
  }

  private Pair<PathFormula,PathFormula> buildExitFormula(Path pPath, CFANode loopHead) {
    // gather CFAEdges from ARTElement whose location is loopHead to end of path
    Vector<CFAEdge> edges = new Vector<CFAEdge>();

    boolean begun = false;
    int N = pPath.size() - 1; // we ignore last pair, since last edge is useless, hence " - 1"
    for (int i = 0; i < N; i++) {
      Pair<ARTElement, CFAEdge> pair = pPath.get(i);
      if (begun) {
        CFAEdge edge = pair.getSecond();
        edges.add(edge);
      } else {
        ARTElement ae = pair.getFirst();
        CFANode loc = AbstractElements.extractLocation(ae);
        if (loc == loopHead) {
          begun = true;
          CFAEdge edge = pair.getSecond();
          edges.add(edge);
        }
      }
    }
    // build path formula for these edges
    PathFormula exitFormula = tpfb.buildPathFormula(edges);
    // TODO: remove, when possible:
    // As a temporary fix, we use emgr to build the exit formula again,
    // so that we get the whole thing, even if it involves negations or
    // disjunctions, which at present TemplateFormulas do not allow.
    PathFormula emgrExitFormula = getFullFormula(edges);

    Pair<PathFormula,PathFormula> exitFormulas = Pair.<PathFormula,PathFormula>of(exitFormula,emgrExitFormula);

    return exitFormulas;
  }

  private PathFormula getFullFormula(List<CFAEdge> edges) {
    PathFormula path = pfmgr.makeEmptyPathFormula();
    for (CFAEdge e : edges) {
      try {
        path = pfmgr.makeAnd(path,e);
      } catch (CPATransferException ex) {
        logger.log(Level.SEVERE, "CPATransferException on edge:\n", e);
      }
    }
    return path;
  }

  private TemplateFormula chooseTemplate(PathFormula pEntryFormula, PathFormula pLoopFormula, PathFormula pExitFormula) {
    // Pull the Formulas out of the PathFormulas, cast them into TemplateFormulas,
    // construct a TemplateChooser, and ask it to choose a template.
    TemplateFormula entryFormula = (TemplateFormula) pEntryFormula.getFormula();
    TemplateFormula loopFormula = (TemplateFormula) pLoopFormula.getFormula();
    TemplateFormula exitFormula = (TemplateFormula) pExitFormula.getFormula();
    // TODO: Make the strategy a configurable option, or else plan a sequence of alternative strategies to be automatically deployed.
    TemplateChooser chooser = new TemplateChooser(entryFormula, loopFormula, exitFormula, TemplateChooserStrategy.LINCOMBLEQ);
    TemplateFormula invTemp = chooser.chooseTemplate();
    return invTemp;
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
  private TemplateNetwork buildSimpleLoopNetwork(CFANode pRoot, CFANode pLoopHead, TemplateFormula pInvTemp,
      PathFormula pEntryFormula, PathFormula pLoopFormula) {

    // Locations:
    Location root = new Location(pRoot);
    Location loopHead = new Location(pLoopHead);

    // Template map:
    TemplateMap tmap = new TemplateMap();
    tmap.put(root, new TemplateTrue());
    tmap.put(loopHead, pInvTemp);

    // Path formulas:
    TemplateFormula entryFormula = (TemplateFormula)pEntryFormula.getFormula();
    TemplateFormula loopFormula = (TemplateFormula)pLoopFormula.getFormula();

    // Transitions:
    Transition entryTrans = new Transition(tmap, root, entryFormula, loopHead);
    Transition loopTrans = new Transition(tmap, loopHead, loopFormula, loopHead);

    // Construct and return program.
    TemplateNetwork prog = new TemplateNetwork(tmap, entryTrans, loopTrans);
    return prog;

  }

  private void addPredicates(CounterexampleTraceInfo<Collection<AbstractionPredicate>> ceti, TemplateFormula phi, Path pPath, CFANode loopHead) throws CPAException {
    // Since we are going to use the methods in PredicateRefiner, we need to pad the List
    // of AbstractionPredicate Collections in ceti to make the predicates in phi correspond to
    // the loop head location, and so that the predicates after that location are all 'false'.
    // For symmetry, we add 'true' predicates before the loop head location.
    Collection<AbstractionPredicate> phiPreds = makeAbstractionPredicates(phi);

    // Make true and false formulas.
    Collection<AbstractionPredicate> trueFormula = new Vector<AbstractionPredicate>();
    trueFormula.add(amgr.makePredicate(emgr.makeTrue()));
    Collection<AbstractionPredicate> falseFormula = new Vector<AbstractionPredicate>();
    falseFormula.add(amgr.makePredicate(emgr.makeFalse()));

    // Get the list of abstraction elements.
    List<Pair<ARTElement, CFANode>> path = transformPath(pPath);

    boolean passedLoop = false;
    // We ignore the error location, so the iteration is over i < N,
    // where N is /one less than/ the length of the path.
    int N = path.size() - 1;
    for (int i = 0; i < N; i++) {
      Pair<ARTElement, CFANode> pair = path.get(i);
      CFANode loc = pair.getSecond();
      if (loc == loopHead) {
        ceti.addPredicatesForRefinement(phiPreds);
        passedLoop = true;
      } else if (passedLoop) {
        ceti.addPredicatesForRefinement(falseFormula);
      } else {
        ceti.addPredicatesForRefinement(trueFormula);
      }
    }
  }

  private Collection<AbstractionPredicate> makeAbstractionPredicates(TemplateFormula loopInv) {
    // Extract the atomic formulas from loopInv,
    // then create equivalent Formulas using emgr,
    // and finally pass these, one atom at a time,
    // to amgr's makePredicate method, which will
    // return an AbstractionPredicate for each atom.

    // Here we use the same booleans (splitItpAtoms, false) that are used in the call to
    // extractAtoms in PredicateRefinementManager.getAtomsAsPredicates:
    Collection<Formula> atoms = tmgr.extractAtoms(loopInv, splitItpAtoms, false);

    Collection<AbstractionPredicate> preds = new Vector<AbstractionPredicate>();

    for (Formula atom : atoms) {
      try {
        TemplateConstraint tc = (TemplateConstraint)atom;
        Formula formula = tc.translate(emgr.getDelegate());
        preds.add( amgr.makePredicate(formula) );
      } catch (ClassCastException e) {
        // This should not happen! If it does, then tmgr.extractAtoms did something wrong.
        logger.log(Level.ALL, "Refinement atom was not in the form of a constraint.", atom);
      }
    }

    return preds;
  }

  private List<Pair<ARTElement, CFANode>> transformPath(Path pPath) {
    List<Pair<ARTElement, CFANode>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(pPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      PredicateAbstractElement pe = extractElementByType(ae, PredicateAbstractElement.class);
      if (pe.isAbstractionElement()) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Pair.of(ae, loc));
      }
    }

    assert pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

}
