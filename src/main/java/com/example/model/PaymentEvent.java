/**
 * 
 */
package com.example.model;

/**
 * @author seetasomagani
 *
 */
public class PaymentEvent {

	public static final String SEP = ":";
	public final static String USERID = "userid";
	public final static String AMOUNT = "amount";
	
	public final long userid, amount;
	
	public PaymentEvent(long userid, long amount) {
		this.userid = userid;
		this.amount = amount;
	}
	
	public String toString() {
		return USERID + SEP + userid + SEP + AMOUNT + SEP + amount;
	}
}
