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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.invariants.Farkas;
import org.sosy_lab.cpachecker.util.invariants.balancer.Balancer;
import org.sosy_lab.cpachecker.util.invariants.balancer.NetworkBuilder;
import org.sosy_lab.cpachecker.util.invariants.balancer.SingleLoopNetworkBuilder;
import org.sosy_lab.cpachecker.util.invariants.balancer.TemplateNetwork;
import org.sosy_lab.cpachecker.util.invariants.balancer.WeispfenningBalancer;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.Lists;

/**
 * Refiner based on finding loop invariants using an external solver such as RedLog.
 * Unfinished and not working.
 */
@SuppressWarnings("deprecation")
@Deprecated
public class InvariantRefiner extends AbstractARGBasedRefiner {

  private final PredicateAbstractionRefinementStrategy predicateRefinementStrategy;
  private final PredicateCPA predicateCpa;
  private final Configuration config;
  private final LogManager logger;
  private final PredicateAbstractionManager amgr;
  private final FormulaManagerView emgr;
  //private final TemplatePathFormulaBuilder tpfb;
  //private final TheoremProver prover;

  final Timer totalRefinement = new Timer();
  final Timer balancing = new Timer();
  final Timer refuting = new Timer();

  public InvariantRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);

    predicateCpa = this.getArgCpa().retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(getClass().getSimpleName() + " needs a PredicateCPA");
    }

    config = predicateCpa.getConfiguration();
    logger = predicateCpa.getLogger();

    amgr = predicateCpa.getPredicateManager();
    emgr = predicateCpa.getFormulaManager();

    //tpfb = new TemplatePathFormulaBuilder();

    //prover = predicateCpa.getTheoremProver();

    predicateRefinementStrategy = new PredicateAbstractionRefinementStrategy(
        config, logger, predicateCpa.getShutdownNotifier(), emgr, amgr,
        predicateCpa.getStaticRefiner(),
        predicateCpa.getSolver());
  }

  public static InvariantRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    return new InvariantRefiner(pCpa);
  }

  @Override
  protected CounterexampleInfo performRefinement(ARGReachedSet pReached, ARGPath pPath) throws CPAException, InterruptedException {

    totalRefinement.start();

    // build the counterexample
    List<BooleanFormula> predicates = buildCounterexampleTrace(pPath);

    if (predicates != null) {
      // the counterexample path was spurious, and we refine
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");

      List<ARGState> path = PredicateCPARefiner.transformPath(pPath);
      predicateRefinementStrategy.performRefinement(pReached, path, predicates, false);

      totalRefinement.stop();
      return CounterexampleInfo.spurious();
    } else {
      // the counterexample path might be feasible or it might not; we don't know
      totalRefinement.stop();
      throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
    }

  }

  private List<BooleanFormula> buildCounterexampleTrace(ARGPath pPath) throws CPAException {

    List<BooleanFormula> ceti = null;

    NetworkBuilder nbuilder = null;
    try {
      nbuilder = new SingleLoopNetworkBuilder(pPath, logger);
    } catch (RefinementFailedException e) {
      // For now we simply throw the exception.
      // Later, we'll defer to an alternative NetworkBuilder.
      throw e;
    }

    TemplateNetwork tnet = nbuilder.nextNetwork();

    // Initialize the Balancer:
    //Balancer balancer = new BasicBalancer(logger);
    //Balancer balancer = new MatrixBalancer(logger);
    //Balancer balancer = new AcyclicColumnRelianceBalancer(logger);
    Balancer balancer = new WeispfenningBalancer(config, logger);

    // Try template networks until one succeeds, or we run out of ideas.
    while (tnet != null) {

      // Reset indices, so they do not grow needlessly out of bound.
      Farkas.resetConstantIndices();
      TemplateTerm.resetParameterIndices();

      // Attempt to balance the TemplateNetwork.
      balancing.start();
      boolean balanced = balancer.balance(tnet);
      balancing.stop();
      logger.log(Level.FINEST, "Balancer took",balancing.getSumTime().formatAs(TimeUnit.SECONDS));

      if (balanced) {
        // If the network balanced, then, since all NetworkBuilders put 'false' at the error
        // location, it follows that the counterexample path was refuted.

        logger.log(Level.FINEST, "Invariants refuted counterexample path.");
        logger.log(Level.ALL, "Invariants:\n", tnet.dumpTemplates());

        // Build a CounterexampleTraceInfo object.
        ceti = new ArrayList<>();
        // Add the predicates for each of the computed invariants.
        addPredicates(ceti, tnet, pPath);
        // Break out of the while loop on invariant template choices.
        break;

      } else {
        // Could not balance the template network.
        logger.log(Level.FINEST, "Could not balance template network.");
        // Get the next network.
        tnet = nbuilder.nextNetwork();
        // If tnet is null, then we have run out of networks to try.
        // Fail.
        if (tnet == null) {
          throw new RefinementFailedException(Reason.InvariantRefinementFailed, pPath);
        }
      }
    }

    return ceti;
  }

  private void addPredicates(List<BooleanFormula> ceti, TemplateNetwork tnet, ARGPath pPath) throws CPAException {
    // Since we are going to use the methods in PredicateRefiner, we need to pad the List
    // of AbstractionPredicate Collections in ceti to make the predicates in phi correspond to
    // the loop head location, and so that the predicates after that location are all 'false'.
    // For symmetry, we add 'true' predicates before the loop head location.

    BooleanFormulaManagerView bfmgr = emgr.getBooleanFormulaManager();

    // Make true and false formulas.
    BooleanFormula trueFormula = bfmgr.makeBoolean(true);
    Collection<AbstractionPredicate> falseFormula = new Vector<>();
    falseFormula.add(amgr.createPredicateFor(bfmgr.makeBoolean(false)));

    // Get the list of abstraction elements.
    List<CFANode> path = transformPath(pPath);

    // We ignore the error location, so the iteration is over i < N,
    // where N is /one less than/ the length of the path.
    int N = path.size() - 1;
    TemplateFormula phi;
    for (int i = 0; i < N; i++) {
      CFANode loc = path.get(i);
      phi = tnet.getTemplate(loc).getTemplateFormula();
      if (phi != null) {
        ceti.add(convertFormula(phi));
      } else {
        ceti.add(trueFormula);
      }
    }
  }

  private BooleanFormula convertFormula(TemplateFormula invariant) {
    return ((TemplateBoolean)invariant).translate(emgr);
  }

  private List<CFANode> transformPath(ARGPath pPath) {
    // Just extracts information from pPath, putting it into
    // convenient form.
    List<CFANode> result = Lists.newArrayList();

    for (ARGState ae : skip(transform(pPath, Pair.<ARGState>getProjectionToFirst()), 1)) {
      PredicateAbstractState pe = extractStateByType(ae, PredicateAbstractState.class);
      if (pe.isAbstractionState()) {
        CFANode loc = AbstractStates.extractLocation(ae);
        result.add(loc);
      }
    }

    return result;
  }

}