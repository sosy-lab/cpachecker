// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class MissingInformation {

  /**
   * This field stores the Expression of the Memory Location that
   * could not be evaluated.
   */
  private final CExpression missingCLeftMemoryLocation;

  /**
   *  This expression stores the Memory Location
   *  to be assigned.
   */
  private final MemoryLocation cLeftMemoryLocation;

  /**
   * Expression could not be evaluated due to missing information. (e.g.
   * missing pointer alias).
   */
  private final CExpression missingCExpressionInformation;

  /**
   * Expression could not be evaluated due to missing information. (e.g.
   * missing pointer alias).
   */
  private final Value cExpressionValue;

  /**
   * The truth Assumption made in this assume edge.
   */
  private final Boolean truthAssumption;

  private CFunctionCallExpression missingFreeInvocation = null;

  @SuppressWarnings("unused")
  public MissingInformation(
      CExpression pMissingCLeftMemoryLocation, CExpression pMissingCExpressionInformation) {
    missingCExpressionInformation = pMissingCExpressionInformation;
    missingCLeftMemoryLocation = pMissingCLeftMemoryLocation;
    cExpressionValue = null;
    cLeftMemoryLocation = null;
    truthAssumption = null;
  }

  //TODO Better checks...don't be lazy, just because class
  // will likely change.

  public boolean hasUnknownValue() {
    return missingCExpressionInformation != null;
  }

  public boolean hasKnownValue() {
    return cExpressionValue != null;
  }

  public boolean hasUnknownMemoryLocation() {
    return missingCLeftMemoryLocation != null;
  }

  public boolean hasKnownMemoryLocation() {
    return cLeftMemoryLocation != null;
  }

  public boolean isMissingAssignment() {
    // TODO Better Name for this method.
    // Checks if a variable needs to be assigned a value,
    // but to evaluate the MemoryLocation, or the value,
    // we lack information.

    return (missingCExpressionInformation != null || missingCLeftMemoryLocation != null)
        && truthAssumption == null;
  }

  public boolean isMissingAssumption() {
    return truthAssumption != null && missingCExpressionInformation != null;
  }

  public MissingInformation(CExpression pMissingCLeftMemoryLocation, Value pCExpressionValue) {
    missingCExpressionInformation = null;
    missingCLeftMemoryLocation = pMissingCLeftMemoryLocation;
    cExpressionValue = pCExpressionValue;
    cLeftMemoryLocation = null;
    truthAssumption = null;
  }

  public MissingInformation(
      MemoryLocation pCLeftMemoryLocation, CExpression pMissingCExpressionInformation) {
    missingCExpressionInformation = pMissingCExpressionInformation;
    missingCLeftMemoryLocation = null;
    cExpressionValue = null;
    cLeftMemoryLocation = pCLeftMemoryLocation;
    truthAssumption = null;
  }

  public MissingInformation(
      AExpression pMissingCLeftMemoryLocation, ARightHandSide pMissingCExpressionInformation) {
    // This constructor casts to CExpression, just to have as few
    // as possible pieces of code for communication cluttering
    // up the transfer relation.
    // Especially, since this class will later be used to
    // communicate missing Information independent of language

    missingCExpressionInformation = (CExpression) pMissingCExpressionInformation;
    missingCLeftMemoryLocation = (CExpression) pMissingCLeftMemoryLocation;
    cExpressionValue = null;
    cLeftMemoryLocation = null;
    truthAssumption = null;
  }

  public MissingInformation(
      Boolean pTruthAssumption, ARightHandSide pMissingCExpressionInformation) {
    // This constructor casts to CExpression, just to have as few
    // as possible pieces of code for communication cluttering
    // up the transfer relation.
    // Especially, since this class will later be used to
    // communicate missing Information independent of language

    missingCExpressionInformation = (CExpression) pMissingCExpressionInformation;
    missingCLeftMemoryLocation = null;
    cExpressionValue = null;
    cLeftMemoryLocation = null;
    truthAssumption = pTruthAssumption;
  }

  public MissingInformation(CFunctionCallExpression pFunctionCallExpression) {
    missingFreeInvocation = pFunctionCallExpression;
    missingCExpressionInformation = null;
    missingCLeftMemoryLocation = null;
    cExpressionValue = null;
    cLeftMemoryLocation = null;
    truthAssumption = null;
  }

  public boolean isFreeInvocation() {
    return missingFreeInvocation != null;
  }

  public Value getcExpressionValue() {
    checkNotNull(cExpressionValue);
    return cExpressionValue;
  }

  public MemoryLocation getcLeftMemoryLocation() {
    checkNotNull(cLeftMemoryLocation);
    return cLeftMemoryLocation;
  }

  @SuppressWarnings("unused")
  public CExpression getMissingCExpressionInformation() {
    checkNotNull(missingCExpressionInformation);
    return missingCExpressionInformation;
  }

  @SuppressWarnings("unused")
  public CExpression getMissingCLeftMemoryLocation() {
    checkNotNull(missingCLeftMemoryLocation);
    return missingCLeftMemoryLocation;
  }

  @SuppressWarnings("unused")
  public Boolean getTruthAssumption() {
    checkNotNull(truthAssumption);
    return truthAssumption;
  }

  public CFunctionCallExpression getMissingFreeInvocation() {
    return missingFreeInvocation;
  }
}
