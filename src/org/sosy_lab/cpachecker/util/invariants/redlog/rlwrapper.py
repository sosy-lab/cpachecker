#!/usr/bin/python

import sys
from subprocess import *


class List:

    def __init__(self,text=''):
        self.striter = StrIter(text)
        self.children = []

    def parse(self,s=None):
        # s a StrIter object
        if not s:
            s = self.striter
        cur = ''
        while s.has_next():
            c = s.next()
            if c in '\n\t': continue
            if c == '{':
                L = List()
                L.parse(s)
                self.children.append(L)
            elif c == ',':
                self.children.append(Expr(cur))
                cur = ''
            elif c == '}':
                self.children.append(Expr(cur))
                cur = ''
                break
            else:
                cur += c

    def __repr__(self):
        return self.padrep('')

    def padrep(self,pad):
        s = pad+'[\n'
        for C in self.children:
            if isinstance(C,Expr):
                s += pad+'    '+C.getText()+'\n'
            else:
                s += C.padrep(pad+'    ')
        s += pad+']\n'
        return s

    def javarep(self):
        # Return a representation designed for use by
        # our java classes.
        s = ''

        # We only want the list of condition-solution pairs.
        L = self.children[0].children

        s += '~begin-list\n'
        for C in L:
            if isinstance(C,Expr): continue
            cond = C.children[0]
            soln = C.children[1]
            s += '~begin-pair\n'
            s += '~begin-cond\n'
            s += cond.getText()+'\n'
            s += '~end-cond\n'
            s += '~begin-soln\n'
            for eq in soln.children:
                s += '~begin-eq\n'
                s += eq.getText()+';\n'
                s += '~end-eq\n'
            s += '~end-soln\n'
            s += '~end-pair\n'
        s += '~end-list\n'
        return s


class Expr:

    def __init__(self, text):
        self.text = text

    def getText(self):
        return self.text

    def __repr__(self):
        print self.text


class StrIter:

    def __init__(self, s):
        self.s = s
        self.L = len(s)
        self.ptr = 0

    def has_next(self):
        return (self.ptr < self.L)

    def next(self):
        if self.ptr < self.L:
            c = self.s[self.ptr]
            self.ptr += 1
        else:
            c = None
        return c


def runrl(formula):

    # diag:
    #open('/home/skieffer/Desktop/rlpout','a').write(formula)
    #

    # Build the input.
    s = ''
    s += 'off output;\n'
    s += 'load redlog;\n'
    s += 'rlset ofsf;\nphi := '
    s += formula
    s += ';\n'
    s += 'on output;\n'
    s += 'on rlqeaprecise;\n'
    s += 'off nat;\n'
    s += 'rlqea phi;\n'

    # Run reduce.
    red = Popen(
        'reduce', stdin=PIPE, stdout=PIPE, stderr=PIPE
    )
    red.stdin.write(s)
    red.stdin.close()
    out = red.stdout.read()
    red.stdout.close()
    
    # diag:
    #open('/home/skieffer/Desktop/rlpout','a').write(out)
    #

    # Process the output.
    if (out.find('***** Segmentation Violation') >= 0):
        t = 'segfault'
    else:
        s = out.find('8:') + 2
        out = out[s:]
        t = out.find('$')
        out = out[:t]
        out = out.strip()

        L = List(text=out)
        L.parse()
        
        t = L.javarep()
    
        #diag:
        #print repr(L)
        #

    return t


def filter():
    formula = sys.stdin.read()
    result = runrl(formula)
    # diag:
    #open('/home/skieffer/Desktop/rlpout','a').write(
    #    '\n'+ '-'*70 + '\n' +
    #    result +
    #    '='*70 + '\n'
    #)
    #
    sys.stdout.write(result)


if __name__ == '__main__':
    filter()

