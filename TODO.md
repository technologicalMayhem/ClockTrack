# Project Completion

- [X] Fix the per-cell database lookups through projections and to extend overviews
- [ ] Implement messages and decide behavior when trying to delete a person that appears in games
- [ ] Implement player and game statistics
- [ ] Implement game filtering (by character appearance, specific player + character combo etc.)
- [ ] Implement localization
- [X] None of the editors right now give any feedback about what is wrong with the entered data beyond not allowing you
  to submit. There should be an indicator showing what is wrong.
- [ ] Include sample data for the presentation (check for requirements)
- [ ] Document symbols
- [ ] Think about a proper form of deployment. Right now I am using javafx's jlink. I have no idea how 'proper' this is
  though. Maybe a single "fat jar" might be better? The deployment should be sensible for the project and simple to get
  running from just a repository state. It should also be relatively easy to share a copy of the app once compiled.
- [X] Think about tinylog. Does it have a place in the project or should it be removed?
- [ ] Think about Person. Should it perhaps be renamed to player? I often refer it to that anyway so the name has become
  awkward. Furthermore, it currently has no real editor whilst being the most likely target for the 'live extension'
  part of the project. It should have a proper editor dialog like the others and perhaps a couple more fields (email,
  notes)
- [ ] Write a project documentation
- [X] Get rid of any remaining magic numbers. Like UI sizing constants.
- [X] Fix the text dialogs accept button. It only listens to key-typed events, so pasting does not update it, and a whitespace-only name passes the empty check. Bind it to the text property and use isBlank().