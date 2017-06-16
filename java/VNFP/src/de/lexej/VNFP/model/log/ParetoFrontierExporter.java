package de.lexej.VNFP.model.log;

import de.lexej.VNFP.algo.ParetoFrontier;
import de.lexej.VNFP.model.solution.Solution;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * This logger prints details of every solution in the final
 * pareto frontier into a given writer as CSV.
 * The writer will be closed afterwards.
 *
 * @author alex
 */
public class ParetoFrontierExporter implements PSAEventLogger {
    private Writer w;

    /**
     * Initializes a new instance of this logger.
     * All output will be written into the given writer.
     * The writer will be closed afterwards.
     *
     * @param w Logging destination.
     */
    public ParetoFrontierExporter(Writer w) {
        this.w = Objects.requireNonNull(w);
    }

    @Override
    public void psaEnd(ParetoFrontier paretoFrontier) {
        try {
            w.write(paretoFrontier.get(0).toStringCsv()[0]);
            for (Solution s : paretoFrontier) {
                w.write("\n");
                w.write(s.toStringCsv()[1]);
            }
            w.write("\n");
            w.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
