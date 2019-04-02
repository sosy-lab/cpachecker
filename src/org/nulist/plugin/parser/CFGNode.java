/**
 * @ClassName CFGNode
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 10:05 AM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.*;

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

    public static boolean isContinueNode(point node){
        try {
            if(isJump(node) && node.get_syntax_kind().equals(point_syntax_kind.getCONTINUE()))
                return true;
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isNodeNode(point node){
        try {
            if(node.get_syntax_kind().equals(point_syntax_kind.getNONE()))
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

    public static boolean isReturnControlPointNode(point node){
        try {
            if(isControl_Point(node) && node.get_syntax_kind().equals(point_syntax_kind.getRETURN()))
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

    public static cfg_edge_vector moveSwitchDefault2Last(cfg_edge_vector switchCaseEdgeVector) throws result{
        if(switchCaseEdgeVector.get((int)switchCaseEdgeVector.size()-1).get_second().name().contains("default"))
            return switchCaseEdgeVector;
        else {
            cfg_edge_vector edgeVector = new cfg_edge_vector();
            int indexDefault=-1;
            for(int i=0;i<switchCaseEdgeVector.size();i++){
                if(switchCaseEdgeVector.get(i).get_second().name().contains("default")){
                    indexDefault = i;
                }else
                    edgeVector.add(switchCaseEdgeVector.get(i));
            }
            if(indexDefault==-1)
                return switchCaseEdgeVector;
            else{
                edgeVector.add(switchCaseEdgeVector.get(indexDefault));
                return edgeVector;
            }
        }
    }

    public static point_vector sortActualInVectorByID(point_vector actualins)throws result {
        point_vector pointVector = new point_vector();
        Map<Integer, Integer> lineMap = new HashMap<>();


        for(int i=0;i<actualins.size();i++){
            int id = Integer.valueOf(actualins.get(i).get_ast(ast_family.getC_NORMALIZED())
                    .children().get(0).as_ast().pretty_print().replace("$param_",""));
            lineMap.put(id,i);
        }
        TreeSet<Integer> treeSet = new TreeSet<>(lineMap.keySet());
        treeSet.comparator();
        for(Integer i:treeSet)
            pointVector.add(actualins.get(lineMap.get(i)));
        return pointVector;
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


    public static boolean isTempVariableAssignment(ast no_ast)throws result{
        try {
            return no_ast.pretty_print().startsWith("$temp") || no_ast.pretty_print().startsWith("*$temp");
//            if(no_ast.is_a(ast_class.getNC_ABSTRACT_STATEMENT()) &&
//                    no_ast.children().get(0).as_ast().is_a(ast_class.getNC_VARIABLE()) &&
//                    no_ast.children().get(0).as_ast().get(ast_ordinal.getBASE_ABS_LOC())
//                            .as_symbol().get_kind().equals(symbol_kind.getINTERMEDIATE())){
//                dumpAST(no_ast.children().get(0).as_ast(),0, no_ast.children().get(0).as_ast().get(ast_ordinal.getBASE_ABS_LOC())
//                        .as_symbol().get_kind().name());
//                return true;
//            }
//            return false;
//            return no_ast.is_a(ast_class.getNC_ABSTRACT_STATEMENT()) &&
//                    no_ast.children().get(0).as_ast().is_a(ast_class.getNC_VARIABLE()) &&
//                    no_ast.children().get(0).as_ast().get(ast_ordinal.getBASE_ABS_LOC())
//                            .as_symbol().get_kind().equals(symbol_kind.getINTERMEDIATE());
        }catch (result r){
            return false;
        }

    }

    public static boolean useTempVariable(ast no_ast)throws result{
        try {
            return !isTempVariableAssignment(no_ast) && no_ast.pretty_print().contains("$temp");
        }catch (result r){
            return false;
        }
    }

    public static int getMemberSize(CType cType){
        if(cType instanceof CTypedefType)
            return getMemberSize(((CTypedefType) cType).getRealType());

        else if(cType instanceof CPointerType)
            return getMemberSize(((CPointerType) cType).getType());

        else if(cType instanceof CElaboratedType)
            return getMemberSize(((CElaboratedType) cType).getRealType());

        else if(cType instanceof CCompositeType){
            int size =0;
            for(CCompositeType.CCompositeTypeMemberDeclaration typeMemberDeclaration:((CCompositeType) cType).getMembers()){
                size += getMemberSize(typeMemberDeclaration.getType());
            }
            return size;
        }else
            return 1;
    }

    public static int getInitMemberSize(CType cType, int deep){

        if(cType instanceof CTypedefType)
            return getInitMemberSize(((CTypedefType) cType).getRealType(),deep);

        else if(cType instanceof CPointerType)
            return getInitMemberSize(((CPointerType) cType).getType(),deep);

        else if(cType instanceof CElaboratedType)
            return getInitMemberSize(((CElaboratedType) cType).getRealType(),deep);

        else if(cType instanceof CCompositeType){
            int size =0;
            for(CCompositeType.CCompositeTypeMemberDeclaration typeMemberDeclaration:((CCompositeType) cType).getMembers()){
                if(deep>0)
                    size += getInitMemberSize(typeMemberDeclaration.getType(), --deep);
                else
                    size ++;
            }
            return size;
        }else
            return 1;
    }

//    public static point pointNextToBlockAssignmentExpr(point baExpression, CType cType)throws result{
//        //should be a struct type == CCompositeType
//        point nextNode;
//        int memberNum;
//        if(baExpression.get_ast(ast_family.getC_UNNORMALIZED()).is_a(ast_class.getUC_INIT()))
//            memberNum = getInitMemberSize(cType,1);
//         else
//             memberNum = getMemberSize(cType);
//
//        nextNode = baExpression.cfg_targets().cbegin().current().get_first();
//        if(memberNum==1)
//                return nextNode;
//        for(int i=1;i<memberNum;i++){
//                if(!nextNode.get_ast(ast_family.getC_NORMALIZED()).is_a(ast_class.getNC_BLOCKASSIGN()))
//                    throw new RuntimeException("This is not a block assignment expression:"+ nextNode.toString());
//                nextNode = nextNode.cfg_targets().cbegin().current().get_first();
//        }
//        return nextNode;
//    }

    public static point pointNextToBlockAssignmentExpr(point baExpression, CType cType)throws result{
        ast no_ast= baExpression.get_ast(ast_family.getC_NORMALIZED());
        int memberNum = getInitMemberSize(cType,0);
        point nextNode = baExpression.cfg_targets().cbegin().current().get_first();

        ast original = no_ast.get(ast_ordinal.getNC_ORIGINAL()).as_ast();
        String originalStr = original.pretty_print();
        while (memberNum>1){
            nextNode = nextNode.cfg_targets().cbegin().current().get_first();
            memberNum--;
        }
        boolean getNextPoint = false;
        while (!getNextPoint){
            if(isExpression(nextNode)){
                ast no_ast1 = nextNode.get_ast(ast_family.getC_NORMALIZED());
                if(no_ast1.is_a(ast_class.getNC_BLOCKASSIGN())){
                    ast original1 = no_ast1.get(ast_ordinal.getNC_ORIGINAL()).as_ast();
                    if(original1.pretty_print().equals(originalStr)){
                        nextNode = nextNode.cfg_targets().cbegin().current().get_first();
                    }else
                        getNextPoint = true;
                }else
                    getNextPoint = true;
            }else
                getNextPoint = true;
        }

        return nextNode;
    }
}
