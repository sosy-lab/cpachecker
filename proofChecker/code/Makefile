SHELL   = /bin/sh

CC	= gcc

OBJS	= driver.o clpintf.o clp.o csm.o obsave.o deftbl.o err.o \
	source.o pdl_gen.o VoidPtrList.o clplib.o CScope.o envmod.o \
	BitSet.o treecon.o nodecode.o parser.o glahead.o auxNUL.o \
	auxEOF.o dflteot.o tabsize.o idn.o Ccomment.o str.o \
	pgs_gen.o treestack.o dfltrepar.o attrmain.o visitprocs.o \
	visitmap.o parserInterface.o obstack.o

EXE	= cabstract.exe

all:	$(EXE)

$(EXE):	$(OBJS)
	$(CC) -o $(EXE) $(CFLAGS) $(OBJS) $(LIBS)

objects: $(OBJS)

clean:
	rm -f a.out *.o *.a *~ core ERRS $(EXE)

tidy:
	rm -f core a.out *.o *~ *.a ERRS

driver.o:	driver.c err.h HEAD.h INIT.h FINL.h eliproto.h clp.h \
		envmod.h deftbl.h BitSet.h Strings.h CScope.h \
		parserInterface.h pdl_gen.h treecon.h DefTableKeyList.h \
		VoidPtrList.h clplib.h VoidPtr.h VoidPtrFunctionTypes.h \
		obstack.h csm.h nodeptr.h

clpintf.o:	clpintf.c clp.h clpintf.h csm.h eliproto.h pdl_gen.h \
		source.h clplib.h DefTableKeyList.h deftbl.h VoidPtrList.h \
		VoidPtr.h VoidPtrFunctionTypes.h obstack.h

clp.o:	clp.c clp.h clpintf.h pdl_gen.h csm.h eliproto.h clplib.h \
		DefTableKeyList.h deftbl.h VoidPtrList.h VoidPtr.h \
		VoidPtrFunctionTypes.h obstack.h

csm.o:	csm.c csm.h csmtbl.h obsave.h obstack.h eliproto.h

obsave.o:	obsave.c obsave.h obstack.h eliproto.h

deftbl.o:	deftbl.c err.h deftbl.h obstack.h obsave.h eliproto.h

err.o:	err.c err.h source.h eliproto.h

source.o:	source.c source.h

pdl_gen.o:	pdl_gen.c pdl_gen.h deftbl.h eliproto.h

VoidPtrList.o:	VoidPtrList.c VoidPtrList.h err.h obstack.h VoidPtr.h \
		VoidPtrFunctionTypes.h eliproto.h

clplib.o:	clplib.c csm.h clplib.h obstack.h eliproto.h

CScope.o:	CScope.c CScope.h envmod.h deftbl.h obstack.h BitSet.h \
		eliproto.h

envmod.o:	envmod.c err.h obstack.h envmod.h obsave.h eliproto.h \
		deftbl.h BitSet.h

BitSet.o:	BitSet.c obstack.h err.h BitSet.h eliproto.h

treecon.o:	treecon.c node.h nodecode.h attrpredef.h visitmap.h \
		treeact.h obstack.h err.h nodeptr.h HEAD.h eliproto.h clp.h \
		envmod.h deftbl.h BitSet.h Strings.h CScope.h \
		parserInterface.h pdl_gen.h treecon.h DefTableKeyList.h \
		VoidPtrList.h clplib.h VoidPtr.h VoidPtrFunctionTypes.h \
		csm.h

nodecode.o:	nodecode.c nodecode.h

parser.o:	parser.c err.h gla.h gsdescr.h reparatur.h pgs_gen.h \
		eliproto.h parsops.h

glahead.o:	glahead.c err.h csm.h source.h obstack.h scanops.h \
		tabsize.h ScanProc.h gla.h xtables.h xcode.h eliproto.h

auxNUL.o:	auxNUL.c source.h gla.h

auxEOF.o:	auxEOF.c 

dflteot.o:	dflteot.c 

tabsize.o:	tabsize.c 

idn.o:	idn.c err.h idn.h csm.h obstack.h obsave.h idntbl.h \
		eliproto.h

Ccomment.o:	Ccomment.c err.h gla.h source.h tabsize.h ScanProc.h \
		eliproto.h

str.o:	str.c csm.h obstack.h eliproto.h

pgs_gen.o:	pgs_gen.c gsdescr.h treestack.h HEAD.h err.h parsops.h \
		eliproto.h nodeptr.h clp.h envmod.h deftbl.h BitSet.h \
		Strings.h CScope.h parserInterface.h pdl_gen.h treecon.h \
		DefTableKeyList.h VoidPtrList.h clplib.h VoidPtr.h \
		VoidPtrFunctionTypes.h obstack.h csm.h

treestack.o:	treestack.c err.h treestack.h eliproto.h nodeptr.h

dfltrepar.o:	dfltrepar.c err.h reparatur.h eliproto.h

attrmain.o:	attrmain.c treestack.h visitprocs.h nodeptr.h HEAD.h node.h \
		treecon.h eliproto.h clp.h envmod.h deftbl.h BitSet.h \
		Strings.h CScope.h err.h parserInterface.h pdl_gen.h \
		DefTableKeyList.h VoidPtrList.h clplib.h VoidPtr.h \
		VoidPtrFunctionTypes.h obstack.h csm.h

visitprocs.o:	visitprocs.c HEAD.h err.h node.h visitprocs.h attrpredef.h \
		visitmap.h clp.h envmod.h deftbl.h BitSet.h Strings.h \
		CScope.h parserInterface.h pdl_gen.h treecon.h \
		DefTableKeyList.h VoidPtrList.h clplib.h eliproto.h \
		VoidPtr.h VoidPtrFunctionTypes.h obstack.h csm.h nodeptr.h

visitmap.o:	visitmap.c visitprocs.h visitmap.h HEAD.h node.h treecon.h \
		eliproto.h clp.h envmod.h deftbl.h BitSet.h Strings.h \
		CScope.h err.h parserInterface.h pdl_gen.h \
		DefTableKeyList.h VoidPtrList.h clplib.h VoidPtr.h \
		VoidPtrFunctionTypes.h obstack.h csm.h nodeptr.h

parserInterface.o:	parserInterface.c parserInterface.h

obstack.o:	obstack.c config.h obstack.h gettext.h

