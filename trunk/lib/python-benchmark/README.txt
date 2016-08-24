To install or upgrade Python libraries, run the following command in the CPAchecker directory:

pip3 wheel --wheel-dir lib/python-benchmark -r lib/python-benchmark/requirements.txt

This upgrades all dependencies to the newest available version.

To add new dependencies, add them to lib/python-benchmark-requirements.txt
in the style of a requirements.txt file for pip
(https://pip.readthedocs.org/en/stable/user_guide/#requirements-files).

Do not forget to check in all files under lib/python-benchmark/.
We currently distribute these libraries in the repository
because downloading them requires "pip3", which many users do not have installed.
