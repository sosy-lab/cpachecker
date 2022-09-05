package org.sosy_lab.cpachecker.cpa.cer;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopAlwaysOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionStore;
import org.sosy_lab.cpachecker.cpa.cer.io.CexMapper;
import org.sosy_lab.cpachecker.cpa.cer.io.CexMapperReport;
import org.sosy_lab.cpachecker.cpa.cer.reducer.CexReducer;
import org.sosy_lab.cpachecker.cpa.cer.io.CexExporterXML;
import org.sosy_lab.cpachecker.cpa.cer.io.CexImporterXML;
import org.sosy_lab.cpachecker.cpa.cer.refiner.CERRefinerReport;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@Options(prefix = "cpa.cer")
public class CERCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

    @Option(
        secure = true,
        name = "file.importFile",
        description = "The import path of the cer reuse file.")
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    private Path importFilePath;

    @Option(
        secure = true,
        name = "failIfImportFileIsMissing",
        description = "whether the analysis should stop if the defined import file is missing or an empty store should be used instead.")
    private boolean failIfImportFileIsMissing = true;

    @Option(
        secure = true,
        name = "file.exportFile",
        description = "The export path for the cer reuse file.")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path exportFilePath;

    @Option(
        secure = true,
        name = "mapCexAtStart",
        description = "whether to match counterexamples during analysis or at start (currently the only mapping option, keep this true)")
    private boolean mapCexAtStart = true;

    @Option(
        secure = true,
        name = "tracksPrecision",
        description = "whether to contain precision information for the value analysis")
    private boolean tracksPrecision = true;

    private final Collection<Cex> cexs;
    private boolean cexsChanged;
    private final CERCPAStatistics statistics;
    private final PrecisionStore precStore;
    private final Configuration config;
    private final CFA cfa;
    private final LogManager logger;
    private final CERTransferRelation transfer;
    private final TimerWrapper cexCreationTimer;

    public CERCPA(Configuration pConfig, LogManager pLogger, CFA pCfa)
            throws InvalidConfigurationException, CPAException {

        pConfig.inject(this, CERCPA.class);
        config = pConfig;
        statistics = new CERCPAStatistics();
        cexCreationTimer = statistics.getCexCreationTimer().getNewTimer();
        cfa = pCfa;
        logger = pLogger;
        cexs = new ArrayList<>();

        if (tracksPrecision) {
            precStore = new PrecisionStore();
        } else {
            precStore = null;
        }
        transfer = new CERTransferRelation(config, precStore, statistics);

        Collection<Cex> importedCex = importCex();
        if (mapCexAtStart) {
            if (importedCex.size() > 0) {
                TimerWrapper mappingTimer = statistics.getMappingTimer().getNewTimer();
                mappingTimer.start();
                mapCexAndAddToStore(importedCex);
                mappingTimer.stop();
            }
        }
        cexsChanged = false;
    }

    public static CPAFactory factory() {
        return AutomaticCPAFactory.forType(CERCPA.class);
    }

    @Override
    public AbstractDomain getAbstractDomain() {
        return new FlatLatticeDomain();
    }

    @Override
    public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
            throws InterruptedException {
        return new CERState(pNode, ImmutableSet.of());
    }

    @Override
    public TransferRelation getTransferRelation() {
        return transfer;
    }

    @Override
    public MergeOperator getMergeOperator() {
        return new MergeSepOperator();
    }

    @Override
    public StopOperator getStopOperator() {
        return new StopAlwaysOperator();
    }

    @SuppressWarnings(value = "unchecked") // For the precision cast to Set<MemoryLocation>.
    public void update(CERRefinerReport pReport) {
        cexCreationTimer.start();
        CexReducer reducer = new CexReducer();
        Deque<CFAEdgeWithAdditionalInfo> pathWithInfos =
                CexReducer.getPathWithPrecisionInfos(
                        pReport.getErrorPath(),
                        pReport.getCutOffRoots(),
                        pReport.getInterpolationTree());

        List<CFAEdgeWithAdditionalInfo> reducedPath = reducer.reduce(pathWithInfos);

        Cex resultCex = new Cex(reducedPath);
        precStore.updateWithCexs(ImmutableSet.of(resultCex));
        cexs.add(resultCex);
        cexsChanged = true;
        cexCreationTimer.stop();
    }

    public void exportCexs() {
        // TODO make gzip an input option
        if (exportFilePath == null) {
            logger.log(
                    Level.INFO,
                    "No CER export file defined. "
                            + "The CER export file can be defined with the parameter '-setprop cpa.cer.file.exportFile=<FILE_PATH>'");
            return;
        }

        if (!cexsChanged
                && importFilePath != null
                && exportFilePath != null
                && importFilePath.equals(exportFilePath)) {
            return;
        }

        try {
            // CexExporterJSON.exportStore(cexs, exportFilePath, false, statistics);
            CexExporterXML exporter = new CexExporterXML(statistics);
            exporter.exportCexs(cexs, exportFilePath);
            logger.log(Level.FINE, "CER data exported to ", exportFilePath);
            return;
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    "Error while exporting cex automaton store. No data were exported.",
                    e);
            return;
        }
    }

    public Collection<Cex> importCex() throws CPAException {
        if (importFilePath == null) {
            logger.log(
                    Level.INFO,
                    "No CER input file defined. "
                            + "The CER input file can be defined with the parameter '-setprop cpa.cer.file.importFile=<FILE_PATH>'");
            return ImmutableList.of();
        }

        try {
            // Collection<Cex> importedCex =
            // CexImporterJSON.importCexs(importFilePath, false, statistics);
            CexImporterXML importer = new CexImporterXML(statistics);
            Collection<Cex> importedCex = importer.importCexs(importFilePath);
            logger.log(Level.FINE, "CER data imported from ", importFilePath);
            return importedCex;
        } catch (NoSuchFileException e) {
            if (failIfImportFileIsMissing) {
                throw new CPAException("The defined CER input file does not exist. ");
            }
            logger.log(
                    Level.WARNING,
                    "The defined CER input file does not exist. An empty store will be used for verification.");
            return ImmutableList.of();
        } catch (Exception e) {
            throw new CPAException(e.getLocalizedMessage());
        }
    }

    public void mapCexAndAddToStore(Collection<Cex> pCexs) {
        CexMapper mapper = new CexMapper(cfa, statistics);
        CexMapperReport mapperResult = mapper.mapCexs(pCexs);
        precStore.updateWithCexs(mapperResult.getMappedCex());
        cexs.addAll(mapperResult.getMappedCex());
    }

    public Optional<PrecisionStore> getPrecisionStore() {
        return Optional.ofNullable(precStore);
    }

    @Override
    public void collectStatistics(Collection<Statistics> pStatsCollection) {
        pStatsCollection.add(statistics);
    }

    public CERCPAStatistics getStatistics() {
        return statistics;
    }
}
