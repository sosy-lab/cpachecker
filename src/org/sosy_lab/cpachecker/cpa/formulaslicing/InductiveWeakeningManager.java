package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.NullLogManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.SolverContext.ProverOptions;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;
import org.sosy_lab.solver.visitors.DefaultBooleanFormulaVisitor;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Finds inductive weakening of formulas (originally: formula slicing).
 * This class operates on formulas, and should be orthogonal to
 * CFA- and CPA-specific concepts.
 */
@Options(prefix="cpa.slicing")
public class InductiveWeakeningManager {

  @Option(secure=true, description="Use syntactic formula slicing, which"
      + " uses only the syntactic structure of the formula and does not involve"
      + " any calls to the SMT solver.")
  private boolean runSyntacticSlicing = false;

  @Option(secure=true, description="Run destructive formula slicing, which starts with an "
  + "unsatisfiable set and tries to add elements to it, making sure it stays unsatisfiable.")
  private boolean runDestructiveSlicing = true;

  @Option(secure=true, description="Use formula slicing based on counterexamples.")
  private boolean runCounterexampleBasedSlicing = false;

  @Option(secure=true, description="Sort selection variables based on syntactic "
      + "similarity to the transition relation")
  private boolean sortSelectionVariablesSyntactic = true;

  @Option(secure=true, description="Limits the number of iteration for the "
      + "destructive slicing strategy. Set to -1 for no limit.")
  private int destructiveIterationLimit = -1;

  @Option(description="Perform light quantifier elimination on intermediate "
      + "variables")
  private boolean performLightQE = false;

  @Option(description="Strategy for abstracting children during CEX weakening")
  private SELECTION_STRATEGY removalSelectionStrategy = SELECTION_STRATEGY.FIRST;

  @Option(description="Factorization weakening strategy", secure=true)
  private boolean runFactorizationStrategy = false;

  private enum SELECTION_STRATEGY {
    ALL,
    FIRST,
    RANDOM,
    LEAST_REMOVALS
  }

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final LogManager logger;
  private final InductiveWeakeningStatistics statistics;

  // TODO: inject a seed for reproducibility.
  private final Random r = new Random();

  public InductiveWeakeningManager(
      Configuration config,
      FormulaManagerView pFmgr,
      Solver pSolver,
      LogManager pLogger,
      InductiveWeakeningStatistics pStatistics
  ) throws InvalidConfigurationException {
    config.inject(this);

    statistics = pStatistics;
    fmgr = pFmgr;
    solver = pSolver;
    logger = pLogger;
    bfmgr = fmgr.getBooleanFormulaManager();
  }


  /**
   * Find the inductive weakening of {@code input} subject to the loop
   * transition over-approximation shown in {@code transition}.
   *
   * @param strengthening Strengthening which is guaranteed to be universally
   * true (under the given path) at the given point.
   */
  public BooleanFormula slice(
      PathFormula input, PathFormula transition,
      BooleanFormula strengthening
  ) throws SolverException, InterruptedException {

    if (runFactorizationStrategy) {
      return factorizationWeakening(input, transition, strengthening);
    }

    logger.log(Level.FINE, "Transition = " + transition.getFormula());
    logger.log(Level.FINE, "Input = " + input.getFormula());

    if (input.getFormula().equals(bfmgr.makeBoolean(true))) {
      return bfmgr.makeBoolean(true);
    }

    // Step 1: get rid of intermediate variables in "input".

    // ...remove atoms containing intermediate variables.
    BooleanFormula noIntermediate;
    final ImmutableMap<BooleanFormula, BooleanFormula> selectionVarsInfo;
    final BooleanFormula query, annotated, primed;
    try {
      statistics.annotationTime.start();
      noIntermediate = removeIntermediate(input);

      if (noIntermediate.equals(bfmgr.makeBoolean(false))) {

        // Shortcut, no atoms with only non-intermediate variables existed in the
        // original formula.
        return bfmgr.makeBoolean(true);
      }

      // Step 2: Annotate conjunctions.

      // Selection variables -> atoms.
      Map<BooleanFormula, BooleanFormula> varsInfoBuilder = new HashMap<>();
      annotated = bfmgr.visit(new LiteralsAnnotator(
              fmgr, new HashMap<BooleanFormula, BooleanFormula>(), varsInfoBuilder),
          noIntermediate);
      selectionVarsInfo = ImmutableMap.copyOf(varsInfoBuilder);
      assert !selectionVarsInfo.isEmpty();

      // This is possible since the formula does not have any intermediate
      // variables.
      primed = fmgr.instantiate(fmgr.uninstantiate(annotated), transition.getSsa());
      BooleanFormula negated = bfmgr.not(primed);

      // Inductiveness checking formula, injecting the known invariant "strengthening".
      query = bfmgr.and(ImmutableList.of(
          annotated,
          transition.getFormula(),
          negated,
          strengthening
      ));
    } finally {
      statistics.annotationTime.stop();
    }

    Set<BooleanFormula> selectorsToAbstractOverApproximation;

    if (runSyntacticSlicing) {
      selectorsToAbstractOverApproximation = syntacticWeakening(selectionVarsInfo, transition);
    } else {
      selectorsToAbstractOverApproximation = ImmutableSet.copyOf(selectionVarsInfo.keySet());
    }
    assert solver.isUnsat(bfmgr.and(bfmgr.and(selectorsToAbstractOverApproximation), query));

    if (runCounterexampleBasedSlicing) {
      selectorsToAbstractOverApproximation = counterexampleBasedWeakening(
          selectionVarsInfo, annotated, query, primed
      );
    }

    if (runDestructiveSlicing) {
      selectorsToAbstractOverApproximation = destructiveWeakening
          (selectionVarsInfo, selectorsToAbstractOverApproximation, transition.getFormula(), query);
    }

    BooleanFormula out = abstractSelectors(
        annotated,
        selectionVarsInfo,
        selectorsToAbstractOverApproximation
    );
    logger.log(Level.FINE, "Slice obtained: ", out);
    return fmgr.uninstantiate(out);
  }

  /**
   * Apply the transformation, replace the atoms marked by the
   * selector variables with 'Top'.
   *
   * @param annotated Annotated input \phi
   * @param selectionVarsInfo Mapping from selectors to the literals they annotate (unprimed \phi)
   * @param selectorsToAbstract Selectors which should be abstracted.
   *
   */
  private BooleanFormula abstractSelectors(
      BooleanFormula annotated,
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      Set<BooleanFormula> selectorsToAbstract) {
    // Step 3:
    // note: it would be probably better to move those different steps to
    // different subroutines.
    Map<BooleanFormula, BooleanFormula> replacement = new HashMap<>();
    for (BooleanFormula f : selectionVarsInfo.keySet()) {

      if (selectorsToAbstract.contains(f)) {
        replacement.put(f, bfmgr.makeBoolean(true));
      } else {
        replacement.put(f, bfmgr.makeBoolean(false));
      }
    }

    BooleanFormula sliced = fmgr.substitute(annotated, replacement);
    return fmgr.simplify(sliced);
  }

  private Set<BooleanFormula> destructiveWeakening(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      Set<BooleanFormula> selectorsToAbstractOverApproximation,
      BooleanFormula transition,
      BooleanFormula query) throws SolverException, InterruptedException {
    List<BooleanFormula> orderedList;
    if (sortSelectionVariablesSyntactic) {
      orderedList = sortBySyntacticSimilarity(
          selectionVarsInfo, selectorsToAbstractOverApproximation, transition);
    } else {
      orderedList = new ArrayList<>(selectionVarsInfo.keySet());
    }

    try {
      statistics.destructiveWeakeningTime.start();
      return destructiveWeakening0(selectionVarsInfo, orderedList, query);
    } finally {
      statistics.destructiveWeakeningTime.stop();
    }
  }

  /**
   * Syntactic formula weakening: slices away all atoms which have variables
   * which were changed (== SSA index changed) by the transition relation.
   * In that case, \phi is exactly the same as \phi',
   * and the formula should be unsatisfiable.
   *
   * @param selectionInfo selection variable -> corresponding atom (instantiated
   * with unprimed SSA).
   *
   * @return Set of selectors which correspond to atoms which *should*
   *         be abstracted.
   */
  private Set<BooleanFormula> syntacticWeakening(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      PathFormula transition) {
    Set<BooleanFormula> out = new HashSet<>();
    for (BooleanFormula selector : selectionInfo.keySet()) {
      BooleanFormula atom = selectionInfo.get(selector);

      // Variables which have the SSA index different to the one after the
      // transition.
      Set<String> deadVars = fmgr.getDeadFunctionNames(atom, transition.getSsa());

      // I don't even understand now why this is valid.
      // Dead vars is not empty
      if (!deadVars.isEmpty()
          // todo: remove this hack
          ||  atom.toString().contains("z3name")
          ) {
        out.add(selector);
      }
    }
    return out;
  }

  private Set<BooleanFormula> counterexampleBasedWeakening(
      final ImmutableMap<BooleanFormula, BooleanFormula> selectionInfo,
      BooleanFormula annotated,
      BooleanFormula query,
      BooleanFormula primed
  ) throws SolverException, InterruptedException {
    try {
      statistics.cexWeakeningTime.start();
      return counterexampleBasedWeakening0(selectionInfo, annotated, query, primed);
    } finally {
      statistics.cexWeakeningTime.stop();
    }
  }
  /**
   * Apply a weakening based on counterexamples derived from solver models.
   *
   * @param selectionInfo Mapping from selectors to literals which they annotate.
   * @param query Inductiveness checking query
   * @param primed \phi'
   *
   * @return A subset of selectors after abstracting which the query becomes inductive.
   */
  private Set<BooleanFormula> counterexampleBasedWeakening0(
      final ImmutableMap<BooleanFormula, BooleanFormula> selectionInfo,
      final BooleanFormula annotated,
      BooleanFormula query,
      BooleanFormula primed
  ) throws SolverException, InterruptedException {

    final Set<BooleanFormula> toAbstract = new HashSet<>();

    try (ProverEnvironment env = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      env.addConstraint(query);
      env.push();

      for (BooleanFormula selector : selectionInfo.keySet()) {
        env.addConstraint(bfmgr.not(selector));
      }

      logger.log(Level.FINE, "Query = " + query);

      while (!env.isUnsat()) {
        final Model m = env.getModel();

        statistics.noCexIterations.incrementAndGet();

        toAbstract.addAll(getSelectorsToAbstract(
            ImmutableSet.copyOf(toAbstract), m, selectionInfo, annotated, primed, logger
        ));


        env.pop();
        for (BooleanFormula selector : selectionInfo.keySet()) {
          if (toAbstract.contains(selector)) {
            env.addConstraint(selector);
          } else {
            env.addConstraint(bfmgr.not(selector));
          }
        }
        env.push();
      }
    }

    return toAbstract;
  }

  private List<BooleanFormula> getSelectorsToAbstract(
      final ImmutableSet<BooleanFormula> toAbstract,
      final Model m,
      final ImmutableMap<BooleanFormula, BooleanFormula> selectionInfo,
      final BooleanFormula annotated,
      final BooleanFormula primed,
      final LogManager usedLogger
  ) {
    final List<BooleanFormula> newToAbstract = new ArrayList<>();

    // Perform the required abstraction.
    bfmgr.visitRecursively(new DefaultBooleanFormulaVisitor<TraversalProcess>() {

      @Override
      protected TraversalProcess visitDefault() {
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitAnd(List<BooleanFormula> operands) {
        // Under negation, AND becomes OR.
        // Abstracting away all children which evaluate to _true_ is sufficient.
        Set<BooleanFormula> filtered = new HashSet<>();
        for (BooleanFormula op : operands) {
          if (shouldAbstract(op)) {
            filtered.add(op);
          }
        }
        return TraversalProcess.custom(filtered);
      }

      @Override
      public TraversalProcess visitOr(List<BooleanFormula> operands) {
        // Under negation, OR becomes AND.
        // ALL children of this node evaluate to true iff the node
        // evaluates to true.
        // Abstracting away any child is sufficient to break the satisfiability.
        Optional<BooleanFormula> selector = findSelector(operands);

        if (selector.isPresent()) {
          if (shouldAbstract(bfmgr.or(operands))) {
            handleAnnotatedLiteral(selector.get());
          }
          return TraversalProcess.SKIP;
        } else {
          return selectChildren(operands);
        }
      }


      private void handleAnnotatedLiteral(BooleanFormula selector) {
        // Don't-care or evaluates-to-false.
        if (!toAbstract.contains(selector)) {
          newToAbstract.add(selector);

          usedLogger.log(Level.FINE, "Model = " + m);
          usedLogger.log(Level.FINE, "Abstracting away", selectionInfo.get(selector));
          usedLogger.log(Level.FINE, "Intermediate result: ", abstractSelectors(
              annotated, selectionInfo, toAbstract
          ));
        }
      }

      private boolean shouldAbstract(BooleanFormula f) {
        Boolean out = m.evaluate(bfmgr.not(f));
        return (out != null && out);
      }

      private Optional<BooleanFormula> findSelector(List<BooleanFormula> orOperands) {
        for (BooleanFormula operand : orOperands) {
          if (selectionInfo.containsKey(operand)) {
            return Optional.of(operand);
          }
        }
        return Optional.absent();
      }

      private TraversalProcess selectChildren(List<BooleanFormula> operands) {
        if (removalSelectionStrategy == SELECTION_STRATEGY.FIRST) {
          BooleanFormula selected = operands.iterator().next();
          return TraversalProcess.custom(ImmutableSet.<Formula>of(selected));
        } else if (removalSelectionStrategy == SELECTION_STRATEGY.RANDOM) {
          int rand = r.nextInt(operands.size());
          return TraversalProcess.custom(operands.subList(rand, rand + 1));
        } else if (removalSelectionStrategy == SELECTION_STRATEGY.LEAST_REMOVALS) {

          BooleanFormula out = Collections.min(operands, new Comparator<BooleanFormula>() {
            @Override
            public int compare(BooleanFormula o1, BooleanFormula o2) {
              return Integer.compare(recursivelyCallSelf(o1).size(), recursivelyCallSelf(o2).size());
            }
          });

          usedLogger.log(Level.FINE, "Choosing ",
              abstractSelectors(out, selectionInfo, ImmutableSet.<BooleanFormula>of()));
          return TraversalProcess.custom(ImmutableSet.of(out));
        } else {
          assert removalSelectionStrategy == SELECTION_STRATEGY.ALL;
          return TraversalProcess.CONTINUE;
        }
      }

      private List<BooleanFormula> recursivelyCallSelf(BooleanFormula f) {

        // Use NullLogManager to avoid log pollution.
        return getSelectorsToAbstract(toAbstract, m, selectionInfo, annotated, f,
                      NullLogManager.getInstance());
      }

    }, primed);

    return newToAbstract;
  }


  /**
   * Implements the destructive algorithm for MUS extraction.
   * Starts with everything abstracted ("true" is inductive),
   * remove selectors which can be removed while keeping the overall query
   * inductive.
   *
   * <p>This is a standard algorithm, however it pays the cost of N SMT calls
   * upfront.
   * Note that since at every iteration the set of abstracted variables is
   * inductive, the algorithm can be terminated early.
   *
   * @param selectionInfo Mapping from selection variables
   *    to the atoms (possibly w/ negation) they represent.
   * @param selectionVars List of selection variables, already determined to
   *    be inductive.
   *    The order is very important and determines which MUS we will get out.
   *
   * @return Set of selectors which correspond to atoms which *should*
   */
  private Set<BooleanFormula> destructiveWeakening0(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      List<BooleanFormula> selectionVars,
      BooleanFormula query
  ) throws SolverException, InterruptedException {

    query = fmgr.simplify(query);
    List<BooleanFormula> abstractedSelectors = selectionVars;

    try (ProverEnvironment env = solver.newProverEnvironment()) {

      //noinspection ResultOfMethodCallIgnored
      env.push(query);

      if (env.isUnsat()) {
        // Questionable, but very useful for testing.
        logger.log(Level.INFO, "Everything is inductive under the transition!",
            "That looks suspicious");
        return ImmutableSet.of();
      }

      // Make everything abstracted.
      BooleanFormula selectionFormula = bfmgr.and(abstractedSelectors);

      //noinspection ResultOfMethodCallIgnored
      env.push(selectionFormula);

      if (!env.isUnsat()) {

        // No non-trivial assignment exists: rely on the caller to return
        // the trivial environment "true".
        return new HashSet<>(selectionVars);
      }

      // Remove the selection constraint.
      env.pop();

      int noRemoved = 0;
      for (int i=0; i<selectionVars.size(); i++) {
        if (destructiveIterationLimit != -1 && i == destructiveIterationLimit) {
          // Terminate early.
          break;
        }

        // Remove this variable from the selection.
        List<BooleanFormula> newSelection = Lists.newArrayList(abstractedSelectors);

        BooleanFormula selVar = selectionVars.get(i);
        Verify.verify(selVar.equals(newSelection.get(i - noRemoved)));

        // Try removing the corresponding element from the selection.
        newSelection.remove(i - noRemoved);

        logger.log(Level.FINE, "Attempting to add an atom",
            selectionInfo.get(selVar));

        //noinspection ResultOfMethodCallIgnored
        env.push(bfmgr.and(newSelection));

        if (env.isUnsat()) {

          // Still unsat: keep that element non-abstracted.
          abstractedSelectors = newSelection;
          noRemoved++;
        } else {
          logger.log(Level.FINE, "Query became non-inductive: not adding the atom");
        }

        env.pop();
      }

      //noinspection ResultOfMethodCallIgnored
      env.push(bfmgr.and(abstractedSelectors));

      Verify.verify(env.isUnsat());
    }
    return new HashSet<>(abstractedSelectors);
  }

  /**
   * Sort selectors by syntacticWeakening similarity, variables most similar to the
   * transition relation come last.
   *
   * todo: might be a good idea to use the information about the variables
   * which get _changed_ inside the transition as well.
   */
  private List<BooleanFormula> sortBySyntacticSimilarity(
      final Map<BooleanFormula, BooleanFormula> selectors,
      Collection<BooleanFormula> inductiveSlice,
      BooleanFormula transitionRelation
  ) {

    final Set<String> transitionVars = fmgr.extractFunctionNames(
        fmgr.uninstantiate(transitionRelation));
    List<BooleanFormula> selectorVars = new ArrayList<>(inductiveSlice);
    Collections.sort(selectorVars, new Comparator<BooleanFormula>() {
      @Override
      public int compare(BooleanFormula s1, BooleanFormula s2) {
        BooleanFormula a1 = selectors.get(s1);
        BooleanFormula a2 = selectors.get(s2);

        // todo: incessant re-uninstantiation is inefficient.
        Set<String> a1Vars = fmgr.extractFunctionNames(fmgr.uninstantiate(a1));
        Set<String> a2Vars = fmgr.extractFunctionNames(fmgr.uninstantiate(a2));

        Set<String> intersection1 = Sets.intersection(a1Vars, transitionVars);
        Set<String> intersection2 = Sets.intersection(a2Vars, transitionVars);

        return Integer.compare(intersection1.size(), intersection2.size());
      }
    });
    return selectorVars;
  }

  /**
   * (and a_1 a_2 a_3 ...)
   * -> gets converted to ->
   * (and (or p_1 a_1) ...)
   */
  private class LiteralsAnnotator
      extends BooleanFormulaManagerView.BooleanFormulaTransformationVisitor {
    private final UniqueIdGenerator controllerIdGenerator =
        new UniqueIdGenerator();
    private final Map<BooleanFormula, BooleanFormula> selectionVars;

    private static final String PROP_VAR = "_FS_SEL_VAR_";

    protected LiteralsAnnotator(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache,

        // Selection variable -> controlled atom.
        Map<BooleanFormula, BooleanFormula> pSelectionVars) {
      super(pFmgr, pCache);
      selectionVars = pSelectionVars;
    }

    @Override
    public BooleanFormula visitAtom(BooleanFormula atom, FunctionDeclaration decl) {
      return bfmgr.or(makeFreshSelector(atom), atom);
    }

    private BooleanFormula makeFreshSelector(BooleanFormula atom) {
      BooleanFormula selector = bfmgr
          .makeVariable(PROP_VAR + controllerIdGenerator.getFreshId());

      selectionVars.put(selector, atom);
      return selector;
    }
  }


  /**
   * Perform NNF.
   *
   * <p>Try to perform QE_LIGHT.
   * For those where it did not work, abstract away atoms which contain
   * bound variables.
   */
  private BooleanFormula removeIntermediate(final PathFormula input)
      throws InterruptedException {

    BooleanFormula nnfied = fmgr.applyTactic(input.getFormula(), Tactic.NNF);

    // Run cheap QE if needed.
    BooleanFormula quantifiedUpdated;
    if (performLightQE) {
      BooleanFormula quantified = fmgr.quantifyDeadVariables(
          nnfied,
          input.getSsa()
      );
      if (quantified == nnfied) {

        // No intermediate variables were found.
        return quantified;
      }
      try {
        statistics.quantifierEliminationTime.start();
        quantifiedUpdated = fmgr.applyTactic(quantified, Tactic.QE_LIGHT);
      } finally {
        statistics.quantifierEliminationTime.stop();
      }
    } else {
      quantifiedUpdated = nnfied;
    }

    BooleanFormula out =  bfmgr.visit(
        new BooleanFormulaTransformationVisitor(fmgr, new HashMap<BooleanFormula, BooleanFormula>()) {
          @Override
          public BooleanFormula visitAtom(BooleanFormula pAtom, FunctionDeclaration decl) {
            if (
                containsBoundVariables(pAtom)
                // TODO: is the line below necessary?
                    || !fmgr.getDeadFunctionNames(pAtom, input.getSsa()).isEmpty()
                ) {
              return bfmgr.makeBoolean(true);
            } else {
              return pAtom;
            }
          }
        }, quantifiedUpdated);

    return fmgr.simplify(out);
  }

  private boolean containsBoundVariables(BooleanFormula atom) {
    final AtomicBoolean containsBound = new AtomicBoolean(false);
    fmgr.visitRecursively(
        new DefaultFormulaVisitor<TraversalProcess>() {
          @Override
          protected TraversalProcess visitDefault(Formula f) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitBoundVariable(Formula f,
              int deBruijnIdx) {
            containsBound.set(true);
            return TraversalProcess.ABORT;
          }}, atom);
    return containsBound.get();
  }

  // TODO: return the constant part as well.

  /**
   * Factorization-based strategy for weakening, which splits the formula in two.
   */
  private BooleanFormula factorizationWeakening(
      PathFormula input,
      PathFormula transition,
      BooleanFormula strengthening
  ) throws SolverException, InterruptedException {
    if (input.getFormula().equals(bfmgr.makeBoolean(true))) {
      return bfmgr.makeBoolean(true);
    }

    BooleanFormula factored = factorCommonTermOut(input.getFormula());

    Set<BooleanFormula> semiClauses = getConjunctionArgs(factored);

    if (semiClauses.isEmpty()) {
      return bfmgr.makeBoolean(true);
    }

    Set<BooleanFormula> candidateSemiClauses = new HashSet<>();

    // Filter out those clauses which have intermediate variables.
    for (BooleanFormula c : semiClauses) {
      // TODO: add light quantifier elimination.
      if (fmgr.getDeadFunctionNames(c, input.getSsa()).isEmpty()) {
        candidateSemiClauses.add(c);
      }
    }

    Set<BooleanFormula> annotatedSemiClauses = new HashSet<>();
    Map<BooleanFormula, BooleanFormula> selectionInfo = new HashMap<>();

    // Annotate each semi-clause with a selector.
    int i = 0;
    for (BooleanFormula c : candidateSemiClauses) {
      BooleanFormula selector = bfmgr.makeVariable("_WEAKENING_SEL_" + i++);
      selectionInfo.put(selector, c);
      annotatedSemiClauses.add(bfmgr.or(selector, c));
    }

    BooleanFormula phi = bfmgr.and(annotatedSemiClauses);
    BooleanFormula phiPrimed = fmgr.instantiate(
        fmgr.uninstantiate(phi), transition.getSsa());

    BooleanFormula query = bfmgr.and(
        ImmutableList.of(
            phi,
            strengthening,
            transition.getFormula(),
            bfmgr.not(phiPrimed)
        )
    );

    Set<BooleanFormula> literalsToAbstract = runHoudini(selectionInfo,
        transition.getSsa(), query);
    return abstractSelectors(fmgr.uninstantiate(phi), selectionInfo, literalsToAbstract);
  }

  /**
   * @return Set of selectors which should be abstracted.
   */
  private Set<BooleanFormula> runHoudini(
      Map<BooleanFormula, BooleanFormula> selectionInfo,
      SSAMap finalSSA,
      BooleanFormula query
  ) throws SolverException, InterruptedException {

    Set<BooleanFormula> selectorsToAbstract = new HashSet<>();

    try (ProverEnvironment env = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      env.push();
      env.addConstraint(query);
      env.push();
      for (BooleanFormula selector : selectionInfo.keySet()) {
        env.addConstraint(bfmgr.not(selector));
      }
      env.push();

      while (!env.isUnsat()) {
        Model m = env.getModel();

        for (Entry<BooleanFormula, BooleanFormula> entry : selectionInfo.entrySet()) {
          BooleanFormula selectorKey = entry.getKey();
          BooleanFormula semiClause = entry.getValue();

          BooleanFormula primedSemiClause = fmgr.instantiate(
              fmgr.uninstantiate(semiClause), finalSSA);
          Boolean val = m.evaluate(primedSemiClause);
          // TODO: careful, might be useful to use negation to exploit don't-matters.
          if (val != null && !val) {
            selectorsToAbstract.add(selectorKey);
          }
        }

        env.pop();

        // TODO: streamline, use solve-with-assumptions interface.
        for (BooleanFormula sel : selectionInfo.keySet()) {
          if (selectorsToAbstract.contains(sel)) {
            env.addConstraint(sel);
          } else {
            env.addConstraint(bfmgr.not(sel));
          }
        }
        env.push();
      }
    }

    return selectorsToAbstract;
  }

  private BooleanFormula factorCommonTermOut(BooleanFormula input) {

    return bfmgr.visit(new BooleanFormulaTransformationVisitor(fmgr, new HashMap<BooleanFormula,
        BooleanFormula>()) {

      @Override
      public BooleanFormula visitOr(List<BooleanFormula> pOperands) {
        List<BooleanFormula> processed = visitIfNotSeen(pOperands);

        Set<BooleanFormula> intersection = null;
        ArrayList<Set<BooleanFormula>> argsReceived = new ArrayList<>();
        for (BooleanFormula op : processed) {
          Set<BooleanFormula> args = getConjunctionArgs(op);

          if (args.isEmpty()) {
            // Fail fast.
            return bfmgr.or(processed);
          }

          argsReceived.add(args);
          if (intersection == null) {
            intersection = args;
          } else {
            intersection = Sets.intersection(intersection, args);
          }
        }

        if (intersection != null && !intersection.isEmpty()) {
          BooleanFormula head = bfmgr.and(intersection);
          List<BooleanFormula> options = new ArrayList<>();
          for (Set<BooleanFormula> args : argsReceived) {
            options.add(bfmgr.and(Sets.difference(args, intersection)));
          }
          return bfmgr.and(
              head,
              bfmgr.or(options)
          );
        } else {
          return bfmgr.or(processed);
        }
      }

    }, input);
  }

  private Set<BooleanFormula> getConjunctionArgs(BooleanFormula f) {
    return bfmgr.visit(new DefaultBooleanFormulaVisitor<Set<BooleanFormula>>() {
      @Override
      protected Set<BooleanFormula> visitDefault() {
        return ImmutableSet.of();
      }

      @Override
      public Set<BooleanFormula> visitAnd(List<BooleanFormula> operands) {
        return ImmutableSet.copyOf(operands);
      }
    }, f);
  }
}
