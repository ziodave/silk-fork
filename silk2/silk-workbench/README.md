Needed plugins in your `~/.sbt/0.13/plugins/plugins.sbt` file:

```
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7.0-RC2")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.3")

addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.2-beta4")
```

Try `mvn clean install` although it might yield errors.

In `target/universal` find the `silk-workbench-2.6.0-SNAPSHOT.zip` distribution file.