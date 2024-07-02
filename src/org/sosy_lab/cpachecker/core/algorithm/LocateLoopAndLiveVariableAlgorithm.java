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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This algorithm extracts loop information from a C program.
 * 
 * <p>
 * There are two kinds of loops, which are classified not by their syntax but by their
 * instrumentation processes. The first is normal loops(for, while, do-while, and goto loops). The
 * second is recursion.
 */
public class LocateLoopAndLiveVariableAlgorithm implements Algorithm {
  private final CFA cfa;
  private final LogManager logger;
  private final CProgramScope cProgramScope;

  public LocateLoopAndLiveVariableAlgorithm(CFA pCfa, LogManager pLogger) {
    if (pCfa.getLoopStructure().orElseThrow().getAllLoops().isEmpty()
        && LoopStructure.getRecursions(pCfa).isEmpty()) {
      throw new IllegalArgumentException("Program does not contain loops!");
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

      for (NormalLoopInfo loopInfo : getAllNormalLoopInfos()) {
        allLoopInfos.append(
            String.format(
                "Loop    %-4d    %s%n",
                loopInfo.loopLocation(),
                loopInfo.liveVariablesAndTypes()));
      }

      for (RecursionInfo recursionInfo : getAllRecursionInfos()) {
        allLoopInfos.append(
            String.format(
                "Recursion    %-10s %-4d    %-6s    %s%n",
                recursionInfo.FunctionName(),
                recursionInfo.locationOfDefinition(),
                recursionInfo.locationOfRecursiveCalls(),
                recursionInfo.parameters()));
      }

      writer.append(allLoopInfos.toString());
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "The creation of file AllLoopInfos.txt failed!");
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private Set<NormalLoopInfo> getAllNormalLoopInfos() {
    Set<NormalLoopInfo> allNormalLoopInfos = new HashSet<>();

    for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      // Determine loop locations. There may be more than one, as some loops have multiple
      // loop heads, e.g., goto loop.
      List<Integer> loopLocations = new ArrayList<>();
      for (CFANode cfaNode : loop.getLoopHeads()) {
        loopLocations.add(
            CFAUtils.allEnteringEdges(cfaNode)
                .first()
                .get()
                .getFileLocation()
                .getStartingLineInOrigin());
      }

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
      liveVariables
          .removeIf(e -> e.contains("::") && e.split("::")[1].startsWith("__CPAchecker_TMP_"));

      // Determine type of each variable
      for (String variable : liveVariables) {
        String type = cProgramScope.lookupVariable(variable).getType().toString();
        liveVariablesAndTypes.put(
            variable.contains("::") ? variable.split("::")[1] : variable,
            type.startsWith("(") ? type.substring(1, type.length() - 2) + "*" : type);
      }

      for (Integer loopLocation : loopLocations) {
        allNormalLoopInfos.add(new NormalLoopInfo(loopLocation, liveVariablesAndTypes));
      }
    }

    return allNormalLoopInfos;
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
      cFunctionCallStatement.getFunctionCallExpression()
          .getParameterExpressions()
          .forEach(e -> CFAUtils.getVariableNamesOfExpression(e).forEach(n -> variables.add(n)));

    } else if (aAstNode instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement =
          (CFunctionCallAssignmentStatement) aAstNode;

      CLeftHandSide cLeftHandSide = cFunctionCallAssignmentStatement.getLeftHandSide();
      CFAUtils.getVariableNamesOfExpression(cLeftHandSide).forEach(e -> variables.add(e));

      CFunctionCallExpression cRightHandSide = cFunctionCallAssignmentStatement.getRightHandSide();
      cRightHandSide.getParameterExpressions()
          .forEach(e -> CFAUtils.getVariableNamesOfExpression(e).forEach(n -> variables.add(n)));
    }

    return variables;
  }

  private Set<RecursionInfo> getAllRecursionInfos() {
    Set<RecursionInfo> allRecursionInfos = new HashSet<>();

    for (Loop recursion : LoopStructure.getRecursions(cfa)) {
      String functionName = "";
      int locationOfDefinition;
      Set<Integer> locationOfRecursiveCalls = new HashSet<>();
      List<String> parameters = new ArrayList<>();

      // Determine function name
      CFAEdge firstIncomingEdge = recursion.getIncomingEdges().stream().findFirst().orElseThrow();
      AAstNode astNode = firstIncomingEdge.getRawAST().orElseThrow();
      if (astNode instanceof CFunctionCallStatement) {
        CFunctionCallStatement cFunctionCallStatement = (CFunctionCallStatement) astNode;
        functionName =
            cFunctionCallStatement.getFunctionCallExpression()
                .getFunctionNameExpression()
                .toString();
      } else if (astNode instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement =
            (CFunctionCallAssignmentStatement) astNode;
        CFunctionCallExpression cRightHandSide =
            cFunctionCallAssignmentStatement.getRightHandSide();
        functionName = cRightHandSide.getFunctionNameExpression().toString();
      }

      // Determine location Of definition
      locationOfDefinition =
          cfa.getAllFunctions().get(functionName).getFileLocation().getStartingLineInOrigin();

      // Determine location of recursive calls
      for (CFAEdge cfaEdge : recursion.getInnerLoopEdges()) {
        if (cfaEdge.getRawStatement().contains(functionName)) {
          locationOfRecursiveCalls.add(cfaEdge.getFileLocation().getStartingLineInOrigin());
        }
      }

      // Determine parameters
      cfa.getAllFunctions()
          .get(functionName)
          .getFunctionParameters()
          .forEach(e -> parameters.add(e.toString()));

      allRecursionInfos.add(
          new RecursionInfo(
              functionName,
              locationOfDefinition,
              locationOfRecursiveCalls,
              parameters));
    }

    return allRecursionInfos;
  }
}


/**
 * Represents a container for normal loop information(for, while, do-while, and goto loop).
 * 
 * @param loopLocation          the line number where the loop is located
 * @param liveVariablesAndTypes the mapping from variable names used, but not declared, in the loop
 *                              to their types
 */
record NormalLoopInfo(int loopLocation, Map<String, String> liveVariablesAndTypes) {
  NormalLoopInfo(int loopLocation, Map<String, String> liveVariablesAndTypes) {
    this.loopLocation = loopLocation;
    this.liveVariablesAndTypes = liveVariablesAndTypes;
  }
}


/**
 * Represents a container for recursion information.
 * 
 * @param FunctionName             the name of the function
 * @param locationOfDefinition     the line number where the function is defined
 * @param locationOfRecursiveCalls a set of line numbers where the recursive calls occur
 * @param parameters               the function's parameters(type + name)
 */
record RecursionInfo(String FunctionName, int locationOfDefinition,
    Set<Integer> locationOfRecursiveCalls, List<String> parameters) {
  RecursionInfo(
      String FunctionName,
      int locationOfDefinition,
      Set<Integer> locationOfRecursiveCalls,
      List<String> parameters) {
    this.FunctionName = FunctionName;
    this.locationOfDefinition = locationOfDefinition;
    this.locationOfRecursiveCalls = locationOfRecursiveCalls;
    this.parameters = parameters;
  }
}
