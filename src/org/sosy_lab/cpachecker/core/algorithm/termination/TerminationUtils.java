// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Multimap;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.AffineFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.NestedRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class TerminationUtils {

  private static final String PRIMED_VARIABLE_POSTFIX = "__TERMINATION_PRIMED";

  private static final String DEREFERENCE_POSTFIX = "__TERMINATION_DEREFERENCED";

  private TerminationUtils() {}

  public static CVariableDeclaration createPrimedVariable(CVariableDeclaration pVariableDecl) {
    return new CVariableDeclaration(
        FileLocation.DUMMY,
        false,
        CStorageClass.AUTO,
        pVariableDecl.getType(),
        pVariableDecl.getName() + PRIMED_VARIABLE_POSTFIX,
        pVariableDecl.getOrigName() + PRIMED_VARIABLE_POSTFIX,
        pVariableDecl.getQualifiedName() + PRIMED_VARIABLE_POSTFIX,
        null);
  }

  public static CVariableDeclaration createDereferencedVariable(CSimpleDeclaration pVariableDecl) {
    CType type = pVariableDecl.getType();
    if (type instanceof CPointerType cPointerType) {
      CType innerType = cPointerType.getType();
      checkArgument(!(innerType instanceof CVoidType));
      checkArgument(!(innerType instanceof CFunctionType));

      return new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          innerType,
          pVariableDecl.getName() + DEREFERENCE_POSTFIX,
          pVariableDecl.getOrigName() + DEREFERENCE_POSTFIX,
          pVariableDecl.getQualifiedName() + DEREFERENCE_POSTFIX,
          null);

    } else {
      throw new IllegalArgumentException(type + " is not a pointer type");
    }
  }

  private static String rightSideOfRankingFunction(String pRankingFunction) {
    int firstEquals = pRankingFunction.indexOf('=');
    return pRankingFunction.substring(firstEquals + 1).trim();
  }

  // The function replaces variable names with annotation \at(..., AnyPrev), i.e. x -> \at(x,
  // AnyPrev) and casts them into a larger type
  private static String wrapTheVariablesWithAtAnyPrev(
      String pRankingFunction, Iterable<IProgramVar> pVars) {
    for (IProgramVar var : pVars) {
      String newVarName = "((__int128)\\at(" + var + ", AnyPrev))";
      pRankingFunction = pRankingFunction.replace(var.toString(), newVarName);
    }
    return pRankingFunction;
  }

  // The function casts the variables into (__int128) as we want to prevent overflows in the
  // witness
  private static String wrapTheVariablesWithCastToLongLong(
      String pRankingFunction, Iterable<IProgramVar> pVars) {
    for (IProgramVar var : pVars) {
      String newVarName = "((__int128)" + var + ")";
      pRankingFunction = pRankingFunction.replace(var.toString(), newVarName);
    }
    return pRankingFunction;
  }

  public static InvariantEntry processSupportingInvariant(
      SupportingInvariant pSupportingInvariant, CFANode pLoopHead, CFAEdge pIncomingLoopEdge) {
    // Ideally, this should be done via AstToCFARelation, however, this breaks due to copying of CFA
    FileLocation fileLocation = pIncomingLoopEdge.getFileLocation();

    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    String invariant =
        wrapTheVariablesWithCastToLongLong(
            pSupportingInvariant.toString(), pSupportingInvariant.getVariables());
    return new InvariantEntry(
        TransitionInvariantUtils.removeFunctionFromVarsName(invariant),
        InvariantRecordType.LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.C,
        locationRecord);
  }

  public static InvariantEntry processRankingFunction(
      Collection<TerminationArgument> pArguments, CFANode pLoopHead, CFAEdge pIncomingLoopEdge) {
    List<String> transitionInvariants = new ArrayList<>();

    // Ideally, this should be done via AstToCFARelation, however, this breaks due to copying of CFA
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
            String.join(" || ", transitionInvariants)),
        InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.EXT_C,
        locationRecord);
  }

  private static void addTransitionInvariant(
      List<String> transitionInvariants, String rankingFunction, Iterable<IProgramVar> variables) {
    final String TMP_KEYWORD = "__CPAchecker_TMP";
    String prevRank =
        rightSideOfRankingFunction(wrapTheVariablesWithAtAnyPrev(rankingFunction, variables));
    String currentRank =
        rightSideOfRankingFunction(wrapTheVariablesWithCastToLongLong(rankingFunction, variables));
    if (prevRank.contains(TMP_KEYWORD)) {
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
