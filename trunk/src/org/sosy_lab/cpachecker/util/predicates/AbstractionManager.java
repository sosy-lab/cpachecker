// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.base.Joiner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager.VariableOrderingStrategy;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class stores a mapping between abstract regions and the corresponding symbolic formula. It
 * is therefore the bridge between the abstract and the symbolic "worlds". It is also responsible
 * for the creation of {@link AbstractionPredicate}s.
 */
@Options(prefix = "cpa.predicate")
public final class AbstractionManager {

  /**
   * This enum represents the different strategies available for sorting the bdd variables that
   * store predicates during the predicate analysis.
   */
  private enum PredicateOrderingStrategy {
    CHRONOLOGICAL(null), // do not execute any reordering, variables are in creation order
    FRAMEWORK_RANDOM(VariableOrderingStrategy.RANDOM),
    FRAMEWORK_SIFT(VariableOrderingStrategy.SIFT),
    FRAMEWORK_SIFTITE(VariableOrderingStrategy.SIFTITE),
    FRAMEWORK_WIN2(VariableOrderingStrategy.WIN2),
    FRAMEWORK_WIN2ITE(VariableOrderingStrategy.WIN2ITE),
    FRAMEWORK_WIN3(VariableOrderingStrategy.WIN3),
    FRAMEWORK_WIN3ITE(VariableOrderingStrategy.WIN3ITE);

    final @Nullable VariableOrderingStrategy frameworkStrategy;

    PredicateOrderingStrategy(VariableOrderingStrategy pFrameworkStrategy) {
      frameworkStrategy = pFrameworkStrategy;
    }
  }

  private final LogManager logger;
  private final RegionManager rmgr;
  private final FormulaManagerView fmgr;

  // Here we keep the mapping abstract predicate variable -> predicate
  private final Map<Region, AbstractionPredicate> absVarToPredicate = new HashMap<>();
  // and the mapping symbolic variable -> predicate
  private final Map<BooleanFormula, AbstractionPredicate> symbVarToPredicate = new HashMap<>();
  // and the mapping atom -> predicate
  private final Map<BooleanFormula, AbstractionPredicate> atomToPredicate = new HashMap<>();

  // Properties for BDD variable ordering:
  @Option(secure = true, name = "abs.predicateOrdering.method", description = "Predicate ordering")
  private PredicateOrderingStrategy varOrderMethod = PredicateOrderingStrategy.CHRONOLOGICAL;

  private final Map<Region, BooleanFormula> toConcreteCache;

  @SuppressFBWarnings(
      value = "VO_VOLATILE_INCREMENT",
      justification =
          "Class is not thread-safe, but concurrent read access to this variable is needed for the"
              + " MBean")
  private volatile int numberOfPredicates = 0;

  @Option(
      secure = true,
      name = "abs.useCache",
      description = "use caching of region to formula conversions")
  private boolean useCache = true;

  private BooleanFormulaManagerView bfmgr;

  public AbstractionManager(
      RegionManager pRmgr, Configuration config, LogManager pLogger, Solver pSolver)
      throws InvalidConfigurationException {
    config.inject(this, AbstractionManager.class);
    logger = pLogger;
    rmgr = pRmgr;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();

    if (useCache) {
      toConcreteCache = new HashMap<>();
    } else {
      toConcreteCache = null;
    }

    // don't store it, we wouldn't know when to unregister anyway
    new AbstractionPredicatesMBean().register();
  }

  public int getNumberOfPredicates() {
    return numberOfPredicates;
  }

  /** creates a Predicate from the Boolean symbolic variable (var) and the atom that defines it */
  @SuppressWarnings("NonAtomicVolatileUpdate") // no thread-safe anyway
  public AbstractionPredicate makePredicate(BooleanFormula atom) {
    AbstractionPredicate result = atomToPredicate.get(atom);

    if (result == null) {
      checkArgument(
          atom.equals(fmgr.uninstantiate(atom)),
          "Regions and AbstractionPredicates should always represent uninstantiated formula, "
              + "but attempting to create predicate for instantiated formula %s",
          atom);

      BooleanFormula symbVar = fmgr.createPredicateVariable("PRED" + numberOfPredicates);
      Region absVar =
          (rmgr instanceof SymbolicRegionManager)
              ? ((SymbolicRegionManager) rmgr).createPredicate(atom)
              : rmgr.createPredicate();

      logger.log(
          Level.FINEST, "Created predicate", absVar, "from variable", symbVar, "and atom", atom);

      result = new AbstractionPredicate(absVar, symbVar, atom);
      symbVarToPredicate.put(symbVar, result);
      absVarToPredicate.put(absVar, result);
      atomToPredicate.put(atom, result);

      numberOfPredicates++;
    }

    return result;
  }

  /** Reorders the BDD variables. */
  public void reorderPredicates() {
    if (varOrderMethod != PredicateOrderingStrategy.CHRONOLOGICAL) {
      rmgr.reorder(verifyNotNull(varOrderMethod.frameworkStrategy));
    }
  }

  /** creates a Predicate that represents "false" */
  public AbstractionPredicate makeFalsePredicate() {
    return makePredicate(bfmgr.makeFalse());
  }

  /**
   * Get predicate corresponding to a variable.
   *
   * @param var A symbolic formula representing the variable. The same formula has to been passed to
   *     makePredicate earlier.
   * @return a Predicate
   */
  public AbstractionPredicate getPredicate(BooleanFormula var) {
    AbstractionPredicate result = symbVarToPredicate.get(var);
    if (result == null) {
      throw new IllegalArgumentException(
          var + " seems not to be a formula corresponding to a single predicate variable.");
    }
    return result;
  }

  /**
   * Convert a Region (typically a BDD over the AbstractionPredicates) into a BooleanFormula (an SMT
   * formula). Each predicate is replaced by its corresponding SMT definition ({@link
   * AbstractionPredicate#getSymbolicAtom()}).
   *
   * <p>The inverse of this method is {@link #convertFormulaToRegion(BooleanFormula)}, except in
   * cases where the predicates in the given regions do not correspond to SMT atoms but to larger
   * SMT formulas.
   *
   * @param af A Region.
   * @return An uninstantiated BooleanFormula.
   */
  public BooleanFormula convertRegionToFormula(Region af) {
    if (rmgr instanceof SymbolicRegionManager) {
      // optimization shortcut
      return ((SymbolicRegionManager) rmgr).toFormula(af);
    }

    Map<Region, BooleanFormula> cache;
    if (useCache) {
      cache = toConcreteCache;
    } else {
      cache = new HashMap<>();
    }
    Deque<Region> toProcess = new ArrayDeque<>();

    cache.put(rmgr.makeTrue(), bfmgr.makeTrue());
    cache.put(rmgr.makeFalse(), bfmgr.makeFalse());

    toProcess.push(af);
    while (!toProcess.isEmpty()) {
      Region n = toProcess.peek();
      if (cache.containsKey(n)) {
        toProcess.pop();
        continue;
      }
      boolean childrenDone = true;
      BooleanFormula m1 = null;
      BooleanFormula m2 = null;

      Triple<Region, Region, Region> parts = rmgr.getIfThenElse(n);
      Region c1 = parts.getSecond();
      Region c2 = parts.getThird();
      if (!cache.containsKey(c1)) {
        toProcess.push(c1);
        childrenDone = false;
      } else {
        m1 = cache.get(c1);
      }
      if (!cache.containsKey(c2)) {
        toProcess.push(c2);
        childrenDone = false;
      } else {
        m2 = cache.get(c2);
      }
      if (childrenDone) {
        assert m1 != null;
        assert m2 != null;

        toProcess.pop();
        Region var = parts.getFirst();

        AbstractionPredicate pred = absVarToPredicate.get(var);
        assert pred != null : var;
        BooleanFormula atom = pred.getSymbolicAtom();

        if (bfmgr.isTrue(m1)) {
          if (bfmgr.isFalse(m2)) {
            // ITE(atom, true, false) <==> atom
            cache.put(n, atom);
          } else {
            // ITE(atom, true, m2) <==> (atom || m2)
            cache.put(n, bfmgr.or(atom, m2));
          }
        } else if (bfmgr.isFalse(m1)) {
          if (bfmgr.isTrue(m2)) {
            // ITE(atom, false, true) <==> !atom
            cache.put(n, bfmgr.not(atom));
          } else {
            // ITE(atom, false, m2) <==> (!atom && m2)
            cache.put(n, bfmgr.and(bfmgr.not(atom), m2));
          }
        } else {
          if (bfmgr.isTrue(m2)) {
            // ITE(atom, m1, true) <==> (!atom || m1)
            cache.put(n, bfmgr.or(bfmgr.not(atom), m1));
          } else if (bfmgr.isFalse(m2)) {
            // ITE(atom, m1, false) <==> (atom && m1)
            cache.put(n, bfmgr.and(atom, m1));
          } else {
            // ITE(atom, m1, m2)
            cache.put(n, bfmgr.ifThenElse(atom, m1, m2));
          }
        }
      }
    }

    BooleanFormula result = cache.get(af);
    assert result != null;

    return result;
  }

  /**
   * checks whether the data region represented by f1 is a subset of that represented by f2
   *
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return true if (f1 => f2), false otherwise
   */
  public boolean entails(Region f1, Region f2) throws SolverException, InterruptedException {
    return rmgr.entails(f1, f2);
  }

  /**
   * Return the set of predicates that occur in a region.
   *
   * <p>Note: this method currently fails with SymbolicRegionManager, and it probably cannot really
   * be fixed either, because when using symbolic regions we do not know what are the predicates (a
   * predicate does not need to be an SMT atom, it can be larger).
   *
   * <p>Thus better avoid using this method if possible.
   */
  public Set<AbstractionPredicate> extractPredicates(Region af) {
    Set<AbstractionPredicate> vars = new HashSet<>();

    Deque<Region> toProcess = new ArrayDeque<>();
    toProcess.push(af);
    while (!toProcess.isEmpty()) {
      Region n = toProcess.pop();

      if (n.isTrue() || n.isFalse()) {
        continue;
      }

      AbstractionPredicate pred = absVarToPredicate.get(n);

      if (pred == null) {
        Triple<Region, Region, Region> parts = rmgr.getIfThenElse(n);

        Region var = parts.getFirst();
        pred = absVarToPredicate.get(var);
        assert pred != null;

        toProcess.push(parts.getSecond());
        toProcess.push(parts.getThird());
      }

      vars.add(pred);
    }

    return vars;
  }

  /**
   * Convert a BooleanFormula (an SMT formula) into a Region (typically a BDD over predicates). Each
   * atom of the BooleanFormula will be one predicate of the Region. To allow more control over what
   * is represented by each predicate, use {@link #makePredicate(BooleanFormula)} and construct the
   * region out of the predicates using {@link #getRegionCreator()}.
   *
   * <p>The inverse of this function is {@link #convertRegionToFormula(Region)}.
   *
   * @param pF An uninstantiated BooleanFormula.
   * @return A region that represents the same state space.
   */
  public Region convertFormulaToRegion(BooleanFormula pF) {
    // Note: Depending on the implementation of RegionManger.fromFormula(),
    // the callback will be used or not and we will end up with the atoms from the formula
    // as AbstractionPredicates or not.
    // This class does not care whether this happens, if the RegionManager implementation
    // can work without AbstractionPredicates for each atom so can we.
    // This will affect statistics, however.
    return rmgr.fromFormula(
        pF,
        fmgr,
        atom -> {
          if (atomToPredicate.containsKey(atom)) {
            return atomToPredicate.get(atom).getAbstractVariable();
          }
          return makePredicate(atom).getAbstractVariable();
        });
  }

  public RegionManager getRegionCreator() {
    return rmgr;
  }

  public interface AbstractionPredicatesMXBean {

    int getNumberOfPredicates();

    String getPredicates();
  }

  private class AbstractionPredicatesMBean extends AbstractMBean
      implements AbstractionPredicatesMXBean {

    public AbstractionPredicatesMBean() {
      super("org.sosy_lab.cpachecker:type=predicate,name=AbstractionPredicates", logger);
    }

    @Override
    public int getNumberOfPredicates() {
      return numberOfPredicates;
    }

    @Override
    public String getPredicates() {
      // TODO this may run into a ConcurrentModificationException
      return Joiner.on('\n').join(absVarToPredicate.values());
    }
  }
}
