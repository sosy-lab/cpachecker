/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy.arg;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getUncoveredChildrenView;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Preconditions;

@Options(prefix = "pcc.arg.cmc")
public class ARG_CMCStrategy extends AbstractStrategy {

  private final Configuration globalConfig;
  private final ShutdownNotifier shutdown;
  private final CFA cfa;

  private ARGState[] roots;
  private boolean proofKnown = false;


  @Option(secure = true, name = "file", description = "write collected assumptions to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumptionsFile = Paths.get("assumptions.txt");
  @Option(secure = true, description = "List of files with configurations to use. ")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;
  // TODO proof checker strategy since it does not account for strengthening which is required to check assumption guiding automaton
  @Option(secure = true,
      description = "Which ARG strategy to use for each partial ARG, strategy based on CPA (true) or on proof checker interface (false)")
  private boolean useArgCpaStrategy = true;
  /* as long as formulae are read require that same manager is used for both, cannot create CPA twice, must wait until assumption automaton ready
   * @Option(secure = true,
      description = "Enable if partial ARGs can be read based using different CPA object than checker")*/
  private boolean interleavedMode = true;

  public ARG_CMCStrategy(Configuration pConfig, LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    pConfig.inject(this);
    globalConfig = pConfig;
    shutdown = pShutdownNotifier;
    cfa = pCfa;
  }

  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException, InterruptedException {
    if (!(pReached instanceof HistoryForwardingReachedSet)) {
      throw new InvalidConfigurationException("Reached sets used by restart algorithm are not memorized. Please enable option analysis.memorizeReachedAfterRestart");
    }

    Collection<ReachedSet> partialReachedSets =
        ((HistoryForwardingReachedSet) pReached).getAllReachedSetsUsedAsDelegates();
    roots = new ARGState[partialReachedSets.size()];

    int index = 0;
    for (ReachedSet partialReached : partialReachedSets) {
      if (partialReached.getFirstState() == null
          || !(partialReached.getFirstState() instanceof ARGState)
          || (extractLocation(partialReached.getFirstState()) == null)) {
        logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      } else {
        stats.increaseProofSize(1);
        roots[index++] = (ARGState) partialReached.getFirstState();
      }
    }

    proofKnown = true;
  }

  @Override
  protected void writeProofToStream(ObjectOutputStream pOut, UnmodifiableReachedSet pReached) throws IOException,
      InvalidConfigurationException, InterruptedException {
    constructInternalProofRepresentation(pReached);
    pOut.writeInt(roots.length);
    for (ARGState root : roots) {
      pOut.writeObject(root);
    }
  }

  @Override
  protected void readProofFromStream(ObjectInputStream pIn) throws ClassNotFoundException,
      InvalidConfigurationException, IOException {
    roots = new ARGState[pIn.readInt()];
  }

  @Override
  public boolean checkCertificate(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Start checking partial ARGs");
    pReachedSet.popFromWaitlist();

    if(interleavedMode) {
      return checkAndReadInterleaved();
    }

    return checkAndReadSequentially();
  }

  private boolean checkAndReadSequentially() {
    try {
      final ReachedSetFactory factory = new ReachedSetFactory(globalConfig, logger);
      List<ARGState> incompleteStates = new ArrayList<>();

      Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
      try {
        streams = openProofStream();
        ObjectInputStream o = streams.getThird();
        o.readInt();

        Object readARG;
        for (int i = 0; i < roots.length; i++) {
          logger.log(Level.FINEST, "Build CPA for reading and checking partial ARG", i);
          GlobalInfo.getInstance().storeCPA(buildPartialCPA(i, factory, true));
          readARG = o.readObject();
          if (!(readARG instanceof ARGState)) { return false; }

          roots[i] = (ARGState) readARG;

          incompleteStates.clear();
          shutdown.shutdownIfNecessary();

          // check current partial ARG
          logger.log(Level.INFO, "Start checking partial ARG ", i);
          if (roots[i] == null
              || !checkPartialARG(factory.create(), roots[i], incompleteStates, i, factory)) {
            logger.log(Level.FINE, "Checking of partial ARG ", i, " failed.");
            return false;
          }
          shutdown.shutdownIfNecessary();

          if (i + 1 != roots.length) {
            // write automaton for next partial ARG
            logger.log(Level.FINE,
                    "Write down report of non-checked states which is provided to next partial ARG check. Report is given by assumption automaton.");
            writeAutomaton(roots[i], incompleteStates);
            shutdown.shutdownIfNecessary();
          }
          logger.log(Level.INFO, "Checking of partial ARG ", i, " finished");

        }
      } catch (IOException | ClassNotFoundException e) {
        logger.logUserException(Level.SEVERE, e, "Partition reading failed. Stop checking");
        return false;
      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Could not set up a configuration for partial ARG checking");
        return false;
      } catch (Exception e2) {
        logger.logException(Level.SEVERE, e2, "Failure during proof reading or checking");
        return false;
      } finally {
        logger.log(Level.INFO, "Stop checking partial ARGs");
        if (streams != null) {
          try {
            streams.getThird().close();
            streams.getSecond().close();
            streams.getFirst().close();
          } catch (IOException e) {
          }
        }
      }

      return incompleteStates.size() == 0 && roots.length > 0;


    } catch (InvalidConfigurationException e1) {
      logger.log(Level.SEVERE, "Cannot create reached sets for partial ARG checking", e1);
      return false;
    }
  }

  private boolean checkAndReadInterleaved() throws InterruptedException, CPAException {
    try {
      final ReachedSetFactory factory = new ReachedSetFactory(globalConfig, logger);


      final AtomicBoolean checkResult = new AtomicBoolean(true);
      final Semaphore partitionsAvailable = new Semaphore(0);

      Thread readerThread = new Thread(new Runnable() {

        @Override
        public void run() {
          Triple<InputStream, ZipInputStream, ObjectInputStream> streams = null;
          try {
            streams = openProofStream();
            ObjectInputStream o = streams.getThird();
            o.readInt();

            Object readARG;
            for (int i = 0; i < roots.length && checkResult.get(); i++) {
              logger.log(Level.FINEST, "Build CPA for correctly reading ", i);
              GlobalInfo.getInstance().storeCPA(buildPartialCPA(i, factory, false));
              readARG = o.readObject();
              if (!(readARG instanceof ARGState)) {
                abortPreparation();
              }

              roots[i] = (ARGState) readARG;

              if (shutdown.shouldShutdown()) {
                abortPreparation();
                break;
              }
              partitionsAvailable.release();
            }
          } catch (IOException | ClassNotFoundException e) {
            logger.logUserException(Level.SEVERE, e, "Partition reading failed. Stop checking");
            abortPreparation();
          } catch (Exception e2) {
            logger.logException(Level.SEVERE, e2, "Unexpected failure during proof reading");
            abortPreparation();
          } finally {
            if (streams != null) {
              try {
                streams.getThird().close();
                streams.getSecond().close();
                streams.getFirst().close();
              } catch (IOException e) {
              }
            }
          }
        }

        private void abortPreparation() {
          checkResult.set(false);
          partitionsAvailable.release();
        }
      });

      try {

        if (proofKnown) {
          partitionsAvailable.release(roots.length);
        } else {
          readerThread.start();
        }

        List<ARGState> incompleteStates = new ArrayList<>();

        // check partial ARGs
        for (int i = 0; i < roots.length && checkResult.get(); i++) {
          //wait until next partial ARG is read
          partitionsAvailable.acquire();
          incompleteStates.clear();
          shutdown.shutdownIfNecessary();

          // check current partial ARG
          logger.log(Level.INFO, "Start checking partial ARG ", i);
          if (!checkResult.get() || roots[i] == null
              || !checkPartialARG(factory.create(), roots[i], incompleteStates, i, factory)) {
            logger.log(Level.FINE, "Checking of partial ARG ", i, " failed.");
            return false;
          }
          shutdown.shutdownIfNecessary();

          if (i + 1 != roots.length) {
            // write automaton for next partial ARG
            logger.log(Level.FINE,
               "Write down report of non-checked states which is provided to next partial ARG check. Report is given by assumption automaton.");
            writeAutomaton(roots[i], incompleteStates);
            shutdown.shutdownIfNecessary();
          }
          logger.log(Level.INFO, "Checking of partial ARG ", i, " finished");
        }


        return checkResult.get() && incompleteStates.size() == 0 && roots.length > 0;

      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Could not set up a configuration for partial ARG checking");
      } finally {
        logger.log(Level.INFO, "Stop checking partial ARGs");
        checkResult.set(false);
        readerThread.interrupt();
      }
    } catch (InvalidConfigurationException e1) {
      logger.log(Level.SEVERE, "Cannot create reached sets for partial ARG checking", e1);
      return false;
    }

    return false;
  }

  private boolean checkPartialARG(ReachedSet pReachedSet, ARGState pRoot, List<ARGState> pIncompleteStates,
      int iterationNumber, ReachedSetFactory reachedSetFactory) throws CPAException, InterruptedException,
      InvalidConfigurationException {
   // set up proof checking configuration for next parital ARG
    ConfigurableProgramAnalysis cpa;
    logger.log(Level.FINER, "Set up proof checking for partial ARG ", iterationNumber);
    logger.log(Level.FINEST, "Build CPA for next proof checking iteration");
    if (interleavedMode) {
      cpa = buildPartialCPA(iterationNumber, reachedSetFactory, true);
    } else {
      cpa = GlobalInfo.getInstance().getCPA().get();
    }

    // set up proof checker
    logger.log(Level.FINEST, "Initialize reached set");
    CFANode mainFun = AbstractStates.extractLocation(pRoot);
    if (mainFun == null) { throw new InvalidConfigurationException(
        "Require that ARG states contain location information."); }
    pReachedSet.add(cpa.getInitialState(mainFun, StateSpacePartition.getDefaultPartition()),
        cpa.getInitialPrecision(mainFun, StateSpacePartition.getDefaultPartition()));

    AbstractARGStrategy partialProofChecker;
    logger.log(Level.FINEST, "Build checking instance");
    if (useArgCpaStrategy) {
      Preconditions.checkState(cpa instanceof PropertyCheckerCPA,
              "Conflicting configuration: Partial ARGs should be checked with CPA based strategy but toplevel CPA is not a PropertyCheckerCPA as needed");
      partialProofChecker = new ARG_CPAStrategy(globalConfig, logger, shutdown, (PropertyCheckerCPA) cpa);
    } else {
      Preconditions.checkState(cpa instanceof ProofChecker,
              "Conflicting configuration: Partial ARGs should be checked with Proof Checker based strategy but CPA does not implmenet ProofChecker interface");
      partialProofChecker = new ARGProofCheckerStrategy(globalConfig, logger, shutdown, (ProofChecker) cpa);
    }

    logger.log(Level.FINER, "Start checking algorithm for partial ARG ", iterationNumber);
    return partialProofChecker.checkCertificate(pReachedSet, pRoot, pIncompleteStates);
  }

  private ConfigurableProgramAnalysis buildPartialCPA(int iterationNumber, ReachedSetFactory pFactory, boolean withSpecification)
      throws InvalidConfigurationException, CPAException {
    // create configuration for current partial ARG checking
    logger.log(Level.FINEST, "Build CPA configuration");
    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    try {
      if (configFiles == null) {
        throw new InvalidConfigurationException(
          "Require that option pcc.arg.configFiles is set for proof checking");
      }
      singleConfigBuilder.loadFromFile(configFiles.get(iterationNumber));
    } catch (IOException e) {
      throw new InvalidConfigurationException("Cannot read configuration for current partial ARG checking.");
    }
    if (globalConfig.hasProperty("specification")) {
      singleConfigBuilder.copyOptionFrom(globalConfig, "specification");
    }
    Configuration singleConfig = singleConfigBuilder.build();

    // create CPA to check current partial ARG
    logger.log(Level.FINEST, "Create CPA instance");
    if (withSpecification) {
      return new CPABuilder(singleConfig, logger, shutdown, pFactory).buildCPAWithSpecAutomatas(cfa);
    } else {
      return new CPABuilder(singleConfig, logger, shutdown, pFactory).buildCPAs(cfa, null);
    }
  }

  private void writeAutomaton(ARGState root, List<ARGState> incompleteNodes) throws CPAException {
    assert(notCovered(incompleteNodes));
    logger.log(Level.FINEST, "Identify states for assumption automaton");

    TreeSet<ARGState> automatonStates = new TreeSet<>(incompleteNodes);
    Deque<ARGState> toAdd = new ArrayDeque<>(incompleteNodes);

    while (!toAdd.isEmpty()) {
      ARGState current = toAdd.pop();
      assert !current.isCovered();

      if (automatonStates.add(current)) {
        // current was not yet contained in parentSet,
        // so we need to handle its parents

        toAdd.addAll(current.getParents());

        for (ARGState coveredByCurrent : current.getCoveredByThis()) {
          toAdd.addAll(coveredByCurrent.getParents());
        }
      }
    }

    try (Writer w = Files.openOutputFile(assumptionsFile)) {
      logger.log(Level.FINEST, "Write assumption automaton to file ", assumptionsFile);
      PCCAssumptionAutomatonWriter.writeAutomaton(w, root.getStateId(), automatonStates , new HashSet<AbstractState>(incompleteNodes));
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not write assumption automaton for next partial ARG checking");
      throw new CPAException("Assumption automaton writing failed", e);
    }
  }

  private boolean notCovered(List<ARGState> nodes){
    for(ARGState state: nodes) {
      if(state.isCovered()){
        return false;
      }
    }
    return true;
  }

  private static class PCCAssumptionAutomatonWriter {

    private static void writeAutomaton(Appendable sb, int initialStateId,
        Set<ARGState> relevantStates, Set<AbstractState> falseAssumptionStates) throws IOException {
      sb.append("OBSERVER AUTOMATON AssumptionAutomaton\n\n");

      sb.append("INITIAL STATE ARG" + initialStateId + ";\n\n");
      sb.append("STATE __TRUE :\n");
      sb.append("    TRUE ->  GOTO __TRUE;\n\n");

      if (!falseAssumptionStates.isEmpty()) {
        sb.append("STATE __FALSE :\n");
        sb.append("    TRUE ->  GOTO __FALSE;\n\n");
      }

      for (final ARGState s : relevantStates) {
        assert !s.isCovered();

        if (falseAssumptionStates.contains(s)) {
          continue;
        }

        sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");

        final StringBuilder descriptionForInnerMultiEdges = new StringBuilder();
        int multiEdgeID = 0;

        final CFANode loc = AbstractStates.extractLocation(s);
        for (final ARGState child : getUncoveredChildrenView(s)) {
          assert !child.isCovered();

          CFAEdge edge = loc.getEdgeTo(extractLocation(child));

          if (edge instanceof MultiEdge) {
            assert (((MultiEdge) edge).getEdges().size() > 1);

            sb.append("    MATCH \"");
            escape(((MultiEdge) edge).getEdges().get(0).getRawStatement(), sb);
            sb.append("\" -> ");
            sb.append("GOTO ARG" + s.getStateId() + "M" + multiEdgeID);

            boolean first = true;
            for (CFAEdge innerEdge : from(((MultiEdge) edge).getEdges()).skip(1)) {

              if (!first) {
                multiEdgeID++;
                descriptionForInnerMultiEdges.append("GOTO ARG" + s.getStateId() + "M" + multiEdgeID + ";\n");
                descriptionForInnerMultiEdges.append("    TRUE -> GOTO __TRUE;\n\n");
              } else {
                first = false;
              }

              descriptionForInnerMultiEdges.append("STATE USEFIRST ARG" + s.getStateId() + "M" + multiEdgeID + " :\n");

              descriptionForInnerMultiEdges.append("    MATCH \"");
              escape(innerEdge.getRawStatement(), descriptionForInnerMultiEdges);
              descriptionForInnerMultiEdges.append("\" -> ");
            }

            finishTransition(descriptionForInnerMultiEdges, child, relevantStates, falseAssumptionStates);
            descriptionForInnerMultiEdges.append(";\n");
            descriptionForInnerMultiEdges.append("    TRUE -> GOTO __TRUE;\n\n");

          } else {

            sb.append("    MATCH \"");
            escape(edge.getRawStatement(), sb);
            sb.append("\" -> ");

            finishTransition(sb, child, relevantStates, falseAssumptionStates);

          }

          sb.append(";\n");
        }
        sb.append("    TRUE -> GOTO __TRUE;\n\n");
        sb.append(descriptionForInnerMultiEdges);

      }
      sb.append("END AUTOMATON\n");
    }

    private static void finishTransition(final Appendable writer, final ARGState child, final Set<ARGState> relevantStates,
        final Set<AbstractState> falseAssumptionStates)
        throws IOException {
      if (falseAssumptionStates.contains(child)) {
        writer.append("GOTO __FALSE");
      } else if (relevantStates.contains(child)) {
        writer.append("GOTO ARG" + child.getStateId());
      } else {
        writer.append("GOTO __TRUE");
      }
    }

    private static void escape(String s, Appendable appendTo) throws IOException {
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        switch (c) {
        case '\n':
          appendTo.append("\\n");
          break;
        case '\"':
          appendTo.append("\\\"");
          break;
        case '\\':
          appendTo.append("\\\\");
          break;
        case '`':
          break;
        default:
          appendTo.append(c);
          break;
        }
      }
    }
  }
}
