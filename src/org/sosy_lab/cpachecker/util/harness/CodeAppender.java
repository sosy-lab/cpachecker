// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.harness;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.cfa.types.AbstractFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JMethodType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.util.testcase.ExpressionTestValue;
import org.sosy_lab.cpachecker.util.testcase.InitializerTestValue;
import org.sosy_lab.cpachecker.util.testcase.TestValue;
import org.sosy_lab.cpachecker.util.testcase.TestValue.AuxiliaryCode;
import org.sosy_lab.cpachecker.util.testcase.TestVector;

class CodeAppender implements Appendable {

  private static final String RETVAL_NAME = "retval";

  private final Appendable appendable;

  public CodeAppender(Appendable pAppendable) {
    appendable = Objects.requireNonNull(pAppendable);
  }

  @Override
  public String toString() {
    return appendable.toString();
  }

  @CanIgnoreReturnValue
  private CodeAppender appendVectorIndexDeclaration(String pInputFunctionVectorIndexName)
      throws IOException {
    appendable.append("  static unsigned int ");
    appendable.append(pInputFunctionVectorIndexName);
    appendln(" = 0;");
    return this;
  }

  @CanIgnoreReturnValue
  public CodeAppender appendDeclaration(Type pType, String pName) throws IOException {
    appendable.append(pType.toASTString(pName));
    appendln(";");
    return this;
  }

  @CanIgnoreReturnValue
  public CodeAppender appendAssignment(String pRetvalName, ARightHandSide pValue)
      throws IOException {
    appendable.append(pRetvalName);
    appendable.append(" = ");
    appendable.append(pValue.toASTString());
    return this;
  }

  @CanIgnoreReturnValue
  CodeAppender appendAssignment(String pRetvalName, TestValue pValue) throws IOException {
    return appendAssignment(pRetvalName, pValue, true);
  }

  @CanIgnoreReturnValue
  private CodeAppender appendAssignment(String pRetvalName, TestValue pValue, boolean pEnclose)
      throws IOException {
    boolean hasAuxiliaryCode = !pValue.getAuxiliaryCode().isEmpty();
    if (hasAuxiliaryCode) {
      if (pEnclose) {
        appendable.append("{ ");
      }
      for (AuxiliaryCode auxiliaryCode : pValue.getAuxiliaryCode()) {
        appendable.append(auxiliaryCode.code());
        if (pEnclose) {
          appendable.append(" ");
        } else {
          appendln();
        }
      }
    }
    appendable.append(pRetvalName);
    appendable.append(" = ");
    appendable.append(pValue.getValue().toASTString());
    appendable.append(";");
    if (hasAuxiliaryCode && pEnclose) {
      appendable.append(" }");
    }
    return this;
  }

  @CanIgnoreReturnValue
  public CodeAppender appendln(String pLine) throws IOException {
    appendable.append(pLine);
    appendln();
    return this;
  }

  @CanIgnoreReturnValue
  public Appendable appendln() throws IOException {
    appendable.append(System.lineSeparator());
    return this;
  }

  @CanIgnoreReturnValue
  @Override
  public CodeAppender append(CharSequence pCsq) throws IOException {
    appendable.append(pCsq);
    return this;
  }

  @CanIgnoreReturnValue
  @Override
  public CodeAppender append(char pChar) throws IOException {
    appendable.append(pChar);
    return this;
  }

  @CanIgnoreReturnValue
  @Override
  public CodeAppender append(CharSequence pCsq, int pStart, int pEnd) throws IOException {
    appendable.append(pCsq, pStart, pEnd);
    return this;
  }

  @CanIgnoreReturnValue
  public CodeAppender append(TestVector pVector) throws IOException {
    for (AVariableDeclaration inputVariable : pVector.getInputVariables()) {
      InitializerTestValue inputValue = pVector.getInputValue(inputVariable);
      List<AuxiliaryCode> auxiliaryCode = inputValue.getAuxiliaryCode();
      Type type = PredefinedTypes.getCanonicalType(inputVariable.getType());
      boolean requiresInitialization = HarnessExporter.canInitialize(type);
      if (requiresInitialization && !auxiliaryCode.isEmpty()) {
        for (AuxiliaryCode statement : auxiliaryCode) {
          appendln(statement.code());
        }
      }
      final AInitializer initializer;
      if (!requiresInitialization) {
        initializer = null;
      } else {
        initializer = inputValue.getValue();
      }
      final AVariableDeclaration internalDeclaration;
      if (inputVariable instanceof CVariableDeclaration) {
        internalDeclaration =
            new CVariableDeclaration(
                FileLocation.DUMMY,
                inputVariable.isGlobal(),
                CStorageClass.AUTO,
                (CType) inputVariable.getType(),
                inputVariable.getName(),
                inputVariable.getOrigName(),
                inputVariable.getQualifiedName(),
                (CInitializer) initializer);
      } else if (inputVariable instanceof JVariableDeclaration) {
        internalDeclaration =
            new JVariableDeclaration(
                FileLocation.DUMMY,
                (JType) inputVariable.getType(),
                inputVariable.getName(),
                inputVariable.getOrigName(),
                inputVariable.getQualifiedName(),
                initializer,
                ((JVariableDeclaration) inputVariable).isFinal());
      } else {
        throw new AssertionError("Unsupported declaration type: " + inputVariable);
      }
      appendln(internalDeclaration.toASTString());
    }
    for (AFunctionDeclaration inputFunction : pVector.getInputFunctions()) {
      List<ExpressionTestValue> inputValues = pVector.getInputValues(inputFunction);
      Type returnType = inputFunction.getType().getReturnType();
      append(inputFunction);
      appendln(" {");
      if (inputFunction instanceof CFunctionDeclaration cDeclaration
          && cDeclaration.getAttributes().contains(FunctionAttribute.NO_RETURN)) {
        // if the function has attribute __noreturn__, make sure
        // that our added definition does not return. There are two major ways to do this:
        // 1. Run into a loop that never terminates.
        // 2. exit/abort.
        // We exit so that we get fast feedback from our test when a noreturn method is called.
        String functionName = inputFunction.getName();
        appendln(
            "  fprintf(stderr, \"Called method "
                + functionName
                + " that has attribute noreturn.\\n\");");
        // The 1 has no special meaning; it's just as good as 2 or 100, because we do not know
        // what other status codes are used in the program.
        // We do not use 0 to avoid confusion with the 'normal' return value that signals
        // that everything is fine.
        appendln("  exit(1);");
      } else if (!returnType.equals(CVoidType.VOID)) {
        String inputFunctionVectorIndexName = "test_vector_index";
        if (inputValues.size() > 1) {
          appendVectorIndexDeclaration(inputFunctionVectorIndexName);
        }
        append("  ");
        appendDeclaration(returnType, RETVAL_NAME);
        if (inputValues.size() == 1) {
          append("  ");
          appendAssignment(RETVAL_NAME, inputValues.iterator().next());
          appendln();
        } else if (inputValues.size() > 1) {
          append("  switch (");
          append(inputFunctionVectorIndexName);
          appendln(") {");
          int i = 0;
          for (TestValue value : inputValues) {
            append("    case ");
            append(Integer.toString(i));
            append(": ");
            appendAssignment(RETVAL_NAME, value);
            appendln(" break;");
            ++i;
          }
          appendln("  }");
          append("  ++");
          append(inputFunctionVectorIndexName);
          appendln(";");
        }
        append("  return ");
        append(RETVAL_NAME);
        appendln(";");
      }
      appendln("}");
    }
    return this;
  }

  @CanIgnoreReturnValue
  public CodeAppender append(AFunctionDeclaration pInputFunction) throws IOException {
    return append(enforceParameterNames(pInputFunction).toASTString(pInputFunction.getName()));
  }

  private static AbstractFunctionType enforceParameterNames(AFunctionDeclaration pInputFunction) {
    AFunctionType functionType = pInputFunction.getType();
    if (functionType instanceof CFunctionType cFunctionType) {
      return new CFunctionTypeWithNames(
          cFunctionType.getReturnType(),
          FluentIterable.from(enforceParameterNames(pInputFunction.getParameters()))
              .filter(CParameterDeclaration.class)
              .toList(),
          functionType.takesVarArgs());
    }
    if (functionType instanceof JMethodType methodType) {
      return new JMethodType(
          methodType.getReturnType(),
          FluentIterable.from(enforceParameterNames(pInputFunction.getParameters()))
              .filter(JType.class)
              .toList(),
          functionType.takesVarArgs());
    }
    throw new AssertionError("Unsupported function type: " + functionType.getClass());
  }

  private static List<AParameterDeclaration> enforceParameterNames(
      List<? extends AParameterDeclaration> pParameters) {
    Set<String> usedNames = Sets.newHashSetWithExpectedSize(pParameters.size());
    int i = 0;
    List<AParameterDeclaration> result = new ArrayList<>(pParameters.size());
    for (AParameterDeclaration parameter : pParameters) {
      AParameterDeclaration declaration = parameter;
      if (!declaration.getName().isEmpty()) {
        usedNames.add(declaration.getName());
      } else {
        String name;
        while (!usedNames.add(name = "p" + i)) {
          ++i;
        }
        if (declaration instanceof CParameterDeclaration) {
          declaration =
              new CParameterDeclaration(FileLocation.DUMMY, (CType) declaration.getType(), name);
        } else if (declaration instanceof JParameterDeclaration jDecl) {
          declaration =
              new JParameterDeclaration(
                  FileLocation.DUMMY,
                  jDecl.getType(),
                  name,
                  jDecl.getQualifiedName() + name,
                  jDecl.isFinal());
        } else {
          throw new AssertionError(
              "Unsupported parameter declaration type: " + declaration.getClass());
        }
      }
      result.add(declaration);
    }
    return result;
  }
}
