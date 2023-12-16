// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.common.log.LogManager;

public class NoOverflowAlgorithm implements Algorithm{
  private final LogManager logger;
  private final CFA cfa;
  private List<StatementInformation> information = new ArrayList<>();
  private List<Integer> lineNumberList = new ArrayList<>();

  public NoOverflowAlgorithm(
      final LogManager pLogger,
      final CFA pCFA) {
    logger = pLogger;
    cfa = pCFA;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    // Optional<ASTStructure> optionalAST = cfa.getASTStructure();
    cfa.getASTStructure();

    for (CFAEdge edge : cfa.edges()) {
      Optional<AAstNode> optionalAAstNode = edge.getRawAST();
      if (optionalAAstNode.isPresent()){
        AAstNode astNode = optionalAAstNode.get();

        // statement
        if (astNode instanceof CAssignment) {

          // right part of statement
          ARightHandSide rightHandSide = ((CAssignment) astNode).getRightHandSide();

          // right part contains binary operations
          // it will transfer x++ to (x+1) and x-- to (x-1)
          if (rightHandSide instanceof CBinaryExpression) {

            // LineNumber
            int lineNumber = edge.getLineNumber();
            if (lineNumberList.contains(lineNumber)) {
              continue;
            }
            lineNumberList.add(lineNumber);

            List<List<String>> expressionList = new ArrayList<>();
            getInformation((CBinaryExpression)rightHandSide, expressionList);
            information.add(new StatementInformation(lineNumber, expressionList));

          }
        }
      }
    }

    String path = "./output/CProgramInformation.txt";
    try (BufferedWriter writer = Files.newBufferedWriter(Path.of(path), StandardCharsets.UTF_8)) {
      for (StatementInformation s : information) {
        writer.write(s.listToString());
        logger.log(Level.INFO, s.listToString());
      }
      writer.write('\n');
      writer.write("END");
    } catch (IOException pE) {
      throw new RuntimeException(pE);
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  public void getInformation(CBinaryExpression operand, List<List<String>> expressionList) {
    List<String> binaryExpression = new ArrayList<>();

    CExpression left = operand.getOperand1();
    CExpression right = operand.getOperand2();
    binaryExpression.add(left.toASTString());
    binaryExpression.add(right.toASTString());
    String operator = String.valueOf(operand.getOperator());
    binaryExpression.add(operator);
    expressionList.add(binaryExpression);

    if (left instanceof CBinaryExpression) {
      getInformation((CBinaryExpression) left, expressionList);
    }
    if (right instanceof CBinaryExpression) {
      getInformation((CBinaryExpression) right, expressionList);
    }
  }

  public static class StatementInformation implements Serializable {
    private int codeLineNumber;
    private int numOfOperator;
    private List<List<String>> expressionList;

    public StatementInformation(int pCodeLineNumber, List<List<String>> pExpressionList) {
      codeLineNumber = pCodeLineNumber;
      numOfOperator = pExpressionList.size();
      expressionList = pExpressionList;
    }

    public String listToString(){
      StringBuilder resultBuilder = new StringBuilder();
      resultBuilder.append(codeLineNumber);
      resultBuilder.append("\n");
      resultBuilder.append(numOfOperator);
      resultBuilder.append("\n");
      for (List<String> innerList : expressionList) {
        for (String element : innerList) {
          resultBuilder.append(element).append(" ");
        }
        resultBuilder.append("\n");
      }
      return resultBuilder.toString();
    }
  }
}

