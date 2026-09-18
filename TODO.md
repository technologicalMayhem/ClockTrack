A list of things I still need to do, apart from unimplemented features:

- [ ] Overhaul the README. It still reflects the old project plan.
- [ ] Remove `System.exit(..)` calls. They appear in the Database class and are an artifact of the initial 'make it
  work' phase. They need to be replaced by a more robust system. Especially because log messages at this point are no
  longer visible directly to the user since I switch to a full GUI approach.
- [ ] Think whether Java 27 (what I am using right now) is a sensible version to go with. It is the latest stable
  release and I picked it since I am rather familiar with modern Java patterns. The project doc only required 17+ so
  technically I meet that criteria. However, it might be sensible to switch to a LTS version like 26 or even 21, if
  possible.
- [ ] Go over the Database code once more. The handling of some SQL is inconsistent I think. In particular the select
  all methods. Reading some best practices for JDBC would be good.
- [ ] Include sample data for the presentation
- [ ] Think about a proper form of deployment. Right now I am using javafx's jlink. I have no idea how 'proper' this is
  though. Maybe a single "fat jar" might be better? The deployment should be sensible for the project and simple to get
  running from just a repository state. It should also be relatively easy to share a copy of the app once compiled.