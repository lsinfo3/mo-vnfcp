# MO-VNFCP
A multi-objective heuristic for VNF chain placement.

This repository contains the sources used for the work published in:
> S. Lange, A. Grigorjew, T. Zinner, P. Tran-Gia, and M. Jarschel,
> “A multi-objective heuristic for the optimization of virtual network function chain placement,”
> in Proceedings of the 29th International Teletraffic Congress (ITC), Genoa, Italy, 2017.

# Dependencies
The project was compiled and tested with Java Version 9.
Some functions require the JavaFX Runtime, which should be included in JDK bundles, but may need a separate installation if only a Java Runtime Environment was installed.

# Execution
You can either load the source code in a Java IDE of your choice and run the respective classes directly,
or simply run the jar file contained in the [artifacts](java/VNFP/out/artifacts/VNFP_jar) folder.
Its first execution should create a default config file [config.js](java/VNFP/resources/config.js) in your current working directory.
Adjust its contents accordingly, for example:
- Set the `basePath` to a valid problem instance, for example contained in `java/VNFP/res/problem_instances/BCAB15`.
- Adjust the `runtime` for faster testing.
- Set `showGui` to `true` to visualize the end results.

# Notes
The problem instance `BCAB15` refers to `Internet2` in the paper.
Other problem instances use the same names as in the paper.

The starting point of the performance comparison is the `Comparison.java`.
The used parameters can be found in the res-folder, e.g., `java/VNFP/res/problem_instances/BCAB15/config.js` for the `Internet2` topology.
The main differences between the instances include:

- `prepMode` (initial solution generation strategy),
- `pReassignVnf` (pRemoveVNF)
- `pNewInstance` (pCreateVNF)
