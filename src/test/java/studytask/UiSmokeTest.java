package studytask;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studytask.controller.TaskController;
import studytask.model.*;
import studytask.view.TaskView;

/** Real JavaFX controls and rendering; run explicitly with -Pui-test. */
class UiSmokeTest {
    @BeforeAll static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(20,TimeUnit.SECONDS));
    }

    @Test void completeWorkflowThroughNativeControls() throws Exception {
        FutureTask<Void> test = new FutureTask<>(() -> {
            TaskModel model = new TaskModel(); TaskView view = new TaskView(model);
            boolean[] consent = {false};
            new TaskController(model,view,text -> consent[0]); model.addListener(view);
            Stage stage = new Stage(); Scene scene = new Scene(view,1280,820);
            scene.getStylesheets().add(getClass().getResource("/studytask.css").toExternalForm());
            stage.setScene(scene); stage.show();
            try {
                capture(view,"empty");
                assertTrue(button(view,"delete-task").isDisabled());
                field(view,"task-title").setText("Demo task"); field(view,"task-course").setText("CS-321");
                button(view,"save-task").fire(); assertEquals(1,model.getTasks().size());
                Task original = model.getTasks().getFirst();
                TableView<Task> table = table(view); table.getSelectionModel().select(0);
                field(view,"task-title").setText("Edited title"); button(view,"save-task").fire();
                assertEquals("Edited title",model.getTask(original.getId()).getTitle()); assertEquals("Demo task",original.getTitle());
                priority(view).setValue(Priority.HIGH); button(view,"apply-priority").fire();
                assertEquals(Priority.HIGH,model.getTask(original.getId()).getPriority());
                button(view,"change-status").fire(); assertEquals(TaskStatus.COMPLETED,model.getTask(original.getId()).getStatus());
                toggle(view,"filter-pending").fire(); assertEquals(0,table.getItems().size());
                toggle(view,"filter-completed").fire(); assertEquals(1,table.getItems().size());
                field(view,"search").setText("does not exist"); assertTrue(table.getItems().isEmpty());
                button(view,"clear-filters").fire(); assertEquals(1,table.getItems().size());
                table.getSelectionModel().select(0); button(view,"delete-task").fire(); assertEquals(1,model.getTasks().size());
                consent[0] = true; button(view,"delete-task").fire(); assertTrue(model.getTasks().isEmpty());
                button(view,"load-examples").fire(); assertEquals(6,model.getTasks().size());
                table.getSelectionModel().select(0);
                capture(view,"desktop");
                stage.setWidth(976); stage.setHeight(719); view.resize(960,680); view.applyCss(); view.layout();
                capture(view,"compact");
                @SuppressWarnings("unchecked") ComboBox<String> courses = (ComboBox<String>)view.lookup("#course-filter");
                courses.setValue("SE-4111"); toggle(view,"filter-completed").fire();
                assertEquals(1,table.getItems().size()); assertEquals("Write the project requirements",table.getItems().getFirst().getTitle());
                button(view,"clear-filters").fire();
                button(view,"new-task").fire(); field(view,"task-title").setText("Preserve this title");
                button(view,"save-task").fire(); assertEquals(6,model.getTasks().size());
                assertEquals("Preserve this title",field(view,"task-title").getText());
                assertEquals("Course is required.",((Label)view.lookup("#form-message")).getText());
                capture(view,"error");
                DatePicker date = (DatePicker)view.lookup("#task-deadline"); date.getEditor().setText("not a date");
                button(view,"save-task").fire(); assertTrue(((Label)view.lookup("#form-message")).getText().contains("yyyy-mm-dd"));
                assertEquals(6,model.getTasks().size());
                assertTrue(TaskView.matches(model.getTasks().getFirst()," se-4111 ",TaskStatus.PENDING,"mvc"));
                assertFalse(TaskView.matches(model.getTasks().getFirst(),"CS-321",null,""));
                return null;
            } finally { model.removeListener(view); stage.close(); }
        });
        Platform.runLater(test); test.get(40,TimeUnit.SECONDS);
    }

    private static Button button(TaskView v,String id) { return (Button)v.lookup("#"+id); }
    private static ToggleButton toggle(TaskView v,String id) { return (ToggleButton)v.lookup("#"+id); }
    private static TextField field(TaskView v,String id) { return (TextField)v.lookup("#"+id); }
    @SuppressWarnings("unchecked") private static TableView<Task> table(TaskView v) { return (TableView<Task>)v.lookup("#task-table"); }
    @SuppressWarnings("unchecked") private static ComboBox<Priority> priority(TaskView v) { return (ComboBox<Priority>)v.lookup("#task-priority"); }
    private static void capture(TaskView view,String name) throws Exception {
        view.applyCss(); view.layout();
        Path path = Path.of(".impeccable","review",name+".png"); Files.createDirectories(path.getParent());
        ImageIO.write(SwingFXUtils.fromFXImage(view.snapshot(null,null),null),"png",path.toFile());
    }
}
