package org.sosy_lab.cpachecker.core.algorithm.legion.selection;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

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
    public PathFormula select(ReachedSet reachedSet) {
        ArrayList<AbstractState> nonDetStates = getNondetStates(reachedSet);
        int rnd = random.nextInt(nonDetStates.size());
        AbstractState target = nonDetStates.get(rnd);
        logger.log(
                Level.INFO,
                "Target: ",
                AbstractStates.extractStateByType(target, LocationState.class));
        PredicateAbstractState ps =
                AbstractStates.extractStateByType(target, PredicateAbstractState.class);
        return ps.getPathFormula();
    }

    /**
     * Find all nondet-marked states from the reachedSet
     */
    ArrayList<AbstractState> getNondetStates(ReachedSet reachedSet) {
        ArrayList<AbstractState> nonDetStates = new ArrayList<>();
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
