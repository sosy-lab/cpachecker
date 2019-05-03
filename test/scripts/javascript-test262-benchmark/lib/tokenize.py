import esprima
from esprima.tokenizer import BufferEntry


class Token:
    """
    Wrap BufferEntry to implement ___eq__ for it
    """

    def __init__(self, buffer_entry: BufferEntry):
        self.bufferEntry = buffer_entry

    def __eq__(self, other):
        return (self.bufferEntry.type == other.bufferEntry.type and
                self.bufferEntry.value == other.bufferEntry.value and
                self.bufferEntry.regex == other.bufferEntry.regex and
                self.bufferEntry.range == other.bufferEntry.range and
                self.bufferEntry.loc == other.bufferEntry.loc)

    def __str__(self):
        return str(self.bufferEntry)

    def __repr__(self):
        return str(self.bufferEntry)


def tokenize(program):
    return list(map(Token, esprima.tokenize(program)))
