/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import com.google.gson.annotations.Expose;

public class ResponseMessageObject {
	@Expose
	public String account ;
	@Expose
	public String amount;
	@Expose
	public int statusCode;

}
