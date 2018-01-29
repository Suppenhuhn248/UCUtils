package de.fuzzlemann.ucutils.utils.teamspeak;

import de.fuzzlemann.ucutils.utils.io.JsonManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Fuzzlemann
 */
public class TSClientQuery {

    public static final File API_KEY_FILE = new File(JsonManager.DIRECTORY, "tsapikey.storage");
    public static String apiKey;

    private static Socket socket;
    private static BufferedReader bufferedReader;
    private static PrintWriter printWriter;

    public static Map<String, String> exec(String command) {
        return exec(command, false);
    }

    private static Map<String, String> exec(String command, boolean auth) {
        String result = rawExec(command, auth);
        if (result == null) return null;

        return ResultParser.parse(result);
    }

    public static String rawExec(String command, boolean auth) {
        return rawExec(command, auth, true);
    }

    private static String rawExec(String command, boolean auth, boolean tryAgain) {
        if (!auth && !connect()) return null;

        String result;
        try {
            while (bufferedReader.ready()) bufferedReader.readLine();
            printWriter.println(command);

            result = bufferedReader.readLine();
            if (tryAgain && result != null && result.equals("error id=1796 msg=currently\\snot\\spossible")) {
                auth();
                return rawExec(command, auth, false);
            }
        } catch (IOException e) {
            if (e instanceof SocketException) {
                connect(false);
                return rawExec(command, auth, false);
            }

            e.printStackTrace();
            return null;
        }

        return result;
    }

    private static boolean connect() {
        return connect(true);
    }

    private static boolean connect(boolean check) {
        if (check && socket != null && !socket.isClosed()) return true;

        try {
            socket = new Socket("127.0.0.1", 25639);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            while (bufferedReader.ready()) bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return auth();
    }

    public static boolean auth() {
        if (apiKey == null) return false;

        Map<String, String> result = exec("auth apikey=" + apiKey, true);
        if (result == null) return false;
        String msg = result.get("msg");

        return msg != null && msg.equals("ok");
    }
}
