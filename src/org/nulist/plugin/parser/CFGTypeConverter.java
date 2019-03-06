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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CFGTypeConverter {
    private final MachineModel machineModel;
    private final LogManager logger;

    private final Map<Integer, CType> typeCache = new HashMap<>();

    public CFGTypeConverter(final MachineModel pMachineModel, final LogManager pLogger) {
        machineModel = pMachineModel;
        logger = pLogger;
        basicTypeInitialization();
    }


    public CType getCType(CFGAST type){
        try {
            String typeString = type.pretty_print();
            CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
            if(cType!=null)
                return cType;
            else if(type.isStructType()){
                return createStructType(type);
            }
            return null;
        }catch (result r){
            return null;
        }

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
    }

    /**
     *@Description generate the struct type from an ast
     *@Param [type]
     *@return org.sosy_lab.cpachecker.cfa.types.c.CType
     **/
    //struct type
    private CType createStructType(CFGAST type) throws result{
        final boolean isConst = false;
        final boolean isVolatile = false;

        //typedef struct: label
        //normal struct: struct label
        String structName = type.pretty_print();
        if(structName.startsWith("const "))//normalize const struct and non-const struct
            structName = structName.substring(6);

        CFGAST struct_type = type.getStructType();

        if(typeCache.containsKey(structName.hashCode())){
            return new CElaboratedType(
                    false,
                    false,
                    CComplexType.ComplexTypeKind.STRUCT,
                    structName,
                    structName,
                    (CComplexType) typeCache.get(structName.hashCode()));
        }


        CCompositeType cStructType =
                new CCompositeType(isConst, isVolatile, CComplexType.ComplexTypeKind.STRUCT, structName, structName);

        //items
        ast_field_vector items = struct_type.get(ast_ordinal.getUC_FIELDS()).as_ast().children();

        List<CCompositeType.CCompositeTypeMemberDeclaration> members =
                new ArrayList<>((int)items.size());

        for (int i = 0; i < items.size(); i++) {
            ast_field member = items.get(i);
            String memberName = member.as_ast().pretty_print();
            CType cMemType = getCType((CFGAST) member.as_ast());
            CCompositeType.CCompositeTypeMemberDeclaration memDecl =
                    new CCompositeType.CCompositeTypeMemberDeclaration(cMemType, memberName);
            members.add(memDecl);
        }

        cStructType.setMembers(members);

        typeCache.put(structName.hashCode(), cStructType);
        return cStructType;
    }

}
