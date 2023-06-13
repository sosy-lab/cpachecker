// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.translators;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public class SignRequirementsTranslator extends CartesianRequirementsTranslator<SignState> {

  public SignRequirementsTranslator(final LogManager pLog) {
    super(SignState.class, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(final SignState pRequirement) {
    return new ArrayList<>(pRequirement.getSignMapView().keySet());
  }

  @Override
  protected List<String> getListOfIndependentRequirements(
      final SignState pRequirement,
      final SSAMap pIndices,
      final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (Map.Entry<String, SIGN> entry : pRequirement.getSignMapView().entrySet()) {
      String var = entry.getKey();
      if (pRequiredVars == null || pRequiredVars.contains(var)) {
        list.add(getRequirement(getVarWithIndex(var, pIndices), entry.getValue()));
      }
    }
    return list;
  }

  private String getRequirement(final String var, final SIGN sign) {
    StringBuilder sb = new StringBuilder();
    Preconditions.checkArgument(sign != SIGN.EMPTY);
    Preconditions.checkArgument(sign != SIGN.ALL);

    switch (sign) {
      case PLUS:
        sb.append("(> ");
        sb.append(var);
        sb.append(" 0)");
        break;
      case MINUS:
        sb.append("(< ");
        sb.append(var);
        sb.append(" 0)");
        break;
      case ZERO:
        sb.append("(= ");
        sb.append(var);
        sb.append(" 0)");
        break;
      case PLUSMINUS:
        sb.append("(or (> ");
        sb.append(var);
        sb.append(" 0) (< ");
        sb.append(var);
        sb.append(" 0))");
        break;
      case PLUS0:
        sb.append("(>= ");
        sb.append(var);
        sb.append(" 0)");
        break;
      case MINUS0:
        sb.append("(<= ");
        sb.append(var);
        sb.append(" 0)");
        break;
      default:
        throw new AssertionError("should never happen");
    }

    return sb.toString();
  }
}
