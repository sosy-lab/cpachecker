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

public class CFGAST extends ast {

    public CFGAST(long cPtr, boolean cMemoryOwn){
        super(cPtr, cMemoryOwn);
    }

    /**
     *@Description check if the type of a given variable (p) is constant array or vector
     *@Param [type]  ast_field = p's normalized ast' type (getNC_TYPE)
     *@return boolean
     **/
    public static boolean isConstantArrayOrVector(ast_field type){
        try {
            String typeName = type.as_ast().pretty_print();

            if(typeName.startsWith("const")){
                ast_class ac = type.as_ast().get_class();
                if(ac.equals(ast_class.getUC_POINTER())||
                        ac.equals(ast_class.getUC_ARRAY())
                        ||ac.equals(ast_class.getUC_VECTOR_TYPE())
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
    public boolean isArrayType(){
        try {
            return this.get_class().equals(ast_class.getNC_ARRAY());
        }catch (result r){
            return false;
        }
    }

    public boolean isPointerType(){
        try {
            return this.get_class().equals(ast_class.getNC_POINTER());
        }catch (result r){
            return false;
        }
    }

    public boolean isStructType(){
        try {
            return this.get_class().equals(ast_class.getNC_STRUCT()) ||
                    this.get(ast_ordinal.getBASE_TYPE()).as_ast().get_class().equals(ast_class.getUC_STRUCT());
        }catch (result r){
            return false;
        }

    }

    public boolean isEnumType(){
        try {
            return this.get_class().equals(ast_class.getUC_ENUM()) ||
                    this.get(ast_ordinal.getBASE_TYPE()).as_ast().get_class().equals(ast_class.getUC_ENUM());
        }catch (result r){
            return false;
        }
    }



    public static boolean isConstantAggregateZero(ast_field type){

        return false;
    }
    /**
     *@Description check if a pointer is null, using its normalized ast
     *@Param []
     *@return boolean
     **/
    public boolean isNullPointer(){
        try {
            ast_field type = this.get(ast_ordinal.getNC_TYPE());//normalized type
            if(type.as_ast().get_class().equals(ast_class.getUC_POINTER())){
                if(this.children().get(1).as_int32()==0){
                    return true;
                }
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isInitializationExpression(){
        try {
            return this.get(ast_ordinal.getNC_IS_INITIALIZATION()).as_boolean();
        }catch (result r){
            return false;
        }
    }

    /**
     *@Description check if the assigned value is constant expression, e.g., right child has the type of c:exprs
     *@Param []
     *@return boolean
     **/
    public boolean isNormalExpression(){
        try {
            //normalizedVarInitAST.children().get(1).as_ast().name() = c:exprs
            return this.get_class().equals(ast_class.getNC_EXPRS());
        }catch (result r){
            return false;
        }
    }

    public boolean isStructElementExpr(){
        try {
            return this.get_class().equals(ast_class.getNC_STRUCTORUNIONREF());
        }catch (result r){
            return false;
        }
    }

    public boolean isPointerExpr(){
        try {
            return this.get_class().equals(ast_class.getNC_POINTEREXPR());
        }catch (result r){
            return false;
        }
    }

    public boolean isPointerAddressExpr(){
        try {
            return this.get_class().equals(ast_class.getNC_ADDREXPR());
        }catch (result r){
            return false;
        }
    }

    public boolean isZeroInitExpr(){
        try {
            return this.get_class().equals(ast_class.getNC_CASTEXPR());
        }catch (result r){
            return false;
        }
    }

/**
 *@Description check if a value is a global constant, using NORMALIZED ast
 *@Param []
 *@return boolean
 **/

    public boolean isGlobalConstant(){
        try {
            if(this.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global()){//global value
                if(this.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const"))//const value
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isGlobalVariable(){
        try {
            if(this.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global()){//global value
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }

    public boolean isConstantType(){
        try {
            return this.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const");
        }catch (result r){
            return false;
        }
    }


    /**
     *@Description check if a value is a constant, include local and global
     *@Param [normalizedVarInitAST]
     *@return boolean
     **/
    public boolean isConstant(){
        try {
            ast value_ast = this.children().get(1).as_ast();
            return value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const");
        }catch (result r){
            return false;
        }
    }

    public CFGAST getStructType() throws result{
        if(this.get_class().equals(ast_class.getUC_STRUCT())){
            return this;
        }else {//for typedef struct
            return (CFGAST) this.get(ast_ordinal.getBASE_TYPE()).as_ast();
        }
    }

    //ast normalizedVarInitAST
    public boolean isUndef(){
        return false;
    }



    public CStorageClass getStorageClass()throws result{
        CStorageClass storageClass;
        switch (this.get(ast_ordinal.getBASE_STORAGE_CLASS()).as_enum_value_string()){
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
    public String normalizingVariableName()throws result{
        assert this!=null;
        return this.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().name().replace("-","_");
    }

    public CBinaryExpression.BinaryOperator getBinaryOperator()throws result{
        if(this.get_class().equals(ast_class.getNC_ADDEXPR()))
            return CBinaryExpression.BinaryOperator.PLUS;
        else if(this.get_class().equals(ast_class.getNC_SUBEXPR()))
            return CBinaryExpression.BinaryOperator.MINUS;
        else if(this.get_class().equals(ast_class.getNC_MULEXPR()))
            return CBinaryExpression.BinaryOperator.MULTIPLY;
        else if(this.get_class().equals(ast_class.getNC_DIVEXPR()))
            return CBinaryExpression.BinaryOperator.DIVIDE;
        else if(this.get_class().equals(ast_class.getNC_MODEXPR()))
            return CBinaryExpression.BinaryOperator.MODULO;
        else if(this.get_class().equals(ast_class.getNC_RIGHTASSIGN()))
            return CBinaryExpression.BinaryOperator.SHIFT_RIGHT;
        else if(this.get_class().equals(ast_class.getNC_LEFTASSIGN()))
            return CBinaryExpression.BinaryOperator.SHIFT_LEFT;
        else if(this.get_class().equals(ast_class.getNC_ANDASSIGN()))
            return CBinaryExpression.BinaryOperator.BINARY_AND;
        else if(this.get_class().equals(ast_class.getNC_ORASSIGN()))
            return CBinaryExpression.BinaryOperator.BINARY_OR;
        else if(this.get_class().equals(ast_class.getNC_XORASSIGN()))
            return CBinaryExpression.BinaryOperator.BINARY_XOR;
        else if(this.get_class().equals(ast_class.getNC_EQUALEXPR()))
            return CBinaryExpression.BinaryOperator.EQUALS;
        else if(this.get_class().equals(ast_class.getNC_NOTEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.NOT_EQUALS;
        else if(this.get_class().equals(ast_class.getNC_GREATEXPR()))
            return CBinaryExpression.BinaryOperator.GREATER_THAN;
        else if(this.get_class().equals(ast_class.getNC_GREATEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.GREATER_EQUAL;
        else if(this.get_class().equals(ast_class.getNC_LESSEXPR()))
            return CBinaryExpression.BinaryOperator.LESS_THAN;
        else if(this.get_class().equals(ast_class.getNC_LESSEQUALEXPR()))
            return CBinaryExpression.BinaryOperator.LESS_EQUAL;
        else
            throw new UnsupportedOperationException("Unsupported predicate");
    }

    public boolean hasRadixField()throws result{
        try {
            this.get(ast_ordinal.getUC_RADIX());
            return true;
        }catch (result r){
            return false;
        }
    }

    public boolean isVariable(){
        try {
            return this.get_class().equals(ast_class.getNC_VARIABLE());
        }catch (result r){
            return false;
        }
    }

    public boolean isValue(){
        try {
            return this.get_class().is_subclass_of(ast_class.getUC_IS_VALUE_CLASS());
        }catch (result r){
            return false;
        }
    }

    public boolean equalClass(ast_class astClass){
        try {
            return this.is_a(astClass);//this.get_class().equals(astClass);
        }catch (result r){
            return false;
        }
    }

    //a symbol can only have unnormalized ast
    public boolean symbolHasInitialization(){
        try {
            try {
                this.get(ast_ordinal.getUC_DYNAMIC_INIT());
                return true;
            }catch (result r){
                ast init = this.get(ast_ordinal.getUC_STATIC_INIT()).as_ast();
                return init.is_a(ast_class.getUC_STATIC_INITIALIZER());
            }
        }catch (result r){
            return false;
        }
    }


}
