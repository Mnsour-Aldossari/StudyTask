# StudyTask

A JavaFX desktop app for university assignments, projects and exams. Implements the seven course requirements using MVC, an Observer interface and an immutable Task ADT.

## طريقة التشغيل

- **المتطلبات:** JDK 21 أو أحدث، وMaven مثبت ومتاح من سطر الأوامر.
- **البناء:** `mvn clean package`
- **التشغيل:** `.\run.ps1`
- **الاختبارات:** `.\run.ps1 -Test`
- **بيانات المهام:** تُحفظ تلقائيًا في `%APPDATA%\StudyTask\tasks.xml` على Windows.

يستخدم `run.ps1` Maven بالوضع العادي افتراضيًا، فيحمّل الاعتمادات الناقصة إلى `.maven-cache` عند توفر الإنترنت. للتشغيل دون إنترنت بعد اكتمال الاعتمادات، استخدم `.\run.ps1 -Offline` أو `.\run.ps1 -Test -Offline`.

## Portable app

Open `Start-StudyTask.cmd` in the portable folder. Keep that entire folder together: it includes the Java runtime and JavaFX libraries. No Java installation is needed for that packaged copy. On Windows, place the portable folder under a path containing only English letters and digits, such as `C:\StudyTask`; this JDK's native launcher fails to start from the Arabic path of this source project.

For development, the script builds the sources and runs them using the installed JDK. Set `JAVA_HOME` to your JDK if necessary. JavaFX dependency setup follows the [official OpenJFX Maven guide](https://openjfx.io/openjfx-docs/maven).

## Use

Enter a title, course and deadline, select a priority, then click **Add task**. A description is optional. Select a task row to edit it, change its priority, mark it completed/pending or delete it. Course navigation, status tabs and search combine to narrow the list. **Clear filters** restores all tasks. Ctrl+N starts a new task; Ctrl+S saves the current form. Dates use `yyyy-mm-dd` or the calendar picker.

The empty screen offers **Load example tasks** for a clearly labeled demonstration session. Normal startup is empty when there are no saved tasks. Every task change is saved automatically and loaded again when the app opens. On Windows, the data file is `%APPDATA%\StudyTask\tasks.xml`; it stays on the computer even if the portable app folder is moved or replaced. If that file cannot be read or written, the app reports the error without silently discarding the change.

## Tests

```powershell
.\run.ps1 -Test
.\run.ps1 -UiTest
```

The normal suite tests the model and storage, including restart, Unicode data, deletion, corrupt files and failed writes. The UI profile adds one complete workflow test on real JavaFX controls, including add, edit, priority, status, combined filters, cancelled/confirmed deletion, invalid input and empty states. It needs an interactive Windows desktop. It writes scene captures to `.impeccable/review/` at desktop and compact sizes. These are programmatic JavaFX integration tests, not OS input automation.

## Structure

- `model/Task.java`: immutable fields, constructor-enforced RI, documented AF, identity equality and hashCode.
- `model/TaskModel.java`: mutable ordered collection, unique ID allocation, defensive snapshots, no-op suppression and observer notifications. No JavaFX imports.
- `persistence/TaskStore.java`: automatic local XML storage with validated loading and atomic file replacement.
- `model/TaskListener.java`: observer interface implemented by the view.
- `view/TaskView.java`: JavaFX controls, presentation filters, responsive layout and model observation.
- `controller/TaskController.java`: UI commands, error reporting and deletion confirmation.
- `StudyTaskApp.java`: composition root, keyboard shortcuts and app lifecycle.
- `src/main/resources/studytask.css`: native JavaFX visual styles.
- `DESIGN.md`: course architecture and shipped visual system. `PRODUCT.md`: product scope.

Task equality identifies the same logical task across immutable versions. Content comparison is separate so a changed title is not mistaken for a no-op. The collection uses Integer keys; equality is also exercised by task selection and by HashSet contract tests. The next ID is saved with the tasks, so IDs are not reused after reopening.

## Package

```powershell
.\run.ps1 -Package
.\package.ps1
```

Packaging creates a portable folder with a trimmed Java runtime using JDK `jlink`. To move the app to another Windows x64 machine, copy its entire generated folder to an English-only path. The packaging script preserves an existing destination and asks for a different `-Destination` on a subsequent build.

## Verification recorded

2026-10-24: nine model tests, three persistence tests and the JavaFX workflow test passed. The packaged application also started with its bundled runtime. Screens captured at 1280x820 and 960x680, plus empty and error states. JUnit's classpath-based JavaFX test startup prints an unnamed-module warning; it did not fail the tests.

The report's Implementation and Lessons learned sections still need the final course write-up.
