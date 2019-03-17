/**
 * @ClassName CFGNode
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 10:05 AM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;

import static org.nulist.plugin.parser.CFGAST.isInitializationExpression;
import static org.nulist.plugin.parser.CFGAST.normalizingVariableName;

public class CFGNode {

    public final static String ACTUAL_IN = "actual-in";
    public final static String ACTUAL_OUT = "actual-out";
    public final static String GLOBAL_ACTUAL_IN = "global-actual-in";
    public final static String GLOBAL_ACTUAL_OUT = "global-actual-out";
    public final static String FORMAL_IN = "formal-in";
    public final static String FORMAL_OUT = "formal-out";
    public final static String GLOBAL_FORMAL_IN = "global-formal-in";
    public final static String GLOBAL_FORMAL_OUT = "global-formal-out";
    public final static String VERTEX_KIND_NULL = "reserved-000";
    public final static String CALLPOST_VERTEX = "reserved-002";
    public final static String END_VERTEX = "reserved-003";
    public final static String USER_DEFINED_VERTEX = "reserved-004";
    public final static String CALL_SITE = "call-site";
    public final static String INDIRECT_CALL = "indirect-call";
    public final static String ENTRY = "entry";
    public final static String BODY = "body";
    public final static String EXIT = "exit";
    public final static String DECLARATION = "declaration";
    public final static String VARIABLE_INITIALIZATION = "variable-initialization";
    public final static String CONTROL_POINT = "control-point";
    public final static String JUMP = "jump";
    public final static String LABEL = "label";
    public final static String SWITCH_CASE = "switch-case";
    public final static String RETURN = "return";
    public final static String EXPRESSION = "expression";
    public final static String UNAVAILABLE = "unavailable";
    public final static String AUXILIARY = "auxiliary";
    public final static String PHI = "phi";
    public final static String PI = "pi";
    public final static String NORMAL_EXIT = "normal-exit";
    public final static String EXCEPTIONAL_EXIT = "exceptional-exit";
    public final static String NORMAL_RETURN = "normal-return";
    public final static String EXCEPTIONAL_RETURN = "exceptional-return";
    public final static String HAMMOCK_EXIT = "hammock-exit";
    public final static String HAMMOCK_HEADER = "hammock-header";
   
    
    public static String getKindName(point point) throws result{
        return point.get_kind().name();
    }

    public static boolean isFormal_In(point point) throws result{
        return point.get_kind().equals(point_kind.getFORMAL_IN());
    }

    public static boolean isFormal_Out(point point) throws result{
        return point.get_kind().equals(point_kind.getFORMAL_OUT());
    }

    public static boolean isGlobalFormal_In(point point) throws result{
        return point.get_kind().equals(point_kind.getGLOBAL_FORMAL_IN());
    }

    public static boolean isGlobalFormal_Out(point point) throws result{
        return point.get_kind().equals(point_kind.getGLOBAL_FORMAL_OUT());
    }

    public static boolean isActual_In(point point) throws result{
        return point.get_kind().equals(point_kind.getACTUAL_IN());
    }

    public static boolean isActual_Out(point point) throws result{
        return point.get_kind().equals(point_kind.getACTUAL_OUT());
    }

    public static boolean isGlobalActual_In(point point) throws result{
        return point.get_kind().equals(point_kind.getGLOBAL_ACTUAL_IN());
    }

    public static boolean isGlobalActual_Out(point point) throws result{
        return point.get_kind().equals(point_kind.getGLOBAL_ACTUAL_OUT());
    }

    public static boolean isControl_Point(point point) throws result{
        return point.get_kind().equals(point_kind.getCONTROL_POINT());
    }

    public static boolean isCall_Site(point point) throws result{
        return point.get_kind().equals(point_kind.getCALL_SITE());
    }

    public static boolean isFunctionEntry(point point) throws result{
        return point.get_kind().equals(point_kind.getENTRY());
    }

    public static boolean isIndirect_Call(point point) throws result{
        return point.get_kind().equals(point_kind.getINDIRECT_CALL());
    }

    public static boolean isFunctionBody(point point) throws result{
        return point.get_kind().equals(point_kind.getBODY());
    }

    public static boolean isFunctionExit(point point) throws result{
        return point.get_kind().equals(point_kind.getEXIT());
    }

    public static boolean isDeclaration(point point) throws result{
        return point.get_kind().equals(point_kind.getDECLARATION());
    }

    public static boolean isJump(point point) throws result{
        return point.get_kind().equals(point_kind.getJUMP());
    }

    public static boolean isLabel(point point) throws result{
        return point.get_kind().equals(point_kind.getLABEL());
    }

    public static boolean isSwitchCase(point point) throws result{
        return point.get_kind().equals(point_kind.getSWITCH_CASE());
    }

    public static boolean isReturn(point point) throws result{
        return point.get_kind().equals(point_kind.getRETURN());
    }

    public static boolean isExpression(point point) throws result{
        return point.get_kind().equals(point_kind.getEXPRESSION());
    }

    public static boolean isInitExpression(point point) throws result{
        ast un_ast = point.get_ast(ast_family.getC_UNNORMALIZED());
        return isExpression(point) && un_ast.is_a(ast_class.getUC_INIT());
    }

    public static boolean isNormalExpression(point point) throws result{
        ast un_ast = point.get_ast(ast_family.getC_UNNORMALIZED());
        return isExpression(point) && !un_ast.is_a(ast_class.getUC_INIT());
    }

    public static boolean isUnavailable(point point) throws result{
        return point.get_kind().equals(point_kind.getUNAVAILABLE());
    }

    public static boolean isVertex_Kind_Null(point point) throws result{
        return point.get_kind().equals(point_kind.getRESERVED_000());
    }

    public static boolean isCallPost_Vertex(point point) throws result{
        return point.get_kind().equals(point_kind.getRESERVED_002());
    }

    public static boolean isEnd_Vertex(point point) throws result{
        return point.get_kind().equals(point_kind.getRESERVED_003());
    }

    public static boolean isUser_Defined_Vertex(point point) throws result{
        return point.get_kind().equals(point_kind.getRESERVED_004());
    }

    public static boolean isAuxiliary(point point) throws result{
        return point.get_kind().equals(point_kind.getAUXILIARY());
    }

    public static boolean isPhi(point point) throws result{
        return point.get_kind().equals(point_kind.getPHI());
    }

    public static boolean isPi(point point) throws result{
        return point.get_kind().equals(point_kind.getPI());
    }

    public static boolean isNormal_Exit(point point) throws result{
        return point.get_kind().equals(point_kind.getNORMAL_EXIT());
    }

    public static boolean isExceptional_Exit(point point) throws result{
        return point.get_kind().equals(point_kind.getEXCPT_EXIT());
    }

    public static boolean isNormal_Return(point point) throws result{
        return point.get_kind().equals(point_kind.getNORMAL_RETURN());
    }

    public static boolean isExceptional_Return(point point) throws result{
        return point.get_kind().equals(point_kind.getNORMAL_RETURN());
    }

    public static boolean isHammock_Header(point point) throws result{
        return point.get_kind().equals(point_kind.getHAMMOCK_HEADER());
    }

    public static boolean isHammock_Exit(point point) throws result{
        return point.get_kind().equals(point_kind.getHAMMOCK_EXIT());
    }

    public static boolean isVariable_Initialization(point point) throws result{
        return point.get_kind().equals(point_kind.getVARIABLE_INITIALIZATION());
    }


    public static int getFileLineNumber(point point) throws result{
        return (int)point.file_line().get_second();
    }

//    public static boolean isGlobalConstant(point point){
//        try {
//            ast ast =  point.get_ast(ast_family.getC_NORMALIZED());
//            ast value_ast = ast.children().get(1).as_ast();
//            return CFGAST.isGlobalConstant(value_ast);
//        }catch (result r){
//            return false;
//        }
//
//    }

    /**
     *@Description check if the variable in the expression is a global variable
     *@Param [expresiion_point]
     *@return boolean
     **/
    public static boolean isGlobalVariable(point point){
        try {
            return point.declared_symbol().is_global();
        }catch (result r){
            return false;
        }
    }

    public static boolean isGoToLabel(point point){
        try {
            return isLabel(point) && point.get_syntax_kind().equals(point_syntax_kind.getLABEL());
        }catch (result r){
            return false;
        }
    }

    public static boolean isElseLabel(point point){
        try {
            return isLabel(point) && point.get_syntax_kind().equals(point_syntax_kind.getELSE());
        }catch (result r){
            return false;
        }
    }

    public static boolean isDoLabel(point point){
        try {
            return isLabel(point) && point.get_syntax_kind().equals(point_syntax_kind.getNONE())
                    && point.characters().equals("do");
        }catch (result r){
            return false;
        }
    }

    public static boolean isGotoNode(point point){
        try {
            if(isJump(point) && point.get_syntax_kind().equals(point_syntax_kind.getGOTO()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isBreakNode(point point){
        try {
            if(isJump(point) && point.get_syntax_kind().equals(point_syntax_kind.getBREAK()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isIfControlPointNode(point point){
        try {
            if(isControl_Point(point) && point.get_syntax_kind().equals(point_syntax_kind.getIF()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isWhileControlPointNode(point point){
        try {
            if(isControl_Point(point) && point.get_syntax_kind().equals(point_syntax_kind.getWHILE()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isDoControlPointNode(point point){
        try {
            if(isControl_Point(point) && point.get_syntax_kind().equals(point_syntax_kind.getDO()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isForControlPointNode(point point){
        try {
            if(isControl_Point(point) && point.get_syntax_kind().equals(point_syntax_kind.getFOR()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isSwitchControlPointNode(point point){
        try {
            if(isControl_Point(point) && point.get_syntax_kind().equals(point_syntax_kind.getSWITCH()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    /**
     *@Description codesurfer stores do, else and other labels as label nodes
     *@Param []
     *@return java.lang.String
     **/
    public static String getLabelName(point point) throws result{

        String labelName = point.characters();

        if(point.get_syntax_kind().equals(point_syntax_kind.getLABEL())){
            labelName = point.get_ast(ast_family.getC_UNNORMALIZED()).get(ast_ordinal.getUC_LABEL()).as_ast().pretty_print();
        }else if(point.get_syntax_kind().equals(point_syntax_kind.getELSE())){
            labelName = point.get_syntax_kind().name();
        }else if(point.get_syntax_kind().equals(point_syntax_kind.getNONE())){
            labelName = point.characters();
        }

        return labelName;
    }

    public static String getGoToLabelName(point point){
        try {
            return point.get_ast(ast_family.getC_UNNORMALIZED())
                    .get(ast_ordinal.getUC_LABEL()).as_ast().pretty_print();
        }catch (result r){
            return "";
        }
    }

    //TODO:
    public static String getRawSignature(point point){
        try {
            ast un_ast = point.get_ast(ast_family.getC_UNNORMALIZED());

            if(isExpression(point) && un_ast.is_a(ast_class.getUC_INIT())){
                return un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print()+" "
                        +point.characters();
            }else

            return point.characters();
        }catch (result r){
            return "";
        }
    }

    /**
     *@Description Check if an IF node has an else node
     *@Param []
     *@return boolean
     **/
    public static boolean hasElseNode(point point) throws  result{
        //  the first edge is the true edge
        // the second edge is the false edge
        cfg_edge_set cfgEdgeSet = point.cfg_targets();
        point falseNode =  cfgEdgeSet.cend().current().get_first();
        return isElseLabel(falseNode);

    }

    //declaration point
    public static String getVariableNameInNode(point point) throws  result{
        ast variable_ast = point.get_ast(ast_family.getC_NORMALIZED());
        return normalizingVariableName(variable_ast);
    }

}
