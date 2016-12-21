package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.RCNFManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

@Options(prefix="cpa.slicing")
public class FormulaSlicingManager implements IFormulaSlicingManager {
  @Option(secure=true, description="Check target states reachability")
  private boolean checkTargetStates = true;

  @Option(secure=true, description="Filter lemmas by liveness")
  private boolean filterByLiveness = true;

  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final InductiveWeakeningManager inductiveWeakeningManager;
  private final Solver solver;
  private final FormulaSlicingStatistics statistics;
  private final RCNFManager rcnfManager;
  private final LiveVariables liveVariables;
  private final LoopStructure loopStructure;

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final LogManager logger;

  FormulaSlicingManager(
      Configuration config,
      CachingPathFormulaManager pPfmgr,
      FormulaManagerView pFmgr,
      CFA pCfa,
      InductiveWeakeningManager pInductiveWeakeningManager,
      RCNFManager pRcnfManager,
      Solver pSolver,
      LogManager pLogger)
      throws InvalidConfigurationException {
    logger = pLogger;
    config.inject(this);
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    inductiveWeakeningManager = pInductiveWeakeningManager;
    solver = pSolver;
    bfmgr = pFmgr.getBooleanFormulaManager();
    rcnfManager = pRcnfManager;
    statistics = new FormulaSlicingStatistics(pPfmgr, pSolver);
    Preconditions.checkState(pCfa.getLiveVariables().isPresent() &&
      pCfa.getLoopStructure().isPresent());
    liveVariables = pCfa.getLiveVariables().get();
    loopStructure = pCfa.getLoopStructure().get();
  }

  @Override
  public Collection<? extends SlicingState> getAbstractSuccessors(
      SlicingState oldState, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    statistics.propagation.start();
    SlicingIntermediateState iOldState;

    if (oldState.isAbstracted()) {
      iOldState = abstractStateToIntermediate(oldState.asAbstracted());
    } else {
      iOldState = oldState.asIntermediate();
    }

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), edge);
    SlicingIntermediateState out = SlicingIntermediateState.of(
        edge.getSuccessor(),
        outPath,
        iOldState.getAbstractParent()
    );
    statistics.propagation.stop();

    return Collections.singleton(out);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      SlicingState pState,
      UnmodifiableReachedSet pStates, AbstractState pFullState)
      throws CPAException, InterruptedException
  {
    SlicingIntermediateState iState;
    if (pState.isAbstracted()) {

      // We do not use the other invariant => do not repeat the computation.
      return Optional.of(
          PrecisionAdjustmentResult.create(
              pState, SingletonPrecision.getInstance(), Action.CONTINUE)
      );
    } else {
      iState = pState.asIntermediate();
    }

    if (checkTargetStates
        && AbstractStates.isTargetState(pFullState)
        && isUnreachableTarget(iState)) {
      return Optional.empty();
    }

    boolean shouldPerformAbstraction = shouldPerformAbstraction(iState.getNode(), pFullState);
    if (shouldPerformAbstraction) {
      Optional<SlicingAbstractedState> oldState = findOldToMerge(
          pStates, pFullState, pState);

      SlicingAbstractedState out;
      if (oldState.isPresent()) {

        // Perform slicing, there is a relevant "to-merge" element.
        Optional<SlicingAbstractedState> slicingOut =
            performSlicing(iState, oldState.get());
        if (slicingOut.isPresent()) {
          out = slicingOut.get();
        } else {
          return Optional.empty();
        }
      } else {

        // No predecessor in the same partition, check reachability and
        // convert the intermediate path to RCNF form.
        if (isUnreachableAbstraction(iState)) {
          return Optional.empty();
        }
        out = SlicingAbstractedState.ofClauses(
            toRcnf(iState),
            iState.getPathFormula().getSsa(),
            iState.getPathFormula().getPointerTargetSet(),
            fmgr,
            iState.getNode(),
            Optional.of(iState)
        );
      }

      return Optional.of(
          PrecisionAdjustmentResult.create(
              out, SingletonPrecision.getInstance(), Action.CONTINUE)
      );
    } else {
      return Optional.of(PrecisionAdjustmentResult.create(
          pState, SingletonPrecision.getInstance(), Action.CONTINUE));
    }
  }

  /**
   * Convert the input state to the set of instantiated lemmas in RCNF.
   */
  private Set<BooleanFormula> toRcnf(SlicingIntermediateState iState)
      throws InterruptedException {
    PathFormula pf = iState.getPathFormula();
    CFANode node = iState.getNode();
    SlicingAbstractedState abstractParent = iState.getAbstractParent();

    BooleanFormula transition = bfmgr.and(
        fmgr.simplify(pf.getFormula()),
        bfmgr.and(abstractParent.getInstantiatedAbstraction())
    );

    Set<BooleanFormula> lemmas = rcnfManager.toLemmasInstantiated(
        pf.updateFormula(transition), fmgr
    );

    Set<BooleanFormula> finalLemmas = new HashSet<>();
    for (BooleanFormula lemma : lemmas) {
      if (filterByLiveness
          && Sets.intersection(
                  ImmutableSet.copyOf(
                      liveVariables
                          .getLiveVariablesForNode(node)
                          .transform(ASimpleDeclaration::getQualifiedName)
                          .filter(s -> s != null)),
                  fmgr.extractFunctionNames(fmgr.uninstantiate(lemma)))
              .isEmpty()) {

        continue;
      }
      finalLemmas.add(fmgr.uninstantiate(lemma));
    }
    return finalLemmas;
  }

  private final Map<Pair<SlicingIntermediateState, SlicingAbstractedState>,
      SlicingAbstractedState> slicingCache = new HashMap<>();

  private Optional<SlicingAbstractedState> performSlicing(
      final SlicingIntermediateState iState,
      final SlicingAbstractedState prevToMerge
  ) throws CPAException, InterruptedException {

    SlicingAbstractedState out = slicingCache.get(Pair.of(iState, prevToMerge));
    if (out != null) {
      statistics.cachedInductiveWeakenings++;
      return Optional.of(out);
    }
    statistics.inductiveWeakeningLocations.add(iState.getNode());

    final SlicingAbstractedState parentState = iState.getAbstractParent();

    Set<BooleanFormula> candidateLemmas = Sets.filter(
        prevToMerge.getAbstraction(),
        input -> allVarsInSSAMap(input,
                prevToMerge.getSSA(),
                iState.getPathFormula().getSsa()));

    PathFormulaWithStartSSA path =
        new PathFormulaWithStartSSA(iState.getPathFormula(), iState
            .getAbstractParent().getSSA());
    if (prevToMerge == parentState) {

      if (parentState.getInductiveUnder().contains(path)) {

        // Optimization for non-nested loops.
        return Optional.of(SlicingAbstractedState.copyOf(parentState));
      }
    }

    Set<BooleanFormula> finalClauses;
    Set<PathFormulaWithStartSSA> inductiveUnder;
    if (isUnreachableAbstraction(iState)) {
      return Optional.empty();
    }
    try {
      statistics.inductiveWeakening.start();
      if (parentState != prevToMerge) {
        finalClauses = inductiveWeakeningManager.findInductiveWeakeningForRCNF(
            parentState.getSSA(),
            parentState.getAbstraction(),
            iState.getPathFormula(),
            candidateLemmas
        );
        inductiveUnder = ImmutableSet.of();
      } else {

        // No nested loops: remove lemmas on both sides.
        finalClauses = inductiveWeakeningManager.findInductiveWeakeningForRCNF(
            parentState.getSSA(),
            iState.getPathFormula(),
            candidateLemmas
        );

        if (finalClauses.equals(candidateLemmas)) {
          inductiveUnder = Sets.union(prevToMerge.getInductiveUnder(),
              ImmutableSet.of(path));
        } else {
          inductiveUnder = ImmutableSet.of(path);
        }
      }
    } catch (SolverException pE) {
      throw new CPAException("Solver call failed", pE);
    } finally {
      statistics.inductiveWeakening.stop();
    }

    out = SlicingAbstractedState.makeSliced(
        finalClauses,
        // It is crucial to use the previous SSA so that PathFormulas stay
        // the same and can be cached.
        prevToMerge.getSSA(),
        iState.getPathFormula().getPointerTargetSet(),
        fmgr,
        iState.getNode(),
        Optional.of(iState),
        inductiveUnder
    );
    slicingCache.put(Pair.of(iState, prevToMerge), out);
    return Optional.of(out);
  }

  /**
   * Check whether target state is unreachable.
   */
  private boolean isUnreachableTarget(SlicingIntermediateState iState)
      throws InterruptedException, CPAException {
    try {
      statistics.reachabilityTargetTimer.start();
      return isUnreachable(iState);
    } finally {
      statistics.reachabilityTargetTimer.stop();
    }
  }

  /**
   * Check whether abstraction location is unreachable.
   */
  private boolean isUnreachableAbstraction(SlicingIntermediateState iState)
      throws CPAException, InterruptedException {
    try {
      statistics.reachabilityAbstractionTimer.start();
      return isUnreachable(iState);
    } finally {
      statistics.reachabilityAbstractionTimer.stop();
    }
  }

  private boolean isUnreachable(SlicingIntermediateState iState)
      throws InterruptedException, CPAException {
    BooleanFormula prevSlice = bfmgr.and(iState.getAbstractParent().getAbstraction());
    BooleanFormula instantiatedParent =
        fmgr.instantiate(prevSlice, iState.getAbstractParent().getSSA());
    BooleanFormula reachabilityQuery = bfmgr.and(
        iState.getPathFormula().getFormula(), instantiatedParent);

    Set<BooleanFormula> constraints = ImmutableSet.copyOf(
        bfmgr.toConjunctionArgs(reachabilityQuery, true));
    CFANode node = iState.getNode();
    statistics.satChecksLocations.add(node);

    try {
      return solver.isUnsat(constraints, node);
    } catch (SolverException pE) {
      logger.log(Level.FINE, "Got solver exception while obtaining unsat core;"
          + "Re-trying without unsat core extraction", pE);
      try {
        return solver.isUnsat(reachabilityQuery);
      } catch (SolverException pE1) {
        throw new CPAException("Solver error occurred", pE1);
      }
    }
  }

  @Override
  public SlicingState getInitialState(CFANode node) {
    return SlicingAbstractedState.empty(fmgr, node);
  }

  @Override
  public boolean isLessOrEqual(SlicingState pState1, SlicingState pState2) {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (pState1.isAbstracted()) {
      return isLessOrEqualAbstracted(pState1.asAbstracted(), pState2.asAbstracted());
    } else {
      return isLessOrEqualIntermediate(pState1.asIntermediate(), pState2.asIntermediate());
    }
  }

  private boolean isLessOrEqualIntermediate(
      SlicingIntermediateState pState1,
      SlicingIntermediateState pState2) {
    SlicingIntermediateState iState1 = pState1.asIntermediate();
    SlicingIntermediateState iState2 = pState2.asIntermediate();
    return (iState1.isMergedInto(iState2) ||
        iState1.getPathFormula().getFormula().equals(iState2.getPathFormula().getFormula()))
        && isLessOrEqualAbstracted(iState1.getAbstractParent(), iState2.getAbstractParent());
  }

  private boolean isLessOrEqualAbstracted(
      SlicingAbstractedState pState1,
      SlicingAbstractedState pState2
  ) {

    // Has at least all the constraints other state does => the state is
    // smaller-or-equal.
    return pState1.getAbstraction().containsAll(pState2.getAbstraction());
  }

  private SlicingIntermediateState joinIntermediateStates(
      SlicingIntermediateState newState,
      SlicingIntermediateState oldState) throws InterruptedException {

    if (!newState.getAbstractParent().equals(oldState.getAbstractParent())) {

      // No merge.
      return oldState;
    }

    if (newState.isMergedInto(oldState)) {
      return oldState;
    } else if (oldState.isMergedInto(newState)) {
      return newState;
    }

    if (oldState.getPathFormula().equals(newState.getPathFormula())) {
      return newState;
    }
    PathFormula mergedPath = pfmgr.makeOr(newState.getPathFormula(),
        oldState.getPathFormula());

    SlicingIntermediateState out = SlicingIntermediateState.of(
        oldState.getNode(), mergedPath, oldState.getAbstractParent()
    );
    newState.setMergedInto(out);
    oldState.setMergedInto(out);
    return out;
  }

  private SlicingIntermediateState abstractStateToIntermediate(
      SlicingAbstractedState pSlicingAbstractedState) {
    return SlicingIntermediateState.of(
        pSlicingAbstractedState.getNode(),
        new PathFormula(
            bfmgr.makeTrue(),
            pSlicingAbstractedState.getSSA(),
            pSlicingAbstractedState.getPointerTargetSet(),
            0), pSlicingAbstractedState
    );
  }

  private boolean shouldPerformAbstraction(CFANode node, AbstractState pFullState) {
    LoopstackState loopState = AbstractStates.extractStateByType(pFullState,
        LoopstackState.class);

    // Slicing is only performed on the loop heads.
    return loopStructure.getAllLoopHeads().contains(node) &&
        (loopState == null || loopState.isLoopCounterAbstracted());
  }

  @Override
  public SlicingState merge(SlicingState pState1, SlicingState pState2) throws InterruptedException {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (pState1.isAbstracted()) {

      // No merge.
      return pState2;
    } else {
      SlicingIntermediateState iState1 = pState1.asIntermediate();
      SlicingIntermediateState iState2 = pState2.asIntermediate();
      return joinIntermediateStates(iState1, iState2);
    }
  }

  /**
   * If the variable got removed from SSAMap along the path, it should not be
   * in the set of candidate lemmas anymore, as one version would be
   * instantiated and another version would not.
   */
  private boolean allVarsInSSAMap(
      BooleanFormula lemma,
      SSAMap oldSsa,
      SSAMap newSsa) {
    for (String var : fmgr.extractVariableNames(lemma)) {
      if (oldSsa.containsVariable(var) != newSsa.containsVariable(var)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Find a previous closest occurrence in ARG in the same partition, or
   * {@code Optional.empty()}
   */
  private Optional<SlicingAbstractedState> findOldToMerge
  (UnmodifiableReachedSet states, AbstractState pArgState, SlicingState state) {
    Set<SlicingAbstractedState> filteredSiblings =
        ImmutableSet.copyOf(
            AbstractStates.projectToType(
                states.getReached(pArgState),
                SlicingAbstractedState.class)
        );
    if (filteredSiblings.isEmpty()) {
      return Optional.empty();
    }

    // We follow the chain of backpointers until we intersect something in the
    // same partition.
    // The chain is necessary as we might have nested loops.
    SlicingState a = state;
    while (true) {
      if (a.isAbstracted()) {
        SlicingAbstractedState aState = a.asAbstracted();

        if (filteredSiblings.contains(aState)) {
          return Optional.of(aState);
        } else {
          if (!aState.getGeneratingState().isPresent()) {
            // Empty.
            return Optional.empty();
          }
          a = aState.getGeneratingState().get().getAbstractParent();
        }
      } else {
        SlicingIntermediateState iState = a.asIntermediate();
        a = iState.getAbstractParent();
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
