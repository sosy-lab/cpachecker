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
    public static boolean isArrayType(ast ast){
        try {
            return ast.is_a(ast_class.getNC_ARRAY()) || ast.is_a(ast_class.getUC_ARRAY());
        }catch (result r){
            return false;
        }
    }


    public static boolean isTypeRef(ast ast){
        try {
            return ast.is_a(ast_class.getUC_TYPEREF());
        }catch (result r){
            return false;
        }
    }


    public static boolean isNullArrayInit(ast ast){
        try {
            ast field1 = ast.children().get(0).as_ast();
            ast field2 = ast.children().get(1).as_ast();

            return isPointerExpr(field1)
                    && isCastExpr(field2)
                    && isArrayType(field2.get(ast_ordinal.getBASE_TYPE()).as_ast())
                    && field2.children().get(1).get(ast_ordinal.getBASE_VALUE()).as_int32()==0;
        }catch (result r){
            return false;
        }
    }

    public static boolean isPointerType(ast ast){
        try {
            return ast.is_a(ast_class.getNC_POINTER())||ast.is_a(ast_class.getUC_POINTER());
        }catch (result r){
            return false;
        }
    }

    public static boolean isStructType(ast ast){
        try {
            return ast.is_a(ast_class.getNC_STRUCT()) || ast.is_a(ast_class.getUC_STRUCT())||
                    ast.get(ast_ordinal.getBASE_TYPE()).as_ast().is_a(ast_class.getUC_STRUCT());
        }catch (result r){
            return false;
        }

    }

    public static boolean isEnumType(ast ast){
        try {
            return ast.is_a(ast_class.getUC_ENUM()) ||
                    ast.get(ast_ordinal.getBASE_TYPE()).as_ast().is_a(ast_class.getUC_ENUM());
        }catch (result r){
            return false;
        }
    }



    public static boolean isConstantAggregateZero(ast ast, CType cType){
        try {
            if(ast.is_a(ast_class.getNC_CASTEXPR())){
                ast type = ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
                return (isPointerType(type)) && ast.children().get(1).get(ast_ordinal.getBASE_VALUE()).as_int32()==0;
            }else if(cType instanceof CPointerType){
                return ast.get(ast_ordinal.getBASE_VALUE()).as_uint32()==0;
            }
            return false;
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
    public static boolean isNullPointer(ast ast){
        try {
            ast_field type = ast.get(ast_ordinal.getNC_TYPE());//normalized type
            if(type.as_ast().is_a(ast_class.getNC_POINTER())){
                if(ast.children().get(1).as_int32()==0){
                    return true;
                }
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isInitializationExpression(ast ast){
        try {
            return ast.get(ast_ordinal.getNC_IS_INITIALIZATION()).as_boolean();
        }catch (result r){
            return false;
        }
    }

    /**
     *@Description check if the assigned value is constant expression, e.g., right child has the type of c:exprs
     *@Param []
     *@return boolean
     **/
    public static boolean isNormalExpression(ast ast){
        try {
            //normalizedVarInitAST.children().get(1).as_ast().name() = c:exprs
            return ast.is_a(ast_class.getNC_EXPRS());
        }catch (result r){
            return false;
        }
    }

    public static boolean isStructElementExpr(ast ast){
        try {
            return ast.is_a(ast_class.getNC_STRUCTORUNIONREF());
        }catch (result r){
            return false;
        }
    }

    public static boolean isPointerExpr(ast ast){
        try {
            return ast.is_a(ast_class.getNC_POINTEREXPR());
        }catch (result r){
            return false;
        }
    }

    public static boolean isPointerAddressExpr(ast ast){
        try {
            return ast.is_a(ast_class.getNC_ADDREXPR());
        }catch (result r){
            return false;
        }
    }

    public static boolean isZeroInitExpr(ast ast){
        try {
            return ast.is_a(ast_class.getNC_CASTEXPR());
        }catch (result r){
            return false;
        }
    }

    public static boolean isCastExpr(ast ast){
        try {
            return ast.is_a(ast_class.getNC_CASTEXPR());
        }catch (result r){
            return false;
        }
    }

/**
 *@Description check if a value is a global constant, using NORMALIZED ast
 *@Param []
 *@return boolean
 **/

    public static boolean isGlobalConstant(ast ast){
        try {
            if(ast.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global()){//global value
                if(ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const"))//const value
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isGlobalVariable(ast ast){
        try {
            if(ast.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global()){//global value
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public static boolean isConstantType(ast ast){
        try {
            return ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const");
        }catch (result r){
            return false;
        }
    }


    /**
     *@Description check if a value is a constant, include local and global
     *@Param [normalizedVarInitAST]
     *@return boolean
     **/
    public static boolean isConstant(ast ast){
        try {
            ast value_ast = ast.children().get(1).as_ast();
            return value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const");
        }catch (result r){
            return false;
        }
    }

    public static ast getStructType(ast ast) throws result{
        if(ast.is_a(ast_class.getNC_STRUCT())){
            return ast;
        }else {//for typedef struct
            return ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
        }
    }

    //ast normalizedVarInitAST
    public static boolean isUndef(ast ast){
        return false;
    }



    public static CStorageClass getStorageClass(ast ast)throws result{
        CStorageClass storageClass;
        switch (ast.get(ast_ordinal.getBASE_STORAGE_CLASS()).as_enum_value_string()){
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

    public static CBinaryExpression.BinaryOperator getBinaryOperator(ast ast)throws result{
        if(ast.is_a(ast_class.getNC_ADDEXPR()))
            return CBinaryExpression.BinaryOperator.PLUS;
        else if(ast.is_a(ast_class.getNC_SUBEXPR()))
            return CBinaryExpression.BinaryOperator.MINUS;
        else if(ast.is_a(ast_class.getNC_MULEXPR()))
            return CBinaryExpression.BinaryOperator.MULTIPLY;
        else if(ast.is_a(ast_class.getNC_DIVEXPR()))
            return CBinaryExpression.BinaryOperator.DIVIDE;
        else if(ast.is_a(ast_class.getNC_MODEXPR()))
            return CBinaryExpression.BinaryOperator.MODULO;
        else if(ast.is_a(ast_class.getNC_RIGHTASSIGN()))
            return CBinaryExpression.BinaryOperator.SHIFT_RIGHT;
        else if(ast.is_a(ast_class.getNC_LEFTASSIGN()))
            return CBinaryExpression.BinaryOperator.SHIFT_LEFT;
        else if(ast.is_a(ast_class.getNC_ANDASSIGN()))
            return CBinaryExpression.BinaryOperator.BINARY_AND;
        else if(ast.is_a(ast_class.getNC_ORASSIGN()))
            return CBinaryExpression.BinaryOperator.BINARY_OR;
        else if(ast.is_a(ast_class.getNC_XORASSIGN()))
            return CBinaryExpression.BinaryOperator.BINARY_XOR;
        else if(ast.is_a(ast_class.getNC_EQUALEXPR()))
            return CBinaryExpression.BinaryOperator.EQUALS;
        else if(ast.is_a(ast_class.getNC_NOTEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.NOT_EQUALS;
        else if(ast.is_a(ast_class.getNC_GREATEXPR()))
            return CBinaryExpression.BinaryOperator.GREATER_THAN;
        else if(ast.is_a(ast_class.getNC_GREATEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.GREATER_EQUAL;
        else if(ast.is_a(ast_class.getNC_LESSEXPR()))
            return CBinaryExpression.BinaryOperator.LESS_THAN;
        else if(ast.is_a(ast_class.getNC_LESSEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.LESS_EQUAL;
        else
            throw new UnsupportedOperationException("Unsupported predicate");
    }

    public static boolean hasRadixField(ast ast)throws result{
        try {
            ast.get(ast_ordinal.getUC_RADIX());
            return true;
        }catch (result r){
            return false;
        }
    }

    public static boolean isVariable(ast ast){
        try {
            return ast.is_a(ast_class.getNC_VARIABLE());
        }catch (result r){
            return false;
        }
    }

    public static boolean isValue(ast ast){
        try {
            return ast.is_a(ast_class.getNC_ABSTRACT_INTEGER_VALUE())||ast.is_a(ast_class.getNC_ABSTRACT_FLOAT_VALUE());
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


}
