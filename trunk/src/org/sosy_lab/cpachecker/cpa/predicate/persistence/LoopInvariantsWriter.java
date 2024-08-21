// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class LoopInvariantsWriter {

  private final CFA cfa;
  private final LogManager logger;
  private final AbstractionManager absmgr;
  private final FormulaManagerView fmgr;
  private final RegionManager rmgr;

  public LoopInvariantsWriter(
      CFA pCfa,
      LogManager pLogger,
      AbstractionManager pAbsMgr,
      FormulaManagerView pFmMgr,
      RegionManager pRegMgr) {
    cfa = pCfa;
    logger = pLogger;
    absmgr = pAbsMgr;
    fmgr = pFmMgr;
    rmgr = pRegMgr;
  }

  private Map<CFANode, Region> getLoopHeadInvariants(UnmodifiableReachedSet reached) {
    if (!cfa.getAllLoopHeads().isPresent()) {
      logger.log(
          Level.WARNING,
          "Cannot dump loop invariants because loop-structure information is not available.");
      return null;
    }

    Map<CFANode, Region> regions = new HashMap<>();

    for (AbstractState state : reached) {
      CFANode loc = extractLocation(state);
      if (cfa.getAllLoopHeads().orElseThrow().contains(loc)) {
        PredicateAbstractState predicateState = getPredicateState(state);
        if (!predicateState.isAbstractionState()) {
          logger.log(
              Level.WARNING,
              "Cannot dump loop invariants because a non-abstraction state was found for a"
                  + " loop-head location.");
          return null;
        }

        Region region = regions.getOrDefault(loc, rmgr.makeFalse());
        region = rmgr.makeOr(region, predicateState.getAbstractionFormula().asRegion());
        regions.put(loc, region);
      }
    }

    return regions;
  }

  public void exportLoopInvariants(Path invariantsFile, UnmodifiableReachedSet reached) {
    Map<CFANode, Region> regions = getLoopHeadInvariants(reached);
    if (regions == null) {
      return;
    }

    try (Writer writer = IO.openOutputFile(invariantsFile, Charset.defaultCharset())) {
      for (CFANode loc :
          from(cfa.getAllLoopHeads().orElseThrow())
              .toSortedSet(Comparator.comparingInt(CFANode::getNodeNumber))) {

        Region region = regions.getOrDefault(loc, rmgr.makeFalse());
        BooleanFormula formula = absmgr.convertRegionToFormula(region);

        writer.append("loop__");
        writer.append(loc.getFunctionName());
        writer.append("__");
        writer.append(
            "" + ((loc.getNumLeavingEdges() == 0) ? 0 : loc.getLeavingEdge(0).getLineNumber()));
        writer.append(":\n");
        fmgr.dumpFormula(formula).appendTo(writer);
        writer.append('\n');
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write loop invariants to file");
    }
  }

  public void exportLoopInvariantsAsPrecision(
      Path invariantPrecisionsFile, UnmodifiableReachedSet reached) {
    Map<CFANode, Region> regions = getLoopHeadInvariants(reached);
    if (regions == null) {
      return;
    }

    Set<String> uniqueDefs = new HashSet<>();
    StringBuilder asserts = new StringBuilder();

    try (Writer writer = IO.openOutputFile(invariantPrecisionsFile, Charset.defaultCharset())) {
      for (CFANode loc :
          from(cfa.getAllLoopHeads().orElseThrow())
              .toSortedSet(Comparator.comparingInt(CFANode::getNodeNumber))) {
        Region region = regions.getOrDefault(loc, rmgr.makeFalse());
        BooleanFormula formula = absmgr.convertRegionToFormula(region);
        Pair<String, List<String>> locInvariant =
            PredicatePersistenceUtils.splitFormula(fmgr, formula);

        for (String def : locInvariant.getSecond()) {
          if (uniqueDefs.add(def)) {
            writer.append(def);
            writer.append("\n");
          }
        }

        asserts.append(loc.getFunctionName());
        asserts.append(" ");
        asserts.append(loc.toString());
        asserts.append(":\n");
        asserts.append(locInvariant.getFirst());
        asserts.append("\n\n");
      }

      writer.append("\n");
      writer.append(asserts);

    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write loop invariants to file");
    }
  }
}
