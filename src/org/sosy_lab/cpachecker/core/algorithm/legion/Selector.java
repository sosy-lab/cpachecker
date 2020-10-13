package org.sosy_lab.cpachecker.core.algorithm.legion;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.java_smt.api.BooleanFormula;

interface Selector {
    /**
     * Select a target state and return the path formula that leads to it.
     */
    BooleanFormula select(ReachedSet pReachedSet);
}
