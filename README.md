# Project Metadata

## GROUP: **comp2022-7e**

NAME1: Francisco Pires, NR1: up201908044, GRADE1: 18, CONTRIBUTION1: 33.(3)%

NAME2: Sérgio da Gama, NR2: up201906690, GRADE2: 18, CONTRIBUTION2: 33.(3)%

NAME3: Tomás Fidalgo, NR3: up201906743, GRADE3: 18, CONTRIBUTION3: 33.(3)%

## GLOBAL Grade of the project: 18

## SUMMARY: (Describe what your tool does and its main features.)

We have developed a jmm (Java Minus Minus) compiler which is capable of compiling programs written in the subset of java called jmm.

## SEMANTIC ANALYSIS: (Refer the semantic rules implemented by your tool.)

- if there are array accesses on non array variables
- if an array index is of type integer
- if the type of the returned expression inside a method is the same as the said method return type
- the existence of 'this' in the main method, which is not allowed
- if a method being called, actually exists and it is defined, or it is being imported
- if the arguments passed to a function call have the same type as the actual function parameters
- if all the arguments to a function call are passed
- if the extended classes are imported
- if the 'while' and 'if' conditional expressions evaluate to a boolean type
- if assignment expressions have corresponding/equal types in the right and left side
- if the method, variable and field names being defined are unique in their respective scope
- if only expressions that evaluate to int are used in arithmetic expressions

## CODE GENERATION: (describe how the code generation of your tool works and identify the possible problems your tool has regarding code generation.)

Our tool starts by converting the code file to a tree representation and generates a symbol tabel that holds the most important information about the code's classes, fields, methods and such.

With that information it then creates a new script in ollir code with the same functionality as the input code but that is more detailed and closer to what the java machine can execute.

Lastly, it takes the generated ollir code and converts it to the executable, stack based jasmin code for the JVM machine.

## PROS: (Identify the most positive aspects of your tool)

It is capable of correctly generating runnable code from a jmm file and warn the user about syntatic and semantic errors that may be present with the indication of the place (line and column) in which the error occurred.

## CONS: (Identify the most negative aspects of your tool)

The generated code may allocate a greater stack size than actually needed as it cannot evaluate the minimum stack size needed and there are no available options for optimization, the available optimizations are allways implemented, those being the substitution of calls like iload, istore and others for iload_0, istore_0, etc for register values from 0 to 3.

# Compilers Project

For this project, you need to install [Java](https://jdk.java.net/), [Gradle](https://gradle.org/install/), and [Git](https://git-scm.com/downloads/) (and optionally, a [Git GUI client](https://git-scm.com/downloads/guis), such as TortoiseGit or GitHub Desktop). Please check the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) for Java and Gradle versions.

## Project setup

There are three important subfolders inside the main folder. First, inside the subfolder named ``javacc`` you will find the initial grammar definition. Then, inside the subfolder named ``src`` you will find the entry point of the application. Finally, the subfolder named ``tutorial`` contains code solutions for each step of the tutorial. JavaCC21 will generate code inside the subfolder ``generated``.

## Compile and Running

To compile and install the program, run ``gradle installDist``. This will compile your classes and create a launcher script in the folder ``./build/install/comp2022-7e/bin``. For convenience, there are two script files, one for Windows (``comp2022-7e.bat``) and another for Linux (``comp2022-7e``), in the root folder, that call tihs launcher script.

After compilation, a series of tests will be automatically executed. The build will stop if any test fails. Whenever you want to ignore the tests and build the program anyway, you can call Gradle with the flag ``-x test``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``./build/reports/tests/test/index.html``.

## Checkpoint 1
For the first checkpoint the following is required:

1. Convert the provided e-BNF grammar into JavaCC grammar format in a .jj file
2. Resolve grammar conflicts, preferably with lookaheads no greater than 2
3. Include missing information in nodes (i.e. tree annotation). E.g. include the operation type in the operation node.
4. Generate a JSON from the AST

### JavaCC to JSON
To help converting the JavaCC nodes into a JSON format, we included in this project the JmmNode interface, which can be seen in ``src-lib/pt/up/fe/comp/jmm/ast/JmmNode.java``. The idea is for you to use this interface along with the Node class that is automatically generated by JavaCC (which can be seen in ``generated``). Then, one can easily convert the JmmNode into a JSON string by invoking the method JmmNode.toJson().

Please check the JavaCC tutorial to see an example of how the interface can be implemented.

### Reports
We also included in this project the class ``src-lib/pt/up/fe/comp/jmm/report/Report.java``. This class is used to generate important reports, including error and warning messages, but also can be used to include debugging and logging information. E.g. When you want to generate an error, create a new Report with the ``Error`` type and provide the stage in which the error occurred.


### Parser Interface

We have included the interface ``src-lib/pt/up/fe/comp/jmm/parser/JmmParser.java``, which you should implement in a class that has a constructor with no parameters (please check ``src/pt/up/fe/comp/CalculatorParser.java`` for an example). This class will be used to test your parser. The interface has a single method, ``parse``, which receives a String with the code to parse, and returns a JmmParserResult instance. This instance contains the root node of your AST, as well as a List of Report instances that you collected during parsing.

To configure the name of the class that implements the JmmParser interface, use the file ``config.properties``.

### Compilation Stages

The project is divided in four compilation stages, that you will be developing during the semester. The stages are Parser, Analysis, Optimization and Backend, and for each of these stages there is a corresponding Java interface that you will have to implement (e.g. for the Parser stage, you have to implement the interface JmmParser).


### config.properties

The testing framework, which uses the class TestUtils located in ``src-lib/pt/up/fe/comp``, has methods to test each of the four compilation stages (e.g., ``TestUtils.parse()`` for testing the Parser stage).

In order for the test class to find your implementations for the stages, it uses the file ``config.properties`` that is in root of your repository. It has four fields, one for each stage (i.e. ``ParserClass``, ``AnalysisClass``, ``OptimizationClass``, ``BackendClass``), and initially it only has one value, ``pt.up.fe.comp.SimpleParser``, associated with the first stage.

During the development of your compiler you will update this file in order to setup the classes that implement each of the compilation stages.
