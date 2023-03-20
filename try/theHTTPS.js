const tls = require("tls");

// host: "example.com",
const options = {
  host: "facebook.com",
  port: 443,
};

const client = tls.connect(options, () => {
  console.log("Connected to server!");

  // Send an HTTP request over the secure connection
  client.write(`GET / HTTP/1.1\r\nHost: ${options.host}\r\n\r\n`);
});

// Listen for data received from the server
client.on("data", (data) => {
  const response = data.toString();
  const [header, body] = response.split("\r\n\r\n");
  console.log(header);
  console.log(body);
});

// Listen for any errors that occur during the connection
client.on("error", (error) => {
  console.error(error);
});

// Close the connection when finished
client.on("end", () => {
  console.log("Disconnected from server.");
});
