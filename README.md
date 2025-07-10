# meghs_fancy_webcrawler

A simple coroutine based web-crawler that makes use of ktor and jsoup to discover all URLs on a domain.

## Getting Started

### Pre-requisites:
   - Java 21
   - Kotlin 2.1.21
   - Gradle 8.13

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

#### In IntelliJ

If you have IntelliJ installed, I recommend using Azul Zulu 21.0.7 as the SDK and just allowing IntelliJ to take the wheel

## Running Tests

Tests are more straightforward and can be run directly with gradle 🧪

```bash
./gradlew test
```

