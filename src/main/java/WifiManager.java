import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;


public class WifiManager {
	private final String routerIP;
	private final String routerPassword;
	private final boolean enableWIFI;

	private static final String WIFI_ENABLE = "1";
	private static final String WIFI_DISABLE = "0";
	
	public static void main(String[] args) {
		if(args.length == 1 && (args[0].equals(WIFI_ENABLE) || args[0].equals(WIFI_DISABLE))) {
			new WifiManager(args[0].equals(WIFI_ENABLE));
		} else {
			System.err.println("Wrong number of arguments (EX. WifiManager 0)");
		}
	}
	
	public WifiManager(boolean enableWIFI) throws RuntimeException {
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream("WifiManager.properties"));

			this.routerIP = properties.getProperty("router.ip");
			this.routerPassword = properties.getProperty("router.password");
			this.enableWIFI = enableWIFI;
			
			changeWIFI();
		} catch (IOException e) {
			throw new RuntimeException("Error: No existe el archivo de propiedades WifiManager.properties");
		}
	}
	
	private void changeWIFI() {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        // Create a local instance of cookie store
        CookieStore cookieStore = new BasicCookieStore();
		// Create local HTTP context
        HttpContext localContext = new BasicHttpContext();
        // Bind custom cookie store to the local context
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		try {
            routerLogin(httpclient, localContext);
            routerStablishWifiMode(httpclient, localContext);
            routerLogout(httpclient, localContext);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
			try {
				httpclient.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
	}
	
	//Router Login
	private void routerLogin(CloseableHttpClient httpclient, HttpContext localContext) throws ClientProtocolException, IOException {
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("pws", routerPassword));
		UrlEncodedFormEntity entity;

		entity = new UrlEncodedFormEntity(formparams, "UTF-8");
		HttpPost httppost = new HttpPost("http://"+routerIP+"/cgi-bin/login.exe");
		httppost.setEntity(entity);
		
		HttpResponse response = httpclient.execute(httppost,localContext);
		EntityUtils.consumeQuietly(response.getEntity());
	}
	
	public void routerStablishWifiMode(CloseableHttpClient httpclient, HttpContext localContext) throws ClientProtocolException, IOException {
		HttpGet httpget = new HttpGet("http://"+routerIP+"/wireless_main.stm");
		HttpResponse response = httpclient.execute(httpget, localContext);
        EntityUtils.consumeQuietly(response.getEntity());
        
		List<NameValuePair> formparamsWifi = new ArrayList<NameValuePair>();
		formparamsWifi.add(new BasicNameValuePair("Wireless_enable", (enableWIFI?WIFI_ENABLE:WIFI_DISABLE)));

		UrlEncodedFormEntity entityWifi = new UrlEncodedFormEntity(formparamsWifi, "UTF-8");
		HttpPost httppostWifi = new HttpPost("http://"+routerIP+"/cgi-bin/wireless_eb.exe");
		httppostWifi.setEntity(entityWifi);
		
		response = httpclient.execute(httppostWifi, localContext);
		EntityUtils.consumeQuietly(response.getEntity());
	}
	
	public void routerLogout(CloseableHttpClient httpclient, HttpContext localContext) throws ClientProtocolException, IOException {
		HttpGet httpgetLogout = new HttpGet("http://"+routerIP+"/cgi-bin/logout.exe");
		HttpResponse response = httpclient.execute(httpgetLogout, localContext);
		EntityUtils.consumeQuietly(response.getEntity());
	}
}
