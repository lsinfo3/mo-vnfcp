import de.uniwue.VNFP.algo.PSA;
import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.algo.ParetoFrontierAnalysis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TestRankings {
	public static void main(String[] args) throws Exception {
		Locale.setDefault(Locale.US);

		ParetoFrontier frontier = PSA.runPSA(args[0]);
		System.out.println();
		System.out.println("Computed " + frontier.size() + " Pareto optimal points. Generating plots...");

		Process process = Runtime.getRuntime().exec("R -e source('~/w/old/ma/R2/plotRankings.R')");
		int exitCode = process.waitFor();
		System.out.println("Done. (ExitCode " + exitCode + ")");
	}

	public static void main2(String[] args) throws Exception {
		for (int i = 0; i < 50; i++) {
			main2(args);
			Path p = Paths.get("/tmp/psa/allPlots.pdf");
			Files.move(p, p.resolveSibling("allPlots." + i + ".pdf"), REPLACE_EXISTING);
		}
	}
}
