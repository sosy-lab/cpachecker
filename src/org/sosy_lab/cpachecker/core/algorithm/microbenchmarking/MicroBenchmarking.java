package org.sosy_lab.cpachecker.core.algorithm.microbenchmarking;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Tickers;
import org.sosy_lab.common.time.Tickers.TickerWithUnit;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.MultiStatistics;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

@Options(prefix = "microBenchmark")
public class MicroBenchmarking implements Algorithm {

  private static class BenchmarkExecutionRun {
    private long startTime;
    private long endTime;
    private long duration;
  }

  private static class MicroBenchmarkingStatistics extends MultiStatistics {

    private long benchmarkExecutionDuration;
    private double averageBenchmarkDuration;

    private double allBenchmarksDurationDelta;

    protected MicroBenchmarkingStatistics(LogManager pLogger) {
      super(pLogger);
    }

    @Override
    public @Nullable String getName() {
      return "Micro Benchmarking";
    }
  }

  private final Algorithm child;
  private final MicroBenchmarkingStatistics stats;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;

  @Option(
      description = "Number of iterations for each algorithm/program combination. Defaults to 10.")
  private int numExecutions = 20;

  @Option(description = "List of algorithm config files to use for the benchmarking process.")
  private List<String> propertyFiles = List.of("micro-benchmarking-predicate.properties");

  @Option(description = "List of programs to run each benchmarking algorithm on.")
  private List<String> programFiles = List.of("loop_1.c");

  public MicroBenchmarking(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      Algorithm child)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    this.child = child;
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;
    this.specification = pSpecification;
    stats = new MicroBenchmarkingStatistics(pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    try {
      TickerWithUnit ticker = Tickers.getCurrentThreadCputime();
      long benchmarkStartTime = ticker.read();

      double benchmarkTimeSum = 0.0;
      double highestBenchmarkTime = 0.0;

      Map<String, List<Map<String, List<BenchmarkExecutionRun>>>> benchmarkTimes = new HashMap<>();

      Iterator<String> propertyFilesIterator = propertyFiles.iterator();
      Iterator<String> programFilesIterator = programFiles.iterator();

      while (propertyFilesIterator.hasNext()) {
        String singleConfigFileName = propertyFilesIterator.next();
        List<Map<String, List<BenchmarkExecutionRun>>> times = new ArrayList<>();
        while (programFilesIterator.hasNext()) {
          String singleProgramFileName = programFilesIterator.next();

          ConfigurationBuilder configurationBuilder = Configuration.builder();
          configurationBuilder.loadFromResource(getClass(), singleConfigFileName);
          Configuration configuration = configurationBuilder.build();
          CFACreator cfaCreator = new CFACreator(configuration, logger, shutdownNotifier);

          Path pathPredicate = getSourceFilePath(singleProgramFileName);
          CFA cfa = cfaCreator.parseFileAndCreateCFA(List.of(pathPredicate.toString()));

          ReachedSetFactory reachedSetFactory = new ReachedSetFactory(configuration, logger);
          CPABuilder cpaBuilder =
              new CPABuilder(configuration, logger, shutdownNotifier, reachedSetFactory);
          final ConfigurableProgramAnalysis cpa =
              cpaBuilder.buildCPAs(cfa, specification, AggregatedReachedSets.empty());

          CoreComponentsFactory factory =
              new CoreComponentsFactory(
                  configuration, logger, shutdownNotifier, AggregatedReachedSets.empty());
          Algorithm algorithm = factory.createAlgorithm(cpa, cfa, specification);
          ReachedSet reached =
              reachedSetFactory.createAndInitialize(
                  cpa, cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

          List<BenchmarkExecutionRun> list =
              runProgramAnalysis(ticker, algorithm, reached, reachedSetFactory, cpa, cfa);
          Map<String, List<BenchmarkExecutionRun>> map = new HashMap<>();
          map.put(singleProgramFileName, list);
          times.add(map);

          logger.logf(
              Level.INFO,
              "Running analysis %s on program %s",
              singleConfigFileName.substring(singleConfigFileName.lastIndexOf('/') + 1),
              singleProgramFileName.substring(singleProgramFileName.lastIndexOf('/') + 1));


        }
        benchmarkTimes.put(singleConfigFileName, times);
      }

      long benchmarkEndTime = ticker.read();

      stats.benchmarkExecutionDuration = benchmarkEndTime - benchmarkStartTime;
      stats.averageBenchmarkDuration =
          benchmarkTimeSum
              / (this.propertyFiles.size() * this.programFiles.size() * this.numExecutions);
      stats.allBenchmarksDurationDelta = highestBenchmarkTime - stats.averageBenchmarkDuration;

      logger.logf(
          Level.INFO,
          "Entire benchmark run took %s nanoseconds.",
          stats.benchmarkExecutionDuration);
      logger.logf(
          Level.INFO,
          "Benchmarks had an average runtime of %s nanoseconds.",
          stats.averageBenchmarkDuration);
      logger.logf(
          Level.INFO,
          "Average runtime delta is +- %s nanoseconds",
          stats.allBenchmarksDurationDelta);
    } catch (InvalidConfigurationException | IOException | ParserException e) {
      logger.logUserException(Level.SEVERE, e, "Failed to create benchmark algorithms!");
    }


    return child.run(pReachedSet);
  }

  private List<BenchmarkExecutionRun> runProgramAnalysis(
      TickerWithUnit ticker,
      Algorithm algorithm,
      ReachedSet reached,
      ReachedSetFactory reachedSetFactory,
      ConfigurableProgramAnalysis cpa,
      CFA cfa)
      throws InterruptedException, CPAException {

    List<BenchmarkExecutionRun> list = new ArrayList<>();
    for (int i = 0; i < numExecutions; i++) {

      long startTime = ticker.read();

      algorithm.run(reached);

      long endTime = ticker.read();
      long timeDiff = endTime - startTime;

      if (i < (numExecutions - 1)) {
        reached =
            reachedSetFactory.createAndInitialize(
                cpa, cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      }


      BenchmarkExecutionRun run = new BenchmarkExecutionRun();
      run.startTime = startTime;
      run.endTime = endTime;
      run.duration = timeDiff;
      list.add(run);
    }
    return list;
  }

  //  private Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> buildBenchmarkAlgorithm(
  //      String propertiesFile, String sourceFile)
  //      throws InvalidConfigurationException, IOException, ParserException, InterruptedException,
  //          CPAException {
  //    CFACreator cfaCreator = new CFACreator(configuration, logger, shutdownNotifier);
  //
  //    Path pathPredicate = getSourceFilePath(sourceFile);
  //    CFA cfa = cfaCreator.parseFileAndCreateCFA(List.of(pathPredicate.toString()));
  //
  //    ReachedSetFactory reachedSetFactory = new ReachedSetFactory(configuration, logger);
  //    CPABuilder cpaBuilder =
  //        new CPABuilder(configuration, logger, shutdownNotifier, reachedSetFactory);
  //    final ConfigurableProgramAnalysis cpa =
  //        cpaBuilder.buildCPAs(cfa, specification, AggregatedReachedSets.empty());
  //
  //    CoreComponentsFactory factory =
  //        new CoreComponentsFactory(
  //            configuration, logger, shutdownNotifier, AggregatedReachedSets.empty());
  //    Algorithm algorithm = factory.createAlgorithm(cpa, cfa, specification);
  //    ReachedSet reached =
  //        reachedSetFactory.createAndInitialize(
  //            cpa, cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
  //    return Triple.of(algorithm, cpa, reached);
  //  }

  private Path getSourceFilePath(String fileName) {

    //    URL url = Resources.getResource(getClass(), fileName);
    try {
      URL url = new URL(fileName);
      URI uri = url.toURI();
      return Path.of(uri);
    } catch (URISyntaxException | MalformedURLException e) {
      return Path.of(fileName);
    }
  }
}