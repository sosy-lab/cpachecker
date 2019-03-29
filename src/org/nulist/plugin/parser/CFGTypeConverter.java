/**
 * @ClassName CFGTypeConverter
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 2/28/19 5:42 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.*;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.nulist.plugin.parser.CFGAST.*;

public class CFGTypeConverter {
    private final LogManager logger;
    private final String STRUCT_PREF = "struct __STRUCT__";
    private final String UNION_PREF = "__UNION__";
    private final String ENUM_PREF = "__ENUM__";
    private static final CSimpleType ARRAY_LENGTH_TYPE = CNumericTypes.LONG_LONG_INT;
    private final Map<Integer, CType> typeCache = new HashMap<>();
    private Map<String, CType> typeMap = new HashMap<>();

    public CFGTypeConverter(final LogManager pLogger) {
        logger = pLogger;
        basicTypeInitialization();
    }

    //
    //since codesurfer normalizes bool into unsigned char, which may confuse model checking
    // we need to input unnormalized type
    public CType getCType(ast type, CFGHandleExpression expressionhandler){
        typeMap = new HashMap<>();
        try {
            String typeString = handleUnnamedType(type);
            CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
            if(cType!=null)
                return cType;
            else{
                CType result = getCType(type, false, expressionhandler);
                typeMap = new HashMap<>();
                return result;
            }
        }catch (result r){
            throw new RuntimeException(r);
        }
    }

    private CType getSubType(ast type, CFGHandleExpression expressionhandler){
        try {
            String typeString = handleUnnamedType(type);
            CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
            if(cType!=null)
                return cType;
            else
                return getCType(type, false, expressionhandler);
        }catch (result r){
            throw new RuntimeException(r);
        }
    }

    private CType getCType(ast type, boolean isConst, CFGHandleExpression expressionhandler){
        try {
            String typeString = type.pretty_print();
            if(typeString.endsWith("<UNNAMED>") || typeString.endsWith("<unnamed>")){
                typeString = handleUnnamedType(type);
            }

            CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
            //if(isConst && !typeString.startsWith("const "))
            //    cType = typeCache.getOrDefault(("const "+typeString).hashCode(),null);
            if(cType!=null)
                return cType;
            else if(!typeMap.isEmpty() && typeMap.containsKey(typeString)){
                return typeMap.get(typeString);
            } else if(isTypeRef(type)){
                ast originTypeAST = type.get(ast_ordinal.getBASE_TYPE()).as_ast();
                CType cTypedefType = null;

                if(typeString.startsWith("const ")){
                    cTypedefType = getCType(originTypeAST, true, expressionhandler);
                }else {
                    CType originType = getCType(originTypeAST, isConst, expressionhandler);
                    cTypedefType = new CTypedefType(originType.isConst(),
                                                                originType.isVolatile(),
                                                                typeString,
                                                                originType);
                }
                typeCache.put(typeString.hashCode(), cTypedefType);
                return cTypedefType;
            }else if(isStructType(type)){//struct
                return createStructType(type, isConst,expressionhandler);
            }else if(isPointerType(type)){
                ast pointedTo = type.get(ast_ordinal.getBASE_POINTED_TO()).as_ast();
                cType = getSubType(pointedTo, expressionhandler);
                CPointerType cPointerType = new CPointerType(isConst, false, cType);
                //typeCache.put(typeString.hashCode(),cPointerType);
                return cPointerType;
            }else if(isArrayType(type)){
                long length = type.get(ast_ordinal.getBASE_NUM_ELEMENTS()).as_uint32();
                ast elementType = type.get(ast_ordinal.getBASE_ELEMENT_TYPE()).as_ast();
                cType = getSubType(elementType, expressionhandler);
                CIntegerLiteralExpression arrayLength =
                        new CIntegerLiteralExpression(
                                FileLocation.DUMMY,
                                ARRAY_LENGTH_TYPE,
                                BigInteger.valueOf(length));
                return   new CArrayType(isConst, false, cType, arrayLength);

            }else if(isVLAType(type)){
                CType elementType = getSubType(type.get(ast_ordinal.getUC_ELEMENT_TYPE()).as_ast(), expressionhandler);
                ast element = type.get(ast_ordinal.getUC_NUM_ELEMENTS()).as_ast();
                CExpression length = expressionhandler.getExpressionFromUC(element, ARRAY_LENGTH_TYPE, FileLocation.DUMMY);
                return new CArrayType(
                        isConst, false, elementType, length);
            }else if(isEnumType(type)){
                ast constantList = type.get(ast_ordinal.getUC_CONSTANT_LIST()).as_ast();
                List<CEnumType.CEnumerator> enumerators = new ArrayList<>();
                for(int i=0;i<constantList.children().size();i++){
                    ast enumer = constantList.children().get(i).as_ast();
                    String name = enumer.pretty_print();
                    long value = enumer.get(ast_ordinal.getBASE_VALUE()).as_ast()
                                    .get(ast_ordinal.getBASE_VALUE()).as_int32();
                    CEnumType.CEnumerator enumerator =
                            new CEnumType.CEnumerator(FileLocation.DUMMY,
                                                      name,
                                                      name,
                                                      value);
                    enumerators.add(enumerator);
                }
                String typeName = typeString;
                if(typeString.endsWith("<UNNAMED>") || typeString.endsWith("<unnamed>")){
                    typeName = handleUnnamedType(type);
                    typeString = "";
                }
                CEnumType cEnumType = new CEnumType(isConst, false,
                        enumerators, typeName, typeString);
                CElaboratedType cElaboratedType= new CElaboratedType(isConst, false,
                                            CComplexType.ComplexTypeKind.ENUM,
                                             typeName,typeString,cEnumType);
                typeCache.put(typeName.hashCode(), cElaboratedType);
                return cElaboratedType;
            }else if(isUnionType(type)){

                ast_field_vector children =null;
                if(type.is_a(ast_class.getUC_UNION()) && type.has_field(ast_ordinal.getUC_FIELDS()))
                    children = type.get(ast_ordinal.getUC_FIELDS()).as_ast().children();
                else
                    children = type.children();

                String typeName = typeString;
                if(typeString.endsWith("<UNNAMED>") || typeString.endsWith("<unnamed>") ){
                    typeName =handleUnnamedType(type);
                    typeString = "";
                }

                List<CCompositeType.CCompositeTypeMemberDeclaration> members = new ArrayList<>();

                if(children!=null && !children.isEmpty())
                for(int i=0;i<children.size();i++){
                    ast memberAST = children.get(i).as_ast();
                    CType memeberType = getSubType(memberAST.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionhandler);
                    String memberName = memberAST.pretty_print();//memberAST.get(ast_ordinal.getBASE_NAME()).as_str();
                    CCompositeType.CCompositeTypeMemberDeclaration memberDeclaration =
                            new CCompositeType.CCompositeTypeMemberDeclaration(memeberType, memberName);
                    members.add(memberDeclaration);
                }

                CCompositeType cCompositeType = new CCompositeType(isConst, false,
                        CComplexType.ComplexTypeKind.UNION,
                        members, typeName, typeString);
                CElaboratedType cElaboratedType= new CElaboratedType(isConst, false,
                        CComplexType.ComplexTypeKind.UNION,
                        typeName, typeString,cCompositeType);
                typeCache.put(typeName.hashCode(), cElaboratedType);
                return cElaboratedType;

            }else if(type.is_a(ast_class.getUC_VECTOR_TYPE())){//C++ only
                System.out.println();
            }else if(type.is_a(ast_class.getUC_ROUTINE_TYPE())){//
                //output function type, then in expression, we convert to CFunctionTypeWithNames in demand
                CType returnType = getSubType(type.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast(), expressionhandler);
                List<CType> paramTypesList = new ArrayList<>();

                if(type.has_field(ast_ordinal.getUC_PARAM_TYPES())){
                    ast param_types = type.get(ast_ordinal.getUC_PARAM_TYPES()).as_ast();
                    for(int i=0;i<param_types.children().size();i++){
                        ast param = param_types.children().get(i).as_ast();
                        CType paramType = getSubType(param.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionhandler);
                        paramTypesList.add(paramType);
                    }
                }
                //function(int a, ...), ... means has_ellipsis is true ==> pTakesVarArgs is true
                return new CFunctionType(returnType, paramTypesList, type.get(ast_ordinal.getUC_HAS_ELLIPSIS()).as_boolean());
            }else if(type.is_a(ast_class.getNC_ROUTINE_TYPE())){
                CType returnType = getSubType(type.get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast(), expressionhandler);
                List<CType> paramTypesList = new ArrayList<>();

                if(type.children().size()>1){
                    for(int i=1;i<type.children().size();i++){
                        ast param = type.children().get(i).as_ast();
                        CType paramType = getSubType(param, expressionhandler);
                        paramTypesList.add(paramType);
                    }
                }
                return new CFunctionType(returnType, paramTypesList, type.get(ast_ordinal.getNC_HAS_ELLIPSIS()).as_boolean());
            }else
                throw new RuntimeException("Unsupported type "+ type.toString());

            return null;
        }catch (result r){
            throw new RuntimeException("Issue in handle type "+ type.toString());
        }
    }

    private String handleUnnamedType(ast type) throws result{
        String typeString = type.pretty_print();
        String typeName ="";
        if(typeString.endsWith("<UNNAMED>") || typeString.endsWith("<unnamed>") ){
            if(isStructType(type)){
                typeName=STRUCT_PREF;
                if(type.is_a(ast_class.getUC_STRUCT()))
                    typeName += type.get(ast_ordinal.getUC_POSITION()).as_uint64();
                else {
                    typeName += type.get(ast_ordinal.getNC_UNNORMALIZED()).get(ast_ordinal.getUC_POSITION()).as_uint64();
                }
                return typeName;
            } else if(isUnionType(type)){
                typeName=UNION_PREF;
                if(type.is_a(ast_class.getUC_UNION()))
                    typeName += type.get(ast_ordinal.getUC_POSITION()).as_uint64();
                else {
                    typeName += type.get(ast_ordinal.getNC_UNNORMALIZED()).get(ast_ordinal.getUC_POSITION()).as_uint64();
                }
                return typeName;
            }else if(isEnumType(type)){
                typeName = ENUM_PREF;
                typeName += type.get(ast_ordinal.getUC_POSITION()).as_uint64();
                return typeName;
            }
            return typeString;
        }else
            return typeString;
    }

    /**
     *@Description basic type initialization
     *@Param []
     *@return void
     **/
    private void basicTypeInitialization(){
        typeCache.put("bool".hashCode(),CNumericTypes.BOOL);
        typeCache.put("void".hashCode(),CVoidType.VOID);
        typeCache.put("int".hashCode(),CNumericTypes.INT);
        typeCache.put("unsigned int".hashCode(),CNumericTypes.UNSIGNED_INT);
        typeCache.put("signed int".hashCode(),CNumericTypes.SHORT_INT);
        typeCache.put("char".hashCode(),CNumericTypes.CHAR);
        typeCache.put("unsigned char".hashCode(),CNumericTypes.UNSIGNED_CHAR);
        typeCache.put("signed char".hashCode(),CNumericTypes.SIGNED_CHAR);
        typeCache.put("short".hashCode(),CNumericTypes.SHORT_INT);
        typeCache.put("unsigned short".hashCode(),CNumericTypes.UNSIGNED_SHORT_INT);
        typeCache.put("long".hashCode(),CNumericTypes.LONG_INT);
        typeCache.put("unsigned long".hashCode(),CNumericTypes.UNSIGNED_LONG_INT);
        typeCache.put("signed long".hashCode(),CNumericTypes.SIGNED_LONG_INT);
        typeCache.put("long long".hashCode(),CNumericTypes.LONG_LONG_INT);
        typeCache.put("unsigned long long".hashCode(),CNumericTypes.UNSIGNED_LONG_LONG_INT);
        typeCache.put("signed long long".hashCode(),CNumericTypes.SIGNED_LONG_LONG_INT);
        typeCache.put("float".hashCode(),CNumericTypes.FLOAT);
        typeCache.put("double".hashCode(),CNumericTypes.DOUBLE);
        typeCache.put("long double".hashCode(),CNumericTypes.LONG_DOUBLE);
        typeCache.put("const bool".hashCode(),CNumericTypes.CONST_BOOL);
        typeCache.put("const int".hashCode(),CNumericTypes.CONST_INT);
        typeCache.put("const unsigned int".hashCode(),CNumericTypes.CONST_UNSIGNED_INT);
        typeCache.put("const signed int".hashCode(),CNumericTypes.CONST_SHORT_INT);
        typeCache.put("const char".hashCode(),CNumericTypes.CONST_CHAR);
        typeCache.put("const unsigned char".hashCode(),CNumericTypes.CONST_UNSIGNED_CHAR);
        typeCache.put("const signed char".hashCode(),CNumericTypes.CONST_SIGNED_CHAR);
        typeCache.put("const short".hashCode(),CNumericTypes.CONST_SHORT_INT);
        typeCache.put("const unsigned short".hashCode(),CNumericTypes.CONST_UNSIGNED_SHORT_INT);
        typeCache.put("const long".hashCode(),CNumericTypes.CONST_LONG_INT);
        typeCache.put("const unsigned long".hashCode(),CNumericTypes.CONST_UNSIGNED_LONG_INT);
        typeCache.put("const signed long".hashCode(),CNumericTypes.CONST_SIGNED_LONG_INT);
        typeCache.put("const long long".hashCode(),CNumericTypes.CONST_LONG_LONG_INT);
        typeCache.put("const unsigned long long".hashCode(),CNumericTypes.CONST_UNSIGNED_LONG_LONG_INT);
        typeCache.put("const signed long long".hashCode(),CNumericTypes.CONST_SIGNED_LONG_LONG_INT);
        typeCache.put("const float".hashCode(),CNumericTypes.CONST_FLOAT);
        typeCache.put("const double".hashCode(),CNumericTypes.CONST_DOUBLE);
        typeCache.put("const long double".hashCode(),CNumericTypes.CONST_LONG_DOUBLE);
    }


    /**
     *@Description generate the struct type from an ast
     *@Param [type]
     *@return org.sosy_lab.cpachecker.cfa.types.c.CType
     **/
    //struct type
    private CType createStructType(ast type, boolean isConst, CFGHandleExpression expressionhandler) throws result{

        final boolean isVolatile = false;

        //typedef struct: label
        //normal struct: struct label
        String structName = type.pretty_print();
        String typeName = structName;
        if(structName.endsWith("<UNNAMED>") || structName.endsWith("<unnamed>") ){
            typeName = handleUnnamedType(type);
            structName = "";
        }

        ast struct_type = getStructType(type);

        if(typeCache.containsKey(typeName.hashCode())){
            return new CElaboratedType(
                    false,
                    false,
                    CComplexType.ComplexTypeKind.STRUCT,
                    typeName,
                    structName,
                    (CComplexType) typeCache.get(typeName.hashCode()));
        }


        CCompositeType cStructType =
                new CCompositeType(isConst, isVolatile, CComplexType.ComplexTypeKind.STRUCT, typeName, structName);
        CElaboratedType cEStructType = new CElaboratedType(isConst,
                isVolatile,
                CComplexType.ComplexTypeKind.STRUCT,
                typeName,
                structName,
                cStructType);
        //normalized type
        ast_field_vector items = null;
        if(struct_type.is_a(ast_class.getUC_STRUCT())){
            if(struct_type.has_field(ast_ordinal.getUC_FIELDS()))
                items = struct_type.get(ast_ordinal.getUC_FIELDS()).as_ast().children();
        }
        else
            items =struct_type.children(); //

        if(items==null || items.isEmpty()){
            typeCache.put(typeName.hashCode(), cEStructType);
            return cEStructType;
        }

        List<CCompositeType.CCompositeTypeMemberDeclaration> members =
                new ArrayList<>((int)items.size());
        typeMap.put(typeName, cEStructType);
        for (int i = 0; i < items.size(); i++) {
            ast member = items.get(i).as_ast();
            String memberName = member.pretty_print();
            String memberTypeName = member.get(ast_ordinal.getBASE_TYPE()).as_ast().pretty_print();
            if(memberTypeName.endsWith("<UNNAMED>") || memberTypeName.endsWith("<unnamed>") ){
                memberTypeName = handleUnnamedType(type);
            }

            if(typeMap.containsKey(memberTypeName)){
                CCompositeType.CCompositeTypeMemberDeclaration memDecl =
                        new CCompositeType.CCompositeTypeMemberDeclaration(typeMap.get(memberTypeName), memberName);
                members.add(memDecl);
            }else {
                CType cMemType = getSubType(member.get(ast_ordinal.getBASE_TYPE()).as_ast(), expressionhandler);
                if(isFunctionPointerType(cMemType))
                    cMemType = convertCFuntionType(cMemType,memberName, FileLocation.DUMMY);
                CCompositeType.CCompositeTypeMemberDeclaration memDecl =
                        new CCompositeType.CCompositeTypeMemberDeclaration(cMemType, memberName);
                members.add(memDecl);
            }
        }

        cStructType.setMembers(members);

        typeCache.put(typeName.hashCode(), cEStructType);
        return cEStructType;
    }

    public CFunctionTypeWithNames convertCFuntionType(CFunctionType cFunctionType, String name, FileLocation fileLocation){
        List<CParameterDeclaration> cParameterDeclarations = new ArrayList<>();
        for(CType type:cFunctionType.getParameters()){
            CParameterDeclaration parameterDeclaration = new CParameterDeclaration(fileLocation, type,"");
            cParameterDeclarations.add(parameterDeclaration);
        }
        CFunctionTypeWithNames typeWithNames = new CFunctionTypeWithNames(cFunctionType.getReturnType(), cParameterDeclarations, cFunctionType.takesVarArgs());
        typeWithNames.setName(name);
        return typeWithNames;
    }

    public CType convertCFuntionType(CType cType, String name, FileLocation fileLocation){
        if(cType instanceof CFunctionType)
            return convertCFuntionType(cType,name, fileLocation);
        else if(cType instanceof CPointerType){
            CFunctionType functionType = (CFunctionType) ((CPointerType) cType).getType();
            CFunctionTypeWithNames cFunctionTypeWithNames = convertCFuntionType(functionType,name,fileLocation);
            return new CPointerType(cType.isConst(),cType.isVolatile(), cFunctionTypeWithNames);
        }else if(cType instanceof CTypedefType){
            CPointerType cPointerType =(CPointerType) ((CTypedefType) cType).getRealType();
            cPointerType = (CPointerType) convertCFuntionType(cPointerType, name, fileLocation);
            return new CTypedefType(cType.isConst(), cType.isVolatile(), ((CTypedefType) cType).getName(), cPointerType);
        }else {
            throw new RuntimeException("This is not a function pointer type");
        }
    }


    public CType getFuntionTypeFromFunctionPointer(CType cType){
        if(cType instanceof CPointerType){
            return ((CPointerType)cType).getType();
        }else if(cType instanceof CTypedefType){
            return getFuntionTypeFromFunctionPointer(((CTypedefType) cType).getRealType());
        }else
            throw new RuntimeException("Not a function pointer type: "+ cType.toString());
    }

    public boolean isFunctionPointerType(CType type){
        if(type instanceof CPointerType){
            CType basicType = ((CPointerType)type).getType();
            if(basicType instanceof CFunctionType || basicType instanceof CFunctionTypeWithNames)
                return true;
        }else if(type instanceof CTypedefType){
            return isFunctionPointerType(((CTypedefType) type).getRealType());
        }
        return false;
    }
}
