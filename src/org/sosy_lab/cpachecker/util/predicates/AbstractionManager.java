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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager.RegionBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

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
  // Here we keep the mapping abstract predicate variable -> predicate
  private final Map<Region, AbstractionPredicate> absVarToPredicate = Maps.newHashMap();
  // and the mapping symbolic variable -> predicate
  private final Map<BooleanFormula, AbstractionPredicate> symbVarToPredicate = Maps.newHashMap();
  // and the mapping atom -> predicate
  private final Map<BooleanFormula, AbstractionPredicate> atomToPredicate = Maps.newHashMap();
  // and the mapping varID -> predicate
  private final Map<Integer, AbstractionPredicate> varIDToPredicate = Maps
      .newHashMap();
  private final Map<Region, BooleanFormula> toConcreteCache;
  private volatile int numberOfPredicates = 0;
  @Option(secure = true, name = "abs.useCache", description = "use caching of region to formula conversions")
  private boolean useCache = true;
  private BooleanFormulaManagerView bfmgr;

  public AbstractionManager(RegionManager pRmgr, FormulaManagerView pFmgr,
      Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this, AbstractionManager.class);
    logger = pLogger;
    rmgr = pRmgr;
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();

    if (useCache) {
      toConcreteCache = new HashMap<>();
    } else {
      toConcreteCache = null;
    }

    new AbstractionPredicatesMBean(); // don't store it, we wouldn't know when to unregister anyway
  }

  public int getNumberOfPredicates() {
    return numberOfPredicates;
  }

  /**
   * creates a Predicate from the Boolean symbolic variable (var) and the atom that defines it
   */
  public AbstractionPredicate makePredicate(BooleanFormula atom) {
    AbstractionPredicate result = atomToPredicate.get(atom);

    if (result == null) {
      BooleanFormula symbVar =
          fmgr.createPredicateVariable("PRED" + numberOfPredicates);
      Region absVar = rmgr.createPredicate();

      logger.log(Level.FINEST, "Created predicate", absVar,
          "from variable", symbVar, "and atom", atom);

      result =
          new AbstractionPredicate(absVar, symbVar, atom,
              numberOfPredicates);
      symbVarToPredicate.put(symbVar, result);
      absVarToPredicate.put(absVar, result);
      atomToPredicate.put(atom, result);
      varIDToPredicate.put(numberOfPredicates, result);
      numberOfPredicates++;

      sortVarsByFrequency();
      //sortVarsBySimilarity();
    }

    return result;
  }

  private void sortVarsByFrequency() {
    final Map<String, Integer> varToFrequency = new HashMap<>();

    // calculate overall variable frequencies
    ArrayList<Integer> predicates = new ArrayList<>();
    for (int i = 0; i < numberOfPredicates; i++) {
      predicates.add(i);
      Set<String> varNames = fmgr.extractVariableNames(varIDToPredicate.get(i).getSymbolicAtom());
      for (String varName : varNames) {
        Integer currentCount = varToFrequency.get(varName);
        varToFrequency.put(varName, (currentCount == null ? 1 : ++currentCount)); // increase var entry by 1
      }
    }

    // sort predicates by variable frequencies
    Collections.sort(predicates, new Comparator<Integer>() {

      @Override
      public int compare(Integer pred1, Integer pred2) {
        Set<String> firstPredVars =
            fmgr.extractVariableNames(varIDToPredicate.get(pred1).getSymbolicAtom());
        Set<String> secondPredVars =
            fmgr.extractVariableNames(varIDToPredicate.get(pred2).getSymbolicAtom());
        int maxFirst = 0;
        for (String var : firstPredVars) {
          int currentVarFreq = varToFrequency.get(var);
          if (currentVarFreq > maxFirst) {
            maxFirst = currentVarFreq;
          }
        }
        int maxSecond = 0;
        for (String var : secondPredVars) {
          int currentVarFreq = varToFrequency.get(var);
          if (currentVarFreq > maxFirst) {
          return -1; // can break the loop as the second predicate has a higher rating
          }
          maxSecond = currentVarFreq;
        }

        return maxFirst - maxSecond;
      }});

    rmgr.setVarOrder(predicates);
  }

  private void sortVarsBySimilarity() {
    int[][] similarities =
        new int[numberOfPredicates][numberOfPredicates];
    final Map<String, Integer> varToFrequency = new HashMap<>();
    for (int i = 0; i < similarities.length; i++) {
      for (int j = 0; j < similarities[i].length; j++) {
        Set<String> firstPredVars =
            fmgr.extractVariableNames(varIDToPredicate.get(i)
                .getSymbolicAtom());
        Set<String> secondPredVars =
            fmgr.extractVariableNames(varIDToPredicate.get(j)
                .getSymbolicAtom());

        // count variables
        for (String varName : firstPredVars) {
          Integer countEntry = varToFrequency.get(varName);
          int count = countEntry == null ? 0 : countEntry;
          varToFrequency.put(varName, ++count);
        }
        for (String varName : secondPredVars) {
          Integer countEntry = varToFrequency.get(varName);
          int count = countEntry == null ? 0 : countEntry;
          varToFrequency.put(varName, ++count);
        }

        firstPredVars.retainAll(secondPredVars);
        similarities[i][j] = firstPredVars.size();
      }
    }

    List<ArrayList<Integer>> partitions = new ArrayList<>();
    int[] setIds = new int[numberOfPredicates];
    int nextSetId = 1;
    for (int i = 0; i < setIds.length; i++) {
      int thisSetID;
      ArrayList<Integer> thisPartition = new ArrayList<>();
      if (setIds[i] == 0) {
        setIds[i] = nextSetId;
        thisSetID = nextSetId++;
        thisPartition = new ArrayList<>();
        partitions.add(thisPartition);
        thisPartition.add(i);
      } else {
        thisSetID = setIds[i];
      }
      for (int j = i + 1; j < similarities[i].length; j++) {
        if (similarities[i][j] > 0) {
          setIds[j] = thisSetID;
          thisPartition.add(j);
        }
      }
    }
    Collections.sort(partitions, new Comparator<ArrayList<Integer>>() {

      @Override
      public int compare(ArrayList<Integer> pArg0,
          ArrayList<Integer> pArg1) {
        return pArg0.size() - pArg1.size();
      }

    });

    ArrayList<Integer> order = new ArrayList<>(numberOfPredicates);
    for (ArrayList<Integer> list : partitions) {
      Collections.sort(list, new Comparator<Integer>() {

        @Override
        public int compare(Integer pred1, Integer pred2) {
          Set<String> firstPredVars =
              fmgr.extractVariableNames(varIDToPredicate.get(pred1).getSymbolicAtom());
          Set<String> secondPredVars =
              fmgr.extractVariableNames(varIDToPredicate.get(pred2).getSymbolicAtom());
          int maxFirst = 0;
          for (String var : firstPredVars) {
            int currentVarFreq = varToFrequency.get(var);
            if (currentVarFreq > maxFirst) {
              maxFirst = currentVarFreq;
            }
          }
          int maxSecond = 0;
          for (String var : secondPredVars) {
            int currentVarFreq = varToFrequency.get(var);
            if (currentVarFreq > maxFirst) {
            return -1; // can break the loop as the second predicate has a higher rating
            }
            maxSecond = currentVarFreq;
          }

          return maxFirst - maxSecond;
        }

      });
      order.addAll(list);
    }

    rmgr.setVarOrder(order);
  }

  /**
   * creates a Predicate that represents "false"
   */
  public AbstractionPredicate makeFalsePredicate() {
    return makePredicate(bfmgr.makeBoolean(false));
  }

  /**
   * Get predicate corresponding to a variable.
   *
   * @param var A symbolic formula representing the variable. The same formula has to been passed to makePredicate
   * earlier.
   * @return a Predicate
   */
  private AbstractionPredicate getPredicate(BooleanFormula var) {
    AbstractionPredicate result = symbVarToPredicate.get(var);
    if (result == null) { throw new IllegalArgumentException(
        var
            + " seems not to be a formula corresponding to a single predicate variable."); }
    return result;
  }

  /**
   * Given an abstract formula (which is a BDD over the predicates), build its concrete representation (which is a
   * symbolic formula corresponding to the BDD,
   * in which each predicate is replaced with its definition)
   */
  public BooleanFormula toConcrete(Region af) {
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

    cache.put(rmgr.makeTrue(), bfmgr.makeBoolean(true));
    cache.put(rmgr.makeFalse(), bfmgr.makeBoolean(false));

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
        assert pred != null;
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
   * Return the set of predicates that occur in a a region. In some cases, this method also returns the predicate
   * 'false' in the set.
   */
  public Set<AbstractionPredicate> extractPredicates(Region af) {
    Set<AbstractionPredicate> vars = new HashSet<>();

    Deque<Region> toProcess = new ArrayDeque<>();
    toProcess.push(af);
    while (!toProcess.isEmpty()) {
      Region n = toProcess.pop();

      if (n.isTrue() || n.isFalse()) {
        vars.add(this.makeFalsePredicate());
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

  public Region buildRegionFromFormula(BooleanFormula pF) {
    return rmgr.fromFormula(pF, fmgr,
        Functions.compose(new Function<AbstractionPredicate, Region>() {

          @Override
          public Region apply(AbstractionPredicate pInput) {
            return pInput.getAbstractVariable();
          }
        }, Functions.forMap(atomToPredicate)));
  }

  public RegionCreator getRegionCreator() {
    return new RegionCreator();
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

  public class RegionCreator {

    public RegionBuilder newRegionBuilder(ShutdownNotifier pShutdownNotifier) {
      return rmgr.builder(pShutdownNotifier);
    }

    /**
     * @return a representation of logical truth
     */
    public Region makeTrue() {
      return rmgr.makeTrue();
    }

    /**
     * @return a representation of logical falseness
     */
    public Region makeFalse() {
      return rmgr.makeFalse();
    }

    /**
     * Creates a region representing a negation of the argument
     * @param f an AbstractFormula
     * @return (!f1)
     */
    public Region makeNot(Region f) {
      return rmgr.makeNot(f);
    }

    /**
     * Creates a region representing an AND of the two argument
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return (f1 & f2)
     */
    public Region makeAnd(Region f1, Region f2) {
      return rmgr.makeAnd(f1, f2);
    }

    /**
     * Creates a region representing an OR of the two argument
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return (f1 | f2)
     */
    public Region makeOr(Region f1, Region f2) {
      return rmgr.makeOr(f1, f2);
    }

    /**
     * Creates a region representing an equality (bi-implication) of the two argument
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return (f1 <=> f2)
     */
    public Region makeEqual(Region f1, Region f2) {
      return rmgr.makeEqual(f1, f2);
    }

    /**
     * Creates a region representing an if then else construct of the three arguments
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @param f3 an AbstractFormula
     * @return (if f1 then f2 else f3)
     */
    public Region makeIte(Region f1, Region f2, Region f3) {
      return rmgr.makeIte(f1, f2, f3);
    }

    /**
     * Creates a region representing an existential quantification of the two argument
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return (\exists f2: f1)
     */
    public Region makeExists(Region f1, Region f2) {
      return rmgr.makeExists(f1, f2);
    }

    public Region getPredicate(BooleanFormula var) {
      return AbstractionManager.this.getPredicate(var).getAbstractVariable();
    }
  }
}
