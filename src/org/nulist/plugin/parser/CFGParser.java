package org.nulist.plugin.parser;

import com.grammatech.cs.compunit;
import com.grammatech.cs.project;
import com.grammatech.cs.project_compunits_iterator;
import com.grammatech.cs.result;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CFGParser
 * @Description CFG parse. Given a project, CFGParser extract its all user-defined compunits to construct cfa
 * @Author Yinbo Yu
 * @Date 2/27/19 3:42 PM
 * @Version 1.0
 **/
public class CFGParser implements Parser{

    private LogManager logger=null;
    private CFABuilder cfaBuilder=null;

    private final Timer parseTimer = new Timer();
    private final Timer cfaCreationTimer = new Timer();


    public CFGParser(){

    }

    public CFGParser(final LogManager pLogger, final MachineModel pMachineModel){
        logger = pLogger ;
        cfaBuilder = new CFABuilder(logger, pMachineModel);
    }

    @Override
    public ParseResult parseFile(String filename) {
        return null;
    }

    @Override
    public ParseResult parseString(String filename, String code) {
        return null;
    }

    public ParseResult parseProject(project project) throws result {
        List<Path> input_file = new ArrayList<>();
        parseTimer.start();

        //the first traverse for building all functionEntry node
        for(project_compunits_iterator cu_it = project.compunits();
            !cu_it.at_end(); cu_it.advance() )
        {
            compunit cu = cu_it.current();
            // only focus on user-defined c files
//            input_file.add(Paths.get(cu.normalized_name()));
//            cfaBuilder.basicBuild(cu);
            if(fileFilter(cu.name()) ||cu.is_library_model())
            {
                input_file.add(Paths.get(cu.normalized_name()));
                cfaBuilder.basicBuild(cu);
            }
        }
        System.out.println("Fisrt Traverse complete!");

        //the second traverse for building intra and inter CFA
        for(project_compunits_iterator cu_it = project.compunits();
            !cu_it.at_end(); cu_it.advance() )
        {
            compunit cu = cu_it.current();
            // only focus on user-defined c files
            if(targetFile(cu.name()))
            {
            //input_file.add(Paths.get(cu.normalized_name()));
                System.out.println(cu.name());
                cfaBuilder.build(cu);
            }
        }


        return new ParseResult(cfaBuilder.functions,
                cfaBuilder.cfaNodes,
                cfaBuilder.getGlobalVariableDeclarations(),
                input_file);

    }

    public static boolean targetFile(String path){
        return path.endsWith("/openair3/NAS/UE/API/USER/at_command.c");
    }


    private boolean fileFilter(String name){
        return name.contains("RRC_Rel14") ||
                name.contains("S1AP_R14") ||
                name.contains("X2AP_R14") ||
                name.contains("openair2/RRC") ||
                name.contains("openair2/NAS") ||
                name.contains("openair2/COMMON") ||
                name.contains("openair3/S1AP") ||
                name.contains("openair3/NAS") ||
                name.contains("openair3/COMMON") ||
                name.contains("openair3/UTILS");
    }

    @Override
    public Timer getCFAConstructionTime() {
        return cfaCreationTimer;
    }

    @Override
    public Timer getParseTime() {
        return parseTimer;
    }
}
