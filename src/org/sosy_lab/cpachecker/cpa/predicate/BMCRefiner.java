// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.ExecutionException;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

public class BMCRefiner implements Refiner {

  private final Solver solver;

  LoadingCache<PredicateAbstractState, Boolean> unsatCache;

  public BMCRefiner(ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    @SuppressWarnings("resource")
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, PredicateRefiner.class);
    solver = predicateCpa.getSolver();
    unsatCache =
        CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(
                new CacheLoader<PredicateAbstractState, Boolean>() {
                  @Override
                  public Boolean load(PredicateAbstractState predState) throws Exception {
                    return solver.isUnsat(predState.getPathFormula().getFormula());
                  }
                });
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return new BMCRefiner(pCpa);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    AbstractState lastState = pReached.getLastState();
    assert (lastState instanceof Targetable) && ((Targetable) lastState).isTarget();
    final boolean success;
    PredicateAbstractState predState =
        AbstractStates.extractStateByType(lastState, PredicateAbstractState.class);
    try {
      if (solver.isUnsat(predState.getAbstractionFormula().getBlockFormula().getFormula())) {
        pReached.remove(lastState);
        success = true;
      } else {
        success = false;
      }
        ImmutableList.Builder<AbstractState> builder = ImmutableList.builder();
        for (AbstractState s : pReached.getWaitlist()) {
          predState = AbstractStates.extractStateByType(s, PredicateAbstractState.class);

          if (unsatCache.get(predState)) {
            builder.add(s);
          }
        }
        for (AbstractState s : builder.build()) {
          pReached.removeOnlyFromWaitlist(s);
        }

    } catch (SolverException | ExecutionException e) {
      throw new CPATransferException("Solver failed during sat check", e);
    }

    return success;
  }

}
