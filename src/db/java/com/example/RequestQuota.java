package com.example;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2018 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class RequestQuota extends VoltProcedure {

    public static final SQLStmt getUser = new SQLStmt("SELECT userid FROM USER WHERE userid = ?;");
    public static final SQLStmt getBalance = new SQLStmt("SELECT balance FROM BALANCE WHERE userid = ?;");
    public static final SQLStmt getProduct = 
    		new SQLStmt("SELECT unit_cost FROM PRODUCT WHERE productid = ?;");
    public static final SQLStmt getCurrentAllocation = 
    		new SQLStmt("SELECT allocated_units FROM USAGE WHERE userid = ? AND productid = ?;");
    public static final SQLStmt getRemainingCredit
        = new SQLStmt("select v.userid, v.balance - sum(u.allocated_units * p.unit_cost )  balance "
                   + "from  BALANCE v " 
                   + ", USAGE u "
                   + ", PRODUCT p "
                   + "where v.userid = ? "
                   + "and   v.userid = u.userid "
                   + "and   p.productid = u.productid "
                   + "group by v.userid, v.balance;");
    
    public static final SQLStmt createAllocation = 
    		new SQLStmt("INSERT INTO USAGE (userid, productid, allocated_units) VALUES (?,?,?);");
    public static final SQLStmt updateAllocation = 
    		new SQLStmt("UPDATE USAGE SET allocated_units = ? WHERE userid = ? AND productid = ?");
       
    public VoltTable[] run(long userId, long productId, int unitsWanted) throws VoltAbortException {

        long currentBalance = 0;
        long unitCost = 0;
        long allocatedAlready = -1;
 
        voltQueueSQL(getUser, userId);
        voltQueueSQL(getBalance, userId);
        voltQueueSQL(getProduct, productId);
        voltQueueSQL(getCurrentAllocation, userId, productId);
        voltQueueSQL(getRemainingCredit, userId);

        VoltTable[] results = voltExecuteSQL();

        if (!results[0].advanceRow()) {
            throw new VoltAbortException("User " + userId + " does not exist");
        }

        if (!results[1].advanceRow()) {
            throw new VoltAbortException("User " + userId + " exists but has no financial history...");
        } else {
            currentBalance = results[1].getLong("BALANCE");
        }

        if (!results[2].advanceRow()) {
            throw new VoltAbortException("Product " + productId + " does not exist");
        } else {
            unitCost = results[2].getLong("UNIT_COST");
        }

        if (results[3].advanceRow()) {
            allocatedAlready = results[3].getLong("allocated_units");
        }

        if (results[4].advanceRow()) {
            // We'll only have a row if we have active reservations...
            currentBalance = results[4].getLong("BALANCE");
        }

         
        long wantToSpend = unitCost * unitsWanted;
        String status = "";
        
        if (wantToSpend > currentBalance ) {
            status = "Not enough money. wantToSpend : currentBalance " + wantToSpend + " : " + currentBalance;
            this.setAppStatusCode(ReferenceData.STATUS_NO_MONEY);
        } else {
            status = "Allocated " + unitsWanted + " units";
            this.setAppStatusCode(ReferenceData.STATUS_UNITS_ALLOCATED);
            
            if (allocatedAlready >= 0 ) {
                // We already have an allocation
                voltQueueSQL(updateAllocation, unitsWanted + allocatedAlready, userId, productId);
            } else {
                voltQueueSQL(createAllocation, userId, productId, unitsWanted);
            }
        }
        
        this.setAppStatusString(status);
        return voltExecuteSQL(true);
    }
}
