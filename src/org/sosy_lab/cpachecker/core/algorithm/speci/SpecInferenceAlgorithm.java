package org.sosy_lab.cpachecker.core.algorithm.speci;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AlgorithmIterationListener;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.speci.SpecInferenceState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

@Options(prefix = "speci")
public class SpecInferenceAlgorithm implements Algorithm {

  private static final String STATE_NAME_PREFIX = "State_";

  private final CPAAlgorithm wrappedAlgorithm;

  @Option(secure=true, name="output.file",
      description="Export the inferred specification as an automaton.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportSpcFile = Paths.get("specification.spc");

  @Options
  public static class SpecInferenceAlgorithmFactory {

    private final ConfigurableProgramAnalysis cpa;
    private final LogManager logger;
    private final ShutdownNotifier shutdownNotifier;
    private final AlgorithmIterationListener iterationListener;
    private final Configuration config;

    public SpecInferenceAlgorithmFactory(ConfigurableProgramAnalysis pCPA, LogManager pLogger,
        Configuration pConfig, ShutdownNotifier pShutdownNotifier, @Nullable AlgorithmIterationListener pIterationListener)
            throws InvalidConfigurationException {

      pConfig.inject(this);
      cpa = pCPA;
      logger = pLogger;
      shutdownNotifier = pShutdownNotifier;
      iterationListener = pIterationListener;
      config = pConfig;
    }

    public SpecInferenceAlgorithm newInstance() throws InvalidConfigurationException {
      return new SpecInferenceAlgorithm(cpa, logger, config, shutdownNotifier, iterationListener);
    }
  }

  private SpecInferenceAlgorithm(ConfigurableProgramAnalysis pCPA, LogManager pLogger, Configuration pConfig,
      ShutdownNotifier pShutdownNotifier, AlgorithmIterationListener pIterationListener)
          throws InvalidConfigurationException {

    Preconditions.checkNotNull(CPAs.retrieveCPA(pCPA, ARGCPA.class), "The analysis must construct an ARG!");

    wrappedAlgorithm = CPAAlgorithm.create(pCPA, pLogger, pConfig, pShutdownNotifier, pIterationListener);
  }

  public static SpecInferenceAlgorithm create(ConfigurableProgramAnalysis pCPA, LogManager pLogger,
      Configuration pConfig, ShutdownNotifier pShutdownNotifier, AlgorithmIterationListener pIterationListener)
          throws InvalidConfigurationException {

    return new SpecInferenceAlgorithmFactory(pCPA, pLogger, pConfig, pShutdownNotifier, pIterationListener).newInstance();
  }

  public static SpecInferenceAlgorithm create(ConfigurableProgramAnalysis pCPA, LogManager pLogger,
      Configuration pConfig, ShutdownNotifier pShutdownNotifier)
          throws InvalidConfigurationException {
    return new SpecInferenceAlgorithmFactory(pCPA, pLogger, pConfig, pShutdownNotifier, null).newInstance();
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    AlgorithmStatus result = wrappedAlgorithm.run(reachedSet);

    extractSpecificationAutomaton(reachedSet);

    return result;
  }

  private void extractSpecificationAutomaton(ReachedSet reachedSet) throws CPAException {
    ARGState argRootState = (ARGState)reachedSet.getFirstState();

    try (PrintWriter writer = new PrintWriter(exportSpcFile.getAbsolutePath())) {

      dumpAutomaton(writer,
          assembleAutomaton(argRootState),
          getNextRelevant(argRootState).getStateId());

    } catch (FileNotFoundException e) {
      throw new CPAException("Writing the automaton file failed!", e);
    }
  }

  private static List<String> assembleAutomaton(ARGState root) {

    List<String> retList = Lists.newLinkedList();

    SpecInferenceState curState = getSpecInfState(root);
    ARGState current = curState.isEmpty() ? getNextRelevant(root) : root;
    SpecInferenceState pred = null;
    int currSink;

    // to save memory and execution time, we do not do recursion if there is only one child
    while (current.getChildren().size() == 1) {
      StringBuilder sb = new StringBuilder();

      ARGState next = getNextRelevant(current);
      curState = getSpecInfState(current);
      currSink = curState.getAutomaton().getSink();

      // generate state in output automaton
      sb.append("STATE USEFIRST ");
      sb.append(STATE_NAME_PREFIX);
      sb.append(current.getStateId());
      sb.append(" :\n");

      // generate transition in output automaton
      sb.append("  ");
      sb.append(curState.getAutomaton().getEdge(currSink - 1).getStatement());
      sb.append(" GOTO ");
      sb.append(STATE_NAME_PREFIX);
      sb.append(next.getStateId());

      /*
       * Check if the controlflow splits in the next state and if a automaton state will be inserted.
       * If that is the case, we have to go to the state which will be inserted.
       */
      pred = curState;
      current = next;
      if (isIf(next) && (!pred.isLessOrEqual(getSpecInfState(current)))) {
        sb.append("_x");
      }

      sb.append(" ;\n\n");

      retList.add(sb.toString());
    }

    StringBuilder sb = new StringBuilder();

    if (current.getChildren().size() == 0) {

      // this state is covered
      if (current.isCovered()) {

        sb.append("DUMMY \n");
        sb.append("  MATCH {$?} -> GOTO ");
        sb.append(STATE_NAME_PREFIX);
        // MAGIC - I assume that a node where the ARG splits has only one parent
        sb.append(current.getCoveringState().getParents().iterator().next().getStateId());
        sb.append(" ;\n\n");

      } else { // at this edge the ARG ends
        // generate accepting dummy state
        sb.append("STATE USEFIRST ");
        sb.append(STATE_NAME_PREFIX);
        sb.append(current.getStateId());
        sb.append(" :\n");
        sb.append("  MATCH {$?} -> GOTO ");
        sb.append(STATE_NAME_PREFIX);
        sb.append(current.getStateId());
        sb.append(" ;\n");
        sb.append("  // ACCEPT\n\n");
      }

      retList.add(sb.toString());

    } else if (current.getChildren().size() > 1) {

      /*
       * Find out if "if". If "if", see if the state is not yet added.
       * If it isn't, add it now.
       */
      if (isIf(current) && (pred == null || !pred.isLessOrEqual(getSpecInfState(current)))) {
        curState = getSpecInfState(current);
        currSink = curState.getAutomaton().getSink();

        sb.append("STATE USEFIRST ");
        sb.append(STATE_NAME_PREFIX);
        sb.append(current.getStateId());
        sb.append("_x :\n");

        sb.append("  ");
        sb.append(curState.getAutomaton().getEdge(currSink - 1).getStatement());
        sb.append(" GOTO ");
        sb.append(STATE_NAME_PREFIX);
        sb.append(current.getStateId());
        sb.append(" ;\n\n");

        retList.add(sb.toString());
        sb = new StringBuilder();
      }

      retList.add("MARKER");
      int marker = retList.size() - 1;

      sb.append("STATE USEALL ");
      sb.append(STATE_NAME_PREFIX);
      sb.append(current.getStateId());
      sb.append(" :\n");

      for (ARGState s : current.getChildren()) {
        List<String> l = assembleAutomaton(s);
        String assume = l.remove(0);

        sb.append(assume.substring(assume.indexOf("\n") + 1));
        sb.deleteCharAt(sb.length() - 1);

        retList.addAll(l);

      }

      retList.set(marker, sb.toString());
    }

    return retList;
  }

  private static ARGState getNextRelevant(ARGState s) {

    ARGState current = s;
    SpecInferenceState currentState = getSpecInfState(current);
    ARGState next = current.getChildren().iterator().next();
    int sink;
    boolean takeFirstNotEmpty = false;

    while (true) {

      // If there is no automaton yet, continue
      if (currentState.isEmpty()) {
        current = next;
        next = current.getChildren().iterator().next();
        currentState = getSpecInfState(current);
        takeFirstNotEmpty = true;
        continue;
      } else {

        if (takeFirstNotEmpty) {
          return current;
        }

        sink = getSpecInfState(current).getAutomaton().getSink();
      }

      if (current.getChildren().size() != 1) {
        return current;
      } else if (next.getChildren().size() != 1) {
        return next;
      } else if (sink != getSpecInfState(next).getAutomaton().getSink()) {
        return next;
      } else {
        current = next;
        next = current.getChildren().iterator().next();
        currentState = getSpecInfState(current);
      }
    }

  }

  private static SpecInferenceState getSpecInfState(AbstractState s) {

    CompositeState compState = (CompositeState) ((ARGState) s).getWrappedState();
    for (AbstractState abstractState : compState.getWrappedStates()) {
      if (abstractState instanceof SpecInferenceState) {
        return (SpecInferenceState) abstractState;
      }
    }

    throw new IllegalArgumentException("Did not find a SpecInferenceState!");
  }

  private static void dumpAutomaton(PrintWriter pOut, List<String> aut, int initState) {

    pOut.print("OBSERVER AUTOMATON AutomatonName\n\n");
    pOut.print("INITIAL STATE ");

    /*
     * See if we find State_<N>_x. If we do, this is probably the initial state.
     *
     * If it is there, it will probably be somewhere at the starting.
     * So we only search the first few lines.
     */
    boolean alternativeInitStatePresent = false;
    for (int i = 0; i < Math.min(15, aut.size() - 1); i++) {
      if (aut.get(i).contains(STATE_NAME_PREFIX + initState + "_x")) {
        alternativeInitStatePresent = true;
        break;
      }
    }

    pOut.print(STATE_NAME_PREFIX);
    pOut.print(initState);
    if(alternativeInitStatePresent) {
      pOut.print("_x");
    }
    pOut.print(" ;\n\n");

    for (String s : aut) {

      pOut.print(s);
      if (!s.contains("// ACCEPT")) {
        pOut.print("  MATCH EXIT -> ERROR;\n\n");
      }
    }

    pOut.print("END AUTOMATON");
  }

  private static boolean isIf(ARGState s) {
    return s.getChildren().size() > 1 &&
        // FIXME: I try to find out if thingy is loop start. How can it be done nicer? Maybe coverage?
        // works though...
        !s.getEdgeToChild(s.getChildren().iterator().next()).getPredecessor().isLoopStart();
  }
}
