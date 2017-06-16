package de.lexej.VNFP.model.log;

import de.lexej.VNFP.algo.ParetoFrontier;
import de.lexej.VNFP.model.solution.Solution;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * This logger prints information about the current solution set
 * into the given writer each time the temperature changes.
 * It can be used to observe solution set developement over time.
 * The writer will be closed afterwards.
 *
 * @author alex
 */
public class SolutionSetExporter implements PSAEventLogger {
    private Writer w;

    /**
     * Initializes a new instance of this logger.
     * All output will be written into the given writer.
     * The writer will be closed afterwards.
     *
     * @param w Logging destination.
     */
    public SolutionSetExporter(Writer w) {
        this.w = Objects.requireNonNull(w);
    }

    @Override
    public void beginTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {
        if (temperatureIndex == 0) {
            try {
                w.write("iteration;solutionIndex;");
                w.write(currentAcceptedSolutions[0].toStringCsv()[0]);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            exportSolutionSet(temperatureIndex, currentAcceptedSolutions);
        }
    }

    @Override
    public void endTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {
        exportSolutionSet(temperatureIndex + 1, currentAcceptedSolutions);
    }

    private void exportSolutionSet(int index, Solution[] solutions) {
        try {
            for (int i = 0; i < solutions.length; i++) {
                w.write("\n" + index + ";" + i + ";");
                w.write(solutions[i].toStringCsv()[1]);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void psaEnd(ParetoFrontier paretoFrontier) {
        try {
            w.write("\n");
            w.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
