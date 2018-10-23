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

public class UpsertUser extends VoltProcedure {

    public static final SQLStmt getUser = new SQLStmt("SELECT userid FROM USER WHERE userid = ?;");
    public static final SQLStmt getBalance = new SQLStmt("SELECT balance FROM BALANCE WHERE userid = ?;");
    public static final SQLStmt addUser = new SQLStmt("INSERT INTO USER (userid, user_blob) VALUES (?,?);");
    public static final SQLStmt updateCredit = new SQLStmt("UPDATE BALANCE set balance = ? where userid = ?;");
    public static final SQLStmt addCredit = new SQLStmt("INSERT into BALANCE values (?, ?)");

    public VoltTable[] run(long userId, long addBalance, String isNew, String json, String purpose) throws VoltAbortException {

        long currentBalance = 0;

        voltQueueSQL(getUser, userId);
        voltQueueSQL(getBalance, userId);

        VoltTable[] results = voltExecuteSQL();

        if (isNew.equalsIgnoreCase("Y")) {

            if (results[0].advanceRow()) {
                throw new VoltAbortException("User " + userId + " exists but shouldn't");
            }
            
            currentBalance = addBalance;
            
            final String status = "Created user " + userId + " with opening credit of " + addBalance;
            voltQueueSQL(addUser, userId, json);
            voltQueueSQL(addCredit, userId, addBalance);
            this.setAppStatusCode(ReferenceData.STATUS_OK);
            this.setAppStatusString(status);

        } else {

            if (!results[0].advanceRow()) {
                throw new VoltAbortException("User " + userId + " does not exist");
            }

            if (!results[1].advanceRow()) {
                voltQueueSQL(addCredit, userId, addBalance);
            } else {
            	currentBalance = results[1].getLong("BALANCE") + addBalance;
            	voltQueueSQL(updateCredit, userId, currentBalance);
            }

            final String status ="Updated user " + userId + " - added credit of " + addBalance + "; balance now " + currentBalance;
            this.setAppStatusCode(ReferenceData.STATUS_OK);
            this.setAppStatusString(status);
        }

        return voltExecuteSQL(true);
    }
}
