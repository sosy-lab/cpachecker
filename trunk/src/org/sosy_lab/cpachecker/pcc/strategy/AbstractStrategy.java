// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.ValidationConfigurationConstructionFailed;
import org.sosy_lab.cpachecker.pcc.util.ProofStatesInfoCollector;
import org.sosy_lab.cpachecker.pcc.util.ValidationConfigurationBuilder;
import org.sosy_lab.cpachecker.util.Triple;

@Options(prefix = "pcc")
public abstract class AbstractStrategy implements PCCStrategy, StatisticsProvider {

  public static final String CONFIG_ZIPENTRY_NAME = "Config";
  public static final String PROOF_ZIPENTRY_NAME = "Proof";
  public static final String ADDITIONAL_PROOFINFO_ZIPENTRY_NAME = "Additional";

  private final Configuration config;
  protected LogManager logger;
  protected final PCStrategyStatistics stats;
  protected final ProofStatesInfoCollector proofInfo;
  private final Collection<Statistics> pccStats = new ArrayList<>();

  protected final Path proofFile;

  @Option(
      secure = true,
      name = "useCores",
      description = "number of cpus/cores which should be used in parallel for proof checking")
  @IntegerOption(min = 1)
  protected int numThreads = 1;

  @Option(
      secure = true,
      name = "storeConfig",
      description = "writes the validation configuration required for checking to proof")
  boolean storeConfig = false;

  protected AbstractStrategy(Configuration pConfig, LogManager pLogger, Path pProofFile)
      throws InvalidConfigurationException {
    pConfig.inject(this, AbstractStrategy.class);
    config = pConfig;
    numThreads = Math.max(1, numThreads);
    numThreads = Math.min(Runtime.getRuntime().availableProcessors(), numThreads);
    logger = pLogger;
    proofFile = pProofFile;
    proofInfo = new ProofStatesInfoCollector(pConfig);
    stats = new PCStrategyStatistics(proofFile);
    pccStats.add(stats);
  }

  @Override
  @SuppressFBWarnings(
      value = "OS_OPEN_STREAM",
      justification =
          "Do not close stream o because it wraps stream zos/fos which need to remain open and"
              + " would be closed if o.close() is called.")
  public void writeProof(UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa) {

    Path dir = proofFile.getParent();

    try {
      if (dir != null) {
        Files.createDirectories(dir);
      }

      try (final OutputStream fos = Files.newOutputStream(proofFile);
          final ZipOutputStream zos = new ZipOutputStream(fos)) {
        zos.setLevel(9);

        ZipEntry ze = new ZipEntry(PROOF_ZIPENTRY_NAME);
        zos.putNextEntry(ze);
        ObjectOutputStream o = new ObjectOutputStream(zos);
        // TODO might also want to write used configuration to the file so that proof checker does
        // not need to get it as an argument
        // write ARG
        writeProofToStream(o, pReached, pCpa);
        o.flush();
        zos.closeEntry();

        // write additional proof information
        int index = 0;
        boolean continueWriting;
        do {
          ze = new ZipEntry(ADDITIONAL_PROOFINFO_ZIPENTRY_NAME + index);
          zos.putNextEntry(ze);
          o = new ObjectOutputStream(zos);
          continueWriting = writeAdditionalProofStream(o);
          o.flush();
          zos.closeEntry();
          index++;
        } while (continueWriting);

        if (storeConfig) {
          ze = new ZipEntry(CONFIG_ZIPENTRY_NAME);
          zos.putNextEntry(ze);
          o = new ObjectOutputStream(zos);
          try {
            writeConfiguration(o);
          } catch (ValidationConfigurationConstructionFailed eIC) {
            logger.logUserException(
                Level.WARNING,
                eIC,
                "Construction of validation configuration failed. Validation configuration is"
                    + " empty.");
          }

          o.flush();
          zos.closeEntry();
        }
      } catch (NotSerializableException eS) {
        logger.logUserException(
            Level.SEVERE,
            eS,
            "Proof cannot be written. Class does not implement Serializable interface");
      } catch (InvalidConfigurationException e) {
        logger.logUserException(
            Level.SEVERE, e, "Proof cannot be constructed due to conflicting configuration.");
      } catch (InterruptedException e) {
        logger.logUserException(
            Level.SEVERE, e, "Proof cannot be written due to time out during proof construction");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    logger.log(Level.INFO, proofInfo.getInfoAsString());
  }

  protected abstract void writeProofToStream(
      ObjectOutputStream out, UnmodifiableReachedSet reached, ConfigurableProgramAnalysis pCpa)
      throws IOException, InvalidConfigurationException, InterruptedException;

  @Override
  public void readProof()
      throws IOException, ClassNotFoundException, InvalidConfigurationException {
    Triple<InputStream, ZipInputStream, ObjectInputStream> proofStream = openProofStream();
    readProofFromStream(proofStream.getThird());
    proofStream.getThird().close();
    proofStream.getSecond().close();
    proofStream.getFirst().close();
  }

  /**
   * Hook for adding additional output in subclasses.
   *
   * @param pOut the outputstream to which should be written
   * @throws IOException may be thrown in subclasses
   */
  protected boolean writeAdditionalProofStream(final ObjectOutputStream pOut) throws IOException {
    return false;
  }

  protected void writeConfiguration(ObjectOutputStream pO)
      throws ValidationConfigurationConstructionFailed, IOException {
    pO.writeObject(
        new ValidationConfigurationBuilder(config)
            .getValidationConfiguration()
            .asPropertiesString());
  }

  protected Triple<InputStream, ZipInputStream, ObjectInputStream> openProofStream()
      throws IOException {
    InputStream fis = Files.newInputStream(proofFile);
    ZipInputStream zis = new ZipInputStream(fis);
    ZipEntry entry = zis.getNextEntry();
    assert entry.getName().equals(PROOF_ZIPENTRY_NAME);
    return Triple.of(fis, zis, new ObjectInputStream(zis));
  }

  public Triple<InputStream, ZipInputStream, ObjectInputStream> openAdditionalProofStream(
      final int index) throws IOException {
    checkArgument(index >= 0, "Not a valid index. Indices must be at least zero.");
    InputStream fis = Files.newInputStream(proofFile);
    ZipInputStream zis = new ZipInputStream(fis);
    for (int i = 0; i <= index; i++) { // skip index+1 entries
      zis.getNextEntry();
    }
    ZipEntry entry = zis.getNextEntry();

    assert entry.getName().equals("ADDITIONAL_PROOFINFO_ZIPENTRY_NAME " + index);
    return Triple.of(fis, zis, new ObjectInputStream(zis));
  }

  protected abstract void readProofFromStream(ObjectInputStream in)
      throws ClassNotFoundException, InvalidConfigurationException, IOException;

  protected void addPCCStatistic(final Statistics pPCCStatistic) {
    pccStats.add(pPCCStatistic);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.addAll(pccStats);
  }

  @Override
  public Collection<Statistics> getAdditionalProofGenerationStatistics() {
    if (proofInfo != null) {
      return Collections.singleton(proofInfo);
    }
    return ImmutableSet.of();
  }

  public static class PCStrategyStatistics implements Statistics {

    protected Timer transferTimer = new Timer();
    protected Timer stopTimer = new Timer();
    protected Timer preparationTimer = new Timer();
    protected Timer propertyCheckingTimer = new Timer();

    protected int countIterations = 0;
    protected int proofSize = 0;
    protected final long fileProofSize;

    public PCStrategyStatistics(final Path pFile) {
      if (pFile != null) {
        fileProofSize = pFile.toFile().length();
      } else {
        fileProofSize = -1;
      }
    }

    @Override
    public String getName() {
      return "Proof Checking Strategy Statistics";
    }

    public Timer getPreparationTimer() {
      return preparationTimer;
    }

    public Timer getStopTimer() {
      return stopTimer;
    }

    public Timer getTransferTimer() {
      return transferTimer;
    }

    public Timer getPropertyCheckingTimer() {
      return propertyCheckingTimer;
    }

    public void increaseIteration() {
      countIterations++;
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println("Number of iterations:                     " + countIterations);
      out.println();
      out.println("Number of proof elements:                     " + proofSize);
      out.println();
      out.println("  Time for preparing proof for checking:          " + preparationTimer);
      out.println(
          "  Time for abstract successor checks:     "
              + transferTimer
              + " (Calls: "
              + transferTimer.getNumberOfIntervals()
              + ")");
      out.println(
          "  Time for covering checks:               "
              + stopTimer
              + " (Calls: "
              + stopTimer.getNumberOfIntervals()
              + ")");
      out.println(" Time for checking property:          " + propertyCheckingTimer);
      out.println("Proof file size (bytes):                      " + fileProofSize);
    }

    public void increaseProofSize(int pIncrement) {
      proofSize += pIncrement;
    }
  }
}
