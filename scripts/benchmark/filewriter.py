import os

def writeFile(name, content):
    """
    Simply write some content to a file, overriding the file if necessary.
    """
    with open(name, "w") as file:
        file.write(content)

class FileWriter:
    """
    The class FileWriter is a wrapper for writing content into a file.
    """

    def __init__(self, filename, content):
        """
        The constructor of FileWriter creates the file.
        If the file exist, it will be OVERWRITTEN without a message!
        """

        self.__filename = filename
        self.__needsRewrite = False
        self.__content = content

        # Open file with "w" at least once so it will be overwritten.
        writeFile(self.__filename, content)

    def append(self, newContent, keep=True):
        """
        Add content to the represented file.
        If keep is False, the new content will be forgotten during the next call
        to this method.
        """
        content = self.__content + newContent
        if keep:
            self.__content = content

        if self.__needsRewrite:
            """
            Replace the content of the file.
            A temporary file is used to avoid loss of data through an interrupt.
            """
            tmpFilename = self.__filename + ".tmp"

            writeFile(tmpFilename, content)

            os.rename(tmpFilename, self.__filename)
        else:
            with open(self.__filename, "a") as file:
                file.write(newContent)

        self.__needsRewrite = not keep

    def replace(self, newContent):
        # clear and append
        self.__content = ''
        self.__needsRewrite = True
        self.append(newContent)
