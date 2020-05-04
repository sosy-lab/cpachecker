The binary for MPI was taken from https://www.open-mpi.org/ using the latest version
(4.0.3 as of this writing).

It is distributed unter the 3-clause BSD license, c.f. https://www.open-mpi.org/community/license.php


Steps for building MPI
----------------------------
> mkdir temp
> cd temp

# The url is taken from the sourceforge repository mentioned above
> wget https://download.open-mpi.org/release/open-mpi/v4.0/openmpi-4.0.3.tar.gz
> cd openmpi-4.0.3

# The binary was build with the option to using it in Java.
# Otherwise, the '--enable-mpi-java' tag can be left out in the following command
> ./configure --enable-mpi-java --prefix=$(pwd)/install
> make

# At this point, the binary could be created using 'sudo make install'
# However, as I wanted it installed only for the current user, I created the binaries
# using the tool 'checkinstall'.
# More information about that can be found at https://stackoverflow.com/a/14516283
> sudo checkinstall

# The binary is now available at temp/openmpi-4.0.3/install/bin/mpiexec
> cd ..

