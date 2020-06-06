/* Copyright 2009-2019 David Hadka
 *
 * This file is part of the MOEA Framework.
 *
 * The MOEA Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * The MOEA Framework is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the MOEA Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package migration;

import java.io.IOException;
import java.io.InputStream;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.util.Vector;

/**
 * Example of binary optimization using the {@link Migration} problem on the
 * {@code software-migration-data} instance.
 */
public class MigrationExample {


    public static void main(String[] args) throws IOException {
        // open the file containing the knapsack problem instance
        InputStream input = Migration.class.getResourceAsStream(
                "software-migration-data.txt");

        if (input == null) {
            System.err.println("Unable to find the file knapsack.100.2");
            System.exit(-1);
        }

        // solve using NSGA-II
        NondominatedPopulation result = new Executor()
                .withProblemClass(Migration.class, input)
                .withAlgorithm("NSGAII")
                .withMaxEvaluations(50000)
                .distributeOnAllCores()
                .run();

        // print the results
        for (int i = 0; i < result.size(); i++) {
            Solution solution = result.get(i);
            double[] objectives = solution.getObjectives();

            // negate objectives to return them to their maximized form
            objectives = Vector.negate(objectives);

            System.out.println("Solution " + (i+1) + ":");
            System.out.println("    Demo version business value: " + objectives[0]);
            System.out.println("    Payed version business value: " + objectives[1]);
            System.out.println("    Binary String: " + solution.getVariable(0));
        }
    }

}
