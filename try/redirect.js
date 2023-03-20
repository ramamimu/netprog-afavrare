const net = require("net");

const client = net.createConnection({ port: 443, host: "intip.in" }, () => {
  client.write(`GET /GPOY/ HTTP/1.1\r\nHost: intip.in\r\n\r\n`);
});

const redirectedClient = net.createConnection({ port: 443, host: "intip.in" }, () => {
  redirectedClient.write(`GET /GPOY/ HTTP/1.1\r\nHost: intip.in\r\n\r\n`);
});

let responseHeaders = "";

client.on("data", (chunk) => {
  responseHeaders += chunk.toString();

  if (responseHeaders.includes("\r\n\r\n")) {
    const [responseStatusLine, ...responseHeaderLines] = responseHeaders.split("\r\n");
    const responseStatusCode = responseStatusLine.split(" ")[1];

    if (responseStatusCode === "301") {
      const locationHeader = responseHeaderLines.find((header) => header.startsWith("Location:"));
      const newUrl = locationHeader.split(" ")[1].trim();

      // Handle the redirect to the new URL here
      // ...
      console.log("locationHeader:", locationHeader);
      console.log("newUrl:", newUrl);
      net.createConnection({ port: 443, host: "intip.in" }, () => {
        redirectedClient.write(`GET /GPOY/ HTTP/1.1\r\nHost: intip.in\r\n\r\n`);
      });
    }
  }
});

client.on("end", () => {
  console.log(responseHeaders);
  console.log("Connection closed");
});

// const redirectedClient = net.createConnection({ port: 443, host: "intip.in" }, () => {
//   redirectedClient.write(`GET /GPOY/ HTTP/1.1\r\nHost: intip.in\r\n\r\n`);
// });
