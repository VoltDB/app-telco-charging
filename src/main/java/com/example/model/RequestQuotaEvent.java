/**
 * 
 */
package com.example.model;

/**
 * @author seetasomagani
 *
 */
public class RequestQuotaEvent {

	public static final String SEP = ":";
	public final static String USERID = "userid";
	public final static String PRODUCTID = "productid";
	public final static String UNITS = "units";
	
	public final long userid, productid;
	public final int unitsWanted;
	
	public RequestQuotaEvent(long userid, long productid, int unitsWanted) {
		this.userid = userid;
		this.productid = productid;
		this.unitsWanted = unitsWanted;
	}
	
	public String toString() {
		return USERID + SEP + userid + SEP + PRODUCTID + SEP + productid + SEP + UNITS + unitsWanted;
	}
}
