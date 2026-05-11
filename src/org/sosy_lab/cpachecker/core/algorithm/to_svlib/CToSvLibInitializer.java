// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerBase;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;

class CToSvLibInitializer {

  // TODO change to option! Dont forget prefix!
  // @Option(secure = true, description = "Use SV-COMP semantics for some extern functions.")
  private ExternalFunctionsEncodingMode encodingModeForExternalFunctions =
      CToSvLibInitializer.ExternalFunctionsEncodingMode.SV_COMP;

  private enum ExternalFunctionsEncodingMode {
    SV_COMP,
    HAVOC
  }

  private final CFA cfa;
  private final SvLibCurrentScope scope;
  private final FormulaManagerView formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final CtoFormulaConverter converter;

  private final String INPUT_DUMMY_VAR_PREFIX;

  CToSvLibInitializer(
      CFA pCFA,
      SvLibCurrentScope pCurrentScope,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      CtoFormulaConverter pConverter,
      String pINPUT_DUMMY_VAR_PREFIX) {
    cfa = pCFA;
    scope = pCurrentScope;
    formulaManager = pFormulaManager;
    pathFormulaManager = pPathFormulaManager;
    converter = pConverter;
    INPUT_DUMMY_VAR_PREFIX = pINPUT_DUMMY_VAR_PREFIX;
  }

  void initialize(ImmutableList.Builder<SvLibCommand> pCommandsCollector)
      throws UnsupportedOperationException, CPATransferException, InterruptedException {
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
      ImmutableList.Builder<CFunctionCallExpression> undeclaredFunctionsCollector =
          ImmutableList.builder();
      Set<SvLibParsingVariableDeclaration> createdHeapModels = new HashSet<>();
      Set<SvLibParsingVariableDeclaration> createdAddressVariables = new HashSet<>();
      for (CFAEdge edge : getAllRelevantEdges(entryNode)) {
        if (edge instanceof CDeclarationEdge declarationEdge) {
          declarationsCollector.add(declarationEdge.getDeclaration());
        } else if (edge instanceof CStatementEdge cStatementEdge
            && cStatementEdge.getStatement()
                instanceof CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement
            && cFunctionCallAssignmentStatement.getRightHandSide().getDeclaration() == null) {
          undeclaredFunctionsCollector.add(
              cFunctionCallAssignmentStatement.getFunctionCallExpression());
        }
        // create PathFormula for each edge and create needed arrays and ADDRESS_OF variables
        initializeHeapForEdge(edge, pCommandsCollector, createdHeapModels, createdAddressVariables);
      }
      ImmutableList<CDeclaration> declarations = declarationsCollector.build();

      // collect declarations of local parameters and global variables + create procedures for
      // external functions
      for (CDeclaration declaration : declarations) {
        if (declaration instanceof CVariableDeclaration variableDeclaration) {
          SvLibType type = convertToSvLibSmtLibType(variableDeclaration.getType());

          if (variableDeclaration.isGlobal()) {
            SvLibParsingVariableDeclaration globalVariable =
                createGlobalVariableDeclaration(variableDeclaration, type);
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
            SvLibProcedureDefinitionCommand externProcedureDefinition =
                createExternProcedureDefinition(functionDeclaration);
            scope.addProcedureDeclaration(externProcedureDefinition.getProcedureDeclaration());
            pCommandsCollector.add(externProcedureDefinition);
          }
        }
      }

      Set<SvLibProcedureDeclaration> proceduresCreatedForUndeclaredFunctions = new HashSet<>();
      for (CFunctionCallExpression functionCallExpression : undeclaredFunctionsCollector.build()) {
        String functionName = functionCallExpression.getFunctionNameExpression().toASTString();
        CType expressionType = functionCallExpression.getExpressionType();

        SvLibProcedureDefinitionCommand procedureDefinitionCommand =
            createProcedureDefinitionForUndeclaredFunction(functionName, expressionType);

        // check to avoid duplicate procedures if undeclared function is called multiple times
        if (!proceduresCreatedForUndeclaredFunctions.contains(
            procedureDefinitionCommand.getProcedureDeclaration())) {
          scope.addProcedureDeclaration(procedureDefinitionCommand.getProcedureDeclaration());
          pCommandsCollector.add(procedureDefinitionCommand);
          proceduresCreatedForUndeclaredFunctions.add(
              procedureDefinitionCommand.getProcedureDeclaration());
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

  private void initializeHeapForEdge(
      CFAEdge pEdge,
      ImmutableList.Builder<SvLibCommand> pCommandsCollector,
      Set<SvLibParsingVariableDeclaration> pCreatedHeapModels,
      Set<SvLibParsingVariableDeclaration> pCreatedAddressVariables)
      throws CPATransferException, InterruptedException {
    PointerTargetSet pointerTargetSetForEdge =
        pathFormulaManager
            .makeAnd(pathFormulaManager.makeEmptyPathFormula(), pEdge)
            .getPointerTargetSet();

    if (!pointerTargetSetForEdge.equals(PointerTargetSet.emptyPointerTargetSet())) {
      PersistentSortedMap<PointerBase, CType> bases = pointerTargetSetForEdge.getBases();
      for (Entry<PointerBase, CType> baseEntry : bases.entrySet().descendingSet()) {
        // create array for heap model if no array for the CType of the PointerBase has been created
        createArrayForHeap(baseEntry.getValue(), pCommandsCollector, pCreatedHeapModels);
        createAddressOfVariables(
            baseEntry.getKey(), baseEntry.getValue(), pCommandsCollector, pCreatedAddressVariables);
      }
    }
  }

  private void createArrayForHeap(
      CType pBaseType,
      ImmutableList.Builder<SvLibCommand> pCommandsCollector,
      Set<SvLibParsingVariableDeclaration> pCreatedHeapModels) {
    // create array for heap model if no such array has been created before
    SvLibParsingVariableDeclaration heapArrayParsingVariableDeclaration =
        createHeapArrayDeclaration(pBaseType);
    if (!pCreatedHeapModels.contains(heapArrayParsingVariableDeclaration)) {
      SvLibVariableDeclarationCommand heapArrayVariableDeclarationCommand =
          new SvLibVariableDeclarationCommand(
              heapArrayParsingVariableDeclaration, FileLocation.DUMMY);
      pCommandsCollector.add(heapArrayVariableDeclarationCommand);
      scope.addVariable(heapArrayParsingVariableDeclaration);
      pCreatedHeapModels.add(heapArrayParsingVariableDeclaration);
    }
  }

  private SvLibParsingVariableDeclaration createHeapArrayDeclaration(CType pElementType) {
    String heapTypeName = "";
    if (pElementType instanceof CSimpleType simpleType) {
      heapTypeName = simpleType.getType().toASTString();
    } else if (pElementType instanceof CArrayType arrayType
        && arrayType.getCanonicalType().getType() instanceof CSimpleType simpleType) {
      heapTypeName = simpleType.getType().toASTString();
    }
    if (heapTypeName.isEmpty()) {
      throw new UnsupportedOperationException(
          "Failed to create array to model heap for CType " + pElementType);
    }
    heapTypeName = "*" + heapTypeName;
    SvLibSmtLibArrayType arrayType =
        new SvLibSmtLibArrayType(
            SvLibSmtLibPredefinedType.INT, convertToSvLibSmtLibType(pElementType));
    return new SvLibParsingVariableDeclaration(
        FileLocation.DUMMY, true, false, arrayType, heapTypeName, heapTypeName, null);
  }

  private void createAddressOfVariables(
      PointerBase pPointerBase,
      CType pBaseType,
      ImmutableList.Builder<SvLibCommand> pCommandsCollector,
      Set<SvLibParsingVariableDeclaration> pCreatedAddressVariables) {
    // Replace :: in addresses of local variables, since : causes an issue with the SV-LIB parser
    String addressName = pPointerBase.formulaEncoding().replace("::", "_") + "@";
    SvLibSmtLibType addressType = convertToSvLibSmtLibType(pBaseType);

    SvLibParsingVariableDeclaration addressVariable =
        new SvLibParsingVariableDeclaration(
            FileLocation.DUMMY, true, false, addressType, addressName, addressName, null);
    if (!pCreatedAddressVariables.contains(addressVariable)) {
      pCreatedAddressVariables.add(addressVariable);
      scope.addVariable(addressVariable);
      pCommandsCollector.add(
          new SvLibVariableDeclarationCommand(addressVariable, FileLocation.DUMMY));
    }
  }

  private SvLibSmtLibType convertToSvLibSmtLibType(CType pCType) {
    FormulaType<?> formulaType = converter.getFormulaTypeFromType(pCType);
    FormulaType<Formula> encodedFormulaType = formulaManager.getEncodedFormulaType(formulaType);

    if (encodedFormulaType.isBooleanType()) {
      return SvLibSmtLibPredefinedType.BOOL;
    } else if (encodedFormulaType.isIntegerType()) {
      return SvLibSmtLibPredefinedType.INT;
    } else if (encodedFormulaType.isStringType()) {
      return SvLibSmtLibPredefinedType.STRING;
    } else if (encodedFormulaType.isRationalType()) {
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
                convertToSvLibSmtLibType(asSimpleType),
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
              convertToSvLibSmtLibType(asSimpleType),
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
    throw new IllegalArgumentException(
        "Cannot remove prefix " + INPUT_DUMMY_VAR_PREFIX + " from name " + pDummyName);
  }

  private ImmutableList<CFAEdge> getAllRelevantEdges(FunctionEntryNode pEntryNode) {
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(pEntryNode, edgeCollector);
    return ImmutableList.copyOf(edgeCollector.getVisitedEdges());
  }

  private SvLibParsingVariableDeclaration createGlobalVariableDeclaration(
      CVariableDeclaration pVariableDeclaration, SvLibType pType) {
    return new SvLibParsingVariableDeclaration(
        FileLocation.DUMMY,
        pVariableDeclaration.isGlobal(),
        pVariableDeclaration.getType().isConst(),
        pType,
        pVariableDeclaration.getName(),
        pVariableDeclaration.getOrigName(),
        null);
  }

  private SvLibProcedureDefinitionCommand createExternProcedureDefinition(
      CFunctionDeclaration pFunctionDeclaration) {
    SvLibProcedureDeclaration externProcedureDeclaration =
        createProcedureDeclarationForExternFunction(pFunctionDeclaration);
    SvLibStatement externProcedureBody = createBodyForExternProcedure(externProcedureDeclaration);
    return new SvLibProcedureDefinitionCommand(
        FileLocation.DUMMY, externProcedureDeclaration, externProcedureBody);
  }

  private SvLibProcedureDefinitionCommand createProcedureDefinitionForUndeclaredFunction(
      String pFunctionName, CType pReturnType) {
    ImmutableList.Builder<SvLibParsingParameterDeclaration> returnParameterCollector =
        ImmutableList.builder();
    if (!(pReturnType instanceof CVoidType)) {
      returnParameterCollector.add(
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY,
              convertToSvLibSmtLibType(pReturnType),
              "__retval__",
              pFunctionName));
    }
    SvLibProcedureDeclaration procedureDeclarationForUndeclaredFunction =
        new SvLibProcedureDeclaration(
            FileLocation.DUMMY,
            pFunctionName,
            ImmutableList.of(),
            returnParameterCollector.build(),
            ImmutableList.of());
    SvLibStatement body = createBodyForExternProcedure(procedureDeclarationForUndeclaredFunction);
    return new SvLibProcedureDefinitionCommand(
        FileLocation.DUMMY, procedureDeclarationForUndeclaredFunction, body);
  }

  private SvLibProcedureDeclaration createProcedureDeclarationForExternFunction(
      CFunctionDeclaration pCFunctionDeclaration) {
    String functionName = pCFunctionDeclaration.getName();

    ImmutableList.Builder<SvLibParsingParameterDeclaration> returnParameterCollector =
        ImmutableList.builder();
    CType originalCReturnType = pCFunctionDeclaration.getType().getReturnType();
    if (!(originalCReturnType instanceof CVoidType)) {
      SvLibType convertedReturnType = convertToSvLibSmtLibType(originalCReturnType);
      SvLibParsingParameterDeclaration returnParameterDeclaration =
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY, convertedReturnType, "_retval_", functionName);
      returnParameterCollector.add(returnParameterDeclaration);
    }

    ImmutableList<CParameterDeclaration> inputParameters = pCFunctionDeclaration.getParameters();
    ImmutableList.Builder<SvLibParsingParameterDeclaration> convertedInputParametersCollector =
        ImmutableList.builder();
    for (int i = 0; i < inputParameters.size(); i++) {
      CParameterDeclaration inputParameter = inputParameters.get(i);
      String inputParameterName =
          !inputParameter.getName().isEmpty()
              ? inputParameter.getName()
              : "inputDummy_" + i + "_extern";
      SvLibParsingParameterDeclaration convertedInputParameter =
          new SvLibParsingParameterDeclaration(
              FileLocation.DUMMY,
              convertToSvLibSmtLibType(inputParameter.getType()),
              inputParameterName,
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

    if (procedureName.startsWith("abort")) {
      return new SvLibAssumeStatement(
          FileLocation.DUMMY,
          new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
          ImmutableList.of(),
          ImmutableList.of(new SvLibTagReference(procedureName, FileLocation.DUMMY)));
    }

    if (encodingModeForExternalFunctions.equals(ExternalFunctionsEncodingMode.SV_COMP)) {
      if (procedureName.startsWith("__VERIFIER_nondet_memory")) {
        // implement when a memory model exists
        throw new UnsupportedOperationException(
            "Transformation of programs that include extern function __VERIFIER_nondet_memory() is"
                + " not supported.");
      } else if (procedureName.startsWith("__VERIFIER_nondet")) {
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
      }
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
