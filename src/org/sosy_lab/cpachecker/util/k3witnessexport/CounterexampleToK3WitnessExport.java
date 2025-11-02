// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.k3witnessexport;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ChoiceStep;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Command;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Declaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3HavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IncorrectTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3LocalVariablesStep;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SelectTraceCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Trace;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TraceEntryCall;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TraceSetGlobalVariable;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TraceStep;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ViolatedProperty;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.k3.K3BlankChoiceEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3CfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.k3.K3DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3ProcedureCallEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3StatementEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.SingleConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;

public class CounterexampleToK3WitnessExport {
  @SuppressWarnings("unused")
  private final LogManager logger;

  private final K3CfaMetadata k3Metadata;

  public CounterexampleToK3WitnessExport(LogManager pLogger, CFA pCFA) {
    Verify.verify(
        pCFA.getMetadata().getK3CfaMetadata().isPresent(),
        "K3 metadata must be present in CFA in order to export a K3 witness.");
    k3Metadata = pCFA.getMetadata().getK3CfaMetadata().orElseThrow();
    logger = pLogger;
  }

  private K3ViolatedProperty getViolatedProperty(CFAEdge pEdge) {
    CFANode nodeWithViolatedTag = pEdge.getPredecessor();
    Set<K3TagProperty> setOfPropertiesAtLeastOneViolated =
        k3Metadata.tagAnnotations().get(nodeWithViolatedTag);
    Set<K3TagProperty> violatedTags;
    // Find the actual violated tags.
    // TODO: Currently we do not handle the case where more than one tag is present.
    if (setOfPropertiesAtLeastOneViolated.size() != 1) {
      throw new UnsupportedOperationException(
          "K3 witness export currently only supports a single violated property tag at a CFANode.");
    } else {
      violatedTags = setOfPropertiesAtLeastOneViolated;
    }

    Set<K3TagReference> tagReferences = k3Metadata.tagReferences().get(nodeWithViolatedTag);
    if (tagReferences.size() == 1) {
      // If we know exactly which tag was violated, include it in the property.
      K3TagReference tagRef = tagReferences.iterator().next();
      return new K3IncorrectTagProperty(tagRef.getFileLocation(), tagRef, violatedTags);
    } else if (tagReferences.isEmpty()) {
      // Otherwise, just return the violated tags without a specific reference.
      return new K3IncorrectTagProperty(FileLocation.DUMMY, violatedTags);
    } else {
      // TODO: There can be multiple tag references, and only one of them could have the actual
      //  property, so we need to be able to identify the correct one.
      throw new UnsupportedOperationException(
          "K3 witness export currently only supports a single violated property tag reference at a"
              + " CFANode.");
    }
  }

  private K3ConstantTerm getValueOrDefault(
      ConcreteState pState, String pVarName, @Nullable String pProcedureName, K3Type pType) {
    // Variable declarations can only be global
    IDExpression idExpr = new IDExpression(pVarName, pProcedureName);
    if (pState.hasValueForLeftHandSide(idExpr)) {
      // Transform the value into a K3ConstantTerm
      Object value = pState.getVariableValue(idExpr);
      return K3ConstantTerm.of(value, pType);
    } else {
      // Return a default value for the variable type
      // not all variables have necessarily been added to the SMT
      // solver (e.g. unused variables), so we need to return
      // a default value here.
      return pType.defaultValue();
    }
  }

  private K3LocalVariablesStep setLocalVariablesForFunctionCall(
      ConcreteState pConcreteState, K3ProcedureDeclaration pProcedureDeclaration) {
    ImmutableMap.Builder<K3IdTerm, K3ConstantTerm> functionCallAssignmentsBuilder =
        ImmutableMap.builder();
    for (K3ParameterDeclaration paramDecl :
        FluentIterable.concat(
            pProcedureDeclaration.getParameters(),
            pProcedureDeclaration.getLocalVariables(),
            pProcedureDeclaration.getReturnValues())) {

      K3ConstantTerm assignedValue =
          getValueOrDefault(
              pConcreteState,
              paramDecl.getName(),
              paramDecl.getProcedureName(),
              paramDecl.getType());

      K3IdTerm idTerm = new K3IdTerm(paramDecl, FileLocation.DUMMY);
      functionCallAssignmentsBuilder.put(idTerm, assignedValue);
    }
    return new K3LocalVariablesStep(functionCallAssignmentsBuilder.build(), FileLocation.DUMMY);
  }

  private K3TraceSetGlobalVariable handleGlobalVariableAssignment(
      ConcreteState pConcreteState, K3Declaration pDeclaration) {
    if (!(pDeclaration instanceof K3VariableDeclaration pVarDecl)) {
      throw new IllegalStateException(
          "Expected only variable or constant declarations in the global declaration phase.");
    }

    K3ConstantTerm assignedValue =
        getValueOrDefault(pConcreteState, pVarDecl.getName(), null, pVarDecl.getType());

    return new K3TraceSetGlobalVariable(
        new K3IdTerm(pVarDecl, FileLocation.DUMMY), assignedValue, FileLocation.DUMMY);
  }

  private K3TraceEntryCall handleTraceEntryCall(
      ConcreteState pConcreteState, K3ProcedureCallEdge pCallEdge) {
    K3ProcedureCallStatement procedureCallStatement = pCallEdge.getFunctionCall();

    // Now build the argument values
    ImmutableList.Builder<K3ConstantTerm> argumentValuesBuilder = ImmutableList.builder();
    for (K3Term argumentExpr : procedureCallStatement.getParameterExpressions()) {
      if (argumentExpr instanceof K3IdTerm pIdTerm
          && pIdTerm.getDeclaration() instanceof K3VariableDeclaration varDecl) {
        K3ConstantTerm argValue =
            getValueOrDefault(pConcreteState, varDecl.getName(), null, varDecl.getType());
        argumentValuesBuilder.add(argValue);
      } else if (argumentExpr instanceof K3ConstantTerm pConstTerm) {
        argumentValuesBuilder.add(pConstTerm);
      } else {
        // More complex expressions are currently not supported.
        // They would require expression evaluation here.
        throw new UnsupportedOperationException(
            "Only ID terms and constant terms are supported as procedure call arguments in K3"
                + " witness export.");
      }
    }

    return new K3TraceEntryCall(
        procedureCallStatement.getProcedureDeclaration(),
        argumentValuesBuilder.build(),
        pCallEdge.getFileLocation());
  }

  public List<K3Command> generateWitnessCommands(CounterexampleInfo pCounterexample) {
    ConcreteStatePath concretePath =
        pCounterexample.getCFAPathWithAssignments().getConcreteStatePath().orElseThrow();
    ImmutableList.Builder<K3TraceSetGlobalVariable> globalVariableAssignmentBuilder =
        ImmutableList.builder();
    @Nullable K3TraceEntryCall entryCall = null;
    ImmutableList.Builder<K3TraceStep> stepsBuilder = ImmutableList.builder();
    @Nullable K3ViolatedProperty violatedProperty = null;

    boolean inGlobalDeclarationPhase = true;

    int stateIndex = 0;
    for (ConcreteStatePathNode state : concretePath) {
      stateIndex++;

      if (!(state instanceof SingleConcreteState pSingleConcreteState)) {
        // Currently this is guaranteed by the type system, so the check is redundant.
        // However, in the future we might have other implementations of ConcreteStatePathNode,
        // so we keep this check here for safety.
        throw new UnsupportedOperationException(
            "Only single concrete states are supported in K3 witness export.");
      }

      ConcreteState concreteState = pSingleConcreteState.getConcreteState();
      CFAEdge edge = pSingleConcreteState.getCfaEdge();

      if (stateIndex == concretePath.size()) {
        // If we get to the last state in the path, we need to handle the violated property.
        violatedProperty = getViolatedProperty(edge);
      } else if (edge instanceof BlankEdge && (!(edge instanceof K3BlankChoiceEdge))) {
        // Blank edges do not contribute to the witness trace.
        continue;
      } else if (inGlobalDeclarationPhase && edge instanceof K3ProcedureCallEdge pCallEdge) {
        // The first procedure call edge is the entry call.
        inGlobalDeclarationPhase = false;
        entryCall = handleTraceEntryCall(concreteState, pCallEdge);
        stepsBuilder.add(
            setLocalVariablesForFunctionCall(
                concreteState, pCallEdge.getFunctionCall().getProcedureDeclaration()));
      } else if (inGlobalDeclarationPhase) {
        // Handle all the global declarations
        if (!(edge instanceof K3DeclarationEdge pDeclarationEdge)) {
          throw new IllegalStateException(
              "Expected global declaration edges in the global declaration phase of K3 witness"
                  + " export.");
        }

        globalVariableAssignmentBuilder.add(
            handleGlobalVariableAssignment(concreteState, pDeclarationEdge.getDeclaration()));

      } else if (edge instanceof K3ProcedureCallEdge pCallEdge) {
        // Initialize all the local variables of the called procedure
        stepsBuilder.add(
            setLocalVariablesForFunctionCall(
                concreteState, pCallEdge.getFunctionCall().getProcedureDeclaration()));
      } else if (edge instanceof K3StatementEdge pStatementEdge
          && pStatementEdge.getStatement() instanceof K3HavocStatement pHavoc) {
        // Handle havoc statements as trace steps
        throw new UnsupportedOperationException(
            "Havoc statements are not yet supported in K3 witness export.");
      } else if (edge instanceof K3BlankChoiceEdge pChoiceEdge) {
        // Handle choice statements as trace steps
        stepsBuilder.add(new K3ChoiceStep(pChoiceEdge.getChoiceIndex(), FileLocation.DUMMY));
      }

      // TODO: Handle leap commands, once CPAchecker can verify programs by using abstractions.
      // In all other cases, we do not need to do anything special for the witness export.
    }

    Verify.verify(entryCall != null, "Entry call must be set in K3 witness export.");
    Verify.verify(violatedProperty != null, "Violated property must be set in K3 witness export.");

    K3Trace trace =
        new K3Trace(
            globalVariableAssignmentBuilder.build(),
            entryCall,
            stepsBuilder.build(),
            violatedProperty,
            // TODO: Currently we do not handle anything other than safety properties,
            //  so no tags are necessary.
            ImmutableList.of(),
            FileLocation.DUMMY);

    return ImmutableList.of(new K3SelectTraceCommand(trace, FileLocation.DUMMY));
  }
}
