/* Copyright 2009-2012 David Hadka
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
package org.moeaframework.core.variable;

import java.util.BitSet;

import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;

/**
 * Helper methods for working with various decision variable types and
 * encodings.  As these methods may internally perform type conversions, it is
 * strongly recommended that these methods are used consistently to access
 * decision variables.  For example, whenever you create a decision variable
 * with {@link #newInt(int, int)}, always set the value with
 * {@link #setInt(Variable, int)} and read the value with
 * {@link #asInt(Variable)}.
 * <p>
 * This class also provides methods for converting between {@link RealVariable}
 * and {@link BinaryVariable} in both binary and gray code formats.
 */
public class EncodingUtils {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private EncodingUtils() {
		super();
	}

	/**
	 * Encodes the specified real variable into a binary variable. The number of
	 * bits used in the encoding is {@code binary.getNumberOfBits()}.
	 * 
	 * @param real the real variable
	 * @param binary the binary variable to which the real value is encoded
	 */
	public static void encode(RealVariable real, BinaryVariable binary) {
		int numberOfBits = binary.getNumberOfBits();
		double lowerBound = real.getLowerBound();
		double upperBound = real.getUpperBound();

		double value = real.getValue();
		double scale = (value - lowerBound) / (upperBound - lowerBound);
		long index = Math.round(scale * ((1L << numberOfBits) - 1));

		encode(index, binary);
	}

	/**
	 * Decodes the specified binary variable into its real value.
	 * 
	 * @param binary the binary variable
	 * @param real the real variable to which the value is decoded
	 */
	public static void decode(BinaryVariable binary, RealVariable real) {
		int numberOfBits = binary.getNumberOfBits();
		double lowerBound = real.getLowerBound();
		double upperBound = real.getUpperBound();

		long index = decode(binary);
		double scale = index / (double)((1L << numberOfBits) - 1);
		double value = lowerBound + (upperBound - lowerBound) * scale;

		real.setValue(value);
	}

	/**
	 * Encodes the integer into the specified binary variable. The number of
	 * bits used in the encoding is {@code binary.getNumberOfBits()}.
	 * 
	 * @param value an integer
	 * @param binary the binary variable to which the value is encoded
	 */
	public static void encode(long value, BinaryVariable binary) {
		int numberOfBits = binary.getNumberOfBits();

		if (value < 0) {
			throw new IllegalArgumentException("negative value");
		}

		if ((numberOfBits < 1) || (numberOfBits > 63)) {
			throw new IllegalArgumentException("invalid number of bits");
		}

		if ((1L << numberOfBits) <= value) {
			throw new IllegalArgumentException(
					"number of bits not sufficient to represent value");
		}

		for (int i = 0; i < numberOfBits; i++) {
			binary.set(i, (value & (1L << i)) != 0);
		}
	}

	/**
	 * Decodes the specified binary variable into its integer value.
	 * 
	 * @param binary the binary variable
	 * @return the integer value of the specified binary variable
	 */
	public static long decode(BinaryVariable binary) {
		int numberOfBits = binary.getNumberOfBits();

		if ((numberOfBits < 1) || (numberOfBits > 63)) {
			throw new IllegalArgumentException("invalid number of bits");
		}

		long value = 0;

		for (int i = 0; i < numberOfBits; i++) {
			if (binary.get(i)) {
				value |= (1L << i);
			}
		}

		return value;
	}

	/**
	 * Converts a binary variable from a binary encoding to gray encoding. The
	 * gray encoding ensures two adjacent values have binary representations
	 * differing in only {@code 1} bit (a Hamming distance of {@code 1}).
	 * 
	 * @param variable the variable to be converted
	 */
	public static void binaryToGray(BinaryVariable variable) {
		int n = variable.getNumberOfBits();

		BitSet binary = variable.getBitSet();

		variable.set(n - 1, binary.get(n - 1));
		for (int i = n - 2; i >= 0; i--) {
			variable.set(i, binary.get(i + 1) ^ binary.get(i));
		}
	}

	/**
	 * Converts a binary variable from a gray encoding to binary encoding.
	 * 
	 * @param variable the variable to be converted
	 */
	public static void grayToBinary(BinaryVariable variable) {
		int n = variable.getNumberOfBits();

		BitSet gray = variable.getBitSet();

		variable.set(n - 1, gray.get(n - 1));
		for (int i = n - 2; i >= 0; i--) {
			variable.set(i, variable.get(i + 1) ^ gray.get(i));
		}
	}
	
	/**
	 * Returns a new floating-point decision variable bounded within the
	 * specified range.
	 * 
	 * @param lowerBound the lower bound of the floating-point value
	 * @param upperBound the upper bound of the floating-point value
	 * @return a new floating-point decision variable bounded within the
	 *         specified range
	 */
	public static RealVariable newReal(double lowerBound, double upperBound) {
		return new RealVariable(lowerBound, upperBound);
	}
	
	/**
	 * Returns a new integer-valued decision variable bounded within the
	 * specified range.
	 * 
	 * @param lowerBound the lower bound of the integer value
	 * @param upperBound the upper bound of the integer value
	 * @return a new integer-valued decision variable bounded within the
	 *         specified range
	 */
	public static RealVariable newInt(int lowerBound, int upperBound) {
		return new RealVariable(lowerBound, Math.nextAfter(
				(double)(upperBound+1), Double.NEGATIVE_INFINITY));
	}
	
	/**
	 * Returns a new boolean decision variable.
	 * 
	 * @return a new boolean decision variable
	 */
	public static BinaryVariable newBoolean() {
		return new BinaryVariable(1);
	}
	
	/**
	 * Returns a new binary decision variable with the specified number of bits.
	 * 
	 * @param length the number of bits in the binary decision variable
	 * @return a new binary decision variable with the specified number of bits
	 */
	public static BinaryVariable newBinary(int length) {
		return new BinaryVariable(length);
	}
	
	/**
	 * Returns a new permutation with the specified number of items.
	 * 
	 * @param length the number of items in the permutation
	 * @return a new permutation with the specified number of items
	 */
	public static Permutation newPermutation(int length) {
		return new Permutation(length);
	}
	
	/**
	 * Returns the value stored in a floating-point decision variable.
	 * 
	 * @param variable the decision variable
	 * @return the value stored in a floating-point decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link RealVariable}
	 */
	public static double asReal(Variable variable) {
		if (variable instanceof RealVariable) {
			return ((RealVariable)variable).getValue();
		} else {
			throw new IllegalArgumentException("not a real variable");
		}
	}
	
	/**
	 * Returns the value stored in an integer-valued decision variable.
	 * 
	 * @param variable the decision variable
	 * @return the value stored in an integer-valued decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link RealVariable}
	 */
	public static int asInt(Variable variable) {
		return (int)Math.floor(asReal(variable));
	}
	
	/**
	 * Returns the value stored in a binary decision variable as a
	 * {@link BitSet}.
	 * 
	 * @param variable the decision variable
	 * @return the value stored in a binary decision variable as a
	 *         {@code BitSet}
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link BinaryVariable}
	 */
	public static BitSet asBitSet(Variable variable) {
		if (variable instanceof BinaryVariable) {
			return ((BinaryVariable)variable).getBitSet();
		} else {
			throw new IllegalArgumentException("not a binary variable");
		}
	}
	
	/**
	 * Returns the value stored in a binary decision variable as a boolean
	 * array.
	 * 
	 * @param variable the decision variable
	 * @return the value stored in a binary decision variable as a boolean
	 *         array
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link BinaryVariable}
	 */
	public static boolean[] asBinary(Variable variable) {
		if (variable instanceof BinaryVariable) {
			BinaryVariable binaryVariable = (BinaryVariable)variable;
			boolean[] result = new boolean[binaryVariable.getNumberOfBits()];
			
			for (int i=0; i<binaryVariable.getNumberOfBits(); i++) {
				result[i] = binaryVariable.get(i);
			}
			
			return result;
		} else {
			throw new IllegalArgumentException("not a binary variable");
		}
	}
	
	/**
	 * Returns the value stored in a boolean decision variable.
	 * 
	 * @param variable the decision variable
	 * @return the value stored in a boolean decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link BinaryVariable}
	 */
	public static boolean asBoolean(Variable variable) {
		boolean[] values = asBinary(variable);
		
		if (values.length == 1) {
			return values[0];
		} else {
			throw new IllegalArgumentException("not a boolean variable");
		}
	}
	
	/**
	 * Returns the value stored in a permutation decision variable.
	 * 
	 * @param variable the decision variable
	 * @return the value stored in a permutation decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link Permutation}
	 */
	public static int[] asPermutation(Variable variable) {
		if (variable instanceof Permutation) {
			return ((Permutation)variable).toArray();
		} else {
			throw new IllegalArgumentException("not a permutation");
		}
	}
	
	/**
	 * Returns the array of floating-point decision variables stored in a
	 * solution.  The solution must contain only floating-point decision
	 * variables.
	 * 
	 * @param solution the solution
	 * @return the array of floating-point decision variables stored in a
	 *          solution
	 * @throws IllegalArgumentException if any decision variable contained in
	 *         the solution is not of type {@link RealVariable}
	 */
	public static double[] asReal(Solution solution) {
		return asReal(solution, 0, solution.getNumberOfVariables());
	}
	
	/**
	 * Returns the array of floating-point decision variables stored in a
	 * solution between the specified indices.  The decision variables located
	 * between the start and end index must all be floating-point decision
	 * variables.
	 * 
	 * @param solution the solution
	 * @param startIndex the start index (inclusive)
	 * @param endIndex the end index (exclusive)
	 * @return the array of floating-point decision variables stored in a
	 *         solution between the specified indices
	 * @throws IllegalArgumentException if any decision variable contained in
	 *         the solution between the start and end index is not of type
	 *         {@link RealVariable}
	 */
	public static double[] asReal(Solution solution, int startIndex,
			int endIndex) {
		double[] result = new double[endIndex - startIndex];
		
		for (int i=startIndex; i<endIndex; i++) {
			result[i-startIndex] = asReal(solution.getVariable(i));
		}
		
		return result;
	}
	
	/**
	 * Returns the array of integer-valued decision variables stored in a
	 * solution.  The solution must contain only integer-valued decision
	 * variables.
	 * 
	 * @param solution the solution
	 * @return the array of integer-valued decision variables stored in a
	 *          solution
	 * @throws IllegalArgumentException if any decision variable contained in
	 *         the solution is not of type {@link RealVariable}
	 */
	public static int[] asInt(Solution solution) {
		return asInt(solution, 0, solution.getNumberOfVariables());
	}
	
	/**
	 * Returns the array of integer-valued decision variables stored in a
	 * solution between the specified indices.  The decision variables located
	 * between the start and end index must all be integer-valued decision
	 * variables.
	 * 
	 * @param solution the solution
	 * @param startIndex the start index (inclusive)
	 * @param endIndex the end index (exclusive)
	 * @return the array of integer-valued decision variables stored in a
	 *         solution between the specified indices
	 * @throws IllegalArgumentException if any decision variable contained in
	 *         the solution between the start and end index is not of type
	 *         {@link RealVariable}
	 */
	public static int[] asInt(Solution solution, int startIndex, int endIndex) {
		int[] result = new int[endIndex - startIndex];
		
		for (int i=startIndex; i<endIndex; i++) {
			result[i-startIndex] = asInt(solution.getVariable(i));
		}
		
		return result;
	}
	
	/**
	 * Sets the value of a floating-point decision variable.
	 * 
	 * @param variable the decision variable
	 * @param value the value to assign the floating-point decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link RealVariable}
	 */
	public static void setReal(Variable variable, double value) {
		if (variable instanceof RealVariable) {
			((RealVariable)variable).setValue(value);
		} else {
			throw new IllegalArgumentException("not a real variable");
		}
	}
	
	/**
	 * Sets the values of all floating-point decision variables stored in the
	 * solution.  The solution must contain only floating-point decision
	 * variables.
	 * 
	 * @param solution the solution
	 * @param values the array of floating-point values to assign the solution
	 * @throws IllegalArgumentException if any decision variable contained in
	 *         the solution is not of type {@link RealVariable}
	 */
	public static void setReal(Solution solution, double[] values) {
		setReal(solution, 0, solution.getNumberOfVariables(), values);
	}
	
	/**
	 * Sets the values of the floating-point decision variables stored in a
	 * solution between the specified indices.  The decision variables located
	 * between the start and end index must all be floating-point decision
	 * variables.
	 * 
	 * @param solution the solution
	 * @param startIndex the start index (inclusive)
	 * @param endIndex the end index (exclusive)
	 * @param values the array of floating-point values to assign the
	 *        decision variables
	 * @throws IllegalArgumentException if any decision variable contained in
	 *         the solution between the start and end index is not of type
	 *         {@link RealVariable}
	 */
	public static void setReal(Solution solution, int startIndex, int endIndex,
			double[] values) {
		for (int i=startIndex; i<endIndex; i++) {
			setReal(solution.getVariable(i), values[i-startIndex]);
		}
	}
	
	/**
	 * Sets the value of an integer-valued decision variable.
	 * 
	 * @param variable the decision variable
	 * @param value the value to assign the integer-valued decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link RealVariable}
	 */
	public static void setInt(Variable variable, int value) {
		setReal(variable, value);
	}
	
	/**
	 * Sets the values of all integer-valued decision variables stored in the
	 * solution.  The solution must contain only integer-valued decision
	 * variables.
	 * 
	 * @param solution the solution
	 * @param values the array of integer values to assign the solution
	 * @throws IllegalArgumentException if any decision variable contained in
	 *         the solution is not of type {@link RealVariable}
	 */
	public static void setInt(Solution solution, int[] values) {
		setInt(solution, 0, solution.getNumberOfVariables(), values);
	}
	
	/**
	 * Sets the values of the integer-valued decision variables stored in a
	 * solution between the specified indices.  The decision variables located
	 * between the start and end index must all be integer-valued decision
	 * variables.
	 * 
	 * @param solution the solution
	 * @param startIndex the start index (inclusive)
	 * @param endIndex the end index (exclusive)
	 * @param values the array of floating-point values to assign the
	 *        decision variables
	 * @throws IllegalArgumentException if any decision variable contained in
	 *         the solution between the start and end index is not of type
	 *         {@link RealVariable}
	 */
	public static void setInt(Solution solution, int startIndex, int endIndex,
			int[] values) {
		for (int i=startIndex; i<endIndex; i++) {
			setInt(solution.getVariable(i), values[i-startIndex]);
		}
	}
	
	/**
	 * Sets the bits in a binary decision variable using the given
	 * {@link BitSet}.
	 * 
	 * @param variable the decision variable
	 * @param bitSet the bits to set in the binary decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link BinaryVariable}
	 */
	public static void setBitSet(Variable variable, BitSet bitSet) {
		if (variable instanceof BinaryVariable) {
			BinaryVariable binaryVariable = (BinaryVariable)variable;
			
			for (int i=0; i<binaryVariable.getNumberOfBits(); i++) {
				binaryVariable.set(i, bitSet.get(i));
			}
		} else {
			throw new IllegalArgumentException("not a binary variable");
		}
	}
	
	/**
	 * Sets the bits in a binary decision variable using the given boolean
	 * array.
	 * 
	 * @param variable the decision variable
	 * @param values the bits to set in the binary decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link BinaryVariable}
	 */
	public static void setBinary(Variable variable, boolean[] values) {
		if (variable instanceof BinaryVariable) {
			BinaryVariable binaryVariable = (BinaryVariable)variable;
			
			if (values.length != binaryVariable.getNumberOfBits()) {
				throw new IllegalArgumentException("must have same number of bits");
			}
			
			for (int i=0; i<binaryVariable.getNumberOfBits(); i++) {
				binaryVariable.set(i, values[i]);
			}
		} else {
			throw new IllegalArgumentException("not a binary variable");
		}
	}
	
	/**
	 * Sets the value of a boolean decision variable.
	 * 
	 * @param variable the decision variable
	 * @param value the value to assign the boolean decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link BinaryVariable}
	 */
	public static void setBoolean(Variable variable, boolean value) {
		setBinary(variable, new boolean[] { value });
	}
	
	/**
	 * Sets the value of a permutation decision variable.
	 * 
	 * @param variable the decision variable
	 * @param values the permutation to assign the permutation decision variable
	 * @throws IllegalArgumentException if the decision variable is not of type
	 *         {@link Permutation}
	 */
	public static void setPermutation(Variable variable, int[] values) {
		if (variable instanceof Permutation) {
			((Permutation)variable).fromArray(values);
		} else {
			throw new IllegalArgumentException("not a permutation");
		}
	}

}
