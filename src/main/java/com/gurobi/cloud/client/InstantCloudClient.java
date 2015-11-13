package com.gurobi.cloud.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.gurobi.cloud.model.InstantCloudLicense;
import com.gurobi.cloud.model.InstantCloudMachine;


public class InstantCloudClient {
  private static final String BASE_URL = "https://cloud.gurobi.com/api/";
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
  private static final String CHARSET = "UTF-8";
  private static final String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";
  private static final String COMMANDS[] = { "licenses", "machines", "launch", "kill" };
  private static final String METHOD[] = { "GET", "GET", "POST", "POST" };
  public String accessId  = null;
  public String secretKey = null;
  public boolean verbose = false;

  public InstantCloudClient(String accessId, String secretKey) {
    this.accessId = accessId;
    this.secretKey = secretKey;
  }

  private static byte[] zeroPad(int length, byte[] bytes) {
    byte[] padded = new byte[length]; // initialized to zero by JVM
    System.arraycopy(bytes, 0, padded, 0, bytes.length);
    return padded;
  }

  private static String encode(byte[] stringArray) {
    String encoded = "";
    // determine how many padding bytes to add to the output
    int paddingCount = (3 - (stringArray.length % 3)) % 3;
    // add any necessary padding to the input
    stringArray = zeroPad(stringArray.length + paddingCount, stringArray);
    // process 3 bytes at a time, churning out 4 output bytes
    // worry about CRLF insertions later
    for (int i = 0; i < stringArray.length; i += 3) {
      int j = ((stringArray[i] & 0xff) << 16) +
        ((stringArray[i + 1] & 0xff) << 8) +
        (stringArray[i + 2] & 0xff);
      encoded = encoded + base64code.charAt((j >> 18) & 0x3f) +
        base64code.charAt((j >> 12) & 0x3f) +
        base64code.charAt((j >> 6) & 0x3f) +
        base64code.charAt(j & 0x3f);
    }
    // replace encoded padding nulls with "="
    return encoded.substring(0, encoded.length() - paddingCount) + "==".substring(0, paddingCount);
  }

  private static String toHexString(byte[] bytes) {
    Formatter formatter = new Formatter();
    for (byte b : bytes) {
      formatter.format(":%02x", b);
    }
    return formatter.toString();
  }

  private String signRequest(String rawString)
    throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
    SecretKeySpec signingKey = new SecretKeySpec(this.secretKey.getBytes(),
                                                 HMAC_SHA1_ALGORITHM);
    Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
    mac.init(signingKey);
    byte[] rawHmac = mac.doFinal(rawString.getBytes());
    if (this.verbose) {
      System.out.println("digest: " + toHexString(rawHmac));
    }
    return encode(rawHmac);
  }

  private static String convertStreamToString(InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  private String sendCommand(String command, Map<String,String> params)
    throws Exception {
    int cmd;
    for (cmd = 0; cmd < 4; cmd++) {
      if (command.equals(this.COMMANDS[cmd])) {
        break;
      }
    }
    if (cmd == 4) {
      throw new Exception("Unknown command " + command);
    }

    String method = this.METHOD[cmd];
    String requestStr = method;
    String query = null;
    String urlStr = null;

    urlStr = this.BASE_URL + command;
    if (method.equals("GET")) {
      urlStr = urlStr + "?id=" + this.accessId;
    }

    params.put("id", this.accessId);
    for (String key: params.keySet()) {
      String val = params.get(key);
      String keyVal = key + '=' + URLEncoder.encode(val, CHARSET);
      requestStr = requestStr + '&' + keyVal;
      if (method.equals("POST")) {
        if (query == null) {
          query = keyVal;
        } else {
          query = query + '&' +  keyVal;
        }
      }
    }

    /* Add date */
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH::mm'Z'");
    df.setTimeZone(tz);
    String now = df.format(new Date());
    requestStr = requestStr + '&' + now;

    /* Sign request */
    String signature = this.signRequest(requestStr);

    if (this.verbose) {
      System.out.println("Request String:" + requestStr);
      System.out.println("Secret Key:" + this.secretKey);
      System.out.println("Signature:" + signature);
      System.out.println("URL:" + urlStr);
      System.out.println("Query:" + query);
    }

    URL url = new URL(urlStr);
    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    connection.setRequestProperty("X-Gurobi-Signature", signature);
    connection.setRequestProperty("X-Gurobi-Date", now);
    if (method.equals("POST")) {
      connection.setDoOutput(true);
      OutputStream output = connection.getOutputStream();
      output.write(query.getBytes(CHARSET));
    }

    int statusCode = connection.getResponseCode();
    InputStream response;
    if (statusCode == 200) {
      response = connection.getInputStream();
    } else {
      System.out.println("Server Error:" + statusCode);
      response = connection.getErrorStream();
    }

    String out = convertStreamToString(response);

    if (statusCode != 200) {
      throw new Exception("Error: " + statusCode + "\n" + out);
    }
    return out;
  }

  public InstantCloudLicense[] getLicenses() throws Exception {
    Map <String, String> params = new HashMap<String, String>();
    String jsonStr = this.sendCommand("licenses", params);
    JSONParser jsonParser = new JSONParser();
    JSONArray  jsonArray  = (JSONArray)jsonParser.parse(jsonStr);
    InstantCloudLicense[] licenses = new InstantCloudLicense[jsonArray.size()];
    for (int i = 0; i < licenses.length; i++) {
      JSONObject jsonObject = (JSONObject) jsonArray.get(i);
      InstantCloudLicense license = new InstantCloudLicense();
      license.licenseId  = (String) jsonObject.get("licenseId");
      license.credit     = (String) jsonObject.get("credit");
      license.expiration = (String) jsonObject.get("expiration");
      license.ratePlan   = (String) jsonObject.get("ratePlan");
      licenses[i] = license;
    }
    return licenses;
  }

  private InstantCloudMachine[] parseMachines(String jsonStr)
    throws Exception {
    JSONParser jsonParser = new JSONParser();
    JSONArray  jsonArray  = (JSONArray)jsonParser.parse(jsonStr);
    InstantCloudMachine[] machines = new InstantCloudMachine[jsonArray.size()];
    for (int i = 0; i < machines.length; i++) {
      JSONObject jsonObject = (JSONObject) jsonArray.get(i);
      InstantCloudMachine machine =  new InstantCloudMachine();
      machine.machineId    = (String) jsonObject.get("_id");
      machine.state        = (String) jsonObject.get("state");
      machine.DNSName      = (String) jsonObject.get("DNSName");
      machine.createTime   = (String) jsonObject.get("createTime");
      machine.machineType  = (String) jsonObject.get("machineType");
      machine.region       = (String) jsonObject.get("region");
      machine.licenseType  = (String) jsonObject.get("licenseType");
      machine.idleShutdown = (Long) jsonObject.get("idleShutdown");
      machine.licenseId    = (String) jsonObject.get("licenseId");
      machine.userPassword = (String) jsonObject.get("userPassword");
      machines[i] = machine;
    }
    return machines;
  }
  public InstantCloudMachine[] getMachines() throws Exception {
    Map <String, String> params = new HashMap<String, String>();
    String jsonStr = this.sendCommand("machines", params);
    return parseMachines(jsonStr);
  }

  public InstantCloudMachine[] killMachines(String[] machineIds) throws Exception {
    Map <String, String> params = new HashMap<String, String>();
    String machineJSON = "[";
    for (int i = 0; i < machineIds.length; i++) {
      machineJSON = machineJSON + '"' + machineIds[i] + '"';
      if (i < machineIds.length - 1) {
        machineJSON = machineJSON + ',';
      }
    }
    machineJSON = machineJSON + ']';
    params.put("machineIds", machineJSON);
    String jsonStr = this.sendCommand("kill", params);
    return parseMachines(jsonStr);
  }

  public InstantCloudMachine[] launchMachines(int numMachines,
                                              String licenseType,
                                              String licenseId,
                                              String userPassword,
                                              String region,
                                              int    idleShutdown,
                                              String machineType)
    throws Exception {
    Map <String, String> params = new HashMap<String, String>();
    params.put("numMachines", String.valueOf(numMachines));
    if (licenseType != null) {
      params.put("licenseType", licenseType);
    }
    if (licenseId != null) {
      params.put("licenseId", licenseId);
    }
    if (userPassword != null) {
      params.put("userPassword", userPassword);
    }
    if (region != null) {
      params.put("region", region);
    }
    if (idleShutdown != -1) {
      params.put("idleShutdown", String.valueOf(idleShutdown));
    }
    if (machineType != null) {
      params.put("machineType", machineType);
    }
    String jsonStr = this.sendCommand("launch", params);
    return parseMachines(jsonStr);
  }
}
