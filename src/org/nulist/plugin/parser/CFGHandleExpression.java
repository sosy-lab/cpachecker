/**
 * @ClassName CFGHandleExpression
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/12/19 5:27 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.nulist.plugin.parser.CFABuilder.pointerOf;
import static org.nulist.plugin.parser.CFGAST.*;
import static org.sosy_lab.cpachecker.cfa.types.c.CVoidType.VOID;

public class CFGHandleExpression {
    private final CFGTypeConverter typeConverter;
    private final CBinaryExpressionBuilder binExprBuilder;
    public Map<Integer, CSimpleDeclaration> variableDeclarations;
    public Map<Integer, ADeclaration> globalVariableDeclarations;
    private final String functionName;

    public CFGHandleExpression(LogManager pLogger,
                               String pFunctionName,
                               CFGTypeConverter typeConverter){
        this.typeConverter = typeConverter;
        this.variableDeclarations = new HashMap<>();
        this.globalVariableDeclarations = new HashMap<>();
        functionName = pFunctionName;
        binExprBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLogger);
    }

    public CFGHandleExpression(LogManager pLogger,
                               CFGTypeConverter typeConverter,
                               String pFunctionName,
                               Map<Integer, CSimpleDeclaration> variableDeclarations,
                               Map<Integer, ADeclaration> globalVariableDeclarations){
        this.typeConverter = typeConverter;
        this.variableDeclarations = variableDeclarations;
        this.globalVariableDeclarations = globalVariableDeclarations;
        binExprBuilder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLogger);
        functionName = pFunctionName;
    }

    public void setGlobalVariableDeclarations(Map<Integer, ADeclaration> globalVariableDeclarations) {
        this.globalVariableDeclarations = globalVariableDeclarations;
    }

    public void setVariableDeclarations(Map<Integer, CSimpleDeclaration> variableDeclarations) {
        this.variableDeclarations = variableDeclarations;
    }

    public CBinaryExpression getBinaryExpression(final ast condition, FileLocation fileLocation) throws result {
        // the only one supported now

        CBinaryExpression.BinaryOperator operator = getBinaryOperator(condition);

        ast variable_ast = condition.children().get(0).as_ast();
        CType op1Type = typeConverter.getCType(variable_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        ast value_ast = condition.children().get(1).as_ast();
        CType op2Type = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

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
    public CExpression getAssignedIdExpression(CSimpleDeclaration assignedVarDeclaration,
                                               final CType pExpectedType, final FileLocation fileLocation){

        String assignedVarName = assignedVarDeclaration.getName();
        if(assignedVarName.equals("functionTest5$return"))
            System.out.println();
        CType expressionType = assignedVarDeclaration.getType();
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
                throw new AssertionError("Unhandled type structure "+ assignedVarName+ " "+ expressionType.toString());
            }
        } else if (expressionType instanceof CPointerType) {
            return new CPointerExpression(fileLocation, pExpectedType, idExpression);
        } else {
            throw new AssertionError("Unhandled types structure " + assignedVarName+ " "+ expressionType.toString());
        }

    }

    /**
     * Returns the id expression to an already declared variable.
     */
    public CExpression getAssignedIdExpression(
            final ast variable_ast, final CType pExpectedType, final FileLocation fileLocation) throws result{
        //logger.log(Level.FINE, "Getting var declaration for point");
        symbol variableSymbol = variable_ast.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();
        String assignedVarName;
        if(variableSymbol.is_formal() ||
                variableSymbol.is_global() ||
                variableSymbol.get_kind().equals(symbol_kind.getRETURN()))
            assignedVarName = variableSymbol.get_ast().pretty_print();
        else
            assignedVarName = normalizingVariableName(variable_ast);

        String normalizedVarName = getNormalizedVariableName(variableSymbol, assignedVarName, fileLocation.getFileName());

        boolean isGlobal = variableSymbol.is_global();
        boolean isFileStatic = variableSymbol.is_file_static();

        if((isGlobal || isFileStatic) && !globalVariableDeclarations.containsKey(normalizedVarName.hashCode())){
            throw new RuntimeException("Global variable has no declaration: " + normalizedVarName);
        }

        if(!isGlobal && !isFileStatic && !variableDeclarations.containsKey(normalizedVarName.hashCode())) {
            throw new RuntimeException("Local variable has no declaration: " + normalizedVarName);
        }
        CSimpleDeclaration assignedVarDeclaration;
        if(isGlobal || isFileStatic){
            assignedVarDeclaration = (CSimpleDeclaration) globalVariableDeclarations.get(normalizedVarName.hashCode());
        }else {
            assignedVarDeclaration = variableDeclarations.get(normalizedVarName.hashCode());
        }
        return getAssignedIdExpression(assignedVarDeclaration, pExpectedType, fileLocation);
    }


    public String getNormalizedVariableName(symbol variable, String originName, String fileName)throws result{

        if(variable.is_file_static()){
            return getSimpleFileName(fileName)+"__static__"+originName;
        }else if(variable.is_local_static()){
            return functionName+"__static__"+originName;
        }else return originName;
    }

    public String getSimpleFileName(String pFileNmae){
        return pFileNmae.substring(pFileNmae.lastIndexOf('/')+1).replace(".c","").replace("-","_");
    }

    public CExpression getNull(final FileLocation pLocation, final CType pType) {
        return new CIntegerLiteralExpression(pLocation, pType, BigInteger.ZERO);
    }

    //functionassignstatement has been handled in function call
    public CStatement getAssignStatement(ast no_ast, FileLocation fileLocation)throws result{
        assert no_ast.get_class().equals(ast_class.getNC_NORMALASSIGN());

        ast left_ast = no_ast.children().get(0).as_ast();
        CType leftType = typeConverter.getCType(left_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CLeftHandSide leftHandSide  = (CLeftHandSide) getExpression(left_ast, leftType, fileLocation);

        ast value_ast = no_ast.children().get(1).as_ast();

        CType rightType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression rightHandSide = getExpression(value_ast, rightType, fileLocation);

        return new CExpressionAssignmentStatement(fileLocation, leftHandSide, rightHandSide);
    }

    public CVariableDeclaration generateInitVarDeclFromUC(ast un_ast, FileLocation fileLocation) throws result{
        assert un_ast.get_class().is_subclass_of(ast_class.getUC_INIT());

        ast init = un_ast.get(ast_ordinal.getUC_DYNAMIC_INIT()).as_ast();
        ast variable = init.get(ast_ordinal.getUC_VARIABLE()).as_ast();
        symbol varSymbol = variable.get(ast_ordinal.getBASE_ABS_LOC()).as_symbol();
        CType cType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());

        CInitializer initializer = getInitializerFromUC(init, cType, fileLocation);

        return generateVariableDeclaration(varSymbol,initializer,fileLocation);
    }

    /**
     *@Description CodeSurfer splits declaration and initialization, e.g., int i=0;-->int i; i=0;
     *             We need to combine the declaration and initialization. TODO
     *@Param [variable]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration
     **/
    public CVariableDeclaration generateVariableDeclaration(symbol variable,
                                                            CInitializer initializer,
                                                            FileLocation fileLocation)throws result{

        String assignedVar = variable.name().replace("-","_");//name-id-->name_id

        if(variableDeclarations.containsKey(assignedVar.hashCode()))
            return (CVariableDeclaration) variableDeclarations.get(assignedVar.hashCode());

        if(variable.get_kind().equals(symbol_kind.getRETURN()) ||
                variable.get_kind().equals(symbol_kind.getUSER()) ||
                variable.get_kind().equals(symbol_kind.getRESULT())){
            CStorageClass storageClass;
            if(variable.get_kind().equals(symbol_kind.getRETURN()))
                storageClass = CStorageClass.AUTO;
            else
                storageClass = getStorageClass(variable.get_ast());

            String normalizedName = assignedVar;
            if(storageClass==CStorageClass.STATIC){
                storageClass = CStorageClass.AUTO;
                normalizedName = functionName+"__static__"+assignedVar;
            }

            CType varType = typeConverter.getCType(variable.get_ast().get(ast_ordinal.getBASE_TYPE()).as_ast());

            CVariableDeclaration newVarDecl =
                    new CVariableDeclaration(
                            fileLocation,
                            variable.is_global(),
                            storageClass,
                            varType,
                            normalizedName,
                            assignedVar,
                            normalizedName,
                            initializer);
            variableDeclarations.put(normalizedName.hashCode(),newVarDecl);
            return newVarDecl;
        }else {
            throw new RuntimeException("Incorrectly calling generateVariableDeclaration from "+ variable.as_string());
        }

    }


    public CStatement getAssignStatementFromUC(ast un_ast, FileLocation fileLocation)throws result{

        CLeftHandSide leftHandSide = null;
        CExpression rightHandSide = null;
        CType cType = typeConverter.getCType(un_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());


        if(un_ast.is_a(ast_class.getUC_INIT())){
            throw new RuntimeException("Init node has no assignstatement"+ un_ast.toString());
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_ASSIGN())){//a=b+1, a=4
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_PRE_DECR())){//--a;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_PRE_INCR())){//++a
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_POST_DECR())){//a--
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_POST_INCR())){//a++
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_ADD_ASSIGN())){//b+=10;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_DIVIDE_ASSIGN())){//b/=10;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_MULTIPLY_ASSIGN())){//b*=10;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_SUBTRACT_ASSIGN())){//b-=10;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_AND_ASSIGN())){//b&=1;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_OR_ASSIGN())){//b|=1;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_XOR_ASSIGN())){//b^=1;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_REMAINDER_ASSIGN())){//b%=1;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_SHIFTL_ASSIGN())){//b<<=1;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_GENERIC_SHIFTR_ASSIGN())){//b>>=1;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
        }else if(un_ast.is_a(ast_class.getUC_AND_ASSIGN())){//b&=1;
            ast operands = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();

        }else if(un_ast.is_a(ast_class.getUC_FUNCTION_CALL())){//actually, is assign, the follow are for

        }else if(un_ast.is_a(ast_class.getUC_INDIRECT())){//function$return=*(p+2)
            ast value_ast = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = value_ast.children().get(0).as_ast()
                    .get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();

            CExpression pointer = getExpressionFromUNNormalizedAST(variable.get(ast_ordinal.getUC_VARIABLE())
                    .as_ast(), cType, fileLocation);
            ast constant = value_ast.children().get(0).as_ast()
                    .get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(1).as_ast();
            ast indexAST = constant.get(ast_ordinal.getUC_CONSTANT()).as_ast()
                    .get(ast_ordinal.getBASE_VALUE()).as_ast();
            CType indexType = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression indexExpr = getExpressionFromUNNormalizedAST(indexAST,indexType, fileLocation);
            CPointerType pointerType = (CPointerType) pointer.getExpressionType();
            CExpression operand = buildBinaryExpression(pointer,indexExpr, CBinaryExpression.BinaryOperator.PLUS);
            CExpression expression = new CPointerExpression(fileLocation, pointerType, operand);
        }else if(un_ast.is_a(ast_class.getUC_SUBSCRIPT())){//function$return=array[index]
            ast value_ast = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast array_ast = value_ast.children().get(0).as_ast();
            ast index_ast = value_ast.children().get(1).as_ast();

            CExpression arrayExpr = getExpressionFromUNNormalizedAST(
                    array_ast.children().get(0).as_ast(), cType, fileLocation);
            CType indexType = typeConverter.getCType(index_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

            CExpression subscriptExpr = getExpressionFromUNNormalizedAST(index_ast,indexType, fileLocation);
            CExpression expression= new CArraySubscriptExpression(fileLocation, cType, arrayExpr, subscriptExpr);
        }else if(un_ast.is_a(ast_class.getUC_ADDRESS_OP())){//function$return=&a;
            ast value_ast = un_ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = value_ast.children().get(0).as_ast();
            CType variableType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            ast variable_ast = variable.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            CExpression operand = getExpressionFromUNNormalizedAST(variable_ast,variableType, fileLocation);
            CType unaryType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression expression=  new CUnaryExpression(fileLocation, unaryType, operand,
                    CUnaryExpression.UnaryOperator.AMPER);
        }else if(un_ast.is_a(ast_class.getUC_FLOAT_VALUE())){//function$return=0.2;

        }else if(un_ast.is_a(ast_class.getUC_INTEGER_VALUE())){//function$return=2;

        }else if(un_ast.is_a(ast_class.getUC_EXPR_VARIABLE())){//function$return=r;
            ast variable = un_ast.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            CExpression expression = getAssignedIdExpression(variable,cType, fileLocation);
        }else {
            throw new RuntimeException("Unsupported expression "+ un_ast.toString());
        }

        return new CExpressionAssignmentStatement(fileLocation,leftHandSide,rightHandSide);
    }


    /**
     *@Description get expression from unnormalized ast for righthandside
     *@Param [ast, type, fileLocation]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    public CExpression getExpressionFromUNNormalizedAST(ast ast, CType expectedType, FileLocation fileLocation) throws result{
        if(ast.is_a(ast_class.getUC_EXPR_CONSTANT())){//=1,true
            ast constant =ast.get(ast_ordinal.getUC_CONSTANT()).as_ast();
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
        }else if(ast.is_a(ast_class.getUC_EXPR_VARIABLE())){//=a
            ast variable = ast.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            return getAssignedIdExpression(variable,expectedType, fileLocation);
        }else if(ast.is_a(ast_class.getUC_VARIABLE())){//=a
            return getAssignedIdExpression(ast,expectedType, fileLocation);
        }else if(ast.is_a(ast_class.getUC_SUBSCRIPT())){// =p[2],
            ast value_ast = ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast array_ast = value_ast.children().get(0).as_ast();
            ast index_ast = value_ast.children().get(1).as_ast();

            CExpression arrayExpr = getExpressionFromUNNormalizedAST(array_ast.children().get(0).as_ast(), expectedType, fileLocation);
            CType indexType = typeConverter.getCType(index_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

            CExpression subscriptExpr = getExpressionFromUNNormalizedAST(index_ast,indexType, fileLocation);
            return new CArraySubscriptExpression(fileLocation, expectedType, arrayExpr, subscriptExpr);
        }else if(ast.is_a(ast_class.getUC_INDIRECT())){//return *(p+1);
            ast value_ast = ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = value_ast.children().get(0).as_ast()
                    .get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(0).as_ast();

            CExpression pointer = getExpressionFromUNNormalizedAST(variable.get(ast_ordinal.getUC_VARIABLE())
                    .as_ast(), expectedType, fileLocation);
            ast constant = value_ast.children().get(0).as_ast()
                    .get(ast_ordinal.getUC_OPERANDS()).as_ast().children().get(1).as_ast();
            ast indexAST = constant.get(ast_ordinal.getUC_CONSTANT()).as_ast()
                    .get(ast_ordinal.getBASE_VALUE()).as_ast();
            CType indexType = typeConverter.getCType(constant.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression indexExpr = getExpressionFromUNNormalizedAST(indexAST,indexType, fileLocation);
            CPointerType pointerType = (CPointerType) pointer.getExpressionType();
            CExpression operand = buildBinaryExpression(pointer,indexExpr, CBinaryExpression.BinaryOperator.PLUS);
            return new CPointerExpression(fileLocation, pointerType, operand);
        }else if(ast.is_a(ast_class.getUC_ADDRESS_OP())){//return &d;
            ast value_ast = ast.get(ast_ordinal.getUC_OPERANDS()).as_ast();
            ast variable = value_ast.children().get(0).as_ast();
            CType variableType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            ast variable_ast = variable.get(ast_ordinal.getUC_VARIABLE()).as_ast();
            CExpression operand = getExpressionFromUNNormalizedAST(variable_ast,variableType, fileLocation);
            CType cType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());
            return new CUnaryExpression(fileLocation, cType, operand, CUnaryExpression.UnaryOperator.AMPER);
        }else {
            throw new RuntimeException("Not support this return type"+ ast.toString());
        }

    }

    //normalized node
    public CExpression getExpression(ast value_ast, CType valueType,FileLocation fileLoc)throws result{
        if(isVariable(value_ast)){//e.g., a = b;
            return getAssignedIdExpression(value_ast, valueType, fileLoc);
        }else if(isValue(value_ast)){//a=2;

            CBasicType basicType = ((CSimpleType) valueType).getType();
            if (basicType == CBasicType.FLOAT || basicType == CBasicType.DOUBLE) {
                BigDecimal value = BigDecimal.valueOf(value_ast.get(ast_ordinal.getBASE_VALUE()).as_flt32());
                return new CFloatLiteralExpression(fileLoc, valueType,value);
            }else if (basicType == CBasicType.BOOL) {
                long value = value_ast.get(ast_ordinal.getBASE_VALUE()).as_uint32();
                assert value==0||value==1;
                if(value==0)
                    return new CIntegerLiteralExpression(
                            fileLoc, CNumericTypes.BOOL, BigInteger.ZERO);//false
                else
                    return new CIntegerLiteralExpression(
                            fileLoc, CNumericTypes.BOOL, BigInteger.ONE);//true
            }else if(basicType == CBasicType.INT){
                BigInteger value = getBasicValue(value_ast);
                if(value==null)
                    throw new RuntimeException("Problem in getting value of "+ value_ast.toString());
                return new CIntegerLiteralExpression(fileLoc,valueType,value);
            }else {
                throw  new RuntimeException("Unsupported type "+valueType.toString());
            }
        }else if(value_ast.get_class().is_subclass_of(ast_class.getNC_ABSTRACT_ARITHMETIC()) ||//e.g., int i = a+1;
                value_ast.get_class().is_subclass_of(ast_class.getNC_ABSTRACT_BITWISE())){// int i =a&1;
            return createFromArithmeticOp(value_ast,fileLoc);
        }else if(isStructElementExpr(value_ast)){//struct element, e.g., int p = astruct.a;

            ast variable = value_ast.children().get(0).as_ast();
            CType varType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression variableExpr = getAssignedIdExpression(variable,varType, fileLoc);
            String fieldName = value_ast.children().get(1).get(ast_ordinal.getBASE_NAME()).as_str();

            return new CFieldReference(fileLoc, valueType, fieldName, variableExpr,
                    valueType instanceof CPointerType);

        }else if(isPointerAddressExpr(value_ast)){//pointer address, e.g., char p[30]="say hello", *p1 = &r;
            return getPointerAddrExpr(value_ast, fileLoc);
        }else if(isZeroInitExpr(value_ast)){//zero initialization, e.g., char *p=NULL, p1[30]={} (aggreate);
                                            // castexpr
                                            // for an array, child 0 *&p[0], child 1 (int[30])0
                                            // for a pointer, child 0: p, child 1: (void*)0
            CType castType = typeConverter.getCType(value_ast.get(ast_ordinal.getNC_TYPE()).as_ast());
            ast operandAST = value_ast.children().get(1).as_ast();
            CType operandType = typeConverter.getCType(operandAST.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression operand = getExpression(operandAST,operandType, fileLoc);
            return new CCastExpression(fileLoc, castType, operand);
        }else if(isPointerExpr(value_ast)){//pointer, e.g., int i = *(p+1);
            CPointerType pointerType = new CPointerType(false,false,valueType);
            CBinaryExpression operand = getBinaryExpression(value_ast,fileLoc);
            return new CPointerExpression(fileLoc, pointerType, operand);
        }else
            throw new RuntimeException("");
    }


    //value_ast.get_class()==[c:addr]
    public CExpression getPointerAddrExpr(ast value_ast, FileLocation fileloc)throws  result{
        assert isPointerAddressExpr(value_ast);
        //addr->(array-ref -> variable | string) | variable
        ast variable = value_ast.children().get(0).as_ast();
        CType cType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        if(isVariable(variable)){
            CType variableType = typeConverter.getCType(variable.get(ast_ordinal.getBASE_TYPE()).as_ast());
            CExpression operand = getExpression(variable,variableType, fileloc);
            return new CUnaryExpression(fileloc, cType, operand, CUnaryExpression.UnaryOperator.AMPER);
        } else if(variable.is_a(ast_class.getNC_ARRAYREF())){
            ast pointedto = variable.children().get(0).as_ast();
            if(isVariable(pointedto)){
                CType variableType = typeConverter.getCType(pointedto.get(ast_ordinal.getBASE_TYPE()).as_ast());
                CExpression operand = getExpression(pointedto, variableType, fileloc);
                return new CUnaryExpression(fileloc, cType, operand, CUnaryExpression.UnaryOperator.AMPER);
            }else if(pointedto.is_a(ast_class.getNC_STRING())){
                String value = pointedto.get(ast_ordinal.getBASE_VALUE()).as_str();
                cType = new CPointerType(true,false,CNumericTypes.CHAR);
                return new CStringLiteralExpression(fileloc, cType, value);
            }
        }

        return null;
    }


    public CExpression createFromArithmeticOp(
            final ast value_ast, final FileLocation fileLocation) throws result {

        CBinaryExpression.BinaryOperator operator = getBinaryOperator(value_ast);

        final CType expressionType = typeConverter.getCType(value_ast.children().get(0).as_ast());

        ast operand1 = value_ast.children().get(0).as_ast(); // First operand
        //logger.log(Level.FINE, "Getting id expression for operand 1");
        CType op1type = typeConverter.getCType(operand1.get(ast_ordinal.getBASE_TYPE()).as_ast());
        CExpression operand1Exp = getExpression(operand1,op1type, fileLocation);

        ast operand2 =  value_ast.children().get(1).as_ast(); // Second operand
        CType op2type = typeConverter.getCType(operand2.get(ast_ordinal.getBASE_TYPE()).as_ast());
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


    // local: UC_INIT-->dynamic init-->const|expr...,
    // global: exprpoint-->un-ast-->static init-->
    /**
     * @Decription
     * local: UC_INIT-->dynamic init-->const|expr...,
     * global: exprpoint-->un-ast-->static init-->
     * input is dynamic init or static init
     **/
    public CInitializer getInitializerFromUC(ast init, CType type, FileLocation fileLocation) throws result{
        CInitializer initializer = null;
        if(init.is_a(ast_class.getUC_DYNAMIC_INIT_CONSTANT())){
            ast constant = init.get(ast_ordinal.getUC_CONSTANT()).as_ast();
            CExpression expression = getExpressionFromUNNormalizedAST(constant, type, fileLocation);
            initializer = new CInitializerExpression(fileLocation, expression);
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_EXPRESSION())){
            ast expr = init.get(ast_ordinal.getUC_EXPR()).as_ast();
        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_CONSTRUCTOR())){

        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_BITWISE_COPY())){

        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_NONCONSTANT_AGGREGATE())){

        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_ZERO())){

        }else if(init.is_a(ast_class.getUC_DYNAMIC_INIT_NONE())){

        }else if(init.is_a(ast_class.getUC_STATIC_INITIALIZER())){

        }else if(init.is_a(ast_class.getUC_NO_INITIALIZER())){
            return null;
        }else
            throw new RuntimeException("Unsupport initializer "+ init.toString());

        return initializer;
    }

    public CInitializer getInitializer(ast no_ast, final FileLocation fileLocation) throws result{
        CInitializer initializer = null;

        if(isInitializationExpression(no_ast)){
            ast value_ast = no_ast.children().get(1).as_ast();
            ast type_ast = no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();
            CType valueType = typeConverter.getCType(value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

            if (isConstantAggregateZero(value_ast, valueType)) {
                CType expressionType = typeConverter.getCType(type_ast);
                initializer = getZeroInitializer(expressionType, fileLocation);
            }else if(isArrayType(type_ast) ||
                    isStructType(type_ast) ||
                    isEnumType(type_ast)){
                initializer = getConstantAggregateInitializer(value_ast, fileLocation);
            } else if(isNullArrayInit(no_ast)){
                List<CInitializer> elementInitializers = new ArrayList<>();
                initializer = new CInitializerList(fileLocation, elementInitializers);
            }else  {
                initializer =  new CInitializerExpression(
                        fileLocation, getExpression(value_ast, valueType, fileLocation));
            }
        }
        return initializer;
    }

    /**
     *@Description handle the aggregate initialization (normalized ast), e.g., int array[5]={1,2,3,4,5};
     *@Param [no_ast, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CInitializer
     **/
    public CInitializer getConstantAggregateInitializer(ast no_ast,
                                                         final FileLocation fileLoc) throws result {

        //ast no_ast = initialPoint.get_ast(ast_family.getC_NORMALIZED());
       // ast_field value = no_ast.children().get(1);
        CType type = typeConverter.getCType(no_ast.get(ast_ordinal.getBASE_TYPE()).as_ast());

        ast_field_vector elements = no_ast.children();
        int length = (int)elements.size();
        if(isNormalExpression(no_ast) && type.equals(CNumericTypes.CHAR)){
            char[] items = new char[length];
            for(int i=0;i<length;i++){
                items[i]=(char) elements.get(i).as_ast().get(ast_ordinal.getBASE_VALUE()).as_int32();
            }
            CType cType = new CPointerType(true,false,CNumericTypes.CHAR);
            CStringLiteralExpression stringLiteralExpression =
                    new CStringLiteralExpression(fileLoc,cType, String.copyValueOf(items,0,items.length-1));
            return new CInitializerExpression(fileLoc,stringLiteralExpression);
        }


        List<CInitializer> elementInitializers = new ArrayList<>(length);
        for(int i=0;i<length;i++){
            ast_field element = elements.get(i);
            ast elementAST = element.as_ast();
            CInitializer elementInitializer;
            ast_field elementType = element.as_ast().get(ast_ordinal.getBASE_TYPE());
            ast elementType_ast = elementType.as_ast();
            if(isConstantAggregateZero(elementAST, type)){
                elementInitializer =
                        getZeroInitializer(typeConverter.getCType(elementType_ast), fileLoc);
            }else if(isArrayType(elementType_ast) ||
                    isStructType(elementType_ast) ||
                    isEnumType(elementType_ast)){
                elementInitializer = getConstantAggregateInitializer(element.as_ast(), fileLoc);
            } else {
                elementInitializer = new CInitializerExpression(
                        fileLoc, (CExpression) getConstant(element.as_ast(),
                                    typeConverter.getCType(elementType_ast),fileLoc));
            }
            elementInitializers.add(elementInitializer);
        }

        CInitializerList aggregateInitializer =
                new CInitializerList(fileLoc, elementInitializers);
        return aggregateInitializer;
    }


    public CInitializer getZeroInitializer(final CType pExpectedType, final FileLocation fileLoc){

        CInitializer init;
        CType canonicalType = pExpectedType.getCanonicalType();
        if (canonicalType instanceof CArrayType) {
            int length = ((CArrayType) canonicalType).getLengthAsInt().getAsInt();
            CType elementType = ((CArrayType) canonicalType).getType().getCanonicalType();
            CInitializer zeroInitializer = getZeroInitializer(elementType, fileLoc);
            List<CInitializer> initializers = Collections.nCopies(length, zeroInitializer);
            init = new CInitializerList(fileLoc, initializers);
        } else if (canonicalType instanceof CCompositeType) {

            List<CCompositeType.CCompositeTypeMemberDeclaration> members = ((CCompositeType) canonicalType).getMembers();
            List<CInitializer> initializers = new ArrayList<>(members.size());
            for (CCompositeType.CCompositeTypeMemberDeclaration m : members) {
                CType memberType = m.getType();
                CInitializer memberInit = getZeroInitializer(memberType, fileLoc);
                initializers.add(memberInit);
            }
            init = new CInitializerList(fileLoc, initializers);

        } else {
            CExpression zeroExpression;
            if (canonicalType instanceof CSimpleType) {
                CBasicType basicType = ((CSimpleType) canonicalType).getType();
                if (basicType == CBasicType.FLOAT || basicType == CBasicType.DOUBLE) {
                    // use expected type for float, not canonical
                    zeroExpression = new CFloatLiteralExpression(fileLoc, pExpectedType, BigDecimal.ZERO);
                } else {
                    zeroExpression = CIntegerLiteralExpression.ZERO;
                }
            } else {
                // use expected type for cast, not canonical
                CType type = new CPointerType(false,false,VOID);
                zeroExpression = new CCastExpression(fileLoc, type, CIntegerLiteralExpression.ZERO);
            }
            init = new CInitializerExpression(fileLoc, zeroExpression);
        }

        return init;
    }

    public CRightHandSide getConstant(ast value_ast, CType pExpectedType, FileLocation fileLoc)
            throws result{

        ast typeAST = value_ast.get(ast_ordinal.getBASE_TYPE()).as_ast();

        if(isNullPointer(value_ast))//null pointer: e.g., p = 0; p= NULL;
        {
            return new CPointerExpression(fileLoc,pExpectedType,getNull(fileLoc,pExpectedType));
        }else if(isUndef(value_ast)){//TODO
            CType constantType = typeConverter.getCType(typeAST);
            String undefName = "__VERIFIER_undef_" + constantType.toString().replace(' ', '_');
            CSimpleDeclaration undefDecl =
                    new CVariableDeclaration(
                            fileLoc,
                            true,
                            CStorageClass.AUTO,
                            pExpectedType,
                            undefName,
                            undefName,
                            undefName,
                            null);
            CExpression undefExpression = new CIdExpression(fileLoc, undefDecl);
            return undefExpression;
        } else if (isVariable(value_ast)) {
            return getAssignedIdExpression(value_ast, pExpectedType, fileLoc);
        } else
            return getExpression(value_ast,pExpectedType, fileLoc);

    }

}
