package org.sosy_lab.cpachecker.core.algorithm.microbenchmarking;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Tickers;
import org.sosy_lab.common.time.Tickers.TickerWithUnit;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
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
    long duration;
  }

  private static class ConfigProgramExecutions {
    private String configFileName;
    private String programFileName;

    private List<BenchmarkExecutionRun> runTimes;

    private double averageRunTime;
    private double variance;
    private double standardDeviation;
  }

  private final Algorithm child;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;

  @Option(
    secure = true,
      description = "Number of iterations for each algorithm/program combination. Defaults to 10.")
  private int numExecutions = 22;

  @Option(
    secure = true,
    required = true,
    description = "List of algorithm config files to use for the benchmarking process.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private List<Path> propertyFiles;

  @Option(
    secure = true,
    required = true,
    description = "List of programs to run each benchmarking algorithm on.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private List<Path> programFiles;

  @Option(
    secure = true,
    description = "Defines the file where the results of the micro benchmark are stored.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFile;

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
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    try {
      logger.logf(Level.INFO, "Starting micro benchmarking");
      TickerWithUnit ticker = Tickers.getCurrentThreadCputime();

      Map<String, List<ConfigProgramExecutions>> benchmarkTimes = new HashMap<>();

      Iterator<Path> propertyFilesIterator = propertyFiles.iterator();
      int index = 0;
      while (propertyFilesIterator.hasNext()) { // Allow for running benchmark applications on
                                                // multiple algorithms
        Path singleConfigFilePath = propertyFilesIterator.next();
        List<ConfigProgramExecutions> runTimes = new ArrayList<>();
        for (Path singleProgramFilePath : programFiles) { // Run a single algorithm against one or
                                                          // more program files

          ConfigurationBuilder configurationBuilder = Configuration.builder();
          configurationBuilder.loadFromFile(singleConfigFilePath);
          Configuration configuration = configurationBuilder.build();
          CFACreator cfaCreator = new CFACreator(configuration, logger, shutdownNotifier);

          CFA cfa =
              cfaCreator.parseFileAndCreateCFA(ImmutableList.of(singleProgramFilePath.toString()));

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
          // This list contains the time each run took to complete

          ConfigProgramExecutions obj = new ConfigProgramExecutions();
          obj.configFileName = singleConfigFilePath.toString();
          obj.programFileName = singleProgramFilePath.toString();
          obj.runTimes = list;
          double averageRunTime = calculateAverage(obj);
          obj.averageRunTime = averageRunTime;
          double variance = calculateVariance(obj, averageRunTime);
          obj.variance = variance;
          double standardDeviation = Math.sqrt(variance);
          obj.standardDeviation = standardDeviation;
          runTimes.add(obj);

          String programFileName =
              singleProgramFilePath.toString();
          logger.logf(
              Level.FINE,
              "Finished running micro benchmark analysis for program file %s",
              programFileName);

        }

        String configFileName =
            singleConfigFilePath.toString();
        logger.logf(
            Level.FINE,
            "Finished running micro benchmark analysis for config file %s",
            configFileName);

        benchmarkTimes.put(singleConfigFilePath.toString() + "-" + index, runTimes);
        index++;
      }

      if (this.outputFile != null) {
        try (Writer writer = IO.openOutputFile(this.outputFile, Charset.defaultCharset())) {
          benchmarkTimes.entrySet()
              .forEach(entry -> entry.getValue().forEach(l -> {
                try {
                  writer.append("Config file: ");
                  writer.append(l.configFileName + "\n");
                  writer.append("Program file: ");
                  writer.append(l.programFileName + "\n");

                  for (BenchmarkExecutionRun run : l.runTimes) {
                    writer.append(String.valueOf(run.duration));
                    writer.append(';');
                  }
                  writer.append("\n");

                  writer.append("Average run time: ");
                  writer.append(String.valueOf(l.averageRunTime) + "\n");

                  writer.append("Variance: ");
                  writer.append(String.valueOf(l.variance) + "\n");

                  writer.append("Standard deviation: ");
                  writer.append(String.valueOf(l.standardDeviation) + "\n");

                  writer.append("\n\n");
                } catch (IOException e) {
                  logger.logfUserException(
                      Level.WARNING,
                      e,
                      "Failed to write run time data to file.");
                }
              }));
        } catch (IOException ex) {
          logger.logUserException(Level.WARNING, ex, "Could not write CFA to dot file");
        }
      }
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


      // Ignore the first two runs because of initialisation outliers
      if (i >= 2) {
        BenchmarkExecutionRun run = new BenchmarkExecutionRun();
        run.duration = timeDiff;
        list.add(run);
      }
      logger.logf(Level.FINEST, "Finished run #%s", i);
    }
    return list;
  }

  private double calculateAverage(ConfigProgramExecutions cpe) {

    long sum = 0;
    for (BenchmarkExecutionRun run : cpe.runTimes) {
      sum += (run.duration / 1000);
    }

    return sum / (double) cpe.runTimes.size();
  }

  private double calculateVariance(ConfigProgramExecutions cpe, double average) {

    double sum = 0.0;
    for (BenchmarkExecutionRun run : cpe.runTimes) {
      sum += Math.pow((run.duration / 1000) - average, 2);
    }

    return sum / (cpe.runTimes.size());
  }
}