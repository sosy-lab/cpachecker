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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

/**
 * This class stores a mapping between abstract regions and the corresponding
 * symbolic formula. It is therefore the bridge between the abstract and the
 * symbolic "worlds".
 * It is also responsible for the creation of {@link AbstractionPredicate}s.
 */
@Options(prefix = "cpa.predicate")
public final class AbstractionManager {
  private final LogManager logger;
  private final RegionManager rmgr;
  private final FormulaManagerView fmgr;
  private final Solver solver;
  // Here we keep the mapping abstract predicate variable -> predicate
  private final Map<Region, AbstractionPredicate> absVarToPredicate = Maps.newHashMap();
  // and the mapping symbolic variable -> predicate
  private final Map<BooleanFormula, AbstractionPredicate> symbVarToPredicate = Maps.newHashMap();
  // and the mapping atom -> predicate
  private final Map<BooleanFormula, AbstractionPredicate> atomToPredicate = Maps.newHashMap();

  // Properties for BDD variable ordering:
  @Option(secure = true, name = "abs.predicateOrdering.method",
      description = "Predicate ordering")
  private PredicateOrderingStrategy varOrderMethod = PredicateOrderingStrategy.CHRONOLOGICAL;
  // mapping predicate variable -> partition containing predicates with this predicate variable
  private final HashMap<String, PredicatePartition> predVarToPartition = new HashMap<>();
  // and mapping partition ID -> set of predicate variables covered by partition
  private final HashMap<Integer, HashSet<String>> partitionIDToPredVars = new HashMap<>();
  private PredicatePartition partition;
  @Option(secure = true, name = "abs.predicateOrdering.partitions",
      description = "Use multiple partitions for predicates")
  private boolean multiplePartitions = false;
  private final LinkedList<Integer> randomListOfVarIDs = new LinkedList<>();

  private final Map<Region, BooleanFormula> toConcreteCache;

  @SuppressFBWarnings(value = "VO_VOLATILE_INCREMENT",
      justification = "Class is not thread-safe, but concurrent read access to this variable is needed for the MBean")
  private volatile int numberOfPredicates = 0;

  @Option(secure = true, name = "abs.useCache", description = "use caching of region to formula conversions")
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
    solver = pSolver;

    if (!this.varOrderMethod.getIsFrameworkStrategy()) {
      this.partition = createNewPredicatePartition();
    }

    if (useCache) {
      toConcreteCache = new HashMap<>();
    } else {
      toConcreteCache = null;
    }

    new AbstractionPredicatesMBean(); // don't store it, we wouldn't know when to unregister anyway
  }

  /**
   * Creates a new predicate partition based on the property varOrderMethod.
   *
   * @return a new predicate partition.
   */
  private PredicatePartition createNewPredicatePartition() {
    switch (this.varOrderMethod) {
      case SIMILARITY:
        return new PredicatePartitionSimilarity(fmgr, solver, logger);
      case FREQUENCY:
        return new PredicatePartitionFrequency(fmgr, solver, logger);
      case IMPLICATION:
        return new PredicatePartitionImplication(fmgr, solver, logger);
      case REV_IMPLICATION:
        return new PredicatePartitionRevImplication(fmgr, solver, logger);
      default:
        return new PredicatePartitionSimilarity(fmgr, solver, logger);
    }
  }

  public int getNumberOfPredicates() {
    return numberOfPredicates;
  }

  /**
   * creates a Predicate from the Boolean symbolic variable (var) and the atom that defines it
   */
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

      logger.log(Level.FINEST, "Created predicate", absVar, "from variable", symbVar, "and atom", atom);

      result = new AbstractionPredicate(absVar, symbVar, atom, numberOfPredicates);
      symbVarToPredicate.put(symbVar, result);
      absVarToPredicate.put(absVar, result);
      atomToPredicate.put(atom, result);

      if (!this.varOrderMethod.getIsFrameworkStrategy()) {
        if (varOrderMethod.equals(PredicateOrderingStrategy.RANDOMLY)) {
          int randomIndex = new Random().nextInt(randomListOfVarIDs.size() + 1);
          randomListOfVarIDs.add(randomIndex, numberOfPredicates);
        } else if (multiplePartitions) {
          updatePartitions(result);
        } else {
          this.partition.insertPredicate(result);
        }
      }

      numberOfPredicates++;
    }

    return result;
  }

  /**
   * Calculates the partition(s) the new predicate is similar to and inserts the new predicate. If there is no similar
   * partition found, a new partition containing the new predicate will be created.
   * If there is more than one partition similar to the new predicate, the similar partitions are merged together and
   * the new predicate is inserted into the partition that results of the merging.
   *
   * @param newPredicate the new predicate as symbolic atom.
   */
  private void updatePartitions(AbstractionPredicate newPredicate) {
    Set<String> predVars = fmgr.extractVariableNames(newPredicate.getSymbolicAtom());
    PredicatePartition firstPartition;

    // this is the case if the new predicate contains just "false" or "true"
    // create an own partition for that
    if (predVars.isEmpty()) {
      firstPartition = createNewPredicatePartition();
      predVarToPartition.put(newPredicate.getSymbolicAtom().toString(), firstPartition);
      partitionIDToPredVars.put(firstPartition.getPartitionID(), new HashSet<>());
    } else {
      HashSet<String> predVarsCoveredByPartition = new HashSet<>(predVars);

      // check which variables of the predicate are covered by the partitions.
      Set<String> varIntersection = new HashSet<>(predVars);
      varIntersection.retainAll(predVarToPartition.keySet());

      if (!varIntersection.isEmpty()) {
        // merge partitions that are similar to the new predicate
        Iterator<String> varIterator = varIntersection.iterator();
        firstPartition = predVarToPartition.get(varIterator.next());
        predVarsCoveredByPartition.addAll(partitionIDToPredVars.get(firstPartition.getPartitionID()));

        while (varIterator.hasNext()) {
          PredicatePartition nextPartition = predVarToPartition.get(varIterator.next());
          firstPartition = firstPartition.merge(nextPartition);
          predVarsCoveredByPartition.addAll(partitionIDToPredVars.get(nextPartition.getPartitionID()));
        }
      } else {
        firstPartition = createNewPredicatePartition();
      }

      // update predicate variables covered by the new partition
      partitionIDToPredVars.put(firstPartition.getPartitionID(), predVarsCoveredByPartition);
      // update keys such that they point to the result partition of the merging
      for (String predVar : predVarsCoveredByPartition) {
        predVarToPartition.put(predVar, firstPartition);
      }
    }

    firstPartition.insertPredicate(newPredicate);
  }

  /**
   * Reorders the BDD variables.
   */
  public void reorderPredicates() {
    if (this.varOrderMethod.getIsFrameworkStrategy()) {
      rmgr.reorder(this.varOrderMethod);
    } else {
      ArrayList<Integer> predicateOrdering = new ArrayList<>(numberOfPredicates);
      if (varOrderMethod.equals(PredicateOrderingStrategy.RANDOMLY)) {
        predicateOrdering.addAll(randomListOfVarIDs);
      } else if (multiplePartitions) {
        HashSet<PredicatePartition> partitions = new HashSet<>(predVarToPartition.values());
        for (PredicatePartition partition : partitions) {
          List<AbstractionPredicate> predicates = partition.getPredicates();

          for (AbstractionPredicate predicate : predicates) {
            predicateOrdering.add(predicate.getVariableNumber());
          }
        }
      } else {
        List<AbstractionPredicate> predicates = partition.getPredicates();
        for (AbstractionPredicate predicate : predicates) {
          predicateOrdering.add(predicate.getVariableNumber());
        }
      }

      rmgr.setVarOrder(predicateOrdering);
    }
  }

  /**
   * creates a Predicate that represents "false"
   */
  public AbstractionPredicate makeFalsePredicate() {
    return makePredicate(bfmgr.makeFalse());
  }

  /**
   * Get predicate corresponding to a variable.
   *
   * @param var A symbolic formula representing the variable. The same formula has to been passed to makePredicate
   * earlier.
   * @return a Predicate
   */
  public AbstractionPredicate getPredicate(BooleanFormula var) {
    AbstractionPredicate result = symbVarToPredicate.get(var);
    if (result == null) {
      throw new IllegalArgumentException(
          var
              + " seems not to be a formula corresponding to a single predicate variable.");
    }
    return result;
  }

  /**
   * Convert a Region (typically a BDD over the AbstractionPredicates)
   * into a BooleanFormula (an SMT formula).
   * Each predicate is replaced by its corresponding SMT definition
   * ({@link AbstractionPredicate#getSymbolicAtom()}).
   *
   * The inverse of this method is {@link #convertFormulaToRegion(BooleanFormula)},
   * except in cases where the predicates in the given regions do not correspond to SMT atoms
   * but to larger SMT formulas.
   *
   * @param af A Region.
   * @return An uninstantiated BooleanFormula.
   */
  public BooleanFormula convertRegionToFormula(Region af) {
    if (rmgr instanceof SymbolicRegionManager) {
      // optimization shortcut
      return ((SymbolicRegionManager)rmgr).toFormula(af);
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
  public boolean entails(Region f1, Region f2) throws SolverException,
      InterruptedException {
    return rmgr.entails(f1, f2);
  }

  /**
   * Return the set of predicates that occur in a region.
   *
   * Note: this method currently fails with SymbolicRegionManager,
   * and it probably cannot really be fixed either, because when using symbolic regions
   * we do not know what are the predicates (a predicate does not need to be an SMT atom,
   * it can be larger).
   *
   * Thus better avoid using this method if possible.
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
   * Convert a BooleanFormula (an SMT formula) into a Region (typically a BDD over predicates).
   * Each atom of the BooleanFormula will be one predicate of the Region.
   * To allow more control over what is represented by each predicate,
   * use {@link #makePredicate(BooleanFormula)} and construct the region out of the predicates
   * using {@link #getRegionCreator()}.
   *
   * The inverse of this function is {@link #convertRegionToFormula(Region)}.
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
    return rmgr.fromFormula(pF, fmgr,
        new Function<BooleanFormula, Region>() {
          @Override
          public Region apply(BooleanFormula pInput) {
            if (atomToPredicate.containsKey(pInput)) {
              return atomToPredicate.get(pInput).getAbstractVariable();
            }
            return makePredicate(pInput).getAbstractVariable();
          }
        });
  }

  public RegionCreator getRegionCreator() {
    return rmgr;
  }

  public static interface AbstractionPredicatesMXBean {

    int getNumberOfPredicates();

    String getPredicates();
  }

  private class AbstractionPredicatesMBean extends AbstractMBean implements
      AbstractionPredicatesMXBean {

    public AbstractionPredicatesMBean() {
      super(
          "org.sosy_lab.cpachecker:type=predicate,name=AbstractionPredicates",
          logger);
      register();
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
