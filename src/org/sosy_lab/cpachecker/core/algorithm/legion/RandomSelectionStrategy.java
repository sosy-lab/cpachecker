package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Selects a random state from the reached set which has a nondet-mark.
 */
public class RandomSelectionStrategy implements Selector {

    private LogManager logger;
    private Random random;

    public RandomSelectionStrategy(LogManager logger){
      this.logger = logger;

      this.random = new Random();
    }

    @Override
    public BooleanFormula select(ReachedSet reachedSet) {
        LinkedList<AbstractState> nonDetStates = getNondetStates(reachedSet);
        int rnd = random.nextInt(nonDetStates.size());
        AbstractState target = nonDetStates.get(rnd);
        logger.log(
                Level.INFO,
                "Target: ",
                AbstractStates.extractStateByType(target, LocationState.class));
        PredicateAbstractState ps =
                AbstractStates.extractStateByType(target, PredicateAbstractState.class);
        return ps.getPathFormula().getFormula();
    }

    /**
     * Find all nondet-marked states from the reachedSet
     */
    LinkedList<AbstractState> getNondetStates(ReachedSet reachedSet) {
        LinkedList<AbstractState> nonDetStates = new LinkedList<>();
        for (AbstractState state : reachedSet.asCollection()) {
            ValueAnalysisState vs =
                    AbstractStates.extractStateByType(state, ValueAnalysisState.class);
            if (vs.nonDeterministicMark) {
                logger.log(Level.FINE, "Nondet state", vs.getConstants().toString());
                nonDetStates.add(state);
            }
        }
        return nonDetStates;
    }

}
