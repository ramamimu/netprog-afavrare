const net = require("net");

// Define the URL and the TCP port
const url = "www.example.com";
const port = 80;

// Create a TCP socket
const client = net.createConnection({ port: port, host: url }, () => {
  console.log("Connected to server!");

  // Send the HTTP request
  client.write("GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n");
});

let header = "";
let body = "";

// Handle the response from the server
client.on("data", async (data) => {
  // Convert the data to a string
  const response = data.toString();

  // Find the position of the header/body separator
  const separatorIndex = response.indexOf("\r\n\r\n");

  // If the separator is found, split the response into header and body sections
  if (separatorIndex >= 0) {
    header = response.slice(0, separatorIndex);
    body = response.slice(separatorIndex + 4);

    console.log("Header:", header);
    console.log("Body:", body);
  }
  await client.end();
});

// Handle errors
client.on("error", (err) => {
  console.error(err);
});

// Handle the connection being closed
client.on("end", () => {
  console.log("Connection closed!");
});
