import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
  java
  kotlin("jvm") version "1.9.22"
  `maven-publish`
  id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "xyz.block"
version = "1.1-SNAPSHOT"

kotlin {
  jvmToolchain(21)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))

  testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
  testImplementation("org.assertj:assertj-core:3.24.2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  signAllPublications()

  configure(
    JavaLibrary(
      javadocJar = JavadocJar.Javadoc(),
      sourcesJar = true,
    )
  )

  pom {
    name.set("UUIDv7")
    description.set("A minimal UUID v7 implementation for Java with Kotlin bindings")
    url.set("https://github.com/block/uuidv7")

    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("repo")
      }
    }
    developers {
      developer {
        id.set("block")
        name.set("Block")
      }
    }
    scm {
      url.set("https://github.com/block/uuidv7/")
      connection.set("scm:git:https://github.com/block/uuidv7.git")
      developerConnection.set("scm:git:ssh://git@github.com/block/uuidv7.git")
    }
  }
}