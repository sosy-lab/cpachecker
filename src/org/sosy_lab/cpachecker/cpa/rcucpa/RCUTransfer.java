/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.rcucpa;

import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.rcucpa")
public class RCUTransfer extends SingleEdgeTransferRelation{

  @Option(
    name = "readLock",
    secure = true,
    description = "Name of a function responsible for " + "acquiring the RCU read lock")
  private String readLockName = "ldv_rcu_read_lock";

  @Option(
    name = "readUnlock",
    secure = true,
    description = "Name of a function responsible for " + "releasing the RCU read lock")
  private String readUnlockName = "ldv_rcu_read_unlock";

  @Option(
    name = "sync",
    secure = true,
    description = "Name of a function responsible for " + "the handling of a grace period")
  private String sync = "ldv_synchronize_rcu";

  @Option(
    name = "assign",
    secure = true,
    description = "Name of a function responsible for " + "assignment to RCU pointers")
  private String assign = "ldv_rcu_assign_pointer";

  @Option(
    name = "deref",
    secure = true,
    description = "Name of a function responsible for " + "dereferences of RCU pointers")
  private String deref = "ldv_rcu_dereference";

  @Option(
    name = "fictReadLock",
    secure = true,
    description = "Name of a function marking a call " + "to a fictional read lock of RCU pointer")
  private String fictReadLock = "ldv_rlock_rcu";

  @Option(
    name = "fictReadUnlock",
    secure = true,
    description = "Name of a function marking a call "
        + "to a fictional read unlock of RCU pointer")
  private String fictReadUnlock = "ldv_runlock_rcu";

  @Option(
    name = "fictWriteLock",
    secure = true,
    description = "Name of a function marking a call " + "to a fictional write lock of RCU pointer")
  private String fictWriteLock = "ldv_wlock_rcu";

  @Option(
    name = "fictWriteUnlock",
    secure = true,
    description = "Name of a function marking a "
        + "call to a fictional write unlock of RCU pointer")
  private String fictWriteUnlock = "ldv_wunlock_rcu";

  @Option(name = "rcuPointersFile", secure = true, description = "Name of a file containing RCU "
      + "pointers")
  @FileOption(Type.OUTPUT_FILE)
  private Path input = Paths.get("RCUPointers");

  private final LogManager logger;
  private final Set<MemoryLocation> rcuPointers;
  private final RCUStatistics stats;

  RCUTransfer(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    logger = pLogger;
    pConfig.inject(this);
    if (input != null) {
      rcuPointers = parseFile(input);
    } else {
      logger.log(Level.WARNING, "RCU information is not given, analysis is useless");
      rcuPointers = ImmutableSet.of();
    }
    stats = new RCUStatistics();
  }

  private Set<MemoryLocation> parseFile(Path pInput) {
    Set<MemoryLocation> result = new TreeSet<>();
    try (BufferedReader reader = Files.newBufferedReader(pInput, Charset.defaultCharset())) {
      String line;
      Pattern locationPattern = Pattern.compile("^(.*)::(.*)$");

      while ((line = reader.readLine()) != null) {
        Matcher matcher = locationPattern.matcher(line);
        if (matcher.find()) {
          String func = matcher.group(1);
          String id = matcher.group(2);
          result.add(MemoryLocation.valueOf(func, id));
        } else {
          // global identifier
          result.add(MemoryLocation.valueOf(line));
        }
      }
      logger.log(Level.INFO, "Finished reading from file " + input);
    } catch (IOException pE) {
      logger.log(Level.WARNING, pE.getMessage());
    }
    logger.log(Level.INFO, "result contents: " + result);
    return result;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    RCUState result = (RCUState) state;

    switch (cfaEdge.getEdgeType()) {
      case DeclarationEdge:
        result = handleDeclaration(((CDeclarationEdge) cfaEdge).getDeclaration(), result,
                            cfaEdge.getPredecessor().getFunctionName());
        break;
      case StatementEdge:
        CStatement statement = ((CStatementEdge) cfaEdge).getStatement();
        if (statement instanceof CExpressionAssignmentStatement) {
          result = handleAssignmentStatement((CExpressionAssignmentStatement) statement, result,
                            cfaEdge.getPredecessor().getFunctionName());
        } else if (statement instanceof CFunctionCallAssignmentStatement) {
          result = handleFunctionCallAssignmentStatement((CFunctionCallAssignmentStatement)
              statement, result, cfaEdge.getPredecessor().getFunctionName());
        } else if (statement instanceof CFunctionCallStatement){
          result = handleFunctionCallStatement(((CFunctionCallStatement) statement).getFunctionCallExpression(),
              result, cfaEdge.getPredecessor().getFunctionName());
        }
        break;
      case FunctionCallEdge:
        CFunctionCallExpression callExpression =
            ((CFunctionCallEdge) cfaEdge).getSummaryEdge().getExpression().getFunctionCallExpression();
        List<CExpression> args = ((CFunctionCallEdge) cfaEdge).getArguments();
        List<CParameterDeclaration> params =
            ((CFunctionCallEdge) cfaEdge).getSuccessor().getFunctionParameters();
        String callerName = cfaEdge.getPredecessor().getFunctionName();
        result =
            assignFunctionArguments(
                result,
                args,
                params,
                callerName,
                cfaEdge.getSuccessor().getFunctionName());
        result = handleFunctionCallStatement(callExpression, result, callerName);
        break;
      case FunctionReturnEdge:
      case ReturnStatementEdge:
      case CallToReturnEdge:
      case AssumeEdge:
      case BlankEdge:
        break;
      default:
        throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

    return ImmutableSet.of(result);
  }

  private RCUState assignFunctionArguments(
      RCUState state,
      List<CExpression> pArgs,
      List<CParameterDeclaration> pParams,
      String callerName,
      String functionName) {

    RCUState result = state;
    IdentifierCreator callerCreator = new IdentifierCreator(callerName);
    IdentifierCreator innerCreator = new IdentifierCreator(functionName);

    // Iterate over parameters to avoid f(..)
    for (int i = 0; i < pParams.size(); i++) {
      CExpression arg = pArgs.get(i);
      CParameterDeclaration param = pParams.get(i);

      AbstractIdentifier argId = arg.accept(callerCreator);
      AbstractIdentifier paramId = innerCreator.createIdentifier(param, 0);

      if (argId.isPointer()) {
        result = handleAssignment(paramId, argId, result, false);
      }
    }

    return result;
  }

  private RCUState handleAssignment(CExpression left, CExpression right,
                                    String functionName,
                                    RCUState state,
      boolean rcuAssign) {

    IdentifierCreator localIc = new IdentifierCreator(functionName);

    AbstractIdentifier rcuPtr = left.accept(localIc);
    AbstractIdentifier ptr = right.accept(localIc);

    if (rcuPtr.isPointer() || ptr.isPointer()) {
      // a = null is also possible
      return handleAssignment(rcuPtr, ptr, state, rcuAssign);
    }
    return state;
  }

  private RCUState handleAssignment(
      AbstractIdentifier left,
      AbstractIdentifier right,
      RCUState state,
      boolean rcuAssign) {

    RCUState result = state;

    MemoryLocation rcuLoc = LocationIdentifierConverter.toLocation(left);
    MemoryLocation loc = LocationIdentifierConverter.toLocation(right);

    if ((rcuLoc != null && rcuPointers.contains(rcuLoc))
        || (loc != null && rcuPointers.contains(loc))) {

      AbstractIdentifier nonTemporaryId = result.getNonTemporaryId(left);
      AbstractIdentifier leftPtr = left;
      if (nonTemporaryId != null) {
        leftPtr = nonTemporaryId;
      }
      nonTemporaryId = result.getNonTemporaryId(left);
      AbstractIdentifier rightPtr = right;
      if (nonTemporaryId != null) {
        rightPtr = nonTemporaryId;
      }

      result = addTmpMappingIfNecessary(left, right, result);
      result = addTmpMappingIfNecessary(right, left, result);

      result = result.addToRelations(leftPtr, rightPtr);
      if (rcuAssign) {
        result = result.addToOutdated(leftPtr);
        result = result.addToRelations(rightPtr, leftPtr);
      }
      if (!leftPtr.equals(left)) {
        result = result.addToRelations(left, rightPtr);
      }

    }
    return result;
  }

  private RCUState addTmpMappingIfNecessary(
      AbstractIdentifier tmpVar,
      AbstractIdentifier nonTmpVar,
      RCUState pResult) {
    if (tmpVar instanceof SingleIdentifier &&
        ((SingleIdentifier) tmpVar).getName().contains("CPAchecker_TMP")) {
      pResult = pResult.addTmpMapping(tmpVar, nonTmpVar);
    }
    return pResult;
  }

  private RCUState handleFunctionCallStatement(CFunctionCallExpression pCallExpression,
                                               RCUState state,
                                               String pFunctionName) {
    CFunctionDeclaration fd = pCallExpression.getDeclaration();
    RCUState result = state;

    if (fd != null) {
      String fName = fd.getName();

      if (fName.contains(readLockName)) {
        result = result.incRCURead();
      } else if (fName.contains(readUnlockName)) {
        result = result.decRCURead();
      } else if (fName.equals(fictReadLock)) {
        result = result.markRead();
      } else if (fName.equals(fictWriteLock)) {
        result = result.markWrite();
      } else if (fName.equals(fictReadUnlock) || fName.equals(fictWriteUnlock)) {
        result = result.clearLock();
      } else if (fName.equals(sync)) {
        stats.synchNum.inc();
        result = result.fillLocal();
      } else if (fName.equals(assign)) {
        stats.assignNum.inc();
        CExpression rcuPtr = pCallExpression.getParameterExpressions().get(0);
        CExpression ptr = pCallExpression.getParameterExpressions().get(1);

        result = handleAssignment(rcuPtr, ptr, pFunctionName, state, true);

      }
    }
    return result;
  }

  private RCUState handleFunctionCallAssignmentStatement(CFunctionCallAssignmentStatement assignment,
                                                         RCUState pResult,
                                                         String functionName) {
    // This case is covered by the normal assignment expression
    CFunctionDeclaration functionDeclaration = assignment.getFunctionCallExpression().getDeclaration();
    if (functionDeclaration != null && functionDeclaration.getName().equals(deref)) {
      stats.derefNum.inc();
      return handleAssignment(assignment.getLeftHandSide(), assignment.getFunctionCallExpression()
          .getParameterExpressions()
          .get(0), functionName, pResult, false);
    }
    return pResult;
  }

  private RCUState handleAssignmentStatement(CExpressionAssignmentStatement assignment,
                                         RCUState pResult,
                                         String functionName) {
    CLeftHandSide leftHandSide = assignment.getLeftHandSide();
    CExpression rightHandSide = assignment.getRightHandSide();

    return handleAssignment(leftHandSide, rightHandSide, functionName, pResult, false);
  }

  private RCUState handleDeclaration(CDeclaration pDeclaration,
                                 RCUState pResult,
                                 String pFunctionName) {

    if (pDeclaration != null && pDeclaration instanceof CVariableDeclaration) {
      IdentifierCreator localIc = new IdentifierCreator(pFunctionName);
      CVariableDeclaration var = (CVariableDeclaration) pDeclaration;
      AbstractIdentifier ail = localIc.createIdentifier(var, 0);

      if (ail != null && ail.isPointer()) {
        if (rcuPointers.contains(LocationIdentifierConverter.toLocation(ail))) {
          CInitializer initializer = ((CVariableDeclaration) pDeclaration).getInitializer();
          if (initializer != null && initializer instanceof CInitializerExpression) {
            AbstractIdentifier init =
                ((CInitializerExpression) initializer).getExpression().accept(localIc);
            pResult = addTmpMappingIfNecessary(ail, init, pResult);
            pResult = pResult.addToRelations(ail, init);
          }
        }
      }
    }
    return pResult;
  }

  RCUStatistics getStatistics() {
    return stats;
  }
}
