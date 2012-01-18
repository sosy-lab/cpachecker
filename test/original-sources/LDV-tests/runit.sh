#use 
#./runit.sh test_file.c
#it will run the test in the current folder
#The CPAchecker trunk directory is set in the environment variable
CPADIR=..
CWD=`pwd`
RESDIR=results
CILLY=~/opt/ldv/dscv/rcv/backends/cpachecker/tools/cil/obj/x86_LINUX/cilly.asm.exe 
mkdir -p "$RESDIR"
( grep --only-match 'BLAST_AUTO\(_[0-9]*\)\?' $1; echo 'BLAST_AUTO'; ) | sort | uniq > $RESDIR/vars_file
for i in `cat $RESDIR/vars_file`; do \
	cmd="gcc -I. -D$i -E $1 -o $1.$i.i"
	#echo $cmd;
	RESULT="$1 $i"
	eval $cmd  || exit 1
	$CILLY $1.$i.i --out $1.$i.cilf.i --printCilAsIs --domakeCFG || exit 1
	cd $CPADIR && ./scripts/cpa.sh -predicateAnalysis -entryfunction main -outputpath $CWD/output-$1.$i.out/ $CWD/$1.$i.cilf.i > $CWD/$1.$i.output 2>&1 || exit 1
	cd $CWD
	VERDICT=`grep --only-match 'VERDICT_\w\+' $1.$i.i | sed 's/VERDICT_//g'`
	CURRENT=`grep --only-match 'CURRENTLY_\w\+' $1.$i.i | sed 's/CURRENTLY_//g'`
	if [ "$CURRENT" = "" ]; then
		RESULT="$RESULT ?";
	elif [ "$VERDICT" = "$CURRENT" ]; then 
		RESULT="$RESULT +";
else 
		RESULT="$RESULT -";
	fi
	CPASTR=`tail -n 1 $1.$i.output | grep --only-match 'reached? \w\+' | sed 's/reached? //g'`
	#echo "$CPASTR"
	CPAVERD=""
	if [ "$CPASTR" = "YES" ]; then 
		CPAVERD="UNSAFE";
	elif [ "$CPASTR" = "NO" ]; then
		CPAVERD="SAFE";
	else
		CPAVERD="UNKNOWN";
	fi
	#echo "CPA result = $CPAVERD"
	if [ "$CPAVERD" = "UNKNOWN" ]; then
		RESULT="$RESULT ?";
	elif [ "$CPAVERD" = "$VERDICT" ]; then

		RESULT="$RESULT +";
	else 
		RESULT="$RESULT -";
	fi
	echo $RESULT	 
done

#cmd="gcc -I. -E $1 -o $1.i"
