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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.io.MoreFiles;
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

  public LoopInvariantsWriter(CFA pCfa, LogManager pLogger, AbstractionManager pAbsMgr,
      FormulaManagerView pFmMgr, RegionManager pRegMgr) {
    this.cfa = pCfa;
    this.logger = pLogger;
    this.absmgr = pAbsMgr;
    this.fmgr = pFmMgr;
    this.rmgr = pRegMgr;
  }

  private Map<CFANode, Region> getLoopHeadInvariants(UnmodifiableReachedSet reached) {
    if (!cfa.getAllLoopHeads().isPresent()) {
      logger.log(Level.WARNING, "Cannot dump loop invariants because loop-structure information is not available.");
      return null;
    }

    Map<CFANode, Region> regions = Maps.newHashMap();

    for (AbstractState state : reached) {
      CFANode loc = extractLocation(state);
      if (cfa.getAllLoopHeads().get().contains(loc)) {
        PredicateAbstractState predicateState = getPredicateState(state);
        if (!predicateState.isAbstractionState()) {
          logger.log(Level.WARNING, "Cannot dump loop invariants because a non-abstraction state was found for a loop-head location.");
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

    try (Writer writer = MoreFiles.openOutputFile(invariantsFile, Charset.defaultCharset())) {
      for (CFANode loc :
          from(cfa.getAllLoopHeads().get())
              .toSortedSet(Comparator.comparingInt(CFANode::getNodeNumber))) {

        Region region = regions.getOrDefault(loc, rmgr.makeFalse());
        BooleanFormula formula = absmgr.convertRegionToFormula(region);

        writer.append("loop__");
        writer.append(loc.getFunctionName());
        writer.append("__");
        writer.append(""+ ((loc.getNumLeavingEdges()==0) ? 0 : loc.getLeavingEdge(0).getLineNumber()));
        writer.append(":\n");
        fmgr.dumpFormula(formula).appendTo(writer);
        writer.append('\n');
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write loop invariants to file");
    }
  }

  public void exportLoopInvariantsAsPrecision(Path invariantPrecisionsFile, UnmodifiableReachedSet reached) {
    Map<CFANode, Region> regions = getLoopHeadInvariants(reached);
    if (regions == null) {
      return;
    }

    Set<String> uniqueDefs = new HashSet<>();
    StringBuilder asserts = new StringBuilder();

    try (Writer writer =
        MoreFiles.openOutputFile(invariantPrecisionsFile, Charset.defaultCharset())) {
      for (CFANode loc :
          from(cfa.getAllLoopHeads().get())
              .toSortedSet(Comparator.comparingInt(CFANode::getNodeNumber))) {
        Region region = regions.getOrDefault(loc, rmgr.makeFalse());
        BooleanFormula formula = absmgr.convertRegionToFormula(region);
        Pair<String, List<String>> locInvariant = PredicatePersistenceUtils.splitFormula(fmgr, formula);

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
