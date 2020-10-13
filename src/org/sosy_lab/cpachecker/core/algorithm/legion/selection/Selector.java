package org.sosy_lab.cpachecker.core.algorithm.legion.selection;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public interface Selector {
    /**
     * Select a target state and return the path formula that leads to it.
     */
    PathFormula select(ReachedSet pReachedSet);
}
