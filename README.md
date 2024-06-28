# Kyle Schwab - PhD Computing Artifact

## Project Setup

There are two options available for installing, building, and running the project:

1. Using docker. This is the recommended solution.
2. [Using JPF Instructions.](https://github.com/javapathfinder/jpf-core/wiki/How-to-install-JPF) This is also outlined further down in the original JPF README.
   - Note: The graphviz package will need to be installed on the host system (e.g., `sudo dnf install graphviz` or `sudo apt-get install graphviz`).

### Using Docker

Instructions for installing Docker onto your system can be found [here](https://docs.docker.com/engine/install/#server).

Before running any project commands, enter the docker environment first:

```bash
# From root of project repo (note: sudo can be used if needed)
./dockerw run

# You should be greeted with the below prompt if successful
📦phd-computing-artifact-X.Y
username@hostname /jpf-core
$
```

While not necessary, the docker container can be built locally if desired:

```bash
# From root of project repo
./provision/build_docker.bash
```

## Project Artifacts

The project artifact changeset can be viewed [here](https://github.com/kschwab/jpf-core/commit/1e2a529c6a10ab45609c605abe31f871371dfc6a).

The source files of relevance are:
* [src/examples/RacerInterleave.java](https://github.com/kschwab/jpf-core/commit/1e2a529c6a10ab45609c605abe31f871371dfc6a#diff-8697dd86f21647703768d83652e84353dff9af16047eb0f6731cb21a135ad811) - The test cases.
* [src/main/gov/nasa/jpf/listener/StateSpaceDot.java](https://github.com/kschwab/jpf-core/commit/1e2a529c6a10ab45609c605abe31f871371dfc6a#diff-ab2249a6184a3c567e6045deb52b978866538a48bb224b779fa929b2c1638576) - The state space graph fixes.
* [src/main/gov/nasa/jpf/vm/ThreadData.java](https://github.com/kschwab/jpf-core/commit/1e2a529c6a10ab45609c605abe31f871371dfc6a#diff-7dd4777e2dc916950678b32bb8bdf64986c9b7d1daea8339b97377ff5e9c9c0b) - The new thread interleave code annotation.
* [src/main/gov/nasa/jpf/vm/ThreadInfo.java](https://github.com/kschwab/jpf-core/commit/1e2a529c6a10ab45609c605abe31f871371dfc6a#diff-37c613a18e737c8fc33f2437106955fa286a9e654beb53306cb4e8b8b48df5fd) - The new thread interleave code annotation.

### Building the JPF Jars (onetime step)

```bash
# From root of project repo
./gradlew buildJars
```

### Running the computing artifact

```bash
# From root of project repo
java -Xmx1G -jar ./build/RunJPF.jar +classpath=. src/examples/RacerInterleave.jpf

# This should generate the "jpf-state-space.dot" file in project root directory.
# It will overwrite any existing ones if present.
# The svg file (jpf-state-space.svg) can then be generated by running the following:
dot -Tsvg jpf-state-space.dot -o jpf-state-space.svg

# There are 21 test cases (0 to 20).
# A convienice script has been provided to run them all:
./run_all_test_cases.py
```

# Java Pathfinder (JPF)
![build master](https://github.com/javapathfinder/jpf-core/actions/workflows/simple_build.yml/badge.svg)

An extensible software model checking framework for Java bytecode programs

## General Information about JPF

All the latest developments, changes, documentation can be found on our
[wiki](https://github.com/javapathfinder/jpf-core/wiki) page.

## Building and Installing

If you are having problems installing and running JPF, please look at the [How
to install
JPF](https://github.com/javapathfinder/jpf-core/wiki/How-to-install-JPF) guide.


We have documented on the wiki a lot of common problems that might occur during the installation and
build processes as reported by the users.  If you are facing any issue, please, make
sure that we have not addressed it in documentation. Otherwise, feel free to
contact us at java-pathfinder@googlegroups.com or open an issue on the Issue
Tracker.

## Documentation

There is a constant effort to update and add JPF documentation on the wiki.
If you would like to contribute in that, please, contact us at
java-pathfinder@googlegroups.com.

Contributions are welcome and we encourage you to get involved with the
community.

Happy Verification
*-- the Java PathFinder team*
