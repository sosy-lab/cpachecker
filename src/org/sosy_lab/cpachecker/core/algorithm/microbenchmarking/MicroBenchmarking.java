package org.sosy_lab.cpachecker.core.algorithm.microbenchmarking;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
  private final ShutdownNotifier pShutdownNotifier;
  private final Specification pSpecification;

  @Option(
    secure = true,
    description = "Number of iterations for each algorithm/program combination. Defaults to 10.")
  private int numExecutions = 22;

  @Option(
    secure = true,
    description = "Number of iterations where result is discarded for warming up the JVM.")
  private int numWarmupExecutions = 20;

  @Option(secure = true, description = "Size of rows and columns for micro benchmarking matrix")
  private int sizeMatrixRowCol = 50;

  @Option(secure = true, description = "Amount of time microbenchmark run for on base line system.")
  private long baseLineRunTime = -1;

  @Option(
    secure = true,
    description = "Percentage which represents the threshold, compared to the base line run time, where the user gets notified..")
  private double warningThreshold = 10;

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
    this.pShutdownNotifier = pShutdownNotifier;
    this.pSpecification = pSpecification;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    try {
      runMicroBenchmark();
    } catch (IOException | InvalidConfigurationException | ParserException | InterruptedException
        | CPAException e) {
      logger.logfException(Level.SEVERE, e, "Failed to run micro benchmark!");
    }

    return child.run(pReachedSet);
  }

  private void runMicroBenchmark()
      throws IOException, InvalidConfigurationException, ParserException, InterruptedException,
      CPAException {
    logger.logf(Level.INFO, "Starting micro benchmarking");
    TickerWithUnit ticker = Tickers.getCurrentThreadCputime();
    long startTime = ticker.read();

    for (int i = 0; i < propertyFiles.size(); i++) { // Iterate over all user-defined analysis'
      for (int z = 0; z < programFiles.size(); z++) {

        logger.log(
            Level.INFO,
            String.format(
                "Running analysis defined in '%s' on program '%s'",
                propertyFiles.get(i),
                programFiles.get(i)));

        ConfigurationBuilder configurationBuilder = Configuration.builder();
        configurationBuilder.loadFromFile(propertyFiles.get(i));
        Configuration configuration = configurationBuilder.build();
        CFACreator cfaCreator = new CFACreator(configuration, logger, pShutdownNotifier);

        CFA cfa =
            cfaCreator.parseFileAndCreateCFA(ImmutableList.of(programFiles.get(z).toString()));

        ReachedSetFactory reachedSetFactory = new ReachedSetFactory(configuration, logger);
        CPABuilder cpaBuilder =
            new CPABuilder(configuration, logger, pShutdownNotifier, reachedSetFactory);
        final ConfigurableProgramAnalysis cpa =
            cpaBuilder.buildCPAs(cfa, pSpecification, AggregatedReachedSets.empty());

        CoreComponentsFactory factory =
            new CoreComponentsFactory(
                configuration,
                logger,
                pShutdownNotifier,
                AggregatedReachedSets.empty());
        Algorithm algorithm = factory.createAlgorithm(cpa, cfa, pSpecification);
        ReachedSet reached =
            reachedSetFactory.createAndInitialize(
                cpa,
                cfa.getMainFunction(),
                StateSpacePartition.getDefaultPartition());

        List<BenchmarkExecutionRun> runTimes =
            runProgramAnalysis(algorithm, reached, reachedSetFactory, cpa, cfa);
        if (this.outputFile != null) {
          try (Writer writer = IO.openOutputFile(this.outputFile, Charset.defaultCharset())) {
            try {
              writer.append("Config file: ");
              writer.append(propertyFiles.get(i).getFileName().toString() + "\n");
              writer.append("Program file: ");
              writer.append(programFiles.get(z).getFileName().toString() + "\n");

              for (BenchmarkExecutionRun run : runTimes) {
                writer.append(String.valueOf(run.duration));
                writer.append(';');
              }
              writer.append("\n");

              writer.append("Runtime in ms: ").append("\n");
              for (BenchmarkExecutionRun run : runTimes) {
                writer.append(String.valueOf(run.duration / 1000000));
                writer.append(';');
              }
              writer.append("\n");


              double overallRunTime =
                  runTimes.stream().map(r -> r.duration / 1000000.0).reduce(0.0, (a, b) -> a + b);
              writer.append("Overall runtime in ms: ").append(String.valueOf(overallRunTime));

              writer.append("\n\n");
              long endTime = ticker.read();
              long time = endTime - startTime;
              writer.append("Microbenchmark took: ").append(String.valueOf(time)).append(" ns");
            } catch (IOException e) {
              logger.logfUserException(Level.WARNING, e, "Failed to write run time data to file.");
            }
          } catch (IOException ex) {
            logger.logUserException(Level.WARNING, ex, "Could not write CFA to dot file");
          }
        }

      }
    }

  }

  private List<BenchmarkExecutionRun> runProgramAnalysis(
      Algorithm algorithm,
      ReachedSet reached,
      ReachedSetFactory reachedSetFactory,
      ConfigurableProgramAnalysis cpa,
      CFA cfa)
      throws InterruptedException {

    Ticker ticker = Ticker.systemTicker();

    List<BenchmarkExecutionRun> list = new ArrayList<>();
    for (int i = 0; i < numWarmupExecutions + numExecutions; i++) {

      if (i < numWarmupExecutions) {
        logger
            .log(Level.INFO, "Running microbenchmarking analysis as warmup iteration #" + (i + 1));
      } else {
        logger.log(Level.INFO, "Running microbenchmarking analysis - iteration #" + (i + 1));
      }

      long startTime = ticker.read();

      try {
        algorithm.run(reached);
      } catch (CPAException | InterruptedException e) {
        logger.log(
            Level.FINE,
            "Error during microbenchmarking run. Ignoring result and continuing...");
        continue;
      }

      long endTime = ticker.read();
      long timeDiff = endTime - startTime;

      if (i < ((numExecutions + numWarmupExecutions) - 1)) { // Create new empty reached set for
                                                             // every iteration
        reached =
            reachedSetFactory.createAndInitialize(
                cpa,
                cfa.getMainFunction(),
                StateSpacePartition.getDefaultPartition());
      }

      if (i >= numWarmupExecutions) {
        BenchmarkExecutionRun run = new BenchmarkExecutionRun();
        run.duration = timeDiff;
        list.add(run);
      }
      logger.logf(Level.INFO, "Finished run #%s in %s ms", (i + 1), timeDiff);

    }
    return list;
  }

  private int[][] generateRandomMatrix() {
    int[][] matrix = new int[sizeMatrixRowCol][sizeMatrixRowCol];
    Random random = new Random();

    for (int i = 0; i < sizeMatrixRowCol; i++) {
      for (int j = 0; j < sizeMatrixRowCol; j++) {
        matrix[i][j] = random.nextInt();
      }
    }

    return matrix;
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

  private int sumFirstRow(int[][] matrix) {
    if (matrix.length <= 0) {
      return 0;
    }

    return Arrays.stream(matrix[0]).reduce(0, (a, b) -> a + b);
  }
}
