package org.nulist.plugin.model.message;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * @ClassName MessageTypeConvert
 * @Description UE, ENB and MME have same message types but in different type name, especially NAS message between UE and MME
 * @Author Yinbo Yu
 * @Date 4/8/19 6:43 PM
 * @Version 1.0
 **/
public class MessageTypeConvert {
    //message id


    //S1AP message convert
    public static void messagetConvert(CType originalType, CType targetType, CVariableDeclaration variableDeclaration){

    }
}
