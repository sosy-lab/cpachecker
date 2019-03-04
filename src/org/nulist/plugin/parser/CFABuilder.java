package org.nulist.plugin.parser;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;
import org.sosy_lab.cpachecker.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;

import static org.nulist.plugin.parser.CSurfOperations.*;
import static org.nulist.plugin.util.ClassTool.getUnsignedInt;
import static org.sosy_lab.cpachecker.cfa.types.c.CFunctionType.NO_ARGS_VOID_FUNCTION;

/**
 * @ClassName CFABuilder
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 2/27/19 4:18 PM
 * @Version 1.0
 **/
public class CFABuilder {
    private final LogManager logger;
    private final MachineModel machineModel;

    private final CFGTypeConverter typeConverter;

    // Variable hashCode(getUnsignedInt(point.hashCode());) -> Variable declaration
    private final Map<Long, CSimpleDeclaration> variableDeclarations;
    // Function name -> Function declaration
    private Map<String, CFunctionDeclaration> functionDeclarations;

    // unnamed basic blocks will be named as 1,2,3,...
    private int basicBlockId;
    protected NavigableMap<String, FunctionEntryNode> functions;

    protected SortedSetMultimap<String, CFANode> cfaNodes;
    protected List<Pair<ADeclaration, String>> globalDeclarations;

    public CFABuilder(final LogManager pLogger, final MachineModel pMachineModel) {
        logger = pLogger;
        machineModel = pMachineModel;

        typeConverter = new CFGTypeConverter(machineModel,logger);

        variableDeclarations = new HashMap<>();
        functionDeclarations = new HashMap<>();

        functions = new TreeMap<>();
        cfaNodes = TreeMultimap.create();
        globalDeclarations = new ArrayList<>();
    }

    protected void addNode(String funcName, CFANode nd) {
        cfaNodes.put(funcName, nd);
    }

    public ParseResult build(compunit cu) throws  result {
        boolean isUserDefinedFile = false;
        String pFileName = cu.normalized_name();

        // Iterate over all procedures in the compilation unit
        // procedure = function
        for (compunit_procedure_iterator proc_it = cu.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();
            //only focus on the function defined by user
            if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED())){
                isUserDefinedFile = true;
                point entryPoint = proc.entry_point();

                // handle the function definition
                String funcName = proc.name();

                FunctionEntryNode en = visitFunction(proc, pFileName);
            }
        }
        if(isUserDefinedFile){
            declareGlobalVariables(cu, pFileName);
        }

        return  null;
    }
    
    /**
     *@Description TODO
     *@Param [procedure, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode
     **/
    protected FunctionEntryNode visitFunction(procedure procedure, final String pFileName) throws result {

        logger.log(Level.FINE, "Creating function: " + procedure.name());

        String functionName = procedure.name();

        FunctionExitNode functionExit = new FunctionExitNode(functionName);
        addNode(functionName,functionExit);

        declareFunction(procedure, pFileName);

        point_set points= procedure.points();
        for(point_set_iterator point_it = points.cbegin();
            !point_it.at_end();point_it.advance()){
            point p = point_it.current();
            if(p.get_kind().equals(point_kind.getFORMAL_IN())||
                    p.get_kind().equals(point_kind.getFORMAL_OUT())
            ||p.get_kind().equals(point_kind.getACTUAL_IN())
            ||p.get_kind().equals(point_kind.getACTUAL_OUT())){

            }else if(p.get_kind().equals(point_kind.getBODY())){

            }
        }


        return null;
    }

    /**
     *@Description TODO
     *@Param [compunit, pFileName]
     *@return void
     **/
    private void declareGlobalVariables(compunit compunit, final String pFileName) throws result {

        for (compunit_procedure_iterator proc_it = compunit.procedures();
             !proc_it.at_end(); proc_it.advance()) {
            procedure proc = proc_it.current();
            //only focus on the function defined by user
            if(proc.get_kind().equals(procedure_kind.getFILE_INITIALIZATION())
                && proc.name().contains("Global_Initialization")){
                visitGlobalItem(proc, pFileName);
            }
        }
        
    }


    private void visitGlobalItem(procedure global_initialization, final String pFileName) throws result,CFGException {

        point_set pointSet = global_initialization.points();
        CType cType = CNumericTypes.INT;
        for(point_set_iterator point_it = pointSet.cbegin();
            !point_it.at_end();point_it.advance()){
            point p = point_it.current();
            CInitializer initializer = null;
            if(p.get_kind().equals(point_kind.getVARIABLE_INITIALIZATION()) ||
                    p.get_kind().equals(point_kind.getEXPRESSION())){
                ast nc_ast = p.get_ast(ast_family.getC_NORMALIZED());
                // for example: int i=0;
                // in nc_ast: children {i, 0}
                //            attributes {is_initialization: true, type: int}
                //has initialization
                if(nc_ast.get(ast_ordinal.getNC_IS_INITIALIZATION()).as_boolean()){
                    ast_field type = nc_ast.get(ast_ordinal.getNC_TYPE());
                    if(isConstantArrayOrVector(type) || isConstantStruct(type)){
                        initializer = getConstantAggregateInitializer(p,pFileName);
                    }else if (initializerRaw.isConstantAggregateZero()) {
                        CType expressionType = typeConverter.getCType(initializerRaw.typeOf());
                        initializer = getZeroInitializer(initializerRaw, expressionType, pFileName);
                    } else {
                        initializer =
                                new CInitializerExpression(
                                        getLocation(p, pFileName),
                                        (CExpression) getConstant(p, pFileName));
                    }

                }else {//// Declaration without initialization
                    initializer = null;
                }
                CDeclaration declaration = (CDeclaration) getAssignedVarDeclaration(p, "", initializer, pFileName);

                globalDeclarations.add(Pair.of(declaration, nc_ast.children().get(0).as_ast().pretty_print()));
            }

        }
    }


    private CRightHandSide getConstant(final point varInitPoint, final String pFileName)
            throws result, CFGException {
        ast vartype_ast = varInitPoint.get_ast(ast_family.getC_NORMALIZED()).children().get(1).as_ast()
        CType expectedType = typeConverter.getCType(vartype_ast.get(ast_ordinal.getNC_TYPE()).as_ast());
        return getConstant(varInitPoint, expectedType, pFileName);
    }

    private CRightHandSide getConstant(final point varInitPoint, CType pExpectedType, final String pFileName)
            throws result, CFGException {
        FileLocation location = getLocation(varInitPoint, pFileName);
        ast nc_ast = varInitPoint.get_ast(ast_family.getC_NORMALIZED());
        ast_field type = nc_ast.get(ast_ordinal.getNC_TYPE());

        if(type.as_ast().pretty_print().equals("const int"))//const int
        {
            int constantValue = nc_ast.children().get(1).as_int32();
            return new CIntegerLiteralExpression(location, pExpectedType, BigInteger.valueOf(constantValue));
        }else if(isNullPointer(nc_ast))//null pointer: e.g., p = 0; p= NULL;
        {
            return new CPointerExpression(location,pExpectedType,getNull(location,pExpectedType));
        }else if(isConstantExpression(nc_ast)){
            return getExpression(varInitPoint,pExpectedType,pFileName);
        }else if(isUndef(nc_ast)){
            CType constantType = typeConverter.getCType(type.as_ast());
            String undefName = "__VERIFIER_undef_" + constantType.toString().replace(' ', '_');
            CSimpleDeclaration undefDecl =
                    new CVariableDeclaration(
                            location,
                            true,
                            CStorageClass.AUTO,
                            pExpectedType,
                            undefName,
                            undefName,
                            undefName,
                            null);
            CExpression undefExpression = new CIdExpression(location, undefDecl);
            return undefExpression;
        } else if(isFunction(varInitPoint)){
            ast_field value_ast = nc_ast.children().get(1);
            String value = value_ast.as_ast().get(ast_ordinal.getBASE_NAME()).as_str();
            String functionName = value.substring(0,value.indexOf("$result"));
            CFunctionDeclaration funcDecl = functionDeclarations.get(functionName);
            CType functionType = funcDecl.getType();

            CIdExpression funcId = new CIdExpression(location, funcDecl);
            if (pointerOf(pExpectedType, functionType)) {
                return new CUnaryExpression(location, pExpectedType, funcId, CUnaryExpression.UnaryOperator.AMPER);
            } else {
                return funcId;
            }
        }
        else if (isGlobalConstant(varInitPoint) && isGlobalVariable(varInitPoint)) {
            return getAssignedIdExpression(varInitPoint, pExpectedType, pFileName);
        } else {
            throw new UnsupportedOperationException("CFG parsing does not support constant " + varInitPoint.characters());
        }
    }

    /**
     * Returns the id expression to an already declared variable. Returns it as a cast, if necessary
     * to match the expected type.
     */
    private CExpression getAssignedIdExpression(
            final point point, final CType pExpectedType, final String pFileName) throws result, CFGException{
        logger.log(Level.FINE, "Getting var declaration for point");

        if(!variableDeclarations.containsKey(getUnsignedInt(point.hashCode()))) {
            throw new CFGException("ID expression has no declaration: " + point.declared_symbol().name());
        }

        CSimpleDeclaration assignedVarDeclaration = variableDeclarations.get(getUnsignedInt(point.hashCode()));
        String assignedVarName = assignedVarDeclaration.getName();
        CType expressionType = assignedVarDeclaration.getType().getCanonicalType();
        CIdExpression idExpression =
                new CIdExpression(
                        getLocation(point, pFileName), expressionType, assignedVarName, assignedVarDeclaration);

        if (expressionType.canBeAssignedFrom(pExpectedType)) {
            return idExpression;

        } else if (pointerOf(pExpectedType, expressionType)) {
            CType typePointingTo = ((CPointerType) pExpectedType).getType().getCanonicalType();
            if (expressionType.canBeAssignedFrom(typePointingTo)
                    || expressionType.equals(typePointingTo)) {
                return new CUnaryExpression(
                        getLocation(point, pFileName), pExpectedType, idExpression, CUnaryExpression.UnaryOperator.AMPER);
            } else {
                throw new AssertionError("Unhandled type structure");
            }
        } else if (expressionType instanceof CPointerType) {
            return new CPointerExpression(getLocation(point, pFileName), pExpectedType, idExpression);
        } else {
            throw new AssertionError("Unhandled types structure");
        }
    }

    private boolean isFunction(point point)throws result{
        ast no_ast=point.get_ast(ast_family.getC_NORMALIZED());
        ast_field value_ast = no_ast.children().get(1);
        try {
            String value = value_ast.as_ast().get(ast_ordinal.getBASE_NAME()).as_str();
            if(value.contains("$result")){
                String functionName = value.substring(0,value.indexOf("$result"));
                if(functionDeclarations.containsKey(functionName))
                    return true;
            }
            return false;
        }catch (result r){
            return false;
        }
    }


    /**
     *@Description TODO
     *@Param [point, pFunctionName, pInitializer, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration
     **/
    private CSimpleDeclaration getAssignedVarDeclaration(
            final point point,
            final String pFunctionName,
            final CInitializer pInitializer,
            final String pFileName) throws result {


        final long itemId = getUnsignedInt(point.hashCode());// point.get_address() is not implemented
        if (!variableDeclarations.containsKey(itemId)) {
            ast nc_ast = point.get_ast(ast_family.getC_NORMALIZED());
            ast un_ast = point.get_ast(ast_family.getC_UNNORMALIZED());

            String assignedVar = nc_ast.children().get(0).as_ast().pretty_print();//the 1st child field store the variable

            final boolean isGlobal = point.declared_symbol().is_global();
            // Support static and other storage classes
            CStorageClass storageClass;
            switch (un_ast.get(ast_ordinal.getBASE_STORAGE_CLASS()).as_enum_value_string()){
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

            CType varType = typeConverter.getCType(nc_ast.get(ast_ordinal.getNC_TYPE()).as_ast());

            // We handle alloca not like malloc, which returns a pointer, but as a general
            // variable declaration. Consider that here by using the allocated type, not the
            // pointer of that type alloca returns.
//            if (pItem.isAllocaInst()) {
//                varType = typeConverter.getCType(pItem.getAllocatedType());
//            }

            if (isGlobal && varType instanceof CPointerType) {
                varType = ((CPointerType) varType).getType();
            }

            CSimpleDeclaration newDecl =
                    new CVariableDeclaration(
                            getLocation(point, pFileName),
                            isGlobal,
                            storageClass,
                            varType,
                            assignedVar,
                            assignedVar,
                            getQualifiedName(assignedVar, pFunctionName),
                            pInitializer);
            assert !variableDeclarations.containsKey(itemId);
            variableDeclarations.put(itemId, newDecl);
        }

        return variableDeclarations.get(itemId);
    }
    
    
    
    /**
     *@Description handle the aggregate initialization, e.g., int array[5]={1,2,3,4,5};
     *@Param [no_ast, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CInitializer
     **/
    private CInitializer getConstantAggregateInitializer(
            ast no_ast, final String pFileName) throws result {

        //ast no_ast = initialPoint.get_ast(ast_family.getC_NORMALIZED());
        ast_field value = no_ast.children().get(1);
        ast_field_vector elements = value.as_ast().children();

        ast_field type = no_ast.get(ast_ordinal.getNC_TYPE());
        int length = (int)elements.size();
        List<CInitializer> elementInitializers = new ArrayList<>(length);
        for(int i=0;i<length;i++){
            ast_field element = elements.get(i);
            CInitializer elementInitializer;
            ast_field elementType = element.as_ast().get(ast_ordinal.getBASE_TYPE());
            if(isConstantArrayOrVector(elementType) || isConstantStruct(elementType)){
                elementInitializer = getConstantAggregateInitializer(element.as_ast(), pFileName);
            }else if(isConstantAggregateZero(elementType)){
                elementInitializer =
                        getZeroInitializer(element, typeConverter.getCType(element.as_ast()), pFileName);
            } else {
                elementInitializer =
                        new CInitializerExpression(
                                getLocation(element.as_ast(), pFileName), (CExpression) getConstant(element, pFileName));
            }
            elementInitializers.add(elementInitializer);
        }

        CInitializerList aggregateInitializer =
                new CInitializerList(getLocation(no_ast, pFileName), elementInitializers);
        return aggregateInitializer;
    }


    private CInitializer getZeroInitializer(
            final point point, final CType pExpectedType, final String pFileName) throws result {
        FileLocation loc = getLocation(point, pFileName);
        CInitializer init;
        CType canonicalType = pExpectedType.getCanonicalType();
        if (canonicalType instanceof CArrayType) {
            int length = ((CArrayType) canonicalType).getLengthAsInt().getAsInt();
            CType elementType = ((CArrayType) canonicalType).getType().getCanonicalType();
            CInitializer zeroInitializer = getZeroInitializer(point, elementType, pFileName);
            List<CInitializer> initializers = Collections.nCopies(length, zeroInitializer);
            init = new CInitializerList(loc, initializers);
        } else if (canonicalType instanceof CCompositeType) {

            List<CCompositeType.CCompositeTypeMemberDeclaration> members = ((CCompositeType) canonicalType).getMembers();
            List<CInitializer> initializers = new ArrayList<>(members.size());
            for (CCompositeType.CCompositeTypeMemberDeclaration m : members) {
                CType memberType = m.getType();
                CInitializer memberInit = getZeroInitializer(point, memberType, pFileName);
                initializers.add(memberInit);
            }
            init = new CInitializerList(loc, initializers);

        } else {
            CExpression zeroExpression;
            if (canonicalType instanceof CSimpleType) {
                CBasicType basicType = ((CSimpleType) canonicalType).getType();
                if (basicType == CBasicType.FLOAT || basicType == CBasicType.DOUBLE) {
                    // use expected type for float, not canonical
                    zeroExpression = new CFloatLiteralExpression(loc, pExpectedType, BigDecimal.ZERO);
                } else {
                    zeroExpression = CIntegerLiteralExpression.ZERO;
                }
            } else {
                // use expected type for cast, not canonical
                zeroExpression = new CCastExpression(loc, pExpectedType, CIntegerLiteralExpression.ZERO);
            }
            init = new CInitializerExpression(loc, zeroExpression);
        }

        return init;
    }

    private CExpression getExpression(
            final point varInitPoint, final CType pExpectedType, final String pFileName)
            throws result,CFGException {
        ast nc_ast = varInitPoint.get_ast(ast_family.getC_NORMALIZED());
        if (isConstantExpression(nc_ast)) {
            return createFromOpCode(varInitPoint, pFileName);
        } else if (isConstant(nc_ast) && !isGlobalVariable(varInitPoint)) {
            return (CExpression) getConstant(varInitPoint, pExpectedType, pFileName);
        } else {
            return getAssignedIdExpression(varInitPoint, pExpectedType, pFileName);
        }
    }


    private CExpression createFromOpCode(
            final point varInitPoint, final String pFileName) throws result {

        ast nc_ast = varInitPoint.get_ast(ast_family.getC_NORMALIZED());
        ast_class operand = nc_ast.children().get(1).as_ast().get_class();

        if(operand.is_subclass_of(ast_class.getNC_ABSTRACT_ARITHMETIC())||
                operand.is_subclass_of(ast_class.getNC_ABSTRACT_BITWISE())){
            return createFromArithmeticOp(varInitPoint, operand, pFileName);
        }else if(operand.equals(ast_class.getNC_STRUCTORUNIONREF())){
            return createGetElementDotExp(varInitPoint, pFileName);
        }else if(operand.equals(ast_class.getNC_POINTEREXPR())){
            return new CCastExpression(getLocation(varInitPoint, pFileName), typeConverter.getCType(pItem
                    .typeOf()), getExpression(varInitPoint.getOperand(0), typeConverter.getCType(pItem
                    .getOperand(0).typeOf()), pFileName));
        }else {
            throw  new UnsupportedOperationException(operand.name());
        }
    }

    private CExpression createGetElementDotExp(final point varInitPoint, final String pFileName)
            throws result {
        ast nc_ast = varInitPoint.get_ast(ast_family.getC_NORMALIZED());
        ast_class operand = nc_ast.children().get(1).as_ast().get_class();

        FileLocation fileLocation = getLocation(varInitPoint, pFileName);

        if (pItem.canBeTransformedFromGetElementPtrToString()) {
            String constant = pItem.getGetElementPtrAsString();
            CType constCharType = new CSimpleType(
                    true, false, CBasicType.CHAR, false, false, false,
                    false, false, false, false);

            CType stringType = new CPointerType(false, false, constCharType);

            return new CStringLiteralExpression(fileLocation, stringType, constant);
        }

        CType currentType = typeConverter.getCType(startPointer.typeOf());
        CExpression currentExpression = getExpression(startPointer, currentType, pFileName);
        currentType = currentExpression.getExpressionType();
        assert pItem.getNumOperands() >= 2
                : "Too few operands in GEP operation : " + pItem.getNumOperands();

        for (int i = 1; i < pItem.getNumOperands(); i++) {
            /* get the value of the index */
            Value indexValue = pItem.getOperand(i);
            CExpression index = getExpression(indexValue, CNumericTypes.INT, pFileName);

            if (currentType instanceof CPointerType) {
                if (valueIsZero(indexValue)) {
                    /* if we do not shift the pointer, just dereference the type (and expression) */
                    currentExpression = getDereference(fileLocation, currentExpression);
                } else {
                    currentExpression =
                            getDereference(fileLocation,
                                    new CBinaryExpression(
                                            fileLocation,
                                            currentType,
                                            currentType,
                                            currentExpression,
                                            index,
                                            CBinaryExpression.BinaryOperator.PLUS));
                }
            } else if (currentType instanceof CArrayType) {
                if (valueIsZero(indexValue)) {
                    /* if we look into the first value, then use operator *
                     * instead of [0], so that Ref can remove the * from
                     * the expression where possible */
                    currentExpression =
                            new CPointerExpression(fileLocation,
                                    currentType.getCanonicalType(),
                                    currentExpression);
                } else {
                    currentExpression =
                            new CArraySubscriptExpression(fileLocation, currentType,
                                    currentExpression, index);
                }
            } else if (currentType instanceof CCompositeType) {
                if (!(index instanceof CIntegerLiteralExpression)) {
                    throw new UnsupportedOperationException(
                            "GEP index to struct only allows integer " + "constant, but is " + index);
                }
                int memberIndex = ((CIntegerLiteralExpression) index).getValue().intValue();
                CCompositeType.CCompositeTypeMemberDeclaration field =
                        ((CCompositeType) currentType).getMembers().get(memberIndex);
                String fieldName = field.getName();
                currentExpression =
                        new CFieldReference(fileLocation, currentType, fieldName,
                                currentExpression, false);
            }

            /* update the expression type */
            currentType = currentExpression.getExpressionType();
        }

        /* we want pointer to the element */
        return getReference(fileLocation, currentExpression);
    }


    private CExpression createFromArithmeticOp(
            final point varInitPoint, final ast_class operand, final String pFileName) throws result {

        ast nc_ast = varInitPoint.get_ast(ast_family.getC_NORMALIZED());

        final CType expressionType = typeConverter.getCType(nc_ast.children().get(0).as_ast());

        // TODO: Currently we only support flat expressions, no nested ones. Make this work
        // in the future.

        ast_class operand1 = varInitPoint.get_ast(ast_family.getC_NORMALIZED()).get_class(); // First operand
        logger.log(Level.FINE, "Getting id expression for operand 1");
        CType op1type = typeConverter.getCType(operand1.typeOf());
        CExpression operand1Exp = getExpression(operand1, op1type, pFileName);

        ast_class operand2 = operand; // Second operand
        CType op2type = typeConverter.getCType(operand2.typeOf());
        logger.log(Level.FINE, "Getting id expression for operand 2");
        CExpression operand2Exp = getExpression(operand2, op2type, pFileName);

        CBinaryExpression.BinaryOperator operation;

        if(operand.equals(ast_class.getNC_ADDEXPR()))
            operation = CBinaryExpression.BinaryOperator.PLUS;
        else if(operand.equals(ast_class.getNC_SUBEXPR()))
            operation = CBinaryExpression.BinaryOperator.MINUS;
        else if(operand.equals(ast_class.getNC_MULEXPR()))
            operation = CBinaryExpression.BinaryOperator.MULTIPLY;
        else if(operand.equals(ast_class.getNC_DIVEXPR()))
            operation = CBinaryExpression.BinaryOperator.DIVIDE;
        else if(operand.equals(ast_class.getNC_MODEXPR()))
            operation = CBinaryExpression.BinaryOperator.MODULO;
        else if(operand.equals(ast_class.getNC_RIGHTASSIGN()))
            operation = CBinaryExpression.BinaryOperator.SHIFT_RIGHT;
        else if(operand.equals(ast_class.getNC_LEFTASSIGN()))
            operation = CBinaryExpression.BinaryOperator.SHIFT_LEFT;
        else if(operand.equals(ast_class.getNC_ANDASSIGN()))
            operation = CBinaryExpression.BinaryOperator.BINARY_AND;
        else if(operand.equals(ast_class.getNC_ORASSIGN()))
            operation = CBinaryExpression.BinaryOperator.BINARY_OR;
        else if(operand.equals(ast_class.getNC_XORASSIGN()))
            operation = CBinaryExpression.BinaryOperator.BINARY_XOR;
        else
            throw new AssertionError("Unhandled operation " + operand.name());


        return new CBinaryExpression(
                getLocation(varInitPoint, pFileName),
                expressionType,
                expressionType,
                operand1Exp,
                operand2Exp,
                operation);
    }
    /**
     *@Description function declaration, need to extract its return type and parameters
     *@Param [function, pFileName]
     *@return void
     **/
    private void declareFunction(procedure function, final String pFileName) throws result{
        String functionName = function.name();

        // Function return type
        point_set formal_outs = function.formal_outs();//get the formal out of function, if type is VOID, the set is empty

        CFunctionType cFuncType = NO_ARGS_VOID_FUNCTION;
        if(!formal_outs.empty()){
            //Note that it is impossible that there are more than one formal out
            point p = formal_outs.cbegin().current();
            cFuncType = (CFunctionType) typeConverter.getCType(p);
        }

        // Parameters
        point_set formal_ins = function.formal_ins();// each point in formal_ins is an input parameter
        List<CParameterDeclaration> parameters = new ArrayList<>((int)formal_ins.size());
        if(!formal_ins.empty()){

            for(point_set_iterator point_it= formal_ins.cbegin();
                !point_it.at_end();point_it.advance())
            {
                point param_point = point_it.current();
                String paramName = param_point.parameter_symbols().get(0).get_ast().pretty_print();
                CType paramType = typeConverter.getCType(param_point);
                CParameterDeclaration parameter =
                        new CParameterDeclaration(getLocation(param_point,pFileName),paramType,paramName);

                parameter.setQualifiedName(getQualifiedName(paramName, functionName));
                variableDeclarations.put(function.file_line().get_second(),parameter);
                parameters.add(parameter);
            }
        }


        // Function declaration, exit
        CFunctionDeclaration functionDeclaration =
                new CFunctionDeclaration(
                        getLocation(function.entry_point(), pFileName), cFuncType, functionName, parameters);
        functionDeclarations.put(functionName, functionDeclaration);
    }
    /**
     * Returns whether the first param is a pointer of the type of the second parameter.<br>
     * Examples:
     *
     * <ul>
     *   <li>pointerOf(*int, int) -> true
     *   <li>pointerOf(**int, *int) -> true
     *   <li>pointerOf(int, int*) -> false
     *   <li>pointerOf(int, int) -> false
     * </ul>
     */
    private boolean pointerOf(CType pPotentialPointer, CType pPotentialPointee) {
        if (pPotentialPointer instanceof CPointerType) {
            return ((CPointerType) pPotentialPointer)
                    .getType()
                    .getCanonicalType()
                    .equals(pPotentialPointee.getCanonicalType());
        } else {
            return false;
        }
    }

    /**
     *@Description TODO
     *@Param [point, pFileName]
     *@return org.sosy_lab.cpachecker.cfa.ast.FileLocation
     **/
    private FileLocation getLocation(point point, final String pFileName) {
        assert point != null;
        return new FileLocation(pFileName, 0, 1, 0, 0);
    }

    private FileLocation getLocation(ast ast, final String pFileName) {
        assert ast != null;
        return new FileLocation(pFileName, 0, 1, 0, 0);
    }

    private String getQualifiedName(String pVarName, String pFuncName) {
        return pFuncName + "::" + pVarName;
    }



    /**
     *@Description the expression of a null pointer. In c, a null pointer means *p=0
     *@Param [pLocation, pType]
     *@return org.sosy_lab.cpachecker.cfa.ast.c.CExpression
     **/
    private CExpression getNull(final FileLocation pLocation, final CType pType) {
        return new CIntegerLiteralExpression(pLocation, pType, BigInteger.ZERO);
    }



}
