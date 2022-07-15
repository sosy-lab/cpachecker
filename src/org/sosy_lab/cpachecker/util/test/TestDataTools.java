// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

public class TestDataTools {

  /**
   * Create a configuration suitable for unit tests (writing output files is disabled).
   *
   * @return A {@link ConfigurationBuilder} which can be further modified and then can be used to
   *     {@link ConfigurationBuilder#build()} a {@link Configuration} object.
   */
  public static ConfigurationBuilder configurationForTest() throws InvalidConfigurationException {
    Configuration typeConverterConfig =
        Configuration.builder().setOption("output.disable", "true").build();
    FileTypeConverter fileTypeConverter = FileTypeConverter.create(typeConverterConfig);
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
    return Configuration.builder().addConverter(FileOption.class, fileTypeConverter);
  }

  public static CIdExpression makeVariable(String varName, CSimpleType varType) {
    FileLocation loc = FileLocation.DUMMY;
    CVariableDeclaration decl =
        new CVariableDeclaration(
            loc, true, CStorageClass.AUTO, varType, varName, varName, varName, null);

    return new CIdExpression(loc, decl);
  }

  public static CFA makeCFA(String... lines) throws ParserException, InterruptedException {
    try {
      return makeCFA(configurationForTest().build(), lines);
    } catch (InvalidConfigurationException e) {
      throw new AssertionError("Default configuration is invalid?");
    }
  }

  public static CFA makeCFA(Configuration config, String... lines)
      throws InvalidConfigurationException, ParserException, InterruptedException {

    CFACreator creator =
        new CFACreator(config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());

    return creator.parseSourceAndCreateCFA(Joiner.on('\n').join(lines));
  }

  /**
   * Convert a given loop-free {@code cfa} to a single {@link PathFormula}.
   *
   * @param ignoreDeclarations Do not include the formula for declarations in the resulting formula.
   *     This can be very convenient if the {@link PathFormula}s from different calls to this method
   *     should be conjoined together.
   * @param initialSSA Starting {@link SSAMap} for the resultant formula.
   * @throws Exception if the given {@code cfa} contains loop.
   */
  public static PathFormula toPathFormula(
      CFA cfa, SSAMap initialSSA, PathFormulaManager pfmgr, boolean ignoreDeclarations)
      throws Exception {
    Map<CFANode, PathFormula> mapping = new HashMap<>(cfa.getAllNodes().size());
    CFANode start = cfa.getMainFunction();

    PathFormula initial =
        pfmgr.makeEmptyPathFormulaWithContext(initialSSA, PointerTargetSet.emptyPointerTargetSet());

    mapping.put(start, initial);
    Deque<CFANode> queue = new ArrayDeque<>();
    queue.add(start);

    while (!queue.isEmpty()) {
      CFANode node = queue.removeLast();
      Preconditions.checkState(!node.isLoopStart(), "Can only work on loop-free fragments");
      PathFormula path = mapping.get(node);

      for (CFAEdge e : CFAUtils.leavingEdges(node)) {
        CFANode toNode = e.getSuccessor();
        PathFormula old = mapping.get(toNode);

        PathFormula n;
        if (ignoreDeclarations
            && e instanceof CDeclarationEdge
            && ((CDeclarationEdge) e).getDeclaration() instanceof CVariableDeclaration) {

          // Skip variable declaration edges.
          n = path;
        } else {
          n = pfmgr.makeAnd(path, e);
        }
        PathFormula out;
        if (old == null) {
          out = n;
        } else {
          out = pfmgr.makeOr(old, n);
        }
        mapping.put(toNode, out);
        queue.add(toNode);
      }
    }

    return mapping.get(cfa.getMainFunction().getExitNode());
  }

  /** Convert a given string to a {@link CFA}, assuming it is a body of a single function. */
  public static CFA toSingleFunctionCFA(CFACreator creator, String... parts)
      throws InvalidConfigurationException, ParserException, InterruptedException {
    return creator.parseSourceAndCreateCFA(getProgram(parts));
  }

  public static CFA toMultiFunctionCFA(CFACreator creator, String... parts)
      throws InvalidConfigurationException, ParserException, InterruptedException {
    return creator.parseSourceAndCreateCFA(Joiner.on('\n').join(parts));
  }

  private static String getProgram(String... parts) {
    return "int main() {" + Joiner.on('\n').join(parts) + "}";
  }

  /**
   * Returns and, if necessary, creates a new empty C or Java program in the given temporary folder.
   */
  public static String getEmptyProgram(TemporaryFolder pTempFolder, boolean isJava)
      throws IOException {
    File tempFile;
    String fileContent;
    String program;
    if (isJava) {
      tempFile = getTempFile(pTempFolder, "Main.java");
      fileContent = "public class Main { public static void main(String... args) {} }";
      program = "Main";
    } else {
      tempFile = getTempFile(pTempFolder, "program.i");
      fileContent = getProgram();
      program = tempFile.toString();
    }
    if (tempFile.createNewFile()) {
      // if the file didn't exist yet, write its content
      IO.writeFile(tempFile.toPath(), StandardCharsets.US_ASCII, fileContent);
    }

    return program;
  }

  /**
   * Returns the file object for the given file name in the given temporary folder. If the described
   * file does not exist, it will <b>not</b> be created.
   */
  private static File getTempFile(TemporaryFolder pTempFolder, String pFileName) {
    return Path.of(pTempFolder.getRoot().toString(), pFileName).toFile();
  }
}
