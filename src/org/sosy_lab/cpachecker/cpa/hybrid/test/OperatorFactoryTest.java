package org.sosy_lab.cpachecker.cpa.hybrid.test;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.hybrid.ValueAnalysisHybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.hybrid.exception.UnsupportedStateException;
import org.sosy_lab.cpachecker.cpa.hybrid.util.StrengthenOperatorFactory;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

import org.junit.Assert;

public class OperatorFactoryTest
{
    @Test
    public void checkFactoryOnStates() throws UnsupportedStateException
    {
        ValueAnalysisState state = new ValueAnalysisState(MachineModel.LINUX64);
        HybridStrengthenOperator<? extends AbstractState> op 
            = StrengthenOperatorFactory.ProvideStrenghtenOperator(state);
        Assert.assertEquals(
            ValueAnalysisHybridStrengthenOperator.class.getName(), 
            op.getClass().getName());
        HybridStrengthenOperator<? extends AbstractState> ope 
            = StrengthenOperatorFactory.ProvideStrenghtenOperator(state);
    }
}