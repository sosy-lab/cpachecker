// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.ANYPREV_SUFFIX;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.AT_PREFIX;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.AffineFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.NestedRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class TerminationArgumentsToWitnessUtils {

  private static String rightSideOfRankingFunction(String pRankingFunction) {
    // The ranking function comes from LassoRanker, so we have little options on how to process it,
    // therefore, we have to work with the strings.
    int firstEquals = pRankingFunction.indexOf('=');
    return pRankingFunction.substring(firstEquals + 1).trim();
  }

  // The function replaces variable names with annotation \at(..., AnyPrev), i.e. x -> \at(x,
  // AnyPrev) and casts them into a larger type
  private static String wrapTheVariablesWithAtAnyPrev(
      String pRankingFunction, Iterable<IProgramVar> pVars) {
    return replaceVariables(
        pRankingFunction,
        pVars,
        varName -> "((__int128)" + AT_PREFIX + varName + ANYPREV_SUFFIX + ")");
  }

  // The function casts the variables into (__int128) as we want to prevent overflows in the
  // witness
  private static String wrapTheVariablesWithCastToLongLong(
      String pRankingFunction, Iterable<IProgramVar> pVars) {
    return replaceVariables(pRankingFunction, pVars, varName -> "((__int128)" + varName + ")");
  }

  /**
   * Replaces every occurrence of the given variables in the expression by the result of the given
   * function. Only whole variable names are replaced, i.e., for variables t and tmp the variable t
   * is not replaced inside tmp. The replacement is done in a single pass, such that the inserted
   * text is never replaced again.
   */
  private static String replaceVariables(
      String pExpression, Iterable<IProgramVar> pVars, Function<String, String> pReplacement) {
    // Longer names come first in the alternation, such that for overlapping names like
    // (* main::p) and main::p the longest one matches
    ImmutableList<String> varNames =
        FluentIterable.from(pVars)
            .transform(IProgramVar::toString)
            .toSortedSet(
                Comparator.comparingInt(String::length)
                    .reversed()
                    .thenComparing(Comparator.naturalOrder()))
            .asList();
    if (varNames.isEmpty()) {
      return pExpression;
    }

    // A variable must not be preceded or followed by characters that can be part of a
    // (qualified) variable name
    Pattern variables =
        Pattern.compile(
            "(?<![\\w:])("
                + FluentIterable.from(varNames).transform(Pattern::quote).join(Joiner.on('|'))
                + ")(?![\\w:])");
    return variables
        .matcher(pExpression)
        .replaceAll(match -> Matcher.quoteReplacement(pReplacement.apply(match.group(1))));
  }

  /** Converts supporting invariant into an invariant entry. */
  public static InvariantEntry convertSupportingInvariantToInvariantEntry(
      SupportingInvariant pSupportingInvariant, CFANode pLoopHead, CFAEdge pIncomingLoopEdge) {
    // Ideally, this should be done via AstToCFARelation, however, this breaks due to copying of CFA
    // This is planned in https://gitlab.com/sosy-lab/software/cpachecker/-/work_items/1483
    FileLocation fileLocation = pIncomingLoopEdge.getFileLocation();

    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    // Supporting invariant is an object from LassoRanker library, and we do not have a transformer
    // to our CExpression. Maybe in future, we could implement such transformer, however, so far,
    // we did not have problems with just using it as string.
    String invariant =
        wrapTheVariablesWithCastToLongLong(
            pSupportingInvariant.toString(), pSupportingInvariant.getVariables());
    return new InvariantEntry(
        TransitionInvariantUtils.removeFunctionFromVarsName(invariant),
        InvariantRecordType.LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.C,
        locationRecord);
  }

  /**
   * Iterates through all the termination arguments in form of ranking functions and converts them
   * to invariant entry expressed with transition invariant.
   */
  public static InvariantEntry convertRankgingFunctionsToTransitionInvariants(
      Collection<TerminationArgument> pArguments, CFANode pLoopHead, CFAEdge pIncomingLoopEdge) {
    ImmutableList.Builder<String> transitionInvariants = ImmutableList.builder();

    // Ideally, this should be done via AstToCFARelation, however, this breaks due to copying of CFA
    // This is planned in https://gitlab.com/sosy-lab/software/cpachecker/-/work_items/1483
    FileLocation fileLocation = pIncomingLoopEdge.getFileLocation();
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    for (TerminationArgument argument : pArguments) {
      RankingFunction rankingFunction = argument.getRankingFunction();
      if (rankingFunction instanceof NestedRankingFunction pNestedRankingFunction) {
        for (AffineFunction nestedRankingFunction : pNestedRankingFunction.getComponents()) {
          addTransitionInvariant(
              transitionInvariants,
              nestedRankingFunction.toString(),
              nestedRankingFunction.getVariables());
        }
      } else {
        addTransitionInvariant(
            transitionInvariants, rankingFunction.toString(), rankingFunction.getVariables());
      }
    }
    return new InvariantEntry(
        TransitionInvariantUtils.removeFunctionFromVarsName(
            FluentIterable.from(transitionInvariants.build())
                .transform(disjunct -> "(" + disjunct + ")")
                .join(Joiner.on(" || "))),
        InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.EXT_C,
        locationRecord);
  }

  private static void addTransitionInvariant(
      ImmutableList.Builder<String> transitionInvariants,
      String rankingFunction,
      Iterable<IProgramVar> variables) {
    String prevRank =
        rightSideOfRankingFunction(wrapTheVariablesWithAtAnyPrev(rankingFunction, variables));
    String currentRank =
        rightSideOfRankingFunction(wrapTheVariablesWithCastToLongLong(rankingFunction, variables));
    if (prevRank.contains(CParserUtils.CPACHECKER_TMP_PREFIX)) {
      transitionInvariants.add("0");
    } else {
      transitionInvariants.add(prevRank + " > " + currentRank);
    }
  }

  public static Set<TerminationArgument> collectArgumentsForNestedLoops(
      Loop pLoop, Set<Loop> pAllLoops, Multimap<Loop, TerminationArgument> pTerminationArguments) {
    Set<TerminationArgument> argumentsForNestedLoops = new HashSet<>();
    for (Loop loop : pAllLoops) {
      for (CFAEdge innerEdge : loop.getInnerLoopEdges()) {
        if (pLoop.getLoopHeads().contains(innerEdge.getPredecessor())) {
          argumentsForNestedLoops.addAll(pTerminationArguments.get(loop));
        }
      }
    }
    return argumentsForNestedLoops;
  }
}
