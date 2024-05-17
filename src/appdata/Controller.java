package appdata;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Controller {

    public String URL = "";
    public String APIKey = "";
    public static String name;
    public TextField checkoutTextBox;
    public Text checkoutText;
    public TextField checkinTextBox;
    public Button checkoutSubmitBtnID;
    public String employeeID;
    public ListView<String> changeList;
    public final AtomicInteger INDEX = new AtomicInteger(0);
    public static final Queue<String> queue= new LinkedList<>();
    public TabPane tabPane;
    public Text employeeIDError;
    public Text assetTagError;
    public ListView employeeList;
    public TextField employeeSearch;


    public Controller() throws IOException {
        readINI();
        String username = "KS1-Check-Bot", password = "1!NDB[?86bfm?gV5";
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url(URL + "/hardware/3656/qr_code")
                .get()
                .header("content-type", "application/json")
                //.addHeader("Authorization", Credentials.basic(username, password))
                .post(formBody)
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.headers().toString());
        System.out.println(response.body().string());
        System.out.println();

//        try {
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url(URL + "/api/v1/reports/activity?limit=2&offset=0")
//                    .get()
//                    .addHeader("Accept", "application/json")
//                    .addHeader("Authorization", "Bearer " + APIKey)
//                    .build();
//            Response response = client.newCall(request).execute();
//            response.close();
//        } catch(Exception e) {
//            Alert alert = new Alert(Alert.AlertType.WARNING);
//            alert.setTitle("Cannot Find Server Host");
//            alert.setHeaderText("Please make sure you are can access the host: \n" + URL);
//            alert.setContentText("Tips: Make sure the server URL matches the URL in the config file\nor Try using a different network connection.");
//            alert.showAndWait();
//            System.exit(1);
//        }
        while(!getEmployeeName(name)) {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("[" + Main.versionNum + "] Snipe IT Authorization");
            dialog.setHeaderText("Snipe IT Authorization");
            dialog.setContentText("Please scan your Badge:");
            Stage popup = (Stage) dialog.getDialogPane().getScene().getWindow();
            popup.getIcons().add(Main.icon);
            Optional<String> result = dialog.showAndWait();
            if(!result.isPresent())
                System.exit(0);
            name = result.get();
            dialog.getDialogPane().setDisable(true);
        }
        thread.start();
        Platform.runLater(search::start);
        checkoutBtnThread.start();
        Platform.runLater(() -> checkoutTextBox.requestFocus());
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    }

    private void readINI() {
        try {
            File myObj = new File("src/appdata/config.ini");
            Scanner myReader = new Scanner(myObj);
            URL = myReader.nextLine();
            APIKey = myReader.nextLine();
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private String getEmployeeID(String id) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL + "/api/v1/users/selectlist?search=" + id)
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + APIKey)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        String userID = response.body().string();
        response.close();
//        System.out.println(userID);
        if (userID.contains("\"total_count\":0"))
            return "No User Found";
        return userID.substring(userID.indexOf("id")+4, userID.indexOf("text")-2);
    }

    private boolean getEmployeeName(String id) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL + "/api/v1/users/selectlist?search=" + id)
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + APIKey)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        String userID = Objects.requireNonNull(response.body()).string();
        response.close();
        if (userID.contains("\"total_count\":0"))
            return false;
        String strCount =  userID.substring(userID.indexOf("\"total_count\":") + 14);
        strCount = strCount.substring(0,strCount.indexOf(","));
//        System.out.println(strCount);
        int count = Integer.parseInt(strCount);
        if (count == 1) {
            name = userID.substring(userID.indexOf("text") + 7, userID.indexOf("(") - 1);
            return true;
        }
        else {
            name = chooseAnEmployee(userID);
            return !name.isEmpty();
        }
    }
    public static String chooseAnEmployee(String userID) {
        userID = userID.substring(13);
        String[] users = userID.split("},\\{");
        ArrayList<String> optionList = new ArrayList<>();
        for (String user : users) {
            user = user.substring(user.indexOf("text")+7,user.indexOf("\",\""));
            optionList.add(user);
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(optionList.get(0), optionList);
        dialog.setTitle("Possible Duplicates");
        dialog.setHeaderText("More than 1 user found");
        dialog.setContentText("Choose a user:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            System.out.println("Your choice: " + result.get());
            return result.get();
        }
        return "";
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    private String getAssetID(String assetTag) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL + "/api/v1/hardware/bytag/"+assetTag)
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + APIKey)
                .build();
        Response response = client.newCall(request).execute();
        String assetID = response.body().string();
        response.close();
//        System.out.println(assetID);
        if (assetID.contains("Asset not found"))
            return "Asset not found";
        //asset = temp;
        return assetID.substring(assetID.indexOf("id")+4, assetID.indexOf("name")-2);
    }

    public boolean checkIn(String assetTag) throws IOException {
        assetTag = assetTag.substring(assetTag.indexOf(" ")+1);
        assetTag = assetTag.substring(assetTag.indexOf(" ")+1);
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"note\":\"Checked in by " + name + "\"}");
        Request request = new Request.Builder()
                .url(URL + "/api/v1/hardware/" + getAssetID(assetTag) + "/checkin")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + APIKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        String outcome = response.body().string();
        response.close();
        System.out.println(outcome);
        outcome = outcome.substring(outcome.indexOf("messages")+11, outcome.indexOf("payload")-3);
        System.out.println(assetTag + " " + outcome);
        return outcome.equals("Asset checked in successfully.");
    }

    private boolean checkOut(String checkOutTransaction, boolean reChecked) throws IOException {
        String original = checkOutTransaction;
        checkOutTransaction = checkOutTransaction.substring(checkOutTransaction.indexOf("Checking out ")+13);
        String assetTag = checkOutTransaction.substring(0, checkOutTransaction.indexOf(" "));
        checkOutTransaction = checkOutTransaction.substring(checkOutTransaction.indexOf(" ") + 1);
        String employeeID = checkOutTransaction.substring(checkOutTransaction.indexOf(" ") + 1, checkOutTransaction.indexOf("\""));
        employeeID = getEmployeeID(employeeID);
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"checkout_to_type\":\"user\",\"note\":\"Checked out by " + name + "\",\"assigned_user\":" + employeeID + "}");
        Request request = new Request.Builder()
                .url(URL + "/api/v1/hardware/" + getAssetID(assetTag) + "/checkout")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + APIKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        String outcome = response.body().string();
        response.close();
        outcome = outcome.substring(outcome.indexOf("messages")+11, outcome.indexOf("payload")-4);
        System.out.println(outcome);
        if (outcome.equals("That asset is not available for checkout") && !reChecked) {
            checkIn(assetTag);
            if (checkOut(original, true)) {
                return true;
            }
        }
        return outcome.equals("Asset checked out successfully");
    }

    public final Thread checkoutBtnThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(5);
                    Platform.runLater(() -> {
                        if (checkoutTextBox.getText().isEmpty() && checkoutText.getText().equals("Scan Asset Tag"))
                            checkoutSubmitBtnID.setText("Next Employee ID");
                        else {
                            checkoutSubmitBtnID.setText("Submit");
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public final Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    TimeUnit.SECONDS.sleep(1);
                    if (!queue.isEmpty()) {
                        if(queue.peek().contains("Checking in ")) {
                            if(checkIn(queue.peek())) {
                                Platform.runLater(() -> {
                                    changeList.getItems().remove(INDEX.get());
                                    queue.remove();
                                });
                            } else {
                                Platform.runLater(() -> {
                                    changeList.getItems().set(INDEX.get(), "Error " + queue.peek());
                                    INDEX.getAndIncrement();
                                    queue.remove();
                                });
                            }
                        } else if (queue.peek().contains("Checking out ")) {
                            if(checkOut(queue.peek(), false)) {
                                Platform.runLater(() -> {
                                    changeList.getItems().remove(INDEX.get());
                                    queue.remove();
                                });
                            } else {
//                                System.out.println(queue.peek());
                                String listEntry = "Error " + changeEntryToName(queue.peek());
//                                System.out.println(listEntry);
                                Platform.runLater(() -> {
                                    System.out.println(listEntry);
                                    System.out.println(INDEX.get());
                                    System.out.println(Arrays.toString(changeList.getItems().toArray()));
                                    changeList.getItems().set(INDEX.get(), listEntry);
                                    INDEX.getAndIncrement();
                                    queue.remove();
                                });
                            }
                        }
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    });

    private String changeEntryToName(String entry) throws IOException {
        entry = entry.substring(13);
        String ast = entry.substring(0, entry.indexOf(" "));
        entry = entry.substring(entry.indexOf(" ")+1);
        entry = entry.substring(entry.indexOf(" ")+1);
        getEmployeeName(entry);
        entry = name;
//        System.out.println(entry);
        return "Checking out " + ast + " to " + entry;
    }

    public void textClear() {
        checkoutText.setText("Scan Employee ID");
        employeeID = "";
        if(tabPane.getSelectionModel().getSelectedIndex() == 1)
            Platform.runLater(() -> checkinTextBox.requestFocus());
        else if(tabPane.getSelectionModel().getSelectedIndex() == 0)
            Platform.runLater(() -> checkoutTextBox.requestFocus());
        else {
            employeeSearch.setText("");
            employeeList.getItems().clear();
            Platform.runLater(() -> employeeSearch.requestFocus());
        }
    }

    public void checkinSubmit() {
        if(!checkinTextBox.getText().isEmpty()) {
            String checkinInput = "Checking in " + checkinTextBox.getText();
            changeList.getItems().add(checkinInput);
            queue.add(checkinInput);
            checkinTextBox.setText("");
        }
        Platform.runLater(() -> checkinTextBox.requestFocus());
    }


    public void checkoutSubmit() throws IOException {
        if (checkoutText.getText().equals("Scan Employee ID") && !checkoutTextBox.getText().isEmpty()) {
            if (getEmployeeName(checkoutTextBox.getText())) {
                employeeIDError.setVisible(false);
                employeeID = checkoutTextBox.getText();
                checkoutText.setText("Scan Asset Tag");
            } else {
                employeeIDError.setVisible(true);
            }
            checkoutTextBox.clear();
        } else if(checkoutTextBox.getText().isEmpty()) {
            checkoutText.setText("Scan Employee ID");
            checkoutTextBox.clear();
        }
        else if (checkoutText.getText().equals("Scan Asset Tag")) {
            if(!getAssetID(checkoutTextBox.getText()).equals("Asset not found")) {
                queue.add("Checking out " + checkoutTextBox.getText() + " to " + employeeID);
                changeList.getItems().add("Checking out " + checkoutTextBox.getText() + " to " + employeeID);
                assetTagError.setVisible(false);
            }
            else {
                assetTagError.setVisible(true);
            }
            checkoutTextBox.clear();
        }
    }

    public void checkoutSubmitBtn() throws IOException {
        if (checkoutText.getText().equals("Scan Asset Tag") && checkoutTextBox.getText().isEmpty()) {
            checkoutText.setText("Scan Employee ID");
        } else
            checkoutSubmit();
        Platform.runLater(() -> checkoutTextBox.requestFocus());
    }

    public void mouseClickListView() {
        String selected = changeList.getSelectionModel().getSelectedItem();
        if(selected != null && Objects.requireNonNull(selected).contains("Error")) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("A Closer Look");
            alert.setHeaderText("Do you want to clear this Error?");
            alert.setContentText(selected);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                changeList.getItems().remove(selected);
                INDEX.getAndDecrement();
            }
        }
    }

    public final Thread search = new Thread(new Runnable() {
        @Override
        public void run() {
            String search = employeeSearch.getText();
            while(true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                    if (!employeeSearch.getText().isEmpty()) {
                        if (!search.equals(employeeSearch.getText())) {
                            search = employeeSearch.getText();
                            OkHttpClient client = new OkHttpClient();
                            //search = search.replace(" ", "%20");
                            Request request = new Request.Builder()
                                    .url(URL + "/api/v1/users?search=" + search + "&limit=10&offset=0&sort=created_at&order=desc&deleted=false&all=false")
                                    .get()
                                    .addHeader("Accept", "application/json")
                                    .addHeader("Authorization", "Bearer " + APIKey)
                                    .addHeader("Content-Type", "application/json")
                                    .build();
                            Response response = client.newCall(request).execute();
                            String strResponse = Objects.requireNonNull(response.body()).string();
                            response.close();
                            strResponse = strResponse.substring(strResponse.indexOf("rows")+8);
                            String[] userList = strResponse.split("},\\{");
                            Platform.runLater(()->employeeList.getItems().clear());
                            for (String userID : userList) {
                                if (userID.contains("id")) {
                                    String finalUserID = userID.substring(userID.indexOf("username") + 11, userID.indexOf("employee_num") - 3);
                                    Platform.runLater(() -> employeeList.getItems().add(finalUserID));
                                }
                            }
                        }
                        search = employeeSearch.getText();
                    }
                    search = employeeSearch.getText();
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public void clearList() {
        if(!changeList.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("A Closer Look");
            alert.setHeaderText("Do you want to clear ALL ERRORS?");
            //alert.setContentText(selected);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                ObservableList<String> ol = changeList.getItems();
                for (String list : ol) {
                    if(list.contains("Error")) {
                        Platform.runLater(() -> changeList.getItems().remove(list));
                        INDEX.getAndDecrement();
                    }
                }
            }
        }
    }

    //employeeList
    public void mouseClickNameList(MouseEvent mouseEvent) {
        String selected;
        if(mouseEvent.getClickCount() >= 2) {
            selected = (String) employeeList.getSelectionModel().getSelectedItem();
            tabPane.getSelectionModel().select(0);
            employeeID = selected;
            checkoutText.setText("Scan Asset Tag");
        }

    }
}
