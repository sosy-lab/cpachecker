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
    private final static String ENB = "OAI-ENB";
    private final static String MME = "OAI-MME";
    private final static String UE = "OAI-UE";

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
        String projectName = project.name();

        //the first traverse for building all functionEntry node
        for(project_compunits_iterator cu_it = project.compunits();
            !cu_it.at_end(); cu_it.advance() )
        {
            compunit cu = cu_it.current();
            // only focus on user-defined c files
//            input_file.add(Paths.get(cu.normalized_name()));
//            cfaBuilder.basicBuild(cu);
            if(filter(cu.name(), projectName) ||cu.is_library_model())
            {
                input_file.add(Paths.get(cu.normalized_name()));
                cfaBuilder.basicBuild(cu, project.name());
            }
        }
        System.out.println("Fisrt Traverse complete!");

        //the second traverse for building intra and inter CFA
        for(project_compunits_iterator cu_it = project.compunits();
            !cu_it.at_end(); cu_it.advance() )
        {
            compunit cu = cu_it.current();
            // only focus on user-defined c files
            if(filter(cu.name(),projectName))
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

    private static boolean filter(String path, String projectName){
        return fileFilter(path, projectName);
    }

    public static boolean targetFile(String path, String projectName){
        return path.endsWith("targets/RT/USER/lte-ru.c");
    }

    public static boolean isProjectMainFunction(String filePath, String projectName){
        if(projectName.equals(ENB))
            return filePath.endsWith("targets/RT/USER/lte-softmodem.c");
        else if(projectName.equals(UE))
            return filePath.endsWith("targets/RT/USER/lte-uesoftmodem.c");
        else if(projectName.equals(MME))
            return filePath.endsWith("openair-cn/src/oai_mme/oai_mme.c");
        else
            return true;
    }

    private static boolean fileFilter(String name, String projectName){
        return (name.contains("RRC_Rel14/LTE_") && (projectName.equals(UE) || projectName.equals(ENB))) || //AS application protocol interfaces between UE and ENB: radio resource control
                //(name.contains("S1AP_R14") && (projectName.equals(MME) || projectName.equals(ENB))) || //application protocol interfaces between MME and ENB: UE context management
                //(name.contains("X2AP_R14") && projectName.equals(ENB)) || //application protocol interfaces between enbs for handover (UE mobility) and/or self organizing network related function:
                (name.contains("openair2/RRC") && (projectName.equals(UE) || projectName.equals(ENB))) || //
                name.contains("openair2/COMMON") ||
                (name.contains("targets/RT/USER") && (projectName.equals(UE) || projectName.equals(ENB))) ||
                //(name.contains("openair3/S1AP") && projectName.equals(ENB)) ||
                (name.contains("openair3/NAS/UE") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/TOOLS") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/API") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/EMM") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/ESM") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/IES") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/ESM") && projectName.equals(UE)) ||
                ((name.contains("openair3/NAS/COMMON/UTIL/nas_timer.c")||
                        name.contains("openair3/NAS/COMMON/UTIL/OctetString.c") ||
                        name.contains("openair3/NAS/COMMON/UTIL/parser.c") ||
                        name.contains("openair3/NAS/COMMON/UTIL/TLVDecoder.c") ||
                        name.contains("openair3/NAS/COMMON/UTIL/TLVEncoder.c")) && projectName.equals(UE)) ||
                name.contains("openair3/COMMON") ||
                name.contains("openair3/UTILS") ||
                (name.contains("openair-cn/src/nas") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/mme") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/oai_mme") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/mme_app") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/common") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/utils") && !name.contains("openair-cn/src/utils/log.c") && projectName.equals(MME));// ||
                //(name.contains("CMakeFiles/r10.5") && projectName.equals(MME));//s1ap
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
