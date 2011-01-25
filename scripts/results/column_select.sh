#!/bin/sh

TEMPS=""
while [ ! -z "$1" ] ; do
	TEMP="`mktemp`"
	cut -s -f "$1" "$2" > "$TEMP"
	TEMPS="$TEMPS $TEMP"
	echo -n "$1" | sed -e "s%[^,]%$2%g" -e 's/,/\t/g' -e 's%test_\(.*\)\.csv%\1%'
	echo -n '\t'
	shift; shift
done
echo

RESULT="`paste $TEMPS`"

rm $TEMPS
echo "$RESULT"
