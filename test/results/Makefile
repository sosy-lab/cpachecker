export PATH=../../scripts/results:$(shell echo $$PATH)
COLUMNS=2,3

table.tex : table.csv ;
	cat "$<" | round.sh | csv_to_latex.sh > "$@"

table.csv : $(wildcard *.csv) ;
	column_select.sh 1,$(addprefix ${COLUMNS} ,$+) > "$@"

.PHONY : clean ;

clean : ;
	-rm table.* 2> /dev/null
