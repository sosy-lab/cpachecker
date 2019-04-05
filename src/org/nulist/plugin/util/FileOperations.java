/**
 * @ClassName FileOperations
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/7/19 11:14 AM
 * @Version 1.0
 **/
package org.nulist.plugin.util;

import com.grammatech.cs.point;
import com.grammatech.cs.procedure;
import com.grammatech.cs.result;
import org.nulist.plugin.CSurfPlugin;
import org.nulist.plugin.parser.CFGNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import static org.nulist.plugin.parser.CFGNode.getFileLineNumber;

public class FileOperations {
    /**
     *@Description TODO
     *@Param [point, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.FileLocation
     **/
    public static FileLocation getLocation(point node, final String pFileName){
        assert node != null;
        try {
            return new FileLocation(pFileName, 0, 1, getFileLineNumber(node), getFileLineNumber(node));
        }catch (result r){
            return null;
        }
    }

    public static FileLocation getLocation(point node) throws result {
        assert node != null;
        String pFileName = node.file_line().get_first().name();
        return new FileLocation(pFileName, 0, 1, getFileLineNumber(node), getFileLineNumber(node));
    }


    public static FileLocation getLocation(int startLine, final String pFileName) {
        return new FileLocation(pFileName, 0, 1, startLine, startLine);
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


    public static Map<Integer, String> readMCCMNCList(){
        Map<Integer, String> mccMNCMap = new HashMap<>();
        String path =CSurfPlugin.class.getResource("").getPath();
        path = path.replace("bin/org/nulist/plugin/","libmodels/mcc_mnc_list.txt");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(path)));

            String in;
            while ((in=reader.readLine())!=null){
                int mcc = Integer.valueOf(in.split(",")[0]);
                String mnc = in.split(",")[1];
                mccMNCMap.put(mcc,mnc);
            }
            reader.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        return mccMNCMap;
    }

}
