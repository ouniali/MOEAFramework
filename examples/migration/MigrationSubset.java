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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.util.Vector;

/**
 * Variant of the software migration problem using the subset encoding.
 */
public class MigrationSubset extends Migration {


    public MigrationSubset(File file) throws IOException {
        super(file);
    }


    public MigrationSubset(InputStream inputStream) throws IOException {
        super(inputStream);
    }


    public MigrationSubset(Reader reader) throws IOException {
        super(reader);
    }

    @Override
    public void evaluate(Solution solution) {
        int[] items = EncodingUtils.getSubset(solution.getVariable(0));
        double[] f = new double[nversions];
        double[] g = new double[nversions];

        // calculate the business values and costs for the versions
        for (int i = 0; i < items.length; i++) {
            for (int j = 0; j < nversions; j++) {
                f[j] += bvalue[j][items[i]];
                g[j] += cost[j][items[i]];
            }
        }

        // check if any costs exceed the budget
        for (int j = 0; j < nversions; j++) {
            if (g[j] <= budget[j]) {
                g[j] = 0.0;
            } else {
                g[j] = g[j] - budget[j];
            }
        }

        // negate the objectives since software migration is maximization
        solution.setObjectives(Vector.negate(f));
        solution.setConstraints(g);
    }

    @Override
    public String getName() {
        return "KnapsackSubset";
    }

    @Override
    public Solution newSolution() {
        Solution solution = new Solution(1, nversions, nversions);
        solution.setVariable(0, EncodingUtils.newSubset(0, nfunctionalities, nfunctionalities));
        return solution;
    }

}
