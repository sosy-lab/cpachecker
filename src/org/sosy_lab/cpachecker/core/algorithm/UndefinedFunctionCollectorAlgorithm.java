/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
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
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

@Options(prefix = "undefFuncCollectorAlgorithm")
public class UndefinedFunctionCollectorAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      name = "file",
      description = "export undefined functions as C file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate envPathFile = PathTemplate.ofFormatString("env.c");

  @Option(secure = true, description = "Set of functions that should be ignored")
  private Set<String> ignoreFunctions =
      ImmutableSet.of("memset", "kfree", "free", "calloc", "malloc");

  @Option(secure = true, description = "On-demand memory allocation function")
  private String odmAllocFunction = "external_alloc";

  @Option(
      secure = true,
      description = "Set of functions "
          + "that are defined by SV-COMP rules and should be ignored")
  private Pattern svcompFunctionsRegexp = Pattern.compile("^__VERIFIER_[a-zA-Z0-9_]*");

  @Option(
      secure = true,
      description = "Set of functions "
          + "that are defined by pthread.h and should be ignored")
  private Pattern pthreadFunctionsRegexp = Pattern.compile("^pthread_[a-zA-Z0-9_]*");

  private final LogManager logger;
  private final UndefinedFunctionCollectorAlgorithmStatistics stats;

  public static class UndefinedFunctionCallCollector extends CFATraversal.DefaultCFAVisitor {
    // TODO this class is copied from CFASecondPassBuilder, can we merge this class with the other visitor?
    // TODO in FunctionCallDumper there exists a similiar class, should we merge?

    private Map<String, AFunctionDeclaration> undefFuncs = new HashMap<>();
    private final CFA cfa;

    public UndefinedFunctionCallCollector(CFA pCfa) {
      this.cfa = pCfa;
    }

    @Override
    public CFATraversal.TraversalProcess visitEdge(final CFAEdge pEdge) {
      switch (pEdge.getEdgeType()) {
        case StatementEdge: {
          final AStatementEdge edge = (AStatementEdge) pEdge;
          if (edge.getStatement() instanceof AFunctionCall) {
            collectUndefinedFunction(edge);
          }
          break;
        }

        case FunctionCallEdge:
        case FunctionReturnEdge:
        case CallToReturnEdge:
        default:
          // nothing to do
      }
      return CFATraversal.TraversalProcess.CONTINUE;
    }

    private void collectUndefinedFunction(AStatementEdge statementEdge) {
      final AFunctionCall call = (AFunctionCall) statementEdge.getStatement();
      final AFunctionDeclaration functionDecl = call.getFunctionCallExpression().getDeclaration();

      // If we have a function declaration, it is a normal call to this function
      if (functionDecl != null) {
        // a call to an undefined function
        if (!cfa.getAllFunctionNames().contains(functionDecl.getName())) {
          undefFuncs.put(functionDecl.getName(), functionDecl);
        }
      }
    }

    public Map<String, AFunctionDeclaration> getUndefFuncs() {
      return undefFuncs;
    }
  }

  private class UndefinedFunctionCollectorAlgorithmStatistics implements Statistics {

    UndefinedFunctionCollectorAlgorithmStatistics(CFA pCfa, Path pFile,
        FormulaEncodingOptions pEncodingOptions) {
      cfa = pCfa;
      filePath = pFile;
      encodingOptions = pEncodingOptions;
    }

    private final CFA cfa;
    private final Path filePath;
    private final FormulaEncodingOptions encodingOptions;


    public Map<String, AFunctionDeclaration> collectUndefinedFunctionsRecursively() {
      // 1.Step: get all function calls
      final UndefinedFunctionCallCollector visitor = new UndefinedFunctionCallCollector(cfa);
      for (final FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
        CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(entryNode, visitor);
      }
      return visitor.getUndefFuncs();
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      Map<String, AFunctionDeclaration> undefFuncs = collectUndefinedFunctionsRecursively();
      pOut.println("Undefined functions count: " + undefFuncs.size());
      try (Writer w =
          IO.openOutputFile(filePath, Charset.defaultCharset())) {
        for (Map.Entry<String, AFunctionDeclaration> k : undefFuncs.entrySet()) {
          printFunction(k.getKey(), k.getValue(), w);
        }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write undefined funcs to the file");
      }
    }

    private boolean isSvcompFunction(String name) {
      return svcompFunctionsRegexp.matcher(name).matches()
          || pthreadFunctionsRegexp.matcher(name).matches()
          //|| encodingOptions.isNondetFunction(name)
          || encodingOptions.isMemoryAllocationFunction(name);
      //|| encodingOptions.isExternModelFunction(name);
    }

    private String assume = "__VERIFIER_assume";
    private String nondetPrefix = "__VEFIRIER_nondet_";

    private void printFunction(String name, AFunctionDeclaration f,
        Writer w) throws IOException {
      if (ignoreFunctions.contains(name)
          || isSvcompFunction(name)) {
        logger.log(Level.FINE, " Skip function: " + name);
        w.write("// Skip function: " + name + "\n\n");
      } else {
        w.write("// Function: " + name + "\n");
        w.write("// with type: " + f.getType() + "\n");
        Type rt = f.getType().getReturnType();
        w.write("// with return type: " + rt + "\n");
        StringBuilder buf = new StringBuilder();
        StringBuilder prepend = new StringBuilder();
        boolean b = printType("  ", prepend, buf, (CType) rt);
        if (b) {
          w.write(prepend.toString());

          w.write(getSignature(name, f.getType()) + "{\n");
          w.write(buf.toString());
          w.write("}\n\n");
        } else {
          w.write("// Ignore function: " + name + "\n\n");
        }
      }
    }

    private String getSignature(String name, IAFunctionType type) {
      String res = name + "(";
      int i = 0;
      for (Type pt : type.getParameters()) {
        if (i == 0) {
          res += pt.toASTString("arg" + i);
        } else {
          res += ", " + pt.toASTString("arg" + i);
        }
        i++;
      }
      if (type.takesVarArgs()) {
        if (i != 0) {
          res += ", ";
        }
        res += "...";
      }
      res += ")";
      return type.getReturnType().toASTString(res);
    }

    private final String odmFunctionDecl = "void *" + odmAllocFunction + "(void)";
    private final String assumeFunctionDecl = "void " + assume + "(int)";

    private boolean printType(String indent, StringBuilder prepend, StringBuilder buf, CType rt) {
      boolean ignore = false;
      if (rt instanceof CVoidType) {
        buf.append(indent + "// Void type\n");
        buf.append(indent + "return;\n");
      } else if (rt instanceof CPointerType) {
        buf.append(indent + "// Pointer type\n");
        prepend.append(odmFunctionDecl + ";\n");
        buf.append(indent + "return (" + rt.toASTString("") + ")" + odmAllocFunction + "();\n");
      } else if (rt instanceof CSimpleType) {
        CSimpleType ct = (CSimpleType) rt;
        Pair<String, String> pair = convertType(ct);
        String nondetFunc = nondetPrefix + pair.getSecond();
        prepend.append(pair.getFirst() + " " + nondetFunc + "(void);\n");
        buf.append(indent + "// Simple type\n");
        buf.append(indent + "return " + nondetFunc + "();\n");
      } else if (rt instanceof CCompositeType) {
        buf.append(indent + "// Composite type\n");
        prepend.append(odmFunctionDecl + ";\n");
        buf.append(indent + rt.toASTString("tmp") + " = (" + rt.toASTString("") + ")"
            + odmAllocFunction + "();\n");
        prepend.append(assumeFunctionDecl + ";\n");
        buf.append(indent + assume + "(tmp != 0);\n");
        buf.append(indent + "return *tmp;\n");
      } else if (rt instanceof CTypedefType) {
        buf.append(indent + "// Typedef type\n");
        CTypedefType tt = (CTypedefType) rt;
        CType real = tt.getRealType();
        buf.append(indent + "// Real type: " + real + "\n");
        ignore = !printType(indent, prepend, buf, real);
      } else {
        ignore = true;
      }
      return !ignore;
    }

    //Copied from SV-COMP rules:
    //bool, char, int, float, double, loff_t, long,
    //pchar, pointer, pthread_t, sector_t, short,
    //size_t, u32, uchar, uint, ulong, unsigned, ushort
    private Pair<String, String> convertType(CSimpleType ct) {
      CBasicType bt = ct.getType();
      if (bt == CBasicType.BOOL) {
        return Pair.of("bool", "bool");
      } else if (bt == CBasicType.CHAR) {
        if (ct.isUnsigned()) {
          return Pair.of("unsigned char", "uchar");
        } else {
          return Pair.of("char", "char");
        }
      } else if (bt == CBasicType.DOUBLE) {
        return Pair.of("double", "double");
      } else if (bt == CBasicType.FLOAT) {
        return Pair.of("float", "float");
      } else if (bt == CBasicType.INT
          || bt == CBasicType.UNSPECIFIED) {
        if (ct.isShort()) {
          if (ct.isUnsigned()) {
            return Pair.of("unsigned short", "ushort");
          } else {
            return Pair.of("short", "short");
          }
        } else if (ct.isLong() || ct.isLongLong()) {
          if (ct.isUnsigned()) {
            return Pair.of("unsigned long", "ulong");
          } else {
            return Pair.of("long", "long");
          }
        } else {
          if (ct.isUnsigned()) {
            return Pair.of("unsigned int", "uint");
          } else {
            return Pair.of("int", "int");
          }
        }
      } else {
        throw new RuntimeException("Unknown type " + ct);
      }
    }

    @Override
    public @Nullable String getName() {
      return "UndefinedFunctionCollectorAlgorithm";
    }
  }

  public UndefinedFunctionCollectorAlgorithm(
      CFA pCfa,
      Configuration config,
      LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    FormulaEncodingOptions enc = new FormulaEncodingOptions(config);
    Path file = envPathFile.getPath();
    this.stats =
        new UndefinedFunctionCollectorAlgorithmStatistics(pCfa, file, enc);
    this.logger = pLogger;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    // clear reached set and therefore waitlist to prevent further warnings:
    pReachedSet.clear();
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

}
