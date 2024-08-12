// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.mockito.internal.matchers.Null;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

/**
 * Class for patterns defined on the transitions of instrumentation automaton. Should not be used
 * outside the Sequentialization operator !
 * Currently supported patterns:
 * - Arbitrary String = The match is true if the string on CFAEdge equals the pattern string.
 * - [cond], [!cond] = The match is true if the CFAEdge is the edge corresponding to satisfaction/
 * not satisfaction of the assumption.
 * - ADD, SUB = The match is true if the type of AST expression on CFAEdge is of type AdditionExpression
 * or SubstractionExpression
 * - true, false = The match is always true/false
 */
public class InstrumentationPattern {
  private patternType type;
  private String pattern;

  public InstrumentationPattern(String pPattern) {
    pattern = pPattern;
    switch (pPattern) {
      case "true" :
        type = patternType.TRUE;
        break;
      case "false" :
        type = patternType.FALSE;
        break;
      case "[cond]" :
        type = patternType.COND;
        break;
      case "[!cond]" :
        type = patternType.NOT_COND;
        break;
      case "ADD" :
        type = patternType.ADD;
        break;
      case "SUB" :
        type = patternType.SUB;
        break;
      default:
        type = patternType.REGEX;
        break;
    }
  }

  /**
   * Checks if the provided CFAEdge matches the pattern.
   * @return Null, if the edge does not match the pattern, otherwise returns the list of matched variables.
   */
  @Nullable
  public ImmutableList<String> MatchThePattern(CFAEdge pCFAEdge) {
    return switch (type) {
      case TRUE -> ImmutableList.of();
      case COND -> isOriginalCond(pCFAEdge) ? ImmutableList.of() : null;
      case NOT_COND -> isNegatedCond(pCFAEdge) ? ImmutableList.of() : null;
      default -> null;
    };
  }

  private boolean isNegatedCond(CFAEdge pCFAEdge) {
    if (pCFAEdge instanceof CAssumeEdge) {
      return !((CAssumeEdge) pCFAEdge).getTruthAssumption();
    }
    return false;
  }

  private boolean isOriginalCond(CFAEdge pCFAEdge) {
    if (pCFAEdge.getPredecessor().getNumLeavingEdges() == 1) {
      return true;
    }
    if (pCFAEdge instanceof CAssumeEdge) {
      return ((CAssumeEdge) pCFAEdge).getTruthAssumption();
    }
    return false;
  }

  private enum patternType {
    TRUE,
    FALSE,
    COND,
    NOT_COND,
    ADD,
    SUB,
    REGEX
  }
}
