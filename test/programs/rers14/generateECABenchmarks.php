<?php

for($i = 1; $i < 10; $i++) {
    if($i == 14 || $i == 15 || $i == 16) {
      continue;
    }
    $template = file_get_contents('./templates/Problem'.$i.'.c');
    
    for($j = 0; $j < 100; $j++) {
	// replace assert by call to __VERIFIER_error()
        $benchmark = str_replace('error_'.$j.': assert(!error_'.$j.');', '__VERIFIER_error();', $template);
        
        $fileName = 'Problem'.str_pad($i, 2, '0', STR_PAD_LEFT).'_'.str_pad($j, 2, '0', STR_PAD_LEFT).'.c';
        file_put_contents($fileName, $benchmark);

	// preprocess file
        passthru('gcc -E '.$fileName.' > '.$fileName.'.i');
        passthru('rm '.$fileName);

        $content = file_get_contents($fileName.'.i');

	// create end-less-loop for each error location NOT relevant in this benchmark -> becomes dead end in CPAchecker, which is preferable
        $content = str_replace(' ((!error_', ' goto error_', $content);
        $content = str_replace(') ? (void) (0) :', ';//) ? (void) (0) :', $content);

	// create end-less-loop for the global error location, which is irrelevant, for some reason as above
        $content = str_replace('fprintf(stderr, "Invalid input: %d\n", input);', 'global_err: goto global_err;', $content);

        echo 'created preprocessed benchmark file '.$fileName.'.i'.PHP_EOL;
        file_put_contents($fileName.'.i', $content);
    }
}
