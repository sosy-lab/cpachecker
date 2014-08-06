for dotfile in $(ls output/*.dot)
do
	if test -f $dotfile
	then 
		dot -Tpdf $dotfile > $dotfile.pdf
		echo generated $dotfile.pdf
	fi
done

