package com.arrayprolc.lunadb.webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.StringTokenizer;

import com.arrayprolc.lunadb.LunaDB;
import com.arrayprolc.lunadb.SetupQuestions;
import com.arrayprolc.lunadb.WebGraphicsHandler;
import com.arrayprolc.lunadb.command.WebCommandInterpreter;
import com.arrayprolc.lunadb.logger.LoggedQuery;
import com.arrayprolc.lunadb.login.LoginManager;
import com.arrayprolc.lunadb.util.UtilFile;

public class WebHandler extends Thread {
    private Socket client;
    private BufferedReader inFromClient = null;
    private DataOutputStream outToClient = null;
    @SuppressWarnings("unused")
    private WebServer ws = null;

    public WebHandler(Socket c, WebServer w) {
        this.client = c;
        this.ws = w;
    }

    @SuppressWarnings("unused")
    public void run() {
        try {
            this.inFromClient = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            this.outToClient = new DataOutputStream(this.client.getOutputStream());
            String requestString = this.inFromClient.readLine();
            String headerLine = requestString;
            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            String httpMethod = tokenizer.nextToken();
            String httpQueryString = tokenizer.nextToken();
            while (this.inFromClient.ready()) {
                requestString = this.inFromClient.readLine();
                String[] v = requestString.split(": ");
            }

            LoggedQuery query = new LoggedQuery(client.getInetAddress(), httpQueryString, httpMethod);

            if (httpMethod.equals("GET")) {
                httpQueryString = URLDecoder.decode(httpQueryString, "UTF-8");
                if (httpQueryString.startsWith("/core")) {
                    try {
                        String s = UtilFile.exportResource(httpQueryString.split("/core")[1].replace("..", "").replace("\"", ""));
                        query = this.sendResponse(200, s, true, query);
                        Files.deleteIfExists(new File(s).toPath());
                        return;
                    } catch (Exception ex) {
                        query = this.sendResponse(404, "FAILED: File not found.", false, query);
                        return;
                    }
                }
                if (httpQueryString.startsWith("/?")) {
                    httpQueryString = httpQueryString.replaceFirst("/?", "/formatted:publicKey!!#");
                }
                if (!SetupQuestions.isDoneWithQuestions()) {
                    String s = SetupQuestions.send(httpQueryString);
                    if (!s.equals("/")) {
                        query = this.sendResponse(200, SetupQuestions.send(httpQueryString), true, query);
                        return;
                    }
                    httpQueryString = "/";
                }
                if (httpQueryString.startsWith("/location-setup")) {
                    httpQueryString = "/";
                }
                if (httpQueryString.equalsIgnoreCase("/")) {
                    httpQueryString = "/formatted:publicKey!!#login-page";
                }
                httpQueryString = httpQueryString.substring(1);
                httpQueryString = httpQueryString.replace("?query=", "");
                httpQueryString = httpQueryString.replace("+", " ");
                httpQueryString = httpQueryString.replace("[poundsign]", "#");
                boolean isFormatted = false;
                if (httpQueryString.startsWith("formatted:")) {
                    isFormatted = true;
                    httpQueryString = httpQueryString.replaceFirst("formatted:", "");
                }
                if (!httpQueryString.contains("!!")) {
                    this.sendResponse(200, "FAILED: No access key has been specified.", false);
                    return;
                }
                String t = httpQueryString.split("!!")[0];
                String q = httpQueryString.split("!!")[1];
                if (isFormatted) {
                    if (t.equals("publicKey")) {
                        if (LoginManager.getInstance().isLoggedIn(this.client.getInetAddress().toString())) {
                            t = LunaDB.getManager().getAccessKey();
                        }
                    }
                }
                if (!(t.equals(LunaDB.getManager().getAccessKey()) || t.equals("publicKey"))) {
                    query = this.sendResponse(200, "FAILED: Incorrect access key. Your IP and query has been logged. ", false, query);
                    // TODO log IP
                    return;
                }

                String response = WebCommandInterpreter.getInstance().interpret(q.replace("%20", " "), this.client.getInetAddress().toString(), !t.equals("publicKey"));
                if (isFormatted) {
                    response = WebGraphicsHandler.handleResponse(q.replace("%20", " "), response, this.client.getInetAddress().toString());
                }
                query = this.sendResponse(200, response, false, query);
                return;
            }
        } catch (Exception localException) {
        }
    }

    public LoggedQuery sendResponse(int statusCode, String responseString, boolean isFile, LoggedQuery query) throws Exception {
        query.setResponseCode(statusCode);
        query.setResponse(responseString);
        query.setFile(isFile);
        sendResponse(statusCode, responseString, isFile);
        return query;
    }

    public void sendResponse(int statusCode, String responseString, boolean isFile) throws Exception {
        String statusLine = null;
        String serverdetails = "Server: Java HTTPServer";
        String contentLengthLine = null;
        String fileName = null;
        String contentTypeLine = "Content-Type: text/html\r\n";
        FileInputStream fin = null;
        statusLine = "HTTP/1.1 200 OK\r\n";
        switch (statusCode) {
        case 200:
            statusLine = "HTTP/1.1 200 OK\r\n";
            break;
        case 500:
            statusLine = "HTTP/1.1 500 Internal Server Error\r\n";
            break;
        case 222:
            statusLine = "HTTP/1.1 222 Ping Response\r\n";
            break;
        default:
            statusLine = "HTTP/1.1 404 Not Found\r\n";
        }
        if (isFile) {
            fileName = responseString;
            fin = new FileInputStream(fileName);
            contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";
            if ((!fileName.endsWith(".htm")) && (!fileName.endsWith(".html"))) {
                contentTypeLine = "Content-Type: application/zip\r\n";
            }
        } else {
            contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
        }
        if (!this.client.isClosed()) {
            this.outToClient.writeBytes(statusLine);
            this.outToClient.writeBytes(serverdetails);
            this.outToClient.writeBytes(contentTypeLine);
            this.outToClient.writeBytes(contentLengthLine);
            this.outToClient.writeBytes("Connection: close\r\n");
            this.outToClient.writeBytes("\r\n");
            if (isFile) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fin.read(buffer)) != -1) {
                    // int bytesRead;
                    this.outToClient.write(buffer, 0, bytesRead);
                }
                fin.close();
            } else {
                this.outToClient.writeBytes(responseString);
            }
            this.outToClient.close();
        }
    }
}