@echo off
set GCC="%~1"
set PARAMETERS="-w"
set SOURCE="%~2"
set EXECUTABLE="%~3"

%GCC% %PARAMETERS% %SOURCE% -o %EXECUTABLE%