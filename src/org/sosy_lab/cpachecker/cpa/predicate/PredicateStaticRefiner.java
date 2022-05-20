// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getAllStatesOnPathsTo;

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.StaticRefiner;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "staticRefiner")
public class PredicateStaticRefiner extends StaticRefiner
    implements ARGBasedRefiner, StatisticsProvider {

  @Option(secure=true, description="Apply mined predicates on the corresponding scope. false = add them to the global precision.")
  private boolean applyScoped = true;

  @Option(secure=true, description="Add all assumptions along a error trace to the precision.")
  private boolean addAllErrorTraceAssumes = false;

  @Option(secure=true, description="Add all assumptions from the control flow automaton to the precision.")
  private boolean addAllControlFlowAssumes = false;

  @Option(secure=true, description="Add all assumptions along the error trace to the precision.")
  private boolean addAssumesByBoundedBackscan = true;

  @Option(secure=true, description = "Dump CFA assume edges as SMTLIB2 formulas to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumePredicatesFile = null;

  @Option(secure = true, description = "split generated heuristic predicates into atoms")
  private boolean atomicPredicates = true;

  private final StatTimer totalTime = new StatTimer("Total time for static refinement");
  private final StatTimer satCheckTime = new StatTimer("Time for path feasibility check");
  private final StatTimer predicateExtractionTime = new StatTimer("Time for predicate extraction from CFA");
  private final StatTimer argUpdateTime = new StatTimer("Time for ARG update");
  private final StatInt foundPredicates = new StatInt(StatKind.SUM, "Number of predicates found statically");

  private final ShutdownNotifier shutdownNotifier;

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView formulaManagerView;
  private final BooleanFormulaManager booleanManager;
  private final PredicateAbstractionManager predAbsManager;
  private final BlockFormulaStrategy blockFormulaStrategy;
  private final InterpolationManager itpManager;
  private final PathChecker pathChecker;
  private final Solver solver;
  private final CFA cfa;
  private final ARGBasedRefiner delegate;

  private boolean usedStaticRefinement = false;

  public PredicateStaticRefiner(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Solver pSolver,
      PathFormulaManager pPathFormulaManager,
      PredicateAbstractionManager pPredAbsManager,
      BlockFormulaStrategy pBlockFormulaStrategy,
      InterpolationManager pItpManager,
      PathChecker pPathChecker,
      CFA pCfa,
      ARGBasedRefiner pDelegate)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);

    pConfig.inject(this);

    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    this.pathFormulaManager = pPathFormulaManager;
    this.predAbsManager = pPredAbsManager;
    this.blockFormulaStrategy = pBlockFormulaStrategy;
    this.itpManager = pItpManager;
    this.pathChecker = pPathChecker;
    this.solver = pSolver;
    this.formulaManagerView = pSolver.getFormulaManager();
    this.booleanManager = formulaManagerView.getBooleanFormulaManager();
    this.delegate = pDelegate;

    if (assumePredicatesFile != null) {
      dumpAssumePredicate(assumePredicatesFile);
    }
  }

  @Override
  public CounterexampleInfo performRefinementForPath(
      final ARGReachedSet pReached, final ARGPath allStatesTrace)
      throws CPAException, InterruptedException {
    // We do heuristics-based refinement only once.
    if (usedStaticRefinement) {
      return delegate.performRefinementForPath(pReached, allStatesTrace);
    }

    totalTime.start();
    try {
      return performStaticRefinementForPath(pReached, allStatesTrace);
    } finally {
      totalTime.stop();
    }
  }

  private CounterexampleInfo performStaticRefinementForPath(
      final ARGReachedSet pReached, final ARGPath allStatesTrace)
      throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "Starting heuristics-based refinement.");

    Set<ARGState> elementsOnPath = getAllStatesOnPathsTo(allStatesTrace.getLastState());
    // No branches/merges in path, it is precise.
    // We don't need to care about creating extra predicates for branching etc.
    boolean branchingOccurred = true;
    if (elementsOnPath.size() == allStatesTrace.size()) {
      elementsOnPath = ImmutableSet.of();
      branchingOccurred = false;
    }

    // create path with all abstraction location elements (excluding the initial element)
    // the last element is the element corresponding to the error location
    final List<ARGState> abstractionStatesTrace = PredicateCPARefiner.filterAbstractionStates(allStatesTrace);
    BlockFormulas formulas =
        blockFormulaStrategy.getFormulasForPath(
            allStatesTrace.getFirstState(), abstractionStatesTrace);
    if (!formulas.hasBranchingFormula()) {
      formulas =
          formulas.withBranchingFormula(pathFormulaManager.buildBranchingFormula(elementsOnPath));
    }

    CounterexampleTraceInfo counterexample;
    satCheckTime.start();
    try {
      counterexample =
          itpManager.buildCounterexampleTraceWithoutInterpolation(formulas);
    } finally {
      satCheckTime.stop();
    }

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");
      usedStaticRefinement = true;

      UnmodifiableReachedSet reached = pReached.asReachedSet();
      ARGState root = (ARGState) reached.getFirstState();
      ARGState targetState = abstractionStatesTrace.get(abstractionStatesTrace.size() - 1);

      PredicatePrecision heuristicPrecision;
      predicateExtractionTime.start();
      try {
        heuristicPrecision = extractPrecisionFromCfa(pReached.asReachedSet(), targetState);
      } catch (CPATransferException | SolverException e) {
        throw new RefinementFailedException(Reason.StaticRefinementFailed, allStatesTrace, e);
      } finally {
        predicateExtractionTime.stop();
      }

      // Import for not forgetting predicates from initial precisions
      PredicatePrecision basePrecision =
          Precisions.extractPrecisionByType(
              pReached.asReachedSet().getPrecision(root), PredicatePrecision.class);
      PredicatePrecision newPrecision = basePrecision.mergeWith(heuristicPrecision);

      shutdownNotifier.shutdownIfNecessary();
      argUpdateTime.start();
      for (ARGState refinementRoot : ImmutableList.copyOf(root.getChildren())) {
        pReached.removeSubtree(
            refinementRoot, newPrecision, Predicates.instanceOf(PredicatePrecision.class));
      }
      argUpdateTime.stop();

      return CounterexampleInfo.spurious();

    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      return pathChecker.handleFeasibleCounterexample(
          allStatesTrace, counterexample, branchingOccurred);
    }
  }

  private boolean isAssumeOnLoopVariable(AssumeEdge e) {
    if (!cfa.getLoopStructure().isPresent()) {
      return false;
    }
    Set<String> referenced =
        CFAUtils.getVariableNamesOfExpression((CExpression) e.getExpression()).toSet();
    Set<String> loopExitConditionVariables =
        cfa.getLoopStructure().orElseThrow().getLoopExitConditionVariables();

    return !Collections.disjoint(referenced, loopExitConditionVariables);
  }

  private Multimap<String, AStatementEdge> buildDirectlyAffectingStatements() {
    Multimap<String, AStatementEdge> directlyAffectingStatements = LinkedHashMultimap.create();

    for (CFANode u : cfa.getAllNodes()) {
      Deque<CFAEdge> edgesToHandle = CFAUtils.leavingEdges(u).copyInto(new ArrayDeque<>());
      while (!edgesToHandle.isEmpty()) {
        CFAEdge e = edgesToHandle.pop();
        if (e instanceof CStatementEdge) {
          CStatementEdge stmtEdge = (CStatementEdge) e;
          if (stmtEdge.getStatement() instanceof CAssignment) {
            CAssignment assign = (CAssignment) stmtEdge.getStatement();

            if (assign.getLeftHandSide() instanceof CIdExpression) {
              String variable = ((CIdExpression)assign.getLeftHandSide()).getDeclaration().getQualifiedName();
              directlyAffectingStatements.put(variable, stmtEdge);
            }
          }
        }
      }
    }
    return directlyAffectingStatements;
  }

  private boolean isContradicting(AssumeEdge assume, AStatementEdge stmt)
      throws SolverException, CPATransferException, InterruptedException {
    // Check stmt ==> assume?

    BooleanFormula stmtFormula = pathFormulaManager.makeAnd(
        pathFormulaManager.makeEmptyPathFormula(), stmt).getFormula();

    BooleanFormula assumeFormula = pathFormulaManager.makeAnd(
        pathFormulaManager.makeEmptyPathFormula(), assume).getFormula();

    BooleanFormula query = formulaManagerView.uninstantiate(booleanManager.and(stmtFormula, assumeFormula));
    boolean contra = solver.isUnsat(query);

    if (contra) {
      logger.log(Level.INFO, "Contradiction found ", query);
    }

    return contra;


    /* [a == 1]
     *  a = <literal>, where <literal> != 1
     *  a = a +-* <literal>
     *
     *  Variable classification can be used!!
     *
     *  if a IN the set Eq
     *
     */
  }

  private boolean hasContradictingOperationInFlow(
      AssumeEdge e, Multimap<String, AStatementEdge> directlyAffectingStatements)
      throws SolverException, CPATransferException, InterruptedException {
    Set<String> referenced =
        CFAUtils.getVariableNamesOfExpression((CExpression) e.getExpression()).toSet();
    for (String varName: referenced) {
      Collection<AStatementEdge> affectedByStmts = directlyAffectingStatements.get(varName);
      for (AStatementEdge stmtEdge: affectedByStmts) {
        if (isContradicting(e, stmtEdge)) {
          return true;
        }
      }
    }
    return false;
  }

  private Set<AssumeEdge> getAllNonLoopControlFlowAssumes(
      Multimap<String, AStatementEdge> directlyAffectingStatements)
      throws SolverException, CPATransferException, InterruptedException {
    Set<AssumeEdge> result = new HashSet<>();

    for (CFANode u : cfa.getAllNodes()) {
      for (CFAEdge e : CFAUtils.leavingEdges(u)) {
        if (e instanceof AssumeEdge) {
          AssumeEdge assume = (AssumeEdge) e;
          if (!isAssumeOnLoopVariable(assume)) {
            if (hasContradictingOperationInFlow(assume, directlyAffectingStatements)) {
              result.add(assume);
            }
          }
        }
      }
    }

    return result;
  }

  private Set<AssumeEdge> getAssumeEdgesAlongPath(
      UnmodifiableReachedSet reached,
      ARGState targetState,
      Multimap<String, AStatementEdge> directlyAffectingStatements)
      throws SolverException, CPATransferException, InterruptedException {
    Set<AssumeEdge> result = new HashSet<>();

    Set<ARGState> allStatesOnPath = ARGUtils.getAllStatesOnPathsTo(targetState);
    for (ARGState s: allStatesOnPath) {
      CFANode u = AbstractStates.extractLocation(s);
      for (CFAEdge e : CFAUtils.leavingEdges(u)) {
        CFANode v = e.getSuccessor();
        Collection<AbstractState> reachedOnV = reached.getReached(v);

        boolean edgeOnTrace = false;
        for (AbstractState ve : reachedOnV) {
          if (allStatesOnPath.contains(ve)) {
            edgeOnTrace = true;
            break;
          }
        }

        if (edgeOnTrace) {
          if (e instanceof AssumeEdge) {
            AssumeEdge assume = (AssumeEdge) e;
            if (!isAssumeOnLoopVariable(assume)) {
              if (hasContradictingOperationInFlow(assume, directlyAffectingStatements)) {
                result.add(assume);
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * This method extracts a precision based only on static information derived from the CFA.
   *
   * @return a precision for the predicate CPA
   */
  private PredicatePrecision extractPrecisionFromCfa(
      UnmodifiableReachedSet pReached, ARGState targetState)
      throws SolverException, CPATransferException, InterruptedException {
    logger.log(Level.FINER, "Extracting precision from CFA...");

    // Predicates that should be tracked on function scope
    Multimap<String, AbstractionPredicate> functionPredicates = ArrayListMultimap.create();

    // Predicates that should be tracked globally
    Collection<AbstractionPredicate> globalPredicates = new ArrayList<>();

    // Determine the ERROR location of the path (last node, or set of nodes if multiple threads)
    Iterable<CFANode> targetLocations = AbstractStates.extractLocations(targetState);

    // Determine the assume edges that should be considered for predicate extraction
    Set<AssumeEdge> assumeEdges = new LinkedHashSet<>();

    Multimap<String, AStatementEdge> directlyAffectingStatements =
        buildDirectlyAffectingStatements();

    if (addAllControlFlowAssumes) {
      assumeEdges.addAll(getAllNonLoopControlFlowAssumes(directlyAffectingStatements));
    } else {
      if (addAllErrorTraceAssumes) {
        assumeEdges.addAll(
            getAssumeEdgesAlongPath(pReached, targetState, directlyAffectingStatements));
      }
      if (addAssumesByBoundedBackscan) {
        assumeEdges.addAll(
            getTargetLocationAssumes(ImmutableList.copyOf(targetLocations)).values());
      }
    }

    // Create predicates for the assume edges and add them to the precision
    for (AssumeEdge assume : assumeEdges) {
      // Create a boolean formula from the assume
      Collection<AbstractionPredicate> preds = assumeEdgeToPredicates(atomicPredicates, assume);

      // Check whether the predicate should be used global or only local
      boolean applyGlobal = true;
      if (applyScoped) {
        for (CIdExpression idExpr :
            CFAUtils.getIdExpressionsOfExpression((CExpression) assume.getExpression())) {
          CSimpleDeclaration decl = idExpr.getDeclaration();
          if (decl instanceof CVariableDeclaration) {
            if (!((CVariableDeclaration) decl).isGlobal()) {
              applyGlobal = false;
            }
          } else if (decl instanceof CParameterDeclaration) {
            applyGlobal = false;
          }
        }
      }

      // Add the predicate to the resulting precision
      if (applyGlobal) {
        logger.log(Level.FINEST, "Global predicates mined", preds);
        globalPredicates.addAll(preds);
      } else {
        logger.log(Level.FINEST, "Function predicates mined", preds);
        String function = assume.getPredecessor().getFunctionName();
        functionPredicates.putAll(function, preds);
      }
    }

    Set<AbstractionPredicate> allPredicates = new HashSet<>(globalPredicates);

    allPredicates.addAll(functionPredicates.values());
    foundPredicates.setNextValue(allPredicates.size());

    logger.log(Level.FINER, "Extracting finished, found", allPredicates.size(), "predicates");

    return new PredicatePrecision(
        ImmutableSetMultimap.of(),
        ArrayListMultimap.create(),
        functionPredicates,
        globalPredicates);
  }

  private Collection<AbstractionPredicate> assumeEdgeToPredicates(
      boolean pAtomicPredicates, AssumeEdge assume)
      throws CPATransferException, InterruptedException {
    BooleanFormula relevantAssumesFormula = pathFormulaManager.makeAnd(
        pathFormulaManager.makeEmptyPathFormula(), assume).getFormula();

    Collection<AbstractionPredicate> preds;
    if (pAtomicPredicates) {
      preds = predAbsManager.getPredicatesForAtomsOf(relevantAssumesFormula);
    } else {
      preds = ImmutableList.of(predAbsManager.getPredicateFor(relevantAssumesFormula));
    }

    return preds;
  }

  private void dumpAssumePredicate(Path target) {
    try (Writer w = IO.openOutputFile(target, Charset.defaultCharset())) {
      for (CFANode u : cfa.getAllNodes()) {
        for (CFAEdge e: CFAUtils.leavingEdges(u)) {
          if (e instanceof AssumeEdge) {
            Collection<AbstractionPredicate> preds = assumeEdgeToPredicates(false, (AssumeEdge) e);
            for (AbstractionPredicate p: preds) {
              w.append(p.getSymbolicAtom().toString());
              w.append("\n");
            }
          }
        }
      }
    } catch (InterruptedException e) {
      logger.logUserException(Level.WARNING, e, "Interrupted, could not write assume predicates to file!");
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "IO exception! Could not write assume predicates to file!");
    } catch (CPATransferException e) {
      logger.logUserException(Level.WARNING, e, "Transfer exception! Could not write assume predicates to file!");
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
    if (delegate instanceof StatisticsProvider) {
      ((StatisticsProvider) delegate).collectStatistics(pStatsCollection);
    }
  }

  private class Stats implements Statistics {
    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter.writingStatisticsTo(pOut)
          .ifUpdatedAtLeastOnce(totalTime)
          .put(foundPredicates)
          .spacer()
          .put(totalTime)
          .beginLevel()
          .put(satCheckTime)
          .putIfUpdatedAtLeastOnce(predicateExtractionTime)
          .putIfUpdatedAtLeastOnce(argUpdateTime);
    }

    @Override
    public String getName() {
      return "Static Predicate Refiner";
    }
  }
}
