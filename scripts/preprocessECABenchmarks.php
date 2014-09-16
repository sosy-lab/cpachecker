<?php

for($i = 1; $i < 2; $i++) {
    for($j = 0; $j < 30; $j++) {
        $benchmark = '/localhome/loewe/workspace/programming/Java/benchmarks/ECA2014/Problem'.str_pad($i, 2, '0', STR_PAD_LEFT).'_'.str_pad($j, 2, '0', STR_PAD_LEFT).'.c';

        //echo(PHP_EOL.'gcc -E '.$benchmark.' > '.$benchmark.'.i');
        passthru('gcc -E '.$benchmark.' > '.$benchmark.'.i');
        passthru('rm '.$benchmark);

        $content = file_get_contents($benchmark.'.i');
        $content = str_replace(' ((!error_', ' goto error_', $content);
        $content = str_replace(') ? (void) (0) :', ';//) ? (void) (0) :', $content);
        $content = str_replace('fprintf(stderr, "Invalid input: %d\n", input);', 'global_err: goto global_err;', $content);
        
        
        file_put_contents($benchmark.'.i', $content);
    }
}
