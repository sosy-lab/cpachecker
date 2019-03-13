/**
 * @ClassName CFGHandleExpression
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/12/19 5:27 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.ast_class;
import com.grammatech.cs.ast_family;
import com.grammatech.cs.ast_ordinal;
import com.grammatech.cs.result;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.nulist.plugin.parser.CFABuilder.pointerOf;

public class CFGHandleExpression {
    private final CFGTypeConverter typeConverter;
    private final CBinaryExpressionBuilder binExprBuilder;
    private Map<Integer, CSimpleDeclaration> variableDeclarations;
    private Map<Integer, ADeclaration> globalVariableDeclarations;

    public CFGHandleExpression(LogManager pLogger,
                               CFGTypeConverter typeConverter){
        this.typeConverter = typeConverter;
        this.variableDeclarations = new HashMap<>();
        this.globalVariableDeclarations = new HashMap<>();
        binExprBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLogger);
    }

    public CFGHandleExpression(LogManager pLogger,
                               CFGTypeConverter typeConverter,
                               Map<Integer, CSimpleDeclaration> variableDeclarations,
                               Map<Integer, ADeclaration> globalVariableDeclarations){
        this.typeConverter = typeConverter;
        this.variableDeclarations = variableDeclarations;
        this.globalVariableDeclarations = globalVariableDeclarations;
        binExprBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLogger);
    }

    public void setGlobalVariableDeclarations(Map<Integer, ADeclaration> globalVariableDeclarations) {
        this.globalVariableDeclarations = globalVariableDeclarations;
    }

    public void setVariableDeclarations(Map<Integer, CSimpleDeclaration> variableDeclarations) {
        this.variableDeclarations = variableDeclarations;
    }

    public CBinaryExpression getBinaryExpression(final CFGAST condition, FileLocation fileLocation) throws result {
        // the only one supported now

        CBinaryExpression.BinaryOperator operator = condition.getBinaryOperator();

        CFGAST variable_ast = (CFGAST) condition.children().get(0).as_ast();
        CType op1Type = typeConverter.getCType((CFGAST) variable_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        CFGAST value_ast = (CFGAST) condition.children().get(1).as_ast();
        CType op2Type = typeConverter.getCType((CFGAST) value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        CCastExpression op1Cast = new CCastExpression(
                fileLocation,
                op1Type,
                getExpression(variable_ast, op1Type, fileLocation));
        CCastExpression op2Cast = new CCastExpression(
                fileLocation,
                op2Type,
                getExpression(value_ast, op2Type, fileLocation));

        return buildBinaryExpression(op1Cast, op2Cast, operator);
    }

    public CBinaryExpression buildBinaryExpression(
            CExpression operand1, CExpression operand2, CBinaryExpression.BinaryOperator op) {
        try {
            return binExprBuilder.buildBinaryExpression(operand1, operand2, op);
        } catch (UnrecognizedCodeException e) {
            e.getParentState();
        }
        return null;
    }

    /**
     * Returns the id expression to an already declared variable. Returns it as a cast, if necessary
     * to match the expected type.
     */
    public CExpression getAssignedIdExpression(CSimpleDeclaration assignedVarDeclaration, final CType pExpectedType, final FileLocation fileLocation){

        String assignedVarName = assignedVarDeclaration.getName();
        CType expressionType = assignedVarDeclaration.getType().getCanonicalType();
        CIdExpression idExpression =
                new CIdExpression(
                        fileLocation, expressionType, assignedVarName, assignedVarDeclaration);

        if (expressionType.canBeAssignedFrom(pExpectedType)) {
            return idExpression;
        } else if (pointerOf(pExpectedType, expressionType)) {
            CType typePointingTo = ((CPointerType) pExpectedType).getType().getCanonicalType();
            if (expressionType.canBeAssignedFrom(typePointingTo)
                    || expressionType.equals(typePointingTo)) {
                return new CUnaryExpression(
                        fileLocation, pExpectedType, idExpression, CUnaryExpression.UnaryOperator.AMPER);
            } else {
                throw new AssertionError("Unhandled type structure");
            }
        } else if (expressionType instanceof CPointerType) {
            return new CPointerExpression(fileLocation, pExpectedType, idExpression);
        } else {
            throw new AssertionError("Unhandled types structure");
        }

    }

    /**
     * Returns the id expression to an already declared variable.
     */
    public CExpression getAssignedIdExpression(
            final CFGAST variable_ast, final CType pExpectedType, final FileLocation fileLocation) throws result{
        //logger.log(Level.FINE, "Getting var declaration for point");

        String assignedVarName =variable_ast.normalizingVariableName();
        boolean isGlobal = variable_ast.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol().is_global();

        if(isGlobal && !globalVariableDeclarations.containsKey(assignedVarName.hashCode())){
            throw new RuntimeException("Global variable has no declaration: " + assignedVarName);
        }

        if(!isGlobal && !variableDeclarations.containsKey(assignedVarName.hashCode())) {
            throw new RuntimeException("Local variable has no declaration: " + assignedVarName);
        }
        CSimpleDeclaration assignedVarDeclaration;
        if(isGlobal){
            assignedVarDeclaration = (CSimpleDeclaration) globalVariableDeclarations.get(assignedVarName.hashCode());
        }else {
            assignedVarDeclaration = variableDeclarations.get(assignedVarName.hashCode());
        }
        return getAssignedIdExpression(assignedVarDeclaration, pExpectedType, fileLocation);
    }

    public CExpression getNull(final FileLocation pLocation, final CType pType) {
        return new CIntegerLiteralExpression(pLocation, pType, BigInteger.ZERO);
    }



    //functionassignstatement has been handled in function call
    public CStatement getAssignStatement(CFGAST no_ast, FileLocation fileLocation)throws result{
        assert no_ast.get_class().equals(ast_class.getNC_NORMALASSIGN());

        CFGAST left_ast = (CFGAST) no_ast.children().get(0).as_ast();
        CType leftType = typeConverter.getCType(left_ast);
        CLeftHandSide leftHandSide  = (CLeftHandSide) getExpression(left_ast, leftType, fileLocation);

        CFGAST value_ast = (CFGAST) no_ast.children().get(1).as_ast();

        CType rightType = typeConverter.getCType(value_ast);
        CExpression rightHandSide = getExpression(value_ast, rightType, fileLocation);

        return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
    }

    //from unnormalized ast
    public CExpressionAssignmentStatement getAssignExpressionFromExprPoint(CFGNode exprNode, FileLocation fileLocation)throws result{
        CFGAST un_ast = (CFGAST) exprNode.get_ast(ast_family.getC_UNNORMALIZED());
        CLeftHandSide leftHandSide = null;
        CExpression rightHandSide = null;
        CType cType;
        if(un_ast.equalClass(ast_class.getUC_INIT())){
            CFGAST init = (CFGAST) un_ast.get(ast_ordinal.getUC_DYNAMIC_INIT()).as_ast();
            CFGAST variable = (CFGAST) init.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            cType = typeConverter.getCType((CFGAST) variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            leftHandSide = (CLeftHandSide) getExpression(variable, cType, fileLocation);
            if(init.equalClass(ast_class.getUC_DYNAMIC_INIT_CONSTANT())){
                CFGAST constant = (CFGAST) init.get(ast_ordinal.getUC_CONSTANT()).as_ast();
            }else if(init.equalClass(ast_class.getUC_DYNAMIC_INIT_EXPRESSION())){

            }else if(init.equalClass(ast_class.getUC_DYNAMIC_INIT_BITWISE_COPY())){

            }

        }else if(un_ast.equalClass(ast_class.getUC_GENERIC_ASSIGN())){

        }else if(un_ast.equalClass(ast_class.getUC_GENERIC_PRE_INCR())){

        }else if(un_ast.equalClass(ast_class.getUC_GENERIC_PRE_INCR())){

        }else if(un_ast.equalClass(ast_class.getUC_GENERIC_ASSIGN())){

        }else if(un_ast.equalClass(ast_class.getUC_GENERIC_ASSIGN())){

        }else if(un_ast.equalClass(ast_class.getUC_GENERIC_ASSIGN())){

        }

        return new CExpressionAssignmentStatement(fileLocation,leftHandSide,rightHandSide);

    }

    /**
     *@Description get expression from unnormalized ast for righthandside
     *@Param [ast, type, fileLocation]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getExpressionFromUNNormalizedAST(CFGAST ast, CType expectedType, FileLocation fileLocation) throws result{
        if(ast.equalClass(ast_class.getUC_DYNAMIC_INIT_CONSTANT())){
            CFGAST constant = (CFGAST) ast.get(ast_ordinal.getUC_CONSTANT()).as_ast();
            return getExpressionFromUNNormalizedAST(constant, expectedType, fileLocation);
        }
//        else if(ast.equalClass(ast_class.getUC_DYNAMIC_INIT_EXPRESSION())){//=
//
//        }else if(ast.equalClass(ast_class.getUC_DYNAMIC_INIT_BITWISE_COPY())){
//
//        }
        else if(ast.equalClass(ast_class.getUC_EXPR_CONSTANT())){//=1,true
            CFGAST constant =(CFGAST) ast.get(ast_ordinal.getUC_CONSTANT()).as_ast();
            if(expectedType.equals(CNumericTypes.BOOL)){
                long value = constant.get(ast_ordinal.getBASE_VALUE()).as_uint32();
                assert value==0||value==1;
                if(value==0)
                    return new CIntegerLiteralExpression(
                            fileLocation, CNumericTypes.BOOL, BigInteger.ZERO);//false
                else
                    return new CIntegerLiteralExpression(
                            fileLocation, CNumericTypes.BOOL, BigInteger.ONE);//true
            }else if(expectedType.getCanonicalType().equals(CNumericTypes.CHAR)){
                char value = (char)constant.get(ast_ordinal.getBASE_VALUE()).as_int8();
                return new CCharLiteralExpression(fileLocation, expectedType, value);
            }else if(expectedType.getCanonicalType().equals(CNumericTypes.INT)){
                BigInteger value = BigInteger.valueOf(constant.get(ast_ordinal.getBASE_VALUE()).as_int32());
                return new CIntegerLiteralExpression(fileLocation,expectedType,value);
            }else if(expectedType.getCanonicalType().equals(CNumericTypes.FLOAT)){
                BigDecimal value = BigDecimal.valueOf(constant.get(ast_ordinal.getBASE_VALUE()).as_flt32());
                return new CFloatLiteralExpression(fileLocation, expectedType,value);
            } else {
                throw  new RuntimeException("Unsupported type "+expectedType.toString());
            }
            //return getExpressionFromUNNormalizedAST(constant, expectedType, fileLocation);
        }else if(ast.equalClass(ast_class.getUC_EXPR_VARIABLE())){//=a
            CFGAST variable = (CFGAST) ast.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            return getAssignedIdExpression(variable,expectedType,fileLocation);
        }else if(ast.equalClass(ast_class.getUC_VARIABLE())){//=a
            return getAssignedIdExpression(ast,expectedType,fileLocation);
        }else if(ast.equalClass(ast_class.getUC_SUBSCRIPT())){// =p[2],
            CFGAST value_ast = (CFGAST) ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            CFGAST array_ast = (CFGAST) value_ast.children().get(0).as_ast();
            CFGAST index_ast = (CFGAST) value_ast.children().get(1).as_ast();

            CExpression arrayExpr = getExpressionFromUNNormalizedAST((CFGAST) array_ast.children().get(0).as_ast(), expectedType, fileLocation);
            CType indexType = typeConverter.getCType((CFGAST) index_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

            CExpression subscriptExpr = getExpressionFromUNNormalizedAST(index_ast,indexType,fileLocation);
            return new CArraySubscriptExpression(fileLocation, expectedType, arrayExpr, subscriptExpr);
        }else if(ast.equalClass(ast_class.getUC_INDIRECT())){//return *(p+1);
            CFGAST value_ast = (CFGAST) ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            CFGAST variable = (CFGAST) value_ast.children().get(0).as_ast()
                    .get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();

            CExpression pointer = getExpressionFromUNNormalizedAST((CFGAST)variable.get(ast_ordinal.getUC_VARIABLE())
                    .as_ast(), expectedType,fileLocation);
            CFGAST constant = (CFGAST) value_ast.children().get(0).as_ast()
                    .get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(1).as_ast();
            CFGAST indexAST = (CFGAST) constant.get(ast_ordinal.getUC_CONSTANT()).as_ast()
                    .get(ast_ordinal.getBASE_VALUE()).as_ast();
            CType indexType = typeConverter.getCType((CFGAST) constant.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression indexExpr = getExpressionFromUNNormalizedAST(indexAST,indexType,fileLocation);
            CPointerType pointerType = (CPointerType) pointer.getExpressionType();
            CExpression operand = buildBinaryExpression(pointer,indexExpr, CBinaryExpression.BinaryOperator.PLUS);
            return new CPointerExpression(fileLocation, pointerType, operand);
        }else if(ast.equalClass(ast_class.getUC_ADDRESS_OP())){//return &d;

            CFGAST value_ast = (CFGAST) ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            CFGAST variable = (CFGAST) value_ast.children().get(0).as_ast();
            CType variableType = typeConverter.getCType((CFGAST) variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CFGAST variable_ast = (CFGAST) variable.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            CExpression operand = getExpressionFromUNNormalizedAST(variable_ast,variableType,fileLocation);
            CType cType = typeConverter.getCType((CFGAST)value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return new CUnaryExpression(fileLocation, cType, operand, CUnaryExpression.UnaryOperator.AMPER);

        }else {
            throw new RuntimeException("Not support this return type"+ ast.toString());
        }

    }

    public CExpression getExpression(CFGAST value_ast, CType valueType, FileLocation fileLoc)throws result{
        if(value_ast.isVariable()){//e.g., a = b;
            return getAssignedIdExpression(value_ast, valueType, fileLoc);
        }else if(value_ast.isValue()){//a=2;
            if(valueType.equals(CNumericTypes.BOOL)){
                long value = value_ast.get(ast_ordinal.getBASE_VALUE()).as_uint32();
                assert value==0||value==1;
                if(value==0)
                    return new CIntegerLiteralExpression(
                            fileLoc, CNumericTypes.BOOL, BigInteger.ZERO);//false
                else
                    return new CIntegerLiteralExpression(
                            fileLoc, CNumericTypes.BOOL, BigInteger.ONE);//true
            }else if(valueType.equals(CNumericTypes.CHAR)){
                char value = (char)value_ast.get(ast_ordinal.getBASE_VALUE()).as_int8();
                return new CCharLiteralExpression(fileLoc, valueType, value);
            }else if(valueType.equals(CNumericTypes.INT)){
                BigInteger value = BigInteger.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_int32());
                return new CIntegerLiteralExpression(fileLoc,valueType,value);
            }else if(valueType.equals(CNumericTypes.FLOAT)||valueType.equals(CNumericTypes.DOUBLE)){
                BigDecimal value = BigDecimal.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_flt32());
                return new CFloatLiteralExpression(fileLoc, valueType,value);
            } else {
                throw  new RuntimeException("Unsupported type "+valueType.toString());
            }
        }else if(value_ast.get_class().is_subclass_of(ast_class.getNC_ABSTRACT_ARITHMETIC()) ||//e.g., int i = a+1;
                value_ast.get_class().is_subclass_of(ast_class.getNC_ABSTRACT_BITWISE())){// int i =a&1;
            return createFromArithmeticOp(value_ast, fileLoc);
        }else if(value_ast.isNormalExpression()){//const expression, e.g.,

        }else if(value_ast.isStructElementExpr()){//struct element, e.g., int p = astruct.a;

            CFGAST variable = (CFGAST) value_ast.children().get(0).as_ast();
            CType varType = typeConverter.getCType((CFGAST)variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression variableExpr = getAssignedIdExpression(variable,varType,fileLoc);
            String fieldName = value_ast.children().get(1).get(ast_ordinal.getBASE_NAME()).as_str();

            return new CFieldReference(fileLoc, valueType, fieldName, variableExpr,
                    valueType instanceof CPointerType);

        }else if(value_ast.isPointerAddressExpr()){//pointer address, e.g., char p[30]="say hello", *p1 = &r;
            return getPointerAddrExpr(value_ast, fileLoc);
        }else if(value_ast.isZeroInitExpr()){//zero initialization, e.g., char *p=NULL(), p1[30]={} (aggreate);

        }else if(value_ast.isPointerExpr()){//pointer, e.g., int i = *(p+1);

        }else if(value_ast.equalClass(ast_class.getNC_CASTEXPR())){
            CType castType = typeConverter.getCType((CFGAST)value_ast.get(ast_ordinal.getNC_TYPE()).as_ast());
            CFGAST operandAST = (CFGAST)value_ast.children().get(1).as_ast();
            CType operandType = typeConverter.getCType((CFGAST)operandAST.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression operand = getExpression(operandAST,operandType,fileLoc);
            return new CCastExpression(fileLoc, castType, operand);
        }else if(value_ast.equalClass(ast_class.getNC_ARRAY())){

        }
        throw new RuntimeException("");
    }


    //
    public CExpression getPointerAddrExpr(CFGAST value_ast, FileLocation fileloc)throws  result{
        assert value_ast.isPointerAddressExpr();
        CFGAST variable = (CFGAST) value_ast.children().get(0).as_ast();
        CType cType = typeConverter.getCType((CFGAST)value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        if(variable.isVariable()){
            CType variableType = typeConverter.getCType((CFGAST) variable.get(ast_ordinal.getBASE_TYPE()).as_ast());

            CExpression operand = getExpression(variable,variableType,fileloc);

            return new CUnaryExpression(fileloc, cType, operand, CUnaryExpression.UnaryOperator.AMPER);
        }else {

        }

        return null;
    }

    public CExpression createFromArithmeticOp(
            final CFGAST value_ast, final FileLocation fileLocation) throws result {

        CBinaryExpression.BinaryOperator operator = value_ast.getBinaryOperator();

        final CType expressionType = typeConverter.getCType((CFGAST) value_ast.children().get(0).as_ast());

        CFGAST operand1 = (CFGAST) value_ast.children().get(0).as_ast(); // First operand
        //logger.log(Level.FINE, "Getting id expression for operand 1");
        CType op1type = typeConverter.getCType((CFGAST) operand1.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression operand1Exp = getExpression(operand1,op1type,fileLocation);

        CFGAST operand2 =  (CFGAST) value_ast.children().get(1).as_ast(); // Second operand
        CType op2type = typeConverter.getCType((CFGAST) operand2.get(ast_ordinal.getBASE_TYPE()).as_ast());
        //logger.log(Level.FINE, "Getting id expression for operand 2");
        CExpression operand2Exp = getExpression(operand2, op2type, fileLocation);

        return new CBinaryExpression(
                fileLocation,
                expressionType,
                expressionType,
                operand1Exp,
                operand2Exp,
                operator);
    }
    
}
