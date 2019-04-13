
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.flywaydb.gradle.FlywayExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
  val kotlinVersion = "1.3.21"

  id("com.github.ben-manes.versions") version "0.21.0"
  id("org.flywaydb.flyway") version "5.2.4" apply false
  id("org.jetbrains.kotlin.jvm") version kotlinVersion apply false
  id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion apply false
  id("org.jlleitschuh.gradle.ktlint") version "7.3.0" apply false
  id("org.springframework.boot") version "2.1.4.RELEASE" apply false
}

val arrowVersion by extra("0.9.0")
val jacksonKotlinVersion by extra( "2.9.8")
val jasyptVersion by extra("1.9.2")
val javaVersion by extra("1.8")
val jaxbVersion by extra("2.3.1")
val jjwtVersion by extra("0.9.1")
val ktlintVersion by extra("0.31.0")
val kotlinVersion by extra("1.3.21")
val restAssuredVersion by extra("3.3.0")
val slugifyVersion by extra("2.3")
val springBootVersion by extra("2.1.4.RELEASE")

class Libs {
  val arrowCoreData = "io.arrow-kt:arrow-core-data:$arrowVersion"
  val arrowCoreExt = "io.arrow-kt:arrow-core-extensions:$arrowVersion"
  val arrowEffectsData = "io.arrow-kt:arrow-effects-data:$arrowVersion"
  val arrowEffectsExt = "io.arrow-kt:arrow-effects-extensions:$arrowVersion"
  val arrowEffectsExtIO = "io.arrow-kt:arrow-effects-io-extensions:$arrowVersion"
  val arrowExtrasData = "io.arrow-kt:arrow-extras-data:$arrowVersion"
  val arrowExtrasExt = "io.arrow-kt:arrow-extras-extensions:$arrowVersion"
  val arrowSyntax = "io.arrow-kt:arrow-syntax:$arrowVersion"
  val arrowTypeclasses = "io.arrow-kt:arrow-typeclasses:$arrowVersion"
  val jacksonKotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonKotlinVersion"
  val jasypt = "org.jasypt:jasypt:$jasyptVersion"
  val jaxb = "javax.xml.bind:jaxb-api:$jaxbVersion"
  val jjwt = "io.jsonwebtoken:jjwt:$jjwtVersion"
  val junitJupiterApi = "org.junit.jupiter:junit-jupiter-api"
  val junitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine"
  val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
  val kotlinStd = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
  val postgresql = "org.postgresql:postgresql"
  val restassured = "io.rest-assured:rest-assured:$restAssuredVersion"
  val slugify = "com.github.slugify:slugify:$slugifyVersion"
  val springBootDevtools = "org.springframework.boot:spring-boot-devtools"
}
val libs by extra(Libs())

class Starters {
  val actuator = "org.springframework.boot:spring-boot-starter-actuator"
  val jdbc = "org.springframework.boot:spring-boot-starter-jdbc"
  val test = "org.springframework.boot:spring-boot-starter-test"
  val undertow = "org.springframework.boot:spring-boot-starter-undertow"
  val web = "org.springframework.boot:spring-boot-starter-web"
}
val starters by extra(Starters())

configure(subprojects.apply {
  remove(project(":realworld-app"))
  remove(project(":realworld-infra"))
}) {
  apply(plugin = "io.spring.dependency-management") // this makes e,g, flyway tasks work
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jetbrains.kotlin.plugin.spring")
  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  version = "0.0.1-SNAPSHOT"

  repositories {
    jcenter()
    mavenCentral()
  }

  configurations {
    all {
      exclude(module = "kotlin-stdlib-jdk7")
      exclude(module = "kotlin-stdlib-jre7")

      resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
          useVersion(kotlinVersion)
          because("use single kotlin version")
        }
      }
    }
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = javaVersion
      freeCompilerArgs = listOf(
        "-Xjsr305=strict",
        "-XXLanguage:+InlineClasses"
      )
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    outputs.upToDateWhen { false }

    testLogging {
      events("passed", "failed", "skipped")
      exceptionFormat = TestExceptionFormat.FULL
    }
  }

  configure<KtlintExtension> {
    version.set(ktlintVersion)
  }

  dependencies {
    "implementation".let {
      it(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

      it(libs.arrowCoreData)
      it(libs.arrowCoreExt)
      it(libs.arrowTypeclasses)
      it(libs.arrowSyntax)
      it(libs.arrowEffectsData)
      it(libs.arrowEffectsExt)
      it(libs.arrowEffectsExtIO)
      it(libs.arrowExtrasData)
      it(libs.arrowExtrasExt)
      it(libs.kotlinStd)
      it(libs.kotlinReflect)
    }

    "runtime"(libs.jaxb)

    "testImplementation".let {
      it(starters.test) {
        exclude(group = "junit", module = "junit")
      }

      it(libs.junitJupiterApi)
      it(libs.junitJupiterEngine)
    }
  }
}

project("realworld-app:web") {
  apply(plugin = "org.springframework.boot")
  apply(plugin = "org.flywaydb.flyway")

  configure<FlywayExtension> {
    url = "jdbc:postgresql://localhost:5432/realworld"
    user = "postgres"
    password = "secret"
    placeholders = mapOf(
      "application_user" to "realworld"
    )
  }

  dependencies {
    "implementation".let {
      it(project(":realworld-domain"))
      it(project(":realworld-infra:persistence"))

      it(starters.actuator)
      it(starters.jdbc)
      it(starters.web) {
        exclude(
          group = "org.springframework.boot",
          module = "spring-boot-starter-tomcat"
        )
      }
      it(starters.undertow)

      it(libs.jacksonKotlin)
    }

    "runtime"(libs.postgresql)

    "testImplementation"(libs.restassured)
  }
}

project("realworld-domain") {
  dependencies {
    "implementation".let {
      it(libs.jasypt)
      it(libs.jjwt)
      it(libs.slugify)
    }
  }
}

project("realworld-infra:persistence") {
  dependencies {
    "implementation".let {
      it(project(":realworld-domain"))

      it(starters.jdbc)

      it(libs.postgresql)
    }
  }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
  resolutionStrategy {
    componentSelection {
      all {
        val rejected = listOf("alpha", "b", "beta", "build-snapshot", "rc", "cr", "m", "preview")
          .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
          .any { it.matches(candidate.version) }
        if (rejected) {
          reject("Release candidate")
        }
      }
    }
  }
  checkForGradleUpdate = true
}
