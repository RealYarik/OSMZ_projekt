package com.vsb.kru13.osmzhttpserver;

import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import androidx.annotation.RequiresApi;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;

public class ClientSocket extends Thread  {

    private Socket s;
    private static final String DEFAULT_FILE = "index.html";
    private static final String FILE_NOT_FOUND = "404.html";
    private Handler messageHandler;
    private static final String MSG_KEY_DATE = "date";
    private static final String MSG_KEY_HTTP= "GET";
    private static final String MSG_KEY_FILENAME= "/";
    private static final String MSG_KEY_FILESIZE= "0";
    public static final String CGI_URL = "cgi-bin";

    private Message msg;
    private Bundle bundle;
    private Semaphore lock;

    public ClientSocket(Socket s, Handler messageHandler,Semaphore lock) {
        this.s = s ;
        this.messageHandler = messageHandler;
        this.lock=lock;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void run() {
        try {
            try {
                sock();
            }catch (NullPointerException e){
                try {
                    s.close();
                }catch (NoSuchElementException el){
                    el.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            msg.setData(bundle);
            messageHandler.sendMessage(msg);
            lock.release();
            Log.d("THREADS", "Free lock " + lock.availablePermits());

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sock() throws IOException,NullPointerException, NoSuchElementException {

        OutputStream o = s.getOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(s.getOutputStream());
        String tmp = in.readLine();
        String fileRequested = null;
        StringTokenizer parse = new StringTokenizer(tmp);
        String method = parse.nextToken().toUpperCase();
        fileRequested = parse.nextToken().toLowerCase();
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        msg = messageHandler.obtainMessage();
        bundle = new Bundle();
        try {

            while (!tmp.isEmpty()) {
                Log.d("SERVER", "MED:" + tmp);
                tmp = in.readLine();
            }

            if (!method.equals("GET")) {
                File file = new File(sdPath + "/not_supported.html");
                int fileLength = (int) file.length();


                String response = "HTTP/1.1 501 Not Implemented\r\n" +
                        "Server: nginx\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + (int) file.length() + "\r\n" +
                        "Connection: close\r\n\r\n";


                out.write(response);
                out.flush();
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];

                int len;
                while((len = fileInputStream.read(buffer)) != -1)
                {
                    o.write(buffer, 0, len);
                }

                bundle.putString(MSG_KEY_DATE, getCurrentTime());
                bundle.putString(MSG_KEY_HTTP, method);
                bundle.putString(MSG_KEY_FILENAME, fileRequested);
                bundle.putString(MSG_KEY_FILESIZE, fileLength+"");


            } else {

                if(fileRequested.contains(CGI_URL)){
                    String cgiCommand = "";
                    Log.d("CGI request", fileRequested);
                    int cgiIndex = fileRequested.indexOf(CGI_URL)+CGI_URL.length()+1;
                    int endIndex =   fileRequested.length();

                    String encodedCommandToExecute = fileRequested.substring(cgiIndex, endIndex);


                    StringBuilder decodedCommandToExecute = new StringBuilder();
                    decodedCommandToExecute.append(URLDecoder.decode(encodedCommandToExecute, "UTF-8"));
                    Log.d("CGI decoded command", decodedCommandToExecute.toString());

                    cgiCommand = decodedCommandToExecute.toString();
                    cgiBuilder(cgiCommand,sdPath,out,fileRequested);
                    s.close();
                    return;

                }

                if (fileRequested.endsWith("/")) {
                    fileRequested += DEFAULT_FILE;
                }
                File file = new File(sdPath + fileRequested);

                if(!file.exists()) {
                    throw new FileNotFoundException();
                }




                String response = "HTTP/1.1 200 OK\r\n" +
                        "Server: nginx\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + (int) file.length() + "\r\n" +
                        "Connection: close\r\n\r\n";


                out.write(response);
                out.flush();

                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];

                int len;
                while((len = fileInputStream.read(buffer)) != -1)
                {
                    o.write(buffer, 0, len);
                }

                bundle.putString(MSG_KEY_DATE, getCurrentTime());
                bundle.putString(MSG_KEY_HTTP, method);
                bundle.putString(MSG_KEY_FILENAME, fileRequested);
                bundle.putString(MSG_KEY_FILESIZE, (int) file.length()+"");

            }
                s.close();
        }catch (FileNotFoundException e) {
            try {
                fileNotFound(out, outToClient,sdPath,fileRequested,method);
                s.close();
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
            }

        }


    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void fileNotFound(BufferedWriter out, OutputStream o, String sdPath, String fileRequested,String method) throws IOException {
        File fileNotFound = new File(sdPath + "/" + FILE_NOT_FOUND);


        if (fileRequested.endsWith(".html")) {

            String respHtml = "<html>\n" +
                    "<head>\n" +
                    "    <title>Hello World!</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<h1>You are on  " +fileRequested.replaceAll("/","")+"</h1>\n" +
                    "</body>\n" +
                    "</html>";

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Server: nginx\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length:" +respHtml.length()+"\r\n" +
                    "Connection: close\r\n\r\n";


            out.write(response+respHtml);
            out.flush();
            bundle.putString(MSG_KEY_DATE, getCurrentTime());
            bundle.putString(MSG_KEY_HTTP, method);
            bundle.putString(MSG_KEY_FILENAME, fileRequested);
            bundle.putString(MSG_KEY_FILESIZE, respHtml.length()+"");
            return;
        }

        String response = "HTTP/1.1 404 Page Not Found\r\n" +
                "Server: nginx\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + (int) fileNotFound.length() + "\r\n" +
                "Connection: close\r\n\r\n";


        out.write(response);
        out.flush();
        FileInputStream fileInputStream = new FileInputStream(fileNotFound);
        byte[] buffer = new byte[1024];

        int len;
        while((len = fileInputStream.read(buffer)) != -1)
        {
            o.write(buffer, 0, len);
        }
        out.close();
        o.close();
        bundle.putString(MSG_KEY_DATE, getCurrentTime());
        bundle.putString(MSG_KEY_HTTP, method);
        bundle.putString(MSG_KEY_FILENAME, fileRequested);
        bundle.putString(MSG_KEY_FILESIZE, (int) fileNotFound.length()+"");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss",Locale.GERMANY);
        return dateFormat.format(new Date());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void cgiBuilder(String cgiCommand, String sdPath, BufferedWriter out,String fileRequested){
            StringBuilder cgiStringBuilder = new StringBuilder();

            try {
                // Create the CGI storage directory if it does not exist
                File cgiDir = new File(sdPath + "/CGI");
                File file = new  File(sdPath + "/CGI/cgi.txt");
                if (! cgiDir.exists()){
                    if (! cgiDir.mkdirs()){
                        Log.d("CGI", "failed to create directory");
                        return;
                    }
                }


                ProcessBuilder pb;
                if(cgiCommand.contains("cat")){
                    pb = new ProcessBuilder("cat", cgiCommand.split(" ")[1]);
                } else {
                    pb = new ProcessBuilder(cgiCommand);
                }
                final Process p = pb.start();

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedWriter bw = new BufferedWriter(
                        new FileWriter(file));

                String line;
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    cgiStringBuilder.append(line + "\n");
                }
                bw.close();


                if(cgiStringBuilder.length() > 0) {
                    Log.d("CGI OUTPUT", cgiStringBuilder.toString());


                    out.write("HTTP/1.0 200 OK\n");
                    out.write("CGI OUTPUT:\n\n");
                    out.write(cgiStringBuilder.toString());
                    out.flush();
                } else {
                    Log.d("CGI EMPTY", "Requested command is not valid to extract");
                    out.write("HTTP/1.0 404 Not Found\n\n");
                    out.write("CGI OUTPUT ERROR:\n\n");
                    out.flush();
                }
                bundle.putString(MSG_KEY_DATE, getCurrentTime());
                bundle.putString(MSG_KEY_HTTP, "GET");
                bundle.putString(MSG_KEY_FILENAME, fileRequested);
                bundle.putString(MSG_KEY_FILESIZE, (int)file.length()+"");

            } catch (Exception ex) {
                System.out.println(ex);
            }

    }

}
