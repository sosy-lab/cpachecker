package org.sosy_lab.cpachecker.cpa.hybrid.test;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.hybrid.ValueAnalysisHybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.hybrid.util.StrengthenOperatorFactory;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public class OperatorFactoryTest
{
    @Test
    public void checkFactoryOnStates() {
        ValueAnalysisState state = new ValueAnalysisState(MachineModel.LINUX64);
        HybridStrengthenOperator op = StrengthenOperatorFactory.ProvideStrenghtenOperator(state);
        Assert.assertEquals(
            ValueAnalysisHybridStrengthenOperator.class.getName(), 
            op.getClass().getName());
    }
}