package cik;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Onlinestatuscheck {

    static Map<String, String> memberList = new HashMap<String, String>();
    static Map<String, String> fmoMemberList = new HashMap<String, String>();
    static int flagForMemberList;

    public static void main(String args[]) {
        while (true) {

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            System.out.println(dateFormat.format(date));

            System.out.println("Checking outfit member statuses...");
            String url = "http://census.daybreakgames.com/get/ps2:v2/outfit?alias=CIK&c:resolve=member_online_status&c:resolve=member_character";
            String urlResponse = getJsonResponse(url);

            JSONParser parser = new JSONParser();
            Object tempObj = new Object();
            try {
                tempObj = parser.parse(urlResponse);
            } catch (Exception e) {
                System.out.println("Could not parse response.");
            }

            if (tempObj == null) {
                return;
            }
            JSONObject jsonResponse = (JSONObject) tempObj;
            JSONArray body = (JSONArray) jsonResponse.get("outfit_list");
            JSONObject declaration = (JSONObject) body.get(0);
            JSONArray members = (JSONArray) declaration.get("members");
            memberList = getMemberList(members);
            compareLists();
            memberList = new HashMap<String, String>();
            flagForMemberList=(flagForMemberList+1)%5;
            System.out.print("\n\n\n");
            try {
                Thread.sleep(300000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static Map<String,String> getMemberList(JSONArray members) {
        for (Object memberObject : members) {
            JSONObject member = (JSONObject) memberObject;
            String onlineStatus = member.get("online_status").equals("0") ? "Offline" : "Online";
            JSONObject nameArray = (JSONObject) member.get("name");
            String name = (String) nameArray.get("first");
            memberList.put(name, onlineStatus);
        }
        return memberList;
    }

    private static void compareLists() {
        if (fmoMemberList.isEmpty() || flagForMemberList == 0) {
            int onlineMembers = 0;

            for (String key : memberList.keySet()) {
                String status = memberList.get(key);
                if (status == null) {
                    continue;
                }
                if (status.equals("Online")) {
                    System.out.println(String.format("%-20s %10s", key, status));
                    onlineMembers++;
                }
            }
            System.out.println(String.format("%d members are currently online.",onlineMembers));
            fmoMemberList = memberList;
            return;
        }

        boolean newEvent = false;
        for (String member : memberList.keySet()) {
            if (fmoMemberList.containsKey(member) && !memberList.containsKey(member)) {
                System.out.println(String.format("%s has left the outfit.", member));
                newEvent = true;
                continue;
            }

            if (!fmoMemberList.containsKey(member) && memberList.containsKey(member)) {
                System.out.println(String.format("%s has joined the outfit!", member));
                newEvent = true;
                continue;
            }

            if (fmoMemberList.get(member).equals("Offline") && memberList.get(member).equals("Online")) {
                System.out.println(String.format("%s just logged in!", member));
                newEvent = true;
                continue;
            }

            if (memberList.get(member).equals("Offline") && fmoMemberList.get(member).equals("Online")) {
                System.out.println(String.format("%s just logged out.", member));
                newEvent = true;
            }
        }

        if (!newEvent) {
            System.out.println("Nothing new to report...");
        }

        fmoMemberList = memberList;
    }

    private static String getJsonResponse(String url) {
        String urlResponse = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            HttpGet httpget = new HttpGet("http://census.daybreakgames.com/get/ps2:v2/outfit?alias=CIK&c:resolve=member_online_status&c:resolve=member_character");
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            urlResponse = httpclient.execute(httpget, responseHandler);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (urlResponse == null) {
            System.out.println("Response was empty.");
            return null;
        }
        return urlResponse;
    }
}
