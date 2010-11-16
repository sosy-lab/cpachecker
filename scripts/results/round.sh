#!/bin/bash

BC_SCRIPT='
	if (n >= 1000) {
		print (n+5)/10*10;
	} else {
		if (n < 1) {
			print n;
		} else {
			f=0.005*10^(length(n)-4);
			scale = 6-length(n);
			print ((n+f)/1);
		}
	}'

# make first line a comment
echo -n '%'

IFS="$(echo -e '\n\t\n')"
while read -a LINE ; do
	echo -n "${LINE[0]}"
	for i in `seq 1 $(( ${#LINE[@]} - 1))` ; do
		NUMBER="${LINE[$i]}"
		echo "$BC_SCRIPT" | bc
		#[[ "$NUMBER" == *.* ]] && NUMBER="$(echo "n=$NUMBER; $BC_SCRIPT" | bc )"
		#[ "${NUMBER:0:1}" = "." ] && NUMBER="0$NUMBER"
		echo -en "\t$NUMBER"
	done
	echo ""
done
