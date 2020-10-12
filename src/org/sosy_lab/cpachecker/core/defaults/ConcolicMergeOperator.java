package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ConcolicMergeOperator implements MergeOperator {

    private static final MergeOperator instance = new ConcolicMergeOperator();

    public static MergeOperator getInstance() {
        return instance;
    }

    @Override
    public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
            throws CPAException, InterruptedException {

        return el1;
    }
}
