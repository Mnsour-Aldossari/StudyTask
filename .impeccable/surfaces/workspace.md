# StudyTask workspace direction contract

Mode: Operate. Native Windows JavaFX. Code-led implementation, based on the user's request to start and delegated design scope. Language confirmed English. No visual comp was approved.

Grounded directions considered: 1 university course catalog (structured reading), 2 library subject index (clear categorization), 3 assessment rubric (aligned comparable data), 4 weekly student planner (date rhythm), 5 campus publication (type discipline), 6 campus wayfinding (destination, course, timing), 7 seminar handout (quiet annotation). Assignment 6 from Impeccable seed e2a9168e is used.

Campus wayfinding lends cool navy, clear blue, an amber timing signal, readable Segoe UI, and a deadline cell pairing the calendar date with a plain-language relative day. It does not replace native controls or make the app imitate a sign. Sidebar lists actual courses, table shows actual tasks, adjacent editor keeps context. No invented analytics or fake overview tiles.

Challenger judgment: timetable slide rack declined (raked glass harms scanning); Crouwel grid competitive on alignment but too rigid for long titles; design annual declined (display-first); boarding pass competitive on deadline readability but too travel-specific; zoo map declined (map topology adds no task value); emission rail declined (monitoring semantics do not match student work). Adopt discipline of alignment, explicit state and concise deadline semantics, not motifs.

Palette: ink #20384D, shell #233F57, shell secondary #C2D2E1, ground #F3F6FA, white #FFFFFF, action #285D89, selection #E6EFF8, border #D6DFE8, secondary #526579, overdue #9E3C35, success #24624B, due-soon #865D0D.

Type: Segoe UI 13 base; 12 metadata; 16 pane title; 26 page title; 20 brand. 8/12/16/24/32 spacing rhythm. Native controls with 6px corners; no card grid, decorative gradient, shadows or icon emoji.

Layout: 188px course rail; flexible work area with title, search, status controls and task table; 310px scrollable editor. At compact desktop widths rail becomes a course combo above the table, editor narrows to 280px. Minimum window 900x640. Screens tested at 1280x820 and 960x680.

States: empty session with New task and optional Load example tasks; no filter results with clear action; selected task edits; inline error retaining input; explicit deletion confirmation; visible session-only storage note and close confirmation with tasks present. Keyboard Ctrl+N for new, Ctrl+S to save; native Tab navigation and focus rings. No motion needed.

Finish evidence: .impeccable/review/desktop.png, compact.png, empty.png, error.png. UI smoke tests additionally exercise add/edit/delete, priority, status and combined filtering.
