package studytask;

import java.io.IOException;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import studytask.controller.TaskController;
import studytask.model.TaskModel;
import studytask.persistence.TaskStore;
import studytask.view.TaskView;

public final class StudyTaskApp extends Application {
    private TaskModel model;
    private TaskView view;

    @Override public void start(Stage stage) {
        String override = System.getProperty("studytask.dataFile");
        TaskStore store = override == null ? TaskStore.defaultStore() : new TaskStore(Path.of(override));
        try {
            model = new TaskModel(store);
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("StudyTask");
            alert.setHeaderText("Saved tasks could not be opened");
            alert.setContentText("Your data file was not changed. Check or restore this file before reopening:\n" + store.file());
            alert.showAndWait();
            Platform.exit();
            return;
        }
        view = new TaskView(model);
        TaskController controller = new TaskController(model, view);
        model.addListener(view);
        Scene scene = new Scene(view, 1280, 820);
        scene.getStylesheets().add(getClass().getResource("/studytask.css").toExternalForm());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), controller::newTask);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), controller::save);
        stage.setTitle("StudyTask"); stage.setMinWidth(900); stage.setMinHeight(640); stage.setScene(scene);
        stage.show();
        if (Boolean.getBoolean("studytask.smoke")) {
            Platform.runLater(() -> {
                System.out.println("StudyTask startup verified");
                stage.setOnCloseRequest(null);
                stage.close();
                Platform.exit();
            });
        }
    }

    @Override public void stop() { if (model != null && view != null) model.removeListener(view); }
    public static void main(String[] args) { launch(args); }
}
