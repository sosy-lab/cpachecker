/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.propertyscope;


import static java.util.stream.StreamSupport.stream;
import static org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeUtil.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.mpa.PropertyStats;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.automaton.MatchInfo;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeUtil.FormulaGlobalsInspector;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeUtil.FormulaVariableResult;
import org.sosy_lab.cpachecker.cpa.propertyscope.ScopeLocation.Reason;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.holder.Holder;
import org.sosy_lab.cpachecker.util.holder.HolderLong;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

@Options(prefix="cpa.propertyscope")
public class PropertyScopeStatistics extends AbstractStatistics {

  @Option(secure=true, description="Do not collect additional statistics but only try to find a "
      + "new entry function and closely related statistics")
  private boolean onlyFindNewEntryFunction = false;

  @Option(secure=true, description="Add all functions from the CFA as nodes of the "
      + "PropertyScopeCallGraph, this assures that the graph contains all function nodes, not "
      + "only the reached ones")
  private boolean prepopulateCallgraph = true;

  @Option(secure = true, description = "Skips ARGStates which are irrelevant inside the "
      + "PropertyScopeGraph, even if the state has multiple parents or children.")
  private boolean skipIrrelevantGraphBranchStates = true;

  @Option(secure = true, description = "Show skipped function entry/exits in the irrelevant"
      + "NOdes of the PropertyScopeGraph")
  private boolean showEntryExitFunctons = false;

  @Option(description = "Where to export the property scope callgraph to")
  @FileOption(Type.OUTPUT_FILE)
  private Path callgraphGraphmlFile = Paths.get("prop_scope_callgraph-%s.graphml");

  @Option(description = "Where to export the property scope graph (reduced ARG) to")
  @FileOption(Type.OUTPUT_FILE)
  private Path graphDotFile = Paths.get("prop_scope_graph-%s.dot");
  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private FormulaManagerView fmgr;

  public PropertyScopeStatistics(Configuration pConfig, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this, this.getClass());
    logger = pLogger;
    cfa = pCfa;

  }

  private Multimap<String, String> generateFuncToUsedVars() {
    Set<Partition> partitions = cfa.getVarClassification().get().getPartitions();
    return partitions.stream().collect(Collector.of(LinkedHashMultimap::create,
        (mumap, part) -> {
          for (CFAEdge edge : part.getEdges().keySet()) {
            if (edge instanceof CDeclarationEdge &&
                ((CDeclarationEdge) edge).getDeclaration().isGlobal()) {
              mumap.put("", ((CDeclarationEdge) edge).getDeclaration().getQualifiedName());
            } else {
              String functionName = edge.getSuccessor().getFunctionName();
              part.getVars().forEach(var -> mumap.put(functionName, var));
            }
          }

        }, (mumap1, mumap2) -> {
          mumap1.putAll(mumap2);
          return mumap1;
        }));
  }

  private boolean onlyUnusedVarsInAbstraction(PredicateAbstractState state,
                                   String function, Multimap<String,String> funcToUsedVars) {
    BooleanFormula formula = state.getAbstractionFormula().asFormula();
    return !fmgr.extractAtoms(formula, false).stream()
        .filter(atom -> fmgr.extractVariableNames(atom).stream()
            .anyMatch(var -> funcToUsedVars.containsEntry(function, var)))
        .findAny().isPresent();

  }

  private static long summedLengthOfTailsUntilNontrue(Stream<List<ARGState>> paths) {
    return paths
        .map(seq -> {
          long counter = 0;
          for (int i = seq.size() - 1; i >= 0; i--) {
            PredicateAbstractState prast =
                extractStateByType(seq.get(i), PredicateAbstractState.class);
            if (prast.isAbstractionState() && !prast.getAbstractionFormula().isTrue()) {
              return counter;
            }
            counter += 1;
          }
          return counter;
        }).reduce(Long::sum).orElse(0L);
  }

  private static int summedLengthOfHeadsUntilNontrue(Stream<List<ARGState>> paths) {
    return paths
        .map(seq -> {
          for (int i = 0; i < seq.size(); i++) {
            PredicateAbstractState prast =
                extractStateByType(seq.get(i), PredicateAbstractState.class);
            if (prast.isAbstractionState() && !prast.getAbstractionFormula().isTrue()) {
              return i;
            }
          }
          return 0;
        }).reduce(Integer::sum).orElse(0);
  }

  private List<Integer> depthOfFormulaSwitchAfterFirstGlobalEncounter(
      Stream<List<ARGState>> paths, boolean mustBeTruefirst) {
    return paths.map(seq -> {
      boolean wasTrueSomewhere = !mustBeTruefirst;
      BooleanFormula firstGlobalFormula = null;
      for (int i = 0; i < seq.size(); i++) {
        Optional<PredicateAbstractState> state = Optional.ofNullable(extractStateByType(seq.get(i),
            PredicateAbstractState.class));
        if (wasTrueSomewhere && state.isPresent() && !state.get().getAbstractionFormula()
            .isTrue()) {

          BooleanFormula instform = state.get().getAbstractionFormula().asInstantiatedFormula();
          FormulaGlobalsInspector insp =
              new FormulaGlobalsInspector(fmgr, instform, Optional.empty(), Optional.empty());


          if (firstGlobalFormula == null && !insp.globalAtoms.isEmpty()) {
            firstGlobalFormula = instform;
          } else if (firstGlobalFormula != null && !instform.equals(firstGlobalFormula)) {
            return i + 1;
          }

        } else if (state.isPresent() && state.get().getAbstractionFormula().isTrue()) {
          wasTrueSomewhere = true;
        }
      }
      return -1;
    }).filter(pInteger -> pInteger > 0).collect(Collectors.toList());
  }

  private static long findNontrueTrueNontrueSequences(List<List<Boolean>> tntseqs) {
    long nttntnum = tntseqs.stream().filter(seq -> {
      int ntFstIdx = seq.indexOf(false);
      int ntLstIdx = seq.lastIndexOf(false);

      if (ntFstIdx == -1 || ntLstIdx == -1) {
        return false;
      }

      for (int i = ntFstIdx; i < ntLstIdx; i++) {
        if (seq.get(i)) {
          return true;
        }
      }

      return false;
    }).count();

    return nttntnum;

  }

  private static List<List<Boolean>> computeTNTSeqs(Collection<List<ARGState>> distinctSeqs) {
    return distinctSeqs.stream()
        .map(seq -> seq.stream()
            .map(state -> extractStateByType(state, PredicateAbstractState.class)
                .getAbstractionFormula().isTrue()).collect(Collectors.toList()))
        .collect(Collectors.toList());
  }

  private static Set<List<ARGState>> extractDistinctAbstractionStateSeqs(
      Stream<List<ARGState>> paths) {
    return paths.map(path -> path.stream()
        .filter(state -> extractStateByType(state, PredicateAbstractState.class).
            isAbstractionState())
        .collect(Collectors.toList()))
        .collect(Collectors.toSet());
  }


  private Optional<String> computeNewEntryFunction(ReachedSet reached, Collection<Reason> reasons) {
    List<String> longestPrefix = null;

    for (AbstractState absSt : reached) {
      PropertyScopeState propState = extractStateByType(absSt, PropertyScopeState.class);

      if (propState.getScopeLocations().stream()
          .anyMatch(sloc -> reasons.contains(sloc.getReason()))) {
        if (longestPrefix == null) {
          longestPrefix = propState.getCallstack();
        } else {
          longestPrefix = longestPrefixOf(longestPrefix, propState.getCallstack());
        }
      }
    }

    return longestPrefix == null ? Optional.empty()
                                 : Optional.of(longestPrefix.get(longestPrefix.size() - 1));
  }

  private void computeDepthOfHighestNonTrueAbstractionInCallstack(
      ReachedSet reached) {
    Set<MatchInfo> matches = ControlAutomatonCPA.getGlobalMatchInfo();
    long depth = Long.MAX_VALUE;
    long withUsedDepth = Long.MAX_VALUE;
    long matchDepth = Long.MAX_VALUE;
    for (AbstractState absSt : reached) {
      CallstackState csSt = extractStateByType(absSt, CallstackState.class);
      Optional<PredicateAbstractState> absState = asNonTrueAbstractionState(absSt);
      Multimap<String, String> funcToUsedVars = generateFuncToUsedVars();
      if (absState.isPresent()) {
        if (!onlyUnusedVarsInAbstraction(absState.get(), csSt.getCurrentFunction(),
            funcToUsedVars)) {
          withUsedDepth = Math.min(csSt.getDepth(), withUsedDepth);
        }
        depth = Math.min(csSt.getDepth(), depth);
      }

      FluentIterable<AutomatonState> automStates =
          asIterable(absSt).filter(AutomatonState.class);

      LocationState locst = extractStateByType(absSt, LocationState.class);
      Set<CFAEdge> outgoingEdges = stream(locst.getOutgoingEdges().spliterator(), false)
          .collect(Collectors.toSet());

      if(matches.stream()
          .anyMatch(mi -> outgoingEdges.contains(mi.edge) && automStates.contains(mi.state))) {

        matchDepth = Math.min(csSt.getDepth(), matchDepth);
      }

    }



    String highestUsedStackKey =
        "Highest point in callstack with non-true abs. and vars used in func.";
    if (withUsedDepth == Long.MAX_VALUE) {
      addKeyValueStatistic(highestUsedStackKey, "<unknown>");
    } else {
      addKeyValueStatistic(highestUsedStackKey, withUsedDepth);
    }

    String highestStackKey = "Highest point in callstack with non-true abstraction formula";
    if (depth == Long.MAX_VALUE) {
      addKeyValueStatistic(highestStackKey, "<unknown>");
    } else {
      addKeyValueStatistic(highestStackKey, depth);
    }

    String highestMatchStackKey = "Highest automaton match point in callstack";
    if (matchDepth == Long.MAX_VALUE) {
      addKeyValueStatistic(highestMatchStackKey, "<unknown>");
    } else {
      addKeyValueStatistic(highestMatchStackKey, matchDepth);
    }

    String fallbackStackKey = "Highest point in callstack with automaton fallback";
    if (withUsedDepth == Long.MAX_VALUE) {
      if (matchDepth == Long.MAX_VALUE) {
        addKeyValueStatistic(highestStackKey, "<unknown>");
      } else {
        addKeyValueStatistic(fallbackStackKey, matchDepth);
      }
    } else {
      addKeyValueStatistic(fallbackStackKey, withUsedDepth);
    }


  }

  private static <T> List<T> longestPrefixOf(List<T> list1, List<T> list2) {
    int minlen = Math.min(list1.size(), list2.size());
    ArrayList<T> newList = Lists.newArrayList();
    for (int i = 0; i < minlen; i++) {
      T elem = list1.get(i);
      if (elem.equals(list2.get(i))) {
        newList.add(elem);
      } else {
        return newList;
      }
    }
    return newList;
  }

  private static Set<FormulaVariableResult> getGlobalVariablesInAbstractionFormulas(
      ReachedSet reached, FormulaManagerView fmgr) {
    return reached.asCollection().stream()
        .map(pAS -> formulaVariableSplitStream(pAS, fmgr)
            .filter(pResult -> pResult.function == null).map(pResult -> pResult)
        ).flatMap(pStringStream -> pStringStream).distinct().collect(Collectors.toSet());
  }

  private static long countNonTrueAbstractionStates(ReachedSet pReached) {
    return pReached.asCollection().stream()
        .filter(as -> asNonTrueAbstractionState(as).isPresent())
        .count();
  }




  private void handleFormulaAtoms(ReachedSet pReached) {
    HolderLong globalAtomSum = Holder.of(0L);
    HolderLong globalConstantAtomSum = Holder.of(0L);
    HolderLong globalloopIncDecAtomSum = Holder.of(0L);
    HolderLong globalloopExitCondVarsSum = Holder.of(0L);
    HolderLong atomSum = HolderLong.of(0);
    Set<String> loopIncDecVariables = cfa.getLoopStructure().get().getLoopIncDecVariables();
    Set<String> loopExitCondVars = cfa.getLoopStructure().get().getLoopExitConditionVariables();
    Set<FormulaVariableResult> globalVarInAtoms = new HashSet<>();

    pReached.asCollection().stream()
        .map(PropertyScopeUtil::asNonTrueAbstractionState)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(as -> {
          BooleanFormula instform = as.getAbstractionFormula().asInstantiatedFormula();
          FormulaGlobalsInspector insp = new FormulaGlobalsInspector(fmgr, instform,
              Optional.of(loopIncDecVariables), Optional.of(loopExitCondVars));
          globalAtomSum.value += insp.globalAtoms.size();
          globalConstantAtomSum.value += insp.globalConstantAtoms.size();
          atomSum.value += insp.atoms.size();
          globalloopIncDecAtomSum.value += insp.globalLoopIncDecAtoms.size();
          globalloopExitCondVarsSum.value += insp.globalLoopExitCondAtoms.size();
          globalVarInAtoms.addAll(insp.globalVariablesInAtoms);
        });

    double globalRatAtoms = globalAtomSum.value / (double) atomSum.value;
    addKeyValueStatistic("Average ratio of formula atoms with global variable",
        Double.isNaN(globalRatAtoms) ? "<unknown>" : globalRatAtoms);

    addKeyValueStatistic("Abs. formula atom sum", atomSum.value);
    addKeyValueStatistic("Abs. formula atoms with global variable sum", globalAtomSum.value);
    addKeyValueStatistic("Abs. formula atoms with global var. and constant sum",
        globalConstantAtomSum.value);

    addKeyValueStatistic("Abs. formula atoms with global and loopExitCondVars sum",
        globalloopExitCondVarsSum.value);

    addKeyValueStatistic("Abs. formula atoms with global and loopIncDecVars sum",
        globalloopIncDecAtomSum.value);

    addKeyValueStatistic("Global vars occuring in Atoms", "[" + globalVarInAtoms.stream()
        .map(Object::toString).collect(Collectors.joining(":")) + "]");
  }

  private static Set<String> collectFunctionsWithNonTrueAbsState(ReachedSet pReachedSet) {
    return pReachedSet.asCollection().stream()
        .filter(st -> asNonTrueAbstractionState(st).isPresent())
        .map(st -> extractStateByType(st, CallstackState.class).getCurrentFunction())
        .collect(Collectors.toSet());
  }

  @Override
  public void printStatistics(
      PrintStream pOut, Result pResult, ReachedSet pReached) {
    fmgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();

    String newEntry = computeNewEntryFunction(pReached, ImmutableSet.of(Reason
        .ABS_FORMULA_VAR_CLASSIFICATION_FORMULA_CHANGE, Reason.AUTOMATON_MATCH))
        .orElse("<unknown>");
    addKeyValueStatistic("New entry Function Candidate", newEntry);

    computeDepthOfHighestNonTrueAbstractionInCallstack(pReached);
    ARGState root = extractStateByType(pReached.getFirstState(), ARGState.class);

    if (!onlyFindNewEntryFunction) {

      Set<String> functionsInScope = collectFunctionsWithNonTrueAbsState(pReached);


      addKeyValueStatistic("Functions with non-true abstraction",
          "[" + String.join(":", functionsInScope) + "]");

      addKeyValueStatistic("Non-true abstraction function count", functionsInScope.size());


      Set<FormulaVariableResult> globalVariablesInAbstractionFormulas =
          getGlobalVariablesInAbstractionFormulas(pReached, fmgr);
      addKeyValueStatistic("Number of global variables in abstraction formulas",
          globalVariablesInAbstractionFormulas.size());

      addKeyValueStatistic("Number of non-true abstraction states",
          countNonTrueAbstractionStates(pReached));


      addKeyValueStatistic("Global observer automaton reached target count",
          ControlAutomatonCPA.getglobalObserverTargetReachCount());

      List<List<ARGState>> paths = allPathStream(root).collect(Collectors.toList());
      Set<List<ARGState>> distinctAbsSeqs = extractDistinctAbstractionStateSeqs(paths.stream());
      List<List<Boolean>> tntSeqs = computeTNTSeqs(distinctAbsSeqs);


      addKeyValueStatistic("NONTRUE-TRUE-NONTRUE sequences",
          findNontrueTrueNontrueSequences(tntSeqs));

      addKeyValueStatistic("Number of extracted ARG Paths", paths.size());

      addKeyValueStatistic("Sum. length of paths tails until first nontrue abs. st.",
          summedLengthOfTailsUntilNontrue(paths.stream()));

      addKeyValueStatistic("Sum. length of paths heads until first nontrue abs. st.",
          summedLengthOfHeadsUntilNontrue(paths.stream()));

      addKeyValueStatistic("Sum. path length",
          paths.stream().map(path -> (long) path.size()).reduce(Long::sum).orElse(0L));

      String globTargetLineNumbers = ControlAutomatonCPA.getGlobalTargetCFAEdges().stream()
          .map(CFAEdge::getLineNumber).map(Object::toString).collect(Collectors.joining(":"));
      addKeyValueStatistic("Global target state line numbers", "[" + globTargetLineNumbers + "]");

      handleFormulaAtoms(pReached);

      List<Integer> globalOtherSwitch =
          depthOfFormulaSwitchAfterFirstGlobalEncounter(paths.stream(), false);
      List<Integer> trueGlobalOtherSwitch =
          depthOfFormulaSwitchAfterFirstGlobalEncounter(paths.stream(), true);

      addKeyValueStatistic("Abs formula glob->other switch avg",
          globalOtherSwitch
              .isEmpty() ? "<unknown>" : globalOtherSwitch
              .stream().map(pInteger -> (long) pInteger).reduce(Long::sum).orElse(0L) / (double)
              globalOtherSwitch.size());

      addKeyValueStatistic("Abs formula TRUE->glob->other switch avg",
          trueGlobalOtherSwitch
              .isEmpty() ? "<unknown>" :
          trueGlobalOtherSwitch
              .stream().map(pInteger -> (long) pInteger)
              .reduce(Long::sum)
              .orElse(0L) / (double) trueGlobalOtherSwitch.size());

    }

    // Write callgraph //

    Set<String> relevantProperties = PropertyStats.INSTANCE.getRelevantProperties().stream()
        .map(Object::toString).collect(Collectors.toSet());


    PropertyScopeCallGraph graph = prepopulateCallgraph ?
                                   PropertyScopeCallGraph.create(root, cfa.getAllFunctionNames()) :
                                   PropertyScopeCallGraph.create(root);

    @SuppressWarnings("unchecked")
    ArrayList<ArrayList<Reason>> outputScopeReasonCombinations = Lists.newArrayList(
        Lists.newArrayList(Reason.ABS_FORMULA),
        Lists.newArrayList(Reason.AUTOMATON_MATCH),
        Lists.newArrayList(Reason.ABS_FORMULA_VAR_CLASSIFICATION),
        Lists.newArrayList(Reason.ABS_FORMULA_VAR_CLASSIFICATION, Reason.AUTOMATON_MATCH),
        Lists.newArrayList(Reason.ABS_FORMULA_VAR_CLASSIFICATION_FORMULA_CHANGE, Reason
            .AUTOMATON_MATCH),
        Lists.newArrayList(Reason.ABS_FORMULA_VAR_CLASSIFICATION_FORMULA_CHANGE)
    );

    for (ArrayList<Reason> reasons : outputScopeReasonCombinations) {
      String reasonsName = reasons.stream().map(Reason::name).collect(Collectors.joining("-"));
      Path outpath = Paths.get(String.format(this.callgraphGraphmlFile.toString(), reasonsName));
      try (Writer w = MoreFiles.openOutputFile(outpath, Charset.defaultCharset())) {
        new PropertyScopeCallGraphToGraphMLWriter(graph, reasons, relevantProperties).writeTo(w);
      } catch (IOException | ParserConfigurationException | TransformerException e) {
        logger.logUserException(Level.WARNING, e, "Could not write PropertyScopeCallGraph to file");
      }
    }

    boolean any_var_class = graph.getNodes().values().stream().anyMatch(node -> node
        .getScopedCFAEdgesCount(Reason.ABS_FORMULA_VAR_CLASSIFICATION) > 0);
    boolean any_automaton_match = graph.getNodes().values().stream().anyMatch(node -> node
        .getScopedCFAEdgesCount(Reason.AUTOMATON_MATCH) > 0);

    addKeyValueStatistic("preferred scope reason",
        any_var_class ? "ABS_FORMULA_VAR_CLASSIFICATION" : any_automaton_match ? "AUTOMATON_MATCH"
                                                                               : "<unknown>");

    // Write psgraph //

    PropertyScopeGraph psGraph = PropertyScopeGraph.create(root, ImmutableSet.of(Reason
        .ABS_FORMULA_VAR_CLASSIFICATION_FORMULA_CHANGE, Reason.AUTOMATON_MATCH),
        skipIrrelevantGraphBranchStates);
    String psGraphReasonsName = psGraph.getScopeReasons().stream()
        .map(Reason::name).collect(Collectors.joining("-"));
    Path psGraphOutPath =
        Paths.get(String.format(this.graphDotFile.toString(), psGraphReasonsName));
    try (Writer w = MoreFiles.openOutputFile(psGraphOutPath, Charset.defaultCharset())) {
      PropertyScopeGraphToDotWriter.write(psGraph, w, showEntryExitFunctons);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write PropertyScopeGraph to DOT file");
    }

    Path psGraphOutPathHinted =
        Paths.get(String.format(this.graphDotFile.toString(), psGraphReasonsName + "-hinted"));
    try (Writer w = MoreFiles.openOutputFile(psGraphOutPathHinted, Charset.defaultCharset())) {
      PropertyScopeGraphToDotWriter.writeHinted(psGraph, w, showEntryExitFunctons);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write PropertyScopeGraph to DOT file");
    }

    // --- //

    super.printStatistics(pOut, pResult, pReached);
  }


  @Override
  public String getName() {
    return "Predicate Property Scope";
  }
}
