package de.uniwue.VNFP.util;

import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.model.log.*;
import de.uniwue.VNFP.model.solution.TrafficAssignment;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Loads and provides values from a Config file.
 * Saves a config file with default values in the classpath if none is provided.
 *
 * @author alex
 */
public class Config {
    private static Config instance;

    private final ScriptEngine js;
    private final String configContent;
    private boolean resourceObjInjected;

    // PSA Config:
    public final int s;
    public final int m;
    public final double tmax;
    public final double tmin;
    public final double rho;
    public final double runtime;

    // Base path for every input- and output-file:
    public final Path inBasePath;
    public final Path outBasePath;

    // PSA Input:
    public final Path topologyFile;
    public final Path vnfLibFile;
    public final Path requestsFile;

    // Output:
    public final boolean executionProgress;
    public final boolean showGui;
    public final Path results;
    public final Path paretoFrontier;
    public final Path paretoFrontierDevObs;
    public final Path vnfLoads;
    public final Path vnfDetails;
    public final Path solutionSets;
    public final Path placementNodes;
    public final Path placementLinks;
    public final Path placementVnfs;
    public final Path placementFlows;
    public final Path feasibleFrontier;
    public final Path solutionOrder;
    public final Path weightVectors;
    public final Path rankingVectors;
    public final Path existingPlacementFlows;

    // Weights:
    public final boolean useWeights;
    public final boolean useDelayInWeights;
    public final boolean useHopsInWeights;

    // Method for retrieving the initial solution set:
    public enum PSAPreparationModes { RAND, SHORT_PSA, LEAST_DELAY, LEAST_CPU, EXISTING }
    public final PSAPreparationModes prepMode;

    /**
     * @return A global (singleton) Config instance. If none is present yet, the default Config will be used.
     */
    public static Config getInstance() {
        if (instance == null) {
            instance = getInstance(Config.class.getClassLoader().getResourceAsStream("config.js"));
        }
        return instance;
    }

    /**
     * @param configStream The content of the config.
     * @return A global (singleton) Config instance.
     */
    public static Config getInstance(InputStream configStream) {
        if (instance != null) {
            System.out.println("Repeated call of Config.getInstance(InputStream configStream); existing instance will be overwritten.");
        }
        instance = new Config(configStream);
        return instance;
    }

    /**
     * Creates a new Config instance with default values;
     */
    public Config(InputStream configStream) {
        Objects.requireNonNull(configStream);

        js = new ScriptEngineManager().getEngineByName("JavaScript");

        try {
            configContent = new Scanner(configStream, "utf-8").useDelimiter("\\Z").next();

            for (PSAPreparationModes mode : PSAPreparationModes.values()) {
                js.put(mode.name(), mode);
            }

            js.eval("function convertArray(type, arr) {\n" +
                    "  var jArr = java.lang.reflect.Array.newInstance(type, arr.length);\n" +
                    "  for (var i = 0; i < arr.length; i++) {\n" +
                    "    jArr[i] = arr[i];\n" +
                    "  }\n" +
                    "  return jArr;\n" +
                    "};\n" +
                    "function objectiveVectorDouble(v) { return convertArray(java.lang.Double.TYPE, objectiveVector(v)); }\n" +
                    "function unfeasibleVectorDouble(v) { return convertArray(java.lang.Double.TYPE, unfeasibleVector(v)); }");

            js.eval("t = 0");
            js.eval("i = 0");
            js.eval("better = 0");
            js.eval("incomp = 0");
            js.eval("runtime = 0");
            js.eval("n = 0");
            js.eval(configContent);
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        s = getAsInt(js, "s");
        m = getAsInt(js, "m");
        tmax = getAsDouble(js, "tmax");
        tmin = getAsDouble(js, "tmin");
        rho = getAsDouble(js, "rho");
        runtime = getAsDouble(js, "runtime");

        Object prepModeTemp = js.get("prepMode");
        if (!(prepModeTemp instanceof PSAPreparationModes)) {
            throw new IllegalArgumentException("prepMode may only be one of " + Arrays.toString(PSAPreparationModes.values()) + " (without quotes)");
        }
        prepMode = (PSAPreparationModes) prepModeTemp;

        inBasePath = Paths.get(getAsString(js, "inBasePath"));
        outBasePath = Paths.get(getAsString(js, "outBasePath"));

        topologyFile = inBasePath.resolve(getAsString(js, "topologyFile"));
        vnfLibFile = inBasePath.resolve(getAsString(js, "vnfLibFile"));
        requestsFile = inBasePath.resolve(getAsString(js, "requestsFile"));

        executionProgress = (js.get("executionProgress") == null || getAsBoolean(js, "executionProgress"));
        showGui = (js.get("showGui") != null && getAsBoolean(js, "showGui"));
        results = (js.get("results") == null ? null : outBasePath.resolve(getAsString(js, "results")));
        paretoFrontier = (js.get("paretoFrontier") == null ? null : outBasePath.resolve(getAsString(js, "paretoFrontier")));
        paretoFrontierDevObs = (js.get("paretoFrontierDevObs") == null ? null : outBasePath.resolve(getAsString(js, "paretoFrontierDevObs")));
        vnfLoads = (js.get("vnfLoads") == null ? null : outBasePath.resolve(getAsString(js, "vnfLoads")));
        vnfDetails = (js.get("vnfDetails") == null ? null : outBasePath.resolve(getAsString(js, "vnfDetails")));
        solutionSets = (js.get("solutionSets") == null ? null : outBasePath.resolve(getAsString(js, "solutionSets")));
        placementNodes = (js.get("placementNodes") == null ? null : outBasePath.resolve(getAsString(js, "placementNodes")));
        placementLinks = (js.get("placementLinks") == null ? null : outBasePath.resolve(getAsString(js, "placementLinks")));
        placementVnfs = (js.get("placementVnfs") == null ? null : outBasePath.resolve(getAsString(js, "placementVnfs")));
        placementFlows = (js.get("placementFlows") == null ? null : outBasePath.resolve(getAsString(js, "placementFlows")));
        feasibleFrontier = (js.get("feasibleFrontier") == null ? null : outBasePath.resolve(getAsString(js, "feasibleFrontier")));
        solutionOrder = (js.get("solutionOrder") == null ? null : outBasePath.resolve(getAsString(js, "solutionOrder")));
        weightVectors = (js.get("weightVectors") == null ? null : outBasePath.resolve(getAsString(js, "weightVectors")));
        rankingVectors = (js.get("rankingVectors") == null ? null : outBasePath.resolve(getAsString(js, "rankingVectors")));
        existingPlacementFlows = (prepMode != PSAPreparationModes.EXISTING ? null : Paths.get(getAsString(js, "existingPlacementFlows")));

        useWeights = getAsBoolean(js, "useWeights");
        useDelayInWeights = useWeights && getAsBoolean(js, "useDelayInWeights");
        useHopsInWeights = useWeights && getAsBoolean(js, "useHopsInWeights");

        // Get those values once to ensure they are available:
        double pReassignVnf = getAsDouble(js, "pReassignVnf");
        double pNewInstance = getAsDouble(js, "pNewInstance");
//        try {
//            ((Invocable) js).invokeFunction("objectiveVector", (Object) new double[new Objs(new String[0]).values().length + 20]);
//        }
//        catch (ScriptException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
    }

    /**
     * Executes the config's formula for 'pReassignVnf' with the given
     * temperature as JavaScript-variable 't'.
     *
     * @param currentTemperature The current temperature level of the PSA algorithm.
     * @param tempIndex          The index of the current temperature iteration (0 <= tempIndex < numberOfTemperatureLevels).
     * @return The value of 'pReassignVnf' after executing config with the given temperature.
     */
    public double pReassignVnf(double currentTemperature, int tempIndex) {
        try {
            js.eval("i = " + tempIndex);
            js.eval("t = " + currentTemperature);
            js.eval(configContent);
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        return getAsDouble(js, "pReassignVnf");
    }

    /**
     * Executes the config's formula for 'pNewInstance' with the given
     * temperature as JavaScript-variable 't'.
     *
     * @param currentTemperature The current temperature level of the PSA algorithm.
     * @param tempIndex          The index of the current temperature iteration (0 <= tempIndex < numberOfTemperatureLevels).
     * @return The value of 'pNewInstance' after executing config with the given temperature.
     */
    public double pNewInstance(double currentTemperature, int tempIndex) {
        try {
            js.eval("i = " + tempIndex);
            js.eval("t = " + currentTemperature);
            js.eval(configContent);
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        return getAsDouble(js, "pNewInstance");
    }

    /**
     * Executes the config's formula for 'acceptWorse' with the given
     * temperature and number of dominating / incomparable solutions.
     *
     * @param tempLevel     The current temperature level of the PSA algorithm.
     * @param tempIndex     The index of the current temperature iteration (0 <= tempIndex < numberOfTemperatureLevels).
     * @param better        The number of generated better neighbours in the last temperature iteration.
     * @param incomp        The number of generated incomparable neighbours in the last temperature iteration.
     * @param numIterations The number of iterations during the last temperature level.
     * @return The value of 'acceptWorse' after executing config with the given parameters.
     */
    public double acceptWorse(double tempLevel, int tempIndex, int better, int incomp, int numIterations) {
        try {
            js.eval("t = " + tempLevel);
            js.eval("i = " + tempIndex);
            js.eval("better = " + better);
            js.eval("incomp = " + incomp);
            js.eval("n = " + numIterations);
            js.eval(configContent);
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        return getAsDouble(js, "acceptWorse");
    }

    /**
     * Executes the config's formula for 'acceptIncomparable' with the given
     * temperature and number of dominating / incomparable solutions.
     *
     * @param tempLevel     The current temperature level of the PSA algorithm.
     * @param tempIndex     The index of the current temperature iteration (0 <= tempIndex < numberOfTemperatureLevels).
     * @param better        The number of generated better neighbours in the last temperature iteration.
     * @param incomp        The number of generated incomparable neighbours in the last temperature iteration.
     * @param numIterations The number of iterations during the last temperature level.
     * @return The value of 'acceptIncomparable' after executing config with the given parameters.
     */
    public double acceptIncomparable(double tempLevel, int tempIndex, int better, int incomp, int numIterations) {
        try {
            js.eval("t = " + tempLevel);
            js.eval("i = " + tempIndex);
            js.eval("better = " + better);
            js.eval("incomp = " + incomp);
            js.eval("n = " + numIterations);
            js.eval(configContent);
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        return getAsDouble(js, "acceptIncomparable");
    }

    /**
     * @return A collection of every Event logger that
     * @throws IOException When creating the Writers fails.
     */
    public Collection<PSAEventLogger> createAllEventLoggers() throws IOException {
        LinkedList<PSAEventLogger> list = new LinkedList<>();

        if (executionProgress) list.add(new ExecutionProgressObserver());
        if (results != null) list.add(new PSAResultPrinter(getWriterFor(results)));
        if (paretoFrontier != null) list.add(new ParetoFrontierExporter(getWriterFor(paretoFrontier)));
        if (paretoFrontierDevObs != null) list.add(new ParetoFrontierDevelopementObserver(getWriterFor(paretoFrontierDevObs)));
        if (vnfLoads != null) list.add(new VnfLoadsExporter(getWriterFor(vnfLoads)));
        if (vnfDetails != null) list.add(new VnfDetailsExporter(getWriterFor(vnfDetails)));
        if (solutionSets != null) list.add(new SolutionSetExporter(getWriterFor(solutionSets)));
        if (placementNodes != null || placementLinks != null || placementVnfs != null || placementFlows != null) {
            list.add(new PlacementExporter(
                    getWriterFor(placementNodes),
                    getWriterFor(placementLinks),
                    getWriterFor(placementVnfs),
                    getWriterFor(placementFlows)
            ));
        }
        if (rankingVectors != null || weightVectors != null) {
            list.add(new RankingsExporter(
                    getWriterFor(feasibleFrontier),
                    getWriterFor(solutionOrder),
                    getWriterFor(weightVectors),
                    getWriterFor(rankingVectors)
            ));
        }

        return list;
    }

    /**
     * Creates a BufferedWriter for the given file and creates the underlying folders, if necessary.
     *
     * @param path Target file path. May be null if no Writer is required.
     * @return BufferedWriter for the given path, or null if path is null.
     */
    private BufferedWriter getWriterFor(Path path) throws IOException {
        if (path == null) return null;
        Files.createDirectories(path.getParent());
        return Files.newBufferedWriter(path);
    }

    /**
     * Attempts to estimate the fields used by the objective functions by injecting one-hot-objectives into it.
     *
     * @param lib VNF library (used for resource data)
     * @return An estimated array of used objectives.
     */
    public Objs.Obj[] getRelevantObjectives(VnfLib lib) {
        ArrayList<Objs.Obj> list = new ArrayList<>();
        Objs objs = new Objs(lib.res);
        injectResources(objs);

        Objs.Obj[] allVals = objs.values();
        double[] vals = new double[allVals.length];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = 1.0;
            Solution s = Solution.getInstance(
                    new ProblemInstance(new NetworkGraph(false), new VnfLib(), new TrafficRequest[0], objs),
                    new TrafficAssignment[0]
            );
            s.vals = vals;
            double[] multiObj = objectiveVector(s);
            vals[i] = 0.0;

            for (double singleObj : multiObj) {
                if (singleObj != 0.0) {
                    list.add(allVals[i]);
                    break;
                }
            }
        }

        return list.toArray(new Objs.Obj[list.size()]);
    }

    /**
     * Calls the config file's objectiveVector-function with the solution's value-array
     * and returns the resulting array.
     *
     * @param s Solution for which we want the objective vector.
     * @return Objective function vector (feasible case) for this solution.
     */
    public double[] objectiveVector(Solution s) {
        Objects.requireNonNull(s);
        Objs objs = s.obj;
        injectResources(objs);

        Object o = null;
        try {
            o = ((Invocable) js).invokeFunction("objectiveVectorDouble", (Object) s.vals);
        }
        catch (ScriptException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return (double[]) o;
    }

    /**
     * Calls the config file's unfeasibleVector-function with the given value-array
     * and returns the result-array.
     *
     * @param vals Raw objective function values from a Solution-Object.
     * @return Objective function vector (unfeasible case) for this solution.
     */
    public double[] unfeasibleVector(double[] vals) {
        Objects.requireNonNull(vals);

        try {
            return (double[]) ((Invocable) js).invokeFunction("unfeasibleVectorDouble", (Object) vals);
        }
        catch (ScriptException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method checks whether the node resource indices (TOTAL_DELAY, ...) were already
     * injected into the javascript environment as variables, and does so if required.
     *
     * @param objs All objectives, including the node resources array.
     */
    private void injectResources(Objs objs) {
        if (!resourceObjInjected) {
            js.put("TOTAL_USED_RESOURCES", Arrays.stream(objs.TOTAL_USED_RESOURCES).mapToInt(o -> o.i).toArray());
            for (Objs.Obj o : objs.values()) {
                js.put(o.name, o.i);
            }
            resourceObjInjected = true;
        }
    }

    /**
     * Saves the copied content into the given OutputStream and closes the stream.
     * @param out Destination of the Config.
     */
    public void writeConfig(OutputStream out) {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        pw.print(configContent);
        pw.flush();
        pw.close();
    }

    /**
     * Retrieves the value of the given JavaScript-Variable and
     * returns it as boolean.
     *
     * @param js  JavaScript-Engine that contains the variable.
     * @param key Name of the variable.
     * @return Value of the variable as a Boolean.
     */
    private static boolean getAsBoolean(ScriptEngine js, String key) {
        Objects.requireNonNull(js);
        Objects.requireNonNull(key);

        Object o = js.get(key);
        if (o instanceof Boolean) {
            return (boolean) o;
        }

        throw new IllegalArgumentException("JavaScript-object '" + key + "' with value '" + o + "' is not a Boolean.");
    }

    /**
     * Retrieves the value of the given JavaScript-Variable and
     * returns it as int.
     * Valid Object types are Integer and Double.
     *
     * @param js  JavaScript-Engine that contains the variable.
     * @param key Name of the variable.
     * @return Value of the variable as an Integer.
     */
    private static int getAsInt(ScriptEngine js, String key) {
        Objects.requireNonNull(js);
        Objects.requireNonNull(key);

        Object o = js.get(key);
        if (o instanceof Integer || o instanceof Double) {
            return (int) o;
        }

        throw new IllegalArgumentException("JavaScript-object '" + key + "' with value '" + o + "' is not a number.");
    }

    /**
     * Retrieves the value of the given JavaScript-Variable and
     * returns it as double.
     * Valid Object types are Integer and Double.
     *
     * @param js  JavaScript-Engine that contains the variable.
     * @param key Name of the variable.
     * @return Value of the variable as double.
     */
    private static double getAsDouble(ScriptEngine js, String key) {
        Objects.requireNonNull(js);
        Objects.requireNonNull(key);

        Object o = js.get(key);
        if (o instanceof Integer) {
            return ((Integer) o).doubleValue();
        }
        if (o instanceof Double) {
            return (double) o;
        }

        throw new IllegalArgumentException("JavaScript-object '" + key + "' with value '" + o + "' is not a number.");
    }

    /**
     * Retrieves the value of the given JavaScript-Variable and
     * returns it as String.
     *
     * @param js  JavaScript-Engine that contains the variable.
     * @param key Name of the variable.
     * @return Value of the variable as String.
     */
    private static String getAsString(ScriptEngine js, String key) {
        Objects.requireNonNull(js);
        Objects.requireNonNull(key);

        Object o = js.get(key);
        if (o instanceof String) {
            return (String) o;
        }

        throw new IllegalArgumentException("JavaScript-object '" + key + "' with value '" + o + "' is not a String.");
    }
}
