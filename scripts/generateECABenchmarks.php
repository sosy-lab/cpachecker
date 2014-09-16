<?php

for($i = 1; $i < 2; $i++) {
    $template = file_get_contents('/localhome/loewe/workspace/programming/Java/benchmarks/ECA2014/templates/Problem'.$i.'.c');
    
    for($j = 0; $j < 30; $j++) {
        $benchmark = str_replace('error_'.$j.': assert(!error_'.$j.');', '__VERIFIER_error();', $template);
        
        $fileName = '/localhome/loewe/workspace/programming/Java/benchmarks/ECA2014/Problem'.str_pad($i, 2, '0', STR_PAD_LEFT).'_'.str_pad($j, 2, '0', STR_PAD_LEFT).'.c';
        file_put_contents($fileName, $benchmark);
        echo PHP_EOL.'writing '.$fileName;
    }
}
