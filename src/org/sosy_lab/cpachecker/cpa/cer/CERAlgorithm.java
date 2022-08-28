package org.sosy_lab.cpachecker.cpa.cer;

import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

public class CERAlgorithm implements Algorithm, StatisticsProvider {

    private final Algorithm wrappedAlgorithm;
    private final CERCPA cerCpa;
    private final LogManager logger;

    public CERAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, LogManager pLogger)
            throws InvalidConfigurationException {

        cerCpa = CPAs.retrieveCPAOrFail(pCpa, CERCPA.class, CERAlgorithm.class);
        wrappedAlgorithm = pAlgorithm;
        logger = pLogger;
    }

    @Override
    public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
        logger.log(Level.FINE, "CERAlgorithm started.");
        AlgorithmStatus status = wrappedAlgorithm.run(pReached);
        cerCpa.exportCexs();
        logger.log(Level.FINE, "CERAlgorithm stopped.");
        return status;
    }

    @Override
    public void collectStatistics(Collection<Statistics> pStatsCollection) {
        if (wrappedAlgorithm instanceof StatisticsProvider) {
            ((StatisticsProvider) wrappedAlgorithm).collectStatistics(pStatsCollection);
        }
    }
}
