/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.apache.http.HttpResponse;



/**
 * Servlet implementation class SimpleServlet
 */

public class SimbankServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SimbankServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		RequestDispatcher rd = request.getRequestDispatcher("/index.html");

		rd.include(request, response);


	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
//		Inserting parameters in SOAP requester
			String xml = "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>"+
					"<soapenv:Body>"+
					"<ns1:UPDACCTOperation xmlns:ns1='http://www.UPDACCT.STCUSTN2.Request.com'>"+
					"<ns1:update_account_record>"+
					"<ns1:account_key>"+
					"<ns1:sort_code>00-00-00</ns1:sort_code>"+
					"<ns1:account_number>"+request.getParameter("accnr")+"</ns1:account_number>"+
					"</ns1:account_key>"+
					"<ns1:account_change>"+request.getParameter("amount")+"</ns1:account_change>"+
					"</ns1:update_account_record></ns1:UPDACCTOperation>"+
					"</soapenv:Body>"+
					"</soapenv:Envelope>"+"\n";

			try {
//		Passing parameters
				Request post = Request.Post("http://host.docker.internal:2080/updateAccount").bodyString(xml, ContentType.APPLICATION_XML);
				Response resp = post.execute();
				HttpResponse simbankResp = resp.returnResponse();
				int statusCode = simbankResp.getStatusLine().getStatusCode();
				response.reset();
				response.setStatus(statusCode);
				ResponseMessageObject obj = new ResponseMessageObject();
				obj.account =request.getParameter("accnr");
				obj.amount=request.getParameter("amount");
				obj.statusCode = statusCode;
				Gson status = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				response.setContentType( "application/json");
			    out.println( status.toJson( obj));

			}catch(Exception e) {
				throw new ServletException("Servlet error",e);
			}


	}




}
