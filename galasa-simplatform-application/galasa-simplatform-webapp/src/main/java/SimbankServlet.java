import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;

import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;





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
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/index.html");
		rd.include(request, response);


	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();

		String output = "";
//		validation checks
		if(!request.getParameter("accnr").matches("[0-9]+") ||request.getParameter("accnr").length()!=9){
			output = "<p id='output'>Invalid account number</p>";
			response.reset();
			response.setStatus(400);
			out.print(output);
			out.flush();
		}else if(!request.getParameter("amount").matches("[0-9]+") ) {
			output = "<p id='output'>Invalid amount</p>";
			response.reset();
			response.setStatus(400);
			out.print(output);
			out.flush();
		}else {
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
				
//		Checking status
				if(statusCode != 200) {
					output = "<p id='output'>Transaction failed</p>";

				}else {
					output = "<p id='output'>Transaction complete</p>";
				}

				out.print(output);
				out.flush();

			}catch(Exception e) {
				e.printStackTrace();
			}
		}


	}




}
