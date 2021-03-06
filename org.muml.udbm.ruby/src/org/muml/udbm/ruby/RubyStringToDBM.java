/*
 * generated by Fujaba - CodeGen2
 */
package org.muml.udbm.ruby;

import java.util.HashMap;
import java.util.HashSet;

import org.muml.udbm.ClockConstraint;
import org.muml.udbm.ClockZone;
import org.muml.udbm.DifferenceClockConstraint;
import org.muml.udbm.SimpleClockConstraint;
import org.muml.udbm.UDBMClock;
import org.muml.udbm.clockconstraint.FalseClockConstraint;
import org.muml.udbm.clockconstraint.RelationalOperator;
import org.muml.udbm.clockconstraint.TrueClockConstraint;


public class RubyStringToDBM extends RubyStringToFed{

	/**
	 * Parses the result of the ruby server back into a federation. The string encodes
	 * the n+1 * n+1 matrix representing the federation, where x0 is contained in line 0 
	 * and column 0. In the string, the zones are separated by "|" characters, the lines
	 * of the matrix of one zone are separated by "&". The entries of the lines are
	 * separated by "\t".
	 * 
	 * Rows of the matrix contain lower bound, columns upper bounds of differences between 
	 * the clocks. The minuend is the clock in the row, the subtrahend is the clock in the
	 * column, e.g. x1 - x0 <= 20 => row 0, column 1 contains upper bound of the difference
	 * 
	 * @param updatedRubyFederation
	 */
	public void updateFromRubyFederationString (RubyFederation fed , String updatedRubyFederation )
	{
		String[] zonesArr = updatedRubyFederation.split("\\|");
		fed.removeAllFromClockZone();

		if (updatedRubyFederation.equals("false")) {
			HashSet<ClockConstraint> singleFalseConstraint = new HashSet<ClockConstraint>(1);
			singleFalseConstraint.add(new FalseClockConstraint());
			fed.addToClockZone(new ClockZone(singleFalseConstraint));
		} else {
			for (int i = 0; i < zonesArr.length; i++) {
				String[] linesArr = zonesArr[i].substring(1,
						zonesArr[i].length() - 1).split("&");

				HashSet<ClockConstraint> constraints = new HashSet<ClockConstraint>();
				HashMap<UDBMClock, SimpleClockConstraint> lBounds = new HashMap<UDBMClock, SimpleClockConstraint>();

				// parse lower bounds of clocks, are contained in the first row
				//as only <= and < are used in the matrix, constraint is implicitly
				//multiplied by (-1).
				String[] curRow = linesArr[0].split("\\t");
				for(int j = 1; j < curRow.length; j++){
					RelationalOperator op;
					curRow[j] = curRow[j].trim();
					if (curRow[j].charAt(1) == '='){
						op = RelationalOperator.GreaterOrEqualOperator;
					} else{
						op = RelationalOperator.GreaterOperator;
					}
					String val = curRow[j].substring(op.toString().length());
					lBounds.put(fed.orderedClocks.get(j-1), new SimpleClockConstraint(fed.orderedClocks.get(j-1), op, -1 * new Integer(val)));
				}

				//parse remaining rows
				for (int j = 1; j < linesArr.length; j++) {

					if (linesArr[j].equals("true")) {
						constraints.add(new TrueClockConstraint());
					} else {


						curRow = linesArr[j].split("\\t");
						//parse each line, entry 0 contains upper bound for clock,
						//remaining entries contain differences, entry k of line j
						//corresponds to difference clock[j] - clock[k] <= or < bound.
						//skip entries on the diagonal axis
						for(int k = 0; k < curRow.length; k++){
							if (j == k) continue;

							RelationalOperator op;
							curRow[k] = curRow[k].trim();
							if (curRow[k].charAt(1) == '='){
								op = RelationalOperator.LessOrEqualOperator;
							} else{
								op = RelationalOperator.LessOperator;
							}

							//check if value equal INF => if true, then this bound is unconstrained => don't create a constraint
							int val;
							if (curRow[k].substring(op.toString().length()).equals("INF")){
								//add lb constraint to constraint set, if this was the upper bound of the clock
								if (k == 0){
									constraints.add(lBounds.get(fed.orderedClocks.get(j-1)));
								}
								continue;
							} else {
								val = new Integer(curRow[k].substring(op.toString().length()));
							}

							if (k == 0){
								//get the constraint for the lower bound,
								SimpleClockConstraint lb = lBounds.get(fed.orderedClocks.get(j-1));
								if (lb.getValue() == val && op == RelationalOperator.LessOrEqualOperator){
									//lb has the same value, combine lb and ub to equals
									constraints.add(new SimpleClockConstraint(fed.orderedClocks.get(j-1), RelationalOperator.EqualOperator, val));
								} else {
									//add lb to constraints and create additional constraint for ub
									constraints.add(lb);
									constraints.add(new SimpleClockConstraint(fed.orderedClocks.get(j-1), op, val));
								}
							} else {
								//create difference constraint
								constraints.add(new DifferenceClockConstraint(fed.orderedClocks.get(j-1), fed.orderedClocks.get(k-1), op, val));
							} //-- end if (k == 0)		
						} //-- end iteration of row entries	
					} // -- end else
				} // -- end iteration of rows

				fed.addToClockZone(new ClockZone(constraints));
			} // -- end iteration of zones
		} // -- end else
	}

}

