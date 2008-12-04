/**
 * 
 */
package cpa.scoperestrictionautomaton.label;

//import org.eclipse.cdt.core.dom.ast.IASTFileLocation;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
//import cfa.objectmodel.c.AssumeEdge;

/**
 * @author holzera
 *
 */
public class LineNumberLabel implements Label<CFAEdge> {

  // TODO: Use mFileName for matching in Nodes
  //private String mFileName;
	private int mLineNumber;
	
	public LineNumberLabel(String pFileName, int pLineNumber) {
	  if (pFileName == null) {
	    throw new IllegalArgumentException();
	  }
	  
	  //mFileName = pFileName;
		mLineNumber = pLineNumber;
	}
	
	/*private boolean matches(IASTFileLocation pFileLocation) {
	  if (!mFileName.equals(pFileLocation.getFileName())) {
	    return false;
	  }
	  
	  return (pFileLocation.getStartingLineNumber() <= mLineNumber && mLineNumber <= pFileLocation.getEndingLineNumber());
	}*/
	
	@Override
	public boolean matches(CFAEdge pEdge) {
		CFANode successor = pEdge.getSuccessor();
		CFANode predecessor = pEdge.getPredecessor();
		
		//TODO: Matching line number in edge label
		/*switch (pEdge.getEdgeType()) {
		case BlankEdge: {
		  BlankEdge mBlankEdge = (BlankEdge)pEdge;
		  
		  //mBlankEdge.
		  
		  AssumeEdge assumeEdge = (AssumeEdge)pEdge;
		  
		  assumeEdge.getExpression().
		  
		  assumeEdge.getExpression().getFileLocation().getStartingLineNumber()
		}
		}*/
		
		return (mLineNumber == predecessor.getLineNumber() || mLineNumber == successor.getLineNumber());
		
		//return (mLineNumber == successor.getLineNumber() && mLineNumber != predecessor.getLineNumber());
	}

}
