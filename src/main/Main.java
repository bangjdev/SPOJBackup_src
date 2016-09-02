package main;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class Main {

	private static String source, destination, sourceHTML, username,
			spojcookie, password;
	private static Console console = System.console();
	private static ArrayList<String> eleList = new ArrayList<String>();

	private static void login() throws IOException {
		HttpURLConnection con;
		do {
			username = console.readLine("Username: ");
			password = String.valueOf(console.readPassword("Password: "));
			System.out.println("Connecting to server...");
			con = connectToServer();
			if (con.getResponseCode() == 200)
				System.out.println("Wrong username or password");
		} while (con.getResponseCode() == 200);
		spojcookie = readCookies(con);
		System.out.println("Access granted!");
	}

	private static HttpURLConnection connectToServer() {
		try {
			System.out.print("Openning connection...");
			URL loginURL = new URL("http://www.spoj.com/login");
			String param = "login_user=" + username + "&password=" + password
					+ "&next_raw=/";
			HttpURLConnection httpCon = (HttpURLConnection) loginURL
					.openConnection();
			System.out.println("OK");

			System.out.print("Sending request...");
			httpCon.setDoOutput(true);
			httpCon.setRequestMethod("POST");
			httpCon.setRequestProperty("Host", "www.spoj.com");
			httpCon.setInstanceFollowRedirects(false);

			OutputStream out = httpCon.getOutputStream();
			out.write(param.getBytes());
			out.close();
			System.out.println("OK");
			return httpCon;
		} catch (MalformedURLException e) {
			System.out.println("Error occured: " + e.toString());
			System.exit(0);
		} catch (IOException e) {
			System.out.println("Error occured: " + e.toString());
			System.exit(0);
		}
		return null;
	}

	private static String readCookies(HttpURLConnection con) throws IOException {
		String headerName = null;
		for (int i = 1; (headerName = con.getHeaderFieldKey(i)) != null; i++) {
			if (headerName.equals("Set-Cookie")) {
				return con.getHeaderField(i);
			}
		}
		return "";
	}

	private static void getInfo() {
		System.out.println("Input destination directory: ");
		destination = console.readLine();
	}

	private static void readData() {
		try {
			source = "http://vn.spoj.com/status/" + username + "/signedlist";
			HttpURLConnection httpCon = (HttpURLConnection) new URL(source)
					.openConnection();
			httpCon.setRequestMethod("GET");
			httpCon.setRequestProperty("Cookie", spojcookie);
			httpCon.setRequestProperty("Connection", "Keep-Alive");
			httpCon.setRequestProperty("Host", "vn.spoj.com");
			httpCon.setRequestProperty("Referer", "http://www.spoj.com/login");
			httpCon.connect();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					httpCon.getInputStream()));
			String inputLine;
			sourceHTML = "";
			while ((inputLine = br.readLine()) != null) {
				sourceHTML += inputLine;
			}
			br.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String downloadCodeOf(String id) {
		String code = "";
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(
					"http://vn.spoj.com/files/src/save/" + id).openConnection();

			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.setRequestProperty("Cookie", spojcookie);
			conn.connect();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				code += inputLine + "\n";
			}
			br.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return code;
	}

	private static boolean saveToFile(String path, String fileName,
			String content) {
		File fpath = new File(path);
		if (!fpath.isDirectory()) {
			fpath.mkdirs();
		}
		try {
			File f = new File(path + fileName);
			PrintWriter pw = new PrintWriter(f);
			pw.write(content);
			pw.flush();
			pw.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void main(String args[]) throws IOException {
		login();
		if (args.length == 0)
			getInfo();
		else
			destination = args[0];
		System.out.print("Loading data from server...");
		readData();
		System.out.println("OK");
		System.out.print("Getting list of solved problems...");
		String[] tempo = sourceHTML.split("\\|");
		for (int i = 0; i < tempo.length; i++)
			eleList.add(tempo[i].trim());
		for (int i = 0; i < eleList.size(); i++) {
			String x = eleList.get(i);
			if (x.equals(""))
				eleList.remove(i);
		}
		int total = 0;
		for (int i = 17; i <= eleList.size(); i += 7)
			if (eleList.get(i + 1).equals("100")
					|| eleList.get(i + 1).equals("AC")) {
				total++;
			}
		System.out.println("OK");
		System.out.println("Start downloading source codes...");
		int cur = 0, err = 0;
		for (int i = 17; i <= eleList.size(); i += 7)
			if (eleList.get(i + 1).equals("100")
					|| eleList.get(i + 1).equals("AC")) {
				String problem = eleList.get(i);
				String extension = (eleList.get(i + 4).equals("C++")) ? "cpp"
						: "pas";
				String srcid = eleList.get(i - 2);
				String fileName = problem + "_" + srcid + "." + extension;
				if (saveToFile(destination + "/" + problem + "/", fileName,
						downloadCodeOf(srcid)))
					System.out.println("    > Downloaded " + (++cur)
							+ " files [" + ((float) cur / total * 100) + "%]");
				else {
					System.out.println("    Error occured while saving "
							+ fileName);
					err++;
				}
			}
		System.out.println("Downloaded " + cur + " files over " + total
				+ "files \n" + "Lost " + err + " files");
	}
}
