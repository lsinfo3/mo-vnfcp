package de.uniwue.VNFP.model.log;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.solution.Solution;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * At every temperature change,
 * this logger prints details of every solution in the current
 * pareto frontier into a given writer as CSV.
 * The writer will be closed afterwards.
 *
 * @author alex
 */
public class ParetoFrontierDevelopementObserver implements PSAEventLogger {
    private Writer w;

    /**
     * Initializes a new instance of this logger.
     * All output will be written into the given writer.
     * The writer will be closed afterwards.
     *
     * @param w Logging destination.
     */
    public ParetoFrontierDevelopementObserver(Writer w) {
        this.w = Objects.requireNonNull(w);
    }

    @Override
    public void beginTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {
        if (temperatureIndex == 0) {
            try {
                w.write("iteration;" + currentParetoFrontier.get(0).toStringCsv()[0]);
                exportFrontier(temperatureIndex, currentParetoFrontier);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void endTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {
        exportFrontier(temperatureIndex + 1, currentParetoFrontier);
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

    private void exportFrontier(int temperatureIndex, ParetoFrontier pf) {
        try {
            for (Solution s : pf) {
                w.write("\n");
                w.write(temperatureIndex + ";" + s.toStringCsv()[1]);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
