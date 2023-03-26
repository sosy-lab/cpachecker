package org.sosy_lab.cpachecker.core.algorithm.microbenchmarking;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Tickers;
import org.sosy_lab.common.time.Tickers.TickerWithUnit;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

      logger.logf(Level.INFO, "Starting micro benchmarking");
      TickerWithUnit ticker = Tickers.getCurrentThreadCputime();

      Map<String, List<ConfigProgramExecutions>> benchmarkTimes = new HashMap<>();

      int[][] firstMatrix = generateRandomMatrix();
      int[][] secondMatrix = generateRandomMatrix();
      int m = firstMatrix.length;
      int n = secondMatrix[0].length;
      int[][] C = new int[m][n];

      List<BenchmarkExecutionRun> runTimes = new ArrayList<>();
      for (int exec = 0; exec < numExecutions; exec++) {

        long startTime = ticker.read();

        for (int i = 0; i < m; i++) {
          for (int j = 0; j < n; j++) {
              for (int k = 0; k < secondMatrix.length; k++) {
                  C[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
              }
          }
        }

        long endTime = ticker.read();
        long timeDiff = endTime - startTime;

        BenchmarkExecutionRun run = new BenchmarkExecutionRun();
        run.duration = timeDiff;
        runTimes.add(run);

      }

      ConfigProgramExecutions obj = new ConfigProgramExecutions();
      obj.configFileName = "";
      obj.programFileName = "matrix-multiplication";
      obj.runTimes = runTimes;
      double averageRunTime = calculateAverage(obj);
      obj.averageRunTime = averageRunTime;
      double variance = calculateVariance(obj, averageRunTime);
      obj.variance = variance;
      double standardDeviation = Math.sqrt(variance);
      obj.standardDeviation = standardDeviation;
      benchmarkTimes.put("matrix-multiplicatioN", List.of(obj));

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


    return child.run(pReachedSet);
  }

  private int[][] generateRandomMatrix() {
    int[][] matrix = new int[20][20];
    Random random = new Random();

    for (int i = 0; i < 20; i++) {
      for (int j = 0; j < 20; j++) {
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
}