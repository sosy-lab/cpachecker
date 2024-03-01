package org.sosy_lab.cpachecker.core.algorithm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This algorithm extracts loop information from a C program.
 * 
 * <p> Specifically, we derive from each loop its loop location and the names of all variables used
 * except those declared inside the loop. A loop location refers to the line number where the loop 
 * head(the keyword while, the condition, and the opening bracket) is located.
 */
public class LocateLoopAndLiveVariableAlgorithm implements Algorithm {
  private final CFA cfa;
  private final LogManager logger;
  private final CProgramScope cProgramScope;

  public LocateLoopAndLiveVariableAlgorithm(CFA pCfa, LogManager pLogger) {
    if (!LoopStructure.getRecursions(pCfa).isEmpty()) {
      throw new IllegalArgumentException("Program should not have recursion!");
    }
    cfa = pCfa;
    logger = pLogger;
    cProgramScope = new CProgramScope(pCfa, pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // Output the collected loop information to a file
    try (BufferedWriter writer =
        Files.newBufferedWriter(Paths.get("output/AllLoopInfos.txt"), StandardCharsets.UTF_8)) {
      StringBuilder allLoopInfos = new StringBuilder();
      for (LoopInfo loopInfo : getAllLoopInfos()) {
        allLoopInfos.append(
            String.format(
                "%-4d    %s%n", loopInfo.loopLocation(), loopInfo.liveVariablesAndTypes()));
      }

      writer.append(allLoopInfos.toString());
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "The creation of file AllLoopInfos.txt failed!");
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private List<LoopInfo> getAllLoopInfos() {
    List<LoopInfo> allLoopInfos = new ArrayList<>();

    for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      // Determine loop location
      // Since we assume that C programs only contain while loops as their loop structures,
      // each while loop can have only one incoming edge connecting a non-loop node and
      // the keyword "while" is located on the edge.
      int loopLocation =
          loop.getIncomingEdges().stream()
              .mapToInt(e -> e.getFileLocation().getStartingLineInOrigin())
              .findAny()
              .orElseThrow();

      // Determine the names of all variables used except those declared inside the loop
      Set<String> liveVariables = new HashSet<>();
      Set<String> variablesDeclaredInsideLoop = new HashSet<>();
      Map<String, String> liveVariablesAndTypes = new HashMap<>();
      for (CFAEdge cfaEdge : loop.getInnerLoopEdges()) {
        if (cfaEdge.getRawAST().isPresent()) {
          AAstNode aAstNode = cfaEdge.getRawAST().orElseThrow();
          if (aAstNode instanceof CSimpleDeclaration) {
            variablesDeclaredInsideLoop.add(((CSimpleDeclaration) aAstNode).getQualifiedName());
          } else {
            liveVariables.addAll(getVariablesFromAAstNode(cfaEdge.getRawAST().orElseThrow()));
          }
        }
      }
      liveVariables.removeAll(variablesDeclaredInsideLoop);
      liveVariables.removeIf(
          e -> e.contains("::") && e.split("::")[1].startsWith("__CPAchecker_TMP_"));

      // Determine type of each variable
      for (String variable : liveVariables) {
        String type = cProgramScope.lookupVariable(variable).getType().toString();
        liveVariablesAndTypes.put(
            variable.contains("::") ? variable.split("::")[1] : variable,
            type.startsWith("(") ? type.substring(1, type.length() - 2) + "*" : type);
      }

      allLoopInfos.add(new LoopInfo(loopLocation, liveVariablesAndTypes));
    }

    return allLoopInfos;
  }

  private Set<String> getVariablesFromAAstNode(AAstNode aAstNode) {
    Set<String> variables = new HashSet<>();

    if (aAstNode instanceof CExpression) {
      CFAUtils.getVariableNamesOfExpression(((CExpression) aAstNode))
          .forEach(e -> variables.add(e));

    } else if (aAstNode instanceof CExpressionStatement) {
      CExpression cExpression = ((CExpressionStatement) aAstNode).getExpression();
      CFAUtils.getVariableNamesOfExpression(cExpression).forEach(e -> variables.add(e));

    } else if (aAstNode instanceof CExpressionAssignmentStatement) {
      CLeftHandSide cLeftHandSide = ((CExpressionAssignmentStatement) aAstNode).getLeftHandSide();
      CFAUtils.getVariableNamesOfExpression(cLeftHandSide).forEach(e -> variables.add(e));

      CExpression cRightHandSide = ((CExpressionAssignmentStatement) aAstNode).getRightHandSide();
      CFAUtils.getVariableNamesOfExpression(cRightHandSide).forEach(e -> variables.add(e));

    } else if (aAstNode instanceof CFunctionCallStatement) {
      CFunctionCallStatement cFunctionCallStatement = (CFunctionCallStatement) aAstNode;
      cFunctionCallStatement
          .getFunctionCallExpression()
          .getParameterExpressions()
          .forEach(e -> CFAUtils.getVariableNamesOfExpression(e).forEach(n -> variables.add(n)));

    } else if (aAstNode instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement =
          (CFunctionCallAssignmentStatement) aAstNode;

      CLeftHandSide cLeftHandSide = cFunctionCallAssignmentStatement.getLeftHandSide();
      CFAUtils.getVariableNamesOfExpression(cLeftHandSide).forEach(e -> variables.add(e));

      CFunctionCallExpression cRightHandSide = cFunctionCallAssignmentStatement.getRightHandSide();
      cRightHandSide
          .getParameterExpressions()
          .forEach(e -> CFAUtils.getVariableNamesOfExpression(e).forEach(n -> variables.add(n)));
    }

    return variables;
  }
}

/**
 * Represents a container for loop information, including a loop location and a mapping from
 * variable names used in the loop to their types.
 */
record LoopInfo(int loopLocation, Map<String, String> liveVariablesAndTypes) {
  LoopInfo(int loopLocation, Map<String, String> liveVariablesAndTypes) {
    this.loopLocation = loopLocation;
    this.liveVariablesAndTypes = liveVariablesAndTypes;
  }
}