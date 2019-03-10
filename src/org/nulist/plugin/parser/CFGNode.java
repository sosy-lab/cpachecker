/**
 * @ClassName CFGNode
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 10:05 AM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;

public class CFGNode extends point {

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

    public CFGNode(long cPtr, boolean cMemoryOwn){
        super(cPtr, cMemoryOwn);
    }


    public String getKindName() throws result{
        return this.get_kind().name();
    }

    public boolean isFormal_In() throws result{
        return this.get_kind().equals(point_kind.getFORMAL_IN());
    }

    public boolean isFormal_Out() throws result{
        return this.get_kind().equals(point_kind.getFORMAL_OUT());
    }

    public boolean isGlobalFormal_In() throws result{
        return this.get_kind().equals(point_kind.getGLOBAL_FORMAL_IN());
    }

    public boolean isGlobalFormal_Out() throws result{
        return this.get_kind().equals(point_kind.getGLOBAL_FORMAL_OUT());
    }

    public boolean isActual_In() throws result{
        return this.get_kind().equals(point_kind.getACTUAL_IN());
    }

    public boolean isActual_Out() throws result{
        return this.get_kind().equals(point_kind.getACTUAL_OUT());
    }

    public boolean isGlobalActual_In() throws result{
        return this.get_kind().equals(point_kind.getGLOBAL_ACTUAL_IN());
    }

    public boolean isGlobalActual_Out() throws result{
        return this.get_kind().equals(point_kind.getGLOBAL_ACTUAL_OUT());
    }

    public boolean isControl_Point() throws result{
        return this.get_kind().equals(point_kind.getCONTROL_POINT());
    }

    public boolean isCall_Site() throws result{
        return this.get_kind().equals(point_kind.getCALL_SITE());
    }

    public boolean isFunctionEntry() throws result{
        return this.get_kind().equals(point_kind.getENTRY());
    }

    public boolean isIndirect_Call() throws result{
        return this.get_kind().equals(point_kind.getINDIRECT_CALL());
    }

    public boolean isFunctionBody() throws result{
        return this.get_kind().equals(point_kind.getBODY());
    }

    public boolean isFunctionExit() throws result{
        return this.get_kind().equals(point_kind.getEXIT());
    }

    public boolean isDeclaration() throws result{
        return this.get_kind().equals(point_kind.getDECLARATION());
    }

    public boolean isJump() throws result{
        return this.get_kind().equals(point_kind.getJUMP());
    }

    public boolean isLabel() throws result{
        return this.get_kind().equals(point_kind.getLABEL());
    }

    public boolean isSwitchCase() throws result{
        return this.get_kind().equals(point_kind.getSWITCH_CASE());
    }

    public boolean isReturn() throws result{
        return this.get_kind().equals(point_kind.getRETURN());
    }

    public boolean isExpression() throws result{
        return this.get_kind().equals(point_kind.getEXPRESSION());
    }

    public boolean isUnavailable() throws result{
        return this.get_kind().equals(point_kind.getUNAVAILABLE());
    }

    public boolean isVertex_Kind_Null() throws result{
        return this.get_kind().equals(point_kind.getRESERVED_000());
    }

    public boolean isCallPost_Vertex() throws result{
        return this.get_kind().equals(point_kind.getRESERVED_002());
    }

    public boolean isEnd_Vertex() throws result{
        return this.get_kind().equals(point_kind.getRESERVED_003());
    }

    public boolean isUser_Defined_Vertex() throws result{
        return this.get_kind().equals(point_kind.getRESERVED_004());
    }

    public boolean isAuxiliary() throws result{
        return this.get_kind().equals(point_kind.getAUXILIARY());
    }

    public boolean isPhi() throws result{
        return this.get_kind().equals(point_kind.getPHI());
    }

    public boolean isPi() throws result{
        return this.get_kind().equals(point_kind.getPI());
    }

    public boolean isNormal_Exit() throws result{
        return this.get_kind().equals(point_kind.getNORMAL_EXIT());
    }

    public boolean isExceptional_Exit() throws result{
        return this.get_kind().equals(point_kind.getEXCPT_EXIT());
    }

    public boolean isNormal_Return() throws result{
        return this.get_kind().equals(point_kind.getNORMAL_RETURN());
    }

    public boolean isExceptional_Return() throws result{
        return this.get_kind().equals(point_kind.getNORMAL_RETURN());
    }

    public boolean isHammock_Header() throws result{
        return this.get_kind().equals(point_kind.getHAMMOCK_HEADER());
    }

    public boolean isHammock_Exit() throws result{
        return this.get_kind().equals(point_kind.getHAMMOCK_EXIT());
    }

    public boolean isVariable_Initialization() throws result{
        return this.get_kind().equals(point_kind.getVARIABLE_INITIALIZATION());
    }


    public int getFileLineNumber() throws result{
        return (int)this.file_line().get_second();
    }

    public boolean isGlobalConstant(){
        try {
            CFGAST ast = (CFGAST) this.get_ast(ast_family.getC_NORMALIZED());
            return ast.isGlobalConstant();
        }catch (result r){
            return false;
        }

    }

    /**
     *@Description check if the variable in the expression is a global variable
     *@Param [expresiion_point]
     *@return boolean
     **/
    public boolean isGlobalVariable(){
        try {
            return this.declared_symbol().is_global();
        }catch (result r){
            return false;
        }
    }

    public boolean isGoToLabel(){
        try {
            return this.isLabel() && this.get_syntax_kind().equals(point_syntax_kind.getLABEL());
        }catch (result r){
            return false;
        }
    }

    public boolean isElseLabel(){
        try {
            return this.isLabel() && this.get_syntax_kind().equals(point_syntax_kind.getELSE());
        }catch (result r){
            return false;
        }
    }

    public boolean isDoLabel(){
        try {
            return this.isLabel() && this.get_syntax_kind().equals(point_syntax_kind.getNONE())
                    && this.characters().equals("do");
        }catch (result r){
            return false;
        }
    }

    public boolean isGotoNode(){
        try {
            if(this.isJump() && this.get_syntax_kind().equals(point_syntax_kind.getGOTO()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isBreakNode(){
        try {
            if(this.isJump() && this.get_syntax_kind().equals(point_syntax_kind.getBREAK()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isIfControlPointNode(){
        try {
            if(this.isControl_Point() && this.get_syntax_kind().equals(point_syntax_kind.getIF()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isWhileControlPointNode(){
        try {
            if(this.isControl_Point() && this.get_syntax_kind().equals(point_syntax_kind.getWHILE()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isDoControlPointNode(){
        try {
            if(this.isControl_Point() && this.get_syntax_kind().equals(point_syntax_kind.getDO()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isForControlPointNode(){
        try {
            if(this.isControl_Point() && this.get_syntax_kind().equals(point_syntax_kind.getFOR()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isSwitchControlPointNode(){
        try {
            if(this.isControl_Point() && this.get_syntax_kind().equals(point_syntax_kind.getSWITCH()))
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
    public String getLabelName() throws result{

        String labelName = this.characters();

        if(this.get_syntax_kind().equals(point_syntax_kind.getLABEL())){
            labelName = this.get_ast(ast_family.getC_UNNORMALIZED()).get(ast_ordinal.getUC_LABEL()).as_ast().pretty_print();
        }else if(this.get_syntax_kind().equals(point_syntax_kind.getELSE())){
            labelName = this.get_syntax_kind().name();
        }else if(this.get_syntax_kind().equals(point_syntax_kind.getNONE())){
            labelName = this.characters();
        }

        return labelName;
    }

    public String getGoToLabelName(){
        try {
            return this.get_ast(ast_family.getC_UNNORMALIZED())
                    .get(ast_ordinal.getUC_LABEL()).as_ast().pretty_print();
        }catch (result r){
            return "";
        }
    }

    public String getRawSignature(){
        try {
            return this.characters();
        }catch (result r){
            return "";
        }
    }

    /**
     *@Description Check if an IF node has an else node
     *@Param []
     *@return boolean
     **/
    public boolean hasElseNode() throws  result{
        //  the first edge is the true edge
        // the second edge is the false edge
        cfg_edge_set cfgEdgeSet = this.cfg_targets();
        CFGNode falseNode = (CFGNode) cfgEdgeSet.cend().current().get_first();
        return falseNode.isElseLabel();

    }
    public String getVariableNameInNode() throws  result{
        CFGAST variable_ast = (CFGAST) this.get_ast(ast_family.getC_NORMALIZED());
        return variable_ast.normalizingVariableName();
    }
}
