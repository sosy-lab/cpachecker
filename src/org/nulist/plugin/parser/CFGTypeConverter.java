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
import java.util.logging.Level;

public class CFGTypeConverter {
    private final MachineModel machineModel;
    private final LogManager logger;

    private final Map<Integer, CType> typeCache = new HashMap<>();

    public CFGTypeConverter(final MachineModel pMachineModel, final LogManager pLogger) {
        machineModel = pMachineModel;
        logger = pLogger;
        typeInitialization();
    }


    public CType getCType(symbol param_symbol) throws result {
        return getCType(param_symbol.get_type());
    }

    public CType getCType(point point) throws result {
        ast type = point.parameter_symbols().get(0).get_type();
        String typeString = type.pretty_print();

        CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
        if(cType!=null)
            return cType;
        else if(typeString.equals("struct")){
            return createStructType(point);
        }

        return null;
    }

    public CType getCType(ast type) throws result {
        String typeString = type.pretty_print();
        CType cType = typeCache.getOrDefault(typeString.hashCode(),null);
        if(cType!=null)
            return cType;
        else {
            return null;
        }
    }


    private void typeInitialization(){
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

    //formal out struct point
    private CType createStructType(point point) throws result{
        final boolean isConst = false;
        final boolean isVolatile = false;

//        if (pStructType.isOpaqueStruct()) {
//            logger.log(Level.INFO, "Ignoring opaque struct");
//        }
        //for struct example: struct test{int a, int b}
        //function example: test function(int c,int d)
        ast un_a = point.get_ast(ast_family.getC_UNNORMALIZED());

        //routine type: test (c,d)
        //un_a.get(ast_ordinal.getBASE_TYPE());

        String structName = un_a.get(ast_ordinal.getBASE_TYPE()).as_ast().
                get(ast_ordinal.getBASE_RETURN_TYPE()).as_ast().pretty_print();

        if(typeCache.containsKey(structName.hashCode())){
            return new CElaboratedType(
                    false,
                    false,
                    CComplexType.ComplexTypeKind.STRUCT,
                    structName,
                    structName,
                    (CComplexType) typeCache.get(structName.hashCode()));
        }

//        ast no_a = point.get_ast(ast_family.getC_NORMALIZED());
//        // type: struct <UNNAMED>
//        no_a.get(ast_ordinal.getBASE_TYPE());


        CCompositeType cStructType =
                new CCompositeType(isConst, isVolatile, CComplexType.ComplexTypeKind.STRUCT, structName, structName);

        //items
        symbol symbol = point.parameter_symbols().get(0);
        ast_field_vector items = symbol.get_type().children();

        List<CCompositeType.CCompositeTypeMemberDeclaration> members =
                new ArrayList<>((int)items.size());

        for (int i = 0; i < items.size(); i++) {
            ast_field member = items.get(i);
            String memberName = member.as_ast().pretty_print();
            CType cMemType = getCType(member.as_ast());
            CCompositeType.CCompositeTypeMemberDeclaration memDecl =
                    new CCompositeType.CCompositeTypeMemberDeclaration(cMemType, memberName);
            members.add(memDecl);
        }

        cStructType.setMembers(members);

        typeCache.put(structName.hashCode(), cStructType);
        return cStructType;
    }

}
