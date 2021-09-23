import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyProxy {
    private static final int BUFSIZ = 102400;
    public static void main(String[] args) throws UnknownHostException,
            IOException {
        String log = null;
        // Port of Proxy Server
        int ProxyPort = 3310;
        // Port of Real Server
        int HostPort = 80;
        // Host name of the of real server
        String HostName = "www.bom.gov.au";
        // Time format
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Ciry to rewrite
        String rewriteCity = "Canberra";
        // Target City
        String targetCity = "LasVegas";

        // Read Host Name from input
        if (args.length >= 1)
            HostName = args[0];
        // Read Proxy Port
        if (args.length >= 2)
            ProxyPort = Integer.parseInt(args[1]);
        ServerSocket serverSocket = null;
        try {
            // Build serverSocket with request port
            serverSocket = new ServerSocket(ProxyPort);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        while (true) {
            // Socket from the client
            Socket ClientSocket = null;
            // Socket with target host server
            Socket HostSocket = null;
            try {
                // Receive request
                ClientSocket = serverSocket.accept();
                // Connect with real Server
                HostSocket = new Socket(HostName, HostPort);
                // Read request and save in reader
                InputStream input = ClientSocket.getInputStream();
                Reader reader = new InputStreamReader(input);
                // Transmit request to real Server
                OutputStream output = HostSocket.getOutputStream();
                // Store request
                char chars[] = new char[BUFSIZ];
                int len;
                if ((len = reader.read(chars)) != -1) {
                    // log the request timestamp
                    log = "Timestamp of Request: " + df.format(new Date()) + "\r\n";
                    String request = new String(chars, 0, len);
                    int n = request.indexOf("HTTP");
                    // log the request content
                    log += "Request: " + request.substring(0, n+8) + "\r\n";
                    // rewrite the request
                    request = request.substring(0, n);
                    request += "HTTP/1.0\r\n" +
                            "Host: " + HostName + "\r\n" +
                            "User-Agent: WebProxy\r\n" +
                            "Accept-Encoding: \r\n" +
                            "\r\n";
                    output.write(request.getBytes());
                    output.flush();
                }

                // Transmit response that read from real Server
                // Store the response
                byte[] bytes = new byte[BUFSIZ];
                // The length of read
                int inputlen;
                // Number of rewrite links
                int LinkCount = 0;
                // Number of rewrite city
                int CityCount = 0;
                // Whether current buff has the response header
                boolean headerFlag = true;
                while (true) {
                    try {
                        if ((inputlen = HostSocket.getInputStream().read(bytes)) > 0) {
                            String str = new String(bytes);
                            if(headerFlag){
                                String response_header = str.split("\r\n")[0];
                                int index = response_header.indexOf(" ");
                                log += "Response Status:" + response_header.substring(index) + "\r\n";
                                headerFlag = false;
                            }

                            if(str.contains("html")){
                                // rewrite simple html link
                                String string = str.replaceAll("<a href=\"(http(s)*://)*" + HostName, "<a href=\"http://localhost:" + ProxyPort);
                                // log the rewrite link
                                Pattern linkPattern = Pattern.compile("<a href=\"(http(s)*://)*" + HostName);
                                LinkCount += countPattern(str,linkPattern);
                                // rewrite the city name Canberra to LasVegas
                                String response = string.replaceAll("((?<=>)[^<]*)"+ rewriteCity +"([^>]*(?=</(.*)>))", "$1" + targetCity + "$2");
                                // log the rewrite city
                                Pattern cityPattern = Pattern.compile(targetCity);
                                CityCount += countPattern(response,cityPattern);
                                ClientSocket.getOutputStream().write(response.getBytes(), 0, inputlen);
                            } else {
                                ClientSocket.getOutputStream().write(bytes, 0, inputlen);
                            }
                            ClientSocket.getOutputStream().flush();
                        } else if (inputlen < 0) {
                            log += "Rewrite links: " + LinkCount + "\r\n";
                            log += "Rewrite Cities: " + CityCount + "\r\n";
                            break;
                        }
                    } catch (InterruptedIOException e) {
                    }
                }
                // write log to STDOUT
                log += "\r\n";
                writeLog(log);


            } catch (Exception e) {
                e.printStackTrace();
                continue;
            } finally {
                // close the sockets
                HostSocket.close();
                ClientSocket.close();
            }
        }

    }

    private static boolean writeLog(String log) throws Exception{

        try {
            File file =new File("STDOUT");
            //        if file doesnt exists, then create it
            if(!file.exists()){
                file.createNewFile();
            }
            //        true = append file
            FileWriter fileWritter = new FileWriter(file.getName(),true);
            fileWritter.write(log);
            fileWritter.flush();
            fileWritter.close();
            System.out.println(log);
        } catch (InterruptedIOException e) {
        }
        return true;

    }

    // count the times of link or city
    public static int countPattern(String references, Pattern referencePattern){
        Matcher matcher = referencePattern.matcher(references);
        int count = 0;
        while(matcher.find()){
            count++;
        }
        return count;
    }
}
