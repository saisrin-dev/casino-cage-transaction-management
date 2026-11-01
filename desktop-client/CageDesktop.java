import java.awt.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
import javax.swing.*;

/** Small legacy-client modernization example. Uses the same secured Spring REST API. */
public class CageDesktop {
  final JFrame frame = new JFrame("CageLedger — Legacy Swing Client / Synthetic Data");
  final JTextArea output = new JTextArea();
  final JTextField base = new JTextField("http://localhost:8080", 24);
  final JTextField user = new JTextField("cashier", 12);
  final JPasswordField password = new JPasswordField(12);
  final HttpClient client =
      HttpClient.newBuilder()
          .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
          .build();
  String token;

  String request(String path, String body, String type) throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base.getText() + path));
    if (body != null)
      b.header("Content-Type", type)
          .header("X-CSRF-TOKEN", token)
          .POST(HttpRequest.BodyPublishers.ofString(body));
    var r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    if (r.statusCode() >= 400) throw new Exception("HTTP " + r.statusCode() + ": " + r.body());
    return r.body();
  }

  String csrf() throws Exception {
    String text = request("/api/csrf", null, null);
    Matcher m = Pattern.compile("\"token\"\\s*:\"([^\"]+)\"").matcher(text);
    if (!m.find()) throw new Exception("Missing CSRF token");
    return m.group(1);
  }

  void background(java.util.concurrent.Callable<String> task) {
    new SwingWorker<String, Void>() {
      protected String doInBackground() throws Exception {
        return task.call();
      }

      protected void done() {
        try {
          output.setText(get());
        } catch (Exception e) {
          output.setText(e.getMessage());
        }
      }
    }.execute();
  }

  void show() {
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1000, 650);
    JPanel controls = new JPanel();
    controls.add(new JLabel("API"));
    controls.add(base);
    controls.add(new JLabel("User"));
    controls.add(user);
    controls.add(new JLabel("Password"));
    controls.add(password);
    JButton login = new JButton("Sign in"),
        patrons = new JButton("Fetch patrons"),
        tx = new JButton("Fetch transactions"),
        logout = new JButton("Sign out");
    controls.add(login);
    controls.add(patrons);
    controls.add(tx);
    controls.add(logout);
    login.addActionListener(
        e ->
            background(
                () -> {
                  token = csrf();
                  String body =
                      "username="
                          + URLEncoder.encode(user.getText(), StandardCharsets.UTF_8)
                          + "&password="
                          + URLEncoder.encode(
                              new String(password.getPassword()), StandardCharsets.UTF_8);
                  request("/api/login", body, "application/x-www-form-urlencoded");
                  token = csrf();
                  SwingUtilities.invokeLater(() -> password.setText(""));
                  return "Signed in. Use the web application for transaction entry.\n"
                      + request("/api/me", null, null);
                }));
    patrons.addActionListener(e -> background(() -> request("/api/patrons", null, null)));
    tx.addActionListener(e -> background(() -> request("/api/transactions", null, null)));
    logout.addActionListener(
        e ->
            background(
                () -> {
                  request("/api/logout", "", "application/x-www-form-urlencoded");
                  return "Signed out";
                }));
    output.setEditable(false);
    output.setLineWrap(true);
    output.setWrapStyleWord(true);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    output.setText("SYNTHETIC DEMO — REST-backed Swing client. Never use real patron data.");
    frame.add(controls, BorderLayout.NORTH);
    frame.add(new JScrollPane(output), BorderLayout.CENTER);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new CageDesktop().show());
  }
}
