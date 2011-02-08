#!/bin/bash

BC_SCRIPT='
	if (n < 0) {
		print "-";
		n *= -1;
	}
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
read LINE
echo "%$LINE"

IFS="$(echo -e '\n\t\n')"
while read -a LINE ; do
	echo -n "${LINE[0]}"
	for i in `seq 1 $(( ${#LINE[@]} - 1))` ; do
		echo -en "\t"
		NUMBER="${LINE[$i]}"
		if [[ "$NUMBER" == *.* ]] ; then
			echo "n=$NUMBER; $BC_SCRIPT" | bc
		else
			echo -n "$NUMBER"
		fi
	done
	echo ""
done
