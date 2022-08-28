package org.sosy_lab.cpachecker.cpa.cer;

import java.io.PrintStream;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;

@SuppressWarnings("deprecation") // replace ThreadSafeTimerContainer
public class CERCPAStatistics implements Statistics {

    private final StatCounter importedCexCounter;
    private final StatCounter exportedCexCounter;
    private final StatCounter mappedCexCounter;
    private final StatCounter unmappedCexCounter;
    private final ThreadSafeTimerContainer transferRelationTimer;
    private final ThreadSafeTimerContainer fileImportTimer;
    private final ThreadSafeTimerContainer fileExportTimer;
    private final ThreadSafeTimerContainer mappingTimer;
    private final ThreadSafeTimerContainer precisionInfoTimer;
    private final ThreadSafeTimerContainer cexCreationTimer;

    private final ThreadSafeTimerContainer feasibilityCheckerTimer;
    private final ThreadSafeTimerContainer refinementTimer;

    public CERCPAStatistics() {
        importedCexCounter = new StatCounter("Number of imported counterexamples");
        exportedCexCounter = new StatCounter("Number of exported counterexamples");
        mappedCexCounter = new StatCounter("Number of successfully mapped counterexamples");
        unmappedCexCounter = new StatCounter("Number of counterexamples that could not be mapped");
        // refinementsCounter = new StatCounter("Number of refined precisions");
        transferRelationTimer =
                new ThreadSafeTimerContainer("Total time for cer transfer relation");
        fileImportTimer = new ThreadSafeTimerContainer("Total time for importing cex from file");
        fileExportTimer = new ThreadSafeTimerContainer("Total time for exporting cex to file");
        cexCreationTimer = new ThreadSafeTimerContainer("Total time for creation of new cex");
        mappingTimer =
                new ThreadSafeTimerContainer(
                        "Total time spend for mapping of imported counterexamples");
        precisionInfoTimer =
                new ThreadSafeTimerContainer(
                        "Total time spend for updating CerState precision information");
        feasibilityCheckerTimer =
                new ThreadSafeTimerContainer("cpaalg - Total time spend for feasibility checks.");
        refinementTimer = new ThreadSafeTimerContainer("cpaalg - Total time spend for refinement.");
    }

    @Override
    public String getName() {
        return "CERCPA";
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
        StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
        writer.put(importedCexCounter);
        writer.put(exportedCexCounter);
        writer.put(fileImportTimer);
        writer.put(fileExportTimer);
        writer.put(mappingTimer);
        writer.put(mappedCexCounter);
        writer.put(unmappedCexCounter);
        writer.put(precisionInfoTimer);
        writer.put(transferRelationTimer);
        writer.put(feasibilityCheckerTimer);
        writer.put(refinementTimer);
        writer.put(cexCreationTimer);
    }

    public StatCounter getImportedCexCounter() {
        return importedCexCounter;
    }

    public ThreadSafeTimerContainer getFileExportTimer() {
        return fileExportTimer;
    }

    public ThreadSafeTimerContainer getFileImportTimer() {
        return fileImportTimer;
    }

    public ThreadSafeTimerContainer getMappingTimer() {
        return mappingTimer;
    }

    public ThreadSafeTimerContainer getPrecisionInfoTimer() {
        return precisionInfoTimer;
    }

    public StatCounter getExportedCexCounter() {
        return exportedCexCounter;
    }

    public StatCounter getMappedCexCounter() {
        return mappedCexCounter;
    }

    public StatCounter getUnmappedCexCounter() {
        return unmappedCexCounter;
    }

    public ThreadSafeTimerContainer getTransferRelationTimer() {
        return transferRelationTimer;
    }

    public ThreadSafeTimerContainer getFeasibilityCheckerTimer() {
        return feasibilityCheckerTimer;
    }

    public ThreadSafeTimerContainer getRefinementTimer() {
        return refinementTimer;
    }

    public ThreadSafeTimerContainer getCexCreationTimer() {
        return cexCreationTimer;
    }
}
