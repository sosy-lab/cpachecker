package cfa.objectmodel;

import cmdline.CPAMain;
import cfa.objectmodel.CFAExitNode;
import cfa.objectmodel.CFANode;


public abstract class CFAFunctionDefinitionNode extends CFANode
{
    private String functionName;
    private String containingFileLocation;
    // Check if call edges are added in the second pass
    private CFAExitNode exitNode;
    
    public CFAFunctionDefinitionNode (int lineNumber, String functionName, String containingFileLocation)
    {
        super (lineNumber);
        this.functionName = functionName;
        this.containingFileLocation = containingFileLocation;
    }
    
    public String getFunctionName ()
    {
        return this.functionName;
    }
    
    public void setFunctionName (String s)
    {
    	this.functionName = s;
    }
    
    public CFANode getExitNode ()
    {
        return this.exitNode;
    }
    
    public void setExitNode (CFAExitNode en)
    {
    	this.exitNode = en;
    }

    public String getContainingFileName(){
    	String filePath = containingFileLocation;
		String[] pathArray = filePath.split(CPAMain.cpaConfig.getProperty("analysis.programs"));
		String fileName = pathArray[1];
		fileName = fileName.replace("/", ".");
		// TODO we assume the file name ends with .c or .h
		fileName = fileName.substring(0, fileName.length()-2);
		return fileName;
    }

}
