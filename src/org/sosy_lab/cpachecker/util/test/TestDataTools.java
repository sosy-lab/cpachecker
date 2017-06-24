/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.test;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class TestDataTools {

  /**
   * Create a configuration suitable for unit tests
   * (writing output files is disabled).
   * @return A {@link ConfigurationBuilder} which can be further modified and then can be used to {@link ConfigurationBuilder#build()} a {@link Configuration} object.
   */
  public static ConfigurationBuilder configurationForTest() throws InvalidConfigurationException {
    Configuration typeConverterConfig = Configuration.builder()
        .setOption("output.disable", "true")
        .build();
    FileTypeConverter fileTypeConverter = FileTypeConverter.create(typeConverterConfig);
    Configuration.getDefaultConverters().put(
        FileOption.class, fileTypeConverter
    );
    return Configuration.builder()
        .addConverter(FileOption.class, fileTypeConverter);
  }

  private static int dummyNodeCounter = 0;

  private static CFANode newDummyNode() {
    return new CFANode("DUMMY" + dummyNodeCounter++);
  }

  public static final CInitializer INT_ZERO_INITIALIZER = new CInitializerExpression(
      FileLocation.DUMMY,
      CIntegerLiteralExpression.ZERO);

  public static Triple<CDeclarationEdge, CFunctionDeclaration, CFunctionType> makeFunctionDeclaration(
      String pFunctionName,
      CType pFunctionReturnType,
      List<CParameterDeclaration> pParameters) {

    CFunctionType functionType = new CFunctionType(
        false, false, checkNotNull(pFunctionReturnType), ImmutableList.<CType>of(), false);
    CFunctionDeclaration fd = new CFunctionDeclaration(
        FileLocation.DUMMY, functionType, pFunctionName, pParameters);
    CDeclarationEdge declEdge = new CDeclarationEdge(
        "", FileLocation.DUMMY, newDummyNode(), newDummyNode(), fd);

    return Triple.of(declEdge, fd, functionType);
  }

  public static Triple<CDeclarationEdge, CVariableDeclaration, CIdExpression> makeDeclaration(
      String varName, CType varType, @Nullable CInitializer initializer) {
    final FileLocation loc = FileLocation.DUMMY;
    final CVariableDeclaration decl = new CVariableDeclaration(
        loc, true, CStorageClass.AUTO, varType, varName, varName, varName, initializer);

    return Triple.of(
        new CDeclarationEdge(
            String.format("%s %s", "dummy", varName),
            FileLocation.DUMMY,
            newDummyNode(),
            newDummyNode(),
            decl),
        decl,
        new CIdExpression(
            FileLocation.DUMMY,
            decl));
  }

  public static CFAEdge makeBlankEdge(String pDescription) {
    return new BlankEdge("", FileLocation.DUMMY, newDummyNode(), newDummyNode(), pDescription);
  }

  public static Pair<CFAEdge, CExpressionAssignmentStatement> makeAssignment(CLeftHandSide pLhs, CExpression pRhs) {
    CExpressionAssignmentStatement stmt = new CExpressionAssignmentStatement(
        FileLocation.DUMMY,
        pLhs,
        pRhs);

    CFAEdge edge = new CStatementEdge(
        "dummy := rhs",
        stmt,
        FileLocation.DUMMY,
        newDummyNode(),
        newDummyNode());

    return Pair.of(edge, stmt);
  }

  public static CIdExpression makeVariable(String varName, CSimpleType varType) {
    FileLocation loc = FileLocation.DUMMY;
    CVariableDeclaration decl = new CVariableDeclaration(
        loc, true, CStorageClass.AUTO, varType, varName, varName, varName, null);

    return new CIdExpression(loc, decl);
  }

  public static Pair<CAssumeEdge, CExpression> makeNegatedAssume(CExpression pAssumeExr) {
    CAssumeEdge assumeEdge = new CAssumeEdge(
        "dummyassume",
        FileLocation.DUMMY,
        newDummyNode(),
        newDummyNode(),
        pAssumeExr,
        false);

    return Pair.of(assumeEdge, pAssumeExr);
  }

  public static Pair<CAssumeEdge, CExpression> makeAssume(CExpression pAssumeExr, CFANode pPred, CFANode pSucc) {
    CAssumeEdge assumeEdge = new CAssumeEdge(
        "dummyassume",
        FileLocation.DUMMY,
        pPred,
        pSucc,
        pAssumeExr,
        true);

    return Pair.of(assumeEdge, pAssumeExr);
  }

  public static Pair<CAssumeEdge, CExpression> makeAssume(CExpression pAssumeExr) {
    return makeAssume(pAssumeExr, newDummyNode(), newDummyNode());
  }

  public static CFA makeCFA(String... lines)
      throws IOException, ParserException, InterruptedException {
    try {
      return makeCFA(configurationForTest().build(), lines);
    } catch (InvalidConfigurationException e) {
      throw new AssertionError("Default configuration is invalid?");
    }
  }

  public static CFA makeCFA(Configuration config, String... lines)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    CFACreator creator =
        new CFACreator(config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());

    return creator.parseSourceAndCreateCFA(Joiner.on('\n').join(lines));
  }

  /**
   * Convert a given loop-free {@code cfa} to a single {@link PathFormula}.
   *
   * @param ignoreDeclarations Do not include the formula for declarations
   * in the resulting formula.
   * This can be very convenient if the {@link PathFormula}s from different
   * calls to this method should be conjoined together.
   *
   * @param initialSSA Starting {@link SSAMap} for the resultant formula.
   *
   * @throws Exception if the given {@code cfa} contains loop.
   */
  public static PathFormula toPathFormula(
      CFA cfa,
      SSAMap initialSSA,
      FormulaManagerView fmgr,
      PathFormulaManager pfmgr,
      boolean ignoreDeclarations
      ) throws Exception {
    Map<CFANode, PathFormula> mapping = new HashMap<>(cfa.getAllNodes().size());
    CFANode start = cfa.getMainFunction();

    PathFormula initial = new PathFormula(
        fmgr.getBooleanFormulaManager().makeTrue(), initialSSA,
        PointerTargetSet.emptyPointerTargetSet(),
        0
    );

    mapping.put(start, initial);
    Deque<CFANode> queue = new LinkedList<>();
    queue.add(start);

    while (!queue.isEmpty()) {
      CFANode node = queue.removeLast();
      Preconditions.checkState(!node.isLoopStart(),
          "Can only work on loop-free fragments");
      PathFormula path = mapping.get(node);

      for (CFAEdge e : CFAUtils.leavingEdges(node)) {
        CFANode toNode = e.getSuccessor();
        PathFormula old = mapping.get(toNode);

        PathFormula n;
        if (ignoreDeclarations &&
            e instanceof CDeclarationEdge &&
            ((CDeclarationEdge) e).getDeclaration() instanceof CVariableDeclaration) {

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
          out = out.updateFormula(fmgr.simplify(out.getFormula()));
        }
        mapping.put(toNode, out);
        queue.add(toNode);
      }
    }

    PathFormula out = mapping.get(cfa.getMainFunction().getExitNode());
    out = out.updateFormula(fmgr.simplify(out.getFormula()));
    return out;
  }

  /**
   * Convert a given string to a {@link CFA},
   * assuming it is a body of a single function.
   */
  public static CFA toSingleFunctionCFA(CFACreator creator, String... parts)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {
    return creator.parseSourceAndCreateCFA(getProgram(parts));
  }

  public static CFA toMultiFunctionCFA(CFACreator creator, String... parts)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {
    return creator.parseSourceAndCreateCFA(Joiner.on('\n').join(parts));
  }

  private static String getProgram(String... parts) {
    return "int main() {" +  Joiner.on('\n').join(parts) + "}";
  }
}
