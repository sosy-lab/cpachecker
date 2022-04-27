// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.ArithmeticOverflowAssumptionBuilder;

/** Transfer relation for the generic assumption generator. */
public class GenericAssumptionsTransferRelation extends SingleEdgeTransferRelation {

  /**
   * List of interfaces used to build the default assumptions made by the model checker for program
   * operations.
   *
   * <p>Modify this to register new kind of assumptions.
   */
  private final List<GenericAssumptionBuilder> assumptionBuilders;

  public GenericAssumptionsTransferRelation(
      CFA pCFA, LogManager logger, Configuration pConfiguration)
      throws InvalidConfigurationException {
    assumptionBuilders =
        ImmutableList.of(new ArithmeticOverflowAssumptionBuilder(pCFA, logger, pConfiguration));
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState el, Precision p, CFAEdge edge) throws CPATransferException {

    List<CExpression> allAssumptions = new ArrayList<>();
    for (GenericAssumptionBuilder b : assumptionBuilders) {
      allAssumptions.addAll(b.assumptionsForEdge(edge));
    }

    return ImmutableSet.of(new GenericAssumptionsState(allAssumptions));
  }
}
