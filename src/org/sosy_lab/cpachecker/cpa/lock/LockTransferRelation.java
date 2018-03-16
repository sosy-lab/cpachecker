/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.lock;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.lock.effects.AbstractLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.AcquireLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.CheckLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ReleaseLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ResetLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.RestoreAllLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.RestoreLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.SaveStateLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.SetLockEffect;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class LockTransferRelation extends SingleEdgeTransferRelation {

  public static class LockStatistics implements Statistics {

    private final StatTimer transferTimer = new StatTimer("Time for transfer");
    private final StatInt lockEffects = new StatInt(StatKind.SUM, "Number of effects");
    private final StatInt locksInState = new StatInt(StatKind.AVG, "Number of locks in state");
    private final StatInt locksInStateWithLocks =
        new StatInt(StatKind.AVG, "Number of locks in state with locks");

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter.writingStatisticsTo(pOut)
          .put(transferTimer)
          .put(lockEffects)
          .put(locksInState)
          .put(locksInStateWithLocks);
    }

    @Override
    public @Nullable String getName() {
      return "LockCPA";
    }
  }

  private final Map<String, AnnotationInfo> annotatedFunctions;

  private final LockInfo lockDescription;
  private final LogManager logger;
  private final LockStatistics stats;

  public LockTransferRelation(Configuration config, LogManager logger)
      throws InvalidConfigurationException {
    this.logger = logger;

    ConfigurationParser parser = new ConfigurationParser(config);

    lockDescription = parser.parseLockInfo();
    annotatedFunctions = parser.parseAnnotatedFunctions();
    assert (annotatedFunctions != null);

    stats = new LockStatistics();
  }

  @Override
  public Collection<AbstractLockState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision pPrecision, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {

    AbstractLockState lockStatisticsElement = (AbstractLockState) element;

    stats.transferTimer.start();

    // First, determine operations with locks
    List<AbstractLockEffect> toProcess = determineOperations(cfaEdge);
    stats.lockEffects.setNextValue(toProcess.size());
    final AbstractLockStateBuilder builder = lockStatisticsElement.builder();

    toProcess.forEach(e -> e.effect(builder));
    AbstractLockState successor = builder.build();

    stats.transferTimer.stop();

    if (successor != null) {
      int locks = successor.getSize();
      stats.locksInState.setNextValue(locks);
      if (locks > 0) {
        stats.locksInStateWithLocks.setNextValue(locks);
      }
      return Collections.singleton(successor);
    } else {
      return Collections.emptySet();
    }
  }

  public Set<LockIdentifier> getAffectedLocks(CFAEdge cfaEdge) {
    return getLockEffects(cfaEdge).transform(LockEffect::getAffectedLock).toSet();
  }

  private FluentIterable<LockEffect> getLockEffects(CFAEdge cfaEdge) {
    try {
      return from(determineOperations(cfaEdge)).filter(LockEffect.class);
    } catch (UnrecognizedCCodeException e) {
      logger.log(Level.WARNING, "The code " + cfaEdge + " is not recognized");
      return FluentIterable.of();
    }
  }

  private List<AbstractLockEffect> determineOperations(CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {

    switch (cfaEdge.getEdgeType()) {
      case FunctionCallEdge:
        return handleFunctionCall((CFunctionCallEdge) cfaEdge);

      case FunctionReturnEdge:
        return handleFunctionReturnEdge((CFunctionReturnEdge) cfaEdge);

      case StatementEdge:
        CStatement statement = ((CStatementEdge) cfaEdge).getStatement();
        return handleStatement(statement);
      case AssumeEdge:
        return handleAssumption((CAssumeEdge) cfaEdge);

      case BlankEdge:
      case ReturnStatementEdge:
      case DeclarationEdge:
      case CallToReturnEdge:
        break;

      default:
        throw new UnrecognizedCCodeException("Unknown edge type", cfaEdge);
    }
    return Collections.emptyList();
  }

  private List<AbstractLockEffect> handleAssumption(CAssumeEdge cfaEdge) {
    CExpression assumption = cfaEdge.getExpression();

    if (assumption instanceof CBinaryExpression) {
      CBinaryExpression binExpression = (CBinaryExpression) assumption;
      if (binExpression.getOperand1() instanceof CIdExpression) {
        String varName = ((CIdExpression) ((CBinaryExpression) assumption).getOperand1()).getName();
        if (lockDescription.getVariableEffectDescription().containsKey(varName)) {
          CExpression val = binExpression.getOperand2();
          if (val instanceof CIntegerLiteralExpression) {
            LockIdentifier id = lockDescription.getVariableEffectDescription().get(varName);
            int level = ((CIntegerLiteralExpression) val).getValue().intValue();
            AbstractLockEffect e =
                CheckLockEffect.createEffectForId(level, cfaEdge.getTruthAssumption(), id);
            return Collections.singletonList(e);
          }
        }
      }
    }
    return Collections.emptyList();
  }

  private ImmutableList<? extends AbstractLockEffect> convertAnnotationToLockEffect(
      Set<LockIdentifier> pAnnotatedIds, LockEffect pEffect) {
    return from(pAnnotatedIds).transform(pEffect::cloneWithTarget).toList();
  }

  private List<AbstractLockEffect> handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge) {
    // CFANode tmpNode = cfaEdge.getSummaryEdge().getPredecessor();
    String fName =
        cfaEdge
            .getSummaryEdge()
            .getExpression()
            .getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString();
    if (annotatedFunctions.containsKey(fName)) {
      List<AbstractLockEffect> result = new ArrayList<>();

      AnnotationInfo currentAnnotation = annotatedFunctions.get(fName);
      if (currentAnnotation.getRestoreLocks().size() == 0
          && currentAnnotation.getFreeLocks().size() == 0
          && currentAnnotation.getResetLocks().size() == 0
          && currentAnnotation.getCaptureLocks().size() == 0) {
        // Not specified annotations are considered to be totally restoring
        AbstractLockEffect restoreAll = RestoreAllLockEffect.getInstance();
        return Collections.singletonList(restoreAll);
      } else {
        if (currentAnnotation.getRestoreLocks().size() > 0) {
          result.addAll(
              convertAnnotationToLockEffect(
                  currentAnnotation.getRestoreLocks(), RestoreLockEffect.getInstance()));
        }
        if (currentAnnotation.getFreeLocks().size() > 0) {
          result.addAll(
              convertAnnotationToLockEffect(
                  currentAnnotation.getFreeLocks(), ReleaseLockEffect.getInstance()));
        }
        if (currentAnnotation.getResetLocks().size() > 0) {
          result.addAll(
              convertAnnotationToLockEffect(
                  currentAnnotation.getResetLocks(), ResetLockEffect.getInstance()));
        }
        if (currentAnnotation.getCaptureLocks().size() > 0) {
          for (LockIdentifier targetId : currentAnnotation.getCaptureLocks()) {
            result.add(
                AcquireLockEffect.createEffectForId(
                    targetId, lockDescription.getMaxLevel(targetId.getName())));
          }
        }
        return result;
      }
    }
    return Collections.emptyList();
  }

  private List<AbstractLockEffect> handleFunctionCallExpression(CFunctionCallExpression function) {
    String functionName = function.getFunctionNameExpression().toASTString();
    if (!lockDescription.getFunctionEffectDescription().containsKey(functionName)) {
      return Collections.emptyList();
    }
    Pair<LockEffect, LockIdUnprepared> locksWithEffect =
        lockDescription.getFunctionEffectDescription().get(functionName);
    LockEffect effect = locksWithEffect.getFirst();
    LockIdUnprepared uId = locksWithEffect.getSecond();

    List<AbstractLockEffect> result = new ArrayList<>();

    if (effect == SetLockEffect.getInstance()) {
      CExpression expression = function.getParameterExpressions().get(0);
      // Replace it by parametrical one
      if (expression instanceof CIntegerLiteralExpression) {
        int newValue = ((CIntegerLiteralExpression) expression).getValue().intValue();
        int max = lockDescription.getMaxLevel(uId.getName());
        if (max < newValue) {
          newValue = max;
        }
        effect = SetLockEffect.createEffectForId(newValue, uId.apply(null));
      } else {
        // We can not process not integers
        return result;
      }
    }

    LockIdentifier id = uId.apply(function.getParameterExpressions());
    if (effect == AcquireLockEffect.getInstance()) {
      effect = AcquireLockEffect.createEffectForId(id, lockDescription.getMaxLevel(uId.getName()));
    } else {
      effect = effect.cloneWithTarget(id);
    }
    result.add(effect);
    return result;
  }

  private List<AbstractLockEffect> handleStatement(CStatement statement) {

    if (statement instanceof CAssignment) {
      /*
       * level = intLock();
       */
      CRightHandSide op2 = ((CAssignment) statement).getRightHandSide();

      if (op2 instanceof CFunctionCallExpression) {
        CFunctionCallExpression function = (CFunctionCallExpression) op2;
        return handleFunctionCallExpression(function);
      } else {
        /*
         * threadDispatchLevel = 1;
         */
        CLeftHandSide leftSide = ((CAssignment) statement).getLeftHandSide();
        CRightHandSide rightSide = ((CAssignment) statement).getRightHandSide();
        String varName = leftSide.toASTString();
        if (lockDescription.getVariableEffectDescription().containsKey(varName)) {
          if (rightSide instanceof CIntegerLiteralExpression) {
            LockIdentifier id = lockDescription.getVariableEffectDescription().get(varName);
            int level = ((CIntegerLiteralExpression) rightSide).getValue().intValue();
            AbstractLockEffect e = SetLockEffect.createEffectForId(level, id);
            return Collections.singletonList(e);
          }
        }
      }

    } else if (statement instanceof CFunctionCallStatement) {
      /*
       * queLock(que);
       */
      CFunctionCallStatement funcStatement = (CFunctionCallStatement) statement;
      return handleFunctionCallExpression(funcStatement.getFunctionCallExpression());
    }
    // No lock-relating operations
    return Collections.emptyList();
  }

  private List<AbstractLockEffect> handleFunctionCall(CFunctionCallEdge callEdge) {
    List<AbstractLockEffect> result = new ArrayList<>();
    if (annotatedFunctions.containsKey(callEdge.getSuccessor().getFunctionName())) {
      AbstractLockEffect saveState = SaveStateLockEffect.getInstance();
      result.add(saveState);
    }
    result.addAll(
        handleFunctionCallExpression(
            callEdge.getSummaryEdge().getExpression().getFunctionCallExpression()));
    return result;
  }

  /**
   * Used in UsageStatisticsCPAStatistics In true case the current line should be highlighted in the
   * final report
   *
   * @param pEdge edge to check
   * @return the verdict
   */
  public String doesChangeTheState(CFAEdge pEdge) {
    return Joiner.on(",").join(getLockEffects(pEdge));
  }

  public Statistics getStatistics() {
    return stats;
  }
}
