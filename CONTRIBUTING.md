## Building

Jan20 builds with [Gradle](http://gradle.org). 
You do not need to install Gradle to build the project, as Gradle can install itself.

To build, run this in the checkout:

    ./gradlew check

That will build the entire project and run all the tests.

### IDE import

The project is setup to work with IntelliJ IDEA.
Run the following in the checkout…

    ./gradlew idea

Then import the project into IDEA or open the .ipr file.

If you use a different IDE or editor you're on your own.    

## Licensing and attribution

Jan20 is licensed under [ASLv2](http://www.apache.org/licenses/LICENSE-2.0). All source code falls under this license.

The source code will not contain attribution information (e.g. Javadoc) for contributed code.
Contributions will be recognised elsewhere in the project documentation.

## Code changes

Code can be submitted via GitHub pull requests.

### Proposing new features

If you would like to implement a new feature, please [raise an issue](https://github.com/workmarket-oss/jan20/issues) before sending a pull request so the feature can be discussed.

### Fixing bugs

If you would like to fix a bug, please [raise an issue](https://github.com/workmarket-oss/jan20/issues) before sending a pull request so it can be discussed.
If the fix is trivial or non controversial then this is not usually necessary.

## Coding style guidelines

The following are some general guide lines to observe when contributing code:

1. All source files must have the appropriate ASLv2 license header
1. All source files use an indent of 2 spaces
1. Everything needs to be tested
1. All public, private and package types and methods must include Javadoc. There is no need to Javadoc internal classes.
