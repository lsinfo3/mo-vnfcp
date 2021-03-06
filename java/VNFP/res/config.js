/*
 Dynamic Config Format

 This entire config file is parsed by a JavaScript engine.
 Strings (such as file paths) must be enclosed in quotes.
 Backslashes and other language-specific symbols must be escaped.
 Comments may be declared with //.
 */

// PSA Config:
s = 8
m = 500
tmax = 50
tmin = 1
rho = 0.75

// If runtime is set and > 0, the algorithm's execution time is set to that value (in seconds).
// In that case, the parameter 'm' is ignored.
runtime = 10

numberOfTemperatureLevels = Math.ceil(Math.log(tmin / tmax) / Math.log(rho))

// PSA Input:
// Base path for every input-file:
// (Absolute paths won't be resolved against the inBasePath.)
inBasePath = "res/problem_instances/internet2"
topologyFile = "topology"
vnfLibFile = "vnfLib"
requestsFile = "requests"

// Output:
// Comment out if the file is not needed.
// ---
showGui = true
executionProgress = true

// Base path for every output-file:
// (Absolute paths won't be resolved against the outBasePath.)
outBasePath = "/tmp/psa"
// Main frontier:
paretoFrontier = "psa_pareto_frontier" // CSV of pareto frontier (objective space)
results = "psa_results" // human readable pareto frontier
// Actual placement:
placementNodes = "psa_placement_nodes" // for each node: used resources, remaining resources, vnf list
placementLinks = "psa_placement_links" // for each link: used bandwidth, remaining bandwidth, flow list
placementVnfs = "psa_placement_vnfs" // for each vnf: load, used capacity, remaining capacity, flow list
placementFlows = "psa_placement_flows" // for each request: delay, route (list of nodes, NFs are applied at nodes in [brackets])
// Decision Making:
feasibleFrontier = "psa_feasible_frontier" // CSV of feasible pareto frontier (objective space, only feasible solutions)
solutionOrder = "psa_order" // CSV with an ordered list of solution IDs for each rank/weight combination
weightVectors = "psa_weights" // uniform, entropy, coeff and std weights for all objectives
rankingVectors = "psa_rankings" // SAW, MEW, TOPSIS and VIKOR ranks for all solutions

// Debug:
//paretoFrontierDevObs = "psa_pareto_frontier_developement" // CSV of pareto frontier developement over time (objective space)
//vnfLoads = "psa_vnf_loads" // CSV of a single solution's VNF loads at every temperature level
//vnfDetails = "psa_vnf_details" // Detailed CSV-overview of a single solution's VNF, including differences and node locations
//solutionSets = "psa_solution_sets" // CSV of the solution set at every temperature change

// Method for retrieving the initial solution set:
// Possible values: RAND, SHORT_PSA, LEAST_DELAY, LEAST_CPU, EXISTING
// (no quotation marks required)
prepMode = LEAST_DELAY
// If prepMode = EXISTING, provide a valid placement here ("placementFlows" file)
existingPlacementFlows = outBasePath + "/prev_psa_placement_flows"

/*
 Define objective vectors for determining dominance relationships.

 Given 2 solutions A and B, the dominance is given by:
 - If A is feasible and B is unfeasible: A dominates B.
 - If both are unfeasible: unfeasibleVectors are compared.
 - If both are feasible: objectiveVectors are compared

 All objectives are to be minimized.

 Possible indices for the value-array 'v' are stored in the following (uppercase) variables.
 Prefix TOTAL indicates a sum of all respective values.
 Prefix MEAN indicates the average of all respective values.
 Prefix MEDIAN indicates the median ((n-1)/2-th smallest) of all respective values.
 Suffix INDEX indicates the ratio (value) / (minimum possible value).
 Infix ROOTED indicates that, before summing up the values, their square root has been taken.

 Link resources:
 - TOTAL_DELAY
 - MEAN_DELAY_INDEX
 - MEDIAN_DELAY_INDEX
 - MAX_DELAY_INDEX
 ---
 - NUMBER_OF_HOPS
 - MEAN_HOPS_INDEX
 - MEDIAN_HOPS_INDEX
 - MAX_HOPS_INDEX

 Migrations:
 - NUMBER_OF_VNF_REPLACEMENTS
 - TOTAL_FLOW_MIGRATION_PENALTY

 Node resources:
 - TOTAL_USED_RESOURCES[0]
 - TOTAL_USED_RESOURCES[1]
 - TOTAL_USED_RESOURCES[2]
 - ...
 They can also be called by their name from the VnfLib:
 If you have defined a resource called "cpu", use:
 - TOTAL_USED_RESOURCE_CPU

 VNF instance resources:
 - TOTAL_ROOTED_VNF_LOADS
 - MEAN_INVERSE_LOAD_INDEX
 - MEDIAN_INVERSE_LOAD_INDEX
 - NUMBER_OF_VNF_INSTANCES

 Indicators of unfeasibility:
 'OVERLOADED' refers to VNF instances on nodes with exhausted resources.
 'EXCESSIVE' refers to VNF types with more instances than permitted.
 - UNFEASIBLE
 - NUMBER_OF_DELAY_VIOLATIONS
 - NUMBER_OF_RESOURCE_VIOLATIONS
 - NUMBER_OF_EXCESSIVE_VNFS
 - NUMBER_OF_CONGESTED_LINKS
 - TOTAL_OVERLOADED_VNF_CAPACITY
 - TOTAL_ROOTED_EXCESSIVE_VNF_CAPACITY
 */
function objectiveVector(v) {
    return [
        v[MEAN_DELAY_INDEX],
        v[TOTAL_USED_RESOURCE_CPU],
        v[NUMBER_OF_VNF_INSTANCES]
        //v[TOTAL_FLOW_MIGRATION_PENALTY]
    ]
}
function unfeasibleVector(v) {
    return [
        v[MEAN_DELAY_INDEX],
        v[MEAN_HOPS_INDEX],
        v[MEAN_INVERSE_LOAD_INDEX],
        v[NUMBER_OF_DELAY_VIOLATIONS] + v[NUMBER_OF_RESOURCE_VIOLATIONS] + v[NUMBER_OF_CONGESTED_LINKS],
        v[TOTAL_OVERLOADED_VNF_CAPACITY],
        v[TOTAL_ROOTED_EXCESSIVE_VNF_CAPACITY]
    ]
}

// Probability for replacing a VNF instance instead of a single flow:
// The current temperature is stored in the variable 't'.
// The variable 'i' contains the iteration number (0 <= i < numberOfTemperatureLevels).
pmin = 0.2
pmax = 0.8
i1 = 0.2 * numberOfTemperatureLevels
i2 = 0.8 * numberOfTemperatureLevels
pReassignVnf = (i2 - i)/(i2 - i1) * (pmax - pmin) + pmin
pReassignVnf = Math.max(pmin, Math.min(pmax, pReassignVnf))
//pReassignVnf = 1.0

// Probability for creating new VNF instances while routing:
pNewInstance = pReassignVnf/2.0

// Acceptance Probabilities:
// The variables 'better' and 'incomp' contain the corresponding numbers of generated neighbour solutions
// during the last temperature level. The total number of generated neighbours is stored in 'n'.
factorWorse = 1.1
factorIncomp = 1.2
maxWorse = 0.25
maxIncomp = 0.5

acceptWorse = (t / tmax * factorWorse) * (better / n)
acceptIncomparable = (t / tmax * factorIncomp) * (better / incomp)

acceptWorse = Math.min(acceptWorse, maxWorse)
acceptIncomparable = Math.min(acceptIncomparable, maxIncomp)

// How much should randomness be influenced by weights:
useWeights = true
useDelayInWeights = true
useHopsInWeights = true
