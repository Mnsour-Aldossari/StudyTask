# StudyTask

<!-- impeccable:product-schema 1 -->

## Platform
Windows desktop application, rendered natively with JavaFX. This is not a web or mobile app.

## Stack
Java 21, JavaFX, Maven. Continues the design accepted for implementation in this chat.

## Users and purpose
University students organizing assignments, projects and exams in one place. Success means adding a task quickly, finding work by course or status, and updating it without losing context.

## Capabilities and constraints
Seven requirements: add, view, edit, delete, update completion status, change priority, filter by course and status. Task includes title, course, deadline, priority, description and status. Course project requires MVC, Observer, an immutable Task ADT, RI/AF and explicit equals/hashCode. Tasks are saved locally after each change and restored on startup.

## Operating context
Personal study on a Windows laptop. Existing course report is in English. User explicitly chose an English interface. No accounts, network service or commercial claims. Demo data must be explicitly labeled and opt-in.

## Brand commitments
StudyTask name. User requests deliberate, distinctive design, avoiding generic AI-generated layouts. An operative desktop interface must remain easy to scan and use.

## Evidence
The course documents are kept locally and excluded from the public repository. DESIGN.md records the accepted software architecture. No existing UI or visual assets.

## Product principles
Keep the task list visible during editing. Preserve input on errors. Express status and priority with words as well as color. Keep UI independent from domain validation. Use keyboard-accessible native controls.
