package org.sosy_lab.cpachecker.cpa.tube_cpa;



import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

class TubeCPAFactory extends AbstractCPAFactory {

    private CFA cfa;
    private final AnalysisDirection analysisDirection;




    public TubeCPAFactory(AnalysisDirection pAnalysisDirection) {
        analysisDirection = pAnalysisDirection;
    }

    @CanIgnoreReturnValue
    @Override
    public <T> org.sosy_lab.cpachecker.cpa.tube.TubeCPAFactory set(T pObject, Class<T> pClass) {
        if (CFA.class.isAssignableFrom(pClass)) {
            cfa = (CFA) pObject;
        } else {
            super.set(pObject, pClass);
        }
        return this;
    }

    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
        checkNotNull(cfa, "CFA instance needed to create LocationCPA");
        return TubeCPA.create();
    }
}
