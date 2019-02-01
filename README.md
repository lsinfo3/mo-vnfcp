# MO-VNFCP
A multi-objective heuristic for VNF chain placement.

This repository contains the sources used for the work published in:
> S. Lange, A. Grigorjew, T. Zinner, P. Tran-Gia, and M. Jarschel,
> “A multi-objective heuristic for the optimization of virtual network function chain placement,”
> in Proceedings of the 29th International Teletraffic Congress (ITC), Genoa, Italy, 2017.

# Dependencies and Compatibility
The project was initially compiled and tested with Oracle Java Version 9.
Some functions require the JavaFX Runtime, which should be included in Oracle JDK bundles, but may need a separate installation if only a Java Runtime Environment was installed.

The latest version of this project was also tested with OpenJDK 11 and OpenJFX 11.

# Execution
You can either load the source code in a Java IDE of your choice and run the respective classes directly,
or simply run the jar file contained in the [artifacts](java/VNFP/out/artifacts/VNFP_jar) folder.
Its first execution should create a default config file [config.js](java/VNFP/resources/config.js) in your current working directory.

If you are using OpenJDK with the open source JavaFX implementation, you may need to add the OpenJFX libraries to your module dependencies.
In addition, executing classes other than `Main` may require the following Java VM arguments:
```
--module-path <PATH TO YOUR OPENJFX LIBS> --add-modules=javafx.controls --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED --add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED --add-exports=javafx.base/com.sun.javafx.beans=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.glass.utils=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
```
Executing `Main` requires a valid path to a config file as first program argument.
Using the configuration from [res/config.js](java/VNFP/res/config.js) should run a quick optimization and directly display its results in the graphical user interface.

Adjust the default configuration to your needs, for example:
- Set the `basePath` to a valid problem instance, for example contained in `java/VNFP/res/problem_instances/internet2`.
- Adjust the `runtime` for faster testing.
- Set `showGui` to `true` to visualize the end results.

