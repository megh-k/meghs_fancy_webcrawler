# meghs_fancy_webcrawler

A simple coroutine based web-crawler that makes use of ktor and jsoup to discover all URLs on a domain.

## What does the Fancy Webcrawler do?

The fancy webcrawler is a very basic webcrawler that prompts the user for a url and returns all the relative urls on the domain.


## Getting Started

### Pre-requisites:
   - Java 21
   - Kotlin 2.1.21
   - Gradle 8.13

### Dependencies

  - Kotlin Coroutines
  - Ktor Client
  - Jsoup

### Dev Dependencies

  - Kotlin Test
  - JUnit 5
  - Ktor MockEngine

### Build and run

#### In the terminal 

Clone the repo, change directory to `meghs_fancy_webcrawler`. Now, since the app makes use of `readln`, gradle run does not work.
Instead, install the distribution and run the installed application.

```bash
git clone git@github.com:megh-k/meghs_fancy_webcrawler.git
cd meghs_fancy_webcrawler
./gradlew installDist
./build/install/meghs_fancy_webcrawler/bin/meghs_fancy_webcrawler
```
Woohoo! now you can start your webcrawling adventure.

If you would like to terminate the application, the command `exit` will stop the application running.

#### In IntelliJ

If you have IntelliJ installed, I recommend using Azul Zulu 21.0.7 as the SDK and just allowing IntelliJ to take the wheel

## Running Tests

Tests are more straightforward and can be run directly with gradle 🧪

```bash
./gradlew test
```

### File structure

```
src/
├── main/
│ ├── kotlin/
│ │ ├── WebCrawler.kt # Crawler class
│ │ ├── utils.kt # URL normalization and validation
│ │ └── Main.kt # application entry point
│
└── test/
├── kotlin/
│ ├── UtilsTest.kt # Unit tests for the utils
| └── WebCrawlerTest.kt # Unit tests for the WebCrawler
└── resources/
  └── fixtures/ # Static HTML files for testing
```