# GoogleDocReader

GoogleDocReader is a Java command-line application that reads the contents of a Google Document using the Google Docs API and prints the document's title and text to the console. It is useful for extracting and displaying the text from Google Docs programmatically.

## Features
- Connects to Google Docs API using OAuth2 credentials
- Reads and prints the title and content of a specified Google Document
- Supports reading tables and table of contents

## Prerequisites
- Java 17 or later
- Maven
- A Google Cloud project with the Google Docs API enabled
- OAuth2 credentials file (`client_secret.json`) downloaded from Google Cloud Console

## Setup
1. Place your `client_secret.json` file in the `config` directory at the root of the project.
2. Make sure you have Maven and Java installed and available in your PATH.

## How to Run
1. Open a terminal in the project root (where `pom.xml` is located).
2. Run the following command to ensure UTF-8 output (important for non-ASCII characters):

   ```powershell
   chcp 65001; $OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::UTF8
   ```

3. Build and run the app with Maven:

   ```powershell
   mvn -U clean compile exec:java --% -Dexec.mainClass=com.example.GoogleDocReader -Dexec.args=<DOCUMENT_ID>
   ```
   Replace `<DOCUMENT_ID>` with the ID of the Google Doc you want to read. If omitted, a default document ID will be used.

## Example
```powershell
mvn -U clean compile exec:java --% -Dexec.mainClass=com.example.GoogleDocReader -Dexec.args=1ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijk123456789
```

## Notes
- The first time you run the app, a browser window will open for Google authentication.
- Tokens are stored in the `tokens` directory for future runs.
- Ensure your `client_secret.json` is valid and not committed to version control.

## License
This project is for educational and demonstration purposes.
