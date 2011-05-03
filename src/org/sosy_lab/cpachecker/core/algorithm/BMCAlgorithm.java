/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.*;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.FILTER_ABSTRACTION_ELEMENTS;
import static org.sosy_lab.cpachecker.util.AbstractElements.IS_TARGET_ELEMENT;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;
import static org.sosy_lab.cpachecker.util.AbstractElements.filterTargetElements;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.CFA.Loop;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@Options(prefix="bmc")
public class BMCAlgorithm implements Algorithm, StatisticsProvider {

  private static final Function<AbstractElement, PredicateAbstractElement> EXTRACT_PREDICATE_ELEMENT
      = AbstractElements.extractElementByTypeFunction(PredicateAbstractElement.class);

  private static class BMCStatistics implements Statistics {

    private final Timer satCheck = new Timer();
    private final Timer assertionsCheck = new Timer();
    
    private final Timer inductionPreparation = new Timer();
    private final Timer inductionCheck = new Timer();
    private int inductionCutPoints = 0;
    
    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      if (satCheck.getNumberOfIntervals() > 0) {
        out.println("Time for final sat check:            " + satCheck);
      }
      if (assertionsCheck.getNumberOfIntervals() > 0) {
        out.println("Time for bounding assertions check:  " + assertionsCheck);
      }
      if (inductionCheck.getNumberOfIntervals() > 0) {
        out.println("Number of cut points for induction:  " + inductionCutPoints);
        out.println("Time for induction formula creation: " + inductionPreparation);
        out.println("Time for induction check:            " + inductionCheck);
      }
    }

    @Override
    public String getName() {
      return "BMC algorithm";
    }
  }
  
  @Option
  private boolean boundingAssertions = true;
  
  @Option
  private boolean checkTargetStates = true;
  
  @Option
  private boolean induction = true;
  
  private final BMCStatistics stats = new BMCStatistics();
  private final Algorithm algorithm;
  private final PredicateCPA predCpa;
  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  
  public BMCAlgorithm(Algorithm algorithm, Configuration config, LogManager logger, ReachedSetFactory pReachedSetFactory) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    this.algorithm = algorithm;
    this.logger = logger;
    reachedSetFactory = pReachedSetFactory;
    
    predCpa = ((WrapperCPA)getCPA()).retrieveWrappedCpa(PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for BMCAlgorithm");
    }
  }

  @Override
  public boolean run(final ReachedSet pReachedSet) throws CPAException {
    final boolean soundInner = algorithm.run(pReachedSet);
    
    if (any(transform(skip(pReachedSet, 1), EXTRACT_PREDICATE_ELEMENT), FILTER_ABSTRACTION_ELEMENTS)) {
      // first element of reached is always an abstraction element, so skip it
      logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
      return soundInner;
    }
    
    FormulaManager fmgr = predCpa.getFormulaManager();
    List<AbstractElement> targetElements = Lists.newArrayList(AbstractElements.filterTargetElements(pReachedSet));
    logger.log(Level.FINER, "Found", targetElements.size(), "potential target elements");

    TheoremProver prover = predCpa.getTheoremProver();
    prover.init();

    boolean safe = true;
    if (checkTargetStates) {
      Formula program = fmgr.makeFalse();
      for (PredicateAbstractElement e : transform(targetElements, EXTRACT_PREDICATE_ELEMENT)) {
        assert e != null : "PredicateCPA exists but did not produce elements!";
        program = fmgr.makeOr(program, e.getPathFormula().getFormula());
      }
      
      logger.log(Level.INFO, "Starting satisfiability check...");
      stats.satCheck.start();
      safe = prover.isUnsat(program);
      stats.satCheck.stop();

    } else {
      safe = targetElements.isEmpty();
    }
    
    logger.log(Level.FINER, "Program is safe?:", safe);
    
    if (safe) {
      pReachedSet.removeAll(targetElements);
    }
    
    boolean sound;
    
    // check loop unwinding assertions, but don't bother if we are unsound anyway
    // or we have found a bug
    if (soundInner && safe && boundingAssertions) {
      Formula assertions = fmgr.makeFalse();
      
      // create formula for unwinding assertions
      for (AbstractElement e : pReachedSet) {
        AssumptionStorageElement asmpt = extractElementByType(e, AssumptionStorageElement.class);
        if (asmpt.isStop()) {
          PredicateAbstractElement pred = extractElementByType(e, PredicateAbstractElement.class);
          assertions = fmgr.makeOr(assertions, pred.getPathFormula().getFormula());
        }
      }
      
      logger.log(Level.INFO, "Starting assertions check...");

      stats.assertionsCheck.start();
      sound = prover.isUnsat(assertions);
      stats.assertionsCheck.stop();

      logger.log(Level.FINER, "Soundness after assertion checks:", sound);

    } else {
      sound = false; // signal that this is unsound
    }
    
    // try to prove program safety via induction, but don't bother if we are unsound anyway,
    // we have found a bug or we have already proved program safety
    if (soundInner && safe && !sound && induction) {
 
      // Induction is currently only possible if there is a single loop.
      // This check can be weakend in the future,
      // e.g. it is ok if there is only a single loop on each path.
      Multimap<String, Loop> loops = CFACreator.loops;
      if (loops.size() > 1) {
        logger.log(Level.WARNING, "Could not use induction for proving program safety, program has too many loops");
        return sound;
      }
      
      if (loops.isEmpty()) {
        // induction is unnecessary, program has no loops
        return sound;
      }
      
      stats.inductionPreparation.start();
      
      Loop loop = Iterables.getOnlyElement(loops.values());

      // function edges do not count as incoming/outgoing edges
      Iterable<CFAEdge> incomingEdges = Iterables.filter(loop.getIncomingEdges(),
                                                         Predicates.not(instanceOf(FunctionReturnEdge.class)));
      Iterable<CFAEdge> outgoingEdges = Iterables.filter(loop.getOutgoingEdges(),
                                                         Predicates.not(instanceOf(FunctionCallEdge.class)));
      
      if (Iterables.size(incomingEdges) > 1) {
        logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many incoming edges", incomingEdges);
        return sound;
      }
      
      if (loop.getLoopHeads().size() > 1) {
        logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many loop heads");
        return sound;
      }
      
      CFANode loopHead = Iterables.getOnlyElement(loop.getLoopHeads());
      
      // check that the loop head is unambigious
      assert loopHead.equals(Iterables.getOnlyElement(incomingEdges).getSuccessor());
      
      // Proving program safety with induction consists of two parts:
      // 1) Prove all paths safe that go only one iteration through the loop.
      //    This is part of the classic bounded model checking done above,
      //    so we don't care about this here.
      // 2) Assume that one loop iteration is safe and prove that the next one is safe, too.
      
      // Suppose that the loop has a single outgoing edge,
      // which leads to the error location. This edge is always an AssumeEdge,
      // and it has a "sibling" which is an inner edge of the loop and leads to
      // the next iteration. We call the latter the continuation edge.
      // The common predecessor node of these two edges will be called cut point.
      // Now we want to show that the control flow of the program will never take
      // the outgoing edge, if it didn't take it in the iteration before.
      // We create three formulas:
      // A is the assumption from the continuation edge in the previous iteration
      // B is the formula for the loop body in the current iteration up to the cut point 
      // C is the assumption from the continuation edge in the current iteration
      //   Note that this is the negation of the assumption from the exit edge.
      // Then we try to prove that the formula (A & B) => C holds.
      // This implies that control flow cannot take the exit edge.

      // The conjunction (A & B) is created by running the CPAAlgorithm starting
      // at the cut point and letting it run until the end of the current iteration
      // (i.e. let it finish the iteration it starts in and complete one more iteration).
      // Then we get the abstract state at the cut point in the last iteration
      // and take its path formula, which is exactly (A & B).
      // C is created manually. It is important to re-use the SSAMap from (A & B)
      // in order to get the indices right.
      
      // Everything above is easily extended to k-induction with k >= 1
      // and to loops that have several outgoing edges (and therefore several
      // cut points).
      // For k-induction, just let the algorithm run a few iterations. Of course
      // the formula for the induction basis needs to contain the same number of
      // iterations. This is ensured because we use the same algorithm and the
      // same CPAs to create the formulas in both cases, so they'll run the same
      // number of iterations in both cases.
      // For several exiting edges, we add each cut-point to the initial reached
      // set, so that A will contain the assumptions from all continuation edges,
      // and we'll create several (A & B) and C formulas, one for each cut point.
      
      
      // Create initial reached set
      ConfigurableProgramAnalysis cpa = getCPA();
      ReachedSet reached = reachedSetFactory.create();
      reached.add(cpa.getInitialElement(loopHead), cpa.getInitialPrecision(loopHead));

      // Run algorithm in order to create formula (A & B)

      logger.log(Level.INFO, "Running algorithm to create induction hypothesis");
      algorithm.run(reached);

      Multimap<CFANode, AbstractElement> reachedPerLocation = Multimaps.index(reached, AbstractElements.EXTRACT_LOCATION);

      // live view of reached set with only the elements in the loop
      Iterable<AbstractElement> loopStates = Iterables.filter(reached, new Predicate<AbstractElement>() {
        @Override
        public boolean apply(AbstractElement pArg0) {
          LoopstackElement loopElement = extractElementByType(pArg0, LoopstackElement.class);
          return loopElement.getLoop() != null;
        }
      });
      
      assert !Iterables.isEmpty(loopStates);
      if (Iterables.any(loopStates, IS_TARGET_ELEMENT)) {
        logger.log(Level.WARNING, "Could not use induction for proving program safety, target state is contained in the loop");
        return sound;
      }

      // Create formulas
      PathFormulaManager pmgr = predCpa.getPathFormulaManager();
      Formula inductions = fmgr.makeTrue();
      
      for (CFAEdge outgoingEdge : outgoingEdges) {
        // filter out exit edges that do not lead to a target state, we don't care about them
        {
          CFANode exitLocation = outgoingEdge.getSuccessor();
          Iterable<AbstractElement> exitStates = reachedPerLocation.get(exitLocation);
          ARTElement lastExitState = (ARTElement)Iterables.getLast(exitStates);
          
          // the states reachable from the exit edge
          Set<ARTElement> outOfLoopStates = lastExitState.getSubtree();
          if (Iterables.isEmpty(filterTargetElements(outOfLoopStates))) {
            // no target state reachable
            continue;
          }
        }
        stats.inductionCutPoints++;
        logger.log(Level.FINEST, "Considering exit edge", outgoingEdge);

        CFANode cutPoint = outgoingEdge.getPredecessor();
        Iterable<AbstractElement> cutPointStates = reachedPerLocation.get(cutPoint);
        AbstractElement lastcutPointState = Iterables.getLast(cutPointStates);
        
        // Create (A & B)
        PathFormula pathFormulaAB = extractElementByType(lastcutPointState, PredicateAbstractElement.class).getPathFormula();
        Formula formulaAB = pathFormulaAB.getFormula();
        assert (!prover.isUnsat(formulaAB));

        // Create C
        PathFormula empty = pmgr.makeEmptyPathFormula(pathFormulaAB); // empty has correct SSAMap
        PathFormula pathFormulaC = pmgr.makeAnd(empty, outgoingEdge);
        // we need to negate it, because we used the outgoing edge, not the continuation edge
        Formula formulaC = fmgr.makeNot(pathFormulaC.getFormula());
        
        // Crate (A & B) => C
        Formula f = fmgr.makeOr(fmgr.makeNot(formulaAB), formulaC);
        
        inductions = fmgr.makeAnd(inductions, f);
      }
      
      // now prove that (A & B) => C is a tautology by checking if the negation is unsatisfiable
      
      inductions = fmgr.makeNot(inductions);
      
      stats.inductionPreparation.stop();
      
      logger.log(Level.INFO, "Starting induction check...");

      stats.inductionCheck.start();
      sound = prover.isUnsat(inductions);
      stats.inductionCheck.stop();
     
      logger.log(Level.FINER, "Soundness after induction check:", sound);
    }
    
    prover.reset();
    return sound;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return algorithm.getCPA();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }  
}
