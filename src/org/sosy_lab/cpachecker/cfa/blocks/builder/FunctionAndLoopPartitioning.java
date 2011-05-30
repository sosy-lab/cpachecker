package org.sosy_lab.cpachecker.cfa.blocks.builder;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;


/**
 * <code>PartitioningHeuristic</code> that creates blocks for each loop- and function-body. 
 * @author dwonisch
 *
 */
public class FunctionAndLoopPartitioning extends LoopPartitioning {
   
  public FunctionAndLoopPartitioning(LogManager pLogger) {
    super(pLogger);   
  }

  @Override
  protected boolean shouldBeCached(CFANode pNode) {
    return pNode instanceof CFAFunctionDefinitionNode || super.shouldBeCached(pNode);
  }  
}
