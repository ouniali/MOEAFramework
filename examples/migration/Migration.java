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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.util.Vector;
import org.moeaframework.util.io.CommentedLineReader;

/**
 * Multiobjective 0/1 software migration problem. Problem instances are loaded from files
 * in the format defined by Eckart Zitzler and Marco Laumanns at <a href=
 * "http://www.tik.ee.ethz.ch/sop/download/supplementary/testProblemSuite/">
 * http://www.tik.ee.ethz.ch/sop/download/supplementary/testProblemSuite/</a>.
 */
public class Migration implements Problem {

    /**
     * The number of versions.
     */
    protected int nversions;

    /**
     * The number of functionalities.
     */
    protected int nfunctionalities;

    /**
     * Entry {@code bvalue[i][j]} is the profit from including functionality {@code j}
     * in version {@code i}.
     */
    protected int[][] bvalue;

    /**
     * Entry {@code cost[i][j]} is the business value incurred from including functionality
     * {@code j} in version {@code i}.
     */
    protected int[][] cost;

    /**
     * Entry {@code budget[i]} is the maximum budget allocated for version {@code i}.
     */
    protected int[] budget;

    /**
     * Constructs a multiobjective 0/1 migration problem instance loaded from
     * the specified file.
     *
     * @param file the file containing the software migration problem instance
     * @throws IOException if an I/O error occurred
     */
    public Migration(File file) throws IOException {
        this(new FileReader(file));
    }

    /**
     * Constructs a multiobjective 0/1 software migration problem instance loaded from
     * the specified input stream.
     *
     * @param inputStream the input stream containing the knapsack problem
     *        instance
     * @throws IOException if an I/O error occurred
     */
    public Migration(InputStream inputStream) throws IOException {
        this(new InputStreamReader(inputStream));
    }

    /**
     * Constructs a multiobjective 0/1 software migration problem instance loaded from
     * the specified reader.
     *
     * @param reader the reader containing the software migration problem instance
     * @throws IOException if an I/O error occurred
     */
    public Migration(Reader reader) throws IOException {
        super();

        load(reader);
    }

    /**
     * Loads the Software migration problem instance from the specified reader.
     *
     * @param reader the file containing the knapsack problem instance
     * @throws IOException if an I/O error occurred
     */
    private void load(Reader reader) throws IOException {
        Pattern specificationLine = Pattern.compile("Software migration problem specification \\((\\d+) versions, (\\d+) functionalities\\)");
        Pattern capacityLine = Pattern.compile(" budget: \\+(\\d+)");
        Pattern weightLine = Pattern.compile("  cost: \\+(\\d+)");
        Pattern profitLine = Pattern.compile("  bvalue: \\+(\\d+)");

        CommentedLineReader lineReader = null;
        String line = null;
        Matcher matcher = null;

        try {
            lineReader = new CommentedLineReader(reader);
            line = lineReader.readLine(); // the problem specification line
            matcher = specificationLine.matcher(line);

            if (matcher.matches()) {
                nversions = Integer.parseInt(matcher.group(1));
                nfunctionalities = Integer.parseInt(matcher.group(2));
            } else {
                throw new IOException("Software migration data file not properly formatted: invalid specification line");
            }

            budget = new int[nversions];
            bvalue = new int[nversions][nfunctionalities];
            cost = new int[nversions][nfunctionalities];

            for (int i = 0; i < nversions; i++) {
                line = lineReader.readLine(); // line containing "="
                line = lineReader.readLine(); // line containing "Version Demo/payed:"
                line = lineReader.readLine(); // the version budget
                matcher = capacityLine.matcher(line);

                if (matcher.matches()) {
                    budget[i] = Integer.parseInt(matcher.group(1));
                } else {
                    throw new IOException("Software migration data file not properly formatted: invalid capacity line");
                }

                for (int j = 0; j < nfunctionalities; j++) {
                    line = lineReader.readLine(); // line containing "functionality j:"
                    line = lineReader.readLine(); // the functionality cost
                    matcher = weightLine.matcher(line);

                    if (matcher.matches()) {
                        cost[i][j] = Integer.parseInt(matcher.group(1));
                    } else {
                        throw new IOException("Software migration data file not properly formatted: invalid weight line");
                    }

                    line = lineReader.readLine(); // the functionality bvalue (business value)
                    matcher = profitLine.matcher(line);

                    if (matcher.matches()) {
                        bvalue[i][j] = Integer.parseInt(matcher.group(1));
                    } else {
                        throw new IOException("Software migration data file not properly formatted: invalid profit line");
                    }
                }
            }
        } finally {
            if (lineReader != null) {
                lineReader.close();
            }
        }
    }

    @Override
    public void evaluate(Solution solution) {
        boolean[] d = EncodingUtils.getBinary(solution.getVariable(0));
        double[] f = new double[nversions];
        double[] g = new double[nversions];

        // calculate the business values and costs for the versions
        for (int i = 0; i < nfunctionalities; i++) {
            if (d[i]) {
                for (int j = 0; j < nversions; j++) {
                    f[j] += bvalue[j][i];
                    g[j] += cost[j][i];
                }
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

        // negate the objectives since software migration problem is maximization
        solution.setObjectives(Vector.negate(f));
        solution.setConstraints(g);
    }

    @Override
    public String getName() {
        return "Migration";
    }

    @Override
    public int getNumberOfConstraints() {
        return nversions;
    }

    @Override
    public int getNumberOfObjectives() {
        return nversions;
    }

    @Override
    public int getNumberOfVariables() {
        return 1;
    }

    @Override
    public Solution newSolution() {
        Solution solution = new Solution(1, nversions, nversions);
        solution.setVariable(0, EncodingUtils.newBinary(nfunctionalities));
        return solution;
    }

    @Override
    public void close() {
        //do nothing
    }

}
