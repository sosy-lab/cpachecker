/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.pcc.strategy;

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
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.pcc.util.ProofStatesInfoCollector;
import org.sosy_lab.cpachecker.util.Triple;

@Options(prefix="pcc")
public abstract class AbstractStrategy implements PCCStrategy, StatisticsProvider {

  protected LogManager logger;
  protected PCStrategyStatistics stats;
  protected ProofStatesInfoCollector proofInfo;
  private Collection<Statistics> pccStats = new ArrayList<>();

  protected final Path proofFile;

  @Option(secure=true,
      name = "useCores",
      description = "number of cpus/cores which should be used in parallel for proof checking")
  @IntegerOption(min=1)
  protected int numThreads = 1;

  public AbstractStrategy(Configuration pConfig, LogManager pLogger, Path pProofFile) throws InvalidConfigurationException {
    pConfig.inject(this, AbstractStrategy.class);
    numThreads = Math.max(1, numThreads);
    numThreads = Math.min(Runtime.getRuntime().availableProcessors(), numThreads);
    logger = pLogger;
    proofFile = pProofFile;
    proofInfo = new ProofStatesInfoCollector(pConfig);
    stats = new PCStrategyStatistics(proofFile);
    pccStats.add(stats);
  }

  @Override
  @SuppressFBWarnings(value="OS_OPEN_STREAM", justification="Do not close stream o because it wraps stream zos/fos which need to remain open and would be closed if o.close() is called.")
  public void writeProof(UnmodifiableReachedSet pReached) {

    Path dir = proofFile.getParent();

    try {
      Files.createDirectories(dir);

      try (final OutputStream fos = Files.newOutputStream(proofFile);
          final ZipOutputStream zos = new ZipOutputStream(fos)) {
        zos.setLevel(9);

        ZipEntry ze = new ZipEntry("Proof");
        zos.putNextEntry(ze);
        ObjectOutputStream o = new ObjectOutputStream(zos);
        //TODO might also want to write used configuration to the file so that proof checker does not need to get it as an argument
        //write ARG
        writeProofToStream(o, pReached);
        o.flush();
        zos.closeEntry();

        // write additional proof information
        int index = 0;
        boolean continueWriting;
        do {
          ze = new ZipEntry("Additional " + index);
          zos.putNextEntry(ze);
          o = new ObjectOutputStream(zos);
          continueWriting = writeAdditionalProofStream(o);
          o.flush();
          zos.closeEntry();
          index++;
        } while (continueWriting);

      } catch (NotSerializableException eS) {
        logger.log(Level.SEVERE, "Proof cannot be written. Class " + eS.getMessage()
            + " does not implement Serializable interface");
      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Proof cannot be constructed due to conflicting configuration.",
            e.getMessage());
      } catch (InterruptedException e) {
        logger.log(Level.SEVERE,
            "Proof cannot be written due to time out during proof construction");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    logger.log(Level.INFO, proofInfo.getInfoAsString());
  }

  protected abstract void writeProofToStream(ObjectOutputStream out, UnmodifiableReachedSet reached)
      throws IOException, InvalidConfigurationException, InterruptedException;


  @Override
  public void readProof() throws IOException, ClassNotFoundException, InvalidConfigurationException {
    Triple<InputStream, ZipInputStream, ObjectInputStream> proofStream = openProofStream();
    readProofFromStream(proofStream.getThird());
    proofStream.getThird().close();
    proofStream.getSecond().close();
    proofStream.getFirst().close();
  }

  /**
   * @param pOut the outputstream to which should be written
   * @throws IOException may be thrown in subclasses
   */
  protected boolean writeAdditionalProofStream(final ObjectOutputStream pOut) throws IOException {
    return false;
  }

  protected Triple<InputStream, ZipInputStream, ObjectInputStream> openProofStream() throws IOException {
    InputStream fis = Files.newInputStream(proofFile);
    ZipInputStream zis = new ZipInputStream(fis);
    ZipEntry entry = zis.getNextEntry();
    assert entry.getName().equals("Proof");
    return Triple.of(fis, zis, new ObjectInputStream(zis));
  }

  public Triple<InputStream, ZipInputStream, ObjectInputStream> openAdditionalProofStream(final int index)
      throws IOException {
    if (index < 0) { throw new IllegalArgumentException("Not a valid index. Indices must be at least zero."); }
    InputStream fis = Files.newInputStream(proofFile);
    ZipInputStream zis = new ZipInputStream(fis);
    ZipEntry entry = null;
    for (int i = 0; i <= 1 + index; i++) {
      entry = zis.getNextEntry();
    }

    assert entry.getName().equals("Additional " + index);
    return Triple.of(fis, zis, new ObjectInputStream(zis));
  }

  protected abstract void readProofFromStream(ObjectInputStream in) throws ClassNotFoundException, InvalidConfigurationException, IOException;

  protected void addPCCStatistic(final Statistics pPCCStatistic) {
    pccStats.add(pPCCStatistic);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.addAll(pccStats);
  }

  @Override
  public Collection<Statistics> getAdditionalProofGenerationStatistics(){
    if(proofInfo != null) {
      Collection<Statistics> stats = new ArrayList<>();
      stats.add(proofInfo);
      return stats;
    }
    return Collections.emptySet();
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
      out.println("  Time for abstract successor checks:     " + transferTimer + " (Calls: "
          + transferTimer.getNumberOfIntervals() + ")");
      out.println("  Time for covering checks:               " + stopTimer + " (Calls: "
          + stopTimer.getNumberOfIntervals()
          + ")");
      out.println(" Time for checking property:          "   + propertyCheckingTimer);
      out.println("Proof file size (bytes):                      "  + fileProofSize);
    }

    public void increaseProofSize(int pIncrement) {
      proofSize+=pIncrement;
    }

  }


}
