# MO-VNFCP
A multi-objective heuristic for VNF chain placement.

This repository contains the sources used for the work published in:
> S. Lange, A. Grigorjew, T. Zinner, P. Tran-Gia, and M. Jarschel,
> “A multi-objective heuristic for the optimization of virtual network function chain placement,”
> in Proceedings of the 29th International Teletraffic Congress (ITC), Genoa, Italy, 2017.

# Notes
The problem instance BCAB15 refers to Internet2 in the paper.
Other problem instances use the same names as in the paper.

The starting point of the performance comparison is the Comparison.java.
The used parameters can be found in the res-folder, e.g., java/VNFP/res/problem_instances/BCAB15/config.js for the Internet2 topology.
The main differences between the instances include:

- prepMode (initial solution generation strategy),
- pReassignVnf (pRemoveVNF)
- pNewInstance (pCreateVNF)
