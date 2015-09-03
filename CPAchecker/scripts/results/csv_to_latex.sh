#!/bin/sh

sed -e '
	s/\([a-zA-Z0-9._]\+[a-z][a-zA-Z0-9._]\+\)/\\verb|\1|/g
	s/\t/ \& /g
	s/\(\d\+\)\.\(\d\+\)/\1\&\2/g
	$! s/$/\\\\/
	s/MO/\\text{\\smallerfontsize MO}/g
	s/>/\\raisebox{0.3ex}[0pt][0pt]{\\smallerfontsize\\ensuremath{>\\,}}/g
	s/ERROR/\\multicolumn{1}{c}{ERROR}/g
	' <&0
