package cpaplugin.cfa.objectmodel.c;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier;

import cpaplugin.cfa.CFAGenerationRuntimeException;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFAVariableInfo;
import cpaplugin.common.TypeNames;

public class FunctionDefinitionNode extends CFAFunctionDefinitionNode
{
    private IASTFunctionDefinition functionDefinition;
    
    public FunctionDefinitionNode (int lineNumber, IASTFunctionDefinition functionDefinition)
    {
        super (lineNumber, functionDefinition.getDeclarator ().getName ().toString ());
        this.functionDefinition = functionDefinition;
    }

    public List<String> getFunctionParameterNames ()
    {
        IASTFunctionDeclarator decl = functionDefinition.getDeclarator ();
        
        List<String> parameterNames = new ArrayList<String> ();
        if (decl instanceof IASTStandardFunctionDeclarator)
        {
            IASTStandardFunctionDeclarator sdecl = (IASTStandardFunctionDeclarator) decl;
            IASTParameterDeclaration [] params = sdecl.getParameters ();

            for (int i = 0; i < params.length; i++)
            {
                IASTParameterDeclaration param = params[i];               
                
                // Get what we need from the declarator
                String name = param.getDeclarator ().getName ().toString ();
                
                if (!name.isEmpty ())
                    parameterNames.add (name);
            }
        }
        
        return parameterNames;
    }
    
    /*
     * NOTE: This function is not used presently, and is here as part of an un-submitted experiment
     *  to make the CPA algorithm completely language independent.  However, in the end I decided it
     *  better to save effort now, and to only create my object model if it is ever desired to support
     *  languages other than C for this CPA Plugin.
     */
    public List<CFAVariableInfo> getFunctionParameters ()
    {
        IASTFunctionDeclarator decl = functionDefinition.getDeclarator ();
                
        if (decl instanceof IASTStandardFunctionDeclarator)
        {
            IASTStandardFunctionDeclarator sdecl = (IASTStandardFunctionDeclarator) decl;
            IASTParameterDeclaration [] params = sdecl.getParameters ();
            
            List<CFAVariableInfo> parameterInfo = new ArrayList<CFAVariableInfo> ();
            
            for (int i = 0; i < params.length; i++)
            {
                IASTParameterDeclaration param = params[i];               
                IASTDeclSpecifier specifier = param.getDeclSpecifier ();
                
                // Get what we need from the declarator
                String name = param.getDeclarator ().getName ().toString ();
                int indirectionLevels = param.getDeclarator ().getPointerOperators ().length;
                
                // Create the CFAVariableInfo
                CFAVariableInfo info = new CFAVariableInfo (name);
                parameterInfo.add (info);
                
                info.setIndirectionLevel (indirectionLevels);
                
                // Now get what we need from the decl specifier
                info.setIsConst (specifier.isConst ());
                info.setIsVolatile (specifier.isVolatile ());
                if (specifier instanceof ICASTSimpleDeclSpecifier)
                {
                    ICASTSimpleDeclSpecifier simpleDecl = (ICASTSimpleDeclSpecifier) specifier;
                    
                    info.setIsLong (simpleDecl.isLong ());
                    info.setIsLongLong (simpleDecl.isLongLong ());
                    info.setIsShort (simpleDecl.isShort ());
                    info.setIsUnsigned (simpleDecl.isUnsigned ());
                    
                    int decltype = simpleDecl.getType ();
                    String type = TypeNames.UnknownStr;
                    switch (decltype)                    
                    {
                    case ICASTSimpleDeclSpecifier.t_Bool:
                        type = TypeNames.BoolStr;
                        break;
                    case ICASTSimpleDeclSpecifier.t_char:
                        type = TypeNames.CharStr;
                        break;
                    case ICASTSimpleDeclSpecifier.t_double:
                        type = TypeNames.DoubleStr;
                        break;
                    case ICASTSimpleDeclSpecifier.t_float:
                        type = TypeNames.FloatStr;
                        break;
                    case ICASTSimpleDeclSpecifier.t_int:
                        type = TypeNames.IntStr;
                        break;
                    case ICASTSimpleDeclSpecifier.t_void:
                        type = TypeNames.VoidStr;
                        break;
                    }
                    
                    info.setType (type);
                }
                else if (specifier instanceof IASTNamedTypeSpecifier)
                {
                    IASTNamedTypeSpecifier namedDecl = (IASTNamedTypeSpecifier) specifier;                   
                    info.setType (namedDecl.getName ().toString ());
                }
                else
                {
                    throw new CFAGenerationRuntimeException ("Support for Declaration Specifier: " + specifier.getClass ().getName () + " not implemented");
                }
                
            }
        }
        
        return null;
    }

}
