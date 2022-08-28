package org.sosy_lab.cpachecker.cpa.cer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.CounterexampleInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionStore;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

@Options(prefix = "cpa.cer")
public class CERTransferRelation extends SingleEdgeTransferRelation {

    @Option(
        secure = true,
        name = "trackValuePrecision",
        description = "whether to contain precision information for the value analysis")
    private boolean tracksPrecision = true;

    @Option(
        secure = true,
        name = "trackFeasibility",
        description = "whether to track reachability for nodes in counterexamples")
    private boolean trackFeasibility = false;

    private final PrecisionStore precStore;
    private int cexInfosSize;

    private final TimerWrapper precisionTimer;
    private final TimerWrapper transferRelationTimer;

    public CERTransferRelation(
            Configuration pConfig,
            PrecisionStore pPrecStore,
            CERCPAStatistics statistics)
            throws InvalidConfigurationException {
        pConfig.inject(this);
        precStore = pPrecStore;

        if (precStore == null && tracksPrecision) {
            throw new InvalidConfigurationException(
                    "Error while creating transfer relation, precision store is null.");
        }

        precisionTimer = statistics.getPrecisionInfoTimer().getNewTimer();
        transferRelationTimer = statistics.getTransferRelationTimer().getNewTimer();

        cexInfosSize = 0;
        if (tracksPrecision) {
            ++cexInfosSize;
        }
        if (trackFeasibility) {
            ++cexInfosSize;
        }
    }

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
            AbstractState pState,
            Precision pPrecision,
            CFAEdge pCfaEdge)
            throws CPATransferException, InterruptedException {
        transferRelationTimer.start();
        CERState state = (CERState) pState;
        List<CounterexampleInformation> cexInfos = new ArrayList<>(cexInfosSize);

        // Precision
        if (tracksPrecision) {
            precisionTimer.start();
            PrecisionInformation newPrecisionInformation =
                    PrecisionInformation.merge(
                            PrecisionInformation.step(state.getPrecisionInfo(), pCfaEdge),
                            precStore.getPrecisionInfoForNode(pCfaEdge.getSuccessor()));
            cexInfos.add(newPrecisionInformation);
            precisionTimer.stop();
        }
        if (trackFeasibility) {
            // Does nothing at the moment
        }

        CERState result = new CERState(pCfaEdge.getSuccessor(), cexInfos);
        transferRelationTimer.stop();
        return Collections.singleton(result);
    }
}
