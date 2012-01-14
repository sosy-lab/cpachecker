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

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.AbstractARTBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.invariants.Farkas;
import org.sosy_lab.cpachecker.util.invariants.balancer.Balancer;
import org.sosy_lab.cpachecker.util.invariants.balancer.NetworkBuilder;
import org.sosy_lab.cpachecker.util.invariants.balancer.SingleLoopNetworkBuilder;
import org.sosy_lab.cpachecker.util.invariants.balancer.TemplateNetwork;
import org.sosy_lab.cpachecker.util.invariants.balancer.Balancer.UIFAxiomStrategy;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConstraint;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormulaManager;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.Lists;

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
  //private final TemplatePathFormulaBuilder tpfb;
  //private final TheoremProver prover;

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
    //tpfb = new TemplatePathFormulaBuilder();

    //prover = predicateCpa.getTheoremProver();

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
      predicateRefiner.performRefinement(pReached, path, counterexample, false);

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

    NetworkBuilder nbuilder = null;
    try {
      nbuilder = new SingleLoopNetworkBuilder(pPath, logger);
    } catch (RefinementFailedException e) {
      // For now we simply throw the exception.
      // Later, we'll defer to an alternative NetworkBuilder.
      throw e;
    }

    TemplateNetwork tnet = nbuilder.nextNetwork();
    Balancer balancer = new Balancer(logger);
    UIFAxiomStrategy axiomStrategy = UIFAxiomStrategy.DONOTUSEAXIOMS;
    // Try template networks until one succeeds, or we run out of ideas.
    while (tnet != null) {

      logger.log(Level.ALL, "\nAxiom strategy:\n", axiomStrategy);

      // Reset indices, so they do not grow needlessly out of bound.
      Farkas.resetConstantIndices();
      TemplateTerm.resetParameterIndices();

      // Attempt to balance the TemplateNetwork.
      balancer.setAxiomStrategy(axiomStrategy);
      balancing.start();
      boolean balanced = balancer.balance(tnet);
      balancing.stop();

      if (balanced) {
        // If the network balanced, then, since it put 'false' at the error
        // location, it follows that the counterexample path was refuted.

        logger.log(Level.FINEST, "Invariants refuted counterexample path.");
        logger.log(Level.ALL, "Invariants:\n", tnet.dumpTemplates());

        // Build a CounterexampleTraceInfo object.
        ceti = new CounterexampleTraceInfo<Collection<AbstractionPredicate>>();
        // Add the predicates for each of the computed invariants.
        addPredicates(ceti, tnet, pPath);
        // Break out of the while loop on invariant template choices.
        break;

      } else {
        // Could not balance the template network.
        logger.log(Level.FINEST, "Could not balance template network.");

        // If we were not using axioms, then we should try using them,
        // on the same network.
        if (axiomStrategy == UIFAxiomStrategy.DONOTUSEAXIOMS) {
          axiomStrategy = UIFAxiomStrategy.USEAXIOMS;

        // Else even axioms did not work, so try the next network, if
        // there is one.
        } else {
          tnet = nbuilder.nextNetwork();
          axiomStrategy = UIFAxiomStrategy.DONOTUSEAXIOMS;
        }

        // If tnet is null, then we have run out of networks to try.
        // Fail.
        if (tnet == null) {
          throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
        }
      }
    }

    return ceti;
  }

  private void addPredicates(CounterexampleTraceInfo<Collection<AbstractionPredicate>> ceti, TemplateNetwork tnet, Path pPath) throws CPAException {
    // Since we are going to use the methods in PredicateRefiner, we need to pad the List
    // of AbstractionPredicate Collections in ceti to make the predicates in phi correspond to
    // the loop head location, and so that the predicates after that location are all 'false'.
    // For symmetry, we add 'true' predicates before the loop head location.

    // Make true and false formulas.
    Collection<AbstractionPredicate> trueFormula = new Vector<AbstractionPredicate>();
    trueFormula.add(amgr.makePredicate(emgr.makeTrue()));
    Collection<AbstractionPredicate> falseFormula = new Vector<AbstractionPredicate>();
    falseFormula.add(amgr.makePredicate(emgr.makeFalse()));

    // Get the list of abstraction elements.
    List<Pair<ARTElement, CFANode>> path = transformPath(pPath);

    // We ignore the error location, so the iteration is over i < N,
    // where N is /one less than/ the length of the path.
    int N = path.size() - 1;
    TemplateFormula phi;
    for (int i = 0; i < N; i++) {
      Pair<ARTElement, CFANode> pair = path.get(i);
      CFANode loc = pair.getSecond();
      phi = tnet.getTemplate(loc).getTemplateFormula();
      if (phi != null) {
        Collection<AbstractionPredicate> phiPreds = makeAbstractionPredicates(phi);
        ceti.addPredicatesForRefinement(phiPreds);
      } else {
        ceti.addPredicatesForRefinement(trueFormula);
      }
    }
  }

  private Collection<AbstractionPredicate> makeAbstractionPredicates(TemplateFormula invariant) {
    // Extract the atomic formulas from 'invariant',
    // then create equivalent Formulas using emgr,
    // and finally pass these, one atom at a time,
    // to amgr's makePredicate method, which will
    // return an AbstractionPredicate for each atom.

    // Here we use the same booleans (splitItpAtoms, false) that are used in the call to
    // extractAtoms in PredicateRefinementManager.getAtomsAsPredicates:
    Collection<Formula> atoms = tmgr.extractAtoms(invariant, splitItpAtoms, false);

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
    // Just extracts information from pPath, putting it into
    // convenient form.
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