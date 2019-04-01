/**
 * @ClassName CFGAST
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 11:58 AM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CFGAST {
    

    /**
     *@Description check if the type of a given variable (p) is constant array or vector
     *@Param [type]  ast_field = p's normalized ast' type (getNC_TYPE)
     *@return boolean
     **/
    public static boolean isConstantArrayOrVector(ast_field type){
        try {
            String typeName = type.as_ast().pretty_print();

            if(typeName.startsWith("const")){
                ast typeast = type.as_ast();
                if(typeast.is_a(ast_class.getUC_POINTER())||
                        typeast.is_a(ast_class.getUC_ARRAY())
                        ||typeast.is_a(ast_class.getUC_VECTOR_TYPE())
                ){
                    return true;
                }
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    //ast type
    public static boolean isArrayType(ast type){
        try {
            return type.is_a(ast_class.getNC_ARRAY()) || type.is_a(ast_class.getUC_ARRAY());
        }catch (result r){
            return false;
        }
    }

    public static boolean isVLAType(ast type){
        try {
            return type.is_a(ast_class.getUC_VLA());
        }catch (result r){
            return false;
        }
    }

    public static boolean isTypeRef(ast type){
        try {
            return type.is_a(ast_class.getUC_TYPEREF());
        }catch (result r){
            return false;
        }
    }


    public static boolean isNullArrayInit(ast type){
        try {
            ast field1 = type.children().get(0).as_ast();
            ast field2 = type.children().get(1).as_ast();

            return isPointerExpr(field1)
                    && isCastExpr(field2)
                    && isArrayType(field2.get(ast_ordinal.getBASE_TYPE()).as_ast())
                    && field2.children().get(1).get(ast_ordinal.getBASE_VALUE()).as_int32()==0;
        }catch (result r){
            return false;
        }
    }

    public static boolean isPointerType(ast type){
        try {
            return type.is_a(ast_class.getNC_POINTER())||type.is_a(ast_class.getUC_POINTER());
        }catch (result r){
            return false;
        }
    }

    public static boolean isStructType(ast type){
        try {
            return type.is_a(ast_class.getNC_STRUCT()) || type.is_a(ast_class.getUC_STRUCT());
        }catch (result r){

            return false;
        }

    }

    public static boolean isUnionType(ast type){
        try {
            return type.is_a(ast_class.getNC_UNION()) || type.is_a(ast_class.getUC_UNION());
        }catch (result r){
            return false;
        }

    }

    public static boolean isEnumType(ast type){
        try {
            return type.is_a(ast_class.getUC_ENUM()) ||
                    type.get(ast_ordinal.getBASE_TYPE()).as_ast().is_a(ast_class.getUC_ENUM());
        }catch (result r){
            return false;
        }
    }



    public static boolean isConstantAggregateZero(ast type, CType cType){
        try {
            if(type.is_a(ast_class.getNC_CASTEXPR())){
                ast basetype = type.get(ast_ordinal.getBASE_TYPE()).as_ast();
                return (isPointerType(basetype)) && type.children().get(1).get(ast_ordinal.getBASE_VALUE()).as_int32()==0;
            }else if(cType instanceof CPointerType){
                return type.get(ast_ordinal.getBASE_VALUE()).as_uint32()==0;
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isConstantAggreateZeroFromUC(ast constant){
        try {
            return constant.is_a(ast_class.getUC_AGGREGATE()) &&
                    !constant.has_field(ast_ordinal.getUC_CONSTANT_LIST());
        }catch (result r){
            return false;
        }
    }

    public static BigInteger getBasicValue(ast value_ast){
        try {
            String valueString = value_ast.pretty_print();
            BigInteger value = BigInteger.valueOf(Long.valueOf(valueString));
            return value;
        }catch (result r){
            return null;
        }
    }

    /**
     *@Description check if a pointer is null, using its normalized ast
     *@Param []
     *@return boolean
     **/
    public static boolean isNullPointer(ast type){
        try {
            ast_field typeField = type.get(ast_ordinal.getNC_TYPE());//normalized type
            if(typeField.as_ast().is_a(ast_class.getNC_POINTER())){
                if(type.children().get(1).as_int32()==0){
                    return true;
                }
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isInitializationExpression(ast expr){
        try {
            return expr.get(ast_ordinal.getNC_IS_INITIALIZATION()).as_boolean();
        }catch (result r){
            return false;
        }
    }

    /**
     *@Description check if the assigned value is constant expression, e.g., right child has the type of c:exprs
     *@Param []
     *@return boolean
     **/
    public static boolean isNormalExpression(ast expr){
        try {
            //normalizedVarInitAST.children().get(1).as_ast().name() = c:exprs
            return expr.is_a(ast_class.getNC_EXPRS());
        }catch (result r){
            return false;
        }
    }

    public static boolean isStructElementExpr(ast expr){
        try {
            return expr.is_a(ast_class.getNC_STRUCTORUNIONREF());
        }catch (result r){
            return false;
        }
    }

    public static boolean isPointerExpr(ast expr){
        try {
            return expr.is_a(ast_class.getNC_POINTEREXPR());
        }catch (result r){
            return false;
        }
    }

    public static boolean isPointerAddressExpr(ast expr){
        try {
            return expr.is_a(ast_class.getNC_ADDREXPR());
        }catch (result r){
            return false;
        }
    }

    public static boolean isZeroInitExpr(ast expr){
        try {
            return expr.is_a(ast_class.getNC_CASTEXPR());
        }catch (result r){
            return false;
        }
    }

    public static boolean isCastExpr(ast expr){
        try {
            return expr.is_a(ast_class.getNC_CASTEXPR());
        }catch (result r){
            return false;
        }
    }

/**
 *@Description check if a value is a global constant, using NORMALIZED ast
 *@Param []
 *@return boolean
 **/

    public static boolean isGlobalConstant(ast expr){
        try {
            if(expr.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global()){//global value
                if(expr.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const"))//const value
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isGlobalVariable(ast expr){
        try {
            if(expr.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global()){//global value
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isConstantType(ast expr){
        try {
            return expr.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const");
        }catch (result r){
            return false;
        }
    }


    /**
     *@Description check if a value is a constant, include local and global
     *@Param [normalizedVarInitAST]
     *@return boolean
     **/
    public static boolean isConstant(ast expr){
        try {
            ast value_ast = expr.children().get(1).as_ast();
            return value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const");
        }catch (result r){
            return false;
        }
    }

    public static ast getStructType(ast type) throws result{
        if(type.is_a(ast_class.getNC_STRUCT()) || type.is_a(ast_class.getUC_STRUCT())){
            return type;
        }else {//for typedef struct
            return type.get(ast_ordinal.getBASE_TYPE()).as_ast();
        }
    }

    //ast normalizedVarInitAST
    public static boolean isUndef(ast type){
        return false;
    }



    public static CStorageClass getStorageClass(ast type)throws result{
        CStorageClass storageClass;
        switch (type.get(ast_ordinal.getBASE_STORAGE_CLASS()).as_enum_value_string()){
            case "static":
                storageClass = CStorageClass.STATIC;
                break;
            case "extern":
                storageClass = CStorageClass.EXTERN;
                break;
            case "typedef":
                storageClass = CStorageClass.TYPEDEF;
                break;
            default:
                storageClass = CStorageClass.AUTO;
                break;
        }

        return storageClass;
    }

    /**
     *@Description normalize variables to avoid same name
     *@Param [varAST]
     *@return void
     **/
    public static String normalizingVariableName(ast variableSymbol)throws result{
        symbol variable = variableSymbol.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();
        if(variable.is_global())
            return variableSymbol.pretty_print();
        else
            return variable.name().replace("-","_");
    }

    public static CBinaryExpression.BinaryOperator getBinaryOperatorFromUC(ast un_ast)throws result{

        if(un_ast.is_a(ast_class.getUC_ABSTRACT_POST_INCR()) || //a++
                 un_ast.is_a(ast_class.getUC_ABSTRACT_ADD_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_ABSTRACT_ADD())){//b+=10;
            return CBinaryExpression.BinaryOperator.PLUS;
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_DIVIDE_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_ABSTRACT_DIVIDE())){//b/=10;
            return CBinaryExpression.BinaryOperator.DIVIDE;
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_MULTIPLY_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_ABSTRACT_MULTIPLY())){//b*=10;
            return CBinaryExpression.BinaryOperator.MULTIPLY;
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_POST_DECR()) || //a--
                 un_ast.is_a(ast_class.getUC_ABSTRACT_SUBTRACT_ASSIGN()) || //b-=10;
                 un_ast.is_a(ast_class.getUC_ABSTRACT_SUBTRACT())){//
            return CBinaryExpression.BinaryOperator.MINUS;
        }else if(un_ast.is_a(ast_class.getUC_AND_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_AND())){//b&=1;
            return CBinaryExpression.BinaryOperator.BINARY_AND;
        }else if(un_ast.is_a(ast_class.getUC_OR_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_OR())){//b|=1;
            return CBinaryExpression.BinaryOperator.BINARY_OR;
        }else if(un_ast.is_a(ast_class.getUC_XOR_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_XOR())){//b^=1;
            return CBinaryExpression.BinaryOperator.BINARY_XOR;
        }else if(un_ast.is_a(ast_class.getUC_REMAINDER_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_REMAINDER())){//b%=1;
            return CBinaryExpression.BinaryOperator.MODULO;
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_SHIFTL_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_ABSTRACT_SHIFTL())){//b<<=1;
            return CBinaryExpression.BinaryOperator.SHIFT_LEFT;
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_SHIFTR_ASSIGN()) ||
                 un_ast.is_a(ast_class.getUC_ABSTRACT_SHIFTR())){//b>>=1;
            return CBinaryExpression.BinaryOperator.SHIFT_RIGHT;
        }else if(un_ast.is_a(ast_class.getUC_ABSTRACT_EQ()))//==
            return CBinaryExpression.BinaryOperator.EQUALS;
        else if(un_ast.is_a(ast_class.getUC_ABSTRACT_NE()))//!=
            return CBinaryExpression.BinaryOperator.NOT_EQUALS;
        else if(un_ast.is_a(ast_class.getUC_ABSTRACT_GT()))//>
            return CBinaryExpression.BinaryOperator.GREATER_THAN;
        else if(un_ast.is_a(ast_class.getUC_ABSTRACT_GE()))//>=
            return CBinaryExpression.BinaryOperator.GREATER_EQUAL;
        else if(un_ast.is_a(ast_class.getUC_ABSTRACT_LT()))//<
            return CBinaryExpression.BinaryOperator.LESS_THAN;
        else if(un_ast.is_a(ast_class.getUC_ABSTRACT_LE()))
            return CBinaryExpression.BinaryOperator.LESS_EQUAL;//<=
        else{
            dumpAST(un_ast,0,un_ast.toString());
            throw new UnsupportedOperationException("Unsupported predicate "+ un_ast.toString());
        }
    }

    //Normalized
    public static CBinaryExpression.BinaryOperator getBinaryOperator(ast oper)throws result{
        if(oper.is_a(ast_class.getNC_ADDEXPR()))
            return CBinaryExpression.BinaryOperator.PLUS;
        else if(oper.is_a(ast_class.getNC_SUBEXPR()))
            return CBinaryExpression.BinaryOperator.MINUS;
        else if(oper.is_a(ast_class.getNC_MULEXPR()))
            return CBinaryExpression.BinaryOperator.MULTIPLY;
        else if(oper.is_a(ast_class.getNC_DIVEXPR()))
            return CBinaryExpression.BinaryOperator.DIVIDE;
        else if(oper.is_a(ast_class.getNC_MODEXPR()))
            return CBinaryExpression.BinaryOperator.MODULO;
        else if(oper.is_a(ast_class.getNC_RIGHTASSIGN()) || oper.is_a(ast_class.getNC_RIGHTSHIFTEXPR()))
            return CBinaryExpression.BinaryOperator.SHIFT_RIGHT;
        else if(oper.is_a(ast_class.getNC_LEFTASSIGN()) || oper.is_a(ast_class.getNC_LEFTSHIFTEXPR()))
            return CBinaryExpression.BinaryOperator.SHIFT_LEFT;
        else if(oper.is_a(ast_class.getNC_ANDASSIGN()) || oper.is_a(ast_class.getNC_BITANDEXPR()))
            return CBinaryExpression.BinaryOperator.BINARY_AND;
        else if(oper.is_a(ast_class.getNC_ORASSIGN()) || oper.is_a(ast_class.getNC_INCLUSIVEOR()) || oper.is_a(ast_class.getNC_OREXPR()))
            return CBinaryExpression.BinaryOperator.BINARY_OR;
        else if(oper.is_a(ast_class.getNC_XORASSIGN()) || oper.is_a(ast_class.getNC_EXCLUSIVEOR()))
            return CBinaryExpression.BinaryOperator.BINARY_XOR;
        else if(oper.is_a(ast_class.getNC_EQUALEXPR()))
            return CBinaryExpression.BinaryOperator.EQUALS;
        else if(oper.is_a(ast_class.getNC_NOTEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.NOT_EQUALS;
        else if(oper.is_a(ast_class.getNC_GREATEXPR()))
            return CBinaryExpression.BinaryOperator.GREATER_THAN;
        else if(oper.is_a(ast_class.getNC_GREATEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.GREATER_EQUAL;
        else if(oper.is_a(ast_class.getNC_LESSEXPR()))
            return CBinaryExpression.BinaryOperator.LESS_THAN;
        else if(oper.is_a(ast_class.getNC_LESSEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.LESS_EQUAL;
        else
            throw new UnsupportedOperationException("Unsupported predicate: "+oper.get_class().name());
    }

    public static boolean hasRadixField(ast type){
        try {
            type.get(ast_ordinal.getUC_RADIX());
            return true;
        }catch (result r){
            return false;
        }
    }

    public static boolean isVariable(ast var){
        try {
            return var.is_a(ast_class.getNC_VARIABLE());
        }catch (result r){
            return false;
        }
    }

    public static boolean isValue(ast value){
        try {
            return value.is_a(ast_class.getNC_ABSTRACT_INTEGER_VALUE())||value.is_a(ast_class.getNC_ABSTRACT_FLOAT_VALUE());
        }catch (result r){
            return false;
        }
    }


    //a symbol can only have unnormalized ast
    public static boolean symbolHasInitialization(ast variable){
        try {
            variable.get(ast_ordinal.getUC_DYNAMIC_INIT());
            return true;
        }catch (result r){
            return false;
        }
    }

    public static boolean staticSymbolHasInitialization(ast variable){
        try {
            ast init = variable.get(ast_ordinal.getUC_STATIC_INIT()).as_ast();
            return init.is_a(ast_class.getUC_STATIC_INITIALIZER());
        }catch (result r){
            return false;
        }
    }

    public static boolean isFunctionPointer(ast variable){
        try {
            ast type = variable.get(ast_ordinal.getBASE_TYPE()).as_ast();
            if(type.is_a(ast_class.getUC_POINTER())){
                ast pointto = type.get(ast_ordinal.getUC_POINTED_TO()).as_ast();
                if(pointto.is_a(ast_class.getUC_ROUTINE()))
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    //expression ast
    public static String getVariableName(ast no_ast, boolean isGlobal)throws result{
        ast variable_ast = no_ast.children().get(0).as_ast();
        if(variable_ast.is_a(ast_class.getNC_POINTEREXPR())){
            //ptr->array-ref->
            ast ptr = variable_ast.children().get(0).as_ast().children().get(0).as_ast();
            if(isGlobal)
                return ptr.children().get(0).as_ast().pretty_print();
            else
                return normalizingVariableName(ptr.children().get(0).as_ast());
        }else if(variable_ast.is_a(ast_class.getNC_ARRAYREF())){

        }else if(variable_ast.is_a(ast_class.getNC_POINTERREF())){

        }else if(variable_ast.is_a(ast_class.getNC_VARIABLE())){
            if(isGlobal)
                return variable_ast.pretty_print();
            else
                return normalizingVariableName(variable_ast);
        }
        return null;
    }

    public static void dumpASTWITHClass(ast target)throws result{
        dumpAST(target,0,target.get_class().name());
    }
    public static void dumpAST(ast target, int mode, String tag){
        try {
            ast_field_vector afv = null;
            switch (mode){
                case 0:
                    afv = target.fields();
                    for(int i=0;i<afv.size();i++)
                        System.out.println(tag+" fields: "+afv.get(i).toString());
                    break;
                case 1:
                    afv = target.children();
                    for(int i=0;i<afv.size();i++)
                        System.out.println(tag+" children: "+afv.get(i).toString());
                    break;
                case 2:
                    afv = target.attributes();
                    for(int i=0;i<afv.size();i++)
                        System.out.println(tag+" attributes: "+afv.get(i).toString());
                    break;
                default:
                    afv = target.fields();
                    for(int i=0;i<afv.size();i++)
                        System.out.println(tag+" fields: "+afv.get(i).toString());
                    break;
            }
        }catch (result r){

        }
    }

    public static int tempvarusedLocation(ast no_ast)throws result{
        int i=0;
        if(no_ast.children().get(0).as_ast().pretty_print().contains("$temp") &&
                no_ast.children().get(1).as_ast().pretty_print().contains("$temp")){
            i=2;
        }else if(no_ast.children().get(1).as_ast().pretty_print().contains("$temp")){
            i=1;
        }else if(no_ast.children().get(0).as_ast().pretty_print().contains("$temp")){
            i=0;
        }else
            i=-1;
        return i;
    }


}
