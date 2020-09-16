/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.simplatform.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.gson.JsonObject;

import dev.galasa.simplatform.main.Simplatform;
import dev.galasa.simplatform.management.facility.BatchJob;
import dev.galasa.simplatform.saf.SecurityAuthorizationFacility;

public class ManagementFacilityListener implements IListener {
	private Socket socket;
	private List<String> headers = new ArrayList<>();
	private List<String> bodyList = new LinkedList<>();

	private Logger log = Logger.getLogger("Simplatform");
	private String path;
	private String authorization;
	private String method;
	private String contentType;
	private String user;

	private static final String CR_LF = "\r\n";
	private static final String HEADER_HTTP_200_OK = "HTTP/1.1 200 OK";
	private static final String HEADER_HTTP_201_CREATED = "HTTP/1.1 201 Created";
	private static final String HEADER_HTTP_400_BAD_REQUEST = "HTTP/1.1 400 Bad Request";
	private static final String HEADER_HTTP_401_UNAUTHORIZED = "HTTP/1.1 401 Unauthorized";
	private static final String HEADER_HTTP_404_NOT_FOUND = "HTTP/1.1 404 Not Found";
	private static final String HEADER_HTTP_500_INTERNAL_SERVER_ERROR = "HTTP/1.1 500 Internal Server Error"; 
	private static final String HEADER_SERVER = "Server: Simplatform  " + Simplatform.getVersion();
	private static final String HEADER_CONNECTION_CLOSE = "Connection: close";
	private static final String HEADER_CONTENT_LENGTH = "Content-Length: ";
	private static final String HEADER_CONTENT_TYPE_TEXT = "Content-Type: text/plain; charset=\"utf-8\"" + CR_LF;
	private static final String HEADER_CONTENT_TYPE_JSON = "Content-Type: application/json; charset=\"utf-8\"" + CR_LF;
	private static Random randomNumber = new Random();
	private static int jobCounter;
	
	private static HashMap<String, BatchJob> batchJobs = new HashMap<>();

	public void run() {
		try {
			if (!processInput()) {
				return;
			}
			path = findPath();
			if (!path.startsWith("/zosmf/")) {
				String message = "Request was not sent to path /zosmf/... Path was '" + path + "'";
				return404(message);
				message = message + " returning 404";
				log.log(Level.WARNING, message);
				return;
			}

			if (authorization == null || !authorization.toLowerCase().startsWith("basic")) {
				log.log(Level.WARNING, "No basic auth header. Returning 401");
				return401();
				return;
			}
			
			// Authenticate the user		
			String base64Credentials = authorization.substring("Basic".length()).trim();
		    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
		    String[] credentials = new String(credDecoded, StandardCharsets.UTF_8).split(":", 2);
		    user = credentials[0];
		    String password = credentials[1];
		    if (!new SecurityAuthorizationFacility().authenticate(user, password)) {
		    	return401();
		    	return;
		    }
			
			if (path.startsWith("/zosmf/restjobs/jobs")) {
				processBatchRequest();
			} else if (path.startsWith("/zosmf/restconsoles/consoles")) {
				processConsoleRequest();
			} else if (path.startsWith("/zosmf/restfiles")) {
				processFileRequest();
			} else {
				String message = "Unimplemented or invalid /zosmf/ Path was " + path;
				return404(message);
				message = message + " returning 404";
				log.log(Level.WARNING, message);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "PROBLEMS!!", e);
			try {
				return500(e);
			} catch (IOException e1) {
				log.log(Level.SEVERE, "PROBLEMS!!", e1);
			}
		}
	}

	private void processBatchRequest() throws IOException {
		String[] tokens = this.path.substring(1).split("/");
		String logNoJobFound = "No job found for reference: '";
		if (getMethod().equals("PUT") && tokens.length == 3) {
			// Batch job submit
			batchJobSubmit();
			return;
		} else if (getMethod().equals("GET")) {
			processBatchGetRequest(tokens);
			return;
        } else if (getMethod().equals("PUT")) {
            BatchJob batchJob = null;
            String key = null;
            if (tokens.length >= 5) {
                key = tokens[3] + "/" + tokens[4];
            }
            batchJob = batchJobs.get(key);
            if (batchJob == null) {
                return404(logNoJobFound + tokens[3] + "(" + tokens[4] + ")'");
                return;
            }
            batchJobCancel(batchJob);
            return;
        } else if (getMethod().equals("DELETE")) {
            BatchJob batchJob = null;
            String key = null;
            if (tokens.length >= 5) {
                key = tokens[3] + "/" + tokens[4];
            }
            batchJob = batchJobs.get(key);
            if (batchJob == null) {
                return404(logNoJobFound + tokens[3] + "(" + tokens[4] + ")'");
                return;
            }
            batchJobPurge(batchJob);
            return;
        }
		// Error case
		return404("Unimplemented or invalid /zosmf/ path " + path);
	}

	private void processBatchGetRequest(String[] tokens) throws IOException {
		BatchJob batchJob = null;
		String key = null;
		if (tokens.length >= 5) {
			key = tokens[3] + "/" + tokens[4];
		}
		batchJob = batchJobs.get(key);
		if (batchJob == null) {
			return404("No job found for reference: '" + tokens[3] + "(" + tokens[4] + ")'");
			return;
		}
		
		if (tokens.length == 5) {
			// Batch job status
			batchJobStatus(batchJob);
		} else if (tokens.length == 6 && tokens[5].equals("files")) {
			// Batch job list files
			batchJobListFiles(batchJob);
		}  else if (tokens.length == 8 && tokens[5].equals("files") && tokens[7].equals("records")) {
			batchJobGetFile(batchJob, tokens[6]);
		}
	}

	private void batchJobSubmit() throws IOException {
		BatchJob batchJob = new BatchJob(String.join("\n", this.bodyList), user, nextJobid());
		if (!batchJob.isSubmitted()) {
			return400(batchJob.getOutput());
			return;
		}
		batchJobs.put(batchJob.getJobname() + "/" + batchJob.getJobid(), batchJob);			
			
		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println(HEADER_HTTP_201_CREATED);
		ps.println(HEADER_SERVER);
		ps.println(HEADER_CONNECTION_CLOSE);
		ps.println(HEADER_CONTENT_LENGTH + batchJob.getOutput().length());
		ps.println(HEADER_CONTENT_TYPE_JSON);
		ps.println(batchJob.getOutput());
		ps.println(CR_LF);
		ps.flush();
		socket.close();
	}

	private static String nextJobid() {
		if (jobCounter == 0 || jobCounter == 99999) {
			jobCounter = randomNumber.nextInt(99999);
		}
		return "JOB" + StringUtils.leftPad(Integer.toString(jobCounter++), 5, "0");
	}

	private void batchJobStatus(BatchJob batchJob) throws IOException {
		batchJob.refreshJobStatus();
		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println(HEADER_HTTP_200_OK);
		ps.println(HEADER_SERVER);
		ps.println(HEADER_CONNECTION_CLOSE);
		ps.println(HEADER_CONTENT_LENGTH + batchJob.getOutput().length());
		ps.println(HEADER_CONTENT_TYPE_JSON);
		ps.println(batchJob.getOutput());
		ps.println(CR_LF);
		ps.flush();
		socket.close();
	}

	private void batchJobListFiles(BatchJob batchJob) throws IOException {
		batchJob.listFiles();
		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println(HEADER_HTTP_200_OK);
		ps.println(HEADER_SERVER);
		ps.println(HEADER_CONNECTION_CLOSE);
		ps.println(HEADER_CONTENT_LENGTH + batchJob.getOutput().length());
		ps.println(HEADER_CONTENT_TYPE_JSON);
		ps.println(batchJob.getOutput());
		ps.println(CR_LF);
		ps.flush();
		socket.close();
	}
	
	private void batchJobGetFile(BatchJob batchJob, String token) throws IOException {
		String fileContenet = batchJob.getFile(token);
		if (fileContenet != null) {
			OutputStream output = socket.getOutputStream();
			PrintStream ps = new PrintStream(output);
			ps.println(HEADER_HTTP_200_OK);
			ps.println(HEADER_SERVER);
			ps.println(HEADER_CONNECTION_CLOSE);
			ps.println(HEADER_CONTENT_LENGTH + fileContenet.length());
			ps.println(HEADER_CONTENT_TYPE_TEXT);
			ps.println(fileContenet);
			ps.println(CR_LF);
			ps.flush();
			socket.close();
			return;
		}
		return404("Job '" + batchJob.getJobname() + "(" + batchJob.getJobid() + ")' does not contain spool file id " + token);
	}

    private void batchJobCancel(BatchJob batchJob) throws IOException {
        batchJob.cancel();
        OutputStream output = socket.getOutputStream();
        PrintStream ps = new PrintStream(output);
        ps.println(HEADER_HTTP_200_OK);
        ps.println(HEADER_SERVER);
        ps.println(HEADER_CONNECTION_CLOSE);
        ps.println(HEADER_CONTENT_LENGTH + batchJob.getOutput().length());
        ps.println(HEADER_CONTENT_TYPE_JSON);
        ps.println(batchJob.getOutput());
        ps.println(CR_LF);
        ps.flush();
        socket.close();
    }

    private void batchJobPurge(BatchJob batchJob) throws IOException {
        batchJobs.remove(batchJob.getJobname() + "/" + batchJob.getJobid());
        batchJobCancel(batchJob);
    }

	private void processConsoleRequest() throws IOException {
		log.log(Level.WARNING, "Console manager feature not implemented returning 404");
		return404("Console manager feature not implemented");
	}

	private void processFileRequest() throws IOException {
		log.log(Level.WARNING, "File manager feature not implemented returning 404");
		return404("File manager feature not implemented");
	}

	private void return401() throws IOException {
		if (socket.isClosed()) {
			return;
		}
		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println(HEADER_HTTP_401_UNAUTHORIZED);
		ps.println(HEADER_SERVER);
		ps.println(HEADER_CONNECTION_CLOSE);
		ps.println(CR_LF);
		ps.flush();
		if (!socket.isClosed()) {
			socket.close();
		}
	}

	private void return400(String content) throws IOException {
		if (socket.isClosed()) {
			return;
		}
		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println(HEADER_HTTP_400_BAD_REQUEST);
		ps.println(HEADER_SERVER);
		ps.println(HEADER_CONNECTION_CLOSE);
		if (content != null) {
			ps.println(HEADER_CONTENT_LENGTH + content.length());
			ps.println(HEADER_CONTENT_TYPE_JSON);
			ps.println(content);
		}
		ps.println(CR_LF);
		ps.flush();
		if (!socket.isClosed()) {
			socket.close();
		}
	}
	
	private void return404(String message) throws IOException {
		if (socket.isClosed()) {
			return;
		}
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("rc", 4);
		jsonObject.addProperty("reason", 10);
		jsonObject.addProperty("stack", "ERROR: " + message);
		jsonObject.addProperty("category", 6);
		jsonObject.addProperty("message", message);

		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println(HEADER_HTTP_404_NOT_FOUND);
		ps.println(HEADER_SERVER);
		ps.println(HEADER_CONNECTION_CLOSE);
		ps.println(HEADER_CONTENT_LENGTH + jsonObject.toString().length());
		ps.println(HEADER_CONTENT_TYPE_JSON);
		ps.println(jsonObject.toString());
		ps.println(CR_LF);
		ps.flush();
		socket.close();
	}

	private void return500(Exception e) throws IOException {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("rc", 99);
		jsonObject.addProperty("reason", 99);
		jsonObject.addProperty("stack", ExceptionUtils.getStackTrace(e));
		jsonObject.addProperty("category", 99);
		jsonObject.addProperty("message", e.getMessage() != null ? e.getMessage() : ExceptionUtils.getStackTrace(e));
		
		OutputStream output = socket.getOutputStream();
		PrintStream ps = new PrintStream(output);
		ps.println(HEADER_HTTP_500_INTERNAL_SERVER_ERROR);
		ps.println(HEADER_SERVER);
		ps.println(HEADER_CONNECTION_CLOSE);
		ps.println(HEADER_CONTENT_LENGTH + jsonObject.toString().length());
		ps.println(HEADER_CONTENT_TYPE_JSON);
		ps.println(jsonObject.toString());
		ps.println(CR_LF);
		ps.flush();
		socket.close();
	}

	private String getMethod() {
		StringTokenizer st = new StringTokenizer(headers.get(0), " ");
		return st.nextToken();
	}

	private String findPath() {
		StringTokenizer st = new StringTokenizer(headers.get(0), " ");
		if (st.countTokens() == 3) {
			st.nextToken();
			return st.nextToken();
		}
		return "";
	}

	private boolean processInput() throws InterruptedException {
		BufferedReader br = null;
		try {
			log.log(Level.INFO, "Received HTTP request from address: " + socket.getInetAddress());
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			log.log(Level.WARNING, "Unable to access input stream from HTTP");
			return false;
		}
		return readInput(br);
	}

	private boolean readInput(BufferedReader br) throws InterruptedException {
        method = "";
        contentType = "";
		boolean alive = false;
		boolean readAllHeaders = false;
		try {
			int count = 0;
			while (!br.ready() && count < 3) {
				Thread.sleep(1000);
				count++;
			}
			while (br.ready()) {
				alive = true;
				String data = br.readLine();
				if (readAllHeaders) {
					log.log(Level.INFO, data);
					bodyList.add(data);
				} else {
					if (data.equals("")) {
						readAllHeaders = true;
						if (isPutJson()) {
						    break;
						}
					} else {
						log.log(Level.INFO, data);
						headers.add(data);
						saveHeaderValues(data);
					}
				}
			}
		} catch (IOException e) {
			alive = false;
			log.log(Level.SEVERE, e.getMessage(), e);
			log.log(Level.WARNING, "Unable to access input stream from HTTP");
		}
		return alive;
	}

	private boolean isPutJson() {
        return method.equals("PUT") && contentType.equals("application/json");
    }

    private void saveHeaderValues(String data) {
        if (data.startsWith("Authorization:")) {
            authorization = data.substring("Authorization: ".length());
        } else if (data.startsWith("X-IBM-Requested-Method:")) {
            method = data.substring("X-IBM-Requested-Method: ".length());
        } else if (data.startsWith("Content-Type:")) {
            contentType = data.substring("Content-Type: ".length());
        }
    }

    public void setSocket(Socket socket) {
		this.socket = socket;

	}

}
