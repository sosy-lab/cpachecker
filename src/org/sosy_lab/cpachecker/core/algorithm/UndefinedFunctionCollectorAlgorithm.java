// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.StandardFunctions;
import org.sosy_lab.cpachecker.util.VerifierNondetFunctions;

@Options(prefix = "undefinedFunctionsCollector")
public class UndefinedFunctionCollectorAlgorithm
    implements Algorithm, StatisticsProvider, Statistics {

  private static final String ASSUME_FUNCTION_NAME = "__VERIFIER_assume";
  private static final String ASSUME_FUNCTION_DECL = "void " + ASSUME_FUNCTION_NAME + "(int);\n";

  @Option(secure = true, description = "export undefined functions as C file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path stubsFile = Path.of("stubs.c");

  @Option(secure = true, description = "Set of functions that should be ignored")
  private Set<String> allowedFunctions =
      ImmutableSet.of(
          // General
          "__assert_fail",
          // Float
          "__finite",
          "__fpclassify",
          "__fpclassifyf",
          "__fpclassifyl",
          "__isinf",
          "__isinff",
          "__isnan",
          "__isnanf",
          "__signbit",
          "__signbitf");

  @Option(secure = true, description = "Ignore functions that are defined by C11")
  private boolean allowC11Functions = true;

  @Option(
      secure = true,
      description = "Ignore functions that are defined by GNU C and not by C11/POSIX")
  private boolean allowGnuCFunctions = true;

  @Option(secure = true, description = "Ignore functions that are defined by POSIX")
  private boolean allowPosixFunctions = true;

  @Option(secure = true, description = "Memory-allocation function that will be used in stubs")
  private String externAllocFunction = "external_alloc";

  @Option(
      secure = true,
      description = "Regexp matching function names that are allowed to be undefined")
  private Pattern allowedFunctionsRegexp = Pattern.compile("^(__VERIFIER|pthread)_[a-zA-Z0-9_]*");

  @Option(secure = true, description = "Regexp matching function names that need not be declared")
  private Pattern allowedUndeclaredFunctionsRegexp = Pattern.compile("^__builtin_[a-zA-Z0-9_]*");

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  private final Map<String, AFunctionDeclaration> undefinedfFunctions = new HashMap<>();
  private final Set<String> undeclaredFunctions = new HashSet<>();

  private final String odmFunctionDecl = "void *" + externAllocFunction + "(void);\n";

  public UndefinedFunctionCollectorAlgorithm(
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws InterruptedException {
    collectUndefinedFunctions();

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void collectUndefinedFunctions() throws InterruptedException {
    for (CFAEdge edge : cfa.edges()) {
      shutdownNotifier.shutdownIfNecessary();
      if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
        final AStatementEdge stmtEdge = (AStatementEdge) edge;
        if (stmtEdge.getStatement() instanceof AFunctionCall aFunctionCall) {
          collectUndefinedFunction(aFunctionCall);
        }
      }
    }
  }

  private void collectUndefinedFunction(AFunctionCall call) {
    final AFunctionDeclaration functionDecl = call.getFunctionCallExpression().getDeclaration();

    if (functionDecl == null) {
      AExpression functionName = call.getFunctionCallExpression().getFunctionNameExpression();
      if (functionName instanceof AIdExpression aIdExpression) {
        // no declaration, but regular function call (no function pointer)
        String name = aIdExpression.getName();
        logger.log(Level.FINE, "Call to undeclared function", name, "found.");
        undeclaredFunctions.add(name);
      }
    } else {
      // a call to an undefined function
      if (!cfa.getAllFunctionNames().contains(functionDecl.getName())) {
        undefinedfFunctions.put(functionDecl.getName(), functionDecl);
      }
    }
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("Total undefined functions called:         " + undefinedfFunctions.size());
    pOut.println(
        "Non-standard undefined functions called:  "
            + (undefinedfFunctions.size()
                - undefinedfFunctions.keySet().stream().filter(this::skipFunction).count()));

    pOut.println("Total undeclared functions called:        " + undeclaredFunctions.size());
    pOut.println(
        "Non-standard undeclared functions called: "
            + undeclaredFunctions.stream()
                .filter(name -> !allowedUndeclaredFunctionsRegexp.matcher(name).matches())
                .count());
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (stubsFile != null) {
      try (Writer w = IO.openOutputFile(stubsFile, Charset.defaultCharset())) {
        for (Map.Entry<String, AFunctionDeclaration> k :
            new TreeMap<>(undefinedfFunctions).entrySet()) {
          printFunction(k.getKey(), k.getValue(), w);
        }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write stubs to the file");
      }
    }
  }

  private boolean skipFunction(String name) {
    return allowedFunctions.contains(name)
        || (allowC11Functions && StandardFunctions.C11_ALL_FUNCTIONS.contains(name))
        || (allowGnuCFunctions && StandardFunctions.GNUC_ALL_FUNCTIONS.contains(name))
        || (allowPosixFunctions && StandardFunctions.POSIX_ALL_FUNCTIONS.contains(name))
        || allowedFunctionsRegexp.matcher(name).matches()
        || allowedUndeclaredFunctionsRegexp.matcher(name).matches();
  }

  private void printFunction(String name, AFunctionDeclaration f, Writer w) throws IOException {
    if (skipFunction(name)) {
      logger.log(Level.FINE, " Skip function: " + name);
      w.write("// Skip function: " + name + "\n\n");
    } else {
      w.write("// Function: " + name + "\n");
      w.write("// with type: " + f.getType() + "\n");
      Type rt = f.getType().getReturnType();
      w.write("// with return type: " + rt + "\n");
      StringBuilder buf = new StringBuilder();
      StringBuilder prepend = new StringBuilder();
      boolean couldBeHandled = printType("  ", prepend, buf, (CType) rt);
      if (couldBeHandled) {
        w.write(prepend.toString());
        w.write(getSignature(name, f.getType()) + " {\n");
        w.write(buf.toString());
        w.write("}\n\n");
      } else {
        w.write("// ignored because stub could not be generated\n\n");
      }
    }
  }

  private String getSignature(String name, AFunctionType type) {
    StringBuilder res = new StringBuilder().append(name).append("(");
    int i = 0;
    for (Type pt : type.getParameters()) {
      if (i == 0) {
        res.append(pt.toASTString("arg" + i));
      } else {
        res.append(", ").append(pt.toASTString("arg" + i));
      }
      i++;
    }
    if (type.takesVarArgs()) {
      if (i != 0) {
        res.append(", ");
      }
      res.append("...");
    }
    res.append(")");
    return type.getReturnType().toASTString(res.toString());
  }

  private boolean printType(String indent, StringBuilder prepend, StringBuilder buf, CType rt) {
    boolean couldBeHandled = true;
    switch (rt) {
      case CVoidType unused -> {
        buf.append(indent + "// Void type\n");
        buf.append(indent + "return;\n");
      }
      case CPointerType unused -> {
        buf.append(indent + "// Pointer type\n");
        prepend.append(odmFunctionDecl);
        buf.append(indent + "return (" + rt.toASTString("") + ")" + externAllocFunction + "();\n");
      }
      case CSimpleType ct -> {
        String nondetFunc =
            VerifierNondetFunctions.getFunctionName(ct)
                .orElseThrow(() -> new RuntimeException("Unknown type " + ct));
        prepend.append(VerifierNondetFunctions.getFunctionDeclaration(ct).orElseThrow() + "\n");
        buf.append(indent + "// Simple type\n");
        buf.append(indent + "return " + nondetFunc + "();\n");
      }
      case CEnumType unused -> {
        buf.append(indent + "// Enum type\n");
        String nondetFunc = VerifierNondetFunctions.PREFIX + "int";
        prepend.append("int " + nondetFunc + "(void);\n");
        buf.append(indent + "return " + nondetFunc + "();\n");
      }
      case CCompositeType unused -> {
        buf.append(indent + "// Composite type\n");
        prepend.append(odmFunctionDecl);
        // We can not use rt.toASTString(), as it produces full definition with all fields
        buf.append(indent + rt + " *tmp = (" + rt + "*)" + externAllocFunction + "();\n");
        prepend.append(ASSUME_FUNCTION_DECL);
        buf.append(indent + ASSUME_FUNCTION_NAME + "(tmp != 0);\n");
        buf.append(indent + "return *tmp;\n");
      }
      case CElaboratedType cElaboratedType -> {
        CType real = cElaboratedType.getRealType();
        if (real == null) {
          couldBeHandled = false;
        } else {
          couldBeHandled = printType(indent, prepend, buf, real);
        }
      }
      case CTypedefType tt -> {
        buf.append(indent + "// Typedef type\n");

        CType real = tt.getRealType();
        buf.append(indent + "// Real type: " + real + "\n");
        couldBeHandled = printType(indent, prepend, buf, real);
      }
      default ->
          throw new AssertionError(
              "Unexpected type '" + rt + "' of class " + rt.getClass().getSimpleName());
    }
    return couldBeHandled;
  }

  @Override
  public @Nullable String getName() {
    return "UndefinedFunctionCollectorAlgorithm";
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }
}
