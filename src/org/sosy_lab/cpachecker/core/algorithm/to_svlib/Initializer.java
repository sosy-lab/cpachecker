// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibCurrentScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibVariableDeclarationCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;

public class Initializer {

  // TODO change to option! Dont forget prefix!
  // @Option(secure = true, description = "Use SV-COMP semantics for some extern functions.")
  private boolean useSvCompSemanticsForExternFunctions = true;

  private final CFA cfa;
  private final SvLibCurrentScope scope;
  private final FormulaManagerView formulaManager;
  private final CtoFormulaConverter converter;

  private final String INPUT_DUMMY_VAR_PREFIX;

  public Initializer(
      CFA pCFA,
      SvLibCurrentScope pCurrentScope,
      FormulaManagerView pFormulaManager,
      CtoFormulaConverter pConverter,
      String pINPUT_DUMMY_VAR_PREFIX) {
    cfa = pCFA;
    scope = pCurrentScope;
    formulaManager = pFormulaManager;
    converter = pConverter;
    INPUT_DUMMY_VAR_PREFIX = pINPUT_DUMMY_VAR_PREFIX;
  }

  void initialize(ImmutableList.Builder<SvLibCommand> pCommandsCollector)
      throws UnsupportedOperationException {
    for (FunctionEntryNode entryNode : cfa.entryNodes()) {
      CFunctionEntryNode cEntryNode = (CFunctionEntryNode) entryNode;
      String procedureName = entryNode.getFunctionName();

      ImmutableList<SvLibParsingParameterDeclaration> inputParameters =
          collectInputParameters(cEntryNode.getFunctionParameters(), procedureName);
      ImmutableList<SvLibParsingParameterDeclaration> returnParameter =
          collectReturnParameter(cEntryNode.getReturnVariable(), procedureName);
      ImmutableList.Builder<SvLibParsingParameterDeclaration> localParametersCollector =
          ImmutableList.builder();

      // create (assignable) dummy parameters for (non-assignable) inputParameters
      for (SvLibParsingParameterDeclaration inputParameter : inputParameters) {
        localParametersCollector.add(createDummyForInputParameter(inputParameter));
      }

      ImmutableList.Builder<CDeclaration> declarationsCollector = ImmutableList.builder();
      for (CFAEdge edge : getAllRelevantEdges(entryNode)) {
        if (edge instanceof CDeclarationEdge declarationEdge) {
          declarationsCollector.add(declarationEdge.getDeclaration());
        }
      }
      ImmutableList<CDeclaration> declarations = declarationsCollector.build();

      // collect declarations of local parameters and global variables
      for (CDeclaration declaration : declarations) {
        if (declaration instanceof CVariableDeclaration variableDeclaration) {
          SvLibType type = convertToSvLibType(variableDeclaration.getType());

          if (variableDeclaration.isGlobal()) {
            SvLibParsingVariableDeclaration globalVariable =
                new SvLibParsingVariableDeclaration(
                    FileLocation.DUMMY,
                    variableDeclaration.isGlobal(),
                    variableDeclaration.getType().isConst(),
                    type,
                    variableDeclaration.getName(),
                    variableDeclaration.getOrigName(),
                    null);
            scope.addVariable(globalVariable);
            pCommandsCollector.add(
                new SvLibVariableDeclarationCommand(globalVariable, FileLocation.DUMMY));
          } else {
            SvLibParsingParameterDeclaration parameter =
                new SvLibParsingParameterDeclaration(
                    FileLocation.DUMMY, type, declaration.getName(), procedureName);
            localParametersCollector.add(parameter);
          }
        } else if (declaration instanceof CFunctionDeclaration functionDeclaration) {
          boolean isExtern = !cfa.getAllFunctionNames().contains(functionDeclaration.getName());
          if (isExtern) {
            SvLibProcedureDeclaration externProcedureDeclaration =
                createProcedureDeclarationForExternFunction(functionDeclaration);
            SvLibStatement externProcedureBody =
                createBodyForExternProcedure(externProcedureDeclaration);

            scope.addProcedureDeclaration(externProcedureDeclaration);
            SvLibProcedureDefinitionCommand externProcedureDefinition =
                new SvLibProcedureDefinitionCommand(
                    FileLocation.DUMMY, externProcedureDeclaration, externProcedureBody);
            pCommandsCollector.add(externProcedureDefinition);
          }
        }
      }

      SvLibProcedureDeclaration procedureDeclaration =
          new SvLibProcedureDeclaration(
              FileLocation.DUMMY,
              procedureName,
              inputParameters,
              returnParameter,
              localParametersCollector.build());
      scope.addProcedureDeclaration(procedureDeclaration);
    }
  }

  private SvLibType convertToSvLibType(CType pCType) {
    FormulaType<?> formulaType = converter.getFormulaTypeFromType(pCType);
    FormulaType<Formula> encodedFormulaType = formulaManager.getEncodedFormulaType(formulaType);

    if (encodedFormulaType.isBooleanType()) {
      return SvLibSmtLibPredefinedType.BOOL;
    } else if (encodedFormulaType.isIntegerType()) {
      return SvLibSmtLibPredefinedType.INT;
    } else if (encodedFormulaType.isStringType()) {
      return SvLibSmtLibPredefinedType.STRING;
    } else if (encodedFormulaType.isFloatingPointType() || encodedFormulaType.isRationalType()) {
      return SvLibSmtLibPredefinedType.REAL;
    } else if (encodedFormulaType.isBitvectorType()) {
      BitvectorType bitvectorType = (BitvectorType) formulaType;
      return new SvLibSmtLibBitVectorType(bitvectorType.getSize());
    }

    throw new UnsupportedOperationException(
        "Transformation to a SvLibType failed for CType " + pCType);
  }

  private ImmutableList<SvLibParsingParameterDeclaration> collectInputParameters(
      ImmutableList<CParameterDeclaration> pParameterDeclarations, String pProcedureName)
      throws UnsupportedOperationException {
    ImmutableList.Builder<SvLibParsingParameterDeclaration> parameterCollector =
        ImmutableList.builder();

    for (CParameterDeclaration parameter : pParameterDeclarations) {
      if (parameter.asVariableDeclaration().getType() instanceof CSimpleType asSimpleType) {
        parameterCollector.add(
            new SvLibParsingParameterDeclaration(
                FileLocation.DUMMY,
                convertToSvLibType(asSimpleType),
                getNameForInputParameterDummy(parameter.getName()),
                pProcedureName));
      }
    }
    return parameterCollector.build();
  }

  private ImmutableList<SvLibParsingParameterDeclaration> collectReturnParameter(
      Optional<CVariableDeclaration> pReturnVariable, String pProcedureName)
      throws UnsupportedOperationException {
    if (pReturnVariable.isEmpty()) {
      return ImmutableList.of();
    }
    if (pReturnVariable.orElseThrow().getType() instanceof CSimpleType asSimpleType) {
      return ImmutableList.of(
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY,
              convertToSvLibType(asSimpleType),
              pReturnVariable.orElseThrow().getName(),
              pProcedureName));
    }
    return ImmutableList.of();
  }

  private SvLibParsingParameterDeclaration createDummyForInputParameter(
      SvLibParsingParameterDeclaration pInputParameter) {
    return new SvLibParsingParameterDeclaration(
        FileLocation.DUMMY,
        pInputParameter.getType(),
        getOriginalNameOfInputParameterDummy(pInputParameter.getName()),
        pInputParameter.getProcedureName());
  }

  private String getNameForInputParameterDummy(String pOriginalName) {
    return INPUT_DUMMY_VAR_PREFIX + pOriginalName;
  }

  private String getOriginalNameOfInputParameterDummy(String pDummyName) {
    if (pDummyName.startsWith(INPUT_DUMMY_VAR_PREFIX)) {
      // return the name without the prefix
      return pDummyName.substring(INPUT_DUMMY_VAR_PREFIX.length());
    }
    // FIXME This case should never occur, so throw instead?
    return pDummyName;
  }

  private ImmutableList<CFAEdge> getAllRelevantEdges(FunctionEntryNode pEntryNode) {
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(pEntryNode, edgeCollector);
    return ImmutableList.copyOf(edgeCollector.getVisitedEdges());
  }

  private SvLibProcedureDeclaration createProcedureDeclarationForExternFunction(
      CFunctionDeclaration pCFunctionDeclaration) {
    String functionName = pCFunctionDeclaration.getName();

    ImmutableList.Builder<SvLibParsingParameterDeclaration> returnParameterCollector =
        ImmutableList.builder();
    CType originalCReturnType = pCFunctionDeclaration.getType().getReturnType();
    if (!(originalCReturnType instanceof CVoidType)) {
      SvLibType convertedReturnType = convertToSvLibType(originalCReturnType);
      SvLibParsingParameterDeclaration returnParameterDeclaration =
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY, convertedReturnType, "_retval_", functionName);
      returnParameterCollector.add(returnParameterDeclaration);
    }

    ImmutableList<CParameterDeclaration> inputParameters = pCFunctionDeclaration.getParameters();
    // TODO Ask: is there a reason to prefer one over the other?
    //  ImmutableList<CType> parameters_as_CType = pCFunctionDeclaration.getType().getParameters();
    ImmutableList.Builder<SvLibParsingParameterDeclaration> convertedInputParametersCollector =
        ImmutableList.builder();
    for (CParameterDeclaration inputParameter : inputParameters) {
      SvLibParsingParameterDeclaration convertedInputParameter =
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY,
              convertToSvLibType(inputParameter.getType()),
              inputParameter.getName(),
              functionName);
      convertedInputParametersCollector.add(convertedInputParameter);
    }

    return new SvLibProcedureDeclaration(
        FileLocation.DUMMY,
        functionName,
        convertedInputParametersCollector.build(),
        returnParameterCollector.build(),
        ImmutableList.of());
  }

  private SvLibStatement createBodyForExternProcedure(
      SvLibProcedureDeclaration pProcedureDeclaration) {
    String procedureName = pProcedureDeclaration.getProcedureName();

    if (procedureName.contains("abort")) {
      return new SvLibAssumeStatement(
          FileLocation.DUMMY,
          new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
          ImmutableList.of(),
          ImmutableList.of(new SvLibTagReference(procedureName, FileLocation.DUMMY)));
    }

    if (useSvCompSemanticsForExternFunctions) {
      if (procedureName.contains("__VERIFIER_nondet_memory")) {
        // implement when a memory model exists
        throw new UnsupportedOperationException(
            "Transformation of programs that include extern function __VERIFIER_nondet_memory() is"
                + " not supported.");
      } else if (procedureName.contains("__VERIFIER_nondet")) {
        if (!pProcedureDeclaration.getReturnValues().isEmpty()) {
          // TODO assume statement to conform to C type
          return new SvLibHavocStatement(
              FileLocation.DUMMY,
              ImmutableList.of(),
              ImmutableList.of(new SvLibTagReference(procedureName, FileLocation.DUMMY)),
              castListToSimpleParsingDeclaration(pProcedureDeclaration.getReturnValues()));
        } /*else {
            throw new UnsupportedOperationException(
                "Failed to create procedure for "
                    + procedureName
                    + " because the return parameter is empty.");
          }*/

      } else if (procedureName.contains("assert_fail")
          || procedureName.contains("__VERIFIER_error")) {
        // FIXME probably better to just leave empty and encode later depending on property
        return new SvLibAssumeStatement(
            FileLocation.DUMMY,
            new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
            ImmutableList.of(),
            ImmutableList.of(new SvLibTagReference(procedureName, FileLocation.DUMMY)));
      }
      // TODO implement verifier_assume?
    }

    // havoc return value for extern, non-void functions
    if (!pProcedureDeclaration.getReturnValues().isEmpty()) {
      return new SvLibHavocStatement(
          FileLocation.DUMMY,
          ImmutableList.of(),
          ImmutableList.of(new SvLibTagReference(procedureName, FileLocation.DUMMY)),
          castListToSimpleParsingDeclaration(pProcedureDeclaration.getReturnValues()));
    }

    // skip for extern, void functions
    return new SvLibAssumeStatement(
        FileLocation.DUMMY,
        new SvLibBooleanConstantTerm(true, FileLocation.DUMMY),
        ImmutableList.of(),
        ImmutableList.of(new SvLibTagReference(procedureName, FileLocation.DUMMY)));
  }

  private ImmutableList<SvLibSimpleParsingDeclaration> castListToSimpleParsingDeclaration(
      ImmutableList<SvLibParsingParameterDeclaration> pParameters) {

    ImmutableList.Builder<SvLibSimpleParsingDeclaration> simpleDeclarations =
        ImmutableList.builder();
    if (!pParameters.isEmpty()) {
      for (SvLibParsingParameterDeclaration parameterDeclaration : pParameters) {
        simpleDeclarations.add(parameterDeclaration);
      }
    }
    return simpleDeclarations.build();
  }
}
