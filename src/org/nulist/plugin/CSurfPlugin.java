package org.nulist.plugin;

import com.grammatech.cs.*;
import java.lang.*;

import static org.nulist.plugin.util.CFGDumping.dumpCFG2Dot;

//Combine CPAChecker as a plugin of CodeSurfer

public class CSurfPlugin {

    //
    public static void main(String args[]){
        if(args==null || args.length<1){
            System.out.println("No parent file path for dumping CFG dot");
            return;
        }
        if(!args[0].endsWith("/"))
            args[0]=args[0]+"/";
        try{
            System.out.println("==================CSURF_PLUGIN_BEGIN==================");
            project proj = project.current();
            for( project_compunits_iterator cu_it = proj.compunits();
                 !cu_it.at_end();
                 cu_it.advance() )
            {
                compunit cu = cu_it.current();
                // Iterate over all procedures in the compilation unit
                for( compunit_procedure_iterator proc_it = cu.procedures();
                     !proc_it.at_end();
                     proc_it.advance() )
                {
                    procedure proc = proc_it.current();

                    if(proc.get_kind().equals(procedure_kind.getUSER_DEFINED()))
                        dumpCFG2Dot(proc,args[0]);

                    /*point_set points;
                    try{
                        points = proc.points();
                    } catch( result r ) {
                        // the points method raises this exception when
                        // invoked on undefined functions
                        if( r.equals(result.getPDG_IS_UNDEFINED()) )
                            continue;
                        throw r;
                    }
                    // Iterate over all points in the procedure
                    for( point_set_iterator point_it = points.cbegin();
                         !point_it.at_end();
                         point_it.advance() )
                    {
                        point p = point_it.current();

                        if( p.get_syntax_kind().equals(point_syntax_kind.getGOTO()) )
                        {
                            sfileinst_line_pair file_line = p.file_line();
                            System.out.println(
                                    "I spy a goto statement: " + p
                                            + " at " + file_line.get_first().name() + ":" + file_line.get_second());
                        }
                    }*/
                }
            }
            System.out.println("==================CSURF_PLUGIN_END==================");
        }catch(result r){
            System.out.println("Uncaught exception: " + r);
        }
    }
}
