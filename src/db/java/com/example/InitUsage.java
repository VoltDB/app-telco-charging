/**
 * 
 */
package com.example;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * @author seetasomagani
 *
 */
public class InitUsage extends VoltProcedure {

	private static final SQLStmt ADD_PRODUCT = new SQLStmt("INSERT INTO USAGE VALUES (?, ?, 0)");

	public VoltTable[] run(long userid, int productCount) {

		int minProductId = 0;
		int maxProductId = productCount;

		for(long productId = minProductId; productId <= maxProductId; productId++) {
			voltQueueSQL(ADD_PRODUCT, userid, productId);
		}
		return voltExecuteSQL(true);
	}
}
