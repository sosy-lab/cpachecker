/**
 * @ClassName FileOperations
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/7/19 11:14 AM
 * @Version 1.0
 **/
package org.nulist.plugin.util;

import com.grammatech.cs.procedure;
import com.grammatech.cs.result;
import org.nulist.plugin.parser.CFGNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class FileOperations {
    /**
     *@Description TODO
     *@Param [point, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.FileLocation
     **/
    public static FileLocation getLocation(CFGNode node, final String pFileName) throws result {
        assert node != null;
        return new FileLocation(pFileName, 0, 1, node.getFileLineNumber(), node.getFileLineNumber());
    }



    public static FileLocation getLocation(int startLine, final String pFileName) {
        return new FileLocation(pFileName, 0, 1, startLine, 0);
    }

    public static FileLocation getLocation(int startLine, int endLine, final String pFileName) {
        return new FileLocation(pFileName, 0, 1, startLine, endLine);
    }

    public static FileLocation getLocation(procedure function, final String pFileName) throws result{
        assert function!=null;
        return getLocation((int)function.entry_point().file_line().get_second(),
                (int)function.exit_point().file_line().get_second(), pFileName);
    }

    public static String getQualifiedName(String pVarName, String pFuncName) {
        return pFuncName + "::" + pVarName;
    }

    public static String getQualifiedFunctionName(String pFuncName, String pFileName) {
        return pFileName + "::" + pFuncName;
    }

}
