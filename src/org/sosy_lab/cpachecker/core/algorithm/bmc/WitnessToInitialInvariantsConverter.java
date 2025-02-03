// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.Invariant;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.InvariantExchangeFormatTransformer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "bmc.kinduction.reuse")
public class WitnessToInitialInvariantsConverter {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;
  private final CFA cfa;

  public WitnessToInitialInvariantsConverter(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pshutdownNotifier,
      final FormulaManagerView pformulaManagerView,
      final PathFormulaManager ppathFormulaManager,
      final CFA pCFA)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pshutdownNotifier;
    formulaManagerView = pformulaManagerView;
    pathFormulaManager = ppathFormulaManager;
    cfa = pCFA;
    config.inject(this);
  }

  @Option(
      secure = true,
      description = "Matches function names to the ones in the witness",
      name = "matchFunctionNames")
  private boolean matchFunctionNames = false;

  @Option(
      secure = true,
      description =
          "Matches witness invariants to near loopheads. If matchFunctionNames, then only unmatched"
              + " leftovers",
      name = "matchNearLoopheads")
  private boolean matchNearLoopheads = false;

  @Option(
      secure = true,
      description =
          "How many Candidates should be generated out of invariant to nearest loophead pairings."
              + " No effect when matchNearLoophead = false",
      name = "numberLoopheadMatches")
  private int numberLoopheadMatches = 3;

  public ImmutableSet<CandidateInvariant> witnessConverter(Path pFileName) {

    InputStream witness;
    ImmutableSet.Builder<CandidateInvariant> candidates = ImmutableSet.builder();

    try {
      // parse file
      witness = MoreFiles.asByteSource(pFileName).openStream();
      List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(witness);
      witness.close();
      InvariantExchangeFormatTransformer transformer =
          new InvariantExchangeFormatTransformer(config, logger, shutdownNotifier, cfa);
      Set<Invariant> invariantSet = transformer.generateInvariantsFromEntries(entries);
      ImmutableSet.Builder<Invariant> leftoverInvariantsBuilder = new ImmutableSet.Builder<>();

      SetMultimap<String, CFANode> loopHeadsPerFunction = HashMultimap.create();
      for (CFANode loopHead : cfa.getAllLoopHeads().orElseThrow()) {
        loopHeadsPerFunction.put(loopHead.getFunctionName(), loopHead);
      }

      ImmutableSet<Invariant> leftoverInvariants = null;
      // match function names
      if (matchFunctionNames) {
        for (Invariant invariant : invariantSet) {
          String name = invariant.getFunction();
          Set<CFANode> loopHeadsWithSameName = loopHeadsPerFunction.get(name);
          // handle same function name on multiple functions
          CFANode candidate = null;
          if (loopHeadsWithSameName.size() > 1) {
            Integer minimumDistance = Integer.MAX_VALUE;

            for (CFANode loopHead : loopHeadsWithSameName) {
              int distance =
                  Math.abs(
                      loopHead.getFunction().getFileLocation().getStartingLineNumber()
                          - invariant.getLine());
              if (distance < minimumDistance) {
                minimumDistance = distance;
                candidate = loopHead;
              }
            }

          } else if (loopHeadsWithSameName.size() == 1) {
            candidate = loopHeadsWithSameName.iterator().next();
          } else {
            leftoverInvariantsBuilder.add(invariant);
          }
          if (candidate != null) {
            loopHeadsPerFunction.remove(name, candidate);
            candidates.add(
                SingleLocationFormulaInvariant.makeLocationInvariant(
                    candidate, toFormula(invariant.getFormula()), formulaManagerView));
          }
        }
        leftoverInvariants = leftoverInvariantsBuilder.build();
      } else {
        leftoverInvariants = (ImmutableSet<Invariant>) invariantSet;
      }

      // match to nearest loopheads
      if (matchNearLoopheads && numberLoopheadMatches > 0 && !leftoverInvariants.isEmpty()) {
        NavigableMap<Integer, CFANode> loopHeadsOrderedByLine = new TreeMap<>();
        for (CFANode loopHead : loopHeadsPerFunction.values()) {
          loopHeadsOrderedByLine.put(
              loopHead.getFunction().getFileLocation().getStartingLineNumber(), loopHead);
        }
        for (Invariant invariant : leftoverInvariants) {
          int line = invariant.getLine();
          int candidatesToGenerate = Math.min(numberLoopheadMatches, loopHeadsOrderedByLine.size());
          Integer nextHigher = loopHeadsOrderedByLine.ceilingKey(line);
          Integer nextLower = loopHeadsOrderedByLine.floorKey(line);
          Integer nextCanditateLine = null;
          for (int i = 0; i < candidatesToGenerate; i++) {
            if (nextHigher == null && nextLower == null) {
              i = candidatesToGenerate;
              nextCanditateLine = null;
            } else if (nextHigher == null) {
              nextCanditateLine = nextLower;
              nextLower = loopHeadsOrderedByLine.floorKey(nextCanditateLine - 1);
            } else if (nextLower == null) {
              nextCanditateLine = nextHigher;
              nextHigher = loopHeadsOrderedByLine.ceilingKey(nextCanditateLine + 1);
            } else if (line - nextLower <= nextHigher - line) {
              nextCanditateLine = nextLower;
              nextLower = loopHeadsOrderedByLine.floorKey(nextCanditateLine - 1);
            } else {
              nextCanditateLine = nextHigher;
              nextHigher = loopHeadsOrderedByLine.ceilingKey(nextCanditateLine + 1);
            }
            if (nextCanditateLine != null) {
              candidates.add(
                  SingleLocationFormulaInvariant.makeLocationInvariant(
                      loopHeadsOrderedByLine.get(nextCanditateLine),
                      toFormula(invariant.getFormula()),
                      formulaManagerView));
            }
          }
        }
      }

    } catch (IOException
        | InvalidConfigurationException
        | InterruptedException
        | CPATransferException e) {
      logger.logUserException(Level.INFO, e.getCause(), "Could not parse witness file");
      return ImmutableSet.of();
    }

    return candidates.build();
  }

  // from org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionBootstrapper
  private BooleanFormula toFormula(ExpressionTree<AExpression> expressionTree)
      throws CPATransferException, InterruptedException {
    ToFormulaVisitor toFormulaVisitor =
        new ToFormulaVisitor(formulaManagerView, pathFormulaManager, null);
    try {
      return expressionTree.accept(toFormulaVisitor);
    } catch (ToFormulaException e) {
      Throwables.throwIfInstanceOf(e.getCause(), CPATransferException.class);
      Throwables.throwIfInstanceOf(e.getCause(), InterruptedException.class);
      Throwables.throwIfUnchecked(e.getCause());
      throw new UnexpectedCheckedException("expression tree to formula", e);
    }
  }
}
// public ImmutableSet<CandidateInvariant> witnessParser(Path pfilename, Solver pSolver)
// throws JsonParseException, JsonMappingException, IOException {
// FormulaManagerView formulaManager = pSolver.getFormulaManager();
// File yamlWitness = pfilename.toFile();
// ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
// JavaType entryType =
// mapper.getTypeFactory().constructCollectionType(List.class, InvariantSetEntry.class);
// List<InvariantSetEntry> entries = mapper.readValue(yamlWitness, entryType);
// ImmutableSet.Builder<CandidateInvariant> invariants = ImmutableSet.builder();
//
// NavigableMap<Integer, CFANode> loopHeadsOrderedByLine = new TreeMap<Integer, CFANode>();
// for (CFANode loopHead : cfa.getAllLoopHeads().orElseThrow()) {
// loopHeadsOrderedByLine.put(loopHead.getEnteringEdge(0).getLineNumber(), loopHead);
// }
//
// for (InvariantSetEntry entry : entries) {
// while (!entry.content.isEmpty()) {
// InvariantEntry invariant = (InvariantEntry) entry.content.remove(0);
// Integer line = invariant.getLocation().getLine();
//
// // find nearest loophead if exact match not found
// if (!loopHeadsOrderedByLine.containsKey(line)) {
// NavigableSet<Integer> lineSet = loopHeadsOrderedByLine.navigableKeySet();
// Integer lowerLine = lineSet.lower(line);
// Integer higherLine = lineSet.higher(line);
// if (lowerLine == null && higherLine != null) {
// if (line - lowerLine < higherLine - line) {
// line = lowerLine;
// } else {
// line = higherLine;
// }
// } else {
// if (lowerLine == null) {
// line = higherLine;
// } else {
// line = lowerLine;
// }
// }
// }
// logger.log(Level.INFO, loopHeadsOrderedByLine.get(line), "Line", line);

        // BooleanFormula booleanInvariant = formulaManager.parse(invariant.getValue());
        //
        // invariants.add(
        // SingleLocationFormulaInvariant
        // .makeLocationInvariant(
        // loopHeadsOrderedByLine.get(line),
        // invariant.getValue()));
// }
// }
//
// return invariants.build();
// }
// }
