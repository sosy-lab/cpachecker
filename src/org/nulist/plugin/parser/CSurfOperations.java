/**
 * @ClassName CSurfOperations
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/2/19 4:50 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;

public class CSurfOperations {

    /**
     *@Description check if the type of a given variable (p) is constant array or vector
     *@Param [type]  ast_field = p's normalized ast' type (getNC_TYPE)
     *@return boolean
     **/
    public static boolean isConstantArrayOrVector(ast_field type) throws  result {
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
    }

    /**
     *@Description check if a type is a constant struct
     *@Param [type] ast_field = p's normalized ast' type (getNC_TYPE)
     *@return boolean
     **/
    public static boolean isConstantStruct(ast_field type) throws  result {
        String typeName = type.as_ast().pretty_print();

        if(typeName.startsWith("const struct")){
            ast_class ac = type.as_ast().get_class();
            if(ac.equals(ast_class.getUC_STRUCT()))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isConstantAggregateZero(ast_field type) throws result{

        return false;
    }
    /**
     *@Description check if a pointer is null, using its normalized ast
     *@Param [normalizedVarInitAST]
     *@return boolean
     **/
    public static boolean isNullPointer(ast normalizedVarInitAST) throws result{
        ast_field type = normalizedVarInitAST.get(ast_ordinal.getNC_TYPE());//normalized type
        if(type.as_ast().get_class().equals(ast_class.getUC_POINTER())){
            if(normalizedVarInitAST.children().get(1).as_int32()==0){
                return true;
            }
        }
        return false;
    }

    /**
     *@Description check if the assigned value is constant expression, e.g., right child has the type of c:exprs
     *@Param [normalizedVarInitAST]
     *@return boolean
     **/
    public static boolean isConstantExpression(ast normalizedVarInitAST) throws result {
        //normalizedVarInitAST.children().get(1).as_ast().name() = c:exprs
        if(normalizedVarInitAST.children().get(1).as_ast().equals(ast_class.getUC_EXPR_CONSTANT())){
            return true;
        }
        return false;
    }

    /**
     *@Description check if a value is a global constant
     *@Param [expression_point]
     *@return boolean
     **/
    public static boolean isGlobalConstant(final point expression_point) throws result{
        ast no_ast=expression_point.get_ast(ast_family.getC_NORMALIZED());
        ast value_ast = no_ast.children().get(1).as_ast();
        if(value_ast.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global()){//global value
            if(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const"))//const value
                return true;
        }
        return false;
    }

    /**
     *@Description check if a value is a constant, include local and global
     *@Param [normalizedVarInitAST]
     *@return boolean
     **/
    public static boolean isConstant(ast normalizedVarInitAST) throws result {
        ast value_ast = normalizedVarInitAST.children().get(1).as_ast();
        if(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print().startsWith("const")){
            return true;
        }
        return false;
    }

    /**
     *@Description check if the variable in the expression is a global variable
     *@Param [expresiion_point]
     *@return boolean
     **/
    public static boolean isGlobalVariable(point expression_point) throws result{
        return  expression_point.declared_symbol().is_global();
    }





    public static boolean isUndef(ast normalizedVarInitAST) throws result{

        return false;
    }
}
