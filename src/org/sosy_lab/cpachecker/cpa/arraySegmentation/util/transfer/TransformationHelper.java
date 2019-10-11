/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation.util.transfer;

import com.google.common.base.Throwables;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedCompletLatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedLocationArrayContentCPA;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.UsageAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TransformationHelper<T extends ExtendedCompletLatticeAbstractState<T>> {

  LogManager logger;

  public TransformationHelper(LogManager pLogger) {
    this.logger = pLogger;
  }

  /**
   *
   * removes all expressions from the segment bound containing the expression pVar
   *
   * @param pVar to be removed
   * @return true, if the segments containing pVar are cleaned, false if any error occurred
   */
  @Nullable
  public ArraySegmentationState<T>
      cleanExprFromSegBounds(AIdExpression pVar, ArraySegmentationState<T> state) {
    state.getSegments().forEach(s -> s.removeExprContainingSubExpr(pVar));
    try {
      state.joinSegmentsWithEmptySegmentBounds();
    } catch (CPAException | InterruptedException e) {
      logger.log(
          Level.SEVERE,
          "An error occured while removing the expression"
              + pVar.toASTString()
              + " from "
              + state.toDOTLabel()
              + Throwables.getStackTraceAsString(e));
      // TODO Enhance error handling
      return null;
    }
    // Remove the '?' at the last segment if present
    if (state.getSegments().isEmpty()) {
      // All segment bounds were removed, report a failure
      logger.log(
          ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL,
          UsageAnalysisTransferRelation.PREFIX
              + "The segmentation has become empty, this is invalid after removing the expression"
              + pVar.toASTString()
              + ". Hence, the error symbol is returned");
      return null;
    }
    state.getSegments().get(state.getSegments().size() - 1).setPotentiallyEmpty(false);
    return state;
  }

}
