/**
 * @ClassName CFGNode
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 10:05 AM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

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
   
    
    public static String getKindName(point node) throws result{
        return node.get_kind().name();
    }

    public static boolean isFormal_In(point node) throws result{
        return node.get_kind().equals(point_kind.getFORMAL_IN());
    }

    public static boolean isFormal_Out(point node) throws result{
        return node.get_kind().equals(point_kind.getFORMAL_OUT());
    }

    public static boolean isGlobalFormal_In(point node) throws result{
        return node.get_kind().equals(point_kind.getGLOBAL_FORMAL_IN());
    }

    public static boolean isGlobalFormal_Out(point node) throws result{
        return node.get_kind().equals(point_kind.getGLOBAL_FORMAL_OUT());
    }

    public static boolean isActual_In(point node) throws result{
        return node.get_kind().equals(point_kind.getACTUAL_IN());
    }

    public static boolean isActual_Out(point node) throws result{
        return node.get_kind().equals(point_kind.getACTUAL_OUT());
    }

    public static boolean isControl_Point(point node) throws result{
        return node.get_kind().equals(point_kind.getCONTROL_POINT());
    }

    public static boolean isCall_Site(point node) throws result{
        return node.get_kind().equals(point_kind.getCALL_SITE());
    }


    public static boolean isFunctionEntry(point node) throws result{
        return node.get_kind().equals(point_kind.getENTRY());
    }

    public static boolean isIndirect_Call(point node) throws result{
        return node.get_kind().equals(point_kind.getINDIRECT_CALL());
    }

    public static boolean isFunctionBody(point node) throws result{
        return node.get_kind().equals(point_kind.getBODY());
    }

    public static boolean isFunctionExit(point node) throws result{
        return node.get_kind().equals(point_kind.getEXIT());
    }

    public static boolean isDeclaration(point node) throws result{
        return node.get_kind().equals(point_kind.getDECLARATION());
    }

    public static boolean isJump(point node) throws result{
        return node.get_kind().equals(point_kind.getJUMP());
    }

    public static boolean isLabel(point node) throws result{
        return node.get_kind().equals(point_kind.getLABEL());
    }

    public static boolean isSwitchCase(point node) throws result{
        return node.get_kind().equals(point_kind.getSWITCH_CASE());
    }

    public static boolean isReturn(point node) throws result{
        return node.get_kind().equals(point_kind.getRETURN());
    }

    public static boolean isExpression(point node) throws result{
        return node.get_kind().equals(point_kind.getEXPRESSION());
    }

    public static boolean isInitExpression(point node) throws result{
        ast un_ast = node.get_ast(ast_family.getC_UNNORMALIZED());
        return isExpression(node) && un_ast.is_a(ast_class.getUC_INIT());
    }

    public static boolean isNormalExpression(point node) throws result{
        ast un_ast = node.get_ast(ast_family.getC_UNNORMALIZED());
        return isExpression(node) && !un_ast.is_a(ast_class.getUC_INIT());
    }

    public static boolean isNormal_Exit(point node) throws result{
        return node.get_kind().equals(point_kind.getNORMAL_EXIT());
    }

    public static boolean isExceptional_Exit(point node) throws result{
        return node.get_kind().equals(point_kind.getEXCPT_EXIT());
    }

    public static boolean isNormal_Return(point node) throws result{
        return node.get_kind().equals(point_kind.getNORMAL_RETURN());
    }

    public static boolean isExceptional_Return(point node) throws result{
        return node.get_kind().equals(point_kind.getNORMAL_RETURN());
    }


    public static boolean isVariable_Initialization(point node) throws result{
        return node.get_kind().equals(point_kind.getVARIABLE_INITIALIZATION());
    }


    public static int getFileLineNumber(point node) throws result{
        return (int)node.file_line().get_second();
    }

    /**
     *@Description check if the variable in the expression is a global variable
     *@Param [expresiion_point]
     *@return boolean
     **/
    public static boolean isGlobalVariable(point node){
        try {
            return node.declared_symbol().is_global();
        }catch (result r){
            return false;
        }
    }

    public static boolean isGoToLabel(point node){
        try {
            return isLabel(node) && node.get_syntax_kind().equals(point_syntax_kind.getLABEL());
        }catch (result r){
            return false;
        }
    }

    public static boolean isElseLabel(point node){
        try {
            return isLabel(node) && node.get_syntax_kind().equals(point_syntax_kind.getELSE());
        }catch (result r){
            return false;
        }
    }

    public static boolean isDoLabel(point node){
        try {
            return isLabel(node) && node.get_syntax_kind().equals(point_syntax_kind.getNONE())
                    && node.characters().equals("do");
        }catch (result r){
            return false;
        }
    }

    public static boolean isGotoNode(point node){
        try {
            if(isJump(node) && node.get_syntax_kind().equals(point_syntax_kind.getGOTO()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isBreakNode(point node){
        try {
            if(isJump(node) && node.get_syntax_kind().equals(point_syntax_kind.getBREAK()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isIfControlPointNode(point node){
        try {
            if(isControl_Point(node) && node.get_syntax_kind().equals(point_syntax_kind.getIF()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isWhileControlPointNode(point node){
        try {
            if(isControl_Point(node) && node.get_syntax_kind().equals(point_syntax_kind.getWHILE()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isDoControlPointNode(point node){
        try {
            if(isControl_Point(node) && node.get_syntax_kind().equals(point_syntax_kind.getDO()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isForControlPointNode(point node){
        try {
            if(isControl_Point(node) && node.get_syntax_kind().equals(point_syntax_kind.getFOR()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isSwitchControlPointNode(point node){
        try {
            if(isControl_Point(node) && node.get_syntax_kind().equals(point_syntax_kind.getSWITCH()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    /**
     *@Description codesurfer stores do, else and other labels as label nodes
     *@Param [node]
     *@return java.lang.String
     **/
    public static String getLabelName(point node) throws result{

        String labelName = node.characters();

        if(node.get_syntax_kind().equals(point_syntax_kind.getLABEL())){
            labelName = node.get_ast(ast_family.getC_UNNORMALIZED()).get(ast_ordinal.getUC_LABEL()).as_ast().pretty_print();
        }else if(node.get_syntax_kind().equals(point_syntax_kind.getELSE())){
            labelName = node.get_syntax_kind().name();
        }else if(node.get_syntax_kind().equals(point_syntax_kind.getNONE())){
            labelName = node.characters();
        }

        return labelName;
    }

    /**
     * @Description //extract the label name
     * @Param [node]
     * @return java.lang.String
     **/
    public static String getGoToLabelName(point node){
        try {
            return node.get_ast(ast_family.getC_UNNORMALIZED())
                    .get(ast_ordinal.getUC_LABEL()).as_ast().pretty_print();
        }catch (result r){
            return "";
        }
    }

    //TODO:
    public static String getRawSignature(point node){
        try {
            ast un_ast = node.get_ast(ast_family.getC_UNNORMALIZED());

            if(isExpression(node) && un_ast.is_a(ast_class.getUC_INIT())){
                return un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print()+" "
                        +node.characters();
            }else

                return node.characters();

        }catch (result r){
            return "";
        }
    }

    /**
     * @Description //incremental sort edges by their line numbers,
     * because the target edges of a CFG node obtained from CodeSurfer may not be in that order
     * @Param [cfgEdgeVector]
     * @return com.grammatech.cs.cfg_edge_vector
     **/
    public static cfg_edge_vector sortVectorByLineNo(cfg_edge_vector cfgEdgeVector)throws result {
        cfg_edge_vector edgeVector = new cfg_edge_vector();
        Map<Long, Integer> lineMap = new HashMap<>();
        for(int i=0;i<cfgEdgeVector.size();i++)
            lineMap.put(cfgEdgeVector.get(i).get_first().file_line().get_second(),i);
        TreeSet<Long> treeSet = new TreeSet<>(lineMap.keySet());
        treeSet.comparator();
        for(Long i:treeSet)
            edgeVector.add(cfgEdgeVector.get(lineMap.get(i)));
        return edgeVector;
    }

    /**
     *@Description Check if an IF node has an else node
     *@Param []
     *@return boolean
     **/
    public static boolean hasElseNode(point node) throws  result{
        //  the first edge is the true edge
        // the second edge is the false edge
        cfg_edge_set cfgEdgeSet = node.cfg_targets();
        point falseNode =  cfgEdgeSet.cend().current().get_first();
        return isElseLabel(falseNode);

    }

    //declaration point
    public static String getVariableNameInNode(point node) throws  result{
        ast variable_ast = node.get_ast(ast_family.getC_NORMALIZED());
        return normalizingVariableName(variable_ast);
    }

}
