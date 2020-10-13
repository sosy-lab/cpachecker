package org.sosy_lab.cpachecker.cpa.legion;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class StatisticsState implements AbstractState {
    private int win, sel, sim;

    int uct() {
        return 0;
    }

}
